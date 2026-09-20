#!/usr/bin/env python3
"""判定客户端 quest_summary 计数占位符 [%n] 的编号规则。

两种候选：
  A) 计数器序号（0-based） -> 3*k+2   （只看声明顺序）
  B) SECTION 号 -> 3*SECTION+2        （按位段号）
输出每个任务的 CSV 计数段顺序、HTML 计数占位符顺序，以及 A/B 命中情况。
"""
import argparse, csv, os, re, sys

STEP_RE = re.compile(r"<step>(.*?)</step>", re.S)
CNT_RE = re.compile(r"\(\[%(\d+)\]/")
SEC_LT = re.compile(r"SECTION_(\d+)\s*<\s*\d+")
PROG_RE = re.compile(r"Progress\((.*?)\)")


def html_counters(path):
    text = open(path, encoding="utf-8-sig", errors="replace").read()
    m = re.search(r'name="quest_summary"(.*?)</HtmlPage>', text, re.S)
    if not m:
        return None
    return [int(x) for x in CNT_RE.findall(m.group(1))]


def load_rows(path, cols):
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
    args = ap.parse_args()
    qrows = load_rows(args.quest_csv, None)
    srows = load_rows(args.script_csv, None)

    stats = {"A": 0, "B": 0, "both": 0, "neither": 0, "total": 0}
    for dirpath, _d, filenames in os.walk(args.dialog_root):
        for fn in filenames:
            m = re.fullmatch(r"quest_q(\d+)\.html", fn)
            if not m:
                continue
            qid = m.group(1)
            rows = qrows.get(qid, []) + srows.get(qid, [])
            secs, seen = [], set()
            for r in rows:
                pm = PROG_RE.search(r[1])
                if not pm:
                    continue
                for s in SEC_LT.findall(pm.group(1)):
                    s = int(s)
                    if s not in seen and ("SECTION_%d<" % s) in pm.group(1):
                        # 只保留当前行自身计数字段；同段重复（itemUseArea+dropMonster）去重
                        if s in seen:
                            continue
                        secs.append(s)
                        seen.add(s)
            if not secs:
                continue
            cnt = html_counters(os.path.join(dirpath, fn))
            if not cnt:
                continue
            predA = [3 * k + 2 for k in range(len(cnt))]
            predB = [3 * s + 2 for s in secs[:len(cnt)]]
            a_ok, b_ok = cnt == predA, cnt == predB
            stats["total"] += 1
            stats["A" if a_ok else "x"] = stats.get("A" if a_ok else "x", 0)
            if a_ok and b_ok:
                stats["both"] += 1
            elif a_ok:
                stats["A"] += 1
            elif b_ok:
                stats["B"] += 1
            else:
                stats["neither"] += 1
            if not (a_ok and b_ok):
                print(f"quest {qid}: csv_sections={secs} html_counters={cnt} A={a_ok} B={b_ok}")
    print("stats:", stats)


if __name__ == "__main__":
    sys.exit(main())
