#!/usr/bin/env python3
"""批次 32：卡多尔迎新族（13800 / 23800）两阶段行阶梯重建。

族判据（2026-09-22）：
- 客户端 quest_q13800/quest_q23800.html 的 quest_summary 恰好 3 行：
  行 0“带上 [quest_13800a/quest_23800a] 去找 <传送点>”（804782/804753）、行 1“移动到卡多尔，和
  Alphion/Pintz 对话”（802431/802433）、行 2“再次和 Alphion/Pintz 对话”（802431/802433）。
- 客户端对话页链给出两个推进动作：select2 -> SELECT2_1(1353) -> SETPRO1(10000) 与
  select3 -> SELECT3_1(1694) -> SELECT3_1_1(1695) -> SETPRO2(10001)，最后 select5 -> SELECT_QUEST_REWARD(1009)；
  页文本分别对应“拿出书信 / 任务说明 / 收尾说明”三段。
- retail `zz_retail_simple_quests.xml`：`<data_driven_quest start_type="TALK" start_ids="804699"
  end_npc_ids="802431"><step type="TALK" ids="804782"/></data_driven_quest>`（23800 同形），
  领奖合同是 REWARD 态 `31 -> DEFAULT_SUCCESS -> 1009`。
- 迁移前 handler `_13800New_Lands_To_Behold` / `_23800A_Full_New_World`：传送点 NPC 的 STEP_TO_1 把
  var0 推到 1，领奖 NPC 的 SELECT_REWARD 直接 `setStatus(REWARD)`（落盘 1，跳过 select3 链）。
- 旧定义把三个 NPC 全部塌陷成“接取 + 一步领奖”，reward 投影停在 0，行 1/行 2 永远拿不到状态
  （审计 MISSING_TAIL_ROWS + ROW_WITHOUT_STATE）。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch32_kaldor_row_ladder.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch32_kaldor_row_ladder.py --apply
"""

from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

DOCUMENTS: dict[int, str] = {}

DOCUMENTS[13800] = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="13800" version="1">
  <metadata name="New Lands to Behold" display-name-id="1111674" min-level="65" max-level="2147483647" category="IMPORTANT" cannot-share="true">
    <races>
      <race id="ELYOS"/>
    </races>
    <work-items>
      <item id="182215482" count="1"/>
    </work-items>
    <rewards>
      <reward kind="GOLD" id="0" amount="150660"/>
      <reward kind="EXP" id="0" amount="3446553"/>
      <reward kind="SELECTABLE_ITEM" id="166050223" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="166050224" amount="1"/>
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
    <!-- QE-051 三行阶梯（批次 32）：客户端 quest_q13800.html 的 quest_summary 共 3 行——行 0“带上
         quest_13800a 去找 LF5_OP1_ZoneTeleport_L(804782)”、行 1“移动到卡多尔，和
         LDF5_Fortress_Alphion_E(802431) 对话”、行 2“再次和 Alphion 对话”。客户端页链给出两个推进动作
         （select2_1 -> SETPRO1=10000、select3_1_1 -> SETPRO2=10001）与领奖页 select5(1009)；
         retail 的 data_driven_quest 只登记了 TALK 804782 一个 step，领奖合同是 REWARD 态 31 ->
         DEFAULT_SUCCESS -> 1009；迁移前 handler 在 804782 的 STEP_TO_1 把 var0 推到 1、在 802431 的
         SELECT_REWARD 直接置 REWARD（落盘 1，跳过了 select3 链的任务说明段）。旧定义把三个 NPC 全塌陷成
         “接取 + 一步领奖”，行 1/行 2 永远拿不到状态；本批按客户端页链重建 started(0)/s1(1)/reward(2)，
         领奖 owner 收敛到 802431，并补旧存档自愈边。
         Row-ladder repair (batch 32): the client journal owns three rows and two advance actions
         (SETPRO1 at the zone-teleport NPC, SETPRO2 in the Alphion briefing chain). The old definition
         collapsed every NPC into accept-and-claim, so rows 1 and 2 never received a state. -->
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
    <!-- 接取：Atmos 804699 交代卡多尔派遣并交付书信 182215482。 / Accept: Atmos hands over the letter. -->
    <dialog type="NPC_START" npc-id="804699" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1">
      <accept-actions>
        <give-item item-id="182215482" count="1"/>
      </accept-actions>
    </dialog>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804699" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <!-- 行 0：向希哥尼亚传送点 804782 出示书信（select2 -> select2_1 -> SETPRO1，var0 0 -> 1）。
         Row 0: show the letter to the Shyggonia zone-teleport NPC. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804782" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804782" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804782" action="SETPRO1"/>
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
    <!-- 行 1：卡多尔要塞 Alphion 802431 的任务说明（select3 -> select3_1 -> select3_1_1 -> SETPRO2，
         var0 1 -> 2 并置 REWARD）。 / Row 1: the Alphion briefing chain advances into REWARD. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802431" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802431" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802431" action="SELECT3_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802431" action="SETPRO2"/>
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
    <!-- 行 2：再次和 Alphion 对话领奖；开奖励窗口时移除书信。 / Row 2: claim from Alphion. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802431" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802431" action="SELECT_QUEST_REWARD"/>
      </event>
      <actions>
        <remove-item item-id="182215482" count="1"/>
      </actions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="802431" source="reward" target="complete" fixed-reward-indices="0 1" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="2"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="3"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

DOCUMENTS[23800] = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="23800" version="1">
  <metadata name="A Full New World" display-name-id="1111687" min-level="65" max-level="2147483647" category="IMPORTANT" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <work-items>
      <item id="182215490" count="1"/>
    </work-items>
    <rewards>
      <reward kind="GOLD" id="0" amount="150660"/>
      <reward kind="EXP" id="0" amount="3446553"/>
      <reward kind="SELECTABLE_ITEM" id="166050223" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="166050224" amount="1"/>
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
    <!-- QE-051 三行阶梯（批次 32）：23800 是 13800 的魔族镜像——行 0“带上 quest_23800a 去找
         DF5_OP1_ZoneTeleport_D(804753)”、行 1“前往卡多尔，和 LDF5_Fortress_Pintz_E(802433) 对话”、
         行 2“再次和 Pintz 对话”；页链同为 SETPRO1(10000) / SETPRO2(10001) / SELECT_QUEST_REWARD(1009)。
         retail `data_driven_quest` 同样只登记 TALK 804753 一个 step，旧定义把三个 NPC 全塌陷成
         “接取 + 一步领奖”，reward 投影停在 0；本批按客户端页链重建 0/1/2 阶梯并补旧存档自愈边。
         Row-ladder repair (batch 32) for the Asmodian mirror; same contract fields as 13800. -->
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
    <!-- 接取：Haldor 804719 交代卡多尔派遣并交付书信 182215490。 / Accept: Haldor hands over the letter. -->
    <dialog type="NPC_START" npc-id="804719" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1">
      <accept-actions>
        <give-item item-id="182215490" count="1"/>
      </accept-actions>
    </dialog>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804719" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <!-- 行 0：向厄夏勒传送点 804753 出示书信（select2 -> select2_1 -> SETPRO1，var0 0 -> 1）。
         Row 0: show the letter to the Ereshkigal zone-teleport NPC. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804753" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804753" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804753" action="SETPRO1"/>
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
    <!-- 行 1：卡多尔要塞 Pintz 802433 的任务说明（select3 -> select3_1 -> select3_1_1 -> SETPRO2，
         var0 1 -> 2 并置 REWARD）。 / Row 1: the Pintz briefing chain advances into REWARD. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802433" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802433" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802433" action="SELECT3_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802433" action="SETPRO2"/>
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
    <!-- 行 2：再次和 Pintz 对话领奖；开奖励窗口时移除书信。 / Row 2: claim from Pintz. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802433" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="802433" action="SELECT_QUEST_REWARD"/>
      </event>
      <actions>
        <remove-item item-id="182215490" count="1"/>
      </actions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="802433" source="reward" target="complete" fixed-reward-indices="0 1" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="2"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="3"/>
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
            print(f"BATCH32_OK {quest_id} already-applied")
            continue
        if check:
            print(f"BATCH32_PENDING {quest_id} differs from target document")
            continue
        path.write_text(document, encoding="utf-8")
        print(f"BATCH32_APPLIED {quest_id}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
