#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import re
import subprocess
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
RETAIl_PREFIX = "src/main/resources/aion/definitions/compact/quests/scripts/"


@dataclass(frozen=True)
class Insertion:
    quest_id: int
    path: Path
    source: str
    target: str
    npc_id: int
    action_name: str
    items: tuple[tuple[int, int], ...]
    success_page: str
    failure_page: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Add CHECK_USER_HAS_QUEST_ITEM routes for retail item-collecting templates.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agents/summary/quest-load-fail/quest-order-audit-current.csv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agents/summary/quest-load-fail/generic-item-collecting-routes.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def read_page_symbols(root: Path) -> dict[int, str]:
    result: dict[int, str] = {}
    path = root / "docs/quest/client-dialog-mapping/client-html-pages.csv"
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            result[int(row["page_id"])] = row["page_constant"].removeprefix("HTML_PAGE_")
    return result


def read_quest_pages(root: Path) -> dict[int, set[int]]:
    result: dict[int, set[int]] = defaultdict(set)
    path = root / "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["source_variant"] == "active" and row["page_mapping"] == "exact":
                result[int(row["quest_id"])].add(int(row["page_id"]))
    return result


def read_item_collecting_templates() -> dict[int, dict[str, str]]:
    files = subprocess.check_output([
        "git", "ls-tree", "-r", "--name-only", "origin/history",
        RETAIl_PREFIX,
    ], cwd=ROOT, text=True).splitlines()
    result: dict[int, dict[str, str]] = {}
    for path in files:
        if not path.endswith(".xml"):
            continue
        payload = subprocess.check_output(["git", "show", f"origin/history:{path}"], cwd=ROOT)
        try:
            root = ET.fromstring(payload)
        except ET.ParseError:
            continue
        for element in root.iter("item_collecting"):
            result[int(element.get("id"))] = element.attrib
    return result


def integer_set(raw: str) -> set[int]:
    return {int(value) for value in raw.split() if value}


def page_symbol(page_id: int | None, fallback: int, symbols: dict[int, str]) -> str:
    resolved = fallback if page_id is None else page_id
    if resolved == 0:
        return ""
    return symbols[resolved]


def collect(args: argparse.Namespace) -> list[Insertion]:
    root = args.root.resolve()
    audit_path = args.audit if args.audit.is_absolute() else root / args.audit
    templates = read_item_collecting_templates()
    symbols = read_page_symbols(root)
    quest_pages = read_quest_pages(root)
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    with audit_path.open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))

    result: list[Insertion] = []
    seen: set[tuple[int, str, int]] = set()
    for row in rows:
        if (not row["unresolved_reason"].startswith("visible client action has no route")
                or row["trigger_action"] != "31"
                or row["client_visible_action"] not in {"39", "20002"}
                or not row["npc_id"]):
            continue
        quest_id = int(row["quest_id"])
        template = templates.get(quest_id)
        if template is None:
            continue
        npc_id = int(row["npc_id"])
        key = (quest_id, row["server_source_state"], npc_id)
        if key in seen:
            continue

        path = quest_dir / f"{quest_id}.xml"
        document = ET.parse(path).getroot()
        statuses = {node.get("label", ""): node.get("status", "")
                    for node in document.findall("./nodes/node")}
        rewards = [label for label, status in statuses.items() if status == "REWARD"]
        if statuses.get(row["server_source_state"]) != "START" or len(rewards) != 1:
            continue
        reward_node = rewards[0]
        owns_report = any(
            report.get("type") == "NPC_REPORT"
            and report.get("npc-id") == row["npc_id"]
            and report.get("source") == row["server_source_state"]
            and report.get("target") == reward_node
            for report in document.findall("./transitions/dialog")
        ) or any(
            report.get("npc-id") == row["npc_id"]
            and report.get("source") == row["server_source_state"]
            and report.get("target") == reward_node
            for report in document.findall("./transitions/npc-report")
        )
        if not owns_report:
            continue
        items = tuple(
            (int(item.get("id")), int(item.get("count")))
            for item in document.findall("./metadata/items/item")
        )
        if not items:
            continue
        if any(
            transition.get("source") == row["server_source_state"]
            and (event := transition.find("./event/dialog")) is not None
            and event.get("npc-id") == row["npc_id"]
            and event.get("action") in {"CHECK_USER_HAS_QUEST_ITEM", "CHECK_USER_HAS_QUEST_ITEM_SIMPLE"}
            for transition in document.findall("./transitions/transition")
        ):
            continue

        action_name = ("CHECK_USER_HAS_QUEST_ITEM_SIMPLE"
                       if row["client_visible_action"] == "20002"
                       else "CHECK_USER_HAS_QUEST_ITEM")
        success_page = page_symbol(
            int(template["check_ok_dialog_id"]) if "check_ok_dialog_id" in template else None,
            5, symbols)
        success_page_id = next(
            (page_id for page_id, symbol in symbols.items() if symbol == success_page), 0)
        if success_page_id not in quest_pages[quest_id]:
            continue
        failure_page_id = int(template.get("check_fail_dialog_id", "0") or 0) or 2716
        failure_page = page_symbol(
            failure_page_id,
            2716, symbols)
        if failure_page_id not in quest_pages[quest_id]:
            failure_page = "CLOSE"
        result.append(Insertion(quest_id, path, row["server_source_state"], reward_node, npc_id,
                                action_name, items, success_page, failure_page))
        seen.add(key)
    return sorted(result, key=lambda item: (item.quest_id, item.source, item.npc_id))


def write_manifest(path: Path, rows: list[Insertion]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "source", "target", "npc_id", "action", "items",
                         "success_page", "failure_page"))
        for row in rows:
            writer.writerow((row.quest_id, row.path.relative_to(ROOT).as_posix(), row.source,
                             row.target, row.npc_id, row.action_name,
                             " ".join(f"{item}:{count}" for item, count in row.items),
                             row.success_page, row.failure_page))


def render(row: Insertion) -> str:
    conditions = "".join(
        f'        <has-item item-id="{item_id}" count="{count}"/>\n'
        for item_id, count in row.items)
    removals = "".join(
        f'        <remove-item item-id="{item_id}" count="{count}"/>\n'
        for item_id, count in row.items)
    success_response = (
        '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        + (f'        <dialog type="SHOW_QUEST_PAGE" page="{row.success_page}"/>\n'
           if row.success_page else '        <close-dialog/>\n')
    )
    failure_response = (f'        <dialog type="SHOW_QUEST_PAGE" page="{row.failure_page}"/>\n'
                        if row.failure_page and row.failure_page != "CLOSE"
                        else '        <close-dialog/>\n')
    return (
        f'    <transition source="{row.source}" target="{row.target}" priority="0">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{row.npc_id}" '
        f'action="{row.action_name}"/>\n'
        '      </event>\n'
        '      <conditions>\n'
        f'{conditions}'
        '      </conditions>\n'
        '      <actions>\n'
        f'{removals}'
        '      </actions>\n'
        '      <after-commit>\n'
        f'{success_response}'
        '      </after-commit>\n'
        '    </transition>\n'
        f'    <transition source="{row.source}" target="{row.source}" priority="1">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{row.npc_id}" '
        f'action="{row.action_name}"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        f'{failure_response}'
        '      </after-commit>\n'
        '    </transition>\n'
    )


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
        updated = source[:closing] + "".join(render(item) for item in items) + source[closing:]
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        ET.fromstring(updated)
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
    print(f"routes={len(rows)} changed={changed} files={len({row.path for row in rows})} "
          f"write={args.write} manifest={manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
