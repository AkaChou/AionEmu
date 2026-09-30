#!/usr/bin/env python3
"""客户端/真端交互物 NPC 登记：ai="quest_use_item" 的模板 id。

来源：src/main/resources/aion/data/static_data/npcs/npc_template_*.xml（NPC 模板的 ai 属性）。
用途：启动期交互对象合同（QuestInteractionObjectValidator）要求"掉落物箱 NPC 必须有 START 态
ACTION_ITEM_USE 路由"，而合成器不该为普通怪物掉落也发这种路由 → 由本表给出精确集合。

生成物：src/main/resources/aion/data/static_data/quest_retail/quest_use_item_npcs.tsv
"""
import os
from pathlib import Path
import re

REPO = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
NPC_DIR = REPO / 'src/main/resources/aion/data/static_data/npcs'
OUT = REPO / 'src/main/resources/aion/data/static_data/quest_retail/quest_use_item_npcs.tsv'


def main():
    ids = set()
    for shard in sorted(NPC_DIR.glob('npc_template_*.xml')):
        text = shard.read_text(encoding='utf-8', errors='replace')
        for match in re.finditer(r'<npc_template[^>]*>', text):
            tag = match.group(0)
            ai = re.search(r'\bai="([^"]*)"', tag)
            npc_id = re.search(r'\bnpc_id="(\d+)"', tag)
            if ai and npc_id and ai.group(1) == 'quest_use_item':
                ids.add(int(npc_id.group(1)))
    rows = ['# 交互物 NPC 登记（ai=quest_use_item 的模板 id；升序）',
            '# 来源：src/main/resources/aion/data/static_data/npcs/npc_template_*.xml 的 ai 属性',
            '# 生成：python3 -B .agents/summary/scriptdll-quest-driver/build_quest_use_item_npcs.py',
            '# 语义：这些 NPC 的任务掉落需要 START 态 ACTION_ITEM_USE 路由（启动期交互对象合同）']
    rows.extend(str(npc_id) for npc_id in sorted(ids))
    OUT.write_text('\n'.join(rows) + '\n', encoding='utf-8')
    print(f'quest_use_item npcs={len(ids)} -> {OUT.relative_to(REPO)}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
