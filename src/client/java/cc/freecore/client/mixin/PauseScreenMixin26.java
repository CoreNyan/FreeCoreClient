package cc.freecore.client.mixin;

import cc.freecore.client.FreeCoreClientRuntime;
import cc.freecore.client.FreeCoreConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.server.dialog.Dialog;
import java.util.Optional;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin26 extends net.minecraft.client.gui.screens.Screen {
    private final long freecore$pauseOpenedAt = System.nanoTime();
    private int freecore$announcementPage = 0;
    private Button freecore$announcementPrev;
    private Button freecore$announcementNext;
    private int freecore$announcementScroll;
    private int freecore$announcementPanelX, freecore$announcementPanelY, freecore$announcementPanelW, freecore$announcementPanelH;
    private boolean freecore$announcementDragging;
    private int freecore$announcementDragOffset;

    /** Ensure wheel input reaches the announcement even though 26.2 provides
     * mouseScrolled as a ContainerEventHandler default method. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (freecore$isInsideAnnouncement(mouseX, mouseY)) {
            FreeCoreConfig cfg = FreeCoreClientRuntime.getConfig();
            int top = height < 420 ? 62 : 82;
            int bottom = height - (("manual".equalsIgnoreCase(cfg.announcementMode)
                    && cfg.announcements != null && cfg.announcements.size() > 1) ? 74 : 48);
            int max = freecore$pauseScrollMax(cfg, top, Math.max(top + 1, bottom));
            if (max > 0 && vertical != 0.0) {
                freecore$announcementScroll = Math.max(0, Math.min(max,
                        freecore$announcementScroll - (int) Math.signum(vertical) * 12));
                return true;
            }
        }
        return false;
    }
    protected PauseScreenMixin26(Component title) { super(title); }

    /**
     * Vanilla 26.2 dereferences minecraft.player while rebuilding the pause
     * menu. During a world transition or a resize the screen can briefly exist
     * after the player has been cleared, which otherwise crashes the client.
     */
    @Inject(method = "getCustomAdditions", at = @At("HEAD"), cancellable = true, require = 0)
    private void freecore$skipCustomAdditionsWithoutPlayer(
            CallbackInfoReturnable<Optional<? extends Holder<Dialog>>> cir) {
        if (net.minecraft.client.Minecraft.getInstance().player == null) {
            cir.setReturnValue(Optional.empty());
        }
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void freecore$injectButtons(CallbackInfo ci) {
        PauseScreen screen = (PauseScreen) (Object) this;
        freecore$announcementPage = 0;
        freecore$announcementScroll = 0;
        // Remove vanilla controls and the redundant "Game Menu" StringWidget.
        // The configured action deck below is the only pause-menu navigation.
        for (GuiEventListener child : java.util.List.copyOf(screen.children())) {
            if (child instanceof Button || child instanceof StringWidget) removeWidget(child);
        }
        freecore$announcementPrev = null;
        freecore$announcementNext = null;
        java.util.List<FreeCoreConfig.ButtonConfig> primary = new java.util.ArrayList<>();
        java.util.List<FreeCoreConfig.ButtonConfig> secondary = new java.util.ArrayList<>();
        java.util.List<FreeCoreConfig.ButtonConfig> utility = new java.util.ArrayList<>();
        for (FreeCoreConfig.ButtonConfig button : FreeCoreClientRuntime.getConfig().pauseButtons) {
            if (button == null || button.label == null) continue;
            String action = button.action == null ? "" : button.action;
            if ("resume".equalsIgnoreCase(action) || "continue".equalsIgnoreCase(action)) primary.add(button);
            else if ("disconnect".equalsIgnoreCase(action)) utility.add(button);
            else secondary.add(button);
        }
        // The pause screen has two independent columns: actions on the left,
        // announcements on the right. Derive every rectangle from the current
        // logical GUI size so shrinking the window cannot make cards collide.
        boolean compact = screen.height < 560 || screen.width < 900;
        int gap = compact ? 8 : 12;
        int leftArea = Math.max(180, screen.width / 2 - 44);
        int leftWidth = Math.min(compact ? 250 : 300, leftArea - 16);
        leftWidth = Math.max(180, leftWidth);
        int leftX = Math.max(12, (screen.width / 2 - leftWidth) / 2 - 4);
        int mainHeight = compact ? 34 : 40;
        int secondaryHeight = compact ? 28 : 32;
        int utilityHeight = compact ? 28 : 32;
        int secondaryColumns = (!compact && leftWidth >= 250 && secondary.size() > 1) ? 2 : 1;
        int secondaryRows = (secondary.size() + secondaryColumns - 1) / secondaryColumns;
        int requiredHeight = primary.size() * mainHeight
                + secondaryRows * secondaryHeight
                + utility.size() * utilityHeight
                + Math.max(0, primary.size() + secondaryRows + utility.size() - 1) * gap;
        // If a very short window still cannot fit, compress gaps before
        // reducing button heights; the cards remain clickable and distinct.
        if (requiredHeight > screen.height - 24) {
            gap = Math.max(4, (screen.height - 24
                    - (primary.size() * mainHeight + secondaryRows * secondaryHeight + utility.size() * utilityHeight))
                    / Math.max(1, primary.size() + secondaryRows + utility.size() - 1));
            requiredHeight = primary.size() * mainHeight + secondaryRows * secondaryHeight
                    + utility.size() * utilityHeight
                    + Math.max(0, primary.size() + secondaryRows + utility.size() - 1) * gap;
        }
        int cursorY = Math.max(12, (screen.height - requiredHeight) / 2);
        for (FreeCoreConfig.ButtonConfig button : primary) {
            freecore$addPauseButton(screen, button, leftX, cursorY, leftWidth, mainHeight, "primary");
            cursorY += mainHeight + gap;
        }
        int secondaryWidth = (leftWidth - (secondaryColumns - 1) * gap) / secondaryColumns;
        for (int i = 0; i < secondary.size(); i++) {
            FreeCoreConfig.ButtonConfig button = secondary.get(i);
            int col = i % secondaryColumns;
            int row = i / secondaryColumns;
            freecore$addPauseButton(screen, button, leftX + col * (secondaryWidth + gap),
                    cursorY + row * (secondaryHeight + gap), secondaryWidth, secondaryHeight, "secondary");
        }
        if (!secondary.isEmpty()) cursorY += ((secondary.size() + secondaryColumns - 1) / secondaryColumns) * (secondaryHeight + gap);
        // A lone utility action (currently “断开连接”) must occupy the same
        // full card width as the action deck above. The previous rule forced
        // every compact layout into two columns, leaving a single button at
        // half width and visibly detached from the stack.
        int utilityColumns = utility.size() <= 1 ? 1 : ((compact || leftWidth < 250) ? 2 : 1);
        int utilityWidth = (leftWidth - (utilityColumns - 1) * gap) / utilityColumns;
        for (int i = 0; i < utility.size(); i++) {
            FreeCoreConfig.ButtonConfig button = utility.get(i);
            int col = i % utilityColumns;
            int row = i / utilityColumns;
            freecore$addPauseButton(screen, button,
                    leftX + col * (utilityWidth + gap), cursorY + row * (utilityHeight + gap),
                    utilityWidth, utilityHeight, "utility");
        }
        freecore$addAnnouncementControls(screen);
    }

    private void freecore$addAnnouncementControls(PauseScreen screen) {
        FreeCoreConfig cfg = FreeCoreClientRuntime.getConfig();
        if (!"manual".equalsIgnoreCase(cfg.announcementMode) || cfg.announcements == null || cfg.announcements.size() < 2) return;
        FreeCoreConfig.ButtonConfig prev = new FreeCoreConfig.ButtonConfig("上一页", "", "announcement_prev");
        prev.style = "announcement_nav"; prev.animationEnabled = false;
        FreeCoreConfig.ButtonConfig next = new FreeCoreConfig.ButtonConfig("下一页", "", "announcement_next");
        next.style = "announcement_nav"; next.animationEnabled = false;
        freecore$announcementPrev = new cc.freecore.client.FreeCoreButton26(0, 0, 52, 18, Component.literal("上一页"), prev, "announcement_nav", ignored -> {
            int count = FreeCoreClientRuntime.getConfig().announcements == null ? 0 : FreeCoreClientRuntime.getConfig().announcements.size();
            if (count > 0) freecore$announcementPage = (freecore$announcementPage - 1 + count) % count;
        });
        freecore$announcementNext = new cc.freecore.client.FreeCoreButton26(0, 0, 52, 18, Component.literal("下一页"), next, "announcement_nav", ignored -> {
            int count = FreeCoreClientRuntime.getConfig().announcements == null ? 0 : FreeCoreClientRuntime.getConfig().announcements.size();
            if (count > 0) freecore$announcementPage = (freecore$announcementPage + 1) % count;
        });
        addRenderableWidget(freecore$announcementPrev);
        addRenderableWidget(freecore$announcementNext);
        System.out.println("[FreeCoreClient] Manual pause announcement controls added");
    }

    private void freecore$addPauseButton(PauseScreen screen, FreeCoreConfig.ButtonConfig button,
                                         int defaultX, int defaultY, int defaultWidth, int defaultHeight, String style) {
            // PauseScreen uses a responsive action deck. Absolute per-button
            // dimensions (notably the disconnect button's old width=250)
            // belong to custom layouts and must not override the shared deck
            // geometry, otherwise resizing produces visibly different cards.
            int bw = defaultWidth;
            int bh = defaultHeight;
            int bx = button.x != null ? button.x : (button.xPercent != null ? Math.round(screen.width * button.xPercent - bw / 2f) : defaultX);
            int by = button.y != null ? button.y : (button.yPercent != null ? Math.round(screen.height * button.yPercent) : defaultY);
            bw = Math.max(96, Math.min(bw, Math.max(96, screen.width / 2 - 24)));
            bh = Math.max(22, Math.min(bh, Math.max(22, screen.height / 5)));
            bx = Math.max(12, Math.min(bx, screen.width - bw - 12));
            by = Math.max(12, Math.min(by, screen.height - bh - 12));
            addRenderableWidget(new cc.freecore.client.FreeCoreButton26(bx, by, bw, bh,
                    Component.literal(button.label), button, style,
                    ignored -> cc.freecore.client.GuiActions26.perform(screen, button)));
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void freecore$pauseAnnouncement(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var list = FreeCoreClientRuntime.getConfig().announcements;
        var cfg = FreeCoreClientRuntime.getConfig();
        if (list == null || list.isEmpty()) return;
        int index = Math.max(0, Math.min(freecore$announcementPage, list.size() - 1));
        if (list.size() > 1 && "carousel".equalsIgnoreCase(cfg.announcementMode)) {
            double interval = cfg.announcementIntervalSeconds == null ? 6.0 : Math.max(1.0, cfg.announcementIntervalSeconds);
            index = (int) ((System.nanoTime() / 1_000_000_000L / (long) interval) % list.size());
        }
        var a = list.get(index);
        if (a == null) return;
        int left = width / 2 + 20;
        freecore$announcementPanelX = left; freecore$announcementPanelY = 40;
        freecore$announcementPanelW = width - left - 20; freecore$announcementPanelH = height - 80;
        graphics.fill(left, 40, width - 20, height - 40, 0xCC101010);
        if ("manual".equalsIgnoreCase(cfg.announcementMode) && list.size() > 1) {
            graphics.fill(left + 8, height - 42, width - 28, height - 41, 0x66444444);
            graphics.centeredText(cc.freecore.client.FreeCoreText.font(),
                    (index + 1) + " / " + list.size(), (left + width - 20) / 2, height - 37, 0xff888888);
        }
        if (freecore$announcementPrev != null && freecore$announcementNext != null) {
            int bh = Math.max(14, Math.min(20, (height - 80) / 8));
            int bw = Math.max(48, Math.min(72, (width - left - 36) / 3));
            // The announcement panel ends at height-40 and is drawn after the
            // widgets, so place navigation in the clear bottom margin.
            int by = height - 40 - bh - 3;
            freecore$announcementPrev.setX(left + 10); freecore$announcementPrev.setY(by);
            freecore$announcementPrev.setWidth(bw); freecore$announcementPrev.setHeight(bh);
            freecore$announcementNext.setX(width - bw - 30); freecore$announcementNext.setY(by);
            freecore$announcementNext.setWidth(bw); freecore$announcementNext.setHeight(bh);
        }
        String title = a.title == null ? "" : a.title;
        if (Boolean.TRUE.equals(cfg.announcementShowDate) && a.date != null && !a.date.isBlank()) title += "  ·  " + a.date;
        boolean compactAnnouncement = height < 420;
        int titleBaseline = compactAnnouncement ? 46 : 55;
        int contentTop = compactAnnouncement ? 62 : 82;
        int contentBottom = height - (("manual".equalsIgnoreCase(cfg.announcementMode) && list.size() > 1) ? 74 : 48);
        contentBottom = Math.max(contentTop + 1, contentBottom);
        int contentWidth = Math.max(40, width - left - 44);
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = a.content == null ? java.util.List.of()
                : cc.freecore.client.FreeCoreText.font().split(cc.freecore.client.FreeCoreText.component(a.content), contentWidth);
        int maxScroll = Math.max(0, lines.size() * 11 - Math.max(1, contentBottom - contentTop));
        freecore$announcementScroll = Math.max(0, Math.min(freecore$announcementScroll, maxScroll));
        if (!title.isBlank()) {
            // Keep title/date visible even in compact GUI scales. The body
            // starts below this baseline, so it cannot overlap the heading.
            graphics.centeredText(cc.freecore.client.FreeCoreText.font(), cc.freecore.client.FreeCoreText.component(title), (left + width - 20) / 2, titleBaseline, 0xFFFFFFFF);
        }
        graphics.enableScissor(left + 4, contentTop, width - 24, contentBottom);
        freecore$drawAnnouncementLines(graphics, lines, left + 12, contentTop, contentBottom, freecore$announcementScroll, 0xFFE0E0E0);
        graphics.disableScissor();
        if (maxScroll > 0 && !Boolean.FALSE.equals(cfg.announcementScrollbarEnabled)) {
            freecore$drawAnnouncementScrollbar(graphics, width - 30, contentTop, contentBottom,
                    freecore$announcementScroll, maxScroll, lines.size() * 11, cfg);
        }
        if ("manual".equalsIgnoreCase(cfg.announcementMode) && list.size() > 1) {
            int bh = Math.max(14, Math.min(20, (height - 80) / 8));
            int bw = Math.max(48, Math.min(72, (width - left - 36) / 3));
            int by = height - 40 - bh - 3;
            int color = 0xffc8c8c8;
            graphics.fill(left + 10, by - 3, width - 30, by - 2, 0x66444444);
            graphics.centeredText(cc.freecore.client.FreeCoreText.font(), "上一页", left + 10 + bw / 2, by + (bh - 8) / 2, color);
            graphics.centeredText(cc.freecore.client.FreeCoreText.font(), "下一页", width - 30 - bw / 2, by + (bh - 8) / 2, color);
            graphics.fill(left + 15, by + bh / 2, left + 20, by + bh / 2 + 1, color);
            graphics.fill(width - 40, by + bh / 2, width - 35, by + bh / 2 + 1, color);
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
        if (freecore$announcementPanelW <= 0 || mouseX < freecore$announcementPanelX || mouseX > freecore$announcementPanelX + freecore$announcementPanelW
                || mouseY < freecore$announcementPanelY || mouseY > freecore$announcementPanelY + freecore$announcementPanelH) return;
        FreeCoreConfig cfg = FreeCoreClientRuntime.getConfig();
        var list = cfg.announcements;
        if (list == null || list.isEmpty()) return;
        int index = list.size() > 1 && "carousel".equalsIgnoreCase(cfg.announcementMode)
                ? (int) ((System.nanoTime() / 1_000_000_000L / Math.max(1L, (long) (cfg.announcementIntervalSeconds == null ? 6.0 : cfg.announcementIntervalSeconds))) % list.size())
                : Math.max(0, Math.min(freecore$announcementPage, list.size() - 1));
        var a = list.get(index);
        if (a == null || a.content == null) return;
        int top = height < 420 ? 62 : 82;
        int bottom = height - (("manual".equalsIgnoreCase(cfg.announcementMode) && list.size() > 1) ? 74 : 48);
        bottom = Math.max(top + 1, bottom);
        int count = cc.freecore.client.FreeCoreText.font().split(cc.freecore.client.FreeCoreText.component(a.content), Math.max(40, width - freecore$announcementPanelX - 44)).size();
        int max = Math.max(0, count * 11 - Math.max(1, bottom - top));
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
        int top = height < 420 ? 62 : 82;
        int bottom = height - (("manual".equalsIgnoreCase(cfg.announcementMode)
                && cfg.announcements != null && cfg.announcements.size() > 1) ? 74 : 48);
        bottom = Math.max(top + 1, bottom);
        int max = freecore$pauseScrollMax(cfg, top, bottom);
        if (max <= 0) return;
        int sx = width - 34;
        if (mouseX < sx - 4 || mouseX > sx + 8 || mouseY < top || mouseY > bottom) return;
        int track = Math.max(1, bottom - top);
        int thumb = Math.max(10, track * track / Math.max(track, max + track));
        int thumbY = top + (track - thumb) * freecore$announcementScroll / max;
        if (mouseY >= thumbY - 2 && mouseY <= thumbY + thumb + 2) {
            freecore$announcementDragging = true;
            freecore$announcementDragOffset = (int) mouseY - thumbY;
        } else {
            freecore$announcementScroll = Math.max(0, Math.min(max,
                    Math.round((float) (mouseY - top - thumb / 2) * max / Math.max(1, track - thumb))));
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
        int top = height < 420 ? 62 : 82;
        int bottom = height - (("manual".equalsIgnoreCase(cfg.announcementMode)
                && cfg.announcements != null && cfg.announcements.size() > 1) ? 74 : 48);
        bottom = Math.max(top + 1, bottom);
        int max = freecore$pauseScrollMax(cfg, top, bottom);
        int track = Math.max(1, bottom - top);
        int thumb = Math.max(10, track * track / Math.max(track, max + track));
        int target = Math.round((float) (mouseY - top - freecore$announcementDragOffset) * max
                / Math.max(1, track - thumb));
        freecore$announcementScroll = Math.max(0, Math.min(max, target));
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

    private int freecore$pauseScrollMax(FreeCoreConfig cfg, int top, int bottom) {
        if (cfg.announcements == null || cfg.announcements.isEmpty()) return 0;
        int index = Math.max(0, Math.min(freecore$announcementPage, cfg.announcements.size() - 1));
        FreeCoreConfig.Announcement a = cfg.announcements.get(index);
        if (a == null || a.content == null) return 0;
        int count = cc.freecore.client.FreeCoreText.font().split(cc.freecore.client.FreeCoreText.component(a.content),
                Math.max(40, width - freecore$announcementPanelX - 44)).size();
        return Math.max(0, count * 11 - Math.max(1, bottom - top));
    }

    private boolean freecore$isInsideAnnouncement(double mouseX, double mouseY) {
        return mouseX >= freecore$announcementPanelX && mouseX <= freecore$announcementPanelX + freecore$announcementPanelW
                && mouseY >= freecore$announcementPanelY && mouseY <= freecore$announcementPanelY + freecore$announcementPanelH;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void freecore$pauseAnimation(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        double t = (System.nanoTime() - freecore$pauseOpenedAt) / 1_000_000_000.0;
        double pulse = (Math.sin(t * 1.4) + 1.0) * 0.5;
        int alpha = 10 + (int) (pulse * 18);
        int sweep = (int) ((Math.sin(t * 0.45) + 1.0) * 0.5 * (width + 160)) - 80;
        graphics.fillGradient(sweep, 0, sweep + 2, height, 0x00ffffff, (alpha << 24) | 0xffffff);
        int y = Math.max(12, height / 2 - 118);
        graphics.fill(width / 4 - 18, y, width / 4 - 16, y + 2, 0x44ffffff);
        graphics.fill(width * 3 / 4 + 16, y, width * 3 / 4 + 18, y + 2, 0x44ffffff);
    }
}
