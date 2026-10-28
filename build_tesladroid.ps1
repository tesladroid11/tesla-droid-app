param(
    [string]$Task = 'assembleDebug'
)

$ErrorActionPreference = 'Stop'

if (-not $env:JAVA_HOME) {
    $studioJava = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path $studioJava) {
        $env:JAVA_HOME = $studioJava
    }
}

$projectDir = $PSScriptRoot
$gradleBin = Join-Path $projectDir 'gradlew.bat'
if (-not (Test-Path $gradleBin)) {
    throw "Gradle executable not found at $gradleBin"
}

Write-Host "Building TeslaDroid ($Task)..." -ForegroundColor Cyan
Push-Location $projectDir
try {
    & $gradleBin $Task
    if ($LASTEXITCODE -ne 0) { throw "Build failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

$builtApk = Join-Path $projectDir 'app\build\outputs\apk\debug\app-debug.apk'

# Resolve Android SDK build tools and signing keystore
$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$buildToolsDir = Join-Path $sdkRoot 'build-tools'
$buildTools = if (Test-Path $buildToolsDir) {
    Get-ChildItem $buildToolsDir -Directory | Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
} else { $null }

$ks = Join-Path $env:USERPROFILE '.android\debug.keystore'
$classesDex = Join-Path $projectDir 'classes10.dex'

if (Test-Path $builtApk) {
    $outDir = Join-Path $projectDir 'app\build\outputs\apk\debug'
    $aligned = Join-Path $outDir 'TeslaDroid_aligned.apk'
    $dest = Join-Path $outDir 'TeslaDroid_aligned_signed.apk'

    # Inject native backend classes (classes10.dex) as next sequential dex
    if (Test-Path $classesDex) {
        Write-Host "Injecting native backend engine into APK..." -ForegroundColor Cyan
        python -c "import zipfile, re, shutil; apk = r'$builtApk'; src_dex = r'$classesDex'; dex_data = open(src_dex, 'rb').read(); zin = zipfile.ZipFile(apk, 'r'); entries = []; [entries.append((item, zin.read(item.filename))) for item in zin.infolist()]; zin.close(); dex_names = [e[0].filename for e in entries if re.match(r'^classes\d*\.dex$', e[0].filename)]; indices = [1 if x == 'classes.dex' else int(re.search(r'\d+', x).group()) for x in dex_names]; next_name = f'classes{max(indices) + 1}.dex'; tmp = apk + '.tmp'; zout = zipfile.ZipFile(tmp, 'w'); [zout.writestr(item, content) for item, content in entries]; zout.writestr(next_name, dex_data); zout.close(); shutil.move(tmp, apk); print(f'Successfully injected backend engine as {next_name}')"
    }

    # Zipalign and sign with debug keystore
    $zipalign = if ($buildTools) { Join-Path $buildTools 'zipalign.exe' } else { $null }
    $apksigner = if ($buildTools) { Join-Path $buildTools 'apksigner.bat' } else { $null }
    if ($zipalign -and $apksigner -and (Test-Path $zipalign) -and (Test-Path $ks)) {
        if (Test-Path $aligned) { Remove-Item $aligned -Force }
        if (Test-Path $dest) { Remove-Item $dest -Force }
        & $zipalign -p -f 4 $builtApk $aligned
        & $apksigner sign --ks $ks --ks-pass pass:android --key-pass pass:android --out $dest $aligned
        if (Test-Path $aligned) { Remove-Item $aligned -Force }
        Write-Host "Built, aligned and signed successfully: $dest" -ForegroundColor Green
    } else {
        Copy-Item $builtApk $dest -Force
        Write-Host "Copied APK to: $dest" -ForegroundColor Yellow
    }
}
