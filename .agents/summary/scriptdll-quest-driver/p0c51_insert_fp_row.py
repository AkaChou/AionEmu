#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-50 / 续片 24：纯 TalkFOBJ 单步行的 IR 指纹外科插入（1 行新增，改值 0 行）。

背景：DD 路由新增纯 TalkFOBJ 单步判据（FOBJ 交互即入领奖），25070 由 XML_RETENTION 翻
RETAIL_TABLE；25052 因声明 collect_item/drop 被路由侧拒绝（码位前移）。

机械（与 p0c39/p0c40 的指纹插入同口径；本冻结表整块升序、无历史尾块）：
  1. 读 `-Dretail.dataDriven.fpOut` 实测 dump（真值来源；只含已退役行）；
  2. 基线数据行与 dump 的**非新增行**逐字节相同（本片不允许任何既有行改值/移位）；
  3. 1 行按 id 升序插入（保持整块升序）；
  4. 断言：打补丁后数据行 == dump 行（逐行逐字节）、基线非新增行逐字节保留、插入集合 == 预期；
  5. 双副本落地（src/test/resources + target/test-classes）。

用法：`python3 -B p0c50_insert_fp_rows.py <dump.tsv>`（dry-run）/ `... <dump.tsv> --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FROZEN = (
	REPO / "src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv",
	REPO / "target/test-classes/quest/retail-data-driven-ir-fingerprints.tsv",
)
NEW_IDS = ("25052",)
# 并行车道的在改行：其冻结值**保持基线**（本片不做任何掩盖手术——漂移由车道自己的片重冻）。
# The parallel lane's in-flight rows: their frozen values stay at the baseline (this slice performs
# no masking surgery; the lane re-freezes them in its own slice).
FOREIGN = ("15042", "16821", "16823", "26821", "26823")


def data_lines(text: str) -> list[str]:
	return [line for line in text.splitlines(keepends=True)
		if not line.startswith("#") and line.strip()]


def main() -> int:
	apply = "--apply" in sys.argv
	args = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(args) != 1:
		print("usage: p0c50_insert_fp_rows.py <dump.tsv> [--apply]")
		return 2
	dump = data_lines(Path(args[0]).read_text(encoding="utf-8"))
	dump_map = {line.split("\t")[0]: line for line in dump}
	baseline_text = FROZEN[0].read_text(encoding="utf-8")
	baseline = data_lines(baseline_text)

	existing = {line.split("\t")[0] for line in baseline}
	for key in NEW_IDS:
		if key in existing:
			print(f"ABORT: new row {key} already present in the frozen file")
			return 1
		if key not in dump_map:
			print(f"ABORT: new row {key} missing from dump (flip not applied yet?)")
			return 1

	# 1) 基线非新增行必须与 dump 逐字节一致（允许且仅允许并行车道的 FOREIGN 行漂移，其值保持基线）。
	problems = []
	drifted = []
	for line in baseline:
		key = line.split("\t")[0]
		if dump_map.get(key) != line:
			if key in FOREIGN:
				drifted.append(key)
				continue
			problems.append(key)
	if problems:
		print(f"ABORT: existing frozen rows drifted (out of scope): {sorted(problems)[:10]}")
		return 1
	if sorted(drifted, key=int) != sorted(FOREIGN, key=int):
		print(f"ABORT: foreign drift set moved (lane state changed): {sorted(drifted)}")
		return 1
	print(f"foreign rows kept at baseline (lane re-freezes them): {sorted(drifted, key=int)}")
	# 2) dump 相对基线的增量必须恰为本批 1 行。
	delta = sorted(set(dump_map) - existing, key=int)
	if delta != sorted(NEW_IDS, key=int):
		print(f"ABORT: dump delta moved — added={delta}")
		return 1
	# 3) 升序插入 1 行。
	patched = list(baseline)
	inserted = []
	for key in sorted(NEW_IDS, key=int):
		position = next((index for index, line in enumerate(patched)
			if int(line.split("\t")[0]) > int(key)), None)
		if position is None:
			print(f"ABORT: new row {key} does not fit inside the ascending block")
			return 1
		patched.insert(position, dump_map[key])
		inserted.append(key)
	# 4) 断言：打补丁后与 dump 相同，FOREIGN 行除外（保持基线值）。
	if len(patched) != len(dump):
		print(f"ABORT: patched rows {len(patched)} != dump rows {len(dump)}")
		return 1
	mismatch = [left.split("\t")[0] for left, right in zip(patched, dump)
		if left != right and left.split("\t")[0] not in FOREIGN]
	if mismatch:
		print(f"ABORT: patched rows != dump rows (out of scope): {mismatch[:10]}")
		return 1
	patched_text = "".join(line for line in baseline_text.splitlines(keepends=True)
		if line.startswith("#") or not line.strip())
	output = patched_text + "".join(patched)

	print(f"PLAN: rows {len(baseline)} -> {len(patched)}; inserted={inserted}; "
		f"non-inserted rows byte-identical to baseline")
	if not apply:
		print("DRY-RUN: no file written (pass --apply to execute)")
		return 0
	for path in FROZEN:
		if path.exists():
			path.write_text(output, encoding="utf-8")
			print(f"written: {path.relative_to(REPO)}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
