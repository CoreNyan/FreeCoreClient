$outDir = 'D:\freecore_runtime_classes'
Remove-Item -Recurse -Force $outDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$minecraftCandidates = @('E:\FreeCore\.minecraft', 'E:\FreeCore 26.2\.minecraft', 'E:\Sample\FreeCore - 副本\.minecraft')
$minecraftDir = $minecraftCandidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'versions\FreeCore\FreeCore.jar') } | Select-Object -First 1
if (-not $minecraftDir) { throw 'A FreeCore Minecraft installation was not found.' }
$jarPaths = (Get-ChildItem (Join-Path $minecraftDir 'versions\FreeCore\mods') -Filter '*.jar').FullName
$jarPaths += (Get-ChildItem (Join-Path $minecraftDir 'libraries') -Recurse -Filter '*.jar').FullName
$jarPaths += (Join-Path $minecraftDir 'versions\FreeCore\FreeCore.jar')
$classPath = [string]::Join(';', $jarPaths)
$sourceFiles = @('src/main/java/cc/freecore/client/BootstrapConfig.java','src/main/java/cc/freecore/client/FreeCoreConfig.java','src/main/java/cc/freecore/client/FreeCoreClientRuntime.java','src/main/java/cc/freecore/client/RemoteIconCache.java','src/main/java/cc/freecore/client/BackgroundManager.java','src/main/java/cc/freecore/client/IconLoader.java','src/main/java/cc/freecore/client/LogoManager.java','src/main/java/cc/freecore/client/ButtonIconManager.java','src/main/java/cc/freecore/client/GuiActions26.java','src/main/java/cc/freecore/client/FreeCoreButton26.java','src/client/java/cc/freecore/client/mixin/TitleScreenMixin26.java','src/client/java/cc/freecore/client/mixin/PauseScreenMixin26.java','src/client/java/cc/freecore/client/mixin/SettingsBackgroundMixin26.java','src/client/java/cc/freecore/client/mixin/MinecraftTitleMixin26.java','src/client/java/cc/freecore/client/mixin/LoadingOverlayMixin26.java','src/client/java/cc/freecore/client/mixin/LevelLoadingScreenMixin26.java','src/client/java/cc/freecore/client/mixin/ProgressScreenMixin26.java')
$sourceFiles += 'src/main/java/cc/freecore/client/FreeCoreText.java'
$sourceFiles += 'src/client/java/cc/freecore/client/mixin/GenericMessageScreenMixin26.java'
& 'C:\Program Files\Java\jdk-25.0.4\bin\javac.exe' -encoding UTF-8 -cp $classPath -d $outDir $sourceFiles
exit $LASTEXITCODE
