# -*- coding: utf-8 -*-
"""验证修复后的 pak: A. 4 个目标条目修复正确; B. 其余条目逐字节等价。"""
import sys, zipfile, difflib
from pathlib import Path
sys.path.insert(0, '.')
from repair_lib import LIVE_PAK
from fixes import FIXES
from npcfuncs_scan import check, to_text

DST = Path('/tmp/data.pak.repaired')

print("=== A. 目标条目修复验证 ===")
with zipfile.ZipFile(DST) as z:
    for e in FIXES:
        text = to_text(z.read(e), e)
        issues = check(text)
        status = "干净 ✓" if not issues else f"仍有问题: {issues}"
        print(f"  {e.split('/')[-1]}: {status}")
        for line in text.split('\n'):
            if '<npcfuncs' in line or 'stigma_enchant' in line:
                print(f"      {line.strip()[:110]}")

print("\n=== B. 全条目等价验证 ===")
with zipfile.ZipFile(LIVE_PAK) as zo, zipfile.ZipFile(DST) as zn:
    oi = zo.infolist(); ni = zn.infolist()
    print(f"条目数: 源 {len(oi)} / 新 {len(ni)}  顺序与名称一致: {[i.filename for i in oi]==[i.filename for i in ni]}")
    same = diff = 0
    diffs = []
    for a, b in zip(oi, ni):
        da, db = zo.read(a.filename), zn.read(b.filename)
        if da == db:
            same += 1
        else:
            diff += 1
            diffs.append(a.filename)
    print(f"逐字节相同: {same}; 不同: {diff}")
    for d in diffs:
        assert d in FIXES, f"意外差异条目: {d}"
        print(f"  预期内差异: {d}")
    # 对 4 个目标条目做文本级 diff（确认只差修复点）
    print("\n=== 目标条目文本级 diff（只应出现修复点）===")
    for e in FIXES:
        old_t = to_text(zo.read(e), e)
        new_t = to_text(zn.read(e), e)
        d = list(difflib.unified_diff(old_t.splitlines(), new_t.splitlines(), lineterm='', n=0))
        print(f"--- {e.split('/')[-1]} ({sum(1 for l in d if l.startswith(('+','-')) and not l.startswith(('+++','---')))} 行变化)")
        for l in d[2:]:
            if l.startswith(('+', '-')):
                print(f"    {l[:130]}")
