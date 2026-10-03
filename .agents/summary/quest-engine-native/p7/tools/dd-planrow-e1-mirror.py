#!/usr/bin/env python3
# P7 步 e1 复算镜像：在 dd-planrow-mirror.py（步 d2 行内轴）之上加两条 e1 轴——
# ① 接取轴（con_quest/con_quest_list → ACQUIRE_CONDITION_UNFACED；acquire 参数名解析失败 →
#    NAME_UNRESOLVED 整行冻结，与 Java create() 的「先裁定后路由」同序）；
# ② 附加动作轴（已落面 case 1/2/4 解析，未落面 case 3/5/6/7/8/9/10 → ACTION_UNFACED）。
# 输出：routed 行/步分桶、冻结原因分桶、unresolved 名集、接取兴趣与动作面直方图。
# 只读，不写仓库数据。
# P7 step e1 offline mirror of DataDrivenNativeRuntime: the d2 per-step axes plus the e1
# acquire axis (condition columns + acquire-parameter resolution, frozen before routing)
# and the extra-action axis (faced cases 1/2/4 parsed, unfaced cases freeze). Read-only.
import re, glob, sys
import xml.etree.ElementTree as ET
from collections import OrderedDict, Counter

REPO = '/Users/mc/IdeaProjects/AionEmu-test'
NPC_DIR = REPO + '/src/main/resources/aion/data/static_data/npcs'
ITEM_DIR = REPO + '/src/main/resources/aion/data/static_data/items/item'
ZONE_FILE = REPO + '/src/main/resources/aion/data/static_data/zones/zones_retail_enterarea.xml'
DD_TABLE = REPO + '/src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml'
RETENTION = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.xml'
ALIASES = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-npc-name-aliases.xml'
GROUPS_XML = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-quest-ai-name-groups.xml'

ATTR = re.compile(r'\b(npc_id|name|name_desc|quest_ai_name)="([^"]*)"')
TRAILING = re.compile(r'^(.*?)(?:\s*,\s*|\s+)(\d+)$')
ABSENT = {"lf6_sensoryarea_q15551_atob","lf6_sensoryarea_q15551_btoa","lf6_sensoryarea_q15552_atod",
          "lf6_sensoryarea_q15552_dtoa","lf6_sensoryarea_q15553_atof","lf6_sensoryarea_q15553_ftoa",
          "lf6_sensoryarea_q15554_atoh","lf6_sensoryarea_q15554_htoa","lf6_sensoryarea_q15601a_dynamic_env",
          "lf6_sensoryarea_q15601b","lf6_sensoryarea_q15602a_dynamic_env","lf6_sensoryarea_q15604a_dynamic_env",
          "lf6_sensoryarea_q15605a_named","lf6_sensoryarea_q15608a_dynamic_env"}
# 真端 FUN_180c4b980 按 kind 解析的载荷列（DataDrivenQuestTable.PAYLOAD_COLUMNS）。
PAYLOAD_COLUMNS = {'hunt': {0}, 'collectitem': {0,1,2,3,4,5}, 'pvp': {0,1,2,3}, 'talk': {0},
                   'enterarea': {0}, 'itemplay': {0}, 'enterworld': {0}, 'talkfobj': {0}}
# 真端 LoadExtraAction guard 放行的附加动作列（DataDrivenQuestTable.EXTRA_ACTION_COLUMNS）。
EXTRA_ACTION_COLUMNS = {'hunt': {4,5}, 'itemplay': set(range(1,11)), 'talk': set(range(1,11)),
                        'enterarea': set(range(1,11)), 'enterworld': set(range(1,11)),
                        'talkfobj': set(range(1,11))}
# 列号 → ExtraAction（列 7/8 都映射 MESSAGE；与 DataDrivenQuestTable.ExtraAction 同构）。
def extra_action(column):
    return {1:'GIVE_ITEMS',2:'REMOVE_ITEMS',3:'TELEPORT',4:'CUTSCENE',5:'SPAWN',6:'DELAY',
            7:'MESSAGE',8:'MESSAGE',9:'ENTER_INSTANCE',10:'TIMER'}.get(column)

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
    for row in ET.parse(ALIASES).getroot().findall('npc_name_alias'):
        name = (row.findtext('name') or '').strip().lower()
        ids_text = (row.findtext('npc_ids') or '').strip()
        if name and ids_text:
            m[name] = [int(x) for x in ids_text.split(',') if x.strip()]
    return m

def load_groups():
    m = {}
    for row in ET.parse(GROUPS_XML).getroot().findall('quest_ai_name_group'):
        name = (row.findtext('quest_ai_name') or '').strip().lower()
        members = (row.findtext('member_name_descs') or '').strip()
        if name and members:
            m[name] = [x.strip().lower() for x in members.split(',') if x.strip()]
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
    for row in ET.parse(RETENTION).getroot().findall('quest'):
        quest_id = (row.findtext('quest_id') or '').strip()
        if quest_id.isdigit():
            owners[int(quest_id)] = (row.findtext('owner') or '').strip()
    raw = open(DD_TABLE, encoding='utf-8').read()
    rows = OrderedDict()
    for m in re.finditer(r'<quest_data_driven>(.*?)</quest_data_driven>', raw, re.S):
        body = m.group(1)
        idm = re.search(r'<id>(\d+)</id>', body)
        if not idm: continue
        qid = int(idm.group(1))
        if owners.get(qid) != 'RETAIL_TABLE': continue
        def field(tag):
            fm = re.search(r'<%s>([^<]*)</%s>' % (tag, tag), body)
            return fm.group(1).strip() if fm else ''
        acquire = field('category_acquire_').lower()
        acquire_param = field('value0_acquire_')
        con_quest = field('con_quest')
        con_quest_list = field('con_quest_list')
        steps = []
        for dm in re.finditer(r'<data>(.*?)</data>', body, re.S):
            d = dm.group(1)
            cat = re.search(r'<category_progress_>([^<]+)</category_progress_>', d)
            if not cat: continue
            cols = {}
            for cm in re.finditer(r'<value(\d+)_progress_>([^<]*)</value\1_progress_>', d):
                v = cm.group(2).strip()
                if v: cols[int(cm.group(1))] = v
            steps.append((cat.group(1).strip().lower(), cols))
        rows[qid] = (acquire, acquire_param, con_quest, con_quest_list, steps)
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

def resolve_monsters(name, by_desc, by_name, aliases, groups):
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

def scan_faced_actions(cat, cols, items, unresolved, qid):
    """镜像 DataDrivenNativeRuntime.scanFacedActions：返回冻结原因或 None。"""
    payloads = PAYLOAD_COLUMNS[cat]
    plans = []
    for column in sorted(cols):
        if column in payloads:
            continue
        action = extra_action(column)
        if action is None:
            continue  # 装载期 guard 外列在 Java 侧直接抛异常，离线镜像按活数据不会出现。
        text = cols[column].strip()
        if action in ('GIVE_ITEMS', 'REMOVE_ITEMS'):
            tokens = re.split(r'[,\s]+', text)
            if len(tokens) % 2 != 0:
                return 'ACTION_UNFACED'
            for i in range(0, len(tokens), 2):
                try:
                    count = int(tokens[i+1])
                except ValueError:
                    return 'ACTION_UNFACED'
                if count <= 0:
                    return 'ACTION_UNFACED'
                if tokens[i].strip().lower() not in items:
                    unresolved.add(tokens[i].strip().lower())
                    return 'NAME_UNRESOLVED'
                plans.append(action)
        elif action == 'CUTSCENE':
            tokens = re.split(r'[,\s]+', text)
            if len(tokens) < 2:
                return 'ACTION_UNFACED'
            try:
                int(tokens[1])
            except ValueError:
                return 'ACTION_UNFACED'
            token = tokens[0].lower()
            if token in ('cutscene', 'cutscene2'):
                plans.append('CUTSCENE')
            elif token in ('movie', 'movie2'):
                plans.append('MOVIE')
            # 其余词形 = 真端 Wrong Type!!（跳过不冻结）
        else:
            return 'ACTION_UNFACED'
    return None

def acquire_axis(acquire, param, by_desc, by_name, aliases, groups, items, unresolved):
    """镜像 DataDrivenNativeRuntime.acquirePlan：返回 kind 或 None（None = 整行冻结 NAME_UNRESOLVED）。"""
    acquire = (acquire or '').strip()
    param = param or ''
    if acquire == 'talk':
        npc_ids = set()
        for names, _trailing in parse_groups(param):
            for n in names:
                got = resolve_monsters(n, by_desc, by_name, aliases, groups)
                if got is None:
                    unresolved.add(n.strip().lower())
                    return None
                npc_ids.update(got)
        return 4 if npc_ids else None
    if acquire == 'itemplay':
        text = param.strip()
        if text.lower() not in items:
            unresolved.add(text.lower())
            return None
        return 3
    if acquire == 'enterworld':
        try: wid = int(param.strip())
        except ValueError: wid = -1
        return 7 if wid > 0 else None
    if acquire in ('levelup', 'leveluplogin'):
        try: level = int(param.strip())
        except ValueError: level = -1
        if level <= 0: return None
        return 10 if acquire == 'leveluplogin' else 8
    return 0  # AcquirePlan.NONE（none / enterarea / 其余）

def main():
    by_desc, by_name = load_local_npcs()
    aliases = load_aliases()
    name_groups = load_groups()
    items = load_items()
    zones = load_zones()
    rows = load_switch_set()
    routed_steps, frozen = Counter(), []
    unresolved_all = set()
    acquire_kinds, acquire_talk_npcs, acquire_items, acquire_worlds, acquire_levels = Counter(), Counter(), Counter(), Counter(), Counter()
    action_rows, action_instances, cond_rows = 0, Counter(), 0
    for qid in sorted(rows):
        acquire, param, con_quest, con_quest_list, steps = rows[qid]
        freeze = None
        step_kinds = []
        if (con_quest and con_quest.strip()) or (con_quest_list and con_quest_list.strip()):
            freeze = 'ACQUIRE_CONDITION_UNFACED'
            cond_rows += 1
        if not freeze:
            for idx, (cat, cols) in enumerate(steps):
                payload = cols.get(0, '')
                if freeze: break
                step_kinds.append(cat)
                if cat == 'hunt' or cat == 'talkfobj':
                    groups = parse_groups(payload)
                    if not groups:
                        freeze = 'PAYLOAD_INVALID'; break
                    for gi, (names, trailing) in enumerate(groups):
                        for n in names:
                            got = resolve_monsters(n, by_desc, by_name, aliases, name_groups)
                            if got is None:
                                freeze = 'NAME_UNRESOLVED'
                                unresolved_all.add(n.strip().lower()); break
                        if freeze: break
                elif cat in ('talk', 'collectitem'):
                    npc_ids = []
                    for names, trailing in parse_groups(payload):
                        for n in names:
                            got = resolve_monsters(n, by_desc, by_name, aliases, name_groups)
                            if got is None:
                                freeze = 'NAME_UNRESOLVED'
                                unresolved_all.add(n.strip().lower()); break
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
                    if text.lower() not in items:
                        freeze = 'NAME_UNRESOLVED'
                        unresolved_all.add(text.lower()); break
                    if count <= 0:
                        freeze = 'PAYLOAD_INVALID'; break
                elif cat == 'enterarea':
                    alias = payload.strip()
                    if alias.upper() not in zones:
                        freeze = 'ZONE_ABSENT' if alias.lower() in ABSENT else 'ZONE_UNRESOLVED'
                        unresolved_all.add(alias.lower()); break
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
                        v = cols.get(cn)
                        if v is not None and v.strip():
                            try: int(v.strip())
                            except ValueError:
                                freeze = 'PAYLOAD_INVALID'; break
                    if freeze: break
                else:
                    freeze = 'PAYLOAD_INVALID'; break
                if freeze is None:
                    freeze = scan_faced_actions(cat, cols, items, unresolved_all, qid)
        kind = None
        if freeze is None:
            kind = acquire_axis(acquire, param, by_desc, by_name, aliases, name_groups, items, unresolved_all)
            if kind is None:
                freeze = 'NAME_UNRESOLVED'
        if freeze:
            frozen.append((qid, freeze))
            continue
        for cat in step_kinds:
            routed_steps[cat] += 1
        acquire_kinds[kind] += 1
        if kind == 4:
            for names, _t in parse_groups(param or ''):
                for n in names:
                    got = resolve_monsters(n, by_desc, by_name, aliases, name_groups)
                    for i in (got or []):
                        acquire_talk_npcs[i] += 1
        elif kind == 3:
            acquire_items[items[(param or '').strip().lower()]] += 1
        elif kind == 7:
            acquire_worlds[int(param.strip())] += 1
        elif kind in (8, 10):
            acquire_levels[int(param.strip())] += 1
        # 动作面直方图（routed 行内已解析的 plan 数）
        had = False
        for cat, cols in steps:
            payloads = PAYLOAD_COLUMNS[cat]
            for column in sorted(cols):
                if column in payloads: continue
                action = extra_action(column)
                if action in ('GIVE_ITEMS', 'REMOVE_ITEMS', 'CUTSCENE'):
                    text = cols[column].strip()
                    tokens = re.split(r'[,\s]+', text)
                    if action in ('GIVE_ITEMS', 'REMOVE_ITEMS'):
                        for i in range(0, len(tokens), 2):
                            action_instances[action] += 1; had = True
                    elif len(tokens) >= 2 and tokens[0].lower() in ('cutscene','cutscene2','movie','movie2'):
                        try:
                            int(tokens[1]); action_instances['CUTSCENE'] += 1; had = True
                        except ValueError: pass
        if had:
            action_rows += 1
    routed_rows = len(rows) - len(frozen)
    print('switch rows:', len(rows))
    print('routed rows:', routed_rows)
    print('routed steps per kind:', dict(sorted(routed_steps.items())))
    print('frozen rows:', len(frozen),
          dict(sorted(Counter(r for _, r in frozen).items())))
    for qid, fr in sorted(frozen):
        print('  frozen', qid, fr)
    print('acquire kinds over routed rows:', dict(sorted(acquire_kinds.items())))
    print('acquire talk npc keys:', len(acquire_talk_npcs), 'item keys:', len(acquire_items),
          'world keys:', dict(sorted(acquire_worlds.items())), 'level keys:', dict(sorted(acquire_levels.items())))
    print('rows with faced actions:', action_rows, 'instances:', dict(sorted(action_instances.items())))
    print('rows with acquire-condition columns (whole switch set):', cond_rows)
    print('unresolved names:', len(unresolved_all))
    for k in sorted(unresolved_all):
        print('  unresolved', k)

if __name__ == '__main__':
    main()
