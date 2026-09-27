#!/usr/bin/env python3
"""P0c-27：1526/1351 双行翻转（CLIENT_ROUTE 降级反转 → 真端驱动）。

手术式清单补丁：以四副本一致的清单为基线（whipsaw 守卫），只改本切片两行；
随后退役生产 XML（main + target/classes）、删目录行（main + target/classes）、
同步全部目标副本。退役收口由 verify_retirement.py 独立核验。

P0c-27: flip quests 1526/1351 to retail ownership (CLIENT_ROUTE downgrade reversal).
Surgical manifest patch from a four-copy-consistent baseline (whipsaw guard), touching
only this slice's two rows; then retire the production XMLs (main + target/classes),
drop the catalog rows (main + target/classes), and sync every target copy. Retirement
closure is independently audited by verify_retirement.py.
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]

MANIFEST_MAIN = REPO / "src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
MANIFEST_TEST = REPO / "src/test/resources/quest/retail-xml-retention.tsv"
MANIFEST_TARGET_MAIN = REPO / "target/classes/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
MANIFEST_TARGET_TEST = REPO / "target/test-classes/quest/retail-xml-retention.tsv"
CATALOG_MAIN = REPO / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"
CATALOG_TARGET = REPO / "target/classes/aion/data/static_data/quest_definition/quest_definition_catalog.xml"
QUESTS_MAIN = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUESTS_TARGET = REPO / "target/classes/aion/data/static_data/quest_definition/quests"

FLIP_IDS = ("1526", "1351")
NEW_ROW = "{qid}\tRETAIL_TABLE\tSimpleTalk\tOK\tretired-xml-in-git-history p0c27-1526-1351-journal-axis-flip.tsv basis=M3D_DOWNGRADE_REVERSED"


def main() -> int:
	# whipsaw 守卫：四副本清单必须先一致，否则 lane 正在写，立刻放弃。 /
	# Whipsaw guard: all four manifest copies must agree, else the lane is mid-write.
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

	# 退役生产 XML 与目录行（main + target 双副本）。 /
	# Retire the production XMLs and catalog rows (main + target copies).
	for qid in FLIP_IDS:
		for quests_dir in (QUESTS_MAIN, QUESTS_TARGET):
			xml = quests_dir / f"{qid}.xml"
			if not xml.exists():
				print(f"ABORT: expected quest XML missing: {xml}")
				return 1
			xml.unlink()
		row = f'  <definition id="{qid}" resource="aion/data/static_data/quest_definition/quests/{qid}.xml" mode="EXECUTABLE" />\n'
		for catalog in (CATALOG_MAIN, CATALOG_TARGET):
			text = catalog.read_text(encoding="utf-8")
			if text.count(row) != 1:
				print(f"ABORT: catalog row for {qid} not unique in {catalog.name}: {text.count(row)}")
				return 1
			catalog.write_text(text.replace(row, ""), encoding="utf-8")

	print(f"flipped={FLIP_IDS} manifest×4 catalog×2 xml×2 each; run verify_retirement.py next")
	return 0


if __name__ == "__main__":
	sys.exit(main())
