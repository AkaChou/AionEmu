#!/usr/bin/env python3
"""审计：客户端页面 quest_summary 的 ([%n]/N) 击杀上限 vs 真端 Quest_SimpleHunt.xml count。

用途：判定「CHS 本地化页面显示 7、服务端要求 10」是个例还是成规模的本地化偏差。
输入：
  A) 真端表 <真端根>/Map/XML/Quest_SimpleHunt.xml
  B) KR 基础页面（客户端 data/Dialogs/Dialogs.pak 解包目录）
  C) CHS 覆盖页面（客户端 L10N/CHS/Data/data.pak 解包目录）
输出：kr-vs-retail.tsv / chs-vs-retail.tsv
"""
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

RETAIL = Path(sys.argv[1])          # 真端 Quest_SimpleHunt.xml
KR_DIR = Path(sys.argv[2])          # KR 基础页面目录
CHS_DIR = Path(sys.argv[3])         # CHS 覆盖页面目录
OUT_DIR = Path(__file__).resolve().parent

COUNTER = re.compile(r"\[%(\d+)\]/(\d+)")
SUMMARY = re.compile(r'<HtmlPage name="quest_summary">.*?</HtmlPage>', re.S)


def load_retail():
    table = {}
    root = ET.parse(RETAIL).getroot()
    for q in root.iter("id"):
        qid = int(q.get("id"))
        counters = []
        n = 1
        while True:
            cnt = q.findtext(f"count{n}")
            if cnt is None:
                break
            counters.append(int(cnt))
            n += 1
        if counters:
            table[qid] = counters
    return table


def page_limits(directory, qid):
    path = directory / f"QUEST_Q{qid}.html"
    if not path.exists():
        return None
    src = path.read_text(encoding="utf-8", errors="replace")
    if src[:2] == "﻿":
        src = src[1:]
    m = SUMMARY.search(src)
    if not m:
        return []            # 有页面但无摘要（非击杀类）
    return [int(b) for _, b in COUNTER.findall(m.group(0))]


def audit(directory, out_name, retail):
    rows = []
    for qid, counts in sorted(retail.items()):
        limits = page_limits(directory, qid)
        if limits is None:
            rows.append((qid, counts, "-", "NO_PAGE"))
            continue
        if not limits:
            rows.append((qid, counts, "-", "NO_SUMMARY"))
            continue
        if sorted(limits) == sorted(counts):
            rows.append((qid, counts, limits, "MATCH"))
        else:
            rows.append((qid, counts, limits, "MISMATCH"))
    out = OUT_DIR / out_name
    with out.open("w", encoding="utf-8") as fh:
        fh.write("quest_id\tretail_counts\tpage_limits\tstatus\n")
        for qid, counts, limits, status in rows:
            c = " ".join(map(str, counts))
            l = " ".join(map(str, limits)) if isinstance(limits, list) else limits
            fh.write(f"{qid}\t{c}\t{l}\t{status}\n")
    stats = defaultdict(int)
    for *_, status in rows:
        stats[status] += 1
    print(f"{out.name}: " + " ".join(f"{k}={v}" for k, v in sorted(stats.items())))
    print("  MISMATCH 明细：")
    for qid, counts, limits, status in rows:
        if status == "MISMATCH":
            print(f"    {qid}: retail={counts} page={limits}")
    return rows


def main():
    retail = load_retail()
    print(f"真端表任务数（含计数）：{len(retail)}")
    audit(KR_DIR, "kr-vs-retail.tsv", retail)
    audit(CHS_DIR, "chs-vs-retail.tsv", retail)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
