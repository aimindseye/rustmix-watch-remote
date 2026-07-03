# r2 Download and Apply Commands

## Watch repo protocol tools overlay

After downloading `rustmix-watch-remote-r2-protocol-tools-overlay.zip` to `~/Downloads`:

```bash
cd /home/mindseye73/Documents/projects/rustmix-watch-remote

unzip -o ~/Downloads/rustmix-watch-remote-r2-protocol-tools-overlay.zip -d .
chmod +x scripts/validate_rrbp_tools.sh

./scripts/validate_repo_layout.sh
./scripts/validate_android_skeleton.sh
./scripts/validate_rrbp_tools.sh

cd android
./gradlew :app-wear:assembleDebug
cd ..

git status
git add .
git commit -m "docs: add RRBP tools and Rustmix-Wave r1 integration notes"
git push
```

## Rustmix-Wave firmware scaffold overlay

After downloading `rustmix-wave-ble-remote-r1-scaffold-overlay.zip` to `~/Downloads`:

```bash
cd /home/mindseye73/Documents/projects/rustmix-wave
# or your actual Rustmix-Wave repo path

unzip -o ~/Downloads/rustmix-wave-ble-remote-r1-scaffold-overlay.zip -d .
chmod +x scripts/validate_rustmix_remote_rrbp.sh

./scripts/validate_rustmix_remote_rrbp.sh

git status
git add firmware/assistant-rs/src/rustmix_remote docs/rustmix-remote scripts/validate_rustmix_remote_rrbp.sh
git commit -m "feat: add Rustmix Remote RRBP parser scaffold"
git push
```
