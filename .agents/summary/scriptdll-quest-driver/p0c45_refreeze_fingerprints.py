#!/usr/bin/env python3
"""P0c-45：链式冻结指纹的外科重冻（只改值，不动布局）。

改动面 = 缺陷面：35010/35011 剪除多余交付 owner（799806 Priamos）后 IR 路由减少，指纹按定义变化；
同批标记域改动（XML_ONLY→CLIENT_MATCH，35024/35025/35026/45024/45025/45026 共 93 行）**不入 IR**，
故这 6 行指纹必须逐字节不变——这就是"标记改动零 IR 影响"的机器证明。（阶段 2 与报告/完成 owner = Andraste(278018)/Finne(278020)，
均不在真端声明集；`N reward REWARD 0` 与客户端末行 2 不符）改道为真端 canonical 合成（talk_npc1/2 =
Sereniti(204210)/Neusa(204224)、报告绑 Moreinen(204211)、reward 投影 2），IR 指纹按定义逐位变化，
其余 287 行必须逐字节不变。

fail-closed 断言：
  1. dump 行数 == 冻结行数；
  2. set 对拍：added/removed 为空、changed 必须逐元素等于 EXPECTED_CHANGED；
  3. 冻结文件里这一行必须仍是 PRE 值（否则说明冻结面已漂移，人工重核）。

用法：`python3 -B p0c44_refreeze_fingerprints.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FROZEN_MAIN = REPO / "src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv"
FROZEN_TARGET = REPO / "target/test-classes/quest/retail-simple-talk-chain-ir-fingerprints.tsv"

EXPECTED_CHANGED = ("35010", "35011")
PRE_VALUES = {
	"35010": "10477f18bf7e8a8bcf9d66e6e67b8f0d8111c6c249e0270e1236d34490b7dea4",
	"35011": "6fd46033b2d728b8d65203b02df5ff1fbd6dbfd81846dbbb2937d772cf1e8849",
}


def load_rows(path: Path) -> list[str]:
	return [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]


def main() -> int:
	positional = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(positional) != 1:
		print("usage: p0c44_refreeze_fingerprints.py <dump.tsv> [--apply]")
		return 2
	apply = "--apply" in sys.argv
	dump = Path(positional[0])
	if not dump.exists():
		print(f"ABORT: dump missing: {dump}")
		return 1

	frozen_lines = FROZEN_MAIN.read_text(encoding="utf-8").splitlines(keepends=True)
	dump_rows = load_rows(dump)
	frozen_rows = load_rows(FROZEN_MAIN)
	if len(dump_rows) != len(frozen_rows):
		print(f"ABORT: row count {len(dump_rows)} != frozen {len(frozen_rows)}")
		return 1

	dump_map = dict(row.split("\t", 1) for row in dump_rows)
	frozen_map = dict(row.split("\t", 1) for row in frozen_rows)
	added = sorted(set(dump_map) - set(frozen_map))
	removed = sorted(set(frozen_map) - set(dump_map))
	changed = tuple(sorted(qid for qid in set(dump_map) & set(frozen_map)
		if dump_map[qid] != frozen_map[qid]))
	print(f"set-diff: added={added} removed={removed} changed={list(changed)}")
	if added or removed or changed != EXPECTED_CHANGED:
		print(f"ABORT: expected changed={list(EXPECTED_CHANGED)} only, no adds/removes")
		return 1
	for qid in EXPECTED_CHANGED:
		if frozen_map[qid] != PRE_VALUES[qid]:
			print(f"ABORT: frozen value for {qid} drifted: {frozen_map[qid]}")
			return 1

	patched = []
	rewritten = 0
	for line in frozen_lines:
		key = line.split("\t", 1)[0] if line and not line.startswith("#") else None
		if key in EXPECTED_CHANGED:
			suffix = "\n" if line.endswith("\n") else ""
			patched.append(f"{key}\t{dump_map[key]}{suffix}")
			rewritten += 1
		else:
			patched.append(line)
	if rewritten != len(EXPECTED_CHANGED):
		print(f"ABORT: rewrote {rewritten} rows, expected {len(EXPECTED_CHANGED)}")
		return 1
	diff_lines = sum(1 for old, new in zip(frozen_lines, patched) if old != new)
	if diff_lines != len(EXPECTED_CHANGED):
		print(f"ABORT: whipsaw guard — {diff_lines} lines differ")
		return 1

	print(f"PLAN: rewrite {list(EXPECTED_CHANGED)} values; layout preserved "
		f"({len(frozen_lines)} lines, {len(frozen_lines) - len(frozen_rows)} header rows)")
	if not apply:
		print("DRY-RUN: no file written (pass --apply to execute)")
		return 0
	payload = "".join(patched)
	FROZEN_MAIN.write_text(payload, encoding="utf-8")
	if FROZEN_TARGET.exists():
		FROZEN_TARGET.write_text(payload, encoding="utf-8")
	print(f"APPLIED: {FROZEN_MAIN} + target copy; verify with RetailSimpleTalkChainGateTest (no -D)")
	return 0


if __name__ == "__main__":
	sys.exit(main())
