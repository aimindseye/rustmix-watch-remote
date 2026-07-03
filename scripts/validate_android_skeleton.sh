#!/usr/bin/env bash
set -euo pipefail

cd /home/mindseye73/Documents/projects/rustmix-watch-remote/android

if [[ ! -f local.properties ]]; then
  if [[ -d "$HOME/Android/Sdk" ]]; then
    echo "sdk.dir=$HOME/Android/Sdk" > local.properties
  elif [[ -d "$HOME/Android/sdk" ]]; then
    echo "sdk.dir=$HOME/Android/sdk" > local.properties
  fi
fi

./gradlew :app-wear:assembleDebug || gradle :app-wear:assembleDebug
