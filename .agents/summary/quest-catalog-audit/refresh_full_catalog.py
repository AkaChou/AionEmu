import os
import re
import xml.etree.ElementTree as ET

REPO_ROOT = "/Users/mc/IdeaProjects/AionEmu-test"
PROD_DIR = os.path.join(REPO_ROOT, "src/main/resources/aion/data/static_data/quest_definition/quests")
CATALOG_PATH = os.path.join(REPO_ROOT, "docs/QUEST_CATALOG.zh-CN.md")
CLIENT_STRINGS_PATH = "/Users/mc/PycharmProjects/unpak/data_unpacked/Strings/client_strings_quest.xml"

# 1. Load client strings
tree_strings = ET.parse(CLIENT_STRINGS_PATH)
id_to_zh = {}
for s in tree_strings.getroot().findall("string"):
    sid = s.find("id")
    sbody = s.find("body")
    if sid is not None and sid.text:
        id_to_zh[sid.text.strip()] = sbody.text if sbody is not None and sbody.text else ""

# 2. Parse all production XMLs
prod_metadata = {}
for fn in sorted(os.listdir(PROD_DIR), key=lambda x: int(x[:-4]) if x[:-4].isdigit() else 999999):
    if not fn.endswith(".xml"):
        continue
    qid = fn[:-4]
    filepath = os.path.join(PROD_DIR, fn)
    tree = ET.parse(filepath)
    root = tree.getroot()
    meta = root.find("metadata")
    if meta is None:
        continue

    name = meta.attrib.get("name", "")
    disp_id = meta.attrib.get("display-name-id", "")
    min_lvl = int(meta.attrib.get("min-level", "1"))
    max_lvl = int(meta.attrib.get("max-level", "2147483647"))
    lvl_str = f"{min_lvl}-{max_lvl}" if max_lvl != 2147483647 else f"{min_lvl}+"

    # Prereqs
    prereq_ids = []
    prereqs = meta.find("prerequisites")
    if prereqs is not None:
        for q in prereqs.findall("quest"):
            if "id" in q.attrib:
                prereq_ids.append(q.attrib["id"])
    start_conds = meta.find("start-conditions")
    if start_conds is not None:
        for cond in start_conds.findall("condition"):
            if cond.attrib.get("type") == "finished" and "quest-id" in cond.attrib:
                prereq_ids.append(cond.attrib["quest-id"])
    start_groups = meta.find("start-condition-groups")
    if start_groups is not None:
        for group in start_groups.findall("group"):
            for cond in group.findall("condition"):
                if cond.attrib.get("type") == "finished" and "quest-id" in cond.attrib:
                    prereq_ids.append(cond.attrib["quest-id"])
    unique_prereqs = []
    for pid in prereq_ids:
        if pid not in unique_prereqs:
            unique_prereqs.append(pid)
    prereq_str = ", ".join(unique_prereqs) if unique_prereqs else "无"

    # Zh title from client strings
    if disp_id in id_to_zh and id_to_zh[disp_id]:
        zh_title = id_to_zh[disp_id]
    else:
        zh_title = f"未找到客户端名称 (nameId={disp_id})"

    prod_metadata[qid] = {
        "name": name,
        "disp_id": disp_id,
        "level": lvl_str,
        "zh": zh_title,
        "prereqs": prereq_str,
    }

# 3. Read and update CATALOG
with open(CATALOG_PATH, "r", encoding="utf-8") as fp:
    lines = fp.readlines()

new_lines = []
zh_updated = 0
en_updated = 0
prereq_updated = 0
level_updated = 0

for line in lines:
    if line.startswith("> 生成日期："):
        new_lines.append("> 生成日期：2026-09-18\n")
        continue
    m = re.match(r"^\|\s*(\d+)\s*\|(.*)$", line)
    if not m:
        new_lines.append(line)
        continue
    qid = m.group(1)
    cols = [c.strip() for c in line.split("|")[1:-1]]
    # cols: 0:qid, 1:level, 2:en, 3:zh, 4:restr, 5:prereq, 6:acquire, 7:file
    if qid in prod_metadata:
        pm = prod_metadata[qid]

        # Level
        if cols[1] != pm["level"]:
            level_updated += 1
            cols[1] = pm["level"]

        # EN Name
        if cols[2] != pm["name"]:
            en_updated += 1
            cols[2] = pm["name"]

        # ZH Name
        if cols[3] != pm["zh"]:
            zh_updated += 1
            cols[3] = pm["zh"]

        # Prereqs
        if cols[5] != pm["prereqs"]:
            prereq_updated += 1
            cols[5] = pm["prereqs"]

        # Reconstruct line:
        new_line = f"| {cols[0]} | {cols[1]} | {cols[2]} | {cols[3]} | {cols[4]} | {cols[5]} | {cols[6]} | {cols[7]} |\n"
        new_lines.append(new_line)
    else:
        new_lines.append(line)

print(f"Catalog stats: ZH updated={zh_updated}, EN updated={en_updated}, Prereqs updated={prereq_updated}, Levels updated={level_updated}")

with open(CATALOG_PATH, "w", encoding="utf-8") as fp:
    fp.writelines(new_lines)

print("Saved updated catalog.")
