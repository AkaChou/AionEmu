#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

export MAVEN_OPTS="${MAVEN_OPTS:--Xms512m -Xmx2g -Dfile.encoding=UTF-8}"
MAVEN_THREADS="${MAVEN_THREADS:-1C}"

if [ "$#" -eq 0 ]; then
  set -- -DskipTests -Daion.external-resources=true package
fi

echo "Maven build threads: $MAVEN_THREADS"
mvn -T "$MAVEN_THREADS" clean "$@"

ARTIFACT="$ROOT_DIR/target/AionEmu.jar"
RESOURCE_AION_DIR="$ROOT_DIR/src/main/resources/aion"
RESOURCE_LOGBACK="$ROOT_DIR/src/main/resources/logback-spring.xml"
AION_HOME="${AION_HOME:-$ROOT_DIR/aion}"
AION_PRESERVE_CONFIG="${AION_PRESERVE_CONFIG:-false}"

if [ ! -f "$ARTIFACT" ]; then
  echo "Missing $ARTIFACT"
  exit 1
fi
if [ ! -d "$RESOURCE_AION_DIR/geo" ]; then
  echo "Missing $RESOURCE_AION_DIR/geo"
  exit 1
fi
if ! find "$RESOURCE_AION_DIR/geo/path" -name '*.path.gz' -print -quit 2>/dev/null | grep -q .; then
  echo "Missing compressed PATH data in $RESOURCE_AION_DIR/geo/path"
  exit 1
fi

echo "Syncing runtime resources..."
mkdir -p "$AION_HOME"
if command -v rsync >/dev/null 2>&1; then
  if [ "$AION_PRESERVE_CONFIG" = "true" ]; then
    # 保持运行时配置不变；下面的循环只补齐缺失文件。
    # Keep runtime configuration untouched; the loop below fills only missing files.
    rsync -a --exclude='config/***' "$RESOURCE_AION_DIR/" "$AION_HOME/"
  else
    rsync -a "$RESOURCE_AION_DIR/" "$AION_HOME/"
  fi
else
  while IFS= read -r -d '' source_file; do
    rel_path="${source_file#"$RESOURCE_AION_DIR/"}"
    target_file="$AION_HOME/$rel_path"
    mkdir -p "$(dirname "$target_file")"
    case "$rel_path" in
      */config/*|config/*)
        if [ "$AION_PRESERVE_CONFIG" != "true" ] || [ ! -e "$target_file" ]; then
          cp -f "$source_file" "$target_file"
        fi
        ;;
      *)
        cp -f "$source_file" "$target_file"
        ;;
    esac
  done < <(find "$RESOURCE_AION_DIR" -type f -print0)
fi

if [ "$AION_PRESERVE_CONFIG" = "true" ]; then
  while IFS= read -r -d '' source_file; do
    rel_path="${source_file#"$RESOURCE_AION_DIR/"}"
    case "$rel_path" in
      */config/*|config/*)
        target_file="$AION_HOME/$rel_path"
        if [ ! -e "$target_file" ]; then
          mkdir -p "$(dirname "$target_file")"
          cp -p "$source_file" "$target_file"
        fi
        ;;
    esac
  done < <(find "$RESOURCE_AION_DIR" -type f -print0)
fi
echo "Runtime resources synchronized."

if [ -f "$RESOURCE_LOGBACK" ]; then
  mkdir -p "$AION_HOME/log"
  if [ "$AION_PRESERVE_CONFIG" != "true" ] || [ ! -e "$AION_HOME/log/logback-spring.xml" ]; then
    cp -f "$RESOURCE_LOGBACK" "$AION_HOME/log/logback-spring.xml"
  fi
fi

cp -f "$ARTIFACT" "$AION_HOME/AionEmu.jar"
cp -f "$ROOT_DIR/scripts/start-silent.sh" "$AION_HOME/start-silent.sh"
cp -f "$ROOT_DIR/scripts/stop-silent.sh" "$AION_HOME/stop-silent.sh"
cp -f "$ROOT_DIR/scripts/shutdown.sh" "$AION_HOME/shutdown.sh"
chmod +x "$AION_HOME/start-silent.sh" "$AION_HOME/stop-silent.sh" "$AION_HOME/shutdown.sh"

echo "Packaged: $AION_HOME/AionEmu.jar"
if [ "$AION_HOME" = "$ROOT_DIR/aion" ]; then
  AION_HOME="./aion"
fi
echo "Start: $AION_HOME/start-silent.sh"
echo "Shutdown: $AION_HOME/shutdown.sh"
echo "Stop:  $AION_HOME/stop-silent.sh"
