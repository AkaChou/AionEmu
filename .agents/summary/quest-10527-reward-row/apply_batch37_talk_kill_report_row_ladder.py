#!/usr/bin/env python3
"""批次 37：两条“交谈 → 击杀 → 报告”任务族的整行阶梯重建（QE-051）。

族判据（2026-09-22，客户端解包 + legacy-quest-dialog-contracts.csv）：
- 26905/26906/26908（Asmodian monster_hunt，三行）：客户端 quest_summary 三行——行 0「和
  Gangleri/Tyr/Svafnir 对话」、行 1「消灭 Dark Raider」、行 2「向同一 NPC 报告」；页链是
  select2 的 SETPRO1（0->1）→ 击杀（1->2）→ select5 的 SELECT_QUEST_REWARD 打开奖励窗。
  legacy 合同 start_npc_ids=204301/204301/204702、end_npc_ids=204372/204369/204817：接取 NPC 只管接取，
  进度/报告/领奖 owner 收敛到任务书点名的 end NPC；迁移把 end NPC 的行 0 对话写成 started->reward、
  reward 投影 0，导致行 1/2 没有状态。
- 3711/4711（Dredgion 舰长，四行）：行 0「和 Mias/Henir 对话」（SETPRO1）、行 1「搜集情报」
  （730196 术古 select2→select2_1→SETPRO2）、行 2「除掉 DrakanBoss(214823)」、行 3「向 Taranis/Votan 报告」；
  legacy 合同 start_npc_ids=end_npc_ids=278501/278001，report_source_status=REWARD、
  report_open_action=QUEST_SELECT→DEFAULT_SUCCESS(10002)、report_action=SELECT_QUEST_REWARD→奖励窗。

两族统一：每个任务书行拥有一个 START/REWARD 投影；reward 投影等于末行；补无 source 的 ENTER_WORLD
自愈边覆盖旧存档 packed 行；删除 started -> reward 直跳与跳过中间行的 SETPRO2 残留。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch37_talk_kill_report_row_ladder.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch37_talk_kill_report_row_ladder.py --apply
"""

from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

DOCUMENTS: dict[int, str] = {}

DOCUMENTS[26905] = '''<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="26905" version="1">
  <metadata name="Occupiers All Over the Place" display-name-id="1140699" min-level="28" max-level="2147483647" category="QUEST">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="85320"/>
      <reward kind="EXP" id="0" amount="186175"/>
      <reward kind="SELECTABLE_ITEM" id="120000864" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="120000865" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="123000899" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="123000900" amount="1"/>
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
    <node label="t1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="k2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <!-- QE-051 自愈边：旧存档停在 REWARD 但 packed 行仍是 0/1，进入世界时补到领奖行 2。 -->
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
    <dialog type="NPC_START" npc-id="204301" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>



    <!-- 击杀任一首领进入领奖。Killing either chief enters reward. -->




    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204372" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <!-- 击杀任一首领进入领奖。Killing either chief enters reward. -->
    <transition source="t1" target="k2" priority="1">
      <event>
        <kill-npc npc-ids="231555 231556"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition source="k2" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204372" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <!-- 行 2：向 204372 报告（客户端 select5 的 SELECT_QUEST_REWARD 打开奖励窗；QE-051 批次 37）。 -->
    <transition source="k2" target="k2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204372" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>
      </after-commit>
    </transition>
    <transition source="t1" target="t1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204372" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="204372" source="reward" target="complete" fixed-reward-indices="0 1" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="2"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="3"/>
      <choice action="SELECTED_QUEST_REWARD3" reward-index="4"/>
      <choice action="SELECTED_QUEST_REWARD4" reward-index="5"/>
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  <transition source="started" target="t1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204372" action="SETPRO1"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
</transitions>
</quest-definition>
'''

DOCUMENTS[26906] = '''<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="26906" version="1">
  <metadata name="Blood-scent of Violent Death" display-name-id="1140700" min-level="36" max-level="2147483647" category="QUEST">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="135000"/>
      <reward kind="EXP" id="0" amount="957871"/>
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
    <node label="t1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="k2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <!-- QE-051 自愈边：旧存档停在 REWARD 但 packed 行仍是 0/1，进入世界时补到领奖行 2。 -->
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
    <dialog type="NPC_START" npc-id="204301" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>



    <!-- 击杀任一首领进入领奖。Killing either chief enters reward. -->




    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204369" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <!-- 击杀任一首领进入领奖。Killing either chief enters reward. -->
    <transition source="t1" target="k2" priority="1">
      <event>
        <kill-npc npc-ids="231558 231559"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition source="k2" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204369" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <!-- 行 2：向 204369 报告（客户端 select5 的 SELECT_QUEST_REWARD 打开奖励窗；QE-051 批次 37）。 -->
    <transition source="k2" target="k2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204369" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>
      </after-commit>
    </transition>
    <transition source="t1" target="t1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204369" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="204369" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  <transition source="started" target="t1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204369" action="SETPRO1"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
</transitions>
</quest-definition>
'''

DOCUMENTS[26908] = '''<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="26908" version="1">
  <metadata name="Blast it, Neritra, I'm a Researcher" display-name-id="1140702" min-level="39" max-level="2147483647" category="QUEST">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="156600"/>
      <reward kind="EXP" id="0" amount="1579054"/>
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
    <node label="t1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="k2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <!-- QE-051 自愈边：旧存档停在 REWARD 但 packed 行仍是 0/1，进入世界时补到领奖行 2。 -->
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
    <dialog type="NPC_START" npc-id="204702" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>



    <!-- 击杀任一首领进入领奖。Killing either chief enters reward. -->




    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204817" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <!-- 击杀任一首领进入领奖。Killing either chief enters reward. -->
    <transition source="t1" target="k2" priority="1">
      <event>
        <kill-npc npc-ids="231570 231571"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition source="k2" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204817" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <!-- 行 2：向 204817 报告（客户端 select5 的 SELECT_QUEST_REWARD 打开奖励窗；QE-051 批次 37）。 -->
    <transition source="k2" target="k2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204817" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>
      </after-commit>
    </transition>
    <transition source="t1" target="t1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204817" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="204817" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  <transition source="started" target="t1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204817" action="SETPRO1"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
</transitions>
</quest-definition>
'''

DOCUMENTS[3711] = '''<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="3711" version="1">
  <metadata name="[Group] To Kill a Captain" display-name-id="1104710" min-level="46" max-level="50" category="QUEST">
    <races>
      <race id="ELYOS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="118580"/>
      <reward kind="EXP" id="0" amount="3884596"/>
      <reward kind="AP" id="0" amount="1500"/>
      <reward kind="ITEM" id="161000004" amount="5"/>
      <reward kind="ITEM" id="186000005" amount="1"/>
      <reward kind="ITEM" id="188050830" amount="1"/>
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
    <node label="s2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="3"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <!-- QE-051 自愈边：旧存档停在 REWARD 但 packed 行仍是 0/1/2，进入世界时补到领奖行 3。 -->
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
        <variable-is field="var0" value="1"/>
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
    <dialog type="NPC_START" npc-id="278501" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="279045" action="QUEST_SELECT"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="279045" action="SETPRO1"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="279045" action="SELECT1_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1_1"/>
      </after-commit>
    </transition>
    <!-- retail 对话流:730196 页面链 select2(1352)→select2_1(1353)→SETPRO2 按钮,推进 var0=1→2。 -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730196" action="QUEST_SELECT"/>
      </event>
      <conditions>
        <variable-is field="var0" value="1"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730196" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730196" action="SETPRO2"/>
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
    <transition source="s2" target="reward">
      <event>
        <kill-npc npc-id="214823"/>
      </event>
      <conditions>
        <variable-is field="var0" value="2"/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278501" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="278501" source="reward" target="complete" fixed-reward-indices="0 1 2 3 4 5" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>

    <!-- 契约外 NPC 仅提供无按钮的进行中反馈,避免页面按钮断链。 -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203844" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>



    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278501" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="279045" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203844" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730196" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203844" action="SELECT1_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730196" action="SELECT1_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1_1"/>
      </after-commit>
    </transition>

  </transitions>
</quest-definition>
'''

DOCUMENTS[4711] = '''<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="4711" version="1">
  <metadata name="[Group] The Dredgion Captain" display-name-id="1120004" min-level="46" max-level="50" category="QUEST">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="118580"/>
      <reward kind="EXP" id="0" amount="3732496"/>
      <reward kind="AP" id="0" amount="1500"/>
      <reward kind="ITEM" id="161000004" amount="5"/>
      <reward kind="ITEM" id="186000010" amount="1"/>
      <reward kind="ITEM" id="188050920" amount="1"/>
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
    <node label="s2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="3"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <!-- QE-051 自愈边：旧存档停在 REWARD 但 packed 行仍是 0/1/2，进入世界时补到领奖行 3。 -->
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
        <variable-is field="var0" value="1"/>
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
    <dialog type="NPC_START" npc-id="278001" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <!-- retail 步骤链（zz_retail_simple_quests.xml）：TALK 279042 → ACTION 730196 → HUNT 214823×1；var0 为步骤状态编码，与 3711 同型。 -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="279042" action="QUEST_SELECT"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="279042" action="SELECT1_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="279042" action="SETPRO1"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- retail 对话流:730196 页面链 select2(1352)→select2_1(1353)→SETPRO2 按钮,推进 var0=1→2。 -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730196" action="QUEST_SELECT"/>
      </event>
      <conditions>
        <variable-is field="var0" value="1"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730196" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730196" action="SETPRO2"/>
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
    <transition source="s2" target="reward">
      <event>
        <kill-npc npc-ids="214823"/>
      </event>
      <conditions>
        <variable-is field="var0" value="2"/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278001" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="278001" source="reward" target="complete" fixed-reward-indices="0 1 2 3 4 5" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>




    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278001" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730196" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730196" action="SELECT1_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="279042" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="279042" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="279042" action="SELECT1_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1_1"/>
      </after-commit>
    </transition>

  </transitions>
</quest-definition>
'''


def main(argv: list[str]) -> int:
    apply = "--apply" in argv
    check = "--check" in argv or not apply
    status = 0
    for quest_id, document in DOCUMENTS.items():
        path = QUESTS / f"{quest_id}.xml"
        current = path.read_text(encoding="utf-8")
        if check:
            if current == document:
                print(f"BATCH37_OK {quest_id} already-applied")
            else:
                print(f"BATCH37_PENDING {quest_id} differs from target document")
                status = 1
            continue
        if current == document:
            print(f"BATCH37_OK {quest_id} already-applied")
            continue
        path.write_text(document, encoding="utf-8")
        print(f"BATCH37_APPLIED {quest_id}")
    return status


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))