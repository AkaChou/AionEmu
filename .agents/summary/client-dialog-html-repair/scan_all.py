# -*- coding: utf-8 -*-
"""全量扫描实机 data.pak 中所有 HTML 的 Aion 解析规则违规。"""
import sys, zipfile
from pathlib import Path
sys.path.insert(0, '.')
from repair_lib import LIVE_PAK
from mixed_content_scan import scan_text
from aionpak.core import decrypt_aion_html_blob

def to_text(blob, name):
    d = decrypt_aion_html_blob(blob, name)
    if d is None:
        d = blob
    if d[:2] in (b'\xff\xfe', b'\xfe\xff') or b'\x00' in d[:64]:
        for enc in ('utf-16', 'utf-16-le', 'utf-16-be'):
            try:
                return d.decode(enc).lstrip('﻿'), enc
            except UnicodeDecodeError:
                continue
    for enc in ('utf-8', 'cp949', 'gb18030'):
        try:
            return d.decode(enc), enc
        except UnicodeDecodeError:
            continue
    return d.decode('utf-8', errors='replace'), 'replace'

def main():
    out = Path('scan_report.txt')
    problems = []
    stats = {'ok': 0, 'mixed': 0, 'syntax': 0, 'decode_error': 0}
    with zipfile.ZipFile(LIVE_PAK) as z:
        names = [n for n in z.namelist() if n.lower().endswith(('.html', '.htm'))]
        for idx, name in enumerate(names, 1):
            try:
                blob = z.read(name)
                text, enc = to_text(blob, name)
            except Exception as e:
                stats['decode_error'] += 1
                problems.append((name, 'read_error', str(e)[:100]))
                continue
            issues = scan_text(text)
            if not issues:
                stats['ok'] += 1
                continue
            kind = issues[0][0]
            stats[kind] = stats.get(kind, 0) + 1
            detail = issues[0][2] if kind == 'mixed' else issues[0][1]
            problems.append((name, kind, str(detail)[:150]))
            if idx % 2000 == 0:
                print(f"...{idx}/{len(names)}", flush=True)
    with out.open('w', encoding='utf-8') as f:
        f.write(f"规模: {len(names)} 个 HTML; 结果: {stats}\n\n")
        for name, kind, detail in problems:
            f.write(f"[{kind}] {name}\n    {detail}\n")
    print("统计:", stats)
    print(f"报告: {out.resolve()}")

main()
