#!/usr/bin/env python3
"""从真端 world_N.xml 中提取 Specter（Ahbana）相关的 territory/生成组定义。

Extract the retail Specter (Ahbana) territory/spawn-group rows from the
UTF-16 world_N.xml so the emu condition-spawn projection can be written
from first-hand evidence.

用法 / Usage:
    python3 extract_specter_rows.py <retail world_N.xml> [needle ...]
"""

import re
import sys

DEFAULT_NEEDLES = ["Specter", "Spectre"]


def main(path, needles=DEFAULT_NEEDLES):
    with open(path, encoding="utf-16") as handle:
        lines = handle.read().split("\n")
    hits = []
    for number, line in enumerate(lines, 1):
        if any(needle in line for needle in needles):
            hits.append((number, line.rstrip("\r")))
    print(f"{len(hits)} line(s) matching {needles}")
    for number, line in hits:
        print(f"{number}: {line}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2:] or DEFAULT_NEEDLES)
