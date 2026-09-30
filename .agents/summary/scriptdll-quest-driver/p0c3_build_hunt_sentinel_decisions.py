#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-3：SimpleHunt 类别哨兵（`_faction_`）行的逐任务裁定表 + 真端侧 IR 指纹登记。

裁定口径（与 M5-b3x 的 CollectItem 哨兵行一致，真端优先）：
- ADOPT_RETAIL / FACTION_GRANT_DAILY    ：真端星期位有发放日 → 退役 XML，系统发放 + 击杀网格由真端定义驱动；
- ADOPT_RETAIL / FACTION_GRANT_DISABLED ：真端星期位全 0（永不发放）→ 同样退役；生产星期位按真端修正为全 0，
  任务在该阵营轮换里不再出现（等价于真端的"下线内容"），不再保留 XML 里的历史接取路由。

输入：p0c3-simple-hunt-sentinel-census.tsv（普查）+ 生产/真端星期位 + 客户端交付 NPC 登记表。
输出：
- src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv（quest_id, verdict, basis, axes, evidence）
- .agents/summary/scriptdll-quest-driver/p0c3-simple-hunt-sentinel-decisions.tsv（同内容的工作副本）
"""
import os
import io
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
TOPIC = REPO / '.agents/summary/scriptdll-quest-driver'
CENSUS = TOPIC / 'p0c3-simple-hunt-sentinel-census.tsv'
REGISTRY = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv'
OUT_RESOURCE = REPO / 'src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv'
OUT_TOPIC = TOPIC / 'p0c3-simple-hunt-sentinel-decisions.tsv'
HEADER = [
	'# P0c-3 SimpleHunt 类别哨兵（_faction_）行逐任务裁定（真端优先）',
	'# quest_id\tverdict\tbasis\taxes\tevidence',
	'# verdict: ADOPT_RETAIL（退役 XML，真端驱动）| KEEP_XML（真端无法表达，保留 XML）',
	'# basis: FACTION_GRANT_DAILY（真端星期位有发放日）| FACTION_GRANT_DISABLED（真端星期位全 0）',
	'# 生成：.agents/summary/scriptdll-quest-driver/p0c3_build_hunt_sentinel_decisions.py',
]


def main():
	rows = [line.split('\t') for line in CENSUS.read_text(encoding='utf-8').splitlines()
		if line and not line.startswith('#') and not line.startswith('quest_id')]
	registered = {}
	for line in REGISTRY.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		registered[int(parts[0])] = parts[1]

	out = []
	for quest_id, sentinel, in_universe, reward, reward_ids, _client, _counters, counters_ok, \
			mask_retail, faction_retail, mask_prod in rows:
		if sentinel != 'faction' or in_universe != '1':
			continue
		if counters_ok != '1':
			continue
		unique = reward_ids != '-' and ',' not in reward_ids
		if not unique and int(quest_id) not in registered:
			continue
		disabled = set(mask_retail) == {'0'}
		basis = 'FACTION_GRANT_DISABLED' if disabled else 'FACTION_GRANT_DAILY'
		axes = []
		axes.append('RETAIL_GRANT_%s' % ('DISABLED' if disabled else 'DAILY'))
		axes.append('HANDIN_CLIENT_REGISTRY' if not unique else 'HANDIN_UNIQUE_NAME')
		if mask_prod != mask_retail:
			axes.append('PROD_BITS_FIX:%s->%s' % (mask_prod, mask_retail))
		evidence = ('retail bits %s; prod bits %s; faction %s; handin=%s; counters ok'
			% (mask_retail, mask_prod, faction_retail, reward if unique else registered[int(quest_id)]))
		out.append((quest_id, 'ADOPT_RETAIL', basis, ' '.join(axes), evidence))

	lines = HEADER + ['\t'.join(row) for row in out]
	text = '\n'.join(lines) + '\n'
	OUT_RESOURCE.write_text(text, encoding='utf-8')
	OUT_TOPIC.write_text(text, encoding='utf-8')
	bases = {}
	for row in out:
		bases[row[2]] = bases.get(row[2], 0) + 1
	print('裁定行数:', len(out), bases)
	print('输出:', OUT_RESOURCE)
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
