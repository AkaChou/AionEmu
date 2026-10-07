#!/usr/bin/env python3
"""审计：客户端 quest_monster.csv 的 simpleQuest 门控上限 vs 真端 Quest_SimpleHunt.xml count。

用途：判定击杀任务「客户端自身逻辑」要求的次数。simpleQuest 行的
`Progress(SECTION_0<count; SECTION_5==0)` 中 count 即客户端认为的击杀上限
（见 QE-125：客户端行驱动，真端块与客户端不一致时以客户端为准）。
输出：client-gate-vs-retail.tsv
"""
import csv
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RETAIL = Path(sys.argv[1])       # 真端 Quest_SimpleHunt.xml
CLIENT_CSV = Path(sys.argv[2])   # 客户端 quest_monster.csv
OUT = Path(__file__).resolve().parent / "client-gate-vs-retail.tsv"

GATE = re.compile(r"SECTION_(\d+)<(\d+)")


def load_retail():
    table = {}
    for q in ET.parse(RETAIL).getroot().iter("id"):
        qid = int(q.get("id"))
        counts, n = [], 1
        while True:
            cnt = q.findtext(f"count{n}")
            if cnt is None:
                break
            counts.append(int(cnt))
            n += 1
        if counts:
            table[qid] = counts
    return table


def load_client_gates():
    gates = {}
    with CLIENT_CSV.open(encoding="utf-8-sig") as fh:
        for row in csv.reader(fh):
            if len(row) < 4 or not row[0].strip().isdigit():
                continue
            if row[3].strip() != "simpleQuest":
                continue
            qid = int(row[0])
            gates.setdefault(qid, []).append([(int(a), int(b)) for a, b in GATE.findall(row[1])])
    return gates


def main():
    retail = load_retail()
    gates = load_client_gates()
    rows = []
    for qid in sorted(retail):
        counts = retail[qid]
        rows_of = gates.get(qid)
        if rows_of is None:
            rows.append((qid, counts, "-", "NO_CLIENT_GATE"))
            continue
        limits = [b for r in rows_of for _, b in r]
        if sorted(limits) == sorted(counts):
            rows.append((qid, counts, limits, "MATCH"))
        else:
            rows.append((qid, counts, limits, "MISMATCH"))
    with OUT.open("w", encoding="utf-8") as fh:
        fh.write("quest_id\tretail_counts\tclient_gate_limits\tstatus\n")
        for qid, counts, limits, status in rows:
            l = " ".join(map(str, limits)) if isinstance(limits, list) else limits
            fh.write(f"{qid}\t{' '.join(map(str, counts))}\t{l}\t{status}\n")
    stats = {}
    for *_, status in rows:
        stats[status] = stats.get(status, 0) + 1
    print(f"{OUT.name}: " + " ".join(f"{k}={v}" for k, v in sorted(stats.items())))
    print("MISMATCH 明细：")
    for qid, counts, limits, status in rows:
        if status == "MISMATCH":
            print(f"  {qid}: retail={counts} client_gate={limits}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
