#!/usr/bin/env python3
"""审计：各族 (SimpleHunt/SimpleSerialHunt/SimpleCollectItem) 中 max_repeat_count>1 的任务
（这些行在 COMPLETE 态应能重开局，但对应 handler 缺 COMPLETE 分支）。
Audit: rows per family with max_repeat_count>1 (repeatable) whose handler lacks the COMPLETE reopen branch."""
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path("src/main/resources/aion/data/static_data/quest/retail")

def table_ids(path, tag="id"):
    ids = set()
    tree = ET.parse(path)
    for el in tree.getroot():
        if el.tag == tag:
            v = el.get("id")
            if v is not None:
                ids.add(int(v))
    return ids

# quest.xml: id -> max_repeat_count
repeatable = {}
tree = ET.parse(ROOT / "quest.xml")
for q in tree.getroot():
    qid = q.findtext("id")
    if qid is None:
        continue
    mr = q.findtext("max_repeat_count")
    repeatable[int(qid)] = int(mr) if mr else 0

families = {
    "SimpleHunt": table_ids(ROOT / "Quest_SimpleHunt.xml"),
    "SimpleSerialHunt": table_ids(ROOT / "Quest_SimpleSerialHunt.xml"),
    "SimpleCollectItem": table_ids(ROOT / "Quest_SimpleCollectItem.xml"),
}
for name, ids in families.items():
    rep = sorted(i for i in ids if repeatable.get(i, 0) > 1)
    print(f"{name}: rows={len(ids)} repeatable={len(rep)}")
    print("  ids:", rep[:40], "..." if len(rep) > 40 else "")
