#!/usr/bin/env python3
"""批次 28：永恒档案馆领奖阶任务 16800/26800 的 legacy 落盘 step 与三段行阶梯。

族级判据（QE-054 + QE-051）：
- 客户端 quest_q16800.html / quest_q26800.html 的 quest_summary 都是 3 行：
  行 0 = 使用激活的塔碎片进入（LF_Tower / DF_Tower）、行 1 = 和永恒之塔警备组长
  Etezar（806232）/ Enfitenta（806233）对话、行 2 = 向代理人维达（806148）/
  佩莱格兰（806149）报告（领奖行）。
- legacy `_16800Into_The_Archives` / `_26800A_Call_For_Champions` 的落盘 step 链：
  * LF_TOWER_SENSORY_AREA_Q16800_210110000 / DF_TOWER_SENSORY_AREA_Q26800_220120000 里
    `changeQuestStep(env, 0, 1, false)` 写 step 1；
  * 806232/806233 的 SET_REWARD 走 `changeQuestStep(env, 1, 2, false)` 写 step 2；
  * IDETERNITY_01_Q16800_301540000 里 `playQuestMovie(env, 931/932)` +
    `changeQuestStep(env, 2, 3, true)` —— 旧引擎这条 `,true)` 只置 REWARD、**不写 nextStep**
    （迁移前 commit `7e9f0316c^` 的 QuestHandler.changeQuestStep，reward 分支只 setStatus），
    所以领奖态真正落盘的 step 仍是 2。
- 因此 reward 投影必须是 2（= legacy 落盘值 = 客户端末行索引），而旧 XML 分别写成
  16800=1（var0 只有 1 bit、没有 s1/s2 阶梯、用 at-distance 206535 + var1 旗标代替 zone 推进）
  与 26800=3（`s2 -> reward` 又显式写 `var0=3`）。

落点（两侧同形 `unaccepted(0)/started(0)/s1(1)/s2(2)/reward(2)/complete(0)`）：
1. 16800：var0 改单字段 width 2 / max 3（删除 var1 旗标位），补 s1/s2 节点、两条 zone 推进与
   806232 的对话阶梯；删除 806075/806148/806232 在 started 态直跳 reward 的 6 条捷径与 806232 的
   npc-complete（领奖 owner 收敛到任务书点名的代理人 806148）；
2. 26800：reward 投影 3 -> 2，删除 `s2 -> reward` 的 `set-variable var0=3`（改由 target 投影生效）；
3. 两侧都补无 source 的 ENTER_WORLD 恢复边：`REWARD && var0=<旧投影值> -> reward(2)`
   （16800 的旧投影是 1、26800 的旧投影是 3），正规态 `var0=2` 不会被重放。

参考模板：quests/18805.xml（批次 27 同判据的领奖行修复）。

Batch 28: legacy-persisted reward step and the three-stage journal ladder for the Archives quests
16800/26800 (both sides rebuilt into the same unaccepted/started/s1/s2/reward/complete shape).
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

QUEST_DIR = (Path(__file__).resolve().parents[3]
             / "src/main/resources/aion/data/static_data/quest_definition/quests")

OLD_PROGRESS_16800 = (
    '  <progress>\n'
    '    <bit-field name="var0" offset="0" width="1" min="0" max="1" persistence="PERSISTENT" scope="LOCAL"/>\n'
    '    <bit-field name="var1" offset="1" width="1" min="0" max="1" persistence="PERSISTENT" scope="LOCAL"/>\n'
    '  </progress>\n')

NEW_PROGRESS_16800 = (
    '  <progress>\n'
    '    <bit-field name="var0" offset="0" width="2" min="0" max="3" persistence="PERSISTENT" scope="LOCAL"/>\n'
    '  </progress>\n')

OLD_NODES_16800 = (
    '  <nodes>\n'
    '    <node label="unaccepted" status="NONE">\n'
    '      <var name="var0" value="0"/>\n'
    '      <var name="var1" value="0"/>\n'
    '    </node>\n'
    '    <node label="started" status="START">\n'
    '      <var name="var0" value="0"/>\n'
    '      <var name="var1" value="0"/>\n'
    '    </node>\n'
    '    <node label="reward" status="REWARD">\n'
    '      <var name="var0" value="1"/>\n'
    '    </node>\n'
    '    <node label="complete" status="COMPLETE">\n'
    '      <var name="var0" value="0"/>\n'
    '    </node>\n'
    '  </nodes>\n')

NEW_NODES_16800 = (
    '  <nodes>\n'
    '    <node label="unaccepted" status="NONE">\n'
    '      <var name="var0" value="0"/>\n'
    '    </node>\n'
    '    <node label="started" status="START">\n'
    '      <var name="var0" value="0"/>\n'
    '    </node>\n'
    '    <node label="s1" status="START">\n'
    '      <var name="var0" value="1"/>\n'
    '    </node>\n'
    '    <node label="s2" status="START">\n'
    '      <var name="var0" value="2"/>\n'
    '    </node>\n'
    '    <node label="reward" status="REWARD">\n'
    '      <var name="var0" value="2"/>\n'
    '    </node>\n'
    '    <node label="complete" status="COMPLETE">\n'
    '      <var name="var0" value="0"/>\n'
    '    </node>\n'
    '  </nodes>\n')

RECOVERY_16800 = (
    '    <!-- QE-054 领奖阶合同：客户端 quest_q16800.html 的 quest_summary 共 3 行（行 0 使用激活的塔碎片进入\n'
    '         LF_Tower、行 1 和 LF_Tower_Etezar_E 对话、行 2 向代理人维达报告）。legacy `_16800Into_The_Archives`\n'
    '         的落盘 step 是 2：LF_TOWER_SENSORY_AREA_Q16800_210110000 把 step 0 推到 1、Etezar（806232）的\n'
    '         SET_REWARD 把 1 推到 2、IDETERNITY_01_Q16800_301540000 的 changeQuestStep(env, 2, 3, true) 只置\n'
    '         REWARD、step 停在 2。旧投影把领奖态写成 1，进入世界时纠正为 2 并下发状态包。\n'
    '         Reward step contract (QE-054): the legacy handler persists step 2 when entering REWARD because\n'
    '         changeQuestStep(..., true) only flips the status; saves persisted at var0=1 are repaired on\n'
    '         enter-world. -->\n'
    '    <transition target="reward">\n'
    '      <event>\n'
    '        <enter-world/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <status-is status="REWARD"/>\n'
    '        <variable-is field="var0" value="1"/>\n'
    '      </conditions>\n'
    '      <actions>\n'
    '        <set-variable field="var0" value="2"/>\n'
    '      </actions>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
    '      </after-commit>\n'
    '    </transition>\n')

ACCEPT_CHAIN_16800 = (
    '    <!-- 区域/事件接取任务：对话仅显示 select_none 履行简报，FINISH_DIALOG 关闭；\n'
    '         start-eligible 供 repeatable 任务在 COMPLETE 态别名复用。 -->\n'
    '    <transition source="unaccepted" target="unaccepted">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="806075" action="QUEST_SELECT"/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <start-eligible/>\n'
    '      </conditions>\n'
    '      <after-commit>\n'
    '        <dialog type="SHOW_QUEST_PAGE" page="SELECT_NONE"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <transition source="unaccepted" target="unaccepted">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="806075" action="FINISH_DIALOG"/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <start-eligible/>\n'
    '      </conditions>\n'
    '      <after-commit>\n'
    '        <close-dialog/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <transition source="unaccepted" target="unaccepted">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="806075" action="SELECT_NONE_1"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <dialog type="SHOW_QUEST_PAGE" page="SELECT_NONE_1"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <transition source="unaccepted" target="started">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="806075" action="QUEST_ACCEPT_SIMPLE"/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <start-eligible/>\n'
    '      </conditions>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="VISIBILITY_REFRESH"/>\n'
    '        <close-dialog/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <transition source="unaccepted" target="unaccepted">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="806075" action="QUEST_REFUSE_SIMPLE"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <close-dialog/>\n'
    '      </after-commit>\n'
    '    </transition>\n')

LADDER_16800 = (
    '    <!-- 进入 LF_Tower 感应区后推进到警备组长阶段。 / Entering the LF tower sensory area advances to Etezar. -->\n'
    '    <transition source="started" target="s1">\n'
    '      <event>\n'
    '        <enter-zone zone="LF_TOWER_SENSORY_AREA_Q16800_210110000"/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <variable-is field="var0" value="0"/>\n'
    '      </conditions>\n'
    '      <actions>\n'
    '        <set-variable field="var0" value="1"/>\n'
    '      </actions>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="PACKET_ONLY"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <transition source="s1" target="s1">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="806232" action="QUEST_SELECT"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <transition source="s1" target="s1">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="806232" action="SELECT2_1"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <transition source="s1" target="s2">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="806232" action="SET_SUCCEED"/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <variable-is field="var0" value="1"/>\n'
    '      </conditions>\n'
    '      <actions>\n'
    '        <set-variable field="var0" value="2"/>\n'
    '      </actions>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="PACKET_ONLY"/>\n'
    '        <close-dialog/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <!-- 进入知识书库后置 REWARD（step 停 2），再播放电影 931。 / Entering the Archives flips REWARD at\n'
    '         step 2 before playing movie 931. -->\n'
    '    <transition source="s2" target="reward">\n'
    '      <event>\n'
    '        <enter-zone zone="IDETERNITY_01_Q16800_301540000"/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <variable-is field="var0" value="2"/>\n'
    '      </conditions>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
    '        <play-movie movie-id="931"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <transition source="reward" target="reward">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="806148" action="QUEST_SELECT"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <npc-complete npc-id="806148" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">\n'
    '      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>\n'
    '    </npc-complete>\n')

RECOVERY_26800 = (
    '    <!-- QE-054 领奖阶合同：客户端 quest_q26800.html 的 quest_summary 共 3 行（行 0 使用激活的塔碎片进入\n'
    '         DF_Tower、行 1 和 DF_Tower_Enfitenta_E 对话、行 2 向代理人佩莱格兰报告）。legacy\n'
    '         `_26800A_Call_For_Champions` 的落盘 step 是 2：DF_TOWER_SENSORY_AREA_Q26800_220120000 把 step 0\n'
    '         推到 1、Enfitenta（806233）的 SET_REWARD 把 1 推到 2、IDETERNITY_01_Q16800_301540000 的\n'
    '         changeQuestStep(env, 2, 3, true) 只置 REWARD、step 停在 2。旧投影把领奖态写成 3，进入世界时\n'
    '         纠正为 2 并下发状态包。\n'
    '         Reward step contract (QE-054): the legacy handler persists step 2 when entering REWARD because\n'
    '         changeQuestStep(..., true) only flips the status; saves persisted at var0=3 are repaired on\n'
    '         enter-world. -->\n'
    '    <transition target="reward">\n'
    '      <event>\n'
    '        <enter-world/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <status-is status="REWARD"/>\n'
    '        <variable-is field="var0" value="3"/>\n'
    '      </conditions>\n'
    '      <actions>\n'
    '        <set-variable field="var0" value="2"/>\n'
    '      </actions>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
    '      </after-commit>\n'
    '    </transition>\n')

NEW_TRANSITIONS_16800 = ('  <transitions>\n' + RECOVERY_16800 + ACCEPT_CHAIN_16800
                         + LADDER_16800.replace('  <transitions>\n', '') + '  </transitions>')

OLD_REWARD_NODE_26800 = ('    <node label="reward" status="REWARD">\n'
                         '      <var name="var0" value="3"/>\n'
                         '    </node>\n')
NEW_REWARD_NODE_26800 = ('    <node label="reward" status="REWARD">\n'
                         '      <var name="var0" value="2"/>\n'
                         '    </node>\n')
OLD_ARCHIVES_ACTIONS_26800 = ('      <actions>\n'
                              '        <set-variable field="var0" value="3"/>\n'
                              '      </actions>\n')

# 旧 16800 现状指纹：var1 旗标位、at-distance 假推进、806232 领奖 owner、六条直跳 reward 的捷径。
FINGERPRINT_16800 = (
    'at-distance npc-id="206535"',
    'npc-complete npc-id="806075"',
    'npc-complete npc-id="806232"',
    'npc-complete npc-id="806148"',
    '<bit-field name="var1" offset="1" width="1"',
)
# 旧 26800 现状指纹：reward 投影 3 + 交接写 var0=3。
FINGERPRINT_26800 = (
    '<var name="var0" value="3"/>',
    '<set-variable field="var0" value="3"/>',
    'play-movie movie-id="932"',
)


def replace_exact(text: str, old: str, new: str, quest_id: int, tag: str) -> str:
    if text.count(old) != 1:
        raise SystemExit(f"BATCH28_{tag}_ANCHOR_MISSING quest={quest_id} count={text.count(old)}")
    return text.replace(old, new, 1)


def rewrite_16800(text: str) -> str:
    if RECOVERY_16800 in text:
        return text
    for marker in FINGERPRINT_16800:
        if marker not in text:
            raise SystemExit(f"BATCH28_16800_FINGERPRINT_MISSING marker={marker}")
    text = replace_exact(text, OLD_PROGRESS_16800, NEW_PROGRESS_16800, 16800, "PROGRESS")
    text = replace_exact(text, OLD_NODES_16800, NEW_NODES_16800, 16800, "NODES")
    start = text.index('  <transitions>\n')
    end = text.index('</transitions>', start) + len('</transitions>')
    return text[:start] + NEW_TRANSITIONS_16800 + text[end:]


def rewrite_26800(text: str) -> str:
    if RECOVERY_26800 in text:
        return text
    for marker in FINGERPRINT_26800:
        if marker not in text:
            raise SystemExit(f"BATCH28_26800_FINGERPRINT_MISSING marker={marker}")
    text = replace_exact(text, OLD_REWARD_NODE_26800, NEW_REWARD_NODE_26800, 26800, "REWARD_NODE")
    # s2 -> reward 交接不再回写旧行，改由 target 投影（var0=2）生效。
    text = replace_exact(text, OLD_ARCHIVES_ACTIONS_26800, '', 26800, "STALE_WRITE")
    anchor = '  <transitions>\n'
    if text.count(anchor) != 1:
        raise SystemExit("BATCH28_26800_TRANSITIONS_ANCHOR_MISSING")
    return text.replace(anchor, anchor + RECOVERY_26800, 1)


REWRITERS = {16800: rewrite_16800, 26800: rewrite_26800}


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply the batch-28 Archives reward-step repair.")
    parser.add_argument("--check", action="store_true", help="只校验，不写文件 / verify only")
    args = parser.parse_args()

    quests = sorted(REWRITERS)
    pending = []
    for quest_id in quests:
        path = QUEST_DIR / f"{quest_id}.xml"
        if not path.exists():
            raise SystemExit(f"missing {path}")
        original = path.read_text(encoding="utf-8")
        target = REWRITERS[quest_id](original)
        if target != original:
            pending.append(quest_id)
            if not args.check:
                path.write_text(target, encoding="utf-8")
    if args.check:
        if pending:
            print(f"BATCH28_PENDING quests={pending}")
            return 1
        print(f"BATCH28_OK quests={quests} already-applied")
        return 0
    if pending:
        print(f"BATCH28_APPLIED quests={pending}")
    else:
        print(f"BATCH28_OK quests={quests} already-applied")
    return 0


if __name__ == "__main__":
    sys.exit(main())
