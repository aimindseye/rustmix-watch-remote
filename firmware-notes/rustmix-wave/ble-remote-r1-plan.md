# Rustmix-Wave BLE Remote r1 Plan

## Goal

Add a compile-gated Rustmix Remote BLE service to Rustmix-Wave.

## Feature gate

```text
RUSTMIX_WAVE_ENABLE_BLE_REMOTE=1
```

## r1 behavior

- Advertise Rustmix Remote service.
- Expose Command characteristic.
- Parse PageNext and PagePrev commands.
- Enqueue RemoteEvent.
- Main app loop maps RemoteEvent to existing reader navigation.

## Safety rules

- No reader state mutation from BLE callback.
- No blocking work inside BLE callback.
- Unknown commands are ignored.
- Duplicate sequence numbers may be ignored.

## Suggested files

```text
src/ble_remote.rs
src/remote_event.rs
```

## Suggested events

```rust
pub enum RemoteEvent {
    PageNext,
    PagePrev,
    Select,
    Back,
    Menu,
    Sleep,
    Wake,
    ScrollUp,
    ScrollDown,
    NextChapter,
    PrevChapter,
    ToggleBookmark,
    Refresh,
}
```
