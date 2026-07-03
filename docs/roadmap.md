# Rustmix Remote Roadmap

## Phase 0: Repo foundation

- README
- Architecture document
- BLE protocol document
- Firmware integration notes
- Android/Wear OS app skeleton

## Phase 1: Wear OS app skeleton

- Dependency-light Android app
- Main remote screen
- Device scan/connect button
- BLE connection manager skeleton
- Page next / previous commands
- Haptic feedback hook

## Phase 2: Rustmix-Wave MVP

- Firmware BLE service behind feature gate
- Command characteristic
- Page next / previous only
- Watch app connects and sends commands

## Phase 3: Rustmix-Wave full navigation

- Back
- Menu
- Select
- Scroll
- Status notify
- Battery/status display

## Phase 4: X4 MVP

- Separate compile-gated BLE service
- No adapter-bind path
- No generic HID
- Page next / previous only

## Phase 5: X4 full navigation

- Back
- Menu
- Select
- Sleep-safe behavior
