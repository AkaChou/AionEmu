#!/usr/bin/env python3
"""批次 33：共享可见槽位族（25094 An Offering of Friendship）的行/状态收口。

族判据（2026-09-22）：
- 客户端 quest_q25094.html 的 quest_summary 有 3 行，但其可见槽位是 `%0 / %3 / %3`：
  行 0「把 quest_25094a 交给 DF5_Bakring_E 吧 [collectitem]」用 `[%0]`，行 1「和 Bakring 对话」与
  行 2「把贝克灵的礼物交给 DF5_Daruku_E」**共用 `[%3]`**。全库 quest_summary 槽位序列里只有这一个
  3 行任务是重复槽位形态（其余 317 个 3 行任务要么 0/3/6，要么其它单调序列）。
- 槽位与状态的关系由已客户端验收的 10527/20527 锁定：16 行任务的槽位是 `0,3,6,...,45`，与 var0=行号
  一一对应（用户实测 reward=15 正确），即 `槽位 = 3 × 状态号`。因此 25094 的客户端真实状态只有 **2 个**：
  状态 0 = 行 0，状态 1 = 行 1 与行 2（同时可见）。行号口径（client_rows=3）在本任务多算了一行，
  这也是审计把它判成 MISSING_TAIL_ROWS + ROW_WITHOUT_STATE 的原因。
- retail `zz_retail_simple_quests.xml`：`<data_driven_quest id="25094" start_type="TALK" start_ids="804929"
  end_npc_ids="804740" reset_world_id="300280000"><step type="COLLECT_ITEM" ids="804929"
  action_ids="702768"/></data_driven_quest>`；领奖合同是 REWARD 态 `31 -> DEFAULT_SUCCESS(10002)`、
  `1009 -> REWARD`、奖励页 `5 -> SHOW_SELECT_QUEST_REWARD_WINDOW1`。
- 迁移前 handler `_25094An_Offering_Of_Friendship`：804929 的 `CHECK_COLLECTED_ITEMS` 调
  `checkQuestItems(env, 0, 1, true, 10000, 10001)`——作者意图是 0 -> 1 且置 REWARD，但旧 helper 的
  reward 分支只 `setStatus(REWARD)`、不写 nextStep（QE-054），所以旧存档实际落盘 var0=0。
- 客户端页链：`select1`(Bakring 收骨骸) -> `CHECK_USER_HAS_QUEST_ITEM` -> `check_user_item_ok`；
  `select2`(「好了，完成啦。你把这个交给 Daruku 吧」) -> `SET_SUCCEED`；
  `select_success`(Daruku) -> `SELECT_QUEST_REWARD`。
- 旧定义把 0 -> 1 的那一步直接塌陷成 `started -> reward` 且 reward 投影抄成 0：领奖态任务书仍显示行 0
  （叫玩家去交骨骸），行 1/行 2 永远拿不到状态。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch33_shared_visible_slot_row.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch33_shared_visible_slot_row.py --apply
"""

from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

DOCUMENTS: dict[int, str] = {}

DOCUMENTS[25094] = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="25094" version="1">
  <metadata name="An Offering of Friendship" display-name-id="1800865" min-level="64" max-level="2147483647" category="IMPORTANT">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <items>
      <item id="182215736" count="10"/>
    </items>
    <rewards>
      <reward kind="GOLD" id="0" amount="146220"/>
      <reward kind="EXP" id="0" amount="9981788"/>
      <reward kind="ITEM" id="186000231" amount="4"/>
      <reward kind="ITEM" id="186000237" amount="30"/>
      <reward kind="SELECTABLE_ITEM" id="166050221" amount="3"/>
      <reward kind="SELECTABLE_ITEM" id="166050222" amount="3"/>
    </rewards>
    <drops>
      <drop npc-id="702768" item-id="182215736" chance="100" each-member="true" collecting-step="0"/>
    </drops>
  </metadata>
  <progress>
    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>
  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="s1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <!-- QE-051 共享槽位收口（批次 33）：客户端 quest_summary 3 行的可见槽位是 %0/%3/%3，即状态 0 = 行 0、
         状态 1 = 行 1 与行 2 同时可见；legacy 的 checkQuestItems(0, 1, true, ...) 作者意图同样是 0 -> 1 后置
         REWARD（旧 helper 的 reward 分支不写 nextStep，落盘仍是 0，QE-054）。因此 reward 投影 = 1，
         并为旧存档补 REWARD/var0=0 -> 1 的 enter-world 自愈边。
         Shared-slot repair (batch 33): the client journal only owns two visibility slots (%0 and %3), so the
         ladder is started(0) -> s1(1), and the reward projection stays on state 1 like the legacy intent. -->
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <!-- 接取：Bakring 804929 委托收集 10 个灼热的龙族骨骸（182215736）。 / Accept: Bakring asks for ten bones. -->
    <dialog type="NPC_START" npc-id="804929" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <!-- 行 0：从龙骨物件 702768 采集。 / Row 0: harvest the dragon-bone object. -->
    <transition source="started" target="started">
      <event>
        <can-act template-id="702768" action-type="ACTION_ITEM_USE"/>
      </event>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="702768" action="USE_OBJECT"/>
      </event>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804929" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <!-- 行 1：把骨骸交给 Bakring（select1 -> select1 的“拿出灼热的龙族骨骸”按钮），任务书切到状态 1
         （行 1 与行 2 同时可见）。 / Row 1: hand the bones over; the journal moves to slot %3. -->
    <transition source="started" target="s1" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804929" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="182215736" count="10"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
        <remove-item item-id="182215736" count="10"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>
      </after-commit>
    </transition>
    <transition source="started" target="started" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804929" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>
      </after-commit>
    </transition>
    <!-- 行 1 -> 行 2：Bakring 的 select2（“好了，完成啦。你把这个交给 Daruku 吧”）按 SET_SUCCEED 收尾。 -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804929" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <!-- 客户端 check_user_item_ok 页只有一个“结束对话”按钮，必须在本节点保留 FINISH_DIALOG 路由，
         否则 QuestClientContractGateTest 报 BUTTON_WITHOUT_ROUTE。 -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804929" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804929" action="SET_SUCCEED"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 防呆：不按 SET_SUCCEED、直接去找 Daruku 也能进入领奖态（行 2 = 交给 Daruku）。 -->
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804740" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <!-- 领奖：Daruku 804740 的 select_success（合同 31 -> DEFAULT_SUCCESS）与奖励窗口。 -->
    <dialog type="NPC_REPORT" npc-id="804740" source="reward" target="reward" page="DEFAULT_SUCCESS"/>
    <npc-complete npc-id="804740" source="reward" target="complete" fixed-reward-indices="0 1 2 3" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="4"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="5"/>
      <preview action="USE_OBJECT"/>
      <fallback actions="SELECTED_QUEST_REWARD3..SELECTED_QUEST_NOREWARD"/>
    </npc-complete>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804929" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
  </transitions>
</quest-definition>
"""


def main(argv: list[str]) -> int:
    apply = "--apply" in argv
    check = "--check" in argv or not apply
    status = 0
    for quest_id, document in DOCUMENTS.items():
        path = QUESTS / f"{quest_id}.xml"
        current = path.read_text(encoding="utf-8")
        if check:
            if current == document:
                print(f"BATCH33_OK {quest_id} already-applied")
            else:
                print(f"BATCH33_PENDING {quest_id} differs from target document")
                status = 1
            continue
        if current == document:
            print(f"BATCH33_OK {quest_id} already-applied")
            continue
        path.write_text(document, encoding="utf-8")
        print(f"BATCH33_APPLIED {quest_id}")
    return status


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
