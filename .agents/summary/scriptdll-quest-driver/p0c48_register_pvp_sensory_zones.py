#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-48：PVP 链批的 EA 区名注册（15673/25673 感官区别名）。

输入 = `p0c41-sensory-area-scan.tsv` 的 OK 行（真端世界文件里同名感官区 NPC 的唯一多边形，
mapid 与 quest.xml 的 quest_permitted_worlds 一致：15673→220110000 / 25673→210100000）。
对这两个别名：
  1. 在 `zones_quest.xml`（src + target/classes）追加 POLYGON/SUB 区条目（区名 = 别名大写 + `_<mapid>`）；
  2. 在 `quest_enterarea_zone_resolution.tsv`（src + target/classes）追加 (quest_id, alias, zone_name)
     行，source=`retail-world-sensory-area`。

幂等守卫：区名已存在或 quest 已有映射行即 ABORT（不重复注册）；只处理本片两个别名。

Registers the two sensory-area zones of the in-chain PVP batch (the 15673/25673 aliases) from the
p0c41 scan's unique-polygon evidence: appends the POLYGON/SUB zone to `zones_quest.xml` and the
alias mapping rows to `quest_enterarea_zone_resolution.tsv` (src + target/classes). Guards abort
when the zone name or the quest mapping already exists.

用法 / usage: python3 -B p0c48_register_pvp_sensory_zones.py [--check]
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

# 别名 → 需要映射的 quest id（1xxxx/2xxxx 成对行各一条映射）。
# Alias to the quest ids that need a mapping row (the paired rows get one row each).
QUEST_IDS = {
	"DF6_SensoryArea_Q15673": ("15673",),
	"LF6_SensoryArea_Q25673": ("25673",),
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
	print(f"OK: {len(selected)} zones, {rows_added} mapping rows")
	return 0


if __name__ == "__main__":
	sys.exit(main())
