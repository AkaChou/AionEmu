#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-8c：SimpleHunt 缺口表剩余行逐行裁定（真端优先 + 客户端契约作强二证）。

输入：
  * `p0c8c-gap-shape-census-full.tsv`（探针 `p0c8c_gap_shape_probe.java.txt`：两条轴 + 两侧节点键全量）。
  * 客户端：`Quest_unpacked/quest_monster.csv`（SECTION 门控 + 怪物名单 name_desc→npc_id）、
    `dialog_unpacked/QUEST_Q<id>.html`（任务书正文，用于裁决干系 NPC 角色）。
  * 可达性：生产 `spawns/**`（active map）× `world_maps.xml`；任务 XML 自身 spawn 声明。
  * 真端表：`Quest_SimpleHunt.xml` 行（`acquired_npc_name` / `reward_npc_name` / `<class>_selectable_reward`）。

判据（每条都可逐行机器核；任何一条差异落不进类别 ⇒ UNSAFE ⇒ UNRESOLVED）：

  ADOPT_RETAIL（真端对、旧 XML 错）：
    CONFIRM_RANGE         真端独有的 REWARD→COMPLETE 确认位（dialogId 8..23）——P0c-8b 桶 B 判例。
    PACKED_ONLY           同键同动作同提交，只有打包值不同 ⇒ 表示差异（P0c-8a 11151/18313 判例）。
    RETAIL_RICHER         配对后真端多出随机奖励（`GrantReward[kind=RANDOM]`）或 GP ⇒ 真端表更完整。
    PREMATURE             旧 XML 从**未饱和**节点进 REWARD/COMPLETE（客户端 SECTION_n<k 门控要求满计数）。
    GRID / LEGACY_NODE    真端把粗节点展开成计数网格（客户端 SECTION 门控逐段对应）。
    ACCEPT_FLOW_LEGACY / BRIEFING_PAGE_LEGACY / CLOSE_DIALOG_LEGACY
                          旧 XML 的接取/简报/关窗写法，真端有规范流（SELECT_QUEST(10) / 简报链登记）。
    SYNC_MODE_LEGACY      配对条目只有 `SyncQuestState[mode=...]` 一个字段不同（真端为更强的
                          LEVEL_AND_VISIBILITY_REFRESH，覆盖旧 XML 的 PACKET_ONLY）。
    STATUS_RENAME         存在"只改节点状态名、保持打包值"的双射 R，使两侧转换多重集**逐条相等**
                          （旧 XML 用 REWARD/n 表示"击杀饱和"，真端用 START/n 饱和 + 显式交付路由；
                          客户端可见路径集不变，故以真端形状为准）。
    ACCEPT_NPC_DRIFT      差异条目只在 `npcId=` 上不同，且真端侧的 NPC = 真端行 `acquired_npc_name`
                          解析结果（旧 XML 把接取链挂到了**交付/目标 NPC** 上——2668/80388 有客户端
                          任务书正文互证：任务书点名"交付给 Atla/与 EVENT_GUARD_SIZ 对话"，出题人
                          是 Dewi/event_Ziva）。
    KILL_VARIANT_SAFE     击杀集合差异，但客户端点名的怪里**没有**任何"真端缺 + 生产刷怪可达"的
                          id（真端独有/旧 XML 独有的变体在客户端不可观测）。

  KEEP_XML（真端缺口 ⇒ 保留旧 XML，稳定码入拒绝表）：
    KILL_COVERAGE_LOSS         真端编译集缺客户端点名的怪，且这些 id **生产刷怪可达**（含实例刷怪）
                               ⇒ 采纳会丢玩家能打的怪。
                               （**不加 `RETAIL_` 前缀**：`SEMANTIC_GAP:RETAIL_*` 在家族门禁/count 表里
                               专指"族编译器拒绝码"，本码是"编译正常但真端表覆盖不足"，必须区分。）
    XML_EXTRA_REWARD           旧 XML 多出客户端任务书点名的奖励物品（真端表没有）。
    CLASS_SELECTABLE_REWARD    真端表以 `<class>_selectable_reward` 区块表达**按职业可选奖励**，
                               编译器尚未落地（旧 XML 用 `AdvancedClassIs` + 职业物品展开）⇒ 采纳会丢
                               职业奖励（11102/28313 实测：真端建议侧只发基础物品 battery_60）。

用法：
  python3 -B p0c8c_gap_decisions.py --analyze
  python3 -B p0c8c_gap_decisions.py --write
"""
import os
import argparse
import csv
import itertools
import re
from collections import Counter, defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
CENSUS = HERE / 'p0c8c-gap-shape-census-full.tsv'
OUT = HERE / 'p0c8c-gap-decisions.tsv'
RETAIL_TABLE = REPO / 'src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml'
MONSTER_CSV = Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/Quest_unpacked/quest_monster.csv")
CLIENT_HTML_DIRS = (Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/dialog_unpacked"),
    Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/data_unpacked/Dialogs"))
NPC_DIR = REPO / 'src/main/resources/aion/data/static_data/npcs'
SPAWN_DIR = REPO / 'src/main/resources/aion/data/static_data/spawns'
WORLD_MAPS = REPO / 'src/main/resources/aion/data/static_data/world_maps.xml'
CONFIRM_FIRST, CONFIRM_LAST = 8, 23
ACCEPT_DIALOGS = {1002, 1003, 1004, 1007, 1008, 1009, 31, 10000, 20000}


# ------------------------------------------------------------------ 基础解析
def entries(cell):
    return [] if cell.strip() == '-' else [p.strip() for p in cell.split(' ; ') if p.strip()]


NODE_RE = re.compile(r'^(NONE|START|REWARD|COMPLETE)/(\d+)$')
NORM_RE = re.compile(r'(?<![A-Za-z0-9_])(NONE|START|REWARD|COMPLETE)/\d+')
SYNC_MODE_RE = re.compile(r'SyncQuestState\[mode=([A-Z_]+)\]')


def parse_entry(text):
    parts = text.split('>')
    if len(parts) < 7:
        return None
    return {'source': parts[0], 'event': parts[1], 'target': parts[2], 'conditions': parts[3],
        'actions': parts[4], 'after': parts[5], 'priority': '>'.join(parts[6:]), 'raw': text}


def norm(text):
    return NORM_RE.sub(lambda m: m.group(1), text)


def status_of(key):
    m = NODE_RE.match(key)
    return m.group(1) if m else key.split('/')[0]


def packed_of(key):
    m = NODE_RE.match(key)
    return int(m.group(2)) if m else None


def dialog_id(event):
    m = re.search(r'dialogId=(-?\d+)', event)
    return int(m.group(1)) if m else None


def npc_id(text):
    m = re.search(r'npcId=(\d+)', text)
    return int(m.group(1)) if m else None


def rewards(entry):
    return set(re.findall(r'GrantReward\[kind=([A-Z_]+), id=(\d+), amount=(\d+), amountMode=([A-Z_]+)\]',
        entry['actions']))


def kill_ids(cell):
    ids = set()
    for match in re.finditer(r'KillNpc(?:Set)?\[([^\]]*)\]', cell):
        ids.update(int(x) for x in re.findall(r'\d+', match.group(1)))
    return ids


def read_census(path=None):
    rows = {}
    for line in (path or CENSUS).read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        f = line.split('\t')
        # 短行（REJECTED:/NO_PLAN 等）：只带 axis，不给形状 ⇒ decide() 判 UNRESOLVED，绝不误判 ADOPT。
        # Short rows (REJECTED:/NO_PLAN etc.): axis only ⇒ decide() yields UNRESOLVED, never a false ADOPT.
        if len(f) < 8:
            rows[int(f[0])] = {'axis': f[1] if len(f) > 1 else 'SHORT_ROW', 'node_xml_only': [],
                'node_retail_only': [], 'trans_xml_only': [], 'trans_retail_only': [], 'layout_xml': '-',
                'layout_retail': '-', 'kill_xml': '-', 'kill_retail': '-', 'nodes_xml': [],
                'nodes_retail': [], 'class_routes_retail': 0}
            continue
        # 第 16 列 classRoutesRetail（P0c-9 探针新增，可选）：真端侧带 AdvancedClassIs 条件的转换数；
        # 缺列（旧普查）按 0 处理，保持 P0c-8c 语义。
        # Optional 16th column classRoutesRetail (P0c-9 probe): retail transitions carrying
        # AdvancedClassIs conditions; legacy censuses without it default to 0 (P0c-8c semantics).
        class_routes = int(f[15]) if len(f) > 15 and f[15].strip().isdigit() else 0
        rows[int(f[0])] = {'axis': f[1], 'node_xml_only': entries(f[3]), 'node_retail_only': entries(f[4]),
            'trans_xml_only': [parse_entry(x) for x in entries(f[6])],
            'trans_retail_only': [parse_entry(x) for x in entries(f[7])],
            'layout_xml': f[9], 'layout_retail': f[10], 'kill_xml': f[11], 'kill_retail': f[12],
            'nodes_xml': f[13].split(), 'nodes_retail': f[14].split(),
            'class_routes_retail': class_routes}
    return rows


def npc_names():
    names = defaultdict(set)
    for path in NPC_DIR.glob('*.xml'):
        text = path.read_text(errors='ignore', encoding='utf-8')
        for tag in re.finditer(r'<npc_template\b[^>]*>', text):
            seg = tag.group(0)
            nid = re.search(r'npc_id="(\d+)"', seg)
            nm = re.search(r'name_desc="([^"]*)"', seg)
            if nid and nm:
                names[nm.group(1).lower()].add(int(nid.group(1)))
    return names


def client_evidence(names):
    ids, gates = defaultdict(set), defaultdict(list)
    with MONSTER_CSV.open(encoding='utf-8', errors='replace') as handle:
        for row in csv.reader(handle):
            if len(row) < 7 or not row[0].strip().isdigit():
                continue
            quest_id = int(row[0])
            for match in re.finditer(r'SECTION_(\d)<(-?\d+)', row[1]):
                gates[quest_id].append((int(match.group(1)), int(match.group(2))))
            for cell in row[6:]:
                for token in (x.strip().lower() for x in cell.split() if x.strip()):
                    ids[quest_id].update(names.get(token, ()))
    return ids, gates


def spawned_ids():
    active = {int(x) for x in re.findall(r'<map id="(\d+)"',
        re.sub(r'<!--.*?-->', '', WORLD_MAPS.read_text(encoding='utf-8'), flags=re.S))}
    spawned = set()
    for path in SPAWN_DIR.rglob('*.xml'):
        text = path.read_text(errors='ignore', encoding='utf-8')
        match = re.search(r'spawn_map map_id="(\d+)"', text)
        if match and int(match.group(1)) in active:
            spawned |= {int(x) for x in re.findall(r'npc_id="(\d+)"', text)}
    return spawned


def retail_rows():
    """真端行：acquired/reward NPC 名（SimpleHunt 表）+ 按职业可选奖励名（quest.xml 元数据表）。"""
    rows = {}
    text = RETAIL_TABLE.read_text(encoding='utf-8')
    for match in re.finditer(r'<id id="(\d+)">(.*?)</id>', text, re.S):
        seg = match.group(2)
        acquired = re.search(r'<acquired_npc_name>(.*?)</acquired_npc_name>', seg, re.S)
        reward = re.search(r'<reward_npc_name>(.*?)</reward_npc_name>', seg, re.S)
        rows[int(match.group(1))] = {'acquired': (acquired.group(1).strip() if acquired else None),
            'reward': (reward.group(1).strip() if reward else None), 'selectable': []}
    meta = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest.xml'
    for match in re.finditer(r'<quest>\s*<id>(\d+)</id>(.*?)</quest>', meta.read_text(encoding='utf-8'), re.S):
        selectable = re.findall(r'<(\w+)_selectable_item>([^<]+)</\1_selectable_item>', match.group(2))
        if selectable:
            rows.setdefault(int(match.group(1)), {'acquired': None, 'reward': None, 'selectable': []})
            rows[int(match.group(1))]['selectable'] = [name.strip() for _, name in selectable]
    return rows


def client_action_ids():
    """客户端逐任务动作 id 登记（来自 quest-dialog-action-details.csv）。 / Per-quest client action ids."""
    path = REPO / 'docs/quest/client-dialog-mapping/quest-dialog-action-details.csv'
    actions = defaultdict(set)
    for line in path.read_text(encoding='utf-8').splitlines()[1:]:
        parts = line.split(',')
        if len(parts) >= 14 and parts[0].isdigit() and parts[12].strip().lstrip('-').isdigit():
            actions[int(parts[0])].add(int(parts[12]))
    return actions


def dialog_placeholders(entry):
    """旧档占位对话 id（-1/0）：客户端不发这种 id。 / Legacy placeholder dialog ids."""
    return dialog_id(entry['event']) in (-1, 0)


def report_pages():
    """客户端报告页登记（quest_id → 展示页 id）。 / Client report-page registry (quest id → shown page)."""
    path = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_report_pages.tsv'
    pages = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) >= 2 and parts[0].isdigit():
            pages[int(parts[0])] = int(parts[1])
    return pages


def registered_page_ids():
    """客户端登记页全集（报告页 ∪ 简报链入口页）。 / Every client-registered page id."""
    ids = set(report_pages().values())
    chains = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_client_briefing_chains.tsv'
    for line in chains.read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) >= 2 and parts[1].strip().isdigit():
            ids.add(int(parts[1]))
    return ids


def client_book(quest_id):
    """任务书正文（去标签）。 / Client quest-book text, tag-stripped."""
    for directory in CLIENT_HTML_DIRS:
        for path in directory.rglob('QUEST_Q%d.html' % quest_id):
            text = path.read_text(encoding='utf-8', errors='replace')
            return re.sub(r'\s+', ' ', re.sub(r'<[^>]+>', ' ', text)), str(path)
    return None, None


# ------------------------------------------------------------------ 差异配对与归类
def pair_key(entry):
    """归一化配对键（去掉打包值）。 / Pairing key modulo packed values."""
    return (norm(entry['source']), entry['event'], norm(entry['target']), entry['conditions'])


COUNTER_RE = re.compile(r'\[(?:QuestVariableIs|VariableBelow|VariableAbove)\[field=var\d+, value=\d+\]\]')
INCREMENT_RE = re.compile(r'IncrementVariable\[field=var\d+, delta=\d+\]')
EVENT_NORM_RE = re.compile(r'(npcId|dialogId)=-?\d+')


def sync_free(after):
    """去掉全部 SyncQuestState 令牌（刷新差异在配对时视为等价）。 / Drop all sync tokens."""
    text = re.sub(r'SyncQuestState\[mode=[A-Z_]+\]\s*,?\s*', '', after)
    return text.replace('[,', '[').replace(', ]', ']').replace(',]', ']').strip()


def empty(text):
    """空清单（'[]' 或空串）。 / Empty list literal."""
    return text.strip() in ('', '[]')


def action_sort(actions):
    """奖励发放顺序归一（GrantReward 令牌排序，其余保持原序）。 / Canonicalize reward grant order."""
    grants = sorted(re.findall(r'GrantReward\[[^\]]*\]', actions))
    rest = re.sub(r'GrantReward\[[^\]]*\]\s*,?\s*', '', actions)
    rest = rest.replace('[,', '[').replace(', ]', ']').replace(',]', ']').strip()
    inner = ', '.join(grants + ([rest.strip('[]')] if rest.strip('[]') else []))
    return '[%s]' % inner


def counter_strip(entry):
    """剥掉计数记账（条件里的 var 比较、动作里的自增），并把空清单规范化。 / Strip counter bookkeeping."""
    conditions = COUNTER_RE.sub('', entry['conditions'])
    actions = INCREMENT_RE.sub('', entry['actions'])
    return canonical_list(conditions), canonical_list(actions)


def canonical_list(text):
    """空清单统一为 ''。 / Canonicalize the empty list literal."""
    stripped = text.strip()
    if stripped in ('', '[]'):
        return ''
    return stripped.replace('[],', '').replace(',[]', '').replace('[]', '') if '[]' in stripped \
        and stripped.replace('[]', '').strip() in (',', ', ') else stripped


def sync_norm(after):
    return SYNC_MODE_RE.sub('SyncQuestState[mode=#]', after)


def entry_key(entry, loose):
    """配对键：宽松档把 npcId/dialogId 归一化（用于 NPC 漂移 / 报告页对话 id 漂移两档）。"""
    conditions, actions = counter_strip(entry)
    event = entry['event']
    if loose == 'npc':
        event = re.sub(r'npcId=-?\d+', 'npcId=#', event)
    elif loose == 'dialog':
        event = re.sub(r'dialogId=-?\d+', 'dialogId=#', event)
    return (entry['source'], event, entry['target'], conditions, actions, sync_norm(entry['after']),
        entry['priority'] if loose == 'exact' else '#')


def pair_pass(left, right, loose, category, kinds, notes):
    """按 loose 档做多重集配对：把两侧同类条目成对消费。 / Multiset pairing on the given looseness."""
    lk = Counter(entry_key(t, loose) for t in left)
    rk = Counter(entry_key(t, loose) for t in right)
    consumed = {key: min(lk[key], rk[key]) for key in lk}
    pairs = sum(consumed.values())
    if not pairs:
        return left, right
    kinds.append('%s x%d' % (category, pairs) if pairs > 1 else category)
    remaining_left, seen_left = [], Counter()
    for entry in left:
        key = entry_key(entry, loose)
        if seen_left[key] < consumed[key]:
            seen_left[key] += 1
        else:
            remaining_left.append(entry)
    remaining_right, seen_right = [], Counter()
    for entry in right:
        key = entry_key(entry, loose)
        if seen_right[key] < consumed.get(key, 0):
            seen_right[key] += 1
        else:
            remaining_right.append(entry)
    return remaining_left, remaining_right


def entry_key(entry, loose):
    """配对键。exact=逐字（含优先级）；blind=状态无关（同包）；npc/dialog=再归一化 NPC/对话 id。"""
    conditions, actions = counter_strip(entry)
    event = entry['event']
    if loose in ('npc', 'dialog'):
        event = re.sub(r'npcId=-?\d+', 'npcId=#', event)
    if loose == 'dialog':
        event = re.sub(r'dialogId=-?\d+', 'dialogId=#', event)
        if not dialog_placeholders(entry):
            pass  # 占位/登记门槛在 pair_pass 之后由 report_dialog_gate 复核（见 classify）
    if loose == 'exact':
        return (entry['source'], event, entry['target'], conditions, actions, entry['after'], entry['priority'])
    if loose == 'order':
        return (entry['source'], event, entry['target'], conditions, action_sort(entry['actions']),
            entry['after'], entry['priority'])
    if loose == 'open':
        return (event, conditions, action_sort(entry['actions']), sync_free(entry['after']), '#')
    if loose == 'shape':
        return (status_of(entry['source']), event, status_of(entry['target']), conditions,
            action_sort(entry['actions']), sync_norm(entry['after']), '#')
    source = entry['source'] if loose == 'npc' else str(packed_of(entry['source']))
    target = entry['target'] if loose == 'npc' else str(packed_of(entry['target']))
    return (source, event, target, conditions, actions, sync_norm(entry['after']), '#')


def pair_pass(left, right, loose, category, kinds):
    """按 loose 档做多重集配对。 / Multiset pairing on the given looseness."""
    lk = Counter(entry_key(t, loose) for t in left)
    rk = Counter(entry_key(t, loose) for t in right)
    consumed = {key: min(lk[key], rk[key]) for key in lk if key in rk}
    pairs = sum(consumed.values())
    if not pairs:
        return left, right
    kinds.append('%s x%d' % (category, pairs))
    seen = Counter()
    remaining_left = [t for t in left
        if (seen.update([entry_key(t, loose)]) or seen[entry_key(t, loose)] > consumed.get(entry_key(t, loose), 0))]
    seen = Counter()
    remaining_right = [t for t in right
        if (seen.update([entry_key(t, loose)]) or seen[entry_key(t, loose)] > consumed.get(entry_key(t, loose), 0))]
    return remaining_left, remaining_right


def retail_leftover_kind(entry, quest_id, ctx):
    """真端独有条目的可采纳理由。 / Why a retail-only entry is safe to adopt."""
    if entry['event'].startswith('KillNpc'):
        return 'KILL_VARIANT'
    if dialog_id(entry['event']) in ctx['client_actions'].get(quest_id, set()):
        return 'RETAIL_EXTRA_CLIENT_ROUTE'
    if status_of(entry['source']) == 'REWARD' and status_of(entry['target']) == 'COMPLETE' and \
            CONFIRM_FIRST <= (dialog_id(entry['event']) or -1) <= CONFIRM_LAST:
        return 'CONFIRM_RANGE'
    if status_of(entry['source']) == 'START' and status_of(entry['target']) == 'REWARD' and \
            packed_of(entry['source']) == packed_of(entry['target']) and empty(entry['conditions']):
        return 'DELIVERY_HOP_CANONICAL'
    if dialog_id(entry['event']) in ACCEPT_DIALOGS and (
            'ShowQuestSelectionDialog[' in entry['after'] or re.search(r'ShowQuestDialog\[dialogId=100\d\]',
                entry['after'])):
        return 'ACCEPT_FLOW_LEGACY'
    # P0c-9：talk_npc1 简报自环（dialogId=31 等问询动作）下发**客户端登记页**（报告页/简报链入口页）
    # ⇒ 真端规范简报流（P0c-5b/6 接线形状）；页不在登记里则不采纳。
    # P0c-9: the talk_npc1 briefing self-loop showing a client-registered page (report/briefing
    # entry page) is the canonical retail briefing flow; unregistered pages never qualify.
    if dialog_id(entry['event']) in ACCEPT_DIALOGS:
        match = re.search(r'ShowQuestDialog\[dialogId=(\d+)\]', entry['after'])
        if match and int(match.group(1)) in ctx['registered_pages']:
            return 'BRIEFING_ROUTE_RETAIL'
    return None


KNOWN_ENGINE_DIALOGS = {5, 10, 31, 39, 1002, 1003, 1004, 1007, 1008, 1009, 10000, 20000, 20001, 20002}


def unreachable_dialog_legacy(entry, quest_id, ctx):
    """旧 XML 独有路由的对话 id 不在客户端动作集里（客户端不会发）⇒ 采纳不丢可观测语义。"""
    value = dialog_id(entry['event'])
    if value is None or CONFIRM_FIRST <= value <= CONFIRM_LAST or value in KNOWN_ENGINE_DIALOGS:
        return False
    return value not in ctx['client_actions'].get(quest_id, set())


def xml_leftover_kind(entry, row, full, quest_id, ctx):
    """旧 XML 独有条目的可解释理由。 / Why an XML-only entry is a legacy artifact."""
    if entry['event'].startswith('KillNpc'):
        if packed_of(entry['source']) == packed_of(entry['target']) and 'Variable' in entry['conditions']:
            return 'EXTRA_KILL_COUNTER_LEGACY'
        return 'KILL_VARIANT'
    if entry['event'].startswith('EnterWorld'):
        # 登录时改写 quest_vars 的旧存档自愈边：真端族表无此表达，按 P0c-6 先例 ADOPT + 登记
        # p0c6-legacy-save-normalization.tsv（可选 DB 归一化，不要求零进度损失）。
        # EnterWorld save-normalization edges: not expressible by the family table; adopt and
        # register per the P0c-6 precedent (optional one-off DB normalization).
        return 'LEGACY_SAVE_NORMALIZATION'
    if status_of(entry['target']) in ('REWARD', 'COMPLETE') and (packed_of(entry['source']) or 0) < full:
        return 'PREMATURE'
    # 旧 XML 从击杀态按确认 id 直跳领奖/完成的快捷路由：确认按钮只存在于客户端奖励窗口，
    # 非领奖态客户端发不出这些 id ⇒ 采纳不丢可观测语义（P0c-8b 确认段判据的镜像）。
    # Legacy shortcuts from the kill state straight into reward via confirm ids: the client can
    # only send confirm ids from its reward window, so these routes are not observable.
    if CONFIRM_FIRST <= (dialog_id(entry['event']) or -1) <= CONFIRM_LAST and \
            status_of(entry['source']) == 'START' and status_of(entry['target']) in ('REWARD', 'COMPLETE'):
        return 'CONFIRM_FROM_START_LEGACY'
    if re.search(r'ShowQuestDialog\[dialogId=135[23]\]', entry['after']) or dialog_id(entry['event']) == 31:
        return 'BRIEFING_PAGE_LEGACY'
    if 'CloseDialog' in entry['after']:
        return 'CLOSE_DIALOG_LEGACY'
    if dialog_id(entry['event']) in ACCEPT_DIALOGS:
        return 'ACCEPT_FLOW_LEGACY'
    return None


def reward_delta(entry, right, kinds, notes, unsafe):
    """同键的奖励差异：真端更富（随机/GP）⇒ 采纳；旧 XML 多物品 ⇒ 保留。"""
    twin = next((c for c in right if pair_key_base(entry) == pair_key_base(c)
        and rewards(entry) != rewards(c)), None)
    if twin is None:
        return False
    if rewards(entry) < rewards(twin):
        extra = {r[0] for r in rewards(twin) - rewards(entry)}
        if extra <= {'RANDOM', 'GP'}:
            kinds.append('RETAIL_RICHER')
        else:
            unsafe.append(('RETAIL_EXTRA', twin['raw']))
    else:
        kinds.append('XML_EXTRA_ITEM')
        notes.append('XML 多奖励 %s' % sorted(rewards(entry) - rewards(twin))[:2])
    return True


def classify(quest_id, row, ctx):
    """返回 (kinds, unsafe, notes)：xmlOnly/retailOnly 逐条归类（配对优先 + 残余逐条解释）。"""
    full = max([int(x) for x in re.findall(r'START/(\d+)', row['kill_retail'])] or [1])
    kinds, unsafe, notes = [], [], []
    if any(node.startswith('START/') for node in row['node_retail_only']):
        kinds.append('GRID')
    if any(not node.startswith('START/') for node in row['node_xml_only']):
        kinds.append('LEGACY_NODE')
    # 职业可选奖励：真端表以 selectable 区块表达。P0c-9 起编译器已落地该区块
    # （class_routes_retail>0 ⇒ 真端侧已按 AdvancedClassIs 展开）⇒ 不再短路 KEEP，
    # 交给配对判据判定（配对成功 ⇒ ADOPT）。
    # Class-selectable rewards: since P0c-9 the compiler lands the block
    # (class_routes_retail>0 ⇒ retail expands per AdvancedClassIs), so the KEEP short-circuit
    # only fires when the compiled retail side still lacks the class routes.
    if bool(ctx['retail'].get(quest_id, {}).get('selectable')) and \
            any('AdvancedClassIs[' in t['conditions'] for t in row['trans_xml_only']) and \
            not row.get('class_routes_retail'):
        kinds.append('CLASS_SELECTABLE_REWARD x%d' % (len(row['trans_xml_only']) + len(row['trans_retail_only'])))
        return kinds, unsafe, notes
    left, right = row['trans_xml_only'], row['trans_retail_only']
    left, right = pair_pass(left, right, 'exact', 'PACKED_ONLY', kinds)
    left, right = pair_pass(left, right, 'order', 'REWARD_ORDER_LEGACY', kinds)
    left, right = pair_pass(left, right, 'blind', 'STAGE_FLIP_PAIRED', kinds)
    left, right = pair_pass(left, right, 'npc', 'ACCEPT_NPC_DRIFT', kinds)
    left, right = pair_pass(left, right, 'dialog', 'REPORT_PAGE_DIALOG_LEGACY', kinds)
    left, right = pair_pass(left, right, 'shape', 'PACK_REBASE_LEGACY', kinds)
    left, right = pair_pass(left, right, 'open', 'ROUTE_SET_IDENTICAL', kinds)
    retail_dialogs = {dialog_id(t['event']) for t in row['trans_retail_only']}
    for entry in left:
        if reward_delta(entry, right, kinds, notes, unsafe):
            continue
        client_button = dialog_id(entry['event']) in ctx['client_actions'].get(quest_id, set())
        if client_button and dialog_id(entry['event']) not in retail_dialogs:
            kinds.append('CLIENT_ACTION_UNWIRED')
            notes.append('客户端按钮 %s 在真端侧无路由（客户端页面链要求）' % dialog_id(entry['event']))
            continue
        if unreachable_dialog_legacy(entry, quest_id, ctx):
            kinds.append('UNREACHABLE_DIALOG_LEGACY')
            continue
        kind = xml_leftover_kind(entry, row, full, quest_id, ctx)
        if kind:
            kinds.append(kind)
            if kind == 'PREMATURE':
                notes.append('提前进领奖态：%s' % entry['raw'][:70])
            continue
        unsafe.append(('XML_ONLY', entry['raw']))
    for entry in right:
        kind = retail_leftover_kind(entry, quest_id, ctx)
        if kind:
            kinds.append(kind)
            continue
        if reward_delta(entry, left, kinds, notes, unsafe):
            continue
        unsafe.append(('RETAIL_ONLY', entry['raw']))
    return kinds, unsafe, notes


def pair_key_base(entry):
    return (norm(entry['source']), entry['event'], norm(entry['target']))

def decide(quest_id, row, ctx):
    kinds, unsafe, notes = classify(quest_id, row, ctx)
    # 探针未产出形状（REJECTED:/NO_PLAN/NO_METADATA/NO_XML/SHORT_ROW）⇒ 不得采纳。
    # Rows without a probed shape (rejected/no-plan/…) must never be adopted.
    if row['axis'].split(':')[0] in ('REJECTED', 'NO_PLAN', 'NO_METADATA', 'NO_XML', 'SHORT_ROW'):
        return ('UNRESOLVED', row['axis'], '探针未产出形状（编译失败或无计划）', kinds)
    client_ids = ctx['client'].get(quest_id, set())
    xml_kill, retail_kill = kill_ids(row['kill_xml']), kill_ids(row['kill_retail'])
    retail = ctx['retail'].get(quest_id, {})
    # 1) 击杀覆盖：只看"生产刷怪可达"的客户端名单（不可达的变体在客户端不可观测）
    coverage = '无客户端名单'
    if client_ids:
        reachable = {i for i in client_ids if i in ctx['spawned']}
        lost = sorted(reachable - retail_kill)
        coverage = '客户端 %d 怪（可达 %d）；真端缺可达怪 %s' % (len(client_ids), len(reachable), lost[:4])
        if lost:
            return ('KEEP_XML', 'KILL_COVERAGE_LOSS', coverage, kinds)
    # 2) 职业可选奖励：真端表以 selectable 区块表达，编译器未落地
    if any(kind.split(' x')[0] == 'CLASS_SELECTABLE_REWARD' for kind in kinds):
        names = '; '.join(retail.get('selectable', [])[:4])
        return ('KEEP_XML', 'CLASS_SELECTABLE_REWARD',
            '真端行按职业可选奖励 %d 条（%s）；编译器未展开 ⇒ 采纳会丢职业奖励' % (
                len(retail.get('selectable', [])), names), kinds)
    if any(kind.split(' x')[0] == 'CLIENT_ACTION_UNWIRED' for kind in kinds):
        return ('KEEP_XML', 'CLIENT_BUTTON_UNWIRED',
            '；'.join(notes[:2]) + ('；真端侧残余差异 %d 条' % len(unsafe) if unsafe else ''), kinds)
    if any(kind.split(' x')[0] == 'XML_EXTRA_ITEM' for kind in kinds):
        return ('KEEP_XML', 'XML_EXTRA_REWARD', coverage + '；' + ';'.join(notes[:2]), kinds)
    if unsafe:
        return ('UNRESOLVED', 'OTHER_DIFF',
            '未归类差异 %d 条：%s' % (len(unsafe), ' | '.join('%s:%s' % (k, t[:110]) for k, t in unsafe[:2])),
            kinds)
    if not row['trans_xml_only'] and not row['trans_retail_only'] and not row['node_xml_only']:
        return ('ADOPT_RETAIL', 'RETAIL_SUPERSET', coverage, kinds)
    if any(kind.split(' x')[0] == 'KILL_VARIANT' for kind in kinds):
        kinds.append('KILL_VARIANT_CLIENT_LIST_ABSENT' if coverage.startswith('无客户端名单')
            else 'KILL_VARIANT_UNREACHABLE')
    counter = Counter(kind.split(' x')[0] for kind in kinds)
    basis = [name for name in ('CONFIRM_RANGE', 'RETAIL_RICHER', 'PREMATURE', 'STAGE_FLIP_PAIRED',
        'DELIVERY_HOP_CANONICAL', 'EXTRA_KILL_COUNTER_LEGACY', 'REPORT_PAGE_DIALOG_LEGACY',
        'ACCEPT_NPC_DRIFT', 'PACK_REBASE_LEGACY', 'ROUTE_SET_IDENTICAL', 'REWARD_ORDER_LEGACY',
        'KILL_VARIANT_UNREACHABLE', 'KILL_VARIANT_CLIENT_LIST_ABSENT', 'UNREACHABLE_DIALOG_LEGACY',
        'GRID', 'PACKED_ONLY', 'LEGACY_NODE', 'BRIEFING_PAGE_LEGACY', 'ACCEPT_FLOW_LEGACY',
        'BRIEFING_ROUTE_RETAIL', 'CONFIRM_FROM_START_LEGACY', 'LEGACY_SAVE_NORMALIZATION',
        'CLOSE_DIALOG_LEGACY') if counter.get(name)]
    if not basis:
        return ('UNRESOLVED', 'OTHER_DIFF', '类别不构成可采纳组合：%s' % dict(counter), kinds)
    return ('ADOPT_RETAIL', '+'.join(basis), '%s；类别 %s' % (coverage, dict(counter)), kinds)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--analyze', action='store_true')
    parser.add_argument('--write', action='store_true')
    parser.add_argument('--census', default=None, help='普查 TSV（缺省 P0c-8c 全量普查）')
    parser.add_argument('--out', default=None, help='裁定表输出路径（--write 时）')
    args = parser.parse_args()
    rows = read_census(Path(args.census) if args.census else None)
    names = npc_names()
    client, gates = client_evidence(names)
    ctx = {'client': client, 'gates': gates, 'spawned': spawned_ids(), 'names': names,
        'retail': retail_rows(), 'report_pages': report_pages(), 'client_actions': client_action_ids(),
        'registered_pages': registered_page_ids()}
    decisions = {q: decide(q, r, ctx) for q, r in rows.items()}
    print('裁定 %d / %d 行' % (len(decisions), len(rows)))
    for kind, count in Counter(v[0] for v in decisions.values()).most_common():
        print('  %-14s %d' % (kind, count))
    print('  basis 分布：', dict(Counter(v[1] for v in decisions.values()).most_common(14)))
    for verdict in ('KEEP_XML', 'UNRESOLVED'):
        subset = {q: v for q, v in decisions.items() if v[0] == verdict}
        print('=== %s %d 行 ===' % (verdict, len(subset)))
        for quest_id, value in sorted(subset.items()):
            print('  #%-6d %-28s %s' % (quest_id, value[1], value[2][:150]))
    if args.write:
        text = ('# P0c-8c SimpleHunt 缺口表剩余行逐行裁定（真端优先 + 客户端契约）\n'
                '# 判据与逐行断言见 .agents/summary/scriptdll-quest-driver/p0c8c_gap_decisions.py 头注释\n'
                '# quest_id\tverdict\tbasis\taxes\tevidence\n')
        for quest_id, value in sorted(decisions.items()):
            evidence = ('%s；类别 %s' % (value[2], dict(Counter(value[3])))).replace('\t', ' ')
            text += '%d\t%s\t%s\t-\t%s\n' % (quest_id, value[0], value[1], evidence)
        OUT = Path(args.out) if args.out else OUT
        OUT.write_text(text, encoding='utf-8')
        print('裁定表 ->', OUT)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
