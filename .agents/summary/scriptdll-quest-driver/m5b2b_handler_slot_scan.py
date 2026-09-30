#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把真端任务脚本的"事件码 → 处理器 → 玩家对象虚槽调用"逐任务摊平。

真端结构（已确证）：
- 每个任务在 ScriptDLL64 里有一个 BSS 静态对象：`FUN_180cb5920(&obj, L"<NPC 名>", <questId>)`
- 对象 `+0xa8` 起是 102 槽事件处理器表：
  `FUN_180cb2ac0(scratch, &obj, <事件码>, <handler>, meta)` / `FUN_180cb2ad0` / `FUN_180cb3070`
- 处理器第 1 参是**玩家对象 IUserImp**，处理器体里 `(**(code **)(*param_1 + 0xNNN))(...)`
  就是对 IUserImp 第 NNN/8 槽的调用（`+0xd0`=GetQuestState、`+0xe8`=GetQuestProgress、
  `+0xf0`=SetQuestProgress、`+0xf8`=SetQuestProgressMemoryOnly、`+0x100`=SetQuestSuccess 等）。

用法：
    python3 -B m5b2b_handler_slot_scan.py 1103 1104
    python3 -B m5b2b_handler_slot_scan.py --object 0x1848e4d90
    python3 -B m5b2b_handler_slot_scan.py --tsv 1103 > out.tsv
"""
from __future__ import annotations
import os

import argparse
import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

# 反编译导出 / Decompiled export
DLL_SOURCE = Path(f"{REPO.parent / '58Server'}/server58/MainServer_ScriptDLL64/ScriptDLL64.c")
# 宿主符号表（用于把虚槽地址写成名字）/ Host symbols
SERVER_SYMBOLS = Path(
	f"{REPO.parent / '58Server'}/server58-source/MainServer_Server64/symbols.tsv"
)
# IUserImp 虚表基址（Server64 / MainServer_Server64）/ IUserImp vtable base
IUSERIMP_VTABLE = 0x1411768d8
# 虚槽偏移 → 语义。来源：IUserImp 虚表逐槽解析 + 宿主符号名。只列已确认的。
IUSERIMP_SLOTS = {
	0x0D0: "GetQuestState",
	0x0D8: "SetQuestAcquired",
	0x0E0: "SetQuestWaiting",
	0x0E8: "GetQuestProgress",
	0x0F0: "SetQuestProgress",
	0x0F8: "SetQuestProgressMemoryOnly",
	0x100: "SetQuestSuccess",
	0x170: "OnAddMissionFromNpcServer",
	0x190: "SendQuestRelatedClientNoti？",
	0x1B8: "ClientShowSlot(演出/通知族)",
	0x1C0: "ClientShowSlot(演出/通知族, +1)",
	0x1C8: "RemoveItemByNameId",
	0x1D0: "RemoveItemById",
	0x1D8: "RemoveAllItemById",
	0x1E0: "RemoveQuestRewardCheckItems",
	0x1E8: "RemoveAllQuestRewardCheckItems",
	0x1F0: "RemoveAllCollectItems",
	0x1F8: "RemoveRecipe",
	0x200: "IsEquipped",
	0x208: "ChangeClass",
	0x220: "EnterInstance",
	0x228: "LockInstance",
	0x230: "LeaveInstance",
}
# 处理器体里的虚调用（变量名在体前部绑定）/ Vtable call inside a handler body
VTABLE_CALL_PATTERN = re.compile(r"\(\*\*\(code \*\*\)\((\w+) \+ (0x[0-9a-fA-F]+)\)\)")
# "lVar = *param_1;" 形式的虚表别名 / Alias of the player vtable
VTABLE_ALIAS_PATTERN = re.compile(r"(\w+) = \*param_1;")
# 任务对象构造 / Quest object construction
CTOR_PATTERN = re.compile(r'FUN_180cb5920\(&DAT_([0-9a-fA-F]+),L"([^"]*)",(0x[0-9a-fA-F]+|\d+)\)')
# 事件码注册：(正则, 第 3 个常量是事件码还是任务 ID)
# Event registration: (pattern, whether the third constant is an event index or a quest id)
REGISTER_PATTERNS = (
	(
		re.compile(
			r"FUN_180cb2ac0\(&DAT_[0-9a-fA-F]+,&DAT_([0-9a-fA-F]+),(0x[0-9a-fA-F]+|\d+),(&?[A-Za-z_0-9]+)"
		),
		True,
	),
	(
		re.compile(
			r"FUN_180cb2ad0\(&DAT_[0-9a-fA-F]+,&DAT_([0-9a-fA-F]+),(0x[0-9a-fA-F]+|\d+),\d+,(&?[A-Za-z_0-9]+)"
		),
		True,
	),
	(
		re.compile(
			r"FUN_180cb3070\(&DAT_[0-9a-fA-F]+,&DAT_([0-9a-fA-F]+),(0x[0-9a-fA-F]+|\d+),\d+,"
		),
		False,
	),
)
# 函数体起始标记（DLL 地址为 9 位十六进制）/ Function body marker
BODY_MARKER = re.compile(r"^/\* ([0-9a-f]{6,10}) \*/")


def parse_hex(token: str) -> int:
	"""解析 `0x...` 或十进制 token。 / Parse a `0x...` or decimal token."""
	return int(token, 16) if token.lower().startswith("0x") else int(token)


def load_lines() -> list[str]:
	"""读取反编译源码行。 / Load decompiled source lines."""
	if not DLL_SOURCE.is_file():
		raise FileNotFoundError(f"missing decompiled source: {DLL_SOURCE}")
	return DLL_SOURCE.read_text(encoding="utf-8", errors="replace").splitlines()


def user_slot_names() -> dict[int, str]:
	"""读 IUserImp 虚表，把偏移 → 宿主符号名（无符号名时回落手写表）。 / Map offset to host symbol."""
	names: dict[int, str] = {}
	try:
		raw = SERVER_SYMBOLS.read_text(encoding="utf-8", errors="replace")
	except OSError:
		raw = ""
	symbols: dict[int, str] = {}
	for line in raw.splitlines():
		parts = line.split("\t")
		if len(parts) < 3:
			continue
		try:
			symbols[int(parts[0], 16)] = parts[2]
		except ValueError:
			continue
	if symbols:
		from m5b2_pe_probe import Pe  # noqa: PLC0415

		pe = Pe(f"{REPO.parent / '58Server'}/MainServer/Server64.exe")
		for offset, label in IUSERIMP_SLOTS.items():
			target = pe.u64(IUSERIMP_VTABLE + offset)
			names[offset] = symbols.get(target) or label
		return names
	return {offset: label for offset, label in IUSERIMP_SLOTS.items()}


def collect_objects(lines: list[str]) -> dict[str, dict]:
	"""收集 任务对象 → {quest_id, npc, line}。 / Collect quest objects."""
	objects: dict[str, dict] = {}
	for index, line in enumerate(lines):
		match = CTOR_PATTERN.search(line)
		if not match:
			continue
		address, npc, quest_id = match.group(1), match.group(2), parse_hex(match.group(3))
		objects.setdefault(address, {"quest_id": quest_id, "npc": npc, "line": index + 1})
	return objects


def collect_registrations(lines: list[str]) -> dict[str, list[dict]]:
	"""收集 任务对象 → [事件码注册…]。 / Collect per-object event registrations."""
	by_object: dict[str, list[dict]] = {}
	for index, line in enumerate(lines):
		for pattern, is_event in REGISTER_PATTERNS:
			match = pattern.search(line)
			if not match:
				continue
			address = match.group(1)
			event = parse_hex(match.group(2))
			handler = match.group(3) if match.lastindex and match.lastindex >= 3 else ""
			by_object.setdefault(address, []).append(
				{"event": event, "handler": handler, "line": index + 1, "is_event": is_event}
			)
			break
	return by_object


def index_functions(lines: list[str]) -> dict[str, tuple[int, int]]:
	"""函数名 → (起始行, 结束行)。 / Function name to line span."""
	spans: dict[str, tuple[int, int]] = {}
	current: str | None = None
	start = 0
	for index, line in enumerate(lines):
		match = BODY_MARKER.match(line)
		if match:
			if current is not None:
				spans[current] = (start, index)
			current = match.group(1)
			start = index
	if current is not None:
		spans[current] = (start, len(lines))
	return spans


def handler_user_slots(lines: list[str], spans: dict[str, tuple[int, int]], handler: str) -> list[str]:
	"""返回处理器体里出现的玩家对象虚槽（去重保序）。 / Player vtable slots used by a handler."""
	# 注册表里写的是 FUN_xxx / &LAB_xxx，函数索引用的是裸地址 / strip the &FUN_/&LAB_ prefix
	bare = handler.removeprefix("&")
	span = spans.get(bare) or spans.get(bare.removeprefix("FUN_").removeprefix("LAB_"))
	if span is None:
		return []
	body = lines[span[0]: span[1]]
	# 玩家对象既可以写成 *param_1，也可以先绑定到局部变量 / either *param_1 or a bound local
	receivers = {"param_1"}
	for line in body:
		match = VTABLE_ALIAS_PATTERN.search(line)
		if match:
			receivers.add(match.group(1))
	seen: list[str] = []
	for line in body:
		for receiver, offset_token in VTABLE_CALL_PATTERN.findall(line):
			if receiver not in receivers:
				continue
			offset = int(offset_token, 16)
			# 只保留落在 IUserImp 虚槽命名表里的偏移 / keep offsets known in the IUserImp slot table
			token = f"{offset:#05x}"
			if offset in IUSERIMP_SLOTS and token not in seen:
				seen.append(token)
	return sorted(seen)


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("quest_ids", nargs="*", type=int)
	parser.add_argument("--object", action="append", default=[])
	parser.add_argument("--tsv", action="store_true")
	parser.add_argument(
		"--all",
		action="store_true",
		help="Dump every quest object instead of selecting ids. / 导出全部任务对象，不做 ID 选择。",
	)
	args = parser.parse_args(argv)

	lines = load_lines()
	objects = collect_objects(lines)
	registrations = collect_registrations(lines)
	spans = index_functions(lines)
	slot_names = user_slot_names()

	wanted: set[str] = {addr.lower().removeprefix("0x") for addr in args.object}
	for address, meta in objects.items():
		if args.all or meta["quest_id"] in args.quest_ids:
			wanted.add(address.lower())
	if not wanted:
		parser.error("need at least one quest id or --object / 至少需要一个任务 ID 或 --object")

	header = ["quest_id", "npc", "object", "event_idx", "handler", "user_slots", "user_slot_names"]
	if args.tsv:
		print("\t".join(header))
	for address in sorted(wanted):
		meta = objects.get(address) or objects.get(address.upper())
		if meta is None:
			print(f"# object {address} 未找到构造点 / no constructor found", file=sys.stderr)
			continue
		for entry in sorted(registrations.get(address, []), key=lambda item: item["event"]):
			slots = handler_user_slots(lines, spans, entry["handler"])
			labels = [slot_names.get(int(offset, 16), "?") for offset in slots]
			slots_text = ",".join(slots) or "-"
			labels_text = ",".join(labels) or "-"
			# 第三种注册函数的第 3 参是任务 ID，不是事件码（标题里用 qid: 前缀区分）
			# The third register helper carries a quest id, not an event index (marked qid:)
			event_text = f"{entry['event']:#x}" if entry["is_event"] else f"qid:{entry['event']:#x}"
			if args.tsv:
				print(
					"\t".join(
						[
							str(meta["quest_id"]),
							meta["npc"],
							address,
							event_text,
							entry["handler"],
							slots_text,
							labels_text,
						]
					)
				)
			else:
				print(
					f"quest {meta['quest_id']} ({meta['npc']}) obj={address} "
					f"event={event_text} handler={entry['handler']} "
					f"slots={slots_text} [{labels_text}]"
				)
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
