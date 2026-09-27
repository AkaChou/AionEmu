#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""常态化修复：并行会话清单重倾打回 wave3 的 18 行时重翻（幻影翻转脚本）。

并行 DD 会话的 retail-xml-retention.tsv 生成器周期性（约 5-7 分钟）全量重倾，
其模型中 wave3 家族（感应区坐骑 15551-15554/25551-25554、IDEternity 感应区对
13967/23967、16827/26827、16839/26839、16987/26987、28252/28253）仍为
FAMILY_PENDING:DataDriven，会覆盖本会话已写入的 RETAIL_TABLE 采纳行，而对应
XML 文件已退役删除 → 生产视图 missing 18、门测试红。

修复判据（与 wave1/2 EA 批行完全同构，evidence 见
enterarea-retired-xml-evidence.tsv 的 wave3 行）：
    <id>	RETAIL_TABLE	DataDriven	OK	retired-xml-in-git-history p5-datadriven-decisions.tsv basis=DD_TALK_HUNT_CHAIN_CLIENT_ROWS

用法：python3 reflip_wave3_retention_rows.py
翻完四副本后立即跑 RetailDataDrivenGateTest + SensoryAreaRideRowContractTest
确认生产视图恢复（抢在下一轮重倾之前）。

Routine repair: the concurrent DD session's retention-manifest writer re-dumps
the whole file every ~5-7 minutes and stamps the 18 wave3 rows back to
FAMILY_PENDING:DataDriven while their XML files are already retired — the
production view then misses 18 quests. This script re-applies the wave3
RETAIL_TABLE flip (same shape as the wave1/2 EA rows) across all four manifest
copies; run the two suites right afterwards to race the next re-dump.
"""

from pathlib import Path

IDS = [13967, 23967,
       15551, 15552, 15553, 15554,
       25551, 25552, 25553, 25554,
       16827, 26827, 16839, 26839, 16987, 26987,
       28252, 28253]

OLD = "{id}\tXML_RETENTION\tDataDriven\tFAMILY_PENDING:DataDriven\tretired-xml-in-git-history"
NEW = ("{id}\tRETAIL_TABLE\tDataDriven\tOK\t"
       "retired-xml-in-git-history p5-datadriven-decisions.tsv "
       "basis=DD_TALK_HUNT_CHAIN_CLIENT_ROWS")

COPIES = [
    "src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
    "src/test/resources/quest/retail-xml-retention.tsv",
    "target/classes/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
    "target/test-classes/quest/retail-xml-retention.tsv",
]


def main() -> None:
    root = Path(__file__).resolve().parents[3]
    for rel in COPIES:
        path = root / rel
        text = path.read_text(encoding="utf-8")
        flipped = 0
        for quest_id in IDS:
            old = OLD.format(id=quest_id) + "\n"
            if old in text:
                text = text.replace(old, NEW.format(id=quest_id) + "\n")
                flipped += 1
        path.write_text(text, encoding="utf-8")
        print(f"{rel}: flipped {flipped}")


if __name__ == "__main__":
    main()
