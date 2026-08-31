$targetJar = 'E:\FreeCore 26.2\.minecraft\versions\FreeCore\mods\freecore-client-1.0.0.jar'
if (Test-Path $targetJar) { Move-Item -LiteralPath $targetJar -Destination ($targetJar + '.previous') -Force }
& 'C:\Program Files\Java\jdk-25.0.4\bin\jar.exe' --create --file $targetJar -C 'D:\freecore_runtime_classes' cc -C 'src/main/resources' fabric.mod.json -C 'src/main/resources' freecoreclient.mixins.json
& 'C:\Program Files\Java\java-21\bin\jar.exe' --list --file $targetJar
