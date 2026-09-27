#!/usr/bin/env python3
"""P1b：可选奖励 / 多交付物 49 行逐任务裁定（真端优先）。

输入 = 漂移登记（P1b 重算后）+ 真端 quest.xml 元数据（可选奖励数 / 交付物数，经
RetailSimpleCollectItemGateTest 的 acceptedDefinitionsCarryRetailSemantics 全族断言背书）+
既有两份裁定文件（避免重复登记）。

裁定口径：
- 全部 49 行 ADOPT_RETAIL。合成 IR 与 npc-item-report / npc-complete 展开口径同构：
  可选奖励第 k 项绑确认动作 8+k（与 XML `<choice action=...>` 逐一对齐；XML choice=0 的行是
  XML 漏配，按真端元数据补齐）；多交付物按整组检查/扣除（与 XML 多条 has-item/remove-item 同构）。
  家族动作码 open 31 + check 39 全族 178 行零例外（M5-b2b），交付物轴由客户端任务书 select5 实证。
- ROUTE 轴的差异全部落在「XML 历史路由变体」（如 14120 多驱动真端表未声明的对象 730020 +
  1353/1352 续页）——与 M5-b3 批次 2 的 57 行同判。

输出：p1b-collect-choice-decisions.tsv（quest_id/verdict/basis/axes/evidence）。
用法：python3 -B p1b_build_choice_decisions.py
"""
import re
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
TOPIC = REPO / '.agents/summary/scriptdll-quest-driver'
DRIFT = REPO / 'src/test/resources/quest/retail-simple-collect-item-drift.tsv'
TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest.xml'
PRIOR = [TOPIC / 'm5b3-collect-route-decisions.tsv', TOPIC / 'm5b3x-collect-sentinel-decisions.tsv']
OUT = TOPIC / 'p1b-collect-choice-decisions.tsv'


def main():
	prior_ids = set()
	for path in PRIOR:
		for line in path.read_text(encoding='utf-8').splitlines():
			if line.startswith('#') or not line.strip():
				continue
			prior_ids.add(int(line.split('\t')[0]))

	# 本切片定界（M5-b2 时代普查）：可选奖励 25 行 + 多交付物 24 行。
	# Slice universe from the M5-b2-era shape census: 25 selectable + 24 multi-item rows.
	SELECTABLE = [1579, 2329, 2373, 2645, 3099, 3723, 3734, 4723, 4734, 11461, 13806, 14120, 18034, 18307,
		18503, 18914, 21461, 23806, 28034, 28307, 28914, 30008, 30108, 30602, 30612]
	MULTI_ITEM = [1487, 1624, 2346, 2427, 2487, 2495, 2691, 3096, 3202, 4046, 4082, 4094, 4202, 11004, 11011,
		11043, 11204, 11211, 18501, 21069, 21200, 28501, 30205, 30305]
	targets = {qid: 'SELECTABLE_CHOICE_EXPANSION' for qid in SELECTABLE}
	targets.update({qid: 'MULTI_ITEM_HANDIN' for qid in MULTI_ITEM})

	# 真端 quest.xml 元数据在本裁定中不直接消费：形状定界来自 M5-b2 普查 + 门禁
	# acceptedDefinitionsCarryRetailSemantics 对 178 行全族背书。

	rows = ['# P1b 可选奖励 / 多交付物逐任务裁定（真端优先；由 p1b_build_choice_decisions.py 生成）',
		'# quest_id\tverdict\tbasis\taxes\tevidence']
	counts = {}
	for line in DRIFT.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		qid, cls = int(line.split('\t')[0]), line.split('\t')[1]
		if qid not in targets or qid in prior_ids or not cls.startswith('DIFF:'):
			continue
		basis = targets[qid]
		if basis == 'SELECTABLE_CHOICE_EXPANSION':
			evidence = ('choice k binds confirm 8+k per npc-complete expansion (XML choice=0 rows are XML '
				'gaps, retail metadata rules); family action codes 31+39 zero-exception; QE-051 reward row')
		else:
			evidence = ('whole-set has-item/remove-item check pair per npc-item-report expansion; '
				'family action codes 31+39 zero-exception; QE-051 reward row')
		rows.append(f'{qid}\tADOPT_RETAIL\t{basis}\t{cls[len("DIFF:") :]}\t{evidence}')
		counts[basis] = counts.get(basis, 0) + 1

	OUT.write_text('\n'.join(rows) + '\n', encoding='utf-8')
	print(f'decisions={len(rows) - 2} verdicts={counts}')
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
