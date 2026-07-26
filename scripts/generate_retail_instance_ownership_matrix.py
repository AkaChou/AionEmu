#!/usr/bin/env python3
"""Project the hand-audited coverage ledger into batchable ownership signatures."""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path


COVERAGE = Path("src/main/resources/aion/definitions/compact/instance/coverage.xml")
REPORT = Path("docs/RETAIL_INSTANCE_OWNERSHIP_MATRIX.json")
DIMENSIONS = ("entry", "spawn", "ai", "path", "door", "stage", "score", "reward", "exit", "recovery")
DISABLED_WORLD = 300260000


def handler_owners(root: Path, worlds: set[int]) -> dict[int, str]:
    result = {}
    directory = root / "src/main/java/com/aionemu/gameserver/instance/handlers/scripts"
    for path in sorted(directory.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        for annotation in re.findall(r"@InstanceID\s*\((.*?)\)", text, re.DOTALL):
            for value in re.findall(r"\b\d{9}\b", annotation):
                world_id = int(value)
                if world_id not in worlds:
                    continue
                owner = str(path.relative_to(root))
                if world_id in result:
                    raise ValueError(f"duplicate instance handlers for {world_id}: {result[world_id]}, {owner}")
                result[world_id] = owner
    return result


def dimension_owners(value: str, world_id: int) -> dict[str, str]:
    assignments = value.split(",")
    if len(assignments) != len(DIMENSIONS):
        raise ValueError(f"invalid dimension ownership count for {world_id}")
    result = {}
    for dimension, assignment in zip(DIMENSIONS, assignments, strict=True):
        name, separator, owner = assignment.partition(":")
        if not separator or name != dimension or not owner:
            raise ValueError(f"invalid dimension ownership for {world_id}: {assignment}")
        result[dimension] = owner
    return result


def build(root: Path) -> dict[str, object]:
    worlds = []
    for node in ET.parse(root / COVERAGE).getroot().findall("world"):
        if int(node.get("id", "0")) == DISABLED_WORLD:
            continue
        world_id = int(node.get("id", "0"))
        worlds.append({
            "world_id": world_id,
            "local_name": node.get("local_name", ""),
            "retail_name": node.get("retail_name", ""),
            "behavior": node.get("behavior", ""),
            "behavior_source": node.get("behavior_source", ""),
            "dimensions": dimension_owners(node.get("dimension_owners", ""), world_id),
        })
    if len(worlds) != 139 or len({world["world_id"] for world in worlds}) != len(worlds):
        raise ValueError(f"expected 139 unique production worlds, found {len(worlds)}")

    handlers = handler_owners(root, {int(world["world_id"]) for world in worlds})
    owner_counts = {dimension: Counter() for dimension in DIMENSIONS}
    grouped: dict[str, list[dict[str, object]]] = defaultdict(list)
    signatures = {}
    for world in worlds:
        world_id = int(world["world_id"])
        world["handler"] = handlers.get(world_id, "")
        handler_dimensions = [dimension for dimension in DIMENSIONS
                              if world["dimensions"][dimension] == "HANDLER"]
        world["handler_dimensions"] = handler_dimensions
        for dimension in DIMENSIONS:
            owner_counts[dimension][world["dimensions"][dimension]] += 1
        signature = {dimension: world["dimensions"][dimension] for dimension in DIMENSIONS}
        key = json.dumps(signature, sort_keys=True, separators=(",", ":"))
        signatures[key] = signature
        grouped[key].append(world)

    batches = []
    for index, key in enumerate(sorted(grouped), 1):
        batch_id = f"B{index:03d}"
        batch_worlds = grouped[key]
        for world in batch_worlds:
            world["batch_id"] = batch_id
        batches.append({
            "id": batch_id,
            "count": len(batch_worlds),
            "dimensions": signatures[key],
            "world_ids": [world["world_id"] for world in batch_worlds],
            "handlers": sorted({str(world["handler"]) for world in batch_worlds if world["handler"]}),
        })

    worlds.sort(key=lambda world: int(world["world_id"]))
    handler_owned = {dimension: owner_counts[dimension]["HANDLER"] for dimension in DIMENSIONS}
    summary = {
        "production_worlds": len(worlds),
        "dimensions": len(DIMENSIONS),
        "batches": len(batches),
        "registered_handler_worlds": len(handlers),
        "behavior_handler_worlds": sum(world["behavior"] == "HANDLER" for world in worlds),
        "worlds_with_handler_dimensions": sum(bool(world["handler_dimensions"]) for world in worlds),
        "handler_dimensions_total": sum(handler_owned.values()),
        "handler_owned_by_dimension": handler_owned,
        "owners_by_dimension": {
            dimension: dict(sorted(owner_counts[dimension].items())) for dimension in DIMENSIONS
        },
    }
    return {
        "version": 2,
        "provenance": {
            "kind": "AUDIT_PROJECTION",
            "input": str(COVERAGE),
            "authoritative_retail_evidence": False,
        },
        "summary": summary,
        "batches": batches,
        "worlds": worlds,
    }


def render(report: dict[str, object]) -> str:
    return json.dumps(report, ensure_ascii=False, indent=2) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    output = args.output or root / REPORT
    content = render(build(root))
    if args.check:
        if not output.is_file() or output.read_text(encoding="utf-8") != content:
            raise SystemExit(f"stale instance ownership matrix: {output}")
        print(f"instance ownership matrix is current: {output}")
        return
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(content, encoding="utf-8")
    print(json.dumps(json.loads(content)["summary"], ensure_ascii=False, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ET.ParseError, ValueError) as error:
        print(error, file=sys.stderr)
        raise SystemExit(1) from error
