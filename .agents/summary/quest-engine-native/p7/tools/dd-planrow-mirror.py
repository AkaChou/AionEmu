#!/usr/bin/env python3
# P7 步 d2 复算镜像：逐行精确镜像 DataDrivenNativeRuntime.planRow 的解析与判定
# （parseGroups/resolveMonsters/载荷分支），输出切换集逐类已路由步数 + 冻结行清单，
# 用于与 Java 门禁测试的实测分桶对拍定位差行。只读，不写仓库数据。
# Offline mirror of DataDrivenNativeRuntime.planRow: reproduces the exact parse/resolve
# paths per kind and prints per-kind routed step counts plus the frozen-row list, to
# cross-check the Java gate counts and locate any divergent row. Read-only.
import re, glob, sys
from collections import OrderedDict

REPO = '/Users/mc/IdeaProjects/AionEmu-test'
NPC_DIR = REPO + '/src/main/resources/aion/data/static_data/npcs'
ITEM_DIR = REPO + '/src/main/resources/aion/data/static_data/items/item'
ZONE_FILE = REPO + '/src/main/resources/aion/data/static_data/zones/zones_retail_enterarea.xml'
DD_TABLE = REPO + '/src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml'
RETENTION = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv'
ALIASES = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-npc-name-aliases.tsv'

ATTR = re.compile(r'\b(npc_id|name|name_desc|quest_ai_name)="([^"]*)"')
TRAILING = re.compile(r'^(.*?)(?:\s*,\s*|\s+)(\d+)$')
ABSENT = {"lf6_sensoryarea_q15551_atob","lf6_sensoryarea_q15551_btoa","lf6_sensoryarea_q15552_atod",
          "lf6_sensoryarea_q15552_dtoa","lf6_sensoryarea_q15553_atof","lf6_sensoryarea_q15553_ftoa",
          "lf6_sensoryarea_q15554_atoh","lf6_sensoryarea_q15554_htoa","lf6_sensoryarea_q15601a_dynamic_env",
          "lf6_sensoryarea_q15601b","lf6_sensoryarea_q15602a_dynamic_env","lf6_sensoryarea_q15604a_dynamic_env",
          "lf6_sensoryarea_q15605a_named","lf6_sensoryarea_q15608a_dynamic_env"}

def load_local_npcs():
    by_desc, by_name = {}, {}
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
            for tgt, nm in ((by_desc, desc), (by_name, name)):
                if nm:
                    k = nm.strip().lower()
                    if k and i not in tgt.setdefault(k, []): tgt[k].append(i)
    return by_desc, by_name

def load_aliases():
    m = {}
    for line in open(ALIASES, encoding='utf-8'):
        line = line.strip()
        if not line or line.startswith('#'): continue
        parts = line.split('\t')
        if len(parts) >= 2 and parts[1].strip():
            m[parts[0].strip().lower()] = [int(x) for x in parts[1].split(',') if x.strip()]
    return m

def load_groups():
    """镜像 NativeNpcNameResolver 的对话名组通道（组键 → 成员 name_desc/name 展开，声明序）。"""
    m = {}
    for line in open(REPO + '/src/main/resources/quest/retail-quest-ai-name-groups.tsv', encoding='utf-8'):
        line = line.strip()
        if not line or line.startswith('#') or line.startswith('quest_ai_name\t'): continue
        parts = line.split('\t')
        if len(parts) < 2 or not parts[1].strip(): continue
        m[parts[0].strip().lower()] = [x.strip().lower() for x in parts[1].split(',') if x.strip()]
    return m

def load_items():
    out = {}
    for path in sorted(glob.glob(ITEM_DIR + '/*.xml')):
        raw = open(path, encoding='utf-8', errors='replace').read()
        for m in re.finditer(r'<item_template\b([^>]*)>', raw):
            item_id = desc = None
            for am in re.finditer(r'\b(id|name_desc)="([^"]*)"', m.group(1)):
                if am.group(1) == 'id': item_id = int(am.group(2))
                elif am.group(1) == 'name_desc': desc = am.group(2)
            if item_id is not None and desc and desc.strip():
                out.setdefault(desc.strip().lower(), item_id)
    return out

def load_zones():
    out = set()
    raw = open(ZONE_FILE, encoding='utf-8', errors='replace').read()
    for m in re.finditer(r'<zone\b[^>]*\bname="([^"]+)"', raw):
        out.add(m.group(1).strip().upper())
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
    rows = OrderedDict()
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
            for cm in re.finditer(r'<value(\d+)_progress_>([^<]*)</value\1_progress_>', d):
                cols[int(cm.group(1))] = cm.group(2)
            steps.append((cat.group(1).strip().lower(), cols))
        rows[qid] = steps
    return rows

def parse_groups(payload):
    groups = []
    if not payload or not payload.strip(): return groups
    for raw in payload.split(';'):
        text = raw.strip()
        if not text: continue
        trailing = 0
        mm = TRAILING.match(text)
        if mm:
            trailing = int(mm.group(2)); text = mm.group(1)
        names = [n.strip() for n in text.split(',') if n.strip()]
        if names: groups.append((names, trailing))
    return groups

def resolve_monsters(name, by_desc, by_name, aliases, groups=None):
    """resolveMonsterIds + 空白拆分回退（返回 None 表示 fail）；组通道兜底（与 Java 同序）。"""
    k = name.strip().lower()
    direct = by_desc.get(k) or by_name.get(k) or aliases.get(k) or []
    if not direct and groups:
        ids = []
        for mem in groups.get(k, []):
            ids.extend(by_desc.get(mem) or by_name.get(mem) or [])
        if ids: return ids
    if direct: return direct
    if any(c.isspace() for c in k):
        out = []
        for tok in k.strip().split():
            part = by_desc.get(tok) or by_name.get(tok) or aliases.get(tok) or []
            if not part: return None
            out.extend(part)
        if out: return out
    return None

def main():
    by_desc, by_name = load_local_npcs()
    aliases = load_aliases()
    name_groups = load_groups()
    items = load_items()
    zones = load_zones()
    rows = load_switch_set()
    routed_steps, frozen = {}, []
    unresolved_all = {}
    for qid in sorted(rows):
        steps = rows[qid]
        freeze = None
        step_kinds = []
        for idx, (cat, cols) in enumerate(steps):
            payload = cols.get(0, '')
            col = lambda n: cols.get(n)
            if freeze: break
            step_kinds.append(cat)
            if cat == 'hunt' or cat == 'talkfobj':
                groups = parse_groups(payload)
                if not groups:
                    freeze = 'PAYLOAD_INVALID'; break
                for gi, (names, trailing) in enumerate(groups):
                    ids = []
                    for n in names:
                        got = resolve_monsters(n, by_desc, by_name, aliases, name_groups)
                        if got is None:
                            freeze = 'NAME_UNRESOLVED'
                            unresolved_all.setdefault(n.strip().lower(), []).append((qid, cat, idx))
                            break
                        ids.extend(got)
                    if freeze: break
            elif cat in ('talk', 'collectitem'):
                npc_ids = []
                for names, trailing in parse_groups(payload):
                    for n in names:
                        got = resolve_monsters(n, by_desc, by_name, aliases, name_groups)
                        if got is None:
                            freeze = 'NAME_UNRESOLVED'
                            unresolved_all.setdefault(n.strip().lower(), []).append((qid, cat, idx))
                            break
                        npc_ids.extend(got)
                    if freeze: break
                if not freeze and not npc_ids:
                    freeze = 'PAYLOAD_INVALID'; break
            elif cat == 'itemplay':
                text = payload.strip()
                count = 1
                mm = TRAILING.match(text)
                if mm:
                    count = int(mm.group(2)); text = mm.group(1).strip()
                if not text:
                    freeze = 'PAYLOAD_INVALID'; break
                if text.strip().lower() not in items:
                    freeze = 'NAME_UNRESOLVED'
                    unresolved_all.setdefault(text.strip().lower(), []).append((qid, cat, idx)); break
                if count <= 0:
                    freeze = 'PAYLOAD_INVALID'; break
            elif cat == 'enterarea':
                alias = payload.strip()
                if alias.upper() not in zones:
                    freeze = 'ZONE_ABSENT' if alias.lower() in ABSENT else 'ZONE_UNRESOLVED'
                    unresolved_all.setdefault(alias.lower(), []).append((qid, cat, idx)); break
            elif cat == 'enterworld':
                try: wid = int(payload.strip())
                except ValueError: wid = -1
                if wid <= 0:
                    freeze = 'PAYLOAD_INVALID'; break
            elif cat == 'pvp':
                try: target = int(payload.strip())
                except ValueError: target = -1
                if target <= 0:
                    freeze = 'PAYLOAD_INVALID'; break
                for cn in (1, 2, 3):
                    v = col(cn)
                    if v is not None and v.strip():
                        try: int(v.strip())
                        except ValueError:
                            freeze = 'PAYLOAD_INVALID'; break
                if freeze: break
            else:
                freeze = 'PAYLOAD_INVALID'; break
        if freeze:
            frozen.append((qid, freeze))
        else:
            for cat in step_kinds:
                routed_steps[cat] = routed_steps.get(cat, 0) + 1
    print('routed rows:', sum(1 for q in rows if q not in {f[0] for f in frozen}))
    print('routed steps per kind:', dict(sorted(routed_steps.items())))
    print('frozen rows:', len(frozen), dict(sorted({r: sum(1 for _, fr in frozen if fr == r) for r in {fr for _, fr in frozen}}.items())))
    for qid, fr in sorted(frozen):
        print('  frozen', qid, fr)
    print('unresolved payload names:', len(unresolved_all))
    for k, v in sorted(unresolved_all.items()):
        print('  unresolved', k, v[:3])
    # 货运：找 talk/collectitem 载荷里「整串解析 vs parseGroups」会分道的地方
    print('--- talk/collectitem payloads with comma/semicolon/trailing-int shape ---')
    for qid in sorted(rows):
        for idx, (cat, cols) in enumerate(rows[qid]):
            if cat in ('talk', 'collectitem'):
                p = cols.get(0, '').strip()
                if (',' in p or ';' in p or TRAILING.match(p)) and qid not in {f[0] for f in frozen}:
                    print('  shape', qid, cat, idx, repr(p))

if __name__ == '__main__':
    main()
