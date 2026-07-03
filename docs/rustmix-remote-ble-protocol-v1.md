# Rustmix Remote BLE Protocol v1

## Overview

Rustmix Remote BLE Protocol, abbreviated RRBP, is a compact BLE GATT protocol for controlling Rustmix e-paper firmware devices from a Wear OS smartwatch.

## Roles

| Device | BLE role |
|---|---|
| Wear OS smartwatch | Central / client |
| Rustmix e-paper device | Peripheral / GATT server |

## Service

Service name:

```text
Rustmix Remote
```

The final UUID should be generated once and then treated as stable.

Initial UUID namespace used by the Android skeleton:

```text
Service:       8f7a0000-6b8f-4a91-9e2c-8d2f5c000001
Command:       8f7a0001-6b8f-4a91-9e2c-8d2f5c000001
Status:        8f7a0002-6b8f-4a91-9e2c-8d2f5c000001
Capabilities:  8f7a0003-6b8f-4a91-9e2c-8d2f5c000001
```

## Characteristics

| Characteristic | Direction | Properties |
|---|---|---|
| Command | Watch to device | Write Without Response |
| Status | Device to watch | Read, Notify |
| Capabilities | Device to watch | Read |

## Command packet

```text
Byte 0: protocol version
Byte 1: sequence number
Byte 2: command
Byte 3: flags
Byte 4: parameter
Byte 5: reserved
```

## Protocol version

```text
0x01 = RRBP v1
```

## Commands

| Value | Command |
|---:|---|
| 0x01 | Page next |
| 0x02 | Page previous |
| 0x03 | Select |
| 0x04 | Back |
| 0x05 | Menu |
| 0x06 | Sleep |
| 0x07 | Wake / keep awake |
| 0x08 | Scroll up |
| 0x09 | Scroll down |
| 0x0A | Next chapter / next file |
| 0x0B | Previous chapter / previous file |
| 0x0C | Toggle bookmark |
| 0x0D | Refresh / ghost cleanup |

## Flags

| Value | Meaning |
|---:|---|
| 0x01 | Long press |
| 0x02 | Repeat |
| 0x04 | Rotary input |
| 0x08 | High priority |
| 0x10 | Require acknowledgement |

## MVP behavior

For the first firmware implementation, only these commands are required:

- 0x01 Page next
- 0x02 Page previous

All unsupported commands should be ignored safely.

## Sequence numbers

The watch increments Byte 1 for every command. The firmware may use this to ignore duplicate writes. Sequence number wraparound from 255 to 0 is allowed.

## Error behavior

The firmware should ignore:

- Unknown protocol versions
- Packets shorter than six bytes
- Unsupported commands
- Commands received while Remote Control is disabled in Settings
- Commands from non-allowed devices after pairing/allowlist support is added
