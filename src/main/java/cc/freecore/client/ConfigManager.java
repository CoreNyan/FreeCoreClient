package cc.freecore.client;

import com.google.gson.JsonParseException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Asynchronous local/remote JSON loader and client-thread applier. */
public final class ConfigManager {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static volatile FreeCoreConfig current = FreeCoreConfig.defaults();
    private static MinecraftClient minecraft;

    private ConfigManager() {}

    public static void initialize(MinecraftClient client) {
        minecraft = client;
        // All disk/network work starts off-thread; startup/render thread is never blocked.
        CompletableFuture.supplyAsync(() -> readBootstrapConfig(client))
                .thenCompose(bootstrap -> CompletableFuture.supplyAsync(() -> readLocalConfig(client))
                        .thenCompose(local -> fetchRemoteConfig(bootstrap.remoteConfigUrl).exceptionally(error -> null)
                                .thenApply(remote -> remote != null ? remote : local)))
                .thenAcceptAsync(ConfigManager::apply, client)
                .exceptionally(error -> { error.printStackTrace(); return null; });
    }

    public static FreeCoreConfig get() { return current; }

    private static BootstrapConfig readBootstrapConfig(MinecraftClient client) {
        Path game = client.runDirectory.toPath();
        Path[] candidates = { game.resolve("config/freecore_bootstrap.json"), game.resolve("freecore_bootstrap.json") };
        for (Path path : candidates) {
            try {
                if (Files.isRegularFile(path)) {
                    BootstrapConfig parsed = BootstrapConfig.GSON.fromJson(Files.readString(path), BootstrapConfig.class);
                    return parsed != null ? parsed : new BootstrapConfig();
                }
            } catch (IOException | JsonParseException ex) {
                ex.printStackTrace();
            }
        }
        return new BootstrapConfig();
    }

    private static FreeCoreConfig readLocalConfig(Minecraft client) {
        Path game = client.runDirectory.toPath();
        Path[] candidates = { game.resolve("config/freecore_config.json"), game.resolve("freecore_config.json"), game.resolve("mods/freecore_config.json") };
        for (Path path : candidates) {
            try {
                if (Files.isRegularFile(path)) {
                    FreeCoreConfig parsed = FreeCoreConfig.GSON.fromJson(Files.readString(path), FreeCoreConfig.class);
                    return parsed != null ? parsed : FreeCoreConfig.defaults();
                }
            } catch (IOException | JsonParseException ex) {
                ex.printStackTrace();
            }
        }
        return FreeCoreConfig.defaults();
    }

    private static CompletableFuture<FreeCoreConfig> fetchRemoteConfig(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isBlank() || remoteUrl.contains("YOUR_")) {
            return CompletableFuture.completedFuture(null);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(remoteUrl))
                .timeout(Duration.ofSeconds(12)).header("Accept", "application/json")
                .GET().build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + response.statusCode());
                    FreeCoreConfig parsed = FreeCoreConfig.GSON.fromJson(response.body(), FreeCoreConfig.class);
                    return parsed != null ? parsed : FreeCoreConfig.defaults();
                });
    }

    private static void apply(FreeCoreConfig config) {
        if (config == null) config = FreeCoreConfig.defaults();
        if (config.buttons == null) config.buttons = FreeCoreConfig.defaults().buttons;
        current = config;
        if (minecraft != null && minecraft.getWindow() != null && config.windowTitle != null && !config.windowTitle.isBlank()) {
            minecraft.getWindow().setTitle(config.windowTitle);
        }
        IconLoader.loadAsync(config.iconUrl, minecraft);
        BackgroundManager.loadAsync(config.backgroundUrl, minecraft);
    }

    public static void toast(String title, String message) {
        if (minecraft != null) minecraft.getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION,
                Text.literal(title), Text.literal(message)));
    }
}
