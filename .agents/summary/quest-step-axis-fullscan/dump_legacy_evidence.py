#!/usr/bin/env python3
"""RISKY 名单 legacy 证据提取：从 git 历史恢复每个任务的旧引擎 handler，
打印其推进/落盘调用（useQuestItem / checkQuestItems / setQuestVar / defaultCloseDialog / ...）。

用法：
    python3 .agents/summary/quest-step-axis-fullscan/dump_legacy_evidence.py
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

# RISKY 名单（reward-contracts-crosscheck.tsv 的 RISKY 行）
RISKY = [1626, 1636, 2122, 2208, 2284, 2333, 2436, 3056, 3200, 3721, 4502, 4721, 11076, 14051,
         21114, 24022, 24023, 24024, 24025, 24030, 24046, 24051, 30111, 30227, 30327]

# 旧引擎推进/落盘 API 关键字
PATTERN = re.compile(
    r"useQuestItem|checkQuestItems|defaultCloseDialog|setQuestVar|defaultOnKillEvent|"
    r"sendQuestSelectionDialog|closeDialog\(|QuestDialog\.|changeQuestStep|"
    r"STEP_TO_|case [0-9]+:|if \(var ==|if \(env\.getQuestVar|getQuestVars\(\)\.getQuestVar\(\)")


def find_last_blob(path: str) -> tuple[str, str] | None:
    """返回（最后含该文件的提交, 文件内容）；文件从未存在则 None。"""
    revs = subprocess.run(["git", "log", "--all", "--format=%H", "--", path],
                          cwd=REPO, capture_output=True, text=True).stdout.split()
    for h in revs[:40]:
        proc = subprocess.run(["git", "show", f"{h}:{path}"],
                              cwd=REPO, capture_output=True, text=True)
        if proc.returncode == 0:
            return h, proc.stdout
    return None


def main() -> int:
    listing = subprocess.run(["git", "log", "--all", "--format=", "--name-only"],
                             cwd=REPO, capture_output=True, text=True).stdout
    seen = set()
    for line in listing.splitlines():
        m = re.match(r"src/main/java/com/aionemu/gameserver/quest/handlers/([a-z]+)/_(\d{4,5})[A-Z].*\.java$", line)
        if not m:
            continue
        qid = int(m.group(2))
        if qid in RISKY and line not in seen:
            seen.add(line)
    for qid in RISKY:
        paths = sorted(p for p in seen if re.search(rf"/_{qid}[A-Z]", p))
        print(f"########## {qid}")
        if not paths:
            print("  (no legacy class)")
            continue
        for path in paths:
            got = find_last_blob(path)
            if got is None:
                print(f"  {path}: (blob not found)")
                continue
            h, text = got
            print(f"  {Path(path).name} @{h[:9]}")
            for i, line in enumerate(text.splitlines(), 1):
                if PATTERN.search(line):
                    print(f"    L{i}: {line.strip()[:150]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
