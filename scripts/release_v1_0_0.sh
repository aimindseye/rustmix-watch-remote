#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="v1.0.0"
APK_SRC="android/app-wear/build/outputs/apk/debug/app-wear-debug.apk"
APK_OUT="rustmix-remote-v1.0.0-debug.apk"

cd android
./gradlew :app-wear:assembleDebug
cd ..

test -f "$APK_SRC"

cp "$APK_SRC" "$APK_OUT"

git diff --check

if ! git rev-parse "$VERSION" >/dev/null 2>&1; then
  git tag -a "$VERSION" -m "Rustmix Remote v1.0.0"
fi

git push origin main
git push origin "$VERSION"

gh release view "$VERSION" >/dev/null 2>&1 && gh release delete "$VERSION" --yes || true

gh release create "$VERSION" \
  "$APK_OUT" \
  --title "Rustmix Remote v1.0.0" \
  --notes-file docs/releases/v1.0.0.md
