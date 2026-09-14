#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Coverage analysis: match tail english comments against glossary dictionary."""
import re, glob, json, collections

TERMS = '/Users/mc/IdeaProjects/AionEmu-test/docs/aion-game-terms-en-zh.md'

def norm(s):
    return re.sub(r'[^a-z0-9 ]', '', s.lower()).strip()

# ---- build dictionary from glossary table rows: | English | 中文 | ----
dic = {}
samples = []
lines = open(TERMS, encoding='utf-8').read().split('\n')
for l in lines:
    if not l.startswith('|') or l.startswith('| English'):
        continue
    cells = [c.strip() for c in l.strip().strip('|').split('|')]
    if len(cells) < 2:
        continue
    en, zh = cells[0], cells[1]
    if not en or not zh:
        continue
    key = norm(en)
    if key and len(key) >= 3:
        dic.setdefault(key, zh)
print('glossary entries:', len(dic))

# ---- scan tail comments in java sources ----
def tail_comments(fp):
    res = []
    for ln in open(fp, encoding='utf-8').read().split('\n'):
        # find '//' outside string (approx by quote parity)
        qi = 0
        cut = None
        in_s = False
        for j, ch in enumerate(ln):
            if in_s:
                if ch == '\\':
                    continue
                if ch == '"':
                    in_s = False
            else:
                if ch == '"':
                    in_s = True
                elif ch == '/' and j + 1 < len(ln) and ln[j + 1] == '/':
                    cut = j
                    break
        if cut is None:
            continue
        code = ln[:cut]
        if not code.strip():
            continue  # pure comment line, not a tail comment
        body = ln[cut + 2:].strip()
        if not body:
            continue
        if re.search(r'[\u4e00-\u9fff]', body):
            continue
        if re.match(r'^(https?://|//|\\|/|\$)', body):
            continue
        if re.fullmatch(r'[\-\*_=~#]+', body):
            continue
        if re.match(r'^\d', body) and not re.search(r'[A-Za-z]{2,}', body):
            continue
        res.append((ln, code, body))
    return res

matched = []
unmatched = []
for fp in glob.glob('/Users/mc/IdeaProjects/AionEmu-test/src/main/java/**/*.java', recursive=True):
    for ln, code, body in tail_comments(fp):
        c = norm(body)
        if len(c) < 3:
            unmatched.append((fp, body, 'too-short'))
            continue
        zh = dic.get(c)
        if zh:
            matched.append((fp, body, zh))
        else:
            unmatched.append((fp, body, 'no-hit'))

print('candidates:', len(matched) + len(unmatched))
print('MATCHED:', len(matched))
print('unmatched:', len(unmatched))
# reasons for unmatched
reasons = collections.Counter()
for fp, b, why in unmatched:
    reasons[why] += 1
print('reasons:', dict(reasons))

# unmatched samples grouped by rough shape
shape = collections.Counter()
for fp, b, why in unmatched:
    if re.match(r'^ID[A-Z]', b):
        shape['ID-* prefix'] += 1
    elif re.match(r'^[A-Z][a-z]+\s', b) and re.search(r'\d', b):
        shape['has-digits'] += 1
    else:
        shape['name-like'] += 1
print('unmatched shapes:', dict(shape))
print('\n-- matched samples --')
for fp, b, zh in matched[:20]:
    print('  ', b, '=>', zh)
print('\n-- unmatched name-like samples --')
k = 0
for fp, b, why in unmatched:
    if shape2 := None:
        pass
    if not re.match(r'^ID[A-Z]', b) and not re.search(r'\d', b):
        print('  ', b)
        k += 1
        if k >= 25:
            break
json.dump({'matched': [[f.split('/com/aionemu/')[1], b, zh] for f, b, zh in matched],
           'unmatched': [[f.split('/com/aionemu/')[1], b] for f, b, w in unmatched]},
          open('/tmp/comment_audit/tail_cov.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
