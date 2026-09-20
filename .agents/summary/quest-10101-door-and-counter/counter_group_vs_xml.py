#!/usr/bin/env python3
"""对同一批任务输出：客户端 quest_summary 计数占位符组索引、生产 XML 未完成击杀路线的写入字段、
客户端 quest_monster/quest_script_monster 合同行。只读证据脚本。"""
import glob
import re
import sys

QUESTIONS = [10101, 10112, 10011, 13945, 15324, 50091, 15546, 17510, 18994, 13705, 25406,
             15001, 10084, 10091, 18931, 11340, 17106]


def html_counters(q):
    hits = glob.glob(f"/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs/*/quest_q{q}.html")
    if not hits:
        return None
    s = open(hits[0], encoding="utf-8", errors="replace").read()
    m = re.search(r'<HtmlPage name="quest_summary">.*?</HtmlPage>', s, re.S)
    if not m:
        return None
    return sorted({(int(n) - 2) // 3 for n in re.findall(r'\[%(\d+)\]', m.group(0)) if int(n) % 3 == 2
                   and re.search(r'\(\[%' + n + r'\]', m.group(0))})


def xml_fields(q):
    p = f"src/main/resources/aion/data/static_data/quest_definition/quests/{q}.xml"
    try:
        s = open(p, encoding="utf-8").read()
    except FileNotFoundError:
        return None, None
    i, j = s.find("<progress>"), s.find("</progress>")
    layout = re.findall(r'name="(var\d)" offset="(\d+)" width="(\d+)"(?:[^>]*max="(\d+)")?', s[i:j])
    kills = []
    for m in re.finditer(r'<transition[^>]*>(.*?)</transition>', s, re.S):
        body = m.group(1)
        if "kill-npc" not in body:
            continue
        head = m.group(0)[:m.group(0).find(">") + 1]
        fields = re.findall(r'<set-variable field="(var\d)" value="([^"]+)"', body)
        inc = re.findall(r'<increment-variable field="(var\d)"', body)
        tgt = re.search(r'target="([^"]+)"', head)
        kills.append((tgt.group(1) if tgt else "?", fields, inc))
    return layout, kills


def csv_rows(path, q):
    out = []
    try:
        for line in open(path, encoding="utf-8", errors="replace"):
            if line.startswith(f"{q},"):
                out.append(line.strip())
    except FileNotFoundError:
        pass
    return out


def main():
    ids = [int(x) for x in sys.argv[1:]] or QUESTIONS
    for q in ids:
        print("=" * 30, q)
        print("  summary counter groups :", html_counters(q))
        layout, kills = xml_fields(q)
        print("  xml bit-fields        :", layout)
        if kills:
            for k in kills[:12]:
                print("    kill ->", k)
        for row in csv_rows("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_monster.csv", q):
            print("  [monster]", row[:160])
        for row in csv_rows("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_script_monster.csv", q):
            print("  [script ]", row[:160])


if __name__ == "__main__":
    main()
