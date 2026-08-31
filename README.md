# FreeCoreClient

Fabric 1.21 client mod skeleton using Gson JSON configuration, asynchronous HTTP updates, TitleScreen/PauseScreen mixins, ConfirmLinkScreen URL confirmation, GLFW window title/icon updates, and a crop-to-fill custom TitleScreen background.

Set `remote_config_url` in the local `config/freecore_bootstrap.json` to your GitHub Raw endpoint. This bootstrap file is read before the client configuration, so changing it takes effect on the next restart. The remote `freecore_config.json` contains only client UI settings. A local `freecore_config.json` in `config/`, the game directory, or `mods/` is used as an offline fallback.

Set `background_url` to an `https://`/`http://` image URL, a `file://` URI, or a local path relative to the game directory. `YOUR_BACKGROUND_URL_OR_LOCAL_PATH_HERE` disables the custom background and keeps vanilla panorama rendering.

Binary updates are configured separately in `freecore_bootstrap.json`. `client_update_api_url` should point to a GitHub Releases API endpoint (for example, `/releases/latest`). The client checks this endpoint asynchronously, downloads a matching `.jar` asset when its Fabric mod version is newer, waits for the current process to exit so Windows file locks are released, and then replaces the installed mod JAR. The title screen displays a restart notice after a successful download. Set `client_update_enabled` to `false` to disable this behavior and use `client_update_asset_prefix` to select a specific asset prefix.

Menu element positions are configurable in `freecore_config.json`:

- `logo_y`, `logo_width`: custom title logo top coordinate and width.
- `announcement_x`, `announcement_y`, `announcement_width`, `announcement_height`: announcement panel rectangle.
- Each button accepts `x`, `y`, `width`, `height` in pixels, or `x_percent`, `y_percent` for responsive placement. `x_percent` is the horizontal center (0.0–1.0); `y_percent` is the top coordinate (0.0–1.0).
