# THE DAY - Live Preview Workflow
$ErrorActionPreference = "Stop"

$JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$GRADLE_BIN = "C:\Users\godby\gradle\gradle-8.7\bin"
$ADB = "C:\Users\godby\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$SCRCPY = "C:\Users\godby\AppData\Local\Microsoft\WinGet\Packages\Genymobile.scrcpy_Microsoft.Winget.Source_8wekyb3d8bbwe\scrcpy-win64-v4.1\scrcpy.exe"

$env:JAVA_HOME = $JAVA_HOME
$env:PATH = "$JAVA_HOME\bin;$GRADLE_BIN;$env:PATH"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  THE DAY - LIVE PREVIEW WORKFLOW" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Check ADB Devices
Write-Host ""
Write-Host "1. Checking connected Android devices..." -ForegroundColor Yellow
$deviceLines = @(& $ADB devices -l | Where-Object { $_ -match "^\S+\s+device\b" })

if ($deviceLines.Count -eq 0) {
    Write-Host "No authorized Android device detected!" -ForegroundColor Red
    Write-Host "Please ensure your phone is connected via USB with USB Debugging enabled," -ForegroundColor Yellow
    Write-Host "or start an Android emulator, then re-run this script." -ForegroundColor Yellow
    & $ADB devices -l
    exit 1
}

$deviceId = ($deviceLines[0].Trim() -split "\s+")[0]
Write-Host "Device connected: $deviceId" -ForegroundColor Green

# 2. Build APK
Write-Host ""
Write-Host "2. Building latest debug APK..." -ForegroundColor Yellow
& "$GRADLE_BIN\gradle.bat" assembleDebug --daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "Gradle build failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "Build successful!" -ForegroundColor Green

# 3. Install and Launch
Write-Host ""
Write-Host "3. Installing THE DAY..." -ForegroundColor Yellow
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
& $ADB -s $deviceId install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "Installation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "Launching com.day.app/.MainActivity..." -ForegroundColor Yellow
& $ADB -s $deviceId shell am start -n com.day.app/.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
Write-Host "App launched successfully!" -ForegroundColor Green

# 4. Live Screen Mirror (scrcpy)
Write-Host ""
Write-Host "4. Starting live screen mirror..." -ForegroundColor Yellow
$scrcpyProc = Get-Process -Name "scrcpy" -ErrorAction SilentlyContinue
if (-not $scrcpyProc) {
    if (Test-Path $SCRCPY) {
        Start-Process -FilePath $SCRCPY -ArgumentList "--always-on-top", "--window-title", "'THE DAY -- Live Preview'", "--stay-awake", "--max-fps=60"
        Write-Host "Live preview window opened via scrcpy (always-on-top)." -ForegroundColor Green
    } else {
        Write-Host "scrcpy not found at default path." -ForegroundColor Yellow
    }
} else {
    Write-Host "scrcpy live preview is already active." -ForegroundColor Green
}

Start-Sleep -Milliseconds 800
& $ADB -s $deviceId exec-out screencap -p > "preview.png"
if (Test-Path "preview.png") {
    Write-Host "Captured UI snapshot to preview.png" -ForegroundColor Green
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  THE DAY is live and running!" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Cyan
