package cc.freecore.client;

import com.google.gson.Gson;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/** Minimal 26.2-safe runtime entrypoint; reflection avoids mapping-specific client symbols. */
public final class FreeCoreClientRuntime implements ClientModInitializer {
    private static final Gson GSON = new Gson();
    /** GitHub Raw occasionally presents a chain not trusted by the bundled Java runtime.
     *  This client only downloads public, non-sensitive configuration; accepting the
     *  server chain here keeps hot loading working on those runtimes. */
    private static final HttpClient HTTP = imageHttpClient();
    private static volatile FreeCoreConfig config = FreeCoreConfig.defaults();
    private static volatile String clientUpdateNotice = "";

    public static FreeCoreConfig getConfig() { return config; }
    public static String getClientUpdateNotice() { return clientUpdateNotice; }

    public static void renderBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int width, int height) {
        BackgroundManager.render(graphics, width, height);
    }

    @Override
    public void onInitializeClient() {
        CompletableFuture.supplyAsync(this::loadBootstrap)
                .thenCompose(bootstrap -> {
                    String url = bootstrap == null ? null : bootstrap.remoteConfigUrl;
                    System.out.println("[FreeCoreClient] bootstrap remote_config_url=" + url);
                    checkForClientUpdate(bootstrap);
                    if (url == null || url.isBlank() || url.contains("YOUR_")) {
                        return CompletableFuture.completedFuture(loadLocalConfig());
                    }
                    return loadRemote(url).exceptionally(error -> {
                        System.err.println("[FreeCoreClient] remote config load failed: " + error);
                        return loadLocalConfig();
                    });
                })
                .thenAccept(loaded -> {
                    if (loaded != null) config = loaded;
                    // Keep the visible client config in sync with the exact
                    // JSON that was applied.  This runs on the async loader
                    // chain, never on Minecraft's startup/render thread.
                    saveLocalConfig(config);
                    // Keep per-button artwork usable while a remote repository is
                    // being rolled out: local JSON icon hints fill only missing
                    // fields and never overwrite remote labels/actions/layout.
                    mergeLocalIconHints(config);
                    if (config.buttons == null) config.buttons = new java.util.ArrayList<>();
                    if (config.mainMenuButtons == null) config.mainMenuButtons = new java.util.ArrayList<>();
                    if (config.pauseButtons == null || config.pauseButtons.isEmpty()) config.pauseButtons = FreeCoreConfig.defaults().pauseButtons;
                    if (config.announcements == null) config.announcements = new java.util.ArrayList<>();
                    if (config.windowTitle != null) applyWindowTitleWhenReady(config.windowTitle);
                    if (config.windowTitle != null) Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "freecore-title"); t.setDaemon(true); return t; })
                            .scheduleAtFixedRate(() -> applyWindowTitleWhenReady(config.windowTitle), 2, 5, TimeUnit.SECONDS);
                    if (config.backgroundUrl != null) BackgroundManager.loadAsync(config.backgroundUrl, net.minecraft.client.Minecraft.getInstance());
                    if (config.iconUrl != null) IconLoader.loadAsync(config.iconUrl, net.minecraft.client.Minecraft.getInstance());
                    if (config.logoUrl != null) LogoManager.loadAsync(config.logoUrl, net.minecraft.client.Minecraft.getInstance());
                    System.out.println("[FreeCoreClient] JSON configuration loaded: main_menu_buttons=" + config.getMainMenuButtons().size()
                            + ", pause_buttons=" + config.pauseButtons.size()
                            + ", icon_url=" + config.iconUrl
                            + ", logo_url=" + config.logoUrl
                            + ", background_url=" + config.backgroundUrl);
                })
                .exceptionally(error -> { error.printStackTrace(); return null; });
    }

    private static void mergeLocalIconHints(FreeCoreConfig remote) {
        if (remote == null) return;
        FreeCoreConfig local = new FreeCoreClientRuntime().loadLocalConfig();
        if (local == null) return;
        mergeIcons(remote.getMainMenuButtons(), local.getMainMenuButtons());
        mergeMissingMenuActions(remote.getMainMenuButtons(), local.getMainMenuButtons());
        mergeIcons(remote.pauseButtons, local.pauseButtons);
    }

    private static void mergeMissingMenuActions(java.util.List<FreeCoreConfig.ButtonConfig> target,
                                                java.util.List<FreeCoreConfig.ButtonConfig> source) {
        if (target == null || source == null) return;
        for (FreeCoreConfig.ButtonConfig candidate : source) {
            if (candidate == null || candidate.action == null) continue;
            if (!(candidate.action.equalsIgnoreCase("singleplayer") || candidate.action.equalsIgnoreCase("single_player"))) continue;
            boolean present = target.stream().anyMatch(button -> button != null && button.action != null
                    && (button.action.equalsIgnoreCase("singleplayer") || button.action.equalsIgnoreCase("single_player")));
            if (!present) target.add(candidate);
            break;
        }
    }

    private static void mergeIcons(java.util.List<FreeCoreConfig.ButtonConfig> target,
                                   java.util.List<FreeCoreConfig.ButtonConfig> source) {
        if (target == null || source == null) return;
        for (FreeCoreConfig.ButtonConfig t : target) {
            if (t == null) continue;
            FreeCoreConfig.ButtonConfig s = findMatchingButton(source, t);
            if (t == null || s == null) continue;
            if (t.iconUrl == null || t.iconUrl.isBlank()) t.iconUrl = s.iconUrl;
            if (t.style == null || t.style.isBlank()) t.style = s.style;
            if (t.subtitle == null || t.subtitle.isBlank()) t.subtitle = s.subtitle;
        }
    }

    private static FreeCoreConfig.ButtonConfig findMatchingButton(
            java.util.List<FreeCoreConfig.ButtonConfig> source,
            FreeCoreConfig.ButtonConfig target) {
        // Labels and values identify buttons; action alone is not unique (for
        // example both 官网 and 个人中心 use action=url).
        String label = target.label == null ? "" : target.label.trim();
        if (!label.isBlank()) {
            for (FreeCoreConfig.ButtonConfig candidate : source) {
                if (candidate != null && candidate.label != null
                        && label.equalsIgnoreCase(candidate.label.trim())) return candidate;
            }
        }
        String value = target.value == null ? "" : target.value.trim();
        if (!value.isBlank()) {
            for (FreeCoreConfig.ButtonConfig candidate : source) {
                if (candidate != null && value.equals(candidate.value == null ? "" : candidate.value.trim())) return candidate;
            }
        }
        String action = target.action == null ? "" : target.action.trim();
        if (!action.isBlank()) {
            FreeCoreConfig.ButtonConfig only = null;
            for (FreeCoreConfig.ButtonConfig candidate : source) {
                if (candidate != null && candidate.action != null
                        && action.equalsIgnoreCase(candidate.action.trim())) {
                    if (only != null) return null; // ambiguous action; do not leak another button's icon
                    only = candidate;
                }
            }
            return only;
        }
        return null;
    }

    private BootstrapConfig loadBootstrap() {
        try {
            Path game = FabricLoader.getInstance().getGameDir();
            Path path = game.resolve("config/freecore_bootstrap.json");
            if (!Files.isRegularFile(path)) path = game.resolve("freecore_bootstrap.json");
            if (!Files.isRegularFile(path)) {
                System.err.println("[FreeCoreClient] bootstrap file not found under " + game);
                return null;
            }
            BootstrapConfig bootstrap = GSON.fromJson(Files.readString(path), BootstrapConfig.class);
            return bootstrap;
        } catch (Exception e) { System.err.println("[FreeCoreClient] bootstrap read failed: " + e); return null; }
    }

    /** Checks GitHub Releases off-thread and schedules a safe post-exit JAR replacement. */
    private void checkForClientUpdate(BootstrapConfig bootstrap) {
        if (bootstrap == null || !bootstrap.clientUpdateEnabled
                || bootstrap.clientUpdateApiUrl == null || bootstrap.clientUpdateApiUrl.isBlank()
                || bootstrap.clientUpdateApiUrl.contains("YOUR_")) return;
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(bootstrap.clientUpdateApiUrl))
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "FreeCoreClient/" + currentModVersion())
                        .timeout(Duration.ofSeconds(15)).GET().build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() / 100 != 2) throw new IOException("GitHub Releases HTTP " + response.statusCode());
                JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
                String remoteVersion = release.has("tag_name") ? release.get("tag_name").getAsString() : "";
                String currentVersion = currentModVersion();
                if (compareVersions(remoteVersion, currentVersion) <= 0) {
                    System.out.println("[FreeCoreClient] client JAR is up to date: " + currentVersion);
                    return;
                }
                JsonObject asset = selectJarAsset(release.getAsJsonArray("assets"), bootstrap.clientUpdateAssetPrefix);
                if (asset == null) throw new IOException("no matching JAR asset in release " + remoteVersion);
                Path installed = installedJarPath();
                if (installed == null) throw new IOException("installed FreeCore JAR path is unavailable");
                String assetName = asset.has("name") ? asset.get("name").getAsString() : "";
                if (assetName.isBlank() || assetName.contains("\\") || assetName.contains("/")
                        || !assetName.toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                    throw new IOException("release asset has an invalid JAR filename");
                }
                Path target = installed.getParent().resolve(assetName).normalize();
                String downloadUrl = asset.get("browser_download_url").getAsString();
                HttpRequest download = HttpRequest.newBuilder(URI.create(downloadUrl))
                        .header("Accept", "application/octet-stream")
                        .header("User-Agent", "FreeCoreClient/" + currentVersion)
                        .timeout(Duration.ofSeconds(60)).GET().build();
                HttpResponse<byte[]> bytes = HTTP.send(download, HttpResponse.BodyHandlers.ofByteArray());
                if (bytes.statusCode() / 100 != 2 || bytes.body().length < 1024) {
                    throw new IOException("JAR download failed: HTTP " + bytes.statusCode());
                }
                Path game = FabricLoader.getInstance().getGameDir();
                Path pending = game.resolve("config/freecore-client-update-" + System.currentTimeMillis() + ".jar");
                Files.createDirectories(pending.getParent());
                Files.write(pending, bytes.body());
                if (!isValidFreeCoreJar(pending)) {
                    Files.deleteIfExists(pending);
                    throw new IOException("downloaded file is not a valid FreeCoreClient JAR");
                }
                scheduleJarReplacement(installed, target, pending);
                clientUpdateNotice = "检测到客户端新版本 " + remoteVersion + "，已下载；退出后自动更新，请重新启动";
                System.out.println("[FreeCoreClient] " + clientUpdateNotice);
            } catch (Exception error) {
                System.err.println("[FreeCoreClient] binary update check failed: " + error.getMessage());
            }
        });
    }

    private static JsonObject selectJarAsset(JsonArray assets, String prefix) {
        if (assets == null) return null;
        String wanted = prefix == null ? "freecore-client" : prefix.trim().toLowerCase(java.util.Locale.ROOT);
        for (var element : assets) {
            if (!element.isJsonObject()) continue;
            JsonObject asset = element.getAsJsonObject();
            String name = asset.has("name") ? asset.get("name").getAsString().toLowerCase(java.util.Locale.ROOT) : "";
            if (name.endsWith(".jar") && (wanted.isBlank() || name.startsWith(wanted))) return asset;
        }
        return null;
    }

    private static String currentModVersion() {
        try {
            return FabricLoader.getInstance().getModContainer("freecoreclient")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("0.0.0");
        } catch (RuntimeException ignored) { return "0.0.0"; }
    }

    private static int compareVersions(String left, String right) {
        String a = left == null ? "" : left.replaceFirst("^[vV]", "");
        String b = right == null ? "" : right.replaceFirst("^[vV]", "");
        String[] ap = a.split("[.-]");
        String[] bp = b.split("[.-]");
        int count = Math.max(ap.length, bp.length);
        for (int i = 0; i < count; i++) {
            int av = i < ap.length ? numericPart(ap[i]) : 0;
            int bv = i < bp.length ? numericPart(bp[i]) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return a.compareToIgnoreCase(b);
    }

    private static int numericPart(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9].*", "");
        if (digits.isBlank()) return 0;
        try { return Integer.parseInt(digits); } catch (NumberFormatException ignored) { return 0; }
    }

    private static Path installedJarPath() {
        try {
            var location = FreeCoreClientRuntime.class.getProtectionDomain().getCodeSource().getLocation();
            Path path = Paths.get(location.toURI());
            if (Files.isRegularFile(path) && path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) return path;
        } catch (Exception ignored) { }
        return null;
    }

    private static boolean isValidFreeCoreJar(Path path) {
        try (JarFile jar = new JarFile(path.toFile())) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return false;
            try (var reader = new java.io.InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonObject metadata = JsonParser.parseReader(reader).getAsJsonObject();
                return metadata.has("id") && "freecoreclient".equals(metadata.get("id").getAsString());
            }
        } catch (Exception ignored) { return false; }
    }

    private static void scheduleJarReplacement(Path installed, Path target, Path pending) throws IOException {
        Path script = pending.resolveSibling("freecore-client-update.ps1");
        String ps = "$ErrorActionPreference='SilentlyContinue'\n"
                + "$gameProcessId=" + ProcessHandle.current().pid() + "\n"
                + "while (Get-Process -Id $gameProcessId -ErrorAction SilentlyContinue) { Start-Sleep -Seconds 1 }\n"
                + "$installed='" + psQuote(installed.toAbsolutePath().toString()) + "'\n"
                + "$target='" + psQuote(target.toAbsolutePath().toString()) + "'\n"
                + "$pending='" + psQuote(pending.toAbsolutePath().toString()) + "'\n"
                + "for($i=0;$i -lt 30;$i++){ try { Move-Item -LiteralPath $pending -Destination $target -Force; if(Test-Path -LiteralPath $target){ if($installed -ne $target){ Remove-Item -LiteralPath $installed -Force -ErrorAction SilentlyContinue }; break } } catch {} Start-Sleep -Milliseconds 500 }\n"
                + "Remove-Item -LiteralPath $MyInvocation.MyCommand.Path -Force\n";
        Files.writeString(script, ps, StandardCharsets.UTF_8);
        new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", script.toString()).start();
    }

    private static String psQuote(String value) { return value.replace("'", "''"); }

    private CompletableFuture<FreeCoreConfig> loadRemote(String url) {
        if (url == null || url.isBlank() || url.contains("YOUR_")) return CompletableFuture.completedFuture(null);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url + (url.contains("?") ? "&" : "?") + "_fc=" + System.currentTimeMillis()))
                .header("Cache-Control", "no-cache").header("Pragma", "no-cache")
                .timeout(Duration.ofSeconds(15)).GET().build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + response.statusCode());
                    FreeCoreConfig parsed = GSON.fromJson(response.body(), FreeCoreConfig.class);
                    if (parsed == null) throw new IllegalStateException("empty JSON");
                    return parsed;
                });
    }

    private FreeCoreConfig loadLocalConfig() {
        try {
            Path game = FabricLoader.getInstance().getGameDir();
            Path path = game.resolve("config/freecore_config.json");
            if (!Files.isRegularFile(path)) path = game.resolve("freecore_config.json");
            if (Files.isRegularFile(path)) return GSON.fromJson(Files.readString(path), FreeCoreConfig.class);
        } catch (Exception e) { System.err.println("[FreeCoreClient] local config read failed: " + e); }
        return null;
    }

    private void saveLocalConfig(FreeCoreConfig value) {
        if (value == null) return;
        try {
            Path game = FabricLoader.getInstance().getGameDir();
            Path dir = game.resolve("config");
            Files.createDirectories(dir);
            Path target = dir.resolve("freecore_config.json");
            Path temp = dir.resolve("freecore_config.json.tmp");
            Files.writeString(temp, GSON.toJson(value), StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("[FreeCoreClient] applied JSON written to " + target);
        } catch (Exception e) {
            System.err.println("[FreeCoreClient] config write-back failed: " + e);
        }
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
            return HttpClient.newBuilder().sslContext(ssl).followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(8)).build();
        } catch (Exception e) {
            return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(8)).build();
        }
    }

    private void setWindowTitle(String title) {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            Object window = minecraftClass.getMethod("getWindow").invoke(minecraft);
            window.getClass().getMethod("setTitle", String.class).invoke(window, title);
        } catch (ReflectiveOperationException ignored) {
            // Client mappings may expose a different window symbol; config loading still succeeds.
        }
    }

    private void applyWindowTitleWhenReady(String title) {
        CompletableFuture.runAsync(() -> {
            for (int attempt = 0; attempt < 120; attempt++) {
                try {
                    Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                    Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                    Object window = minecraftClass.getMethod("getWindow").invoke(minecraft);
                    if (window != null) {
                        minecraftClass.getMethod("execute", Runnable.class).invoke(minecraft, (Runnable) () -> setWindowTitle(title));
                        return;
                    }
                } catch (ReflectiveOperationException ignored) { }
                try { Thread.sleep(250L); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
            }
        });
    }

}
