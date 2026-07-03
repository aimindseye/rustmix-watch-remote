# rustmix-x4-firmware BLE Remote r1 Plan

## Goal

Add a minimal, compile-gated Rustmix Remote BLE receiver to X4.

## Feature gates

```text
R10_BLE_X4_ENABLE_ADAPTER_BIND=0
X4_ENABLE_PAGE_REMOTE_BLE=1
```

## r1 behavior

- Compile-gated only.
- No generic HID.
- No BLE scanning.
- No adapter bind.
- No reconnect storm.
- No reader state mutation from BLE callback.

## First executable behavior

Only support:

- Page next
- Page previous

Both commands should enqueue RemoteEvent and be consumed by the main app loop.

## Safety rules

- Keep the existing accepted X4 BLE adapter-bind quarantine intact.
- Do not route through the previous R10 adapter-bind path.
- Do not scan for external remotes.
- Do not parse generic HID.
- Do not modify reader state inside a BLE callback.
