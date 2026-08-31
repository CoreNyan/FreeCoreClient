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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class BackgroundManager {
    private static final HttpClient HTTP = imageHttpClient();
    private static volatile Identifier id;
    private static volatile int width, height;
    private BackgroundManager() {}
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
    public static void loadAsync(String source, Minecraft minecraft) {
        if (source == null || source.isBlank() || source.startsWith("YOUR_")) return;
        System.out.println("[FreeCoreClient] Loading static background: " + source);
        CompletableFuture.supplyAsync(() -> read(source, minecraft)).thenAcceptAsync(bytes -> install(bytes, minecraft), minecraft)
                .exceptionally(error -> { error.printStackTrace(); return null; });
    }
    private static byte[] read(String source, Minecraft minecraft) {
        try {
            if (source.startsWith("http://") || source.startsWith("https://")) {
                HttpRequest req = HttpRequest.newBuilder(URI.create(source + (source.contains("?") ? "&" : "?") + "_fc=" + System.currentTimeMillis()))
                        .header("Cache-Control", "no-cache").header("Pragma", "no-cache")
                        .timeout(Duration.ofSeconds(15)).GET().build();
                HttpResponse<byte[]> r = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (r.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + r.statusCode());
                System.out.println("[FreeCoreClient] Background downloaded: " + r.body().length + " bytes");
                return r.body();
            }
            Path p = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source);
            if (!p.isAbsolute()) p = minecraft.gameDirectory.toPath().resolve(p);
            return Files.readAllBytes(p.normalize());
        } catch (Exception e) { throw new IllegalStateException("Unable to load background", e); }
    }
    private static void install(byte[] bytes, Minecraft minecraft) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            NativeImage image = NativeImage.read(in);
            Identifier textureId = Identifier.fromNamespaceAndPath("freecoreclient", "background");
            minecraft.getTextureManager().register(textureId, new DynamicTexture(() -> "freecore_background", image));
            id = textureId; width = image.getWidth(); height = image.getHeight();
            System.out.println("[FreeCoreClient] Background installed: " + width + "x" + height);
        } catch (Exception e) { e.printStackTrace(); }
    }
    public static boolean render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        Identifier textureId = id;
        if (textureId == null || width <= 0 || height <= 0) {
            // Website-inspired monochrome fallback, available while a remote
            // image is downloading.
            graphics.fillGradient(0, 0, screenWidth, screenHeight, 0xff202020, 0xff080808);
            int step = Math.max(24, screenWidth / 32);
            int drift = (int) ((System.nanoTime() / 35_000_000L) % step);
            for (int x = -step + drift; x < screenWidth; x += step) {
                graphics.fill(x, 0, x + 1, screenHeight, 0x18ffffff);
            }
            for (int y = -step + drift; y < screenHeight; y += step) {
                graphics.fill(0, y, screenWidth, y + 1, 0x18ffffff);
            }
            drawMotion(graphics, screenWidth, screenHeight);
            return true;
        }
        float scale = Math.max((float) screenWidth / width, (float) screenHeight / height);
        int w = Math.round(width * scale), h = Math.round(height * scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, (screenWidth - w) / 2, (screenHeight - h) / 2,
                0f, 0f, w, h, width, height, width, height, -1);
        drawMotion(graphics, screenWidth, screenHeight);
        return true;
    }

    /** Several low-contrast layers give a remote or procedural background a
     * deliberate sense of motion without turning it into a distracting loop. */
    private static void drawMotion(GuiGraphicsExtractor graphics, int w, int h) {
        long now = System.nanoTime();
        double time = now / 1_000_000_000.0;
        // Ease the diagonal drift instead of moving it at a constant speed.
        double eased = (Math.sin(time * 0.42) + 1.0) * 0.5;
        int diagonal = (int) (eased * (w + h + 160)) - h - 80;
        for (int offset = -h; offset < w + h; offset += Math.max(180, w / 6)) {
            int x = diagonal + offset;
            graphics.fill(x, 0, x + 1, h, 0x12ffffff);
        }

        double breathing = (Math.sin(time * 0.9) + 1.0) * 0.5;
        int beamWidth = Math.max(70, (int) (w * (0.10 + breathing * 0.08)));
        double beamPhase = (Math.sin(time * 0.55 + 1.2) + 1.0) * 0.5;
        int beamX = (int) (beamPhase * (w + beamWidth * 2L)) - beamWidth;
        int beamAlpha = 0x12 + (int) (breathing * 0x20);
        graphics.fillGradient(beamX, 0, beamX + beamWidth, h, 0x00000000, (beamAlpha << 24) | 0xffffff);

        // Three nodes travel on independent elliptical orbits; their pulsing
        // halos provide the same layered motion as the website's live backdrop.
        drawNode(graphics, (int) (w * .50 + Math.cos(time * .55) * w * .28),
                (int) (h * .46 + Math.sin(time * .55) * h * .20), breathing);
        drawNode(graphics, (int) (w * .22 + Math.cos(time * .83 + 2) * w * .12),
                (int) (h * .30 + Math.sin(time * .83 + 2) * h * .15), 1.0 - breathing);
        drawNode(graphics, (int) (w * .79 + Math.cos(time * .67 + 4) * w * .13),
                (int) (h * .72 + Math.sin(time * .67 + 4) * h * .12), .65 + breathing * .35);

        // Non-linear horizon shimmer, independent from the diagonal beam.
        int horizonY = (int) (h * (.52 + Math.sin(time * .7) * .07));
        int horizonAlpha = 0x18 + (int) (breathing * 0x30);
        graphics.fillGradient(0, horizonY, w, horizonY + 1, 0x00000000, (horizonAlpha << 24) | 0xffffff);

        // Small corner brackets breathe in size, avoiding a static grey frame.
        int radius = Math.max(28, (int) (Math.min(w, h) * (.16 + breathing * .035)));
        drawBracket(graphics, w / 2 - radius, h / 2 - radius, radius, 0x28ffffff);
        drawBracket(graphics, w / 2 + radius, h / 2 + radius, radius / 2, 0x1cffffff);
    }

    private static void drawNode(GuiGraphicsExtractor graphics, int x, int y, double strength) {
        int alpha = Math.max(10, Math.min(90, (int) (18 + strength * 54)));
        graphics.fill(x - 2, y - 2, x + 3, y + 3, (alpha << 24) | 0xffffff);
        graphics.fill(x - 6, y, x + 7, y + 1, ((alpha / 3) << 24) | 0xffffff);
        graphics.fill(x, y - 6, x + 1, y + 7, ((alpha / 3) << 24) | 0xffffff);
    }

    private static void drawBracket(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        int arm = Math.max(8, r / 3);
        graphics.fill(cx - r, cy - r, cx - r + arm, cy - r + 1, color);
        graphics.fill(cx - r, cy - r, cx - r + 1, cy - r + arm, color);
        graphics.fill(cx + r - arm, cy + r - 1, cx + r, cy + r, color);
        graphics.fill(cx + r - 1, cy + r - arm, cx + r, cy + r, color);
    }
}
