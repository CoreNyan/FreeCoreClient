$stagedJar = 'E:\LynnH Ma\Progarmming\Java Project\LynnH Ma\FreeCoreClient\freecore-client-26.2-compiled.jar'
if (Test-Path $stagedJar) { Remove-Item -LiteralPath $stagedJar -Force }
& 'C:\Program Files\Java\jdk-25.0.4\bin\jar.exe' --create --file $stagedJar -C 'D:\freecore_runtime_classes' cc -C 'src/main/resources' fabric.mod.json -C 'src/main/resources' freecoreclient.mixins.json
& 'C:\Program Files\Java\jdk-25.0.4\bin\jar.exe' --list --file $stagedJar
