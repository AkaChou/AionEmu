#!/usr/bin/env python3
"""批次 23：39713（[Daily] Fresh Powder）绿帽团日任行阶梯收口 / Batch 23 journal-row ladder for 39713.

背景 / Why
---------
客户端 quest_q39713.html 的 quest_summary 共 3 行（行 0..2），与魔族镜像 49713 同形：

  0 和[绿林团南部卡塔拉姆支部]对话
  1 把[净化粉末]撒在[遗忘沼泽的污染根源]上
  2 向[绿林团南部卡塔拉姆支部]报告

客户端页/按钮也与 49713 一致：select2 = HACTION_SETPRO1（收下净化粉末）、
select5 = HACTION_SELECT_QUEST_REWARD（报告结果）、ask_quest_accept = HACTION_FINISH_DIALOG。

但天族侧定义在迁移时把整条阶梯塌陷成 9 条无守卫的 `started -> reward` 直跳
（npc-item-report ×3 + SET_SUCCEED ×3 + SELECT_QUEST_REWARD ×3）：行 1、行 2 没有任何
START/REWARD 状态（MISSING_TAIL_ROWS），`started --QUEST_SELECT` 又错开成 select5（报告页），
reward 投影停在 0 而客户端领奖行是 2（QE-051 行错位），`npc-item-report` 还在报告时重复要求
已经用掉的净化粉末 182215285。

本脚本按魔族镜像 49713 的已对齐形态重建阶梯：

  started --QUEST_SELECT--> started（select2，领取粉末页）
  started --SETPRO1--> powder-received(1)（发 182215285 并写 var0=1）
  powder-received --use-item 182215285--> powder-used(2)（消耗道具并写 var0=2）
  powder-used --QUEST_SELECT--> powder-used（select5，报告页）
  powder-used --SELECT_QUEST_REWARD--> reward(2)（QE-051 领奖行）
  另补无 source `REWARD + var0==0 -> 2` 的 enter-world 自愈边，纠正旧存档。

变量宽度同时从 var0(1 bit, max 1) 抬到 var0(2 bit, max 3)，与镜像 49713 同形；
`npc-complete` 宏（reward 预览 + reward -> complete ×3）保持不动。

The Elyos variant of the Greenhat daily collapsed its three client journal rows into nine unguarded
`started -> reward` jumps, so rows 1 and 2 had no state at all, the reward projection stayed on row 0
instead of the client's reward row 2, and the report route still required the powder the player had
already used. This script rebuilds the row ladder after the already-aligned Asmodian twin 49713 and adds
the source-less `REWARD + var0==0 -> 2` enter-world self-heal for legacy saves.

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch23_faction_daily_rows.py [--check]
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

QUEST_ID = "39713"
QUEST_FILE = (Path(__file__).resolve().parents[3]
              / "src/main/resources/aion/data/static_data/quest_definition/quests"
              / f"{QUEST_ID}.xml")

NPCS = ("800936", "800937", "800938")
ITEM_ID = "182215285"
REWARD_ROW = 2
LEGACY_REWARD_ROW = 0

# ------------------------------------------------------------------ 精确锚点 / exact anchors

PROGRESS_OLD = ('    <bit-field name="var0" offset="0" width="1" min="0" max="1" '
                'persistence="PERSISTENT" scope="LOCAL"/>\n')
PROGRESS_NEW = ('    <!-- 行号阶梯：var0 是任务书行号（0..2），与魔族镜像 49713 同形。\n'
                '         Journal row index: var0 holds the quest_summary row (0..2); identical in shape to\n'
                '         the Asmodian twin 49713. -->\n'
                '    <bit-field name="var0" offset="0" width="2" min="0" max="3" '
                'persistence="PERSISTENT" scope="LOCAL"/>\n')

NODES_OLD = ('    <node label="started" status="START">\n'
             '      <var name="var0" value="0"/>\n'
             '    </node>\n'
             '    <node label="reward" status="REWARD">\n'
             '      <var name="var0" value="0"/>\n'
             '    </node>\n')
NODES_NEW = ('    <node label="started" status="START">\n'
             '      <var name="var0" value="0"/>\n'
             '    </node>\n'
             '    <node label="powder-received" status="START">\n'
             '      <var name="var0" value="1"/>\n'
             '    </node>\n'
             '    <node label="powder-used" status="START">\n'
             '      <var name="var0" value="2"/>\n'
             '    </node>\n'
             '    <node label="reward" status="REWARD">\n'
             '      <var name="var0" value="2"/>\n'
             '    </node>\n')


def started_block_old(npc: str) -> str:
    """旧形态：行 0 直接以 select5 报告并带无守卫的 started -> reward 直跳。"""
    return (
        f'    <transition source="started" target="started">\n'
        f'      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>\n'
        f'      </event>\n'
        f'      <after-commit>\n'
        f'        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>\n'
        f'      </after-commit>\n'
        f'    </transition>\n'
        f'    <npc-item-report npc-id="{npc}" source="started" target="reward" '
        f'item-id="{ITEM_ID}" required="1" failure-page="CLOSE"/>\n'
        f'    <transition source="started" target="reward">\n'
        f'      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SET_SUCCEED"/>\n'
        f'      </event>\n'
        f'      <after-commit>\n'
        f'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        f'        <close-dialog/>\n'
        f'      </after-commit>\n'
        f'    </transition>\n')


def started_block_new(npc: str, first: bool) -> str:
    """目标形态：行 1 领取粉末并推进一格；use-item 阶梯只在第一个块后展开一次。"""
    header = ('    <!-- 行 1「把净化粉末撒在遗忘沼泽的污染根源上」：先向三名支部成员中任意一位领取粉末，\n'
              '         行号推进到 1；旧定义在这里直接跳到领奖态，行 1/行 2 都永远不亮。\n'
              '         Row 1 ("scatter the powder over the swamp"): the player first receives the powder from\n'
              '         any of the three branch members and the journal advances by exactly one step; the legacy\n'
              '         definition jumped straight to the reward state, so rows 1 and 2 never lit up. -->\n') if first else ''
    block = (
        header
        + f'    <transition source="started" target="started">\n'
        f'      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>\n'
        f'      </event>\n'
        f'      <after-commit>\n'
        f'        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>\n'
        f'      </after-commit>\n'
        f'    </transition>\n'
        f'    <transition source="started" target="powder-received">\n'
        f'      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SETPRO1"/>\n'
        f'      </event>\n'
        f'      <actions>\n'
        f'        <give-item item-id="{ITEM_ID}" count="1"/>\n'
        f'        <set-variable field="var0" value="1"/>\n'
        f'      </actions>\n'
        f'      <after-commit>\n'
        f'        <sync-quest-state mode="PACKET_ONLY"/>\n'
        f'        <close-dialog/>\n'
        f'      </after-commit>\n'
        f'    </transition>\n')
    return block


LADDER = (
    '    <!-- 行 2「向绿林团南部卡塔拉姆支部报告」前的使用步骤：在污染区域使用净化粉末，\n'
    '         消耗道具并把行号推进到 2（魔族镜像 49713 同形）。\n'
    '         The step before row 2 ("report to the South Katalam branch"): use the powder at the\n'
    '         contamination site, consume it and advance the journal to row 2 (mirrors 49713). -->\n'
    f'    <transition source="powder-received" target="powder-used">\n'
    f'      <event>\n'
    f'        <use-item item-id="{ITEM_ID}"/>\n'
    f'      </event>\n'
    f'      <conditions>\n'
    f'        <variable-is field="var0" value="1"/>\n'
    f'        <has-item item-id="{ITEM_ID}" count="1"/>\n'
    f'      </conditions>\n'
    f'      <actions>\n'
    f'        <remove-item item-id="{ITEM_ID}" count="1"/>\n'
    f'        <set-variable field="var0" value="2"/>\n'
    f'      </actions>\n'
    f'      <after-commit>\n'
    f'        <sync-quest-state mode="PACKET_ONLY"/>\n'
    f'      </after-commit>\n'
    f'    </transition>\n'
)


def select5_block(npc: str) -> str:
    """行 2 的报告页自环（started->reward 直跳的替代前置）。"""
    return (
        f'    <transition source="powder-used" target="powder-used">\n'
        f'      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>\n'
        f'      </event>\n'
        f'      <after-commit>\n'
        f'        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>\n'
        f'      </after-commit>\n'
        f'    </transition>\n'
    )


# 领奖路由：旧的 `started -> reward` 无守卫直跳改成 `powder-used -> reward`（QE-051 领奖行 2）。
REWARD_ROUTE_OLD_800936 = (
    '      <transition source="started" target="reward">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="800936" action="SELECT_QUEST_REWARD"/>\n'
    '      </event>\n')
REWARD_ROUTE_NEW_800936 = (
    LADDER
    + select5_block("800936")
    + '    <transition source="powder-used" target="reward">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="800936" action="SELECT_QUEST_REWARD"/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <variable-is field="var0" value="2"/>\n'
    '      </conditions>\n')


def reward_route_old(npc: str) -> str:
    return ('    <transition source="started" target="reward">\n'
            '      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SELECT_QUEST_REWARD"/>\n'
            '      </event>\n')


def reward_route_new(npc: str) -> str:
    return (select5_block(npc)
            + '    <transition source="powder-used" target="reward">\n'
            '      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="SELECT_QUEST_REWARD"/>\n'
            '      </event>\n'
            '      <conditions>\n'
            '        <variable-is field="var0" value="2"/>\n'
            '      </conditions>\n')


# 旧存档自愈边 / enter-world self-heal for saves persisted at the old projection.
RECOVERY = (
    '    <!-- QE-051 旧存档自愈：reward 态但 packed var0 仍是旧投影 0 的存档\n'
    '         （修复前任意一名成员的报告直跳都会留下这种存档）在进入世界时纠正到领奖行 2。\n'
    '         QE-051 self-heal: a REWARD save whose packed var0 is still the legacy projection 0 (left by\n'
    '         any of the old unguarded report jumps) is repaired to the reward row 2 on enter-world. -->\n'
    '    <transition target="reward">\n'
    '      <event>\n'
    '        <enter-world/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <status-is status="REWARD"/>\n'
    f'        <variable-is field="var0" value="{LEGACY_REWARD_ROW}"/>\n'
    '      </conditions>\n'
    '      <actions>\n'
    f'        <set-variable field="var0" value="{REWARD_ROW}"/>\n'
    '      </actions>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '</transitions>\n'
)

RECOVERY_MARKER = '    <transition target="reward">\n'


def rewrite(text: str) -> str:
    """把 39713 定义改写成目标形态（幂等）。 / Rewrite the 39713 definition to the target shape."""
    if PROGRESS_NEW in text and NODES_NEW in text and RECOVERY_MARKER in text:
        return text
    for old, new in ((PROGRESS_OLD, PROGRESS_NEW), (NODES_OLD, NODES_NEW)):
        if text.count(old) != 1:
            raise SystemExit(f"BATCH23_ANCHOR_MISSING count={text.count(old)}: {old.splitlines()[0]}")
        text = text.replace(old, new, 1)
    for index, npc in enumerate(NPCS):
        old, new = started_block_old(npc), started_block_new(npc, first=index == 0)
        if text.count(old) != 1:
            raise SystemExit(f"BATCH23_STARTED_ANCHOR_MISSING npc={npc} count={text.count(old)}")
        text = text.replace(old, new, 1)
    if text.count(REWARD_ROUTE_OLD_800936) != 1:
        raise SystemExit("BATCH23_REWARD_ANCHOR_MISSING npc=800936 "
                         f"count={text.count(REWARD_ROUTE_OLD_800936)}")
    text = text.replace(REWARD_ROUTE_OLD_800936, REWARD_ROUTE_NEW_800936, 1)
    for npc in NPCS[1:]:
        old, new = reward_route_old(npc), reward_route_new(npc)
        if text.count(old) != 1:
            raise SystemExit(f"BATCH23_REWARD_ANCHOR_MISSING npc={npc} count={text.count(old)}")
        text = text.replace(old, new, 1)
    if text.count('</transitions>\n') != 1:
        raise SystemExit("BATCH23_TRANSITIONS_END_MISSING "
                         f"count={text.count('</transitions>')}")
    text = text.replace('</transitions>\n', RECOVERY, 1)
    return text


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply the batch-23 39713 row ladder.")
    parser.add_argument("--check", action="store_true", help="只校验，不写文件 / verify only")
    args = parser.parse_args()

    if not QUEST_FILE.exists():
        raise SystemExit(f"missing {QUEST_FILE}")
    original = QUEST_FILE.read_text(encoding="utf-8")
    target = rewrite(original)
    state = "already-applied" if target == original else "pending"
    if args.check:
        if state == "already-applied":
            print(f"BATCH23_OK quest={QUEST_ID} already-applied")
            return 0
        print(f"BATCH23_PENDING quest={QUEST_ID}")
        return 1
    if state == "pending":
        QUEST_FILE.write_text(target, encoding="utf-8")
        print(f"BATCH23_APPLIED quest={QUEST_ID}")
    else:
        print(f"BATCH23_OK quest={QUEST_ID} already-applied")
    return 0


if __name__ == "__main__":
    sys.exit(main())
