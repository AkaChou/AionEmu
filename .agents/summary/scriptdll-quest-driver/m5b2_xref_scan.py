#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""在 ScriptDLL64.dll 的 .text 中线性反汇编，找出引用指定数据地址（字符串等）的函数。

用法:
    python3 m5b2_xref_scan.py <targets_file>
targets_file 每行形如 `0x0123c218\tDataDrivenQuest - Collect Item`。
输出: 每个目标地址 -> 引用它的函数（symbols.tsv 中的最近符号）与指令地址。
"""
import os
import bisect
import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

sys.path.insert(0, str(Path(__file__).resolve().parent))
from m5b2_pe_probe import Pe, load_symbols  # noqa: E402

DLL = f"{REPO.parent / '58Server'}/MainServer/ScriptDLL64.dll"


def main():
    src = Path(sys.argv[1])
    targets = {}
    for line in src.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        addr, _, desc = line.partition("\t")
        targets[int(addr, 16)] = desc
    print(f"目标地址 {len(targets)} 个")

    pe = Pe(DLL)
    secs = {s[0]: s for s in pe.sections}
    _, tva, _, traw, trs = secs[".text"]
    tlo, thi = pe.image_base + tva, pe.image_base + tva + trs
    syms = load_symbols()
    addrs = sorted(syms)
    import capstone
    md = capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_64)
    md.detail = True
    code = pe.read(tlo, trs)
    hits = {}
    for insn in md.disasm(code, tlo):
        for op in insn.operands:
            if op.type == capstone.x86.X86_OP_MEM and op.mem.base == capstone.x86.X86_REG_RIP:
                tgt = insn.address + insn.size + op.mem.disp
                if tgt in targets:
                    i = bisect.bisect_right(addrs, insn.address) - 1
                    fn = addrs[i] if i >= 0 else 0
                    hits.setdefault(tgt, []).append((fn, insn.address, insn.mnemonic + " " + insn.op_str))
    for tgt, desc in sorted(targets.items(), key=lambda kv: kv[0]):
        hs = hits.get(tgt, [])
        fns = sorted({h[0] for h in hs})
        print(f"\n{tgt:#x} {desc}")
        if not hs:
            print("   (无引用)")
        for fn in fns:
            n = sum(1 for h in hs if h[0] == fn)
            print(f"   {fn:#x} {syms.get(fn,'')}  refs={n}")


if __name__ == "__main__":
    main()
