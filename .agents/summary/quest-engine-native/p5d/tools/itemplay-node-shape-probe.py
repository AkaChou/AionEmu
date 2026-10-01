#!/usr/bin/env python3
"""真端 ItemPlay 行节点/槽形态探针（逐行坐实，非类比）。

真端形态（`server58/MainServer_ScriptDLL64/ScriptDLL64.c`）：
  * 节点 `FUN_180cb5920(&DAT_node, L"<NPC 名>", <questId>)` —— 每个 NPC 一个节点；
  * 槽   `FUN_180cb3070(&DAT_slot, &DAT_node, <questId>, <slot>, <index>, 0)` —— slot 数字面量；
  * 行主注册 `FUN_180cb2eb0(&DAT_x, <questId>, 5, <hash>, <thunk>)` —— 第 3 实参 = 家族/种类 id。

用法：python3 itemplay-node-shape-probe.py <questId...> [--root <真端根>]
"""
from __future__ import annotations

import argparse
import collections
import re
import sys

C_RELATIVE = "server58/MainServer_ScriptDLL64/ScriptDLL64.c"
NODE_RE = re.compile(r'FUN_180cb5920\(&(DAT_[0-9a-f]+),L"([^"]+)",(0x[0-9a-f]+|\d+)\)')
SLOT_RE = re.compile(
    r'FUN_180cb3070\(&(DAT_[0-9a-f]+),&(DAT_[0-9a-f]+),(0x[0-9a-f]+|\d+),(0x[0-9a-f]+|\d+),(0x[0-9a-f]+|\d+),0\)')
KIND_RE = re.compile(
    r'FUN_180cb2eb0\(&(DAT_[0-9a-f]+),(0x[0-9a-f]+|\d+),(0x[0-9a-f]+|\d+),(0x[0-9a-f]+|\d+),(FUN_[0-9a-f]+|&LAB_[0-9a-f]+)\)')

SLOT_NAMES = {0: "0(slot0)", 3: "3(step)", 4: "4(slot4)", 0x1E: "0x1e(chain)", 0x35: "0x35(movie)"}


def num(text: str) -> int:
    return int(text, 16) if text.startswith("0x") else int(text)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("quest_ids", nargs="+", type=int)
    parser.add_argument("--root", default="/Users/mc/IdeaProjects/58Server")
    args = parser.parse_args(argv)
    lines = open(f"{args.root}/{C_RELATIVE}", encoding="utf-8", errors="replace").read().splitlines()

    node_name: dict[str, str] = {}
    node_of_quest: dict[int, list[tuple[str, str, int]]] = collections.defaultdict(list)
    for i, line in enumerate(lines, 1):
        m = NODE_RE.search(line)
        if m:
            node_name[m.group(1)] = m.group(2)
            node_of_quest[num(m.group(3))].append((m.group(1), m.group(2), i))

    slots: dict[int, list[tuple[str, int, int, int]]] = collections.defaultdict(list)
    for i, line in enumerate(lines, 1):
        m = SLOT_RE.search(line)
        if m:
            slots[num(m.group(3))].append((m.group(2), num(m.group(4)), num(m.group(5)), i))

    kinds: dict[int, list[tuple[int, str, int]]] = collections.defaultdict(list)
    for i, line in enumerate(lines, 1):
        m = KIND_RE.search(line)
        if m:
            kinds[num(m.group(2))].append((num(m.group(3)), m.group(5), i))

    for quest_id in args.quest_ids:
        print(f"== quest {quest_id} ==")
        for kind, thunk, line in kinds.get(quest_id, []):
            print(f"   主注册 kind={kind} thunk={thunk} :{line}")
        grouped: dict[str, list[tuple[int, int, int]]] = collections.defaultdict(list)
        for node, slot, index, line in slots.get(quest_id, []):
            grouped[node].append((slot, index, line))
        for node, name, reg_line in node_of_quest.get(quest_id, []):
            entries = grouped.get(node, [])
            shape = ", ".join(f"{SLOT_NAMES.get(s, s)}#{i}:{ln}" for s, i, ln in entries) or "-"
            print(f"   node {name:<32} 注册:{reg_line} 槽: {shape}")
    return 0


if __name__ == "__main__":
    sys.exit(main(__import__("sys").argv[1:]))
