#!/usr/bin/env python3
"""刷点高度投影缺口审计（只读）。

统计每个 world 是否具备刷点时的 Z 投影数据源：
  - geo    : src/main/resources/aion/geo/<worldId>.geo.gz        -> projectGroundZ / getZ
  - terrain: src/main/resources/aion/geo/<worldId>.png (高度图)    -> getTerrainZ（TERRAIN_DISABLED_MAPS 内的 world 视为没有）
  - path   : src/main/resources/aion/geo/path/<worldId>.idx+.path.gz -> projectGroundPoint

然后统计这些 world 里的**可移动、非飞行**刷点数量，以及哪些点在 spawn 时完全拿不到
投影源（只能退回 XML 里的 z）。

用法：python3 .agents/summary/spawn-z-audit/audit_projection_gaps.py
输出：stdout 摘要 + 同目录 report.md / worlds.csv
"""
import csv
import os
import re
import sys
from collections import defaultdict

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
GEO_DIR = os.path.join(ROOT, "src/main/resources/aion/geo")
PATH_DIR = os.path.join(GEO_DIR, "path")
SPAWN_DIR = os.path.join(ROOT, "src/main/resources/aion/data/static_data/spawns")
NPC_DIR = os.path.join(ROOT, "src/main/resources/aion/data/static_data/npcs")
GEOWORLD_LOADER = os.path.join(
    ROOT, "src/main/java/com/aionemu/gameserver/geoEngine/GeoWorldLoader.java")
WORLD_MAPS = os.path.join(ROOT, "src/main/resources/aion/data/static_data/world_maps.xml")


def parse_terrain_disabled_maps():
    text = open(GEOWORLD_LOADER, encoding="utf-8").read()
    m = re.search(r"TERRAIN_DISABLED_MAPS\s*=\s*Set\.of\((.*?)\);", text, re.S)
    if not m:
        return set()
    return {int(v) for v in re.findall(r"\d{6,}", m.group(1))}


def active_world_ids():
    """world_maps.xml 中未注释的 <map id=...>；注释掉的 world 不会加载，其刷点数据是死数据。"""
    text = open(WORLD_MAPS, encoding="utf-8", errors="ignore").read()
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
    return {int(v) for v in re.findall(r'<map\s+id="(\d+)"', text)}


def collect_geo_sources():
    geo, terrain, path = set(), set(), set()
    for name in os.listdir(GEO_DIR):
        full = os.path.join(GEO_DIR, name)
        if not os.path.isfile(full):
            continue
        if name.endswith(".geo.gz") or name.endswith(".geo"):
            stem = name.split(".")[0]
            if stem.isdigit():
                geo.add(int(stem))
        elif name.endswith(".png") and not name.endswith("_materials.png"):
            for token in name[: -len(".png")].split(","):
                if token.isdigit():
                    terrain.add(int(token))
    for name in os.listdir(PATH_DIR):
        if name.endswith(".idx"):
            stem = name[: -len(".idx")]
            if stem.isdigit() and os.path.isfile(os.path.join(PATH_DIR, stem + ".path.gz")):
                path.add(int(stem))
    return geo, terrain, path


def collect_npc_speeds():
    """npc_id -> 最大移动速度（run/walk），0 表示不可移动。"""
    speed = {}
    for name in sorted(os.listdir(NPC_DIR)):
        if not (name.startswith("npc_template") and name.endswith(".xml")):
            continue
        text = open(os.path.join(NPC_DIR, name), encoding="utf-8", errors="ignore").read()
        for block in re.finditer(r"<npc_template\b([^>]*)>(.*?)</npc_template>", text, re.S):
            attrs = block.group(1)
            npc_id = re.search(r'npc_id="(\d+)"', attrs)
            if not npc_id:
                continue
            stats = re.search(r"<stats\b([^>]*)/?>", block.group(2))
            run = walk = 0.0
            if stats:
                r = re.search(r'run_speed="([\d.]+)"', stats.group(1))
                w = re.search(r'walk_speed="([\d.]+)"', stats.group(1))
                run = float(r.group(1)) if r else 0.0
                walk = float(w.group(1)) if w else 0.0
            speed[int(npc_id.group(1))] = max(run, walk)
    return speed


def collect_spots():
    """world_id -> list of dict(spot)"""
    per_world = defaultdict(list)
    for dirpath, _dirs, files in os.walk(SPAWN_DIR):
        for name in files:
            if not name.endswith(".xml"):
                continue
            m = re.match(r"(\d{6,})_", name)
            if not m:
                continue
            world_id = int(m.group(1))
            try:
                text = open(os.path.join(dirpath, name), encoding="utf-8", errors="ignore").read()
            except OSError:
                continue
            for spawn in re.finditer(r"<spawn\b([^>]*)>(.*?)</spawn>", text, re.S):
                sp = re.search(r'npc_id="(\d+)"', spawn.group(1))
                if not sp:
                    continue
                npc_id = int(sp.group(1))
                for spot in re.finditer(r"<spot\b([^>]*)/>", spawn.group(2)):
                    a = dict(re.findall(r'(\w+)="([^"]*)"', spot.group(1)))
                    if "x" not in a or "z" not in a:
                        continue
                    per_world[world_id].append({
                        "npc_id": npc_id,
                        "file": os.path.relpath(os.path.join(dirpath, name), ROOT),
                        "resolve_z": a.get("resolve_z", "false").lower() == "true",
                        "fly": a.get("fly", "0") not in ("0", "", "false"),
                        "random_walk": int(a.get("random_walk", "0") or 0),
                        "walker_id": a.get("walker_id", ""),
                        "x": float(a["x"]), "y": float(a["y"]), "z": float(a["z"]),
                    })
    return per_world


def main():
    disabled = parse_terrain_disabled_maps()
    geo, terrain_raw, path = collect_geo_sources()
    terrain = {w for w in terrain_raw if w not in disabled}
    npc_speed = collect_npc_speeds()
    per_world = collect_spots()
    active = active_world_ids()
    inactive_worlds = sorted(w for w in per_world if w not in active)

    rows = []
    for world_id, spots in sorted(per_world.items()):
        if world_id not in active:
            continue
        has_geo = world_id in geo
        has_terrain = world_id in terrain
        has_path = world_id in path
        name = ""
        for d in spots:
            name = os.path.basename(d["file"]).split("_", 1)[-1].rsplit(".", 1)[0]
            break

        mobile = [s for s in spots
                  if not s["fly"] and npc_speed.get(s["npc_id"], 1.0) > 0]
        # spawn 时能拿到投影源？
        def has_spawn_projection(s):
            if s["resolve_z"]:
                return has_geo or has_path or has_terrain
            return has_path or has_terrain

        geo_covered = [s for s in mobile if s["resolve_z"] and has_geo]
        path_dependent = [s for s in mobile if not s["resolve_z"]]
        no_projection = [s for s in mobile if not has_spawn_projection(s)]
        # 有 geo 的图，随机游走怪在移动中会被 WalkManager 的 10s Z 检查纠正
        no_projection_no_runtime = [s for s in no_projection
                                    if not (has_geo and (s["random_walk"] > 0 or s["walker_id"]))]
        rows.append({
            "world_id": world_id,
            "name": name,
            "geo": has_geo,
            "terrain": has_terrain,
            "path": has_path,
            "terrain_disabled": world_id in disabled,
            "spots": len(spots),
            "mobile_spots": len(mobile),
            "mobile_geo_covered": len(geo_covered),
            "mobile_path_dependent": len(path_dependent),
            "mobile_no_projection": len(no_projection),
            "mobile_no_projection_no_runtime": len(no_projection_no_runtime),
        })

    def bucket(r):
        t = r["terrain"]
        p = r["path"]
        if not r["geo"]:
            return "NO_GEO"
        if not t and not p:
            return "NO_TERRAIN_NO_PATH"
        if not t:
            return "PATH_ONLY"
        if not p:
            return "TERRAIN_ONLY"
        return "OK"

    summary = defaultdict(lambda: {"worlds": 0, "mobile": 0, "geo_covered": 0, "path_dependent": 0,
                                  "no_proj": 0, "no_proj_no_runtime": 0})
    for r in rows:
        b = bucket(r)
        summary[b]["worlds"] += 1
        summary[b]["mobile"] += r["mobile_spots"]
        summary[b]["geo_covered"] += r["mobile_geo_covered"]
        summary[b]["path_dependent"] += r["mobile_path_dependent"]
        summary[b]["no_proj"] += r["mobile_no_projection"]
        summary[b]["no_proj_no_runtime"] += r["mobile_no_projection_no_runtime"]

    order = ["OK", "TERRAIN_ONLY", "PATH_ONLY", "NO_TERRAIN_NO_PATH", "NO_GEO"]
    print("world_maps.xml 中的有效 world: %d ；被注释(不加载)但有刷点文件的 world: %d %s"
          % (len(active), len(inactive_worlds), inactive_worlds[:10]))
    print("world 总数(有刷点且已加载): %d   geo:%d  terrain(有效):%d  path:%d  terrain_disabled:%d"
          % (len(rows), len(geo), len(terrain), len(path), len(disabled)))
    print()
    print("%-18s %6s %10s %10s %12s %12s %14s" % ("bucket", "worlds", "mobile", "geo_covered",
          "path_depend", "no_projection", "no_proj&no_rt"))
    for b in order:
        s = summary[b]
        print("%-18s %6d %10d %10d %12d %12d %14d" % (b, s["worlds"], s["mobile"], s["geo_covered"],
              s["path_dependent"], s["no_proj"], s["no_proj_no_runtime"]))

    print()
    only_path = [r for r in rows if bucket(r) == "PATH_ONLY"]
    only_path.sort(key=lambda r: -r["mobile_path_dependent"])
    print("无 terrain（PATH 为唯一兜底）的 world 中，依赖 PATH 投影的可移动刷点 top10：")
    print("%-12s %-26s %10s %14s" % ("world", "name", "mobile", "path_depend"))
    for r in only_path[:10]:
        print("%-12d %-26s %10d %14d" % (r["world_id"], r["name"][:25], r["mobile_spots"], r["mobile_path_dependent"]))

    risky = [r for r in rows if bucket(r) in ("NO_TERRAIN_NO_PATH", "NO_GEO")]
    risky.sort(key=lambda r: -r["mobile_no_projection_no_runtime"])
    print()
    print("风险最高的 world（可移动、无投影源、且运行期也拿不到修正的刷点数）：")
    print("%-12s %-28s %8s %8s %8s %8s" % ("world", "name", "spots", "mobile", "no_proj", "no_rt"))
    for r in risky[:15]:
        print("%-12d %-28s %8d %8d %8d %8d" % (
            r["world_id"], r["name"][:27], r["spots"], r["mobile_spots"],
            r["mobile_no_projection"], r["mobile_no_projection_no_runtime"]))

    out_dir = os.path.dirname(os.path.abspath(__file__))
    with open(os.path.join(out_dir, "worlds.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()) + ["bucket"])
        w.writeheader()
        for r in sorted(rows, key=lambda r: (bucket(r), r["world_id"])):
            r2 = dict(r, bucket=bucket(r))
            w.writerow(r2)
    with open(os.path.join(out_dir, "report.md"), "w", encoding="utf-8") as f:
        f.write("# 刷点 Z 投影缺口审计（只读）\n\n")
        f.write("判据：geo=%s；terrain=%s（TERRAIN_DISABLED_MAPS 内视为无）；path=%s\n\n"
                % ("geo/<id>.geo.gz", "geo/<id>.png", "geo/path/<id>.idx+.path.gz"))
        f.write("只统计 world_maps.xml 中已启用（未被注释）的 world；"
                "被注释但有刷点文件的 world: %s\n\n" % (inactive_worlds,))
        f.write("| bucket | worlds | mobile_spots | no_projection | no_proj&no_runtime |\n")
        f.write("|---|---|---|---|---|\n")
        for b in order:
            s = summary[b]
            f.write("| %s | %d | %d | %d | %d |\n" % (b, s["worlds"], s["mobile"], s["no_proj"], s["no_proj_no_runtime"]))
        f.write("\n## 风险 world（前 40）\n\n")
        f.write("| world_id | name | spots | mobile | no_projection | no_proj&no_runtime |\n|---|---|---|---|---|---|\n")
        for r in risky[:40]:
            f.write("| %d | %s | %d | %d | %d | %d |\n" % (
                r["world_id"], r["name"], r["spots"], r["mobile_spots"],
                r["mobile_no_projection"], r["mobile_no_projection_no_runtime"]))
    print()
    print("已写出: .agents/summary/spawn-z-audit/report.md / worlds.csv")
    return 0


if __name__ == "__main__":
    sys.exit(main())
