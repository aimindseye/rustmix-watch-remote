# RRBP v1 Test Vectors

RRBP is the Rustmix Remote BLE Protocol used by the Wear OS app and Rustmix firmware.

Packet format:

```text
Byte 0: protocol version = 0x01
Byte 1: sequence number
Byte 2: command
Byte 3: flags
Byte 4: parameter
Byte 5: reserved
```

## MVP vectors

| Action | Sequence | Bytes |
|---|---:|---|
| Page next | 0 | `01 00 01 00 00 00` |
| Page previous | 1 | `01 01 02 00 00 00` |
| Select | 2 | `01 02 03 00 00 00` |
| Back | 3 | `01 03 04 00 00 00` |
| Menu | 4 | `01 04 05 00 00 00` |

## Long press vector

| Action | Sequence | Flags | Bytes |
|---|---:|---:|---|
| Long press next | 5 | `0x01` | `01 05 01 01 00 00` |

## Rotary vector

| Action | Sequence | Flags | Bytes |
|---|---:|---:|---|
| Rotary scroll down | 6 | `0x04` | `01 06 09 04 00 00` |

## Firmware parsing rule

Unsupported commands should be ignored safely.
Duplicate sequence values should be ignored safely after the first accepted command.
Invalid protocol versions should be rejected.
Short packets should be rejected.
