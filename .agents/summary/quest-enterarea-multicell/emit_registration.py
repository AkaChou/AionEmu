#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QE-109：把多胞感官区扫描结果写进生产注册表（幂等）。

输入：`multi-cell-scan.tsv`（真端世界文件逐胞证据）。
输出：
  1. `zones_quest.xml` 追加 POLYGON/SUB 区条目——一个区名一条，内含该别名的**全部胞** `<points>`；
  2. `quest_enterarea_zone_resolution.tsv`（生产 + 门测试双副本）追加别名行。

用法 / usage: python3 -B emit_registration.py [--check]
"""
from __future__ import annotations

import sys
from collections import OrderedDict
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
SCAN = Path(__file__).with_name('multi-cell-scan.tsv')
ZONES = REPO / 'src/main/resources/aion/data/static_data/zones/zones_quest.xml'
ALIAS_TABLES = (
	REPO / 'src/main/resources/quest/quest_enterarea_zone_resolution.tsv',
	REPO / 'src/test/resources/quest/quest_enterarea_zone_resolution.tsv',
)
SOURCE = 'retail-world-sensory-area'
# 早期（a2306c8e2）用「嗅感区 NPC 出生点 + r=10 球体」近似注册的 4 行：本片按真端 <sensory_area>
# 多边形改回 POLYGON（单胞）。只改几何，不改名字与解析表。
# The four rows registered in a2306c8e2 as "spawn point + r=10 sphere" approximations: normalised here
# to the retail <sensory_area> polygon (single cell). Names and the resolution table stay unchanged.
LEGACY_SPHERE_ZONES = (
	'DF5_SENSORYAREA_Q16987_220080000',
	'LF5_SENSORYAREA_Q26987_210070000',
	'DF5_SENSORYAREA_Q30722_220080000',
	'LF5_SENSORYAREA_Q30772_210070000',
)
LEGACY_SPHERE_ALIASES = (
	'DF5_SensoryArea_Q16987',
	'LF5_SensoryArea_Q26987',
	'DF5_SensoryArea_Q30722',
	'LF5_SensoryArea_Q30772',
)
# 本次解锁的行：quest -> [(真端别名, 目标区名)]。区名由扫描器按「别名大写 + _mapid」惯例给出。
QUESTS = OrderedDict((
	(13962, ('IDEternity_War_ShugoSeller',)),
	(23962, ('IDEternity_War_ShugoSeller',)),
	(15322, tuple(f'DF5_SensoryArea_65_Deva_Q15322{c}' for c in 'bdfhj')),
	(25322, tuple(f'LF5_SensoryArea_65_Deva_Q25322{c}' for c in 'bdfhj')),
))


def read_scan() -> tuple[OrderedDict[str, list[tuple[str, str, list[tuple[str, str]]]]], dict[str, str]]:
	"""zone_name -> [(top, bottom, points)]，以及 alias -> zone_name。"""
	cells: OrderedDict[str, list[tuple[str, str, list[tuple[str, str]]]]] = OrderedDict()
	alias_zone: dict[str, str] = {}
	for line in SCAN.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		if len(parts) < 8 or parts[7] == 'NO_SENSORY_AREA':
			continue
		alias, world_dir, mapid, _cell, top, bottom, points, zone_name = parts[:8]
		alias_zone[alias] = zone_name
		ring = [tuple(pair.split(',')) for pair in points.split(';') if pair]
		cells.setdefault(zone_name, []).append((top, bottom, ring))
	return cells, alias_zone


def zone_snippet(zone_name: str, mapid: str, cell_list: list[tuple[str, str, list[tuple[str, str]]]],
		indent: str = '\t') -> str:
	out = [f'{indent}<zone mapid="{mapid}" name="{zone_name}" area_type="POLYGON" zone_type="SUB">']
	for top, bottom, ring in cell_list:
		out.append(f'{indent}\t<points bottom="{float(bottom):.6f}" top="{float(top):.6f}">')
		for x, y in ring:
			out.append(f'{indent}\t\t<point x="{float(x):.6f}" y="{float(y):.6f}"/>')
		out.append(f'{indent}\t</points>')
	out.append(f'{indent}</zone>')
	return '\n'.join(out)


def patch_zones(cells, alias_zone, check: bool) -> str:
	text = ZONES.read_text(encoding='utf-8')
	wanted = {alias_zone[a] for aliases in QUESTS.values() for a in aliases}
	blocks = []
	for zone_name, cell_list in cells.items():
		if zone_name not in wanted:
			continue
		aliases = [a for a, z in alias_zone.items() if z == zone_name]
		mapid = zone_name.rsplit('_', 1)[1]
		blocks.append(f'\t<!-- 感官区（真端世界文件 {len(cell_list)} 胞）：{", ".join(aliases)} -->')
		blocks.append(zone_snippet(zone_name, mapid, cell_list))
	block = '\t<!-- QE-109 多胞感官区：区名 = 真端别名大写 + _mapid，逐胞 <points>（进入任一胞即算进入） -->\n' \
	+ '\n'.join(blocks) + '\n'
	if all(zone_name in text for zone_name in wanted):
		return 'zones: already present'
	appended = len(blocks) // 2
	if check:
		return 'zones: WOULD APPEND ' + str(appended)
	assert text.rstrip().endswith('</zones>')
	updated = text.rstrip()[: -len('</zones>')] + block + '</zones>\n'
	ZONES.write_text(updated, encoding='utf-8')
	return f'zones: appended {appended}'


def rewrite_legacy_sphere_zones(cells, alias_zone, check: bool) -> str:
	"""把 4 行 r=10 球体近似换成真端多边形（名字不变）。 / Rewrites the four sphere approximations."""
	text = ZONES.read_text(encoding='utf-8')
	changed = []
	for zone_name, alias in zip(LEGACY_SPHERE_ZONES, LEGACY_SPHERE_ALIASES):
		start = text.find(f'name="{zone_name}"')
		if start < 0:
			continue
		open_tag = text.rfind('<zone ', 0, start)
		close = text.find('</zone>', start)
		if open_tag < 0 or close < 0:
			continue
		if 'area_type="POLYGON"' in text[open_tag:close]:
			continue
		line_start = text.rfind('\n', 0, open_tag) + 1
		indent = text[line_start:open_tag]
		mapid = zone_name.rsplit('_', 1)[1]
		block = zone_snippet(zone_name, mapid, cells[zone_name], indent) + '\n'
		text = text[:line_start] + block + text[close + len('</zone>\n'):]
		changed.append(zone_name)
	if not changed:
		return 'legacy sphere rows: unchanged'
	if check:
		return 'legacy sphere rows: WOULD REWRITE ' + ','.join(changed)
	ZONES.write_text(text, encoding='utf-8')
	return 'legacy sphere rows rewritten: ' + ','.join(changed)


def patch_alias_tables(cells, alias_zone, check: bool) -> str:
	rows = []
	for quest_id, aliases in QUESTS.items():
		for alias in aliases:
			zone_name = alias_zone[alias]
			rows.append(f'{quest_id}\t{alias}\t{zone_name}\t{SOURCE}')
	report = []
	for table in ALIAS_TABLES:
		text = table.read_text(encoding='utf-8')
		missing = [row for row in rows if row.split('\t')[1] not in text]
		if not missing:
			report.append(f'{table.parent.name}: rows present')
			continue
		if check:
			report.append(f'{table.parent.name}: WOULD ADD {len(missing)}')
			continue
		updated = text.rstrip('\n') + '\n' + '\n'.join(missing) + '\n'
		table.write_text(updated, encoding='utf-8')
		report.append(f'{table.parent.name}: added {len(missing)}')
	return 'aliases ' + '; '.join(report)


def main() -> int:
	check = '--check' in sys.argv
	cells, alias_zone = read_scan()
	for quest_id, aliases in QUESTS.items():
		missing = [a for a in aliases if a not in alias_zone]
		if missing:
			raise SystemExit(f'quest {quest_id}: alias without scan evidence: {missing}')
	print(rewrite_legacy_sphere_zones(cells, alias_zone, check))
	print(patch_zones(cells, alias_zone, check))
	print(patch_alias_tables(cells, alias_zone, check))
	return 0


if __name__ == '__main__':
	sys.exit(main())
