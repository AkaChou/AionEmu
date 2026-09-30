#!/usr/bin/env python3
"""P5：把 owner=RETAIL_TABLE 的 DataDriven 任务（P5-1 Talk+hunt / P5-2 Talk+CollectItem 采纳行）壳 XML 退役。

DataDriven 的现役 XML 是纯 metadata 壳（无进度/节点/路由——本服从未有 DataDriven 运行时），
退役 = 删除壳 + 由真端驱动注入完整定义。

用法：python3 -B p5_retire_datadriven_xml.py [--dry-run]
"""
import os
import argparse
import hashlib
import re
import subprocess
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
RETENTION = REPO / 'src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv'
PROD_DIR = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quests'
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml'
EVIDENCE = REPO / '.agents/summary/scriptdll-quest-driver/p5-retired-datadriven-evidence.tsv'


def migrated_ids():
	ids = []
	for line in RETENTION.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		if len(parts) >= 3 and parts[1] == 'RETAIL_TABLE' and parts[2] == 'DataDriven':
			ids.append(parts[0])
	return ids


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('--dry-run', action='store_true')
	args = parser.parse_args()
	ids = migrated_ids()
	moved, kept, missing = [], [], []
	evidence = ['# P5 退役证据（DataDriven；P5-1 Talk+hunt + P5-2 Talk+CollectItem）：quest_id / 退役前壳 XML sha256 / 退役后位置']
	for quest_id in ids:
		source = PROD_DIR / f'{quest_id}.xml'
		rel = str(source.relative_to(REPO))
		if source.is_file():
			digest = hashlib.sha256(source.read_bytes()).hexdigest()
			if not args.dry_run:
				source.unlink()
			moved.append(quest_id)
			evidence.append(f'{quest_id}\t{digest}\tgit-history:{rel}')
		elif subprocess.run(['git', 'cat-file', '-e', f'HEAD:{rel}'], cwd=REPO,
				capture_output=True).returncode == 0:
			kept.append(quest_id)
			evidence.append(f'{quest_id}\t-\t(已退役)')
		else:
			missing.append(quest_id)
	text = CATALOG.read_text(encoding='utf-8')
	wanted = set(ids)
	removed = 0
	lines = []
	for line in text.splitlines(keepends=True):
		match = re.search(r'<definition id="(\d+)"', line)
		if match and match.group(1) in wanted:
			removed += 1
			continue
		lines.append(line)
	if not args.dry_run:
		CATALOG.write_text(''.join(lines), encoding='utf-8')
		EVIDENCE.write_text('\n'.join(evidence) + '\n', encoding='utf-8')
	# target/classes 对账（P5-4 教训）：Maven resources 只拷贝不删除，已删 XML 的陈旧副本会残留并让
	# 生产类目视图仍看到它们 → verifyProductionCoverage 误报 wrongOwner。按源目录差集清理。
	# target/classes reconciliation (P5-4 lesson): Maven resources copies but never deletes, so stale
	# copies of deleted XMLs keep the production view seeing them → spurious wrongOwner. Clean by diff.
	target_quests = REPO / 'target/classes/aion/data/static_data/quest/definitions/quests'
	if target_quests.is_dir() and not args.dry_run:
		src_dir = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quests'
		src_names = {p.name for p in src_dir.glob('*.xml')}
		stale = [p for p in target_quests.glob('*.xml') if p.name not in src_names]
		for p in stale:
			p.unlink()
		print(f'target/classes stale copies removed={len(stale)}')
	remaining = len(re.findall(r'<definition id="\d+"', ''.join(lines)))
	print(f'migrated={len(ids)} moved={len(moved)} already_retired={len(kept)} missing={len(missing)}')
	print(f'catalog entries removed={removed} remaining={remaining}')
	return 0 if not missing else 1


if __name__ == '__main__':
	raise SystemExit(main())
