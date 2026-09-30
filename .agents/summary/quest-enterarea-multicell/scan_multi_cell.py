#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QE-109 证据扫描：多胞感官区别名的全部胞（只读）。

对 `RETAIL_ENTERAREA_ZONE_UNRESOLVED` 桶里的多胞别名，在真端世界文件
（`<真端根>/Map/Worlds/<dir>/world.xml`，UTF-16）中列出**同名感官区 NPC 的全部
`sensory_area` 胞**（多边形 + top/bottom），并给出 mapid 与建议区名。
只读：仅写自己的 TSV。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
WORLDS = Path(f"{REPO.parent / '58Server'}/Map/Worlds")
ID_MAPPINGS = REPO / 'src/main/resources/aion/definitions/compact/id-mappings.xml'
OUT = Path(__file__).with_name('multi-cell-scan.tsv')

ALIASES = (
	'IDEternity_War_ShugoSeller',
	'DF5_SensoryArea_65_Deva_Q15322b',
	'DF5_SensoryArea_65_Deva_Q15322d',
	'DF5_SensoryArea_65_Deva_Q15322f',
	'DF5_SensoryArea_65_Deva_Q15322h',
	'DF5_SensoryArea_65_Deva_Q15322j',
	'LF5_SensoryArea_65_Deva_Q25322b',
	'LF5_SensoryArea_65_Deva_Q25322d',
	'LF5_SensoryArea_65_Deva_Q25322f',
	'LF5_SensoryArea_65_Deva_Q25322h',
	'LF5_SensoryArea_65_Deva_Q25322j',
	# 早期以「球体 r=10」近似注册的 4 行：本片统一改回真端多边形形（单胞）。
	# The four rows registered earlier as r=10 spheres: normalised to the retail polygon shape here.
	'DF5_SensoryArea_Q16987',
	'LF5_SensoryArea_Q26987',
	'DF5_SensoryArea_Q30722',
	'LF5_SensoryArea_Q30772',
)


def read_text(path: Path) -> str:
	raw = path.read_bytes()
	encoding = 'utf-16' if raw[:2] in (b'\xff\xfe', b'\xfe\xff') else 'utf-8'
	return raw.decode(encoding, errors='ignore')


def map_ids() -> dict[str, str]:
	text = ID_MAPPINGS.read_text(encoding='utf-8')
	out = {}
	for match in re.finditer(r'<data id="(\d+)"[^>]*>([^<]+)</data>', text):
		out.setdefault(match.group(2).strip().lower(), match.group(1))
	return out


def cells(text: str, alias: str) -> list[tuple[list[tuple[str, str]], str, str]]:
	"""同名 NPC 的全部感官区胞（含无 sensory_area 的同名 NPC 不产生胞）。"""
	out = []
	for name in re.finditer(r'<name>' + re.escape(alias) + r'</name>', text, re.IGNORECASE):
		# NPC 块的边界：下一个 <npc 或 </npc> 之前的窗口
		tail = text[name.end():name.end() + 6000]
		block_start = tail.find('<sensory_area>')
		if block_start < 0:
			continue
		block_end = tail.find('</sensory_area>', block_start)
		if block_end < 0:
			continue
		block = tail[block_start:block_end]
		pts = re.findall(r'<x>([\d.\-]+)</x>\s*<y>([\d.\-]+)</y>', block)
		top = re.search(r'<top>([\d.\-]+)</top>', block)
		bottom = re.search(r'<bottom>([\d.\-]+)</bottom>', block)
		if pts and top and bottom:
			out.append((pts, top.group(1), bottom.group(1)))
	return out


def main() -> int:
	aliases = sys.argv[1:] or list(ALIASES)
	ids = map_ids()
	world_files = []
	for world_dir in sorted(p for p in WORLDS.iterdir() if p.is_dir()):
		for name in ('world.xml', 'world_N.xml'):
			path = world_dir / name
			if path.is_file():
				world_files.append((world_dir.name, path))
	rows = ['# QE-109 多胞感官区扫描 / multi-cell sensory-area scan',
		'# alias\tworld_dir\tmapid\tcell\ttop\tbottom\tpoints\tzone_name']
	for alias in aliases:
		needle = alias.lower()
		for world_dir, path in world_files:
			text = read_text(path)
			if needle not in text.lower():
				continue
			found = cells(text, alias)
			if not found:
				continue
			mapid = ids.get(world_dir.lower(), '?')
			zone_name = alias.upper() + '_' + mapid
			print(f'{alias}: world={world_dir} mapid={mapid} cells={len(found)} -> {zone_name}')
			for index, (pts, top, bottom) in enumerate(found):
				rows.append('\t'.join([alias, world_dir, mapid, str(index), top, bottom,
					';'.join(f'{x},{y}' for x, y in pts), zone_name]))
			break
		else:
			rows.append(f'{alias}\t-\t-\t-\t-\t-\t-\tNO_SENSORY_AREA')
			print(f'{alias}: NO_SENSORY_AREA')
	OUT.write_text('\n'.join(rows) + '\n', encoding='utf-8')
	print('written', OUT)
	return 0


if __name__ == '__main__':
	sys.exit(main())
