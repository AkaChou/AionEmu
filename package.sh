#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

export MAVEN_OPTS="${MAVEN_OPTS:--Xms512m -Xmx2g -Dfile.encoding=UTF-8}"

if [ "$#" -eq 0 ]; then
  set -- -DskipTests package
fi

mvn clean "$@"

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
