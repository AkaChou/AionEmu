# -*- coding: utf-8 -*-
"""npcfuncs 专项扫描：模拟 Aion「读元素值，both cdata and child element」报错规则。
判据（对每个 <npcfuncs> 元素）：
  - 有子元素 且 有非空白直接文本（自身 text 或子元素 tail）→ mixed（会触发 both cdata 报错）
  - ET 语法错误 → syntax（单独记录，可能触发其他解析错误）
"""
import sys, zipfile
import xml.etree.ElementTree as ET
from pathlib import Path
sys.path.insert(0, '.')
from repair_lib import LIVE_PAK
from aionpak.core import decrypt_aion_html_blob

def to_text(blob, name):
    d = decrypt_aion_html_blob(blob, name) or blob
    if d[:2] in (b'\xff\xfe', b'\xfe\xff'):
        return d.decode('utf-16').lstrip('﻿')
    return d.decode('utf-8', errors='replace')

def check(text):
    issues = []
    try:
        root = ET.fromstring(text)
    except ET.ParseError as e:
        return [('syntax', str(e).replace('\n', ' ')[:120])]
    for elem in root.iter():
        if elem.tag.rsplit('}', 1)[-1] != 'npcfuncs':
            continue
        direct = []
        if elem.text and elem.text.strip():
            direct.append(elem.text.strip()[:60])
        for c in elem:
            if c.tail and c.tail.strip():
                direct.append(c.tail.strip()[:60])
        if direct and len(elem) > 0:
            issues.append(('mixed', f"直接文本={direct}"))
    return issues

def main():
    out = Path('npcfuncs_report.txt')
    mixed, syntax = [], []
    with zipfile.ZipFile(LIVE_PAK) as z:
        names = [n for n in z.namelist() if n.lower().endswith(('.html', '.htm'))]
        for name in names:
            try:
                text = to_text(z.read(name), name)
            except Exception:
                continue
            if '<npcfuncs' not in text:
                continue
            for kind, detail in check(text):
                if kind == 'mixed':
                    mixed.append((name, detail))
                elif kind == 'syntax':
                    syntax.append((name, detail))
    with out.open('w', encoding='utf-8') as f:
        f.write(f"npcfuncs mixed: {len(mixed)}\n")
        for n, d in mixed:
            f.write(f"[mixed] {n}\n    {d}\n")
        f.write(f"\n含有 npcfuncs 但 ET 语法报错: {len(syntax)}\n")
        for n, d in syntax:
            f.write(f"[syntax] {n}\n    {d}\n")
    print(f"npcfuncs mixed: {len(mixed)}")
    print(f"npcfuncs 语法报错: {len(syntax)}")
    for n, d in mixed[:30]:
        print(f"  [mixed] {n} | {d[:80]}")
    for n, d in syntax[:30]:
        print(f"  [syntax] {n} | {d[:80]}")

if __name__ == "__main__":
    main()
