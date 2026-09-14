#!/usr/bin/env python3
"""补齐剩余任务(EXPLICIT + PARTIAL 类)的 <work-items> 声明, 对齐 quest_data.xml。

排除 5 个 MISMATCH 类, 它们声明的内容与 legacy 不一致(物品集不同或重复), 需逐任务人工核对。
用法: backfill_remaining_work_items.py [--write]
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(".").resolve()
DEF_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
CATALOG = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"

# 内容与 legacy 不一致, 不自动改: 4942 物品集完全不同; 19026/19032/29026/29032 含重复条目且顺序不同。
EXCLUDE = {4942, 19026, 19032, 29026, 29032}
# 仅为 legacy 顺序不同(1131/19008), 集合一致, 按集合比较已通过, 跳过改写以保持最小 diff。
SKIP_ORDER_ONLY = {1131, 19008}

legacy = {}
for quest in ET.parse(ROOT / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml").getroot().iter("quest"):
    node = quest.find("quest_work_items")
    if node is None:
        continue
    items = [(int(w.get("item_id")), int(w.get("count") or 1)) for w in node.findall("quest_work_item")]
    if items:
        legacy[int(quest.get("id"))] = items

executable = {int(d.get("id")) for d in ET.parse(CATALOG).getroot().iter("definition")
              if d.get("mode") == "EXECUTABLE"}

PRECEDING = ["races", "classes", "gender", "repeat", "prerequisites", "items", "inventory-items"]
BLOCK = re.compile(r"^[ \t]*<work-items>.*?^[ \t]*</work-items>[ \t]*\n?", re.M | re.S)
write = "--write" in sys.argv

def preceding_end(body, name):
    ends = [m.end() for m in re.finditer(rf"^[ \t]*</{name}>[ \t]*$", body, re.M)]
    ends += [m.end() for m in re.finditer(rf"^[ \t]*<{name}\b[^>]*/>[ \t]*$", body, re.M)]
    return max(ends) if ends else None

def top_level_indent(body, name):
    for m in re.finditer(rf"^([ \t]*)</?{name}\b", body, re.M):
        return m.group(1)
    return "    "

changed, skipped = [], []
for qid, items in sorted(legacy.items()):
    if qid not in executable or qid in EXCLUDE or qid in SKIP_ORDER_ONLY:
        continue
    path = DEF_DIR / f"{qid}.xml"
    if not path.exists():
        continue
    text = path.read_text(encoding="utf-8")
    md_start = text.index("<metadata")
    md_end = text.index("</metadata>", md_start)
    head, body, tail = text[:md_start], text[md_start:md_end], text[md_end:]

    existing = BLOCK.search(body)
    if existing:
        declared = [(int(m.group(1)), int(m.group(2))) for m in re.finditer(
            r'<item\s+id="(\d+)"\s+count="(\d+)"', existing.group(0))]
        if sorted(declared) == sorted(items):
            continue  # 已正确声明
        skipped.append((qid, "declared differs", declared, items))
        continue

    insert_at, indent = None, "    "
    for name in PRECEDING:
        end = preceding_end(body, name)
        if end is not None and (insert_at is None or end > insert_at):
            insert_at, indent = end, top_level_indent(body, name)
    if insert_at is None:
        insert_at, indent = body.index(">") + 1, "    "

    block = [f"{indent}<work-items>"]
    block += [f'{indent}  <item id="{i}" count="{c}"/>' for i, c in items]
    block.append(f"{indent}</work-items>")
    new_text = head + body[:insert_at] + "\n" + "\n".join(block) + body[insert_at:] + tail
    if write:
        path.write_text(new_text, encoding="utf-8")
    changed.append(qid)

print(f"{'WROTE' if write else 'DRY-RUN'}: 回填 {len(changed)} 个任务")
print("  " + " ".join(map(str, changed)))
if skipped:
    print(f"跳过 {len(skipped)}:")
    for s in skipped:
        print(f"  {s}")
