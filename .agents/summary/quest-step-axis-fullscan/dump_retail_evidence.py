#!/usr/bin/env python3
"""RISKY 名单真端证据提取：每任务的 0xf0(SetProgress)/0x100(状态推进)/0x110(轴推进)/槽位注册
调用及其上下文，供人工复核「批次抬行 vs 真端轴保持」。

用法：
    python3 .agents/summary/quest-step-axis-fullscan/dump_retail_evidence.py 1626 1636 ...
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())


def resolve_external(candidates: list[Path], probe: str) -> Path:
    for base in candidates:
        if (base / probe).exists():
            return base
    raise SystemExit(f"cannot resolve external root for {probe}")


RETAIL_ROOT = resolve_external(
    [REPO.parent / "58Server", Path.home() / "IdeaProjects" / "58Server"],
    "server58/MainServer_ScriptDLL64/ScriptDLL64.c")
RETAIL_C = RETAIL_ROOT / "server58" / "MainServer_ScriptDLL64" / "ScriptDLL64.c"

CONTEXT = 3  # 前后行数


def main() -> int:
    text = RETAIL_C.read_text(encoding="utf-8", errors="ignore").splitlines()
    for quest_id in sys.argv[1:]:
        q = int(quest_id)
        hex_id = f"0x{q:x}"
        print(f"########## quest {q} ({hex_id}) ##########")
        for i, line in enumerate(text):
            if hex_id not in line:
                continue
            # 只显示推进/注册相关行（0xf0 / 0x100 / 0x110 / cb3070 / cb2eb0）
            if not re.search(r"0xf0\)\)|0x100\)\)|0x110\)\)|FUN_180cb3070|FUN_180cb2eb0", line):
                continue
            lo = max(0, i - CONTEXT)
            hi = min(len(text), i + CONTEXT + 1)
            print(f"--- L{i + 1}")
            for j in range(lo, hi):
                print(f"  {text[j].rstrip()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
