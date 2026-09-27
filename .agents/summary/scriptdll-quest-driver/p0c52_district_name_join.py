#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-52：区名（quest_ai_name）→ npc_id 映射证据（客户端 npc 块 + 服务端模板 name_desc）。

P0c-52: district-name (quest_ai_name) -> npc_id mapping evidence, joining the client npc
blocks (id / name / quest_ai_name) with the server-side generated npc templates
(npc_id / name_desc / name). Read-only census; output goes to stdout.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
NPC_DIR = REPO / "src/main/resources/aion/data/static_data/npcs"
CLIENT_NPC = Path("/Users/mc/PycharmProjects/unpak/npcs_unpacked/client_npcs_npc.xml")
DISTRICT = re.compile(r"^LDF4_Advance_Village_Guard_[LD]_(North|South|East|West)$")


def client_district_blocks() -> list[tuple[str, str, str, str]]:
	"""(client_id, client_name, quest_ai_name, race-ish hint) for district quest_ai_names."""
	out: list[tuple[str, str, str, str]] = []
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
				if cur_quest_ai and DISTRICT.match(cur_quest_ai):
					out.append((cur_id or "?", cur_name or "-", cur_quest_ai, "-"))
	return out


def server_templates() -> dict[str, tuple[str, str, str]]:
	"""{npc_id: (name_desc, display_name, race)} over every npc template file."""
	out: dict[str, tuple[str, str, str]] = {}
	for path in sorted(NPC_DIR.glob("*.xml")):
		text = path.read_text(encoding="utf-8", errors="replace")
		for match in re.finditer(r"<npc_template\b([^>]*)>", text):
			attrs = match.group(1)
			nid = re.search(r'npc_id="(\d+)"', attrs)
			if not nid:
				continue
			desc = re.search(r'name_desc="([^"]*)"', attrs)
			name = re.search(r'\bname="([^"]*)"', attrs)
			race = re.search(r'\brace="([^"]*)"', attrs)
			out[nid.group(1)] = (desc.group(1) if desc else "-",
				name.group(1) if name else "-", race.group(1) if race else "-")
	return out


def main() -> int:
	templates = server_templates()
	print(f"server npc templates indexed: {len(templates)}")
	blocks = client_district_blocks()
	print(f"client district blocks: {len(blocks)}")
	for cid, cname, qai, _ in blocks:
		hit = templates.get(cid)
		print(f"  [{qai}] client_id={cid} client_name={cname} "
			f"server={'MISSING' if hit is None else hit[0] + ' / ' + hit[1] + ' / ' + hit[2]}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
