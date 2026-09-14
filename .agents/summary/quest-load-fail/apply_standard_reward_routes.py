#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import re
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
PATH_PATTERN = re.compile(r"-> ([^ ]+) \+ page (\d+)$")


@dataclass(frozen=True)
class RewardRoute:
    quest_id: int
    path: Path
    source_node: str
    target_node: str
    npc_id: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Add standard START -> REWARD SELECT_QUEST_REWARD routes for client-proven NPCs.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agents/summary/quest-load-fail/quest-order-audit-current.csv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agents/summary/quest-load-fail/standard-reward-routes.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def collect(args: argparse.Namespace) -> list[RewardRoute]:
    root = args.root.resolve()
    audit = args.audit if args.audit.is_absolute() else root / args.audit
    with audit.open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    result: list[RewardRoute] = []
    seen: set[tuple[int, str, str, str]] = set()
    for row in rows:
        if (not row["unresolved_reason"].startswith("visible client action has no route")
                or row["client_visible_action"] != "1009" or not row["npc_id"]):
            continue
        match = PATH_PATTERN.search(row["actual_path"])
        if match is None:
            continue
        quest_id = int(row["quest_id"])
        source_node = match.group(1)
        path = quest_dir / f"{quest_id}.xml"
        document = ET.parse(path).getroot()
        statuses = {node.get("label", ""): node.get("status", "") for node in document.findall("./nodes/node")}
        if statuses.get(source_node) != "START":
            continue
        rewards = [label for label, status in statuses.items() if status == "REWARD"]
        if len(rewards) != 1:
            continue
        if any(
            transition.get("source") == source_node and transition.get("target") == rewards[0]
            and (event := transition.find("./event/dialog")) is not None
            and event.get("type") == "TALK_TO_NPC" and event.get("npc-id") == row["npc_id"]
            and event.get("action") == "SELECT_QUEST_REWARD"
            for transition in document.findall("./transitions/transition")):
            continue
        key = (quest_id, source_node, rewards[0], row["npc_id"])
        if key in seen:
            continue
        result.append(RewardRoute(quest_id, path, source_node, rewards[0], row["npc_id"]))
        seen.add(key)
    return sorted(result, key=lambda item: (item.quest_id, item.source_node, item.npc_id))


def write_manifest(path: Path, rows: list[RewardRoute]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "source_node", "target_node", "npc_id"))
        for row in rows:
            writer.writerow((row.quest_id, row.path.relative_to(ROOT).as_posix(), row.source_node,
                             row.target_node, row.npc_id))


def render(row: RewardRoute) -> str:
    return (
        f'    <transition source="{row.source_node}" target="{row.target_node}">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{row.npc_id}" action="SELECT_QUEST_REWARD"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        '        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>\n'
        '      </after-commit>\n'
        '    </transition>\n'
    )


def apply(rows: list[RewardRoute]) -> int:
    grouped: dict[Path, list[RewardRoute]] = defaultdict(list)
    for row in rows:
        grouped[row.path].append(row)
    for path, items in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        closing = source.rfind("</transitions>")
        if closing < 0:
            raise RuntimeError(f"missing transitions closing tag: {path}")
        updated = source[:closing] + "".join(render(row) for row in items) + source[closing:]
        if hashlib.sha256(path.read_bytes()).digest() != hashlib.sha256(before).digest():
            raise RuntimeError(f"concurrent change detected before write: {path}")
        ET.fromstring(updated)
        path.write_text(updated, encoding="utf-8")
    return len(rows)


def main() -> int:
    args = parse_args()
    rows = collect(args)
    manifest = args.manifest if args.manifest.is_absolute() else args.root.resolve() / args.manifest
    write_manifest(manifest, rows)
    print(f"reward_routes={len(rows)} files={len({row.path for row in rows})} manifest={manifest}")
    if args.write:
        print(f"changed_routes={apply(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
