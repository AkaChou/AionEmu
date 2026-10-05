#!/usr/bin/env python3
"""对 (quest, SETPROn) 列表：打印真端分支上下文 + 该函数发页/推进指纹。"""
import re, sys, json
PATH = '/Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c'
text = open(PATH, encoding='utf-8', errors='replace').read().split('\n')
starts = [(i, m.group(1)) for i, l in enumerate(text) for m in [re.match(r'/\* (180[0-9a-f]{6,}) \*/', l.strip())] if m]
bounds = []
for k, (ln, addr) in enumerate(starts):
    end = starts[k+1][0] if k+1 < len(starts) else len(text)
    bounds.append((addr, ln, end))

TARGETS = json.load(open(sys.argv[1]))   # {"1007": [3], ...}
for qid_s, steps in TARGETS.items():
    qid = int(qid_s); hexid = format(qid, 'x')
    qpat = re.compile(r'0x' + hexid + r'[^0-9a-f]')
    print(f'\n########## quest {qid} (0x{hexid}) target SETPRO{steps} ##########')
    for stepn in steps:
        act = 0x270f + stepn
        act_hex = f'0x{act:x}'; act_dec = str(10000 + stepn - 1)
        found = False
        for addr, s, e in bounds:
            body = text[s:e]
            if not any(qpat.search(l) for l in body): continue
            idxs = [i for i, l in enumerate(body) if re.search(r'==\s*(' + act_hex + r'|' + act_dec + r')\b', l)]
            if not idxs: continue
            found = True
            pages = [l.strip()[:120] for l in body if '0x188' in l and re.search(r',\s*0x' + hexid + r'\)', l)]
            prog = [l.strip()[:120] for l in body if re.search(r'0x(f0|f8)\)\)\([^,]*,0x' + hexid + r',', l)]
            print(f'-- FUN_{addr} @L{s+1}-{e}: 函数发页 {len(pages)} 处 / 推进 {len(prog)} 处')
            for p in pages: print('     PAGE:', p)
            for p in prog: print('     PROG:', p)
            for i in idxs:
                lo, hi = max(0, i-6), min(len(body), i+18)
                print(f'   >>> 分支上下文 L{s+1+i-6}..:')
                for j in range(lo, hi):
                    print(f'   {s+1+j}: {body[j]}')
                print('   ----')
        if not found: print(f'!! SETPRO{stepn} ({act_hex}) 未找到含 quest 引用的分支')
