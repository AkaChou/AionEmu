#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""扫描 .text 中对指定地址区间（字符串表等）的 RIP 相对引用，输出 xref 明细。"""
import os
import bisect
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from m5b2_pe_probe import Pe, load_symbols  # noqa: E402

DLL = f"{os.environ.get('AION_RETAIL_ROOT', os.path.expanduser('~/IdeaProjects/58Server'))}/MainServer/ScriptDLL64.dll"


def main():
    lo, hi = int(sys.argv[1], 16), int(sys.argv[2], 16)
    pe = Pe(DLL)
    secs = {s[0]: s for s in pe.sections}
    _, tva, _, _, trs = secs[".text"]
    tlo = pe.image_base + tva
    syms = load_symbols()
    addrs = sorted(syms)
    import capstone
    md = capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_64)
    md.detail = True
    out = []
    for insn in md.disasm(pe.read(tlo, trs), tlo):
        for op in insn.operands:
            if op.type == capstone.x86.X86_OP_MEM and op.mem.base == capstone.x86.X86_REG_RIP:
                tgt = insn.address + insn.size + op.mem.disp
                if lo <= tgt <= hi:
                    i = bisect.bisect_right(addrs, insn.address) - 1
                    fn = addrs[i] if i >= 0 else 0
                    out.append((fn, insn.address, tgt, insn.mnemonic + " " + insn.op_str))
    groups = {}
    for fn, addr, tgt, txt in out:
        groups.setdefault(fn, []).append((addr, tgt, txt))
    print(f"引用 [{lo:#x},{hi:#x}] 的函数 {len(groups)} 个，共 {len(out)} 处")
    for fn in sorted(groups):
        print(f"\n=== {fn:#x} {syms.get(fn,'')} ({len(groups[fn])} refs)")
        for addr, tgt, txt in groups[fn][:40]:
            print(f"   {addr:#x} -> {tgt:#x}  {txt}")


if __name__ == "__main__":
    main()
