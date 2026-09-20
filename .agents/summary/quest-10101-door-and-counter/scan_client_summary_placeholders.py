#!/usr/bin/env python3
"""扫描 Aion 5.8 客户端 quest_summary 占位符 [%n] 与客户端 SECTION 计数的对应关系。

输入（机器本地解包目录，路径不写入仓库）:
  --dialog-root  <client>/data_unpacked/Dialogs
  --quest-csv    <client>/Quest_unpacked/quest_monster.csv
  --script-csv   <client>/Quest_unpacked/quest_script_monster.csv
输出: 每个任务的 <step> 占位符清单 + CSV 中的 SECTION 计数行。
"""
import argparse
import csv
import os
import re
import sys

STEP_RE = re.compile(r"<step>(.*?)</step>", re.S)
VIS_RE = re.compile(r'visible="\[%(\d+)\]"')
COL_RE = re.compile(r'color="\[%(\d+)\]"')
CNT_RE = re.compile(r"\(\[%(\d+)\]/")


def scan_html(path):
    text = open(path, encoding="utf-8-sig", errors="replace").read()
    m = re.search(r'name="quest_summary"(.*?)</HtmlPage>', text, re.S)
    if not m:
        return None
    body = m.group(1)
    steps = STEP_RE.findall(body)
    out = []
    for i, s in enumerate(steps):
        vis = [int(x) for x in VIS_RE.findall(s)]
        col = [int(x) for x in COL_RE.findall(s)]
        cnt = [int(x) for x in CNT_RE.findall(s)]
        out.append((i, sorted(set(vis)), sorted(set(col)), cnt, re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", s))[:60]))
    return out


def load_csv(path):
    rows = {}
    with open(path, encoding="utf-8-sig", errors="replace") as fh:
        for r in csv.reader(fh):
            if len(r) < 7 or not r[0].isdigit():
                continue
            rows.setdefault(r[0], []).append(r)
    return rows


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dialog-root", required=True)
    ap.add_argument("--quest-csv", required=True)
    ap.add_argument("--script-csv", required=True)
    ap.add_argument("--quests", default="")
    args = ap.parse_args()

    qrows = load_csv(args.quest_csv)
    srows = load_csv(args.script_csv)
    wanted = set(args.quests.split(",")) if args.quests else None

    for dirpath, _dirnames, filenames in os.walk(args.dialog_root):
        for fn in sorted(filenames):
            m = re.fullmatch(r"quest_q(\d+)\.html", fn)
            if not m:
                continue
            qid = m.group(1)
            if wanted and qid not in wanted:
                continue
            steps = scan_html(os.path.join(dirpath, fn))
            if not steps:
                continue
            has_cnt = any(s[3] for s in steps)
            csv_rows = qrows.get(qid, []) + srows.get(qid, [])
            sections = sorted({int(x) for r in csv_rows for x in re.findall(r"SECTION_(\d+)", r[1])})
            if not has_cnt and not sections:
                continue
            print(f"### quest {qid}  csv_sections={sections}")
            for r in csv_rows:
                print(f"    CSV {r[1]} | {r[3]} | {r[5]} | {r[6][:60]}")
            for (i, vis, col, cnt, txt) in steps:
                print(f"    step{i}: visible={vis} color={col} counters={cnt} :: {txt}")


if __name__ == "__main__":
    sys.exit(main())
