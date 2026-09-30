#!/usr/bin/env python3
"""P5：DataDriven 形状普查（quest_id / acquire / 步骤序列 / 步数）。

输入 = 入仓真端表 data_driven_quest.xml + 保留清单（生产宇宙）。
输出 = p5-datadriven-shape-census.tsv（1508 行）+ p5-datadriven-family.txt。
用法：python3 -B p5-datadriven-shape-census.py
"""
import os
import re
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/data_driven_quest.xml'
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml'
RETENTION = REPO / 'src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv'
OUT = REPO / '.agents/summary/scriptdll-quest-driver/p5-datadriven-shape-census.tsv'
FAMILY = REPO / '.agents/summary/scriptdll-quest-driver/p5-datadriven-family.txt'


def main():
	text = TABLE.read_text(encoding='utf-8')
	rows = {}
	for body in re.findall(r'<quest_data_driven>(.*?)</quest_data_driven>', text, re.S):
		m = re.search(r'<id>(\d+)</id>', body)
		if m:
			rows[int(m.group(1))] = body
	catalog = {int(v) for v in re.findall(r'<definition id="(\d+)"', CATALOG.read_text(encoding='utf-8'))}
	retired = set()
	for line in RETENTION.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		retired.add(int(parts[0]))
	family = sorted(set(rows) & (catalog | retired))

	def acquire(body):
		m = re.search(r'<category_acquire_>([^<]*)</category_acquire_>', body)
		return m.group(1).strip() if m else 'none'

	def steps(body):
		out = []
		for data in re.findall(r'<data>(.*?)</data>', body, re.S):
			c = re.search(r'<category_progress_>([^<]*)</category_progress_>', data)
			if c:
				out.append(c.group(1).strip().lower())
		return out

	out = ['# P5 DataDriven 形状普查基线（quest_id / acquire / 步骤序列 / 步数；禁止手改）',
		'# quest_id\tacquire\tstep_sequence\tstep_count']
	for quest_id in family:
		body = rows[quest_id]
		s = steps(body)
		out.append(f'{quest_id}\t{acquire(body)}\t{">".join(s) if s else "-"}\t{len(s)}')
	OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')
	FAMILY.write_text('\n'.join(map(str, family)) + '\n', encoding='utf-8')
	print(f'family={len(family)} census rows={len(out) - 2}')
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
