#!/usr/bin/env python3
"""批次 40：三行「接取 → 和行 0 NPC 对话 → 和行 1 NPC 对话 → 向行 2 NPC 报告领奖」族（QE-051）。

族判据（2026-09-22，客户端 quest_summary + HTML 页链 + 客户端 NPC 名表）：
- 11072（天族 50 级）/ 21081（魔族 50 级，Gelkmaros 武器补给）/ 24150（魔族，贝鲁斯兰要塞）三家的
  客户端任务书都是三行、槽位 %0/%3/%6，且页链完全同型：
  - 接取 NPC 走 `select1`（11072 还有 `select1_1`）→ `ask_quest_accept` → `quest_accept_1`；
  - 行 0 的 NPC 走 `select2`（按钮 SELECT2_1）→ `select2_1`（按钮 SETPRO1）；
  - 行 1 的 NPC 走 `select3`（按钮 SELECT3_1）→ `select3_1`（按钮 SETPRO2）；
  - 行 2 的 NPC 走 `select5`（按钮 SELECT_QUEST_REWARD）→ 领奖窗口 1。
  行号与 NPC 由客户端任务书逐行点名，客户端名表给出 id：
  11072 Borriello 798907 / Delus 798960 / Seneca 798937（接取与报告同一人）；
  21081 Hler 799225（只接取）/ Agovard 799332 / Renato 799217 / Sepsi 799202；
  24150 Nerthus 204702（接取与报告同一人）/ Bestla 204733 / Horu 204734。
- 旧定义把三家都塌陷成“每个任务 NPC 都能接取 + 都能领奖”的扁平模板：
  只有 `SELECT2/SELECT2_1/SETPRO1 -> reward`，`SELECT3/SELECT3_1/SETPRO2` 与 `SELECT5` 完全缺失，
  因此行 1/行 2 永远拿不到状态，且 21081/24150 会在行 0 的 NPC 处直接进领奖态。
- 本批收敛 owner（接取只在接取 NPC、领奖只在行 2 NPC），并把行阶梯投影成
  started(0) -> s1(1) -> reward(2)；preview 只保留 USE_OBJECT，SELECT_QUEST_REWARD 由 reward 态
  `NPC_REPORT`（page=SELECT5）展开，避免同 NPC 同动作的 AMBIGUOUS_TRANSITION。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch40_three_npc_talk_ladder.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch40_three_npc_talk_ladder.py --apply
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_IDS = (11072, 21081, 24150)

FAMILY: dict[int, dict[str, object]] = {
	11072: {
		"accept_npc": 798937,
		"row0_npc": 798907,
		"row1_npc": 798960,
		"row2_npc": 798937,
		"accept_second_page": True,
		"accept_actions": "",
		"reward_inner": '      fixed-reward-indices="0 1 2" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">\n'
		                '      <preview actions="USE_OBJECT"/>',
		"note": "补给食品线：Seneca 委托、Borriello 发放、Delus 收下、回 Seneca 复命。",
	},
	21081: {
		"accept_npc": 799225,
		"row0_npc": 799332,
		"row1_npc": 799217,
		"row2_npc": 799202,
		"accept_second_page": False,
		"accept_actions": '      <accept-actions>\n        <give-item item-id="182214017" count="1"/>\n      </accept-actions>\n',
		"reward_inner": '      fixed-reward-indices="0" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">\n'
		                '      <preview actions="USE_OBJECT"/>',
		"note": "Gelkmaros 武器补给：Hler 发令、Agovard/Renato 会签、Sepsi 发武器。",
	},
	24150: {
		"accept_npc": 204702,
		"row0_npc": 204733,
		"row1_npc": 204734,
		"row2_npc": 204702,
		"accept_second_page": False,
		"accept_actions": '      <accept-actions>\n        <give-item item-id="182215460" count="1"/>\n      </accept-actions>\n',
		"reward_inner": '      fixed-reward-indices="0 1 2 3" complete-reward-index="0" finish="SELECTION_DIALOG">\n'
		                '      <choice action="SELECTED_QUEST_REWARD1" reward-index="4"/>\n'
		                '      <choice action="SELECTED_QUEST_REWARD2" reward-index="5"/>\n'
		                '      <preview actions="USE_OBJECT"/>',
		"note": "贝鲁斯兰要塞崩溃：Nerthus 委托、Bestla 诊断、Horu 装装置、回 Nerthus 复命。",
	},
}


def tidy(text: str) -> str:
	return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def metadata_block(text: str, quest_id: int) -> str:
	match = re.search(r"  <metadata .*?</metadata>\n", text, re.S)
	if match is None:
		raise SystemExit(f"BATCH40_ERROR {quest_id}: cannot locate metadata")
	return match.group(0)


def body(quest_id: int) -> str:
	spec = FAMILY[quest_id]
	accept_npc = spec["accept_npc"]
	row0_npc, row1_npc, row2_npc = spec["row0_npc"], spec["row1_npc"], spec["row2_npc"]
	accept_actions = spec["accept_actions"]
	second_page = (
		f"""    <!-- 接取对话第二页（客户端 select1 的按钮 SELECT1_1）。 / Second accept page. -->
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{accept_npc}" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
""" if spec["accept_second_page"] else "")
	return f"""  <!-- QE-051 行阶梯收口（批次 40）：客户端任务书三行、槽位 %0/%3/%6，状态 0/1/2 与行一一对应。
       {spec["note"]}
       Row ladder closure (batch 40): the accept chain, then one talking owner per journal row
       (select2/SETPRO1 for row 0, select3/SETPRO2 for row 1, select5/SELECT_QUEST_REWARD for row 2). -->
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
    <!-- 旧存档自愈：迁移前的 started -> reward 直跳落盘 REWARD/var0=0，把它提到行 2。
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
    <!-- 接取：只在接取 NPC 上。 / Accept only on the accept NPC. -->
    <dialog type="NPC_START" npc-id="{accept_npc}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1">
{accept_actions}    </dialog>
{second_page}    <!-- 行 0：和 {row0_npc} 对话（select2 -> select2_1 -> SETPRO1）。 / Row 0 owner. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{row0_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{row0_npc}" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{row0_npc}" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{row0_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：和 {row1_npc} 对话（select3 -> select3_1 -> SETPRO2）。 / Row 1 owner. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{row1_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{row1_npc}" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{row1_npc}" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{row1_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 2：和 {row2_npc} 对话（select5）+ 领奖窗口 1。 / Row 2 owner and reward. -->
    <dialog type="NPC_REPORT" npc-id="{row2_npc}" source="reward" target="reward" page="SELECT5"/>
    <npc-complete npc-id="{row2_npc}" source="reward" target="complete"
{spec["reward_inner"]}
    </npc-complete>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{row2_npc}" action="FINISH_DIALOG"/>
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
				print(f"BATCH40_OK {quest_id} already-applied")
			else:
				print(f"BATCH40_PENDING {quest_id} differs from target document")
				status = 1
			continue
		if current == document:
			print(f"BATCH40_OK {quest_id} already-applied")
			continue
		path.write_text(document, encoding="utf-8")
		print(f"BATCH40_APPLIED {quest_id}")
	return status


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
