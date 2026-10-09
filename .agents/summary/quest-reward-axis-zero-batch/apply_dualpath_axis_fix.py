#!/usr/bin/env python3
"""双路径型（普通 started→reward 边 + batch8 ENTER 自愈边 set 1）的轴收口。

对每个任务执行两处改动（同 10522/10525 先例）：
1. reward 节点投影 var0 → 0（新流程经普通边进 REWARD，轴保持接取值 0）；
2. enter-world 自愈边反转：条件 var0=0 → var0=1（捕获 batch8 时代被自愈边本身
   污染的 REWARD/var0=1 存量档），动作 set 1 → set 0（落盘纠正）。

用法：python3 apply_dualpath_axis_fix.py <questId...>
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
DEF = ROOT / "src/main/resources/aion/data/static_data/quest/definitions/quests"

REWARD_NODE = re.compile(r'(<node label="reward"[^>]*>)(.*?)(</node>)', re.S)
NODE_VAR0 = re.compile(r'<var name="var0" value="(\d+)"/>')
# unsourced enter-world → reward 自愈边（含 conditions/actions 整块）
HEAL_EDGE = re.compile(
    r'(<transition\b(?![^>]*source=)[^>]*target="reward"[^>]*>\s*<event>\s*<enter-world/>'
    r'.*?</transition>)', re.S)


def fix(quest_id: int) -> str:
    path = DEF / f"{quest_id}.xml"
    text = path.read_text(encoding="utf-8")
    notes = []

    def rewrite_reward(block: re.Match) -> str:
        body = NODE_VAR0.sub('<var name="var0" value="0"/>', block.group(2), count=1)
        return block.group(1) + body + block.group(3)

    new_text, n = REWARD_NODE.subn(rewrite_reward, text, count=1)
    if n == 1:
        notes.append("proj->0")

    def rewrite_heal(block: re.Match) -> str:
        edge = block.group(1)
        edge2 = edge.replace('<variable-is field="var0" value="0"/>',
                             '<variable-is field="var0" value="1"/>', 1)
        edge2 = edge2.replace('<set-variable field="var0" value="1"/>',
                              '<set-variable field="var0" value="0"/>', 1)
        return edge2

    new_text, n2 = HEAL_EDGE.subn(rewrite_heal, new_text, count=1)
    if n2 == 1:
        notes.append("heal reversed")
    if not notes:
        return f"{quest_id}: SKIP nothing to do"
    if n2 == 0:
        return f"{quest_id}: FAIL heal edge not found (proj {notes})"
    path.write_text(new_text, encoding="utf-8")
    return f"{quest_id}: " + ", ".join(notes)


def main() -> int:
    ids = [int(a) for a in sys.argv[1:]]
    if not ids:
        print("usage: apply_dualpath_axis_fix.py <questId...>")
        return 2
    for quest_id in ids:
        print(fix(quest_id))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
