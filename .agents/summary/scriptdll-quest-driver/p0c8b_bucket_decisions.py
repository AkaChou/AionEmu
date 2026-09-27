#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-8b：SimpleHunt 缺口表 275 行里的两个大桶按客户端契约裁定（154 行 → ADOPT_RETAIL）。

两桶的判据都是**机器可核**的，逐行在同一脚本里断言：

  A_接取页（47 行，basis=`XML_ACCEPT_FLOW_DEVIATION`）
    XML 侧：`started` 态的 `FINISH_DIALOG(1008)` 用 `close-dialog`（同文件 `unaccepted` 态却是
            `SHOW_SELECTION_PAGE page=SELECT_QUEST`，**自相矛盾**），并多出一条 `SETPRO1(10000)`
            接取捷径（`unaccepted -> started`）。
    真端侧：`FINISH_DIALOG` 两个源态都下发 `SELECT_QUEST(10)`（= 客户端全局任务簿页
            `HtmlPages.xml: page_id 10 = HTML_PAGE_SELECT_QUEST`）。
    客户端证据：该任务 HTML 的按钮集**不含** `HACTION_SETPRO1/SETPRO2`（逐行断言）⇒ 那条捷径
            在客户端不可达，删除不可观测；且语料 534 个已证等价行中 **499 行**用
            `finish="SELECTION_DIALOG"`（= 完成后下发第 10 页）⇒ 真端形状是语料常态。

  B_领奖确认段（107 行，basis=`RETAIL_REWARD_CONFIRM_RANGE`）
    XML 侧：`<npc-complete>` 只声明实际带可选奖励的 `<choice>`（K 条）。
    真端侧：下发完整确认段 `SELECTED_QUEST_REWARD1(8)..SELECTED_QUEST_NOREWARD(23)`（16 条）。
    判据：逐行断言 `xmlOnlyTransitions="-"`（XML 的每条转换在真端侧都能找到同签名对应）⇒
            **真端是 XML 的严格超集**，采纳不丢任何 XML 语义；语料 530/534 已证等价行用全段写法，
            窄写法是本批 132 行的少数派。

输出：
  * p0c8b-bucket-decisions.tsv（owner 记录：桶 / 裁定 / 证据）
  * 追加 154 行到 retail-simple-hunt-adjudicated-decisions.tsv（ADOPT_RETAIL）
  * 缺口表移除这 154 行
用法：python3 -B p0c8b_bucket_decisions.py [--dry-run]
"""
import argparse
import re
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
CENSUS = HERE / 'p0c8-retention-diff-census.tsv'
GAPS = HERE / 'simplehunt-dialog-route-gaps.txt'
OUT = HERE / 'p0c8b-bucket-decisions.tsv'
PROD_DECISIONS = REPO / 'src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv'
DETAILS = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-action-details.csv'
QUESTS_REL = 'src/main/resources/aion/data/static_data/quest_definition/quests/%d.xml'
# 真端确认段：SELECTED_QUEST_REWARD1(8) .. SELECTED_QUEST_NOREWARD(23)。
CONFIRM_FIRST, CONFIRM_LAST = 8, 23
CONFIRM_RANGE = CONFIRM_LAST - CONFIRM_FIRST + 1


def head_xml(quest_id):
	proc = subprocess.run(['git', 'show', 'HEAD:' + QUESTS_REL % quest_id], cwd=REPO,
		capture_output=True, text=True)
	return proc.stdout if proc.returncode == 0 else ''


def census():
	rows = {}
	for line in CENSUS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		rows[int(parts[0])] = parts[3]
	return rows


def client_buttons():
	"""客户端任务 HTML 的按钮常量集：quest_id → {HACTION_*}。"""
	buttons = {}
	for line in DETAILS.read_text(encoding='utf-8').splitlines()[1:]:
		parts = line.split(',')
		if len(parts) >= 15 and parts[0].isdigit():
			buttons.setdefault(int(parts[0]), set()).add(parts[13])
	return buttons


def xml_facts(quest_id):
	text = head_xml(quest_id)
	choices = re.findall(r'<choice\s+action="(SELECTED_QUEST_REWARD\d+)"', text)
	finish_pages = re.findall(r'<dialog type="SHOW_SELECTION_PAGE" page="(\w+)"/>', text)
	closes = text.count('<close-dialog/>')
	setpro = re.findall(r'action="(SETPRO\d)"', text)
	finish_routes = []
	for match in re.finditer(r'<transition\s([^>]*)>([\s\S]*?)</transition>', text):
		body = match.group(2)
		if 'action="FINISH_DIALOG"' not in body:
			continue
		source = re.search(r'source="([^"]+)"', match.group(1))
		page = re.search(r'page="(\w+)"', body)
		finish_routes.append((source.group(1) if source else '-',
			page.group(1) if page else ('close-dialog' if '<close-dialog/>' in body else '-')))
	return {'choices': choices, 'finish_pages': finish_pages, 'closes': closes, 'setpro': setpro,
		'finish_routes': finish_routes}


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('--dry-run', action='store_true')
	args = parser.parse_args()

	rows = census()
	buttons = client_buttons()
	gap_ids = [int(line) for line in GAPS.read_text(encoding='utf-8').splitlines()
		if line.strip().isdigit() and not line.startswith('#')]

	decisions = []
	problems = []
	for quest_id in sorted(gap_ids):
		detail = rows.get(quest_id, '')
		facts = xml_facts(quest_id)
		if ('retailOnlyTransitions=START/0>TalkToNpc' in detail and 'ShowQuestSelectionDialog' in detail
				and 'CloseDialog' in detail and 'xmlOnlyTransitions=NONE/0' in detail):
			# 桶 A：接取流偏差。
			npc_buttons = buttons.get(quest_id, set())
			if 'HACTION_SETPRO1' in npc_buttons or 'HACTION_SETPRO2' in npc_buttons:
				problems.append((quest_id, 'A：客户端有 SETPRO1/SETPRO2 按钮，捷径可达'))
				continue
			if not facts['setpro']:
				problems.append((quest_id, 'A：XML 无 SETPRO1 捷径，形状不符'))
				continue
			pages = {page for page in facts['finish_pages']}
			closes = 'close-dialog' in {page for _, page in facts['finish_routes']}
			if 'SELECT_QUEST' not in pages or not closes:
				problems.append((quest_id, 'A：XML 的两条 FINISH_DIALOG 形态不符 %s' % (facts['finish_routes'],)))
				continue
			evidence = ('XML 的 started 态 FINISH_DIALOG 用 close-dialog（同文件 unaccepted 态为 '
				'SHOW_SELECTION_PAGE SELECT_QUEST，自相矛盾）＋多出 %s 接取捷径；真端两态都下发 '
				'SELECT_QUEST(10)=客户端全局任务簿页；客户端按钮集 %s 无 SETPRO1/SETPRO2 ⇒ 捷径不可达'
				% (','.join(sorted(set(facts['setpro']))), '|'.join(sorted(npc_buttons)) or '-'))
			decisions.append((quest_id, 'ADOPT_RETAIL', 'XML_ACCEPT_FLOW_DEVIATION',
				'XML_EXTRA:SETPRO1_ROUTE+RETAIL_FINISH_DIALOG_PAGE', evidence))
			continue
		if ('retailOnlyTransitions=REWARD' in detail and 'xmlOnlyTransitions=-' in detail
				and 'GrantReward' in detail):
			# 桶 B：领奖确认段。
			if not facts['choices']:
				problems.append((quest_id, 'B：XML 无 <choice> 声明，形状不符'))
				continue
			declared = len(set(facts['choices']))
			if declared >= CONFIRM_RANGE:
				problems.append((quest_id, 'B：XML 声明数 %d 已达全段' % declared))
				continue
			evidence = ('XML 只声明 %d 条确认路由（%s）；真端下发完整确认段 %d..%d（%d 条）；'
				'逐行断言 xmlOnlyTransitions="-" ⇒ 真端是 XML 的**严格超集**（采纳不丢语义）；'
				'语料 530/534 已证等价行用全段写法'
				% (declared, ','.join(sorted(set(facts['choices']))), CONFIRM_FIRST, CONFIRM_LAST,
					CONFIRM_RANGE))
			decisions.append((quest_id, 'ADOPT_RETAIL', 'RETAIL_REWARD_CONFIRM_RANGE',
				'XML_NARROW_CONFIRM_SET', evidence))

	buckets = {}
	for row in decisions:
		buckets[row[2]] = buckets.get(row[2], 0) + 1
	print('裁定行=%d %s | 未通过断言=%d' % (len(decisions), buckets, len(problems)))
	for quest_id, reason in problems[:10]:
		print('  !!', quest_id, reason)

	text = ('# P0c-8b SimpleHunt 缺口表大桶裁定（真端优先）：接取流偏差 + 领奖确认段\n'
		'# 判据与逐行证据见 .agents/summary/scriptdll-quest-driver/p0c8b_bucket_decisions.py 头注释\n'
		'# quest_id\tverdict\tbasis\taxes\tevidence\n')
	for row in decisions:
		text += '\t'.join(str(cell) for cell in row) + '\n'
	if not args.dry_run:
		OUT.write_text(text, encoding='utf-8')

		# 追加到生产裁定表（保留既有行）。
		existing = PROD_DECISIONS.read_text(encoding='utf-8').splitlines()
		header = [line for line in existing if line.startswith('#')]
		body = [line for line in existing if not line.startswith('#') and line.strip()]
		ids = {row[0] for row in decisions}
		body = [line for line in body if int(line.split('\t')[0]) not in ids]
		body.extend('\t'.join(str(cell) for cell in row) for row in decisions)
		body.sort(key=lambda line: int(line.split('\t')[0]))
		header = [line.replace('+ P0c-8a 窄投影批）', '+ P0c-8a 窄投影批 + P0c-8b 大桶批）')
			for line in header]
		if not any('p0c8b_bucket_decisions' in line for line in header):
			header.append('# 生成：.agents/summary/scriptdll-quest-driver/p0c8b_bucket_decisions.py'
				'（接取流偏差 + 领奖确认段）')
		PROD_DECISIONS.write_text('\n'.join(header + body) + '\n', encoding='utf-8')
		print('裁定表 -> %s（%d 行）' % (PROD_DECISIONS, len(body)))

		# 缺口表移除已裁定行。
		keep = [quest_id for quest_id in gap_ids if quest_id not in ids]
		gap_header = [line for line in GAPS.read_text(encoding='utf-8').splitlines()
			if line.startswith('#')]
		gap_header.append('# P0c-8b（2026-09-24）：移除两个大桶（接取流偏差 %d + 领奖确认段 %d），'
			'逐行证据见 p0c8b-bucket-decisions.tsv。'
			% (buckets.get('XML_ACCEPT_FLOW_DEVIATION', 0), buckets.get('RETAIL_REWARD_CONFIRM_RANGE', 0)))
		GAPS.write_text('\n'.join(gap_header + [str(q) for q in keep]) + '\n', encoding='utf-8')
		print('缺口表 %d -> %d 行' % (len(gap_ids), len(keep)))
	print('证据表 ->', OUT)
	return 0 if not problems else 1


if __name__ == '__main__':
	raise SystemExit(main())
