#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-10e：SimpleTalk RETAIL_TALK_CHAIN 322 行链式合成普查（v2，编译器级名字通道）。

判据（真端表形状权威 + 客户端强二证据 + XML 仅对照）：
  A. 表级：链深度（talk_npc1..3）、物品轴/过场/item_check → 波次切分（A 纯链 / B 链+物品复合）；
  B. 名字证据（编译器级三通道）：npc_templates name_desc 裸查 + `npc_` 去前缀别名 +
     客户端交付登记 quest_client_reward_npcs.tsv（reward 复合势力名专用）；
  C. XML 对照形状：stage 阶梯签名（链 NPC@SETPROk 推进、var0 阶梯、QUEST_SELECT/SELECT2_x 纯页视图）；
  D. 客户端证据：quest-dialog-pages.csv 的 select2 系页 + SETPRO 按钮存在性。

输出：p0c10e-talk-chain-census.tsv
  quest_id / depth / wave / names(| 连接，编译器级解析态) / depth_echo / client_select2 /
  client_setpro / xml_ladder / verdict
"""
import re
import collections
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
TALK_TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml'
RETENTION = REPO / 'src/test/resources/quest/retail-xml-retention.tsv'
NPC_DIR = REPO / 'src/main/resources/aion/data/static_data/npcs'
QDIR = REPO / 'src/main/resources/aion/data/static_data/quest/definitions/quests'
CLIENT_CSV = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-pages.csv'
REWARD_REG = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_reward_npcs.tsv'
OUT = HERE / 'p0c10e-talk-chain-census.tsv'

chain_ids = []
for line in RETENTION.read_text(encoding='utf-8').splitlines():
    if line.startswith('#') or not line.strip():
        continue
    p = line.split('\t')
    if len(p) >= 4 and p[2] == 'SimpleTalk' and p[3] == 'SEMANTIC_GAP:RETAIL_TALK_CHAIN':
        chain_ids.append(int(p[0]))
chain_ids.sort()

rows = {}
xml = TALK_TABLE.read_text(encoding='utf-8')
for m in re.finditer(r'<id id="(\d+)">(.*?)</id>', xml, re.S):
    qid, body = int(m.group(1)), m.group(2)

    def t(tag, b=body):
        mm = re.search(r'<%s>([^<]*)</%s>' % (tag, tag), b)
        return mm.group(1).strip() if mm else None
    steps = [t('talk_npc%d' % i) for i in (1, 2, 3) if t('talk_npc%d' % i)]
    rows[qid] = dict(acq=t('acquired_npc_name'), rew=t('reward_npc_name'), steps=steps,
        gives=bool(re.search(r'<give_item\d?>', body)), removes=bool(re.search(r'<remove_item\d?>', body)),
        item_check=bool(re.search(r'<item_check>', body)), cut=t('cutsceneid1') is not None)

# 名字通道 1+2：name_desc 裸查 + npc_ 去前缀别名（同 RetailNpcNameIndex）
name_map = collections.defaultdict(set)
for npc_file in sorted(NPC_DIR.glob('npc_template_*.xml')):
    text = npc_file.read_text(encoding='utf-8', errors='ignore')
    for m in re.finditer(r'<npc_template\b[^>]*>', text):
        tag = m.group()
        nm = re.search(r'name_desc="([^"]*)"', tag)
        nid = re.search(r'\bnpc_id="(\d+)"', tag)
        if nm and nid:
            name_map[nm.group(1)].add(int(nid.group(1)))
            stem = nm.group(1)
            if stem.lower().startswith('npc_'):
                name_map[stem[4:]].add(int(nid.group(1)))

# 名字通道 3：客户端交付 NPC 集登记（reward 复合势力名专用）
client_reward = {}
for line in REWARD_REG.read_text(encoding='utf-8').splitlines():
    if line.startswith('#') or not line.strip():
        continue
    parts = line.split('\t')
    client_reward[int(parts[0])] = parts[1]


def resolve(name, quest_id, role):
    if name is None:
        return '-'
    if name.startswith('_') and name.endswith('_') and len(name) > 2:
        return 'SENTINEL:' + name
    ids = name_map.get(name, set())
    if len(ids) == 1:
        return str(next(iter(ids)))
    if role == 'rew' and quest_id in client_reward:
        return 'CLIENT:' + client_reward[quest_id]
    if len(ids) > 1:
        return 'AMBIGUOUS(%d):%s' % (len(ids), name)
    return 'UNRESOLVED:' + name
    # 注意：不得用 ids.pop()——会破坏性消费集合，同名第二次解析即误报 UNRESOLVED（v1 教训）。


# 客户端证据：select2 系页 + SETPRO 按钮
client_pages = collections.defaultdict(set)
client_setpro = set()
with CLIENT_CSV.open(encoding='utf-8-sig') as fh:
    header = fh.readline().rstrip('\n').split(',')
    col = {name: i for i, name in enumerate(header)}
    for line in fh:
        p = line.rstrip('\n').split(',')
        if len(p) < len(header):
            continue
        qid = int(p[col['quest_id']])
        client_pages[qid].add(p[col['html_page_name']])
        if 'setpro' in p[col['page_constant']].lower():
            client_setpro.add(qid)


def xml_signature(qid):
    text = (QDIR / ('%d.xml' % qid)).read_text(encoding='utf-8')
    nodes = {}
    for m in re.finditer(r'<node label="([^"]+)" status="([A-Z]+)">\s*<var name="var0" value="(\d+)"/>', text):
        nodes[m.group(1)] = (m.group(2), int(m.group(3)))
    adv = []
    views = 0
    others = []
    for m in re.finditer(r'<transition source="([^"]+)" target="([^"]+)">\s*<event>\s*'
                         r'<dialog type="TALK_TO_NPC" npc-id="(\d+)" action="([A-Z0-9_]+)"/>', text):
        src, dst, npc, action = m.group(1), m.group(2), int(m.group(3)), m.group(4)
        if action.startswith('SETPRO') and src in nodes and dst in nodes \
                and nodes[src][1] == nodes[dst][1] == 'START' and nodes[dst][2] == nodes[src][2] + 1:
            adv.append((npc, action, nodes[src][2], nodes[dst][2]))
        elif action == 'QUEST_SELECT' or action.startswith('SELECT2'):
            if src == dst:
                views += 1
            else:
                others.append('%s@%d %s->%s' % (action, npc, src, dst))
        elif not (action.startswith('QUEST_') or action in ('FINISH_DIALOG', 'CLOSE_DIALOG')):
            others.append('%s@%d' % (action, npc))
    if others:
        return 'DEVIATION:' + ';'.join(sorted(set(others))[:3])
    ladder = '->'.join('%d:%d>%d' % (npc, f, tv) for npc, a, f, tv in sorted(adv)) if adv else 'NONE'
    return 'LADDER:%s|views=%d' % (ladder, views)


waves = collections.Counter()
verdicts = collections.Counter()
out = ['# P0c-10e SimpleTalk 链式合成普查（322 行 RETAIL_TALK_CHAIN；v2 编译器级名字通道）',
    '# quest_id\tdepth\twave\tnames\tclient_select2\tclient_setpro\txml_ladder\tverdict']
for qid in chain_ids:
    r = rows[qid]
    compound = r['gives'] or r['removes'] or r['item_check'] or r['cut']
    wave = 'B_COMPOUND' if compound else 'A_PURE_CHAIN'
    waves[wave] += 1
    names = [resolve(r['acq'], qid, 'acq')] \
        + [resolve(s, qid, 't%d' % (i + 1)) for i, s in enumerate(r['steps'])] \
        + [resolve(r['rew'], qid, 'rew')]
    name_ok = all(n.isdigit() or n == '-' or n.startswith(('SENTINEL', 'CLIENT')) for n in names)
    pages = client_pages.get(qid, set())
    sel2 = any(pg.startswith('select2') for pg in pages)
    setpro = qid in client_setpro
    sig = xml_signature(qid)
    verdict = 'OK_' + wave
    if not name_ok:
        verdict = 'NAME_EVIDENCE_NEEDED'
    verdicts[verdict] += 1
    out.append('%d\t%d\t%s\t%s\t%d\t%d\t%s\t%s' % (
        qid, len(r['steps']), wave, '|'.join(names), 1 if sel2 else 0, 1 if setpro else 0, sig, verdict))

OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')
print('普查 -> %s（%d 行）' % (OUT, len(chain_ids)))
print('波次：', dict(waves))
print('判定：', dict(verdicts))
print('名字证据待补的行（样例）：',
    [int(l.split('\t')[0]) for l in out[2:] if l.endswith('NAME_EVIDENCE_NEEDED')][:12])
