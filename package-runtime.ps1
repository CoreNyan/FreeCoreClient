$targetJar = 'E:\FreeCore 26.2\.minecraft\versions\FreeCore\mods\freecore-client-1.0.0.jar'
if (Test-Path $targetJar) { Move-Item -LiteralPath $targetJar -Destination ($targetJar + '.previous') -Force }
$versionLine = Get-Content 'gradle.properties' | Where-Object { $_ -match '^mod_version=' } | Select-Object -First 1
$modVersion = if ($versionLine) { ($versionLine -split '=', 2)[1].Trim() } else { '1.0.0' }
$metadataDir = Join-Path $env:TEMP ('freecore-runtime-metadata-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $metadataDir | Out-Null
$metadata = Get-Content 'src/main/resources/fabric.mod.json' -Raw | ConvertFrom-Json
$metadata.version = $modVersion
$metadata | ConvertTo-Json -Depth 10 | Set-Content (Join-Path $metadataDir 'fabric.mod.json') -Encoding UTF8
Copy-Item 'src/main/resources/freecoreclient.mixins.json' (Join-Path $metadataDir 'freecoreclient.mixins.json')
& 'C:\Program Files\Java\jdk-25.0.4\bin\jar.exe' --create --file $targetJar -C 'D:\freecore_runtime_classes' cc -C $metadataDir fabric.mod.json -C $metadataDir freecoreclient.mixins.json
Remove-Item -Recurse -Force $metadataDir -ErrorAction SilentlyContinue
& 'C:\Program Files\Java\java-21\bin\jar.exe' --list --file $targetJar
