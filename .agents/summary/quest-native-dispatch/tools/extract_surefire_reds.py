#!/usr/bin/env python3
"""从 run_quest_gates.sh 的 Maven 日志提取失败集（全限定类名.方法名）。

Extract the failure set (fully-qualified Class.method) from a run_quest_gates.sh Maven log.
Maven 的 Results 摘要用短类名，脚本用同一日志的 `Running <FQN>` / `-- in <FQN>` 行还原全限定名。
Maven prints short class names in the summary; this restores FQNs from the same log.
"""
from __future__ import annotations

import re
import sys

RUNNING = re.compile(r"Running ([\w.$]+)")
IN_CLASS = re.compile(r"-- in ([\w.$]+)")
SUMMARY = re.compile(r"^\[ERROR\]\s{2,}(\S+)")


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2
    lines = open(sys.argv[1], encoding="utf-8", errors="replace").read().splitlines()

    simple_to_fqn: dict[str, str] = {}
    for line in lines:
        for m in (RUNNING.search(line), IN_CLASS.search(line)):
            if m:
                fqn = m.group(1)
                simple_to_fqn[fqn.rsplit(".", 1)[-1]] = fqn

    reds: set[str] = set()
    in_reds = False
    for line in lines:
        if line.startswith("[ERROR] Failures:") or line.startswith("[ERROR] Errors:"):
            in_reds = True
            continue
        if in_reds:
            if line == "[ERROR] " or line == "[ERROR]":
                in_reds = False
                continue
            m = SUMMARY.match(line)
            if not m:
                continue
            # 行号/断言链都跟在第一个 ":" 之后；参数化下标 "()[1]" 不含 ":"
            token = m.group(1).split(":", 1)[0]
            simple, _, method = token.partition(".")
            fqn = simple_to_fqn.get(simple, simple)
            reds.add(f"{fqn}.{method}" if method else fqn)

    for entry in sorted(reds):
        print(entry)
    return 0


if __name__ == "__main__":
    sys.exit(main())
