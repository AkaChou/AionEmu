#!/usr/bin/env python3
"""为「迁移时漏掉 <work-items> 声明」的生产任务回填 legacy work items。

权威来源: quest_data.xml 的 <quest_work_items>。
插入位置: XSD metadata 顺序要求 work-items 位于 inventory-items 之后、rewards 之前。

用法: backfill_work_items.py [--write]
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(".").resolve()
QUEST_DATA = ROOT / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml"
DEF_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
CATALOG = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"

# 上一轮审计确认的「无声明且无兜底」任务。1192 单独手工修复。
MISSING = """1218 1319 1322 1324 1345 1371 1422 1452 1464 1469 1483 1484 1527 1528 1634 1636 1643 1721 1724 1725
1909 1918 1963 1964 2239 2667 3036 3037 3041 3085 3086 3087 3088 3319 3921 3933 3934 3935 3938 3939 3970 3973
4004 4011 4501 4542 4732 4921 4938 4939 4943 4966 10010 11008 11010 11012 11026 11068 11072 11077 11103 11107
11116 11117 11118 11139 11455 11458 11460 13800 13951 14122 14152 15000 15301 15302 15303 15304 15305 15306
15545 18301 18302 18303 18808 18954 20010 23951 25301 25302 25303 25304 25305 25545 28213 28301 28302 28303
28601 28808 29000 29002 29008 30055 30202 30210 30213 30302 30308 30710 51020""".split()

legacy = {}
for quest in ET.parse(QUEST_DATA).getroot().iter("quest"):
    node = quest.find("quest_work_items")
    if node is None:
        continue
    items = [(int(w.get("item_id")), int(w.get("count") or 1)) for w in node.findall("quest_work_item")]
    if items:
        legacy[int(quest.get("id"))] = items

ANCHOR = re.compile(
    r"^(?P<indent>[ \t]*)</(?:inventory-items|items|prerequisites|repeat|gender|classes|races)>\s*$"
)

write = "--write" in sys.argv
changed, skipped = [], []
for qid in sorted(int(q) for q in MISSING):
    path = DEF_DIR / f"{qid}.xml"
    if not path.exists():
        skipped.append((qid, "xml missing"))
        continue
    text = path.read_text(encoding="utf-8")
    if "<work-items>" in text:
        skipped.append((qid, "already declared"))
        continue
    lines = text.split("\n")
    # metadata 段的起点
    try:
        md_start = next(i for i, l in enumerate(lines) if "<metadata" in l)
    except StopIteration:
        skipped.append((qid, "no metadata"))
        continue
    # 找 metadata 段内最深/最后的可用锚点
    insert_at = None
    for i in range(md_start, len(lines)):
        if "</metadata>" in lines[i]:
            break
        m = ANCHOR.match(lines[i])
        if m:
            insert_at = i + 1
            insert_indent = m.group("indent") + "  "
    if insert_at is None:
        skipped.append((qid, "no anchor"))
        continue
    block = [f"{insert_indent}<work-items>"]
    for item_id, count in legacy[qid]:
        block.append(f'{insert_indent}  <item id="{item_id}" count="{count}"/>')
    block.append(f"{insert_indent}</work-items>")
    lines[insert_at:insert_at] = block
    if write:
        path.write_text("\n".join(lines), encoding="utf-8")
    changed.append(qid)

print(f"{'WROTE' if write else 'DRY-RUN'}: 回填 {len(changed)} 个任务")
print(f"跳过 {len(skipped)}: {skipped[:20]}")
