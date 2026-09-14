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
ITEM_REPORT_PATTERN = re.compile(r"<npc-item-report\b[^>]*/>")


@dataclass(frozen=True)
class Edit:
    quest_id: int
    path: Path
    kind: str
    source: str
    target: str
    npc_id: int
    items: tuple[tuple[int, int], ...]
    block: str
    success_page: str
    failure_page: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Clone item-report aliases and add simple 20002 turn-in routes.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agents/summary/quest-load-fail/quest-order-audit-current.csv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agents/summary/quest-load-fail/simple-item-check-aliases.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def attributes(raw: str) -> dict[str, str]:
    return dict(re.findall(r'([A-Za-z_][\w-]*)="([^"]*)"', raw))


CLIENT_PAGES: dict[int, set[int]] = defaultdict(set)


def load_client_pages(root: Path) -> None:
    with (root / "docs/quest/client-dialog-mapping/quest-dialog-pages.csv").open(
            encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            CLIENT_PAGES[int(row["quest_id"])].add(int(row["page_id"]))


def collect(args: argparse.Namespace) -> list[Edit]:
    root = args.root.resolve()
    load_client_pages(root)
    audit_path = args.audit if args.audit.is_absolute() else root / args.audit
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    with audit_path.open(encoding="utf-8-sig", newline="") as stream:
        audit = list(csv.DictReader(stream))

    result: list[Edit] = []
    seen: set[tuple[int, str, int]] = set()
    for row in audit:
        if (not row["unresolved_reason"].startswith("visible client action has no route")
                or row["client_visible_action"] != "20002"
                or not row["npc_id"]):
            continue
        quest_id = int(row["quest_id"])
        source = row["server_source_state"]
        npc_id = int(row["npc_id"])
        key = (quest_id, source, npc_id)
        if key in seen:
            continue
        path = quest_dir / f"{quest_id}.xml"
        source_text = path.read_text(encoding="utf-8")
        document = ET.parse(path).getroot()
        statuses = {node.get("label", ""): node.get("status", "")
                    for node in document.findall("./nodes/node")}
        rewards = [label for label, status in statuses.items() if status == "REWARD"]
        if statuses.get(source) != "START" or len(rewards) != 1:
            continue
        has_existing_route = any(
            attrs.get("source") == source and attrs.get("npc-id") == row["npc_id"]
            for raw in ITEM_REPORT_PATTERN.findall(source_text)
            if (attrs := attributes(raw))
        )
        if not has_existing_route:
            for raw in TRANSITION_PATTERN.findall(source_text):
                block = ET.fromstring(raw)
                if block.get("source") != source:
                    continue
                event = block.find("./event/dialog")
                if (event is not None and event.get("npc-id") == row["npc_id"]
                        and event.get("action") in (
                            "CHECK_USER_HAS_QUEST_ITEM",
                            "CHECK_USER_HAS_QUEST_ITEM_SIMPLE")):
                    has_existing_route = True
                    break
        if has_existing_route:
            continue

        item_report_templates: set[str] = set()
        for raw in ITEM_REPORT_PATTERN.findall(source_text):
            attrs = attributes(raw)
            if attrs.get("source") != source or attrs.get("target") != rewards[0]:
                continue
            if attrs.get("npc-id") == row["npc_id"]:
                continue
            item_report_templates.add(re.sub(r'(\bnpc-id=")[^"]*(")', r"\g<1>*\g<2>", raw, count=1))
        if len(item_report_templates) == 1:
            block = item_report_templates.pop().replace("*", row["npc_id"], 1)
            result.append(Edit(quest_id, path, "CLONE_ITEM_REPORT", source, rewards[0], npc_id,
                               (), block, "", ""))
            seen.add(key)
            continue

        transition_templates: dict[str, str] = {}
        transition_npcs: set[str] = set()
        for raw in TRANSITION_PATTERN.findall(source_text):
            block = ET.fromstring(raw)
            if block.get("source") != source:
                continue
            event = block.find("./event/dialog")
            if event is None or event.get("action") != "CHECK_USER_HAS_QUEST_ITEM":
                continue
            if event.get("npc-id") == row["npc_id"]:
                continue
            transition_npcs.add(event.get("npc-id", ""))
            normalized = re.sub(r'(\bnpc-id=")[^"]*(")', r"\g<1>*\g<2>", raw, count=1)
            transition_templates[normalized] = raw
        if len(transition_npcs) == 1 and len(transition_templates) in (1, 2):
            grouped = "\n".join(
                template.replace("*", row["npc_id"], 1)
                for template in transition_templates.values()
            )
            result.append(Edit(quest_id, path, "CLONE_ITEM_CHECK", source, rewards[0], npc_id,
                               (), grouped, "", ""))
            seen.add(key)
            continue

        items = tuple(
            (int(item.get("id")), int(item.get("count")))
            for item in document.findall("./metadata/items/item")
        )
        if not items:
            continue
        success_page = ("CHECK_USER_ITEM_OK" if 10000 in CLIENT_PAGES[quest_id]
                        else "SHOW_SELECT_QUEST_REWARD_WINDOW1")
        if 10001 in CLIENT_PAGES[quest_id]:
            failure_page = "CHECK_USER_ITEM_FAIL"
        elif 2716 in CLIENT_PAGES[quest_id]:
            failure_page = "SELECT6"
        else:
            failure_page = ""
        result.append(Edit(quest_id, path, "DIRECT_SIMPLE", source, rewards[0], npc_id,
                           items, "", success_page, failure_page))
        seen.add(key)
    return sorted(result, key=lambda item: (item.quest_id, item.npc_id))


def write_manifest(path: Path, rows: list[Edit]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "kind", "source", "target", "npc_id", "items",
                         "success_page", "failure_page"))
        for row in rows:
            writer.writerow((row.quest_id, row.path.relative_to(ROOT).as_posix(), row.kind,
                             row.source, row.target, row.npc_id,
                             " ".join(f"{item}:{count}" for item, count in row.items),
                             row.success_page, row.failure_page))


def render_direct(row: Edit) -> str:
    conditions = "".join(
        f'        <has-item item-id="{item_id}" count="{count}"/>\n'
        for item_id, count in row.items)
    removals = "".join(
        f'        <remove-item item-id="{item_id}" count="{count}"/>\n'
        for item_id, count in row.items)
    success_response = (
        '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        f'        <dialog type="SHOW_QUEST_PAGE" page="{row.success_page}"/>\n')
    failure_response = (
        f'        <dialog type="SHOW_QUEST_PAGE" page="{row.failure_page}"/>\n'
        if row.failure_page else '        <close-dialog/>\n')
    return (
        f'    <transition source="{row.source}" target="{row.target}" priority="0">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{row.npc_id}" '
        'action="CHECK_USER_HAS_QUEST_ITEM_SIMPLE"/>\n'
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
        'action="CHECK_USER_HAS_QUEST_ITEM_SIMPLE"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        f'{failure_response}'
        '      </after-commit>\n'
        '    </transition>\n'
    )


def apply(rows: list[Edit], write: bool) -> int:
    grouped: dict[Path, list[Edit]] = defaultdict(list)
    for row in rows:
        grouped[row.path].append(row)
    for path, items in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        closing = source.rfind("</transitions>")
        if closing < 0:
            raise RuntimeError(f"missing transitions closing tag: {path}")
        additions = []
        for item in items:
            additions.append(render_direct(item) if item.kind == "DIRECT_SIMPLE"
                             else item.block + "\n")
        updated = source[:closing] + "".join(additions) + source[closing:]
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
