#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -f "$SCRIPT_DIR/AionEmu.jar" ]; then
  DEFAULT_AION_HOME="$SCRIPT_DIR"
else
  DEFAULT_AION_HOME="$ROOT_DIR/aion"
fi
AION_HOME="${AION_HOME:-$DEFAULT_AION_HOME}"
LOG_DIR="${AION_LOG_DIR:-$AION_HOME/log}"
PID_FILE="${AION_PID_FILE:-$LOG_DIR/aionemu.pid}"
SHUTDOWN_TIMEOUT="${AION_SHUTDOWN_TIMEOUT:-120}"

if ! [[ "$SHUTDOWN_TIMEOUT" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid AION_SHUTDOWN_TIMEOUT: $SHUTDOWN_TIMEOUT" >&2
  exit 2
fi

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

echo "Requesting graceful AionEmu shutdown: pid=$PID"
kill "$PID"
DEADLINE=$((SECONDS + SHUTDOWN_TIMEOUT))
while kill -0 "$PID" 2>/dev/null; do
  if (( SECONDS >= DEADLINE )); then
    echo "Timed out waiting ${SHUTDOWN_TIMEOUT}s for AionEmu to stop: pid=$PID" >&2
    exit 1
  fi
  sleep 1
done

rm -f "$PID_FILE"
echo "AionEmu shut down gracefully: pid=$PID"
