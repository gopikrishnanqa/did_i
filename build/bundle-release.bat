@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0.."
set "ROOT=%CD%"
set "OUT_DIR=%ROOT%\build\out"
set "BUMP_ARGS="

if not defined JAVA_HOME set "JAVA_HOME=D:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem Optional: --no-bump | --minor | --major
:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="--no-bump" set "BUMP_ARGS=--no-bump"
if /I "%~1"=="--minor" set "BUMP_ARGS=--minor"
if /I "%~1"=="--major" set "BUMP_ARGS=--major"
shift
goto parse_args
:args_done

echo.
echo === Did I? (didicheck): Android bundleRelease ===
echo Root: %ROOT%
echo.

if not exist "%ROOT%\credentials\keystore.properties" (
  echo ERROR: Missing credentials\keystore.properties
  echo Copy credentials\keystore.properties.example and fill real passwords.
  exit /b 1
)
if not exist "%ROOT%\credentials\druanlabs-upload-key.jks" (
  echo ERROR: Missing credentials\druanlabs-upload-key.jks
  exit /b 1
)

where node >nul 2>&1
if errorlevel 1 (
  echo ERROR: node is not on PATH
  exit /b 1
)

echo Bumping version / versionCode...
call node "%ROOT%\scripts\bump-release-version.js" %BUMP_ARGS%
if errorlevel 1 exit /b 1

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

set "NDK_OK="
for %%P in ("%ANDROID_SDK_ROOT%" "%ANDROID_HOME%" "%LOCALAPPDATA%\Android\Sdk") do (
  if not defined NDK_OK if exist "%%~P\ndk" (
    for /d %%D in ("%%~P\ndk\*") do set "NDK_OK=1"
  )
)
if not defined NDK_OK if exist "%ROOT%\local.properties" (
  powershell -NoProfile -Command ^
    "$p = @{}; Get-Content -LiteralPath '%ROOT%\local.properties' | ForEach-Object { if ($_ -match '^sdk\.dir=(.*)$') { $p.sdk = $matches[1] -replace '\\\\','\' } };" ^
    "if ($p.sdk -and (Get-ChildItem -LiteralPath (Join-Path $p.sdk 'ndk') -Directory -ErrorAction SilentlyContinue)) { exit 0 }; exit 1"
  if not errorlevel 1 set "NDK_OK=1"
)
if not defined NDK_OK (
  echo ERROR: Android NDK not found. Play needs native debug symbols for the .so files in this AAB.
  echo Install NDK ^(Side by side^) in Android Studio: SDK Manager -^> SDK Tools.
  echo Then re-run this script.
  exit /b 1
)

echo Running bundleRelease ...
if exist "%ROOT%\.tools\gradle-8.9\bin\gradle.bat" (
  call "%ROOT%\.tools\gradle-8.9\bin\gradle.bat" bundleRelease --no-daemon
) else (
  call "%ROOT%\gradlew.bat" bundleRelease --no-daemon
)
set "GRADLE_EXIT=!ERRORLEVEL!"
if not "!GRADLE_EXIT!"=="0" (
  echo ERROR: bundleRelease failed with exit !GRADLE_EXIT!
  exit /b !GRADLE_EXIT!
)

set "AAB=%ROOT%\.gradle-build\app\outputs\bundle\release\app-release.aab"
if not exist "%AAB%" (
  echo ERROR: AAB not found at %AAB%
  exit /b 1
)

for /f "usebackq delims=" %%V in (`node -p "require('./VERSION.json').version"`) do set "APP_VER=%%V"
for /f "usebackq delims=" %%C in (`node -p "require('./VERSION.json').androidVersionCode"`) do set "APP_CODE=%%C"
for /f "usebackq delims=" %%N in (`node -p "require('./VERSION.json').notes"`) do set "APP_NOTES=%%N"

set "DEST=%OUT_DIR%\didicheck-!APP_VER!-code!APP_CODE!.aab"
copy /Y "%AAB%" "%DEST%" >nul
copy /Y "%ROOT%\build\play-default-notes.txt" "%OUT_DIR%\play-release-notes-!APP_VER!.txt" >nul

set "MAPPING=%ROOT%\.gradle-build\app\outputs\mapping\release\mapping.txt"
set "MAPPING_DEST=%OUT_DIR%\didicheck-!APP_VER!-code!APP_CODE!-mapping.txt"
if exist "%MAPPING%" (
  copy /Y "%MAPPING%" "%MAPPING_DEST%" >nul
)

set "SYMBOLS="
set "SYMBOLS_AGP=%ROOT%\.gradle-build\app\outputs\native-debug-symbols\release\native-debug-symbols.zip"
set "MERGED_LIBS=%ROOT%\.gradle-build\app\intermediates\merged_native_libs\release\mergeReleaseNativeLibs\out\lib"
set "SYMBOLS_DEST=%OUT_DIR%\didicheck-!APP_VER!-code!APP_CODE!-native-debug-symbols.zip"
if exist "%SYMBOLS_AGP%" (
  copy /Y "%SYMBOLS_AGP%" "%SYMBOLS_DEST%" >nul
  set "SYMBOLS=%SYMBOLS_DEST%"
) else if exist "%MERGED_LIBS%" (
  powershell -NoProfile -Command "Compress-Archive -Path (Join-Path '%MERGED_LIBS%' '*') -DestinationPath '%SYMBOLS_DEST%' -Force"
  if exist "%SYMBOLS_DEST%" set "SYMBOLS=%SYMBOLS_DEST%"
)

echo.
echo SUCCESS
echo   version !APP_VER!  versionCode !APP_CODE!
echo   notes: !APP_NOTES!
echo   %AAB%
echo   %DEST%
if defined SYMBOLS (
  echo   native symbols: !SYMBOLS!
  echo   Upload the AAB to Play. Symbols are also inside the bundle.
) else (
  echo   WARNING: native-debug-symbols.zip was not produced. Upload may still warn in Play Console.
)
if exist "%MAPPING_DEST%" (
  echo   R8 mapping: %MAPPING_DEST%
  echo   Upload this mapping file under App bundle explorer if Play asks for deobfuscation.
)
echo.
echo Running git-release-push ...
call "%ROOT%\build\git-release-push.bat"
if errorlevel 1 echo WARNING: git-release-push failed. AAB is still at %DEST%
echo.
exit /b 0
