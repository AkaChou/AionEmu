#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""暴力扫描 .text 中 RIP 相对 disp32 引用（不依赖线性反汇编同步），按目标区间过滤。

用法: python3 m5b2_refscan.py <lo_hex> <hi_hex> [--fn]
  --fn 额外用 ScriptDLL64.c 的函数边界归组
"""
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from m5b2_pe_probe import Pe  # noqa: E402


def main():
    lo, hi = int(sys.argv[1], 16), int(sys.argv[2], 16)
    pe = Pe(f"{os.environ.get('AION_RETAIL_ROOT', os.path.expanduser('~/IdeaProjects/58Server'))}/MainServer/ScriptDLL64.dll")
    secs = {s[0]: s for s in pe.sections}
    _, tva, _, _, trs = secs[".text"]
    tlo = pe.image_base + tva
    blob = pe.read(tlo, trs)
    hits = []
    n = trs - 4
    for p in range(n):
        d = int.from_bytes(blob[p:p + 4], "little")
        # 目标 = (tlo + p + 4) + d  （disp32 末尾即 RIP）
        tgt = tlo + p + 4 + d
        if lo <= tgt < hi:
            hits.append((tgt, tlo + p))
    print(f"命中 {len(hits)} 处 disp32 -> [{lo:#x},{hi:#x})")
    by_tgt = {}
    for tgt, movaddr in hits:
        by_tgt.setdefault(tgt, []).append(movaddr)
    for tgt in sorted(by_tgt):
        addrs = by_tgt[tgt]
        print(f"  target {tgt:#x}  引用点 {len(addrs)}: " + ", ".join(hex(a) for a in addrs[:8]))


if __name__ == "__main__":
    main()
