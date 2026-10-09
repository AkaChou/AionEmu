#!/usr/bin/env python3
"""把「无任何 var0 写入面」任务的 reward 节点投影归 0（10522 同病批次收口）。

判据（2026-10-09 排查定谳，见同目录 AUDIT.zh-CN.md）：任务从接取到 REWARD 的路径上没有
任何 var0 写入面（无 set-variable、无引擎外写入方）⇒ 轴恒为接取值 0 ⇒ reward 投影必须 = 0。
只改 `<node label="reward">` 块内的 var0 值；已经是 0 的行跳过（幂等）。

用法：
  python3 apply_reward_axis_zero.py 30231 1311 18302      # 显式任务列表
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
DEF = ROOT / "src/main/resources/aion/data/static_data/quest/definitions/quests"

REWARD_NODE = re.compile(r'(<node label="reward"[^>]*>)(.*?)(</node>)', re.S)
VAR0 = re.compile(r'<var name="var0" value="(\d+)"/>')
NODE_VAR = re.compile(r'<var name="var0" value="(\d+)"/>')


def fix(quest_id: int) -> str:
    path = DEF / f"{quest_id}.xml"
    text = path.read_text(encoding="utf-8")

    m = REWARD_NODE.search(text)
    if not m:
        return f"{quest_id}: SKIP no reward node"
    vm = VAR0.search(m.group(2))
    if not vm:
        return f"{quest_id}: SKIP reward node has no var0"
    old = int(vm.group(1))
    if old == 0:
        return f"{quest_id}: SKIP already 0"

    def rewrite(block: re.Match) -> str:
        body = NODE_VAR.sub('<var name="var0" value="0"/>', block.group(2), count=1)
        return block.group(1) + body + block.group(3)

    new_text, count = REWARD_NODE.subn(rewrite, text, count=1)
    if count != 1:
        return f"{quest_id}: FAIL rewrite"
    path.write_text(new_text, encoding="utf-8")
    return f"{quest_id}: {old} -> 0"


def main() -> int:
    ids = [int(a) for a in sys.argv[1:]]
    if not ids:
        print("usage: apply_reward_axis_zero.py <questId...>")
        return 2
    for quest_id in ids:
        print(fix(quest_id))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
