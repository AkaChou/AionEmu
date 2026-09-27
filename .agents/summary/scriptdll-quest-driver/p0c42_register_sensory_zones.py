#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-42：感官区注册 + DD 别名映射（EA 区名桶 wave8，IDInfinity 判例批量化）。

输入 = `p0c41-sensory-area-scan.tsv`（真端世界文件里感官区 NPC 的多边形 + top/bottom + mapid）。
本脚本对**证据唯一**的别名：
  1. 在 `zones_quest.xml`（src + target/classes）追加 POLYGON/SUB 区条目（区名 = 别名大写 + `_<mapid>`）；
  2. 在 `quest_enterarea_zone_resolution.tsv`（src + target/classes）追加 (quest_id, alias, zone_name)
     行，source=`retail-world-sensory-area`（p0c41 扫描证据）。
多胞别名（同一 NPC 名在真端世界文件里对应多个互异多边形）**不注册**：单多边形区遮不住
"任一格进入"语义，错格注册 = 静默死边，按"无据不猜"留拒（13962/23962 六格、15322/25322 三格）。

Reads the p0c41 scan and, for aliases whose evidence is unique, appends the POLYGON/SUB zone to
`zones_quest.xml` (src + target/classes) and the (quest_id, alias, zone_name) mapping rows to
`quest_enterarea_zone_resolution.tsv` (src + target/classes, source=`retail-world-sensory-area`).
Multi-cell aliases (one npc name mapping to several distinct retail polygons) are deliberately NOT
registered: a single-polygon zone cannot express "enter any of them", and registering the wrong
cell would be a silent dead edge — they stay rejected under the no-guessing rule.

用法 / usage: python3 -B p0c42_register_sensory_zones.py [--check]
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
SCAN = REPO / ".agents/summary/scriptdll-quest-driver/p0c41-sensory-area-scan.tsv"
ZONES = (
	REPO / "src/main/resources/aion/data/static_data/zones/zones_quest.xml",
	REPO / "target/classes/aion/data/static_data/zones/zones_quest.xml",
)
ALIASES = (
	REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_enterarea_zone_resolution.tsv",
	REPO / "target/classes/aion/data/static_data/quest_retail/quest_enterarea_zone_resolution.tsv",
)
EVIDENCE = "retail-world-sensory-area"

# 多胞别名：真端世界里同名 NPC 有多个互异多边形 → 不注册（见模块注释）。
# Multi-cell aliases: the same npc name owns several distinct retail polygons — not registered.
MULTI_CELL = {
	"IDEternity_War_ShugoSeller": 6,
	"DF5_SensoryArea_65_Deva_Q15322b": 3,
}

# 别名 → 需要映射的 quest id（成对 1xxxx/2xxxx 共享别名）。
# Alias to the quest ids that need a mapping row (the paired ids share the alias).
QUEST_IDS = {
	"IDEternity_02_SensoryArea_Q16820a": ("16820", "26820"),
	"IDLDF5_Fortress_War_SensoryArea_Q17500a": ("17500", "27500"),
	"IDAbRe_Core_03_SensoryArea_Q17525": ("17525", "27525"),
	"IDF6_LF1_SensoryArea_Q18996": ("18996", "28996"),
	"DF5_SensoryArea_Q20501a": ("20501",),
	"DF5_SensoryArea_Q20503a": ("20503",),
	"DF5_SensoryArea_Q20507a": ("20507",),
}


def scan_rows() -> dict[str, dict[str, str]]:
	rows = {}
	for line in SCAN.read_text(encoding="utf-8").splitlines():
		if not line.strip() or line.startswith("#"):
			continue
		parts = line.split("\t")
		if len(parts) >= 9 and parts[8] == "OK":
			rows[parts[0]] = {"mapid": parts[2], "top": parts[4], "bottom": parts[5],
				"zone": parts[6], "polygon": parts[7]}
	return rows


def zone_block(zone_name: str, mapid: str, top: str, bottom: str, polygon: str) -> str:
	points = "\n".join(f'\t\t\t<point x="{x}" y="{y}"/>'
		for x, y in (pair.split(",") for pair in polygon.split(";")))
	return (f'\t<zone mapid="{mapid}" name="{zone_name}" area_type="POLYGON" zone_type="SUB">\n'
		f'\t\t<points bottom="{bottom}" top="{top}">\n{points}\n\t\t</points>\n\t</zone>\n')


def main() -> int:
	check_only = "--check" in sys.argv[1:]
	rows = scan_rows()
	selected = {alias: data for alias, data in rows.items() if alias in QUEST_IDS}
	missing = sorted(set(QUEST_IDS) - set(selected))
	if missing:
		print(f"ABORT: scan rows missing for {missing}")
		return 1
	blocks = "".join(zone_block(data["zone"], data["mapid"], data["top"], data["bottom"],
		data["polygon"]) for _, data in sorted(selected.items()))
	for path in ZONES:
		text = path.read_text(encoding="utf-8")
		if "</zones>" not in text or text.count("</zones>") != 1:
			print(f"ABORT: {path.name}: zones root tag not unique")
			return 1
		for data in selected.values():
			if f'name="{data["zone"]}"' in text:
				print(f"ABORT: {path.name}: zone {data['zone']} already registered")
				return 1
		if not check_only:
			path.write_text(text.replace("</zones>", blocks + "</zones>"), encoding="utf-8")
		print(f"{path.name}: +{len(selected)} zones{'' if not check_only else ' (check only)'}")
	rows_added = 0
	for path in ALIASES:
		lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
		existing = {line.split("\t")[0] for line in lines if line.strip() and not line.startswith("#")}
		additions = []
		for alias, data in sorted(selected.items()):
			for quest_id in QUEST_IDS[alias]:
				if quest_id in existing:
					print(f"ABORT: {path.name}: quest {quest_id} already has a mapping row")
					return 1
				additions.append(f"{quest_id}\t{alias}\t{data['zone']}\t{EVIDENCE}\n")
				rows_added += 1
		if not lines[-1].endswith("\n"):
			additions.insert(0, "\n")
		if not check_only:
			path.write_text("".join(lines) + "".join(additions), encoding="utf-8")
		print(f"{path.name}: +{len(additions)} mapping rows{'' if not check_only else ' (check only)'}")
	for alias, cells in sorted(MULTI_CELL.items()):
		print(f"DEFERRED {alias}: {cells} distinct retail cells (multi-cell alias, no guessing)")
	print(f"OK: {len(selected)} zones, {rows_added} mapping rows, {len(MULTI_CELL)} deferred aliases")
	return 0


if __name__ == "__main__":
	sys.exit(main())
