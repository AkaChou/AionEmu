#!/usr/bin/env python3
"""P0c-46：链式冻结指纹的外科重冻（只改值，不动布局）。

改动面 = 缺陷面：17 个任务的**交付角色收窄**（剪掉链上非声明 NPC 的 npc-complete 块，
80752 另含一条重复的领奖窗 R 行）后 IR 路由减少，指纹按定义变化；其余 268 行必须逐字节不变
—— 这就是"剪除只落在 17 个任务、未触及任何其它登记行"的机器证明。

fail-closed 断言：
  1. dump 行数 == 冻结行数；
  2. set 对拍：added/removed 为空、changed 必须逐元素等于 EXPECTED_CHANGED；
  3. 冻结文件里这 17 行必须仍是 PRE 值（否则冻结面已漂移，人工重核）。

用法：`python3 -B p0c46_refreeze_fingerprints.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FROZEN_MAIN = REPO / "src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv"
FROZEN_TARGET = REPO / "target/test-classes/quest/retail-simple-talk-chain-ir-fingerprints.tsv"

EXPECTED_CHANGED = ("1484", "2266", "2271", "2480", "2538", "2663", "2914", "2954", "3037",
                    "3041", "3087", "3093", "4052", "11010", "11103", "11460", "80752")
PRE_VALUES = {
	"1484": "ca6f709c6288ec0a55acff853318da85aba671190e55714f5c9c473700c24ec3",
	"2266": "da9758be52a39e641d78697bcde8b7638bb6a8400083cb920766584be7caea4c",
	"2271": "60d121fd36c388f208287713274fd12f8e1fbc1da5500b69f646456604b865b3",
	"2480": "304c6ba3f38bf8ecece28e62ae07718949b19952e6cdc1b1acf069b37b4ef6fa",
	"2538": "b7fc537cb0a0bc54523bd87c811ee2d75c0450befe67fec75677247af613cd8a",
	"2663": "3dfbeb96de879f10eff51ab905746422ae117aeed679aea4c04570e429f821f1",
	"2914": "f4098cb856cedb25cd75e491aab18bc87aa53dc658035518ea907ff48f624e6c",
	"2954": "0ffc2e7f3f3b8e40bc53f8a3aa86e1ad08814dccc3d857824b38b133473350df",
	"3037": "4b78187034a606b745caf68f0b7e4d12b6145ee7a93a3ff3fc1376b849ab5120",
	"3041": "b26a93f764948808b6fa7ed3453651129b51826e65b9c98dcc9cd26c0236f732",
	"3087": "803d4f812b4f04a981930c7fb420695982e28e1f28d3e461f4b5dee82809180f",
	"3093": "e95c839114f56041779d06356f7e40da18d77ebdf6b5b4604f7c9b1901525d65",
	"4052": "70d3fec0673f00476e2daf6bbba52c74f3f6b76476a24d1fa6cbe66d06127c96",
	"11010": "5ddeb2156fff9d71fe02ff7d258a12f3e4a619155e350a4e4f10b0f4150ee5a5",
	"11103": "2f7ec1c9da6797e8c07d90201c9e2336d4d752924c76f2a1c08a7ae7e28b4a1a",
	"11460": "3c6f2f72be998adf1c4c74d96b99b78d53725f4ce88cb95015531849f0a69132",
	"80752": "70abf4b292aaf30e9aee8398c2833f693dc93b773c0659e7a1c2fe16da4ac7d0",
}


def load_rows(path: Path) -> list[str]:
	return [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]


def main() -> int:
	positional = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(positional) != 1:
		print("usage: p0c46_refreeze_fingerprints.py <dump.tsv> [--apply]")
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
