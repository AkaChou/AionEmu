#!/usr/bin/env bash
# JSONL 存储格式探针运行器（只读：不启动服务端、不修改仓库内容）。
# Runner for the JSONL storage-format probe (read-only).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PROBE_DIR="$ROOT/.agents/summary/item-format-migration"
JAVA="${JAVA:-/Users/mc/Library/Java/JavaVirtualMachines/azul-26.0.2.1/Contents/Home/bin/java}"
CP_FILE="${CP_FILE:-/tmp/aion-runtime-cp.txt}"
WARMUP="${WARMUP:-2}"
ITERATIONS="${ITERATIONS:-5}"
SHARD="${SHARD:-$ROOT/src/main/resources/aion/data/static_data/items/item/item_template_100000001_100601382.xml}"
JSONL="${JSONL:-/tmp/item-shard-1.compact.jsonl}"
CLASSPATH="$ROOT/target/classes:$(cat "$CP_FILE"):/tmp/probe-classes"
JVM_OPTS=(-Xms512m -Xmx3g -Dfile.encoding=UTF-8)

REPORT="$PROBE_DIR/results-$(date +%Y%m%d-%H%M%S).txt"
{
	echo "== 物品模板存储格式探针报告 / item-template storage format probe $(date '+%Y-%m-%d %H:%M:%S %Z') =="
	"$JAVA" -version
	echo "shard=$SHARD"
	echo "jsonl=$JSONL"
	echo "warmup=$WARMUP iterations=$ITERATIONS"
	echo
	for scenario in jaxb-item sax-scan-item jsonl-parse jsonl-item; do
		target="$SHARD"
		case "$scenario" in jsonl-*) target="$JSONL" ;; esac
		echo "### scenario=$scenario"
		"$JAVA" "${JVM_OPTS[@]}" -cp "$CLASSPATH" JsonlItemProbe "$scenario" "$target" "$WARMUP" "$ITERATIONS" \
			|| echo "FAILED scenario=$scenario"
		echo
	done
} 2>&1 | tee "$REPORT"
echo "报告已写入 / report written to: $REPORT"
