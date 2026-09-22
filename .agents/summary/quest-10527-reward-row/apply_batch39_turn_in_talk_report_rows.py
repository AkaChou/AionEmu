#!/usr/bin/env python3
"""批次 39：三行「交付物品 -> 对话/招供 -> 向最终 NPC 报告并领奖」族的行/状态收口（QE-051）。

族判据（2026-09-22，客户端 quest_summary + HTML 页链 + 客户端 NPC 名表）：
- 3013（Elyos，23 级，item 182208008 ×1）/ 3217（Elyos，43 级，item 182209095 ×3）/
  4217（Asmodian，43 级，item 182209110 ×3）的客户端任务书都是三行、槽位 %0/%3/%6：
  - 3013 行 0「搜索 DigCherubim/Cherubim2Wp 把撕碎信纸交给 Shugo_LF2a_1」、行 1「让 Hecuba 招供」、
    行 2「向 Shugo_LF2a_1 报告结果」；
  - 3217 行 0「找回侦察报告书交给 Nasuri」、行 1「和 Nasuri 对话」、行 2「和 Gorgos 对话」；
  - 4217 行 0「找回旧包袱交给 Parten」、行 1「和 Parten 对话」、行 2「和 Savrina 对话」。
- 客户端页链证据（Dialogs/QUEST_Q<id>.html）：
  - 三家都是 `select1`(丢弃/检查物品按钮 HACTION_CHECK_USER_HAS_QUEST_ITEM) -> `check_user_item_ok`；
  - 3013 `check_user_item_ok` 的按钮是「结束对话」(FINISH_DIALOG)，行 1 由 Hecuba 798146 的
    `select2 -> SELECT2_1 -> SELECT2_1_1 -> SET_SUCCEED` 承担，行 2 由 798132 的
    `select_success -> SELECT_QUEST_REWARD` 承担；
  - 3217/4217 `check_user_item_ok` 的按钮就是 SET_SUCCEED（行 1 当场收尾），行 2 的
    `select_success -> SELECT_QUEST_REWARD` 分别属于 Gorgos 204590 / Savrina 204773
    （客户端 npcs_unpacked/client_npcs_npc.xml 的 STR_NPC_Gorgos / STR_NPC_Savrina 映射）。
- 旧定义把三行塌陷成 `started(0) -> reward(0)` 的直跳（3217/4217 还在两个 NPC 上各挂一份领奖段，
  3013 把 798132 的报告页写成 Hecuba 的 SELECT2），因此行 1/行 2 永远拿不到状态。
- 本批把行阶梯投影成 started(0) -> s1(1) -> reward(2)：交付物品时推进到行 1，SET_SUCCEED 推进到行 2，
  行 2 的报告对话（客户端 select_success 页）打开奖励窗口 1 并由该 NPC 完成领奖。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch39_turn_in_talk_report_rows.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch39_turn_in_talk_report_rows.py --apply
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_IDS = (3013, 3217, 4217)

# quest_id -> (start_npc, report_npc, item_id, item_count)
FAMILY: dict[int, tuple[int, int, int, int]] = {
	3013: (798132, 798132, 182208008, 1),
	3217: (798335, 204590, 182209095, 3),
	4217: (798336, 204773, 182209110, 3),
}

# quest_id -> npc-complete 片段的固定奖励索引与选择项（沿用迁移前定义）
# preview 只保留 USE_OBJECT：SELECT_QUEST_REWARD 已由 reward 态的 NPC_REPORT 展开提供，两者同 NPC 同动作
# 会触发 AMBIGUOUS_TRANSITION（QE-051 批次 39 编译实测）。
# The preview keeps only USE_OBJECT: SELECT_QUEST_REWARD is served by the reward-state NPC_REPORT expansion,
# and declaring both would trip AMBIGUOUS_TRANSITION for the same NPC/action pair.
REWARD_BLOCK: dict[int, str] = {
	3013: """      fixed-reward-indices="0 1 2" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="3"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="4"/>
      <preview actions="USE_OBJECT"/>""",
	3217: """      fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT"/>""",
	4217: """      fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT"/>""",
}


def tidy(text: str) -> str:
	return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def metadata_block(text: str, quest_id: int) -> str:
	match = re.search(r"  <metadata .*?</metadata>\n", text, re.S)
	if match is None:
		raise SystemExit(f"BATCH39_ERROR {quest_id}: cannot locate metadata")
	return match.group(0)


def body(quest_id: int) -> str:
	start_npc, report_npc, item_id, item_count = FAMILY[quest_id]
	reward = REWARD_BLOCK[quest_id]
	row_one_owner = 798146 if quest_id == 3013 else start_npc
	row_one_page = "CHECK_USER_ITEM_OK" if quest_id != 3013 else "CHECK_USER_ITEM_OK"
	header = f"""  <!-- QE-051 行阶梯收口（批次 39）：客户端任务书三行、槽位 %0/%3/%6，状态 0/1/2 与行一一对应。
       Row ladder closure (batch 39): the client journal owns rows %0/%3/%6, so each row keeps a state.
       行 0 交付物品 -> 行 1 对话/SET_SUCCEED -> 行 2 报告领奖。 -->
"""
	if quest_id == 3013:
		return header + f"""  <progress>
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
    <!-- 旧存档自愈：迁移前的 started -> reward 直跳落盘 REWARD/var0=0（物品已被收走），
         把它提到行 2（报告领奖行）。 / Heal the pre-migration REWARD/var0=0 save to row 2. -->
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
    <!-- 接取：Shugo_LF2a_1 798132 委托找回撕碎信纸的其余部分。 / Accept from 798132. -->
    <dialog type="NPC_START" npc-id="{start_npc}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <!-- 行 0：798132 的 select1 页「递交信的其余部分」。 / Row 0: hand the letter fragments over. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="{item_id}" count="{item_count}"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
        <remove-item item-id="{item_id}" count="{item_count}"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>
      </after-commit>
    </transition>
    <transition source="started" target="started" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：Hecuba 798146 的 select2 链。 / Row 1: the Hecuba confession chain. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798146" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798146" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798146" action="SELECT2_1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798146" action="SET_SUCCEED"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798146" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 2：向 {start_npc} 报告（客户端 select_success 页）+ 领奖。 / Row 2: report back and take the reward. -->
    <dialog type="NPC_REPORT" npc-id="{start_npc}" source="reward" target="reward" page="DEFAULT_SUCCESS"/>
    <npc-complete npc-id="{start_npc}" source="reward" target="complete"
{reward}
    </npc-complete>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
  </transitions>
</quest-definition>
"""
	return header + f"""  <progress>
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
    <!-- 旧存档自愈：迁移前的 started -> reward 直跳落盘 REWARD/var0=0（物品已被收走），
         把它提到行 2（报告领奖行）。 / Heal the pre-migration REWARD/var0=0 save to row 2. -->
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
    <!-- 接取：{start_npc} 的 select_none 页。 / Accept from {start_npc}. -->
    <dialog type="NPC_START" npc-id="{start_npc}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <!-- 行 0：{start_npc} 的 select1 页（拿出收集物）。 / Row 0: hand the collected item over. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="{item_id}" count="{item_count}"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
        <remove-item item-id="{item_id}" count="{item_count}"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>
      </after-commit>
    </transition>
    <transition source="started" target="started" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1 -> 行 2：{start_npc} 的 check_user_item_ok 页以 SET_SUCCEED 收尾对话。 -->
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="SET_SUCCEED"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 2：和 {report_npc} 对话并领奖（客户端 select_success 页 -> 奖励窗口 1）。 -->
    <dialog type="NPC_REPORT" npc-id="{report_npc}" source="reward" target="reward" page="DEFAULT_SUCCESS"/>
    <npc-complete npc-id="{report_npc}" source="reward" target="complete"
{reward}
    </npc-complete>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{report_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
  </transitions>
</quest-definition>
"""


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
				print(f"BATCH39_OK {quest_id} already-applied")
			else:
				print(f"BATCH39_PENDING {quest_id} differs from target document")
				status = 1
			continue
		if current == document:
			print(f"BATCH39_OK {quest_id} already-applied")
			continue
		path.write_text(document, encoding="utf-8")
		print(f"BATCH39_APPLIED {quest_id}")
	return status


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
