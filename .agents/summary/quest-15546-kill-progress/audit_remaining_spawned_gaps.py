#!/usr/bin/env python3
"""对剩余"只登记未刷新基础变体"的任务，列出客户端契约声明的、世界中确实刷新的缺口 ID。"""
from __future__ import annotations
import re, csv, pathlib, collections

ROOT = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
SD = ROOT / "src/main/resources/aion/data/static_data"
Q = SD / "quest_definition/quests"
CSV = pathlib.Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_monster.csv")

names = collections.defaultdict(list)
for f in (SD / "npcs").glob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    for tag in re.finditer(r'<npc_template\b[^>]*>', t):
        seg = tag.group(0)
        nid = re.search(r'npc_id="(\d+)"', seg)
        nm = re.search(r'name_desc="([^"]*)"', seg)
        if nid and nm:
            names[nm.group(1).lower()].append(int(nid.group(1)))

active = {int(x) for x in re.findall(r'<map id="(\d+)"',
             re.sub(r'<!--.*?-->', '', (SD / "world_maps.xml").read_text(encoding="utf-8"), flags=re.S))}
spawned = set()
for f in (SD / "spawns").rglob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    m = re.search(r'spawn_map map_id="(\d+)"', t)
    if m and int(m.group(1)) in active:
        spawned |= {int(x) for x in re.findall(r'npc_id="(\d+)"', t)}

rows = collections.defaultdict(list)
for row in csv.reader(CSV.open(encoding="utf-8", errors="ignore")):
    if len(row) >= 8 and row[0].isdigit():
        names_in_row = [x.strip().lower() for cell in row[7:] for x in cell.replace(",", " ").split() if x.strip()]
        rows[int(row[0])].append((row[1], names_in_row))

TARGETS = [int(x) for x in """25506 25507 25509 25510 25512 25513 25515 25516 25518 25519 25521 25522 25524 25525
25527 25528 25530 25533 25534 25640 42003 42004 42005 42006 42103 42104 42105 42106 51077 51078""".split()]
for qid in TARGETS:
    text = (Q / f"{qid}.xml").read_text(encoding="utf-8")
    reg = {int(x) for a in re.findall(r'<kill-npc npc-ids="([0-9 ]+)"', text) for x in a.split()} | \
          {int(b) for b in re.findall(r'<kill-npc npc-id="(\d+)"', text)}
    sections = [c for c, _ in rows.get(qid, [])]
    want = {i for _, ms in rows.get(qid, []) for n in ms for i in names.get(n, [])}
    missing_live = sorted((want & spawned) - reg)
    missing_all = sorted(want - reg)
    print(f"{qid}\treg={len(reg)}\tclient={len(want)}\tsections={len(sections)}\tmissing_live={len(missing_live)}\textra={len(reg-want)}")
    if missing_live:
        print(f"    {missing_live}")
