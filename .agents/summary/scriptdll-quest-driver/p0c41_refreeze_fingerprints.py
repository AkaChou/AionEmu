#!/usr/bin/env python3
"""P0c-41：链式冻结指纹的外科重冻（只改值，不动布局）。

改动面 = 缺陷面：select6 关闭出口闭包让 1932/3092 各多一条 reward 态 FINISH_DIALOG(1008) 路由，
冻结指纹按定义逐位变化（IR 指纹包含全部路由），其余 283 行必须逐字节不变。

fail-closed 断言：
  1. dump 行数 == 冻结行数；
  2. set 对拍：added/removed 为空、changed 必须逐元素等于 EXPECTED_CHANGED；
  3. 冻结文件里这两行必须仍是 PRE 值（否则说明冻结面已漂移，人工重核）。

用法：`python3 -B p0c41_refreeze_fingerprints.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FROZEN_MAIN = REPO / "src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv"
FROZEN_TARGET = REPO / "target/test-classes/quest/retail-simple-talk-chain-ir-fingerprints.tsv"

EXPECTED_CHANGED = ("1932", "3092")
PRE_VALUES = {
	"1932": "798faa83cc6c01e7c0d26db2587dd3740436ee2ad50b67b237b08718f8152f2a",
	"3092": "c0eee0f3b6dfb985ff729797d335b6543f822b13a60524aa9e3f372fb3efa85a",
}


def load_rows(path: Path) -> list[str]:
	return [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]


def main() -> int:
	positional = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(positional) != 1:
		print("usage: p0c41_refreeze_fingerprints.py <dump.tsv> [--apply]")
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
