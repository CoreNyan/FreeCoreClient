package cc.freecore.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** A compact glass-style button matching the FreeCore website visual language. */
public final class FreeCoreButton26 extends Button {
    private final String iconUrl;
    private final String subtitle;
    private final String style;
    private final FreeCoreConfig.ButtonConfig buttonConfig;
    private final long createdAt = System.nanoTime();

    /** Configuration-first constructor. All visual overrides are read from JSON. */
    public FreeCoreButton26(int x, int y, int width, int height, Component message,
                            FreeCoreConfig.ButtonConfig config, String fallbackStyle, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.buttonConfig = config;
        this.iconUrl = config == null || config.iconUrl == null ? "" : config.iconUrl;
        this.subtitle = config == null || config.subtitle == null ? "" : config.subtitle;
        this.style = config != null && config.style != null && !config.style.isBlank()
                ? config.style.toLowerCase(java.util.Locale.ROOT)
                : (fallbackStyle == null ? "secondary" : fallbackStyle);
    }

    /** Compatibility constructor for integrations compiled against the older API. */
    public FreeCoreButton26(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.buttonConfig = null;
        this.iconUrl = "";
        this.subtitle = "";
        this.style = "secondary";
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        boolean hot = isHoveredOrFocused();
        if ("announcement_nav".equals(style)) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            int color = hot ? 0xffffffff : 0xffb8b8b8;
            int line = hot ? 0xfff5f5f5 : 0xff666666;
            graphics.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, line);
            graphics.centeredText(FreeCoreText.font(), getMessage(), x + w / 2, y + (h - 8) / 2, color);
            if (w >= 42) {
                boolean next = getMessage().getString().contains("下一");
                int ax = next ? x + w - 10 : x + 6;
                int ay = y + h / 2;
                if (next) {
                    graphics.fill(ax - 3, ay - 3, ax, ay - 2, color);
                    graphics.fill(ax - 1, ay - 2, ax + 2, ay, color);
                    graphics.fill(ax - 3, ay + 1, ax, ay + 2, color);
                } else {
                    graphics.fill(ax, ay - 3, ax + 3, ay - 2, color);
                    graphics.fill(ax - 2, ay - 2, ax + 1, ay, color);
                    graphics.fill(ax, ay + 1, ax + 3, ay + 2, color);
                }
            }
            return;
        }
        boolean primary = "primary".equals(style);
        boolean utility = "utility".equals(style);
        long now = System.nanoTime();
        double speed = buttonConfig == null || buttonConfig.animationSpeed == null ? 1.0 : Math.max(0.05, buttonConfig.animationSpeed);
        boolean animate = buttonConfig == null || buttonConfig.animationEnabled == null || buttonConfig.animationEnabled;
        double wave = animate ? (Math.sin(now / (420_000_000.0 / speed)) + 1.0) * 0.5 : 0.0;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        // Staggered entrance gives the action deck a deliberate, restrained arrival.
        long delay = buttonConfig != null && buttonConfig.animationDelay != null
                ? Math.max(0, buttonConfig.animationDelay)
                : Math.abs((long) getMessage().getString().hashCode() % 5L) * 65L;
        float enter = Math.max(0f, Math.min(1f, (now - createdAt) / 1_000_000_000f - delay / 1000f));
        enter = enter * enter * (3f - 2f * enter);
        y += Math.round((1f - enter) * 8f);

        // Chamfered panels avoid the generic rectangular Minecraft-button look.
        int lift = hot && !utility ? 1 : 0;
        y -= lift;
        int cut = buttonConfig != null && buttonConfig.cornerCut != null ? buttonConfig.cornerCut : (primary ? 7 : utility ? 5 : 6);
        int shadow = color(buttonConfig == null ? null : buttonConfig.shadowColor, 0x55000000);
        int shadowOffset = buttonConfig == null || buttonConfig.shadowOffset == null ? 3 : Math.max(0, buttonConfig.shadowOffset);
        freecore$chamfer(graphics, x + shadowOffset, y + shadowOffset, w, h, cut, shadow, shadow);
        int border = color(buttonConfig == null ? null : (hot ? buttonConfig.borderHover : buttonConfig.border), primary ? (hot ? 0xffffffff : 0xffdedede)
                : utility ? (hot ? 0xffa8a8a8 : 0xff454545)
                : (hot ? 0xfff7f7f7 : 0xff626262));
        int stroke = primary ? 2 : 1;
        freecore$chamfer(graphics, x, y, w, h, cut, border, border);
        int top = color(buttonConfig == null ? null : (hot ? buttonConfig.backgroundHover : buttonConfig.background), primary ? (hot ? 0xff4a4a4a : 0xff292929)
                : utility ? (hot ? 0xff242424 : 0xff111111)
                 : (hot ? 0xff303030 : 0xff1a1a1a));
        int bottom = color(buttonConfig == null ? null : (hot ? buttonConfig.backgroundHover : buttonConfig.background), primary ? 0xff070707 : utility ? 0xff080808 : (hot ? 0xff111111 : 0xff0c0c0c));
        int inset = utility ? 1 : stroke;
        freecore$chamferGradient(graphics, x + inset, y + inset, w - inset * 2, h - inset * 2, Math.max(2, cut - inset), top, bottom);
        if (primary) {
            graphics.fill(x + 2, y + 3, x + 4, y + h - 3, 0xffffffff);
        } else if (utility) {
            graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, hot ? 0xffb8b8b8 : 0xff555555);
        }
        if (primary) {
            // A hairline pulse keeps the CTA alive without a distracting reflective band.
            int pulseX = x + 10 + (int) ((Math.sin(now / 1_200_000_000.0) + 1.0) * 0.5 * Math.max(1, w - 24));
            graphics.fill(pulseX, y + 2, Math.min(x + w - 2, pulseX + 1), y + 3, hot ? 0x66ffffff : 0x28ffffff);
            graphics.fill(x + 1, y + 3, x + 4, y + h - 3, 0xffffffff);
            int pulse = 0x88 + (int) (wave * 0x77);
            graphics.fill(x + w - 22, y + h / 2 - 2, x + w - 18, y + h / 2 + 2, (pulse << 24) | 0xffffff);
        }

        int marker = hot ? 0xffffffff : 0xff858585;
        int defaultIconBox = primary ? Math.min(30, Math.max(22, h - 8)) : utility ? Math.min(18, Math.max(14, h - 8)) : Math.min(24, Math.max(18, h - 8));
        int iconBox = buttonConfig != null && buttonConfig.iconSize != null ? Math.max(8, Math.min(h - 4, buttonConfig.iconSize)) : defaultIconBox;
        int iconX = x + 8 + (buttonConfig == null || buttonConfig.iconOffsetX == null ? 0 : buttonConfig.iconOffsetX);
        int iconY = y + (h - iconBox) / 2 + (buttonConfig == null || buttonConfig.iconOffsetY == null ? 0 : buttonConfig.iconOffsetY);
        if (!utility) {
            graphics.fill(iconX, iconY, iconX + iconBox, iconY + 1, hot ? 0xffeeeeee : 0xff555555);
            graphics.fill(iconX, iconY + iconBox - 1, iconX + iconBox, iconY + iconBox, hot ? 0xffeeeeee : 0xff555555);
            graphics.fill(iconX, iconY, iconX + 1, iconY + iconBox, hot ? 0xffeeeeee : 0xff555555);
            graphics.fill(iconX + iconBox - 1, iconY, iconX + iconBox, iconY + iconBox, hot ? 0xffeeeeee : 0xff555555);
        }
        Identifier remoteIcon = ButtonIconManager.get(iconUrl);
        if (remoteIcon == null && !iconUrl.isBlank() && !iconUrl.startsWith("YOUR_")) {
            ButtonIconManager.loadAsync(iconUrl, Minecraft.getInstance());
        }
        if (remoteIcon != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, remoteIcon, iconX + 2, iconY + 2,
                    0f, 0f, iconBox - 4, iconBox - 4,
                    ButtonIconManager.width(iconUrl), ButtonIconManager.height(iconUrl),
                    ButtonIconManager.width(iconUrl), ButtonIconManager.height(iconUrl), -1);
        }
        if (!utility) {
            graphics.fill(x + w - 13, y + h / 2 - 4, x + w - 11, y + h / 2 - 2, marker);
            graphics.fill(x + w - 11, y + h / 2 - 2, x + w - 9, y + h / 2, marker);
            graphics.fill(x + w - 9, y + h / 2, x + w - 7, y + h / 2 + 2, marker);
        }
        int textColor = color(buttonConfig == null ? null : (hot ? buttonConfig.textHover : buttonConfig.textColor), isActive() ? 0xfff5f5f5 : 0xffa7a7a7);
        if (hot && (buttonConfig == null || buttonConfig.textHover == null || buttonConfig.textHover.isBlank())) textColor = 0xffffffff;
        int textCenter = x + (w + iconBox + 10) / 2 + (buttonConfig == null || buttonConfig.textOffsetX == null ? 0 : buttonConfig.textOffsetX);
        int textOffsetY = buttonConfig == null || buttonConfig.textOffsetY == null ? 0 : buttonConfig.textOffsetY;
        Component displayMessage = FreeCoreText.component(getMessage().getString());
        if (!utility && !subtitle.isBlank() && h >= 34) {
            graphics.centeredText(FreeCoreText.font(), displayMessage, textCenter, y + h / 2 - 8 + textOffsetY, textColor);
            graphics.centeredText(FreeCoreText.font(), FreeCoreText.component(subtitle), textCenter, y + h / 2 + 4 + textOffsetY, hot ? 0xffdddddd : 0xff9c9c9c);
        } else {
            graphics.centeredText(FreeCoreText.font(), displayMessage, textCenter, y + (h - 8) / 2 + textOffsetY, textColor);
        }
    }

    private static int color(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String s = raw.trim().replace("#", "");
        try {
            long value = Long.parseLong(s, 16);
            if (s.length() <= 6) value |= 0xff000000L;
            return (int) value;
        } catch (NumberFormatException ignored) { return fallback; }
    }

    private static void freecore$chamfer(GuiGraphicsExtractor g, int x, int y, int w, int h, int cut, int top, int bottom) {
        freecore$chamferGradient(g, x, y, w, h, cut, top, bottom);
    }

    private static void freecore$chamferGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int cut, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        // Keep the chamfer proportional to the actual button height.  A fixed
        // 8-12px cut on compact 15-20px buttons produces pointed, visibly
        // skewed shapes; capping it at a quarter of the height keeps every
        // responsive size in the same visual family.
        int c = Math.max(1, Math.min(cut, Math.min(w / 6, Math.max(2, h / 4))));
        for (int row = 0; row < h; row++) {
            int inset = row < c ? c - row : row >= h - c ? row - (h - c - 1) : 0;
            int left = x + inset;
            int right = x + w - inset;
            float t = h <= 1 ? 0f : row / (float) (h - 1);
            g.fill(left, y + row, Math.max(left + 1, right), y + row + 1, freecore$mix(top, bottom, t));
        }
    }

    private static int freecore$mix(int a, int b, float t) {
        int aa = (a >>> 24) & 0xff, ar = (a >>> 16) & 0xff, ag = (a >>> 8) & 0xff, ab = a & 0xff;
        int ba = (b >>> 24) & 0xff, br = (b >>> 16) & 0xff, bg = (b >>> 8) & 0xff, bb = b & 0xff;
        int ca = Math.round(aa + (ba - aa) * t), cr = Math.round(ar + (br - ar) * t);
        int cg = Math.round(ag + (bg - ag) * t), cb = Math.round(ab + (bb - ab) * t);
        return (ca << 24) | (cr << 16) | (cg << 8) | cb;
    }

}
