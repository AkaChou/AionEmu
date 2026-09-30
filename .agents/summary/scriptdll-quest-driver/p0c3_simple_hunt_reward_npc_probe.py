#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-3 探测：SimpleHunt 复合势力报告名能否走客户端 dic 链解出交付 NPC 集。"""
import os
import re
from pathlib import Path

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
UNPAK = Path(f"{os.environ.get('AION_UNPACK_ROOT', os.path.expanduser('~/PycharmProjects/unpak'))}/data_unpacked")
CENSUS = REPO / '.agents/summary/scriptdll-quest-driver/p0c3-simple-hunt-sentinel-census.tsv'
OUT = REPO / '.agents/summary/scriptdll-quest-driver/p0c3-simple-hunt-reward-npc-probe.tsv'


def main():
	rows = [line.split('\t') for line in CENSUS.read_text(encoding='utf-8').splitlines()
		if line and not line.startswith('#') and not line.startswith('quest_id')]
	targets = [r for r in rows if r[1] == 'faction' and r[2] == '1' and r[4] == '-']
	estr = {}
	for f in (UNPAK / 'Strings').glob('*.xml'):
		text = f.read_text(encoding='utf-8', errors='replace')
		for m in re.finditer(r'<name>STR_DIC_E_([^<]+)</name>\s*<body>([^<]*)</body>', text):
			estr[m.group(1)] = m.group(2)
	descs = {}
	for f in sorted((REPO / 'src/main/resources/aion/data/static_data/npcs').glob('npc_template_*.xml')):
		for m in re.finditer(r'<npc_template\s+([^>]*?)/?>', f.read_text(encoding='utf-8')):
			attrs = dict(re.findall(r'([\w]+)="([^"]*)"', m.group(1)))
			nid = attrs.get('npc_id')
			if nid and attrs.get('name_desc') and nid not in descs:
				descs[attrs['name_desc']] = nid

	out = ['# quest_id\treward_ref\tclient_npc_ids\tsource_dic\tnpc_descs']
	resolved = 0
	for quest_id, _sentinel, _univ, reward, _ids, _cids, _counters, _ok, _mask, _fac, _prod in targets:
		hits = list((UNPAK / 'Dialogs').rglob(f'quest_q{quest_id}.html'))
		if not hits:
			out.append(f'{quest_id}\t{reward}\tNO_HTML\t-\t-')
			continue
		text = hits[0].read_text(encoding='utf-8', errors='replace')
		id_set, dic_hits, desc_hits = {}, [], []
		for token in re.findall(r'\[%dic:STR_DIC_E_([^\]]+)\]', text):
			body = estr.get(token)
			if body is None:
				continue
			dic_hits.append(token)
			for name in re.findall(r'STR_DIC_N_([^\]]+)\]', body):
				nid = descs.get(name)
				if nid:
					id_set[nid] = name
					desc_hits.append(name)
		if id_set:
			resolved += 1
			ordered = sorted(id_set, key=int)
			out.append(f'{quest_id}\t{reward}\t{",".join(ordered)}\t{",".join(sorted(set(dic_hits)))}\t'
				f'{",".join(id_set[n] for n in ordered)}')
		else:
			out.append(f'{quest_id}\t{reward}\tUNRESOLVED\t{",".join(sorted(set(dic_hits)))}\t-')
	OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')
	print(f'宇宙内复合报告名行={len(targets)} 客户端可解={resolved}')
	for line in out[1:]:
		print(line)
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
