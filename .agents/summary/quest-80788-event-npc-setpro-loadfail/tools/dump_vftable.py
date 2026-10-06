#!/usr/bin/env python3
"""从真端 ScriptDLL64.dll 读出指定 VA 处的虚表函数指针列表（只读）。
Read the function-pointer list of a vftable at a given VA from the retail ScriptDLL64.dll (read-only).
用法 / Usage: python3 dump_vftable.py <dll> <vftable_va_hex> [count]
"""
import struct, sys

def pe_sections(path):
    data = open(path, "rb").read()
    e_lfanew = struct.unpack_from("<I", data, 0x3C)[0]
    assert data[e_lfanew:e_lfanew+4] == b"PE\0\0", "not PE"
    coff = e_lfanew + 4
    nsec, = struct.unpack_from("<H", data, coff + 2)
    opt_size, = struct.unpack_from("<H", data, coff + 16)
    opt = coff + 20
    magic, = struct.unpack_from("<H", data, opt)
    assert magic == 0x20B, "not PE32+"
    image_base, = struct.unpack_from("<Q", data, opt + 24)
    sec = opt + opt_size
    out = []
    for i in range(nsec):
        s = sec + i * 40
        name = data[s:s+8].rstrip(b"\0").decode("latin1")
        vsize, vaddr, rawsize, rawptr = struct.unpack_from("<IIII", data, s + 8)
        out.append((name, vaddr, vsize, rawptr, rawsize))
    return data, image_base, out

def va_to_off(image_base, secs, va):
    rva = va - image_base
    for name, vaddr, vsize, rawptr, rawsize in secs:
        if vaddr <= rva < vaddr + max(vsize, rawsize):
            off = rawptr + (rva - vaddr)
            return off
    return None

def main():
    path, va_hex = sys.argv[1], sys.argv[2]
    count = int(sys.argv[3]) if len(sys.argv) > 3 else 24
    data, image_base, secs = pe_sections(path)
    va = int(va_hex, 16)
    off = va_to_off(image_base, secs, va)
    print(f"image_base=0x{image_base:x} va=0x{va:x} file_off=0x{off:x}")
    for i in range(count):
        p, = struct.unpack_from("<Q", data, off + i * 8)
        if p == 0:
            print(f"[{i:3d}] 0x{p:016x}  (null)")
            continue
        foff = va_to_off(image_base, secs, p)
        print(f"[{i:3d}] 0x{p:016x}  file_off=0x{foff:x}" if foff is not None else f"[{i:3d}] 0x{p:016x}")

main()
