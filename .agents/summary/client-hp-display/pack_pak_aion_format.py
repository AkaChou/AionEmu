#!/usr/bin/env python3
"""按 Aion 5.8 原生 .pak 格式封包（容器签名 XOR + 载荷前 32 字节前缀 XOR）。
Pack a folder into the native Aion 5.8 .pak layout (XORed signatures + XORed payload prefix).

`aion_pak.py pack` 产出的是**标准 zip**；原生 pak 还有两处变换：

1. 三处 zip 签名 XOR 0xFF（本地头 / 中央目录 / EOCD）；
2. 每个条目**压缩流前 min(32, csize) 字节**再 XOR `AION_XOR_TABLES[version]`，
   表内偏移由 `(csize & 31) * 32`（v1）或 `csize & 1023`（v2）决定。

本脚本以**原始 pak 为模板**读取条目顺序与时间戳/属性，再按上述格式重新封装，
产物与原 pak 结构一致（只换内容）。所有参数（XOR 版本）从原 pak 自动探测。

`aion_pak.py pack` emits a plain zip. Native paks additionally XOR the three zip
signatures and XOR the first min(32, csize) bytes of each compressed payload with
`AION_XOR_TABLES[version]` at an offset derived from the compressed size. This script
mirrors the original pak's entry order/metadata and auto-detects the XOR version.

Usage / 用法:
  python3 pack_pak_aion_format.py <folder> <original.pak> <out.pak>
"""

from __future__ import annotations
import os

import struct
import sys
import zlib
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

sys.path.insert(0, f"{REPO.parent / 'PycharmProjects' / 'unpak'}")
from aionpak.core import _aion_xor_offset  # noqa: E402
from aionpak.aion_tables import AION_XOR_TABLES  # noqa: E402

BASE_ZIP = 0x10000000  # 占位，无实际用途（保持风格一致）
LOCAL = b"PK\x03\x04"
CENTRAL = b"PK\x01\x02"
EOCD = b"PK\x05\x06"


def obf(sig: bytes) -> bytes:
    """把 4 字节 zip 签名 XOR 0xFF / XOR the 4-byte signature with 0xFF."""
    return bytes(b ^ 0xFF for b in sig)


def walk_local(pak: Path):
    """按结构遍历本地文件头链，返回 [(name, method, flags, mtime, mdate, crc, csize, usize, data_off)]。"""
    d = pak.read_bytes()
    out, pos = [], 0
    while pos + 30 <= len(d) and d[pos:pos + 4] == obf(LOCAL):
        (ver, flags, method, mtime, mdate, crc, csize, usize, nlen, elen) = struct.unpack_from("<HHHHHIIIHH", d, pos + 4)
        name = d[pos + 30:pos + 30 + nlen].decode("latin-1")
        data_off = pos + 30 + nlen + elen
        out.append((name, method, flags, mtime, mdate, crc, csize, usize, data_off))
        if csize == 0:
            raise SystemExit("data-descriptor 条目（本地头大小为 0）暂不支持")
        pos = data_off + csize
    return d, out


def detect_version(d: bytes, entries) -> int:
    """用首个 deflate 条目试解，探测载荷前缀 XOR 的版本。"""
    for name, method, _f, _mt, _md, crc, csize, usize, off in entries:
        if method != 8:
            continue
        payload = d[off:off + csize]
        for version in sorted(AION_XOR_TABLES):
            table = AION_XOR_TABLES[version]
            tbl_off = _aion_xor_offset(version, csize)
            cand = bytearray(payload[:32])
            for i in range(len(cand)):
                cand[i] ^= table[tbl_off + i]
            try:
                raw = bytes(cand) + payload[32:]
                out = zlib.decompressobj(-15).decompress(raw)
            except zlib.error:
                continue
            if len(out) == usize and (zlib.crc32(out) & 0xFFFFFFFF) == crc:
                print(f"  探测到 XOR 版本 v{version}（样本 {name}）")
                return version
    raise SystemExit("无法探测 XOR 版本")


def pack(folder: Path, template: Path, out: Path) -> None:
    d, entries = walk_local(template)
    version = detect_version(d, entries)
    files = {p.relative_to(folder).as_posix(): p for p in folder.rglob("*") if p.is_file()}

    body = bytearray()
    central = bytearray()
    written = 0
    for name, _method, flags, mtime, mdate, _crc, _csize, _usize, _off in entries:
        src = files.get(name)
        if src is None:
            raise SystemExit(f"输入目录缺少模板条目: {name}")
        data = src.read_bytes()
        comp = zlib.compressobj(9, zlib.DEFLATED, -15)
        payload = comp.compress(data) + comp.flush()
        csize, usize = len(payload), len(data)
        crc = zlib.crc32(data) & 0xFFFFFFFF

        # 载荷前缀 XOR：只改前 min(32, csize) 字节，压缩流其余部分与头字段不动。
        table = AION_XOR_TABLES[version]
        tbl_off = _aion_xor_offset(version, csize)
        n = min(32, csize)
        payload = bytearray(payload)
        for i in range(n):
            payload[i] ^= table[tbl_off + i]
        payload = bytes(payload)

        nlen = len(name.encode("latin-1"))
        local_off = len(body)
        body += obf(LOCAL) + struct.pack("<HHHHHIIIHH", 20, flags, 8, mtime, mdate, crc, csize, usize, nlen, 0)
        body += name.encode("latin-1") + payload
        # version made by 用 0x0014（DOS），与原生 pak 一致；0x031E 是 Python zipfile 的 Unix 标记。
        # Mirror the original's "version made by" (0x0014, DOS) instead of zipfile's Unix marker.
        central += obf(CENTRAL) + struct.pack("<HHHHHHIIIHHHHHII", 0x0014, 20, flags, 8, mtime, mdate,
                                              crc, csize, usize, nlen, 0, 0, 0, 0, 0x20, local_off)
        central += name.encode("latin-1")
        written += 1

    cd_off = len(body)
    body += central
    body += obf(EOCD) + struct.pack("<HHHHIIH", 0, 0, written, written, len(central), cd_off, 0)
    out.write_bytes(bytes(body))
    print(f"{folder} + {template.name} -> {out}")
    print(f"  条目: {written}  容器: {len(body):,} 字节  XOR 版本: v{version}")


if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    pack(Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]))
