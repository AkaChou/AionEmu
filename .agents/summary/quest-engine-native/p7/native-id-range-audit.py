#!/usr/bin/env python3
# 真端表/typed 目录的 quest id 范围审计（附近任务轴的 0xFFFF 闸门是否可达）。
# Quest id range audit for the retail tables and the typed catalog (is the 0xFFFF gate reachable?).
import re, sys, pathlib

ROOT = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
NATIVE = ROOT / 'src/main/resources/aion/data/static_data/quest/retail'
TYPED = ROOT / 'src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml'

ROW = re.compile(r'<id id="(\d+)">')

def strip_comments(text):
    return re.sub(r'<!--.*?-->', '', text, flags=re.S)

def scan(path):
    text = strip_comments(path.read_text(encoding='utf-8', errors='replace'))
    return sorted({int(m) for m in ROW.findall(text)})

print('file\tactive_rows\tmax_id\tids_gt_0xffff')
for path in sorted(NATIVE.glob('Quest_*.xml')):
    ids = scan(path)
    over = [i for i in ids if i > 0xFFFF]
    print(f'{path.name}\t{len(ids)}\t{ids[-1] if ids else "-"}\t{len(over)}')
    if over:
        print('   over:', over[:20], '...' if len(over) > 20 else '')

text = TYPED.read_text(encoding='utf-8', errors='replace')
ids = sorted({int(m) for m in re.findall(r'<quest id="(\d+)"', text)})
over = [i for i in ids if i > 0xFFFF]
print(f'typed-catalog\t{len(ids)}\t{ids[-1] if ids else "-"}\t{len(over)}')
if over:
    print('   over:', over[:20], '...' if len(over) > 20 else '')
