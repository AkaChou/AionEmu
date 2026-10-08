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


def retail_world_mapids() -> dict[str, str]:
    """真端 world 短名(小写) → 本仓 mapid。

    首选 WorldId.xml 的 id（当该 id 存在于本仓 world_maps.xml 时可信：ab1/lf4/lf6/df6/
    idstation/IDRaksha_solo/IDSweep 等）；600x 段真端 id 与客户端 id 不同号，故对
    content 反查过的世界用显式覆盖（ldf5a ↔ 600050000：本仓该 map 的区名含 LDF5A_ITEMUSEAREA_*）。
    """
    repo_maps = set()
    world_maps = REPO / "src/main/resources/aion/data/static_data/world_maps.xml"
    text = world_maps.read_text(encoding="utf-8", errors="ignore")
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)  # 注释掉的 map（如 [Master Server]）不算存在
    for m in re.finditer(r'<map id="(\d+)"', text):
        repo_maps.add(m.group(1))
    overrides = {"ldf5a": "600050000"}  # 内容反查（LDF5A_ITEMUSEAREA_* 同时出现在两处）
    out: dict[str, str] = dict(overrides)
    world_id = RETAIL / "Map" / "XML" / "Subzones" / "WorldId.xml"
    if world_id.is_file():
        text = world_id.read_text(encoding="utf-16", errors="ignore")
        for attrs, name in re.findall(r"<data\b([^>]*)>([^<]+)</data>", text):
            mid = re.search(r'id="(\d+)"', attrs)
            if mid and mid.group(1) in repo_maps:
                out.setdefault(name.strip().lower(), mid.group(1))
    return out


def main() -> None:
    registered = scan_registered_zones()
    uses = scan_item_useareas()
    spheres = scan_retail_spheres()
    worlds = scan_retail_worlds(set(uses))
    world_mapids = retail_world_mapids()

    out = Path(__file__).resolve().parent / "itemusearea-zone-registry.tsv"
    gaps: list[str] = []
    bucket: dict[str, list[str]] = {"fixable": [], "world_absent": [], "no_retail_def": []}
    rows: list[str] = []
    for usearea in sorted(uses):
        zone_file = registered.get(usearea.upper())
        geo = spheres.get(usearea.upper(), [])
        geo_txt = " | ".join("zone=%s layer=%s x=%s y=%s z=%s r=%s" % g for g in geo) or "-"
        world_list = worlds.get(usearea.upper(), [])
        world_txt = ",".join(world_list) or "-"
        candidates = world_list + [g[0] for g in geo]  # world.xml 目录名 + source_sphere 的 zone 短名
        resolved = sorted({w.lower() for w in candidates if w.lower() in world_mapids})
        if zone_file is not None:
            status = "REGISTERED"
        elif resolved:
            status = "GAP:fixable"
            bucket["fixable"].append(usearea)
        elif candidates:
            status = "GAP:world_absent"
            bucket["world_absent"].append(usearea)
        else:
            status = "GAP:no_retail_def"
            bucket["no_retail_def"].append(usearea)
        resolved_txt = "mapid=" + ",".join(world_mapids[w] for w in resolved) if resolved else "-"
        if zone_file is None:
            gaps.append(usearea)
        rows.append("\t".join([usearea, status, zone_file or "-", resolved_txt, geo_txt, world_txt, "; ".join(uses[usearea])]))
    out.write_text("usearea\tstatus\tregistered_in\tresolved_mapid\tretail_source_sphere\tretail_world_xml\titems\n"
                   + "\n".join(rows) + "\n", encoding="utf-8")

    print("usearea 总数: %d / 已注册: %d / GAP(未注册): %d" % (len(uses), len(uses) - len(gaps), len(gaps)))
    print("  GAP:fixable=%d  world_absent=%d  no_retail_def=%d" % (
        len(bucket["fixable"]), len(bucket["world_absent"]), len(bucket["no_retail_def"])))
    for group in ("fixable", "world_absent", "no_retail_def"):
        for g in bucket[group]:
            print("  [%s] %s" % (group, g))
    print("明细输出: %s" % out)


if __name__ == "__main__":
    main()
