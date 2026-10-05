#!/usr/bin/env python3
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
    for addr, s, e in bounds:
        body = text[s:e]
        if not any(qpat.search(l) for l in body): continue
        pages = [l.strip()[:100] for l in body if '0x188' in l and re.search(r',\s*0x' + hexid + r'\)', l)]
        if not pages and not any(re.search(r'0x271[0-9a-d]\b', l) for l in body) and 'caf3c0' not in body and 'caf350' not in body and 'caf740' not in body and 'cab520' not in body:
            continue
        setpros = sorted(set(re.search(r'0x271([0-9a-d])\b', l).group(1) for l in body if re.search(r'0x271[0-9a-d]\b', l)))
        helpers = [h for h in ('caf3c0','caf350','caf740','cab520','caf5f0') if h in body]
        print(f'  FUN_{addr} @L{s+1}-{e} setpro=[{",".join(setpros)}] helpers={helpers} pages={len(pages)}')
        for p in pages[:8]: print('      P:', p)
