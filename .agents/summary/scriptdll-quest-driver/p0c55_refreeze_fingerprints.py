#!/usr/bin/env python3
"""P0c-55：链式冻结指纹的外科重冻（只改值，不动布局）。

改动面 = 缺陷面：9 个任务的阶段腿按真端 `talk_npc<k>` + 客户端页链重建（阶段 2..K 的页/推进动作名、
领奖页下发、越界阶段行剪除）⇒ 其冻结 IR 指纹按定义变化；其余行必须逐字节不变 —— 这就是"重建只落在
9 个任务、未触及任何其它登记行"的机器证明。

fail-closed 断言：
  1. dump 行数 == 冻结行数；
  2. set 对拍：added/removed 为空、changed 必须逐元素等于 EXPECTED_CHANGED；
  3. 冻结文件里这 9 行必须仍是 PRE 值（否则冻结面已漂移，人工重核）。

用法：`python3 -B p0c55_refreeze_fingerprints.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FROZEN_MAIN = REPO / "src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv"
FROZEN_TARGET = REPO / "target/test-classes/quest/retail-simple-talk-chain-ir-fingerprints.tsv"

EXPECTED_CHANGED = ("1323", "1394", "1484", "2480", "2538", "3093", "4052", "11010", "11103")
PRE_VALUES = {
	"1323": "9e3f26da94c224099501f0feb12a17c8863c71ab1e89a7d22eb348317f6746a7",
	"1394": "28b4a995b27e9ef0d5574fc905d24018ff2ea9ffdd96412b09e9820bca67a2a1",
	"1484": "26f182968fa1ce97d05d9629f8268ad9c76338e2812f8142d39652bc126c0533",
	"2480": "64f405c4a1cdf6a1f2a8ae980430d7444e29eab2c2c9033678b306c07992a04f",
	"2538": "2a479a5486e51d4729f0485b9d38da1309348c075c95b8f9adf64a36f0f8bf98",
	"3093": "c75071eae6b2efba762197526718dee5d30dd739c9e36c6247201a9518c1d57a",
	"4052": "9b3b4fdd8abc050e4c4b248492ef3f1e7202596e4e8313caaac9a2b1a5602769",
	"11010": "16d599090aaaeef1d6aeb901a57573f70a26f32e390c6a5887677824c1844dc3",
	"11103": "1c90e9b1a5d5d2fcb0d989fad2a24c4699df93326bc62a5619970d8093f88ce8",
}


def load_rows(path: Path) -> list[str]:
	return [line for line in path.read_text(encoding="utf-8").splitlines()
		if line and not line.startswith("#")]


def main() -> int:
	positional = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(positional) != 1:
		print("usage: p0c55_refreeze_fingerprints.py <dump.tsv> [--apply]")
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
	print(f"PLAN: rewrite {list(EXPECTED_CHANGED)} values; layout preserved "
		f"({len(frozen_lines)} lines, {len(frozen_map)} quest rows)")
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
