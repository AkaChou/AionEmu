#!/usr/bin/env python3
"""批次 14：事件族“两行、末行是与领奖 NPC 的对话”领奖行收口（4 个任务）。

族定义（与批次 13 同一模板）：客户端 quest_summary 恰好 2 行，行 0 是“使用道具/击杀目标”，行 1 是
“与 X 对话 / 向 X 报告”，且行 1 的 NPC 就是定义里的领奖/完成 NPC。定义把 reward 投影停在 0，于是
REWARD 态在任务书里仍渲染行 0，行 1 永远没有状态；本批把 `reward` 节点投影 0 → 1，并补一条无
source 的 enter-world 自愈边（REWARD/var0=0 -> set var0=1），纠正旧存档。

两组证据：
- 80255/80256（活动烟花族）：同族 80257/80258/80259/80260 在批次 1-7（commit 7a7d27809）已收口为
  `reward var0=1` + 同形自愈边，本批两个是漏网项——它们的行 1 用字面中文名（“和帕尔图对话”），
  没有 `STR_DIC_N_` 键，所以按“末行 NPC 键”筛选的批次 1-7 没抓到；归属由客户端 NPC 表确认
  （831163 = event_Parutoo、831164 = event_Boobanah），且与 npc-complete owner 一致。
- 80601/80606（德雷得奇安事件族“头本”）：legacy `_80601Fight_Of_The_Navigators` /
  `_80606The_Good_News_And_Bad` 的击杀分支显式 `setQuestVarById(0, 1)` 之后才 `setStatus(REWARD)`，
  即 legacy 的领奖行就是 1；typed 击杀事务也保留 `set-variable var0=1`，但 `reward` 节点投影仍是 0，
  于是经 `NPC_REPORT`（无 actions，投影补足为 0）进入领奖态的存档匹配不到任何 reward 路由。
  客户端 NPC 表 831831 = event_Isda、831832 = event_Charmeine，对应行 1“向伊斯达/夏尔梅因报告”。
  同族 80602..80605/80607..80610 的 var0 是 3/4/5/9 的阶段计数（审计 STATES_BEYOND_ROWS），
  不是行索引，本批不动。

Batch 14: the event-quest subgroup of the "two-row journal whose second row talks to the reward NPC"
family. The reward projection moves 0 -> 1 and a source-less enter-world heal edge repairs stale
REWARD/var0=0 saves. 80255/80256 are the two stragglers of the 80255..80260 series whose aligned
siblings 80257..80260 were closed by batch 1-7; 80601/80606 are the Dredgion event chain heads whose
legacy handlers explicitly wrote `setQuestVarById(0, 1)` before `setStatus(REWARD)`.

边界（本批不改）：
- 80602..80605 / 80607..80610：客户端同是 2 行但 var0 是 3/4/5/9 的阶段计数（STATES_BEYOND_ROWS），
  属“var0 不是行索引”族，需单独设计。
- 50008/51008：legacy 用 `setQuestVarById(0, var0 + 1)` 把 var0 当 0→2 的投递计数，且行内 NPC 名
  （HousingLf_Event_ShugoSanta / E_HousingDF_Event_ShugaShugo）不在 5.8 客户端 NPC 表里，归属无法核对。
- 1123/2484/4712/2842/1466：行内名字是 `STR_DIC_LA12`/`STR_DIC_FLA07` 等非 NPC 键，或 completion
  owner 不唯一（2484/4712），或 reward 节点根本没有 var0 投影（1466），需单独取证。
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
    80255: ("帕尔图", 831163, "客户端行 1 = 和帕尔图对话（行内无 dic 键），客户端 NPC 表 831163 = event_Parutoo；npc-complete owner = 831163；同族 80257-80260 已是 reward var0=1 + 自愈边（7a7d27809）"),
    80256: ("布巴纳", 831164, "客户端行 1 = 和布巴纳对话（行内无 dic 键），客户端 NPC 表 831164 = event_Boobanah；npc-complete owner = 831164；同族 80257-80260 已是 reward var0=1 + 自愈边（7a7d27809）"),
    80601: ("伊斯达", 831831, "客户端行 1 = 向伊斯达报告，客户端 NPC 表 831831 = event_Isda；legacy `_80601Fight_Of_The_Navigators` 击杀分支 setQuestVarById(0, 1) 后 setStatus(REWARD)，typed 击杀事务同写法；npc-complete owner = 831831"),
    80606: ("夏尔梅因", 831832, "客户端行 1 = 向夏尔梅因报告，客户端 NPC 表 831832 = event_Charmeine；legacy `_80606The_Good_News_And_Bad` 同 80601 结构；npc-complete owner = 831832"),
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
        print(f"BATCH14_APPLY_FAIL quest={quest_id} reason=reward-node-not-0")
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
        print(f"BATCH14_{'CHECK' if args.check else 'APPLY'}_{'OK' if ok else 'FAIL'} quest={quest_id}")
    return 0 if all(ok for _, ok in results) else 1


if __name__ == "__main__":
    sys.exit(main())
