# -*- coding: utf-8 -*-
"""从实机 data.pak 重建修复后的 pak（离线输出到 /tmp，不触碰实机文件）。"""
import sys, hashlib
from functools import partial
from pathlib import Path
sys.path.insert(0, '.')
from repair_lib import LIVE_PAK, repair_pak
from fixes import FIXES, apply_fix

DST = Path('/tmp/data.pak.repaired')

def fix_fn(entry, text):
    return apply_fix(entry, text)

def sha256(p, limit=None):
    h = hashlib.sha256()
    with open(p, 'rb') as f:
        while chunk := f.read(1 << 20):
            h.update(chunk)
    return h.hexdigest()

print(f"源: {LIVE_PAK} ({LIVE_PAK.stat().st_size:,} bytes)")
fixes = {e: partial(fix_fn, e) for e in FIXES}
repair_pak(LIVE_PAK, DST, fixes)
print(f"输出: {DST} ({DST.stat().st_size:,} bytes)")
print(f"源 SHA-256:   {sha256(LIVE_PAK)}")
print(f"新包 SHA-256: {sha256(DST)}")
