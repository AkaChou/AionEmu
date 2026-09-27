#!/usr/bin/env python3
"""P0c-57：链式冻结指纹的外科重冻（只改值，不动布局）。

改动面 = 缺陷面：18 个任务的**接取入口块**按真端接取 owner 收窄（每个链上 NPC 一条 `NPC_START`
块的遗留扩散被剪：块 + 接取族行）⇒ 其冻结 IR 指纹按定义变化；其余 267 行必须逐字节不变 ——
这就是"入口收窄只落在 18 个任务、未触及任何其它登记行"的机器证明。

fail-closed 断言：
  1. dump 行数 == 冻结行数；
  2. set 对拍：added/removed 为空、changed 必须逐元素等于 EXPECTED_CHANGED；
  3. **changed 集合 == 裁定表 `p0c57-accept-entrance-decisions.tsv` 的任务集合**（声明面与实测面互证）；
  4. 冻结文件里这 18 行必须仍是 PRE 值（否则冻结面已漂移，人工重核）。

用法：`python3 -B p0c57_refreeze_fingerprints.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
TOPIC = REPO / ".agents/summary/scriptdll-quest-driver"
DECISIONS = TOPIC / "p0c57-accept-entrance-decisions.tsv"
FROZEN_MAIN = REPO / "src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv"
FROZEN_TARGET = REPO / "target/test-classes/quest/retail-simple-talk-chain-ir-fingerprints.tsv"

EXPECTED_CHANGED = ("1484", "2271", "2480", "2538", "2646", "2663", "2914", "2954",
	"3037", "3041", "3087", "3093", "3966", "4052", "11010", "11103", "11460", "16990")
PRE_VALUES = {
	"1484": "7b609792f7968f0d3a0d1d25ce1aa6035902e6e4f60151cc6b1d12e0bd19c009",
	"2271": "082f7ec7a1a61adc200a72f94cb0d42cfd4c33385951cafcecbb28916ff4d756",
	"2480": "3c29ea1326c3649d13be359f4d0a4ebf4e4a7bbea6ab5d6096ec3ce7f3a61c01",
	"2538": "ca9dc32cd08fce937cb25b6bc087b82a2e48ae1e58e3a716035178dc45ab41b1",
	"2646": "d6de0a5e76d9ea7427af4aa39e93d4a12fcfb4d01e17dde5f346f4c7aeee9deb",
	"2663": "694c8576277e992e9904310957e21adea13853c283dbac12fd72352891713e6f",
	"2914": "6b416033d827922aa75cddb283a12723a173e4d7fabac9af10adfd08238b73d3",
	"2954": "e4ef83e2c096980b3851088bc064468180139cf5da2606c27d37b6a41ad50852",
	"3037": "5376d4f4c7d1c7e00519394ccb68084bd3684be403bce05f6a6e25469c9d2097",
	"3041": "e786474aeb7c444e4a574b412e6805f312f165a348683eea86cc8037c78e9fd0",
	"3087": "f2ea400fc59668b7a8cbcd904a1a5edccde9821aa6d2208af24ab8725ac5af38",
	"3093": "faaf844625566c7e2e01f79ecdc62d2ad533d8ac6ebbc21db1610ec06a429e34",
	"3966": "80c10bc940cf1e882fb53fb005c77e1fb42dd5aa37c56fd935085e64d41c28da",
	"4052": "19dd806148bfe887b8256eb73c98d3d510d63fdb970f6f40a1e70327decd7ae2",
	"11010": "c57343fb194787edc9e03beaa12965c6c5941d62f8cdda79c96a2da9c0ae1dd1",
	"11103": "a4259fee233e20272f1b3c3d222939e868dbcc8382b6c82132a6313b323388c3",
	"11460": "074d551703e46608c019fd92cd35315e316e962697478e16b500d0f925d3e951",
	"16990": "63b493c3cac54149ff1e7facbeae3e3e251c9e244003c11b9fa159ad7c052f41",
}


def load_rows(path: Path) -> list[str]:
	return [line for line in path.read_text(encoding="utf-8").splitlines()
		if line and not line.startswith("#")]


def decision_quests() -> list[str]:
	rows = load_rows(DECISIONS)
	return sorted({row.split("\t")[0] for row in rows[1:]}, key=int)


def main() -> int:
	positional = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(positional) != 1:
		print("usage: p0c57_refreeze_fingerprints.py <dump.tsv> [--apply]")
		return 2
	apply = "--apply" in sys.argv
	dump = Path(positional[0])
	if not dump.exists():
		print(f"ABORT: dump missing: {dump}")
		return 1

	declared = decision_quests()
	if declared != sorted(EXPECTED_CHANGED, key=int):
		print(f"ABORT: decision table quests {declared} != EXPECTED_CHANGED")
		return 1

	frozen_lines = FROZEN_MAIN.read_text(encoding="utf-8").splitlines(keepends=True)
	dump_rows = load_rows(dump)
	frozen_rows = load_rows(FROZEN_MAIN)
	if len(dump_rows) != len(frozen_rows):
		print(f"ABORT: row count {len(dump_rows)} != frozen {len(frozen_rows)}")
		return 1
	dump_map = dict(row.split("\t") for row in dump_rows)
	frozen_map = dict(row.split("\t") for row in frozen_rows)
	added = sorted(set(dump_map) - set(frozen_map), key=int)
	removed = sorted(set(frozen_map) - set(dump_map), key=int)
	changed = sorted([q for q in dump_map if q in frozen_map and dump_map[q] != frozen_map[q]], key=int)
	if added or removed or set(changed) != set(EXPECTED_CHANGED):
		print(f"ABORT: expected changed={sorted(EXPECTED_CHANGED, key=int)} only, no adds/removes")
		print(f"       changed={changed} added={added} removed={removed}")
		return 1
	for qid in EXPECTED_CHANGED:
		if frozen_map[qid] != PRE_VALUES[qid]:
			print(f"ABORT: frozen {qid} is not the recorded pre value (freeze surface drifted)")
			return 1
	print(f"PLAN: rewrite {len(EXPECTED_CHANGED)} values; layout preserved "
		f"({len(frozen_lines)} lines, {len(frozen_map)} quest rows); "
		f"declared == measured == decision table")
	if not apply:
		print("DRY-RUN: pass --apply to write")
		return 0
	rewritten = 0
	diff_lines = 0
	for index, line in enumerate(frozen_lines):
		if not line or line.startswith("#"):
			continue
		qid, value = line.rstrip("\n").split("\t")
		if qid in EXPECTED_CHANGED:
			new = dump_map[qid]
			if new != value:
				frozen_lines[index] = line.replace(value, new, 1)
				diff_lines += 1
			rewritten += 1
	if rewritten != len(EXPECTED_CHANGED):
		print(f"ABORT: rewrote {rewritten} rows, expected {len(EXPECTED_CHANGED)}")
		return 1
	if diff_lines != len(EXPECTED_CHANGED):
		print(f"ABORT: {diff_lines} value lines changed, expected {len(EXPECTED_CHANGED)}")
		return 1
	FROZEN_MAIN.write_text("".join(frozen_lines), encoding="utf-8")
	if FROZEN_TARGET.exists():
		FROZEN_TARGET.write_text("".join(frozen_lines), encoding="utf-8")
	print(f"APPLIED: {FROZEN_MAIN}（同步 {FROZEN_TARGET.name if FROZEN_TARGET.exists() else '-'}）")
	return 0


if __name__ == "__main__":
	sys.exit(main())
