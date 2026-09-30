#!/usr/bin/env python3
"""P3：把 owner=RETAIL_TABLE 的 SimpleSerialHunt 任务从生产 XML 定义中退役。

做两件事（都可用 git 回退，不做任何提交）：
1) 删除生产资源 `quest/definitions/quests/<id>.xml`（不保留测试作用域副本，历史由 git 承担）；
2) 从生产白名单 `quest_definition_catalog.xml` 移除对应 `<definition>` 行。

用法：python3 -B p3_retire_serial_hunt_xml.py [--dry-run]
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
EVIDENCE = REPO / '.agents/summary/scriptdll-quest-driver/p3-retired-serial-hunt-evidence.tsv'


def migrated_ids():
	ids = []
	for line in RETENTION.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		if len(parts) >= 3 and parts[1] == 'RETAIL_TABLE' and parts[2] == 'SimpleSerialHunt':
			ids.append(parts[0])
	return ids


def sha256(path):
	return hashlib.sha256(path.read_bytes()).hexdigest()


def in_git_history(rel):
	return subprocess.run(['git', 'cat-file', '-e', f'HEAD:{rel}'], cwd=REPO,
		capture_output=True).returncode == 0


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('--dry-run', action='store_true')
	args = parser.parse_args()

	ids = migrated_ids()
	moved, kept, missing = [], [], []
	evidence = ['# P3 退役证据（SimpleSerialHunt）：quest_id / 退役前生产 XML sha256 / 退役后位置']
	for quest_id in ids:
		source = PROD_DIR / f'{quest_id}.xml'
		rel = str(source.relative_to(REPO))
		if source.is_file():
			digest = sha256(source)
			if not args.dry_run:
				source.unlink()
			moved.append(quest_id)
			evidence.append(f'{quest_id}\t{digest}\tgit-history:{rel}')
		elif in_git_history(rel):
			kept.append(quest_id)
			evidence.append(f'{quest_id}\t-\t(已退役，内容在 git 历史里)')
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

	remaining = len(re.findall(r'<definition id="\d+"', ''.join(lines)))
	print(f'migrated={len(ids)} moved={len(moved)} already_retired={len(kept)} missing={len(missing)}')
	print(f'catalog entries removed={removed} remaining={remaining}')
	if missing:
		print('MISSING:', missing[:20])
	return 0 if not missing else 1


if __name__ == '__main__':
	raise SystemExit(main())
