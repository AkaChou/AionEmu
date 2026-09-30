#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""QE-109 翻转四件套：retention 双副本 + 删 XML + 删目录登记行 + drift 登记。指纹另行冻结。

Input: /tmp/dd-qe109.tsv（`-Dretail.dataDriven.equivOut` 重算结果）。
"""
from __future__ import annotations

import pathlib

REPO = next(p for p in pathlib.Path(__file__).resolve().parents if (p / "pom.xml").is_file())
IDS = (13962, 23962, 15322, 25322)
BASIS = 'qe-109-quest-multi-cell-sensory-decisions.tsv basis=DD_MULTI_CELL_SENSORY_AREA'
RATIOS = {13962: '27/4/4', 23962: '25/2/6', 15322: '22/13/124', 25322: '22/13/120'}
RETENTION = (
	REPO / 'src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv',
	REPO / 'src/test/resources/quest/retail-xml-retention.tsv',
)
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml'
QUESTS_DIR = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quests'
DRIFT = REPO / 'src/test/resources/quest/retail-data-driven-drift.tsv'


def flip_retention() -> None:
	for path in RETENTION:
		lines = path.read_text(encoding='utf-8').splitlines()
		out = []
		for line in lines:
			if line.startswith('#') or not line.strip():
				out.append(line)
				continue
			parts = line.split('\t')
			if int(parts[0]) in IDS:
				parts[1] = 'RETAIL_TABLE'
				parts[2] = 'DataDriven'
				parts[3] = 'OK'
				parts[4] = f'retired-xml-in-git-history {BASIS} shared/onlyXml/onlyRetail={RATIOS[int(parts[0])]}'
			out.append('\t'.join(parts))
		path.write_text('\n'.join(out) + '\n', encoding='utf-8')
		print(f'retention flipped: {path.relative_to(REPO)}')


def drop_catalog_rows() -> None:
	text = CATALOG.read_text(encoding='utf-8').splitlines()
	kept = [line for line in text if not any(f'<definition id="{qid}"' in line for qid in IDS)]
	print(f'catalog rows removed: {len(text) - len(kept)}')
	CATALOG.write_text('\n'.join(kept) + '\n', encoding='utf-8')


def drop_xml() -> None:
	for qid in IDS:
		path = QUESTS_DIR / f'{qid}.xml'
		if path.exists():
			path.unlink()
			print(f'xml removed: {path.name}')


def update_drift() -> None:
	dump = pathlib.Path('/tmp/dd-qe109.tsv').read_text(encoding='utf-8').splitlines()
	new_rows = {int(line.split('\t')[0]): line for line in dump
		if line[:1].isdigit() and int(line.split('\t')[0]) in IDS}
	adopted = next(line for line in dump if line.startswith('# ADOPTED'))
	lines = DRIFT.read_text(encoding='utf-8').splitlines()
	out = []
	for line in lines:
		if line.startswith('# ADOPTED'):
			out.append(adopted)
			continue
		if line[:1].isdigit() and int(line.split('\t')[0]) in IDS:
			out.append(new_rows[int(line.split('\t')[0])])
			continue
		out.append(line)
	DRIFT.write_text('\n'.join(out) + '\n', encoding='utf-8')
	print('drift updated:', adopted)


if __name__ == '__main__':
	flip_retention()
	drop_catalog_rows()
	drop_xml()
	update_drift()
