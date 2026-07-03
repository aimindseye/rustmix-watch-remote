#!/usr/bin/env bash
set -euo pipefail

cd /home/mindseye73/Documents/projects/rustmix-watch-remote

git add android scripts/validate_android_skeleton.sh
if git diff --cached --quiet; then
  echo "No Android skeleton changes to commit."
else
  git commit -m "feat: add Wear OS skeleton for Rustmix Remote"
fi

git push
