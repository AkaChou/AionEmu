#!/usr/bin/env python3
# P7 步 e2 复算镜像：在 dd-planrow-e1-mirror.py 之上按 e2 裁定重算——
# ① 动作面收全：case 3/5/7 落面（teleport 5 整数 / spawn 组形 / say 字符串键→id 表）；
#    case 6/8 只在 enterarea/talkfobj 步活（def 侧槽未定名 ⇒ 冻结），其余 kind = 真端死列忽略；
#    case 9/10 保持 ACTION_UNFACED。
# ② con_quest/con_quest_list = 目录显示元数据非闸门 ⇒ 不再冻结（35 行解冻）。
# ③ 挑战接取哨兵 `_challengetask_` = 接取 NPC 即 reward_npc_name ⇒ 6 行解冻。
# 输出：routed 行/步分桶、冻结原因分桶、unresolved 名集、接取兴趣与动作面直方图。只读。
# P7 step e2 offline mirror: full action faces (teleport/spawn/say), the c8d0-only def-side
# columns frozen, con_quest no longer gating, and the challenge sentinel resolved to the
# reward npc. Read-only.
import re, glob, sys
from collections import OrderedDict, Counter

REPO = '/Users/mc/IdeaProjects/AionEmu-test'
NPC_DIR = REPO + '/src/main/resources/aion/data/static_data/npcs'
ITEM_DIR = REPO + '/src/main/resources/aion/data/static_data/items/item'
ZONE_FILE = REPO + '/src/main/resources/aion/data/static_data/zones/zones_retail_enterarea.xml'
DD_TABLE = REPO + '/src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml'
RETENTION = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv'
ALIASES = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-npc-name-aliases.tsv'
GROUPS_TSV = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-quest-ai-name-groups.tsv'
STRINGS_TSV = REPO + '/src/main/resources/aion/data/static_data/quest/retail/retail-quest-string-ids.tsv'

ATTR = re.compile(r'\b(npc_id|name|name_desc|quest_ai_name)="([^"]*)"')
TRAILING = re.compile(r'^(.*?)(?:\s*,\s*|\s+)(\d+)$')
ABSENT = {"lf6_sensoryarea_q15551_atob","lf6_sensoryarea_q15551_btoa","lf6_sensoryarea_q15552_atod",
          "lf6_sensoryarea_q15552_dtoa","lf6_sensoryarea_q15553_atof","lf6_sensoryarea_q15553_ftoa",
          "lf6_sensoryarea_q15554_atoh","lf6_sensoryarea_q15554_htoa","lf6_sensoryarea_q15601a_dynamic_env",
          "lf6_sensoryarea_q15601b","lf6_sensoryarea_q15602a_dynamic_env","lf6_sensoryarea_q15604a_dynamic_env",
          "lf6_sensoryarea_q15605a_named","lf6_sensoryarea_q15608a_dynamic_env"}
PAYLOAD_COLUMNS = {'hunt': {0}, 'collectitem': {0,1,2,3,4,5}, 'pvp': {0,1,2,3}, 'talk': {0},
                   'enterarea': {0}, 'itemplay': {0}, 'enterworld': {0}, 'talkfobj': {0}}
SENTINEL = '_challengetask_'

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
    m = {}
    for line in open(GROUPS_TSV, encoding='utf-8'):
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

def load_strings():
    m = {}
    for line in open(STRINGS_TSV, encoding='utf-8'):
        line = line.strip()
        if not line or line.startswith('#'): continue
        parts = line.split('\t')
        if len(parts) >= 2: m[parts[0].strip()] = int(parts[1])
    return m

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
        def field(tag):
            fm = re.search(r'<%s>([^<]*)</%s>' % (tag, tag), body)
            return fm.group(1).strip() if fm else ''
        acquire = field('category_acquire_').lower()
        acquire_param = field('value0_acquire_')
        reward_npc = field('reward_npc_name')
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
        rows[qid] = (acquire, acquire_param, reward_npc, steps)
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

def parse_retail_int(token):
    text = token.strip()
    start = 1 if text[:1] in ('+', '-') else 0
    end = start
    while end < len(text) and text[end].isdigit(): end += 1
    if end == start: return 0
    try: return int(text[:end])
    except ValueError: return 0

def scan_faced_actions(cat, cols, items, strings, by_desc, by_name, aliases, groups, unresolved):
    """镜像 DataDrivenNativeRuntime.scanFacedActions（步 e2）：返回冻结原因或 None。"""
    payloads = PAYLOAD_COLUMNS[cat]
    def extra_column(c):
        return {1:'GIVE',2:'REMOVE',3:'TELEPORT',4:'CUTSCENE',5:'SPAWN',6:'DELAY',
                7:'MESSAGE',8:'MESSAGE8',9:'INSTANCE',10:'TIMER'}.get(c)
    def tokenize(text, seps):
        # Java split 默认去尾空 token；Python re.split 会留 ⇒ 过滤对齐。
        return [t for t in re.split(seps, text.strip()) if t]
    for column in sorted(cols):
        if column in payloads:
            continue
        action = extra_column(column)
        if action is None:
            continue
        text = cols[column].strip()
        if action in ('GIVE', 'REMOVE'):
            tokens = tokenize(text, r'[,\s]+')
            if len(tokens) % 2 != 0: return 'ACTION_UNFACED'
            for i in range(0, len(tokens), 2):
                if parse_retail_int(tokens[i+1]) <= 0: return 'ACTION_UNFACED'
                if tokens[i].strip().lower() not in items:
                    unresolved.add(tokens[i].strip().lower()); return 'NAME_UNRESOLVED'
        elif action == 'CUTSCENE':
            tokens = tokenize(text, r'[,\s]+')
            if len(tokens) < 2: return 'ACTION_UNFACED'
            if parse_retail_int(tokens[1]) == 0 and not tokens[1].strip('0'): return 'ACTION_UNFACED'
            if tokens[0].lower() not in ('cutscene','cutscene2','movie','movie2'): pass  # Wrong Type!! 跳过
        elif action == 'TELEPORT':
            tokens = tokenize(text, r'[,\s]+')
            if len(tokens) < 5: return 'ACTION_UNFACED'
            if parse_retail_int(tokens[0]) <= 0: return 'ACTION_UNFACED'
        elif action == 'SPAWN':
            tokens = tokenize(text, r'[,\s;]+')
            i = 0
            while i < len(tokens):
                if tokens[i].lower() == 'relative': relative = True
                elif tokens[i].lower() == 'absolute': relative = False
                else: return 'ACTION_UNFACED'
                i += 1
                if i >= len(tokens): return 'ACTION_UNFACED'
                got = resolve_monsters(tokens[i], by_desc, by_name, aliases, groups)
                if got is None:
                    unresolved.add(tokens[i].strip().lower()); return 'NAME_UNRESOLVED'
                i += 1
                fields = 2 if relative else 6
                if i + fields > len(tokens): return 'ACTION_UNFACED'
                if parse_retail_int(tokens[i]) <= 0: return 'ACTION_UNFACED'
                i += fields
        elif action == 'MESSAGE' and column == 7:
            if text not in strings:
                unresolved.add(text.lower()); return 'NAME_UNRESOLVED'
        elif action in ('MESSAGE8', 'DELAY'):
            if cat in ('enterarea', 'talkfobj'):
                return 'ACTION_UNFACED'  # def 侧槽未定名，活面步冻结
            # 其余 kind：真端执行器无 case 6/8 = 装载即死列 ⇒ 忽略
        elif action == 'INSTANCE':
            # 2026-10-02 偏差修复第三批取证：装载面已坐实（creationId, worldId, leaveProgress,
            # 成员名），但立即执行面在 ScriptDLL64 无读者（+0x40/+0x44 仅两读者）⇒ 维持冻结。
            return 'ACTION_UNFACED'
        elif action == 'TIMER':
            # 2026-10-02 落面（真端 FUN_180c49610 case 10 + 到期面 FUN_180c46d80）：
            # 载荷 = `秒, 目标步, 旗标`，旗标 0=推进 / 1=弃任。
            tokens = re.split(r'[,;\s]+', (text or '').strip())
            if len(tokens) < 3 or not tokens[0] or not tokens[1] or not tokens[2]:
                return 'ACTION_UNFACED'
            seconds, dest, flag = (parse_retail_int(t) for t in tokens[:3])
            if seconds <= 0 or dest <= 0 or dest > 63 or flag not in (0, 1):
                return 'ACTION_UNFACED'
        else:
            return 'ACTION_UNFACED'
    return None

def acquire_axis(acquire, param, reward_npc, by_desc, by_name, aliases, groups, items, unresolved):
    acquire = (acquire or '').strip()
    param = param or ''
    if acquire == 'talk':
        npc_ids = set()
        if (param or '').strip() == SENTINEL:
            got = resolve_monsters(reward_npc or '', by_desc, by_name, aliases, groups)
            if got is None:
                unresolved.add((reward_npc or '').strip().lower()); return None
            npc_ids.update(got)
        else:
            for names, _trailing in parse_groups(param):
                for n in names:
                    got = resolve_monsters(n, by_desc, by_name, aliases, groups)
                    if got is None:
                        unresolved.add(n.strip().lower()); return None
                    npc_ids.update(got)
        return 4 if npc_ids else None
    if acquire == 'itemplay':
        text = param.strip()
        if text.lower() not in items:
            unresolved.add(text.lower()); return None
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
    if acquire == 'enterarea':
        # 步 f kind-6 接取面：非空别名 = 接取侧名字哈希树登记（区未注册时永不命中 = 真端死边镜像）；
        # 空别名 = 真端注册空名哈希 ⇒ 永不接取（同 none）。
        # Step f kind-6 face: a non-empty alias registers in the acquire-side name-hash tree (an
        # unregistered zone never fires, mirroring the retail-dead edge); an empty alias never hits.
        return 6 if (param or '').strip() else 0
    return 0

def main():
    by_desc, by_name = load_local_npcs()
    aliases = load_aliases()
    name_groups = load_groups()
    items = load_items()
    strings = load_strings()
    zones = load_zones()
    rows = load_switch_set()
    routed_steps, frozen = Counter(), []
    unresolved_all = set()
    acquire_kinds, action_rows, action_instances = Counter(), 0, Counter()
    for qid in sorted(rows):
        acquire, param, reward_npc, steps = rows[qid]
        freeze = None
        step_kinds = []
        for idx, (cat, cols) in enumerate(steps):
            payload = cols.get(0, '')
            if freeze: break
            step_kinds.append(cat)
            if cat == 'hunt' or cat == 'talkfobj':
                groups = parse_groups(payload)
                if not groups:
                    freeze = 'PAYLOAD_INVALID'; break
                for names, trailing in groups:
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
            else:
                freeze = 'PAYLOAD_INVALID'; break
            if freeze is None:
                freeze = scan_faced_actions(cat, cols, items, strings, by_desc, by_name, aliases,
                                            name_groups, unresolved_all)
        kind = None
        if freeze is None:
            kind = acquire_axis(acquire, param, reward_npc, by_desc, by_name, aliases, name_groups,
                                items, unresolved_all)
            if kind is None:
                freeze = 'NAME_UNRESOLVED'
        if freeze:
            frozen.append((qid, freeze))
            continue
        for cat in step_kinds:
            routed_steps[cat] += 1
        acquire_kinds[kind] += 1
        had = False
        for cat, cols in steps:
            payloads = PAYLOAD_COLUMNS[cat]
            for column in sorted(cols):
                if column in payloads: continue
                text = cols[column].strip()
                if column in (1, 2):
                    tokens = re.split(r'[,\s]+', text)
                    for i in range(0, len(tokens) - 1, 2):
                        action_instances['GIVE_ITEMS' if column == 1 else 'REMOVE_ITEMS'] += 1; had = True
                elif column == 4:
                    tokens = re.split(r'[,\s]+', text)
                    if len(tokens) >= 2 and tokens[0].lower() in ('cutscene','cutscene2','movie','movie2'):
                        action_instances['CUTSCENE'] += 1; had = True
                elif column == 3:
                    if len(re.split(r'[,\s]+', text)) >= 5:
                        action_instances['TELEPORT'] += 1; had = True
                elif column == 5:
                    tokens = re.split(r'[,\s;]+', text)
                    i = 0
                    while i < len(tokens):
                        if tokens[i].lower() not in ('absolute','relative'): break
                        i += 1
                        if i >= len(tokens): break
                        got = resolve_monsters(tokens[i], by_desc, by_name, aliases, name_groups)
                        if got is None: break
                        i += 1
                        fields = 2 if tokens[i-2].lower() == 'relative' else 6
                        if i + fields > len(tokens): break
                        for _ in (got or []):
                            action_instances['SPAWN'] += 1; had = True
                        i += fields
                elif column == 7:
                    action_instances['SAY'] += 1; had = True
    routed_rows = len(rows) - len(frozen)
    print('switch rows:', len(rows))
    print('routed rows:', routed_rows)
    print('routed steps per kind:', dict(sorted(routed_steps.items())))
    print('frozen rows:', len(frozen), dict(sorted(Counter(r for _, r in frozen).items())))
    for qid, fr in sorted(frozen):
        print('  frozen', qid, fr)
    print('acquire kinds over routed rows:', dict(sorted(acquire_kinds.items())))
    print('rows with faced actions:', action_rows if action_rows else '(recount below)')
    print('action instances:', dict(sorted(action_instances.items())))
    print('unresolved names:', len(unresolved_all))
    for k in sorted(unresolved_all):
        print('  unresolved', k)

if __name__ == '__main__':
    main()
