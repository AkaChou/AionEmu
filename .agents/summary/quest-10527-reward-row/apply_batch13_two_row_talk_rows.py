#!/usr/bin/env python3
"""批次 13：5.8 “两行、末行是与领奖 NPC 的对话”族的领奖行收口（6 个任务）。

族定义：客户端 quest_summary 恰好 2 行 —— 行 0 是“前往/交付/进入/接取对话”，行 1 是“（再次）
与 X 对话 / 向 X 报告”，且**行 1 点名的 NPC 就是定义里的领奖/完成 NPC**。定义原先把 reward
投影停在 0，于是 REWARD 态在任务书里仍然渲染行 0（已完成的目标行），行 1 永远没有状态。
本批把 `reward` 节点投影 0 → 1，并补一条无 source 的 enter-world 自愈边，把旧存档里的
REWARD/var0=0（旧投影）纠正为 1；1926/2938 原有的 `REWARD && var0==1` 领奖态入口边保留，
自愈边与其条件互斥，不会产生 AMBIGUOUS_TRANSITION。

Batch 13: the "two-row journal whose second row talks to the reward NPC" family (6 quests). The
reward projection moves 0 -> 1 and a source-less enter-world heal edge repairs stale REWARD/var0=0
saves.

边界（本批不改）：
- QE-045 锁：13965/23965（enter-zone 置 REWARD）、15674/25674（CHECK_COLLECTED_ITEMS 置 REWARD）
  是同形的 2 行任务（行 1 = 与 835217/835220/806114/806116 对话），但它们的 reward 投影 0 与
  `REWARD && var0==1` 恢复边是 commit f6aff952a“保留 legacy packed step”的基线，并被
  LegacyRewardStepProjectionRegressionTest 的 20 例硬锁；改投影 = 重定 QE-045 基线，按 QE-045
  boundary 必须有客户端验收证据才可动，故本批只登记证据、不改文件（见 QE045_DEFERRED）。
- 50008/51008：legacy handler 用 `setQuestVarById(0, var0 + 1)` 把 var0 当 0→2 的投递计数，
  与行号语义冲突，须按“计数槽 + 行状态”单独设计；且客户端行内的 HousingLf_Event_ShugoSanta /
  E_HousingDF_Event_ShugaShugo 在 5.8 客户端 NPC 表中不存在，归属无法核对。
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

# quest_id -> (行 1 NPC 名, 行 1 NPC id, legacy handler 证据)
FAMILY = {
    1926: ("Latri", 203894, "客户端行 1 = 和 STR_DIC_N_Latri 对话；legacy `_1926Secret_Library_Access` 在 203894 talk 处 setStatus(REWARD)，REWARD 态在 203894 开 10002 -> 5"),
    2938: ("Izwin", 204267, "客户端行 1 = 和 STR_DIC_N_Izwin 对话；legacy `_2938Secret_Library_Access` 在 204267 talk 处 setStatus(REWARD)，REWARD 态在 204267 开 10002 -> 5"),
    39003: ("DF2a_Nevma_G_LHM", 800504, "客户端行 0 = DF2a_Ionia_E_LHW(800512) SETPRO1、行 1 = STR_DIC_NPC_DF2a_Nevma_G_LHM(800504) SELECT5；定义 NPC_REPORT 与 npc-complete 都在 800504"),
    49003: ("dromik", 800505, "客户端行 0 = Noorn(800511) SETPRO1、行 1 = STR_DIC_N_dromik(800505) SELECT5；定义 NPC_REPORT 与 npc-complete 都在 800505"),
    80989: ("IDRUN_Entrance_guide", 836196, "客户端行 1 = 回到大城市再次和咕咕咻对话；定义 SET_SUCCEED/DEFAULT_SUCCESS 与 npc-complete 都在 836196（客户端 NPC 表名 IDRUN_Entrance_guide）"),
    80990: ("IDRUN_Entrance_guide", 836196, "同 80989（另一阵营事件变体），客户端行 1 与定义 NPC 同为 836196"),
}

# QE-045 锁覆盖的同形任务：本批只登记证据，不写文件。
# Same shape, but QE-045 (f6aff952a "preserve legacy packed steps" + LegacyRewardStepProjectionRegressionTest)
# locks the reward projection to the legacy packed step 0; deferred until client evidence re-baselines it.
QE045_DEFERRED = {
    13965: ("LF6_Benian_E", 835217, "enter-zone IDETERNITY_WAR_Q13965 置 REWARD，REWARD 态在 835217 开 10002/5"),
    23965: ("DF6_Ottar_E", 835220, "同 13965 结构，REWARD 态在 835220"),
    15674: ("LF6_Ilisia_E", 806114, "806114 的 CHECK_COLLECTED_ITEMS 置 REWARD，REWARD 态在 806114 开 10002/5"),
    25674: ("DF6_Reinhard_E", 806116, "806116 的 CHECK_COLLECTED_ITEMS 置 REWARD，REWARD 态在 806116 开 10002/5"),
}

ENTRY_ROUTE_ANCHOR = ("    <transition target=\"reward\">\n"
                      "      <event>\n"
                      "        <enter-world/>\n"
                      "      </event>\n"
                      "      <conditions>\n"
                      "        <status-is status=\"REWARD\"/>\n"
                      "        <variable-is field=\"var0\" value=\"1\"/>")

HEAL_ROUTE = """    <transition target="reward">
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

REWARD_NODE_OLD = re.compile(r'(<node label="reward" status="REWARD">\s*<var name="var0" value=")0(")')
REWARD_NODE_NEW = re.compile(r'(<node label="reward" status="REWARD">\s*<var name="var0" value=")1(")')


def comment(quest_id: int) -> str:
    name, npc_id, evidence = FAMILY[quest_id]
    return (
        f"    <!-- QE-051 领奖行：客户端 quest_q{quest_id}.html 只有 2 行（行 0 目标/交付、行 1 与 {name} 对话）；\n"
        f"         旧 reward 投影停在 0，所以领奖态仍渲染行 0。证据：{evidence}。\n"
        f"         因此 reward 投影固定为领奖行 1，并补 REWARD/var0=0 的 enter-world 自愈边（旧存档旧值\n"
        f"         只会来自旧投影，语义无歧义）。\n"
        f"         Reward-row contract (QE-051): the second client journal row talks to {name}({npc_id}) and the\n"
        f"         legacy handler enters REWARD on that same NPC, so the reward projection is 1 and stale\n"
        f"         REWARD/var0=0 saves are healed on enter-world. -->\n"
    )


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def contract_ok(text: str, quest_id: int) -> bool:
    return (REWARD_NODE_NEW.search(text) is not None
            and HEAL_ROUTE in text
            and text.count(HEAL_ROUTE) == 1
            and not REWARD_NODE_OLD.search(text))


def apply_quest(quest_id: int, check: bool) -> bool:
    path = QUESTS / f"{quest_id}.xml"
    text = read(path)
    if check:
        return contract_ok(text, quest_id)
    if contract_ok(text, quest_id):
        return True
    # 1) reward 节点投影 0 -> 1（只改 reward 节点的 var0）。
    #    Reward node projection 0 -> 1 (only the reward node's var0).
    new_text, replaced = REWARD_NODE_OLD.subn(r"\g<1>1\g<2>", text, count=1)
    if replaced != 1:
        print(f"BATCH13_APPLY_FAIL quest={quest_id} reason=reward-node-not-0")
        return False
    # 2) 补无 source 的 enter-world 自愈边。
    #    Add the source-less enter-world heal edge.
    block = comment(quest_id) + HEAL_ROUTE
    if ENTRY_ROUTE_ANCHOR in new_text:
        new_text = new_text.replace(ENTRY_ROUTE_ANCHOR, block + ENTRY_ROUTE_ANCHOR, 1)
    elif "    <npc-complete" in new_text:
        marker = "    <npc-complete"
        new_text = new_text.replace(marker, block + marker, 1)
    else:
        marker = "  </transitions>"
        new_text = new_text.replace(marker, block + marker, 1)
    path.write_text(new_text, encoding="utf-8")
    return contract_ok(new_text, quest_id)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="只验证目标状态，不写文件")
    args = parser.parse_args()

    results = [(quest_id, apply_quest(quest_id, args.check)) for quest_id in FAMILY]
    for quest_id, ok in results:
        print(f"BATCH13_{'CHECK' if args.check else 'APPLY'}_{'OK' if ok else 'FAIL'} quest={quest_id}")
    return 0 if all(ok for _, ok in results) else 1


if __name__ == "__main__":
    sys.exit(main())
