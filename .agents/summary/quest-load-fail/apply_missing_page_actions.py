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
ACTION_NAME_PATTERN = re.compile(r"^\s*([A-Z][A-Z0-9_]*)\(", re.MULTILINE)


@dataclass(frozen=True)
class Insertion:
    quest_id: int
    path: Path
    source_node: str
    npc_id: str
    action_id: int
    action_name: str
    page_id: int
    page_name: str
    close_dialog: bool
    start_eligible: bool
    evidence_source: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Insert missing page-turn and finish-dialog routes proven by Aion 5.8 client buttons.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agents/summary/quest-load-fail/quest-order-audit-current.csv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agents/summary/quest-load-fail/missing-page-actions.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def read_two_columns(path: Path, id_field: str, value_field: str) -> tuple[dict[int, str], dict[str, int]]:
    by_id: dict[int, str] = {}
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            by_id[int(row[id_field])] = row[value_field]
    return by_id, {value: item_id for item_id, value in by_id.items()}


def collect(args: argparse.Namespace) -> list[Insertion]:
    root = args.root.resolve()
    mapping = root / "docs/quest/client-dialog-mapping"
    actions_by_id, _ = read_two_columns(mapping / "client-hyperlinks.csv", "action_id", "action_constant")
    pages_by_id, pages_by_name = read_two_columns(mapping / "client-html-pages.csv", "page_id", "page_constant")
    quest_pages: dict[int, set[int]] = defaultdict(set)
    with (mapping / "quest-dialog-pages.csv").open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["source_variant"] == "active":
                quest_pages[int(row["quest_id"])].add(int(row["page_id"]))
    repeatable_quests: set[int] = set()
    complete_nodes: dict[int, set[str]] = defaultdict(set)
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    for quest_path in quest_dir.glob("*.xml"):
        document = ET.parse(quest_path).getroot()
        repeat = document.find("./metadata/repeat")
        if repeat is not None and int(repeat.get("max-repeat-count", "0")) > 1:
            repeatable_quests.add(int(quest_path.stem))
        complete_nodes[int(quest_path.stem)] = {
            node.get("label", "") for node in document.findall("./nodes/node")
            if node.get("status") == "COMPLETE"
        }

    audit_path = args.audit if args.audit.is_absolute() else root / args.audit
    with audit_path.open(encoding="utf-8-sig", newline="") as stream:
        audit_rows = list(csv.DictReader(stream))

    grouped: dict[tuple[int, str, str, int, int], list[dict[str, str]]] = defaultdict(list)
    for row in audit_rows:
        if not row["unresolved_reason"].startswith("visible client action has no route"):
            continue
        try:
            action_id = int(row["client_visible_action"])
        except ValueError:
            continue
        if not row["npc_id"] and action_id != 1008:
            continue
        action_constant = actions_by_id.get(action_id, "")
        action_name = action_constant.removeprefix("HACTION_")
        page_id = 0
        if action_id == 1008:
            page_name = ""
        elif (action_name.startswith("SELECT") and not action_name.startswith("SELECTED_")
              and not action_name.startswith("SELECT_QUEST")):
            page_name = action_name
            page_id = pages_by_name.get("HTML_PAGE_" + action_name, 0)
            if page_id not in quest_pages[int(row["quest_id"])]:
                continue
        else:
            continue
        match = PATH_PATTERN.search(row["actual_path"])
        if match is None:
            continue
        source_node = match.group(1)
        grouped[(int(row["quest_id"]), source_node, row["npc_id"], action_id, page_id)].append(row)

    result: list[Insertion] = []
    for (quest_id, source_node, npc_id, action_id, page_id), rows in grouped.items():
        quest_path = root / "src/main/resources/aion/data/static_data/quest_definition/quests" / f"{quest_id}.xml"
        result.append(Insertion(
            quest_id=quest_id,
            path=quest_path,
            source_node=source_node,
            npc_id=npc_id,
            action_id=action_id,
            action_name=actions_by_id[action_id].removeprefix("HACTION_"),
            page_id=page_id,
            page_name=pages_by_id.get(page_id, "").removeprefix("HTML_PAGE_"),
            close_dialog=action_id == 1008,
            start_eligible=(quest_id in repeatable_quests and source_node in complete_nodes[quest_id]),
            evidence_source=rows[0]["evidence_source"],
        ))
    return sorted(result, key=lambda item: (item.quest_id, item.source_node, item.npc_id, item.action_id))


def write_manifest(path: Path, rows: list[Insertion]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "source_node", "npc_id", "action_id", "action_name",
                         "response", "page_id", "page_name", "start_eligible", "evidence_source"))
        for row in rows:
            writer.writerow((row.quest_id, row.path.relative_to(ROOT).as_posix(), row.source_node,
                             row.npc_id, row.action_id, row.action_name,
                             "CLOSE_DIALOG" if row.close_dialog else "SHOW_QUEST_PAGE",
                             row.page_id, row.page_name, row.start_eligible, row.evidence_source))


def render(row: Insertion) -> str:
    response = "        <close-dialog/>" if row.close_dialog else (
        f'        <dialog type="SHOW_QUEST_PAGE" page="{row.page_name}"/>')
    event = ('        <dialog type="QUEST_ACTION" action="' + row.action_name + '"/>\n'
             if not row.npc_id else
             '        <dialog type="TALK_TO_NPC" npc-id="' + row.npc_id + '" action="' + row.action_name + '"/>\n')
    conditions = ('      <conditions>\n'
                  '        <start-eligible/>\n'
                  '      </conditions>\n' if row.start_eligible else '')
    return (
        '    <transition source="' + row.source_node + '" target="' + row.source_node + '">\n'
        + '      <event>\n'
        + event
        + '      </event>\n'
        + conditions
        + '      <after-commit>\n'
        + response + '\n'
        + '      </after-commit>\n'
        + '    </transition>\n'
    )


def apply_rows(rows: list[Insertion]) -> int:
    grouped: dict[Path, list[Insertion]] = defaultdict(list)
    for row in rows:
        grouped[row.path].append(row)
    for path, items in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        closing = source.rfind("</transitions>")
        if closing < 0:
            raise RuntimeError(f"missing transitions closing tag: {path}")
        addition = "".join(render(item) for item in items)
        updated = source[:closing] + addition + source[closing:]
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        ET.fromstring(updated)
        path.write_text(updated, encoding="utf-8")
    return len(rows)


def main() -> int:
    args = parse_args()
    rows = collect(args)
    manifest = args.manifest if args.manifest.is_absolute() else args.root.resolve() / args.manifest
    write_manifest(manifest, rows)
    print(f"insertions={len(rows)} files={len({row.path for row in rows})} manifest={manifest}")
    if args.write:
        print(f"changed_routes={apply_rows(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
