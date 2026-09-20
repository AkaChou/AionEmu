#!/usr/bin/env python3
"""对照客户端 quest_summary 计数占位符组索引与客户端监控合同给出的 counter_section。

Compares the counter placeholder group index in the client quest_summary HTML with the
counter_section recorded in docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv.
只读脚本，仅用于本任务证据收集。
"""
import csv
import glob
import re
import sys

DIALOG_ROOTS = glob.glob("/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs/*")
CSV_PATH = "docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv"


def html_for(quest_id):
    hits = [p for r in DIALOG_ROOTS for p in
            glob.glob(f"{r}/quest_q{quest_id}.html")]
    return hits[0] if hits else None


def summary_counters(quest_id):
    path = html_for(quest_id)
    if not path:
        return None
    text = open(path, encoding="utf-8", errors="replace").read()
    m = re.search(r'<HtmlPage name="quest_summary">.*?</HtmlPage>', text, re.S)
    if not m:
        return None
    block = m.group(0)
    out = []
    for mm in re.finditer(r'\(\[%(\d+)\]\s*/\s*(\d+)\)', block):
        out.append((int(mm.group(1)), int(mm.group(2))))
    return out


def main():
    agree = disagree = 0
    rows = []
    with open(CSV_PATH, encoding="utf-8-sig") as fh:
        for row in csv.DictReader(fh):
            qid = int(row["quest_id"])
            cs = int(row["counter_section"])
            counters = summary_counters(qid)
            if not counters:
                continue
            groups = sorted({(n - 2) // 3 for n, _ in counters if n % 3 == 2})
            ok = groups == [cs]
            if ok:
                agree += 1
            else:
                disagree += 1
                rows.append((qid, cs, counters, groups))
    print(f"agree={agree} disagree={disagree}")
    for r in rows[:40]:
        print("quest=%s counter_section=%s counters=%s groups=%s" % r)


if __name__ == "__main__":
    sys.exit(main())
