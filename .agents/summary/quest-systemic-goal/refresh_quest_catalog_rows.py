#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
按生产 XML 当前 metadata 刷新 docs/QUEST_CATALOG.zh-CN.md 的目标行（只动第 2 列「接取等级」）。

刷新范围：P1/P2 修复的 36 个任务 + Goal F 点名的 28649 文档漂移行。
显示规则（与目录头部说明一致）：
  存在有效 max-level（非 2147483647）时输出 `min-max`，否则输出 `min+`。
其余行与列保持不变；重复运行幂等。
"""
import os
import re

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
CATALOG = os.path.join(REPO, "docs", "QUEST_CATALOG.zh-CN.md")
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")

TARGETS = {
    # P1 min-level 修复
    "1648", "2231", "2641", "19000", "19001", "19002", "19003", "25407", "25408",
    # P2 max-level 修复
    "4711", "4712", "4722", "15665", "15666", "18833", "18835", "25665", "25666",
    "29672", "29673", "29674", "29675", "29676", "29684", "29685", "29686", "29687",
    "29688", "80621", "80622", "80643", "80644", "27525", "50074", "80945", "80946",
    "80878",
    # 文档漂移修复
    "28649",
}

META_RE = re.compile(r'<metadata[^>]*min-level="(\d+)"[^>]*max-level="(\d+)"')


def level_label(qid):
    with open(os.path.join(PROD_DIR, qid + ".xml"), encoding="utf-8") as fh:
        head = fh.read(900)
    m = META_RE.search(head)
    if not m:
        raise SystemExit(f"{qid}: metadata min/max not found")
    mn, mx = int(m.group(1)), int(m.group(2))
    return f"{mn}-{mx}" if mx != 2147483647 else f"{mn}+"


def main():
    with open(CATALOG, encoding="utf-8") as fh:
        lines = fh.readlines()
    changed = []
    for i, line in enumerate(lines):
        m = re.match(r"^\| (\d+) \|", line)
        if not m:
            continue
        qid = m.group(1)
        if qid not in TARGETS:
            continue
        cols = line.split("|")
        if len(cols) < 3:
            continue
        new_label = level_label(qid)
        old_label = cols[2].strip()
        if old_label == new_label:
            continue
        cols[2] = f" {new_label} "
        lines[i] = "|".join(cols)
        changed.append(f"{qid}: {old_label} -> {new_label}")
    with open(CATALOG, "w", encoding="utf-8") as fh:
        fh.writelines(lines)
    print(f"refreshed {len(changed)} catalog rows:")
    for c in changed:
        print(" ", c)


if __name__ == "__main__":
    main()
