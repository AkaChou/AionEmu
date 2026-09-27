#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-53：50089/50090 两行采纳回退（客户端契约门拦下：任务书未声明接取页）。

回调面（手术式，逐面断言只碰这两行）：
  1. 生产 XML 还原（`git show HEAD:...`，main + target/classes）；
  2. catalog 定义行还原（main + target/classes）；
  3. 保留清单 4 副本 + .agents 快照：RETAIL_TABLE → XML_RETENTION（原因回到交付名未解析）。

漂移登记与冻结指纹由 `p0c53_apply_revert_registries.py` 用**实测 dump** 逐字回填（不猜 detail）。

Revert of the 50089/50090 adoption (the client contract gate rejects them: the task HTML does not
declare the accept pages): restore the production XMLs, the catalog rows and the manifest rows; the
drift and fingerprint registries are refilled from the measured dump by a second script.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FLIP_IDS = ("50089", "50090")

MANIFESTS = [
	REPO / "src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
	REPO / "src/test/resources/quest/retail-xml-retention.tsv",
	REPO / "target/classes/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
	REPO / "target/test-classes/quest/retail-xml-retention.tsv",
]
MANIFEST_SNAPSHOT = REPO / ".agents/summary/scriptdll-quest-driver/retail-xml-retention.tsv"
CATALOGS = [
	REPO / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml",
	REPO / "target/classes/aion/data/static_data/quest_definition/quest_definition_catalog.xml",
]
QUEST_DIRS = [
	REPO / "src/main/resources/aion/data/static_data/quest_definition/quests",
	REPO / "target/classes/aion/data/static_data/quest_definition/quests",
]
RETAIL_ROW = ("{qid}\tRETAIL_TABLE\tDataDriven\tOK\tretired-xml-in-git-history "
	"p0c53-group-table-decisions.tsv basis=DD_AINAME_GROUP_TABLE\n")
XML_ROW = ("{qid}\tXML_RETENTION\tDataDriven\tSEMANTIC_GAP:RETAIL_REWARD_NPC_UNRESOLVED\t"
	"retail-data-driven-drift.tsv\n")


def restore_xmls() -> int:
	for quest_dir in QUEST_DIRS:
		for quest_id in FLIP_IDS:
			xml = quest_dir / f"{quest_id}.xml"
			if xml.exists():
				continue
			restored = subprocess.run(
				["git", "show",
					f"HEAD:src/main/resources/aion/data/static_data/quest_definition/quests/{quest_id}.xml"],
				cwd=REPO, capture_output=True, check=True).stdout
			xml.write_bytes(restored)
	print(f"xml restored: {len(FLIP_IDS)} × {len(QUEST_DIRS)} dirs")
	return 0


def restore_catalog() -> int:
	for catalog in CATALOGS:
		lines = catalog.read_text(encoding="utf-8").splitlines(keepends=True)
		pattern = re.compile(r'\s*<definition id="(\d+)"')
		present = {int(m.group(1)) for line in lines if (m := pattern.match(line))}
		added = 0
		for quest_id in sorted(int(quest) for quest in FLIP_IDS):
			if quest_id in present:
				continue
			row = (f'  <definition id="{quest_id}" '
				f'resource="aion/data/static_data/quest_definition/quests/{quest_id}.xml" '
				f'mode="EXECUTABLE" />\n')
			position = next((index for index, line in enumerate(lines)
				if (m := pattern.match(line)) and int(m.group(1)) > quest_id), len(lines) - 1)
			lines.insert(position, row)
			added += 1
		catalog.write_text("".join(lines), encoding="utf-8")
		print(f"{catalog.relative_to(REPO)}: {added} catalog rows restored")
	return 0


def restore_manifest(path: Path) -> int:
	lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
	changed = []
	for index, line in enumerate(lines):
		quest_id = line.split("\t", 1)[0]
		if quest_id in FLIP_IDS and line.startswith(RETAIL_ROW.format(qid=quest_id)):
			lines[index] = XML_ROW.format(qid=quest_id)
			changed.append(quest_id)
	if not changed:
		print(f"{path.relative_to(REPO)}: already at XML_RETENTION")
		return 0
	if sorted(changed, key=int) != sorted(FLIP_IDS, key=int):
		print(f"ABORT: {path.name}: restored {len(changed)} rows, expected {len(FLIP_IDS)}")
		return 1
	path.write_text("".join(lines), encoding="utf-8")
	print(f"{path.relative_to(REPO)}: {len(changed)} rows -> XML_RETENTION")
	return 0


def main() -> int:
	for step in (restore_xmls, restore_catalog):
		status = step()
		if status:
			return status
	for path in MANIFESTS:
		if path.exists():
			status = restore_manifest(path)
			if status:
				return status
	if MANIFEST_SNAPSHOT.exists():
		restore_manifest(MANIFEST_SNAPSHOT)
	print("revert complete; run the DD gate dump, then p0c53_apply_revert_registries.py")
	return 0


if __name__ == "__main__":
	sys.exit(main())
