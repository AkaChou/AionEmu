#!/usr/bin/env python3
"""把已退役任务在 docs/QUEST_CATALOG.zh-CN.md 里的「定义文件」列改为"已退役"标注。

退役 XML 已从仓库删除（内容在 git 历史里可回溯），仓库不再保留测试作用域冻结副本，
因此不生成任何链接。

用法：python3 refresh_catalog_doc_links.py [--apply]
"""
from __future__ import annotations

import argparse
import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
DOC = REPO / "docs/QUEST_CATALOG.zh-CN.md"
RETENTION = REPO / "src/test/resources/quest/retail-xml-retention.tsv"
PROD_LINK = "(../src/main/resources/aion/data/static_data/quest_definition/quests/%s.xml)"
RETIRED_LINK = "(../src/test/resources/quest/retired/%s.xml)"
RETIRED_TEXT = "已退役（真端驱动；XML 见 git 历史）"
NOTE = ("> 迁移说明（2026-09-23 真端文件驱动改造）：已由真端文件驱动（退役）的任务，其生产 XML 已删除，"
        "仓库内不再保留副本；「定义文件」列标注为「已退役（真端驱动）」，历史内容走 git 历史"
        "（`git log --follow -- src/main/resources/aion/data/static_data/quest_definition/quests/<id>.xml`）。\n")


def retired_ids() -> list[str]:
    """已退役任务 id（保留清单 owner=RETAIL_TABLE）。"""
    ids = []
    for line in RETENTION.read_text(encoding="utf-8").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) >= 2 and parts[1] == "RETAIL_TABLE":
            ids.append(parts[0])
    return ids


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    text = DOC.read_text(encoding="utf-8")
    retired = sorted(retired_ids(), key=int)
    replaced = 0
    for quest_id in retired:
        # 兼容两种历史形态：仍在指生产 XML 的链接，以及曾指冻结副本的链接。
        for needle in (f"[quest_definition/quests/{quest_id}.xml]{PROD_LINK % quest_id}",
                       f"[quest/retired/{quest_id}.xml]{RETIRED_LINK % quest_id}"):
            if needle in text:
                text = text.replace(needle, RETIRED_TEXT)
                replaced += 1
    if "迁移说明（2026-09-23" not in text:
        anchor = "> 接取等级："
        index = text.index(anchor)
        text = text[:index] + NOTE + text[index:]
    dangling = [q for q in retired if PROD_LINK % q in text or RETIRED_LINK % q in text]
    print(f"retired={len(retired)} replaced={replaced} dangling={len(dangling)}")
    if args.apply:
        DOC.write_text(text, encoding="utf-8")
    print("dry-run" if not args.apply else "applied")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
