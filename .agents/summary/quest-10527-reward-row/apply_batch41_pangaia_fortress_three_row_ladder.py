#!/usr/bin/env python3
"""批次 41：潘盖亚要塞战三行族（14220 天族 / 24220 魔族）的行/状态收口（QE-051）。

族判据（2026-09-22，客户端 quest_summary + HTML 页链 + 客户端 NPC 名表）：
- 14220/24220 客户端任务书都是三行、槽位 %0/%3/%6：
  - 14220 行 0「和 GAb1_Astarin_E 对话」、行 1「参加潘盖亚要塞战，和 GAb1_Ag_all 对话」、
    行 2「和 GAb1_Carley_E 对话」；
  - 24220 行 0「和 GAb1_Krondel_E 对话」、行 1 同上、行 2「和 GAb1_Leaivink_E 对话」。
- 页链（`Dialogs/10000_19999/quest_q14220.html` 与 `20000_29999/quest_q24220.html` 一致）：
  `select_none`（按钮 QUEST_ACCEPT_SIMPLE/QUEST_REFUSE_SIMPLE，本文点名“去见见 Astarin/Krondel”）
  -> 行 0 的 `select1`（按钮 SETPRO1）
  -> 行 1 的 `select2`（按钮 SELECT2_1）-> `select2_1`（按钮 SETPRO2）
  -> 行 2 的 `select_success`(10002)（按钮 SELECT_QUEST_REWARD）-> `select_quest_reward1` 领奖页。
- owner（客户端 npcs_unpacked/client_npcs_npc.xml + strings_unpacked/client_strings_dic_etc.xml）：
  14220 接取/报告 = Carley 802540、行 0 = Astarin 802541；
  24220 接取/报告 = Leaivink 802542、行 0 = Krondel 802543；
  行 1 的 `STR_DIC_N_GAb1_Ag_all` 是“潘盖亚情报员”，按入口位置有 4 个变体
  （GAb1_01_BelosAg01_E=802544 / GAb1_02_AspidaAg01_E=802545 / GAb1_03_AthantosAg01_E=802546 /
  GAb1_04_DysilonAg01_E=802547，race=BROWNIE 中立），两个任务共用。
- 旧定义把行 1/行 2 全挂到 802540/802542 上：`NPC_REPORT started->reward page=SELECT2` +
  `SETPRO2 -> reward` 直跳，行 0 的 Astarin/Krondel 与 Ag_all 完全没有路由，
  行 1/行 2 永远拿不到状态。
- 本批把行阶梯投影成 started(0) -> s1(1) -> reward(2)：Astarin/Krondel 的 SETPRO1 推进到行 1，
  四个 Ag_all 变体的 SETPRO2 推进到行 2，行 2 由接取 NPC 的 `select_success` + 奖励窗口 1 收口。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch41_pangaia_fortress_three_row_ladder.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch41_pangaia_fortress_three_row_ladder.py --apply
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_IDS = (14220, 24220)

# 行 1 的“潘盖亚情报员”按要塞战入口位置分 4 个中立变体（BROWNIE）。
AG_ALL_VARIANTS = (802544, 802545, 802546, 802547)

FAMILY: dict[int, dict[str, int]] = {
	14220: {"accept_npc": 802540, "row0_npc": 802541},
	24220: {"accept_npc": 802542, "row0_npc": 802543},
}

REWARD_INNER = ('      fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" '
                'complete-reward-index="0" finish="SELECTION_DIALOG">\n'
                '      <preview actions="USE_OBJECT"/>')


def tidy(text: str) -> str:
	return "\n".join(line.rstrip() for line in text.splitlines()) + "\n"


def metadata_block(text: str, quest_id: int) -> str:
	match = re.search(r"  <metadata .*?</metadata>\n", text, re.S)
	if match is None:
		raise SystemExit(f"BATCH41_ERROR {quest_id}: cannot locate metadata")
	return match.group(0)


def ag_all_routes() -> str:
	blocks = []
	for npc_id in AG_ALL_VARIANTS:
		blocks.append(f"""    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
""")
	return "".join(blocks)


def body(quest_id: int) -> str:
	spec = FAMILY[quest_id]
	accept_npc, row0_npc = spec["accept_npc"], spec["row0_npc"]
	faction = "天族" if quest_id == 14220 else "魔族"
	return f"""  <!-- QE-051 行阶梯收口（批次 41）：潘盖亚要塞战三行族（{faction}）。
       客户端任务书三行、槽位 %0/%3/%6：行 0 与行 0 NPC 对话（select1/SETPRO1）、
       行 1 与 4 个“潘盖亚情报员”变体之一对话（select2/SELECT2_1/SETPRO2）、
       行 2 回接取 NPC 报告（select_success/SELECT_QUEST_REWARD）。
       Row ladder closure (batch 41): one state per journal row of the Pangaia fortress family. -->
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
    <!-- 接取：{accept_npc} 的 select_none 页（“去见见”行 0 的 NPC）。 / Accept page. -->
    <dialog type="NPC_START" npc-id="{accept_npc}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <!-- 行 0：和 {row0_npc} 对话（select1 -> SETPRO1）。 / Row 0 owner. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{row0_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
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
    <!-- 行 1：和“潘盖亚情报员”4 个变体之一对话（select2 -> select2_1 -> SETPRO2）。 / Row 1 owners. -->
{ag_all_routes()}    <!-- 行 2：回 {accept_npc} 报告（select_success）+ 奖励窗口 1。 / Row 2 owner. -->
    <dialog type="NPC_REPORT" npc-id="{accept_npc}" source="reward" target="reward" page="DEFAULT_SUCCESS"/>
    <npc-complete npc-id="{accept_npc}" source="reward" target="complete"
{REWARD_INNER}
    </npc-complete>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{accept_npc}" action="FINISH_DIALOG"/>
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
				print(f"BATCH41_OK {quest_id} already-applied")
			else:
				print(f"BATCH41_PENDING {quest_id} differs from target document")
				status = 1
			continue
		if current == document:
			print(f"BATCH41_OK {quest_id} already-applied")
			continue
		path.write_text(document, encoding="utf-8")
		print(f"BATCH41_APPLIED {quest_id}")
	return status


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
