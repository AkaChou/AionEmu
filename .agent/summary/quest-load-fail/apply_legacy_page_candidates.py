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
OPENING_DIALOG = re.compile(r"<dialog\b[^>]*>")
TRANSITION_PATTERN = re.compile(r"(?P<block><transition\b[^>]*>.*?</transition>)", re.DOTALL)
BLOCK_TAG_PATTERN = re.compile(r"(?P<block><(?P<tag>npc-start|npc-report)\b[^>]*?/>)", re.DOTALL)


@dataclass(frozen=True)
class Replacement:
    quest_id: int
    path: Path
    kind: str
    source_node: str
    target_node: str
    npc_id: str
    action_id: int
    action_name: str
    old_page: str
    new_page: str
    evidence_page: int
    evidence_source: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Apply unique legacy-template page candidates to explicit task dialog routes.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agent/summary/quest-load-fail/quest-order-audit-current.csv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agent/summary/quest-load-fail/legacy-page-candidates.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def read_two_columns(path: Path, id_field: str, value_field: str) -> tuple[dict[int, str], dict[str, int]]:
    by_id: dict[int, str] = {}
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            by_id[int(row[id_field])] = row[value_field]
    return by_id, {value: item_id for item_id, value in by_id.items()}


def collect(args: argparse.Namespace) -> list[Replacement]:
    root = args.root.resolve()
    mapping = root / "docs/quest/client-dialog-mapping"
    actions_by_id, _ = read_two_columns(mapping / "client-hyperlinks.csv", "action_id", "action_constant")
    pages_by_id, pages_by_name = read_two_columns(mapping / "client-html-pages.csv", "page_id", "page_constant")
    contracts: dict[int, dict[str, str]] = {}
    grouped_contracts: dict[int, list[dict[str, str]]] = defaultdict(list)
    with (mapping / "legacy-quest-dialog-contracts.csv").open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["contract_scope"] == "FULL":
                grouped_contracts[int(row["quest_id"])].append(row)
    for quest_id, rows in grouped_contracts.items():
        if len(rows) == 1:
            contracts[quest_id] = rows[0]
    quest_pages: dict[int, set[int]] = defaultdict(set)
    with (mapping / "quest-dialog-pages.csv").open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["source_variant"] == "active":
                quest_pages[int(row["quest_id"])].add(int(row["page_id"]))

    audit_path = args.audit if args.audit.is_absolute() else root / args.audit
    with audit_path.open(encoding="utf-8-sig", newline="") as stream:
        audit_rows = list(csv.DictReader(stream))
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    result: dict[tuple[object, ...], Replacement] = {}
    for row in audit_rows:
        if not row["unresolved_reason"].startswith("compiled IR emits a task page"):
            continue
        quest_id = int(row["quest_id"])
        contract = contracts.get(quest_id)
        if contract is None:
            continue
        old_page_id = int(row["shown_page"])
        action_id = int(row["trigger_action"])
        source_node = row["server_source_state"]
        candidate = 0
        if action_id == 31 and source_node in ("unaccepted", "complete"):
            candidate = int(contract["start_page_id"] or 0)
        elif action_id == 31:
            candidate = int(contract["report_page_id"] or 0)
        elif source_node == "reward":
            candidate = int(contract["reward_page_id"] or 0)
        if candidate == 0 or candidate == old_page_id or candidate not in quest_pages[quest_id]:
            continue
        match = PATH_PATTERN.search(row["actual_path"])
        if match is None:
            continue
        target_node = match.group(1)
        action_name = actions_by_id.get(action_id, "").removeprefix("HACTION_")
        old_page = pages_by_id[old_page_id].removeprefix("HTML_PAGE_")
        new_page = pages_by_id[candidate].removeprefix("HTML_PAGE_")
        path = quest_dir / f"{quest_id}.xml"
        document = ET.parse(path).getroot()
        matches: list[str] = []

        def matches_page_route(element: ET.Element) -> bool:
            return (element.get("source") == source_node
                    and (element.get("target") == target_node
                         or (target_node == source_node)))

        for dialog in document.findall("./transitions/dialog"):
            if (dialog.get("type") == "NPC_REPORT" and dialog.get("npc-id") == row["npc_id"]
                    and matches_page_route(dialog) and dialog.get("page") == old_page):
                matches.append("DIALOG")
            elif (dialog.get("type") == "NPC_START" and dialog.get("npc-id") == row["npc_id"]
                    and matches_page_route(dialog) and dialog.get("start-page") == old_page):
                matches.append("START_DIALOG")
        for block in document.findall("./transitions/npc-start"):
            if block.get("type") not in (None, "NPC_START"):
                continue
            if (block.get("npc-id") == row["npc_id"] and matches_page_route(block)
                    and block.get("start-page") == old_page):
                matches.append("NPC_START_BLOCK")
        for block in document.findall("./transitions/npc-report"):
            if block.get("type") not in (None, "NPC_REPORT"):
                continue
            if (block.get("npc-id") == row["npc_id"] and matches_page_route(block)
                    and block.get("page") == old_page):
                matches.append("NPC_REPORT_BLOCK")
        for transition in document.findall("./transitions/transition"):
            if transition.get("source") != source_node or transition.get("target") != target_node:
                continue
            event = transition.find("./event/dialog")
            if event is None or event.get("npc-id") != row["npc_id"] or event.get("action") != action_name:
                continue
            if any(response.get("type") == "SHOW_QUEST_PAGE" and response.get("page") == old_page
                   for response in transition.findall("./after-commit/dialog")):
                matches.append("TRANSITION")
        if len(matches) != 1:
            continue
        kind = matches[0]
        key = (quest_id, kind, source_node, target_node, row["npc_id"], action_id, old_page, new_page)
        result[key] = Replacement(
            quest_id=quest_id,
            path=path,
            kind=kind,
            source_node=source_node,
            target_node=target_node,
            npc_id=row["npc_id"],
            action_id=action_id,
            action_name=action_name,
            old_page=old_page,
            new_page=new_page,
            evidence_page=candidate,
            evidence_source=row["source_file"],
        )
    return sorted(result.values(), key=lambda item: (
        item.quest_id, item.kind, item.source_node, item.npc_id, item.action_id))


def write_manifest(path: Path, rows: list[Replacement]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "kind", "source_node", "target_node", "npc_id",
                         "action_id", "action_name", "old_page", "new_page", "evidence_page",
                         "evidence_source"))
        for row in rows:
            writer.writerow((row.quest_id, row.path.relative_to(ROOT).as_posix(), row.kind,
                             row.source_node, row.target_node, row.npc_id, row.action_id,
                             row.action_name, row.old_page, row.new_page, row.evidence_page,
                             row.evidence_source))


def replace_attribute(block: str, attribute: str, old: str, new: str) -> str:
    updated = re.sub(rf'(\b{re.escape(attribute)}\s*=\s*"){re.escape(old)}(")', rf'\g<1>{new}\g<2>',
                     block, count=1)
    if updated == block:
        raise RuntimeError(f"failed to replace {attribute}={old}")
    return updated


def apply(rows: list[Replacement]) -> int:
    grouped: dict[Path, list[Replacement]] = defaultdict(list)
    for row in rows:
        grouped[row.path].append(row)
    changed = 0
    for path, items in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        remaining = list(items)

        def replace_dialog(match: re.Match[str]) -> str:
            nonlocal changed
            block = match.group(0)
            attrs = dict(re.findall(r'([A-Za-z_][\w-]*)="([^"]*)"', block))
            for index, item in enumerate(remaining):
                if item.kind not in {"DIALOG", "START_DIALOG"}:
                    continue
                expected_type = "NPC_REPORT" if item.kind == "DIALOG" else "NPC_START"
                attribute = "page" if item.kind == "DIALOG" else "start-page"
                if (attrs.get("type") == expected_type and attrs.get("npc-id") == item.npc_id
                        and attrs.get("source") == item.source_node
                        and (attrs.get("target") == item.target_node
                             or item.target_node == item.source_node)
                        and attrs.get(attribute) == item.old_page):
                    remaining.pop(index)
                    changed += 1
                    return replace_attribute(block, attribute, item.old_page, item.new_page)
            return block

        source = OPENING_DIALOG.sub(replace_dialog, source)

        def replace_transition(match: re.Match[str]) -> str:
            nonlocal changed
            block = match.group("block")
            opening = re.match(r"<transition\b[^>]*>", block).group(0)
            attrs = dict(re.findall(r'([A-Za-z_][\w-]*)="([^"]*)"', opening))
            for index, item in enumerate(remaining):
                if item.kind != "TRANSITION":
                    continue
                if (attrs.get("source") == item.source_node and attrs.get("target") == item.target_node
                        and f'<dialog type="TALK_TO_NPC" npc-id="{item.npc_id}" action="{item.action_name}"/>' in block
                        and f'page="{item.old_page}"' in block):
                    remaining.pop(index)
                    changed += 1
                    return replace_attribute(block, "page", item.old_page, item.new_page)
            return block

        source = TRANSITION_PATTERN.sub(replace_transition, source)

        def replace_block(match: re.Match[str]) -> str:
            nonlocal changed
            block = match.group("block")
            tag = match.group("tag")
            attrs = dict(re.findall(r'([A-Za-z_][\w-]*)="([^"]*)"', block))
            expected_kind = "NPC_START_BLOCK" if tag == "npc-start" else "NPC_REPORT_BLOCK"
            attribute = "start-page" if tag == "npc-start" else "page"
            for index, item in enumerate(remaining):
                if item.kind != expected_kind:
                    continue
                if (attrs.get("npc-id") == item.npc_id and attrs.get("source") == item.source_node
                        and (attrs.get("target") == item.target_node
                             or item.target_node == item.source_node)
                        and attrs.get(attribute) == item.old_page):
                    remaining.pop(index)
                    changed += 1
                    return replace_attribute(block, attribute, item.old_page, item.new_page)
            return block

        source = BLOCK_TAG_PATTERN.sub(replace_block, source)
        if remaining:
            raise RuntimeError(f"could not locate {len(remaining)} legacy page candidates in {path}")
        if hashlib.sha256(path.read_bytes()).digest() != hashlib.sha256(before).digest():
            raise RuntimeError(f"concurrent change detected before write: {path}")
        ET.fromstring(source)
        path.write_text(source, encoding="utf-8")
    return changed


def main() -> int:
    args = parse_args()
    rows = collect(args)
    manifest = args.manifest if args.manifest.is_absolute() else args.root.resolve() / args.manifest
    write_manifest(manifest, rows)
    print(f"candidates={len(rows)} files={len({row.path for row in rows})} manifest={manifest}")
    if args.write:
        print(f"changed_routes={apply(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
