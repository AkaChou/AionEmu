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
TRANSITION_PATTERN = re.compile(r"(?P<block><transition\b[^>]*>.*?</transition>)", re.DOTALL)


@dataclass(frozen=True)
class Clone:
    quest_id: int
    path: Path
    source_node: str
    npc_id: str
    action_id: int
    action_name: str
    templates: tuple[str, ...]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Clone an existing same-source/same-action NPC transition for a client-proven NPC alias.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agent/summary/quest-load-fail/quest-order-audit-current.csv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agent/summary/quest-load-fail/missing-action-npc-clones.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def read_two_columns(path: Path, id_field: str, value_field: str) -> dict[int, str]:
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return {int(row[id_field]): row[value_field] for row in csv.DictReader(stream)}


def collect(args: argparse.Namespace) -> list[Clone]:
    root = args.root.resolve()
    mapping = root / "docs/quest/client-dialog-mapping"
    actions = read_two_columns(mapping / "client-hyperlinks.csv", "action_id", "action_constant")
    audit_path = args.audit if args.audit.is_absolute() else root / args.audit
    with audit_path.open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    xml_cache: dict[int, str] = {}
    result: list[Clone] = []
    seen: set[tuple[int, str, str, int]] = set()
    for row in rows:
        if not row["unresolved_reason"].startswith("visible client action has no route") or not row["npc_id"]:
            continue
        try:
            action_id = int(row["client_visible_action"])
        except ValueError:
            continue
        action_name = actions.get(action_id, "").removeprefix("HACTION_")
        match = PATH_PATTERN.search(row["actual_path"])
        if not action_name or match is None:
            continue
        quest_id = int(row["quest_id"])
        source_node = match.group(1)
        key = (quest_id, source_node, row["npc_id"], action_id)
        if key in seen:
            continue
        path = quest_dir / f"{quest_id}.xml"
        source = xml_cache.setdefault(quest_id, path.read_text(encoding="utf-8"))
        templates: dict[str, str] = {}
        template_npcs: set[str] = set()
        for transition in TRANSITION_PATTERN.findall(source):
            opening = re.match(r"<transition\b[^>]*>", transition).group(0)
            attrs = dict(re.findall(r'([A-Za-z_][\w-]*)="([^"]*)"', opening))
            if attrs.get("source") != source_node:
                continue
            event = re.search(r'<dialog\b[^>]*type="TALK_TO_NPC"[^>]*/>', transition)
            if event is None:
                continue
            event_attrs = dict(re.findall(r'([A-Za-z_][\w-]*)="([^"]*)"', event.group(0)))
            if event_attrs.get("action") != action_name or event_attrs.get("npc-id") == row["npc_id"]:
                continue
            template_npcs.add(event_attrs.get("npc-id", ""))
            normalized = re.sub(r'(\bnpc-id=")[^"]*(")', r'\g<1>*\g<2>', transition, count=1)
            templates[normalized] = transition
        if len(template_npcs) != 1 or len(templates) not in (1, 2):
            continue
        updated = tuple(
            re.sub(r'(\bnpc-id=")[^"]*(")', rf'\g<1>{row["npc_id"]}\g<2>', template, count=1)
            for template in templates.values()
        )
        result.append(Clone(quest_id, path, source_node, row["npc_id"], action_id, action_name, updated))
        seen.add(key)
    return sorted(result, key=lambda item: (item.quest_id, item.source_node, item.npc_id, item.action_id))


def write_manifest(path: Path, rows: list[Clone]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "source_node", "npc_id", "action_id", "action_name",
                         "template_count"))
        for row in rows:
            writer.writerow((row.quest_id, row.path.relative_to(ROOT).as_posix(), row.source_node,
                             row.npc_id, row.action_id, row.action_name, len(row.templates)))


def apply(rows: list[Clone]) -> int:
    grouped: dict[Path, list[Clone]] = defaultdict(list)
    for row in rows:
        grouped[row.path].append(row)
    for path, items in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        closing = source.rfind("</transitions>")
        if closing < 0:
            raise RuntimeError(f"missing transitions closing tag: {path}")
        addition = "".join(template + "\n" for row in items for template in row.templates)
        updated = source[:closing] + addition + source[closing:]
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
    print(f"clones={len(rows)} files={len({row.path for row in rows})} manifest={manifest}")
    if args.write:
        print(f"changed_routes={apply(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
