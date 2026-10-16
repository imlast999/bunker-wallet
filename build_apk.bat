@echo off
setlocal enabledelayedexpansion

title CleanVault / Bunker - Android APK Build Pipeline

echo =====================================================================
echo           BUNKER / CLEANVAULT RWA WALLET - APK BUILD PIPELINE        
echo =====================================================================
echo.

cd /d "%~dp0"

:: ---------------------------------------------------------------------
:: 1. VERIFY JAVA_HOME AND JDK ACCESSIBILITY
:: ---------------------------------------------------------------------
echo [1/5] Checking Java Development Kit (JDK)...

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
        echo   [OK] JAVA_HOME is set: !JAVA_HOME!
        goto check_java_version
    )
)

:: Try finding Java in PATH
where java.exe >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "JAVA_EXE=java.exe"
    echo   [OK] Java found in system PATH.
    goto check_java_version
)

:: Auto-detect common Android Studio embedded JDKs
if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
    set "JAVA_EXE=!JAVA_HOME!\bin\java.exe"
    echo   [OK] Auto-detected Android Studio JBR at !JAVA_HOME!
    goto check_java_version
)

if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    set "JAVA_EXE=!JAVA_HOME!\bin\java.exe"
    echo   [OK] Auto-detected Android Studio JBR at !JAVA_HOME!
    goto check_java_version
)

echo.
echo [ERROR] No valid JDK or JAVA_HOME found!
echo Please install JDK 17 or set JAVA_HOME pointing to your JDK / Android Studio JBR directory.
goto build_failed

:check_java_version
"!JAVA_EXE!" -version
echo.

:: ---------------------------------------------------------------------
:: 2. VERIFY OR CREATE local.properties (ANDROID SDK DIR)
:: ---------------------------------------------------------------------
echo [2/5] Checking Android SDK configuration (local.properties)...

set "SDK_FOUND="
if exist "local.properties" (
    findstr /i "sdk.dir" "local.properties" >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        echo   [OK] Existing local.properties with sdk.dir detected.
        set "SDK_FOUND=1"
    )
)

if not defined SDK_FOUND (
    echo   [INFO] Auto-detecting Android SDK path...
    set "CANDIDATE_SDK="

    if defined ANDROID_HOME if exist "!ANDROID_HOME!\platform-tools" set "CANDIDATE_SDK=!ANDROID_HOME!"
    if not defined CANDIDATE_SDK if defined ANDROID_SDK_ROOT if exist "!ANDROID_SDK_ROOT!\platform-tools" set "CANDIDATE_SDK=!ANDROID_SDK_ROOT!"
    if not defined CANDIDATE_SDK if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools" set "CANDIDATE_SDK=%LOCALAPPDATA%\Android\Sdk"

    if defined CANDIDATE_SDK (
        echo   [OK] Found Android SDK at: !CANDIDATE_SDK!
        :: Escape backslashes for local.properties
        set "FORMATTED_SDK=!CANDIDATE_SDK:\=/!"
        echo sdk.dir=!FORMATTED_SDK!> local.properties
        echo   [OK] Generated local.properties successfully.
    ) else (
        echo.
        echo [WARNING] Could not auto-locate Android SDK!
        echo Please ensure Android SDK is installed or set 'sdk.dir' manually in local.properties.
        echo Expected default: %LOCALAPPDATA%\Android\Sdk
    )
)
echo.

:: ---------------------------------------------------------------------
:: 3. CLEAN & COMPILE DEBUG APK VIA GRADLE
:: ---------------------------------------------------------------------
echo [3/5] Compiling Bunker Android Debug APK via Gradle...
echo Running: gradlew.bat assembleDebug
echo.

if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found in project root!
    goto build_failed
)

if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo   [INFO] gradle-wrapper.jar missing. Downloading official Gradle wrapper jar...
    powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar', 'gradle\wrapper\gradle-wrapper.jar')"
    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        echo [ERROR] Could not download or find gradle-wrapper.jar!
        goto build_failed
    )
    echo   [OK] gradle-wrapper.jar restored.
)

call gradlew.bat assembleDebug
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Gradle compilation failed with exit code %ERRORLEVEL%!
    goto build_failed
)

echo.
echo   [OK] Gradle assembleDebug completed successfully.
echo.

:: ---------------------------------------------------------------------
:: 4. LOCATE & PACKAGE APK INTO dist\
:: ---------------------------------------------------------------------
echo [4/5] Packaging build artifact...

set "SOURCE_APK=app\build\outputs\apk\debug\app-debug.apk"
set "DIST_DIR=dist"
set "TARGET_APK=%DIST_DIR%\Bunker-Debug.apk"

if not exist "%SOURCE_APK%" (
    echo [ERROR] Generated APK could not be found at: %SOURCE_APK%
    goto build_failed
)

if not exist "%DIST_DIR%" (
    mkdir "%DIST_DIR%"
)

copy /y "%SOURCE_APK%" "%TARGET_APK%" >nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to copy APK to %TARGET_APK%
    goto build_failed
)

echo =====================================================================
echo                       BUILD SUCCESSFUL!                              
echo =====================================================================
echo.
echo Output APK: %CD%\%TARGET_APK%
for %%A in ("%TARGET_APK%") do (
    set "APK_SIZE=%%~zA"
    set /a APK_SIZE_MB=!APK_SIZE! / 1048576
    echo APK Size  : !APK_SIZE_MB! MB (!APK_SIZE! bytes^)
)
echo.

:: ---------------------------------------------------------------------
:: 5. CHECK FOR CONNECTED ADB DEVICES & OFFER DIRECT INSTALLATION
:: ---------------------------------------------------------------------
echo [5/5] Checking for connected Android devices via ADB...

where adb.exe >nul 2>&1
if %ERRORLEVEL% neq 0 (
    if defined CANDIDATE_SDK if exist "!CANDIDATE_SDK!\platform-tools\adb.exe" (
        set "PATH=!PATH!;!CANDIDATE_SDK!\platform-tools"
    ) else if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "PATH=!PATH!;%LOCALAPPDATA%\Android\Sdk\platform-tools"
    )
)

where adb.exe >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo   [INFO] ADB is not in system PATH. Skipping direct device install prompt.
    goto finish_build
)

:: Query connected online devices
set /a DEVICE_COUNT=0
set "LAST_DEVICE="
for /f "tokens=1,2" %%A in ('adb devices 2^>nul') do (
    if "%%B"=="device" (
        set /a DEVICE_COUNT+=1
        set "LAST_DEVICE=%%A"
        echo   Connected Device ID: %%A
    )
)

if !DEVICE_COUNT! GTR 0 (
    echo.
    echo Found !DEVICE_COUNT! active Android device^(s^) connected via USB/WiFi.
    set /p INSTALL_CHOICE="Do you want to install Bunker-Debug.apk automatically now? (Y/N): "
    if /i "!INSTALL_CHOICE!"=="Y" (
        echo.
        echo Installing %TARGET_APK% to device...
        adb install -r "%TARGET_APK%"
        if !ERRORLEVEL! equ 0 (
            echo.
            echo [SUCCESS] Bunker app installed successfully on device!
            echo Launching CleanVault / Bunker...
            adb shell monkey -p com.cleanvault.wallet -c android.intent.category.LAUNCHER 1 >nul 2>&1
        ) else (
            echo.
            echo [WARNING] ADB install reported an issue. Please verify device permissions/USB debugging.
        )
    ) else (
        echo Skipping device installation.
    )
) else (
    echo   [INFO] No USB/Wi-Fi devices currently detected with ADB debugging authorized.
    echo   Connect your device and enable USB debugging to install directly.
)

:finish_build
echo.
echo Ready to distribute: %CD%\%TARGET_APK%
echo Press any key to exit.
pause >nul
exit /b 0

:build_failed
echo.
echo =====================================================================
echo                         BUILD FAILED!                                
echo =====================================================================
echo Please review the Gradle logs above to resolve any missing dependencies
echo or environmental configurations.
echo.
pause
exit /b 1
