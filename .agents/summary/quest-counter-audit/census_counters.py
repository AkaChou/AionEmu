#!/usr/bin/env python3
"""只读普查：任务 XML 击杀计数器目标值 vs 客户端 quest_monster.csv SECTION_1 门控。"""
import csv, re
from collections import Counter
from pathlib import Path

CLIENT = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_monster.csv")
QUESTS = Path("src/main/resources/aion/data/static_data/quest_definition/quests")

gates = {}
for row in csv.reader(CLIENT.open(encoding="utf-8", errors="ignore")):
    if len(row) < 2 or not row[0].isdigit():
        continue
    for value in re.findall(r"SECTION_1<(\d+)", row[1]):
        gates.setdefault(int(row[0]), []).append(int(value))

delta = Counter()
examples = {}
for path in sorted(QUESTS.glob("*.xml")):
    qid = int(path.stem)
    if qid not in gates or len(gates[qid]) != 1:
        continue
    text = path.read_text(encoding="utf-8")
    if "kill-npc" not in text:
        continue
    sets = [int(v) for f, v in re.findall(r'<set-variable field="(var1)" value="(\d+)"/>', text)]
    if not sets:
        continue
    target = max(sets)
    gate = gates[qid][0]
    key = target - gate
    delta[key] += 1
    examples.setdefault(key, []).append(qid)

print("目标值 - 客户端门控 的分布（SECTION_1 单行任务）:")
for key in sorted(delta):
    print(f"  delta={key:+d}: {delta[key]} 个任务  e.g. {examples[key][:12]}")
