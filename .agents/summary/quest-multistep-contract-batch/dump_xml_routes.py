#!/usr/bin/env python3
"""打印任务 XML 的对话路由（source/target/npc/action/page），用于判断现有结构是否可批量改写。"""
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"


def main() -> int:
    for raw in sys.argv[1:]:
        quest_id = int(raw)
        root = ET.parse(QUESTS / f"{quest_id}.xml").getroot()
        print(f"########## {quest_id}")
        for transition in root.find("transitions") or []:
            tag = transition.tag
            if tag in ("dialog", "npc-complete"):
                print(f"  [{tag}] source={transition.get('source')} target={transition.get('target')} "
                      f"npc={transition.get('npc-id')} action={transition.get('action')} "
                      f"actions={transition.get('actions')} start-page={transition.get('start-page')}")
                continue
            event = transition.find("event")
            is_dialog = False
            if event is not None:
                for element in event:
                    if element.tag == "dialog":
                        is_dialog = True
                        after = [a.get("page") for a in transition.findall("after-commit/dialog")
                                 if a.get("type") == "SHOW_QUEST_PAGE"]
                        print(f"  {transition.get('source'):12s} -> {transition.get('target'):12s} "
                              f"npc={element.get('npc-id')} action={element.get('action')} "
                              f"actions={element.get('actions')} after={after}")
            if not is_dialog:
                passes = True
        print()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
