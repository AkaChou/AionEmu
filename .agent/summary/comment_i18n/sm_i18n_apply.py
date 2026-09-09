#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Apply new Chinese segments for SM_SYSTEM_MESSAGE defects.

usage: python3 fix_sm.py <mapping.json>
mapping: { "<line>": "new Chinese text (without leading '*', trailing ' / english')" }
The comment line is `\s*\* 中文 / English`. Left side is replaced entirely.
"""
import json, re, sys

FP = 'src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java'
mapping = json.load(open(sys.argv[1], encoding='utf-8'))
lines = open(FP, encoding='utf-8').read().split('\n')

sep_re = re.compile(r'\s/\s')
done = 0
for ln, new_zh in mapping.items():
    i = int(ln) - 1
    orig = lines[i]
    m = sep_re.search(orig)
    if not m:
        print(f'no " / " separator at line {ln}: {orig!r}')
        sys.exit(1)
    head = orig[:m.start()]
    en = orig[m.end():]
    # head = indentation + '* ...' ; keep everything up to first non-* non-space text
    hm = re.match(r'^(\s*\*+\s*)(.*)$', head)
    lead, old_zh = hm.group(1), hm.group(2)
    # If old_zh is empty (already fixed) skip silently but count
    lines[i] = f'{lead}{new_zh} / {en}'
    done += 1
open(FP, 'w', encoding='utf-8').write('\n'.join(lines))
print('applied', done)
