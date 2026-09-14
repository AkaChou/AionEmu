#!/usr/bin/env python3
"""用外部客户端解包（Quest.pak / NPCs）解析 2155 个 data-driven 任务的接取/领奖 NPC ID。

Resolves the acquire/reward NPC ids for data-driven quests from the provided client
unpacks (Quest_unpacked/data_driven_quest.xml + npcs_unpacked name->id tables).
"""
from __future__ import annotations

import csv
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

UNPACK = Path("/Users/mc/PycharmProjects/unpak")
OUT = Path(__file__).resolve().parent / "data-driven-npc-evidence.csv"


def npc_name_map() -> dict[str, set[int]]:
    names: dict[str, set[int]] = defaultdict(set)
    for fn in ("client_npcs_npc.xml", "client_npcs_monster.xml",
               "client_npcs_std_npc.xml", "client_npcs_std_monster.xml",
               "client_npcs_abyss_monster.xml", "client_npcs_std_abyss_monster.xml"):
        path = UNPACK / "npcs_unpacked" / fn
        if not path.exists():
            continue
        current_id = None
        for event, elem in ET.iterparse(path):
            if elem.tag == "id" and elem.text and elem.text.strip().isdigit():
                current_id = int(elem.text)
            elif elem.tag == "name" and elem.text and current_id is not None:
                name = elem.text.strip()
                if name and not name.startswith("STR_"):
                    names[name].add(current_id)
                current_id = None
    return names


def main() -> int:
    names = npc_name_map()
    print(f"npc names resolved: {len(names)}")

    rows = []
    ambiguous = 0
    no_npc = 0
    for event, elem in ET.iterparse(UNPACK / "Quest_unpacked" / "data_driven_quest.xml"):
        if elem.tag != "quest_data_driven":
            continue
        qid = (elem.findtext("id") or "").strip()
        acquire_npc_name = (elem.findtext("value0_acquire_") or "").strip()
        reward_npc_name = (elem.findtext("reward_npc_name") or "").strip()
        acquire_cat = (elem.findtext("category_acquire_") or "").strip()
        elem.clear()
        if not qid.isdigit():
            continue

        def resolve(name: str) -> str:
            nonlocal ambiguous
            if not name:
                return ""
            ids = sorted(names.get(name, set()))
            if len(ids) == 1:
                return str(ids[0])
            if not ids:
                return ""
            ambiguous += 1
            return "AMBIG:" + ",".join(map(str, ids[:6]))

        rows.append({
            "quest_id": qid,
            "acquire_category": acquire_cat,
            "acquire_npc_name": acquire_npc_name,
            "acquire_npc_id": resolve(acquire_npc_name),
            "reward_npc_name": reward_npc_name,
            "reward_npc_id": resolve(reward_npc_name),
        })
    with OUT.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)
    resolved = sum(1 for r in rows if r["acquire_npc_id"] and not r["acquire_npc_id"].startswith("AMBIG"))
    print(f"data-driven quests: {len(rows)} acquire-npc resolved: {resolved} "
          f"ambiguous: {ambiguous} no-npc: {no_npc}")
    print(f"output: {OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
