#!/usr/bin/env python3
"""Synchronize proven retail static-door state and control IDs without reformatting XML."""

import argparse
import re
import xml.etree.ElementTree as ET
from pathlib import Path

from generate_retail_instance_door_matrix import (
    STATIC_DOORS,
    production_worlds,
    retail_static_doors,
)


WORLD_TAG = re.compile(r'<world\b[^>]*\bworld="(\d+)"')
DOOR_ID = re.compile(r'\bdoorid="(\d+)"')
STATE = re.compile(r"\s+state=(?:\"[^\"]*\"|'[^']*')")
RETAIL_ID = re.compile(r"\s+retailid=(?:\"[^\"]*\"|'[^']*')")
START_DOOR = re.compile(r"<staticdoor(?:\s|>)")


def state_value(value: str | None) -> int:
    if not value:
        return 0
    return int(value, 16) if value.casefold().startswith("0x") else int(value)


def retail_state(door: dict[str, object], current: int) -> int:
    return ((1 if door["opened"] else 0)
            | (2 if door["clickable"] else 0)
            | (4 if door["closeable"] else 0)
            | (current & 8))


def same_point(node: ET.Element, door: dict[str, object]) -> bool:
    return all(node.get(key) not in {None, ""}
               and abs(float(node.get(key, "0")) - float(door[key])) <= 0.01 for key in ("x", "y", "z"))


def uncommented_lines(lines: list[str]) -> list[str]:
    result = []
    in_comment = False
    for line in lines:
        active, position = "", 0
        while position < len(line):
            if in_comment:
                end = line.find("-->", position)
                if end < 0:
                    break
                position = end + 3
                in_comment = False
            else:
                start = line.find("<!--", position)
                if start < 0:
                    active += line[position:]
                    break
                active += line[position:start]
                position = start + 4
                in_comment = True
        result.append(active)
    if in_comment:
        raise ValueError("unterminated XML comment")
    return result


def synchronized_content(root: Path, worlds: set[int]) -> tuple[str, int]:
    source = retail_static_doors(root, worlds)
    target = root / STATIC_DOORS
    text = target.read_text(encoding="utf-8")
    document = ET.fromstring(text)
    changes = {}

    for world in document.findall("world"):
        world_id = int(world.get("world", "0"))
        if world_id not in worlds:
            continue
        retail_index = {int(door["editor_id"]): door for door in source[world_id]["doors"]}
        for node in world.findall("staticdoor"):
            if node.get("type", "DOOR") != "DOOR":
                continue
            candidates = {int(node.get("doorid", "0")), int(node.get("retailid", "0"))} - {0}
            matched = [retail_index[candidate] for candidate in candidates
                       if candidate in retail_index and same_point(node, retail_index[candidate])]
            if len(matched) != 1:
                continue
            current_state = state_value(node.get("state"))
            desired_state = retail_state(matched[0], current_state)
            current_retail_id = int(node.get("retailid", "0"))
            desired_retail_id = int(matched[0]["fields"].get("id", "0"))
            if desired_state != current_state or desired_retail_id != current_retail_id:
                changes[(world_id, int(node.get("doorid", "0")))] = desired_state, desired_retail_id

    lines = text.splitlines(keepends=True)
    active_lines = uncommented_lines(lines)
    if sum(bool(START_DOOR.search(line)) for line in active_lines) != len(document.findall(".//staticdoor")):
        raise ValueError("staticdoor start tags must stay on one line")
    current_world = None
    applied = set()
    for index, line in enumerate(active_lines):
        world = WORLD_TAG.search(line)
        if world:
            current_world = int(world.group(1))
        if "</world>" in line:
            current_world = None
            continue
        door = DOOR_ID.search(line) if "<staticdoor" in line else None
        key = None if door is None or current_world is None else (current_world, int(door.group(1)))
        if key not in changes:
            continue
        desired_state, desired_retail_id = changes[key]
        updated = STATE.sub("", lines[index])
        updated = RETAIL_ID.sub("", updated)
        if desired_retail_id:
            updated = DOOR_ID.sub(lambda match: match.group(0) + f' retailid="{desired_retail_id}"', updated, count=1)
        if desired_state:
            closing = "/>" if updated.rstrip().endswith("/>") else ">"
            position = updated.rfind(closing)
            updated = updated[:position] + f' state="0x{desired_state:x}"' + updated[position:]
        lines[index] = updated
        applied.add(key)
    if applied != set(changes):
        raise ValueError(f"failed to update static doors: {sorted(set(changes) - applied)}")
    content = "".join(lines)
    ET.fromstring(content)
    return content, len(changes)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    target = root / STATIC_DOORS
    content, count = synchronized_content(root, set(production_worlds(root)))
    if args.check:
        if count or target.read_text(encoding="utf-8") != content:
            raise SystemExit(f"stale retail static-door metadata: {count} updates required")
        print("retail static-door metadata is current")
        return
    target.write_text(content, encoding="utf-8")
    print(f"updated {count} retail static-door definitions")


if __name__ == "__main__":
    main()
