#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

JAR_FILE="${AION_JAR_FILE:-$ROOT_DIR/target/AionEmu.jar}"
LOG_DIR="${AION_LOG_DIR:-$ROOT_DIR/logs}"
LOG_FILE="${AION_LOG_FILE:-$LOG_DIR/aionemu.log}"
PID_FILE="${AION_PID_FILE:-$LOG_DIR/aionemu.pid}"

mkdir -p "$LOG_DIR"

if [ -f "$PID_FILE" ]; then
  OLD_PID="$(cat "$PID_FILE")"
  if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
    echo "AionEmu is already running: pid=$OLD_PID"
    exit 0
  fi
  rm -f "$PID_FILE"
fi

if [ ! -f "$JAR_FILE" ]; then
  echo "Missing $JAR_FILE"
  echo "Run ./maven-package.sh first."
  exit 1
fi

AION_HEAP_OPTS="${AION_HEAP_OPTS:--Xms2g -Xmx8g}"
AION_GC_OPTS="${AION_GC_OPTS:--XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=30 -XX:+ParallelRefProcEnabled -XX:+UseStringDeduplication}"
AION_SAFETY_OPTS="${AION_SAFETY_OPTS:--XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$LOG_DIR -XX:+ExitOnOutOfMemoryError}"
AION_SYSTEM_OPTS="${AION_SYSTEM_OPTS:--Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Duser.timezone=Asia/Shanghai}"
AION_JVM_OPTS="${AION_JVM_OPTS:-$AION_HEAP_OPTS $AION_GC_OPTS $AION_SAFETY_OPTS $AION_SYSTEM_OPTS}"

nohup java $AION_JVM_OPTS -jar "$JAR_FILE" >>"$LOG_FILE" 2>&1 &
PID="$!"
echo "$PID" >"$PID_FILE"

echo "AionEmu started silently: pid=$PID"
echo "Log: $LOG_FILE"
echo "Pid: $PID_FILE"
