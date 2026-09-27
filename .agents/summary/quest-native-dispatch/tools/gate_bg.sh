#!/usr/bin/env bash
# 后台不阻塞地跑长命令（门禁/T3/mvn）：nohup + disown + 哨兵 + 超时看门狗。
# Run a long command detached: nohup + disown + completion sentinel + watchdog.
#
# 用法 / Usage:
#   tools/gate_bg.sh <日志名> <超时秒> -- <命令...>
# 例 / e.g.:
#   tools/gate_bg.sh T3-w7 my3  1500 -- bash -lc 'cd /private/tmp/aion-t3-warm && mvn -o -B test ...'
#
# 完成后：日志尾部出现 `[DONE] exit=…`；执行体只需一次
#   tail -3 .agents/summary/quest-native-dispatch/gates/<日志名>.log
# 判定，**不要 sleep 轮询、不要等进程退出事件**。
set -euo pipefail

NAME="${1:?日志名必填 / log name required}"
LIMIT="${2:?超时秒必填 / limit seconds required}"
shift 2
[ "${1:-}" = "--" ] && shift

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
LOG="${ROOT}/.agents/summary/quest-native-dispatch/gates/${NAME}.log"
mkdir -p "$(dirname "${LOG}")"

nohup python3 "${ROOT}/.agents/summary/quest-native-dispatch/tools/gate_run.py" \
  "${LIMIT}" "${LOG}" "$@" >/dev/null 2>&1 </dev/null &
disown

echo "started log=${LOG} limit=${LIMIT}s pid=$!"
