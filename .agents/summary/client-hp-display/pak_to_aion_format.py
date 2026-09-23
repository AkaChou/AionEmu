#!/usr/bin/env python3
"""把标准 zip 转成 Aion 5.8 .pak 格式 / Convert a standard zip into Aion 5.8 .pak layout.

Aion 5.8 的 .pak 实质是 zip，但**三处 zip 签名被 XOR 0xFF**：
本地文件头 `PK\\x03\\x04` -> `af b4 fc fb`，中央目录 `PK\\x01\\x02` -> `af b4 fd fe`，
EOCD `PK\\x05\\x06` -> `af b4 fa f9`。其余字节（deflate 流、名称、时间戳）保持原样。

The Aion 5.8 .pak is a zip whose three signatures are XORed with 0xFF; every other
byte (deflate streams, names, timestamps) is untouched. `aion_pak.py pack` emits a
plain zip, so its output must be run through this step before deployment — otherwise
the client sees a foreign container header.

Usage / 用法:
  python3 pak_to_aion_format.py <in.pak> <out.pak>
"""

from __future__ import annotations

import struct
import sys

LOCAL = b"PK\x03\x04"
CENTRAL = b"PK\x01\x02"
EOCD = b"PK\x05\x06"


def flip(data: bytearray, offset: int) -> None:
    """把 4 字节签名 XOR 0xFF。 / XOR the 4-byte signature with 0xFF."""
    data[offset:offset + 4] = bytes(b ^ 0xFF for b in data[offset:offset + 4])


def convert(src: str, dst: str) -> None:
    data = bytearray(open(src, "rb").read())
    size = len(data)

    # 本地文件头链：逐条按结构推进，不依赖字节搜索。
    # Local file header chain: walk by structure rather than by byte search.
    local_offsets: list[int] = []
    pos = 0
    while pos + 30 <= size and bytes(data[pos:pos + 4]) == LOCAL:
        raw_size = struct.unpack_from("<I", data, pos + 18)[0]
        name_len = struct.unpack_from("<H", data, pos + 26)[0]
        extra_len = struct.unpack_from("<H", data, pos + 28)[0]
        if raw_size == 0:
            # 带 data descriptor（流式写入）时本地头的大小字段为 0，无法线性推进；
            # 此时放弃本地头链，只处理中央目录与 EOCD。
            # A zero size means a data descriptor is used; give up on this chain.
            local_offsets = []
            break
        local_offsets.append(pos)
        pos += 30 + name_len + extra_len + raw_size

    # 中央目录：由 EOCD 给出偏移与条目数。
    # Central directory: its offset is taken from the EOCD.
    eocd = data.rfind(EOCD)
    if eocd < 0:
        raise SystemExit("EOCD not found: the input is not a standard zip")
    cd_offset = struct.unpack_from("<I", data, eocd + 16)[0]
    central_offsets: list[int] = []
    pos = cd_offset
    while pos + 46 <= size and bytes(data[pos:pos + 4]) == CENTRAL:
        central_offsets.append(pos)
        name_len = struct.unpack_from("<H", data, pos + 28)[0]
        extra_len = struct.unpack_from("<H", data, pos + 30)[0]
        comment_len = struct.unpack_from("<H", data, pos + 32)[0]
        pos += 46 + name_len + extra_len + comment_len

    for offset in local_offsets:
        flip(data, offset)
    for offset in central_offsets:
        flip(data, offset)
    flip(data, eocd)

    open(dst, "wb").write(data)
    print(f"{src} -> {dst}")
    print(f"  local headers flipped:   {len(local_offsets)}")
    print(f"  central entries flipped: {len(central_offsets)}")
    print(f"  eocd flipped at:         {eocd:#x}")
    print(f"  size: {size} -> {len(data)}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    convert(sys.argv[1], sys.argv[2])
