package cc.freecore.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Asynchronously downloads per-button icon images and installs them as GUI textures. */
public final class ButtonIconManager {
    private static final Map<String, Identifier> IDS = new ConcurrentHashMap<>();
    private static final Map<String, int[]> SIZES = new ConcurrentHashMap<>();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private ButtonIconManager() {}

    public static Identifier get(String source) { return source == null ? null : IDS.get(source); }
    public static int width(String source) { return size(source, 0); }
    public static int height(String source) { return size(source, 1); }
    private static int size(String source, int index) {
        int[] size = source == null ? null : SIZES.get(source);
        return size == null ? 1 : Math.max(1, size[index]);
    }

    public static void loadAsync(String source, Minecraft minecraft) {
        if (source == null || source.isBlank() || source.startsWith("YOUR_")
                || IDS.containsKey(source) || !IN_FLIGHT.add(source)) return;
        RemoteIconCache.loadAsync(source, minecraft)
                .thenAcceptAsync(bytes -> install(source, bytes, minecraft), minecraft)
                .whenComplete((ignored, error) -> IN_FLIGHT.remove(source))
                .exceptionally(error -> { System.err.println("[FreeCoreClient] Button icon failed: " + source + " -> " + error); return null; });
    }

    private static void install(String source, byte[] bytes, Minecraft minecraft) {
        if (bytes == null) return;
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
            NativeImage image = NativeImage.read(input);
            // Material's PNG exports are black-on-transparent. Re-tint the
            // opaque pixels to white at decode time so they remain legible on
            // the black glass cards, while preserving anti-aliased alpha.
            for (int yy = 0; yy < image.getHeight(); yy++) {
                for (int xx = 0; xx < image.getWidth(); xx++) {
                    int pixel = image.getPixel(xx, yy);
                    int alpha = pixel & 0xFF000000;
                    if (alpha != 0) image.setPixelABGR(xx, yy, alpha | 0x00FFFFFF);
                }
            }
            Identifier id = Identifier.fromNamespaceAndPath("freecoreclient", "button_icon_" + Integer.toUnsignedString(source.hashCode()));
            minecraft.getTextureManager().register(id, new DynamicTexture(() -> "freecore_button_icon", image));
            IDS.put(source, id);
            SIZES.put(source, new int[]{image.getWidth(), image.getHeight()});
            System.out.println("[FreeCoreClient] Button icon installed: " + source + " " + image.getWidth() + "x" + image.getHeight());
        } catch (Exception error) {
            System.err.println("[FreeCoreClient] Button icon decode failed: " + source + " -> " + error);
        }
    }
}
