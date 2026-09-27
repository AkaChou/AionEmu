#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-50：25052 码位前移登记同步（RETAIL_STEP_UNSUPPORTED → RETAIL_FOBJ_COLLECT_UNSUPPORTED）。

路由侧新增纯 TalkFOBJ 单步行判据后，25052（声明 collect_item 5 + drop_monster）的拒绝码从
`RETAIL_STEP_UNSUPPORTED` 前移到 `RETAIL_FOBJ_COLLECT_UNSUPPORTED`（更准确的形状判据）。本脚本
把**码位前移**同步到两处登记（只改 25052 一行，其余逐字节保留）：
  1. 漂移登记 `src/test/resources/quest/retail-data-driven-drift.tsv`（门禁消费，2 副本 + .agents 快照）；
  2. 保留清单 reason 列 `retail-xml-retention.tsv` 的 `SEMANTIC_GAP:<码>`（5 副本，口径一致性）。

Syncs the 25052 code shift caused by the new pure-TalkFOBJ router predicate into both registries
(the enforced drift registry and the retention manifest's reason column), touching that row only.

用法：`python3 -B p0c50_shift_25052_code.py <drift-dump.tsv> [--apply]`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUEST_ID = "25052"
OLD_CODE = "REJECTED:RETAIL_STEP_UNSUPPORTED"
DRIFT = (
	REPO / "src/test/resources/quest/retail-data-driven-drift.tsv",
	REPO / "target/test-classes/quest/retail-data-driven-drift.tsv",
)
DRIFT_SNAPSHOT = REPO / ".agents/summary/scriptdll-quest-driver/dd-drift-fresh.tsv"
MANIFESTS = (
	REPO / "src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
	REPO / "src/test/resources/quest/retail-xml-retention.tsv",
	REPO / "target/classes/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
	REPO / "target/test-classes/quest/retail-xml-retention.tsv",
	REPO / ".agents/summary/scriptdll-quest-driver/retail-xml-retention.tsv",
)


def main() -> int:
	apply = "--apply" in sys.argv
	args = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(args) != 1:
		print("usage: p0c50_shift_25052_code.py <drift-dump.tsv> [--apply]")
		return 2
	dump_line = next((line for line in Path(args[0]).read_text(encoding="utf-8").splitlines()
		if line.startswith(QUEST_ID + "\t")), None)
	if dump_line is None:
		print("ABORT: quest 25052 missing from the classification dump")
		return 1
	new_code = dump_line.split("\t")[1]
	if new_code == OLD_CODE or not new_code.startswith("REJECTED:"):
		print(f"ABORT: unexpected classification for 25052: {new_code}")
		return 1

	changed = 0
	for path in (*DRIFT, DRIFT_SNAPSHOT):
		if not path.exists():
			continue
		lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
		hits = 0
		for index, line in enumerate(lines):
			if line.startswith(QUEST_ID + "\t" + OLD_CODE):
				lines[index] = dump_line + "\n"
				hits += 1
		if hits != 1:
			print(f"ABORT: {path.name}: expected exactly one 25052 row, found {hits}")
			return 1
		if apply:
			path.write_text("".join(lines), encoding="utf-8")
		changed += 1
		print(f"{path.relative_to(REPO)}: 25052 {OLD_CODE} -> {new_code}")

	printed = 0
	# 清单 reason 列去掉 `REJECTED:` 前缀（清单写 `SEMANTIC_GAP:<码>`，漂移登记写 `REJECTED:<码>`）。
	# The manifest reason drops the `REJECTED:` prefix (the manifest writes SEMANTIC_GAP:<code> while
	# the drift registry writes REJECTED:<code>).
	old_manifest_code = OLD_CODE.removeprefix("REJECTED:")
	new_manifest_code = new_code.removeprefix("REJECTED:")
	for path in MANIFESTS:
		if not path.exists():
			continue
		text = path.read_text(encoding="utf-8")
		old_row = f"{QUEST_ID}\tXML_RETENTION\tDataDriven\tSEMANTIC_GAP:{old_manifest_code}"
		new_row = f"{QUEST_ID}\tXML_RETENTION\tDataDriven\tSEMANTIC_GAP:{new_manifest_code}"
		if text.count(old_row) != 1:
			print(f"ABORT: {path.name}: manifest row not unique ({text.count(old_row)})")
			return 1
		if apply:
			path.write_text(text.replace(old_row, new_row), encoding="utf-8")
		printed += 1
	print(f"PLAN: drift files {changed}, manifest files {printed}; apply={apply}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
