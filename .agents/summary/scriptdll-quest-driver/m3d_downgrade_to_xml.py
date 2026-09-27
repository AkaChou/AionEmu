#!/usr/bin/env python3
"""M3-d：把"真端合成无法表达客户端契约"的任务降级回 XML 所有权。

做三件事（都可用 git 回退，不做任何提交）：
1) 从 git 历史复原生产 XML（`git show HEAD:<path>`；退役 = 删除，历史在 git 里）；
2) 把该任务的原 `<definition>` 行按 id 顺序插回 `quest_definition_catalog.xml`；
3) 保留清单三份副本（.agents / src/test/resources / src/main/resources）里该行 owner 改
   `XML_RETENTION`、reason 改 `SEMANTIC_GAP:<CODE>`、evidence 记为降级原因。

用法：python3 -B m3d_downgrade_to_xml.py <code> <quest_id> [<quest_id> ...] [--dry-run]
"""
import argparse
import pathlib
import re
import subprocess

REPO = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
XML_DIR = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quests'
CATALOG = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml'
LEDGERS = [
	REPO / '.agents/summary/scriptdll-quest-driver/retail-xml-retention.tsv',
	REPO / 'src/test/resources/quest/retail-xml-retention.tsv',
	REPO / 'src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv',
]
# 持久降级登记：build_retention_list.py 重新生成清单时以本文件为最高优先级，
# 否则"真端表族 + 驱动已实现"的行会被重新算回 RETAIL_TABLE（虚报已由真端驱动）。
# Persistent downgrade registry; the retention generator consults it first.
DOWNGRADE_REGISTRY = REPO / '.agents/summary/scriptdll-quest-driver/m3d-downgraded-quests.tsv'
REGISTRY_HEADER = ('# 真端语义缺口降级登记（quest_id, code, note）\n'
	'# 由 m3d_downgrade_to_xml.py 维护；build_retention_list.py 最高优先级消费。\n')


def git_show(rel):
	proc = subprocess.run(['git', 'show', f'HEAD:{rel}'], cwd=REPO, capture_output=True)
	if proc.returncode != 0:
		raise SystemExit(f'git show failed for {rel}')
	return proc.stdout


def catalog_line(quest_id):
	text = git_show('src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml')
	m = re.search(rb'.*<definition id="%d" .*/>' % quest_id, text)
	if not m:
		raise SystemExit(f'no catalog line for {quest_id}')
	return m.group(0).decode('utf-8') + '\n'


def insert_catalog_line(line, quest_id, dry_run):
	lines = CATALOG.read_text(encoding='utf-8').splitlines(keepends=True)
	if any(f'id="{quest_id}"' in l for l in lines):
		return False
	position = len(lines) - 1
	for index, existing in enumerate(lines):
		m = re.search(r'<definition id="(\d+)"', existing)
		if m and int(m.group(1)) > quest_id:
			position = index
			break
	lines.insert(position, line)
	if not dry_run:
		CATALOG.write_text(''.join(lines), encoding='utf-8')
	return True


def downgrade_ledger(quest_id, code, reason_note, dry_run):
	changed = 0
	for ledger in LEDGERS:
		if not ledger.is_file():
			continue
		lines = ledger.read_text(encoding='utf-8').splitlines()
		out = []
		for entry in lines:
			if entry.startswith('#') or not entry.strip():
				out.append(entry)
				continue
			parts = entry.split('\t')
			if int(parts[0]) == quest_id and parts[1] == 'RETAIL_TABLE':
				parts[1] = 'XML_RETENTION'
				parts[3] = f'SEMANTIC_GAP:{code}'
				parts[4] = f'downgraded:{reason_note}'
				changed += 1
			out.append('\t'.join(parts))
		if not dry_run:
			ledger.write_text('\n'.join(out) + '\n', encoding='utf-8')
	return changed


def register_downgrade(quest_id, code, reason_note, dry_run):
	"""把降级事实写进持久登记（幂等；已有行不覆盖）。
	Upsert the downgrade fact into the persistent registry (idempotent).
	"""
	rows = {}
	if DOWNGRADE_REGISTRY.is_file():
		for entry in DOWNGRADE_REGISTRY.read_text(encoding='utf-8').splitlines():
			if entry.startswith('#') or not entry.strip():
				continue
			parts = entry.split('\t')
			rows[int(parts[0])] = (parts[1], parts[2] if len(parts) > 2 else '')
	if quest_id in rows:
		return False
	rows[quest_id] = (code, reason_note)
	if not dry_run:
		with DOWNGRADE_REGISTRY.open('w', encoding='utf-8') as fh:
			fh.write(REGISTRY_HEADER)
			for qid in sorted(rows):
				fh.write(f'{qid}\t{rows[qid][0]}\t{rows[qid][1]}\n')
	return True


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('code')
	parser.add_argument('quest_ids', nargs='+', type=int)
	parser.add_argument('--dry-run', action='store_true')
	parser.add_argument('--note', default='retail-synthesis-gap')
	args = parser.parse_args()
	restored = 0
	for quest_id in args.quest_ids:
		rel = f'src/main/resources/aion/data/static_data/quest_definition/quests/{quest_id}.xml'
		target = XML_DIR / f'{quest_id}.xml'
		line = catalog_line(quest_id)
		if target.is_file():
			print(f'  {quest_id}: XML 已在仓（跳过复原）')
		else:
			if not args.dry_run:
				target.write_bytes(git_show(rel))
			restored += 1
		added = insert_catalog_line(line, quest_id, args.dry_run)
		rows = downgrade_ledger(quest_id, args.code, args.note, args.dry_run)
		registered = register_downgrade(quest_id, args.code, args.note, args.dry_run)
		print(f'  {quest_id}: catalog_added={added} ledger_rows={rows} registry_added={registered}')
	print(f'code={args.code} quests={len(args.quest_ids)} xml_restored={restored} dry_run={args.dry_run}')
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
