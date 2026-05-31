#!/usr/bin/env bash
# Pull Beacon debug NDJSON from a connected device into the workspace log file.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/.cursor/debug-b98d12.log"
PKG="com.beacon.app"
REMOTE="files/debug-b98d12.log"

adb exec-out run-as "$PKG" cat "$REMOTE" > "$OUT" 2>/dev/null || {
  echo "Could not read via run-as; trying logcat snapshot..."
  adb logcat -d -s BeaconDebug:I | sed 's/^.*BeaconDebug: //' > "$OUT" || true
}

if [[ -s "$OUT" ]]; then
  echo "Wrote $(wc -l < "$OUT" | tr -d ' ') lines to $OUT"
else
  echo "No debug log yet. Reproduce in the app, then run this script again."
fi
