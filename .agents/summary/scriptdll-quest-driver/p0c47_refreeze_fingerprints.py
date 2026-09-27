#!/usr/bin/env python3
"""P0c-47：链式冻结指纹的外科重冻（只改值，不动布局）。

改动面 = 缺陷面：2964（交付流从阶段 NPC 改绑接取/交付 owner：改道重合成）+ 11106/21036/39003/49003
（阶段推进行去掉领奖窗下发令牌）共 5 个任务，指纹按定义变化；其余 280 行必须逐字节不变
—— 这就是"收窄只落在 5 个任务、未触及任何其它登记行"的机器证明。

fail-closed 断言：
  1. dump 行数 == 冻结行数；
  2. set 对拍：added/removed 为空、changed 必须逐元素等于 EXPECTED_CHANGED；
  3. 冻结文件里这 17 行必须仍是 PRE 值（否则冻结面已漂移，人工重核）。

用法：`python3 -B p0c47_refreeze_fingerprints.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FROZEN_MAIN = REPO / "src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv"
FROZEN_TARGET = REPO / "target/test-classes/quest/retail-simple-talk-chain-ir-fingerprints.tsv"

EXPECTED_CHANGED = ("2964", "11106", "21036", "39003", "49003")
PRE_VALUES = {
	"2964": "825b8465bb197f40a360445ef384c65c03730d6f7c162f19b5889b40b8ae463a",
	"11106": "a09dbff9f771e03f28469a96330868ecdf531e84900035e29969fd7893bf16b3",
	"21036": "9ca551c8329969430c4394f41ae436ba9e51cff87d498d1fdefd2c574b8e0ea0",
	"39003": "abab65952fe7663bf85b1d92222e442f22b71ca632eff328cecbcfc599f8026a",
	"49003": "be086687227a817aad1fe2fa3623a3e8dae6c5488f2e71c0eabd533908f916f6",
}


def load_rows(path: Path) -> list[str]:
	return [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]


def main() -> int:
	positional = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(positional) != 1:
		print("usage: p0c47_refreeze_fingerprints.py <dump.tsv> [--apply]")
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
	# 集合比较（字符串排序 ≠ 数字排序；17 个 id 跨 4/5 位时两种排序不同——P0c-45 两 id 时侥幸一致）。
	if added or removed or set(changed) != set(EXPECTED_CHANGED):
		print(f"ABORT: expected changed={sorted(EXPECTED_CHANGED, key=int)} only, no adds/removes")
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
