#!/usr/bin/env bash
# 静态数据 XML 解析器探针运行器（只读：不启动服务端、不修改仓库内容）。
# Runner for the static-data XML parser probe (read-only: never starts the server, never writes repo content).
#
# 前置条件 / Precondition:
#   mvn -B -q dependency:build-classpath -Dmdep.outputFile=/tmp/aion-runtime-cp.txt -DincludeScope=runtime
#
# 用法 / Usage:
#   bash .agents/summary/startup-perf/xml-parser-probe/run-probe.sh
#   WARMUP=2 ITERATIONS=5 bash .../run-probe.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
PROBE_DIR="$ROOT/.agents/summary/startup-perf/xml-parser-probe"
JAVA="${JAVA:-/Users/mc/Library/Java/JavaVirtualMachines/azul-26.0.2.1/Contents/Home/bin/java}"
CP_FILE="${CP_FILE:-/tmp/aion-runtime-cp.txt}"
WARMUP="${WARMUP:-2}"
ITERATIONS="${ITERATIONS:-5}"

WSTX_JAR="$HOME/.m2/repository/com/fasterxml/woodstox/woodstox-core/7.1.1/woodstox-core-7.1.1.jar"
STAX2_JAR="$HOME/.m2/repository/org/codehaus/woodstox/stax2-api/4.2.2/stax2-api-4.2.2.jar"

if [[ ! -f "$CP_FILE" ]]; then
	echo "缺少运行时 classpath 文件 / missing runtime classpath file: $CP_FILE" >&2
	echo "请先执行 / run first:" >&2
	echo "  mvn -B -q dependency:build-classpath -Dmdep.outputFile=$CP_FILE -DincludeScope=runtime" >&2
	exit 1
fi
for jar in "$WSTX_JAR" "$STAX2_JAR"; do
	if [[ ! -f "$jar" ]]; then
		echo "缺少依赖 / missing dependency: $jar" >&2
		exit 1
	fi
done

# Woodstox 置于 classpath 前部，保证两个 Woodstox 版本中固定使用 7.1.1。
# Woodstox is placed at the front of the classpath so 7.1.1 is always the version in use.
CP="$ROOT/target/classes:$WSTX_JAR:$STAX2_JAR:$(cat "$CP_FILE")"
JVM_OPTS=(-Xms512m -Xmx3g -Dfile.encoding=UTF-8)

ITEM_SHARD="$ROOT/src/main/resources/aion/data/static_data/items/item/item_template_100000001_100601382.xml"
NPC_SHARD="$ROOT/src/main/resources/aion/data/static_data/npcs/npc_template_200000_216188.xml"
NPC_AI="$ROOT/src/main/resources/aion/definitions/compact/ai/npc-ai.xml"
SKILL_PART="$ROOT/src/main/resources/aion/definitions/compact/skills/skill_templates_part_010.xml"

REPORT="$PROBE_DIR/results-$(date +%Y%m%d-%H%M%S).txt"

run_case() {
	local variant="$1" scenario="$2" file="$3"
	local props=()
	if [[ "$variant" == "woodstox" ]]; then
		props=(-Djavax.xml.parsers.SAXParserFactory=com.ctc.wstx.sax.WstxSAXParserFactory
			-Djavax.xml.stream.XMLInputFactory=com.ctc.wstx.stax.WstxInputFactory)
	fi
	echo "### variant=$variant scenario=$scenario"
	"$JAVA" "${JVM_OPTS[@]}" ${props[@]+"${props[@]}"} -cp "$CP" \
		"$PROBE_DIR/XmlParserProbe.java" "$scenario" "$file" "$WARMUP" "$ITERATIONS"
}

{
	echo "== 静态数据解析器探针报告 / static-data parser probe report $(date '+%Y-%m-%d %H:%M:%S %Z') =="
	"$JAVA" -version
	echo "warmup=$WARMUP iterations=$ITERATIONS"
	for variant in jdk woodstox; do
		run_case "$variant" jaxb-item "$ITEM_SHARD" || echo "FAILED variant=$variant scenario=jaxb-item"
		run_case "$variant" jaxb-npc "$NPC_SHARD" || echo "FAILED variant=$variant scenario=jaxb-npc"
		run_case "$variant" stax-mappings "$NPC_AI" || echo "FAILED variant=$variant scenario=stax-mappings"
		run_case "$variant" sax-skill-part "$SKILL_PART" || echo "FAILED variant=$variant scenario=sax-skill-part"
	done
	echo "### same-JVM interleaved control"
	"$JAVA" "${JVM_OPTS[@]}" -cp "$CP" "$PROBE_DIR/XmlParserProbe.java" \
		jaxb-item-explicit "$ITEM_SHARD" "$WARMUP" "$ITERATIONS" \
		|| echo "FAILED scenario=jaxb-item-explicit"
} 2>&1 | tee "$REPORT"

echo "报告已写入 / report written to: $REPORT"
