#!/usr/bin/env python3
"""批次 51：3938/4942（神圣圣殿骑士晋级双子）补齐任务书末两行状态（step 9/10）。

证据链 / Evidence chain:
- 客户端任务书（QUEST_Q3938.html / QUEST_Q4942.html）的 quest_summary 各 11 行，可见槽位
  0/3/6/.../30（每行 3 个槽位：visible/color + 保留位）：
  行 0 = 和 Lavirintos(203701)/Kvasir(204053) 对话选择制造技术，
  行 1..6 = 六种工艺名人分支（各自交一枚圣物，推进到 var0=7），
  行 7 = “和制造名人对话”（var0=7，798316/798317 处 CHECK_COLLECTED_ITEMS 收 186000077 推进 var0=8），
  行 8 = “带着圣物去见大神官 Jucleas(203752)/Balder(204075)，举行神圣仪式”（var0=8），
  行 9 = 举行仪式（大神官交出 186000081/186000085；`select9_1` 的 SET_SUCCEED），
  行 10 = 回 Lavirintos/Kvasir 出示徽章领奖（客户端 `select_success` 的 SELECT_QUEST_REWARD，
      进入 REWARD）。
- 迁移前 handler（origin/history:.../_3938Well_Rounded.java）的阶梯是
  0(接取) → 1..6(六种工艺分支) → 7(制造名人交圣物) → 8(大神官处 SET_REWARD 收仪式道具)
  → REWARD（回 Lavirintos 出示 186000081/186000085 领奖）：任务书 11 行的末两行分别对应
  step 9（仪式完成）与 step 10（领奖行，进入 REWARD）。
- 现状缺陷：typed 定义的 START/REWARD 阶梯只到 s8，reward 投影停在 8（3938）或 0（4942），
  于是末两行永远没有状态、任务书在仪式后停在行 8；审计判
  ROW_BEHIND | MISSING_TAIL_ROWS | ROW_WITHOUT_STATE（缺行 9、10）。
- 落点：
  1. 新增 `<node label="s9" status="START">`（step 9）与 `<node label="s10" status="REWARD">`（step 10），
     删除旧 `reward` 节点；
  2. 大神官处 `SET_SUCCEED` 收仪式道具路由 s8 -> s9（并补 var0=8 门控）；
  3. 起始 NPC 处补 s9 -> s10 末行领取路由（`SELECT_QUEST_REWARD` -> 奖励窗；失败页回落同节点），
     `npc-complete` 归属迁到 s10（领奖行进入后 choice 按钮完成），预览只保留 `USE_OBJECT`，
     避免与领取路由在同一步上产生同事件双路由；
  4. 无 source 的 `REWARD/var0<10 -> s10` 自愈边，让迁移期停在 var0=0/8 的领奖存档登录后落到末行。
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

QUESTS = {
    3938: {"ritual_npc": 203752, "ritual_item": 186000081, "handover_npc": 203701, "old_reward": 8},
    4942: {"ritual_npc": 204075, "ritual_item": 186000085, "handover_npc": 204053, "old_reward": 0},
}
FINAL_STEP = 10
QUEST_DIR = Path('src/main/resources/aion/data/static_data/quest/definitions/quests')


def node(label: str, status: str, value: int) -> str:
    return (f'    <node label="{label}" status="{status}">\n'
            f'      <var name="var0" value="{value}"/>\n'
            f'    </node>\n')


def heal_edge(old_reward: int) -> str:
    return f"""    <!-- 自愈边：迁移期把 REWARD 投影停在 var0={old_reward}，领奖态旧存档进世界时补到末行 10。
         Heal edge: the migrated REWARD projection stopped at {old_reward}, so stale reward saves move to step 10. -->
    <transition target="s10">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-below field="var0" value="10"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="10"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
"""


def claim_route(spec: dict) -> str:
    npc = spec['handover_npc']
    return f"""    <!-- 末行（step 10）：回起始 NPC 出示徽章领奖（客户端 select_success 的 SELECT_QUEST_REWARD）。
         Row 10 hand-over: the starting NPC takes the badge and opens the reward window. -->
    <transition source="s9" target="s10" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SELECT_QUEST_REWARD"/>
      </event>
      <conditions>
        <variable-is field="var0" value="9"/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="s9" target="s9" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
"""


def transform(quest_id: int, text: str) -> str:
    spec = QUESTS[quest_id]
    old_reward = spec['old_reward']
    if node('s10', 'START', FINAL_STEP) in text:
        return text  # 幂等：已按新结构落码

    if node('s9', 'REWARD', 9) in text:
        text = text.replace(node('s9', 'REWARD', 9) + node('s10', 'START', FINAL_STEP),
                            node('s9', 'START', 9) + node('s10', 'REWARD', FINAL_STEP), 1)
    elif node('s9', 'START', 9) in text:
        text = text.replace(node('s9', 'START', 9),
                            node('s9', 'START', 9) + node('s10', 'REWARD', FINAL_STEP), 1)
    else:
        s8 = node('s8', 'START', 8)
        assert s8 in text, f'quest {quest_id}: s8 node not found'
        text = text.replace(
            s8,
            s8 + node('s9', 'START', 9) + node('s10', 'REWARD', FINAL_STEP),
            1)

    old_reward_node = node('reward', 'REWARD', old_reward)
    if old_reward_node not in text:
        old_reward_node = node('reward', 'REWARD', FINAL_STEP)
    assert old_reward_node in text, f'quest {quest_id}: reward node not found'
    text = text.replace(old_reward_node, '', 1)

    # 大神官处收仪式道具：路由改到 step 9 的 REWARD 行。
    if 'target="s10" priority="0"' in text:
        text = text.replace('target="s10" priority="0"', 'target="s9" priority="0"', 1)
    else:
        text = text.replace('target="reward" priority="0"', 'target="s9" priority="0"', 1)
    assert 'target="s9" priority="0"' in text, f'quest {quest_id}: ritual route not found'

    # 仪式路由补 var0=8 门控（4942 侧迁移时漏了）。
    ritual_pattern = re.compile(
        r'(<transition source="s8" target="s9" priority="0">.*?<conditions>\n)'
        r'((?:\s*<[^>]+/>\n)*)'
        r'(\s*</conditions>)', re.S)
    match = ritual_pattern.search(text)
    assert match is not None, f'quest {quest_id}: ritual conditions block not found'
    if '<variable-is field="var0" value="8"/>' not in match.group(2):
        text = (text[:match.start(2)]
                + f'        <variable-is field="var0" value="8"/>\n{match.group(2)}'
                + text[match.end(2):])
    # 目标节点 s9 自带 var0=9 投影，仪式路由里遗留的 var0=8 写入会覆盖它。
    text = re.sub(
        r'(<transition source="s8" target="s9" priority="0">.*?<actions>\n)'
        r'\s*<set-variable field="var0" value="8"/>\n',
        r'\1',
        text,
        count=1,
        flags=re.S)

    # 旧 reward 节点的 USE_OBJECT 成功页路由与 npc-complete 归属迁到 step 9；
    # SELECT_QUEST_REWARD 领取路由由新的 s9 -> s10 claim_route 承担，避免同事件重复。
    # 旧 reward 节点上的领奖窗口路由不再需要：USE_OBJECT/SELECT_QUEST_REWARD 由新的
    # s9 -> s10 领取路由与 npc-complete 预览承担，因此统一下沉到 claim_route，避免同事件重复。
    text = re.sub(
        r'    <transition source="reward" target="reward">\n'
        r'      <event>\n'
        r'        <dialog type="TALK_TO_NPC" npc-id="\d+" action="[A-Z0-9_]+"/>\n'
        r'      </event>\n'
        r'      <after-commit>\n'
        r'        <dialog type="SHOW_QUEST_PAGE" page="[A-Z0-9_]+"/>\n'
        r'      </after-commit>\n'
        r'    </transition>\n?',
        '',
        text)
    text = re.sub(r'<npc-complete npc-id="(\d+)" source="reward"',
                  r'<npc-complete npc-id="\1" source="s10"', text)
    # 4942 迁移时在仪式路由里就下发奖励窗；3938 只有 close-dialog。统一为 close-dialog，
    # 奖励窗改由末行 s9 -> s10 的 SELECT_QUEST_REWARD 打开，任务书才不会停在仪式行。
    text = re.sub(
        r'(<transition source="s8" target="s9" priority="0">.*?<after-commit>\n'
        r'\s*<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n)'
        r'\s*<dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>\n',
        r'\1        <close-dialog/>\n',
        text,
        count=1,
        flags=re.S)
    # 4942 的 npc-complete 预览只保留 USE_OBJECT：SELECT_QUEST_REWARD 由新的 s9 -> s10
    # 领取路由承担，避免同一步同事件双路由；3938 原本没有预览，需要补一条。
    text = text.replace('<preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>',
                        '<preview actions="USE_OBJECT"/>')
    if '<preview actions=' not in text:
        text = text.replace(
            '    </npc-complete>\n',
            '      <preview actions="USE_OBJECT"/>\n    </npc-complete>\n',
            1)
    assert 'source="reward"' not in text, f'quest {quest_id}: reward-node routes not migrated'

    marker = '  <transitions>\n'
    assert marker in text
    block = heal_edge(old_reward) + claim_route(spec)
    if '<variable-below field="var0" value="10"/>' in text:
        return text
    text = text.replace(marker, marker + block, 1)
    return text


def _use_object_route(spec: dict) -> str:
    return f"""    <transition source="s9" target="s9">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{spec['handover_npc']}" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
"""


def main() -> int:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check', action='store_true')
    group.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    status = 0
    for quest_id, spec in QUESTS.items():
        path = QUEST_DIR / f'{quest_id}.xml'
        text = path.read_text(encoding='utf-8')
        applied = (node('s10', 'REWARD', FINAL_STEP) in text
                   and node('s9', 'START', 9) in text
                   and 'source="s9" target="s10"' in text
                   and 'source="reward"' not in text
                   and '<variable-below field="var0" value="10"/>' in text
                   and node('reward', 'REWARD', spec['old_reward']) not in text
                   and node('s10', 'START', FINAL_STEP) not in text)
        if args.check:
            print(f'{quest_id}: {"OK" if applied else "NEEDS_APPLY"} ({path})')
            status |= 0 if applied else 1
            continue
        if applied:
            print(f'{quest_id}: already applied (idempotent)')
            continue
        path.write_text(transform(quest_id, text), encoding='utf-8')
        print(f'{quest_id}: applied s9(START)/s10(REWARD) + claim route + heal edge ({path})')
    return status


if __name__ == '__main__':
    sys.exit(main())
