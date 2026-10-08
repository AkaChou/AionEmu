#!/usr/bin/env python3
"""对 BT 双页 N/H 对逐点计算最近距离，量化 N 侧刷点是否都有 H 孪生。

For each retail dual-page N/H pair, computes the nearest H spot for every N
spot: a far/unmatched N spot means tagging the whole N spawn element would
also remove positions that have no H twin.

用法 / Usage:
    python3 audit_pair_spot_distance.py <spawn file>
"""

import collections
import re
import sys

PAIRS = [
    (216159, 216240), (216184, 216265), (216185, 216266), (216186, 216267), (216187, 216268),
    (216204, 216285), (216206, 216287), (216207, 216288), (216208, 216289), (216209, 216290),
    (216210, 216291), (216212, 216293), (216213, 216294), (216222, 216303), (216223, 216304),
    (216226, 216307), (216227, 216308), (216228, 216309), (216229, 216310), (216231, 216312),
    (216236, 216317), (216583, 216587), (216584, 216588), (216585, 216589),
]

SPAWN_RE = re.compile(r'<spawn npc_id="(\d+)"')
SPOT_RE = re.compile(r'<spot x="([\d.]+)" y="([\d.]+)" z="([\d.]+)"')


def load(path):
    coords = collections.defaultdict(list)
    current = None
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            match = SPAWN_RE.search(line)
            if match:
                current = int(match.group(1))
                continue
            match = SPOT_RE.search(line)
            if match and current is not None:
                coords[current].append(tuple(map(float, match.groups())))
    return coords


def main(path, tolerance):
    coords = load(path)
    total_far = 0
    for h_id, n_id in PAIRS:
        h_pts = coords.get(h_id, [])
        n_pts = coords.get(n_id, [])
        near = 0
        far_lines = []
        for n_pt in n_pts:
            if h_pts:
                best = min(sum((a - b) ** 2 for a, b in zip(n_pt, h_pt)) for h_pt in h_pts) ** 0.5
            else:
                best = float("inf")
            if best <= tolerance:
                near += 1
            else:
                far_lines.append(f"    N {n_pt} -> H {h_id} nearest {best:.2f}m")
        total_far += len(far_lines)
        print(f"H {h_id} / N {n_id}: N spots {len(n_pts)}, H spots {len(h_pts)}, near {near}, far {len(far_lines)}")
        for line in far_lines:
            print(line)
    print(f"total far N spots (>{tolerance}m from any H spot): {total_far}")


if __name__ == "__main__":
    main(sys.argv[1], float(sys.argv[2]) if len(sys.argv) > 2 else 1.0)
