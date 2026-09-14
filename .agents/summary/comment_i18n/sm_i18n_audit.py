#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Analyze SM_SYSTEM_MESSAGE javadoc comments for machine-translation defects.

A comment line is `中文… / English…` (some have @param prefixes, several lines per block).
Defect = the Chinese segment contains English words that clearly leaked from the English
segment (not placeholders like %0/[%SkillName], not capitalized proper nouns already
acceptable, not short abbreviations).
"""
import re, json, collections

FP = 'src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_SYSTEM_MESSAGE.java'
lines = open(FP, encoding='utf-8').read().split('\n')
n = len(lines)

PLACEHOLDER = re.compile(r'%[A-Za-z0-9_]+|\[%[^\]]*\]|@param\s+\w+|/\w+')

def leak_words(zh):
    """english tokens in zh that are likely leak (lowercase words, or known leaked-verb particles)."""
    out = []
    for tok in re.findall(r"[A-Za-z][A-Za-z0-9'\-]*", zh):
        t = tok.lower()
        if len(t) < 2:
            continue
        # skip placeholders-ish tokens
        if re.fullmatch(r'(param|value\d*|id|ids|num\d*|skillname|count|npc|pc|user|l10n|str|msg|ap|gp|cd|st|s|d|atk|def)', t):
            continue
        # skip capitalized proper nouns / acronyms (keep as candidate though if verb-like)
        if tok[0].isupper():
            continue
        out.append(tok)
    return out

def split_zh_en(s):
    s = s.strip()
    # find last ' / ' separator (english usually after it)
    idx = s.rfind(' / ')
    if idx == -1:
        return s, None
    return s[:idx], s[idx+3:]

records = []          # comment block records (all, incl ok)
defects = []
for i in range(n):
    l = lines[i]
    if not l.strip().startswith('*'):
        continue
    body = re.sub(r'^\s*\*\s?', '', l).strip()
    if not body:
        continue
    zh, en = split_zh_en(body)
    if en is None:
        # no '/': bilingual guess impossible; if contains both, flag later
        continue
    if not re.search(r'[\u4e00-\u9fff]', zh):
        continue  # pure english comment (left side not chinese)
    leaks = leak_words(zh)
    if leaks:
        defects.append((i+1, zh, en, leaks))

print('total defective lines:', len(defects))
by_len = collections.Counter()
for _, zh, en, lk in defects:
    by_len[len(lk)] += 1
print('leak-word count distribution:', dict(sorted(by_len.items())))
# samples first 40
for k, (ln, zh, en, lk) in enumerate(defects[:40]):
    print(f'{ln}: [{",".join(lk)}] {zh[:80]} || {en[:60]}')
json.dump([{'line': a, 'zh': b, 'en': c, 'leaks': d} for a, b, c, d in defects],
          open('/tmp/comment_audit/sm_defects.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
