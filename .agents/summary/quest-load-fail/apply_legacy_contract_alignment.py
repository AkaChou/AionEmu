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
START_TAG = re.compile(r'[ \t]*<dialog\b(?=[^>]*\btype="NPC_START")[^>]*?/>[ \t]*\n?')
REPORT_TAG = re.compile(r'[ \t]*<dialog\b(?=[^>]*\btype="NPC_REPORT")[^>]*?/>[ \t]*\n?')
COMPLETE_BLOCK = re.compile(
    r'[ \t]*<npc-complete\b(?=[^>]*\bnpc-id="(?P<npc>\d+)")[^>]*>.*?</npc-complete>[ \t]*\n?',
    re.DOTALL,
)


@dataclass(frozen=True)
class Edit:
    quest_id: int
    path: Path
    kind: str
    npc_id: int
    old_value: str
    new_value: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Align NPC_START/NPC_REPORT owners and pages with full legacy contracts.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--baseline", type=Path,
                        default=Path("src/test/resources/quest/quest-client-contract-baseline.tsv"))
    parser.add_argument("--contracts", type=Path,
                        default=Path("docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agents/summary/quest-load-fail/legacy-contract-alignment.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def failing_quests(path: Path) -> set[int]:
    result: set[int] = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("BUTTON_WITHOUT_ROUTE\t"):
            result.add(int(line.split("\t", 2)[1]))
    return result


def read_contracts(path: Path) -> dict[int, dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return {int(row["quest_id"]): row for row in csv.DictReader(stream)
                if row["contract_scope"] == "FULL"}


def tag_npc(tag: str) -> int:
    match = re.search(r'\bnpc-id="(\d+)"', tag)
    if match is None:
        raise RuntimeError(f"dialog macro has no npc-id: {tag}")
    return int(match.group(1))


def collect(args: argparse.Namespace) -> list[Edit]:
    root = args.root.resolve()
    baseline = args.baseline if args.baseline.is_absolute() else root / args.baseline
    contracts_path = args.contracts if args.contracts.is_absolute() else root / args.contracts
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    contracts = read_contracts(contracts_path)
    result: list[Edit] = []
    for quest_id in sorted(failing_quests(baseline)):
        contract = contracts.get(quest_id)
        if contract is None:
            continue
        starts = {int(value) for value in contract["start_npc_ids"].split() if value != "0"}
        ends = {int(value) for value in contract["end_npc_ids"].split() if value != "0"}
        path = quest_dir / f"{quest_id}.xml"
        source = path.read_text(encoding="utf-8")

        if starts:
            for match in list(START_TAG.finditer(source)):
                tag = match.group(0).strip()
                npc_id = tag_npc(tag)
                if npc_id not in starts:
                    result.append(Edit(quest_id, path, "REMOVE_START", npc_id, tag, ""))
                    continue
                old_page = re.search(r'\bstart-page="([^"]*)"', tag)
                if old_page is not None and old_page.group(1) != contract["start_page"]:
                    result.append(Edit(quest_id, path, "START_PAGE", npc_id,
                                       old_page.group(1), contract["start_page"]))

        if ends:
            for match in list(REPORT_TAG.finditer(source)):
                tag = match.group(0).strip()
                npc_id = tag_npc(tag)
                if npc_id not in ends:
                    result.append(Edit(quest_id, path, "REMOVE_REPORT", npc_id, tag, ""))
                    continue
                old_page = re.search(r'\bpage="([^"]*)"', tag)
                if old_page is not None and contract["report_page"] and old_page.group(1) != contract["report_page"]:
                    result.append(Edit(quest_id, path, "REPORT_PAGE", npc_id,
                                       old_page.group(1), contract["report_page"]))

            for match in list(COMPLETE_BLOCK.finditer(source)):
                npc_id = int(match.group("npc"))
                if npc_id not in ends:
                    result.append(Edit(quest_id, path, "REMOVE_COMPLETE", npc_id,
                                       match.group(0).strip(), ""))
    return result


def write_manifest(path: Path, edits: list[Edit]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "kind", "npc_id", "old_value", "new_value"))
        for edit in edits:
            writer.writerow((edit.quest_id, edit.path.relative_to(ROOT).as_posix(), edit.kind,
                             edit.npc_id, edit.old_value, edit.new_value))


def apply(edits: list[Edit], write: bool) -> int:
    grouped: dict[Path, list[Edit]] = defaultdict(list)
    for edit in edits:
        grouped[edit.path].append(edit)
    changed = 0
    for path, items in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        for edit in items:
            if edit.kind in {"REMOVE_START", "REMOVE_REPORT", "REMOVE_COMPLETE"}:
                pattern = re.compile(
                    r'[ \t]*' + re.escape(edit.old_value) + r'[ \t]*\n?')
                source, count = pattern.subn("", source, count=1)
                if count != 1:
                    raise RuntimeError(
                        f"cannot remove unique {edit.kind} NPC {edit.npc_id} in {path}")
            else:
                dialog_type = "NPC_START" if edit.kind == "START_PAGE" else "NPC_REPORT"
                attribute = "start-page" if edit.kind == "START_PAGE" else "page"
                pattern = re.compile(
                    rf'(<dialog\b(?=[^>]*\btype="{dialog_type}")'
                    rf'(?=[^>]*\bnpc-id="{edit.npc_id}")'
                    rf'[^>]*\b{attribute}="){re.escape(edit.old_value)}(")')
                replacement = rf'\g<1>{edit.new_value}\g<2>'
                source, count = pattern.subn(replacement, source, count=1)
                if count != 1:
                    raise RuntimeError(
                        f"cannot update {edit.kind} NPC {edit.npc_id} in {path}")
            changed += 1
        ET.fromstring(source)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        if write:
            path.write_text(source, encoding="utf-8")
    return changed


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    edits = collect(args)
    manifest = args.manifest if args.manifest.is_absolute() else root / args.manifest
    write_manifest(manifest, edits)
    changed = apply(edits, args.write)
    kinds: dict[str, int] = defaultdict(int)
    for edit in edits:
        kinds[edit.kind] += 1
    print(f"edits={len(edits)} changed={changed} files={len({edit.path for edit in edits})} "
          f"kinds={dict(kinds)} write={args.write} manifest={manifest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
