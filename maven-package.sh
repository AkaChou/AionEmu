#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

export MAVEN_OPTS="${MAVEN_OPTS:--Xms512m -Xmx2g -Dfile.encoding=UTF-8}"

if [ "$#" -eq 0 ]; then
  set -- -DskipTests package
fi

mvn "$@"

ARTIFACT="$ROOT_DIR/target/AionEmu.jar"
if [ -f "$ARTIFACT" ]; then
  echo "Packaged: $ARTIFACT"
fi
