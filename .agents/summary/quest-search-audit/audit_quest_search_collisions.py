import os
import re
import csv
import glob
from pathlib import Path
from collections import defaultdict

# 1. Load NPC templates: ID -> name, desc, etc.
# Check sources for client NPCs
npc_sources = glob.glob("/Users/mc/PycharmProjects/unpak/npcs_unpacked/client_npcs_*.xml")
print(f"Found {len(npc_sources)} NPC source files")

npc_id_to_desc = {} # npc_id -> desc (e.g. STR_NPC_Ventus)
npc_id_to_name = {} # npc_id -> name (internal dev name)
desc_to_npc_ids = defaultdict(set)

npc_block_regex = re.compile(r'<npc_client>(.*?)</npc_client>', re.DOTALL)
id_regex = re.compile(r'<id>(\d+)</id>')
name_regex = re.compile(r'<name>([^<]+)</name>')
desc_regex = re.compile(r'<desc>([^<]+)</desc>')

for src in npc_sources:
    with open(src, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    for m in npc_block_regex.finditer(content):
        block = m.group(1)
        id_m = id_regex.search(block)
        if not id_m:
            continue
        nid = int(id_m.group(1))
        name_m = name_regex.search(block)
        name = name_m.group(1).strip() if name_m else ""
        desc_m = desc_regex.search(block)
        desc = desc_m.group(1).strip() if desc_m else ""

        npc_id_to_name[nid] = name
        npc_id_to_desc[nid] = desc
        if desc:
            desc_to_npc_ids[desc].add(nid)

print(f"Loaded {len(npc_id_to_name)} client NPCs, {len(desc_to_npc_ids)} distinct desc tags")

# Find descs that map to multiple NPC IDs
ambiguous_descs = {desc: ids for desc, ids in desc_to_npc_ids.items() if len(ids) > 1}
print(f"Found {len(ambiguous_descs)} ambiguous desc tags with multiple NPC IDs")

# 2. Also check Chinese body names in string files
# Strings where body is same
string_sources = [
    "/Users/mc/PycharmProjects/unpak/data_unpacked/Strings/client_strings_monster.xml",
    "/Users/mc/PycharmProjects/unpak/data_unpacked/Strings/client_strings_npc.xml",
    "/Users/mc/PycharmProjects/unpak/data_unpacked/Strings/client_strings_dic_people.xml",
    "/Users/mc/PycharmProjects/unpak/data_unpacked/Strings/client_strings_dic_monster.xml"
]

desc_to_body = {}
body_to_descs = defaultdict(set)

str_block_regex = re.compile(r'<string>(.*?)</string>', re.DOTALL)
body_regex = re.compile(r'<body>([^<]+)</body>')

for src in string_sources:
    if not os.path.exists(src):
        continue
    with open(src, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    for m in str_block_regex.finditer(content):
        block = m.group(1)
        name_m = name_regex.search(block)
        body_m = body_regex.search(block)
        if name_m and body_m:
            s_name = name_m.group(1).strip()
            # body might be "Name;Description..." in dictionary
            raw_body = body_m.group(1).strip()
            display_name = raw_body.split(';')[0].strip()
            desc_to_body[s_name] = display_name
            body_to_descs[display_name].add(s_name)

print(f"Loaded {len(desc_to_body)} string entries, {len(body_to_descs)} distinct body names")

# Check display names that map to multiple NPC IDs across different descs
body_to_npc_ids = defaultdict(set)
for desc, ids in desc_to_npc_ids.items():
    if desc in desc_to_body:
        body = desc_to_body[desc]
        body_to_npc_ids[body].update(ids)

ambiguous_bodies = {body: ids for body, ids in body_to_npc_ids.items() if len(ids) > 1}
print(f"Found {len(ambiguous_bodies)} display names that map to multiple NPC IDs across all NPCs")

# 3. Now let's check which active quests actually use these ambiguous NPCs or dic links
quest_files = glob.glob("src/main/resources/aion/data/static_data/quest_definition/quests/*.xml")
print(f"Scanning {len(quest_files)} server quest XML files...")

quest_npc_regex = re.compile(r'npc-id="(\d+)"')

# Check which quests reference any of the ambiguous NPC IDs
quest_to_ambiguous_npcs = defaultdict(dict)

for qf in quest_files:
    qid = int(Path(qf).stem)
    with open(qf, 'r', encoding='utf-8') as f:
        q_content = f.read()

    referenced_npcs = {int(x) for x in quest_npc_regex.findall(q_content)}
    for nid in referenced_npcs:
        # Check if this NPC shares a display name with other NPCs
        desc = npc_id_to_desc.get(nid, "")
        body = desc_to_body.get(desc, "")
        if body and len(body_to_npc_ids[body]) > 1:
            all_nids = body_to_npc_ids[body]
            quest_to_ambiguous_npcs[qid][nid] = {
                "body": body,
                "desc": desc,
                "competing_nids": all_nids - {nid}
            }

print(f"Found {len(quest_to_ambiguous_npcs)} quests referencing an NPC with ambiguous display name")

# Print top examples
out_csv = ".agents/summary/quest-search-audit/quest_npc_name_collisions.csv"
with open(out_csv, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(["quest_id", "quest_npc_id", "display_name", "desc_tag", "competing_npc_ids", "competing_names"])
    for qid in sorted(quest_to_ambiguous_npcs.keys()):
        for nid, info in quest_to_ambiguous_npcs[qid].items():
            competing = sorted(list(info["competing_nids"]))
            competing_names = [f"{cnid}:{npc_id_to_name.get(cnid, '')}({npc_id_to_desc.get(cnid, '')})" for cnid in competing]
            writer.writerow([
                qid, nid, info["body"], info["desc"],
                " ".join(map(str, competing)),
                " | ".join(competing_names)
            ])

print(f"Written collision report to {out_csv}")
