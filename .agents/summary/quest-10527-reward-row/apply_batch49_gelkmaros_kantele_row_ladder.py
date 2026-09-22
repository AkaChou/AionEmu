#!/usr/bin/env python3
"""批次 49：21027（[Group] Fearless Kantele）补齐客户端 3 行阶梯并收敛领奖 owner。

证据链 / Evidence chain:
- 客户端 quest_q21027.html 的 quest_summary 共 3 行：行 0 = 找到被关押的 Kantele（799255），
  行 1 = 从 Owllau 精锐身上找钥匙交给 Kantele，行 2 = 和 Asathor（799254）对话报平安。
- 客户端页 select1(1011)/select1_1(1012)/select2(1352) 的按钮分别是
  HACTION_SELECT1_1(1012)/SETPRO1(10000)/CHECK_USER_HAS_QUEST_ITEM(39)，成功/失败页
  check_user_item_ok(10000)/fail(10001)，领奖页 select_success(10002) 的按钮是
  HACTION_SELECT_QUEST_REWARD(1009)。
- 迁移前 handler `_21027FearlessKantele`（7e9f0316c^）：799254 = 接取 + REWARD 领奖 owner，
  799255 = Kantele：var0==0 显示 1011 且 `STEP_TO_1` -> defaultCloseDialog(0,1)；
  var0==1 显示 1352 且 `checkQuestItems(1, 2, true, 10000, 10001)`（交付钥匙、var0=2、置 REWARD）。
- typed 迁移把交错状态塌陷成 799254 上的 `NPC_REPORT -> reward` 直跳 + reward 投影 0，
  行 1/2 没有状态（审计 ROW_BEHIND | MISSING_TAIL_ROWS | ROW_WITHOUT_STATE，visible=0）。
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

QUEST = Path('src/main/resources/aion/data/static_data/quest_definition/quests/21027.xml')

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
    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
"""

NEW_TRANSITIONS = """  <transitions>
    <!-- QE-051 领奖行合同 / QE-051 reward-row contract：客户端 quest_summary 共 3 行
         （行 0 = 找到 Kantele 799255、行 1 = 从 Owllau 身上找钥匙交给 Kantele、行 2 = 和 Asathor 799254 对话）。
         旧定义把交钥匙收在 Asathor 身上并直跳 reward，行 1/2 没有状态；改后 Kantele 走 0 -> 1 阶梯与
         CHECK_USER_HAS_QUEST_ITEM 交付（1 -> 2 + REWARD），接取与领奖都收在 Asathor 上（QE-052 owner 收敛）。
         The client journal declares 3 rows; Kantele now walks the ladder while the offer and the
         completion stay on Asathor. -->
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
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
        <variable-is field="var0" value="1"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <dialog type="NPC_START" npc-id="799254" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>

    <!-- 行 0：被关押的 Kantele（799255）；客户端页 select1(1011) 的按钮 HACTION_SELECT1_1(1012)
         打开说明页 select1_1(1012)，其按钮 HACTION_SETPRO1(10000) 把任务推进到行 1。
         Row 0: imprisoned Kantele (799255); select1(1011) opens select1_1(1012) via HACTION_SELECT1_1,
         whose HACTION_SETPRO1(10000) advances the journal to row 1. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799255" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799255" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="stage1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799255" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 行 1：把 STR_DIC_I_QUEST_21027a（182207824）交给 Kantele；页 select2(1352) 的按钮
         HACTION_CHECK_USER_HAS_QUEST_ITEM(39) 有钥匙时消耗并进 REWARD，没有时回失败页。
         Row 1: hand the key 182207824 to Kantele; select2(1352) carries HACTION_CHECK_USER_HAS_QUEST_ITEM(39). -->
    <transition source="stage1" target="stage1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799255" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="stage1" target="reward" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799255" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="182207824" count="1"/>
      </conditions>
      <actions>
        <remove-item item-id="182207824" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>
      </after-commit>
    </transition>
    <transition source="stage1" target="stage1" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799255" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>
      </after-commit>
    </transition>
    <!-- check_user_item_ok / check_user_item_fail 两个结果页的按钮都是 HACTION_FINISH_DIALOG(1008)，
         需要各自所在状态（reward / stage1）的关闭路由，否则门禁报 BUTTON_WITHOUT_ROUTE。
         Both result pages carry HACTION_FINISH_DIALOG(1008), so each needs a close route in the state
         that renders it (reward / stage1). -->
    <transition source="stage1" target="stage1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799255" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799255" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 行 2（领奖行）：和 Asathor（799254）对话；客户端页 select_success(10002 = DEFAULT_SUCCESS)
         的按钮 HACTION_SELECT_QUEST_REWARD(1009) 由 npc-complete 的 preview 路由承接。
         Row 2 (reward row): report to Asathor (799254); the select_success button stays on the
         npc-complete preview route. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799254" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="799254" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
"""


def apply_sections(current: str) -> str:
    updated, nodes_replaced = re.subn(r'  <nodes>.*?</nodes>\n', NEW_NODES, current,
                                      count=1, flags=re.S)
    if nodes_replaced != 1:
        raise SystemExit('FAIL: nodes 段未替换 / nodes block was not replaced')
    updated, transitions_replaced = re.subn(r'  <transitions>.*?</transitions>',
                                            NEW_TRANSITIONS.rstrip('\n'), updated,
                                            count=1, flags=re.S)
    if transitions_replaced != 1:
        raise SystemExit('FAIL: transitions 段未替换 / transitions block was not replaced')
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
        print('CHECK_OK 21027 already at batch-49 target state')
        return 0
    expected = apply_sections(current)
    if args.check:
        print('CHECK_PENDING 21027 needs batch-49 apply')
        return 0
    QUEST.write_text(expected, encoding='utf-8')
    print('APPLIED 21027 -> 3-row ladder (0/1/2) with Kantele hand-over and Asathor owner trim')
    return 0


if __name__ == '__main__':
    sys.exit(main())
