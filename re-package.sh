#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AION_PRESERVE_CONFIG=true bash "$ROOT_DIR/package.sh" "$@"
