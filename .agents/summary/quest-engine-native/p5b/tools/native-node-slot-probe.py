#!/usr/bin/env python3
"""真端交付节点槽探针：按 quest id 列出节点注册点、0x1e/0x35 槽注册点与 thunk 名。

真端形态（P4B/P1B/P5B 同一不变量）：
  * 节点注册 `FUN_180cb5920(&DAT_X, L"<NPC 名>", <questId>)` —— 第三实参是 **quest id**（不是句柄）；
  * 交付节点槽 `FUN_180cb2ac0(&DAT_Y, &DAT_X, <slot>, <thunk>, 0)` —— `0x1e` = 链式接取窗、`0x35` = PlayMovie。

用法（真端根默认为 /Users/mc/IdeaProjects/58Server）：
    python3 native-node-slot-probe.py 18833 18834 13400 23400
可选：`--root <真端根>` 覆盖根目录（脚本只写报告里的 `<真端根>` 规范名，不写机器路径）。
"""
from __future__ import annotations

import argparse
import collections
import re
import sys

C_RELATIVE = "server58/MainServer_ScriptDLL64/ScriptDLL64.c"
NODE_RE = re.compile(r'FUN_180cb5920\(&(DAT_[0-9a-f]+),L"([^"]+)",(0x[0-9a-f]+|\d+)\)')
SLOT_RE = re.compile(
	r'FUN_180cb2ac0\(&(DAT_[0-9a-f]+),&(DAT_[0-9a-f]+),(0x[0-9a-f]+|\d+),(FUN_[0-9a-f]+|&LAB_[0-9a-f]+)'
)
INTERESTING_SLOTS = ("0x1e", "0x35")


def parse_int(text: str) -> int:
	return int(text, 16) if text.startswith("0x") else int(text)


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description="真端交付节点 0x1e/0x35 槽探针")
	parser.add_argument("quest_ids", nargs="+", type=int, help="要复算的 quest id（十进制）")
	parser.add_argument("--root", default="/Users/mc/IdeaProjects/58Server", help="真端根目录")
	args = parser.parse_args(argv)

	source = f"{args.root}/{C_RELATIVE}"
	try:
		lines = open(source, encoding="utf-8", errors="replace").read().splitlines()
	except OSError as error:
		print(f"无法读取真端原码 {C_RELATIVE}: {error}", file=sys.stderr)
		return 2

	nodes: dict[int, list[tuple[str, str, int]]] = collections.defaultdict(list)
	for number, line in enumerate(lines, 1):
		match = NODE_RE.search(line)
		if match:
			nodes[parse_int(match.group(3))].append((match.group(1), match.group(2), number))

	slots: dict[str, list[tuple[int, str, str]]] = collections.defaultdict(list)
	for number, line in enumerate(lines, 1):
		match = SLOT_RE.search(line)
		if match:
			slots[match.group(2)].append((number, match.group(3), match.group(4)))

	for quest_id in args.quest_ids:
		registrations = nodes.get(quest_id)
		if not registrations:
			print(f"quest {quest_id}: 无节点注册")
			continue
		for node, npc, line in registrations:
			relevant = [entry for entry in slots.get(node, []) if entry[1] in INTERESTING_SLOTS]
			print(f"quest {quest_id} npc={npc} node={node} 注册=:{line} 槽(0x1e/0x35)={relevant}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
