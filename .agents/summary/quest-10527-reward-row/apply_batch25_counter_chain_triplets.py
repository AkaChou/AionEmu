#!/usr/bin/env python3
"""批次 25：COUNTER_CHAIN 三槽族与 2842 领奖投影收口 / Batch 25 counter-chain triplets + 2842 reward row.

背景 / Why
---------
`audit_section0_report_row_closure.py` 的 COUNTER_CHAIN 口径在 2026-09-22 后仍报 13 个 GAP：
其中 1842/1843/1844/2843/2844/2845 六个是 `ClientQuestSectionAlignmentTest.EXTENDED_COUNTER_QUESTS`
已登记的“扩展计数器”（var0 需要 7 bit 装 80，var1 只能落在 bit 7），属审计脚本未登记的口径例外；
本脚本只处理真正的三槽族与 2842：

* 18033（[Alliance] Retreat Into Death，ELYOS）/ 28033（[Alliance] Confusing the Chain of Command，ASMODIAN）
  客户端 quest_monster.csv 是三条链式 0/1 记录：
    SECTION_0<1 -> idf5_td_nor_fi_n_65_ae (230744)
    SECTION_1<1 -> idf5_td_nor_kn_n_65_ae (230745)
    SECTION_2<1 -> idf5_td_nor_ra_n_65_ae (230749)
  旧定义把三条记录压成一个 `counter-grid`（单维 var0 required=1，任意一只怪即可满足），
  SECTION_1/SECTION_2 永远为 0 -> 客户端任务书第 2、3 个计数永远显示 0/1、第 2/3 行永远不消失
  （QE-053 的典型症状）。本脚本按 13918/23918 的已验收模板重建三槽串行阶梯。
  同时按 QE-052 收敛 owner：客户端行 1 点名 801281（LDF5b_Demades_E）/ 801280（LDF5b_Latkel_E），
  18033 的报告与领奖从 801037（Stifas，行 0 前的接受 NPC）改到 801281，28033 从 801047 改到 801280。

* 28313（A Wealthy Patron，ASMODIAN）客户端同样是三条链式记录（各 3 个难度变体）：
    SECTION_0 -> 217371/246131/248077（IDStation_Hugen_NM_58An 系列）
    SECTION_1 -> 217373/246132/248078（IDStation_ShulackFlight_NM_58_An 系列）
    SECTION_2 -> 217376/246133/248079（IDStation_DrakanNinja_NM_58_An 系列）
  旧定义是“步骤号”单槽（var0 = 已击杀组数 0..3），同样让 SECTION_1/SECTION_2 永远为 0。

* 2842（The Zephyr Island Treasure Chamber，ASMODIAN）的 39 杀计数路线在最后一次击杀时递增到 39，
  但 reward 节点投影仍是 var0=0 —— QuestMutationPlanner#matchesSourceNode 要求 source 节点逐字段全等，
  于是领奖态存档（var0=39）匹配不到任何 reward 路由，玩家在领奖阶段卡死。投影改成饱和值 39。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch25_counter_chain_triplets.py [--check]
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

QUEST_DIR = (Path(__file__).resolve().parents[3]
             / "src/main/resources/aion/data/static_data/quest_definition/quests")

CHAIN_PROGRESS = '''  <progress>
    <!-- 客户端 quest_monster.csv 的行是链式 0/1 计数槽：行 0 = SECTION_0<1 且 SECTION_5==0，
         行 1 = SECTION_1<1，行 2 = SECTION_2<1。var0..var2 就是这三个槽（各占 SECTION_n = 6n 位），
         每只（组）怪只把自己那一槽推到 1，行随之沉下、下一行亮起；SECTION_5 不声明（必须保持 0）。
         Client quest_monster.csv chains three 0/1 counter slots (row n shows while SECTION_n<1 and
         SECTION_5==0); var0..var2 are exactly those slots, so every target owns one counter at 6n and
         SECTION_5 (never declared) stays 0. -->
    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
    <bit-field name="var1" offset="6" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
    <bit-field name="var2" offset="12" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>
'''

CHAIN_NODES = '''  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
      <var name="var1" value="0"/>
      <var name="var2" value="0"/>
    </node>
    <node label="started" status="START">
      <var name="var0" value="0"/>
      <var name="var1" value="0"/>
      <var name="var2" value="0"/>
    </node>
    <node label="k1" status="START">
      <var name="var0" value="1"/>
      <var name="var1" value="0"/>
      <var name="var2" value="0"/>
    </node>
    <node label="k2" status="START">
      <var name="var0" value="1"/>
      <var name="var1" value="1"/>
      <var name="var2" value="0"/>
    </node>
    <node label="k3" status="START">
      <var name="var0" value="1"/>
      <var name="var1" value="1"/>
      <var name="var2" value="1"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
      <var name="var1" value="1"/>
      <var name="var2" value="1"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
      <var name="var1" value="0"/>
      <var name="var2" value="0"/>
    </node>
  </nodes>
'''

KILL_STEP = '''    <transition source="{source}" target="{target}">
      <event>
        <kill-npc npc-ids="{npc_ids}"/>
      </event>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
'''

REWARD_HEAL = '''    <!-- 旧网格/步骤模型存档（REWARD + 只写到旧投影）在进入世界时补齐三槽，
         否则 QuestMutationPlanner#matchesSourceNode 匹配不到新的 (1,1,1) 领奖投影；{why_zh}
         Legacy grid/step saves (REWARD carrying only the old projection) are completed to all three
         slots on enter-world; {why_en} -->
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        {condition}
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
        <set-variable field="var1" value="1"/>
        <set-variable field="var2" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
'''

# 18033/28033 的旧模型是 0/1 网格（START 节点只有 var0=0/1），领奖投影也只写 var0=1：
# 自愈条件必须排除三槽已饱和的正规领奖态，否则每次进入世界都会重放一次刷新。
# The army pair's legacy model is a 0/1 grid whose only reward projection was var0=1, so the heal
# condition must exclude the saturated projection instead of re-firing on every enter-world.
ARMY_HEAL_CONDITION = '<variable-sum-below fields="var0 var1 var2" value="3"/>'
ARMY_HEAL_WHY_ZH = '条件排除三槽已饱和的正规领奖态（旧网格领奖投影只有 var0=1）。'
ARMY_HEAL_WHY_EN = ('the condition excludes the saturated projection so a legitimate\n'
                    '         reward save is never replayed')

# 28313 的旧模型是步骤号单槽（领奖投影 var0=3），与新版 (1,1,1) 不可能混淆。
# 28313's legacy model stored the step number (reward projection var0=3), which cannot collide with
# the new (1,1,1) projection.
NINEVEH_HEAL_CONDITION = '<variable-is field="var0" value="3"/>'
NINEVEH_HEAL_WHY_ZH = '旧步骤模型的领奖投影是 var0=3，与新版 (1,1,1) 不可能混淆。'
NINEVEH_HEAL_WHY_EN = ('var0=3 is the legacy step-model projection and never\n'
                       '         collides with the new (1,1,1) ladder')


def reward_heal(condition: str, why_zh: str, why_en: str) -> str:
    return REWARD_HEAL.format(condition=condition, why_zh=why_zh, why_en=why_en)


def army_transitions(quest_id: int, accept_npc: int, report_npc: int) -> str:
    """18033/28033 的过渡块：三只怪各推一槽 + 报告/领奖 owner 收敛。"""
    return (
        '  <transitions>\n'
        f'    <!-- 接取 owner 保持 {accept_npc}（行 0 前的接受页）；报告与领奖收敛到客户端行 1 点名的\n'
        f'         {report_npc}（QE-052）。\n'
        f'         Accept owner stays {accept_npc}; the report/reward owner is the row-1 NPC named by the\n'
        f'         client journal, {report_npc} (QE-052). -->\n'
        f'    <dialog type="NPC_START" npc-id="{accept_npc}" source="unaccepted" target="started" '
        'selection-sources="unaccepted started" start-page="SHOW_ASK_QUEST_ACCEPT_WINDOW"/>\n'
        '    <!-- 三只怪各推自己那一槽（230744 -> var0、230745 -> var1、230749 -> var2）；乱序击杀不计数。\n'
        '         Each target owns its own slot (230744/230745/230749 -> var0/var1/var2). -->\n'
        + KILL_STEP.format(source="started", target="k1", npc_ids="230744")
        + KILL_STEP.format(source="k1", target="k2", npc_ids="230745")
        + KILL_STEP.format(source="k2", target="k3", npc_ids="230749")
        + reward_heal(ARMY_HEAL_CONDITION, ARMY_HEAL_WHY_ZH, ARMY_HEAL_WHY_EN)
        + f'    <dialog type="NPC_REPORT" npc-id="{report_npc}" source="k3" target="reward" page="SELECT2"/>\n'
        + f'    <npc-complete npc-id="{report_npc}" source="reward" target="complete" '
        'fixed-reward-indices="0 1 2" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" '
        'complete-reward-index="0" finish="SELECTION_DIALOG">\n'
        '      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>\n'
        '    </npc-complete>\n'
        '  </transitions>\n'
    )


NINEVEH_KILLS_OLD = (
    '    <transition source="started" target="k1">\n'
    '      <event>\n'
    '        <kill-npc npc-id="217371"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="PACKET_ONLY"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <transition source="k1" target="k2">\n'
    '      <event>\n'
    '        <kill-npc npc-id="217373"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="PACKET_ONLY"/>\n'
    '      </after-commit>\n'
    '    </transition>\n'
    '    <transition source="k2" target="k3">\n'
    '      <event>\n'
    '        <kill-npc npc-id="217376"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="PACKET_ONLY"/>\n'
    '      </after-commit>\n'
    '    </transition>\n')

NINEVEH_KILLS_NEW = (
    '    <!-- 三组强敌各推自己那一槽（217371/246131/248077 -> var0，217373/246132/248078 -> var1，\n'
    '         217376/246133/248079 -> var2）；乱序击杀不计数。\n'
    '         Each elite group owns its own slot; kills out of order do not count. -->\n'
    + KILL_STEP.format(source="started", target="k1", npc_ids="217371 246131 248077")
    + KILL_STEP.format(source="k1", target="k2", npc_ids="217373 246132 248078")
    + KILL_STEP.format(source="k2", target="k3", npc_ids="217376 246133 248079")
    + '    <!-- 旧步骤模型（var0 = 已完成组数 0..3）的无损迁移：var0=1 与新 k1 同形无需转换，\n'
      '         var0=2 -> k2、var0=3 -> k3；旧领奖投影 var0=3 补齐到 (1,1,1)。\n'
      '         Legacy step-model saves (var0 = completed groups) map onto k2/k3; the old reward\n'
      '         projection var0=3 is completed to (1,1,1). -->\n'
    + '''    <transition target="k2">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="START"/>
        <variable-is field="var0" value="2"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
        <set-variable field="var1" value="1"/>
        <set-variable field="var2" value="0"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
'''
    + '''    <transition target="k3">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="START"/>
        <variable-is field="var0" value="3"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
        <set-variable field="var1" value="1"/>
        <set-variable field="var2" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
'''
    + reward_heal(NINEVEH_HEAL_CONDITION, NINEVEH_HEAL_WHY_ZH, NINEVEH_HEAL_WHY_EN))

BLOCKS = {
    18033: {"progress": CHAIN_PROGRESS, "nodes": CHAIN_NODES,
            "transitions": army_transitions(18033, 801037, 801281)},
    28033: {"progress": CHAIN_PROGRESS, "nodes": CHAIN_NODES,
            "transitions": army_transitions(28033, 801047, 801280)},
    28313: {"progress": CHAIN_PROGRESS, "nodes": CHAIN_NODES},
}

FIXED_REWARD_INDEX = {18033: "0 1 2", 28033: "0 1 2", 28313: "0"}


def replace_block(text: str, tag: str, new_block: str) -> str:
    pattern = re.compile(rf"  <{tag}>.*?\n[ ]*</{tag}>\n", re.S)
    if len(pattern.findall(text)) != 1:
        raise SystemExit(f"BATCH25_BLOCK_ANCHOR_MISSING tag={tag}")
    return pattern.sub(lambda _match: new_block, text, count=1)


def rewrite_chain(quest_id: int, text: str) -> str:
    if quest_id == 28313:
        return rewrite_nineveh(text)
    marker = f'offset="12" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"'
    if (marker in text and f'source="k3"' in text and ARMY_HEAL_CONDITION in text
            and f'npc-id="{ {18033: 801281, 28033: 801280}[quest_id] }"' in text):
        return text
    for tag in ("progress", "nodes", "transitions"):
        text = replace_block(text, tag, BLOCKS[quest_id][tag])
    return text


def rewrite_nineveh(text: str) -> str:
    """28313：重建三槽与击杀路线，保留按职业展开的 reward -> complete 分支。"""
    if ('217371 246131 248077' in text and 'offset="12"' in text
            and 'value="2"/>' in text and '<transition target="k2">' in text):
        return text
    for tag in ("progress", "nodes"):
        text = replace_block(text, tag, BLOCKS[28313][tag])
    if text.count(NINEVEH_KILLS_OLD) != 1:
        raise SystemExit(f"BATCH25_28313_KILL_ANCHOR_MISSING count={text.count(NINEVEH_KILLS_OLD)}")
    return text.replace(NINEVEH_KILLS_OLD, NINEVEH_KILLS_NEW, 1)


def rewrite_2842(text: str) -> str:
    old = ('    <node label="reward" status="REWARD">\n'
           '      <var name="var0" value="0"/>\n'
           '    </node>\n')
    new = ('    <!-- 39 杀计数路线的最后一只把 var0 递增到 39；reward 投影必须是饱和值，\n'
           '         否则 matchesSourceNode 逐字段全等会把领奖态存档挡在所有 reward 路由之外。\n'
           '         The final kill of the 39-count route increments var0 to 39; the reward projection\n'
           '         must carry that saturated counter or the reward save matches no reward route. -->\n'
           '    <node label="reward" status="REWARD">\n'
           '      <var name="var0" value="39"/>\n'
           '    </node>\n')
    if new in text:
        return text
    if text.count(old) != 1:
        raise SystemExit(f"BATCH25_2842_ANCHOR_MISSING count={text.count(old)}")
    return text.replace(old, new, 1)


def rewrite(quest_id: int, text: str) -> str:
    if quest_id == 2842:
        return rewrite_2842(text)
    return rewrite_chain(quest_id, text)


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply the batch-25 counter-chain triplets.")
    parser.add_argument("--check", action="store_true", help="只校验，不写文件 / verify only")
    args = parser.parse_args()

    quests = sorted(BLOCKS) + [2842]
    pending = []
    for quest_id in quests:
        path = QUEST_DIR / f"{quest_id}.xml"
        if not path.exists():
            raise SystemExit(f"missing {path}")
        original = path.read_text(encoding="utf-8")
        target = rewrite(quest_id, original)
        if target != original:
            pending.append(quest_id)
            if not args.check:
                path.write_text(target, encoding="utf-8")
    if args.check:
        if pending:
            print(f"BATCH25_PENDING quests={pending}")
            return 1
        print(f"BATCH25_OK quests={quests} already-applied")
        return 0
    if pending:
        print(f"BATCH25_APPLIED quests={pending}")
    else:
        print(f"BATCH25_OK quests={quests} already-applied")
    return 0


if __name__ == "__main__":
    sys.exit(main())
