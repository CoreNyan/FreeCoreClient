# FreeCore Client

FreeCore Client 是一个基于 Fabric 的 Minecraft 客户端 Mod，面向 Minecraft 1.21/26.2。它通过 Mixin 重做主菜单和暂停菜单，并使用 Gson JSON 配置驱动标题、背景、图标、按钮和公告。

## 功能

- 主菜单和暂停菜单自定义布局、按钮、图标与公告栏。
- 公告支持日期、历史记录、轮播或手动翻页、滚轮滚动和滚动条。
- 背景、LOGO、窗口标题和应用图标支持 URL 或本地路径。
- 所有网络请求在后台线程执行，不阻塞 Minecraft 启动线程。
- URL 按钮使用 Minecraft 原生确认页面后打开浏览器。
- 设置、资源包、加载页面等菜单可复用 FreeCore 的视觉背景。
- 使用 Minecraft 原版字体，不依赖自定义字体文件。

## 配置文件

启动引导配置位于游戏目录的 `config/freecore_bootstrap.json`。它只负责指定远程 JSON 地址和客户端 JAR 更新地址：

```json
{
  "remote_config_url": "https://raw.githubusercontent.com/CoreNyan/freecore-client-config-json/main/freecore_config.json",
  "client_update_enabled": true,
  "client_update_api_url": "https://api.github.com/repos/CoreNyan/FreeCoreClient/releases/latest",
  "client_update_asset_prefix": "freecore-client"
}
```

界面配置位于 `config/freecore_config.json`，也可以放在游戏目录根目录作为离线备用配置。客户端启动时会异步读取远程 JSON；远程读取成功后，会把实际应用的配置写回 `config/freecore_config.json`。

## JAR 自动更新

当 `client_update_enabled` 为 `true` 时，客户端会在后台访问 GitHub Releases API，并比较当前 Fabric Mod 版本与 Release 的 `tag_name`。

如果发现新版本，客户端会：

1. 选择名称匹配 `client_update_asset_prefix` 的 `.jar` 资产。
2. 下载到 `config/` 下的临时文件。
3. 检查 JAR 内是否存在合法的 `fabric.mod.json` 且 Mod ID 为 `freecoreclient`。
4. 生成后台替换任务。
5. 等 Minecraft 进程退出后再替换 `mods/freecore-client-*.jar`。
6. 下一次启动自动使用新 JAR。

游戏运行期间不会强行覆盖正在加载的 JAR。检测到更新后，主菜单会显示“已下载，请重新启动”的提示。没有 Release、网络不可用或资产不匹配时，会继续使用当前版本，不影响启动。

## 菜单布局

主菜单按钮使用 `main_menu_buttons`，暂停菜单按钮使用 `pause_buttons`。按钮支持：

- `x`、`y`、`width`、`height`：固定 GUI 像素。
- `x_percent`、`y_percent`、`width_percent`、`height_percent`：按窗口比例适配。
- `style`、`icon_url`、`background`、`border`、`corner_cut`、动画参数等外观字段。

公告栏支持以下主要字段：

- `announcement_mode`：`carousel`、`manual` 或 `static`。
- `announcement_show_date`：是否显示日期。
- `announcement_navigation_gap`：正文与翻页控件之间的间距。
- `announcement_scrollbar_enabled`、`announcement_scrollbar_width`：滚动条开关和宽度。
- `announcement_x`、`announcement_y`、`announcement_width`、`announcement_height`：公告栏位置和尺寸。

## 编译

项目使用 Gradle Loom 和 Java 21。运行：

```powershell
./gradlew build
```

如需为当前 FreeCore 运行环境编译并安装，可使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\compile-runtime.ps1
powershell -ExecutionPolicy Bypass -File .\package-runtime.ps1
```
