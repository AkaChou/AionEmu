#!/usr/bin/env python3
"""审计“行索引步进（var0 行走）+ 客户端计数槽”同型任务。

Audits quests whose kill route only walks the journal-row variable var0 while the client
quest_summary displays a counter in slot [%3k+2]; the counter value must live in SECTION_k
(offset 6*k), because the client reads the section, not the row index.

输出：同型候选（同一 NPC 连杀、未写独立计数字段、客户端存在计数槽）与其显示槽位。
只读脚本，仅用于证据收集。
"""
import csv
import glob
import re
import xml.etree.ElementTree as ET

QUEST_GLOB = "src/main/resources/aion/data/static_data/quest_definition/quests/*.xml"
DIALOG_ROOTS = glob.glob("/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs/*")
CLIENT_CSV = ("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_monster.csv",
              "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_script_monster.csv")


def summary_counters(quest_id):
    for root in DIALOG_ROOTS:
        for path in glob.glob(f"{root}/quest_q{quest_id}.html"):
            text = open(path, encoding="utf-8-sig", errors="replace").read()
            block = re.search(r'name="quest_summary"(.*?)</HtmlPage>', text, re.S)
            if not block:
                return None
            return [(int(n), int(m)) for n, m in
                    re.findall(r'\(\[%(\d+)\]\s*/\s*(\d+)\)', block.group(1))]
    return None


def client_rows():
    rows = {}
    for path in CLIENT_CSV:
        for row in csv.reader(open(path, encoding="utf-8-sig", errors="replace")):
            if len(row) >= 7 and row[0].isdigit():
                rows.setdefault(row[0], []).append((row[3], row[1]))
    return rows


def main():
    client = client_rows()
    hits = []
    for path in sorted(glob.glob(QUEST_GLOB)):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        quest_id = root.get("id")
        fields = {f.get("name") for f in root.findall("./progress/bit-field")}
        if any(field != "var0" for field in fields):
            continue
        per_npc = {}
        for transition in root.findall("./transitions/transition"):
            event = transition.find("event/kill-npc")
            if event is None:
                continue
            actions = {a.get("field"): a.get("value")
                       for a in transition.findall("./actions/set-variable")}
            per_npc.setdefault(event.get("npc-id") or event.get("npc-ids"), []).append(actions.get("var0"))
        if not any(len([v for v in values if v and v.isdigit()]) >= 2 for values in per_npc.values()):
            continue
        counters = summary_counters(quest_id)
        if not counters:
            continue
        groups = sorted({(n - 2) // 3 for n, _m in counters if n % 3 == 2})
        stages = sorted({int(v) for values in per_npc.values() for v in values if v and v.isdigit()})
        hits.append((quest_id, stages, groups, counters, client.get(quest_id)))
    print(f"stage-walk kill quests with client counters: {len(hits)}")
    for quest_id, stages, groups, counters, rows in hits:
        # 计数槽 group 非 0 时，只有服务端真实写入该 SECTION 才会有分子；var0 行走本身不喂槽。
        flag = "OK" if set(groups) <= {0} else "COUNTER_SECTION_MISSING"
        print(f"  quest={quest_id} kill-var0={stages} client-counter-groups={groups} slots={counters} {flag}")
        if rows:
            print(f"      client rows: {rows}")


if __name__ == "__main__":
    main()
