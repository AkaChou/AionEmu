#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""常态化修复：并行会话清单重倾把本会话的采纳行打回 XML_RETENTION 时重翻（幂等）。

并行 DD 会话的 retail-xml-retention.tsv 生成器周期性全量重倾（其模型里这些行仍是
XML_RETENTION / FAMILY_PENDING:DataDriven），而对应 XML 已退役删除 → 生产视图 missing、
`retentionManifestMatchesDriverOwnership` 与契约门同时转红。本脚本按波次把本会话已退役的
9 行写回 RETAIL_TABLE（每行各自的证据串保持不变），四副本同步；行已是 RETAIL_TABLE 时
按幂等跳过。

Routine repair: the concurrent DD session's retention-manifest writer periodically re-dumps the
whole file and stamps this session's adopted rows back to XML_RETENTION / FAMILY_PENDING while
their XML shells are already deleted — the production view then misses quests and both the
retention gate and the contract gate turn red. This script re-applies the RETAIL_TABLE flip for
this session's nine retired rows, one evidence string per wave, across all four manifest copies;
rows already carrying RETAIL_TABLE are skipped (idempotent).

用法 / usage: python3 -B p0c40_reflip_my_wave_rows.py [--check]
翻完立即跑 RetailDataDrivenGateTest（抢在下一轮重倾之前）。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]

# 波次 → (ids, 证据串) / wave -> (ids, evidence string)
WAVES = {
	"wave4-enterarea-zone-decisions.tsv basis=DD_ENTERAREA_ZONE_RESOLUTION": ("16800", "26800"),
	"wave5-idinfinity-zone-decisions.tsv basis=DD_ENTERAREA_ZONE_RESOLUTION": ("18252", "18253"),
	"wave6-narrow-rider-decisions.tsv basis=DD_NARROW_RIDER_CHAIN": ("15613", "25084"),
	"wave7-narrow-itemplay-decisions.tsv basis=DD_NARROW_ITEMPLAY_RIDER": ("15000", "15680", "25023"),
}
EVIDENCE_PREFIX = "retired-xml-in-git-history "
COPIES = (
	"src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
	"src/test/resources/quest/retail-xml-retention.tsv",
	"target/classes/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
	"target/test-classes/quest/retail-xml-retention.tsv",
)


def main() -> int:
	check_only = "--check" in sys.argv[1:]
	owner = {}
	for evidence, ids in WAVES.items():
		for quest_id in ids:
			owner[quest_id] = evidence
	problems = []
	for rel in COPIES:
		path = REPO / rel
		lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
		flipped = []
		for index, line in enumerate(lines):
			quest_id = line.split("\t", 1)[0]
			if quest_id not in owner:
				continue
			if line.startswith(f"{quest_id}\tRETAIL_TABLE\t"):
				if EVIDENCE_PREFIX + owner[quest_id] not in line:
					problems.append(f"{rel}:{quest_id}: RETAIL_TABLE 但证据串不符 / evidence mismatch")
				continue
			lines[index] = (f"{quest_id}\tRETAIL_TABLE\tDataDriven\tOK\t"
				f"{EVIDENCE_PREFIX}{owner[quest_id]}\n")
			flipped.append(quest_id)
		if flipped and not check_only:
			path.write_text("".join(lines), encoding="utf-8")
		print(f"{rel}: flipped {len(flipped)} {sorted(flipped)}")
	if problems:
		print("PROBLEMS: " + "; ".join(problems))
		return 1
	print("OK: " + ("check-only" if check_only else "re-flipped") + " 9 rows across 4 copies")
	return 0


if __name__ == "__main__":
	sys.exit(main())
