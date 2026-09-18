#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AION_PRESERVE_CONFIG=true bash "$SCRIPT_DIR/package.sh" "$@"
