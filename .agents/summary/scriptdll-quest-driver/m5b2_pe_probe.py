#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PE/vtable/反汇编探针：用于 58 真端 ScriptDLL64.dll 的虚槽反查。

用法:
    python3 m5b2_pe_probe.py disasm <va_hex> [n]
    python3 m5b2_pe_probe.py vtable <va_hex> [n]
    python3 m5b2_pe_probe.py func <va_hex>      # 打印函数首部反汇编
"""
import struct
import sys
from pathlib import Path

DLL = Path("/Users/mc/IdeaProjects/58Server/MainServer/ScriptDLL64.dll")
SYMS = Path("/Users/mc/IdeaProjects/58Server/server58-source/MainServer_ScriptDLL64/symbols.tsv")


class Pe:
    def __init__(self, path):
        self.data = Path(path).read_bytes()
        d = self.data
        e = struct.unpack_from("<I", d, 0x3C)[0]
        self.image_base = struct.unpack_from("<Q", d, e + 24 + 24)[0]
        nsec = struct.unpack_from("<H", d, e + 6)[0]
        optsize = struct.unpack_from("<H", d, e + 20)[0]
        secoff = e + 24 + optsize
        self.sections = []
        for i in range(nsec):
            base = secoff + i * 40
            name = d[base:base + 8].rstrip(b"\0").decode()
            vsize, vaddr, rsize, raddr = struct.unpack_from("<IIII", d, base + 8)
            self.sections.append((name, vaddr, vsize, raddr, rsize))

    def rva2off(self, rva):
        for name, va, vsize, raw, rsize in self.sections:
            if va <= rva < va + max(vsize, rsize):
                return raw + (rva - va)
        raise ValueError(f"rva {rva:#x} not mapped")

    def read(self, va, n):
        off = self.rva2off(va - self.image_base)
        return self.data[off:off + n]

    def u64(self, va):
        return struct.unpack("<Q", self.read(va, 8))[0]

    def u32(self, va):
        return struct.unpack("<I", self.read(va, 4))[0]


def load_symbols():
    m = {}
    for line in SYMS.read_text(encoding="utf-8", errors="replace").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        addr = parts[0].strip()
        if not addr:
            continue
        try:
            m[int(addr, 16)] = parts[2]
        except ValueError:
            continue
    return m


def main():
    pe = Pe(DLL)
    cmd = sys.argv[1]
    va = int(sys.argv[2], 16)
    n = int(sys.argv[3]) if len(sys.argv) > 3 else 24

    if cmd == "vtable":
        syms = load_symbols()
        print(f"vtable@{va:#x}")
        for i in range(n):
            ent = pe.u64(va + i * 8)
            name = syms.get(ent, "")
            print(f"  [+{i*8:#05x}] slot {i:2d} -> {ent:#x} {name}")
        return

    import capstone
    md = capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_64)
    code = pe.read(va, 8 * n)
    for insn in md.disasm(code, va):
        print(f"{insn.address:#x}: {insn.mnemonic}\t{insn.op_str}")
        n -= 1
        if n <= 0:
            break


if __name__ == "__main__":
    main()
