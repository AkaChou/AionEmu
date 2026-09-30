#!/usr/bin/env python3
"""P3：SimpleSerialHunt 10 行逐任务裁定（真端优先）。

裁定口径：全部 ADOPT_RETAIL。合成定义 = 客户端 quest_monster.csv 链式 SECTION 门控的串行阶梯
（乱序击杀不计数；阶段计数/刷怪名单 = quest_client_hunt_stages.tsv）。与历史 XML 的差异轴全部落在：
- XML_EXTRA:NODE/ROUTE：XML 的并行计数网格（含 32 节点组合）被客户端链式契约取代（18911/18912 等）；
- XML_EXTRA:ROUTE（13918/23918）：XML 的 EnterWorld 旧存档迁移自愈路由（AionEmu 历史迁移面）；
- RETAIL_EXTRA:DIALOG_31/1008：报告 NPC 在 NONE 态的选择页/关窗出口（npc-start 规范形，与已驱动
  SimpleHunt 同构，XML 缺配）。

输出：p3-serial-hunt-decisions.tsv（quest_id/verdict/basis/axes/evidence）。
用法：python3 -B p3_build_serial_hunt_decisions.py
"""
import os
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
TOPIC = REPO / '.agents/summary/scriptdll-quest-driver'
DRIFT = REPO / 'src/test/resources/quest/retail-simple-serial-hunt-drift.tsv'
OUT = TOPIC / 'p3-serial-hunt-decisions.tsv'

EVIDENCE = ('client chained SECTION gates (quest_monster.csv) via quest_client_hunt_stages.tsv; '
	'retail table Quest_SimpleSerialHunt.xml; hunt npc-start/npc-report/npc-complete canonical flows')


def main():
	rows = ['# P3 SimpleSerialHunt 逐任务裁定（真端优先；由 p3_build_serial_hunt_decisions.py 生成）',
		'# quest_id\tverdict\tbasis\taxes\tevidence']
	counts = {}
	for line in DRIFT.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		qid, cls = line.split('\t')[0], line.split('\t')[1]
		basis = 'CLIENT_CHAINED_SERIAL_LADDER'
		axes = cls[len('DIFF:'):] if cls.startswith('DIFF:') else cls
		rows.append(f'{qid}\tADOPT_RETAIL\t{basis}\t{axes}\t{EVIDENCE}')
		counts[basis] = counts.get(basis, 0) + 1
	OUT.write_text('\n'.join(rows) + '\n', encoding='utf-8')
	print(f'decisions={len(rows) - 2} verdicts={counts}')
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
