#!/usr/bin/env python3
"""对客户端 quest_script_monster.csv 中数值型 Progress 的 killedByUser 条目做全库对照。

For every quest whose client script monster contract uses the numeric Progress form for
killedByUser, report: HTML counter placeholder groups, and how the production XML models the
kill route (row-advance on var0 vs. dedicated counter var). 只读证据脚本。
"""
import glob
import re
import sys
from collections import Counter

SCRIPT_CSV = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_script_monster.csv"
DIALOGS = "/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs"
QUEST_XML_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"


def numeric_kill_quests():
    out = {}
    for line in open(SCRIPT_CSV, encoding="utf-8", errors="replace"):
        parts = line.strip().split(",")
        if len(parts) < 4 or parts[3] != "killedByUser":
            continue
        qid = int(parts[0])
        prog = parts[1]
        m = re.fullmatch(r"Progress\((\d+)~!(\d+)\)", prog)
        if m:
            out.setdefault(qid, []).append((int(m.group(1)), int(m.group(2))))
        else:
            out.setdefault(qid, [])
    return out


def html_counter_groups(q):
    hits = glob.glob(f"{DIALOGS}/*/quest_q{q}.html")
    if not hits:
        return None
    s = open(hits[0], encoding="utf-8", errors="replace").read()
    m = re.search(r'<HtmlPage name="quest_summary">.*?</HtmlPage>', s, re.S)
    if not m:
        return None
    block = m.group(0)
    groups = set()
    for mm in re.finditer(r'\(\[%(\d+)\]\s*/\s*(\d+)\)', block):
        n = int(mm.group(1))
        groups.add(((n - 2) // 3, int(mm.group(2))))
    return sorted(groups)


def kill_shape(q):
    try:
        s = open(f"{QUEST_XML_DIR}/{q}.xml", encoding="utf-8").read()
    except FileNotFoundError:
        return None
    fields = set(re.findall(r'name="(var\d)"', s))
    inc, sets_row, sets_other = Counter(), 0, 0
    for m in re.finditer(r'<transition[^>]*>(.*?)</transition>', s, re.S):
        body = m.group(1)
        if "kill-npc" not in body:
            continue
        for f in re.findall(r'<increment-variable field="(var\d)"', body):
            inc[f] += 1
        for f, v in re.findall(r'<set-variable field="(var\d)" value="(\d+)"', body):
            if f == "var0":
                sets_row += 1
            else:
                sets_other += 1
    return {"fields": sorted(fields), "inc": dict(inc), "set_var0": sets_row, "set_other": sets_other}


def main():
    quests = numeric_kill_quests()
    rows = []
    for q, ranges in sorted(quests.items()):
        if not ranges:
            continue
        shape = kill_shape(q)
        rows.append((q, ranges, html_counter_groups(q), shape))
    print(f"quests with numeric killedByUser ranges: {len(rows)}")
    with_counter = [r for r in rows if r[2]]
    print(f"  of which HTML shows a counter: {len(with_counter)}")
    for q, ranges, groups, shape in with_counter:
        print(f"q={q} ranges={ranges} html_counters={groups} xml={shape}")
    # distribution of xml shapes
    dist = Counter()
    for q, ranges, groups, shape in rows:
        if not shape:
            dist["NO_XML"] += 1
        elif shape["inc"]:
            dist["counter_inc:" + ",".join(sorted(shape["inc"]))] += 1
        elif shape["set_row"]:
            dist["row_advance_only"] += 1
        else:
            dist["other"] += 1
    print("shape distribution:", dict(dist))


if __name__ == "__main__":
    sys.exit(main())
