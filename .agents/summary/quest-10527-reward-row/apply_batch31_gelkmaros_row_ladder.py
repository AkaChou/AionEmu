#!/usr/bin/env python3
"""批次 31：Gelkmaros 三行交接塌陷族（21217 / 21244 / 21249）行阶梯重建。

族判据（2026-09-22）：
- 客户端 quest_summary 恰好 3 行，行 0/行 1 是“交给/转达/对话”，行 2 是“交给领奖 NPC / 向领奖 NPC 报告”；
- legacy handler 有 0 -> 1 -> 2 的 step 事件链（defaultCloseDialog / setQuestVar + defaultCloseDialog(...,true,false)），
  领奖态落盘 step 2（QE-054：reward 分支只 setStatus，不写 nextStep）；
- 迁移后的旧定义把全部 NPC 塌陷成“接取 + 一步领奖”（started -> reward），行 1/行 2 永远拿不到 START/REWARD 状态
  （审计 MISSING_TAIL_ROWS / ROW_WITHOUT_STATE）。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch31_gelkmaros_row_ladder.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch31_gelkmaros_row_ladder.py --apply
"""

from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

DOCUMENTS: dict[int, str] = {}

DOCUMENTS[21217] = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="21217" version="1">
  <metadata name="New Research Plan" display-name-id="1127120" min-level="54" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <work-items>
      <item id="182207890" count="1"/>
    </work-items>
    <rewards>
      <reward kind="GOLD" id="0" amount="26320"/>
      <reward kind="EXP" id="0" amount="6517414"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="21216"/>
    </start-conditions>
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
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <!-- QE-051 三行阶梯（批次 31）：客户端 quest_q21217.html 的 quest_summary 共 3 行——行 0
         “把伊塔尔的报告书交给 Wolfgang”（799239）、行 1 “...Fjoersvith”（798713）、行 2 “...Barretta”（799226）。
         legacy `_21217NewResearchPlan`：799316 接取并给报告书 182207890，799239 的 defaultCloseDialog(0, 1) 把
         step 推到 1，798713 的 setQuestVar(2) + defaultCloseDialog(2, 2, true, false) 把 step 推到 2 并置 REWARD，
         799226 是唯一领奖 owner。旧定义把四个 NPC 全塌陷成“接取 + 一步领奖”，行 1/行 2 永远拿不到状态；本批按
         legacy 事件链重建阶梯，并补旧存档 REWARD/var0=0 -> 2 的 enter-world 自愈边。
         Row-ladder repair (batch 31): the client journal owns three rows (Wolfgang -> Fjoersvith -> Barretta).
         The legacy handler advances step 0 -> 1 at 799239 and persists step 2 with REWARD at 798713; the old
         definition collapsed every NPC into accept-and-claim, so rows 1 and 2 never received a state. -->
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
    <!-- 接取：Itar 799316 交付伊塔尔的报告书。 / Accept: Itar hands over the report item. -->
    <dialog type="NPC_START" npc-id="799316" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1">
      <accept-actions>
        <give-item item-id="182207890" count="1"/>
      </accept-actions>
    </dialog>
    <!-- 行 0：向 Wolfgang 799239 交报告书；select2 -> select2_1 页链后 SETPRO1 推进到 s1（var0=1）。
         Row 0: hand the report to Wolfgang; the select2 -> select2_1 chain ends with SETPRO1. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799239" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799239" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799239" action="SETPRO1"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：向 Fjoersvith 798713 交报告书；select3 -> select3_1 页链后 SETPRO2 置 REWARD（落盘 var0=2）。
         Row 1: hand the report to Fjoersvith; SETPRO2 flips REWARD with persisted step 2. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798713" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798713" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798713" action="SETPRO2"/>
      </event>
      <conditions>
        <variable-is field="var0" value="1"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 2：向 Barretta 799226 领奖；工作物品在开奖励窗口时移除。
         Row 2: claim from Barretta; the work item is removed when the reward window opens. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799226" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799226" action="SELECT_QUEST_REWARD"/>
      </event>
      <actions>
        <remove-item item-id="182207890" count="1"/>
      </actions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="799226" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG"/>
  </transitions>
</quest-definition>
"""

DOCUMENTS[21244] = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="21244" version="1">
  <metadata name="Search For The Biolab" display-name-id="1127164" min-level="54" max-level="2147483647" category="QUEST">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <work-items>
      <item id="182207924" count="1"/>
    </work-items>
    <rewards>
      <reward kind="GOLD" id="0" amount="8400"/>
      <reward kind="EXP" id="0" amount="4020773"/>
    </rewards>
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
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <!-- QE-051 三行阶梯（批次 31）：客户端 quest_q21244.html 的 quest_summary 共 3 行——行 0
         “向 Batalrion 转达塔纳尔的话”（799318）、行 1 “向 Helen 转达塔纳尔的话”（799320）、行 2
         “把用皮绳捆好的卷轴交给 Tanar”（799317）。legacy `_21244SearchForTheBiolab`：799317 接取，
         799318 的 defaultCloseDialog(0, 1) 把 step 推到 1，799320 的 setQuestVar(2) +
         defaultCloseDialog(2, 2, true, false) 把 step 推到 2 并置 REWARD，同时 giveQuestItem(182207924)；
         799317 在 REWARD 态 removeQuestItem 并结束对话。旧定义把三个 NPC 塌陷成“接取 + 一步领奖”，
         行 1/行 2 永远拿不到状态；本批按 legacy 事件链重建阶梯并补旧存档自愈边。
         Row-ladder repair (batch 31): the client journal owns three rows (Batalrion -> Helen -> Tanar) and the
         legacy handler persists step 2 with REWARD at 799320 while handing over the scroll work item. -->
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
    <!-- 接取：Tanar 799317 简报“残骸之海发现”。 / Accept: Tanar briefs the discovery. -->
    <dialog type="NPC_START" npc-id="799317" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1"/>
    <!-- 行 0：向 Batalrion 799318 转达塔纳尔的话；select2 -> select2_1 页链后 SETPRO1 推进到 s1。
         Row 0: relay to Batalrion; the select2 -> select2_1 chain ends with SETPRO1. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799318" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799318" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799318" action="SETPRO1"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：向 Helen 799320 转达；SETPRO2 置 REWARD（落盘 var0=2）并接过卷轴 182207924。
         Row 1: relay to Helen; SETPRO2 flips REWARD and hands over the scroll. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799320" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799320" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799320" action="SETPRO2"/>
      </event>
      <conditions>
        <variable-is field="var0" value="1"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
        <give-item item-id="182207924" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 2：把卷轴交给 Tanar 799317 领奖；开奖励窗口时移除工作物品。
         Row 2: hand the scroll back to Tanar; the work item is removed when the reward window opens. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799317" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799317" action="SELECT_QUEST_REWARD"/>
      </event>
      <actions>
        <remove-item item-id="182207924" count="1"/>
      </actions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="799317" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG"/>
  </transitions>
</quest-definition>
"""

DOCUMENTS[21249] = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="21249" version="1">
  <metadata name="The Invincible Starket" display-name-id="1127169" min-level="54" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="EXP" id="0" amount="6517414"/>
      <reward kind="SELECTABLE_ITEM" id="164000066" amount="16"/>
      <reward kind="SELECTABLE_ITEM" id="164000121" amount="16"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="21248"/>
    </start-conditions>
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
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <!-- QE-051 三行阶梯（批次 31）：客户端 quest_q21249.html 的 quest_summary 共 3 行——行 0
         “和 Tonistar 对话”（799416）、行 1 “和变身成德拉坎的 Tonistar 对话”（799529）、行 2
         “向 Javis 转达托尼斯塔的话”（799417）。legacy `_21249TheInvincibleStarket`：799416 的
         defaultCloseDialog(0, 1) 把 step 推到 1，799529 的 setQuestVar(2) + defaultCloseDialog(2, 2, true, false)
         把 step 推到 2 并置 REWARD，799417 是领奖 owner。旧定义把三个 NPC 塌陷成“一步 SET_SUCCEED 置 REWARD”，
         行 1/行 2 永远拿不到状态；本批按 legacy 事件链重建阶梯并补旧存档自愈边。
         Row-ladder repair (batch 31): the client journal owns three rows (Tonistar -> Tonistar_Drakan -> Javis);
         the legacy handler advances step 0 -> 1 at 799416 and persists step 2 with REWARD at 799529.
         799416 的换装召唤（删除本体 + 生成 799529）不在本批范围，仍按现状由世界中的 799529 承接行 1。 -->
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
    <!-- 接取：Tonistar 799416 简报越狱。 / Accept: Tonistar briefs the breakout. -->
    <dialog type="NPC_START" npc-id="799416" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <!-- 行 0：和 Tonistar 799416 对话；select1 的 SETPRO1 推进到 s1（var0=1）。
         Row 0: talk to Tonistar; SETPRO1 advances to s1. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799416" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799416" action="SETPRO1"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：和变身成德拉坎的 Tonistar 799529 对话；select2 的 SET_SUCCEED 置 REWARD（落盘 var0=2）。
         Row 1: talk to Tonistar_Drakan; SET_SUCCEED flips REWARD with persisted step 2. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799529" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799529" action="SET_SUCCEED"/>
      </event>
      <conditions>
        <variable-is field="var0" value="1"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 2：向 Javis 799417 转达托尼斯塔的话并领奖。 / Row 2: relay to Javis and claim the reward. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799417" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799417" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="799417" source="reward" target="complete" fixed-reward-indices="0" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="1"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="2"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""


def main(argv: list[str]) -> int:
    check = "--check" in argv
    apply = "--apply" in argv
    if not check and not apply:
        print(__doc__)
        return 2
    for quest_id, document in sorted(DOCUMENTS.items()):
        path = QUESTS / f"{quest_id}.xml"
        current = path.read_text(encoding="utf-8")
        if current == document:
            print(f"BATCH31_OK {quest_id} already-applied")
            continue
        if check:
            print(f"BATCH31_PENDING {quest_id} differs from target document")
            continue
        path.write_text(document, encoding="utf-8")
        print(f"BATCH31_APPLIED {quest_id}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
