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
import sys


ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / ".agent/summary/quest"))
from quest_dialog_symbols import load_maps  # noqa: E402
TRANSITION_PATTERN = re.compile(r"<transition\b[^>]*>.*?</transition>", re.DOTALL)
PATH_TARGET_PATTERN = re.compile(r"-> ([^ ]+) \+ page \d+$")


@dataclass(frozen=True)
class Replacement:
    quest_id: int
    path: Path
    source: str
    target: str
    npc_id: str
    action_id: str
    page: str
    target_node: str
    actual_path: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Close priority=1 fallback routes whose hard-coded page is absent from task HTML.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agent/summary/quest-load-fail/quest-order-audit-current.csv"))
    parser.add_argument("--transitions", type=Path,
                        default=Path(".agent/summary/quest-load-fail/compiled-transitions.tsv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agent/summary/quest-load-fail/missing-fallback-closures.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def read_priority_one_routes(path: Path) -> set[tuple[str, str, str, str, str, str, str]]:
    result: set[tuple[str, str, str, str, str, str, str]] = set()
    with path.open(encoding="utf-8", newline="") as stream:
        for row in csv.DictReader(stream, delimiter="\t"):
            if row["priority"] != "1":
                continue
            result.add((row["quest_id"], row["source_node"], row["target_node"],
                        row["event_owner"], row["event_action"], row["after_commit"], row["priority"]))
    return result


def collect(args: argparse.Namespace) -> list[Replacement]:
    root = args.root.resolve()
    audit_path = args.audit if args.audit.is_absolute() else root / args.audit
    transitions_path = args.transitions if args.transitions.is_absolute() else root / args.transitions
    priority_one = read_priority_one_routes(transitions_path)
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    result: list[Replacement] = []
    seen: set[tuple[str, str, str, str, str, str]] = set()
    with audit_path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if not row["unresolved_reason"].startswith("compiled IR emits a task page"):
                continue
            match = PATH_TARGET_PATTERN.search(row["actual_path"])
            if match is None or row["shown_page"] not in ("2716", "2375"):
                continue
            target_node = match.group(1)
            if target_node != row["server_source_state"] or row["trigger_action"] not in ("39", "1009"):
                continue
            page_name = "SELECT6" if row["shown_page"] == "2716" else "SELECT5"
            after_commit = f"[SHOW_QUEST_PAGE:{row['shown_page']}]"
            route_key = (row["quest_id"], row["server_source_state"], target_node, row["npc_id"],
                         row["trigger_action"], after_commit, "1")
            if route_key not in priority_one:
                continue
            key = (row["quest_id"], row["server_source_state"], target_node, row["npc_id"],
                   row["trigger_action"], row["shown_page"])
            if key in seen:
                continue
            seen.add(key)
            result.append(Replacement(
                quest_id=int(row["quest_id"]),
                path=quest_dir / f"{row['quest_id']}.xml",
                source=row["server_source_state"],
                target=target_node,
                npc_id=row["npc_id"],
                action_id=row["trigger_action"],
                page=page_name,
                target_node=target_node,
                actual_path=row["actual_path"],
            ))
    return sorted(result, key=lambda item: (item.quest_id, item.npc_id, item.action_id, item.page))


def write_manifest(path: Path, rows: list[Replacement]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "source", "target", "npc_id", "action_id", "page",
                         "replacement", "actual_path"))
        for row in rows:
            writer.writerow((row.quest_id, row.path.relative_to(ROOT).as_posix(), row.source,
                             row.target, row.npc_id, row.action_id, row.page, "CLOSE_DIALOG",
                             row.actual_path))


def event_matches(block: ET.Element, row: Replacement, actions_by_id: dict[int, str]) -> bool:
    action_name = actions_by_id[int(row.action_id)]
    for event in block.findall("./event/dialog"):
        if event.get("type") != "TALK_TO_NPC" or event.get("npc-id") != row.npc_id:
            continue
        values = [event.get("action", ""), *event.get("actions", "").replace(",", " ").split()]
        if row.action_id in values or action_name in values:
            return True
    return False


def apply(rows: list[Replacement], write: bool) -> tuple[int, list[str]]:
    actions_by_id, _, _, _ = load_maps(ROOT)
    grouped: dict[Path, list[Replacement]] = defaultdict(list)
    for row in rows:
        grouped[row.path].append(row)
    changed = 0
    unresolved: list[str] = []
    for path, replacements in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        blocks = list(TRANSITION_PATTERN.finditer(source))
        edits: list[tuple[int, int, str]] = []
        file_unresolved: list[str] = []
        for row in replacements:
            matched: list[re.Match[str]] = []
            for match in blocks:
                raw = match.group(0)
                try:
                    block = ET.fromstring(raw)
                except ET.ParseError:
                    continue
                if (block.get("source") != row.source or block.get("target") != row.target
                        or block.get("priority") != "1"
                        or not event_matches(block, row, actions_by_id)):
                    continue
                page = block.find("./after-commit/dialog[@type='SHOW_QUEST_PAGE']")
                if page is not None and page.get("page") == row.page:
                    matched.append(match)
            if len(matched) != 1:
                value = (f"{row.quest_id}:{row.source}:{row.target}:{row.npc_id}:"
                         f"{row.action_id}:{row.page}")
                unresolved.append(value)
                file_unresolved.append(value)
                continue
            match = matched[0]
            updated = re.sub(r'<dialog\s+type="SHOW_QUEST_PAGE"\s+page="'
                             + re.escape(row.page) + r'"\s*/>', '<close-dialog/>', match.group(0), count=1)
            if updated == match.group(0):
                raise RuntimeError(f"failed to replace failure page in {path}: {row}")
            edits.append((match.start(), match.end(), updated))
        if file_unresolved:
            continue
        for start, end, updated in sorted(edits, reverse=True):
            source = source[:start] + updated + source[end:]
            changed += 1
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        ET.fromstring(source)
        if write:
            path.write_text(source, encoding="utf-8")
    return changed, unresolved


def main() -> int:
    args = parse_args()
    rows = collect(args)
    manifest = args.manifest if args.manifest.is_absolute() else args.root.resolve() / args.manifest
    write_manifest(manifest, rows)
    changed, unresolved = apply(rows, args.write)
    print(f"candidates={len(rows)} changed={changed} unresolved={len(unresolved)} "
          f"write={args.write} manifest={manifest}")
    for value in unresolved:
        print(f"UNRESOLVED {value}")
    return 1 if unresolved else 0


if __name__ == "__main__":
    raise SystemExit(main())
