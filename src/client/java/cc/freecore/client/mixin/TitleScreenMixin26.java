package cc.freecore.client.mixin;

import cc.freecore.client.FreeCoreClientRuntime;
import cc.freecore.client.FreeCoreConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin26 extends net.minecraft.client.gui.screens.Screen {
    private int freecore$layoutWidth = -1;
    private int freecore$layoutHeight = -1;
    private int freecore$announcementPage = 0;
    private Button freecore$announcementPrev;
    private Button freecore$announcementNext;
    private boolean freecore$updateDialogShown;
    private int freecore$announcementScroll;
    private int freecore$announcementPanelX, freecore$announcementPanelY, freecore$announcementPanelW, freecore$announcementPanelH;
    private int freecore$lastAnnouncementIndex = -1;
    private boolean freecore$announcementDragging;
    private int freecore$announcementDragOffset;

    /** 26.2 defines this as a ContainerEventHandler default method; override
     * it directly so the announcement always receives wheel input. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (freecore$announcementPanelW > 0 && freecore$isInsideAnnouncement(mouseX, mouseY)) {
            int max = freecore$scrollMax(FreeCoreClientRuntime.getConfig());
            if (max > 0 && vertical != 0.0) {
                freecore$announcementScroll = Math.max(0, Math.min(max,
                        freecore$announcementScroll - (int) Math.signum(vertical) * 12));
                return true;
            }
        }
        return false;
    }

    protected TitleScreenMixin26(Component title) { super(title); }
    @Inject(method = "init", at = @At("TAIL"))
    private void freecore$injectButtons(CallbackInfo ci) {
        // A title screen can be recreated after connecting/disconnecting. Do
        // not reuse the previous instance's resize/layout bookkeeping.
        freecore$layoutWidth = -1;
        freecore$layoutHeight = -1;
        freecore$announcementPage = 0;
        freecore$announcementScroll = 0;
        freecore$lastAnnouncementIndex = -1;
        freecore$rebuildButtons((TitleScreen) (Object) this);
    }

    private void freecore$rebuildButtons(TitleScreen screen) {
        for (GuiEventListener child : java.util.List.copyOf(screen.children())) {
            if (child instanceof Button) removeWidget(child);
        }
        freecore$announcementPrev = null;
        freecore$announcementNext = null;
        System.out.println("[FreeCoreClient] TitleScreen replacing vanilla buttons; configured="
                + FreeCoreClientRuntime.getConfig().getMainMenuButtons().size());
        FreeCoreConfig cfg = FreeCoreClientRuntime.getConfig();
        boolean cards = !"custom".equalsIgnoreCase(cfg.buttonLayout);
        java.util.List<FreeCoreConfig.ButtonConfig> explicitButtons = new java.util.ArrayList<>();
        java.util.List<FreeCoreConfig.ButtonConfig> primaryButtons = new java.util.ArrayList<>();
        java.util.List<FreeCoreConfig.ButtonConfig> secondaryButtons = new java.util.ArrayList<>();
        java.util.List<FreeCoreConfig.ButtonConfig> utilityButtons = new java.util.ArrayList<>();
        for (FreeCoreConfig.ButtonConfig candidate : cfg.getMainMenuButtons()) {
            if (candidate == null || candidate.label == null) continue;
            // Per-button coordinates are only authoritative in custom layout
            // mode. In responsive cards mode, using x/y from a previous window
            // size causes buttons to overlap after resize or reconnect.
            if (!cards && freecore$hasExplicitGeometry(candidate)) {
                explicitButtons.add(candidate);
                continue;
            }
            switch (freecore$style(candidate, cfg)) {
                case "primary" -> primaryButtons.add(candidate);
                case "utility" -> utilityButtons.add(candidate);
                default -> secondaryButtons.add(candidate);
            }
        }
        if (cards && screen.height < 300) {
            freecore$rebuildCompact(screen, cfg);
            freecore$addAnnouncementControls(screen, cfg);
            return;
        }
        int gap = Math.max(4, cfg.buttonGap == null ? 8 : cfg.buttonGap);
        if (screen.height < 400) gap = Math.min(gap, 5);
        if (!cards) {
            int index = 0;
            for (FreeCoreConfig.ButtonConfig button : cfg.getMainMenuButtons()) {
                if (button == null || button.label == null) continue;
                int bw = button.width != null ? button.width : (button.widthPercent != null ? Math.round(screen.width * button.widthPercent) : Math.min(320, screen.width - 40));
                int bh = button.height != null ? button.height : (button.heightPercent != null ? Math.round(screen.height * button.heightPercent) : 28);
                bw = Math.max(100, Math.min(bw, Math.max(100, screen.width - 24)));
                bh = Math.max(24, Math.min(bh, Math.max(24, screen.height / 8)));
                int bx = button.x != null ? button.x : (button.xPercent != null ? Math.round(screen.width * button.xPercent - bw / 2f) : screen.width / 2 - bw / 2);
                int by = button.y != null ? button.y : (button.yPercent != null ? Math.round(screen.height * button.yPercent) : screen.height / 2 - 20 + index * (bh + 4));
                bx = Math.max(12, Math.min(bx, screen.width - bw - 12));
                by = Math.max(12, Math.min(by, screen.height - bh - 12));
                freecore$addButton(screen, button, bx, by, bw, bh, freecore$style(button, cfg));
                index++;
            }
            freecore$addAnnouncementControls(screen, cfg);
            return;
        }

        float widthPercent = cfg.mainMenuWidthPercent == null ? 0.86f : Math.max(0.45f, Math.min(0.96f, cfg.mainMenuWidthPercent));
        int maxWidth = cfg.mainMenuMaxWidth == null ? 720 : Math.max(280, cfg.mainMenuMaxWidth);
        int contentWidth = Math.min(Math.max(180, screen.width - 24), Math.min(maxWidth, Math.round(screen.width * widthPercent)));
        int contentX = (screen.width - contentWidth) / 2;
        int primaryHeight = Math.max(32, Math.min(cfg.primaryButtonHeight == null ? 44 : cfg.primaryButtonHeight, screen.height < 400 ? 38 : 58));
        int secondaryHeight = Math.max(30, Math.min(cfg.secondaryButtonHeight == null ? 40 : cfg.secondaryButtonHeight, screen.height < 400 ? 34 : 46));
        int utilityHeight = Math.max(24, Math.min(cfg.utilityButtonHeight == null ? 28 : cfg.utilityButtonHeight, screen.height < 400 ? 26 : 38));
        int primaryColumns = 1;
        int configuredColumns = cfg.buttonColumns == null ? 3 : cfg.buttonColumns;
        // Four secondary cards read much better as a balanced 2x2 grid than
        // a 3+1 last row, and this remains responsive on narrow windows.
        if (secondaryButtons.size() == 4) configuredColumns = Math.min(configuredColumns, 2);
        int secondaryColumns = Math.max(1, Math.min(secondaryButtons.size(), Math.min(configuredColumns, contentWidth >= 270 ? 3 : 2)));
        int utilityColumns = Math.max(1, Math.min(2, utilityButtons.size()));
        int primaryRows = freecore$rows(primaryButtons.size(), primaryColumns);
        int secondaryRows = freecore$rows(secondaryButtons.size(), secondaryColumns);
        int utilityRows = freecore$rows(utilityButtons.size(), utilityColumns);
        int totalHeight = freecore$groupHeight(primaryRows, primaryHeight, gap)
                + freecore$groupHeight(secondaryRows, secondaryHeight, gap)
                + freecore$groupHeight(utilityRows, utilityHeight, gap);
        int populatedGroups = (primaryRows > 0 ? 1 : 0) + (secondaryRows > 0 ? 1 : 0) + (utilityRows > 0 ? 1 : 0);
        totalHeight += Math.max(0, populatedGroups - 1) * gap;
        int cursorY = Math.max(12, screen.height - totalHeight - 12);

        float primaryWidthPercent = cfg.primaryButtonWidthPercent == null ? 1.0f : Math.max(0.4f, Math.min(1.0f, cfg.primaryButtonWidthPercent));
        int primaryWidth = Math.max(180, Math.min(contentWidth, Math.round(contentWidth * primaryWidthPercent)));
        int primaryX = (screen.width - primaryWidth) / 2;
        for (int i = 0; i < primaryButtons.size(); i++) {
            int row = i / primaryColumns;
            freecore$addButton(screen, primaryButtons.get(i), primaryX, cursorY + row * (primaryHeight + gap), primaryWidth, primaryHeight, "primary");
        }
        if (primaryRows > 0) cursorY += freecore$groupHeight(primaryRows, primaryHeight, gap) + gap;

        if (secondaryRows > 0) {
            int secondaryWidth = (contentWidth - (secondaryColumns - 1) * gap) / secondaryColumns;
            for (int i = 0; i < secondaryButtons.size(); i++) {
                int col = i % secondaryColumns;
                int row = i / secondaryColumns;
                freecore$addButton(screen, secondaryButtons.get(i), contentX + col * (secondaryWidth + gap),
                        cursorY + row * (secondaryHeight + gap), secondaryWidth, secondaryHeight, "secondary");
            }
            cursorY += freecore$groupHeight(secondaryRows, secondaryHeight, gap) + gap;
        }

        if (utilityRows > 0) {
            int requestedUtilityWidth = cfg.utilityButtonWidth == null ? 144 : Math.max(96, cfg.utilityButtonWidth);
            int utilityWidth = Math.min(requestedUtilityWidth, (contentWidth - (utilityColumns - 1) * gap) / utilityColumns);
            int utilityGridWidth = utilityColumns * utilityWidth + (utilityColumns - 1) * gap;
            int utilityX = (screen.width - utilityGridWidth) / 2;
            for (int i = 0; i < utilityButtons.size(); i++) {
                int col = i % utilityColumns;
                int row = i / utilityColumns;
                freecore$addButton(screen, utilityButtons.get(i), utilityX + col * (utilityWidth + gap),
                        cursorY + row * (utilityHeight + gap), utilityWidth, utilityHeight, "utility");
            }
        }
        // Explicitly positioned buttons are rendered once, after the responsive
        // groups, so per-button geometry never gets duplicated or overwritten.
        for (FreeCoreConfig.ButtonConfig button : explicitButtons) {
            freecore$addConfiguredButton(screen, button);
        }
        freecore$addAnnouncementControls(screen, cfg);
    }

    private void freecore$addAnnouncementControls(TitleScreen screen, FreeCoreConfig cfg) {
        if (!"manual".equalsIgnoreCase(cfg.announcementMode) || cfg.announcements == null || cfg.announcements.size() < 2) return;
        FreeCoreConfig.ButtonConfig prev = new FreeCoreConfig.ButtonConfig("上一页", "", "announcement_prev");
        prev.style = "announcement_nav"; prev.animationEnabled = false;
        FreeCoreConfig.ButtonConfig next = new FreeCoreConfig.ButtonConfig("下一页", "", "announcement_next");
        next.style = "announcement_nav"; next.animationEnabled = false;
        freecore$announcementPrev = new cc.freecore.client.FreeCoreButton26(0, 0, 48, 18, Component.literal("上一页"), prev, "announcement_nav", ignored -> {
            int count = FreeCoreClientRuntime.getConfig().announcements == null ? 0 : FreeCoreClientRuntime.getConfig().announcements.size();
            if (count > 0) freecore$announcementPage = (freecore$announcementPage - 1 + count) % count;
        });
        freecore$announcementNext = new cc.freecore.client.FreeCoreButton26(0, 0, 48, 18, Component.literal("下一页"), next, "announcement_nav", ignored -> {
            int count = FreeCoreClientRuntime.getConfig().announcements == null ? 0 : FreeCoreClientRuntime.getConfig().announcements.size();
            if (count > 0) freecore$announcementPage = (freecore$announcementPage + 1) % count;
        });
        addRenderableWidget(freecore$announcementPrev);
        addRenderableWidget(freecore$announcementNext);
        System.out.println("[FreeCoreClient] Manual announcement controls added");
    }

    private void freecore$positionAnnouncementControls(int x, int y, int w, int h) {
        if (freecore$announcementPrev == null || freecore$announcementNext == null) return;
        int bh = Math.max(12, Math.min(18, h - 7));
        int bw = Math.max(38, Math.min(58, (w - 28) / 5));
        int by = Math.max(y + 2, y + h - bh - 3);
        freecore$announcementPrev.setX(x + 8); freecore$announcementPrev.setY(by);
        freecore$announcementPrev.setWidth(bw); freecore$announcementPrev.setHeight(bh);
        freecore$announcementNext.setX(x + w - bw - 8); freecore$announcementNext.setY(by);
        freecore$announcementNext.setWidth(bw); freecore$announcementNext.setHeight(bh);
    }

    private boolean freecore$isAnnouncementControl(GuiEventListener child) {
        return child == freecore$announcementPrev || child == freecore$announcementNext;
    }

    private static int freecore$rows(int count, int columns) {
        return count <= 0 ? 0 : (int) Math.ceil((double) count / Math.max(1, columns));
    }

    private static int freecore$groupHeight(int rows, int height, int gap) {
        return rows <= 0 ? 0 : rows * height + (rows - 1) * gap;
    }

    /** Packs every configured action below a reserved announcement strip. */
    private void freecore$rebuildCompact(TitleScreen screen, FreeCoreConfig cfg) {
        int gap = Math.max(1, Math.min(3, cfg.buttonGap == null ? 2 : cfg.buttonGap));
        boolean manualNav = "manual".equalsIgnoreCase(cfg.announcementMode)
                && cfg.announcements != null && cfg.announcements.size() > 1;
        int logoY = Math.max(4, Math.min(10, cfg.logoY == null ? 4 : cfg.logoY));
        int logoSize = Math.min(52, Math.max(32, Math.round(screen.height * 0.24f)));
        int announcementY = logoY + logoSize + 4;
        // Keep enough vertical room in compact mode for title + at least one
        // readable body line + the integrated pager. The old 40-44px panel
        // collapsed the text viewport to one pixel at GUI heights around 280,
        // making glyphs appear to overlap while scrolling.
        int announcementH = manualNav
                ? Math.min(80, Math.max(72, Math.round(screen.height * 0.27f)))
                : Math.min(54, Math.max(46, Math.round(screen.height * 0.18f)));
        int cursorY = Math.max(announcementY + announcementH + 10, Math.round(screen.height * 0.44f));
        cursorY = Math.min(cursorY, screen.height - 8);
        java.util.List<FreeCoreConfig.ButtonConfig> primary = new java.util.ArrayList<>();
        java.util.List<FreeCoreConfig.ButtonConfig> secondary = new java.util.ArrayList<>();
        java.util.List<FreeCoreConfig.ButtonConfig> utility = new java.util.ArrayList<>();
        for (FreeCoreConfig.ButtonConfig button : cfg.getMainMenuButtons()) {
            if (button == null || button.label == null) continue;
            switch (freecore$style(button, cfg)) {
                case "primary" -> primary.add(button);
                case "utility" -> utility.add(button);
                default -> secondary.add(button);
            }
        }
        int secondaryRows = secondary.size();
        int utilityRows = freecore$rows(utility.size(), Math.min(2, Math.max(1, utility.size())));
        int available = Math.max(1, screen.height - cursorY - 8);
        int desired = primary.size() * 24 + secondaryRows * 17 + utilityRows * 17
                + Math.max(0, primary.size() + secondaryRows + utilityRows - 1) * gap;
        float scale = Math.min(1.0f, available / (float) Math.max(1, desired));
        int primaryHeight = Math.max(18, Math.round(24 * scale));
        int secondaryHeight = Math.max(14, Math.round(17 * scale));
        int utilityHeight = Math.max(14, Math.round(17 * scale));
        for (FreeCoreConfig.ButtonConfig button : primary) {
            int bw = freecore$compactWidth(button, screen.width, 0.70f, 180);
            int bh = Math.min(freecore$compactHeight(button, screen.height, 0.10f, 18, 28), primaryHeight);
            int by = Math.max(12, Math.min(cursorY, screen.height - bh - 8));
            freecore$addButton(screen, button, (screen.width - bw) / 2, by, bw, bh, "primary");
            cursorY = by + bh + gap;
        }
        for (FreeCoreConfig.ButtonConfig button : secondary) {
            int bw = freecore$compactWidth(button, screen.width, 0.30f, 112);
            int bh = Math.min(freecore$compactHeight(button, screen.height, 0.055f, 14, 22), secondaryHeight);
            int by = Math.max(12, Math.min(cursorY, screen.height - bh - 8));
            freecore$addButton(screen, button, (screen.width - bw) / 2, by, bw, bh, "secondary");
            cursorY = by + bh + gap;
        }
        if (!utility.isEmpty()) {
            int columns = Math.min(2, utility.size());
            int requested = 0;
            for (FreeCoreConfig.ButtonConfig button : utility) {
                requested = Math.max(requested, freecore$compactWidth(button, screen.width, 0.22f, 88));
            }
            int utilityWidth = Math.min((screen.width - 24 - (columns - 1) * gap) / columns, Math.max(88, requested));
            for (int i = 0; i < utility.size(); i++) {
                FreeCoreConfig.ButtonConfig button = utility.get(i);
                int col = i % columns;
                int row = i / columns;
                int bh = Math.min(freecore$compactHeight(button, screen.height, 0.05f, 14, 22), utilityHeight);
                int by = Math.max(12, Math.min(cursorY + row * (bh + gap), screen.height - bh - 8));
                int gridWidth = columns * utilityWidth + (columns - 1) * gap;
                freecore$addButton(screen, button, (screen.width - gridWidth) / 2 + col * (utilityWidth + gap), by, utilityWidth, bh, "utility");
            }
        }
    }

    private static int freecore$compactWidth(FreeCoreConfig.ButtonConfig button, int screenWidth,
                                             float fallbackPercent, int minimum) {
        int width = button.width != null ? button.width
                : (button.widthPercent != null ? Math.round(screenWidth * button.widthPercent)
                : Math.round(screenWidth * fallbackPercent));
        return Math.max(Math.min(minimum, screenWidth - 24), Math.min(screenWidth - 24, width));
    }

    private static int freecore$compactHeight(FreeCoreConfig.ButtonConfig button, int screenHeight,
                                              float fallbackPercent, int minimum, int maximum) {
        int height = button.height != null ? button.height
                : (button.heightPercent != null ? Math.round(screenHeight * button.heightPercent)
                : Math.round(screenHeight * fallbackPercent));
        return Math.max(minimum, Math.min(maximum, height));
    }

    private static boolean freecore$hasExplicitGeometry(FreeCoreConfig.ButtonConfig b) {
        return b.x != null || b.y != null || b.width != null || b.height != null
                || b.xPercent != null || b.yPercent != null || b.widthPercent != null || b.heightPercent != null;
    }

    private void freecore$addConfiguredButton(TitleScreen screen, FreeCoreConfig.ButtonConfig button) {
        int guiWidth = screen.width, guiHeight = screen.height;
        int bw = button.width != null ? button.width : (button.widthPercent != null ? Math.round(guiWidth * button.widthPercent) : Math.min(320, guiWidth - 40));
        int bh = button.height != null ? button.height : (button.heightPercent != null ? Math.round(guiHeight * button.heightPercent) : 32);
        bw = Math.max(80, Math.min(bw, Math.max(80, guiWidth - 24)));
        bh = Math.max(20, Math.min(bh, Math.max(20, guiHeight / 4)));
        int bx = button.x != null ? button.x : (button.xPercent != null ? Math.round(guiWidth * button.xPercent - bw / 2f) : guiWidth / 2 - bw / 2);
        int by = button.y != null ? button.y : (button.yPercent != null ? Math.round(guiHeight * button.yPercent - bh / 2f) : guiHeight / 2 - bh / 2);
        bx = Math.max(12, Math.min(bx, guiWidth - bw - 12));
        by = Math.max(12, Math.min(by, guiHeight - bh - 12));
        freecore$addButton(screen, button, bx, by, bw, bh, freecore$style(button, FreeCoreClientRuntime.getConfig()));
    }

    private static String freecore$style(FreeCoreConfig.ButtonConfig button, FreeCoreConfig cfg) {
        if (button.style != null && !button.style.isBlank()) {
            String configured = button.style.toLowerCase(java.util.Locale.ROOT);
            if (configured.equals("primary") || configured.equals("utility")) return configured;
            return "secondary";
        }
        String action = button.action == null ? "" : button.action;
        String featuredAction = cfg.featuredAction == null ? "server" : cfg.featuredAction;
        if (Boolean.TRUE.equals(button.featured) || action.equalsIgnoreCase(featuredAction)) return "primary";
        if (action.equalsIgnoreCase("options") || action.equalsIgnoreCase("quit")) return "utility";
        return "secondary";
    }

    private void freecore$addButton(TitleScreen screen, FreeCoreConfig.ButtonConfig button,
                                    int x, int y, int width, int height, String style) {
        addRenderableWidget(new cc.freecore.client.FreeCoreButton26(x, y, width, height,
                Component.literal(button.label), button, style,
                ignored -> cc.freecore.client.GuiActions26.perform(screen, button)));
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void freecore$announcement(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;
        boolean layoutChanged = freecore$layoutWidth != width || freecore$layoutHeight != height;
        if (layoutChanged) {
            freecore$rebuildButtons(screen);
            freecore$layoutWidth = width;
            freecore$layoutHeight = height;
        }
        var cfg = FreeCoreClientRuntime.getConfig();
        String updateNotice = FreeCoreClientRuntime.getClientUpdateNotice();
        if (!freecore$updateDialogShown && updateNotice != null && !updateNotice.isBlank()) {
            freecore$updateDialogShown = true;
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            // A notification is informational, not a confirmation flow. Use a
            // buttonless overlay so it cannot obscure the menu or imply an
            // action the user must acknowledge.
            minecraft.setScreenAndShow(new net.minecraft.client.gui.screens.Screen(
                    cc.freecore.client.FreeCoreText.component("客户端更新")) {
                private int freecore$ticks;
                @Override public void tick() {
                    if (++freecore$ticks >= 120) {
                        FreeCoreClientRuntime.clearClientUpdateNotice();
                        minecraft.setScreenAndShow(screen);
                    }
                }
                @Override public boolean shouldCloseOnEsc() { return true; }
                @Override public void extractRenderState(GuiGraphicsExtractor overlay, int mouseX, int mouseY, float delta) {
                    overlay.fill(0, 0, width, height, 0x99000000);
                    int panelWidth = Math.min(width - 32, 620);
                    int panelHeight = 92;
                    int panelX = (width - panelWidth) / 2;
                    int panelY = (height - panelHeight) / 2;
                    overlay.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xee121212);
                    overlay.fill(panelX, panelY, panelX + panelWidth, panelY + 2, 0xfff2f2f2);
                    overlay.centeredText(cc.freecore.client.FreeCoreText.font(),
                            cc.freecore.client.FreeCoreText.component("客户端更新"), width / 2, panelY + 18, 0xffffffff);
                    var lines = cc.freecore.client.FreeCoreText.font().split(
                            cc.freecore.client.FreeCoreText.component(updateNotice), panelWidth - 28);
                    int y = panelY + 39;
                    for (var line : lines) {
                        if (y >= panelY + panelHeight - 8) break;
                        overlay.centeredText(cc.freecore.client.FreeCoreText.font(), line, width / 2, y, 0xffdddddd);
                        y += 11;
                    }
                }
            });
        }
        var announcements = cfg.announcements;
        if (announcements == null || announcements.isEmpty()) return;
        int announcementIndex = freecore$announcementIndex(cfg, announcements.size());
        var a = announcements.get(announcementIndex);
        if (a == null) return;
        if (announcementIndex != freecore$lastAnnouncementIndex) {
            freecore$announcementScroll = 0;
            freecore$lastAnnouncementIndex = announcementIndex;
        }
        int x = cfg.announcementXPercent != null ? Math.round(width * cfg.announcementXPercent)
                : (cfg.announcementX == null ? 20 : cfg.announcementX);
        int y = cfg.announcementYPercent != null ? Math.round(height * cfg.announcementYPercent)
                : (cfg.announcementY == null ? 200 : cfg.announcementY);
        int w = cfg.announcementWidthPercent != null ? Math.round(width * cfg.announcementWidthPercent)
                : (cfg.announcementWidth == null ? width - x - 20 : cfg.announcementWidth);
        int h = cfg.announcementHeightPercent != null ? Math.round(height * cfg.announcementHeightPercent)
                : (cfg.announcementHeight == null ? 76 : cfg.announcementHeight);
        w = Math.max(220, Math.min(w, width - 24));
        h = Math.max(76, Math.min(h, height / 2));
        x = Math.max(12, Math.min(x, width - w - 12));
        y = Math.max(12, Math.min(y, height - h - 12));

        // Keep the announcement from covering the configured logo when a
        // compact window or percentage combination would otherwise collide.
        int logoY = cfg.logoY == null ? 40 : cfg.logoY;
        int logoSize = Math.min(cfg.logoWidth == null ? 120 : Math.max(48, cfg.logoWidth), Math.max(48, Math.round(height * 0.30f)));
        if (height < 300) {
            logoY = Math.max(4, Math.min(logoY, 10));
            logoSize = Math.min(52, Math.max(32, Math.round(height * 0.24f)));
            x = 12;
            w = Math.max(160, width - 24);
            boolean compactManual = "manual".equalsIgnoreCase(cfg.announcementMode) && announcements.size() > 1;
            y = logoY + logoSize + 4;
            h = compactManual
                    ? Math.min(80, Math.max(72, Math.round(height * 0.27f)))
                    : Math.min(54, Math.max(46, Math.round(height * 0.18f)));
        }
        if (y < logoY + logoSize + 14 && x < width / 2 + logoSize / 2 && x + w > width / 2 - logoSize / 2) {
            y = Math.min(height - h - 12, logoY + logoSize + 14);
        }

        // Also avoid every configured button. Percentage layouts are commonly
        // tuned independently, so resolve the final rectangles at render time
        // and move or compact the announcement when they intersect.
        int firstButtonY = height;
        for (GuiEventListener child : children()) {
            if (child instanceof Button b && !freecore$isAnnouncementControl(child) && b.visible && b.getWidth() > 0 && b.getHeight() > 0) {
                firstButtonY = Math.min(firstButtonY, b.getY());
            }
        }
        if (height >= 300 && firstButtonY < height) {
            int minimum = Math.max(12, logoY + logoSize + 14);
            int available = firstButtonY - minimum - 14;
            if (available >= 64) {
                // Reserve a clear gap above the first button and fit the panel
                // into the remaining space, regardless of its original percent.
                h = Math.min(h, available);
                y = firstButtonY - h - 14;
            } else {
                // Very short windows get a compact panel rather than an overlap.
                // Reduce only the inter-panel gap (not the logo clearance) so a
                // 360px-high GUI still has room for announcement text.
                int compactMinimum = Math.max(12, logoY + logoSize + 4);
                y = compactMinimum;
                // 32 px is the smallest panel that still leaves a title row;
                // never force the old 76 px minimum into the button region.
                h = Math.max(32, Math.min(h, Math.max(32, firstButtonY - y - 14)));
            }
        }
        if (height < 300) {
            // Compact layout mirrors freecore$rebuildCompact: the panel has a
            // fixed reserved strip and the action stack starts below it.
            boolean manualControls = "manual".equalsIgnoreCase(cfg.announcementMode)
                    && announcements.size() > 1;
            y = Math.max(12, Math.min(y, height - (manualControls ? 80 : 54) - 8));
            h = manualControls
                    ? Math.min(80, Math.max(72, Math.round(height * 0.27f)))
                    : Math.min(54, Math.max(46, Math.round(height * 0.18f)));
        }
        y = Math.max(12, Math.min(y, height - h - 12));
        freecore$announcementPanelX = x; freecore$announcementPanelY = y;
        freecore$announcementPanelW = w; freecore$announcementPanelH = h;
        freecore$positionAnnouncementControls(x, y, w, h);
        if (layoutChanged) {
            System.out.println("[FreeCoreClient] announcement layout gui=" + width + "x" + height
                    + " panel=" + x + "," + y + "," + w + "x" + h + " firstButtonY=" + firstButtonY);
        }

        // Monochrome glass panel: neutral blacks and whites match the site
        // theme and remain legible over both the grid and remote backgrounds.
        graphics.fillGradient(x + 3, y + 3, x + w + 3, y + h + 3, 0x66000000, 0x33000000);
        graphics.fillGradient(x, y, x + w, y + h, 0xee181818, 0xee090909);
        graphics.fill(x, y, x + 3, y + h, 0xfff2f2f2);
        graphics.fill(x + 3, y, x + w, y + 1, 0x99ffffff);
        graphics.fill(x + 3, y + h - 1, x + w, y + h, 0x55777777);
        graphics.fill(x + w - 42, y, x + w, y + 1, 0xffffffff);
        boolean manualNavigation = "manual".equalsIgnoreCase(cfg.announcementMode) && announcements.size() > 1;
        int navigationHeight = Math.max(12, Math.min(18, h - 7));
        int navigationTop = y + h - navigationHeight - 3;
        int navigationGap = Math.max(6, Math.min(40, cfg.announcementNavigationGap == null ? 12 : cfg.announcementNavigationGap));
        if (manualNavigation) {
            graphics.fill(x + 8, y + h - 18, x + w - 8, y + h - 17, 0x66444444);
            graphics.centeredText(cc.freecore.client.FreeCoreText.font(),
                    (announcementIndex + 1) + " / " + announcements.size(), x + w / 2, y + h - 13, 0xff888888);
        }
        String announcementTitle = a.title == null ? "" : a.title;
        if (Boolean.TRUE.equals(cfg.announcementShowDate) && a.date != null && !a.date.isBlank()) {
            announcementTitle += "  ·  " + a.date;
        }
        boolean compactAnnouncement = height < 300;
        // Keep the title/date visible at every size.  The body starts from a
        // fixed distance below the title baseline instead of using the old
        // 38px large-window offset, which created an oversized empty band.
        int titleBaseline = compactAnnouncement ? y + 5 : y + 7;
        int contentTop = compactAnnouncement ? y + 15 : y + 18;
        // Keep a visible breathing gap between the last text line and the
        // integrated pagination strip.
        int contentBottom = manualNavigation ? navigationTop - navigationGap : y + h - 6;
        if (contentBottom <= contentTop) contentBottom = contentTop + 1;
        int contentWidth = Math.max(40, w - 34);
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = a.content == null ? java.util.List.of()
                : cc.freecore.client.FreeCoreText.font().split(cc.freecore.client.FreeCoreText.component(a.content), contentWidth);
        int maxScroll = Math.max(0, lines.size() * 11 - Math.max(1, contentBottom - contentTop));
        freecore$announcementScroll = Math.max(0, Math.min(freecore$announcementScroll, maxScroll));
        // Draw the title outside the body clip. The body clip starts exactly
        // at contentTop so fractional scroll offsets can never paint a line
        // over the title/date row.
        if (compactAnnouncement) {
            if (!announcementTitle.isBlank()) graphics.centeredText(cc.freecore.client.FreeCoreText.font(), cc.freecore.client.FreeCoreText.component(announcementTitle), x + w / 2, titleBaseline, 0xfff5f5f5);
        } else {
            if (!announcementTitle.isBlank()) graphics.centeredText(cc.freecore.client.FreeCoreText.font(), cc.freecore.client.FreeCoreText.component(announcementTitle), x + w / 2, titleBaseline, 0xfff5f5f5);
        }
        graphics.enableScissor(x + 4, contentTop, x + w - 4, contentBottom);
        freecore$drawAnnouncementLines(graphics, lines, compactAnnouncement ? x + 12 : x + 14,
                contentTop, contentBottom, freecore$announcementScroll, 0xffd1d1d1);
        graphics.disableScissor();
        if (maxScroll > 0 && !Boolean.FALSE.equals(cfg.announcementScrollbarEnabled)) {
            freecore$drawAnnouncementScrollbar(graphics, x + w - 10, contentTop, contentBottom,
                    freecore$announcementScroll, maxScroll, lines.size() * 11, cfg);
        }
        if (manualNavigation) {
            freecore$drawAnnouncementNav(graphics, x, y, w, h);
        }
    }

    private static void freecore$drawAnnouncementLines(GuiGraphicsExtractor graphics,
                                                        java.util.List<net.minecraft.util.FormattedCharSequence> lines,
                                                        int x, int top, int bottom, int scroll, int color) {
        int first = Math.max(0, scroll / 11);
        int y = top - (scroll % 11);
        for (int i = first; i < lines.size() && y < bottom; i++, y += 11) {
            if (y + 10 >= top) graphics.text(cc.freecore.client.FreeCoreText.font(), lines.get(i), x, y, color);
        }
    }

    private static void freecore$drawAnnouncementScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                                                            int scroll, int maxScroll, int contentHeight, FreeCoreConfig cfg) {
        int track = Math.max(1, bottom - top);
        int thumb = Math.max(10, track * track / Math.max(track, contentHeight));
        int thumbY = top + (track - thumb) * scroll / Math.max(1, maxScroll);
        int width = Math.max(2, Math.min(8, cfg.announcementScrollbarWidth == null ? 3 : cfg.announcementScrollbarWidth));
        graphics.fill(x, top, x + width, bottom, freecore$color(cfg.announcementScrollbarTrackColor, 0x55333333));
        graphics.fill(x, thumbY, x + width, thumbY + thumb, freecore$color(cfg.announcementScrollbarThumbColor, 0xffaaaaaa));
    }

    private static int freecore$color(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            String s = value.trim();
            if (s.startsWith("#")) s = s.substring(1);
            long parsed = Long.parseLong(s, 16);
            if (s.length() <= 6) parsed |= 0xFF000000L;
            return (int) parsed;
        } catch (RuntimeException ignored) { return fallback; }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 0)
    private void freecore$announcementScroll(double mouseX, double mouseY, double horizontal, double vertical,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (freecore$announcementPanelW <= 0 || !freecore$isInsideAnnouncement(mouseX, mouseY)) return;
        FreeCoreConfig cfg = FreeCoreClientRuntime.getConfig();
        var list = cfg.announcements;
        if (list == null || list.isEmpty()) return;
        int index = freecore$announcementIndex(cfg, list.size());
        var a = list.get(index);
        if (a == null || a.content == null) return;
        int top = height < 300 ? freecore$announcementPanelY + 15 : freecore$announcementPanelY + 18;
        boolean manualNavigation = "manual".equalsIgnoreCase(cfg.announcementMode) && list.size() > 1;
        int navHeight = Math.max(12, Math.min(18, freecore$announcementPanelH - 7));
        int navTop = freecore$announcementPanelY + freecore$announcementPanelH - navHeight - 3;
        int navGap = Math.max(6, Math.min(40, cfg.announcementNavigationGap == null ? 12 : cfg.announcementNavigationGap));
        int bottom = manualNavigation ? navTop - navGap : freecore$announcementPanelY + freecore$announcementPanelH - 6;
        bottom = Math.max(top + 1, bottom);
        int lines = cc.freecore.client.FreeCoreText.font().split(cc.freecore.client.FreeCoreText.component(a.content), Math.max(40, freecore$announcementPanelW - 34)).size();
        int max = Math.max(0, lines * 11 - Math.max(1, bottom - top));
        if (max <= 0) return;
        freecore$announcementScroll = Math.max(0, Math.min(max, freecore$announcementScroll - (int) Math.signum(vertical) * 12));
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void freecore$announcementScrollbarClicked(MouseButtonEvent event, boolean doubleClick,
                                                       CallbackInfoReturnable<Boolean> cir) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (button != 0 || !freecore$isInsideAnnouncement(mouseX, mouseY)) return;
        FreeCoreConfig cfg = FreeCoreClientRuntime.getConfig();
        if (Boolean.FALSE.equals(cfg.announcementScrollbarEnabled)) return;
        int[] range = freecore$scrollRange(cfg);
        if (range[1] <= 0) return;
        int sx = freecore$announcementPanelX + freecore$announcementPanelW - 14;
        if (mouseX < sx - 4 || mouseX > sx + 8 || mouseY < range[0] || mouseY > range[1]) return;
        int top = range[0], bottom = range[1];
        int thumb = freecore$scrollThumbHeight(cfg, top, bottom);
        int thumbY = top + (bottom - top - thumb) * freecore$announcementScroll
                / Math.max(1, freecore$scrollMax(cfg));
        if (mouseY >= thumbY - 2 && mouseY <= thumbY + thumb + 2) {
            freecore$announcementDragging = true;
            freecore$announcementDragOffset = (int) mouseY - thumbY;
        } else {
            int target = Math.round((float) (mouseY - top - thumb / 2) * freecore$scrollMax(cfg)
                    / Math.max(1, bottom - top - thumb));
            freecore$announcementScroll = Math.max(0, Math.min(freecore$scrollMax(cfg), target));
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true, require = 0)
    private void freecore$announcementScrollbarDragged(MouseButtonEvent event,
                                                       double deltaX, double deltaY,
                                                       CallbackInfoReturnable<Boolean> cir) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (!freecore$announcementDragging || button != 0) return;
        FreeCoreConfig cfg = FreeCoreClientRuntime.getConfig();
        int[] range = freecore$scrollRange(cfg);
        int thumb = freecore$scrollThumbHeight(cfg, range[0], range[1]);
        int track = Math.max(1, range[1] - range[0] - thumb);
        int target = Math.round((float) (mouseY - range[0] - freecore$announcementDragOffset) * freecore$scrollMax(cfg) / track);
        freecore$announcementScroll = Math.max(0, Math.min(freecore$scrollMax(cfg), target));
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, require = 0)
    private void freecore$announcementScrollbarReleased(MouseButtonEvent event,
                                                         CallbackInfoReturnable<Boolean> cir) {
        int button = event.button();
        if (button == 0 && freecore$announcementDragging) {
            freecore$announcementDragging = false;
            cir.setReturnValue(true);
        }
    }

    private int[] freecore$scrollRange(FreeCoreConfig cfg) {
        int top = height < 300 ? freecore$announcementPanelY + 15 : freecore$announcementPanelY + 18;
        int bottom = freecore$announcementPanelY + freecore$announcementPanelH
                - 6;
        if ("manual".equalsIgnoreCase(cfg.announcementMode)
                && cfg.announcements != null && cfg.announcements.size() > 1) {
            int navHeight = Math.max(12, Math.min(18, freecore$announcementPanelH - 7));
            int navTop = freecore$announcementPanelY + freecore$announcementPanelH - navHeight - 3;
            int navGap = Math.max(6, Math.min(40, cfg.announcementNavigationGap == null ? 12 : cfg.announcementNavigationGap));
            bottom = navTop - navGap;
        }
        return new int[]{top, Math.max(top + 1, bottom)};
    }

    private int freecore$scrollMax(FreeCoreConfig cfg) {
        if (cfg.announcements == null || cfg.announcements.isEmpty()) return 0;
        int index = freecore$announcementIndex(cfg, cfg.announcements.size());
        FreeCoreConfig.Announcement a = cfg.announcements.get(index);
        if (a == null || a.content == null) return 0;
        int[] range = freecore$scrollRange(cfg);
        int lines = cc.freecore.client.FreeCoreText.font().split(
                cc.freecore.client.FreeCoreText.component(a.content), Math.max(40, freecore$announcementPanelW - 34)).size();
                return Math.max(0, lines * 11 - (range[1] - range[0]));
    }

    private int freecore$scrollThumbHeight(FreeCoreConfig cfg, int top, int bottom) {
        int track = Math.max(1, bottom - top);
        int content = Math.max(track, freecore$scrollMax(cfg) + track);
        return Math.max(10, track * track / content);
    }

    private boolean freecore$isInsideAnnouncement(double mouseX, double mouseY) {
        return mouseX >= freecore$announcementPanelX && mouseX <= freecore$announcementPanelX + freecore$announcementPanelW
                && mouseY >= freecore$announcementPanelY && mouseY <= freecore$announcementPanelY + freecore$announcementPanelH;
    }

    private void freecore$drawAnnouncementNav(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int bh = Math.max(12, Math.min(18, h - 7));
        int bw = Math.max(38, Math.min(58, (w - 28) / 5));
        int by = y + h - bh - 3;
        int color = 0xffc8c8c8;
        graphics.fill(x + 8, by - 3, x + w - 8, by - 2, 0x55444444);
        graphics.centeredText(cc.freecore.client.FreeCoreText.font(), "上一页", x + 8 + bw / 2, by + (bh - 8) / 2, color);
        graphics.centeredText(cc.freecore.client.FreeCoreText.font(), "下一页", x + w - 8 - bw / 2, by + (bh - 8) / 2, color);
        graphics.fill(x + 13, by + bh / 2, x + 18, by + bh / 2 + 1, color);
        graphics.fill(x + 13, by + bh / 2 - 3, x + 16, by + bh / 2, color);
        graphics.fill(x + w - 18, by + bh / 2, x + w - 13, by + bh / 2 + 1, color);
        graphics.fill(x + w - 16, by + bh / 2 - 3, x + w - 13, by + bh / 2, color);
    }

    private int freecore$announcementIndex(FreeCoreConfig cfg, int count) {
        if (count <= 1) return 0;
        if ("manual".equalsIgnoreCase(cfg.announcementMode)) return Math.max(0, Math.min(freecore$announcementPage, count - 1));
        double seconds = cfg.announcementIntervalSeconds == null ? 6.0 : Math.max(1.0, cfg.announcementIntervalSeconds);
        return (int) ((System.nanoTime() / 1_000_000_000L / (long) seconds) % count);
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V"))
    private void freecore$logo(LogoRenderer vanilla, GuiGraphicsExtractor graphics, int screenWidth, float alpha) {
        var cfg = FreeCoreClientRuntime.getConfig();
        int logoWidth = cfg.logoWidth == null ? 500 : cfg.logoWidth;
        if (height < 300) logoWidth = Math.min(52, Math.max(32, Math.round(height * 0.24f)));
        if (!cc.freecore.client.LogoManager.render(graphics, screenWidth, height,
                cfg.logoY == null ? 40 : cfg.logoY,
                logoWidth,
                cfg.logoX,
                cfg.logoAnimationEnabled == null || cfg.logoAnimationEnabled,
                cfg.logoAnimationSpeed == null ? 1.0f : cfg.logoAnimationSpeed,
                cfg.logoOrbitRadius == null ? 18 : cfg.logoOrbitRadius,
                cfg.logoGlowStrength == null ? 42 : cfg.logoGlowStrength)) {
            vanilla.extractRenderState(graphics, screenWidth, alpha);
        }
    }

    /** Remove the diagonal vanilla splash text; announcements are supplied by JSON. */
    @org.spongepowered.asm.mixin.injection.Redirect(method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SplashRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V"))
    private void freecore$hideVanillaSplash(SplashRenderer vanilla, GuiGraphicsExtractor graphics, int screenWidth, Font font, float alpha) { }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;extractPanorama(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"))
    private void freecore$background(TitleScreen self, GuiGraphicsExtractor graphics, float delta) {
        cc.freecore.client.BackgroundManager.render(graphics, width, height);
    }
}
