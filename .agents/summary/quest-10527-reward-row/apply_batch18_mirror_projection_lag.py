#!/usr/bin/env python3
"""批次 18：镜像单侧投影落后族（11110/14201/16974/17160/17161/17526）。

族级口径（批次 1-3 的镜像方法）：同形镜像对（q ↔ q±10000）客户端 quest_summary 行数相同、末行 NPC
都能在任务内对上（审计 last_row_npc_matches_quest=True），但只有一侧的 reward 投影等于领奖行。
一族里“已对齐的那一侧”就是另一侧的参照基线。

本批 6 个任务都已具备完整的 0..N-1 行状态（审计 rows_without_state 为空），只是 reward 节点仍停在行 0：
- 11110 ← 21110（行 1 = 和 Suleion 对话；21110 已是 1）
- 14201 ← 24201（行 2 = 和 Atropos 对话；24201 已是 2；legacy `changeQuestStep(2->2)` 也确认领奖态保持 step 2）
- 16974 ← 26974（行 1 = 和 IDLDF5_Under_01_Theano_E 对话）
- 17160 ← 27160（行 1 = 向 LF5_Atmos_E 报告）
- 17161 ← 27161（行 1 = 向 LF5_Atmos_E 报告）
- 17526 ← 27526（行 1 = 向 Ab1_Plania_E 报告）
修复 = reward 投影 0 -> 镜像值 + 无 source 的 `REWARD && var0==0 -> set 镜像值` 自愈边。

**同族但锁定、本批不改**：16837/16986/16988 被 QuestPrematureRewardRouteExclusionTest 断言
“完成报告后 packed var0 保持 0”（RewardCase.reward = {var0:0}），属既有基线，需客户端观测才能重定。

Batch 18 mirrors the batch-1..3 method: when one shard of a mirror pair already projects the reward
journal row and the other still sits on row 0, the aligned shard is the reference. 16837/16986/16988 are
excluded because an existing gate locks their post-report packed step at 0.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

# quest_id -> (镜像任务, 镜像的领奖行, 末行 NPC 说明)
TARGETS = {
    11110: (21110, 1, "和 Suleion（799075）对话"),
    14201: (24201, 2, "和 Atropos 对话（legacy changeQuestStep(2->2) 亦确认领奖态保持 packed step 2）"),
    16974: (26974, 1, "和 IDLDF5_Under_01_Theano_E 对话"),
    17160: (27160, 1, "向 LF5_Atmos_E 报告"),
    17161: (27161, 1, "向 LF5_Atmos_E 报告"),
    17526: (27526, 1, "向 Ab1_Plania_E 报告"),
}
STALE_ROW = 0

HEAL = """    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="{row}"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
"""


def comment(quest_id: int) -> str:
    mirror, row, last_row = TARGETS[quest_id]
    return f"""    <!-- QE-051 领奖行（批次 18，镜像单侧投影落后族）：镜像对 {quest_id} / {mirror} 的客户端 quest_summary
         行数相同、末行 NPC 都能在任务内对上，但 {mirror} 的 reward 投影已经是 {row}（领奖行），
         {quest_id} 却仍停在行 0——一族里已对齐的那一侧就是参照基线：行 {row} = “{last_row}”。
         因此 {quest_id} 的 reward 投影固定为 {row}，并补 REWARD/var0=0 -> {row} 的 enter-world 自愈边。
         Mirror-pair reward-row contract (batch 18): the aligned shard projects row {row}, so this shard
         must project the same row and heal stale REWARD/var0=0 saves. -->
"""


def reward_node(value: int) -> re.Pattern[str]:
    return re.compile(r'(<node label="reward" status="REWARD">\s*<var name="var0" value=")' + str(value) + r'(")')


def heal_block(quest_id: int) -> str:
    return HEAL.format(row=TARGETS[quest_id][1])


def contract_ok(text: str, quest_id: int) -> bool:
    row = TARGETS[quest_id][1]
    return (reward_node(row).search(text) is not None
            and reward_node(STALE_ROW).search(text) is None
            and text.count(heal_block(quest_id)) == 1)


def apply_quest(quest_id: int, check: bool) -> bool:
    path = QUESTS / f"{quest_id}.xml"
    text = path.read_text(encoding="utf-8")
    if check:
        return contract_ok(text, quest_id)
    if contract_ok(text, quest_id):
        return True
    row = TARGETS[quest_id][1]
    new_text, replaced = reward_node(STALE_ROW).subn(r"\g<1>" + str(row) + r"\g<2>", text, count=1)
    if replaced != 1:
        print(f"BATCH18_APPLY_FAIL quest={quest_id} reason=reward-node")
        return False
    if heal_block(quest_id) not in new_text:
        new_text = new_text.replace("    <npc-complete", comment(quest_id) + heal_block(quest_id) + "    <npc-complete", 1)
    path.write_text(new_text, encoding="utf-8")
    return contract_ok(new_text, quest_id)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="只验证目标状态，不写文件")
    args = parser.parse_args()
    results = [(quest_id, apply_quest(quest_id, args.check)) for quest_id in TARGETS]
    for quest_id, ok in results:
        print(f"BATCH18_{'CHECK' if args.check else 'APPLY'}_{'OK' if ok else 'FAIL'} quest={quest_id}")
    return 0 if all(ok for _, ok in results) else 1


if __name__ == "__main__":
    sys.exit(main())
