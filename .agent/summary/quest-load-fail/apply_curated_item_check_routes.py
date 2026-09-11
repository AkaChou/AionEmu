#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
PLAN = (
    (3936, "started", 203710, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "CHECK_USER_ITEM_FAIL"),
    (3937, "started", 203708, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "CHECK_USER_ITEM_FAIL"),
    (4940, "started", 204050, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "CHECK_USER_ITEM_FAIL"),
    (4941, "started", 204060, "SHOW_SELECT_QUEST_REWARD_WINDOW1", "CHECK_USER_ITEM_FAIL"),
    (21027, "started", 799254, "CHECK_USER_ITEM_OK", "CHECK_USER_ITEM_FAIL"),
)


@dataclass(frozen=True)
class Insertion:
    quest_id: int
    path: Path
    source: str
    target: str
    npc_id: int
    items: tuple[tuple[int, int], ...]
    success_page: str
    failure_page: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Apply curated client-and-handler-proven CHECK_USER_HAS_QUEST_ITEM routes.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agent/summary/quest-load-fail/curated-item-check-routes.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def collect(root: Path) -> list[Insertion]:
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    result: list[Insertion] = []
    for quest_id, source, npc_id, success_page, failure_page in PLAN:
        path = quest_dir / f"{quest_id}.xml"
        document = ET.parse(path).getroot()
        statuses = {node.get("label", ""): node.get("status", "")
                    for node in document.findall("./nodes/node")}
        rewards = [label for label, status in statuses.items() if status == "REWARD"]
        if statuses.get(source) != "START" or len(rewards) != 1:
            raise RuntimeError(f"quest {quest_id} no longer has one START -> REWARD contract")
        items = tuple(
            (int(item.get("id")), int(item.get("count")))
            for item in document.findall("./metadata/items/item")
        )
        if not items:
            raise RuntimeError(f"quest {quest_id} has no metadata item requirements")
        result.append(Insertion(quest_id, path, source, rewards[0], npc_id, items,
                                success_page, failure_page))
    return result


def write_manifest(path: Path, rows: list[Insertion]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "source", "target", "npc_id", "items",
                         "success_page", "failure_page"))
        for row in rows:
            writer.writerow((row.quest_id, row.path.relative_to(ROOT).as_posix(), row.source,
                             row.target, row.npc_id,
                             " ".join(f"{item}:{count}" for item, count in row.items),
                             row.success_page, row.failure_page))


def render(row: Insertion) -> str:
    conditions = "".join(
        f'        <has-item item-id="{item_id}" count="{count}"/>\n'
        for item_id, count in row.items)
    removals = "".join(
        f'        <remove-item item-id="{item_id}" count="{count}"/>\n'
        for item_id, count in row.items)
    return (
        f'    <transition source="{row.source}" target="{row.target}" priority="0">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{row.npc_id}" '
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
        f'        <dialog type="SHOW_QUEST_PAGE" page="{row.success_page}"/>\n'
        '      </after-commit>\n'
        '    </transition>\n'
        f'    <transition source="{row.source}" target="{row.source}" priority="1">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{row.npc_id}" '
        'action="CHECK_USER_HAS_QUEST_ITEM"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        f'        <dialog type="SHOW_QUEST_PAGE" page="{row.failure_page}"/>\n'
        '      </after-commit>\n'
        '    </transition>\n'
    )


def apply(rows: list[Insertion], write: bool) -> int:
    grouped: dict[Path, list[Insertion]] = defaultdict(list)
    for row in rows:
        grouped[row.path].append(row)
    for path, items in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        closing = source.rfind("</transitions>")
        if closing < 0:
            raise RuntimeError(f"missing transitions closing tag: {path}")
        updated = source[:closing] + "".join(render(item) for item in items) + source[closing:]
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        ET.fromstring(updated)
        if write:
            path.write_text(updated, encoding="utf-8")
    return len(rows)


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    rows = collect(root)
    manifest = args.manifest if args.manifest.is_absolute() else root / args.manifest
    write_manifest(manifest, rows)
    changed = apply(rows, args.write)
    print(f"routes={len(rows)} changed={changed} files={len({row.path for row in rows})} "
          f"write={args.write} manifest={manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
