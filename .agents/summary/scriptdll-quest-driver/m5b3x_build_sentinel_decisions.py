#!/usr/bin/env python3
"""M5-b3x：哨兵行（系统发放形状）逐任务裁定生成。

输入 = m5b3x-collect-sentinel-grants.tsv（真端阵营星期位 + 客户端接取页 + DLL 三元组证据）
     + src/test/resources/quest/retail-simple-collect-item-drift.tsv（漂移分类）。

裁定口径（真端优先）：
- 真端每天发放（1111111）且本族驱动可编译（漂移 = DIFF:…）→ ADOPT_RETAIL：
  XML 的 NPC 接取半边是历史错误（真端普查 43/43 无生命周期三元组、客户端只有委托书页、
  真端由 npcfactions_quest 星期位系统发放），按真端放行并退役；
- 真端全 0（RETAIL_DISABLED）行若可编译同判 ADOPT_RETAIL（真端"定义在、发放关"，
  生产星期位已同步修 0，任务不会被发放）；本族 12 行禁用行全部是复合奖励引用（编译拒绝），
  故实际不产生裁定行，留 XML 走 SEMANTIC_GAP:RETAIL_REWARD_NPC_FACTION_COMPOSITE；
- 复合奖励引用 / 可选奖励 / 多物品等编译拒绝行不进本裁定（保留清单走拒绝码路径）。

输出：m5b3x-collect-sentinel-decisions.tsv（quest_id/verdict/basis/axes/evidence）。
用法：python3 -B m5b3x_build_sentinel_decisions.py
"""
import os
import re
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
TOPIC = REPO / '.agents/summary/scriptdll-quest-driver'
GRANTS = TOPIC / 'm5b3x-collect-sentinel-grants.tsv'
DRIFT = REPO / 'src/test/resources/quest/retail-simple-collect-item-drift.tsv'
OUT = TOPIC / 'm5b3x-collect-sentinel-decisions.tsv'


def main():
	drift = {}
	for line in DRIFT.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		drift[int(parts[0])] = parts[1]

	rows = ['# M5-b3x 哨兵行（系统发放）逐任务裁定（真端优先；由 m5b3x_build_sentinel_decisions.py 生成）',
		'# quest_id\tverdict\tbasis\taxes\tevidence']
	verdicts = {}
	for line in GRANTS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#'):
			continue
		parts = line.split('\t')
		quest_id, reward, faction, retail_bits, prod_bits, in_universe, style, triplet, verdict = parts[:9]
		if not in_universe == 'yes':
			continue
		classification = drift.get(int(quest_id), '')
		if verdict == 'GRANT_DAILY' and classification.startswith('DIFF:'):
			rows.append(f'{quest_id}\tADOPT_RETAIL\tFACTION_GRANT_DAILY\t{classification[len("DIFF:") :]}'
				f'\tretail no-triplet 43/43; client {style} only; faction {faction} bits {retail_bits}; '
				f'prod bits {prod_bits}')
			verdicts['ADOPT_RETAIL'] = verdicts.get('ADOPT_RETAIL', 0) + 1
		elif verdict == 'RETAIL_DISABLED' and classification.startswith('DIFF:'):
			rows.append(f'{quest_id}\tADOPT_RETAIL\tFACTION_GRANT_DISABLED\t{classification[len("DIFF:") :]}'
				f'\tretail bits {retail_bits} (never granted); prod bits fixed {prod_bits}; client {style} only')
			verdicts['ADOPT_RETAIL_DISABLED'] = verdicts.get('ADOPT_RETAIL_DISABLED', 0) + 1
		else:
			verdicts[f'skip:{classification.split(":")[0]}'] = verdicts.get(f'skip:{classification.split(":")[0]}', 0) + 1

	OUT.write_text('\n'.join(rows) + '\n', encoding='utf-8')
	print(f'decisions={len(rows) - 2} verdicts={verdicts}')
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
