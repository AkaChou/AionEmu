#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""通用 MSVC RTTI 虚表扫描（DLL/EXE 通用）。

用途：在宿主进程（Server64.exe / NPCSvr64.exe）里定位"槽 0x1b8"所属的类与实现函数。
用法：
    python3 re_vtable_scan.py <binary> [类名过滤] [--minslots N] [--dump]
"""
import struct
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from m5b2_pe_probe import Pe, load_symbols  # noqa: E402


def build(pe):
    secs = {s[0]: s for s in pe.sections}
    _, text_va, _, _, text_rsize = secs[".text"]
    text_lo, text_hi = pe.image_base + text_va, pe.image_base + text_va + text_rsize
    _, rdata_va, _, _, rdata_rsize = secs[".rdata"]
    rlo, rhi = pe.image_base + rdata_va, pe.image_base + rdata_va + rdata_rsize

    def q(va):
        return pe.u64(va)

    cols = {}
    va = rlo
    while va + 20 <= rhi:
        sig, off, cd, td_rva, ccd_rva = struct.unpack("<IIIII", pe.read(va, 20))
        if sig == 1 and off < 0x10000 and cd < 0x10000 and td_rva < 0x8000000:
            try:
                name = pe.read(pe.image_base + td_rva + 16, 400).split(b"\0")[0].decode("ascii", "replace")
            except Exception:
                name = ""
            if name.startswith(".?A"):
                cols[va] = name
        va += 8

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
    return pe, vts, (text_lo, text_hi)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    binary = sys.argv[1]
    argv = list(sys.argv[2:])
    minslots = 0
    if "--minslots" in argv:
        i = argv.index("--minslots")
        minslots = int(argv[i + 1])
        del argv[i:i + 2]
    args = [a for a in argv if not a.startswith("--")]
    pat = args[0] if args else ""
    dump = "--dump" in sys.argv
    pe = Pe(binary)
    pe, vts, (tlo, thi) = build(pe)
    syms = load_symbols() if "--syms" in sys.argv else {}
    print(f"{binary}: RTTI 虚表 {len(vts)}")
    rows = sorted(vts.items(), key=lambda kv: -kv[1][1])
    for vt, (name, n, colva) in rows:
        if pat and pat.lower() not in name.lower():
            continue
        if n < minslots:
            continue
        slot55 = pe.u64(vt + 0x1B8) if n > 55 else None
        print(f"{vt:#012x} slots={n:4d} col={colva:#x} slot55={slot55 and hex(slot55)}  {name}")
        if dump and slot55:
            for i in range(0x1B8 // 8, min(n, 0x1B8 // 8 + 6)):
                e = pe.u64(vt + i * 8)
                print(f"      slot {i:2d} (+{i*8:#05x}) -> {e:#x} {syms.get(e, '')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
