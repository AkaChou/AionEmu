#!/usr/bin/env python3
"""P3：客户端 quest_monster.csv → SimpleSerialHunt 串行阶段契约登记表。

证据链：客户端解包 `Quest_unpacked/quest_monster.csv` 的 `Progress(SECTION_n<count; SECTION_(n-1)==count')`
行给出逐段链式门控（乱序不计数）、段内计数与完整刷怪名单（含真端表未列的同名/变体刷怪）。
刷怪名小写匹配本服 npc `name_desc`（唯一命中）→ npc id。

产出：
- `src/main/resources/aion/data/static_data/quest_retail/quest_client_hunt_stages.tsv`
  （quest_id / stage / count / npc_ids / names）；
- 证据副本 `p3-client-hunt-stages-evidence.tsv`。

用法：python3 -B p3_client_hunt_stages.py
"""
import os
import csv
import re
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
UNPAK = Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/Quest_unpacked/quest_monster.csv")
TOPIC = REPO / '.agents/summary/scriptdll-quest-driver'
TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleSerialHunt.xml'
OUT = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_hunt_stages.tsv'
EVIDENCE = TOPIC / 'p3-client-hunt-stages-evidence.tsv'
FAMILY = [13918, 16991, 18911, 18912, 23918, 26991, 28911, 28912, 30600, 30610]


def main():
	desc_to_ids = {}
	for f in sorted((REPO / 'src/main/resources/aion/data/static_data/npcs').glob('npc_template_*.xml')):
		for m in re.finditer(r'<npc_template\s+([^>]*?)/?>', f.read_text(encoding='utf-8')):
			attrs = dict(re.findall(r'([\w]+)="([^"]*)"', m.group(1)))
			nid = attrs.get('npc_id')
			if nid and attrs.get('name_desc'):
				desc_to_ids.setdefault(attrs['name_desc'].lower(), set()).add(int(nid))

	rows = {}
	with UNPAK.open(encoding='utf-8', errors='replace') as fh:
		reader = csv.reader(fh)
		next(reader)
		for row in reader:
			if row and row[0].isdigit() and int(row[0]) in FAMILY:
				rows.setdefault(int(row[0]), []).append(row)

	out = ['# 客户端串行阶段契约登记（SimpleSerialHunt；由 p3_client_hunt_stages.py 生成）',
		'# quest_id\tstage\tcount\tnpc_ids\tnames']
	unresolved = []
	for quest_id in FAMILY:
		for stage_index, row in enumerate(rows.get(quest_id, []), start=1):
			gate = row[1]
			m = re.search(rf'SECTION_{stage_index - 1}<(\d+)', gate)
			count = int(m.group(1)) if m else 1
			ids, names = [], []
			for name in row[6:]:
				if not name:
					continue
				hits = desc_to_ids.get(name.lower(), set())
				if len(hits) == 1:
					npc_id = next(iter(hits))
					ids.append(str(npc_id))
					names.append(name)
				else:
					unresolved.append(f'{quest_id}/{stage_index}/{name}:{len(hits)}')
			if ids:
				out.append(f'{quest_id}\t{stage_index}\t{count}\t{",".join(ids)}\t{",".join(names)}')
	OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')
	EVIDENCE.write_text('\n'.join(out) + '\n', encoding='utf-8')
	print(f'stages={len(out) - 2} unresolved={len(unresolved)}')
	if unresolved:
		print('unresolved:', unresolved[:10])
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
