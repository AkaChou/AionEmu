#!/usr/bin/env python3
"""Extract a single decompiled function body from the Ghidra dump.

用法: python3 extract_helper_body.py FUN_180cb13b0 [FUN_xxx ...]
默认从 58Server 的 ScriptDLL64.c 读取；可用 --src 覆盖。
The dump is latin-1 encoded; bodies end at the first column-0 '}'.
"""
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

import os
import argparse
import re


DEFAULT_SRC = f"{REPO.parent / '58Server'}/server58/MainServer_ScriptDLL64/ScriptDLL64.c"


def extract(lines, name):
    head = re.compile(r"^[A-Za-z_][\w \*]*\b" + re.escape(name) + r"\(")
    for idx, line in enumerate(lines):
        if not head.match(line):
            continue
        out = []
        for body_line in lines[idx:]:
            out.append(body_line)
            if body_line.startswith("}"):
                break
        return idx + 1, out
    return None, None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("names", nargs="+")
    ap.add_argument("--src", default=DEFAULT_SRC)
    ap.add_argument("--max-lines", type=int, default=0, help="0 = 不截断")
    args = ap.parse_args()

    with open(args.src, encoding="latin-1") as fh:
        lines = fh.read().splitlines()
    for name in args.names:
        line_no, body = extract(lines, name)
        print(f"===== {name} (line {line_no}, {len(body) if body else 0} lines) =====")
        if body is None:
            print("NOT FOUND")
            continue
        shown = body if not args.max_lines else body[: args.max_lines]
        print("\n".join(shown))
        if args.max_lines and len(body) > args.max_lines:
            print(f"... [{len(body) - args.max_lines} lines omitted]")


if __name__ == "__main__":
    main()
