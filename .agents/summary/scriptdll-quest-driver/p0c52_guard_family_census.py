#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-52：Village_Guard 族名称三源对拍（DD 表 / 服务端 npc 模板 / 客户端 npc 块）。

P0c-52: three-source cross-tab for the Village_Guard family (DD table / server npc
templates / client npc blocks). Read-only census: no file is written outside stdout.
"""
from __future__ import annotations
import os

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
DD_TABLE = REPO / "src/main/resources/aion/data/static_data/quest_retail/data_driven_quest.xml"
NPC_TEMPLATE_DIR = REPO / "src/main/resources/aion/data/static_data/npcs"
CLIENT_NPC = Path(f"{os.environ.get('AION_UNPACK_ROOT', os.path.expanduser('~/PycharmProjects/unpak'))}/npcs_unpacked/client_npcs_npc.xml")
CLIENT_STRINGS = Path(f"{os.environ.get('AION_UNPACK_ROOT', os.path.expanduser('~/PycharmProjects/unpak'))}/strings_unpacked/client_strings_dic_etc.xml")

TOKEN = "Village_Guard"


def dd_rows() -> list[tuple[str, str, str, str]]:
	"""(quest_id, category_acquire_, value0_acquire_, reward_npc_name) carrying the token."""
	text = DD_TABLE.read_text(encoding="utf-8", errors="replace")
	rows: list[tuple[str, str, str, str]] = []
	for block in re.findall(r"<quest_data_driven>.*?</quest_data_driven>", text, re.S):
		if TOKEN not in block:
			continue
		qid = re.search(r"<id>(\d+)</id>", block)
		cat = re.search(r"<category_acquire_>([^<]*)</category_acquire_>", block)
		acq = re.search(r"<value0_acquire_>([^<]*)</value0_acquire_>", block)
		reward = re.search(r"<reward_npc_name>([^<]*)</reward_npc_name>", block)
		rows.append((qid.group(1) if qid else "?", cat.group(1) if cat else "-",
			acq.group(1) if acq else "-", reward.group(1) if reward else "-"))
	return rows


def server_templates() -> list[tuple[str, str]]:
	"""(npc_id, template_name) from every npc template file carrying the token."""
	out: list[tuple[str, str]] = []
	for path in sorted(NPC_TEMPLATE_DIR.glob("*.xml")):
		text = path.read_text(encoding="utf-8", errors="replace")
		if TOKEN not in text:
			continue
		for block in re.findall(r"<npc_template\b[^>]*>.*?</npc_template>", text, re.S):
			if TOKEN not in block:
				continue
			nid = re.search(r'\bid="(\d+)"', block)
			name = re.search(r"<name>([^<]+)</name>", block)
			if nid and name:
				out.append((nid.group(1), name.group(1)))
	return out


def client_blocks() -> list[tuple[str, str, str]]:
	"""(client_id, name, quest_ai_name) from the client npc file (single streaming pass)."""
	out: list[tuple[str, str, str]] = []
	cur_id = cur_name = cur_quest_ai = None
	with CLIENT_NPC.open("r", encoding="utf-8", errors="replace") as handle:
		for line in handle:
			line = line.strip()
			if line == "<npc_client>":
				cur_id = cur_name = cur_quest_ai = None
			elif line.startswith("<id>") and cur_id is None:
				cur_id = line[4:-5]
			elif line.startswith("<name>") and cur_name is None:
				cur_name = line[6:-7]
			elif line.startswith("<quest_ai_name>"):
				cur_quest_ai = line[16:-17]
			elif line == "</npc_client>":
				if cur_name and TOKEN in cur_name:
					out.append((cur_id or "?", cur_name, cur_quest_ai or "-"))
				elif cur_quest_ai and TOKEN in cur_quest_ai:
					out.append((cur_id or "?", cur_name or "-", cur_quest_ai))
	return out


def client_string_keys() -> list[str]:
	keys: list[str] = []
	with CLIENT_STRINGS.open("r", encoding="utf-8", errors="replace") as handle:
		for line in handle:
			if TOKEN in line:
				keys.append(line.strip()[:160])
	return keys


def main() -> int:
	print("== DD table rows carrying the token ==")
	rows = dd_rows()
	for qid, cat, acq, reward in rows:
		print(f"  {qid} cat={cat} acquire={acq} reward={reward}")
	print(f"  total {len(rows)}")

	print("== server npc templates carrying the token ==")
	tpl = server_templates()
	for nid, name in tpl:
		print(f"  {nid} {name}")
	print(f"  total {len(tpl)}")

	print("== client npc blocks carrying the token ==")
	blocks = client_blocks()
	for cid, name, qai in blocks:
		print(f"  {cid} name={name} quest_ai_name={qai}")
	print(f"  total {len(blocks)}")

	print("== client string keys carrying the token ==")
	keys = client_string_keys()
	for key in keys[:40]:
		print(f"  {key}")
	print(f"  total {len(keys)}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
