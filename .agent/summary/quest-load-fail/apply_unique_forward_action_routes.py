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
TRANSITION_PATTERN = re.compile(r"<transition\b[^>]*>.*?</transition>", re.DOTALL)
ACTION_NAMES = {"10000": "SETPRO1", "10001": "SETPRO2", "1009": "SELECT_QUEST_REWARD"}


@dataclass(frozen=True)
class Insertion:
    quest_id: int
    path: Path
    source: str
    target: str
    npc_id: int
    action_id: int
    action_name: str
    kind: str
    block: str
    priority: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Clone the unique forward-looking action for an unresolved SETPRO client button.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agent/summary/quest-load-fail/quest-order-audit-current.csv"))
    parser.add_argument("--transitions", type=Path,
                        default=Path(".agent/summary/quest-load-fail/compiled-transitions.tsv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agent/summary/quest-load-fail/unique-forward-action-routes.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def collect(args: argparse.Namespace) -> list[Insertion]:
    root = args.root.resolve()
    audit_path = args.audit if args.audit.is_absolute() else root / args.audit
    transition_path = args.transitions if args.transitions.is_absolute() else root / args.transitions
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    with audit_path.open(encoding="utf-8-sig", newline="") as stream:
        audit = list(csv.DictReader(stream))
    with transition_path.open(encoding="utf-8", newline="") as stream:
        compiled = list(csv.DictReader(stream, delimiter="\t"))

    by_node: dict[tuple[str, str, str], list[dict[str, str]]] = defaultdict(list)
    for transition in compiled:
        if transition["event_type"] == "TALK_TO_NPC":
            by_node[(transition["quest_id"], transition["source_node"],
                     transition["event_owner"])].append(transition)

    result: list[Insertion] = []
    seen: set[tuple[int, str, int, int]] = set()
    for row in audit:
        if (not row["unresolved_reason"].startswith("visible client action has no route")
                or row["client_visible_action"] not in ACTION_NAMES
                or not row["npc_id"]):
            continue
        action_id = row["client_visible_action"]
        key = (int(row["quest_id"]), row["server_source_state"], int(row["npc_id"]), int(action_id))
        if key in seen:
            continue
        options = [
            transition for transition in by_node.get(
                (row["quest_id"], row["server_source_state"], row["npc_id"]), [])
            if transition["event_action"] not in {action_id, "31", "-1", "1008"}
            and transition["target_node"] != row["server_source_state"]
        ]
        contracts = {
            (transition["event_action"], transition["target_node"], transition["priority"],
             transition["after_commit"])
            for transition in options
        }
        if len(contracts) != 1:
            continue
        forward_action, target, priority, _ = next(iter(contracts))
        if forward_action not in ACTION_NAMES:
            continue
        path = quest_dir / f"{row['quest_id']}.xml"
        source_text = path.read_text(encoding="utf-8")
        forward_name = ACTION_NAMES[forward_action]
        raw_matches: list[str] = []
        for raw in TRANSITION_PATTERN.findall(source_text):
            block = ET.fromstring(raw)
            if (block.get("source") != row["server_source_state"]
                    or block.get("target") != target):
                continue
            event = block.find("./event/dialog")
            if event is None or event.get("npc-id") != row["npc_id"]:
                continue
            if event.get("action") == forward_name:
                raw_matches.append(raw)
        if len(raw_matches) == 1:
            block = re.sub(
                r'(\baction=")[^"]*(")',
                rf'\g<1>{ACTION_NAMES[action_id]}\g<2>',
                raw_matches[0], count=1)
            kind = "EXPLICIT"
        elif forward_action == "1009":
            document = ET.parse(path).getroot()
            target_status = next(
                node.get("status") for node in document.findall("./nodes/node")
                if node.get("label") == target)
            if target_status != "REWARD":
                continue
            priority_attr = f' priority="{priority}"' if priority else ""
            block = (
                f'    <transition source="{row["server_source_state"]}" target="{target}"'
                f'{priority_attr}>\n'
                '      <event>\n'
                f'        <dialog type="TALK_TO_NPC" npc-id="{row["npc_id"]}" '
                f'action="{ACTION_NAMES[action_id]}"/>\n'
                '      </event>\n'
                '      <after-commit>\n'
                '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
                '        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>\n'
                '      </after-commit>\n'
                '    </transition>\n'
            )
            kind = "REWARD_SHORTHAND"
        else:
            continue
        result.append(Insertion(int(row["quest_id"]), path, row["server_source_state"], target,
                                int(row["npc_id"]), int(action_id), ACTION_NAMES[action_id],
                                kind, block, priority))
        seen.add(key)
    return sorted(result, key=lambda item: (item.quest_id, item.source, item.npc_id, item.action_id))


def write_manifest(path: Path, rows: list[Insertion]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "source", "target", "npc_id", "action_id",
                         "action_name", "kind"))
        for row in rows:
            writer.writerow((row.quest_id, row.path.relative_to(ROOT).as_posix(), row.source,
                             row.target, row.npc_id, row.action_id, row.action_name, row.kind))


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
        addition = "".join(
            item.block if item.block.endswith("\n") else item.block + "\n"
            for item in items)
        updated = source[:closing] + addition + source[closing:]
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        ET.fromstring(updated)
        if write:
            path.write_text(updated, encoding="utf-8")
    return len(rows)


def main() -> int:
    args = parse_args()
    rows = collect(args)
    root = args.root.resolve()
    manifest = args.manifest if args.manifest.is_absolute() else root / args.manifest
    write_manifest(manifest, rows)
    changed = apply(rows, args.write)
    print(f"routes={len(rows)} changed={changed} files={len({row.path for row in rows})} "
          f"write={args.write} manifest={manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
