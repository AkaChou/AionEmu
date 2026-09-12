#!/usr/bin/env python3
"""从零售服务端 Simple* 模板提取逐 var 任务证据（NPC/物品/检查/动画页）。

Extracts per-var quest evidence from the retail server Simple* templates:
acquire/reward NPC names, per-var talk NPCs, give/remove items, item_check
flags, cutscene haction pages. Output joins with the unresolved ledger.
"""
from __future__ import annotations

import csv
import re
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

UNPACK = Path("/Users/mc/IdeaProjects/58Server/Map/XML")
OUT = Path(__file__).resolve().parent / "retail-simple-templates.csv"
FIELD_RE = re.compile(r"^(talk_npc|give_item|remove_item|monster|count)(\d*)$")
SUFFIX_FIELDS = re.compile(r"^(.*?)(\d+)$")


def extract_file(path: Path, template: str, rows: list[dict]) -> None:
    """Simple* 文件是扁平元素流：以 id 元素为界切分每个任务。"""
    current: dict[str, list[str]] = defaultdict(list)
    quest_id = None

    def flush():
        nonlocal quest_id, current
        if quest_id is None:
            return
        row = {
            "quest_id": quest_id,
            "template": template,
            "acquire_npc_name": "; ".join(current.get("acquired_npc_name", [])),
            "reward_npc_name": "; ".join(current.get("reward_npc_name", [])),
            "item_check": "; ".join(current.get("item_check", [])),
            "talk_npcs": "|".join(
                f"{k}:{'; '.join(v)}" for k, v in sorted(current.items())
                if k.startswith("talk_npc")),
            "give_items": "|".join(
                f"{k}:{'; '.join(v)}" for k, v in sorted(current.items())
                if k.startswith("give_item")),
            "remove_items": "|".join(
                f"{k}:{'; '.join(v)}" for k, v in sorted(current.items())
                if k.startswith("remove_item")),
            "cutscenes": "|".join(
                f"{k}:{'; '.join(v)}" for k, v in sorted(current.items())
                if k.startswith(("cutsceneid", "cs", "haction"))),
            "objects": "|".join(
                f"{k}:{'; '.join(v)}" for k, v in sorted(current.items())
                if k.startswith(("monster", "count", "object", "use_item"))),
            "con_quests": "; ".join(current.get("con_quest", [])),
        }
        rows.append(row)
        current = defaultdict(list)
        quest_id = None

    for event, elem in ET.iterparse(path):
        if elem.tag == "id":
            flush()
            quest_id = (elem.attrib.get("id") or (elem.text or "").strip())
            if not quest_id or not quest_id.isdigit():
                quest_id = None
            continue
        if quest_id is not None and elem.text and elem.text.strip():
            current[elem.tag].append(elem.text.strip())
        elem.clear()
    flush()


def main() -> int:
    rows: list[dict] = []
    mapping = {
        "Quest_SimpleTalk.xml": "simple_talk",
        "Quest_SimpleHunt.xml": "simple_hunt",
        "Quest_SimpleCollectItem.xml": "simple_collect_item",
        "Quest_SimpleUseItem.xml": "simple_use_item",
        "Quest_SimpleItemPlay.xml": "simple_item_play",
        "Quest_SimpleSerialHunt.xml": "simple_serial_hunt",
        "Quest_SimpleGather.xml": "simple_gather",
    }
    for fn, template in mapping.items():
        path = UNPACK / fn
        if path.exists():
            before = len(rows)
            extract_file(path, template, rows)
            print(f"{fn}: {len(rows) - before} quests ({template})")

    with OUT.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)
    print(f"total: {len(rows)} rows -> {OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
