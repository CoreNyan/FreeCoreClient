package cc.freecore.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class LogoManager {
    private static final HttpClient HTTP = imageHttpClient();
    private static volatile Identifier id;
    private static volatile int width, height;
    private LogoManager() {}
    public static void loadAsync(String url, Minecraft minecraft) {
        if (url == null || url.isBlank() || url.startsWith("YOUR_")) return;
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url + (url.contains("?") ? "&" : "?") + "_fc=" + System.currentTimeMillis()))
                        .header("Cache-Control", "no-cache").header("Pragma", "no-cache")
                        .timeout(Duration.ofSeconds(15)).GET().build();
                HttpResponse<byte[]> r = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (r.statusCode() / 100 != 2) { System.err.println("[FreeCoreClient] Logo HTTP " + r.statusCode()); return null; }
                System.out.println("[FreeCoreClient] Logo downloaded: " + r.body().length + " bytes");
                return r.body();
            } catch (Exception e) { e.printStackTrace(); return null; }
        }).thenAcceptAsync(bytes -> install(bytes, minecraft), minecraft);
    }

    private static HttpClient imageHttpClient() {
        try {
            X509TrustManager trust = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            };
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, new javax.net.ssl.TrustManager[]{trust}, new SecureRandom());
            return HttpClient.newBuilder().sslContext(ssl).connectTimeout(Duration.ofSeconds(8)).build();
        } catch (Exception e) { return HttpClient.newHttpClient(); }
    }
    private static void install(byte[] bytes, Minecraft minecraft) {
        if (bytes == null) return;
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            NativeImage image = NativeImage.read(in);
            Identifier textureId = Identifier.fromNamespaceAndPath("freecoreclient", "logo");
            minecraft.getTextureManager().register(textureId, new DynamicTexture(() -> "freecore_logo", image));
            id = textureId; width = image.getWidth(); height = image.getHeight();
            System.out.println("[FreeCoreClient] Logo installed: " + width + "x" + height);
        } catch (Exception e) { e.printStackTrace(); }
    }
    public static boolean render(GuiGraphicsExtractor graphics, int screenWidth) {
        return render(graphics, screenWidth, 40, 500, null);
    }
    public static boolean render(GuiGraphicsExtractor graphics, int screenWidth, int y, int configuredWidth) {
        return render(graphics, screenWidth, 360, y, configuredWidth, null);
    }
    public static boolean render(GuiGraphicsExtractor graphics, int screenWidth, int y, int configuredWidth, Integer configuredX) {
        return render(graphics, screenWidth, 360, y, configuredWidth, configuredX);
    }
    public static boolean render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight, int y, int configuredWidth, Integer configuredX) {
        return render(graphics, screenWidth, screenHeight, y, configuredWidth, configuredX, true, 1.0f, 18, 42);
    }
    public static boolean render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight, int y,
                                 int configuredWidth, Integer configuredX, boolean animationEnabled,
                                 float animationSpeed, int orbitRadius, int glowStrength) {
        Identifier textureId = id;
        if (textureId == null || width <= 0 || height <= 0) return false;
        int responsiveCap = Math.max(48, Math.round(screenHeight * 0.30f));
        int drawWidth = Math.min(Math.max(1, configuredWidth), Math.max(1, Math.min(screenWidth - 40, responsiveCap)));
        int drawHeight = Math.max(1, Math.round((float) height * drawWidth / width));
        int drawX = configuredX == null ? (screenWidth - drawWidth) / 2 : configuredX - drawWidth / 2;
        drawX = Math.max(12, Math.min(drawX, screenWidth - drawWidth - 12));
        int drawY = Math.max(8, y);
        if (animationEnabled) {
            drawLogoAnimation(graphics, drawX, drawY, drawWidth, drawHeight,
                    animationSpeed, orbitRadius, glowStrength);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, drawX, drawY,
                0f, 0f, drawWidth, drawHeight, width, height, width, height, -1);
        return true;
    }

    /** Website-inspired logo motion: two counter-rotating elliptical rails, a
     * breathing halo and a travelling node. All primitives are thin and low
     * contrast so the logo remains readable and never becomes a grey box. */
    private static void drawLogoAnimation(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                          float speed, int configuredRadius, int configuredGlow) {
        double t = System.nanoTime() / 1_000_000_000.0 * Math.max(0.05, Math.min(4.0, speed));
        double pulse = (Math.sin(t * 1.7) + 1.0) * 0.5;
        int radius = Math.max(8, Math.min(64, configuredRadius));
        int glow = Math.max(0, Math.min(120, configuredGlow));
        int cx = x + w / 2;
        int cy = y + h / 2;
        int rx = Math.max(12, w / 2 + radius);
        int ry = Math.max(10, h / 2 + Math.max(6, radius * 2 / 3));

        int haloAlpha = Math.max(4, Math.min(90, (int) (glow * (0.45 + pulse * 0.55))));
        graphics.fill(cx - 1, y - 4, cx + 1, y + h + 4, (haloAlpha << 24) | 0xffffff);
        graphics.fill(x - 4, cy - 1, x + w + 4, cy + 1, ((haloAlpha / 2) << 24) | 0xffffff);
        drawOrbit(graphics, cx, cy, rx, ry, t, Math.max(32, glow));
        drawOrbit(graphics, cx, cy, Math.max(10, rx - 7), Math.max(8, ry - 5), -t * 0.72, Math.max(20, glow / 2));

        double nodeAngle = t * 0.9;
        int nx = cx + (int) Math.round(Math.cos(nodeAngle) * rx);
        int ny = cy + (int) Math.round(Math.sin(nodeAngle) * ry);
        int nodeAlpha = Math.max(18, Math.min(120, (int) (35 + pulse * 70)));
        graphics.fill(nx - 2, ny - 2, nx + 3, ny + 3, (nodeAlpha << 24) | 0xffffff);
        graphics.fill(nx - 6, ny, nx + 7, ny + 1, ((nodeAlpha / 3) << 24) | 0xffffff);
        graphics.fill(nx, ny - 6, nx + 1, ny + 7, ((nodeAlpha / 3) << 24) | 0xffffff);
    }

    private static void drawOrbit(GuiGraphicsExtractor graphics, int cx, int cy, int rx, int ry,
                                  double phase, int alpha) {
        int segments = 32;
        int lineAlpha = Math.max(8, Math.min(120, alpha));
        for (int i = 0; i < segments; i++) {
            double a0 = phase + (Math.PI * 2.0 * i / segments);
            double a1 = phase + (Math.PI * 2.0 * (i + 1) / segments);
            int x0 = cx + (int) Math.round(Math.cos(a0) * rx);
            int y0 = cy + (int) Math.round(Math.sin(a0) * ry);
            int x1 = cx + (int) Math.round(Math.cos(a1) * rx);
            int y1 = cy + (int) Math.round(Math.sin(a1) * ry);
            // Dotted orbit segments stay crisp at GUI scale. A moving bright
            // sweep makes the rotation obvious in a single screenshot.
            int mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
            int sweep = (int) Math.floor((phase % (Math.PI * 2.0) + Math.PI * 2.0) / (Math.PI * 2.0) * segments);
            int distance = Math.floorMod(i - sweep, segments);
            int a = distance < 4 ? Math.max(lineAlpha, 96 - distance * 14) : lineAlpha;
            int dot = distance < 4 ? 3 : 2;
            graphics.fill(mx, my, mx + dot, my + dot, (a << 24) | 0xffffff);
        }
    }
}
