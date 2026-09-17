package cc.freecore.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Persistent, conditionally revalidated cache for URL-backed UI icons. */
public final class RemoteIconCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HttpClient HTTP = imageHttpClient();
    private static final Map<String, CompletableFuture<byte[]>> REQUESTS = new ConcurrentHashMap<>();

    private RemoteIconCache() {}

    public static CompletableFuture<byte[]> loadAsync(String source, Minecraft minecraft) {
        return loadAsync(source, minecraft.gameDirectory.toPath());
    }

    static CompletableFuture<byte[]> loadAsync(String source, Path gameDirectory) {
        if (source == null || source.isBlank() || source.startsWith("YOUR_")) {
            return CompletableFuture.completedFuture(null);
        }
        if (!isRemote(source)) {
            return CompletableFuture.supplyAsync(() -> readLocal(source, gameDirectory));
        }
        return REQUESTS.computeIfAbsent(source, key -> CompletableFuture
                .supplyAsync(() -> fetchRemote(key, gameDirectory))
                .whenComplete((ignored, error) -> REQUESTS.remove(key)));
    }

    private static byte[] fetchRemote(String source, Path gameDirectory) {
        Path directory = gameDirectory.resolve("config/freecoreclient/icon-cache");
        String key = sha256(source.getBytes(StandardCharsets.UTF_8));
        Path dataPath = directory.resolve(key + ".img");
        Path metadataPath = directory.resolve(key + ".json");
        try {
            Files.createDirectories(directory);
            byte[] cached = Files.isRegularFile(dataPath) ? Files.readAllBytes(dataPath) : null;
            Metadata metadata = readMetadata(metadataPath);
            boolean cacheValid = cached != null && metadata != null && source.equals(metadata.url)
                    && sha256(cached).equalsIgnoreCase(empty(metadata.sha256));

            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(source))
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .timeout(Duration.ofSeconds(15)).GET();
            if (cacheValid && !empty(metadata.etag).isBlank()) {
                request.header("If-None-Match", metadata.etag);
            }
            if (cacheValid && !empty(metadata.lastModified).isBlank()) {
                request.header("If-Modified-Since", metadata.lastModified);
            }

            HttpResponse<byte[]> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 304 && cacheValid) {
                metadata.checkedAt = System.currentTimeMillis();
                writeMetadata(metadataPath, metadata);
                System.out.println("[FreeCoreClient] Icon cache current: " + source);
                return cached;
            }
            if (response.statusCode() / 100 != 2 || response.body().length == 0) {
                throw new IOException("HTTP " + response.statusCode());
            }

            byte[] downloaded = response.body();
            String downloadedHash = sha256(downloaded);
            if (cacheValid && downloadedHash.equalsIgnoreCase(metadata.sha256)) {
                updateMetadata(metadata, response, downloadedHash);
                writeMetadata(metadataPath, metadata);
                System.out.println("[FreeCoreClient] Icon cache unchanged: " + source);
                return cached;
            }

            writeAtomically(dataPath, downloaded);
            Metadata updated = new Metadata();
            updated.url = source;
            updateMetadata(updated, response, downloadedHash);
            writeMetadata(metadataPath, updated);
            System.out.println("[FreeCoreClient] Icon cache updated: " + source
                    + " (" + downloaded.length + " bytes)");
            return downloaded;
        } catch (Exception error) {
            try {
                if (Files.isRegularFile(dataPath)) {
                    System.err.println("[FreeCoreClient] Icon validation failed; using cache: "
                            + source + " -> " + error.getMessage());
                    return Files.readAllBytes(dataPath);
                }
            } catch (IOException cacheError) {
                error.addSuppressed(cacheError);
            }
            throw new IllegalStateException("Unable to load icon " + source, error);
        }
    }

    private static byte[] readLocal(String source, Path gameDirectory) {
        try {
            Path path = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source);
            if (!path.isAbsolute()) path = gameDirectory.resolve(path);
            return Files.readAllBytes(path.normalize());
        } catch (Exception error) {
            throw new IllegalStateException("Unable to load local icon " + source, error);
        }
    }

    private static void updateMetadata(Metadata metadata, HttpResponse<?> response, String hash) {
        metadata.etag = response.headers().firstValue("ETag").orElse("");
        metadata.lastModified = response.headers().firstValue("Last-Modified").orElse("");
        metadata.sha256 = hash;
        metadata.checkedAt = System.currentTimeMillis();
    }

    private static Metadata readMetadata(Path path) {
        if (!Files.isRegularFile(path)) return null;
        try {
            return GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Metadata.class);
        } catch (Exception error) {
            return null;
        }
    }

    private static void writeMetadata(Path path, Metadata metadata) throws IOException {
        writeAtomically(path, GSON.toJson(metadata).getBytes(StandardCharsets.UTF_8));
    }

    private static void writeAtomically(Path target, byte[] bytes) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temporary, bytes);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static boolean isRemote(String source) {
        return source.startsWith("http://") || source.startsWith("https://");
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }

    private static HttpClient imageHttpClient() {
        try {
            X509TrustManager trust = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            };
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, new javax.net.ssl.TrustManager[]{trust}, new SecureRandom());
            return HttpClient.newBuilder().sslContext(ssl).version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(8)).build();
        } catch (Exception error) {
            return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(8)).build();
        }
    }

    private static final class Metadata {
        String url;
        String etag;
        String lastModified;
        String sha256;
        long checkedAt;
    }
}
