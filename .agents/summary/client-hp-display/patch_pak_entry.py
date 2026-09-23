#!/usr/bin/env python3
"""替换 Aion .pak 内的单个 XML 条目，并保持其原始编码。
Replace a single XML entry inside an Aion .pak while preserving its original encoding.

Aion 的本地化 pak（如 `L10N/CHS/Data/data.pak`）把 XML 存成 **UTF-16LE + BOM**，并在声明里写
`encoding="utf-16"`；直接回封 UTF-8 文本客户端很可能不认。`aion_pak.py unpack` 会把它解码成
UTF-8，所以回封时必须把编码和声明都还原。

Aion's localization paks store XML as UTF-16LE with a BOM and declare
`encoding="utf-16"`; `aion_pak.py unpack` decodes that to UTF-8, so the encoding and
the declaration must both be restored on the way back in.

与 `aion_pak.py pack` 不同，本脚本**只改指定条目**：其余条目逐字节复制，不会被改写或重新压缩。
Unlike `aion_pak.py pack`, only the named entry is touched — every other entry is
copied byte-for-byte.

Usage / 用法:
  python3 patch_pak_entry.py <pak> <entry-name> <decoded-utf8-file> <out.pak> [utf-16|utf-8]
"""

from __future__ import annotations

import re
import sys
import zipfile

ENCODING_DECLARATION = re.compile(r'encoding="[^"]*"')


def patch_entry(pak: str, entry: str, decoded: str, out: str, encoding: str = "utf-16") -> None:
    # newline="" 保留原始行尾（这些 XML 是 CRLF）。
    # newline="" keeps the original CRLF line endings intact.
    text = open(decoded, encoding="utf-8", newline="").read()
    text, count = ENCODING_DECLARATION.subn(f'encoding="{encoding}"', text, count=1)
    if count != 1:
        raise SystemExit("the XML declaration has no encoding attribute to rewrite")

    payload = b"\xff\xfe" + text.encode("utf-16-le") if encoding == "utf-16" else text.encode("utf-8")

    replaced = 0
    with zipfile.ZipFile(pak) as zin, zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zout:
        for info in zin.infolist():
            data = zin.read(info.filename)
            if info.filename == entry:
                data = payload
                replaced += 1
            # 逐条重建 ZipInfo，保留时间戳/属性/压缩方式。
            # Rebuild each ZipInfo so timestamps, attributes and method survive.
            copy = zipfile.ZipInfo(info.filename, date_time=info.date_time)
            copy.compress_type = info.compress_type
            copy.external_attr = info.external_attr
            copy.internal_attr = info.internal_attr
            copy.create_system = info.create_system
            zout.writestr(copy, data)

    if replaced != 1:
        raise SystemExit(f"expected exactly one match for {entry!r}, found {replaced}")
    print(f"{pak} -> {out}")
    print(f"  replaced: {entry}")
    print(f"  payload:  {len(payload)} bytes, encoding={encoding}")


if __name__ == "__main__":
    if len(sys.argv) not in (5, 6):
        raise SystemExit(__doc__)
    patch_entry(*sys.argv[1:])
