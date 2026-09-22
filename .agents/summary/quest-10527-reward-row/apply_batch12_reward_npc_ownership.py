#!/usr/bin/env python3
"""批次 12：领奖 NPC 归属核实族 + 30614 报告行收口。

本脚本可重放、幂等，默认直接改写；`--check` 只验证目标状态。

修复对象：
- 19064 / 29064：客户端 2 行（行 0 与 Undin/Darfen 对话、行 1 与 Jucleas/Balder 对话），
  retail `end_npc_ids=203752/204075`、legacy handler 在 798450/798452 的 STEP_TO_1 写 var0=1；
  旧 typed 定义把领奖/completion 放在起始 NPC 203701/204053，且 reward 投影停在 0。
  修复：新增 s1(START,var0=1)、把领奖/completion 改到 203752/204075、reward 投影改 1，
  旧存档 reward var0=0/2 自愈到 1。
- 21455：retail `end_npc_ids=799244` 与 legacy `_21455Ingredients_For_The_Antidote` 都确认
  领奖 NPC 是 799244（客户端末行“交给 Unset”）；旧 typed 把领奖/completion 放在 799404。
  同时把换物品动作移到客户端 select2_1 页的 SETPRO1 路由，删除没有客户端按钮的 SETPRO2 死路由。
- 30614：客户端 quest_summary 只有 2 行（战斗行 0 + 向 Astella 报告行 1），旧 reward 投影停在
  行 0；领奖 NPC 归属核实为 Astella=800327（客户端行 1 的命名 NPC 与同文件 quest_complete
  「阿斯泰拉说…」一致，同族 30610/30611/30612/30613 的「客户端命名 NPC ↔ 定义 NPC」4/4
  一致，2026-08-06 客户端/真端交叉审计 f737cfef1 亦使用 800327）；legacy contract
  `terath_dredgion.xml` 的 start_npc_ids=800326 是单一派生源，2026-09-13 的自动对齐
  10a2e7e57 仅据此把 800327 改成 800326。本修订恢复 800327，并补 enter-world 自愈边与
  reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS 入口页。

Batch 12: reward-NPC ownership audit family plus the 30614 report-row fix. Idempotent and
replayable; `--check` verifies the target contract without writing.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

CONSTRUCTION_NODES = """  <nodes>
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
      <var name="var0" value="1"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>"""

CONSTRUCTION_TRANSITIONS = """  <transitions>
    <!-- QE-051 + 领奖 NPC 归属：客户端 quest_q{quest_id}.html 只有 2 行 —
         行 0 和 {mid_npc_name}({mid_npc}) 对话（select2/1352），行 1 带着徽章和圣物与
         {end_npc_name}({end_npc}) 对话（select5/2375）。retail data_driven_quest
         end_npc_ids={end_npc}、step0 TALK {mid_npc}、step1 COLLECT_ITEM {end_npc}；
         legacy handler 在 {mid_npc} 的 STEP_TO_1 写 var0=1、在 {end_npc}
         用 checkQuestItemsSimple(1,2,true,5) 进入 REWARD。reward 投影按 QE-051 固定为
         客户端领奖行 1；旧存档 reward var0=0（旧 typed 投影）与 var0=2（旧 handler 的 +1
         语义）在进入世界时自愈到 1。客户端没有 DEFAULT_SUCCESS(10002) 页，领奖态入口直接
         打开 select_quest_reward1(5) 奖励窗口。
         Reward-row and reward-NPC contract (QE-051): the two client rows are {mid_npc_name}
         then {end_npc_name}; retail end_npc_ids={end_npc} matches the legacy handler
         (var0 0->1 at {mid_npc}, completion at {end_npc}). Stale reward rows 0/2 heal to 1. -->
    <transition target="reward">
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
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="2"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <dialog type="NPC_START" npc-id="{start_npc}" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1"/>
    <!-- 行 0：{mid_npc_name}({mid_npc})。legacy handler: START_DIALOG -> 1352 / STEP_TO_1 -> var0 0->1 -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{mid_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{mid_npc}" action="SETPRO1"/>
      </event>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：{end_npc_name}({end_npc})。legacy handler: var0==1 -> 2375；CHECK_COLLECTED_ITEMS_SIMPLE -> REWARD + 奖励窗口 -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{end_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{end_npc}" action="CHECK_USER_HAS_QUEST_ITEM_SIMPLE"/>
      </event>
      <conditions>
        <has-item item-id="{item1}" count="1"/>
        <has-item item-id="{item2}" count="1"/>
      </conditions>
      <actions>
        <remove-item item-id="{item1}" count="1"/>
        <remove-item item-id="{item2}" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{end_npc}" action="CHECK_USER_HAS_QUEST_ITEM_SIMPLE"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{end_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 领奖态入口：客户端没有 DEFAULT_SUCCESS 页，点任务行直接打开奖励窗口 -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{end_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{end_npc}" action="SELECT_QUEST_REWARD"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="{end_npc}" source="reward" target="complete" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>"""

CONSTRUCTION_CASES = {
    19064: dict(start_npc=203701, mid_npc=798450, end_npc=203752,
                mid_npc_name="Undin", end_npc_name="Jucleas",
                item1=182213237, item2=186000081),
    29064: dict(start_npc=204053, mid_npc=798452, end_npc=204075,
                mid_npc_name="Darfen", end_npc_name="Balder",
                item1=182213239, item2=186000085),
}

SETPRO2_ROUTE = re.compile(
    r'\n    <transition source="started" target="started">\n'
    r'      <event>\n'
    r'        <dialog type="TALK_TO_NPC" npc-id="799240" action="SETPRO2"/>\n'
    r'      </event>.*?</transition>', re.S)

SETPRO1_ROUTE_21455 = re.compile(
    r'    <transition source="started" target="reward">\n'
    r'      <event>\n'
    r'        <dialog type="TALK_TO_NPC" npc-id="799240" action="SETPRO1"/>\n'
    r'      </event>\n'
    r'      <after-commit>\n'
    r'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
    r'        <close-dialog/>\n'
    r'      </after-commit>\n'
    r'    </transition>')

SETPRO1_ROUTE_21455_NEW = """    <transition source="started" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799240" action="SETPRO1"/>
      </event>
      <conditions>
        <has-item item-id="182209514" count="1"/>
      </conditions>
      <actions>
        <give-item item-id="182209515" count="1"/>
        <remove-item item-id="182209514" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>"""

SELECT5_ON_START_ROUTE_21455 = re.compile(
    r'\n    <transition source="started" target="started">\n'
    r'      <event>\n'
    r'        <dialog type="TALK_TO_NPC" npc-id="799404" action="QUEST_SELECT"/>\n'
    r'      </event>\n'
    r'      <after-commit>\n'
    r'        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>\n'
    r'      </after-commit>\n'
    r'    </transition>')

REWARD_SELECT_21455_NEW = """    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799244" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
"""


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def construction_ok(text: str, case: dict[str, object]) -> bool:
    return (f'<node label="s1" status="START">' in text
            and f'<node label="reward" status="REWARD">\n      <var name="var0" value="1"/>' in text
            and f'<dialog type="TALK_TO_NPC" npc-id="{case["mid_npc"]}" action="SETPRO1"/>' in text
            and f'<dialog type="TALK_TO_NPC" npc-id="{case["end_npc"]}" action="CHECK_USER_HAS_QUEST_ITEM_SIMPLE"/>' in text
            and f'<npc-complete npc-id="{case["end_npc"]}"' in text
            and '<variable-is field="var0" value="2"/>' in text)


def apply_construction(quest_id: int, case: dict[str, object], check: bool) -> bool:
    path = QUESTS / f"{quest_id}.xml"
    text = read(path)
    if check:
        return construction_ok(text, case)
    if construction_ok(text, case):
        return True
    nodes = CONSTRUCTION_NODES
    transitions = CONSTRUCTION_TRANSITIONS.format(quest_id=quest_id, **case)
    new_text = re.sub(r"  <nodes>.*?</nodes>", nodes, text, count=1, flags=re.S)
    new_text = re.sub(r"  <transitions>.*?</transitions>", transitions, new_text,
                      count=1, flags=re.S)
    path.write_text(new_text, encoding="utf-8")
    return construction_ok(new_text, case)


def contract_21455_ok(text: str) -> bool:
    return ('action="SETPRO2"' not in text
            and '领奖 NPC 归属' in text
            and '<npc-complete npc-id="799244"' in text
            and 'npc-id="799404" action="SELECT_QUEST_REWARD"' not in text
            and 'npc-id="799404" action="QUEST_SELECT"' not in text
            and 'npc-id="799244" action="QUEST_SELECT"' in text
            and 'action="SETPRO1"/>\n      </event>\n      <conditions>\n'
                '        <has-item item-id="182209514" count="1"/>' in text)


HEADER_21455_NEW = """    <!-- QE-051 领奖行 + 领奖 NPC 归属：客户端 quest_q21455.html 的 quest_summary 共 2 行 —
         行 0「在果实失效之前，交给 Schiemann」、行 1「把奥德解毒剂交给 Unset」。retail
         end_npc_ids=799244 与 legacy `_21455Ingredients_For_The_Antidote`（799240 STEP_TO_1
         换物品、799244 进 REWARD）一致；客户端 select2_1 的 HACTION_SETPRO1 与 select5 的
         CHECK_USER_HAS_QUEST_ITEM_SIMPLE 也与该归属吻合，accept 仍是 799404（Miener）。末行
         旧投影停在 0，属同族 249 个 retail 单步任务中 184 个已对齐后的落后项；进入世界时把旧存档
         纠正为 1 并下发状态包。
         Reward-row and reward-NPC ownership (QE-051): the second journal row hands the antidote to
         Unset(799244), matching retail end_npc_ids=799244 and the legacy handler (item swap at
         799240, REWARD at 799244); the accept NPC stays Miener(799404). The old projection stopped
         at row 0 while 184 of 249 sibling retail single-step quests already project 1, and saves
         persisted at 0 are repaired on enter-world. -->
"""


def apply_21455(check: bool) -> bool:
    path = QUESTS / "21455.xml"
    text = read(path)
    if check:
        return contract_21455_ok(text)
    if contract_21455_ok(text):
        return True
    new_text = SETPRO2_ROUTE.sub("", text, count=1)
    new_text = SETPRO1_ROUTE_21455.sub(SETPRO1_ROUTE_21455_NEW, new_text, count=1)
    new_text = new_text.replace(
        '<dialog type="TALK_TO_NPC" npc-id="799404" action="SELECT_QUEST_REWARD"/>',
        '<dialog type="TALK_TO_NPC" npc-id="799244" action="SELECT_QUEST_REWARD"/>')
    new_text = new_text.replace(
        '<npc-complete npc-id="799404"', '<npc-complete npc-id="799244"')
    new_text = SELECT5_ON_START_ROUTE_21455.sub("", new_text, count=1)
    new_text = re.sub(r'    <!-- QE-051 领奖行.*?-->',
                      HEADER_21455_NEW.rstrip("\n"), new_text, count=1, flags=re.S)
    marker = '    <npc-complete npc-id="799244"'
    if REWARD_SELECT_21455_NEW not in new_text:
        new_text = new_text.replace(marker, REWARD_SELECT_21455_NEW + marker, 1)
    path.write_text(new_text, encoding="utf-8")
    return contract_21455_ok(new_text)


COMMENT_30614 = """    <!-- QE-051 领奖行 + 领奖 NPC 归属：客户端 quest_q30614.html 只有 2 行（战斗行 0 + 报告行 1
         「向 Astella 报告」），旧 reward 投影停在行 0。领奖 NPC 核实为 Astella=800327：
         ① 客户端行 1 直指 Astella；② 同文件 quest_complete「阿斯泰拉说必须…」与同族
         30610/30611/30612/30613 的「客户端命名 NPC ↔ 定义 NPC」4/4 一致；③ 2026-08-06
         客户端/真端交叉审计 f737cfef1 即使用 800327。legacy contract terath_dredgion.xml 的
         start_npc_ids=800326 是单一派生源（该文件对 30610 也丢掉了中间的 Aluna 对话步骤），
         10a2e7e57 仅据此把 800327 改成 800326，本修订恢复 800327。旧存档 REWARD/var0=0
         在进入世界时自愈到 1。
         Reward-row and reward-NPC ownership (QE-051): the client journal's second row reports to
         Astella and quest_complete names the same NPC; siblings 30610-30613 confirm the naming
         convention and the 2026-08-06 client/live-server cross audit already used 800327. The
         legacy contract is a single derived source and was the only input of the 2026-09-13
         automated pass that broke it, so this repair restores 800327 for start, report,
         completion and the reward entry page. -->
"""


def contract_30614_ok(text: str) -> bool:
    return ('<var name="var0" value="1"/>\n      <var name="var1" value="6"/>' in text
            and 'enter-world/>' in text
            and '<dialog type="NPC_START" npc-id="800327"' in text
            and '<dialog type="NPC_REPORT" npc-id="800327"' in text
            and '<npc-complete npc-id="800327"' in text
            and 'npc-id="800327" action="QUEST_SELECT"' in text
            and 'page="DEFAULT_SUCCESS"' in text
            and 'npc-id="800326"' not in text
            and text.count('领奖态入口页') == 1
            and '      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>\n    <!-- 领奖态入口页' not in text)


def apply_30614(check: bool) -> bool:
    path = QUESTS / "30614.xml"
    text = read(path)
    if check:
        return contract_30614_ok(text)
    if contract_30614_ok(text):
        return True
    reward_old = ('    <node label="reward" status="REWARD">\n'
                  '      <var name="var0" value="0"/>\n'
                  '      <var name="var1" value="6"/>\n'
                  '      <var name="var2" value="15"/>\n'
                  '    </node>')
    reward_new = ('    <node label="reward" status="REWARD">\n'
                  '      <var name="var0" value="1"/>\n'
                  '      <var name="var1" value="6"/>\n'
                  '      <var name="var2" value="15"/>\n'
                  '    </node>')
    new_text = text.replace(reward_old, reward_new, 1)
    # 领奖 NPC 归属：撤回 10a2e7e57 仅依据 legacy contract 单源做的 800327 -> 800326 对齐。
    # Reward-NPC ownership: undo the single-source (legacy contract) 800327 -> 800326 alignment.
    new_text = new_text.replace('npc-id="800326"', 'npc-id="800327"')
    new_text = re.sub(r'    <!-- QE-051 领奖行合同：客户端 quest_q30614\.html.*?-->',
                      COMMENT_30614.rstrip("\n"), new_text, count=1, flags=re.S)
    heal = COMMENT_30614 + """    <transition target="reward">
    <transition target="reward">
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
"""
    report_marker = '    <dialog type="NPC_REPORT" npc-id="800327" source="a0b6c15" target="reward" page="DEFAULT_SUCCESS"/>'
    if report_marker in new_text and 'enter-world/>\n      </event>\n      <conditions>\n        <status-is status="REWARD"/>\n        <variable-is field="var0" value="0"/>' not in new_text:
        new_text = new_text.replace(report_marker, heal + report_marker, 1)
    entry = """    <!-- 领奖态入口页：reward + 800327 QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002) -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="800327" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
"""
    # 若旧版本曾把入口页误插进 npc-complete 内部，先移出，再插到完成块之后。
    # If an earlier revision placed the entry route inside npc-complete, move it after the block.
    new_text = new_text.replace(
        '    <!-- 领奖态入口页：reward + 800326 QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002) -->',
        '    <!-- 领奖态入口页：reward + 800327 QUEST_SELECT(31) -> DEFAULT_SUCCESS(10002) -->')
    # 同一入口页只允许出现一次；旧修订曾因注释文本不同而重复插入。
    # The reward entry page must appear exactly once; an earlier revision duplicated it.
    while new_text.count(entry) > 1:
        new_text = new_text.replace(entry, '', 1)
    misplaced = '      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>\n' + entry
    if misplaced in new_text:
        new_text = new_text.replace(
            misplaced,
            '      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>\n', 1)
    complete_end = '    </npc-complete>'
    if entry not in new_text:
        new_text = new_text.replace(complete_end, complete_end + '\n' + entry.rstrip('\n'), 1)
    path.write_text(new_text, encoding="utf-8")
    return contract_30614_ok(new_text)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="只验证目标状态，不写文件")
    args = parser.parse_args()

    results = []
    for quest_id, case in CONSTRUCTION_CASES.items():
        results.append((quest_id, apply_construction(quest_id, case, args.check)))
    results.append((21455, apply_21455(args.check)))
    results.append((30614, apply_30614(args.check)))

    for quest_id, ok in results:
        print(f"BATCH12_{'CHECK' if args.check else 'APPLY'}_{'OK' if ok else 'FAIL'} quest={quest_id}")
    return 0 if all(ok for _, ok in results) else 1


if __name__ == "__main__":
    sys.exit(main())
