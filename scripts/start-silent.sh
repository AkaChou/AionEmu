#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

CLEAN_AION=false
while getopts ":c" opt; do
  case "$opt" in
    c)
      CLEAN_AION=true
      ;;
    *)
      echo "Usage: $0 [-c]"
      exit 1
      ;;
  esac
done
shift $((OPTIND - 1))
if [ "$#" -ne 0 ]; then
  echo "Usage: $0 [-c]"
  exit 1
fi

if [ -f "$SCRIPT_DIR/AionEmu.jar" ]; then
  DEFAULT_AION_HOME="$SCRIPT_DIR"
else
  DEFAULT_AION_HOME="$ROOT_DIR/aion"
fi
AION_HOME="${AION_HOME:-$DEFAULT_AION_HOME}"
JAR_FILE="${AION_JAR_FILE:-$AION_HOME/AionEmu.jar}"
LOG_DIR="${AION_LOG_DIR:-$AION_HOME/log}"
LOG_FILE="${AION_LOG_FILE:-$LOG_DIR/aionemu.log}"
PID_FILE="${AION_PID_FILE:-$LOG_DIR/aionemu.pid}"
RESOURCE_AION_DIR="$ROOT_DIR/src/main/resources/aion"
RESOURCE_LOGBACK="$ROOT_DIR/src/main/resources/logback-spring.xml"

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
  echo "Run ./package.sh first."
  exit 1
fi

if [ "$CLEAN_AION" = "true" ]; then
  find "$AION_HOME" -mindepth 1 -maxdepth 1 \
    ! -name AionEmu.jar ! -name start-silent.sh ! -name stop-silent.sh ! -name shutdown.sh \
    -exec rm -rf -- {} +
  echo "Cleaned startup directory: $AION_HOME"
fi

mkdir -p "$LOG_DIR"
if [ -d "$RESOURCE_AION_DIR" ]; then
  while IFS= read -r -d '' source_file; do
    target_file="$AION_HOME/${source_file#"$RESOURCE_AION_DIR/"}"
    if [ ! -e "$target_file" ]; then
      mkdir -p "$(dirname "$target_file")"
      cp "$source_file" "$target_file"
    fi
  done < <(find "$RESOURCE_AION_DIR" -type f -print0)
fi
if [ -f "$RESOURCE_LOGBACK" ] && [ ! -e "$LOG_DIR/logback-spring.xml" ]; then
  cp "$RESOURCE_LOGBACK" "$LOG_DIR/logback-spring.xml"
fi

AION_HEAP_OPTS="${AION_HEAP_OPTS:--Xms2g -Xmx8g}"
AION_GC_OPTS="${AION_GC_OPTS:--XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=30 -XX:+ParallelRefProcEnabled -XX:+UseStringDeduplication}"
AION_SAFETY_OPTS="${AION_SAFETY_OPTS:--XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$LOG_DIR -XX:+ExitOnOutOfMemoryError}"
AION_SYSTEM_OPTS="${AION_SYSTEM_OPTS:--Dfile.encoding=UTF-8 -Djava.net.preferIPv4Stack=true -Duser.timezone=Asia/Shanghai}"
AION_PATH_OPTS="-Daion.home=$AION_HOME -Daion.log.dir=$LOG_DIR"
AION_JVM_OPTS="${AION_JVM_OPTS:-$AION_HEAP_OPTS $AION_GC_OPTS $AION_SAFETY_OPTS $AION_PATH_OPTS $AION_SYSTEM_OPTS}"

nohup java $AION_JVM_OPTS -jar "$JAR_FILE" >>"$LOG_FILE" 2>&1 &
PID="$!"
echo "$PID" >"$PID_FILE"

echo "AionEmu started silently: pid=$PID"
echo "Log: $LOG_FILE"
echo "查看日志: tail -f \"$LOG_FILE\""
echo "Pid: $PID_FILE"
