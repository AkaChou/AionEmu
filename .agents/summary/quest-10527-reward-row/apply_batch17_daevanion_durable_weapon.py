#!/usr/bin/env python3
"""批次 17：圣灵守护者武器事件族（80290/80291/80294/80295）领奖行收口。

全库扫描（legacy 入口 helper：useQuestItem / defaultCloseDialog / checkQuestItems /
checkQuestItemsSimple / changeQuestStep，共 237 个涉及“step != nextStep 且 reward=true”的任务）后，
“nextStep 被迁移丢弃且 nextStep == 客户端任务书末行”的只剩 4 个：15300/25300（QE-045 锁定基线，
不得动）与 80291/80295（本批收口）。

- 80291/80295（Durable Daevanion Weapon，天/魔）：客户端 quest_summary 2 行——行 0“收集 5 个
  relic_weapon_30，交给 EVENT_Zephyrin/EVENT_Lilyolin”，行 1“从 X 那里获得圣灵守护者武器”。
  legacy `checkQuestItems(env, 0, 1, true, 5, 0)`：把 packed step 从 0 推到 1 并置 REWARD，
  迁移只保留 REWARD、把 nextStep=1 丢了（reward 节点投影仍是 0）→ 领奖态任务书停在行 0。
  修复：reward 投影 0 -> 1 + 无 source 的 `REWARD && var0==0 -> set 1` 自愈边。
- 80290/80294（同族护甲变体，EVENT_Zephyrin/EVENT_Lilyolin）：客户端 quest_summary 只有 1 行
  （“收集 10 个 relic_armor_30，交给 X”），reward 投影却是 1 → 领奖态落在客户端不存在的行号上
  （审计 STATE_OUT_OF_RANGE），任务书在领奖态无行可高亮。
  修复：reward 投影 1 -> 0 + 无 source 的 `REWARD && var0==1 -> set 0` 自愈边。
  注意：这两个任务在迁移前仓库里没有 legacy handler（同族只有 80291/80295 的 handler），
  这里以客户端行数（1 行）+ 同族 80291/80295 的 legacy nextStep 证据 + 844 个已对齐的单行任务为准。

Batch 17 closes the Durable Daevanion Weapon event family: the weapon variants keep the legacy
`checkQuestItems(env, 0, 1, true, 5, 0)` reward step (1) and the armour variants must stay on the
client's only journal row (0).
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

COMMENT = """    <!-- QE-051 领奖行（批次 17，圣灵守护者武器事件族）：客户端 quest_q{quest}.html 的 quest_summary
         共 {rows} 行，行 {row} = “{last_row}”。
         {evidence}
         Reward-row contract (batch 17): the client journal has {rows} row(s) and the reward projection must
         be {row}. -->
"""

# quest_id -> (领奖行, 旧投影, 旧值集合, 说明)
TARGETS = {
    80291: (1, 0, "legacy `_80291Durable_Daevanion_Weapon` 的 checkQuestItems(env, 0, 1, true, 5, 0) 把 packed step\n         从 0 推到 1 并置 REWARD，迁移丢了 nextStep=1（reward 投影仍是 0）"),
    80295: (1, 0, "legacy `_80295Durable_Daevanion_Weapon` 的 checkQuestItems(env, 0, 1, true, 5, 0) 同上（魔族镜像，同 831387 = event_Lilyolin）"),
    80290: (0, 1, "同族护甲变体：只有 1 行任务书，领奖态必须落在行 0（当前投影 1 落在客户端不存在的行号上，审计 STATE_OUT_OF_RANGE）；\n         迁移前仓库无该任务 handler，以客户端行数 + 同族 80291 的 legacy nextStep 证据为准"),
    80294: (0, 1, "同族护甲变体：只有 1 行任务书，领奖态必须落在行 0（当前投影 1 越界）；\n         迁移前仓库无该任务 handler，以客户端行数 + 同族 80295 的 legacy nextStep 证据为准"),
}

HEAL = """    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="{stale}"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="{row}"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
"""

LAST_ROWS = {
    80290: "收集10个 STR_DIC_LC_relic_armor_30，交给 STR_DIC_N_EVENT_Zephyrin",
    80291: "从 STR_DIC_N_EVENT_Zephyrin 那里获得圣灵守护者武器",
    80294: "收集10个 STR_DIC_DC_relic_armor_30，交给 STR_DIC_N_EVENT_Lilyolin",
    80295: "从 STR_DIC_N_EVENT_Lilyolin 那里获得圣灵守护者武器",
}


def reward_node(value: int) -> re.Pattern[str]:
    return re.compile(r'(<node label="reward" status="REWARD">\s*<var name="var0" value=")' + str(value) + r'(")')


def heal_block(quest_id: int) -> str:
    row, stale = TARGETS[quest_id][0], TARGETS[quest_id][1]
    return HEAL.format(stale=stale, row=row)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def contract_ok(text: str, quest_id: int) -> bool:
    row, stale = TARGETS[quest_id][0], TARGETS[quest_id][1]
    return (reward_node(row).search(text) is not None
            and reward_node(stale).search(text) is None
            and text.count(heal_block(quest_id)) == 1
            and text.count("<npc-complete ") == 1)


def apply_quest(quest_id: int, check: bool) -> bool:
    path = QUESTS / f"{quest_id}.xml"
    text = read(path)
    if check:
        return contract_ok(text, quest_id)
    if contract_ok(text, quest_id):
        return True
    row, stale = TARGETS[quest_id][0], TARGETS[quest_id][1]
    new_text = text
    if reward_node(stale).search(new_text):
        new_text, replaced = reward_node(stale).subn(r"\g<1>" + str(row) + r"\g<2>", new_text, count=1)
        if replaced != 1:
            print(f"BATCH17_APPLY_FAIL quest={quest_id} reason=reward-node")
            return False
    if heal_block(quest_id) not in new_text:
        block = COMMENT.format(quest=quest_id, rows=2 if row == 1 else 1, row=row,
                               last_row=LAST_ROWS[quest_id],
                               # 注释里把 evidence 缩进对齐
                               evidence="         " + TARGETS[quest_id][2])
        new_text = new_text.replace("    <npc-complete", block + heal_block(quest_id) + "    <npc-complete", 1)
    path.write_text(new_text, encoding="utf-8")
    return contract_ok(new_text, quest_id)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="只验证目标状态，不写文件")
    args = parser.parse_args()
    results = [(quest_id, apply_quest(quest_id, args.check)) for quest_id in TARGETS]
    for quest_id, ok in results:
        print(f"BATCH17_{'CHECK' if args.check else 'APPLY'}_{'OK' if ok else 'FAIL'} quest={quest_id}")
    return 0 if all(ok for _, ok in results) else 1


if __name__ == "__main__":
    sys.exit(main())
