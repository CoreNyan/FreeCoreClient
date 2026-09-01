package cc.freecore.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Gson-backed configuration model. No YAML conversion is involved. */
public final class FreeCoreConfig {
    public static final Gson GSON = new GsonBuilder().create();

    @SerializedName("window_title")
    public String windowTitle = "FreeCore - 自由核心";
    @SerializedName("icon_url")
    public String iconUrl = "YOUR_ICON_URL_HERE";
    @SerializedName("theme_color")
    public String themeColor = "black_and_white";
    /** cards = responsive website-like grid; custom = honor each button's x/y. */
    @SerializedName("button_layout") public String buttonLayout = "cards";
    @SerializedName("button_columns") public Integer buttonColumns = 3;
    @SerializedName("button_gap") public Integer buttonGap = 8;
    @SerializedName("button_card_width_percent") public Float buttonCardWidthPercent = 0.26f;
    @SerializedName("button_card_height") public Integer buttonCardHeight = 38;
    @SerializedName("featured_action") public String featuredAction = "server";
    @SerializedName("featured_button_height") public Integer featuredButtonHeight = 48;
    @SerializedName("featured_button_width_percent") public Float featuredButtonWidthPercent = 0.52f;
    /** Responsive width shared by the three-tier main-menu action deck. */
    @SerializedName("main_menu_width_percent") public Float mainMenuWidthPercent = 0.86f;
    @SerializedName("main_menu_max_width") public Integer mainMenuMaxWidth = 720;
    @SerializedName("primary_button_width_percent") public Float primaryButtonWidthPercent = 1.0f;
    @SerializedName("primary_button_height") public Integer primaryButtonHeight = 44;
    @SerializedName("secondary_button_height") public Integer secondaryButtonHeight = 34;
    @SerializedName("utility_button_height") public Integer utilityButtonHeight = 28;
    @SerializedName("utility_button_width") public Integer utilityButtonWidth = 144;
    @SerializedName("loading_screen_enabled") public boolean loadingScreenEnabled = true;
    @SerializedName("loading_screen_color") public int loadingScreenColor = 0xff000000;
    @SerializedName("loading_logo_scale") public int loadingLogoScale = 1;
    /** HTTP(S), file:// URI, or a local absolute/relative path under the game directory. */
    @SerializedName("background_url")
    public String backgroundUrl = "YOUR_BACKGROUND_URL_OR_LOCAL_PATH_HERE";
    @SerializedName("logo_url")
    public String logoUrl = "YOUR_LOGO_URL_HERE";
    @SerializedName("logo_x") public Integer logoX;
    @SerializedName("logo_y") public Integer logoY = 20;
    @SerializedName("logo_width") public Integer logoWidth = 120;
    /** Animated logo accents are independently configurable and remain opt-out compatible. */
    @SerializedName("logo_animation_enabled") public Boolean logoAnimationEnabled = true;
    @SerializedName("logo_animation_speed") public Float logoAnimationSpeed = 1.0f;
    @SerializedName("logo_orbit_radius") public Integer logoOrbitRadius = 18;
    @SerializedName("logo_glow_strength") public Integer logoGlowStrength = 42;
    @SerializedName("announcement_x") public Integer announcementX = 20;
    @SerializedName("announcement_x_percent") public Float announcementXPercent = 0.14f;
    @SerializedName("announcement_y") public Integer announcementY = 200;
    @SerializedName("announcement_width") public Integer announcementWidth;
    @SerializedName("announcement_width_percent") public Float announcementWidthPercent = 0.72f;
    @SerializedName("announcement_height") public Integer announcementHeight = 76;
    @SerializedName("announcement_y_percent") public Float announcementYPercent = 0.32f;
    @SerializedName("announcement_height_percent") public Float announcementHeightPercent = 0.17f;
    /** static shows the first item; carousel rotates through all items. */
    @SerializedName("announcement_mode") public String announcementMode = "carousel";
    @SerializedName("announcement_interval_seconds") public Float announcementIntervalSeconds = 6.0f;
    @SerializedName("announcement_show_date") public Boolean announcementShowDate = true;
    /** Minimum breathing room, in GUI pixels, between announcement text and pagination controls. */
    @SerializedName("announcement_navigation_gap") public Integer announcementNavigationGap = 12;
    /** Shows a scrollbar whenever the current announcement exceeds its viewport. */
    @SerializedName("announcement_scrollbar_enabled") public Boolean announcementScrollbarEnabled = true;
    @SerializedName("announcement_scrollbar_width") public Integer announcementScrollbarWidth = 3;
    @SerializedName("announcement_scrollbar_track_color") public String announcementScrollbarTrackColor = "#55333333";
    @SerializedName("announcement_scrollbar_thumb_color") public String announcementScrollbarThumbColor = "#FFAAAAAA";
    /** @deprecated use main_menu_buttons. Kept for backward compatibility. */
    @Deprecated
    public List<ButtonConfig> buttons = new ArrayList<>();
    @SerializedName("main_menu_buttons")
    public List<ButtonConfig> mainMenuButtons = new ArrayList<>();
    @SerializedName("pause_buttons")
    public List<ButtonConfig> pauseButtons = new ArrayList<>();
    public List<Announcement> announcements = new ArrayList<>();

    public static FreeCoreConfig defaults() {
        FreeCoreConfig config = new FreeCoreConfig();
        config.buttons.add(new ButtonConfig("官网", "https://freecore.cc", "url"));
        config.buttons.add(new ButtonConfig("个人中心", "https://account.freecore.cc", "url"));
        config.buttons.add(new ButtonConfig("官方QQ群", "1085070135", "copy"));
        config.mainMenuButtons.add(new ButtonConfig("加入服务器", "mc.freecore.cc", "server"));
        config.mainMenuButtons.add(new ButtonConfig("单人游戏", "", "singleplayer"));
        config.mainMenuButtons.add(new ButtonConfig("官网", "https://freecore.cc", "url"));
        config.mainMenuButtons.add(new ButtonConfig("个人中心", "https://account.freecore.cc", "url"));
        config.mainMenuButtons.add(new ButtonConfig("官方QQ群", "1085070135", "copy"));
        config.mainMenuButtons.add(new ButtonConfig("设置", "", "options"));
        config.mainMenuButtons.add(new ButtonConfig("退出游戏", "", "quit"));
        config.pauseButtons.add(new ButtonConfig("继续游戏", "", "resume"));
        config.pauseButtons.add(new ButtonConfig("统计信息", "", "stats"));
        config.pauseButtons.add(new ButtonConfig("官网", "https://freecore.cc", "url"));
        config.pauseButtons.add(new ButtonConfig("个人中心", "https://account.freecore.cc", "url"));
        config.pauseButtons.add(new ButtonConfig("QQ交流群", "1085070135", "copy"));
        config.pauseButtons.add(new ButtonConfig("断开连接", "", "disconnect"));
        return config;
    }

    public List<ButtonConfig> getMainMenuButtons() {
        return mainMenuButtons != null && !mainMenuButtons.isEmpty() ? mainMenuButtons : buttons;
    }

    /**
     * Removes null/empty entries Gson may produce when a remote JSON array has
     * a trailing comma (for example: [{...},]).  Keeping the model normalized
     * prevents phantom announcement pages and null dereferences in screens.
     */
    public void sanitize() {
        if (announcements == null) announcements = new ArrayList<>();
        announcements.removeIf(item -> item == null
                || (isBlank(item.title) && isBlank(item.content) && isBlank(item.date)));
        if (buttons == null) buttons = new ArrayList<>();
        if (mainMenuButtons == null) mainMenuButtons = new ArrayList<>();
        if (pauseButtons == null) pauseButtons = new ArrayList<>();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static final class ButtonConfig {
        public String label;
        public String value;
        public String action;
        @SerializedName("featured") public Boolean featured;
        /** primary = server CTA, secondary = links, utility = settings/quit. */
        public String style;
        /** Optional second line, mainly used by the primary server CTA. */
        public String subtitle;
        /** HTTP(S), file:// or game-directory-relative image URL for this button. */
        @SerializedName("icon_url") public String iconUrl;
        public Integer x;
        public Integer y;
        public Integer width;
        public Integer height;
        /** Per-button visual parameters. Hex colors may be written as #RRGGBB or #AARRGGBB. */
        public String background;
        @SerializedName("background_hover") public String backgroundHover;
        public String border;
        @SerializedName("border_hover") public String borderHover;
        @SerializedName("text_color") public String textColor;
        @SerializedName("text_hover") public String textHover;
        @SerializedName("shadow_color") public String shadowColor;
        @SerializedName("shadow_offset") public Integer shadowOffset;
        @SerializedName("corner_cut") public Integer cornerCut;
        @SerializedName("icon_size") public Integer iconSize;
        @SerializedName("icon_offset_x") public Integer iconOffsetX;
        @SerializedName("icon_offset_y") public Integer iconOffsetY;
        @SerializedName("text_offset_x") public Integer textOffsetX;
        @SerializedName("text_offset_y") public Integer textOffsetY;
        @SerializedName("animation_enabled") public Boolean animationEnabled;
        @SerializedName("animation_speed") public Float animationSpeed;
        @SerializedName("animation_delay") public Integer animationDelay;
        @SerializedName("width_percent") public Float widthPercent;
        @SerializedName("height_percent") public Float heightPercent;
        @SerializedName("x_percent")
        public Float xPercent;
        @SerializedName("y_percent")
        public Float yPercent;

        public ButtonConfig() {}

        public ButtonConfig(String label, String value, String action) {
            this.label = label;
            this.value = value;
            this.action = action;
        }
    }

    public static final class Announcement {
        public String title;
        public String content;
        public String date;
    }
}
