#!/usr/bin/env python3
"""核对修复后的 15 个任务满足"族内已刷新变体必须全部登记"的不变量（供 Java 门禁对齐）。"""
from __future__ import annotations
import re, pathlib, collections

ROOT = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
SD = ROOT / "src/main/resources/aion/data/static_data"
Q = SD / "quest_definition/quests"

active_maps = {int(x) for x in re.findall(r'<map id="(\d+)"',
                re.sub(r'<!--.*?-->', '', (SD / "world_maps.xml").read_text(encoding="utf-8"), flags=re.S))}
print("active maps containing 210100000/220110000/600200000:",
      {m: m in active_maps for m in (210100000, 220110000, 600200000)})

names: dict[int, str] = {}
for f in (SD / "npcs").glob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    for tag in re.finditer(r'<npc_template\b[^>]*>', t):
        seg = tag.group(0)
        nid = re.search(r'npc_id="(\d+)"', seg)
        nm = re.search(r'name_desc="([^"]*)"', seg)
        if nid and nm:
            names[int(nid.group(1))] = nm.group(1)

spawned: set[int] = set()
for f in (SD / "spawns").rglob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    mid = re.search(r'spawn_map map_id="(\d+)"', t)
    if not mid or int(mid.group(1)) not in active_maps:
        continue
    spawned |= {int(x) for x in re.findall(r'npc_id="(\d+)"', t)}

def family(name: str) -> str | None:
    m = re.fullmatch(r'(?:lf6|df6)_(?:t_)?(.+?)_\d+_[a-z]{1,2}', name.lower())
    return m.group(1) if m else None

by_family = collections.defaultdict(set)
for i, n in names.items():
    fam = family(n)
    if fam:
        by_family[fam].add(i)

QUESTS = [15546, 25546, 25500, 25501, 25503, 25504, 42001, 42002, 80891, 80892, 80897, 80898, 80927, 80928, 80929]
failures = []
for qid in QUESTS:
    text = (Q / f"{qid}.xml").read_text(encoding="utf-8")
    reg = {int(x) for a in re.findall(r'<kill-npc npc-ids="([0-9 ]+)"', text) for x in a.split()} | \
          {int(b) for b in re.findall(r'<kill-npc npc-id="(\d+)"', text)}
    fams = {family(names[i]) for i in reg if family(names.get(i, ""))}
    missing = set()
    for fam in fams:
        missing |= {i for i in by_family[fam] if i in spawned} - reg
    live = reg & spawned
    status = "OK" if not missing and live else "FAIL"
    print(f"{qid}: {status} registered={len(reg)} live={len(live)} missing_spawned_siblings={sorted(missing)}")
    if missing or not live:
        failures.append(qid)
print("failures:", failures)
