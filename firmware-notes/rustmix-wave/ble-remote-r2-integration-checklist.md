# Rustmix-Wave BLE Remote r2 Integration Checklist

Use this checklist after applying the Rustmix-Wave BLE Remote r1 scaffold to the Rustmix-Wave repo.

## Compile gate

Recommended gate:

```text
RUSTMIX_WAVE_ENABLE_BLE_REMOTE=1
```

## Required behavior

- Device advertises Rustmix Remote service only when enabled.
- Command characteristic accepts 6-byte RRBP v1 packets.
- `0x01` maps to next page.
- `0x02` maps to previous page.
- Unsupported commands are ignored safely.
- Duplicate sequence numbers are ignored safely.
- BLE callback never mutates reader state directly.

## Physical test matrix

| Screen | Next | Previous | Expected |
|---|---|---|---|
| Reader TXT | yes | yes | page changes once |
| Reader EPUB | yes | yes | page changes once |
| Library | optional | optional | no crash if not routed yet |
| Settings | optional | optional | no crash if not routed yet |
| Sleep image | no | no | ignored unless wake support is explicit |

## Rejection criteria

Reject the firmware change if any of these happen:

- Boot loop.
- BLE reconnect storm.
- Page turns repeat without input.
- Existing physical buttons stop working.
- Reader state corruption.
- Sleep/wake regression.
- Wi-Fi transfer regression.
