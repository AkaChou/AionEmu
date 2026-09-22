#!/usr/bin/env python3
"""扫描客户端 quest_summary 的“空行槽位”任务族 / Scan quests whose quest_summary slots are blank.

背景 / Why
---------
QE-051 的行号口径把 quest_summary 里的每个 `<step>` 当成一行任务书。但序幕/过场类任务（例如
1000 Prologue）的客户端模板固定渲染 4 个空槽位（`<font color="[%1]"> </font>` 全是空白，
行 0 只是挂了 `[%collectitem]` 占位符），这类任务在审计里会被判成
NO_REWARD_ROW / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE（rows_without_state=1 2 3），
但服务端没有可推进的行——按行号补阶梯只会造出永远不显示的目标。

本脚本只做只读分类，输出两类清单：
  ALL_BLANK       —— 所有 step 的可见文本都为空（含 collectitem 占位）
  TRAILING_BLANK  —— 前面有非空行、末尾若干 step 为空
供审计例外登记与人工核对使用。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/audit_blank_journal_slots.py [--out-csv PATH]
"""

from __future__ import annotations

import argparse
import importlib.util
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location(
    "audit_reward_row_vs_client_steps", HERE / "audit_reward_row_vs_client_steps.py")
AUDIT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUDIT)

STEP_RE = re.compile(r"<step[^>]*>(.*?)</step>", re.S)
TAG_RE = re.compile(r"<[^>]+>")
PLACEHOLDER_RE = re.compile(r"\[%[^\]]*\]")


def step_texts(html: str) -> list[str] | None:
    """quest_summary 的每行可见文本（去标签/去占位符/去空白）；无页面返回 None。"""
    match = re.search(r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', html, re.S)
    if not match:
        return None
    steps = STEP_RE.findall(match.group(1))
    if not steps:
        return None
    texts = []
    for step in steps:
        plain = PLACEHOLDER_RE.sub("", TAG_RE.sub(" ", step))
        texts.append(re.sub(r"\s+", " ", plain).strip())
    return texts


def classify(texts: list[str]) -> str | None:
    if not texts:
        return None
    if all(not text for text in texts):
        return "ALL_BLANK"
    if not texts[-1]:
        return "TRAILING_BLANK"
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description="Scan blank quest_summary slots.")
    parser.add_argument("--out-csv", type=Path, default=None)
    args = parser.parse_args()

    index = AUDIT.client_index()
    rows = []
    for quest_id, path in sorted(index.items()):
        html = path.read_text(encoding="utf-8", errors="ignore")
        texts = step_texts(html)
        if texts is None:
            continue
        verdict = classify(texts)
        if verdict is None:
            continue
        blank_slots = [str(i) for i, text in enumerate(texts) if not text]
        rows.append({
            "quest_id": quest_id,
            "verdict": verdict,
            "rows": len(texts),
            "blank_slots": " ".join(blank_slots),
            "non_blank_prefix": " | ".join(text[:40] for text in texts if text)[:120],
            "client_file": path.name,
        })

    all_blank = [row for row in rows if row["verdict"] == "ALL_BLANK"]
    trailing = [row for row in rows if row["verdict"] == "TRAILING_BLANK"]
    print(f"quests_scanned={len(index)} blank_rows={len(rows)}")
    print(f"ALL_BLANK={len(all_blank)} TRAILING_BLANK={len(trailing)}")
    print("\n[ALL_BLANK] 全空槽位（无可见任务书行）：")
    for row in all_blank[:200]:
        print(f"  {row['quest_id']}\trows={row['rows']}\tblank={row['blank_slots']}\t{row['client_file']}")
    print("\n[TRAILING_BLANK] 尾随空槽位：")
    for row in trailing[:200]:
        print(f"  {row['quest_id']}\trows={row['rows']}\tblank={row['blank_slots']}\tprefix={row['non_blank_prefix']}")
    if args.out_csv:
        header = ["quest_id", "verdict", "rows", "blank_slots", "non_blank_prefix", "client_file"]
        lines = ["\t".join(header)]
        for row in rows:
            lines.append("\t".join(str(row[key]) for key in header))
        args.out_csv.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"\nwrote {args.out_csv} rows={len(rows)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
