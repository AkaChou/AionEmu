#!/usr/bin/env python3
"""批次 45：三行族 1938（天族，顺序阶梯）与 2922（魔族，二选一分支阶梯）的行/状态收口（QE-051）。

族判据（2026-09-22，客户端 quest_summary + HTML 页链 + 客户端 NPC 名表 + 迁移前 Java handler）：
- 1938（Elyos，36 级，Black Cloud Fakery，前置 1471）客户端任务书三行、槽位 %0/%3/%6：
  行 0「和 Shugo_LF3_1 对话」、行 1「调查 LF3_Nakaching_E」、行 2「向 Likasas 报告」。
  页链（Dialogs/QUEST_Q1938.html）：`select1`(ASK_QUEST_ACCEPT) -> `ask_quest_accept` ->
  `quest_accept_1`（里卡萨斯委托）；行 0 = Shugo_LF3_1 `select2`(SELECT2_1 转达里卡萨斯的话) ->
  `select2_1`(SETPRO1)；行 1 = LF3_Nakaching_E `select3`(SELECT3_1 威胁) -> `select3_1`(SETPRO2)；
  行 2 = Likasas `select5`(SELECT_QUEST_REWARD 报告结果) -> `select_quest_reward1` 领奖页。
  客户端名表：203703 = Likasas（接取/报告 owner）、798069 = Shugo_LF3_1、805836 = LF3_Nakaching_E。
  旧定义把三行塌陷成 `started(0) -> reward(0)` 的直跳（接取与报告都挂在 203703 上，行 0/行 1
  的 NPC 完全没有路由），因此行 1/行 2 永远拿不到状态。
- 2922（Asmodian，21 级，Fascinating Gift，前置 2921）客户端任务书三行、槽位 %0/%3/%6：
  行 0「接受 Daskair 的请求」、行 1「向 Shugo_DC1_3 定做箱子」、行 2「向 Lanse 定做耳坠」。
  页链（Dialogs/QUEST_Q2922.html）：`select_none` -> `ask_quest_accept` -> `quest_accept_1`
  （两个互斥选择按钮 SELECT1_1 箱子 / SELECT1_2 耳坠）-> `select1_1`(SETPRO10) / `select1_2`(SETPRO20)；
  箱子分支 `select2`(SELECT_QUEST_REWARD) -> `select_quest_reward1`；耳坠分支
  `select3`(SELECT_QUEST_REWARD) -> `select_quest_reward2`；走错人时 `select2_1`（Shugo_DC1_3：
  「我只做箱子不做耳环」）/`select3_1`（Lanse：「我不做箱子」）。
  客户端名表：204261 = Daskair（唯一接取 owner）、798058 = Shugo_DC1_3（箱子分支）、
  204108 = Lanse（耳坠分支）。迁移前 Java handler `_2922FascinatingGift` 的 step 落盘是
  STEP_TO_10 -> var0=10 + REWARD、STEP_TO_20 -> var0=20 + REWARD（打包 step，不是行号），
  此后在 798058/204108 上按 var0 分流领奖；迁移把两个分支塌陷成 `started(0) -> reward(0)`，
  行 1/行 2 都没有状态，且 798058/204108 上各挂了一份重复的接取/报告/领奖段。
- 本批把两支分别投影成“行 0 -> 行 1（箱子）/ 行 2（耳坠）”的互斥 REWARD 行：
  reward1(var0=1, 箱子) / reward(var0=2, 耳坠)，完成奖励索引用 0（2923/2924 只按 finished 前置，
  不区分分支）。旧存档自愈：START/REWARD 下 var0=10/20（迁移前 step）折算成行 1/行 2，
  REWARD/var0=0（迁移后 XML 直接落盘 REWARD）按迁移后 SELECT2 页链默认归到箱子行 1。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch45_three_row_ladder_and_branch_reward.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch45_three_row_ladder_and_branch_reward.py --apply
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_IDS = (1938, 2922)

# 1938 owner：接取/报告 = 203703 Likasas；行 0 = 798069 Shugo_LF3_1；行 1 = 805836 LF3_Nakaching_E
Q1938_LIKASAS = 203703
Q1938_SHUGOS = 798069
Q1938_NAKACHING = 805836

# 2922 owner：接取 = 204261 Daskair；箱子分支 = 798058 Shugo_DC1_3；耳坠分支 = 204108 Lanse
Q2922_DASKAIR = 204261
Q2922_SHUGODC = 798058
Q2922_LANSE = 204108


def tidy(text: str) -> str:
	return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def metadata_block(text: str, quest_id: int) -> str:
	match = re.search(r"  <metadata .*?</metadata>\n", text, re.S)
	if match is None:
		raise SystemExit(f"BATCH45_ERROR {quest_id}: cannot locate metadata")
	return match.group(0)


def body_1938() -> str:
	return f"""  <!-- QE-051 行阶梯收口（批次 45）：黑云伪造事件三行族。
       客户端任务书三行、槽位 %0/%3/%6：行 0 和 Shugo_LF3_1 {Q1938_SHUGOS} 对话（select2 链的 SETPRO1）、
       行 1 调查 LF3_Nakaching_E {Q1938_NAKACHING}（select3 链的 SETPRO2）、
       行 2 向 Likasas {Q1938_LIKASAS} 报告领奖（select5 页）。
       Row ladder closure (batch 45): three journal rows, one state each. -->
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
    <!-- 旧存档自愈：迁移前的 started -> reward 直跳落盘 REWARD/var0=0，把它提到行 2（报告领奖行）。
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
    <!-- 接取：Likasas {Q1938_LIKASAS} 的 select1 页。 / Accept from Likasas. -->
    <dialog type="NPC_START" npc-id="{Q1938_LIKASAS}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1"/>
    <!-- 行 0：Shugo_LF3_1 的 select2 -> select2_1 -> SETPRO1 链。 / Row 0: the Shugo_LF3_1 confession chain. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q1938_SHUGOS}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q1938_SHUGOS}" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q1938_SHUGOS}" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q1938_SHUGOS}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：LF3_Nakaching_E 的 select3 -> select3_1 -> SETPRO2 链。 / Row 1: the Nakaching interrogation chain. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q1938_NAKACHING}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q1938_NAKACHING}" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q1938_NAKACHING}" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q1938_NAKACHING}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 2：向 Likasas 报告（客户端 select5 页 -> 奖励窗口 1）并领奖。 / Row 2: report back and take the reward. -->
    <dialog type="NPC_REPORT" npc-id="{Q1938_LIKASAS}" source="reward" target="reward" page="SELECT5"/>
    <npc-complete npc-id="{Q1938_LIKASAS}" source="reward" target="complete"
      fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT"/>
    </npc-complete>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q1938_LIKASAS}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
  </transitions>
</quest-definition>
"""


def body_2922() -> str:
	return f"""  <!-- QE-051 行阶梯收口（批次 45）：Pandaemonium 定做礼物二选一分支族。
       客户端任务书三行、槽位 %0/%3/%6：行 0 接受 Daskair {Q2922_DASKAIR} 的请求、
       行 1 向 Shugo_DC1_3 {Q2922_SHUGODC} 定做箱子（select1_1 -> SETPRO10）、
       行 2 向 Lanse {Q2922_LANSE} 定做耳坠（select1_2 -> SETPRO20）；两个分支互斥，
       各自投影成 Reward 行 reward1(var0=1) / reward(var0=2)。迁移前的 step 10/20 折算成行 1/行 2。
       Row ladder closure (batch 45): two mutually exclusive branch rows project to reward1/reward. -->
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
    <!-- 旧存档自愈：迁移前 Java handler 的 step 10/20 与迁移后 XML 直接落盘的 REWARD/var0=0
         都折算回行号（箱子=行 1、耳坠=行 2）。
         / Heal pre-migration step 10/20 saves and the typed-XML REWARD/var0=0 save. -->
    <transition target="reward1">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="START"/>
        <variable-is field="var0" value="10"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
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
        <status-is status="START"/>
        <variable-is field="var0" value="20"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition target="reward1">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="10"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
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
        <variable-is field="var0" value="20"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition target="reward1">
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
    <!-- 接取：唯一 owner Daskair（select_none 链）。 / Accept only on Daskair. -->
    <dialog type="NPC_START" npc-id="{Q2922_DASKAIR}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <!-- 行 0 -> 行 1（箱子分支）/ 行 2（耳坠分支）：两个互斥选择按钮各自冻结分支行。 -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_DASKAIR}" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_DASKAIR}" action="SELECT1_2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_2"/>
      </after-commit>
    </transition>
    <transition source="started" target="reward1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_DASKAIR}" action="SETPRO10"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_DASKAIR}" action="SETPRO20"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1（箱子）：Shugo_DC1_3 的 select2 页 -> 奖励窗口 1；对 Lanse 显示 select3_1（走错人）。 -->
    <transition source="reward1" target="reward1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_SHUGODC}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="reward1" target="reward1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_SHUGODC}" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="reward1" target="reward1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_SHUGODC}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="reward1" target="reward1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_LANSE}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="reward1" target="reward1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_LANSE}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <npc-complete npc-id="{Q2922_SHUGODC}" source="reward1" target="complete"
      fixed-reward-indices="0" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT"/>
    </npc-complete>
    <!-- 行 2（耳坠）：Lanse 的 select3 页 -> 奖励窗口 1；对 Shugo_DC1_3 显示 select2_1（走错人）。 -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_LANSE}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_LANSE}" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_LANSE}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_SHUGODC}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{Q2922_SHUGODC}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <npc-complete npc-id="{Q2922_LANSE}" source="reward" target="complete"
      fixed-reward-indices="0" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""


def body(quest_id: int) -> str:
	return body_1938() if quest_id == 1938 else body_2922()


def build(quest_id: int, current: str) -> str:
	metadata = metadata_block(current, quest_id)
	return tidy(f'<?xml version="1.0" encoding="UTF-8"?>\n<quest-definition id="{quest_id}" version="1">\n'
	            + metadata + body(quest_id))


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
				print(f"BATCH45_OK {quest_id} already-applied")
			else:
				print(f"BATCH45_PENDING {quest_id} differs from target document")
				status = 1
			continue
		if current == document:
			print(f"BATCH45_OK {quest_id} already-applied")
			continue
		path.write_text(document, encoding="utf-8")
		print(f"BATCH45_APPLIED {quest_id}")
	return status


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
