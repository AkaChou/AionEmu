#!/usr/bin/env python3
"""扫描 spawn 文件里完全同坐标的多条刷点，用于定位「同位置多只 NPC」重叠。

Scan spawn files for spots sharing identical (x, y, z) coordinates to locate
overlapping same-position NPC spawn groups (e.g. retail dual-page N/H pairs
flattened into a single page). Spawn elements whose spawn_page range excludes
the probed instance page (default 0) are skipped, matching
SpawnEngine.matchesInstance semantics.

用法 / Usage:
    python3 scan_duplicate_spots.py <spawn file> [instance page, default 0]
"""

import collections
import re
import sys

SPAWN_RE = re.compile(r'<spawn\s+npc_id="(\d+)"([^>]*)>')
SPOT_RE = re.compile(r'<spot\s+x="([\d.]+)"\s+y="([\d.]+)"\s+z="([\d.]+)"')
PAGE_RE = re.compile(r'spawn_page="(\d+)"')
PAGE_END_RE = re.compile(r'spawn_page_end="(\d+)"')


def matches_page(rest, page):
    match = PAGE_RE.search(rest)
    if not match:
        return True
    start = int(match.group(1))
    end_match = PAGE_END_RE.search(rest)
    end = int(end_match.group(1)) if end_match else start
    return start <= page <= end


def scan(path, page):
    by_coord = collections.defaultdict(list)
    current = None
    active = True
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            match = SPAWN_RE.search(line)
            if match:
                current = match.group(1)
                active = matches_page(match.group(2), page)
                continue
            match = SPOT_RE.search(line)
            if match and current and active:
                by_coord[(match.group(1), match.group(2), match.group(3))].append(current)
    return by_coord


def main(paths):
    page = int(paths[-1]) if paths[-1].isdigit() else 0
    files = paths[:-1] if paths[-1].isdigit() else paths
    for path in files:
        duplicates = {coord: ids for coord, ids in scan(path, page).items() if len(ids) > 1}
        print(f"== {path} (page {page}): {len(duplicates)} duplicate coordinates ==")
        for (x, y, z), ids in duplicates.items():
            print(f"  ({x}, {y}, {z}) -> npc_ids {sorted(set(ids), key=int)}")


if __name__ == "__main__":
    main(sys.argv[1:])
