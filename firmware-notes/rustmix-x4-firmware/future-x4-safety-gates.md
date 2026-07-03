# Future X4 Safety Gates

X4 support should start only after Rustmix-Wave page turning is physically accepted.

Required X4 feature gates:

```text
R10_BLE_X4_ENABLE_ADAPTER_BIND=0
X4_ENABLE_PAGE_REMOTE_BLE=1
```

Rules:

- Do not reuse the earlier adapter-bind path.
- Do not scan for remotes.
- Do not implement HID first.
- Do not mutate reader state in BLE callbacks.
- Start with page next / previous only.
- Keep all remote logic compile-gated and default-off until stable.
