$outDir = 'D:\freecore_classes'
Remove-Item -Recurse -Force $outDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$jarPaths = (Get-ChildItem 'E:\FreeCore 26.2\.minecraft\versions\FreeCore\mods' -Filter '*.jar').FullName
$jarPaths += 'E:\FreeCore 26.2\.minecraft\versions\FreeCore\FreeCore.jar'
$classPath = [string]::Join(';', $jarPaths)
$sourceFiles = (Get-ChildItem src/main/java,src/client/java -Recurse -Filter '*.java').FullName
& 'C:\Program Files\Java\java-21\bin\javac.exe' -encoding UTF-8 -cp $classPath -d $outDir $sourceFiles
exit $LASTEXITCODE
