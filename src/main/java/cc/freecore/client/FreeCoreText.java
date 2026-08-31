package cc.freecore.client;

import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/** Keeps configured labels in one place so all custom screens render the same text. */
public final class FreeCoreText {
    private FreeCoreText() {}

    public static String display(String value) {
        if (value == null || value.isBlank()) return value == null ? "" : value;
        return value;
    }

    public static Component component(String value) {
        // Use the same font description as vanilla widgets and settings screens.
        return Component.literal(display(value));
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }
}
