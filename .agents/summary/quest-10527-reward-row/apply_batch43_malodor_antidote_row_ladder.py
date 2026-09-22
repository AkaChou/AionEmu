#!/usr/bin/env python3
"""批次 43：食人花解毒剂族 2239（Malodor Antidote）的行/状态收口（QE-051）。

族判据（2026-09-22，客户端 quest_summary + HTML 页链 + 客户端 NPC 名表）：
- 2239 客户端任务书三行、槽位 %0/%3/%6：
  行 0「和 Vovetirn 对话」、行 1「把 quest_2239a 交给 Vovetirn[collectitem]」、
  行 2「把食人花解毒剂交给 Gilungk」。
- 页链（Dialogs/QUEST_Q2239.html）：
  - 接取：Gilungk 203613 的 `select1` -> `select1_1` -> `ask_quest_accept` -> `quest_accept_1`
    （文本“请您帮我去问问 Vovetirn 该怎么办吧”）；
  - 行 0：Vovetirn 203630 的 `select2`（“转达基隆克的话”）-> `select2_1`（SELECT2_1_1）
    -> `select2_1_1`（SETPRO1，文本“请您快去搜集食人花的外皮”）；
  - 行 1：Vovetirn 的 `select3`（CHECK_USER_HAS_QUEST_ITEM，“拿出食人花的外皮”）->
    成功 `select3_2`（SETPRO2，“好了…快去把解毒剂给基隆克吧”）/ 失败 `select3_1`；
  - 行 2：Gilungk 的 `select4`（SETPRO3，“拿出食人花解毒剂”）-> `select_quest_reward1` 领奖页。
- 物品合同：行 1 交 3 个 182203228（食人花外皮，掉落自 210482/210483），Vovetirn 做出
  工作物品 182203227（食人花解毒剂）；行 2 交给 Gilungk 并领奖。
- 旧定义错位：Gilungk 与 Vovetirn 都能接取 + 领奖，`SETPRO1` 直接 `started -> reward` 并把
  外皮移除挂在这一步；行 1/行 2 没有状态，Vovetirn 的 select3 链与 Gilungk 的 select4 完全缺失。
- 本批把行阶梯投影成 started(0) -> s1(1) -> reward(2)：Vovetirn 的 SETPRO1 推进到行 1，
  CHECK+SETPRO2 完成行 1 并发出解毒剂，Gilungk 的 select4/SETPRO3 打开奖励窗口 1 收口。
  客户端按钮 SETPRO3 只表示“交出解毒剂”，落点仍是 var0=2 的 REWARD 行（3 行任务的槽位只到 2）。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch43_malodor_antidote_row_ladder.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch43_malodor_antidote_row_ladder.py --apply
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_ID = 2239
GILUNGK = 203613
VOVETIRN = 203630
PEEL_ITEM = 182203228
PEEL_COUNT = 3
ANTIDOTE_ITEM = 182203227

REWARD_INNER = ('      fixed-reward-indices="0 1 2 3" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" '
                'complete-reward-index="0" finish="SELECTION_DIALOG">\n'
                '      <preview actions="USE_OBJECT"/>')


def tidy(text: str) -> str:
	return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def metadata_block(text: str, quest_id: int) -> str:
	match = re.search(r"  <metadata .*?</metadata>\n", text, re.S)
	if match is None:
		raise SystemExit(f"BATCH43_ERROR {quest_id}: cannot locate metadata")
	return match.group(0)


def body() -> str:
	return f"""  <!-- QE-051 行阶梯收口（批次 43）：食人花解毒剂三行族。
       客户端任务书三行、槽位 %0/%3/%6：行 0 和 Vovetirn 对话（select2 链的 SETPRO1）、
       行 1 交 3 个食人花外皮做解毒剂（select3 的 CHECK 链 + SETPRO2）、
       行 2 把解毒剂交给 Gilungk（select4 的 SETPRO3）。
       Row ladder closure (batch 43): one state per journal row of the Malodor Antidote quest. -->
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
    <!-- 旧存档自愈：迁移前的 SETPRO1 直跳落盘 REWARD/var0=0，把它提到行 2。
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
    <!-- 接取：只在闯祸的厨师 Gilungk {GILUNGK} 上。 / Accept only on Gilungk. -->
    <dialog type="NPC_START" npc-id="{GILUNGK}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1"/>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{GILUNGK}" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <!-- 行 0：和 Vovetirn {VOVETIRN} 对话（select2 -> select2_1 -> select2_1_1 -> SETPRO1）。 -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{VOVETIRN}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{VOVETIRN}" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{VOVETIRN}" action="SELECT2_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1_1"/>
      </after-commit>
    </transition>
    <!-- 行 0 -> 行 1。 / Row 0 to row 1. -->
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{VOVETIRN}" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{VOVETIRN}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：交 3 个食人花外皮（select3 的 CHECK 链）。 / Row 1: hand the three peels over. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{VOVETIRN}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{VOVETIRN}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="{PEEL_ITEM}" count="{PEEL_COUNT}"/>
      </conditions>
      <actions>
        <remove-item item-id="{PEEL_ITEM}" count="{PEEL_COUNT}"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_2"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{VOVETIRN}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <!-- 行 1 -> 行 2：解毒剂做好，交给玩家。 / Row 1 to row 2: the antidote is handed over. -->
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{VOVETIRN}" action="SETPRO2"/>
      </event>
      <actions>
        <give-item item-id="{ANTIDOTE_ITEM}" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{VOVETIRN}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 2：把解毒剂交给 Gilungk（客户端 select4 的 SETPRO3）+ 奖励窗口 1。 -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{GILUNGK}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT4"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{GILUNGK}" action="SETPRO3"/>
      </event>
      <conditions>
        <has-item item-id="{ANTIDOTE_ITEM}" count="1"/>
      </conditions>
      <actions>
        <remove-item item-id="{ANTIDOTE_ITEM}" count="1"/>
      </actions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="{GILUNGK}" source="reward" target="complete"
{REWARD_INNER}
    </npc-complete>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{GILUNGK}" action="FINISH_DIALOG"/>
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
			print(f"BATCH43_OK {QUEST_ID} already-applied")
			return 0
		print(f"BATCH43_PENDING {QUEST_ID} differs from target document")
		return 1
	if current == document:
		print(f"BATCH43_OK {QUEST_ID} already-applied")
		return 0
	path.write_text(document, encoding="utf-8")
	print(f"BATCH43_APPLIED {QUEST_ID}")
	return 0


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
