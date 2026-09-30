#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""扫描 ScriptDLL64.dll 的 MSVC RTTI：CompleteObjectLocator -> 虚表 -> 各槽实现。

用途：定位 trampoline 里 `(**(code **)(lVar1 + 0x1b8))` 所属类与具体实现函数。

用法:
    python3 m5b2_vtable_scan.py [类名过滤] [--dump+0x1b8]
"""
import os
import struct
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from m5b2_pe_probe import Pe, load_symbols  # noqa: E402

DLL = f"{os.environ.get('AION_RETAIL_ROOT', os.path.expanduser('~/IdeaProjects/58Server'))}/MainServer/ScriptDLL64.dll"


def build(pe):
    secs = {s[0]: s for s in pe.sections}
    _, text_va, _, _, text_rsize = secs[".text"]
    text_lo, text_hi = pe.image_base + text_va, pe.image_base + text_va + text_rsize
    _, rdata_va, _, _, rdata_rsize = secs[".rdata"]
    rlo, rhi = pe.image_base + rdata_va, pe.image_base + rdata_va + rdata_rsize

    def q(va):
        return pe.u64(va)

    # 1) 枚举 CompleteObjectLocator -> 类名
    cols = {}
    va = rlo
    while va + 20 <= rhi:
        sig, off, cd, td_rva, ccd_rva = struct.unpack("<IIIII", pe.read(va, 20))
        if sig == 1 and off < 0x10000 and cd < 0x10000 and td_rva < 0x6000000:
            try:
                name = pe.read(pe.image_base + td_rva + 16, 300).split(b"\0")[0].decode("ascii", "replace")
            except Exception:
                name = ""
            if name.startswith(".?A"):
                cols[va] = name
        va += 8

    # 2) 在 .rdata 中找指向 COL 的指针 -> 其下一个 qword 即虚表首槽
    vts = {}
    va = rlo
    while va + 8 <= rhi:
        v = q(va)
        if v in cols:
            n = 0
            while va + 8 + n * 8 < rhi and text_lo <= q(va + 8 + n * 8) < text_hi:
                n += 1
            vts[va + 8] = (cols[v], n, va)
        va += 8
    return pe, vts


def main():
    pe = Pe(DLL)
    pe, vts = build(pe)
    syms = load_symbols()
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    pat = args[0] if args else ""
    dump = "--dump" in sys.argv
    print(f"RTTI 虚表总数 {len(vts)}")
    for vt, (name, n, colva) in sorted(vts.items()):
        if pat and pat not in name:
            continue
        slot55 = pe.u64(vt + 0x1B8) if n > 55 else None
        print(f"{vt:#012x} slots={n:4d} col={colva:#x} slot55={slot55 and hex(slot55)}  {name}")
        if dump and slot55:
            for i in range(0x1B8 // 8, min(n, 0x1B8 // 8 + 6)):
                e = pe.u64(vt + i * 8)
                print(f"      slot {i:2d} (+{i*8:#05x}) -> {e:#x} {syms.get(e,'')}")


if __name__ == "__main__":
    main()
