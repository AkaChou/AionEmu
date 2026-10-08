#!/usr/bin/env python3
"""清理 BT spawn 文件中真端双页 N 侧与 H 侧精确重叠的刷点。

Prunes Beshmundir dual-page duplicates in the spawn file: fully-overlapped N
spawn elements get spawn_page="1" (the retail page-1 twin, skipped by the
default page-0 instance); partially-overlapped N elements keep their
non-overlapped spots and lose only the exactly-duplicated ones.

用法 / Usage:
    python3 prune_duplicate_n_spots.py <spawn file>
"""

import re
import sys

PAIRS = [
    (216159, 216240), (216184, 216265), (216185, 216266), (216186, 216267), (216187, 216268),
    (216204, 216285), (216206, 216287), (216207, 216288), (216208, 216289), (216209, 216290),
    (216210, 216291), (216212, 216293), (216213, 216294), (216222, 216303), (216223, 216304),
    (216226, 216307), (216227, 216308), (216228, 216309), (216229, 216310), (216231, 216312),
    (216236, 216317), (216583, 216587), (216584, 216588), (216585, 216589),
]

SPAWN_START_RE = re.compile(r'^(?P<indent>\s*)<spawn\s+npc_id="(?P<npc_id>\d+)"(?P<rest>[^>]*)>\s*$')
SPOT_RE = re.compile(r'^\s*<spot\s+x="([\d.]+)" y="([\d.]+)" z="([\d.]+)"')


def main(path):
    with open(path, encoding="utf-8") as handle:
        lines = handle.readlines()

    blocks = []
    current = None
    for idx, line in enumerate(lines):
        match = SPAWN_START_RE.match(line)
        if match:
            current = {"start": idx, "line": line, "npc_id": int(match.group("npc_id")), "spots": []}
            blocks.append(current)
            continue
        if line.strip() == "</spawn>":
            current = None
            continue
        match = SPOT_RE.match(line)
        if match and current is not None:
            current["spots"].append((idx, tuple(map(float, match.groups()))))

    n_to_h = {n: h for h, n in PAIRS}
    coords_of = {}
    for block in blocks:
        coords_of.setdefault(block["npc_id"], []).extend(c for _, c in block["spots"])

    tag_lines = {}
    delete_lines = set()
    report = []
    for block in blocks:
        n_id = block["npc_id"]
        if n_id not in n_to_h:
            continue
        h_coords = set(coords_of.get(n_to_h[n_id], []))
        overlapped = [(idx, coord) for idx, coord in block["spots"] if coord in h_coords]
        if not overlapped:
            continue
        if len(overlapped) == len(block["spots"]):
            match = SPAWN_START_RE.match(block["line"])
            tag_lines[block["start"]] = (
                f'{match.group("indent")}<spawn npc_id="{match.group("npc_id")}"'
                f'{match.group("rest")} spawn_page="1">\n')
            report.append(f'tag spawn_page="1"  npc {n_id}: {len(overlapped)} of {len(block["spots"])} spot(s) overlapped')
        else:
            for idx, _ in overlapped:
                delete_lines.add(idx)
            kept = len(block["spots"]) - len(overlapped)
            report.append(f'prune {len(overlapped)} spot(s)  npc {n_id}: kept {kept} non-overlapped spot(s)')

    with open(path, "w", encoding="utf-8") as handle:
        for idx, line in enumerate(lines):
            if idx in tag_lines:
                handle.write(tag_lines[idx])
            elif idx in delete_lines:
                continue
            else:
                handle.write(line)

    print("\n".join(report))
    print(f"total: {len(tag_lines)} tagged element(s), {len(delete_lines)} pruned spot line(s)")


if __name__ == "__main__":
    main(sys.argv[1])
