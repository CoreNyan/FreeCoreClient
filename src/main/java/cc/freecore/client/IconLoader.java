package cc.freecore.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class IconLoader {
    private static final HttpClient HTTP = imageHttpClient();
    private static HttpClient imageHttpClient() {
        try {
            X509TrustManager trust = new X509TrustManager() { public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];} public void checkClientTrusted(X509Certificate[] c,String a){} public void checkServerTrusted(X509Certificate[] c,String a){} };
            SSLContext ssl = SSLContext.getInstance("TLS"); ssl.init(null, new javax.net.ssl.TrustManager[]{trust}, new SecureRandom());
            return HttpClient.newBuilder().sslContext(ssl).connectTimeout(Duration.ofSeconds(8)).build();
        } catch (Exception e) { return HttpClient.newHttpClient(); }
    }
    private IconLoader() {}
    public static void loadAsync(String url, Minecraft minecraft) {
        if (url == null || url.isBlank() || url.startsWith("YOUR_")) return;
        System.out.println("[FreeCoreClient] Loading application icon: " + url);
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url + (url.contains("?") ? "&" : "?") + "_fc=" + System.currentTimeMillis()))
                        .header("Cache-Control", "no-cache").header("Pragma", "no-cache")
                        .timeout(Duration.ofSeconds(15)).GET().build();
                HttpResponse<byte[]> r = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (r.statusCode() / 100 != 2) { System.err.println("[FreeCoreClient] Icon HTTP " + r.statusCode()); return null; }
                System.out.println("[FreeCoreClient] Icon downloaded: " + r.body().length + " bytes");
                return r.body();
            } catch (Exception e) { e.printStackTrace(); return null; }
        }).thenAcceptAsync(bytes -> apply(bytes, minecraft), minecraft)
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
