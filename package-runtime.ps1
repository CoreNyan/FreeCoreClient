$versionLine = Get-Content 'gradle.properties' | Where-Object { $_ -match '^mod_version=' } | Select-Object -First 1
$modVersion = if ($versionLine) { ($versionLine -split '=', 2)[1].Trim() } else { '1.0.0' }
$minecraftCandidates = @('E:\FreeCore\.minecraft', 'E:\FreeCore 26.2\.minecraft', 'E:\Sample\FreeCore - 副本\.minecraft')
$minecraftDir = $minecraftCandidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'versions\FreeCore\FreeCore.jar') } | Select-Object -First 1
if (-not $minecraftDir) { throw 'A FreeCore Minecraft installation was not found.' }
$modsDir = Join-Path $minecraftDir 'versions\FreeCore\mods'
$targetJar = Join-Path $modsDir "freecore-client-$modVersion.jar"
Get-ChildItem -LiteralPath $modsDir -Filter 'freecore-client-*.jar' | Where-Object { $_.FullName -ne $targetJar } | ForEach-Object {
    Move-Item -LiteralPath $_.FullName -Destination ($_.FullName + '.previous') -Force
}
if (Test-Path $targetJar) { Move-Item -LiteralPath $targetJar -Destination ($targetJar + '.previous') -Force }
$metadataDir = Join-Path $env:TEMP ('freecore-runtime-metadata-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $metadataDir | Out-Null
$metadata = Get-Content 'src/main/resources/fabric.mod.json' -Raw | ConvertFrom-Json
$metadata.version = $modVersion
$metadata | ConvertTo-Json -Depth 10 | Set-Content (Join-Path $metadataDir 'fabric.mod.json') -Encoding UTF8
Copy-Item 'src/main/resources/freecoreclient.mixins.json' (Join-Path $metadataDir 'freecoreclient.mixins.json')
& 'C:\Program Files\Java\jdk-25.0.4\bin\jar.exe' --create --file $targetJar -C 'D:\freecore_runtime_classes' cc -C $metadataDir fabric.mod.json -C $metadataDir freecoreclient.mixins.json
Remove-Item -Recurse -Force $metadataDir -ErrorAction SilentlyContinue
& 'C:\Program Files\Java\java-21\bin\jar.exe' --list --file $targetJar
