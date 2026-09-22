#!/usr/bin/env python3
"""批次 36：活动“巧克力塔族 + 术古货箱族”的客户端任务书行阶梯重建（QE-051）。

族判据（2026-09-22，全部来自客户端解包与零售 quest_data）：
- 巧克力塔族（`select1` / `check_user_item_ok` 页链，客户端 collect_progress=0）：
  50020 / 80300 / 80301 / 80306 / 80307（50019/51019 已在批次 35 收口）。
  三行 = 行 0「消灭活动怪、收集巧克力交给术古」([%collectitem])、行 1「在爱情巧克力塔上使用工作物」、行 2「和术古对话」。
- 术古货箱族（`select4` / `select5` 页链，客户端 collect_progress=4、零售 collecting_step=4）：
  50021 / 51021 / 50022 / 51022 / 80302 / 80303 / 80308 / 80309。
  三行 = 行 0「消灭活动怪 ([%2]/3)」、行 1「和单身部队成员/恋人术古对话」、行 2「在术古货物箱里找到道具并交付」([%collectitem])。

客户端页链（HtmlPages.xml：select1=1011 / select4=2034 / select5=2375 / check_user_item_ok=10000 /
check_user_item_fail=10001 / select_success=10002）决定按钮动作归属：
- 塔族：select1 的 CHECK_USER_HAS_QUEST_ITEM（交出巧克力）推进行 0 -> 行 1；check_user_item_ok 的 SETPRO2 就地关闭；
  塔物件 (TALK_TO_NPC <塔> USE_OBJECT) 推进行 1 -> 行 2；行 2 的 QUEST_SELECT 打开 10002 领奖入口。
- 货箱族：select4 的 SETPRO2（结束对话）推进行 0 -> 行 1、行 1 -> 行 2；select5 的 CHECK_USER_HAS_QUEST_ITEM（拿出道具）
  在行 2 完成交付并打开 select_success(10002)；该页的 SELECT_QUEST_REWARD(1009) 打开奖励窗口，货箱的 ACTION_ITEM_USE 资格落在行 1（START + collecting-step=1），check_user_item_fail 的 FINISH_DIALOG 关闭。

已知边界（本批不改，登记待客户端验收）：
- 活动怪 219315/219316/219639/219640 与本族全部 NPC/物件（701466/701467/831402/831403、701470/701774、
  202549/799763）在本检出内都没有静态 spawn——整族内容由活动系统运行时下发，因此按批次 35 口径只重建行阶梯、
  不给活动怪加“未击杀即锁死”的门（客户端 quest_script 的 `Progress(0~2)` 击杀计数与引擎 var0=行号 的差异见报告）。
- 塔族行 1 的塔物件与货箱族行 2 的货箱同样由活动系统刷出，塔族保留 s1 的「直接找术古」防呆入口。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch36_event_row_ladder.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch36_event_row_ladder.py --apply
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

# 塔族 / Chocolate-tower family: (npc, collect item, tower object)
FAMILY_A: dict[int, tuple[int, int, int]] = {
	50020: (202549, 182215173, 701466),
	80300: (799763, 182215288, 831402),
	80301: (799763, 182215289, 831402),
	80306: (799763, 182215294, 831403),
	80307: (799763, 182215295, 831403),
}

# 货箱族 / Shugo cargo-box family: (npc, collect item, box object)
FAMILY_B: dict[int, tuple[int, int, int]] = {
	50021: (202549, 182215176, 701470),
	51021: (202549, 182215182, 701470),
	50022: (202549, 182215177, 701470),
	51022: (202549, 182215183, 701470),
	80302: (799763, 182215292, 701774),
	80303: (799763, 182215293, 701774),
	80308: (799763, 182215298, 701774),
	80309: (799763, 182215299, 701774),
}

HEADER_A = """<?xml version="1.0" encoding="UTF-8"?>
<!--
  {qid} 活动巧克力塔族：客户端任务书三行阶梯（QE-051 批次 36）。
  Client journal rows (quest_q{qid}.html): row 0 collect+hand-in ([%collectitem]), row 1 decorate the
  love chocolate tower ({tower}), row 2 report back to {npc}. Pages: select1 CHECK_USER_HAS_QUEST_ITEM ->
  check_user_item_ok SETPRO2 -> select_success SELECT_QUEST_REWARD; select_quest_reward1 = reward window.
  行 0「收集 {item} x3 交给 {npc}」-> 行 1「在爱情巧克力塔({tower})上使用工作物」-> 行 2「和 {npc} 对话」。
  Migrated definition collapsed rows 1/2 (npc-item-report straight into REWARD, tower never wired).
-->
"""

HEADER_B = """<?xml version="1.0" encoding="UTF-8"?>
<!--
  {qid} 术古货箱族：客户端任务书三行阶梯（QE-051 批次 36）。
  Client journal rows (quest_q{qid}.html): row 0 kill counter ([%2]/3), row 1 talk to the squad shugo,
  row 2 find the item in the cargo box ({box}) and hand it over ([%collectitem]). Pages: select4 SETPRO2
  (结束对话) -> select5 CHECK_USER_HAS_QUEST_ITEM (拿出道具) -> select_success SELECT_QUEST_REWARD.
  行 0「消灭活动怪 (/3)」-> 行 1「和术古对话」-> 行 2「在术古货物箱({box})里找到 {item} x3 交给 {npc}」。
  Migrated definition collapsed rows 1/2 and dropped the collect-step=4 gate of the box.
-->
"""

TALLY = """  <progress>
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
"""

HEAL = """    <!-- QE-051 自愈边：旧存档停在 REWARD 但 packed 行仍是 {row}，进入世界时补到领奖行 2。
         Heal edges: stale REWARD saves with packed row {row} are pushed to the reward row. -->
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="{row}"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
"""

ACCEPT = """    <!-- 接取：起始对话 / start dialogs -->
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT_NONE"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="ASK_QUEST_ACCEPT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_ASK_QUEST_ACCEPT_WINDOW"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_ACCEPT_1"/>
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
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_ACCEPT_SIMPLE"/>
      </event>
      <conditions>
        <start-eligible/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SETPRO1"/>
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
        <dialog type="TALK_TO_NPC" npc-id="{npc}" actions="QUEST_REFUSE_1 QUEST_REFUSE_2 QUEST_REFUSE_SIMPLE"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <dialog type="SHOW_SELECTION_PAGE" page="SELECT_QUEST"/>
      </after-commit>
    </transition>
"""

FAMILY_A_ROWS = """    <!-- 行 0：交出收集物（客户端 select1 页的“交出”按钮 = CHECK_USER_HAS_QUEST_ITEM），任务书切到行 1。
         Row 0 -> row 1: hand in the collected chocolate and move the journal to the tower row. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="{item}" count="3"/>
      </conditions>
      <actions>
        <remove-item item-id="{item}" count="3"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>
      </after-commit>
    </transition>
    <!-- check_user_item_ok 页的确认按钮（SETPRO2）：此刻已在行 1，就地关闭对话框，不再额外推进。 -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SETPRO2"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1 -> 行 2：在爱情巧克力塔上使用工作物（塔由活动系统刷出）。 -->
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{tower}" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 防呆：活动塔未刷出时直接找术古对话也能进入领奖行，避免卡在行 1。 -->
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
"""

FAMILY_B_ROWS = """    <!-- 行 0 -> 行 1：客户端 select4 页（结算击杀后 NPC 指向货箱的对话），SETPRO2 = 结束对话。
         Row 0 -> row 1: the client's select4 page ends the kill row. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT4"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1 -> 行 2：同一对话页在“和术古对话”行再确认一次，进入货箱取物行。 -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT4"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 收物行（行 1，START 态）：生产门禁 QuestInteractionObjectValidator 要求 quest_use_item 货箱的
         ACTION_ITEM_USE 资格路由落在 START 态且 var0 == collecting-step；零售 collecting_step=4 落在客户端第二组
         3 bit 槽位（3..5）即行 1，故收敛为 collecting-step=1。客户端 select5 页的“拿出道具”
         （CHECK_USER_HAS_QUEST_ITEM）在行 2 完成交付并打开 select_success(10002)。
         Loot row = row 1 (START) so the box's ACTION_ITEM_USE eligibility route carries collecting-step=1;
         the client's select5 check button hands the loot in on the reward row. -->
    <transition source="s1" target="s1">
      <event>
        <can-act template-id="{box}" action-type="ACTION_ITEM_USE"/>
      </event>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="{item}" count="3"/>
      </conditions>
      <actions>
        <remove-item item-id="{item}" count="3"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>
      </after-commit>
    </transition>
"""

TAIL_A = """    <!-- 领奖态入口页（10002）与 check_user_item_fail 的关闭按钮。 -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
  </transitions>
</quest-definition>
"""

TAIL_B = """    <!-- check_user_item_fail 的关闭按钮（10002 领奖入口页由交付成功的 CHECK 路由打开 = select_success）。 -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
  </transitions>
</quest-definition>
"""


def extract(text: str, pattern: str, what: str, quest_id: int) -> str:
	match = re.search(pattern, text, re.S)
	if match is None:
		raise SystemExit(f"BATCH36_ERROR {quest_id}: cannot locate {what}")
	return match.group(0)


def settlement_block(text: str, quest_id: int) -> str:
	"""保留各任务自己的奖励结算块（npc-complete 或手写 reward 结算 transition）。"""
	matched = re.search(r"\n    <npc-complete .*?</npc-complete>\n", text, re.S)
	if matched is not None:
		return matched.group(0)
	block = ""
	for pattern in (r"\n    <transition source=\"reward\" target=\"reward\">\n"
	                r"      <event>\n"
	                r"        <dialog type=\"TALK_TO_NPC\" npc-id=\"\d+\" "
	                r"actions=\"USE_OBJECT SELECT_QUEST_REWARD\"/>\n"
	                r"      </event>[\s\S]*?</transition>\n",
	                r"\n    <transition source=\"reward\" target=\"complete\">[\s\S]*?</transition>\n"):
		block += extract(text, pattern, "reward settlement transition", quest_id)
	if not block:
		raise SystemExit(f"BATCH36_ERROR {quest_id}: no reward settlement block")
	return block


def build_family_a(quest_id: int, text: str) -> str:
	npc, item, tower = FAMILY_A[quest_id]
	metadata = extract(text, r"  <metadata .*?</metadata>\n", "metadata", quest_id)
	return (HEADER_A.format(qid=quest_id, item=item, npc=npc, tower=tower)
	        + f'<quest-definition id="{quest_id}" version="1">\n'
	        + metadata + TALLY
	        + HEAL.format(row=0) + HEAL.format(row=1)
	        + ACCEPT.format(npc=npc)
	        + FAMILY_A_ROWS.format(npc=npc, item=item, tower=tower)
	        + settlement_block(text, quest_id)
	        + TAIL_A.format(npc=npc))


def build_family_b(quest_id: int, text: str) -> str:
	npc, item, box = FAMILY_B[quest_id]
	metadata = extract(text, r"  <metadata .*?</metadata>\n", "metadata", quest_id)
	metadata = re.sub(r'collecting-step="\d+"', 'collecting-step="1"', metadata)
	return (HEADER_B.format(qid=quest_id, item=item, npc=npc, box=box)
	        + f'<quest-definition id="{quest_id}" version="1">\n'
	        + metadata + TALLY
	        + HEAL.format(row=0) + HEAL.format(row=1)
	        + ACCEPT.format(npc=npc)
	        + FAMILY_B_ROWS.format(npc=npc, item=item, box=box)
	        + settlement_block(text, quest_id)
	        + TAIL_B.format(npc=npc))


def main(argv: list[str]) -> int:
	apply = "--apply" in argv
	check = "--check" in argv or not apply
	status = 0
	for quest_id in sorted(FAMILY_A) + sorted(FAMILY_B):
		path = QUESTS / f"{quest_id}.xml"
		current = path.read_text(encoding="utf-8")
		builder = build_family_a if quest_id in FAMILY_A else build_family_b
		document = builder(quest_id, current)
		if check:
			if current == document:
				print(f"BATCH36_OK {quest_id} already-applied")
			else:
				print(f"BATCH36_PENDING {quest_id} differs from target document")
				status = 1
			continue
		if current == document:
			print(f"BATCH36_OK {quest_id} already-applied")
			continue
		path.write_text(document, encoding="utf-8")
		print(f"BATCH36_APPLIED {quest_id}")
	return status


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
