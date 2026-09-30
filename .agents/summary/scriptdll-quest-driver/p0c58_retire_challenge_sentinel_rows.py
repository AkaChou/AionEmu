#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""续片 30 / P0c-58：挑战任务哨兵 6 行翻转退役（手术式清单补丁）。

- 6 行（`p0c58-challenge-sentinel-decisions.tsv` 的 ADOPT 行，basis=DD_CHALLENGE_SENTINEL_ACCEPT_AT_REWARD_NPC）：
  `XML_RETENTION` → `RETAIL_TABLE`（接取人 = 真端交付 NPC 本人）；
- 四副本清单一致为基线（whipsaw 守卫），只改本批 6 行；随后退役生产 XML（main + target/classes）、
  删 catalog 行（main + target/classes）、同步 .agents 快照副本；收口由 verify_retirement.py 核验。

Surgical manifest patch: the challenge-sentinel slice's six rows flip to retail ownership (ids cross-checked
against the decision table). Manifest copies must be four-way identical first (whipsaw guard); the
retired XMLs, catalog rows and the .agents snapshot follow.
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
DECISIONS = REPO / ".agents/summary/scriptdll-quest-driver/p0c58-challenge-sentinel-decisions.tsv"

EXPECTED_FLIPS = ("17160", "17161", "17162", "27160", "27161", "27162")
NEW_ROW = ("{qid}\tRETAIL_TABLE\tDataDriven\tOK\tretired-xml-in-git-history "
	"p0c58-challenge-sentinel-decisions.tsv basis=DD_CHALLENGE_SENTINEL_ACCEPT_AT_REWARD_NPC")


def flip_ids() -> tuple[str, ...]:
	"""决策表的 ADOPT 行 = 本片翻转集（与预期清单互校，防口径漂移）。"""
	ids = []
	for line in DECISIONS.read_text(encoding="utf-8").splitlines():
		if not line.strip() or line.startswith("#"):
			continue
		parts = line.split("\t")
		if parts[0] == "quest_id":
			continue
		if parts[-2] == "ADOPT":
			ids.append(parts[0])
	if sorted(ids, key=int) != sorted(EXPECTED_FLIPS, key=int):
		print(f"ABORT: decisions ADOPT set {sorted(ids, key=int)} != expected {sorted(EXPECTED_FLIPS, key=int)}")
		sys.exit(1)
	return tuple(ids)


def patched_line(line: str, flips: tuple[str, ...]) -> str:
	qid = line.split("\t", 1)[0]
	if qid in flips:
		return NEW_ROW.format(qid=qid) + "\n"
	return line


def main() -> int:
	flips = flip_ids()
	baselines = [path.read_bytes() for path in
		(MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST)]
	if any(bytes_ != baselines[0] for bytes_ in baselines[1:]):
		print("ABORT: manifest copies diverge — lane storm, retry later")
		return 1
	lines = baselines[0].decode("utf-8").splitlines(keepends=True)
	flipped = []
	for index, line in enumerate(lines):
		qid = line.split("\t", 1)[0]
		if qid in flips:
			if not line.startswith(f"{qid}\tXML_RETENTION\t"):
				print(f"ABORT: quest {qid} is not XML_RETENTION: {line.strip()}")
				return 1
			flipped.append(qid)
		lines[index] = patched_line(line, flips)
	if sorted(flipped, key=int) != sorted(flips, key=int):
		print(f"ABORT: expected flips {sorted(flips, key=int)}, matched {sorted(flipped, key=int)}")
		return 1
	patched = "".join(lines)
	patched_bytes = patched.encode("utf-8")
	changed = sum(1 for old, new in zip(baselines[0].decode("utf-8").splitlines(),
		patched.splitlines()) if old != new)
	if changed != len(flips):
		print(f"ABORT: whipsaw guard — {changed} lines changed, expected {len(flips)}")
		return 1
	for path in (MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST):
		path.write_bytes(patched_bytes)

	if MANIFEST_AGENTS.exists():
		snapshot = MANIFEST_AGENTS.read_text(encoding="utf-8")
		snapshot_lines = [patched_line(line, flips) for line in snapshot.splitlines(keepends=True)]
		MANIFEST_AGENTS.write_text("".join(snapshot_lines), encoding="utf-8")
		print("snapshot copy patched")

	for qid in flips:
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

	print(f"flipped={len(flips)} rows; manifest×4(+snapshot) catalog×2 xml×2 each; "
		f"run verify_retirement.py next")
	return 0


if __name__ == "__main__":
	sys.exit(main())
