#!/usr/bin/env python3
"""子区扫描：把 LF6/DF6 子区任务族的击杀目标补齐为客户端契约声明的全部变体。

幂等：已补齐的文件第二次运行不会产生变化。
"""
from __future__ import annotations
import re, csv, pathlib, collections

ROOT = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
SD = ROOT / "src/main/resources/aion/data/static_data"
Q = SD / "quest_definition/quests"
CLIENT_CSV = pathlib.Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_monster.csv")

name_to_ids: dict[str, list[int]] = collections.defaultdict(list)
for f in (SD / "npcs").glob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    for tag in re.finditer(r'<npc_template\b[^>]*>', t):
        seg = tag.group(0)
        nid = re.search(r'npc_id="(\d+)"', seg)
        nm = re.search(r'name_desc="([^"]*)"', seg)
        if nid and nm:
            name_to_ids[nm.group(1).lower()].append(int(nid.group(1)))

contract: dict[int, set[int]] = collections.defaultdict(set)
for row in csv.reader(CLIENT_CSV.open(encoding="utf-8", errors="ignore")):
    if len(row) < 6 or not row[0].isdigit():
        continue
    tokens = [tok for cell in row[5:] for tok in re.split(r'[,\s]+', cell.strip().lower())
              if tok and re.fullmatch(r'[a-z0-9_]+', tok) and '_' in tok and not tok.startswith('section')]
    for name in tokens:
        contract[int(row[0])].update(name_to_ids.get(name, []))

TARGETS = [int(x) for x in """25506 25507 25509 25510 25512 25513 25515 25516 25518 25519 25521 25522 25524 25525
25527 25528 25530 25533 25534 25640 42003 42004 42005 42006 42103 42104 42105 42106 51077 51078""".split()]

KILL_NPC = re.compile(r'<kill-npc npc-ids="([0-9 ]+)"/>')
summary = []
for qid in TARGETS:
    path = Q / f"{qid}.xml"
    text = path.read_text(encoding="utf-8")
    registered = {int(x) for a in KILL_NPC.findall(text) for x in a.split()} | \
                 {int(b) for b in re.findall(r'<kill-npc npc-id="(\d+)"', text)}
    wanted = registered | contract.get(qid, set())
    if not registered or not contract.get(qid):
        raise SystemExit(f"{qid}: missing registered targets or client contract")
    new_attr = f'<kill-npc npc-ids="{" ".join(str(i) for i in sorted(wanted))}"/>'
    text, count = KILL_NPC.subn(new_attr, text)
    if count == 0:
        raise SystemExit(f"{qid}: no kill-npc set found")
    path.write_text(text, encoding="utf-8")
    summary.append((qid, len(registered), len(wanted), count))

for qid, before, after, count in summary:
    print(f"{qid}: {before} -> {after} ids across {count} route(s)")
print("patched", len(summary), "quest definitions")
