#!/usr/bin/env python3
"""硬缺陷：击杀路线登记的目标全无刷新，但同族兄弟模板在同一区域有刷新。"""
from __future__ import annotations
import re, pathlib, collections

ROOT = pathlib.Path(__file__).resolve().parents[3]
SD = ROOT / "src/main/resources/aion/data/static_data"
QUESTS = SD / "quest_definition/quests"

names: dict[int, str] = {}
for f in (SD / "npcs").glob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    for m in re.finditer(r'npc_template\b[^>]*?npc_id="(\d+)"[^>]*?>', t):
        n = re.search(r'name_desc="([^"]*)"', m.group(0))
        if n:
            names[int(m.group(1))] = n.group(1)
active_maps = {int(x) for x in re.findall(
    r'<map id="(\d+)"',
    re.sub(r'<!--.*?-->', '', (SD / "world_maps.xml").read_text(encoding="utf-8"), flags=re.S))}
spawned: set[int] = set()
for f in (SD / "spawns").rglob("*.xml"):
    text = f.read_text(errors="ignore", encoding="utf-8")
    declared = re.search(r'spawn_map map_id="(\d+)"', text)
    if not declared or int(declared.group(1)) not in active_maps:
        continue
    spawned |= {int(x) for x in re.findall(r'npc_id="(\d+)"', text)}

def family(name: str):
    m = re.match(r'^(LF6|DF6)_(T_)?(.+?)_(\d+)_([a-z]{1,2})$', name)
    return (m.group(1), m.group(3).lower(), int(m.group(4))) if m else None

by_family = collections.defaultdict(set)
for i, n in names.items():
    fam = family(n)
    if fam:
        by_family[(fam[0], fam[1])].add(i)

for q in sorted(QUESTS.glob("*.xml"), key=lambda p: int(p.stem)):
    text = q.read_text(encoding="utf-8")
    groups = [sorted(int(x) for x in a.split()) if a else [int(b)]
              for a, b in re.findall(r'<kill-npc npc-ids="([0-9 ]+)"|<kill-npc npc-id="(\d+)"', text)]
    if not groups:
        continue
    reg = {i for g in groups for i in g}
    if any(i in spawned for i in reg):
        continue
    live_siblings = set()
    for i in reg:
        fam = family(names.get(i, ""))
        if fam:
            live_siblings |= {j for j in by_family[(fam[0], fam[1])] if j in spawned}
    if live_siblings:
        meta = re.search(r'name="([^"]*)"', text)
        race = re.search(r'<race id="([A-Z]+)"', text)
        print(f"{q.stem}\t{race.group(1) if race else '?'}\t{meta.group(1) if meta else '?'}")
        print(f"    registered: {' '.join(f'{i}({names.get(i,'?')})' for i in sorted(reg))}")
        print(f"    spawned siblings: {' '.join(f'{j}({names.get(j,'?')})' for j in sorted(live_siblings))}")
