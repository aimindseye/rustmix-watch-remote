# Download Apply Commands

These commands assume the zip files were downloaded to `~/Downloads` and the local repo is:

```text
/home/mindseye73/Documents/projects/rustmix-watch-remote
```

## Apply complete overlay

```bash
cd /home/mindseye73/Documents/projects/rustmix-watch-remote
unzip -o ~/Downloads/rustmix-watch-remote-complete-r1-overlay.zip -d .
chmod +x scripts/*.sh
./scripts/validate_repo_layout.sh
```

## Initialize git and push

```bash
cd /home/mindseye73/Documents/projects/rustmix-watch-remote

git status
git add .
git commit -m "chore: initialize Rustmix Remote repo"

git branch -M main
git remote add origin https://github.com/aimindseye/rustmix-watch-remote.git 2>/dev/null || \
  git remote set-url origin https://github.com/aimindseye/rustmix-watch-remote.git

git push -u origin main
```

## Android build setup

```bash
cd /home/mindseye73/Documents/projects/rustmix-watch-remote/android

cat > local.properties <<EOF
sdk.dir=$HOME/Android/Sdk
EOF

# If your SDK path is lowercase, use this instead:
# sdk.dir=$HOME/Android/sdk

# Create a Gradle wrapper if the repo does not already have one.
gradle wrapper --gradle-version 9.1.0

./gradlew :app-wear:assembleDebug
```

## Install to paired Wear OS watch

```bash
cd /home/mindseye73/Documents/projects/rustmix-watch-remote/android
./gradlew :app-wear:installDebug
```
