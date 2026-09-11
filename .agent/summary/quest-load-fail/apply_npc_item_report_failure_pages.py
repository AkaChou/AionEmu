#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
BLOCK_PATTERN = re.compile(r"<npc-item-report\b[^>]*?/>")


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Select a client-owned failure response for npc-item-report blocks.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agent/summary/quest-load-fail/npc-item-report-failure-pages.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def read_pages(root: Path) -> set[tuple[int, int]]:
    result: set[tuple[int, int]] = set()
    path = root / "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["source_variant"] == "active":
                result.add((int(row["quest_id"]), int(row["page_id"])))
    return result


def collect(root: Path) -> list[tuple[int, Path, str, str]]:
    pages = read_pages(root)
    result: list[tuple[int, Path, str, str]] = []
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    for path in sorted(quest_dir.glob("*.xml"), key=lambda candidate: int(candidate.stem)):
        quest_id = int(path.stem)
        if (quest_id, 2716) in pages:
            continue
        source = path.read_text(encoding="utf-8")
        for match in BLOCK_PATTERN.finditer(source):
            block = match.group(0)
            if "failure-page=" in block:
                continue
            failure_page = "CLOSE"
            result.append((quest_id, path, block, failure_page))
    return result


def write_manifest(path: Path, rows: list[tuple[int, Path, str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "failure_page", "block"))
        for quest_id, file_path, block, failure_page in rows:
            writer.writerow((quest_id, file_path.relative_to(ROOT).as_posix(), failure_page, block))


def apply(root: Path, rows: list[tuple[int, Path, str, str]]) -> int:
    grouped: dict[Path, list[tuple[str, str]]] = {}
    for _, path, block, failure_page in rows:
        grouped.setdefault(path, []).append((block, failure_page))
    changed = 0
    for path, replacements in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        for block, failure_page in replacements:
            updated_block = block[:-2].rstrip() + f' failure-page="{failure_page}"/>'
            if source.count(block) != 1:
                raise RuntimeError(f"block is not unique in {path}: {block}")
            source = source.replace(block, updated_block, 1)
            changed += 1
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        ET.fromstring(source)
        path.write_text(source, encoding="utf-8")
    return changed


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    rows = collect(root)
    manifest = args.manifest if args.manifest.is_absolute() else root / args.manifest
    write_manifest(manifest, rows)
    print(f"blocks={len(rows)} files={len({row[1] for row in rows})} "
          f"kinds={dict(Counter(row[3] for row in rows))} manifest={manifest}")
    if args.write:
        print(f"changed={apply(root, rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
