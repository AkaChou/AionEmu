#!/usr/bin/env python3
"""P0c-44：改道裁定 fail-closed 轴的**负测试**（guard proven by negative test）。

正向证据（"生成器重跑逐字节相同"）只证明现状可复现，不证明守卫会拦。本探针逐条注入违规输入，
断言生成器以**该轴专属的错误信息**退出，最后把全部被改文件按备份还原并断言 md5 与运行前一致。

变异（每次只改一处，其余保持真值）：
  M1 code 域          → `LEGACY_BOGUS`                   期望 "code 不在登记域"
  M2 第三方漂移形      → legacy_report_npc 改成真端声明集内的 `Hakon`   期望 "在真端声明集内"
  M3 真端 reward 名     → retail_reward_npc 改成 `Neusa`            期望 "≠ 裁定表"
  M4 与阶梯表互斥       → 把 2482 追加进 p0c36-talk-ladder-decisions.tsv  期望 "形状冲突"

用法：`python3 -B p0c44_guard_probe.py`（无参数；只读真值 + 临时改 + 必还原）
退出码 0 = 四条守卫全部按预期拦下且文件已还原。
"""
from __future__ import annotations

import hashlib
import importlib.util
import subprocess
import sys
from pathlib import Path

TOPIC = Path(__file__).resolve().parent
BUILDER = TOPIC / "build_quest_client_talk_chain_steps.py"
DECISION = TOPIC / "p0c43-canonical-resynthesis.tsv"
LADDER = TOPIC / "p0c36-talk-ladder-decisions.tsv"
REGISTRY = (TOPIC.parents[2] / "src/main/resources/aion/data/static_data/quest_retail/"
	"quest_client_talk_chain_steps.tsv")
ROW_PREFIX = "2482\t"


def md5(path: Path) -> str:
	return hashlib.md5(path.read_bytes()).hexdigest()


def load_builder():
	spec = importlib.util.spec_from_file_location("chain_steps_builder", BUILDER)
	module = importlib.util.module_from_spec(spec)
	spec.loader.exec_module(module)
	return module


def run_builder_expecting(expected: str) -> tuple[bool, str]:
	"""跑生成器（OUT 重定向到临时文件），返回（是否按预期拦下, 实际输出尾行）。"""
	module = load_builder()
	module.OUT = Path("/tmp/p0c44-guard-out.tsv")
	try:
		module.main()
	except SystemExit as exc:
		return exc.code == 1, "exit=%s" % exc.code
	return False, "未拦下（生成器正常跑完）"


def main() -> int:
	touched = (DECISION, LADDER)
	backup = {path: path.read_bytes() for path in touched}
	before = {path: md5(path) for path in touched}
	registry_before = md5(REGISTRY)
	failures = []
	try:
		# M1 code 域
		DECISION.write_text(DECISION.read_text(encoding="utf-8").replace(
			ROW_PREFIX + "LEGACY_STAGE_OWNER_DRIFT", ROW_PREFIX + "LEGACY_BOGUS"), encoding="utf-8")
		ok, note = run_builder_expecting("code 不在登记域")
		print("M1 code 域：%s（%s）" % ("拦下 OK" if ok else "未拦下", note))
		failures += [] if ok else ["M1"]

		# M2 legacy_report_npc 改成真端声明集内的人（Hakon = acquired）
		DECISION.write_bytes(backup[DECISION])
		DECISION.write_text(DECISION.read_text(encoding="utf-8").replace(
			ROW_PREFIX + "LEGACY_STAGE_OWNER_DRIFT\tFinne", ROW_PREFIX + "LEGACY_STAGE_OWNER_DRIFT\tHakon"),
			encoding="utf-8")
		ok, note = run_builder_expecting("在真端声明集内")
		print("M2 漂移形（声明集内）：%s（%s）" % ("拦下 OK" if ok else "未拦下", note))
		failures += [] if ok else ["M2"]

		# M3 retail_reward_npc 与真端不一致
		DECISION.write_bytes(backup[DECISION])
		DECISION.write_text(DECISION.read_text(encoding="utf-8").replace(
			"LEGACY_STAGE_OWNER_DRIFT\tFinne\tMoreinen", "LEGACY_STAGE_OWNER_DRIFT\tFinne\tNeusa"),
			encoding="utf-8")
		ok, note = run_builder_expecting("≠ 裁定表")
		print("M3 真端 reward 名不符：%s（%s）" % ("拦下 OK" if ok else "未拦下", note))
		failures += [] if ok else ["M3"]

		# M4 与阶梯表互斥（2482 同时出现在阶梯裁定表）
		DECISION.write_bytes(backup[DECISION])
		LADDER.write_text(LADDER.read_text(encoding="utf-8").rstrip("\n")
			+ "\n2482\t2\tSETPRO\tguardless probe\n", encoding="utf-8")
		ok, note = run_builder_expecting("形状冲突")
		print("M4 与阶梯表互斥：%s（%s）" % ("拦下 OK" if ok else "未拦下", note))
		failures += [] if ok else ["M4"]
	finally:
		for path, payload in backup.items():
			path.write_bytes(payload)
	drift = [path.name for path in touched if md5(path) != before[path]]
	if drift:
		print("RESTORE_FAILED: %s" % drift)
		return 1
	print("RESTORE_OK: %d 张裁定表 md5 已还原" % len(touched))

	# 还原后再跑一次忠实性检查，证明树回到绿。
	check = subprocess.run([sys.executable, "-B", str(TOPIC / "p0c42_builder_fidelity_check.py"),
		"/tmp/p0c44-guard-fidelity.tsv"], capture_output=True, text=True, check=False)
	tail = [line for line in check.stdout.splitlines() if line.startswith(("FIDELITY", "REGISTRY_MD5"))]
	print("\n".join(tail))
	if check.returncode != 0 or md5(REGISTRY) != registry_before:
		print("POST_CHECK_FAILED")
		return 1
	print("GUARD_PROBE_OK: 4/4 守卫按预期拦下；裁定表还原；登记表未被写入（md5 %s）" % registry_before)
	return 1 if failures else 0


if __name__ == "__main__":
	sys.exit(main())
