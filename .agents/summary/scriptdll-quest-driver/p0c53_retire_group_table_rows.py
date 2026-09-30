#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-53 / 续片 27：真端对话名组表扩域批 25 行翻转退役 + 2 行拒绝码前移（手术式清单补丁）。

- 25 行：`XML_RETENTION` → `RETAIL_TABLE`（依据 `p0c53-group-table-decisions.tsv`，basis=DD_AINAME_GROUP_TABLE）；
- 2 行（15480/25480）：接取名已解析、改在击杀目标名上如实拒绝 ⇒ 原因列 `SEMANTIC_GAP:` 前缀后
  换成 `RETAIL_MONSTER_UNRESOLVED`（drift 同码，由 p0c53_update_drift_rows.py 同步）；
- 四副本清单一致为基线（whipsaw 守卫），只改本批行；随后退役生产 XML（main + target/classes）、
  删 catalog 行（main + target/classes）、同步 .agents 快照副本；收口由 verify_retirement.py 核验。

Surgical manifest patch: 25 rows flip to retail ownership, 2 rows shift their rejection code. The
manifest copies must be four-way identical first (whipsaw guard); the retired XMLs, catalog rows and
the .agents snapshot follow.
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]

MANIFEST_MAIN = REPO / "src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
MANIFEST_TEST = REPO / "src/test/resources/quest/retail-xml-retention.tsv"
MANIFEST_TARGET_MAIN = REPO / "target/classes/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
MANIFEST_TARGET_TEST = REPO / "target/test-classes/quest/retail-xml-retention.tsv"
MANIFEST_AGENTS = REPO / ".agents/summary/scriptdll-quest-driver/retail-xml-retention.tsv"
CATALOG_MAIN = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml"
CATALOG_TARGET = REPO / "target/classes/aion/data/static_data/quest/definitions/quest_definition_catalog.xml"
QUESTS_MAIN = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
QUESTS_TARGET = REPO / "target/classes/aion/data/static_data/quest/definitions/quests"

FLIP_IDS = ("15476", "15477", "15478", "15479", "18743", "25476", "25477", "25478", "25479", "28743",
	"50068", "50069", "50072", "50073", "50074", "50088", "50089", "50090", "50091", "50092", "50094",
	"50095", "50104", "50105", "50107")
SHIFT_IDS = ("15480", "25480")
SHIFT_FROM = "SEMANTIC_GAP:RETAIL_ACQUIRE_NPC_UNRESOLVED"
SHIFT_TO = "SEMANTIC_GAP:RETAIL_MONSTER_UNRESOLVED"
NEW_ROW = ("{qid}\tRETAIL_TABLE\tDataDriven\tOK\tretired-xml-in-git-history "
	"p0c53-group-table-decisions.tsv basis=DD_AINAME_GROUP_TABLE")


def patched_line(line: str) -> str:
	qid = line.split("\t", 1)[0]
	if qid in FLIP_IDS:
		return NEW_ROW.format(qid=qid) + "\n"
	if qid in SHIFT_IDS and SHIFT_FROM in line:
		return line.replace(SHIFT_FROM, SHIFT_TO)
	return line


def main() -> int:
	baselines = [path.read_bytes() for path in
		(MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST)]
	if any(bytes != baselines[0] for bytes in baselines[1:]):
		print("ABORT: manifest copies diverge — lane storm, retry later")
		return 1
	lines = baselines[0].decode("utf-8").splitlines(keepends=True)
	flipped, shifted = [], []
	for index, line in enumerate(lines):
		qid = line.split("\t", 1)[0]
		if qid in FLIP_IDS:
			if not line.startswith(f"{qid}\tXML_RETENTION\t"):
				print(f"ABORT: quest {qid} is not XML_RETENTION: {line.strip()}")
				return 1
			flipped.append(qid)
		if qid in SHIFT_IDS:
			if SHIFT_FROM not in line:
				print(f"ABORT: quest {qid} does not carry {SHIFT_FROM}: {line.strip()}")
				return 1
			shifted.append(qid)
		lines[index] = patched_line(line)
	if sorted(flipped, key=int) != sorted(FLIP_IDS, key=int):
		print(f"ABORT: expected flips {sorted(FLIP_IDS, key=int)}, matched {sorted(flipped, key=int)}")
		return 1
	if sorted(shifted, key=int) != sorted(SHIFT_IDS, key=int):
		print(f"ABORT: expected shifts {sorted(SHIFT_IDS, key=int)}, matched {sorted(shifted, key=int)}")
		return 1
	patched = "".join(lines)
	patched_bytes = patched.encode("utf-8")
	changed = sum(1 for old, new in zip(baselines[0].decode("utf-8").splitlines(),
		patched.splitlines()) if old != new)
	if changed != len(FLIP_IDS) + len(SHIFT_IDS):
		print(f"ABORT: whipsaw guard — {changed} lines changed, expected "
			f"{len(FLIP_IDS) + len(SHIFT_IDS)}")
		return 1
	for path in (MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST):
		path.write_bytes(patched_bytes)

	if MANIFEST_AGENTS.exists():
		snapshot = MANIFEST_AGENTS.read_text(encoding="utf-8")
		snapshot_lines = [patched_line(line) for line in snapshot.splitlines(keepends=True)]
		MANIFEST_AGENTS.write_text("".join(snapshot_lines), encoding="utf-8")
		print("snapshot copy patched")

	for qid in FLIP_IDS:
		for quests_dir in (QUESTS_MAIN, QUESTS_TARGET):
			xml = quests_dir / f"{qid}.xml"
			if not xml.exists():
				print(f"ABORT: expected quest XML missing: {xml}")
				return 1
			xml.unlink()
		row = (f'  <definition id="{qid}" '
			f'resource="aion/data/static_data/quest/definitions/quests/{qid}.xml" mode="EXECUTABLE" />\n')
		for catalog in (CATALOG_MAIN, CATALOG_TARGET):
			text = catalog.read_text(encoding="utf-8")
			if text.count(row) != 1:
				print(f"ABORT: catalog row for {qid} not unique in {catalog.name}: {text.count(row)}")
				return 1
			catalog.write_text(text.replace(row, ""), encoding="utf-8")

	print(f"flipped={len(FLIP_IDS)} rows; shifted={len(SHIFT_IDS)} code(s); manifest×4(+snapshot) "
		f"catalog×2 xml×2 each; run verify_retirement.py next")
	return 0


if __name__ == "__main__":
	sys.exit(main())
