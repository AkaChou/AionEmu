#!/usr/bin/env python3
"""生成 Iluma/Norsvold 击杀目标契约快照，并对快照做客户端契约 + 刷怪可达性自检。"""
from __future__ import annotations
import re, csv, pathlib, collections

ROOT = pathlib.Path('/Users/mc/IdeaProjects/AionEmu-test')
SD = ROOT / "src/main/resources/aion/data/static_data"
Q = SD / "quest_definition/quests"
OUT = ROOT / "src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv"

ZONE = [int(x) for x in """15546 25546 25500 25501 25503 25504 42001 42002 80891 80892 80897 80898 80927 80928 80929
25506 25507 25509 25510 25512 25513 25515 25516 25518 25519 25521 25522 25524 25525 25527 25528
25530 25533 25534 25640 42003 42004 42005 42006 42103 42104 42105 42106 51077 51078""".split()]

name_to_ids = collections.defaultdict(list)
for f in (SD / "npcs").glob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    for tag in re.finditer(r'<npc_template\b[^>]*>', t):
        seg = tag.group(0)
        nid = re.search(r'npc_id="(\d+)"', seg)
        nm = re.search(r'name_desc="([^"]*)"', seg)
        if nid and nm:
            name_to_ids[nm.group(1).lower()].append(int(nid.group(1)))

active = {int(x) for x in re.findall(r'<map id="(\d+)"',
             re.sub(r'<!--.*?-->', '', (SD / "world_maps.xml").read_text(encoding="utf-8"), flags=re.S))}
spawned = set()
for f in (SD / "spawns").rglob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    m = re.search(r'spawn_map map_id="(\d+)"', t)
    if m and int(m.group(1)) in active:
        spawned |= {int(x) for x in re.findall(r'npc_id="(\d+)"', t)}

client = collections.defaultdict(set)
for row in csv.reader((ROOT / "docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv").open(encoding="utf-8-sig")):
    if len(row) < 8 or not row[0].isdigit():
        continue
    for tok in [x.strip().lower() for cell in row[7:] for x in cell.split() if x.strip()]:
        client[int(row[0])].update(name_to_ids.get(tok, []))

rows = []
problems = []
for qid in sorted(ZONE):
    text = (Q / f"{qid}.xml").read_text(encoding="utf-8")
    targets = {int(x) for a in re.findall(r'<kill-npc npc-ids="([0-9 ]+)"', text) for x in a.split()} | \
              {int(b) for b in re.findall(r'<kill-npc npc-id="(\d+)"', text)}
    if not targets:
        problems.append((qid, "no kill targets"))
        continue
    if not (targets & spawned):
        problems.append((qid, "no spawned target"))
    if qid in client and not client[qid] <= targets:
        problems.append((qid, f"client contract gap {sorted(client[qid] - targets)[:5]}"))
    rows.append((qid, " ".join(str(i) for i in sorted(targets))))

OUT.write_text(
    "# Aion 5.8 Iluma(210100000)/Norsvold(220110000) 击杀目标契约快照\n"
    "# source: Quest_unpacked/quest_monster.csv（客户端 SECTION 怪物名单）经 npc_template 名称->ID 解析 + 生产刷怪数据可达性自检\n"
    "# targets = 生产任务登记的全部击杀目标（客户端声明的基础变体 + 世界中刷新的 T_ 变体）\n"
    "# 生成脚本: .agents/summary/quest-15546-kill-progress/generate_kill_target_contract_tsv.py\n"
    "quest_id\ttargets\n" + "".join(f"{q}\t{t}\n" for q, t in rows),
    encoding="utf-8")
print(f"wrote {OUT.relative_to(ROOT)} rows={len(rows)}")
print("self-check problems:", problems)
