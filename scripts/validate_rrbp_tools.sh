#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/tools/rrbp-cli"

NEXT="$(go run . encode --seq 0 next)"
PREV="$(go run . encode --seq 1 prev)"
MENU="$(go run . encode --seq 4 menu)"
LONG_NEXT="$(go run . encode --seq 5 --flags 1 next)"

[ "$NEXT" = "01 00 01 00 00 00" ] || { echo "next vector mismatch: $NEXT"; exit 1; }
[ "$PREV" = "01 01 02 00 00 00" ] || { echo "prev vector mismatch: $PREV"; exit 1; }
[ "$MENU" = "01 04 05 00 00 00" ] || { echo "menu vector mismatch: $MENU"; exit 1; }
[ "$LONG_NEXT" = "01 05 01 01 00 00" ] || { echo "long next vector mismatch: $LONG_NEXT"; exit 1; }

go run . parse "01 00 01 00 00 00" >/tmp/rrbp_parse_next.txt
grep -q "command=Page next" /tmp/rrbp_parse_next.txt

echo "RRBP tools validation: OK"
