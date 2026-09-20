# Android release builds (Play Console AAB)

## One command

**Windows (PowerShell / cmd):**

```bat
build\bundle-release.bat
```

**Git Bash / WSL / macOS / Linux:**

```bash
bash build/bundle-release.sh
```

## What the script does

1. **Bumps version** in `VERSION.json` (patch by default) and `androidVersionCode` (+1)
2. Syncs `versionName` / `versionCode` in `app/build.gradle.kts`
3. Uses repo-root `credentials/keystore.properties` + upload `.jks`
4. Checks that an Android NDK is installed (needed for native debug symbols)
5. Runs `bundleRelease` with `ndk.debugSymbolLevel=SYMBOL_TABLE`
6. Copies the AAB to `build/out/didicheck-<version>-code<code>.aab`
7. Copies `native-debug-symbols.zip` next to the AAB when available
8. Copies default Play “What’s new” text from `build/play-default-notes.txt`
9. Runs `git-release-push` (commit + push)

### Version flags

```bat
build\bundle-release.bat
build\bundle-release.bat --minor
build\bundle-release.bat --major
build\bundle-release.bat --no-bump
```

## Signing setup

1. Copy `credentials/keystore.properties.example` → `credentials/keystore.properties`
2. Add `credentials/druanlabs-upload-key.jks` (same upload key as other Druan Labs apps is fine)
3. Fill store and key passwords in `keystore.properties`

## Output

- `.gradle-build/app/outputs/bundle/release/app-release.aab`
- `build/out/didicheck-<version>-code<code>.aab` (copy for Play upload)
- `build/out/didicheck-<version>-code<code>-native-debug-symbols.zip` (if produced)
- `build/out/play-release-notes-<version>.txt`

Do not put real passwords in this folder — keep them in `credentials/keystore.properties` only.
