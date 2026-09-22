#!/usr/bin/env python3
"""批次 16：领奖 owner 收敛（4712 新增收口 + 2484 复核）。

口径（批次 12 确立）：客户端 quest_summary 领奖行点名的 NPC 就是领奖/completion NPC；行 0 的
交互对象、接取 NPC 与行 0 目标物件都不得兼任领奖。

- 4712（Escape From The Dredgion，魔族）：行 0 = “打开 STR_DIC_E_Dreadgion_Prison_Door 后，和
  IDAB1_Dreadgion_prisoner_dark 对话”（[%0]），行 1 = “向 STR_DIC_N_Henir 报告”（[%3] = 值 1）。
  legacy `_4712Escape_From_The_Dredgion`：START 态下囚犯 798327/798330 的 STEP_TO_1 →
  `defaultCloseDialog(env, 0, 1, true, false)`（**写 var0=1 并置 REWARD**，同时 onDelete 删除囚犯），
  REWARD 态只处理 279042（`sendQuestDialog(10002)` / `sendQuestEndDialog`）→ 领奖台 = 279042 = Henir。
  Henir 解键：`STR_DIC_N_Henir` 出现在 4711–4716 行内，且 4713/4714/4716（审计已 ALIGNED，reward=1）
  的 completion owner 全部是 279042 —— 同族 4/4 交叉一致。
  本批：reward 投影 0→1 + 无 source 自愈边（旧存档 REWARD/var0=0，含 legacy 之前的 from=0），
  并删除囚犯 798327/798330 上的 npc-complete（保留它们的 NPC_REPORT -> reward 入口路由，即行 0 的
  开监狱门交互），使领奖唯一落在 279042。
- 2484（Our Man In Elysea，魔族）复核：批次 15 只收口了行投影，completion 仍同时登记在烽火对象
  700267 与接取 NPC 204407 上。legacy `_2484OurManInElysea` 只在 203331（Hippolyta_Q2484）处
  `setStatus(REWARD)`（700267 只 `setQuestVarById(0, 1)`，204407 只负责接取）→ 本批删除 204407 与
  700267 上的 npc-complete，保留三条 NPC_REPORT -> reward 入口路由，使领奖唯一落在 203331。

Batch 16 trims the reward completion owners to the client journal's reward-row NPC, keeping the
row-0 entry routes: 4712 (Dredgion escape; Henir = 279042, resolved through 4713/4714/4716) and 2484
(legacy entered REWARD only at Hippolyta 203331, never on the beacon 700267 or the start NPC 204407).
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

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

COMMENT_4712 = """    <!-- QE-051 领奖行 + QE-052 领奖 owner：客户端 quest_q4712.html 只有 2 行（行 0 开监狱门并与囚犯对话、
         行 1 向 Henir 报告）；legacy `_4712Escape_From_The_Dredgion` 在囚犯 798327/798330 处
         defaultCloseDialog(env, 0, 1, true, false)（写 var0=1 并置 REWARD，囚犯随即 onDelete），
         REWARD 态只在 279042 处发送 10002/结束对话。Henir 解键：同族 4713/4714/4716 的 completion
         owner 全部是 279042（客户端行内 STR_DIC_N_Henir）。因此 reward 投影固定为领奖行 1，
         自愈旧存档 REWARD/var0=0，并删除囚犯上的 npc-complete（保留其入口路由）。
         Reward-row + ownership contract: the second journal row reports to Henir(279042) and the legacy
         handler entered REWARD on the prisoners with var0=1 while only 279042 ever ends the quest. -->
"""

# 目标状态：quest_id -> (行 1 领奖 NPC, 必须删除 npc-complete 的非领奖 owner)
TARGETS = {
    4712: (279042, (798327, 798330)),
    2484: (203331, (204407, 700267)),
}

REWARD_NODE_OLD = re.compile(r'(<node label="reward" status="REWARD">\s*<var name="var0" value=")0(")')
REWARD_NODE_NEW = re.compile(r'(<node label="reward" status="REWARD">\s*<var name="var0" value=")1(")')


def completion_block(npc_id: int) -> re.Pattern[str]:
    return re.compile(r'[ \t]*<npc-complete npc-id="%d".*?</npc-complete>\n' % npc_id, re.S)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def contract_ok(text: str, quest_id: int) -> bool:
    owner, trimmed = TARGETS[quest_id]
    return (REWARD_NODE_NEW.search(text) is not None
            and text.count(HEAL_ROUTE) == 1
            and not REWARD_NODE_OLD.search(text)
            and f'<npc-complete npc-id="{owner}"' in text
            and all(completion_block(npc).search(text) is None for npc in trimmed))


def apply_quest(quest_id: int, check: bool) -> bool:
    path = QUESTS / f"{quest_id}.xml"
    text = read(path)
    if check:
        return contract_ok(text, quest_id)
    if contract_ok(text, quest_id):
        return True
    owner, trimmed = TARGETS[quest_id]
    new_text = text
    # 1) reward 节点投影 0 -> 1。 / Reward node projection 0 -> 1.
    if REWARD_NODE_OLD.search(new_text):
        new_text, replaced = REWARD_NODE_OLD.subn(r"\g<1>1\g<2>", new_text, count=1)
        if replaced != 1:
            print(f"BATCH16_APPLY_FAIL quest={quest_id} reason=reward-node")
            return False
    # 2) 补无 source 自愈边（4712 需要；2484 已有）。 / Add the source-less heal edge when missing.
    if HEAL_ROUTE not in new_text:
        block = (COMMENT_4712 if quest_id == 4712 else "") + HEAL_ROUTE
        new_text = new_text.replace("    <npc-complete", block + "    <npc-complete", 1)
    # 3) 删除非领奖 owner 的 npc-complete 完成块。 / Drop completion blocks owned by non-reward NPCs.
    for npc in trimmed:
        new_text, removed = completion_block(npc).subn("", new_text, count=1)
        if removed != 1:
            print(f"BATCH16_APPLY_FAIL quest={quest_id} reason=npc-complete-{npc}-not-found")
            return False
    path.write_text(new_text, encoding="utf-8")
    return contract_ok(new_text, quest_id)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="只验证目标状态，不写文件")
    args = parser.parse_args()

    results = [(quest_id, apply_quest(quest_id, args.check)) for quest_id in TARGETS]
    for quest_id, ok in results:
        print(f"BATCH16_{'CHECK' if args.check else 'APPLY'}_{'OK' if ok else 'FAIL'} quest={quest_id}")
    return 0 if all(ok for _, ok in results) else 1


if __name__ == "__main__":
    sys.exit(main())
