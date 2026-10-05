#!/usr/bin/env python3
"""对给定 quest id 列表，在 ScriptDLL64.c 中定位含该 id 的函数并打印指纹。"""
import re, sys
PATH = '/Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c'
text = open(PATH, encoding='utf-8', errors='replace').read().split('\n')
# 函数切分：形如 /* 180f734b0 */
starts = [(i, m.group(1)) for i, l in enumerate(text) for m in [re.match(r'/\* (180[0-9a-f]{6,}) \*/', l.strip())] if m]
bounds = []
for k, (ln, addr) in enumerate(starts):
    end = starts[k+1][0] if k+1 < len(starts) else len(text)
    bounds.append((addr, ln, end))

def funcs_of(hexid):
    pat = re.compile(r'0x' + hexid + r'\b')
    out = []
    for addr, s, e in bounds:
        body = text[s:e]
        hit = [i for i, l in enumerate(body) if pat.search(l)]
        if not hit: continue
        steps = sorted(set(int(m.group(1), 16) for l in body for m in [re.search(r'0x271([0-9a-d])\b', l)] if m and re.search(r'==\s*0x271', l)))
        pages = [l.strip() for l in body if '0x188' in l and re.search(r',\s*0x' + hexid + r'\)', l)]
        prog  = [l.strip() for l in body if re.search(r'0x(f0|f8)\)\)\([^,]*,0x' + hexid + r',', l)]
        if not steps and not pages and not prog: continue
        out.append({'addr': addr, 'line': s+1, 'end': e, 'steps': steps, 'pages': pages, 'prog': prog})
    return out

for qid in [int(x) for x in sys.argv[1:]]:
    hexid = format(qid, 'x')
    fs = funcs_of(hexid)
    print(f'===== quest {qid} (0x{hexid}) : {len(fs)} handler candidates =====')
    for f in fs:
        print(f"  FUN_{f['addr']} @L{f['line']}-{f['end']} steps={['0x271%x'%s for s in f['steps']]}")
        for p in f['pages'][:6]: print('      page:', p[:110])
        for p in f['prog'][:6]: print('      prog:', p[:110])
