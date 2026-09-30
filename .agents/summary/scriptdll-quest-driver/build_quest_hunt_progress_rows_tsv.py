#!/usr/bin/env python3
"""生成客户端 hunt 进度行登记表 quest_client_hunt_progress_rows.tsv（混合链 SECTION 校准）。

数据源 = 原始客户端 quest_monster.csv 的 simpleQuest 行。每行给出：行阶梯（SECTION_0==k）、
计数段（SECTION_m<count，m>=1）、计数目标（门中的 count）与该行全部怪物名（第 7 列起）。
校准判据（混合链客户端 SECTION 缺陷返工片）：
- 同一 ladder_row 的行 = 同一 hunt 块的并行目标（18990 形：僵尸 SECTION_1 / 公主 SECTION_2）；
- 多段共享计数（15306 形：五段全 SECTION_1，行阶梯区分段）；
- 行按 ladder_row 升序分组后与 hunt 步块序一一对应（组数 != hunt 步数 → 编译器如实 DEFER）。
非 simpleQuest 行（questItemDropMonster 等物件行）不是计数行，跳过；无计数段的门
（Progress(1) 等）也跳过。生成器只做扫描/聚合，不做改名。
"""
import os
import collections
import csv
import pathlib
import re

CLIENT_CSV = pathlib.Path(f"{os.environ.get('AION_UNPACK_ROOT', os.path.expanduser('~/PycharmProjects/unpak'))}/Quest_unpacked/quest_monster.csv")
ROOT = pathlib.Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
OUT = ROOT / "src/main/resources/aion/data/static_data/quest_retail/quest_client_hunt_progress_rows.tsv"

GATE = re.compile(r'^Progress\(SECTION_0==(\d+)(?:; SECTION_([1-9]\d*)<(\d+))?\)$')

rows_by_quest = collections.defaultdict(list)
skipped_forms = collections.Counter()
simple_rows = 0
with CLIENT_CSV.open(encoding="utf-8-sig") as f:
    for r in csv.reader(f):
        if not r or not r[0].strip().isdigit():
            continue
        if len(r) < 7 or r[3].strip() != "simpleQuest":
            continue
        simple_rows += 1
        m = GATE.match(r[1].strip())
        if not m:
            skipped_forms[r[1].strip()] += 1
            continue
        ladder_row = int(m.group(1))
        if m.group(2) is None:
            skipped_forms["no-counter-section"] += 1
            continue
        section = int(m.group(2))
        count = int(m.group(3))
        monsters = [c.strip() for c in r[6:] if c.strip()]
        if not monsters:
            skipped_forms["no-monsters"] += 1
            continue
        qid = int(r[0].strip())
        rows_by_quest[qid].append((ladder_row, section, count, monsters))

lines = [
    "# 客户端 hunt 进度行登记表（混合链 SECTION 校准；source: Quest_unpacked/quest_monster.csv simpleQuest 行）",
    "# Client hunt progress rows (mixed-chain SECTION calibration; simpleQuest rows of quest_monster.csv).",
    "# quest_id\tladder_row\tsection\tcount\tmonsters(';' joined)",
]
for qid in sorted(rows_by_quest):
    for ladder_row, section, count, monsters in sorted(rows_by_quest[qid]):
        lines.append(f"{qid}\t{ladder_row}\t{section}\t{count}\t{';'.join(monsters)}")
OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")

quests = len(rows_by_quest)
total_rows = sum(len(v) for v in rows_by_quest.values())
multi = sum(1 for v in rows_by_quest.values()
            if len({(lr, sr) for lr, sr, _, _ in v}) != len({lr for lr, _, _, _ in v}))
print(f"quests={quests} rows={total_rows} simpleQuest_rows={simple_rows} "
      f"multi-section-quests={multi} skipped={dict(skipped_forms)}")
for probe in (15101, 15304, 15314, 25304, 25314, 18990, 18992, 15306, 25306):
    v = rows_by_quest.get(probe, [])
    groups = collections.defaultdict(list)
    for lr, sr, ct, mn in v:
        groups[lr].append((sr, ct, len(mn)))
    print(f"  {probe}: groups={sorted((k, sorted(g)) for k, g in groups.items())}")
