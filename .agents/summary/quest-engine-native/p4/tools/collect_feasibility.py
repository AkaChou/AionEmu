#!/usr/bin/env python3
"""P4 SimpleCollectItem 可行性复算（只读）：对象/NPC/物品符号解析 + 相机 required 对拍。

Evidence sources:
  - <真端根>/Map/XML/Quest_SimpleCollectItem.xml   (family table, UTF-16LE)
  - <真端根>/Map/XML/quest.xml                     (quest metadata, UTF-16LE)
  - <仓库根>/src/main/resources/aion/data/static_data/npcs/npc_template_*.xml
  - <仓库根>/src/main/resources/aion/data/static_data/items/item_template_*.xml (via RetailItemNameIndex)
"""
import glob
import os
import re
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))
TRUE_ROOT = os.environ.get('TRUE_ROOT', '/Users/mc/IdeaProjects/58Server')

def text(path, enc):
    with open(path, encoding=enc, errors='replace') as handle:
        return handle.read()

def rows(blob, tag='quest'):
    return re.findall(r'<%s>(.*?)</%s>' % (tag, tag), blob, re.S)

def index_quests(blob):
    out = {}
    for body in rows(blob):
        m = re.search(r'<id>(\d+)</id>', body)
        if m:
            out[int(m.group(1))] = body
    return out

def main():
    table_blob = text(os.path.join(TRUE_ROOT, 'Map/XML/Quest_SimpleCollectItem.xml'), 'utf-16')
    trows = re.findall(r'<id id="(\d+)">(.*?)</id>', table_blob, re.S)
    quest_blob = text(os.path.join(TRUE_ROOT, 'Map/XML/quest.xml'), 'utf-16')
    meta = index_quests(quest_blob)
    print('collect table rows:', len(trows))
    no_object = [q for q, b in trows if not re.search(r'<object1>', b)]
    no_meta = [q for q, b in trows if int(q) not in meta]
    missing_collect = []
    for q, b in trows:
        mb = meta.get(int(q), '')
        if not re.search(r'<collect_item1>', mb):
            missing_collect.append(q)
    print('rows without object1 :', len(no_object), no_object[:20])
    print('rows without metadata :', len(no_meta), no_meta[:20])
    print('rows without collect_item1:', len(missing_collect), missing_collect[:20])
    widths = {}
    for q, b in trows:
        mb = meta.get(int(q), '')
        counts = [int(c) for c in re.findall(r'<collect_item\d+>\s*\S+\s+(\d+)\s*</collect_item\d+>', mb)]
        if not counts:
            continue
        widths[q] = max(counts)
    over63 = {q: c for q, c in widths.items() if c > 63}
    print('collect quests with counts:', len(widths), 'over 63:', len(over63), dict(list(over63.items())[:10]))

if __name__ == '__main__':
    sys.exit(main())
