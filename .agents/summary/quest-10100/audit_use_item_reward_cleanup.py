#!/usr/bin/env python3
"""列出 use-item 进入 REWARD 且由本任务发放道具的清理合同。

List cleanup contracts for quest-granted items consumed by a use-item route
that enters REWARD. The output is evidence for manual classification; it does
not decide whether an item is intentionally retained.
"""

from __future__ import annotations

import argparse
import csv
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[3],
        help="repository root",
    )
    return parser.parse_args()


def int_attr(element: ET.Element, attribute: str) -> int:
    return int(element.get(attribute, "0"))


def audit(root: Path) -> list[dict[str, object]]:
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    rows: list[dict[str, object]] = []
    for path in sorted(quest_dir.glob("*.xml")):
        definition = ET.parse(path).getroot()
        quest_id = int_attr(definition, "id")
        statuses = {
            node.get("label", ""): node.get("status", "")
            for node in definition.iter("node")
        }
        given_items = {
            int_attr(item, "item-id")
            for item in definition.iter("give-item")
            if item.get("item-id")
        }
        work_items = {
            int_attr(item, "id")
            for item in definition.findall("metadata/work-items/item")
            if item.get("id")
        }
        completion_routes = [
            transition
            for transition in definition.iter("transition")
            if statuses.get(transition.get("source", "")) == "REWARD"
            and statuses.get(transition.get("target", "")) == "COMPLETE"
        ]

        for transition in definition.iter("transition"):
            event = transition.find("event/use-item")
            if event is None or not event.get("item-id"):
                continue
            item_id = int_attr(event, "item-id")
            if item_id not in given_items:
                continue
            if statuses.get(transition.get("target", "")) != "REWARD":
                continue
            if statuses.get(transition.get("source", "")) == "REWARD":
                continue

            route_removal = any(
                int_attr(action, "item-id") == item_id
                for action in transition.findall("actions/remove-item")
                if action.get("item-id")
            )
            completion_removal = bool(completion_routes) and all(
                any(
                    int_attr(action, "item-id") == item_id
                    for action in route.findall("actions/remove-item")
                    if action.get("item-id")
                )
                for route in completion_routes
            )
            rows.append(
                {
                    "quest_id": quest_id,
                    "item_id": item_id,
                    "source": transition.get("source", ""),
                    "route_remove": route_removal,
                    "work_item_cleanup": item_id in work_items,
                    "completion_remove_all_routes": completion_removal,
                    "file": str(path.relative_to(root)),
                }
            )
    return rows


def main() -> None:
    args = parse_args()
    rows = audit(args.root.resolve())
    writer = csv.DictWriter(
        sys.stdout,
        fieldnames=[
            "quest_id",
            "item_id",
            "source",
            "route_remove",
            "work_item_cleanup",
            "completion_remove_all_routes",
            "file",
        ],
        delimiter="\t",
        lineterminator="\n",
    )
    writer.writeheader()
    writer.writerows(rows)


if __name__ == "__main__":
    main()
