#!/usr/bin/env python3
"""统计 PATH 投影失败点的 NPC 类型分布（只读）。"""
import glob
import importlib.util
import os
import re
import sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("proj", os.path.join(HERE, "audit_path_projection.py"))
proj = importlib.util.module_from_spec(spec)
spec.loader.exec_module(proj)
gaps = proj.gaps


def collect_npc_meta():
    meta = {}
    for f in glob.glob(os.path.join(gaps.NPC_DIR, "npc_template_*.xml")):
        text = open(f, encoding="utf-8", errors="ignore").read()
        for m in re.finditer(r"<npc_template\b([^>]*)>(.*?)</npc_template>", text, re.S):
            a = m.group(1)
            nid = re.search(r'npc_id="(\d+)"', a)
            if not nid:
                continue
            stats = re.search(r"<stats\b([^>]*)/?>", m.group(2))
            s = stats.group(1) if stats else ""
            walk = float(re.search(r'walk_speed="([\d.]+)"', s).group(1)) if re.search(r'walk_speed="([\d.]+)"', s) else 0.0
            run = float(re.search(r'run_speed="([\d.]+)"', s).group(1)) if re.search(r'run_speed="([\d.]+)"', s) else 0.0
            meta[nid.group(1)] = {
                "npc_type": (re.search(r'npc_type="([^"]*)"', a) or [None, "?"])[1],
                "type": (re.search(r'\btype="([^"]*)"', a) or [None, "?"])[1],
                "abyss": (re.search(r'abyss_type="([^"]*)"', a) or [None, "-"])[1],
                "ai": (re.search(r'\bai="([^"]*)"', a) or [None, "?"])[1],
                "speed": max(walk, run),
            }
    return meta


def main():
    disabled = gaps.parse_terrain_disabled_maps()
    geo, terrain_raw, path = gaps.collect_geo_sources()
    terrain = {w for w in terrain_raw if w not in disabled}
    meta = collect_npc_meta()
    per_world = gaps.collect_spots()
    active = gaps.active_world_ids()
    targets = sorted(w for w in per_world if w in active and w in geo and w in path and w not in terrain)

    failed_types, failed_ais, failed_abyss = Counter(), Counter(), Counter()
    depend_types = Counter()
    total_failed = 0
    for world_id in targets:
        mobile = [s for s in per_world[world_id]
                  if not s["fly"] and meta.get(str(s["npc_id"]), {}).get("speed", 1.0) > 0 and not s["resolve_z"]]
        if not mobile:
            continue
        reader = proj.PathReader(world_id)
        for spot in mobile:
            m = meta.get(str(spot["npc_id"]), {"npc_type": "?", "ai": "?", "abyss": "-"})
            depend_types[m["npc_type"]] += 1
            if reader.project(spot["x"], spot["y"], spot["z"], proj.nan_terrain) is None:
                total_failed += 1
                failed_types[m["npc_type"]] += 1
                failed_ais[m["ai"]] += 1
                failed_abyss[m["abyss"]] += 1
        del reader

    print("依赖 PATH 的点按 npc_type:", dict(depend_types))
    print()
    print("投影失败总数:", total_failed)
    print("失败点按 npc_type:", failed_types.most_common())
    print()
    print("失败点按 ai (top12):", failed_ais.most_common(12))
    print()
    print("失败点按 abyss_type (top12):", failed_abyss.most_common(12))
    return 0


if __name__ == "__main__":
    sys.exit(main())
