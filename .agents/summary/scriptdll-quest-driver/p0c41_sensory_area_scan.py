#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-41：EA 区名桶「感官区 NPC」证据扫描（IDInfinity 判例的批量化）。

对 `RETAIL_ENTERAREA_ZONE_UNRESOLVED` 桶里每个未解析别名，在真端世界文件
（`/Users/mc/IdeaProjects/58Server/Map/Worlds/<dir>/world.xml`，UTF-16）中找同名**感官区 NPC**
（`<name><别名></name>` + `<sensory_area>` 多边形 + top/bottom），再经
`aion/definitions/compact/id-mappings.xml` 把世界目录名换算成 mapid，产出可直接注册进
`zones_quest.xml` 的 POLYGON/SUB 区条目草稿。

输出：stdout 摘要 + `.agents/summary/scriptdll-quest-driver/p0c41-sensory-area-scan.tsv`
（列：alias, world_dir, mapid, points, top, bottom, zone_name, status）。

For each unresolved alias in the enter-area zone bucket this scans the retail world files for a
same-named sensory-area NPC (its `<sensory_area>` polygon plus top/bottom), converts the world
directory into a mapid through `id-mappings.xml`, and prints a ready-to-register POLYGON/SUB zone
draft. Read-only: it only writes its own TSV.

用法 / usage: python3 -B p0c41_sensory_area_scan.py [alias ...]
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
WORLDS = Path('/Users/mc/IdeaProjects/58Server/Map/Worlds')
ID_MAPPINGS = REPO / 'src/main/resources/aion/definitions/compact/id-mappings.xml'
OUT = REPO / '.agents/summary/scriptdll-quest-driver/p0c41-sensory-area-scan.tsv'

# 区名桶里当前未解析的别名（成对 1xxxx/2xxxx 只扫一次）。
# The aliases currently unresolved in the zone-name bucket (paired ids scanned once).
ALIASES = (
	'IDEternity_War_ShugoSeller',
	'DF5_SensoryArea_65_Deva_Q15322b',
	'IDEternity_02_SensoryArea_Q16820a',
	'IDLDF5_Fortress_War_SensoryArea_Q17500a',
	'IDAbRe_Core_03_SensoryArea_Q17525',
	'IDF6_LF1_SensoryArea_Q18996',
	'DF5_SensoryArea_Q20501a',
	'DF5_SensoryArea_Q20503a',
	'DF5_SensoryArea_Q20507a',
)


def read_text(path: Path) -> str:
	raw = path.read_bytes()
	encoding = 'utf-16' if raw[:2] in (b'\xff\xfe', b'\xfe\xff') else 'utf-8'
	return raw.decode(encoding, errors='ignore')


def map_ids() -> dict[str, str]:
	"""id-mappings.xml: 世界目录名（小写） -> mapid 字符串（世界条目带属性，正则须容属性）。"""
	text = ID_MAPPINGS.read_text(encoding='utf-8')
	out = {}
	for match in re.finditer(r'<data id="(\d+)"[^>]*>([^<]+)</data>', text):
		out.setdefault(match.group(2).strip().lower(), match.group(1))
	return out


def sensory_area(text: str, alias: str) -> tuple[list[tuple[str, str]], str, str] | None:
	"""别名感官区 NPC 的 (多边形点, top, bottom)；同名前缀（SPG_ 领地）不算证据。"""
	for match in re.finditer(r'<name>' + re.escape(alias) + r'</name>', text, re.IGNORECASE):
		tail = text[match.end():match.end() + 4000]
		block_start = tail.find('<sensory_area>')
		if block_start < 0:
			continue
		block_end = tail.find('</sensory_area>', block_start)
		if block_end < 0:
			continue
		block = tail[block_start:block_end]
		points = re.findall(r'<x>([\d.\-]+)</x>\s*<y>([\d.\-]+)</y>', block)
		top = re.search(r'<top>([\d.\-]+)</top>', block)
		bottom = re.search(r'<bottom>([\d.\-]+)</bottom>', block)
		if points and top and bottom:
			return points, top.group(1), bottom.group(1)
	return None


def main() -> int:
	aliases = sys.argv[1:] or list(ALIASES)
	ids = map_ids()
	world_files = []
	for world_dir in sorted(p for p in WORLDS.iterdir() if p.is_dir()):
		for name in ('world.xml', 'world_N.xml'):
			path = world_dir / name
			if path.is_file():
				world_files.append((world_dir.name, path))
	print(f'worlds with world.xml: {len(world_files)} (map-id index: {len(ids)})')
	rows = ['# P0c-41 感官区证据扫描 / sensory-area evidence scan',
		'# alias\tworld_dir\tmapid\tpoints\ttop\tbottom\tzone_name\tpolygon\tstatus']
	for alias in aliases:
		needle = alias.lower()
		found = None
		for world_dir, path in world_files:
			text = read_text(path)
			if needle not in text.lower():
				continue
			area = sensory_area(text, alias)
			if area is not None:
				found = (world_dir, path.name, area)
				break
		if found is None:
			rows.append(f'{alias}\t-\t-\t0\t-\t-\t-\t-\tNO_SENSORY_AREA'
				'\t同名 NPC 无 sensory_area 多边形 / no same-named sensory-area polygon')
			print(f'{alias}: NO_SENSORY_AREA')
			continue
		world_dir, _name, (points, top, bottom) = found
		mapid = ids.get(world_dir.lower(), '?')
		# 区名 = 别名大写 + mapid 后缀（遗留任务区同名惯例；别名本身即真端感官区 NPC 名）。
		# Zone name = the uppercased alias plus the mapid suffix (the legacy task-zone convention;
		# the alias itself is the retail sensory-area npc name).
		zone_name = alias.upper() + '_' + mapid
		polygon = ';'.join(f'{x},{y}' for x, y in points)
		rows.append(f'{alias}\t{world_dir}\t{mapid}\t{len(points)}\t{top}\t{bottom}\t{zone_name}\t{polygon}\tOK')
		print(f'{alias}: world={world_dir} mapid={mapid} points={len(points)} top={top} bottom={bottom}'
			f' -> {zone_name}')
	OUT.write_text('\n'.join(rows) + '\n', encoding='utf-8')
	print('written', OUT.name)
	return 0


if __name__ == '__main__':
	sys.exit(main())
