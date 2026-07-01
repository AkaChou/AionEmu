#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

AION_HOME="${AION_HOME:-$ROOT_DIR/aion}"
LOG_DIR="${AION_LOG_DIR:-$AION_HOME/log}"
PID_FILE="${AION_PID_FILE:-$LOG_DIR/aionemu.pid}"
STOP_TIMEOUT="${AION_STOP_TIMEOUT:-30}"
FORCE_STOP="${AION_FORCE_STOP:-true}"

if [ ! -f "$PID_FILE" ]; then
  echo "AionEmu is not running: missing $PID_FILE"
  exit 0
fi

PID="$(cat "$PID_FILE")"
if [ -z "$PID" ] || ! kill -0 "$PID" 2>/dev/null; then
  echo "AionEmu is not running: stale pid=$PID"
  rm -f "$PID_FILE"
  exit 0
fi

echo "Stopping AionEmu: pid=$PID"
kill "$PID"

elapsed=0
while kill -0 "$PID" 2>/dev/null; do
  if [ "$elapsed" -ge "$STOP_TIMEOUT" ]; then
    if [ "$FORCE_STOP" = "true" ]; then
      echo "AionEmu did not stop within ${STOP_TIMEOUT}s; killing pid=$PID"
      kill -9 "$PID" 2>/dev/null || true
      break
    fi
    echo "AionEmu is still running after ${STOP_TIMEOUT}s: pid=$PID"
    exit 1
  fi
  sleep 1
  elapsed=$((elapsed + 1))
done

rm -f "$PID_FILE"
echo "AionEmu stopped: pid=$PID"
