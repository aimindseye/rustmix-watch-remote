# Rustmix Remote

Rustmix Remote is a Wear OS smartwatch app for controlling Rustmix e-paper firmware devices.

The first targets are:

- Rustmix-Wave on the Waveshare ESP32-S3 3.97-inch e-paper device
- rustmix-x4-firmware on the Xteink X4

Rustmix Remote intentionally stays separate from the Vaachak platform. It is firmware-native and optimized for devices where the Rustmix firmware controls both sides of the input model.

## Protocol direction

Rustmix Remote uses a custom BLE GATT protocol instead of generic Bluetooth HID.

```text
Wear OS watch
    ↓ BLE GATT command write
Rustmix firmware BLE remote service
    ↓
RemoteEvent queue
    ↓
Existing reader/navigation event handling
```

## Goals

- Page next / previous from a smartwatch
- Back, menu, select, and scroll navigation
- Reader-friendly haptic feedback
- Low-power BLE command path
- Rustmix-Wave first, X4 second
- No Vaachak branding or Vaachak platform dependencies

## Non-goals

- Boox support
- Android phone/tablet support
- Kobo support
- Generic HID keyboard mode
- Cloud sync
- Phone companion app

Those belong in a separate Vaachak-focused app.

## Status

Initial architecture and app skeleton.
