#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-11：物品 name_desc → item_id 索引生成（builder 的 collect_item 通道依赖）。

与生产 RetailItemNameIndex 同口径：name_desc 小写键、首个命中保留（真端同名同 id）。
用法：python3 -B p0c11_build_item_name_index.py
"""
import glob
import re
from collections import OrderedDict
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
FILES = sorted(glob.glob(str(REPO / 'src/main/resources/aion/data/static_data/items/item/item_template_*.xml')))
TEMPLATE = re.compile(r'<item_template\b[^>]*>')
NAME = re.compile(r'name_desc="([^"]*)"')
ID = re.compile(r'\bid="(\d+)"')


def main():
    out = OrderedDict()
    for path in FILES:
        text = open(path, encoding='utf-8', errors='ignore').read()
        for match in TEMPLATE.finditer(text):
            tag = match.group()
            name = NAME.search(tag)
            item_id = ID.search(tag)
            if name and item_id:
                key = name.group(1).lower()
                out.setdefault(key, item_id.group(1))
    lines = ['# item name_desc → item_id（首个命中；与 RetailItemNameIndex 同口径）',
        '# 由 p0c11_build_item_name_index.py 生成', 'name_desc\titem_id']
    lines += ['%s\t%s' % (k, v) for k, v in out.items()]
    (HERE / 'item_name_index.tsv').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print('entries:', len(out), '→ item_name_index.tsv')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
