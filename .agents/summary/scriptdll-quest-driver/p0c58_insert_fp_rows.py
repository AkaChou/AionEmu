#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""续片 30 / P0c-58：IR 指纹外科手术（6 行新增：挑战任务哨兵的 ADOPT 集）。

背景：挑战任务哨兵轴（`value0_acquire_ = _challengetask_` → 接取人回退为真端交付 NPC）落地——
6 行由 XML_RETENTION 翻 RETAIL_TABLE（新增冻结指纹）。机械与 p0c39..p0c56 同口径：
  1. 读 `-Dretail.dataDriven.fpOut` 实测 dump（真值来源；只含已退役行）；
  2. 基线非本批行必须与 dump 逐字节相同（RECORRECT 白名单逐行必须是"确有改值"，FOREIGN 保持基线）；
  3. 6 行按 id 升序插入（保持整块升序）；
  4. 断言：打补丁后数据行 == dump 行（FOREIGN 除外）、基线非本批行逐字节保留；
  5. 双副本落地（src/test/resources + target/test-classes）。

用法：`python3 -B p0c58_insert_fp_rows.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
DECISIONS = REPO / ".agents/summary/scriptdll-quest-driver/p0c58-challenge-sentinel-decisions.tsv"
FROZEN = (
	REPO / "src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv",
	REPO / "target/test-classes/quest/retail-data-driven-ir-fingerprints.tsv",
)
RECORRECT: tuple[str, ...] = ()
FOREIGN = ()


def new_ids() -> tuple[str, ...]:
	ids = []
	for line in DECISIONS.read_text(encoding="utf-8").splitlines():
		if not line.strip() or line.startswith("#"):
			continue
		parts = line.split("\t")
		if parts[0] == "quest_id":
			continue
		if parts[-2] == "ADOPT":
			ids.append(parts[0])
	return tuple(ids)


def data_lines(text: str) -> list[str]:
	return [line for line in text.splitlines(keepends=True)
		if not line.startswith("#") and line.strip()]


def main() -> int:
	apply = "--apply" in sys.argv
	args = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(args) != 1:
		print("usage: p0c58_insert_fp_rows.py <dump.tsv> [--apply]")
		return 2
	ids = new_ids()
	dump = data_lines(Path(args[0]).read_text(encoding="utf-8"))
	dump_map = {line.split("\t")[0]: line for line in dump}
	baseline_text = FROZEN[0].read_text(encoding="utf-8")
	baseline = data_lines(baseline_text)

	existing = {line.split("\t")[0] for line in baseline}
	for key in ids:
		if key in existing:
			print(f"ABORT: new row {key} already present in the frozen file")
			return 1
		if key not in dump_map:
			print(f"ABORT: new row {key} missing from dump (flip not applied yet?)")
			return 1

	problems, recorrected, drifted = [], [], []
	for line in baseline:
		key = line.split("\t")[0]
		observed = dump_map.get(key)
		if observed is None:
			print(f"ABORT: frozen row {key} absent from the dump")
			return 1
		if observed != line:
			if key in RECORRECT:
				recorrected.append(key)
			elif key in FOREIGN:
				drifted.append(key)
			else:
				problems.append(key)
	if problems:
		print(f"ABORT: existing frozen rows drifted (out of scope): {sorted(problems, key=int)[:10]}")
		return 1
	if sorted(recorrected, key=int) != sorted(RECORRECT, key=int):
		print(f"ABORT: re-correct set moved — changed {sorted(recorrected, key=int)}, "
			f"expected {sorted(RECORRECT, key=int)}")
		return 1
	if sorted(drifted, key=int) != sorted(FOREIGN, key=int):
		print(f"ABORT: foreign drift set moved (lane state changed): {sorted(drifted, key=int)}")
		return 1
	delta = sorted(set(dump_map) - existing, key=int)
	if delta != sorted(ids, key=int):
		print(f"ABORT: dump delta moved — added={delta}")
		return 1

	patched = list(baseline)
	inserted = []
	for key in sorted(ids, key=int):
		position = next((index for index, line in enumerate(patched)
			if int(line.split("\t")[0]) > int(key)), None)
		if position is None:
			print(f"ABORT: new row {key} does not fit inside the ascending block")
			return 1
		patched.insert(position, dump_map[key])
		inserted.append(key)
	for index, line in enumerate(patched):
		key = line.split("\t")[0]
		if key in RECORRECT:
			patched[index] = dump_map[key]
	if len(patched) != len(dump):
		print(f"ABORT: patched rows {len(patched)} != dump rows {len(dump)}")
		return 1
	mismatch = [left.split("\t")[0] for left, right in zip(patched, dump)
		if left != right and left.split("\t")[0] not in FOREIGN]
	if mismatch:
		print(f"ABORT: patched rows != dump rows (out of scope): {mismatch[:10]}")
		return 1
	header = "".join(line for line in baseline_text.splitlines(keepends=True)
		if line.startswith("#") or not line.strip())
	output = header + "".join(patched)

	print(f"PLAN: rows {len(baseline)} -> {len(patched)}; inserted={len(inserted)}; "
		f"re-corrected={sorted(RECORRECT, key=int)}")
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
