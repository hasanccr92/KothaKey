@echo off
setlocal
set APK=%~dp0KothaKey-debug.apk
where adb >nul 2>&1
if errorlevel 1 (
  echo adb was not found on PATH. Install Android platform-tools, then run this again.
  exit /b 1
)
if not exist "%APK%" (
  echo %APK% is missing.
  exit /b 1
)
echo Waiting for a device...
adb start-server
adb wait-for-device
echo Installing %APK%
adb install -r "%APK%"
if errorlevel 1 exit /b 1
echo Opening KothaKey setup...
adb shell am start -n com.kothakey/.ime.SetupActivity
endlocal
