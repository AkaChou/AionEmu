#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

AION_HOME="${AION_HOME:-$ROOT_DIR/aion}"
RESOURCE_AION_DIR="$ROOT_DIR/src/main/resources/aion"
RESOURCE_LOGBACK="$ROOT_DIR/src/main/resources/logback-spring.xml"

if [ ! -d "$RESOURCE_AION_DIR" ]; then
  echo "Missing $RESOURCE_AION_DIR"
  exit 1
fi

while IFS= read -r -d '' source_file; do
  rel_path="${source_file#"$RESOURCE_AION_DIR/"}"
  target_file="$AION_HOME/$rel_path"
  mkdir -p "$(dirname "$target_file")"
  case "$rel_path" in
    */config/*|config/*)
      if [ ! -e "$target_file" ]; then
        cp "$source_file" "$target_file"
      fi
      ;;
    *)
      cp -f "$source_file" "$target_file"
      ;;
  esac
done < <(find "$RESOURCE_AION_DIR" -type f -print0)

if [ -f "$RESOURCE_LOGBACK" ]; then
  mkdir -p "$AION_HOME/log"
  if [ ! -e "$AION_HOME/log/logback-spring.xml" ]; then
    cp "$RESOURCE_LOGBACK" "$AION_HOME/log/logback-spring.xml"
  fi
fi

echo "Refreshed $AION_HOME; existing config files were kept."
