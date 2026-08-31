package cc.freecore.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/** Asynchronously downloads per-button icon images and installs them as GUI textures. */
public final class ButtonIconManager {
    private static final HttpClient HTTP = imageHttpClient();
    private static final Map<String, Identifier> IDS = new ConcurrentHashMap<>();
    private static final Map<String, int[]> SIZES = new ConcurrentHashMap<>();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private ButtonIconManager() {}

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
        } catch (Exception ignored) {
            return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        }
    }

    public static Identifier get(String source) { return source == null ? null : IDS.get(source); }
    public static int width(String source) { return size(source, 0); }
    public static int height(String source) { return size(source, 1); }
    private static int size(String source, int index) {
        int[] size = source == null ? null : SIZES.get(source);
        return size == null ? 1 : Math.max(1, size[index]);
    }

    public static void loadAsync(String source, Minecraft minecraft) {
        if (source == null || source.isBlank() || source.startsWith("YOUR_") || !IN_FLIGHT.add(source)) return;
        CompletableFuture.supplyAsync(() -> read(source, minecraft))
                .thenAcceptAsync(bytes -> install(source, bytes, minecraft), minecraft)
                .exceptionally(error -> { System.err.println("[FreeCoreClient] Button icon failed: " + source + " -> " + error); return null; });
    }

    private static byte[] read(String source, Minecraft minecraft) {
        try {
            if (source.startsWith("http://") || source.startsWith("https://")) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(source))
                        .header("Cache-Control", "no-cache").timeout(Duration.ofSeconds(15)).GET().build();
                HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + response.statusCode());
                System.out.println("[FreeCoreClient] Button icon downloaded: " + source + " (" + response.body().length + " bytes)");
                return response.body();
            }
            Path path = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source);
            if (!path.isAbsolute()) path = minecraft.gameDirectory.toPath().resolve(path);
            return Files.readAllBytes(path.normalize());
        } catch (Exception error) {
            throw new IllegalStateException("Unable to load button icon", error);
        }
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
