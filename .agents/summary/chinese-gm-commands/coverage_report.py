#!/usr/bin/env python3
"""按世界统计中文传送点覆盖度。

Coverage report: for every world in world_maps.xml, list the Chinese teleloc
entries (map-level first) so the 移动 command can be planned.
"""
import re
import xml.etree.ElementTree as ET

WM = "src/main/resources/aion/data/static_data/world_maps.xml"
TSV = ".agents/summary/chinese-gm-commands/teleloc_zh.tsv"

worlds = []
for node in ET.parse(WM).getroot().findall("map"):
    worlds.append((int(node.get("id")), node.get("name")))

rows = []
with open(TSV, encoding="utf-8") as fh:
    header = fh.readline()
    for line in fh:
        loc_id, mapid, en, name_id, key, zh, src, x, y, z = line.rstrip("\n").split("\t")
        rows.append(dict(loc_id=int(loc_id), mapid=int(mapid), en=en, name_id=int(name_id), zh=zh,
                         key=key, src=src, x=x, y=y, z=z))

by_map = {}
for r in rows:
    by_map.setdefault(r["mapid"], []).append(r)

missing = []
for wid, name in worlds:
    entries = by_map.get(wid, [])
    zone = [r for r in entries if r["zh"] and r["key"].endswith("_ZONE")]
    withpos = [r for r in entries if r["zh"] and r["x"]]
    zpos = [r for r in zone if r["x"]]
    flag = "OK" if zpos else ("ZONE_NO_POS" if zone else ("SUB_ONLY" if withpos else "NONE"))
    print(f"{wid}\t{name}\t{flag}\tzone={len(zone)}\tzone_pos={len(zpos)}\tall={len(entries)}\tpos={len(withpos)}"
          f"\t{zone[0]['zh'] if zone else ''}\t{withpos[0]['zh'] if withpos else ''}")
    if not zpos:
        missing.append((wid, name, flag, [ (r['loc_id'], r['zh'], r['en'], bool(r['x'])) for r in entries ]))

print("\n=== worlds without a Chinese map-level teleport with coordinates ===")
for wid, name, flag, entries in missing:
    print(wid, name, flag, entries[:6])
