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
