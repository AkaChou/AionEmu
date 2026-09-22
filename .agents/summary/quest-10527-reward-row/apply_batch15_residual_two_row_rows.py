#!/usr/bin/env python3
"""批次 15：残余“两行、末行是与领奖 NPC 的对话”族收口（1123 / 2484）。

族定义与批次 13/14 相同：客户端 quest_summary 恰好 2 行，行 1 是“和 X 对话 / 任务完成！和 X 对话”。
本批是审批过的同形候选里的最后两个可安全收口的任务（其余见 DEFERRED）。

- 1123（Where's Tutty?，天族）：行 0 = 到 STR_DIC_FLA07 寻找 STR_DIC_LA53 的踪迹（[%0]）；
  行 1 = “任务完成！和 STR_DIC_LA12 对话。”（[%3] = 值 1）。`STR_DIC_LA12` 是客户端“actor 名”命名空间，
  不在 client_npcs_npc.xml 的 `STR_DIC_N_*` 键里，故审计的 `last_row_npc_matches_quest` 为 False；
  本批用同族交叉证据解键：1006（末行“和 STR_DIC_LA12 对话，选择将来之路”）、1122、1124、30507
  四个任务的 `npc-complete` owner 都是 790001，而客户端 NPC 表 790001 = Pernos，即 STR_DIC_LA12 = Pernos；
  1123 自己的 start owner 与 npc-complete owner 也都是 790001，与之自洽。定义把 reward 投影停在 0，
  而进入 REWARD 的唯一路由（LF1_SENSORY_AREA_Q1123_210010000 enter-zone + play-movie 11）不写 var0，
  于是领奖态在任务书里渲染已完成的行 0。
- 2484（Our Man In Elysea，魔族）：行 0 = 在 LF2_BeaconFire_Q2484 上点燃烽火（[%0]）；
  行 1 = “和 STR_DIC_N_Hippolyta 对话。”（[%3] = 值 1）。legacy `_2484OurManInElysea`（7e9f0316c^）
  在 700267（烽火对象）处 `setQuestVarById(0, 1)`，之后在 203331（Hippolyta_Q2484）处 `setStatus(REWARD)`，
  即 legacy 的领奖行就是 1；typed 定义的三条 `NPC_REPORT`（204407/700267/203331 -> reward）都没有
  var0 动作，靠 reward 投影补足，所以投影必须等于 1。

Batch 15 closes the last two safely repairable quests of the two-row reward-row family. Both move the
reward projection to 1 and add the source-less enter-world heal edge for stale REWARD/var0=0 saves.

DEFERRED（本批不改，只登记证据）：
- 1466：被 `Quest1466ClientDialogAlignmentTest` 硬锁（`reward` 节点必须是**无投影**、报告路由必须写
  `var0=2`，且 headless retail journey 通过）。按 QE-051 收到行 1 需要同时改该锁（补投影 + 自愈边 +
  把报告路由的 2 改成 1），属重定基线，需一次客户端观测（领奖态任务书显示行 0/行 1/空白）。
- 4712：客户端行 1 = “向 STR_DIC_N_Henir 报告”，但定义的 completion owner 是 279042（客户端表无名）
  与 798327/798330（IDAB1_Dreadgion_prisoner_dark1/4）——归属疑与批次 12 同型的“报告 NPC 未登记”，
  需单独的归属取证。
- 2842：var0 是 0..39 的击杀计数（行 0 显示 `[%2]/39`），且行 1 的可见槽位是 `[%15]`（= 值 5），
  不是 1，属“var0 不是行索引”族。
- 50008/51008：legacy 用 `setQuestVarById(0, var0 + 1)` 把 var0 当 0→2 的投递计数，行内 NPC 名
  （HousingLf_Event_ShugoSanta / E_HousingDF_Event_ShugaShugo）不在 5.8 客户端 NPC 表里。
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

# quest_id -> (行 1 NPC 名, 行 1 NPC id, 证据)
FAMILY = {
    1123: ("Pernos", 790001, "行 1 = “任务完成！和 STR_DIC_LA12 对话。”；STR_DIC_LA12 由同族 1006/1122/1124/30507 的 npc-complete owner 全部 = 790001 解出，客户端 NPC 表 790001 = Pernos；本任务 start owner 与 npc-complete owner 同为 790001；进入 REWARD 的 enter-zone 路由不写 var0"),
    2484: ("Hippolyta_Q2484", 203331, "行 1 = 和 STR_DIC_N_Hippolyta 对话（客户端表 203331 = Hippolyta_Q2484）；legacy `_2484OurManInElysea` 在 700267（LF2_BeaconFire_Q2484）处 setQuestVarById(0, 1)，随后在 203331 处 setStatus(REWARD)"),
}

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
        f"    <!-- QE-051 领奖行：客户端 quest_q{quest_id}.html 只有 2 行（行 0 目标、行 1 与 {name} 对话）；\n"
        f"         旧 reward 投影停在 0，所以领奖态仍渲染行 0。证据：{evidence}。\n"
        f"         因此 reward 投影固定为领奖行 1，并补 REWARD/var0=0 的 enter-world 自愈边（旧存档旧值\n"
        f"         只会来自旧投影，语义无歧义）。\n"
        f"         Reward-row contract (QE-051): the second client journal row talks to {name}({npc_id}) and the\n"
        f"         definition's reward completion NPC is the same id, so the reward projection is 1 and stale\n"
        f"         REWARD/var0=0 saves are healed on enter-world. -->\n"
    )


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def contract_ok(text: str, quest_id: int) -> bool:
    return (REWARD_NODE_NEW.search(text) is not None
            and text.count(HEAL_ROUTE) == 1
            and not REWARD_NODE_OLD.search(text))


def apply_quest(quest_id: int, check: bool) -> bool:
    path = QUESTS / f"{quest_id}.xml"
    text = read(path)
    if check:
        return contract_ok(text, quest_id)
    if contract_ok(text, quest_id):
        return True
    # 1) reward 节点投影 0 -> 1。 / Reward node projection 0 -> 1.
    new_text, replaced = REWARD_NODE_OLD.subn(r"\g<1>1\g<2>", text, count=1)
    if replaced != 1:
        print(f"BATCH15_APPLY_FAIL quest={quest_id} reason=reward-node-not-0")
        return False
    # 2) 补无 source 的 enter-world 自愈边。 / Add the source-less enter-world heal edge.
    block = comment(quest_id) + HEAL_ROUTE
    if "    <npc-complete" in new_text:
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
        print(f"BATCH15_{'CHECK' if args.check else 'APPLY'}_{'OK' if ok else 'FAIL'} quest={quest_id}")
    return 0 if all(ok for _, ok in results) else 1


if __name__ == "__main__":
    sys.exit(main())
