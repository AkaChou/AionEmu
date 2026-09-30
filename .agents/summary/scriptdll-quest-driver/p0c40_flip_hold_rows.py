#!/usr/bin/env python3
"""P0c-40：3 行 HOLD（21460/29070/29071）接取对话页链收口后的采纳 flip。

判准（见 p0c40-accept-chain-decisions.tsv，探针 P0c40AcceptChainProbeTest 实测）：
链式路径补 acceptContinuation（SELECT1_1/SELECT1_1_1 客户端续页）后，
真端侧 I = 0 致命 / 0 未达（XML 侧同 3 行 13 个未达页由真端侧闭合）；
全目录同轴 45 行（42 采纳行 + 本 3 行）的 1011→1012 死按钮一次清零。

机械（沿用 p0c39_adopt_flip_rows.py 的手术式流程）：
  1. 清单四副本一致为基线（whipsaw 守卫），只改本批 3 行 → owner 翻 RETAIL_TABLE；
  2. .agents 快照副本按行单独手术（可能落后于生产副本）；
  3. 退产生产 XML（main + target/classes）——文件已 tracked，内容留在 git 历史；
  4. 删目录行（quest_definition_catalog.xml，main + target/classes）；
  5. 收口由 verify_retirement.py 独立核验（catalog 1341 + retired 4883 = 6224）。

用法：`python3 -B p0c40_flip_hold_rows.py`（dry-run）/ `... --apply`。
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

FLIP_IDS = ("21460", "29070", "29071")
NEW_ROW = ("{qid}\tRETAIL_TABLE\tSimpleTalk\tOK\tretired-xml-in-git-history "
	"p0c40-accept-chain-decisions.tsv basis=CHAIN_ACCEPT_CONTINUATION")


def main() -> int:
	apply = "--apply" in sys.argv
	baselines = [path.read_bytes() for path in
		(MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST)]
	if any(bytes_ != baselines[0] for bytes_ in baselines[1:]):
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

	# XML / 目录前置检查（dry-run 也要过，避免半截落地）。
	for qid in FLIP_IDS:
		for quests_dir in (QUESTS_MAIN, QUESTS_TARGET):
			xml = quests_dir / f"{qid}.xml"
			if not xml.exists():
				print(f"ABORT: expected quest XML missing: {xml}")
				return 1
		row = (f'  <definition id="{qid}" '
			f'resource="aion/data/static_data/quest/definitions/quests/{qid}.xml" mode="EXECUTABLE" />\n')
		for catalog in (CATALOG_MAIN, CATALOG_TARGET):
			count = catalog.read_text(encoding="utf-8").count(row)
			if count != 1:
				print(f"ABORT: catalog row for {qid} not unique in {catalog}: {count}")
				return 1

	print(f"PLAN: flip {len(FLIP_IDS)} rows -> {list(FLIP_IDS)}")
	if not apply:
		print("DRY-RUN: no file written (pass --apply to execute)")
		return 0

	for path in (MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST):
		path.write_bytes(patched)
	if MANIFEST_AGENTS.exists():
		snapshot_lines = MANIFEST_AGENTS.read_text(encoding="utf-8").splitlines(keepends=True)
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
			(quests_dir / f"{qid}.xml").unlink()
		row = (f'  <definition id="{qid}" '
			f'resource="aion/data/static_data/quest/definitions/quests/{qid}.xml" mode="EXECUTABLE" />\n')
		for catalog in (CATALOG_MAIN, CATALOG_TARGET):
			text = catalog.read_text(encoding="utf-8")
			catalog.write_text(text.replace(row, ""), encoding="utf-8")

	print(f"APPLIED: flipped={list(FLIP_IDS)} manifest×4(+snapshot) catalog×2 xml×2 each; "
		"run verify_retirement.py + fingerprint refreeze next")
	return 0


if __name__ == "__main__":
	sys.exit(main())
