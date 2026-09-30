#!/usr/bin/env python3
"""生成逐段击杀目标登记表 quest_client_kill_targets_stages.tsv（多段 DD hunt 行的变体轴裁定）。

数据源 = 原始客户端 quest_monster.csv（每 SECTION 一行、每行给出该段全部名字变体：
base 66/67 + T_ 实刷 66/67）。名字经 npc_template `name_desc`（小写）解析为 npc id 集合。
段号取 Progress 门里的计数段 `SECTION_n<count`（SECTION_0 是行标记，段从 1 起），
与 DD 表分号段顺序（BoundCounter.slot 1..N）一一对应。
当前覆盖 = 15546/25546（仅有的多段 + 客户端名单超集行）；后续多段行按需追加。
"""
import os
import csv
import pathlib
import re
import collections

ROOT = pathlib.Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
SD = ROOT / "src/main/resources/aion/data/static_data"
CLIENT_CSV = pathlib.Path(f"{os.environ.get('AION_UNPACK_ROOT', os.path.expanduser('~/PycharmProjects/unpak'))}/Quest_unpacked/quest_monster.csv")
OUT = SD / "quest_retail/quest_client_kill_targets_stages.tsv"
QUESTS = [15546, 25546]

name_to_ids = collections.defaultdict(list)
for f in (SD / "npcs").glob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    for tag in re.finditer(r'<npc_template\b[^>]*>', t):
        seg = tag.group(0)
        nid = re.search(r'npc_id="(\d+)"', seg)
        nm = re.search(r'name_desc="([^"]*)"', seg)
        if nid and nm:
            name_to_ids[nm.group(1).lower()].append(int(nid.group(1)))

rows_out = []
problems = []
for qid in QUESTS:
    per_stage = collections.defaultdict(set)
    seen_sections = []
    with CLIENT_CSV.open(encoding="utf-8-sig") as f:
        for r in csv.reader(f):
            if not r or r[0].strip() != str(qid):
                continue
            gate = r[1]
            m = re.search(r'SECTION_(\d+)<(\d+)', gate)
            if not m:
                problems.append((qid, gate, "no counter section"))
                continue
            slot = int(m.group(1))
            seen_sections.append((slot, int(m.group(2))))
            for tok in [x.strip().lower() for cell in r[6:] for x in cell.split() if x.strip()]:
                ids = name_to_ids.get(tok)
                if not ids:
                    problems.append((qid, tok, "unresolved name"))
                    continue
                per_stage[slot].update(ids)
    slots = sorted(per_stage)
    if not slots:
        problems.append((qid, "-", "no stage rows"))
        continue
    print(f'quest {qid}: slots {slots} required {sorted(set(s for _, s in seen_sections))}')
    for slot in slots:
        rows_out.append((qid, slot, " ".join(str(i) for i in sorted(per_stage[slot]))))

OUT.write_text(
    "# 逐段击杀目标登记表（多段 DD hunt 行的变体轴裁定；段号 = DD 计数槽 1..N）\n"
    "# source: Quest_unpacked/quest_monster.csv 每 SECTION 行的名字变体经 npc_template name_desc 解析\n"
    "# 生成脚本: .agents/summary/scriptdll-quest-driver/generate_stage_kill_targets.py\n"
    "# quest_id\tstage\ttargets\n" +
    "".join(f"{qid}\t{slot}\t{targets}\n" for qid, slot, targets in rows_out),
    encoding="utf-8")
print(f'wrote {OUT} rows={len(rows_out)} problems={problems}')
