package cc.freecore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.TitleScreen;

public final class GuiActions26 {
    private GuiActions26() {}
    public static void perform(Screen parent, FreeCoreConfig.ButtonConfig button) {
        Minecraft mc = Minecraft.getInstance();
        String action = button.action == null ? "url" : button.action.toLowerCase();
        try {
            switch (action) {
                case "quit", "exit" -> mc.stop();
                case "resume", "continue" -> mc.setScreenAndShow(null);
                case "options", "settings" -> mc.setScreenAndShow(new OptionsScreen(parent, mc.options, false));
                case "singleplayer", "single_player" -> {
                    // Keep this reflection-based for mapping compatibility across 1.21.x.
                    Class<?> type = Class.forName("net.minecraft.client.gui.screens.worldselection.SelectWorldScreen");
                    java.lang.reflect.Constructor<?> ctor = type.getDeclaredConstructor(Screen.class);
                    mc.setScreenAndShow((Screen) ctor.newInstance(parent));
                }
                case "stats", "statistics" -> { if (mc.player != null) mc.setScreenAndShow(new StatsScreen(parent, mc.player.getStats())); }
                case "disconnect" -> {
                    // Never return to the stale PauseScreen after the world is torn down.
                    // Minecraft waits for the integrated server to save, then restores this screen;
                    // a fresh title screen is the only valid destination once level == null.
                    mc.disconnect(new TitleScreen(), false);
                }
                case "server", "join_server" -> {
                    ServerData data = new ServerData("FreeCore", button.value == null ? "mc.freecore.cc" : button.value, ServerData.Type.OTHER);
                    ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(data.ip), data, false, null);
                }
                case "copy" -> mc.keyboardHandler.setClipboard(button.value == null ? "" : button.value);
                default -> ConfirmLinkScreen.confirmLinkNow(parent, button.value, true);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
