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


@dataclass(frozen=True)
class Insertion:
    quest_id: int
    path: Path
    source: str
    target: str
    npc_id: str
    action: str
    kind: str
    work_items: tuple[tuple[str, str], ...]
    actual_path: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Insert standard accept/refuse/ask/set-reward control routes.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agents/summary/quest-load-fail/quest-order-audit-final.csv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agents/summary/quest-load-fail/standard-control-routes.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def collect(args: argparse.Namespace) -> list[Insertion]:
    root = args.root.resolve()
    audit_path = args.audit if args.audit.is_absolute() else root / args.audit
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    cache: dict[int, tuple[Path, ET.Element, dict[str, str], dict[str, str], tuple[tuple[str, str], ...]]] = {}
    for path in quest_dir.glob("*.xml"):
        quest_id = int(path.stem)
        document = ET.parse(path).getroot()
        statuses = {node.get("label", ""): node.get("status", "")
                    for node in document.findall("./nodes/node")}
        targets = {status: next((label for label, actual in statuses.items() if actual == status), "")
                   for status in ("START", "REWARD")}
        counts = {status: sum(actual == status for actual in statuses.values())
                  for status in ("START", "REWARD")}
        work_items = tuple((item.get("id", ""), item.get("count", ""))
                           for item in document.findall("./metadata/work-items/item"))
        cache[quest_id] = (path, document, statuses,
                           {**targets,
                            "START_COUNT": str(counts["START"]),
                            "REWARD_COUNT": str(counts["REWARD"])},
                           work_items)

    result: list[Insertion] = []
    seen: set[tuple[int, str, str, str]] = set()
    with audit_path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if not row["unresolved_reason"].startswith("visible client action has no route"):
                continue
            quest_id = int(row["quest_id"])
            path, _, statuses, targets, work_items = cache[quest_id]
            source = row["server_source_state"]
            source_status = statuses.get(source)
            visible = row["client_visible_action"]
            kind = target = action = ""
            if visible in ("1003", "20001") and source_status == "NONE":
                kind, target = "REFUSE", source
                action = "QUEST_REFUSE_1" if visible == "1003" else "QUEST_REFUSE_SIMPLE"
            elif visible == "20000" and source_status == "NONE" and targets["START_COUNT"] == "1":
                kind, target, action = "ACCEPT_SIMPLE", targets["START"], "QUEST_ACCEPT_SIMPLE"
            elif visible == "1007" and source_status == "NONE":
                kind, target, action = "ASK_ACCEPT", source, "ASK_QUEST_ACCEPT"
            elif visible == "10255" and source_status == "START" and targets["REWARD_COUNT"] == "1":
                kind, target, action = "SET_REWARD", targets["REWARD"], "SET_SUCCEED"
            else:
                continue
            key = (quest_id, source, row["npc_id"], visible)
            if key in seen:
                continue
            seen.add(key)
            result.append(Insertion(quest_id, path, source, target, row["npc_id"], action,
                                    kind, work_items if kind == "SET_REWARD" else (),
                                    row["actual_path"]))
    return sorted(result, key=lambda item: (item.quest_id, item.source, item.npc_id, item.action))


def write_manifest(path: Path, rows: list[Insertion]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "source", "target", "npc_id", "action", "kind",
                         "work_items", "actual_path"))
        for row in rows:
            writer.writerow((row.quest_id, row.path.relative_to(ROOT).as_posix(), row.source,
                             row.target, row.npc_id, row.action, row.kind,
                             " ".join(f"{item_id}:{count}" for item_id, count in row.work_items),
                             row.actual_path))


def render(row: Insertion) -> str:
    if row.kind in ("REFUSE", "ACCEPT_SIMPLE"):
        conditions = "      <conditions><start-eligible/></conditions>\n" if row.kind == "ACCEPT_SIMPLE" else ""
        after = ("        <sync-quest-state mode=\"VISIBILITY_REFRESH\"/>\n"
                 "        <close-dialog/>\n") if row.kind == "ACCEPT_SIMPLE" else "        <close-dialog/>\n"
        return (
            f'    <transition source="{row.source}" target="{row.target}">\n'
            '      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{row.npc_id}" action="{row.action}"/>\n'
            '      </event>\n'
            f'{conditions}'
            '      <after-commit>\n'
            f'{after}'
            '      </after-commit>\n'
            '    </transition>\n'
        )
    if row.kind == "ASK_ACCEPT":
        return (
            f'    <transition source="{row.source}" target="{row.target}">\n'
            '      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{row.npc_id}" action="{row.action}"/>\n'
            '      </event>\n'
            '      <after-commit>\n'
            '        <dialog type="SHOW_QUEST_PAGE" page="SHOW_ASK_QUEST_ACCEPT_WINDOW"/>\n'
            '      </after-commit>\n'
            '    </transition>\n'
        )
    if row.kind == "SET_REWARD":
        conditions = "".join(
            f'        <has-item item-id="{item_id}" count="{count}"/>\n'
            for item_id, count in row.work_items)
        actions = "".join(
            f'        <remove-item item-id="{item_id}" count="ALL"/>\n'
            for item_id, _ in row.work_items)
        condition_block = ("      <conditions>\n" + conditions + "      </conditions>\n"
                           if conditions else "")
        action_block = ("      <actions>\n" + actions + "      </actions>\n"
                        if actions else "")
        return (
            f'    <transition source="{row.source}" target="{row.target}">\n'
            '      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{row.npc_id}" action="{row.action}"/>\n'
            '      </event>\n'
            f'{condition_block}{action_block}'
            '      <after-commit>\n'
            '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
            '        <close-dialog/>\n'
            '      </after-commit>\n'
            '    </transition>\n'
        )
    raise ValueError(f"unsupported insertion kind {row.kind}")


def apply(rows: list[Insertion], write: bool) -> int:
    grouped: dict[Path, list[Insertion]] = defaultdict(list)
    for row in rows:
        grouped[row.path].append(row)
    changed = 0
    for path, items in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        closing = source.rfind("</transitions>")
        if closing < 0:
            raise RuntimeError(f"missing transitions closing tag: {path}")
        updated = source[:closing] + "".join(render(row) for row in items) + source[closing:]
        ET.fromstring(updated)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        if write:
            path.write_text(updated, encoding="utf-8")
        changed += len(items)
    return changed


def main() -> int:
    args = parse_args()
    rows = collect(args)
    manifest = args.manifest if args.manifest.is_absolute() else args.root.resolve() / args.manifest
    write_manifest(manifest, rows)
    changed = apply(rows, args.write)
    kinds = defaultdict(int)
    for row in rows:
        kinds[row.kind] += 1
    print(f"candidates={len(rows)} changed={changed} files={len({row.path for row in rows})} "
          f"kinds={dict(kinds)} write={args.write} manifest={manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
