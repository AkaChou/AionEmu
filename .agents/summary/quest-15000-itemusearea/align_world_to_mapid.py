#!/usr/bin/env python3
"""真端 Map/Worlds/<world>/world.xml ↔ 仓库 zones_<mapid>.xml 内容对齐（zone 名重叠度）。

目的：批量修复 usearea 缺失区前，为「真端 world 目录短名」确定本仓 mapid（WorldId.xml 的 id
与客户端 mapid 在 600x 段并不一致）。判据：仓库 zones_<mapid>.xml 的区名（去 `_<mapid>` 后缀、
大写）与真端 world.xml 的 <name> 集合的重叠数；重叠越高越可信。

用法：python3 -B .agents/summary/quest-15000-itemusearea/align_world_to_mapid.py [world ...]
输出：控制台表格（每 world 的 top-5 候选 mapid + 重叠数）。
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
ZONE_DIR = REPO / "src/main/resources/aion/data/static_data/zones"
RETAIL_WORLDS = REPO.parent / "58Server" / "Map" / "Worlds"

NAME = re.compile(r'<name>([^<]+)</name>')


def repo_map_names() -> dict[str, set[str]]:
    out: dict[str, set[str]] = {}
    for path in sorted(ZONE_DIR.glob("zones_*.xml")):
        mid = path.stem.replace("zones_", "")
        if not mid.isdigit():
            continue
        text = path.read_text(encoding="utf-8", errors="ignore")
        names = set()
        for raw in re.findall(r'<zone\b[^>]*\bname="([^"]+)"', text):
            up = raw.upper()
            names.add(re.sub(r"_%s$" % mid, "", up))
            names.add(up)
        out[mid] = names
    return out


def world_names(world: str) -> set[str]:
    path = RETAIL_WORLDS / world / "world.xml"
    if not path.is_file():
        return set()
    return {m.strip().upper() for m in NAME.findall(path.read_text(encoding="utf-16", errors="ignore"))}


def main() -> None:
    worlds = sys.argv[1:] or [
        "ldf4b", "ldf4a", "tiamat_down", "ldf5b", "ldf5a", "lf4", "lf4_m", "lf5", "lf6", "df6",
        "ab1", "idstation", "idstation_event", "IDRaksha_solo", "IDSweep", "IDSweep_02", "ldf5_under",
        "ldf5_fortress", "lf1a", "df1a", "df5", "df4", "df4_m", "IDLDF4A", "IDCatacome",
    ]
    repo = repo_map_names()
    for w in worlds:
        wn = world_names(w)
        if not wn:
            print(f"{w:18s} (真端无 world.xml)")
            continue
        scored = sorted(((len(wn & rn), mid) for mid, rn in repo.items()), reverse=True)
        top = ", ".join(f"{mid}:{n}" for n, mid in scored[:5] if n > 0) or "-"
        print(f"{w:18s} names={len(wn):5d}  top: {top}")


if __name__ == "__main__":
    main()
