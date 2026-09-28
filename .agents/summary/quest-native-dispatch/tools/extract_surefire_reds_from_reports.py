#!/usr/bin/env python3
"""从 surefire XML 报告提取失败集（逐 testcase，保留参数化下标）。

Extract the failure set from surefire XML reports, one entry per failing testcase.

必须只覆盖本次运行独占的 report 目录；否则历史 XML（含已删类）会污染红集。
`--only-classes <log>` 用同一运行日志里的 `Running <FQN>` / `-- in <FQN>` 过滤，
适合报告目录被多次运行复用的场合（判例：T1）。
Use --only-classes when the report directory is reused across runs.
"""
from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RUNNING = re.compile(r"Running ([\w.$]+)")
IN_CLASS = re.compile(r"-- in ([\w.$]+)")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("reports_dir")
    parser.add_argument("--only-classes", help="Maven log whose Running/-- in classes scope the reports")
    args = parser.parse_args()

    allowed = None
    if args.only_classes:
        allowed = set()
        for line in Path(args.only_classes).read_text(encoding="utf-8", errors="replace").splitlines():
            for m in (RUNNING.search(line), IN_CLASS.search(line)):
                if m:
                    allowed.add(m.group(1))

    reds = set()
    for path in Path(args.reports_dir).glob("TEST-*.xml"):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for tc in root.iter("testcase"):
            classname = tc.get("classname") or ""
            if allowed is not None and classname not in allowed:
                continue
            if tc.find("failure") is not None or tc.find("error") is not None:
                reds.add(f"{classname}.{tc.get('name')}")

    for entry in sorted(reds):
        print(entry)
    return 0


if __name__ == "__main__":
    sys.exit(main())
