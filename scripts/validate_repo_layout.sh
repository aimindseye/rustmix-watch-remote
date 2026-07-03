#!/usr/bin/env bash
set -euo pipefail

required=(
  README.md
  docs/architecture.md
  docs/rustmix-remote-ble-protocol-v1.md
  docs/roadmap.md
  docs/device-split.md
  firmware-notes/rustmix-wave/ble-remote-r1-plan.md
  firmware-notes/rustmix-x4-firmware/ble-remote-r1-plan.md
)

for f in "${required[@]}"; do
  if [[ ! -f "$f" ]]; then
    echo "missing: $f" >&2
    exit 1
  fi
done

echo "Rustmix Remote repo layout: OK"
