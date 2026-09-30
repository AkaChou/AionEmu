#!/usr/bin/env python3
"""客户端对话页登记（三张）：交付型五页 / 接取入口页 / 交付型例外。

口径：客户端任务书 HTML 的页面顺序就是流程（docs/quest/client-dialog-mapping）。

1) quest_client_handin_pages.tsv —— 交付型五页齐备（select_none / select1 / check_user_item_ok /
   check_user_item_fail / select_success），且页面名集合恰好是标准模板；
2) quest_client_entry_pages.tsv —— 首个客户端页面是 select_none 家族时登记入口页 id
   （家族规范形默认显示 select1，但这类客户端根本没有 select1，会触发契约门禁 PAGE_NOT_IN_TASK_HTML；
   例：DataDriven hunt 行 424 行、P5-1 退役后暴露的既有回归）；
3) quest_client_handin_exceptions.tsv —— 交付型「像但不标准」的任务（页面集合多出 select_none_1 /
   select2 / SET_SUCCEED 等）→ 合成器按稳定码拒绝、保留 XML。
"""
import os
import csv
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
MAPPING = REPO / 'docs/quest/client-dialog-mapping'
PAGES = MAPPING / 'quest-dialog-pages.csv'
ACTIONS = MAPPING / 'quest-dialog-action-details.csv'
OUT_DIR = REPO / 'src/main/resources/aion/data/static_data/quest_retail'
# 人工核定的暂缓清单（客户端流程需要逐任务形状，标准模板无法表达）：并入 exceptions 登记。
CURATED_DEFERRED = Path(__file__).resolve().parent / 'p52-handin-deferred-quests.tsv'

HANDIN_ROLES = ['select_none', 'select1', 'check_user_item_ok', 'check_user_item_fail', 'select_success']
# 标准模板的完整页面名集合（其余页面名 = 变体，不进交付型登记）。
TEMPLATE_PAGES = set(HANDIN_ROLES) | {
	'select_quest_reward1', 'quest_summary', 'quest_complete',
	'select_acqusitive_quest_desc', 'select_progressive_quest_desc',
	# 接取确认对页（50058 形：select_none 窗口按钮 = ACCEPT_1(1002)/REFUSE_1(1003)，接受后客户端
	# 落在 quest_accept_1 页（其 39 按钮 = 交付检查），拒绝后落在 quest_refuse_1 页（1008 关窗）——
	# 规范 acceptFlow 已发 QUEST_ACCEPT_1→ShowQuestDialog(1003)/QUEST_REFUSE_1→ShowQuestDialog(1004)
	# 与两页按钮出口，故这两页属标准模板而非变体。
	# The accept-confirmation page pair (the 50058 shape): the canonical accept flow already emits
	# QUEST_ACCEPT_1 → page 1003, QUEST_REFUSE_1 → page 1004 and both pages' button exits.
	'quest_accept_1', 'quest_refuse_1',
	# select_none 续页（15478 形：select_none 页唯一按钮 SELECT_NONE_1(4763)，接取/拒绝按钮
	# 落在该续页上；遗留 XML 有 unaccepted→unaccepted SELECT_NONE_1 → SHOW SELECT_NONE_1 路由，
	# 由 acceptFlow 的阶梯出口补齐）。
	# The select_none continuation page (the 15478 shape): accept/refuse live on it and the
	# legacy XML routes SELECT_NONE_1 → SHOW SELECT_NONE_1, emitted by the accept ladder exit.
	'select_none_1'}


def main():
	by_quest = {}
	with PAGES.open(encoding='utf-8-sig') as handle:
		for row in csv.DictReader(handle):
			by_quest.setdefault(row['quest_id'], []).append(
				(int(row['page_order']), row['html_page_name'], int(row['page_id'])))
	# ok 页（check_user_item_ok）的可见按钮：只有 FINISH_DIALOG(1008) = 本地关闭，交付成功后
	# 服务器必须直接开领奖窗（旧 XML repaired 形状与交接审计正向锁一致）；有 SELECT_QUEST_REWARD(1009)
	# 的 ok 页可交互，成功分支照常显示 ok 页本身。
	# Visible buttons of the ok page: FINISH_DIALOG-only means a local close, so the hand-over
	# branch must open the reward window directly; an interactive ok page is shown as-is.
	page_actions = {}
	with ACTIONS.open(encoding='utf-8-sig') as handle:
		for row in csv.DictReader(handle):
			if not row.get('action_id', '').strip():
				continue
			page_actions.setdefault((row['quest_id'], int(row['page_id'])), set()).add(
				int(row['action_id']))
	handin, entry, exceptions = [], [], []
	for quest_id in sorted(by_quest, key=int):
		pages = sorted(by_quest[quest_id])
		names = [name for _, name, _ in pages]
		page_map = {name: page_id for _, name, page_id in pages}
		first_name = names[0]
		if first_name.startswith('select_none'):
			entry.append(f'{quest_id}\t{page_map[first_name]}')
		if set(names) <= TEMPLATE_PAGES:
			if all(role in page_map for role in HANDIN_ROLES):
				ok_actions = page_actions.get((quest_id, page_map['check_user_item_ok']), set())
				ok_local_close = bool(ok_actions) and ok_actions <= {1008}
				handin.append(f'{quest_id}\t'
					+ '\t'.join(str(page_map[role]) for role in HANDIN_ROLES)
					+ f'\t{str(ok_local_close).lower()}')
			elif 'select_none_1' in names:
				# select_none 阶梯入口但五页不全（交付对手是交互物/FOBJ 等非交付 NPC 形）：仍属交付型
				# 家族，标准五页形状无法表达 → 登记为例外（25073 形：交付在 FOBJ 对象上，页面只有
				# select_none/select_none_1/select1/select_success）。
				# A select_none-ladder row missing some of the five roles is still hand-in-like but not
				# expressible by the standard shape, so it stays an exception (the 25073 shape: the
				# hand-over happens on a FOBJ object).
				missing = ','.join(role for role in HANDIN_ROLES if role not in page_map)
				exceptions.append(f'{quest_id}\tHANDIN_LIKE_MISSING_ROLES={missing}')
			continue
		# 入口是 select_none 家族、但页面集合超出标准模板（多出 ask_quest_accept / select_none_1 /
		# select2 / SET_SUCCEED 等）：接取窗口之外的按钮无法在标准形状里逐一定位 → 登记为例外。
		# Hand-in-like quests whose client page set exceeds the standard template are exceptions.
		extra = sorted(set(names) - TEMPLATE_PAGES)
		exceptions.append(f'{quest_id}\tEXTRA_CLIENT_PAGES={",".join(extra)}')
	headers = {
		'quest_client_handin_pages.tsv': [
			'# 客户端交付型对话页登记（quest_id, select_none, select1, check_user_item_ok, '
			'check_user_item_fail, select_success, ok_local_close）',
			'# 来源：docs/quest/client-dialog-mapping/quest-dialog-pages.csv（客户端任务书 HTML 页面索引）；'
			'ok_local_close 来自 quest-dialog-action-details.csv（ok 页可见按钮 ⊆ {FINISH_DIALOG(1008)}）',
			'# 生成：python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_handin_pages.py',
			'# 语义：五页齐备且页面集合 = 标准模板 → 该任务走「交付型」对话流；变体进 *_exceptions.tsv；'
			'ok_local_close=true 时交付成功分支直接开领奖窗（ok 页本地关闭，显示它会形成死端）'],
		'quest_client_entry_pages.tsv': [
			'# 客户端接取入口页登记（quest_id, entry_page）：首个客户端页面是 select_none 家族时登记',
			'# 来源：同上（客户端任务书 HTML 页面索引）',
			'# 语义：家族规范形的 QUEST_SELECT 默认显示 select1，这类客户端没有 select1（显示它会触发',
			'#       PAGE_NOT_IN_TASK_HTML），因此接取入口页必须由本表给出。'],
		'quest_client_handin_exceptions.tsv': [
			'# 交付型例外登记（quest_id, reason）：客户端是交付型但页面集合超出标准模板',
			'# 来源：同上；这些任务的真端合成无法在标准交付形状内表达 → 稳定码拒绝、保留 XML'],
	}
	if CURATED_DEFERRED.is_file():
		for line in CURATED_DEFERRED.read_text(encoding='utf-8').splitlines():
			if line.startswith('#') or not line.strip():
				continue
			parts = line.split('\t')
			# 暂缓行即使页面集合匹配模板也要进 exceptions（逐任务形状的客户端流程）。
			handin = [row for row in handin if row.split('\t')[0] != parts[0]]
			exceptions.append(f'{parts[0]}\tDEFERRED_{parts[1]}')
	exceptions = sorted(set(exceptions), key=lambda row: int(row.split('\t')[0]))
	for name, rows in (('quest_client_handin_pages.tsv', handin), ('quest_client_entry_pages.tsv', entry),
			('quest_client_handin_exceptions.tsv', exceptions)):
		target = OUT_DIR / name
		target.write_text('\n'.join(headers[name] + rows) + '\n', encoding='utf-8')
		print(f'{name}: {len(rows)} rows')
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
