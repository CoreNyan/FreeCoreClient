package cc.freecore.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;

public final class IconLoader {
    private IconLoader() {}
    public static void loadAsync(String url, Minecraft minecraft) {
        if (url == null || url.isBlank() || url.startsWith("YOUR_")) return;
        System.out.println("[FreeCoreClient] Loading application icon: " + url);
        RemoteIconCache.loadAsync(url, minecraft)
                .thenAcceptAsync(bytes -> apply(bytes, minecraft), minecraft)
                .exceptionally(error -> { System.err.println("[FreeCoreClient] Icon install task failed: " + error); return null; });
    }
    private static void apply(byte[] bytes, Minecraft minecraft) {
        if (bytes == null) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // A 1254x1254 PNG is larger than LWJGL's per-thread MemoryStack.
            // Allocate the encoded payload from native heap instead of the stack.
            ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
            encoded.put(bytes).flip();
            java.nio.IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), comp = stack.mallocInt(1);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, w, h, comp, 4);
            if (pixels == null) { System.err.println("[FreeCoreClient] STB could not decode icon: " + STBImage.stbi_failure_reason()); return; }
            GLFWImage.Buffer image = GLFWImage.malloc(1);
            try {
                image.position(0).width(w.get(0)).height(h.get(0)).pixels(pixels);
                Window window = minecraft.getWindow();
                if (window == null || window.handle() == 0L) throw new IllegalStateException("GLFW window is not ready");
                GLFW.glfwSetWindowIcon(window.handle(), image);
                System.out.println("[FreeCoreClient] Application icon installed: " + w.get(0) + "x" + h.get(0));
            } finally { image.free(); STBImage.stbi_image_free(pixels); MemoryUtil.memFree(encoded); }
        } catch (Throwable e) { System.err.println("[FreeCoreClient] Application icon install failed: " + e); e.printStackTrace(); }
    }
}
