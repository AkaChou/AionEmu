#!/usr/bin/env python3
"""批次 22：24046（The Shadow Calls）副本段行状态收口 / Batch 22 journal-row ladder for 24046.

背景 / Why
---------
客户端 quest_q24046.html 的 quest_summary 共 8 行（行 0..7），末行是向 Muninn(203550) 报告：

  0 到 DF2A 和 Phyper 见面                     1 查看 Phyper 的预言是否会成为现实
  2 收到了传票！到 Srudgelmir 去看一下          3 和竞技场管理员 Galm 进行对话
  4 进入 DC1_door_Q2076 寻找沉默审判官          5 找到 IDDC1_Arena_3F_Exit 逃出秘密监狱
  6 和 Srudgelmir 进行对话                     7 向伊斯夏尔肯的 Muninn 报告监狱里发生的事情

旧定义（与迁移前 legacy handler `_24046The_Shadow_Calls` 同形）在与 Galm 对话后直接
`changeQuestStep(3, 5)`，把行 4 整个略过；reward 投影也停在 6，于是行 4 永远不亮、
领奖行（行 7）与行 6 共用同一个 var0=6 投影（QE-051 行错位）。

本脚本把副本段补成串行行号阶梯：3->4（与 Galm 对话并传送进审判所副本）、
4->5（已进入副本世界 320120000）、5->6（用出口物件 700369 逃出）、6->REWARD(7)（向 Muninn 报告），
并补 `REWARD + var0==6 -> 7` 的无 source enter-world 自愈边（旧存档纠正到领奖行），
与天族镜像 14046 的 8 行阶梯同形。

The client journal of 24046 has eight rows and ends with the report to Muninn(203550). The legacy
handler (and the migrated definition) jumped `changeQuestStep(3, 5)` at Garm, so row 4 never lit up and
the reward row shared projection 6 with row 6. This script rebuilds the instance segment as a serial
row ladder (3->4 enter, 4->5 inside the Shadow Court instance, 5->6 escape through object 700369,
6->REWARD(7) report) plus a `REWARD + var0==6 -> 7` enter-world self-heal, mirroring the Elyos twin 14046.

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch22_shadow_court_row_ladder.py [--check]
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

QUEST_ID = "24046"
QUEST_FILE = (Path(__file__).resolve().parents[3]
              / "src/main/resources/aion/data/static_data/quest_definition/quests"
              / f"{QUEST_ID}.xml")

# ------------------------------------------------------------------ 精确锚点 / exact anchors

PROGRESS_OLD = ('    <bit-field name="var0" offset="0" width="4" min="0" max="6" '
                'persistence="PERSISTENT" scope="LOCAL"/>\n')
PROGRESS_NEW = ('    <!-- 行号阶梯：var0 是任务书行号（0..7），不是客户端 SECTION 计数槽；与天族镜像 14046 同形。\n'
                '         Journal row index: var0 holds the quest_summary row (0..7), not a SECTION counter\n'
                '         slot; identical in shape to the Elyos twin 14046. -->\n'
                '    <bit-field name="var0" offset="0" width="3" min="0" max="7" '
                'persistence="PERSISTENT" scope="LOCAL"/>\n')

NODES_OLD = ('    <node label="s3" status="START">\n'
             '      <var name="var0" value="3"/>\n'
             '    </node>\n'
             '    <node label="s5" status="START">\n')
NODES_NEW = ('    <node label="s3" status="START">\n'
             '      <var name="var0" value="3"/>\n'
             '    </node>\n'
             '    <node label="s4" status="START">\n'
             '      <var name="var0" value="4"/>\n'
             '    </node>\n'
             '    <node label="s5" status="START">\n')

REWARD_OLD = ('    <node label="reward" status="REWARD">\n'
              '      <var name="var0" value="6"/>\n'
              '    </node>\n')
REWARD_NEW = ('    <node label="reward" status="REWARD">\n'
              '      <var name="var0" value="7"/>\n'
              '    </node>\n')

TRANSITIONS_OLD = ('  <transitions>\n'
                   '    <transition source="unaccepted" target="started">\n')
TRANSITIONS_NEW = ('  <transitions>\n'
                   '    <!-- 领奖行合同（QE-051）：客户端 quest_q24046.html 的 quest_summary 共 8 行，\n'
                   '         末行是向 Muninn(203550) 报告；旧投影停在 6（第 7 行），进入世界时纠正为 7，\n'
                   '         与天族镜像 14046 的 `REWARD && var0==6 -> 7` 自愈边同形。\n'
                   '         Reward-row contract (QE-051): the eight quest_q24046 journal rows end with the\n'
                   '         report to Muninn(203550); saves persisted at row 6 are repaired to row 7 on\n'
                   '         enter-world, mirroring the Elyos twin 14046. -->\n'
                   '    <transition target="reward">\n'
                   '      <event>\n'
                   '        <enter-world/>\n'
                   '      </event>\n'
                   '      <conditions>\n'
                   '        <status-is status="REWARD"/>\n'
                   '        <variable-is field="var0" value="6"/>\n'
                   '      </conditions>\n'
                   '      <actions>\n'
                   '        <set-variable field="var0" value="7"/>\n'
                   '      </actions>\n'
                   '      <after-commit>\n'
                   '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
                   '      </after-commit>\n'
                   '    </transition>\n'
                   '    <transition source="unaccepted" target="started">\n')

SETPRO4_OLD = ('    <transition source="s3" target="s5">\n'
               '      <event>\n'
               '        <dialog type="TALK_TO_NPC" npc-id="204089" action="SETPRO4"/>\n'
               '      </event>\n'
               '      <conditions>\n'
               '        <variable-is field="var0" value="3"/>\n'
               '      </conditions>\n'
               '      <actions>\n'
               '        <set-variable field="var0" value="5"/>\n'
               '      </actions>\n')
SETPRO4_NEW = ('    <!-- 行 4「进入 DC1_door_Q2076 寻找沉默审判官」：与竞技场管理员加尔姆对话后传送进审判所副本，\n'
               '         行号只推进一格（旧 handler 从 3 直接跳到 5，客户端第 5 行永远不亮）。\n'
               '         Row 4 ("enter the DC1 door and look for the silent judge"): the arena manager Garm teleports\n'
               '         the player into the Shadow Court instance and the journal row advances by exactly one step;\n'
               '         the legacy handler jumped 3 -> 5, so the client\'s fifth row never lit up. -->\n'
               '    <transition source="s3" target="s4">\n'
               '      <event>\n'
               '        <dialog type="TALK_TO_NPC" npc-id="204089" action="SETPRO4"/>\n'
               '      </event>\n'
               '      <conditions>\n'
               '        <variable-is field="var0" value="3"/>\n'
               '      </conditions>\n'
               '      <actions>\n'
               '        <set-variable field="var0" value="4"/>\n'
               '      </actions>\n')

ESCAPE_OLD = '    <transition source="s5" target="s6">\n'
ESCAPE_NEW_HEAD = (
    '    <!-- 行 5「找到 IDDC1_Arena_3F_Exit 逃出秘密监狱」：踏入审判所副本世界即亮起\n'
    '         （旧 handler 用 var0=5 表示副本内状态）。\n'
    '         Row 5 ("find the 3F exit and escape the secret prison"): lights up as soon as the player is\n'
    '         inside the Shadow Court instance (the legacy handler used var0=5 for the in-instance state). -->\n'
    '    <transition source="s4" target="s5">\n'
    '      <event>\n'
    '        <enter-world/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <variable-is field="var0" value="4"/>\n'
    '        <world-is world-id="320120000" expected="true"/>\n'
    '      </conditions>\n'
    '      <actions>\n'
    '        <set-variable field="var0" value="5"/>\n'
    '      </actions>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="PACKET_ONLY"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <!-- 传送未生效（副本已满 / 掉线）时把行 4 存档退回行 3，玩家可重新找加尔姆进入，\n'
    '         避免卡在无对话的行 4。\n'
    '         When the instance teleport never took effect (instance full, disconnect), a row-4 save rolls\n'
    '         back to row 3 so the player can re-enter through Garm instead of being stuck on a row without\n'
    '         dialogue. -->\n'
    '    <transition source="s4" target="s3">\n'
    '      <event>\n'
    '        <enter-world/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <variable-is field="var0" value="4"/>\n'
    '        <world-is world-id="320120000" expected="false"/>\n'
    '      </conditions>\n'
    '      <actions>\n'
    '        <set-variable field="var0" value="3"/>\n'
    '      </actions>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="PACKET_ONLY"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    + ESCAPE_OLD)

SETPRO4_MARKER = '    <transition source="s3" target="s4">\n'
INSIDE_MARKER = '        <world-is world-id="320120000" expected="true"/>\n'


def rewrite(text: str) -> str:
    """把 24046 定义改写成目标形态（幂等）。 / Rewrite the 24046 definition to the target shape."""
    if (PROGRESS_NEW in text and NODES_NEW in text and REWARD_NEW in text
            and SETPRO4_MARKER in text and INSIDE_MARKER in text):
        return text
    for old, new in ((PROGRESS_OLD, PROGRESS_NEW), (NODES_OLD, NODES_NEW),
                     (REWARD_OLD, REWARD_NEW), (TRANSITIONS_OLD, TRANSITIONS_NEW),
                     (SETPRO4_OLD, SETPRO4_NEW), (ESCAPE_OLD, ESCAPE_NEW_HEAD)):
        if text.count(old) != 1:
            raise SystemExit(f"BATCH22_ANCHOR_MISSING count={text.count(old)}: {old.splitlines()[0]}")
        text = text.replace(old, new, 1)
    return text


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply the batch-22 24046 row ladder.")
    parser.add_argument("--check", action="store_true", help="只校验，不写文件 / verify only")
    args = parser.parse_args()

    if not QUEST_FILE.exists():
        raise SystemExit(f"missing {QUEST_FILE}")
    original = QUEST_FILE.read_text(encoding="utf-8")
    target = rewrite(original)
    state = "already-applied" if target == original else "pending"
    if args.check:
        if state == "already-applied":
            print(f"BATCH22_OK quest={QUEST_ID} already-applied")
            return 0
        print(f"BATCH22_PENDING quest={QUEST_ID}")
        return 1
    if state == "pending":
        QUEST_FILE.write_text(target, encoding="utf-8")
        print(f"BATCH22_APPLIED quest={QUEST_ID}")
    else:
        print(f"BATCH22_OK quest={QUEST_ID} already-applied")
    return 0


if __name__ == "__main__":
    sys.exit(main())
