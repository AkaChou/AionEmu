#!/usr/bin/env python3
"""item 模板 usearea ↔ zones_*.xml 注册覆盖审计 + 真端 source_sphere.csv 几何对拍。

背景（2026-10-08，Q15000 维修工具无法使用，QE-043 同类）：
- item_template 的 `uselimits usearea="X"` 由 PlayerRestrictions#canUseItem 校验；
  X 未在 zones_*.xml 注册 ⇒ ZoneName.get 告警「缺少区域」并回退 NONE ⇒ 是否在区内恒 false
  ⇒ 1300143「无法在此处使用该物品」拦截。
- 真端权威几何：<真端根>/Map/XML/Subzones/source_sphere.csv 的 itemUseArea 行
  （列序 name,type,zone,layer,x,y,z,r,...；zone 短名 lf1a/lf5/... 对应 mapid）。

用法：
    python3 .agents/summary/quest-15000-itemusearea/audit_itemusearea_zone_registry.py
输出：
    itemusearea-zone-registry.tsv        逐 usearea 一行（含注册状态/真端几何/使用它的 item）
    控制台摘要（GAP = 未注册的 usearea）
"""

from __future__ import annotations

import re
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
ITEM_DIR = REPO / "src/main/resources/aion/data/static_data/items/item"
ZONE_DIR = REPO / "src/main/resources/aion/data/static_data/zones"

# 真端根（同宿主目录约定）：<仓库根>/../58Server
RETAIL = REPO.parent / "58Server"
SOURCE_SPHERE = RETAIL / "Map" / "XML" / "Subzones" / "source_sphere.csv"

USEAREA = re.compile(r'uselimits\b[^>]*\busearea="([^"]+)"')
ITEM_TAG = re.compile(r"<item_template\b[^>]*>")
ITEM_ID = re.compile(r'\bid="(\d+)"')
ITEM_DESC = re.compile(r'name_desc="([^"]*)"')
ITEM_NAME = re.compile(r'\bname="([^"]*)"')
ZONE_NAME = re.compile(r'<zone\b[^>]*\bname="([^"]+)"')


def scan_registered_zones() -> dict[str, str]:
    """zone name(上) → 声明文件（全量 zones_*.xml）。"""
    found: dict[str, str] = {}
    for path in sorted(ZONE_DIR.glob("zones_*.xml")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for name in ZONE_NAME.findall(text):
            found.setdefault(name.upper(), path.name)
    return found


def scan_item_useareas() -> dict[str, list[str]]:
    """usearea(原样) → [item 描述]。"""
    uses: dict[str, list[str]] = {}
    for path in sorted(ITEM_DIR.glob("*.xml")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        starts = [m.start() for m in ITEM_TAG.finditer(text)]
        for i, start in enumerate(starts):
            end = starts[i + 1] if i + 1 < len(starts) else len(text)
            seg = text[start:end]  # 元素整段（开标签 + 子元素体），uselimits 是子元素
            m = USEAREA.search(seg)
            if not m:
                continue
            item_id = ITEM_ID.search(seg)
            desc = ITEM_DESC.search(seg)
            name = ITEM_NAME.search(seg)
            label = "{} id={} name_desc={} name={}".format(
                path.name,
                item_id.group(1) if item_id else "?",
                desc.group(1) if desc else "?",
                name.group(1) if name else "?",
            )
            uses.setdefault(m.group(1), []).append(label)
    return uses


def scan_retail_spheres() -> dict[str, list[tuple[str, str, str, str, str, str]]]:
    """usearea 名(上) → [(zone, layer, x, y, z, r)]，来自真端 source_sphere.csv。"""
    spheres: dict[str, list[tuple[str, str, str, str, str, str]]] = {}
    if not SOURCE_SPHERE.is_file():
        return spheres
    for line in SOURCE_SPHERE.read_text(encoding="utf-8", errors="ignore").splitlines()[1:]:
        cols = line.split(",")
        if len(cols) < 8 or cols[1].strip() != "itemUseArea":
            continue
        name = cols[0].strip()
        if not name.lower().startswith("usearea_"):
            continue
        key = name[len("usearea_"):].upper()
        spheres.setdefault(key, []).append(
            (cols[2].strip(), cols[3].strip(), cols[4].strip(), cols[5].strip(), cols[6].strip(), cols[7].strip()))
    return spheres


def scan_retail_worlds(names: set[str]) -> dict[str, list[str]]:
    """usearea 名(上) → [真端 World 目录名]，来自 Map/Worlds/*/world.xml 的 <item_use_area><name>（大小写不敏感）。"""
    worlds_dir = RETAIL / "Map" / "Worlds"
    found: dict[str, list[str]] = {}
    if not worlds_dir.is_dir():
        return found
    for path in sorted(worlds_dir.glob("*/world.xml")):
        try:
            text = path.read_text(encoding="utf-16", errors="ignore").lower()
        except OSError:
            continue
        for name in names:
            if "<name>%s</name>" % name.lower() in text:
                found.setdefault(name.upper(), []).append(path.parent.name)
    return found


def main() -> None:
    registered = scan_registered_zones()
    uses = scan_item_useareas()
    spheres = scan_retail_spheres()
    worlds = scan_retail_worlds(set(uses))

    out = Path(__file__).resolve().parent / "itemusearea-zone-registry.tsv"
    gaps: list[str] = []
    rows: list[str] = []
    for usearea in sorted(uses):
        zone_file = registered.get(usearea.upper())
        geo = spheres.get(usearea.upper(), [])
        geo_txt = " | ".join("zone=%s layer=%s x=%s y=%s z=%s r=%s" % g for g in geo) or "-"
        world_txt = ",".join(worlds.get(usearea.upper(), [])) or "-"
        rows.append("\t".join([usearea, zone_file or "*** GAP ***", geo_txt, world_txt, "; ".join(uses[usearea])]))
        if zone_file is None:
            gaps.append(usearea)
    out.write_text("usearea\tregistered_in\tretail_source_sphere\tretail_world_xml\titems\n" + "\n".join(rows) + "\n",
                   encoding="utf-8")

    print("usearea 总数: %d / 已注册: %d / GAP(未注册): %d" % (len(uses), len(uses) - len(gaps), len(gaps)))
    for g in gaps:
        geo = spheres.get(g.upper(), [])
        geo_txt = " | ".join("zone=%s layer=%s x=%s y=%s z=%s r=%s" % gg for gg in geo) or "(真端 source_sphere 无该行)"
        print("  GAP %-40s %s" % (g, geo_txt))
        for item in uses[g]:
            print("      item: %s" % item)
    print("明细输出: %s" % out)


if __name__ == "__main__":
    main()
