#!/usr/bin/env bash
# Wrapper for scripts/MigrateToAionEmu.java: merges al_server_gs + al_server_ls into aion_emu.
# The Java tool is run in source-file mode; its only external dependency is the MySQL driver
# jar taken from the local Maven repository (override with AION_MYSQL_DRIVER).
#
# Usage: scripts/migrate-to-aion-emu.sh
# Env overrides: MYSQL_HOST MYSQL_PORT MYSQL_USER MYSQL_PASSWORD
#                SRC_GS_DB SRC_LS_DB DST_DB V1_SQL AION_MYSQL_DRIVER
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

DRIVER_JAR="${AION_MYSQL_DRIVER:-}"
if [ -z "$DRIVER_JAR" ]; then
  DRIVER_JAR="$(find "$HOME/.m2/repository/com/mysql/mysql-connector-j" \
    -name 'mysql-connector-j-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' 2>/dev/null \
    | sort -V | tail -1)"
fi
if [ ! -f "$DRIVER_JAR" ]; then
  echo "MySQL driver jar not found; set AION_MYSQL_DRIVER to a mysql-connector-j jar path."
  exit 1
fi

export V1_SQL="${V1_SQL:-$ROOT_DIR/src/main/resources/db/migration/V1__init.sql}"
echo "Using MySQL driver: $DRIVER_JAR"
exec java -cp "$DRIVER_JAR" "$SCRIPT_DIR/MigrateToAionEmu.java"
