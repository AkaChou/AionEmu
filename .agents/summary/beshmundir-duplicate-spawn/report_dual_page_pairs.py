#!/usr/bin/env python3
"""关联 spawn 文件里同坐标的「真端双页」副本怪，输出保留侧（H）与待标注侧（N）。

Correlates same-coordinate spawn entries with npc template names for a
Beshmundir-Temple-style dual-page instance: the handler-backed H_ side is kept,
the N_ page-1 twin must be tagged spawn_page=1 so the default instance page 0
skips it (SpawnEngine.matchesInstance).

用法 / Usage:
    python3 report_dual_page_pairs.py <spawn file> <npc template file> [more]
"""

import collections
import re
import sys

SPAWN_RE = re.compile(r'<spawn\s+npc_id="(\d+)"')
SPOT_RE = re.compile(r'<spot\s+x="([\d.]+)"\s+y="([\d.]+)"\s+z="([\d.]+)"')
TEMPLATE_RE = re.compile(r'name_desc="([^"]+)"[^>]*?npc_id="(\d+)"')


def load_templates(paths):
    names = {}
    for path in paths:
        with open(path, encoding="utf-8") as handle:
            for line in handle:
                for desc, npc_id in TEMPLATE_RE.findall(line):
                    names[npc_id] = desc
    return names


def classify(desc):
    if re.search(r'IDCatacombsH_', desc):
        return "H"
    if re.search(r'IDCatacombsN_', desc):
        return "N"
    return "?"


def scan_spawns(path):
    by_coord = collections.defaultdict(list)
    seen = set()
    current = None
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            match = SPAWN_RE.search(line)
            if match:
                current = match.group(1)
                seen.add(current)
                continue
            match = SPOT_RE.search(line)
            if match and current:
                by_coord[(match.group(1), match.group(2), match.group(3))].append(current)
    return by_coord, seen


def main(spawn_path, template_paths):
    names = load_templates(template_paths)
    by_coord, seen = scan_spawns(spawn_path)

    pairs = {}
    anomalies = []
    for coord, ids in sorted(by_coord.items()):
        unique = sorted(set(ids), key=int)
        if len(unique) < 2:
            continue
        classes = {i: classify(names.get(i, "")) for i in unique}
        h_side = [i for i in unique if classes[i] == "H"]
        n_side = [i for i in unique if classes[i] == "N"]
        if len(unique) == 2 and len(h_side) == 1 and len(n_side) == 1:
            pairs.setdefault((h_side[0], n_side[0]), []).append(coord)
        else:
            anomalies.append((coord, unique, classes))

    to_tag = set()
    print(f"dual-page pairs: {len(pairs)}")
    for (h_id, n_id), coords in sorted(pairs.items(), key=lambda kv: int(kv[0][1])):
        print(f"  keep H {h_id} ({names.get(h_id)}) | tag N {n_id} ({names.get(n_id)}) | {len(coords)} spot(s)")
        to_tag.add(n_id)

    print(f"\nN-side ids to tag spawn_page=\"1\" ({len(to_tag)}):")
    print("  " + " ".join(sorted(to_tag, key=int)))

    if anomalies:
        print(f"\nanomalous duplicate coordinates ({len(anomalies)}):")
        for coord, ids, classes in anomalies:
            print(f"  {coord} -> {ids} {classes}")

    n_in_spawn = sorted({i for i in seen if classify(names.get(i, "")) == "N"}, key=int)
    unpaired = sorted(set(n_in_spawn) - to_tag, key=int)
    print(f"\nN-side ids present in spawn file: {len(n_in_spawn)}; unpaired (left untouched): {unpaired}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2:])
