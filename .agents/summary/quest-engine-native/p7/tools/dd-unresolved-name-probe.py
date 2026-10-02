#!/usr/bin/env python3
# P7 步 d2 探针：DD 切换集 hunt/talk/talkfobj 载荷名逐名裁定（只读）。
# 复算 NativeNpcNameResolver 的解析序（name_desc → name → 别名台账），
# 对仍失败的名单按真端事实分类：quest_ai_name 组别名 / WorldId 副本世界名 / 真端同名 / 无匹配。
# Read-only probe: recompute the resolver order over all switch-set payload names and
# classify each failure against retail facts (quest_ai_name / world names / retail <name>).
import re, sys, glob, os
from collections import OrderedDict

REPO = '/Users/mc/IdeaProjects/AionEmu-test'
RETAIL = '/Users/mc/IdeaProjects/58Server/Map/XML'
NPC_DIR = REPO + '/src/main/resources/aion/data/static_data/npcs'
DD_TABLE = REPO + '/src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml'
RETENTION = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv'
ALIASES = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-npc-name-aliases.tsv'
OUT = os.path.dirname(os.path.abspath(__file__)) + '/../dd-unresolved-name-adjudication.tsv'

ATTR = re.compile(r'\b(npc_id|name|name_desc|quest_ai_name)="([^"]*)"')

def load_local_npcs():
    by_desc, by_name, by_all = {}, {}, {}
    for path in sorted(glob.glob(NPC_DIR + '/npc_template_*.xml')):
        raw = open(path, encoding='utf-8', errors='replace').read()
        for m in re.finditer(r'<npc_template\b([^>]*)>', raw):
            npc_id = name = desc = None
            for am in ATTR.finditer(m.group(1)):
                if am.group(1) == 'npc_id': npc_id = am.group(2)
                elif am.group(1) == 'name': name = am.group(2)
                elif am.group(1) == 'name_desc': desc = am.group(2)
            if npc_id is None: continue
            i = int(npc_id)
            for key, nm in (('d', desc), ('n', name)):
                if nm:
                    k = nm.strip().lower()
                    if not k: continue
                    lst = by_all.setdefault(k, [])
                    if i not in lst: lst.append(i)
                    tgt = by_desc if key == 'd' else by_name
                    lst2 = tgt.setdefault(k, [])
                    if i not in lst2: lst2.append(i)
    return by_desc, by_name, by_all

def load_aliases(path=None):
    m = {}
    for line in open(path or ALIASES, encoding='utf-8'):
        line = line.strip()
        if not line or line.startswith('#'): continue
        parts = line.split('\t')
        if len(parts) >= 2 and parts[1].strip():
            m[parts[0].strip().lower()] = [int(x) for x in parts[1].split(',') if x.strip()]
    return m

def load_retail_npcs():
    """真端 npcs.xml：子元素 <quest_ai_name>/<name> 索引（归类证据用）。"""
    raw = open(RETAIL + '/npcs.xml', encoding='utf-16').read()
    qai, names = {}, set()
    for m in re.finditer(r'<npc>(.*?)</npc>', raw, re.S):
        body = m.group(1)
        idm = re.search(r'<id>(\d+)</id>', body)
        nid = int(idm.group(1)) if idm else 0
        nm = re.search(r'<name>([^<]*)</name>', body)
        if nm and nm.group(1).strip():
            names.add(nm.group(1).strip().lower())
        qa = re.search(r'<quest_ai_name>([^<]*)</quest_ai_name>', body)
        if qa and qa.group(1).strip() and nid:
            qai.setdefault(qa.group(1).strip().lower(), [])
            if nid not in qai[qa.group(1).strip().lower()]:
                qai[qa.group(1).strip().lower()].append(nid)
    return qai, names

def load_monster_target_keys():
    """镜像 RetailNpcNameIndex.addMonsterTargetAliases 的硬编码击杀目标键。
    这些名字的正确展开轴是「击杀目标等价集」（P0a 家族裁定），不是 quest_ai_name 组员；
    步 d2 曾因本探针缺这个镜像把 8 个 boss 名错按 quest_ai_name 写进台账，撞旧车道
    addVersionedNpcIdAliases 的重复闸引发 load 失败重试风暴（等集幂等守卫已补）。"""
    java = REPO + '/src/main/java/com/aionemu/gameserver/questEngine/retail/RetailNpcNameIndex.java'
    out = {}
    raw = open(java, encoding='utf-8').read()
    m = re.search(r'addMonsterTargetAliases\(Map<String, Set<Integer>> byName\) \{(.*?)\n\t\}', raw, re.S)
    if not m:
        return out
    for pm in re.finditer(r'byName\.put\("([^"]+)",\s*Set\.of\(([^)]*)\)', m.group(1), re.S):
        out[pm.group(1).strip().lower()] = True
    return out

def load_declared_group_keys():
    """旧车道对话名组表（quest/retail-quest-ai-name-groups.tsv）已声明的组键。
    组键的权威载体 = 该组表；台账不得收录组键（台账行会进旧车道 spawn 通道，
    破坏通道互斥闸——QE-133 同类事故的第二形态）。原生车道经 NativeNpcNameResolver
    的组通道直读同一张表。"""
    out = set()
    path = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-quest-ai-name-groups.tsv'
    for line in open(path, encoding='utf-8'):
        s = line.strip()
        if not s or s.startswith('#') or s.startswith('quest_ai_name\t'): continue
        out.add(s.split('\t')[0].strip().lower())
    return out

def load_world_names():
    """真端 WorldId.xml：<data id="WORLDID" ...>NAME</data> 世界名。"""
    out = set()
    for path in glob.glob(RETAIL + '/**/WorldId.xml', recursive=True):
        try:
            raw = open(path, encoding='utf-16', errors='replace').read()
        except (OSError, UnicodeError):
            continue
        for m in re.finditer(r'<data\s+id="\d+"[^>]*>([^<>]+)</data>', raw):
            name = m.group(1).strip().lower()
            if name:
                out.add(name)
    return out

def load_item_names():
    """本仓物品模板 name_desc 索引（itemplay 载荷解析）。"""
    out = set()
    for path in glob.glob(REPO + '/src/main/resources/aion/data/static_data/items/item/*.xml'):
        raw = open(path, encoding='utf-8', errors='replace').read()
        for m in re.finditer(r'name_desc="([^"]+)"', raw):
            out.add(m.group(1).strip().lower())
    return out

def load_zone_names():
    """本仓进区注册（zones_retail_enterarea.xml 的 zone name）。"""
    out = set()
    path = REPO + '/src/main/resources/aion/data/static_data/zones/zones_retail_enterarea.xml'
    raw = open(path, encoding='utf-8', errors='replace').read()
    for m in re.finditer(r'<zone\b[^>]*\bname="([^"]+)"', raw):
        out.add(m.group(1).strip().lower())
    return out

def load_switch_set():
    owners = {}
    for line in open(RETENTION, encoding='utf-8'):
        line = line.strip()
        if not line or line.startswith('#'): continue
        cols = line.split('\t')
        if len(cols) >= 2 and cols[0].isdigit():
            owners[int(cols[0])] = cols[1]
    raw = open(DD_TABLE, encoding='utf-8').read()
    rows = {}
    for m in re.finditer(r'<quest_data_driven>(.*?)</quest_data_driven>', raw, re.S):
        body = m.group(1)
        idm = re.search(r'<id>(\d+)</id>', body)
        if not idm: continue
        qid = int(idm.group(1))
        if owners.get(qid) != 'RETAIL_TABLE': continue
        steps = []
        for dm in re.finditer(r'<data>(.*?)</data>', body, re.S):
            d = dm.group(1)
            cat = re.search(r'<category_progress_>([^<]+)</category_progress_>', d)
            if not cat: continue
            cols = {}
            for cm in re.finditer(r'<(value\d+_progress_)>([^<]*)</\1>', d):
                cols[cm.group(1)] = cm.group(2)
            steps.append((cat.group(1).strip().lower(), cols))
        rows[qid] = steps
    return rows

TRAILING = re.compile(r'^(.*?)(?:\s*,\s*|\s+)(\d+)$')

def hunt_names(payload):
    """复算运行时 parseGroups + 空白拆分回退后的名字集。"""
    names = []
    for raw in payload.split(';'):
        t = raw.strip()
        if not t: continue
        mm = TRAILING.match(t)
        if mm: t = mm.group(1)
        for name in t.split(','):
            n = name.strip()
            if n: names.append(n)
    return names

def resolve(name, by_desc, by_name, by_all, aliases):
    k = name.strip().lower()
    if not k: return True
    if by_desc.get(k) or by_name.get(k) or aliases.get(k): return True
    # 空格分隔名单回退：逐 token 解析（复算 resolveMonsters）
    if any(c.isspace() for c in k):
        return all(resolve(tok, by_desc, by_name, by_all, aliases) for tok in k.split())
    return bool(by_all.get(k))

def main():
    emit = '--emit-aliases' in sys.argv
    aliases_override = None
    if '--aliases' in sys.argv:
        aliases_override = sys.argv[sys.argv.index('--aliases') + 1]
    by_desc, by_name, by_all = load_local_npcs()
    aliases = load_aliases(aliases_override)
    qai, retail_names = load_retail_npcs()
    worlds = load_world_names()
    kill_targets = load_monster_target_keys()
    declared_groups = load_declared_group_keys()
    zones = load_zone_names()
    items = load_item_names()
    rows = load_switch_set()
    unresolved = OrderedDict()
    for qid in sorted(rows):
        for idx, (cat, cols) in enumerate(rows[qid]):
            names = []
            if cat == 'hunt':
                names = hunt_names(cols.get('value0_progress_', ''))
            elif cat in ('talk', 'collectitem'):
                n = cols.get('value0_progress_', '').strip()
                if n: names = [n]
            elif cat == 'talkfobj':
                names = hunt_names(cols.get('value0_progress_', ''))
            elif cat == 'itemplay':
                n = cols.get('value0_progress_', '').strip()
                mm = TRAILING.match(n)
                if mm: n = mm.group(1).strip()
                if n and n.lower() not in items:
                    names = [n]  # 物品索引未命中才入裁定（itemplay 载荷 = 物品名）。
                else:
                    names = []
            elif cat == 'enterarea':
                n = cols.get('value0_progress_', '').strip()
                if n and n.lower() not in zones:
                    names = [n]  # 进区别名未登记成区（含真端缺席 LF6 面，§10.3-#23）。
                else:
                    names = []
            for n in names:
                if resolve(n, by_desc, by_name, by_all, aliases): continue
                # 空白名单拆分后失败的 token
                toks = [n]
                if any(c.isspace() for c in n):
                    toks = [t for t in n.split() if not resolve(t, by_desc, by_name, by_all, aliases)] or [n]
                for t in toks:
                    k = t.strip().lower()
                    if k not in unresolved:
                        unresolved[k] = [qid, cat, idx]
    lines = ['name\tquest\tstep_kind\tstep_index\tretail_quest_ai_name\tretail_exact_name\tworld_name\tqai_npc_ids\tverdict']
    for k, (qid, cat, idx) in unresolved.items():
        in_qai = 'Y' if k in qai else ''
        in_names = 'Y' if k in retail_names else ''
        in_world = 'Y' if k in worlds else ''
        ids = ','.join(str(i) for i in qai.get(k, []))
        if k in declared_groups:
            verdict = 'GROUP_TABLE_DECLARED_CANDIDATE'
        elif k in kill_targets:
            verdict = 'KILL_TARGET_HARDCODE_CANDIDATE'
        elif in_qai:
            verdict = 'QUEST_AI_NAME_GROUP_CANDIDATE'
        elif in_world and not in_names:
            verdict = 'WORLD_WIDE_KILL_CANDIDATE'
        elif in_names:
            verdict = 'RETAIL_NAME_LOCAL_MISS'
        else:
            verdict = 'NO_RETAIL_MATCH'
        lines.append(f"{k}\t{qid}\t{cat}\t{idx}\t{in_qai}\t{in_names}\t{in_world}\t{ids}\t{verdict}")
    with open(OUT, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')
    if emit:
        alias_path = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-npc-name-aliases.tsv'
        existing = {}
        for line in open(alias_path, encoding='utf-8'):
            s = line.strip()
            if not s or s.startswith('#'): continue
            parts = s.split('\t')
            if len(parts) >= 2:
                existing[parts[0].strip().lower()] = parts[1].strip()
        added = 0
        with open(alias_path, 'a', encoding='utf-8') as f:
            f.write('# P7 步 d2（2026-10-02）：DD 切换集载荷名的真端 quest_ai_name 组（dd-unresolved-name-probe.py 生成）。\n')
            for k, (qid, cat, idx) in unresolved.items():
                ids = qai.get(k)
                if not ids or k in existing or k in kill_targets or k in declared_groups: continue
                f.write(k + '\t' + ','.join(str(i) for i in sorted(ids)) + '\n')
                added += 1
        print('alias rows appended:', added)
    print('local npcs indexed names:', len(by_all))
    print('switch rows:', len(rows), ' unresolved names:', len(unresolved))
    from collections import Counter
    print(Counter(l.split('\t')[-1] for l in lines[1:]))
    print('written', OUT)

if __name__ == '__main__':
    main()
