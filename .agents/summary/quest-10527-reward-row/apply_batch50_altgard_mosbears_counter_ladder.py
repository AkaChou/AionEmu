#!/usr/bin/env python3
"""批次 50：2289（Rampaging Mosbears / 巴斯佩尔特村的棕熊）恢复击杀计数行 + 三行对话阶梯。

证据链 / Evidence chain:
- 客户端 quest_q2289.html 的 quest_summary 共 4 行：行 0 = 消灭 MosbearS_13/14（计数槽 [%2]/5），
  行 1 = 回巴斯佩尔特村找 Gefion（203616），行 2 = 从 Skanin（203618）处获取情报，
  行 3 = 杀掉 MosbearNamed_17_An 并把角带给 Gefion（[%collectitem]）。
- 客户端 quest_script_monster.csv：`2289,Progress(0~4),,killedByUser,,1,MosbearS_13_An` 与
  `...MosbearS_14_An` —— 击杀行占 SECTION_0 = 0..4（五次击杀），第 5 次把 var0 推到 5；
  客户端 quest.xml 的 `<collect_progress>7</collect_progress>` 把收物行钉在 step 7。
- 迁移前 handler `_2289RampagingMosbears`（origin/history）用同一条阶梯：
  `defaultOnKillEvent(env, {210564,210584}, 0, 5)` 把 var0 推到 5；Gefion 在 var0==5 显示 1352，
  `STEP_TO_2` = defaultCloseDialog(5, 6)；Skanin 在 var0==6 显示 1693，`STEP_TO_3` =
  defaultCloseDialog(6, 7, 182203017, 1, 0, 0)（给 Hunter's Secret Remedy）；Gefion 在 var0==7 显示 2034，
  `CHECK_COLLECTED_ITEMS` = checkQuestItems(7, 7, true, 5, 2120)（成功页 5 = 奖励窗、失败页 2120 = select4_2）。
- 迁移把整条击杀阶梯丢掉、reward 投影停在 var0=0，行 1/2/3 永远不亮
  （审计 ROW_BEHIND | MISSING_TAIL_ROWS | ROW_WITHOUT_STATE，visible=0）；同一时段 `<drops>` 的
  collecting-step 仍是 0，角（182203016）在击杀行就能掉。

参考先例 / Precedents: 2303（客户端 Progress(11~14)/(15) 与 legacy var0 11..15 同口径）、
10101/20101（Progress(2~!4) 单变量阶梯 + 奖励行投影 = 最后一行）、QE-012 判定规则（SECTION_n = 6n）。
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

QUEST = Path('src/main/resources/aion/data/static_data/quest_definition/quests/2289.xml')

HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<!--
  2289 巴斯佩尔特村的棕熊：击杀计数行（0..4）+ 三行对话阶梯（批次 50，QE-012/QE-051）。
  Client journal (quest_q2289.html, 4 rows): row 0 kills MosbearS_13/14 ([%2]/5), row 1 reports to
  Gefion, row 2 takes Skanin's intel, row 3 kills MosbearNamed and brings its horn to Gefion
  ([%collectitem]). quest_script_monster declares Progress(0~4) for both MosbearS mobs and quest.xml
  declares collect_progress=7, so the kill row owns SECTION_0 0..4 (five kills) and the collect row is
  step 7, the same ladder the legacy handler walked. The migrated definition dropped the kill phase
  and projected REWARD at var0=0, so rows 1..3 could never light up.
-->
"""

NEW_METADATA_ADDITIONS = """    <work-items>
      <item id="182203017" count="1"/>
    </work-items>
"""

NEW_NODES = """  <nodes>
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
    <node label="s4" status="START">
      <var name="var0" value="4"/>
    </node>
    <node label="s5" status="START">
      <var name="var0" value="5"/>
    </node>
    <node label="s6" status="START">
      <var name="var0" value="6"/>
    </node>
    <node label="s7" status="START">
      <var name="var0" value="7"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="7"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
"""

NEW_TRANSITIONS = """  <transitions>
    <!-- 自愈边：迁移期 reward 投影停在 0，领奖态旧存档进世界时补到收物/领奖行 7。
         Heal edge: the migrated REWARD projection stopped at 0, so stale reward saves move to step 7. -->
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="7"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <!-- 自愈边：迁移期 collecting-step=0 让角在击杀行就能掉，已持角（182203016）的 START 存档
         进世界时直接落到收物行 7，避免修复后必须重打五次。
         Heal edge: with the old collecting-step=0 the horn could drop during the hunt, so a START save
         already holding it moves to the collect row instead of redoing the five kills. -->
    <transition target="s7">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="START"/>
        <has-item item-id="182203016" count="1"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="7"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <!-- 接取：Gefion（203616）。客户端页链 select1(1011) -> ask_quest_accept(4) -> quest_accept_1(1003)
         的按钮 HACTION_SETPRO1(10000) 只关窗，不推进击杀行。
         Offer stays on Gefion; the accept chain's SETPRO1 button only closes the window. -->
    <dialog type="NPC_START" npc-id="203616" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1"/>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203616" action="SETPRO1"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 行 0：五次击杀。客户端 quest_script 声明 Progress(0~4)，计数在 SECTION_0（var0）：
         第 1..4 次只回包（计数显示刷新），第 5 次把 var0 推到 5 并刷新可见性，任务书切到行 1。
         Row 0: five kills accumulate SECTION_0; the fifth kill moves the journal to row 1. -->
    <transition source="started" target="s1">
      <event>
        <kill-npc npc-ids="210564 210584"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s2">
      <event>
        <kill-npc npc-ids="210564 210584"/>
      </event>
      <conditions>
        <variable-is field="var0" value="1"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
    <transition source="s2" target="s3">
      <event>
        <kill-npc npc-ids="210564 210584"/>
      </event>
      <conditions>
        <variable-is field="var0" value="2"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="3"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
    <transition source="s3" target="s4">
      <event>
        <kill-npc npc-ids="210564 210584"/>
      </event>
      <conditions>
        <variable-is field="var0" value="3"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="4"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
    <transition source="s4" target="s5">
      <event>
        <kill-npc npc-ids="210564 210584"/>
      </event>
      <conditions>
        <variable-is field="var0" value="4"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="5"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>

    <!-- 行 1：回村找 Gefion 汇报。页链 select2(1352) -> select2_1(1353) -> select2_1_1(1354)，
         最后一段先播影片 62（legacy SELECT_ACTION_1354 = playQuestMovie(62) + sendQuestDialog(1354)），
         页 1354 的按钮 HACTION_SETPRO2(10001) 把 var0 推到 6。
         Row 1: report to Gefion; the 1354 page-turn plays movie 62 and SETPRO2 moves to step 6. -->
    <transition source="s5" target="s5">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203616" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="s5" target="s5">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203616" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="s5" target="s5">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203616" action="SELECT2_1_1"/>
      </event>
      <after-commit>
        <play-movie movie-id="62"/>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1_1"/>
      </after-commit>
    </transition>
    <transition source="s5" target="s6">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203616" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 行 2：被关着的 Skanin（203618）。页链 select3(1693) -> select3_1(1694)，
         按钮 HACTION_SETPRO3(10002) 把 var0 推到 7 并给出 Hunter's Secret Remedy（182203017）。
         Row 2: Skanin's intel; SETPRO3 moves to step 7 and hands over the remedy 182203017. -->
    <transition source="s6" target="s6">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203618" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="s6" target="s6">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203618" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="s6" target="s7">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203618" action="SETPRO3"/>
      </event>
      <actions>
        <give-item item-id="182203017" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 行 3：收物行。Gefion 的 select4(2034) 只有 HACTION_CHECK_USER_HAS_QUEST_ITEM(39)；
         持角时消耗并进 REWARD（成功页 5 = 奖励窗），缺角时回 select4_2(2120)，
         该页唯一的 HACTION_FINISH_DIALOG(1008) 在收物行关窗。
         Row 3: hand the horn to Gefion; CHECK_USER_HAS_QUEST_ITEM opens the reward window and
         select4_2's FINISH_DIALOG closes the dialog while staying on the collect row. -->
    <transition source="s7" target="s7">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203616" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT4"/>
      </after-commit>
    </transition>
    <npc-item-report npc-id="203616" source="s7" target="reward" item-id="182203016" required="1" failure-page="SELECT4_2"/>
    <transition source="s7" target="s7">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203616" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>

    <!-- 领奖行：REWARD 态与 Gefion 对话重开奖励窗（客户端只有 select_quest_reward1 = 页 5，
         没有 select_success）；奖励按钮由 npc-complete 预览承接，owner 收敛到 Gefion。
         Reward row: Gefion owns the only completion; the reward window button stays on npc-complete. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203616" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="203616" source="reward" target="complete" fixed-reward-indices="0 1 2" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
"""


def apply_sections(current: str) -> str:
    if '<work-items>' in current:
        raise SystemExit('FAIL: 2289 已经包含 work-items，请人工确认 / work-items already present')
    if 'collecting-step="0"' not in current:
        raise SystemExit('FAIL: 2289 的 drops 没有 collecting-step="0" 前置条件 / pre-condition missing')
    if '<dialog type="NPC_REPORT" npc-id="203616" source="started" target="reward" page="SELECT2"/>' not in current:
        raise SystemExit('FAIL: 2289 不是迁移期折叠形态 / unexpected pre-state')

    updated, header_replaced = re.subn(r'\A<\?xml version="1\.0" encoding="UTF-8"\?>\n',
                                       HEADER, current, count=1)
    if header_replaced != 1:
        raise SystemExit('FAIL: 2289 头部未替换 / header was not replaced')

    updated, items_replaced = re.subn(r'(    </items>\n)',
                                      r'\1' + NEW_METADATA_ADDITIONS, updated, count=1)
    if items_replaced != 1:
        raise SystemExit('FAIL: 2289 items 段未定位 / items block was not located')

    updated, drop_replaced = re.subn(r'collecting-step="0"', 'collecting-step="7"', updated, count=1)
    if drop_replaced != 1:
        raise SystemExit('FAIL: 2289 collecting-step 未改写 / collecting-step was not rewritten')

    updated, nodes_replaced = re.subn(r'  <nodes>.*?</nodes>', NEW_NODES.rstrip('\n'), updated,
                                      count=1, flags=re.S)
    if nodes_replaced != 1:
        raise SystemExit('FAIL: 2289 nodes 段未替换 / nodes block was not replaced')

    updated, transitions_replaced = re.subn(r'  <transitions>.*?</transitions>',
                                            NEW_TRANSITIONS.rstrip('\n'), updated,
                                            count=1, flags=re.S)
    if transitions_replaced != 1:
        raise SystemExit('FAIL: 2289 transitions 段未替换 / transitions block was not replaced')
    return updated


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    if not args.apply and not args.check:
        parser.error('use --check or --apply')
    current = QUEST.read_text(encoding='utf-8')
    done = (HEADER in current and NEW_METADATA_ADDITIONS in current
            and 'collecting-step="7"' in current and NEW_NODES in current and NEW_TRANSITIONS in current)
    if done:
        print('CHECK_OK 2289 already at batch-50 target state')
        return 0
    expected = apply_sections(current)
    if args.check:
        print('CHECK_PENDING 2289 needs batch-50 apply')
        return 0
    QUEST.write_text(expected, encoding='utf-8')
    print('APPLIED 2289 -> kill counter row (SECTION_0 0..4) + report/intel/collect ladder, reward var0=7')
    return 0


if __name__ == '__main__':
    sys.exit(main())
