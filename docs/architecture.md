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
