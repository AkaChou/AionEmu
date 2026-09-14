#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / ".agents/summary/quest"))
from quest_dialog_symbols import attributes, load_maps  # noqa: E402


TRANSITION_PATTERN = re.compile(r"<transition\b[^>]*>.*?</transition>", re.DOTALL)
OPENING_DIALOG_PATTERN = re.compile(r"<dialog\b[^>]*>")
PATH_TARGET_PATTERN = re.compile(r"-> ([^ ]+) \+ page \d+$")


@dataclass(frozen=True)
class Replacement:
    quest_id: int
    path: Path
    source: str
    target: str
    npc_id: str
    action_id: int
    old_page: str
    new_page: str
    candidate_page_id: int
    client_file: str
    client_sha256: str
    evidence: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Apply task-owned page candidates closed by the compiled dialog route graph.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agents/summary/quest-load-fail/quest-order-audit-current.csv"))
    parser.add_argument("--transitions", type=Path,
                        default=Path(".agents/summary/quest-load-fail/compiled-transitions.tsv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agents/summary/quest-load-fail/unique-page-candidates.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def read_client_pages(root: Path) -> dict[tuple[int, int], dict[str, object]]:
    mapping = root / "docs/quest/client-dialog-mapping"
    rows: dict[tuple[int, int], dict[str, object]] = {}
    with (mapping / "quest-dialog-pages.csv").open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["source_variant"] != "active" or row["page_mapping"] != "exact":
                continue
            key = (int(row["quest_id"]), int(row["page_id"]))
            rows[key] = {
                "page_id": key[1],
                "page_order": int(row["page_order"]),
                "client_file": row["source_file"],
                "client_sha256": row["source_sha256"],
                "actions": set(),
            }

    with (mapping / "quest-dialog-action-details.csv").open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["source_variant"] != "active" or row["action_mapping"] != "exact" or not row["action_id"]:
                continue
            key = (int(row["quest_id"]), int(row["page_id"]))
            if key in rows:
                actions = rows[key]["actions"]
                assert isinstance(actions, set)
                actions.add(int(row["action_id"]))
    return rows


def read_pages_by_id(root: Path) -> dict[int, str]:
    result: dict[int, str] = {}
    path = root / "docs/quest/client-dialog-mapping/client-html-pages.csv"
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            result[int(row["page_id"])] = row["page_constant"].removeprefix("HTML_PAGE_")
    return result


def read_routes(path: Path) -> dict[tuple[int, str, str, int], list[tuple[str, str]]]:
    routes: dict[tuple[int, str, str, int], list[tuple[str, str]]] = defaultdict(list)
    with path.open(encoding="utf-8", newline="") as stream:
        for row in csv.DictReader(stream, delimiter="\t"):
            if (row["event_type"] not in ("TALK_TO_NPC", "QUEST_DIALOG")
                    or not row["event_action"].isdigit()):
                continue
            routes[(int(row["quest_id"]), row["source_node"], row["event_owner"],
                    int(row["event_action"]))].append((row["target_node"], row["after_commit"]))
    return routes


def collect(args: argparse.Namespace) -> tuple[list[Replacement], dict[str, int]]:
    root = args.root.resolve()
    audit_path = args.audit if args.audit.is_absolute() else root / args.audit
    transitions_path = args.transitions if args.transitions.is_absolute() else root / args.transitions
    client_pages = read_client_pages(root)
    pages_by_id = read_pages_by_id(root)
    routes = read_routes(transitions_path)
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    node_status: dict[tuple[int, str], str] = {}
    for path in quest_dir.glob("*.xml"):
        document = ET.parse(path).getroot()
        quest_id = int(document.get("id", path.stem))
        for node in document.findall("./nodes/node"):
            node_status[(quest_id, node.get("label", ""))] = node.get("status", "")

    replacements: list[Replacement] = []
    stats = defaultdict(int)
    seen: set[tuple[object, ...]] = set()
    with audit_path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if not row["unresolved_reason"].startswith("compiled IR emits a task page"):
                continue
            match = PATH_TARGET_PATTERN.search(row["actual_path"])
            if match is None:
                stats["invalid_path"] += 1
                continue
            quest_id = int(row["quest_id"])
            shown_page = int(row["shown_page"])
            trigger_action = int(row["trigger_action"])
            target = match.group(1)
            candidates: list[int] = []
            for (candidate_quest, page_id), page in client_pages.items():
                if candidate_quest != quest_id or page_id == shown_page:
                    continue
                actions = page["actions"]
                assert isinstance(actions, set)
                if trigger_action in actions:
                    continue
                if not actions:
                    if node_status.get((quest_id, target)) in ("COMPLETE", "FAILED"):
                        candidates.append(page_id)
                    continue
                action_routes = [routes.get((quest_id, target, row["npc_id"], action), [])
                                 for action in actions]
                if not all(action_routes):
                    continue
                if any(
                    any(next_target != target or "SHOW_QUEST_PAGE:5" in after_commit
                        for next_target, after_commit in route_options)
                    for route_options in action_routes):
                    candidates.append(page_id)
            stats[f"candidate_count_{len(candidates)}"] += 1
            if len(candidates) != 1:
                continue
            candidate_page_id = candidates[0]
            key = (quest_id, row["server_source_state"], target, row["npc_id"], trigger_action,
                   shown_page, candidate_page_id)
            if key in seen:
                continue
            seen.add(key)
            page = client_pages[(quest_id, candidate_page_id)]
            replacements.append(Replacement(
                quest_id=quest_id,
                path=quest_dir / f"{quest_id}.xml",
                source=row["server_source_state"],
                target=target,
                npc_id=row["npc_id"],
                action_id=trigger_action,
                old_page=pages_by_id[shown_page],
                new_page=pages_by_id[candidate_page_id],
                candidate_page_id=candidate_page_id,
                client_file=str(page["client_file"]),
                client_sha256=str(page["client_sha256"]),
                evidence=row["evidence_source"],
            ))
            stats["manifest"] += 1
    return sorted(replacements, key=lambda item: (item.quest_id, item.old_page, item.new_page,
                                                   item.npc_id, item.action_id, item.target)), stats


def write_manifest(path: Path, replacements: list[Replacement]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "source", "target", "npc_id", "action_id", "old_page",
                         "new_page", "candidate_page_id", "client_file", "client_sha256", "evidence"))
        for item in replacements:
            writer.writerow((item.quest_id, item.path.relative_to(ROOT).as_posix(), item.source,
                             item.target, item.npc_id, item.action_id, item.old_page, item.new_page,
                             item.candidate_page_id, item.client_file, item.client_sha256, item.evidence))


def action_token_matches(token: str, action_id: int, actions_by_name: dict[str, int]) -> bool:
    if token.isdigit():
        return int(token) == action_id
    if ".." in token:
        first, last = token.split("..", 1)
        first_id = int(first) if first.isdigit() else actions_by_name.get(first)
        last_id = int(last) if last.isdigit() else actions_by_name.get(last)
        return (first_id is not None and last_id is not None
                and first_id <= action_id <= last_id)
    return actions_by_name.get(token) == action_id


def action_matches(element: ET.Element, action_id: int, actions_by_name: dict[str, int]) -> bool:
    action = element.get("action")
    if action is not None and action_token_matches(action, action_id, actions_by_name):
        return True
    expression = element.get("actions", "").replace(",", " ")
    for token in expression.split():
        if action_token_matches(token, action_id, actions_by_name):
            return True
    return False


def transition_contains_event(block: ET.Element, item: Replacement,
        actions_by_name: dict[str, int]) -> bool:
    for event in block.findall("./event/*"):
        if event.tag == "dialog":
            if item.npc_id:
                if event.get("type") != "TALK_TO_NPC" or event.get("npc-id") != item.npc_id:
                    continue
            elif event.get("type") not in ("QUEST_ACTION", "QUEST_DIALOG"):
                continue
            if action_matches(event, item.action_id, actions_by_name):
                return True
        elif (event.tag == "quest-dialog" and not item.npc_id
                and action_matches(event, item.action_id, actions_by_name)):
            return True
    return False


def replace_transition(source: str, item: Replacement,
        actions_by_name: dict[str, int]) -> tuple[str, int]:
    matches: list[tuple[int, int, str]] = []
    for match in TRANSITION_PATTERN.finditer(source):
        raw = match.group(0)
        try:
            block = ET.fromstring(raw)
        except ET.ParseError:
            continue
        if block.get("source") != item.source or block.get("target") != item.target:
            continue
        if not transition_contains_event(block, item, actions_by_name):
            continue
        for response in block.findall("./after-commit/dialog"):
            if response.get("type") == "SHOW_QUEST_PAGE" and response.get("page") == item.old_page:
                matches.append((match.start(), match.end(), raw))
                break
    if len(matches) != 1:
        return source, 0
    start, end, raw = matches[0]
    updated = re.sub(r'(\bpage\s*=\s*")' + re.escape(item.old_page) + r'(")',
                     rf'\g<1>{item.new_page}\g<2>', raw, count=1)
    if updated == raw:
        return source, 0
    return source[:start] + updated + source[end:], 1


def replace_shorthand(source: str, item: Replacement) -> tuple[str, int]:
    changed = 0

    def replace(match: re.Match[str]) -> str:
        nonlocal changed
        attrs = attributes(match.group(0))
        route_type = attrs.get("type")
        if route_type not in ("NPC_START", "NPC_REPORT"):
            return match.group(0)
        if attrs.get("npc-id", "") != item.npc_id or attrs.get("source", "") != item.source:
            return match.group(0)
        if route_type == "NPC_REPORT" and attrs.get("target", "") != item.target:
            return match.group(0)
        if route_type == "NPC_START" and item.action_id == 31:
            attribute = "start-page"
        elif route_type == "NPC_REPORT":
            attribute = "page"
        else:
            return match.group(0)
        if attrs.get(attribute) != item.old_page:
            return match.group(0)
        updated = re.sub(rf'(\b{attribute}\s*=\s*")[^"]*(")',
                         rf'\g<1>{item.new_page}\g<2>', match.group(0), count=1)
        changed += 1
        return updated

    return OPENING_DIALOG_PATTERN.sub(replace, source), changed


def apply(replacements: list[Replacement], write: bool) -> tuple[int, list[str]]:
    _, actions_by_name, _, _ = load_maps(ROOT)
    grouped: dict[Path, list[Replacement]] = defaultdict(list)
    for item in replacements:
        grouped[item.path].append(item)
    changed = 0
    unresolved: list[str] = []
    for path, items in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        file_unresolved: list[str] = []
        for item in items:
            source, count = replace_transition(source, item, actions_by_name)
            if count == 0:
                source, count = replace_shorthand(source, item)
            if count != 1:
                value = (f"{item.quest_id}:{item.source}:{item.target}:{item.npc_id}:"
                         f"{item.action_id}:{item.old_page}->{item.new_page}")
                file_unresolved.append(value)
                unresolved.append(value)
                continue
            changed += count
        if file_unresolved:
            continue
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        ET.fromstring(source)
        if write:
            path.write_text(source, encoding="utf-8")
    return changed, unresolved


def main() -> int:
    args = parse_args()
    replacements, stats = collect(args)
    manifest = args.manifest if args.manifest.is_absolute() else args.root.resolve() / args.manifest
    write_manifest(manifest, replacements)
    print(f"candidates={len(replacements)} files={len({item.path for item in replacements})} "
          f"manifest={manifest} stats={dict(sorted(stats.items()))}")
    changed, unresolved = apply(replacements, args.write)
    print(f"changed={changed} unresolved={len(unresolved)} write={args.write}")
    for value in unresolved:
        print(f"UNRESOLVED {value}")
    return 1 if args.write and unresolved else 0


if __name__ == "__main__":
    raise SystemExit(main())
