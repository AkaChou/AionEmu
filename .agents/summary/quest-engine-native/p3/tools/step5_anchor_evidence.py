#!/usr/bin/env python3
"""P3 步骤 5 证据导出：为 10 个待重锚测试类冻结真端行 + 静态数据 id + 客户端页集。

口径（计划 §8.9）：断言只来自真端表行（Quest_SimpleTalk.xml / quest.xml）+ 静态数据
（npc_template / item name_desc / quest_name_string_ids）+ 客户端对话契约页集；
不复用 Java 侧实现取数。输出 TSV 供测试类冻结字面量，并作为漂移证据留档。
"""
import importlib.util
import pathlib
import re

REPO = pathlib.Path(__file__).resolve().parents[5]
TOOLS = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('gen', TOOLS / 'gen_simple_talk_native_anchors.py')
gen = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gen)

DATA = REPO / 'src/main/resources/aion/data/static_data'
CONTRACT = REPO / 'src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv'
NAME_IDS = DATA / 'quest/retail/quest_name_string_ids.tsv'
OUT = REPO / '.agents/summary/quest-engine-native/p3/step5-anchor-evidence.tsv'

TARGETS = [
    1101, 1104, 1105, 1106, 1108, 1110, 1115, 1116, 1117, 1118, 1131, 1141, 1156, 1158,
    1183, 1414, 1605, 1648, 1691, 1913, 2414, 2511, 2569, 2631, 2654, 2434, 49603,
    80016, 80018, 80030, 80033, 80034, 80035, 80036, 80037, 80369, 80386, 80487, 80538,
    1102, 1149, 1914, 1915, 1916, 80028, 80031, 80032, 19671, 19673, 29671,
]


def read(path):
    raw = pathlib.Path(path).read_bytes()
    return raw.decode('utf-16' if raw[:2] in (b'\xff\xfe', b'\xfe\xff') else 'utf-8', errors='replace')


def load_pages():
    pages = {}
    for line in read(CONTRACT).splitlines():
        if line.startswith('#') or not line.strip():
            continue
        quest, page, name = line.split('\t')
        pages.setdefault(int(quest), {})[int(page)] = name
    return pages


def load_name_ids():
    ids = {}
    for line in read(NAME_IDS).splitlines():
        if line.startswith('#') or not line.strip():
            continue
        parts = line.split('\t')
        if len(parts) >= 2 and parts[0].strip().isdigit():
            ids[int(parts[0])] = parts[1].strip()
    return ids


def resolve_symbol(items, symbol):
    if not symbol:
        return ''
    parts = symbol.split()
    stem = parts[0].lower()
    iid = items.get(stem)
    if iid is None and stem.startswith('item_'):
        iid = items.get(stem[len('item_'):])
    if iid is None:
        return 'UNRESOLVED:' + symbol
    return '%d x %s' % (iid, parts[1] if len(parts) > 1 else 1)


def main():
    rows = gen.load_table()
    quests = gen.load_quests()
    resolver = gen.load_npcs()
    items = gen.load_items()
    pages = load_pages()
    name_ids = load_name_ids()
    header = ['quest_id', 'dev_name', 'acquire_name', 'acquire_npc', 'reward_name', 'reward_npc',
              'relays', 'relay_npcs', 'give', 'remove', 'step_gives', 'step_removes',
              'item_check', 'gate_items', 'con_quest', 'cutscene', 'client_pages', 'name_string_id',
              'category1', 'minlevel', 'maxlevel', 'race', 'class', 'max_repeat', 'reward_exp', 'reward_gold',
              'reward_items', 'drop_monster', 'drop_npc', 'drop_item', 'drop_prob', 'finished_cond',
              'reward_title', 'npctaction', 'inventory_items']
    out = ['# P3 步骤 5 冻结证据（真端表行 + 静态数据 id + 客户端页集）；生成器 p3/tools/step5_anchor_evidence.py',
           '\t'.join(header)]
    for quest_id in sorted(set(TARGETS)):
        row = rows.get(quest_id, {})
        quest = quests.get(quest_id, {})
        acquire = gen.npc_id(resolver, row.get('acquired_npc_name'))
        reward = gen.npc_id(resolver, row.get('reward_npc_name'))
        relays = [(i, gen.npc_id(resolver, row.get('talk_npc%d' % i))) for i in (1, 2, 3)
                  if row.get('talk_npc%d' % i)]
        gate, verdict = gen.gate(items, quests, quest_id)
        def q(name, default=''):
            values = quest.get(name)
            return values[0] if values else default
        def numbered_q(base):
            out = []
            for key, values in quest.items():
                m = re.fullmatch(rf'{base}(\d+)', key)
                if m and values:
                    out.append((int(m.group(1)), values[0]))
            return ';'.join(resolve_symbol(items, v) for _, v in sorted(out))
        rewards = numbered_q('reward_item1_')
        drops = ';'.join(filter(None, [
            '|'.join(x for x in (q('drop_monster_1'), str(gen.npc_id(resolver, q('drop_monster_1'))),
                                 resolve_symbol(items, q('drop_item_1')), q('drop_prob_1')) if x)])
        )
        fields = [
            str(quest_id),
            row.get('dev_name', ''),
            row.get('acquired_npc_name', ''),
            str(acquire) if acquire is not None else '',
            row.get('reward_npc_name', ''),
            str(reward) if reward is not None else '',
            str(len(relays)),
            ','.join('%d=%s' % (i, npc) for i, npc in relays),
            resolve_symbol(items, row.get('give_item')),
            resolve_symbol(items, row.get('remove_item')),
            ';'.join(resolve_symbol(items, row.get('give_item%d' % i)) for i in (1, 2, 3)
                     if row.get('give_item%d' % i)),
            ';'.join(resolve_symbol(items, row.get('remove_item%d' % i)) for i in (1, 2, 3)
                     if row.get('remove_item%d' % i)),
            row.get('item_check', ''),
            ';'.join('%d x %d' % s for s in gate),
            row.get('con_quest', ''),
            '%s/%s' % (row.get('cutsceneid1', ''), row.get('cs1_haction', '')),
            ','.join('%d=%s' % (p, pages[quest_id][p]) for p in sorted(pages.get(quest_id, {}))),
            name_ids.get(quest_id, ''),
            q('category1'),
            q('minlevel_permitted') or q('client_level'),
            q('maxlevel_permitted'),
            q('race_permitted'),
            q('class_permitted'),
            q('max_repeat_count'),
            q('reward_exp1'),
            q('reward_gold1'),
            rewards,
            q('drop_monster_1'),
            str(gen.npc_id(resolver, q('drop_monster_1'))),
            resolve_symbol(items, q('drop_item_1')),
            q('drop_prob_1'),
            q('finished_quest_cond1'),
            q('reward_title1'),
            q('npcfaction_name'),
            numbered_q('inventory_item_name'),
        ]
        # 行尾空列不写尾随制表符（TSV 读取语义不变，`git diff --check` 干净）。
        # Trailing empty columns are trimmed so the frozen TSV stays whitespace-clean.
        out.append('\t'.join(fields).rstrip('\t'))
    OUT.write_text('\n'.join(out) + '\n', encoding='utf-8')
    print('wrote', OUT, len(out) - 2, 'rows')


if __name__ == '__main__':
    main()
