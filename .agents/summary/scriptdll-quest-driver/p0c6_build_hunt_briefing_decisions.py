#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-6：把 SimpleHunt `talk_npc1` 简报行登记进逐任务裁定表（真端优先，basis=BRIEFING_STEP）。

判据（全部机器可读、可复核）：
1. 真端表 `Quest_SimpleHunt.xml` 的 `<talk_npc1>` 非空，且任务在本服宇宙（保留清单）内 → 23 行；
2. `talk_npc1` 唯一解析到 NPC（NPC 注册表名称索引，见 npc_name_index.tsv）；
3. 客户端任务书存在 select2 简报链（`quest_client_briefing_chains.tsv`，末按钮 SETPRO1/SETPRO2）；
4. 客户端击杀行门控 `SECTION_5==0`（`Quest_unpacked/quest_monster.csv`）；
5. 报告页由客户端契约给出（简报行 = select5/2375，见 `quest_client_report_pages.tsv`）。

裁定：
* ADOPT_RETAIL（21 行）——退役历史 XML（它的对话框轴是历史内容：多出接取 NPC 的
  SETPRO1/SELECT2_1 路由、报告页混用 select2）。语义由客户端契约 + 真端侧冻结 IR 承担。
* KEEP_XML（2 行：14112/14123）——`QUEST_SPAWN_UNEXPRESSED`：任务把**击杀目标或交付 NPC**
  用 after-commit 刷怪边放进世界（`spawn-npc-at-player`/`spawn-npc-current-or-default`），而该 NPC
  在本服 `spawns/**` 里没有任何静态 spot，真端家族表也没有刷怪列（真端世界文件里的开放地图与
  客户端 Quest.pak 都不含该放置，ScriptDLL 只登记 name→quest）。删掉 XML 会让任务不可完成，
  属"真端无法表达"的降级行，必须保留 XML。

输入：真端表、保留清单、简报链登记、报告页登记、NPC 名索引、客户端 quest_monster.csv、生产 XML（对照）、
     git HEAD 的退役 XML（刷怪轴证据）、spawns/**（静态刷怪普查）
输出：src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv（与工作副本同步）
用法：python3 -B p0c6_build_hunt_briefing_decisions.py
"""
import os
import re
import subprocess
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
HERE = Path(__file__).resolve().parent
TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml'
RETENTION = REPO / 'src/test/resources/quest/retail-xml-retention.tsv'
CHAINS = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_briefing_chains.tsv'
REPORT_PAGES = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_report_pages.tsv'
NPC_INDEX = HERE / 'npc_name_index.tsv'
MONSTER = Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/Quest_unpacked/quest_monster.csv")
QUESTS = REPO / 'src/main/resources/aion/data/static_data/quest_definition/quests'
PROD_DECISIONS = REPO / 'src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv'
WORK_DECISIONS = HERE / 'p0c6-simple-hunt-briefing-decisions.tsv'
HEADER = [
	'# SimpleHunt 类别哨兵/简报行逐任务裁定（真端优先）：`_faction_`/`_area_`（哨兵）+ `talk_npc1`（简报）',
	'# quest_id\tverdict\tbasis\taxes\tevidence',
	'# verdict: ADOPT_RETAIL（退役 XML，真端驱动）| KEEP_XML（真端无法表达，保留 XML）',
	'# basis: FACTION_GRANT_DAILY | FACTION_GRANT_DISABLED | AREA_GRANT | BRIEFING_STEP |',
	'#        QUEST_SPAWN_UNEXPRESSED（任务刷怪边是唯一生成路径，真端表无刷怪列）',
	'# 生成：.agents/summary/scriptdll-quest-driver/p0c3_build_hunt_sentinel_decisions.py（faction）',
	'#       .agents/summary/scriptdll-quest-driver/p0c4_register_area_grants.py（area）',
	'#       .agents/summary/scriptdll-quest-driver/p0c6_build_hunt_briefing_decisions.py（briefing）',
]

# 真端无法表达的"任务刷怪"轴（P0c-6 复核结论）：XML 的 after-commit 刷怪边是这两个任务 NPC
# 在世界上存在的唯一途径，真端家族表没有刷怪列 → 降级回 XML（KEEP_XML）。
# The quest spawn edge is the only way these NPCs exist in the world; no retail table carries a spawn column.
SPAWN_UNEXPRESSED = {
	14112: 'reward_npc_name=Soul_Kato->203195（XML: spawn-npc-at-player slot=kato）',
	14123: 'monster1=hippolyta_Q14123->206360（XML: spawn-npc-current-or-default slot=peddler-hippola）',
}


def field(block, tag):
	match = re.search(r'<%s>(.*?)</%s>' % (tag, tag), block, re.S)
	return match.group(1).strip() if match else ''


def table_rows():
	"""真端表行 → {quest_id: (acquired, talk, reward, counters)}。"""
	rows = {}
	for match in re.finditer(r'<id id="(\d+)">([\s\S]*?)</id>', TABLE.read_text(encoding='utf-8')):
		block = match.group(2)
		counters = []
		for slot in range(1, 9):
			count = field(block, 'count%d' % slot)
			if count:
				counters.append('%d/%s' % (slot, count))
		rows[int(match.group(1))] = (field(block, 'acquired_npc_name'), field(block, 'talk_npc1'),
			field(block, 'reward_npc_name'), ','.join(counters))
	return rows


def registry(path, column=1):
	"""登记表 → {quest_id: 第 column 列}。 / Registry rows keyed by quest id."""
	rows = {}
	for line in path.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		parts = line.split('\t')
		if len(parts) > column:
			rows[int(parts[0])] = parts[column].strip()
	return rows


def npc_ids():
	"""名称 → npc_id 集合（与 RetailNpcNameIndex 同源的普查表）。"""
	index = {}
	for line in NPC_INDEX.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip() or line.startswith('name_desc\t'):
			continue
		parts = line.split('\t')
		if len(parts) >= 2 and parts[1].strip().isdigit():
			index.setdefault(parts[0].strip().lower(), set()).add(int(parts[1].strip()))
	return index


def client_gates(quest_id):
	"""客户端 quest_monster.csv 的门控表达式（击杀行）。"""
	gates = []
	for line in MONSTER.read_text(encoding='utf-8', errors='replace').splitlines():
		parts = line.split(',')
		if len(parts) >= 2 and parts[0].strip().isdigit() and int(parts[0].strip()) == quest_id:
			gates.append(parts[1].strip())
	return gates


def xml_dialog_axes(quest_id):
	"""生产 XML 的简报轴（对照证据）：工作树优先，退役行回退 git HEAD 并标注来源。"""
	path = QUESTS / ('%d.xml' % quest_id)
	if path.is_file():
		text, origin = path.read_text(encoding='utf-8'), ''
	else:
		text = head_xml(quest_id)
		if not text:
			return 'xml absent'
		origin = '(git HEAD) '
	pairs = set()
	for route in re.findall(r'<transition[^>]*>([\s\S]*?)</transition>', text):
		npc = re.search(r'npc-id="(\d+)"', route)
		action = re.search(r'action="([A-Z0-9_]+)"', route)
		if npc and action and action.group(1) in ('QUEST_SELECT', 'SETPRO1', 'SETPRO2', 'SELECT2_1', 'SELECT2_1_1',
				'SELECT2_2', 'SELECT1_1'):
			pairs.add('%s:%s' % (npc.group(1), action.group(1)))
	shorthand = re.findall(r'<dialog type="NPC_START"[^>]*npc-id="(\d+)"', text)
	return '%s%s' % (origin, 'npc_start=%s; %s'
		% (','.join(sorted(set(shorthand))) or '-', ' '.join(sorted(pairs)) or '-'))


def head_xml(quest_id):
	"""git HEAD 里的（退役前）生产 XML；不存在返回空串。 / The pre-retirement XML from git HEAD."""
	rel = 'src/main/resources/aion/data/static_data/quest_definition/quests/%d.xml' % quest_id
	proc = subprocess.run(['git', 'show', 'HEAD:' + rel], cwd=REPO, capture_output=True, text=True)
	return proc.stdout if proc.returncode == 0 else ''


def spawn_targets(quest_id):
	"""XML after-commit 刷怪边点名的模板 id 集。 / Template ids spawned by the quest XML."""
	return {int(x) for x in re.findall(r'<spawn-npc[^>]*template-id="(\d+)"', head_xml(quest_id))}


def head_spawn_edges(quest_id):
	"""刷怪边摘要（标签 / 槽位 / 模板 id / 次数）。 / Compact spawn-edge summary."""
	summary = {}
	for edge in re.findall(r'<spawn-npc[^>]*>', head_xml(quest_id)):
		slot = re.search(r'slot="([^"]+)"', edge)
		template = re.search(r'template-id="(\d+)"', edge)
		key = '%s slot=%s template-id=%s' % (edge.split()[0].lstrip('<'),
			slot.group(1) if slot else '-', template.group(1) if template else '-')
		summary[key] = summary.get(key, 0) + 1
	return '; '.join('%s x%d' % (key, count) for key, count in sorted(summary.items())) or '-'


def static_spawn_ids():
	"""spawns/** 的静态刷怪 npc id 集。 / Npc ids with a static spawn."""
	ids = set()
	for path in (REPO / 'src/main/resources/aion/data/static_data/spawns').rglob('*.xml'):
		ids.update(int(x) for x in re.findall(r'<spawn\s+npc_id="(\d+)"',
			path.read_text(encoding='utf-8', errors='ignore')))
	return ids


def briefing_rows():
	universe = set()
	for line in RETENTION.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		universe.add(int(line.split('\t')[0]))
	chains = registry(CHAINS, column=2)
	report_pages = registry(REPORT_PAGES)
	names = npc_ids()
	spawns = static_spawn_ids()
	rows = []
	for quest_id, (acquired, talk, reward, counters) in sorted(table_rows().items()):
		if not talk or quest_id not in universe:
			continue
		resolved = sorted(names.get(talk.lower(), ()))
		chain = chains.get(quest_id, '')
		last = chain.split()[-1].split(':')[0] if chain else '-'
		gates = client_gates(quest_id)
		section5 = all('SECTION_5==0' in gate for gate in gates) and bool(gates)
		evidence = ('retail talk_npc1=%s->%s; acquired=%s; reward=%s; counters=%s; '
			'client chain=%s (terminal=%s); report_page=%s; kill gate SECTION_5==0=%s; '
			'old xml { %s }') % (
			talk, ','.join(str(npc) for npc in resolved) or '-', acquired, reward, counters,
			chain or '-', last, report_pages.get(quest_id, '-'), 'y' if section5 else 'n',
			xml_dialog_axes(quest_id))
		assert len(resolved) == 1, (quest_id, talk, resolved)
		assert last in ('10000', '10001'), (quest_id, chain)
		assert section5, (quest_id, gates)
		if quest_id in SPAWN_UNEXPRESSED:
			# 刷怪轴：击杀/交付 NPC 只由任务自身的 after-commit 边进世界，真端表无刷怪列。 /
			# Spawn axis: the quest's own after-commit edge is the only source of this NPC.
			spawn_evidence = ('retail table has no spawn column; %s; XML spawn edges=%s; '
				'static spawns/** for %s: none; ScriptDLL registers the name->quest binding only '
				'(fun_196.cpp:933)') % (
				SPAWN_UNEXPRESSED[quest_id], head_spawn_edges(quest_id),
				','.join(str(npc) for npc in sorted(spawn_targets(quest_id))))
			assert not (spawn_targets(quest_id) & spawns), (quest_id, spawn_targets(quest_id) & spawns)
			rows.append((quest_id, 'KEEP_XML', 'QUEST_SPAWN_UNEXPRESSED',
				'XML_EXTRA:SPAWN_NPC', spawn_evidence + '; ' + evidence))
			continue
		rows.append((quest_id, 'ADOPT_RETAIL', 'BRIEFING_STEP', 'RETAIL_BRIEFING_CHAIN', evidence))
	return rows


def main():
	existing = []
	for line in PROD_DECISIONS.read_text(encoding='utf-8').splitlines():
		if line.startswith('#') or not line.strip():
			continue
		existing.append(tuple(line.split('\t')))
	briefing = briefing_rows()
	ids = {row[0] for row in briefing}
	kept = [row for row in existing if int(row[0]) not in ids]
	rows = sorted(kept + briefing, key=lambda row: int(row[0]))
	text = '\n'.join(HEADER + ['\t'.join(str(cell) for cell in row) for row in rows]) + '\n'
	PROD_DECISIONS.write_text(text, encoding='utf-8')
	WORK_DECISIONS.write_text(text, encoding='utf-8')
	print('哨兵行:', len(kept), ' 简报行:', len(briefing), ' 合计:', len(rows))
	for row in briefing:
		print(' ', row[0], row[1], row[2], row[3], '|', row[4][:130])
	return 0


if __name__ == '__main__':
	raise SystemExit(main())
