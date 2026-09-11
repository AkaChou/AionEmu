#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import re
import subprocess
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
START_PATTERN = re.compile(r'<dialog\b[^>]*type="NPC_START"[^>]*?/>')


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Remove fabricated NPC_START owners contradicted by legacy addOnQuestStart.")
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--audit", type=Path,
                        default=Path(".agent/summary/quest-load-fail/quest-order-audit-control-routes.csv"))
    parser.add_argument("--manifest", type=Path,
                        default=Path(".agent/summary/quest-load-fail/legacy-start-owner-cleanup.csv"))
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def legacy_handlers() -> dict[str, str]:
    output = subprocess.check_output(["git", "ls-tree", "-r", "--name-only", "origin/history"], text=True)
    result: dict[str, str] = {}
    for path in output.splitlines():
        if "/quest/handlers/" not in path or not path.endswith(".java"):
            continue
        match = re.search(r"(?:^|_)(\d+)(?:[A-Z_]|$)", path.rsplit("/", 1)[-1])
        if match:
            result.setdefault(match.group(1), path)
    return result


def collect(args: argparse.Namespace) -> list[tuple[int, Path, str, str, str]]:
    root = args.root.resolve()
    audit_path = args.audit if args.audit.is_absolute() else root / args.audit
    failing: set[str] = set()
    with audit_path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            if row["unresolved_reason"].startswith("visible client action has no route"):
                failing.add(row["quest_id"])

    handlers = legacy_handlers()
    quest_dir = root / "src/main/resources/aion/data/static_data/quest_definition/quests"
    result: list[tuple[int, Path, str, str, str]] = []
    for quest_id in sorted(failing, key=int):
        handler_path = handlers.get(quest_id)
        if handler_path is None:
            continue
        handler = subprocess.check_output(
            ["git", "show", f"origin/history:{handler_path}"], text=True)
        starts = set(re.findall(r"registerQuestNpc\((\d+)\)\.addOnQuestStart", handler))
        if not starts:
            continue
        path = quest_dir / f"{quest_id}.xml"
        source = path.read_text(encoding="utf-8")
        xml_starts: dict[str, str] = {}
        for block in START_PATTERN.findall(source):
            npc_match = re.search(r'npc-id="(\d+)"', block)
            if npc_match:
                xml_starts[npc_match.group(1)] = block
        for npc_id in sorted(set(xml_starts) - starts, key=int):
            if not re.search(rf"(?:\bcase\s+{npc_id}\s*:|\btargetId\s*==\s*{npc_id}\b)", handler):
                continue
            result.append((int(quest_id), path, npc_id, " ".join(sorted(starts, key=int)), handler_path))
    return result


def write_manifest(path: Path, rows: list[tuple[int, Path, str, str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(("quest_id", "file", "removed_npc_id", "registered_start_npcs", "legacy_handler"))
        for quest_id, file_path, npc_id, starts, handler in rows:
            writer.writerow((quest_id, file_path.relative_to(ROOT).as_posix(), npc_id, starts, handler))


def apply(rows: list[tuple[int, Path, str, str, str]], write: bool) -> int:
    grouped: dict[Path, list[str]] = defaultdict(list)
    for _, path, npc_id, _, _ in rows:
        grouped[path].append(npc_id)
    changed = 0
    for path, npc_ids in grouped.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        for npc_id in npc_ids:
            matches = [block for block in START_PATTERN.findall(source)
                       if re.search(rf'npc-id="{re.escape(npc_id)}"', block)]
            if len(matches) != 1:
                raise RuntimeError(f"NPC_START {npc_id} is not unique in {path}")
            source = source.replace(matches[0], "", 1)
            changed += 1
        ET.fromstring(source)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change detected before write: {path}")
        if write:
            path.write_text(source, encoding="utf-8")
    return changed


def main() -> int:
    args = parse_args()
    rows = collect(args)
    manifest = args.manifest if args.manifest.is_absolute() else args.root.resolve() / args.manifest
    write_manifest(manifest, rows)
    print(f"remove_blocks={len(rows)} files={len({row[1] for row in rows})} "
          f"quests={len({row[0] for row in rows})} manifest={manifest}")
    if args.write:
        print(f"changed={apply(rows, True)}")
    else:
        apply(rows, False)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
