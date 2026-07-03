# Rustmix Remote vs Vaachak Remote Split

## Rustmix Remote

Rustmix Remote is for Rustmix firmware devices.

Repository:

```text
rustmix-watch-remote
```

App name:

```text
Rustmix Remote
```

Scope:

- Rustmix-Wave
- rustmix-x4-firmware
- Custom BLE GATT
- Firmware-native event injection

Non-scope:

- Boox
- Android phones/tablets
- Kobo
- Generic Bluetooth HID
- Vaachak platform branding

## Future Vaachak app

A future Vaachak app should be a separate repo and should focus on Android/Boox capabilities.

Possible scope:

- Boox readers
- Android phones/tablets
- HID keyboard/remote behavior
- Accessibility-service-based navigation if appropriate
- Android intent/media/key routing where useful

This separation lets Rustmix Remote remain minimal, native, and firmware-safe while allowing the Vaachak app to use the broader Android ecosystem.
