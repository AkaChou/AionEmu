#!/usr/bin/env python3
"""批次 42：布鲁斯特豪宁献花族 4033（A Bloom in Brusthonin）的行/状态收口（QE-051）。

族判据（2026-09-22，客户端 quest_summary + HTML 页链 + 客户端 NPC 名表）：
- 4033 客户端任务书三行、槽位 %0/%3/%6：
  行 0「采集 DF2B_herb_d_n_c_40a 并交给 Heintz[collectitem]」、行 1「把花环献给
  OBJ_DF2A_Tombstone_Q4033」、行 2「与 Heintz 对话」。
- 页链（Dialogs/QUEST_Q4033.html）：
  - 接取：Heintz 205155 的 `select1`（按钮 SELECT1_1）-> `select1_1`（按钮 ASK_QUEST_ACCEPT）
    -> `ask_quest_accept` -> `quest_accept_1`；
  - 行 0：`select2`（按钮 CHECK_USER_HAS_QUEST_ITEM）-> `select2_1`（按钮 SELECT2_1_1）
    -> `select2_1_1`（按钮 SETPRO1，文本“请代我将花环送到 [墓碑]”）；
  - 行 1：墓碑物件 700379 的 `select3`（按钮 SELECT3_1）-> `select3_1`（“将花环放在墓碑前。结束观察”）；
  - 行 2：Heintz 的 `select4`（按钮 SELECT_QUEST_REWARD）-> `select_quest_reward1` 领奖页。
- 物品合同：行 0 交 5 个 152000463（水仙花）换花环工作物品 182209042（SETPRO1 的 give-item）；
  行 1 墓碑吃掉 182209042。
- 旧定义错位：`SETPRO1` 是 `started -> started` 自环（只发花环不推进），墓碑 `SELECT3_1`
  直接 `started -> reward` 并带 `has-item`，行 2 用 `SELECT_QUEST_REWARD` 直跳 reward/var0=0，
  因此行 1/行 2 没有状态，行 0 的花环也永远拿不到正确的中间行。
- 本批把行阶梯投影成 started(0) -> s1(1) -> reward(2)：SETPRO1 推进到行 1，墓碑 SELECT3_1
  从 s1 推进到行 2，行 2 由 Heintz 的 `select4` 显式路由 + 奖励窗口 1 收口。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch42_tombstone_flower_row_ladder.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch42_tombstone_flower_row_ladder.py --apply
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_ID = 4033
HEINTZ = 205155
TOMBSTONE = 700379
FLOWER_ITEM = 152000463
FLOWER_COUNT = 5
WREATH_ITEM = 182209042

REWARD_INNER = ('      fixed-reward-indices="0 1 2" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" '
                'complete-reward-index="0" finish="SELECTION_DIALOG">\n'
                '      <preview actions="USE_OBJECT"/>')


def tidy(text: str) -> str:
	return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def metadata_block(text: str, quest_id: int) -> str:
	match = re.search(r"  <metadata .*?</metadata>\n", text, re.S)
	if match is None:
		raise SystemExit(f"BATCH42_ERROR {quest_id}: cannot locate metadata")
	return match.group(0)


def body() -> str:
	return f"""  <!-- QE-051 行阶梯收口（批次 42）：布鲁斯特豪宁献花三行族。
       客户端任务书三行、槽位 %0/%3/%6：行 0 交水仙花并拿花环（select2 链的 SETPRO1）、
       行 1 到墓碑献花（物件 {TOMBSTONE} 的 select3 链）、行 2 回 Heintz 报告领奖（select4）。
       Row ladder closure (batch 42): one state per journal row of the Brusthonin flower quest. -->
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
    <!-- 旧存档自愈：迁移前的 SETPRO1/SELECT3_1 直跳落盘 REWARD/var0=0，把它提到行 2。
         / Heal the pre-migration REWARD/var0=0 save to row 2. -->
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
    <!-- 接取：Heintz {HEINTZ} 的 select1 链。 / Accept chain on Heintz. -->
    <dialog type="NPC_START" npc-id="{HEINTZ}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1"/>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{HEINTZ}" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <!-- 行 0：交水仙花换花环（select2 -> CHECK -> select2_1 -> SELECT2_1_1 -> select2_1_1 -> SETPRO1）。 -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{HEINTZ}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{HEINTZ}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="{FLOWER_ITEM}" count="{FLOWER_COUNT}"/>
      </conditions>
      <actions>
        <remove-item item-id="{FLOWER_ITEM}" count="{FLOWER_COUNT}"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{HEINTZ}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{HEINTZ}" action="SELECT2_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1_1"/>
      </after-commit>
    </transition>
    <!-- 行 0 -> 行 1：拿到花环（SETPRO1 从 started 推进到 s1）。 / Row 0 to row 1. -->
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{HEINTZ}" action="SETPRO1"/>
      </event>
      <actions>
        <give-item item-id="{WREATH_ITEM}" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{HEINTZ}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：在墓碑 {TOMBSTONE} 献花（select3 -> SELECT3_1 -> 行 2）。 / Row 1: offer the wreath. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{TOMBSTONE}" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{TOMBSTONE}" action="SELECT3_1"/>
      </event>
      <conditions>
        <has-item item-id="{WREATH_ITEM}" count="1"/>
      </conditions>
      <actions>
        <remove-item item-id="{WREATH_ITEM}" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- quest_use_item 交互物 {TOMBSTONE}：s1 态对话路由需 ACTION_ITEM_USE 资格声明。 -->
    <transition source="s1" target="s1">
      <event>
        <can-act template-id="{TOMBSTONE}" action-type="ACTION_ITEM_USE"/>
      </event>
    </transition>
    <!-- 行 2：回 Heintz 报告（客户端 select4 的 SELECT_QUEST_REWARD）+ 奖励窗口 1。 -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{HEINTZ}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT4"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{HEINTZ}" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="{HEINTZ}" source="reward" target="complete"
{REWARD_INNER}
    </npc-complete>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{HEINTZ}" action="FINISH_DIALOG"/>
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
			print(f"BATCH42_OK {QUEST_ID} already-applied")
			return 0
		print(f"BATCH42_PENDING {QUEST_ID} differs from target document")
		return 1
	if current == document:
		print(f"BATCH42_OK {QUEST_ID} already-applied")
		return 0
	path.write_text(document, encoding="utf-8")
	print(f"BATCH42_APPLIED {QUEST_ID}")
	return 0


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
