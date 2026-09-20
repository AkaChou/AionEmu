#!/usr/bin/env bash
# jar + AOT 启动测量运行器（P2 实验）：启动 → 等"服务器已就绪" → SIGINT 优雅停止。
# jar + AOT startup measurement runner (P2 experiment): start → wait for readiness → graceful SIGINT.
#
# 用法 / Usage:
#   bash .agents/summary/startup-perf/aot-jar-run.sh <tag> [额外 JVM 参数 / extra JVM options...]
#   AOT_NO_JFR=1 bash ... <tag> ...   # 关闭 JFR（record/create 步骤必须关闭，见下）
#
# 产出 / Outputs: /tmp/aot-<tag>.log, /tmp/aot-<tag>.jfr（未用 JFR 时为 /dev/null）
#
# 说明 / Notes:
#   1) 用 -cp target/AionEmu.jar <JarLauncher> 启动而不是 -jar：AOT 记录的 classpath 条目必须与
#      create/use 步骤完全一致（-jar 形态被记成只有文件名的 "AionEmu.jar"，create 步骤会找不到）。
#      Launches via -cp instead of -jar so the AOT-recorded classpath entry stays identical across steps.
#   2) AOTMode=record 与 JFR 同时开启会让 Zulu 26 在 JfrTypeSet::serialize 崩溃（实测 SIGSEGV），
#      因此 record/create 步骤必须用 AOT_NO_JFR=1 关闭录制。AOTMode=record with JFR crashes Zulu 26
#      inside JfrTypeSet::serialize (observed SIGSEGV), so record/create disable the recording.
set -uo pipefail

TAG="$1"
shift
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
JAVA="${JAVA:-/Users/mc/Library/Java/JavaVirtualMachines/azul-26.0.2.1/Contents/Home/bin/java}"
LOG="/tmp/aot-${TAG}.log"
JFR="/tmp/aot-${TAG}.jfr"
RECORDER_OPTS=()
if [[ -n "${AOT_NO_JFR:-}" ]]; then
	RECORDER_OPTS=(-XX:-FlightRecorder)
	JFR="/dev/null"
else
	RECORDER_OPTS=(-XX:StartFlightRecording=name="AOT-${TAG}",settings=profile,filename="$JFR",duration=60s,dumponexit=true)
fi

cd "$ROOT"
echo "### tag=$TAG extra=[$*] recorder=[${RECORDER_OPTS[*]}] log=$LOG"
"$JAVA" "$@" "${RECORDER_OPTS[@]}" \
	-cp target/AionEmu.jar org.springframework.boot.loader.launch.JarLauncher > "$LOG" 2>&1 &
PID=$!
READY=0
for _ in $(seq 1 150); do
	if grep -qE "服务器已就绪|启动成功" "$LOG" 2>/dev/null; then
		READY=1
		break
	fi
	if ! kill -0 "$PID" 2>/dev/null; then
		break
	fi
	sleep 1
done
sleep 2
if kill -0 "$PID" 2>/dev/null; then
	kill -INT "$PID"
	for _ in $(seq 1 40); do
		kill -0 "$PID" 2>/dev/null || break
		sleep 1
	done
	kill -0 "$PID" 2>/dev/null && kill -TERM "$PID"
	wait "$PID" 2>/dev/null
	echo "### exit=$? ready=$READY"
else
	echo "### process exited before ready (ready=$READY)"
fi
grep -E "静态数据解析完成|staticDataLifecycle|启动完成，耗时|服务器已就绪" "$LOG" | tail -4
[ "$JFR" != "/dev/null" ] && ls -l "$JFR"
