#!/usr/bin/env python3
"""精确不变量:对每个 legacy work item,必须满足其一
  (A) 在 metadata 声明 <work-items>  -> 引擎在完成/放弃时自动清理;
  (B) 每一条「真正进入 REWARD」的转换(目标 REWARD 且源非 REWARD)都显式 remove-item。
只满足「定义里某处有 remove-item」不算 —— 1192 正是栽在这一点上。
"""
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(".").resolve()
DEF_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
CATALOG = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"

legacy = {}
for quest in ET.parse(ROOT / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml").getroot().iter("quest"):
    node = quest.find("quest_work_items")
    if node is None:
        continue
    items = [int(w.get("item_id")) for w in node.findall("quest_work_item")]
    if items:
        legacy[int(quest.get("id"))] = items

executable = {int(d.get("id")) for d in ET.parse(CATALOG).getroot().iter("definition")
              if d.get("mode") == "EXECUTABLE"}

kind_a, kind_b, defects = [], [], []
for qid, items in sorted(legacy.items()):
    if qid not in executable:
        continue
    path = DEF_DIR / f"{qid}.xml"
    if not path.exists():
        continue
    root = ET.parse(path).getroot()
    if root.find("metadata/work-items") is not None:
        kind_a.append(qid); continue
    status_of = {n.get("label"): n.get("status") for n in root.iter("node")}
    entries = [t for t in list(root.iter("transition")) + list(root.iter("dialog"))
               if status_of.get(t.get("target")) == "REWARD"
               and status_of.get(t.get("source") or "") != "REWARD"]
    if not entries:
        defects.append((qid, items, "no route enters REWARD"))
        continue
    bad = []
    for t in entries:
        rem = {int(e.get("item-id")) for e in t.iter("remove-item") if e.get("item-id")}
        missing = [i for i in items if i not in rem]
        if missing:
            bad.append((t.get("source") or "(source-less)", missing))
    if bad:
        defects.append((qid, items, bad))
    else:
        kind_b.append(qid)

print(f"legacy work item 任务总数(EXECUTABLE): {len(kind_a)+len(kind_b)+len(defects)}")
print(f"  (A) 声明 <work-items>:                       {len(kind_a)}")
print(f"  (B) 每条入 REWARD 路由都显式 remove:           {len(kind_b)}")
print(f"  (X) 真缺陷 — 存在不交出物品的入 REWARD 路由:    {len(defects)}")
print()
for qid, items, detail in defects:
    print(f"  {qid}: work={items} -> {detail}")
