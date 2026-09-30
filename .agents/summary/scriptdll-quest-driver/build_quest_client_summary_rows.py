#!/usr/bin/env python3
"""生成客户端任务书行数登记表（quest_client_summary_rows.tsv）。

用途：AionEmu 的 `reward` 节点 `var0` 投影必须等于客户端任务书（`quest_summary`）的**末行行号**
（memory-bank QE-051：领奖投影必须等于客户端任务书领奖行；客户端把行号 n 映射到 visible 槽位 3n）。
真端模板表没有行数信息，因此行数从客户端 HTML 烘焙成只读资源，供 SimpleTalk 合成器与门禁使用。

源：`<客户端解包根>/data_unpacked/Dialogs/**/quest_q<id>.html` 的
`<HtmlPage name="quest_summary">` 内 `<step>` 数量（大小写不敏感，文件名有 `QUEST_Q`/`quest_q` 两种写法）。

输出：src/main/resources/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv
格式：quest_id \t rows（rows >= 1；无 quest_summary 页的任务不入表）
"""
from __future__ import annotations
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

import os

import pathlib
import re
import sys

DIALOGS = pathlib.Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/data_unpacked/Dialogs")
OUT = pathlib.Path(
    "src/main/resources/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv")
NAME = re.compile(r"quest_q(\d+)\.html", re.IGNORECASE)
SUMMARY = re.compile(r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', re.DOTALL | re.IGNORECASE)
STEP = re.compile(r"<step>")


def collect():
    rows: dict[int, int] = {}
    for path in sorted(DIALOGS.rglob("*.html")):
        match = NAME.fullmatch(path.name)
        if match is None:
            continue
        quest_id = int(match.group(1))
        text = path.read_text(encoding="utf-8", errors="replace")
        summary = SUMMARY.search(text)
        if summary is None:
            continue
        count = len(STEP.findall(summary.group(1)))
        if count <= 0:
            continue
        previous = rows.get(quest_id)
        if previous is not None and previous != count:
            # 同名多副本必须一致，否则登记表不可信。
            raise SystemExit(f"conflicting summary row counts for {quest_id}: {previous} vs {count}")
        rows[quest_id] = count
    return rows


def main() -> int:
    rows = collect()
    lines = [
        "# Aion 5.8 客户端任务书行数登记（quest_id, rows）",
        "# 来源：data_unpacked/Dialogs/**/quest_q<id>.html 的 quest_summary 页 <step> 数量",
        "# 生成：.agents/summary/scriptdll-quest-driver/build_quest_client_summary_rows.py",
        "# 语义：REWARD 节点 var0 投影 = rows - 1（末行行号）；客户端 visible 槽位 = 3 * 行号（QE-051）。",
    ]
    lines.extend(f"{quest_id}\t{rows[quest_id]}" for quest_id in sorted(rows))
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {OUT} rows={len(rows)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
