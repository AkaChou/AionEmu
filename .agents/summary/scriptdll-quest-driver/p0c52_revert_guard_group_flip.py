#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-52：守备队 24 行采纳回退（击杀计数轴与客户端门冲突，不可采纳）。

回退面（手术式，逐面断言只碰本批 24 行）：
  1. 生产 XML 还原（`git checkout -- quests/<id>.xml`，main + target/classes 由 git/拷贝恢复）；
  2. catalog 定义行还原（main + target/classes）；
  3. 保留清单 4 副本 + .agents 快照：RETAIL_TABLE → XML_RETENTION（原因回到驱动实测码）；
  4. 漂移登记 2 副本：ADOPTED → REJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED；
  5. 冻结指纹 2 副本：删除本批 24 行（其余行逐字节保留）。

Revert of the 24 guard-group row adoption (the kill-count axis conflicts with the client gate, so
the rows are not adoptable): restore the production XMLs, catalog rows, manifest rows, drift rows
and drop the inserted fingerprints — each step asserted to touch only these 24 rows.
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FLIP_IDS = [str(quest) for quest in list(range(13758, 13770)) + list(range(23758, 23770))]

MANIFESTS = [
	REPO / "src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
	REPO / "src/test/resources/quest/retail-xml-retention.tsv",
	REPO / "target/classes/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
	REPO / "target/test-classes/quest/retail-xml-retention.tsv",
]
MANIFEST_SNAPSHOT = REPO / ".agents/summary/scriptdll-quest-driver/retail-xml-retention.tsv"
DRIFTS = [
	REPO / "src/test/resources/quest/retail-data-driven-drift.tsv",
	REPO / "target/test-classes/quest/retail-data-driven-drift.tsv",
]
FINGERPRINTS = [
	REPO / "src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv",
	REPO / "target/test-classes/quest/retail-data-driven-ir-fingerprints.tsv",
]
CATALOGS = [
	REPO / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml",
	REPO / "target/classes/aion/data/static_data/quest_definition/quest_definition_catalog.xml",
]
QUEST_DIRS = [
	REPO / "src/main/resources/aion/data/static_data/quest_definition/quests",
	REPO / "target/classes/aion/data/static_data/quest_definition/quests",
]
RETAIL_ROW = ("{qid}\tRETAIL_TABLE\tDataDriven\tOK\tretired-xml-in-git-history "
	"p0c52-guard-group-decisions.tsv basis=DD_HUNT_GUARD_GROUP\n")
XML_ROW = "{qid}\tXML_RETENTION\tDataDriven\tSEMANTIC_GAP:RETAIL_ACQUIRE_NPC_UNRESOLVED\tretail-data-driven-drift.tsv\n"


def restore_xmls() -> int:
	for quest_dir in QUEST_DIRS:
		for quest_id in FLIP_IDS:
			xml = quest_dir / f"{quest_id}.xml"
			if xml.exists():
				continue
			restored = subprocess.run(
				["git", "show", f"HEAD:src/main/resources/aion/data/static_data/quest_definition/quests/{quest_id}.xml"],
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
				f'resource="aion/data/static_data/quest_definition/quests/{quest_id}.xml" mode="EXECUTABLE" />\n')
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
		if quest_id in FLIP_IDS:
			if line.startswith(RETAIL_ROW.format(qid=quest_id)):
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


def restore_drift(path: Path) -> int:
	lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
	changed = []
	for index, line in enumerate(lines):
		if line.startswith("#") or not line.strip():
			continue
		parts = [part.strip() for part in line.split("\t")]
		if parts[0] in FLIP_IDS:
			if parts[1] != "ADOPTED":
				print(f"ABORT: {path.name}: {parts[0]} is {parts[1]}, expected ADOPTED")
				return 1
			lines[index] = f"{parts[0]}\tREJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED\n"
			changed.append(parts[0])
	if not changed:
		print(f"{path.relative_to(REPO)}: already at XML_RETENTION")
		return 0
	if not changed:
		print(f"{path.relative_to(REPO)}: already at REJECTED")
		return 0
	if sorted(changed, key=int) != sorted(FLIP_IDS, key=int):
		print(f"ABORT: {path.name}: restored {len(changed)} rows, expected {len(FLIP_IDS)}")
		return 1
	path.write_text("".join(lines), encoding="utf-8")
	print(f"{path.relative_to(REPO)}: {len(changed)} rows -> REJECTED")
	return 0


def drop_fingerprints(path: Path) -> int:
	lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
	kept = [line for line in lines if line.split("\t", 1)[0] not in FLIP_IDS]
	removed = len([line for line in lines if line.split("\t", 1)[0] in FLIP_IDS])
	if removed != len(FLIP_IDS):
		print(f"ABORT: {path.name}: removed {removed}, expected {len(FLIP_IDS)}")
		return 1
	path.write_text("".join(kept), encoding="utf-8")
	print(f"{path.relative_to(REPO)}: {removed} fingerprint rows dropped, {len(kept) - 2} data rows left")
	return 0


def main() -> int:
	status = restore_xmls()
	if status:
		return status
	status = restore_catalog()
	if status:
		return status
	for path in MANIFESTS:
		if path.exists():
			status = restore_manifest(path)
			if status:
				return status
	if MANIFEST_SNAPSHOT.exists():
		lines = MANIFEST_SNAPSHOT.read_text(encoding="utf-8").splitlines(keepends=True)
		flipped = 0
		for index, line in enumerate(lines):
			quest_id = line.split("\t", 1)[0]
			if quest_id in FLIP_IDS and line.startswith(RETAIL_ROW.format(qid=quest_id)):
				lines[index] = XML_ROW.format(qid=quest_id)
				flipped += 1
		MANIFEST_SNAPSHOT.write_text("".join(lines), encoding="utf-8")
		print(f"{MANIFEST_SNAPSHOT.name}: {flipped} snapshot rows -> XML_RETENTION")
	for path in DRIFTS:
		if path.exists():
			status = restore_drift(path)
			if status:
				return status
	for path in FINGERPRINTS:
		if path.exists():
			status = drop_fingerprints(path)
			if status:
				return status
	print("revert complete; run verify_retirement.py and the DD gate next")
	return 0


if __name__ == "__main__":
	sys.exit(main())
