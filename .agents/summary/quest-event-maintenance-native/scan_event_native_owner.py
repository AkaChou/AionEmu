#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""D 立项诊断：318 事件任务的 owner 分布静态复现（isNativeOwner 上界分析）。

复现口径（与生产 Java 同源静态文件）：
- 七族注册集 = Quest_{SimpleTalk,SimpleHunt,SimpleSerialHunt,SimpleCollectItem,
  SimpleUseItem,SimpleItemPlay,CombineTask}.xml 的 <id id="N"> 全集
  （SimpleTalkHandler.java:212-220 的 owned 构建；routed = owned − xmlOwnedIds）
- xmlOwnedIds = quest_definition_catalog.xml 的 definition id 集（733 行）
- DataDriven 切换集 = retail-xml-retention.xml 中 owner=RETAIL_TABLE ∧ family=DataDriven
  （DataDrivenNativeRuntime.retentionSwitchSet）
- isNativeOwner ≈ (七族注册 − catalog) ∪ DD 切换集
"""
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path("src/main/resources/aion/data/static_data")
RETAIL = ROOT / "quest/retail"
CATALOG = ROOT / "quest/definitions/quest_definition_catalog.xml"
RETENTION = RETAIL / "retail-xml-retention.xml"
EVENTS = ROOT / "events_config/events_config.xml"

FAMILY_FILES = {
    "SimpleTalk": "Quest_SimpleTalk.xml",
    "SimpleHunt": "Quest_SimpleHunt.xml",
    "SimpleSerialHunt": "Quest_SimpleSerialHunt.xml",
    "SimpleCollectItem": "Quest_SimpleCollectItem.xml",
    "SimpleUseItem": "Quest_SimpleUseItem.xml",
    "SimpleItemPlay": "Quest_SimpleItemPlay.xml",
    "CombineTask": "Quest_CombineTask.xml",
}


def load_quest_ids(path: Path, tag: str) -> set[int]:
    """读表文件的 quest id 全集（递归找 tag 元素取 id 属性）。"""
    tree = ET.parse(path)
    ids = set()
    for elem in tree.getroot().iter(tag):
        raw = elem.get("id")
        if raw and raw.isdigit():
            ids.add(int(raw))
    return ids


catalog_ids = load_quest_ids(CATALOG, "definition")

family_ids: dict[str, set[int]] = {}
for family, fname in FAMILY_FILES.items():
    family_ids[family] = load_quest_ids(RETAIL / fname, "id")
registered = set().union(*family_ids.values())
routed_families = registered - catalog_ids

retention_rows = []
for row in ET.parse(RETENTION).getroot().iter("quest"):
    qid = row.findtext("quest_id")
    owner = row.findtext("owner")
    fam = row.findtext("family")
    if qid and owner:
        retention_rows.append((int(qid), owner, fam or ""))
dd_switch = {q for q, owner, fam in retention_rows if owner == "RETAIL_TABLE" and fam == "DataDriven"}
native_owner_upper = routed_families | dd_switch

# 事件任务 id：events_config.xml 的 <quests><maintainable>…;</maintainable>（分号分隔）
event_ids: set[int] = set()
for elem in ET.parse(EVENTS).getroot().iter():
    if elem.tag in ("startable", "maintainable") and elem.text:
        for token in re.split(r"[;\s]+", elem.text.strip()):
            if token.isdigit():
                event_ids.add(int(token))

owned_by_family = {f: event_ids & (ids - catalog_ids) for f, ids in family_ids.items()}

print(f"catalog 行数            : {len(catalog_ids)}")
print(f"七族注册集              : {len(registered)}（routed（−catalog）= {len(routed_families)}）")
print(f"DD 切换集               : {len(dd_switch)}")
print(f"事件任务总数            : {len(event_ids)}")
print(f"事件 ∩ catalog（XML 保留行）: {sorted(event_ids & catalog_ids)}")
print(f"事件 ∩ 七族 routed       : {len(event_ids & routed_families)}")
for f, ids in sorted(owned_by_family.items()):
    if ids:
        print(f"    {f:18s}: {len(ids)}  {sorted(ids)[:12]}{' …' if len(ids) > 12 else ''}")
print(f"事件 ∩ DD 切换集         : {len(event_ids & dd_switch)}  {sorted(event_ids & dd_switch)[:12]}")
print(f"事件 ∩ isNativeOwner 上界: {len(event_ids & native_owner_upper)}")
uncovered = event_ids - native_owner_upper - catalog_ids
print(f"事件未覆盖（非 native 非 catalog）: {len(uncovered)}  {sorted(uncovered)}")

# 未覆盖细查：在 retention 里的 owner/family
ret_by_id = {q: (owner, fam) for q, owner, fam in retention_rows}
print("\n未覆盖行的 retention 归属：")
for qid in sorted(uncovered):
    print(f"    {qid}: {ret_by_id.get(qid, 'not-in-retention')}")

# 事件任务的 retention 归属分布
print("\n事件任务的 retention 归属分布：")
buckets: dict[str, list[int]] = {}
for qid in sorted(event_ids):
    owner, fam = ret_by_id.get(qid, ("<not-in-retention>", ""))
    buckets.setdefault(f"{owner}/{fam or '-'}", []).append(qid)
for key, ids in sorted(buckets.items()):
    print(f"    {key:28s}: {len(ids):3d}  {ids[:10]}{' …' if len(ids) > 10 else ''}")

# routed 七族 vs catalog 交叉确认：routed 行必然不在 catalog
print(f"\n断言 routed ∩ catalog = ∅: {len(routed_families & catalog_ids) == 0}")
