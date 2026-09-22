#!/usr/bin/env python3
"""批次 44：发光体玻璃瓶族 11118（Making Setzkiki Laugh）的五行状态收口（QE-051）。

族判据（2026-09-22，客户端 quest_summary + HTML 页链 + 客户端 NPC 名表 + 迁移前 Java handler）：
- 11118 客户端任务书五行、槽位 %0/%3/%6/%9/%12：
  行 0「向 Cinisca 征询意见」、行 1「把昏掉的 Foam Wisp 装进瓶子，交给 Cinisca[collectitem]」、
  行 2「带着装有发光体的玻璃瓶，在 Shulack_LF4_2 身边打开」、行 3「和 Shulack_LF4_2 对话」、
  行 4「和 Shulack_LF4_1 对话」。
- 客户端页链（Dialogs/10000_19999/QUEST_Q11118.html）：
  `select_none`（ASK_QUEST_ACCEPT）-> `ask_quest_accept` -> `quest_accept_1`（塞伊金 798985 委托）；
  Cinisca 798963：`select1`（SETPRO1）-> `select2`（CHECK_USER_HAS_QUEST_ITEM）-> 成功
  `check_user_item_ok` / 失败 `check_user_item_fail`；Shulack_LF4_2 798986：`select4`
  （SET_SUCCEED，“发光体实在太美了…真让人怀念。结束对话。”）；Shulack_LF4_1 798985：
  `select_success`（SELECT_QUEST_REWARD，“讲述经过”）-> `select_quest_reward1` 领奖页。
- 物品合同：交 20 个 182206794（发光体）换工作物品 182206795（装光玻璃瓶）。
- **迁移前 Java handler 的 owner 笔误**：`_11118MakingSetzkikiLaugh` 声明
  `npc_ids = {798985, 798963, 798986}`（意图用妹妹 Shulack_LF4_2 = 798986），但正文分支写成
  `targetId == 798984`（Shugo_LF4_5，术古商人），且 798984 从未注册 talk 监听——按该脚本
  798984 的分支永远不会触发。迁移把 798984 固化成 select4 的 owner，同时把 reward 投影塌成 var0=0，
  行 3/行 4 没有状态。
- 本批按“客户端任务书逐行点名 + npc_ids 声明”修正为 798986，并把阶梯展开成
  started(0) -> s1(1) -> s2(2) -> s3(3) -> reward(4)：Cinisca 的 SETPRO1/CHECK 推进前两行，
  妹妹的对话（打开瓶子）推进到行 3，SET_SUCCEED 推进到行 4，最后回哥哥 798985 领奖。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch44_foam_wisp_five_row_ladder.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch44_foam_wisp_five_row_ladder.py --apply
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_ID = 11118
SEIJIN = 798985
CINISCA = 798963
SETZKIKI = 798986
WISP_ITEM = 182206794
WISP_COUNT = 20
BOTTLE_ITEM = 182206795


def tidy(text: str) -> str:
	return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def metadata_block(text: str, quest_id: int) -> str:
	match = re.search(r"  <metadata .*?</metadata>\n", text, re.S)
	if match is None:
		raise SystemExit(f"BATCH44_ERROR {quest_id}: cannot locate metadata")
	return match.group(0)


def body() -> str:
	return f"""  <!-- QE-051 行阶梯收口（批次 44）：发光体玻璃瓶五行族。
       客户端任务书五行、槽位 %0/%3/%6/%9/%12：行 0 问 Cinisca、行 1 交 20 个发光体换玻璃瓶、
       行 2 在妹妹 Shulack_LF4_2 {SETZKIKI} 身边打开瓶子、行 3 和妹妹对话、
       行 4 回哥哥 Shulack_LF4_1 {SEIJIN} 报告领奖。
       Row ladder closure (batch 44): five journal rows, one state each. -->
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
    <node label="s3" status="START">
      <var name="var0" value="3"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="4"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <!-- 旧存档自愈：迁移前的 REWARD 可能带 var0=0（迁移后 XML 落盘）或 var0=3（迁移前 Java
         handler 的 setQuestVarById(0, var+1)），两种都提到行 4。
         / Heal both pre-migration REWARD saves (var0=0 from the typed XML, var0=3 from the Java handler). -->
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="4"/>
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
        <variable-is field="var0" value="3"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="4"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <!-- 接取：哥哥 Shulack_LF4_1 {SEIJIN}。 / Accept from the brother. -->
    <dialog type="NPC_START" npc-id="{SEIJIN}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <!-- 行 0：向补给兵 Cinisca {CINISCA} 征询意见（select1 -> SETPRO1）。 -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{CINISCA}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{CINISCA}" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{CINISCA}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：交 20 个发光体换玻璃瓶（select2 的 CHECK 链）。 / Row 1: hand the wisps over. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{CINISCA}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s2" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{CINISCA}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="{WISP_ITEM}" count="{WISP_COUNT}" expected="true"/>
      </conditions>
      <actions>
        <remove-item item-id="{WISP_ITEM}" count="{WISP_COUNT}"/>
        <give-item item-id="{BOTTLE_ITEM}" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{CINISCA}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="{WISP_ITEM}" count="{WISP_COUNT}" expected="false"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{CINISCA}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- CHECK 成功时状态已经切到 s2，而客户端 check_user_item_ok 页的唯一按钮是“结束对话”，
         s2 必须保留 FINISH_DIALOG 路由，否则 QuestClientContractGateTest 报 BUTTON_WITHOUT_ROUTE。 -->
    <transition source="s2" target="s2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{CINISCA}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 2 -> 行 3：在妹妹 Shulack_LF4_2 {SETZKIKI} 身边打开瓶子（select4 页）。
         客户端 select4 页的按钮是 SET_SUCCEED，两条入口（对话/使用瓶子）都进 select4。
         Row 2 to row 3: open the bottle by the sister. -->
    <transition source="s2" target="s3">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{SETZKIKI}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT4"/>
      </after-commit>
    </transition>
    <transition source="s2" target="s3">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{SETZKIKI}" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT4"/>
      </after-commit>
    </transition>
    <!-- 行 3 -> 行 4：和妹妹对话结束（select4 的“结束对话”）。 / Row 3 to row 4. -->
    <transition source="s3" target="s3">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{SETZKIKI}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT4"/>
      </after-commit>
    </transition>
    <transition source="s3" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{SETZKIKI}" action="SET_SUCCEED"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s2" target="s2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{SETZKIKI}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s3" target="s3">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{SETZKIKI}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 4：回哥哥 {SEIJIN} 报告领奖（客户端 select_success 的 SELECT_QUEST_REWARD）。 -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{SEIJIN}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{SEIJIN}" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{SEIJIN}" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="{SEIJIN}" source="reward" target="complete" fixed-reward-indices="0 1 2" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG"/>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{SEIJIN}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
  </transitions>
</quest-definition>
"""


def build(current: str) -> str:
	metadata = metadata_block(current, QUEST_ID)
	return tidy(f'<?xml version="1.0" encoding="UTF-8"?>\n<quest-definition id="{QUEST_ID}" version="1">\n'
	            + metadata + body())


def main(argv: list[str]) -> int:
	apply = "--apply" in argv
	check = "--check" in argv or not apply
	path = QUESTS / f"{QUEST_ID}.xml"
	current = tidy(path.read_text(encoding="utf-8"))
	document = build(current)
	if check:
		if current == document:
			print(f"BATCH44_OK {QUEST_ID} already-applied")
			return 0
		print(f"BATCH44_PENDING {QUEST_ID} differs from target document")
		return 1
	if current == document:
		print(f"BATCH44_OK {QUEST_ID} already-applied")
		return 0
	path.write_text(document, encoding="utf-8")
	print(f"BATCH44_APPLIED {QUEST_ID}")
	return 0


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
