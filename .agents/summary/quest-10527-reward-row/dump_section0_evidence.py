#!/usr/bin/env python3
"""逐条打印“客户端 quest_script 声明了 SECTION_0”的任务证据，供人工判定 var0 是否就是任务书行索引。

输出（stdout，建议重定向到 section0-evidence.txt）：
- 客户端 quest_summary 的每一行文案 + visible 槽位
- 客户端 quest_script_monster.csv 里该任务声明了 SECTION_0 的 Progress(...) 原文
- 服务端 typed 定义的 START/REWARD 节点（label/status/var0）与 var0 字段上限、进入 reward 的路线
- 镜像任务（q ± 10000）的同样摘要

用法：
    python3 .agents/summary/quest-10527-reward-row/dump_section0_evidence.py [quest_id ...]
"""

from __future__ import annotations

import importlib.util
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]

spec = importlib.util.spec_from_file_location("audit", HERE / "audit_reward_row_vs_client_steps.py")
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


def visible_slots(quest_id: int) -> list[str]:
    path = audit.client_index().get(quest_id)
    if path is None:
        return []
    text = path.read_text(encoding="utf-8", errors="ignore")
    page = re.search(r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', text, re.S)
    if page is None:
        return []
    slots = []
    for step in re.findall(r"<step>(.*?)</step>", page.group(1), re.S):
        match = re.search(r'visible="\[%(\d+)\]"', step)
        slots.append(f"%{match.group(1)}" if match else "?")
    return slots


def definition_nodes(quest_id: int) -> str:
    summary = audit.definition_summary(quest_id)
    if summary is None:
        return "    (无 typed 定义)"
    lines = [f"    var0_max={summary['var0_max']} increments_var0={summary['increments_var0']} "
             f"reward={summary['reward']} recovery={summary['recovery']}",
             f"    visible_rows={summary['visible_rows']}",
             f"    handovers={summary['handovers']} writes={summary['handover_writes']}"]
    path = audit.QUESTS_DIR / f"{quest_id}.xml"
    text = path.read_text(encoding="utf-8")
    nodes = re.search(r"<nodes>(.*?)</nodes>", text, re.S)
    for node in re.findall(r"<node\b[^>]*label=\"([^\"]+)\"[^>]*status=\"([^\"]+)\"[^>]*>(.*?)</node>",
                           nodes.group(1) if nodes else "", re.S):
        var0 = re.search(r'name="var0" value="(-?\d+)"', node[2])
        lines.append(f"      {node[0]} {node[1]} var0={var0.group(1) if var0 else '?'}")
    return "\n".join(lines)


def dump(quest_id: int) -> None:
    rows = audit.client_rows(quest_id) or []
    slots = visible_slots(quest_id)
    print(f"\n=== quest {quest_id} ({len(rows)} 行) ===")
    for index, row in enumerate(rows):
        slot = slots[index] if index < len(slots) else "?"
        print(f"    row{index} {slot}: {row[:90]}")
    print(f"    script: {audit.client_section0(quest_id) or '(无 SECTION_0 声明)'}")
    print("  server:")
    print(definition_nodes(quest_id))
    mirror = audit.mirror_of(quest_id)
    if mirror is not None and (audit.QUESTS_DIR / f"{mirror}.xml").exists():
        mirror_rows = audit.client_rows(mirror) or []
        print(f"  mirror {mirror} ({len(mirror_rows)} 行) script: {audit.client_section0(mirror) or '(无)'}")
        print("    server:")
        print(definition_nodes(mirror))


def main() -> None:
    import sys

    requested = [int(value) for value in sys.argv[1:] if value.isdigit()]
    if not requested:
        requested = sorted(audit.CLIENT_SECTION0.keys() if audit.CLIENT_SECTION0 else [])
    for quest_id in requested:
        dump(quest_id)


if __name__ == "__main__":
    main()
