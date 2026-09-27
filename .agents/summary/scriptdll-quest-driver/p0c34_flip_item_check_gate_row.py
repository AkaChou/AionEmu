#!/usr/bin/env python3
"""P0c-34：item_check 门通道首行入台——1152 翻转（RETAIL_TABLE）。

手术式清单补丁：四副本一致的清单为基线（whipsaw 守卫），只改本切片一行；
随后退役生产 XML（main + target/classes）、删目录行（main + target/classes）、
同步目标副本。指纹由 RetailSimpleTalkChainGateTest 重冻结模式产出后外科安装；
退役收口由 verify_retirement.py 独立核验。

Surgical manifest patch from a four-copy-consistent baseline (whipsaw guard), touching only this
slice's row; then retire the production XML, drop the catalog row, and sync every target copy.
Retirement closure is independently audited by verify_retirement.py.
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

FLIP_ID = "1152"
NEW_ROW = ("1152\tRETAIL_TABLE\tSimpleTalk\tOK\tretired-xml-in-git-history "
	"p0c34-item-check-gate-channel-decisions.tsv basis=ITEM_CHECK_GATE_CHANNEL")


def main() -> int:
	baselines = [path.read_bytes() for path in
		(MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST)]
	if any(bytes != baselines[0] for bytes in baselines[1:]):
		print("ABORT: manifest copies diverge — lane storm, retry later")
		return 1

	lines = baselines[0].decode("utf-8").splitlines(keepends=True)
	flipped = []
	for index, line in enumerate(lines):
		if line.split("\t", 1)[0] == FLIP_ID:
			if not line.startswith(f"{FLIP_ID}\tXML_RETENTION\t"):
				print(f"ABORT: quest {FLIP_ID} is not XML_RETENTION: {line.strip()}")
				return 1
			lines[index] = NEW_ROW + "\n"
			flipped.append(FLIP_ID)
	if flipped != [FLIP_ID]:
		print(f"ABORT: expected row {FLIP_ID}, matched {flipped}")
		return 1
	patched = "".join(lines).encode("utf-8")
	changed = sum(1 for old, new in zip(baselines[0].decode("utf-8").splitlines(),
		patched.decode("utf-8").splitlines()) if old != new)
	if changed != 1:
		print(f"ABORT: whipsaw guard — {changed} lines changed, expected 1")
		return 1
	for path in (MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST):
		path.write_bytes(patched)

	for quests_dir in (QUESTS_MAIN, QUESTS_TARGET):
		xml = quests_dir / f"{FLIP_ID}.xml"
		if not xml.exists():
			print(f"ABORT: expected quest XML missing: {xml}")
			return 1
		xml.unlink()
	row = (f'  <definition id="{FLIP_ID}" '
		f'resource="aion/data/static_data/quest_definition/quests/{FLIP_ID}.xml" '
		f'mode="EXECUTABLE" />\n')
	for catalog in (CATALOG_MAIN, CATALOG_TARGET):
		text = catalog.read_text(encoding="utf-8")
		if text.count(row) != 1:
			print(f"ABORT: catalog row for {FLIP_ID} not unique in {catalog.name}: {text.count(row)}")
			return 1
		catalog.write_text(text.replace(row, ""), encoding="utf-8")

	print(f"flipped={FLIP_ID} manifest×4 catalog×2 xml×2; run verify_retirement.py next")
	return 0


if __name__ == "__main__":
	sys.exit(main())
