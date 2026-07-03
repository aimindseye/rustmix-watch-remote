#!/usr/bin/env bash
set -euo pipefail

cd /home/mindseye73/Documents/projects/rustmix-watch-remote

git add README.md .gitignore docs firmware-notes scripts
if git diff --cached --quiet; then
  echo "No docs/protocol changes to commit."
else
  git commit -m "docs: add rustmix remote architecture and protocol"
fi

git branch -M main
git remote add origin https://github.com/aimindseye/rustmix-watch-remote.git 2>/dev/null || \
  git remote set-url origin https://github.com/aimindseye/rustmix-watch-remote.git

git push -u origin main
