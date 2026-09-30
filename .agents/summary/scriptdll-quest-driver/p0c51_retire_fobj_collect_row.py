#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-51 / 续片 25：FOBJ 采集行 —— 25052 翻转退役（掉落驱动 FOBJ 形：collect_item + 掉落源）。

手术式清单补丁：四副本一致的清单为基线（whipsaw 守卫），只改本批四行；随后退役生产 XML
（main + target/classes）、删 catalog 行（main + target/classes）、同步 .agents 快照副本。
退役收口由 verify_retirement.py 独立核验。

Surgical manifest patch for the single-step TalkFOBJ batch: flip the row to retail ownership from a
four-copy-consistent baseline (whipsaw guard), retire the production XMLs (main + target/classes),
drop the catalog rows and sync the .agents snapshot.
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

FLIP_IDS = ("25052",)
NEW_ROW = ("{qid}\tRETAIL_TABLE\tDataDriven\tOK\tretired-xml-in-git-history "
	"p0c51-fobj-collect-decisions.tsv basis=DD_FOBJ_COLLECT")


def main() -> int:
	baselines = [path.read_bytes() for path in
		(MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST)]
	if any(bytes != baselines[0] for bytes in baselines[1:]):
		print("ABORT: manifest copies diverge — lane storm, retry later")
		return 1

	lines = baselines[0].decode("utf-8").splitlines(keepends=True)
	flipped = []
	for index, line in enumerate(lines):
		qid = line.split("\t", 1)[0]
		if qid in FLIP_IDS:
			if not line.startswith(f"{qid}\tXML_RETENTION\t"):
				print(f"ABORT: quest {qid} is not XML_RETENTION: {line.strip()}")
				return 1
			lines[index] = NEW_ROW.format(qid=qid) + "\n"
			flipped.append(qid)
	if sorted(flipped) != sorted(FLIP_IDS):
		print(f"ABORT: expected rows {sorted(FLIP_IDS)}, matched {sorted(flipped)}")
		return 1
	patched = "".join(lines).encode("utf-8")
	changed = sum(1 for old, new in zip(baselines[0].decode("utf-8").splitlines(),
		patched.decode("utf-8").splitlines()) if old != new)
	if changed != len(FLIP_IDS):
		print(f"ABORT: whipsaw guard — {changed} lines changed, expected {len(FLIP_IDS)}")
		return 1
	for path in (MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST):
		path.write_bytes(patched)

	if MANIFEST_AGENTS.exists():
		snapshot = MANIFEST_AGENTS.read_text(encoding="utf-8")
		snapshot_lines = snapshot.splitlines(keepends=True)
		snapshot_flipped = 0
		for index, line in enumerate(snapshot_lines):
			qid = line.split("\t", 1)[0]
			if qid in FLIP_IDS and line.startswith(f"{qid}\tXML_RETENTION\t"):
				snapshot_lines[index] = NEW_ROW.format(qid=qid) + "\n"
				snapshot_flipped += 1
		MANIFEST_AGENTS.write_text("".join(snapshot_lines), encoding="utf-8")
		print(f"snapshot copy rows flipped: {snapshot_flipped}")

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

	print(f"flipped={FLIP_IDS} manifest×4(+snapshot) catalog×2 xml×2 each; run verify_retirement.py next")
	return 0


if __name__ == "__main__":
	sys.exit(main())
