#!/usr/bin/env python3
"""审计 quest_data.xml 的 quest_work_items 是否已迁移到生产 quest_definition XML。

分档:
  OK        已声明 <work-items> 且与 legacy 一致
  EXPLICIT  XML 未声明, 但对全部物品都有显式 remove-item 兜底
  PARTIAL   XML 未声明, 只有部分物品有 remove-item 兜底
  MISSING   XML 未声明, 且该物品完全没有 remove-item -> 完成后必然残留
"""
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(".").resolve()
QUEST_DATA = ROOT / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml"
DEF_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
CATALOG = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"

legacy = {}
for quest in ET.parse(QUEST_DATA).getroot().iter("quest"):
    node = quest.find("quest_work_items")
    if node is None:
        continue
    items = [(int(w.get("item_id")), int(w.get("count") or 1)) for w in node.findall("quest_work_item")]
    if items:
        legacy[int(quest.get("id"))] = items

executable = {int(d.get("id")) for d in ET.parse(CATALOG).getroot().iter("definition")
              if d.get("mode") == "EXECUTABLE"}

ok, explicit, partial, missing, mismatch = [], [], [], [], []
for qid, items in sorted(legacy.items()):
    if qid not in executable:
        continue
    path = DEF_DIR / f"{qid}.xml"
    if not path.exists():
        continue
    root = ET.parse(path).getroot()
    md = root.find("metadata")
    wi = md.find("work-items") if md is not None else None
    declared = [(int(i.get("id")), int(i.get("count"))) for i in wi.findall("item")] if wi is not None else []
    if declared:
        if sorted(declared) == sorted(items):
            ok.append(qid)
        else:
            mismatch.append((qid, items, declared))
        continue
    removed = {int(e.get("item-id")) for e in root.iter("remove-item") if e.get("item-id")}
    gaps = [i for i, _ in items if i not in removed]
    if not gaps:
        explicit.append(qid)
    elif len(gaps) == len(items):
        missing.append((qid, items))
    else:
        partial.append((qid, gaps))

total = len(ok) + len(explicit) + len(missing) + len(partial) + len(mismatch)
print(f"legacy quest_data.xml 声明 quest_work_items 且为 EXECUTABLE: {total} 个任务")
print(f"  OK        已正确迁移 <work-items>:        {len(ok)}")
print(f"  EXPLICIT  显式 remove-item 全兜底:        {len(explicit)}")
print(f"  PARTIAL   部分物品无兜底:                 {len(partial)}")
print(f"  MISSING   无声明且无兜底(必然残留道具):    {len(missing)}")
print(f"  MISMATCH  声明内容与 legacy 不一致:        {len(mismatch)}")
print()
print(f"=== MISSING 任务 ID（{len(missing)} 个，完成后道具必然残留）===")
print(" ".join(str(q) for q, _ in missing))
print()
print("=== PARTIAL（需人工核对） ===")
for qid, gaps in partial:
    print(f"  {qid}: 缺兜底 {gaps}")
print()
print("=== MISMATCH ===")
for qid, litems, ditems in mismatch:
    print(f"  {qid}: legacy={litems} declared={ditems}")
