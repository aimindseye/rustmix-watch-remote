#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

rm -f APPLY_README_ARCH_RELEASE_DOCS.sh
rm -rf rustmix_watch_remote_v1_docs_overlay
rm -f scripts/apply_rustmix_remote_v1_docs.sh

mkdir -p docs/images docs/releases docs/validation scripts

python3 - <<'PY'
from pathlib import Path

p = Path("README.md")
existing = p.read_text() if p.exists() else "# Rustmix Remote\n"

start = "<!-- RUSTMIX_REMOTE_V1_START -->"
end = "<!-- RUSTMIX_REMOTE_V1_END -->"

section = f"""{start}

## Rustmix Remote v1.0.0

Rustmix Remote is a Wear OS Bluetooth remote for Rustmix reader devices. The v1.0.0 release is validated with the Waveshare ESP32-S3 3.97-inch e-paper device running the Rustmix-Wave BLE feature firmware.

### Validated hardware

- Watch: Samsung Wear OS watch
- Reader device: Waveshare ESP32-S3 3.97-inch e-paper device
- Firmware: Rustmix-Wave with `rustmix-remote-ble` feature enabled
- Transport: BLE GATT
- Protocol: Rustmix Remote BLE Protocol / RRBP

### Screenshots

Remote screen:

![Rustmix Remote watch remote screen](docs/images/rustmix-remote-watch-remote-screen.jpg)

Device screen:

![Rustmix Remote watch device screen](docs/images/rustmix-remote-watch-device-screen.jpg)

### Accepted behavior

- Swipeable two-screen Wear OS UI works.
- Remote screen provides Previous, Next, Disconnect, and Device navigation.
- Device screen provides saved BLE address, Connect Saved, Scan / Fallback, and Remote navigation.
- Direct saved-address fallback works with the accepted Rustmix-Wave BLE MAC.
- TXT reader Previous/Next page turning works.
- EPUB reader Previous/Next page turning works.
- RRBP GATT write path is unchanged from the accepted r1 implementation.

### Connectivity notes

The app attempts BLE scan first. On the validated Samsung Wear OS watch, scan registration succeeds but scan callbacks may not reliably surface the Rustmix-Wave advertisement. For the accepted v1.0.0 workflow, use the Device screen and connect with the saved BLE address.

The validated Rustmix-Wave BLE address ended in `3D:66`. Your device may differ. Check the Rustmix-Wave firmware monitor for a line like:

```
Bluetooth MAC: xx:xx:xx:xx:xx:xx
```

Enter that BLE MAC on the Device screen, tap Save Address, then tap Connect Saved.

### Firmware requirements

Rustmix-Wave must be built with the BLE feature enabled:

```
cd /home/mindseye73/Documents/projects/rustmix-wave

export PATH="$HOME/.cargo/bin:$PATH"
export RUSTFLAGS="${{RUSTFLAGS:-}} --cfg esp_idf_version_least_5_5_0"

cargo +esp build \\
  --release \\
  --target xtensa-esp32s3-espidf \\
  --features rustmix-remote-ble
```

Flash the firmware ELF:

```
espflash flash --chip esp32s3 --monitor \\
  target/xtensa-esp32s3-espidf/release/waveshare-epd397-rust-app
```

Expected firmware logs:

```
rustmix-wave=rustmix-remote-gap event=AdvertisingStarted(Success)
rustmix-wave=rustmix-remote-gatts event=PeerConnected
rustmix-wave=rustmix-remote-gatts event=Write
rustmix-wave=rustmix-remote-command status=enqueued
rustmix-wave=rustmix-remote-event event=page-next route=reader-page
rustmix-wave=rustmix-remote-event event=page-previous route=reader-page
```

### Important firmware boundary

The Rustmix-Wave `rustmix-remote-ble` feature build owns the ESP32-S3 modem. Wi-Fi, NTP, weather networking, and Wi-Fi transfer are intentionally skipped in this BLE feature build. Normal non-BLE Rustmix-Wave builds must continue to preserve Wi-Fi behavior.

{end}
"""

if start in existing and end in existing:
    before = existing.split(start)[0].rstrip()
    after = existing.split(end, 1)[1].lstrip()
    new_text = before + "\n\n" + section + "\n" + after
else:
    new_text = existing.rstrip() + "\n\n" + section + "\n"

p.write_text(new_text)
PY

cat > docs/architecture.md <<'EOF'
# Rustmix Remote Architecture

Rustmix Remote is a Wear OS BLE remote for Rustmix reader firmware.

## v1.0.0 architecture

```
Samsung Wear OS Watch
        |
        | BLE GATT write
        v
Rustmix-Wave BLE GATT service
        |
        | RRBP command queue
        v
Rustmix-Wave main loop
        |
        | reader input event
        v
TXT / EPUB reader page navigation
```

## Protocol

Rustmix Remote uses the Rustmix Remote BLE Protocol, abbreviated RRBP.

Validated service UUID:

```
8f7a0000-6b8f-4a91-9e2c-727573740001
```

Validated command characteristic UUID:

```
8f7a0001-6b8f-4a91-9e2c-727573740001
```

RRBP command packets are 6 bytes:

```
byte 0: version
byte 1: sequence
byte 2: command
byte 3: flags
byte 4: parameter
byte 5: reserved
```

Accepted command examples:

```
01 00 01 00 00 00 = page next
01 02 02 00 00 00 = page previous
```

## Firmware safety model

The BLE callback parses and enqueues only. Reader state is mutated by the Rustmix-Wave main loop after the command is drained from the queue.

Accepted embedded boundary:

```
BLE callback: parse/enqueue only
main loop: mutate UI/reader state
```

## Connectivity model

The app attempts scan first. If scan does not produce a Rustmix result, the app falls back to direct GATT connect using the saved BLE MAC address.

This is the accepted v1.0.0 behavior for the Samsung Wear OS validation device.

## Rustmix-Wave BLE feature build boundary

The Rustmix-Wave `rustmix-remote-ble` build owns the ESP32-S3 modem.

In this build:

- BLE GATT is enabled.
- Wi-Fi is intentionally skipped.
- NTP is unavailable.
- Weather/network services are unavailable.
- Wi-Fi transfer is unavailable.
- TXT and EPUB page turning over BLE are accepted.

Normal non-BLE Rustmix-Wave builds must continue to preserve Wi-Fi behavior.
EOF

cat > docs/validation/rustmix-wave-reader-page-turn-r2.md <<'EOF'
# Rustmix Remote BLE r2 Validation: Rustmix-Wave Reader Page Turning

Status: accepted on hardware.

Validated path:

- Watch: Samsung Wear OS watch running Rustmix Remote
- Firmware target: Rustmix-Wave on Waveshare ESP32-S3 3.97-inch e-paper
- Transport: BLE GATT
- Protocol: Rustmix Remote BLE Protocol / RRBP

Accepted UI behavior:

- Swipeable two-screen UI works.
- Remote screen provides Previous, Next, Disconnect, and Device navigation.
- Device screen provides saved BLE address, Connect Saved, Scan / Fallback, and Remote navigation.
- Direct saved-address fallback works for the accepted Rustmix-Wave BLE MAC.
- Disconnect button is present and usable.
- Status text reports connection and write activity.

Accepted reader behavior:

- TXT reader Previous/Next page turning works.
- EPUB reader Previous/Next page turning works.
- RRBP GATT write path is unchanged from accepted r1.

Important constraints:

- Scan remains available, but the accepted reliable connection path is saved/direct BLE address fallback.
- Rustmix-Wave BLE feature build owns the modem; Wi-Fi is intentionally skipped in this feature build.
- Normal non-BLE firmware builds must preserve Wi-Fi behavior.
EOF

cat > docs/releases/v1.0.0.md <<'EOF'
# Rustmix Remote v1.0.0

Rustmix Remote v1.0.0 is the first hardware-validated Wear OS release for Rustmix-Wave page turning.

## Highlights

- Wear OS app for Rustmix reader devices.
- Swipeable Remote and Device screens.
- Saved BLE address support.
- Scan with saved-address fallback.
- Disconnect button.
- Previous and Next page controls.
- Status text for connection and write activity.
- BLE GATT RRBP command transport.

## Hardware validation

Validated with:

- Samsung Wear OS watch
- Waveshare ESP32-S3 3.97-inch e-paper device
- Rustmix-Wave BLE feature firmware

Accepted reader behavior:

- TXT previous/next page turning
- EPUB previous/next page turning

## Firmware requirements

Rustmix-Wave must be built with:

```
--features rustmix-remote-ble
```

The current accepted ESP-IDF build path also uses:

```
RUSTFLAGS="${RUSTFLAGS:-} --cfg esp_idf_version_least_5_5_0"
```

## Known limitation

BLE scan remains available, but the accepted reliable connection path for the validated Samsung Wear OS watch is direct saved-address fallback.

## Firmware boundary

The Rustmix-Wave BLE feature build owns the modem, so Wi-Fi is intentionally skipped in this build. Normal Rustmix-Wave builds must preserve Wi-Fi behavior.
EOF

cat > scripts/release_v1_0_0.sh <<'EOF'
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
EOF

chmod +x scripts/release_v1_0_0.sh

echo "Prepared v1.0.0 docs and release script."
