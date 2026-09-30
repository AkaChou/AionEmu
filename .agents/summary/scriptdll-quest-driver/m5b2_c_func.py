#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""从反编译 ScriptDLL64.c 中按地址取出函数正文。

用法: python3 m5b2_c_func.py <va_hex> [行数]
"""
import os
import bisect
import re
import sys
from pathlib import Path

C_SRC = Path(f"{os.environ.get('AION_RETAIL_ROOT', os.path.expanduser('~/IdeaProjects/58Server'))}/server58/MainServer_ScriptDLL64/ScriptDLL64.c")
_CACHE = {}


def load():
    if "text" in _CACHE:
        return _CACHE
    text = C_SRC.read_text(encoding="utf-8", errors="replace")
    marks = [(m.start(), int(m.group(1), 16)) for m in re.finditer(r"/\* ([0-9a-f]{6,}) \*/", text)]
    _CACHE["text"] = text
    _CACHE["marks"] = marks
    _CACHE["by_va"] = [m[1] for m in marks]
    return _CACHE


def func_at(va):
    c = load()
    i = bisect.bisect_right(c["by_va"], va) - 1
    if i < 0:
        return None
    start = c["marks"][i][0]
    end = c["marks"][i + 1][0] if i + 1 < len(c["marks"]) else len(c["text"])
    return c["by_va"][i], c["text"][start:end]


def main():
    va = int(sys.argv[1], 16)
    n = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    got = func_at(va)
    if not got:
        print("未找到")
        return
    addr, body = got
    print(f"// 包含 {va:#x} 的函数 {addr:#x}")
    if n:
        body = "\n".join(body.splitlines()[:n])
    print(body)


if __name__ == "__main__":
    main()
