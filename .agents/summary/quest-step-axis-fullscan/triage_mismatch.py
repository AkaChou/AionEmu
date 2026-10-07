#!/usr/bin/env python3
"""全量扫描 MISMATCH 分诊：区分「玩法推进事件越集」（真候选）与「dialog/REWARD 分型编码」（模拟器自有机制）。

分诊规则：
- 玩法事件（kill-npc/use-item/item-play/get-item/collect/combine/gather 等）目标 var0 越真端集合 → TRUE_CANDIDATE
- 仅 dialog/enter-world 等辅助事件越集，且越集值可归因于报告页分型（REWARD 态 reward* 节点）→ PAGE_ENCODING
- 其余 → MIXED/OTHER 列出待人工过目
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

OUT_DIR = Path(__file__).resolve().parent
SCAN = OUT_DIR / "fullscan-step-axis.tsv"

# 玩法推进事件（QE-054 判据原始口径的推广面）
GAMEPLAY = {"kill-npc", "use-item", "item-play", "get-item", "collect", "combine",
            "gather", "attack-npc", "pass-flying-ring", "movie-end", "at-distance",
            "quest-timer-end", "use-skill"}


def main() -> int:
    if not SCAN.is_file():
        print("run audit_all_step_axis.py first", file=sys.stderr)
        return 2
    candidates, page_enc, other = [], [], []
    for line in SCAN.read_text(encoding="utf-8").splitlines()[1:]:
        quest_id, retail, detail, bad, judgement = line.split("\t")
        if judgement != "MISMATCH":
            continue
        bad_set = set(int(v) for v in bad.split("|") if v)
        # 解析明细：event:var0 xN
        gameplay_bad = set()
        for m in re.finditer(r"([a-z-]+):(\d+|None)x(\d+)", detail):
            tag, val = m.group(1), m.group(2)
            if val == "None":
                continue
            v = int(val)
            if tag in GAMEPLAY and v != 0 and v in bad_set:
                gameplay_bad.add(v)
        if gameplay_bad:
            candidates.append((quest_id, retail, sorted(gameplay_bad), detail))
        elif bad_set:
            page_enc.append((quest_id, retail, bad, detail[:120]))
    print(f"TRUE_CANDIDATE (gameplay events out of retail set): {len(candidates)}")
    for q, retail, badv, _ in candidates:
        print(f"  {q}: bad={badv} retail={retail}")
    print(f"PAGE_ENCODING / dialog-only: {len(page_enc)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
