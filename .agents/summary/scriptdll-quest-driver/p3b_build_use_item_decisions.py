#!/usr/bin/env python3
"""P3b：SimpleUseItem 104 行逐任务裁定（真端优先）。

裁定口径：
- 可编译（DIFF/EQUIVALENT）→ ADOPT_RETAIL：合成定义 = 客户端契约的规范形状（用物品接取 →
  无目标对话接受/拒绝/关窗 → 报告 NPC SELECT5 + 1009 交付 → npc-complete 确认 8..23）；
  差异轴全部落在已知类别：领奖投影（QE-051：1107 类 XML 的 var0=1 为历史投影，客户端单行任务
  末行 = 0）、XML 侧的 NPC_START/NPC_REPORT 宏块中间步骤（SELECT2_1/SETPRO1 等过渡页，
  客户端契约无对应按钮面）、item-report/set-succeed 变体（交付检查对与 collect 族同构）。
- RETAIL_REWARD_NPC_UNRESOLVED（30720/30723）→ 不进裁定（保留清单走拒绝码路径，KEEP_XML 语义）。

输出：p3b-use-item-decisions.tsv（quest_id/verdict/basis/axes/evidence）。
用法：python3 -B p3b_build_use_item_decisions.py
"""
from pathlib import Path

REPO = Path('/Users/mc/IdeaProjects/AionEmu-test')
TOPIC = REPO / '.agents/summary/scriptdll-quest-driver'
DRIFT = REPO / 'src/test/resources/quest/retail-simple-use-item-drift.tsv'
OUT = TOPIC / 'p3b-use-item-decisions.tsv'

EVIDENCE = ('canonical item-use accept (UseItem -> ask window) + targetless 1002/1003/1008 + '
	'SELECT5 report (31 + 1009 hand-in) + npc-complete 8..23; reward projection per QE-051 '
	'client journal rows; item symbols resolve via strip-ITEM_ lowercase name_desc (104/104 unique)')


def main():
	rows = ['# P3b SimpleUseItem 逐任务裁定（真端优先；由 p3b_build_use_item_decisions.py 生成）',
		'# quest_id\tverdict\tbasis\taxes\tevidence']
	counts = {}
	for line in DRIFT.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		qid, cls = line.split('\t')[0], line.split('\t')[1]
		if cls.startswith('DIFF:'):
			rows.append(f'{qid}\tADOPT_RETAIL\tUSE_ITEM_CANONICAL\t{cls[len("DIFF:") :]}\t{EVIDENCE}')
			counts['ADOPT_RETAIL'] = counts.get('ADOPT_RETAIL', 0) + 1
		elif cls == 'EQUIVALENT':
			rows.append(f'{qid}\tADOPT_RETAIL\tUSE_ITEM_CANONICAL\t-\t{EVIDENCE}')
			counts['ADOPT_RETAIL'] = counts.get('ADOPT_RETAIL', 0) + 1
		else:
			counts['skip:' + cls.split(':')[0]] = counts.get('skip:' + cls.split(':')[0], 0) + 1
	OUT.write_text('\n'.join(rows) + '\n', encoding='utf-8')
	print(f'decisions={len(rows) - 2} verdicts={counts}')
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
