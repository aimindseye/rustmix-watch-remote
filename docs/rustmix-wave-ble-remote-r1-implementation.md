# Rustmix-Wave BLE Remote r1 Implementation

## Goal

Add Rustmix Remote BLE receiver support to Rustmix-Wave without disturbing the accepted reader, calendar, voice notes, sleep, Wi-Fi transfer, and Indic EPUB baseline.

## Scope for r1

r1 is intentionally small:

- Advertise the Rustmix Remote BLE service.
- Expose the command characteristic.
- Accept RRBP v1 command writes.
- Convert `PageNext` and `PagePrevious` into `RemoteEvent` values.
- Enqueue events for the main firmware loop.
- Main loop routes events through the same page-turn path used by existing buttons.

## Non-goals for r1

- No HID support.
- No phone companion.
- No generic remote support.
- No direct reader-state mutation inside BLE callbacks.
- No sleep/wake command execution until page turns are physically accepted.

## Suggested firmware module layout

```text
firmware/assistant-rs/src/rustmix_remote/
  mod.rs
  rrbp.rs
  integration_stub.rs
```

## Safety contract

BLE callback may only:

1. Validate packet length and protocol version.
2. Parse command.
3. Drop duplicates.
4. Push a `RemoteEvent` into a queue.

The UI/main loop owns reader state mutation.

```text
BLE write callback
    ↓
RRBP parse
    ↓
RemoteEvent enqueue
    ↓
main firmware loop drains queue
    ↓
existing input/navigation handler
```

## First accepted physical test

1. Flash Rustmix-Wave with BLE remote enabled.
2. Open a known EPUB or TXT book.
3. Connect Rustmix Remote from watch.
4. Tap `Next Page`.
5. Confirm page advances once.
6. Tap `Previous`.
7. Confirm page returns once.
8. Leave device idle for 2 minutes.
9. Confirm no reconnect storm and no unexpected page turns.
