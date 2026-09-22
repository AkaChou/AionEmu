#!/usr/bin/env python3
"""批次 48：11012（Practical Nursing）补齐客户端 4 行阶梯并收敛领奖 owner。

证据链 / Evidence chain:
- 客户端 quest_q11012.html 的 quest_summary 共 4 行：行 0/1/2 = 依次治疗
  LF4_patient_1/2/3（799072/799073/799074），行 3 = 和 Naiting（799071）对话。
- 客户端页 select1(1011)/select2(1352)/select3(1693) 的按钮分别是
  HACTION_SETPRO1(10000)/SETPRO2(10001)/SETPRO3(10002)，领奖页 select_success(10002) 的按钮是
  HACTION_SELECT_QUEST_REWARD(1009)。
- 迁移前 handler `_11012PracticalNursing`（7e9f0316c^）在 799072(var0==0)/799073(var0==1)/
  799074(var0==2) 上各消耗 1 个绷带 182206715 并把 var0 推一格，799074 处直接 setStatus(REWARD)，
  799071 是接取与领奖 owner；typed 迁移把 4 个 NPC 都写成 NPC_REPORT -> reward 直跳，
  行 1/2/3 没有状态（审计 MISSING_TAIL_ROWS）。
- 接取发放 3 个绷带（legacy sendQuestStartDialog(env, 182206715, 3)）需要显式 accept-actions
  （QE-049：work-items 只声明不自动发放）。
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

QUEST = Path('src/main/resources/aion/data/static_data/quest_definition/quests/11012.xml')

OLD_NODES = """  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="0"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
"""

NEW_NODES = """  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="stage1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="stage2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="3"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
"""

OLD_TRANSITIONS = """  <transitions>
    <dialog type="NPC_START" npc-id="799071" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <dialog type="NPC_REPORT" npc-id="799071" source="started" target="reward" page="SELECT2"/>
    <npc-complete npc-id="799071" source="reward" target="complete" fixed-reward-indices="0 1 2 3" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>

    <dialog type="NPC_REPORT" npc-id="799072" source="started" target="reward" page="SELECT2"/>
    <npc-complete npc-id="799072" source="reward" target="complete" fixed-reward-indices="0 1 2 3" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>

    <dialog type="NPC_REPORT" npc-id="799073" source="started" target="reward" page="SELECT2"/>
    <npc-complete npc-id="799073" source="reward" target="complete" fixed-reward-indices="0 1 2 3" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>

    <dialog type="NPC_REPORT" npc-id="799074" source="started" target="reward" page="SELECT2"/>
    <npc-complete npc-id="799074" source="reward" target="complete" fixed-reward-indices="0 1 2 3" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
    <transition source="started" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799071" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="started" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799072" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="started" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799073" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="started" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799074" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
  </transitions>
"""

NEW_TRANSITIONS = """  <transitions>
    <!-- QE-051 领奖行合同 / QE-051 reward-row contract：客户端 quest_summary 共 4 行
         （行 0/1/2 = 依次治疗 STR_DIC_NPC_LF4_patient_1/2/3（799072/799073/799074），行 3 = 和 STR_DIC_N_Naiting（799071）对话）。
         旧定义把 4 个 NPC 都挂上 NPC_REPORT -> reward 直跳，行 1/2/3 没有状态；改后三名患者各推进一格并各消耗
         1 个绷带 182206715，接取与领奖都收在 799071 上（QE-052 owner 收敛）。
         The client journal declares 4 rows; the old definition let all four NPCs jump straight to
         reward, so rows 1-3 had no states. Each patient now advances exactly one row and both the
         offer and the completion stay on 799071. -->
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="3"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="2"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="3"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <dialog type="NPC_START" npc-id="799071" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE">
      <accept-actions>
        <give-item item-id="182206715" count="3"/>
      </accept-actions>
    </dialog>

    <!-- 行 0：LF4_patient_1（799072）；客户端页 select1(1011) 的按钮是 HACTION_SETPRO1(10000)，每次治疗消耗 1 个绷带。
         Row 0: LF4_patient_1 (799072); the client page select1(1011) carries HACTION_SETPRO1(10000) and each
         treatment consumes one bandage. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799072" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="stage1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799072" action="SETPRO1"/>
      </event>
      <actions>
        <remove-item item-id="182206715" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 行 1：LF4_patient_2（799073）；客户端页 select2(1352) 的按钮是 HACTION_SETPRO2(10001)。
         Row 1: LF4_patient_2 (799073); the client page select2(1352) carries HACTION_SETPRO2(10001). -->
    <transition source="stage1" target="stage1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799073" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="stage1" target="stage2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799073" action="SETPRO2"/>
      </event>
      <actions>
        <remove-item item-id="182206715" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 行 2：LF4_patient_3（799074）；客户端页 select3(1693) 的按钮是 HACTION_SETPRO3(10002)。
         Row 2: LF4_patient_3 (799074); the client page select3(1693) carries HACTION_SETPRO3(10002). -->
    <transition source="stage2" target="stage2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799074" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="stage2" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799074" action="SETPRO3"/>
      </event>
      <actions>
        <remove-item item-id="182206715" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 行 3（领奖行）：和 STR_DIC_N_Naiting（799071）对话；客户端页 select_success(10002 = DEFAULT_SUCCESS)
         的按钮 HACTION_SELECT_QUEST_REWARD(1009) 由 npc-complete 的 preview 路由承接，避免 AMBIGUOUS_TRANSITION
         （批次 19 教训）。
         Row 3 (reward row): talk to Naiting (799071); the client select_success(10002) button stays on the
         npc-complete preview route. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799071" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>

    <npc-complete npc-id="799071" source="reward" target="complete" fixed-reward-indices="0 1 2 3" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
"""


def render(current: str) -> str:
    if OLD_NODES not in current:
        raise SystemExit('FAIL: 11012.xml nodes 段与预期不符 / unexpected nodes block')
    if OLD_TRANSITIONS not in current:
        raise SystemExit('FAIL: 11012.xml transitions 段与预期不符 / unexpected transitions block')
    updated = current.replace(OLD_NODES, NEW_NODES, 1).replace(OLD_TRANSITIONS, NEW_TRANSITIONS, 1)
    if re.search(r'<node label="reward" status="REWARD">\s*<var name="var0" value="0"/>', updated):
        raise SystemExit('FAIL: reward 投影仍是 0')
    return updated


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    if not args.apply and not args.check:
        parser.error('use --check or --apply')
    current = QUEST.read_text(encoding='utf-8')
    if NEW_NODES in current and NEW_TRANSITIONS in current:
        print('CHECK_OK 11012 already at batch-48 target state')
        return 0
    expected = render(current)
    if args.check:
        print('CHECK_PENDING 11012 needs batch-48 apply')
        return 0
    QUEST.write_text(expected, encoding='utf-8')
    print('APPLIED 11012 -> 4-row ladder (0/1/2/3) with accept grant + owner trim')
    return 0


if __name__ == '__main__':
    sys.exit(main())
