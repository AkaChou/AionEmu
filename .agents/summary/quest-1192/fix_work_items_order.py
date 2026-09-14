#!/usr/bin/env python3
"""把误插到 metadata 前部的 <work-items> 块移到 schema 规定的正确位置。

quest_definition.xsd 的 metadata 子元素顺序:
  races -> classes -> gender -> repeat -> prerequisites -> items ->
  inventory-items -> work-items -> rewards/...
work-items 必须紧跟在「前置集合」中最后一个实际存在的元素之后。
前置元素可能是 <x>...</x> 也可能是自闭合的 <x .../>。
"""
import re
import sys
from pathlib import Path

DEF_DIR = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
PRECEDING = ["races", "classes", "gender", "repeat", "prerequisites", "items", "inventory-items"]
BLOCK = re.compile(r"^[ \t]*<work-items>.*?^[ \t]*</work-items>[ \t]*\n?", re.M | re.S)


def preceding_end(body, name):
    """返回前置元素 name 在 body 中的结束偏移（含自闭合形式），不存在则 None。"""
    ends = []
    for m in re.finditer(rf"^[ \t]*</{name}>[ \t]*$", body, re.M):
        ends.append(m.end())
    for m in re.finditer(rf"^[ \t]*<{name}\b[^>]*/>[ \t]*$", body, re.M):
        ends.append(m.end())
    return max(ends) if ends else None


def top_level_indent(body, name):
    """返回该前置块的缩进，用于对齐 work-items。"""
    for m in re.finditer(rf"^([ \t]*)</?{name}\b", body, re.M):
        return m.group(1)
    return "    "


fixed = []
for path in sorted(DEF_DIR.glob("*.xml")):
    text = path.read_text(encoding="utf-8")
    if "<work-items>" not in text:
        continue
    md_start = text.index("<metadata")
    md_end = text.index("</metadata>", md_start)
    head, body, tail = text[:md_start], text[md_start:md_end], text[md_end:]

    match = BLOCK.search(body)
    if not match:
        continue
    block_lines = [l.strip() for l in match.group(0).rstrip("\n").split("\n")]
    body_wo = BLOCK.sub("", body)

    insert_at, indent = None, "    "
    for name in PRECEDING:
        end = preceding_end(body_wo, name)
        if end is not None and (insert_at is None or end > insert_at):
            insert_at, indent = end, top_level_indent(body_wo, name)
    if insert_at is None:
        insert_at, indent = body_wo.index(">") + 1, "    "

    # 按目标缩进重排块
    rebuilt = [f"{indent}<work-items>"]
    for l in block_lines[1:-1]:
        rebuilt.append(f"{indent}  {l}")
    rebuilt.append(f"{indent}</work-items>")

    body_new = body_wo[:insert_at] + "\n" + "\n".join(rebuilt) + body_wo[insert_at:]
    new_text = head + body_new + tail
    if new_text != text:
        path.write_text(new_text, encoding="utf-8")
        fixed.append(path.name)

print(f"重排 {len(fixed)} 个文件: {fixed}")
