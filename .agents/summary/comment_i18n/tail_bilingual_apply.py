#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Append Chinese to tail English-name comments using the glossary dictionary.

Only exact normalized matches are applied. Format becomes:  // 中文 / English原文
Run: python3 apply_tail.py   (writes changes in place; prints stats)
"""
import re, glob, collections, sys

TERMS = '/Users/mc/IdeaProjects/AionEmu-test/docs/aion-game-terms-en-zh.md'
ROOT = '/Users/mc/IdeaProjects/AionEmu-test/src/main/java'

def norm(s):
    return re.sub(r'[^a-z0-9 ]', '', s.lower()).strip()

# ---- dictionary: prefer non-level-form zh for repeated keys ----
rom = re.compile(r'^(.+?)[\s_]*[ivxIVX]+$')
keyvals = collections.defaultdict(list)
for l in open(TERMS, encoding='utf-8').read().split('\n'):
    if not l.startswith('|') or l.startswith('| English'):
        continue
    c = [x.strip() for x in l.strip().strip('|').split('|')]
    if len(c) < 2 or not c[0] or not c[1]:
        continue
    k = norm(c[0])
    if k and len(k) >= 3:
        keyvals[k].append(c[1])

def pick(vals):
    uniq = list(dict.fromkeys(vals))
    plain = []
    for v in uniq:
        m = rom.match(v)
        if not (m and len(m.group(1)) >= 2):
            plain.append(v)
    return plain[0] if plain else uniq[0]

dic = {k: pick(v) for k, v in keyvals.items()}
print('dict keys:', len(dic))

def cut_index(ln):
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
                return j
    return -1

changed = 0
changed_files = set()
for fp in glob.glob(ROOT + '/**/*.java', recursive=True):
    lines = open(fp, encoding='utf-8').read().split('\n')
    out = []
    file_changed = False
    for ln in lines:
        cut = cut_index(ln)
        if cut < 0:
            out.append(ln)
            continue
        code = ln[:cut]
        if not code.strip():
            out.append(ln)  # pure comment line
            continue
        body = ln[cut + 2:].strip()
        if not body:
            out.append(ln)
            continue
        if re.search(r'[\u4e00-\u9fff]', body) or re.match(r'^(https?://|//|\\|/|\$)', body):
            out.append(ln)
            continue
        if re.fullmatch(r'[\-\*_=~#]+', body):
            out.append(ln)
            continue
        # already bilingual ' / '?
        if re.search(r'\s/\s', body):
            out.append(ln)
            continue
        c = norm(body)
        if len(c) < 3:
            out.append(ln)
            continue
        zh = dic.get(c)
        if not zh:
            out.append(ln)
            continue
        out.append(code + '// ' + zh + ' / ' + body)
        changed += 1
        file_changed = True
    if file_changed:
        open(fp, 'w', encoding='utf-8').write('\n'.join(out))
        changed_files.add(fp)
print('applied changes:', changed, 'in', len(changed_files), 'files')
