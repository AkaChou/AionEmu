#!/usr/bin/env bash
# run_quest_gates.sh - Tiered quest regression runner / 分档任务回归执行器
#
# Tiers / 分档：
#   T1  fixed global gates only, seconds-scale, run after every edit
#       仅固定全局门禁，秒级，每次改完就跑
#   T2  T1 + every test class that mentions the given quest ids
#       T1 + 命中给定任务 ID 的全部测试类
#   T3  the whole questEngine test tree, the only "zero new failures" evidence
#       整个 questEngine 测试树，是"零新增失败"的唯一证据来源
#
# Usage / 用法：
#   .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T1
#   .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T2 2585 1136 2237 14150
#   .agents/summary/scriptdll-quest-driver/run_quest_gates.sh T3
#
# Environment / 环境变量：
#   QUEST_FORK_COUNT  Surefire fork count, default 1; 2 is the safest speed-up
#                     Surefire 分叉进程数，默认 1；设为 2 是风险最低的加速手段
#   QUEST_LOG_DIR     Log directory, default <tool dir>/gates
#                     日志目录，默认 <工具目录>/gates
#
# Exit codes / 退出码：
#   0  tier passed / 该档通过
#   1  maven / surefire failed / maven 或 surefire 失败
#   2  invalid arguments / 参数非法
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
SELECTOR_TOOL="${SCRIPT_DIR}/affected_quest_tests.py"
FORK_COUNT="${QUEST_FORK_COUNT:-1}"
LOG_DIR="${QUEST_LOG_DIR:-${SCRIPT_DIR}/gates}"

if [[ $# -lt 1 ]]; then
	echo "usage: run_quest_gates.sh {T1|T2|T3} [quest ids...] / 用法: run_quest_gates.sh {T1|T2|T3} [任务 ID...]" >&2
	exit 2
fi

TIER="$1"
shift
QUEST_IDS=("$@")
mkdir -p "${LOG_DIR}"
STAMP="$(date +%H%M%S)"
LOG_FILE="${LOG_DIR}/${TIER}-${STAMP}.log"

# The Python selector tool owns the canonical T1 list; never duplicate it here.
# T1 清单的唯一来源是 Python 选择器工具，此处不复制。
resolve_t1_selector() {
	python3 -B - "${SELECTOR_TOOL}" <<'PY'
import importlib.util
import sys

spec = importlib.util.spec_from_file_location("affected_quest_tests", sys.argv[1])
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
print(",".join(module.T1_GATE_CLASSES))
PY
}

case "${TIER}" in
T1)
	SELECTOR="-Dtest=$(resolve_t1_selector)"
	;;
T2)
	if [[ ${#QUEST_IDS[@]} -eq 0 ]]; then
		echo "T2 requires at least one quest id / T2 至少需要一个任务 ID" >&2
		exit 2
	fi
	SELECTOR="$(
		python3 -B "${SELECTOR_TOOL}" --json "${QUEST_IDS[@]}" |
			python3 -c 'import json, sys; print(json.load(sys.stdin)["selectors"]["combined"])'
	)"
	;;
T3)
	SELECTOR="-Dtest=com.aionemu.gameserver.questEngine.**"
	;;
*)
	echo "unknown tier: ${TIER} / 未知分档: ${TIER}" >&2
	exit 2
	;;
esac

echo "[${TIER}] selector / 选择器: ${SELECTOR}"
echo "[${TIER}] forkCount=${FORK_COUNT} log=${LOG_FILE}"

START_SECONDS=${SECONDS}
cd "${REPO_ROOT}"
# One maven invocation compiles tests and runs them; no separate test-compile pass.
# 单次 maven 调用即包含测试编译与执行，不再单独跑 test-compile。
set +e
echo SKIP-MVN -o -B test "${SELECTOR}" -DfailIfNoTests=false -DforkCount="${FORK_COUNT}" >"${LOG_FILE}" 2>&1
STATUS=$?
set -e
ELAPSED=$((SECONDS - START_SECONDS))

grep -E "Tests run:|BUILD (SUCCESS|FAILURE)" "${LOG_FILE}" | tail -20 || true
echo "[${TIER}] exit=${STATUS} elapsed=${ELAPSED}s log=${LOG_FILE}"
exit "${STATUS}"
