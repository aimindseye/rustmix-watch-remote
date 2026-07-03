# Rustmix Remote Architecture

## Product

Rustmix Remote is a Wear OS smartwatch remote for Rustmix e-paper firmware devices.

It is intentionally separate from the Vaachak platform. Rustmix Remote is optimized for custom firmware targets where we control both sides of the connection.

## Targets

### Phase 1

- Rustmix-Wave / Waveshare ESP32-S3 3.97-inch e-paper

### Phase 2

- rustmix-x4-firmware / Xteink X4

## Primary protocol

Rustmix Remote uses a custom BLE GATT protocol.

The watch acts as BLE central/client.

The e-paper firmware acts as BLE peripheral/server and advertises the Rustmix Remote service.

## Data flow

```text
Watch UI gesture
    ↓
Rustmix Remote command
    ↓
BLE GATT write
    ↓
Firmware BLE command parser
    ↓
RemoteEvent queue
    ↓
Main firmware event loop
    ↓
Existing reader/UI action
```

## Firmware safety rule

BLE callbacks must not directly mutate reader or UI state.

BLE callbacks may only:

1. Validate the packet.
2. Convert it to a RemoteEvent.
3. Push the event onto a queue.

The main firmware loop consumes the event and routes it through the existing input/navigation path.

## MVP commands

- Page next
- Page previous

## Later commands

- Back
- Menu
- Select
- Scroll up
- Scroll down
- Sleep
- Wake / keep awake
- Toggle bookmark
- Refresh / ghost cleanup

## Rustmix-Wave first

Rustmix-Wave should be the first firmware target because the ESP32-S3 has more headroom and the firmware already has accepted Reader, Library, Settings, sleep, refresh, and navigation behavior.

## X4 second

X4 support should be more conservative because earlier BLE adapter-bind work was intentionally quarantined. X4 should use a separate, minimal BLE remote receiver and must not reuse the adapter-bind path.

## Non-goals

- Bluetooth HID keyboard emulation
- Boox support
- Android phone support
- Vaachak platform integration
- Generic remote control for third-party e-readers
