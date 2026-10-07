#!/usr/bin/env python3
"""遗留问题 C 组扫查：两份批次 CONTRACTS 名单（第三批 + 镜像批）在册任务
的 reward 投影与客户端任务书行数（quest_client_summary_rows.tsv）比对。

判据（新维度，补「真端无常量」两源不可判）：
- 客户端按 row[axis] 显示（4338/10525 校准）；投影 > 行数-1 ⇒ 领奖态任务书整块空白（真缺陷）。
- 投影 == 行数-1 ⇒ 指向末行（多为报告行）。
- 投影 < 行数-1 ⇒ 指向中间行（无实机证据前不判错；QE-054 边界：禁机械抬行）。

用法：python3 .agents/summary/quest-residuals-dabc/C-sweep.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
XML_DIR = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
ROWS_TSV = REPO / "src/test/resources/quest/quest_client_summary_rows.tsv"
TESTS = [
    REPO / "src/test/java/com/aionemu/gameserver/questEngine/definition/JournalRewardRowRepairContractTest.java",
    REPO / "src/test/java/com/aionemu/gameserver/questEngine/definition/MirrorPairRewardRowContractTest.java",
]


def client_rows() -> dict[int, int]:
    rows: dict[int, int] = {}
    for line in ROWS_TSV.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        qid, count = line.split("\t")[:2]
        rows[int(qid)] = int(count)
    return rows


def contracts() -> dict[int, tuple[int, int]]:
    """questId -> (rewardRow, staleRow) 取自两份批次名单。"""
    out: dict[int, tuple[int, int]] = {}
    for test in TESTS:
        text = test.read_text(encoding="utf-8")
        # new Contract(qid, rewardRow, staleRow)  /  new Contract(qid, mirrorId, rewardRow, staleRow)?
        for m in re.finditer(r"new Contract\((\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)", text):
            out.setdefault(int(m.group(1)), (int(m.group(2)), int(m.group(3))))
    return out


def reward_projection(quest_id: int) -> int | None:
    path = XML_DIR / f"{quest_id}.xml"
    if not path.is_file():
        return None
    text = path.read_text(encoding="utf-8")
    m = re.search(r'<node label="reward" status="REWARD">\s*<var name="var0" value="(\d+)"', text)
    return int(m.group(1)) if m else None


def main() -> int:
    rows = client_rows()
    listed = contracts()
    out_of_range, at_last, below_last, no_xml, no_rows = [], [], [], [], []
    for qid, (reward_row, stale_row) in sorted(listed.items()):
        projection = reward_projection(qid)
        if projection is None:
            no_xml.append((qid, reward_row, stale_row))
            continue
        count = rows.get(qid)
        if count is None or count <= 0:
            no_rows.append((qid, projection))
            continue
        if projection > count - 1:
            out_of_range.append((qid, projection, count, reward_row, stale_row))
        elif projection == count - 1:
            at_last.append((qid, projection, count))
        else:
            below_last.append((qid, projection, count))

    print(f"在册 {len(listed)}；无 XML {len(no_xml)}；无行数登记 {len(no_rows)}")
    print(f"OUT_OF_RANGE {len(out_of_range)}  AT_LAST {len(at_last)}  BELOW_LAST {len(below_last)}")
    print("\n# OUT_OF_RANGE（投影越过客户端行数 = 空白缺陷候选）")
    for qid, p, c, r, s in out_of_range:
        print(f"  {qid}: reward={p} rows={c} batch=({r},{s})")
    print("\n# BELOW_LAST（投影指向中间行，无实机证据前不判错）")
    for qid, p, c in below_last[:40]:
        print(f"  {qid}: reward={p} rows={c}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
