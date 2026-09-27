#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-52 普查：Advance 村守卫家族（LDF4_Advance_Village_Guard_{L,D}_{North,South,East,West}）
的 24 个真端 id 的模板名与刷怪坐标（用于 map 到 DD 家族名的双证据核验）。

只读扫描：npc 模板（name_desc/npc_id）+ spawns/**（spot 坐标），输出到 stdout。
"""
from __future__ import annotations

import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
NPCS = REPO / "src/main/resources/aion/data/static_data/npcs"
SPAWNS = REPO / "src/main/resources/aion/data/static_data/spawns"

GROUPS = {
	"L_North": [805269, 805270, 805271],
	"L_East": [805272, 805273, 805274],
	"L_West": [805275, 805276, 805277],
	"L_South": [805278, 805279, 805280],
	"D_North": [805282, 805283, 805284],
	"D_East": [805285, 805286, 805287],
	"D_West": [805288, 805289, 805290],
	"D_South": [805291, 805292, 805293],
}


def template_names() -> dict[int, str]:
	names: dict[int, str] = {}
	for path in sorted(NPCS.iterdir()):
		if path.suffix != ".xml":
			continue
		text = path.read_text(encoding="utf-8", errors="replace")
		for match in re.finditer(r"<npc_template ([^>]*?)>", text):
			attrs = dict(re.findall(r'([a-z_]+)="([^"]*)"', match.group(1)))
			npc_id = attrs.get("npc_id", "")
			if npc_id.isdigit():
				names[int(npc_id)] = attrs.get("name_desc", "")
	return names


def spawn_spots(wanted: set[int]) -> dict[int, list[tuple[float, float, float]]]:
	spots: dict[int, list[tuple[float, float, float]]] = {}
	for path in sorted(SPAWNS.rglob("*.xml")):
		text = path.read_text(encoding="utf-8", errors="replace")
		for match in re.finditer(
				r'<spawn npc_id="(\d+)">\s*<spot x="([-\d.]+)" y="([-\d.]+)" z="([-\d.]+)"', text):
			npc_id = int(match.group(1))
			if npc_id in wanted:
				spots.setdefault(npc_id, []).append(
					(float(match.group(2)), float(match.group(3)), float(match.group(4))))
	return spots


def main() -> int:
	wanted = {npc_id for ids in GROUPS.values() for npc_id in ids}
	names = template_names()
	spots = spawn_spots(wanted)
	for group, ids in GROUPS.items():
		print(f"== {group}")
		for npc_id in ids:
			print(f"   {npc_id} {names.get(npc_id, '?')} spots={len(spots.get(npc_id, []))} "
				f"{spots.get(npc_id, [])[:2]}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
