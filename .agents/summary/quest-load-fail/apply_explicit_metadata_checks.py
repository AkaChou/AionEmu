#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import re
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
PLAN = (
    (11051, "started", 798989, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT6"),
    (11052, "started", 798984, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT6"),
    (11053, "started", 799017, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT6"),
    (11054, "started", 799017, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT6"),
    (11055, "started", 798990, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT6"),
    (21051, "started", 799291, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT6"),
    (21052, "started", 799268, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT6"),
    (21053, "started", 799320, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT6"),
    (21054, "started", 799318, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT6"),
    (21055, "started", 799295, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "SELECT6"),
    (16942, "started", 802350, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "CHECK_USER_ITEM_FAIL"),
    (26942, "started", 802353, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "CHECK_USER_ITEM_FAIL"),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Add handler-proven metadata item checks.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def existing_check(root: ET.Element, source: str, npc_id: int) -> bool:
    return any(
        transition.get("source") == source
        and (dialog := transition.find("./event/dialog")) is not None
        and dialog.get("npc-id") == str(npc_id)
        and dialog.get("action") == "CHECK_USER_HAS_QUEST_ITEM"
        for transition in root.findall("./transitions/transition")
    )


def render(source: str, target: str, npc_id: int,
           items: tuple[tuple[int, int], ...], success_page: str, failure_page: str) -> str:
    conditions = "".join(
        f'        <has-item item-id="{item_id}" count="{count}"/>\n'
        for item_id, count in items)
    removals = "".join(
        f'        <remove-item item-id="{item_id}" count="{count}"/>\n'
        for item_id, count in items)
    return (
        f'    <transition source="{source}" target="{target}" priority="0">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" '
        'action="CHECK_USER_HAS_QUEST_ITEM"/>\n'
        '      </event>\n'
        '      <conditions>\n'
        f'{conditions}'
        '      </conditions>\n'
        '      <actions>\n'
        f'{removals}'
        '      </actions>\n'
        '      <after-commit>\n'
        '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        f'        <dialog type="SHOW_QUEST_PAGE" page="{success_page}"/>\n'
        '      </after-commit>\n'
        '    </transition>\n'
        f'    <transition source="{source}" target="{source}" priority="1">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" '
        'action="CHECK_USER_HAS_QUEST_ITEM"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        f'        <dialog type="SHOW_QUEST_PAGE" page="{failure_page}"/>\n'
        '      </after-commit>\n'
        '    </transition>\n'
    )


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    changed = 0
    for quest_id, source, npc_id, success_page, failure_page in PLAN:
        path = quest_dir / f"{quest_id}.xml"
        before = digest(path)
        text = path.read_text(encoding="utf-8")
        document = ET.fromstring(text)
        if existing_check(document, source, npc_id):
            continue
        items = tuple(
            (int(item.get("id")), int(item.get("count")))
            for item in document.findall("./metadata/items/item")
        )
        rewards = [
            node.get("label") for node in document.findall("./nodes/node")
            if node.get("status") == "REWARD"
        ]
        if not items or len(rewards) != 1:
            raise RuntimeError(f"quest {quest_id} lacks metadata items or one reward node")
        closing = text.rfind("</transitions>")
        if closing < 0:
            raise RuntimeError(f"quest {quest_id} lacks transitions closing tag")
        updated = text[:closing] + render(
            source, rewards[0], npc_id, items, success_page, failure_page) + text[closing:]
        ET.fromstring(updated)
        if digest(path) != before:
            raise RuntimeError(f"concurrent change detected before write: {path}")
        if args.write:
            path.write_text(updated, encoding="utf-8")
        changed += 1
    print(f"changed={changed} write={args.write}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
