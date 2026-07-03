#!/usr/bin/env bash
set -euo pipefail

cd /home/mindseye73/Documents/projects/rustmix-watch-remote
chmod +x scripts/*.sh
./scripts/validate_repo_layout.sh

git add .
if git diff --cached --quiet; then
  echo "No changes to commit."
else
  git commit -m "chore: initialize Rustmix Remote"
fi

git branch -M main
git remote add origin https://github.com/aimindseye/rustmix-watch-remote.git 2>/dev/null || \
  git remote set-url origin https://github.com/aimindseye/rustmix-watch-remote.git

git push -u origin main
