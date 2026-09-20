#!/usr/bin/env python3
"""汇总：客户端 quest_summary 计数占位符 [%n] 与客户端 CSV 的 SECTION_n 计数/行门控对照表。"""
import argparse, csv, os, re, sys

STEP_RE = re.compile(r"<step>(.*?)</step>", re.S)
VIS_RE = re.compile(r'visible="\[%(\d+)\]"')
COL_RE = re.compile(r'color="\[%(\d+)\]"')
CNT_RE = re.compile(r"\(\[%(\d+)\]/")
SEC_RE = re.compile(r"SECTION_(\d+)\s*(==|<|<=|>|>=)\s*(\d+)")
PROG_RE = re.compile(r"Progress\((.*?)\)")


def scan_html(path):
    text = open(path, encoding="utf-8-sig", errors="replace").read()
    m = re.search(r'name="quest_summary"(.*?)</HtmlPage>', text, re.S)
    if not m:
        return None
    body = m.group(1)
    steps = STEP_RE.findall(body)
    out = []
    for i, s in enumerate(steps):
        vis = sorted({int(x) for x in VIS_RE.findall(s)})
        col = sorted({int(x) for x in COL_RE.findall(s)})
        cnt = [int(x) for x in CNT_RE.findall(s)]
        out.append((i, vis, col, cnt))
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
    ap.add_argument("--limit", type=int, default=0)
    args = ap.parse_args()
    qrows, srows = load_csv(args.quest_csv), load_csv(args.script_csv)

    printed = 0
    for dirpath, _d, filenames in os.walk(args.dialog_root):
        for fn in sorted(filenames):
            m = re.fullmatch(r"quest_q(\d+)\.html", fn)
            if not m:
                continue
            qid = m.group(1)
            rows = qrows.get(qid, []) + srows.get(qid, [])
            counters = []
            for r in rows:
                pm = PROG_RE.search(r[1])
                cond = pm.group(1) if pm else r[1]
                secs = SEC_RE.findall(cond)
                if not secs:
                    continue
                counters.append((secs, r[3], r[5], len(r[6].split(","))))
            if not counters:
                continue
            steps = scan_html(os.path.join(dirpath, fn))
            if not steps or not any(s[3] for s in steps):
                continue
            print(f"quest {qid}")
            for secs, stype, num, mcount in counters:
                print("   CSV gate/cond:", " ".join(f"S{a}{op}{b}" for a, op, b in secs), "|", stype, "| num=", num, "| monsters=", mcount)
            for (i, vis, col, cnt) in steps:
                print(f"   step{i}: visible={vis} color={col} counters={cnt}")
            printed += 1
            if args.limit and printed >= args.limit:
                return 0
    return 0


if __name__ == "__main__":
    sys.exit(main())
