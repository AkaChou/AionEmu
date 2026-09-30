#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-6：把"真端无法表达任务刷怪"的降级行（14112/14123）从退役状态恢复回 XML。

背景：P0c-6 简报批原先把 23 行全部按 ADOPT_RETAIL 退役；复核发现其中 2 行的 NPC（14112 的交付 NPC
`Soul_Kato`=203195、14123 的击杀目标 `Peddler Hippola`=206360）**只由任务自身的 after-commit 刷怪边
进世界**（`spawns/**` 无静态 spot，真端家族表无刷怪列），退役会让任务不可完成 → 改判 KEEP_XML。
本脚本负责恢复生产资源（内容取自 git HEAD，不做任何 git 状态变更）：

1. 写回 `src/main/resources/aion/data/static_data/quest_definition/quests/<id>.xml`；
2. 把对应的 `<definition .../>` 行按 id 顺序插回 `quest_definition_catalog.xml`（行文本取自 HEAD）。

用法：python3 -B p0c6_restore_spawn_gap_rows.py [--dry-run]
"""
import os
import argparse
import re
import subprocess
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
QUESTS_REL = 'src/main/resources/aion/data/static_data/quest_definition/quests'
CATALOG_REL = 'src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml'
CATALOG = REPO / CATALOG_REL
IDS = (14112, 14123)


def head(rel):
	"""git HEAD 的文件内容；不存在返回 None。 / File content at git HEAD, or None."""
	proc = subprocess.run(['git', 'show', 'HEAD:' + rel], cwd=REPO, capture_output=True, text=True)
	return proc.stdout if proc.returncode == 0 else None


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('--dry-run', action='store_true')
	args = parser.parse_args()

	restored, skipped, missing = [], [], []
	for quest_id in IDS:
		rel = '%s/%d.xml' % (QUESTS_REL, quest_id)
		target = REPO / rel
		if target.is_file():
			skipped.append(quest_id)
			continue
		text = head(rel)
		if text is None:
			missing.append(quest_id)
			continue
		if not args.dry_run:
			target.write_text(text, encoding='utf-8')
		restored.append(quest_id)
	if missing:
		raise SystemExit('HEAD 缺少 XML：%s' % missing)

	catalog_text = CATALOG.read_text(encoding='utf-8')
	lines = catalog_text.splitlines(keepends=True)
	present = {int(m.group(1)) for m in
		(re.search(r'<definition id="(\d+)"', line) for line in lines) if m}
	added = []
	for quest_id in restored:
		if quest_id in present:
			continue
		entry = next((line for line in head(CATALOG_REL).splitlines(keepends=True)
			if re.search(r'<definition id="%d"' % quest_id, line)), None)
		if entry is None:
			raise SystemExit('HEAD 的 catalog 缺少 id=%d' % quest_id)
		index = next(index for index, line in enumerate(lines)
			if re.search(r'<definition id="(\d+)"', line)
			and int(re.search(r'<definition id="(\d+)"', line).group(1)) > quest_id)
		lines.insert(index, entry)
		added.append(quest_id)
	if not args.dry_run and added:
		CATALOG.write_text(''.join(lines), encoding='utf-8')

	entries = len(re.findall(r'<definition id="\d+"', CATALOG.read_text(encoding='utf-8')))
	print('restored_xml=%s skipped=%s catalog_added=%s catalog_entries=%d%s'
		% (restored, skipped, added, entries, ' (dry-run)' if args.dry_run else ''))
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
