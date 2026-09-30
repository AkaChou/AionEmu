#!/usr/bin/env python3
# 批 1（哨兵接取族 192 行）逐行取证：模板表字段 × NPC 名解析 × 区域绑定 × 客户端入口证据。
# Batch-1 per-row forensics for the acquire-sentinel family.
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

import os
import re
from collections import Counter

ROOT = f"{REPO}"
QR = f"{ROOT}/src/main/resources/aion/data/static_data/quest_retail"
NPC_INDEX = f"{ROOT}/.agents/summary/scriptdll-quest-driver/npc_name_index.tsv"
AREAS = f"{ROOT}/src/main/resources/aion/definitions/compact/ai/ai-areas.xml"
CLIENT = f"{ROOT}/docs/quest/client-dialog-mapping/client-lifecycle-alignment.csv"

BATCH1 = f"/tmp/b1-rows.tsv"

# --- npc name index: name -> ids
npc_ids = {}
with open(NPC_INDEX, encoding="utf-8") as fh:
    header = fh.readline()
    for line in fh:
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 2:
            npc_ids[parts[0]] = parts[1]

# --- area bindings
bound = set()
with open(AREAS, encoding="utf-8") as fh:
    for m in re.finditer(r'<quest_area\b[^>]*>', fh.read()):
        q = re.search(r'quests="([^"]*)"', m.group(0))
        if q:
            for tok in q.group(1).split():
                if tok.isdigit():
                    bound.add(int(tok))

# --- client lifecycle rows per quest
client = {}
import csv
with open(CLIENT, encoding="utf-8-sig") as fh:
    for row in csv.DictReader(fh):
        qid = int(row["quest_id"])
        client.setdefault(qid, []).append(
            f"{row['route_type']}@{row['npc_id']}:{row['source_status']}->{row['target_status']}"
            f"/p{row['actual_page']}")

# --- retail template rows per family
def parse_ids(path):
    rows = {}
    cur = None
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            m = re.match(r'\s*<id id="(\d+)">', line)
            if m:
                cur = int(m.group(1)); rows[cur] = {}
                continue
            if cur is not None:
                m = re.match(r'\s*<([a-z_0-9]+)>(.*)</[a-z_0-9]+>', line)
                if m and m.group(1) != "progress_info":
                    rows[cur][m.group(1)] = m.group(2).strip()
                elif line.strip() == "</id>":
                    cur = None
    return rows

hunt = parse_ids(f"{QR}/Quest_SimpleHunt.xml")
talk = parse_ids(f"{QR}/Quest_SimpleTalk.xml")
itemplay = parse_ids(f"{QR}/Quest_SimpleItemPlay.xml")

# --- data driven rows
dd = {}
cur = None
with open(f"{QR}/data_driven_quest.xml", encoding="utf-8") as fh:
    for line in fh:
        m = re.match(r'\s*<id>(\d+)</id>', line)
        if m:
            cur = int(m.group(1)); dd[cur] = {"steps": []}
            continue
        if cur is not None:
            if re.match(r'\s*</quest_data_driven>', line):
                cur = None; continue
            m = re.match(r'\s*<category_acquire_>(.*)</category_acquire_>', line)
            if m: dd[cur]["acq"] = m.group(1); continue
            m = re.match(r'\s*<acquire_param_>(.*)</acquire_param_>', line)
            if m: dd[cur]["acqparam"] = m.group(1); continue
            m = re.match(r'\s*<reward_npc_name>(.*)</reward_npc_name>', line)
            if m: dd[cur]["reward"] = m.group(1); continue
            m = re.match(r'\s*<category_progress_>(.*)</category_progress_>', line)
            if m: dd[cur]["steps"].append(m.group(1))

def resolve(name):
    if name is None: return "null"
    ids = npc_ids.get(name)
    if ids is None: return "MISSING"
    n = len(ids.split(","))
    return f"unique({ids})" if n == 1 else f"multi({n})"

out = []
stats = Counter()
with open(BATCH1) as fh:
    for line in fh:
        qid_s, fam, reason = line.rstrip("\n").split("\t")
        qid = int(qid_s)
        code = reason.split(":", 1)[1]
        if fam == "DataDriven":
            d = dd.get(qid, {})
            acq = d.get("acq", "?")
            row = (qid, fam, code, f"acq={acq}", f"param={d.get('acqparam','-')}",
                   f"reward={d.get('reward','?')}", f"reward_res={resolve(d.get('reward'))}",
                   f"steps={','.join(d['steps']) or '-'}",
                   f"area_bound={qid in bound}",
                   f"client={'|'.join(client.get(qid, []))[:150]}")
        elif fam in ("SimpleHunt", "SimpleTalk", "SimpleItemPlay"):
            t = (hunt if fam == "SimpleHunt" else talk if fam == "SimpleTalk" else itemplay).get(qid, {})
            acqn = t.get("acquired_npc_name")
            row = (qid, fam, code, f"acq={acqn}", f"acq_res={resolve(acqn)}",
                   f"reward={t.get('reward_npc_name','?')}",
                   f"reward_res={resolve(t.get('reward_npc_name'))}",
                   f"area_bound={qid in bound}",
                   f"client={'|'.join(client.get(qid, []))[:150]}")
        else:
            row = (qid, fam, code, "UNKNOWN-FAMILY")
        stats[(fam, code)] += 1
        out.append("\t".join(str(x) for x in row))

with open("/tmp/b1-forensics.tsv", "w") as fh:
    fh.write("\n".join(out) + "\n")
print("rows:", len(out))
for k, v in sorted(stats.items()):
    print(k, v)
