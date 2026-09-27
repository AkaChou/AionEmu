#!/usr/bin/env python3
"""P0c-54：链式冻结指纹的外科重冻（只改值，不动布局）。

改动面 = 缺陷面：2964（交付流从阶段 NPC 改绑接取/交付 owner：改道重合成）+ 11106/21036/39003/49003
（阶段推进行去掉领奖窗下发令牌）共 5 个任务，指纹按定义变化；其余 280 行必须逐字节不变
—— 这就是"收窄只落在 5 个任务、未触及任何其它登记行"的机器证明。

fail-closed 断言：
  1. dump 行数 == 冻结行数；
  2. set 对拍：added/removed 为空、changed 必须逐元素等于 EXPECTED_CHANGED；
  3. 冻结文件里这 17 行必须仍是 PRE 值（否则冻结面已漂移，人工重核）。

用法：`python3 -B p0c54_refreeze_fingerprints.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FROZEN_MAIN = REPO / "src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv"
FROZEN_TARGET = REPO / "target/test-classes/quest/retail-simple-talk-chain-ir-fingerprints.tsv"

EXPECTED_CHANGED = ("11070", "11105", "11106", "11117", "19004", "21004", "21036", "21136", "2611", "3001", "3023", "39003", "49003")
PRE_VALUES = {
	"11070": "efcb7c71cc8a9f75dfcf304a0504f6af22f760f479d4b06f25efdcee1b12ad13",
	"11105": "8734965d3ae7c95fef021f392598238741cde1b1972c634e9906e9d07071baea",
	"11106": "448bae4392685d0e7988b7dbac60cb75993e29b89f1c8fa03fbe3a9b1599cccf",
	"11117": "08c7811b88791a1bab1c338bd003bc55e6a2f10c2572093318ebe9e542a70c1a",
	"19004": "969867cd7c400d583a3467dbbec5872e193d90ed1ed25b62d59b7d1745995abd",
	"21004": "b59da50aa5c70222183c1cd5c47141bf7788cc267e42acf574a25e840b98843c",
	"21036": "8dd3ae2374a07d09cd018b4c7426f0bbfc7182da9a1710d68b8d3b84e8a11038",
	"21136": "53939281b07626a451bfa8e034eeb862e55f13c2fbff2e88a0ccaf09b4bb1359",
	"2611": "22b78ff304fdfe24d821395fc8b16438e768bcb0f60beb7c310246957db12c30",
	"3001": "7cf3c49327452c494ed229eb20044e307e7d2839e348fce7929d25b8eb450651",
	"3023": "bd35b0a0d79956ab03f3092610d1fbcaa116f4929a96c236b0fcb0a67c102216",
	"39003": "e10cdbc9604d33b6268fe9d793ad2d35c8d59c140311c19a684e0b838e750c31",
	"49003": "dab95cd0ae922d2ccc47154b8f81d66db5067e193ccde2ae1433a889d98c024a",
}


def load_rows(path: Path) -> list[str]:
	return [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]


def main() -> int:
	positional = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(positional) != 1:
		print("usage: p0c54_refreeze_fingerprints.py <dump.tsv> [--apply]")
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
