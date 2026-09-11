#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / ".agent/summary/quest"))
from quest_dialog_symbols import attributes, load_maps  # noqa: E402


OPENING_DIALOG = re.compile(r"<dialog\b[^>]*>")


@dataclass(frozen=True)
class ClientPage:
    page_id: int
    page_order: int
    source_file: str
    source_sha256: str
    actions: frozenset[int]


@dataclass(frozen=True)
class Replacement:
    quest_id: int
    path: Path
    route_type: str
    npc_id: str
    source: str
    target: str
    old_page: str
    new_page: str
    candidate_page_id: int
    matching_actions: str
    source_file: str
    source_sha256: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Replace missing NPC_START/NPC_REPORT task pages only when one client page is unambiguous.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agent/summary/quest-load-fail/client-page-candidates.csv"))
    parser.add_argument("--write", action="store_true", help="Apply the manifest after validating every target.")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def read_client_pages(root: Path) -> dict[int, dict[int, ClientPage]]:
    mapping = root / "docs/quest/client-dialog-mapping"
    rows: dict[tuple[int, int], dict[str, str]] = {}
    with (mapping / "quest-dialog-pages.csv").open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["source_variant"] != "active" or row["page_mapping"] != "exact":
                continue
            rows[(int(row["quest_id"]), int(row["page_id"]))] = row

    actions: dict[tuple[int, int], set[int]] = {key: set() for key in rows}
    with (mapping / "quest-dialog-action-details.csv").open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["source_variant"] != "active" or row["action_mapping"] != "exact":
                continue
            key = (int(row["quest_id"]), int(row["page_id"]))
            if key in actions and row["action_id"]:
                actions[key].add(int(row["action_id"]))

    result: dict[int, dict[int, ClientPage]] = {}
    for (quest_id, page_id), row in rows.items():
        result.setdefault(quest_id, {})[page_id] = ClientPage(
            page_id=page_id,
            page_order=int(row["page_order"]),
            source_file=row["source_file"],
            source_sha256=row["source_sha256"],
            actions=frozenset(actions[(quest_id, page_id)]),
        )
    return result


def page_id(raw: str, pages_by_name: dict[str, int]) -> int:
    if raw in pages_by_name:
        return pages_by_name[raw]
    return int(raw)


def collect_replacements(root: Path, pages_by_name: dict[str, int],
        client_pages: dict[int, dict[int, ClientPage]]) -> list[Replacement]:
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    replacements: list[Replacement] = []
    for path in sorted(quest_dir.glob("*.xml"), key=lambda candidate: int(candidate.stem)):
        quest_id = int(path.stem)
        available = client_pages.get(quest_id, {})
        if not available:
            continue
        document = ET.parse(path).getroot()
        for element in document.findall("./transitions/dialog"):
            route_type = element.get("type")
            if route_type not in ("NPC_START", "NPC_REPORT"):
                continue
            old_page = element.get("start-page") if route_type == "NPC_START" else element.get("page")
            if old_page is None:
                continue
            old_page_id = page_id(old_page, pages_by_name)
            if old_page_id in available:
                continue

            selected: ClientPage | None = None
            matching_actions: set[int] = set()
            if route_type == "NPC_START":
                candidates = [available[item_id] for item_id in (4, 4762) if item_id in available]
                if len(candidates) == 1:
                    selected = candidates[0]
            else:
                candidates = []
                for item_id in (2375, 10002):
                    page = available.get(item_id)
                    if page is None:
                        continue
                    matches = page.actions.intersection({1009})
                    if matches:
                        candidates.append((page, matches))
                if len(candidates) == 1:
                    selected, matching_actions = candidates[0]
            if selected is None:
                continue
            new_page = next(name for name, item_id in pages_by_name.items() if item_id == selected.page_id)
            replacements.append(Replacement(
                quest_id=quest_id,
                path=path,
                route_type=route_type,
                npc_id=element.get("npc-id", ""),
                source=element.get("source", ""),
                target=element.get("target", ""),
                old_page=old_page,
                new_page=new_page,
                candidate_page_id=selected.page_id,
                matching_actions=" ".join(str(action) for action in sorted(matching_actions)),
                source_file=selected.source_file,
                source_sha256=selected.source_sha256,
            ))
    return replacements


def write_manifest(path: Path, replacements: list[Replacement]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "route_type", "npc_id", "source", "target", "old_page",
                         "new_page", "candidate_page_id", "matching_actions", "client_source_file",
                         "client_source_sha256"))
        for item in replacements:
            writer.writerow((item.quest_id, item.path.relative_to(ROOT).as_posix(), item.route_type,
                             item.npc_id, item.source, item.target, item.old_page, item.new_page,
                             item.candidate_page_id, item.matching_actions, item.source_file,
                             item.source_sha256))


def apply_replacements(replacements: list[Replacement]) -> int:
    grouped: dict[Path, list[Replacement]] = {}
    for replacement in replacements:
        grouped.setdefault(replacement.path, []).append(replacement)
    changed = 0
    for path, items in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        remaining = list(items)

        def replace(match: re.Match[str]) -> str:
            nonlocal changed
            attrs = attributes(match.group(0))
            for index, item in enumerate(remaining):
                if (attrs.get("type") != item.route_type
                        or attrs.get("npc-id") != item.npc_id
                        or attrs.get("source") != item.source
                        or attrs.get("target") != item.target):
                    continue
                attribute = "start-page" if item.route_type == "NPC_START" else "page"
                if attrs.get(attribute) != item.old_page:
                    continue
                updated = re.sub(
                    rf'(\b{re.escape(attribute)}\s*=\s*")[^"]*(")',
                    rf'\g<1>{item.new_page}\g<2>', match.group(0), count=1)
                if updated == match.group(0):
                    raise RuntimeError(f"failed to replace {attribute} in {path}")
                remaining.pop(index)
                changed += 1
                return updated
            return match.group(0)

        updated = OPENING_DIALOG.sub(replace, source)
        if remaining:
            raise RuntimeError(f"could not locate {len(remaining)} candidate routes in {path}")
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        ET.fromstring(updated)
        path.write_text(updated, encoding="utf-8")
    return changed


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    _, _, _, pages_by_name = load_maps(root)
    clients = read_client_pages(root)
    replacements = collect_replacements(root, pages_by_name, clients)
    manifest = args.manifest if args.manifest.is_absolute() else root / args.manifest
    write_manifest(manifest, replacements)
    print(f"candidates={len(replacements)} manifest={manifest}")
    if args.write:
        print(f"changed_routes={apply_replacements(replacements)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
