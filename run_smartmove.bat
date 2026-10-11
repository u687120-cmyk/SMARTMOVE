@echo off
echo ======================================================
echo   SMARTMOVE AI - Automated Device Build ^& Launcher
echo ======================================================

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ADB=D:\Android\platform-tools\adb.exe"

echo.
echo [1/3] Setting up ADB port forwarding (tcp:5000 -^> localhost:5000)...
"%ADB%" reverse tcp:5000 tcp:5000

echo.
echo [2/3] Compiling and Installing SMARTMOVE AI Debug APK...
call gradlew.bat installDebug --console=plain

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed. Stopping background daemons...
    call gradlew.bat --stop
    exit /b 1
)

echo.
echo [3/3] Launching SMARTMOVE AI on connected device...
"%ADB%" shell am start -n com.smartmove.ai/.MainActivity

echo.
echo ======================================================
echo   SMARTMOVE AI is now running on your phone!
echo ======================================================
