#!/usr/bin/env python3
"""对抽样 quest 检查真端 0x3f0(FINISH_DIALOG) 处理：自定义分支是否存在、是否发页。"""
import re, sys
PATH = '/Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c'
text = open(PATH, encoding='utf-8', errors='replace').read().split('\n')
starts = [(i, m.group(1)) for i, l in enumerate(text) for m in [re.match(r'/\* (180[0-9a-f]{6,}) \*/', l.strip())] if m]
bounds = []
for k, (ln, addr) in enumerate(starts):
    end = starts[k+1][0] if k+1 < len(starts) else len(text)
    bounds.append((addr, ln, end))
for qid in [int(x) for x in sys.argv[1:]]:
    hexid = format(qid, 'x')
    qpat = re.compile(r'0x' + hexid + r'[^0-9a-f]')
    print(f'===== quest {qid} (0x{hexid}) =====')
    hz = False
    for addr, s, e in bounds:
        body = text[s:e]
        if not any(qpat.search(l) for l in body): continue
        f0 = [i for i, l in enumerate(body) if re.search(r'(==|!=)\s*0x3f0\b', l)]
        if not f0: continue
        hz = True
        pages = [l.strip()[:110] for l in body if '0x188' in l and re.search(r',\s*0x' + hexid + r'\)', l)]
        print(f'  FUN_{addr} @L{s+1}-{e}: 0x3f0 分支 {len(f0)} 处 / 发页 {len(pages)} 处')
        for i in f0:
            lo, hi = max(0, i-3), min(len(body), i+14)
            print(f'   >>> L{s+1+i-3}:')
            for j in range(lo, hi): print(f'   {s+1+j}: {body[j]}')
            print('   ----')
        for p in pages[:5]: print('     PAGE:', p)
    if not hz:
        print('  （无自定义 0x3f0 分支 → 走通用口 FUN_180caf150：仅刷新，零发页）')
