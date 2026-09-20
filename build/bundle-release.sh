#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$ROOT/build/out"
cd "$ROOT"

BUMP_ARGS=()
for arg in "$@"; do
  case "$arg" in
    --no-bump|--minor|--major) BUMP_ARGS+=("$arg") ;;
  esac
done

if [[ -z "${JAVA_HOME:-}" && -d "/c/Program Files/Android/Android Studio/jbr" ]]; then
  JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
  export JAVA_HOME
fi

echo
echo "=== Did I? (didicheck): Android bundleRelease ==="
echo "Root: $ROOT"
echo

if [[ ! -f "$ROOT/credentials/keystore.properties" ]]; then
  echo "ERROR: Missing credentials/keystore.properties"
  echo "Copy credentials/keystore.properties.example and fill real passwords."
  exit 1
fi
if [[ ! -f "$ROOT/credentials/druanlabs-upload-key.jks" ]]; then
  echo "ERROR: Missing credentials/druanlabs-upload-key.jks"
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "ERROR: node is not on PATH"
  exit 1
fi

echo "Bumping version / versionCode..."
node "$ROOT/scripts/bump-release-version.js" "${BUMP_ARGS[@]+"${BUMP_ARGS[@]}"}"

mkdir -p "$OUT_DIR"

SDK_DIR=""
if [[ -f "$ROOT/local.properties" ]]; then
  SDK_DIR="$(sed -n 's/^sdk.dir=//p' "$ROOT/local.properties" | tail -n 1 | sed 's/\\\\/\\/g')"
fi
SDK_DIR="${SDK_DIR:-${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}}"
if [[ -z "$SDK_DIR" && -d "${LOCALAPPDATA:-}/Android/Sdk" ]]; then
  SDK_DIR="${LOCALAPPDATA}/Android/Sdk"
fi
NDK_OK=""
if [[ -n "$SDK_DIR" && -d "$SDK_DIR/ndk" ]]; then
  shopt -s nullglob
  ndk_dirs=("$SDK_DIR/ndk"/*)
  shopt -u nullglob
  if ((${#ndk_dirs[@]})); then
    NDK_OK=1
  fi
fi
if [[ -z "$NDK_OK" ]]; then
  echo "ERROR: Android NDK not found. Play needs native debug symbols for the .so files in this AAB."
  echo "Install NDK (Side by side) in Android Studio: SDK Manager -> SDK Tools."
  echo "Then re-run this script."
  exit 1
fi

echo "Running bundleRelease ..."
if [[ -x "$ROOT/.tools/gradle-8.9/bin/gradle" ]]; then
  "$ROOT/.tools/gradle-8.9/bin/gradle" bundleRelease --no-daemon
elif [[ -f "$ROOT/gradlew" ]]; then
  bash "$ROOT/gradlew" bundleRelease --no-daemon
else
  cmd.exe //c "$ROOT/gradlew.bat" bundleRelease --no-daemon
fi

AAB="$ROOT/.gradle-build/app/outputs/bundle/release/app-release.aab"
if [[ ! -f "$AAB" ]]; then
  echo "ERROR: AAB not found at $AAB"
  exit 1
fi

APP_VER="$(node -p "require('./VERSION.json').version")"
APP_CODE="$(node -p "require('./VERSION.json').androidVersionCode")"
APP_NOTES="$(node -p "require('./VERSION.json').notes")"
DEST="$OUT_DIR/didicheck-${APP_VER}-code${APP_CODE}.aab"
cp -f "$AAB" "$DEST"
cp -f "$ROOT/build/play-default-notes.txt" "$OUT_DIR/play-release-notes-${APP_VER}.txt"

SYMBOLS_AGP="$ROOT/.gradle-build/app/outputs/native-debug-symbols/release/native-debug-symbols.zip"
MERGED_LIBS="$ROOT/.gradle-build/app/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib"
SYMBOLS_DEST="$OUT_DIR/didicheck-${APP_VER}-code${APP_CODE}-native-debug-symbols.zip"
SYMBOLS=""
if [[ -f "$SYMBOLS_AGP" ]]; then
  cp -f "$SYMBOLS_AGP" "$SYMBOLS_DEST"
  SYMBOLS="$SYMBOLS_DEST"
elif [[ -d "$MERGED_LIBS" ]]; then
  (
    cd "$MERGED_LIBS"
    if command -v zip >/dev/null 2>&1; then
      zip -r -q "$SYMBOLS_DEST" .
    else
      python - "$SYMBOLS_DEST" <<'PY'
import sys, zipfile, os
dest = sys.argv[1]
with zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as zf:
    for root, _, files in os.walk("."):
        for name in files:
            path = os.path.join(root, name)
            zf.write(path, path.replace("\\", "/"))
PY
    fi
  )
  if [[ -f "$SYMBOLS_DEST" ]]; then
    SYMBOLS="$SYMBOLS_DEST"
  fi
fi

echo
echo "SUCCESS"
echo "  version $APP_VER  versionCode $APP_CODE"
echo "  notes: $APP_NOTES"
echo "  $AAB"
echo "  $DEST"
if [[ -n "$SYMBOLS" ]]; then
  echo "  native symbols: $SYMBOLS"
  echo "  Upload the AAB to Play. Symbols are also inside the bundle."
else
  echo "  WARNING: native-debug-symbols.zip was not produced. Upload may still warn in Play Console."
fi
echo
echo "Running git-release-push ..."
if ! bash "$ROOT/build/git-release-push.sh"; then
  echo "WARNING: git-release-push failed. AAB is still at $DEST"
fi
echo
