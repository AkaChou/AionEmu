#!/usr/bin/env python3
"""M3 起点：SimpleTalk 族形状勘察（真端表 × 生产 XML）。

对 `Quest_SimpleTalk.xml` 全表与仓库生产任务取交集，按
(acquired==reward, talk_npc 链长, give_item, remove_item, item_check, con_quest, cutscene)
分桶，并统计其生产 XML 的节点形状与过渡签名种类，作为 M3 合成器的起点基线。

输出：.agents/summary/scriptdll-quest-driver/retail-simple-talk-shapes.tsv
"""
from __future__ import annotations

import collections
import pathlib
import re

REPO = pathlib.Path(__file__).resolve().parents[3]
PROD = REPO / 'src/main/resources/aion/data/static_data'
QUESTS = PROD / 'quest_definition/quests'
TABLE = PROD / 'quest_retail/Quest_SimpleTalk.xml'
CATALOG = PROD / 'quest_definition/quest_definition_catalog.xml'
OUT = REPO / '.agents/summary/scriptdll-quest-driver/retail-simple-talk-shapes.tsv'


def field(body: str, name: str) -> str:
    m = re.search(rf'<{name}>(.*?)</{name}>', body, re.S)
    return m.group(1).strip() if m else ''


def main() -> int:
    catalog = {int(v) for v in re.findall(r'<definition id="(\d+)"',
                                          CATALOG.read_text(encoding='utf-8'))}
    body = TABLE.read_text(encoding='utf-8').split(']>', 1)[1]
    rows = {int(m.group(1)): m.group(2) for m in re.finditer(r'<id id="(\d+)">(.*?)</id>', body, re.S)}
    rows = {q: b for q, b in rows.items() if q in catalog}
    shapes = collections.Counter()
    detail = []
    for quest_id, row in sorted(rows.items()):
        path = QUESTS / f'{quest_id}.xml'
        text = path.read_text(encoding='utf-8') if path.exists() else ''
        nodes = tuple(re.findall(r'<node label="([^"]+)" status="([^"]+)"', text))
        blocks = tuple(x or y for x, y in re.findall(r'<dialog type="([A-Z_]+)"|<(npc-complete)\b', text))
        chain = sum(1 for i in (1, 2, 3) if field(row, f'talk_npc{i}'))
        shape = (
            'same' if field(row, 'acquired_npc_name') == field(row, 'reward_npc_name') else 'diff',
            f'chain{chain}',
            'give' if '<give_item' in row else '-',
            'rem' if '<remove_item' in row else '-',
            'check' if '<item_check' in row else '-',
            'cond' if '<con_quest' in row else '-',
            'cut' if '<cutsceneid1' in row else '-',
        )
        shapes[shape] += 1
        detail.append((quest_id, shape, nodes, blocks))
    missing = sum(1 for _, _, nodes, _ in detail if not nodes)
    with OUT.open('w', encoding='utf-8') as fh:
        fh.write('# SimpleTalk 形状勘察（quest_id, shape, node 形状, 过渡块序列）\n')
        fh.write(f'# 表内行数={len(rows)}（∩ 生产 catalog）\n')
        for shape, count in shapes.most_common():
            fh.write(f'# {count}\t{"/".join(shape)}\n')
        for quest_id, shape, nodes, blocks in detail:
            fh.write(f'{quest_id}\t{"/".join(shape)}\t'
                     f'{",".join(f"{l}:{s}" for l, s in nodes)}\t{",".join(blocks)}\n')
    print(f'table∩catalog={len(rows)} 无节点={missing} 形状种类={len(shapes)}')
    for shape, count in shapes.most_common(10):
        print(f'  {count:5d}  {"/".join(shape)}')
    print('wrote', OUT)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
