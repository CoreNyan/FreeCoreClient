package cc.freecore.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;

import java.awt.Desktop;
import java.net.URI;

public final class GuiActions {
    private GuiActions() {}

    public static void perform(Screen parent, FreeCoreConfig.ButtonConfig button) {
        if (button == null || button.value == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if ("copy".equalsIgnoreCase(button.action)) {
            client.keyboardHandler.setClipboard(button.value);
            ConfigManager.toast(button.label == null ? "FreeCore" : button.label, "已复制: " + button.value);
            return;
        }
        if ("url".equalsIgnoreCase(button.action) || button.value.startsWith("http://") || button.value.startsWith("https://")) {
            client.setScreen(new ConfirmLinkScreen(confirmed -> {
                if (confirmed) try { Desktop.getDesktop().browse(URI.create(button.value)); } catch (Exception ex) { ex.printStackTrace(); }
                client.setScreen(parent);
            }, button.value, true));
            return;
        }
        ConfigManager.toast(button.label == null ? "FreeCore" : button.label, button.value);
    }
}
