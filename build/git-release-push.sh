#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

VER="$(node -p "require('./VERSION.json').version")"
if [[ -z "$VER" ]]; then
  echo "ERROR: could not read version from VERSION.json"
  exit 1
fi

echo git add .
git add .

if git diff --cached --quiet; then
  echo "no file changes — empty commit Release $VER"
  git commit --allow-empty -m "Release $VER"
else
  echo "git commit -m \"Release $VER\""
  git commit -m "Release $VER"
fi

echo git push
git push
echo "Pushed Release $VER"
