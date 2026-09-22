#!/usr/bin/env python3
"""批次 38：活动“Lover or Loner? / Bitter or Sweet?”阵营选择族的三行状态与完成奖励索引（QE-051）。

族判据（2026-09-22，客户端 quest_summary + quest_data 前置条件）：
- 80298 / 80299（Elyos）与 80304 / 80305（Asmodian）客户端任务书三行、槽位 %0/%3/%6：
  行 0「坦坦荡荡的回答到底是有恋人，还是孤单一人吧！」、行 1「去见坠入爱河的术古」、
  行 2「去见单身部队成员」。页链 select_none(4762) -> select1_1(1012) ->
  select2_1(1353) / select2_2(1438) -> select2_1_1(1354) -> select3_1(1694) / select3_2(1779)。
- SETPRO1（1694 页的结束按钮）冻结完成奖励索引 1，SETPRO2（1779 页）冻结索引 2；
  后续 80300/80302/80306/80308 的 start condition 是 reward-mode=1，80301/80303/80307/80309 是
  reward-mode=2。旧定义把 SETPRO2 直接送到 reward/var0=0，SETPRO1 只走到 started/var0=0，
  所以三行中只有行 0 有状态，且旧存档的 REWARD/var0=0 无法区分完成分支。
- 本批把两个分支各自投影成 REWARD 行：reward1(var0=1, 情侣) / reward(var0=2, 单身)；
  完成索引用 npc-complete 的 complete-reward-index 1/2 持久化。由于物理奖励组只有一个，
  expander 按“单组 + 非零完成索引”保留状态语义（见 QuestXmlBlockExpander.rewardGroup）。
  显式 -1/1009 预览路由覆盖自动推导，避免完成索引 2 被 QuestDialogPage.rewardWindowForTier(2)
  猜成窗口 3；两个分支分别打开客户端声明过的奖励窗口 1 / 2。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch38_branch_choice_reward_index.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch38_branch_choice_reward_index.py --apply
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_IDS = (80298, 80299, 80304, 80305)

HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<!--
  {qid} 活动阵营选择族：客户端任务书三行阶梯（QE-051 批次 38）。
  Client journal rows (quest_q{qid}.html): row 0 answer lover/loner, row 1 go to the lover shugo,
  row 2 go to the single squad member. SETPRO1 freezes completion reward index 1; SETPRO2 freezes
  index 2. The follow-up quests 80300/80302/80306/80308 require reward-mode 1 and
  80301/80303/80307/80309 require reward-mode 2.
  行 0「回答有恋人还是孤单一人」-> 行 1「去见坠入爱河的术古」（SETPRO1 / 索引 1）或
  行 2「去见单身部队成员」（SETPRO2 / 索引 2）。旧定义把行 1/2 塌陷成 var0=0。
-->
"""

BODY = """  <progress>
    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>
  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="reward1" status="REWARD">
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
    <!-- QE-051 自愈边：旧存档停在 REWARD/var0=0（旧 SETPRO2 单人分支）时补到行 2。 -->
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

    <!-- 接取对话：QUEST_SELECT 显示 select_none 继续对话；通用接取按钮保留兼容。 -->
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT_NONE"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="ASK_QUEST_ACCEPT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_ASK_QUEST_ACCEPT_WINDOW"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="QUEST_ACCEPT_1"/>
      </event>
      <conditions>
        <start-eligible/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="QUEST_ACCEPT_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="QUEST_ACCEPT_SIMPLE"/>
      </event>
      <conditions>
        <start-eligible/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" actions="QUEST_REFUSE_1 QUEST_REFUSE_2 QUEST_REFUSE_SIMPLE"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 未接取分支也保留客户端页链；最后一步同时完成接取并冻结完成奖励索引。 -->
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT2_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT2_2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_2"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT3_2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_2"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="reward1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SETPRO1"/>
      </event>
      <conditions>
        <start-eligible/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SETPRO2"/>
      </event>
      <conditions>
        <start-eligible/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW2"/>
      </after-commit>
    </transition>

    <!-- 已接取（通用接取窗口）后的同一页链。 -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT_NONE"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT2_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT2_2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT3_2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_2"/>
      </after-commit>
    </transition>
    <transition source="started" target="reward1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="started" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 奖励态重开窗口：显式覆盖 -1/1009 预览，避免完成索引 1/2 被猜成窗口 2/3。 -->
    <transition source="reward1" target="reward1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="reward1" target="reward1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW2"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799763" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW2"/>
      </after-commit>
    </transition>

    <!-- 完成路由：complete-reward-index 1/2 是后续任务的 reward-mode 分流标志。 -->
    <npc-complete npc-id="799763" source="reward1" target="complete" fixed-reward-indices="0" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="1" finish="SELECTION_DIALOG"/>
    <npc-complete npc-id="799763" source="reward" target="complete" fixed-reward-indices="0" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="2" finish="SELECTION_DIALOG"/>
  </transitions>
</quest-definition>
"""


def tidy(text: str) -> str:
	return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def metadata_block(text: str, quest_id: int) -> str:
	match = re.search(r"  <metadata .*?</metadata>\n", text, re.S)
	if match is None:
		raise SystemExit(f"BATCH38_ERROR {quest_id}: cannot locate metadata")
	return match.group(0)


def build(quest_id: int, current: str) -> str:
	metadata = metadata_block(current, quest_id)
	return tidy(HEADER.format(qid=quest_id)
	            + f'<quest-definition id="{quest_id}" version="1">\n'
	            + metadata + BODY)


def main(argv: list[str]) -> int:
	apply = "--apply" in argv
	check = "--check" in argv or not apply
	status = 0
	for quest_id in QUEST_IDS:
		path = QUESTS / f"{quest_id}.xml"
		current = tidy(path.read_text(encoding="utf-8"))
		document = build(quest_id, current)
		if check:
			if current == document:
				print(f"BATCH38_OK {quest_id} already-applied")
			else:
				print(f"BATCH38_PENDING {quest_id} differs from target document")
				status = 1
			continue
		if current == document:
			print(f"BATCH38_OK {quest_id} already-applied")
			continue
		path.write_text(document, encoding="utf-8")
		print(f"BATCH38_APPLIED {quest_id}")
	return status


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
