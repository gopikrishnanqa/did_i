@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

for /f "usebackq delims=" %%V in (`node -p "require('./VERSION.json').version"`) do set "VER=%%V"
if "%VER%"=="" (
  echo ERROR: could not read version from VERSION.json
  exit /b 1
)

echo git add .
git add .
if errorlevel 1 exit /b 1

git diff --cached --quiet
if errorlevel 1 (
  echo git commit -m "Release %VER%"
  git commit -m "Release %VER%"
) else (
  echo no file changes — empty commit Release %VER%
  git commit --allow-empty -m "Release %VER%"
)
if errorlevel 1 exit /b 1

echo git push
git push
if errorlevel 1 exit /b 1

echo Pushed Release %VER%
exit /b 0
