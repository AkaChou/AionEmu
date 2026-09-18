import os
import re
import xml.etree.ElementTree as ET

REPO_ROOT = "/Users/mc/IdeaProjects/AionEmu-test"
PROD_DIR = os.path.join(REPO_ROOT, "src/main/resources/aion/data/static_data/quest_definition/quests")
QUEST_DATA_PATH = os.path.join(REPO_ROOT, "src/main/resources/aion/data/static_data/quest_data/quest_data.xml")
CLIENT_STRINGS_PATH = "/Users/mc/PycharmProjects/unpak/data_unpacked/Strings/client_strings_quest.xml"
CLIENT_QUEST_PATH = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"

# 1. Load client strings mapping
tree_strings = ET.parse(CLIENT_STRINGS_PATH)
name_to_string = {}
for s in tree_strings.getroot().findall("string"):
    sid = s.find("id")
    sname = s.find("name")
    sbody = s.find("body")
    if sid is not None and sname is not None:
        name_to_string[sname.text.strip()] = (sid.text.strip(), sbody.text if sbody is not None and sbody.text else "")

# 2. Load client quest.xml
tree_quest = ET.parse(CLIENT_QUEST_PATH)
client_quests = {}
for q in tree_quest.getroot().findall("quest"):
    qid = q.find("id")
    if qid is not None and qid.text:
        desc = q.find("desc")
        client_quests[qid.text.strip()] = desc.text.strip() if desc is not None and desc.text else ""

# 3. Identify all tasks needing updates
tasks_to_fix = {}
for fn in sorted(os.listdir(PROD_DIR), key=lambda x: int(x[:-4]) if x[:-4].isdigit() else 999999):
    if not fn.endswith(".xml"):
        continue
    qid = fn[:-4]
    client_desc = client_quests.get(qid)
    if client_desc and client_desc in name_to_string:
        expected_id, expected_zh = name_to_string[client_desc]
        tasks_to_fix[qid] = {
            "expected_id": expected_id,
            "expected_zh": expected_zh,
            "desc": client_desc
        }

print(f"Total valid client quests known in server: {len(tasks_to_fix)}")

# 4. Fix quest_definition XML
xml_modified = 0
for qid, info in tasks_to_fix.items():
    xml_path = os.path.join(PROD_DIR, qid + ".xml")
    with open(xml_path, "r", encoding="utf-8") as fp:
        content = fp.read()

    # Match display-name-id
    m = re.search(r'(<metadata\b[^>]*\bdisplay-name-id=")([^"]*)(")', content)
    if m:
        curr_id = m.group(2)
        if curr_id != info["expected_id"]:
            new_content = content[:m.start(2)] + info["expected_id"] + content[m.end(2):]
            with open(xml_path, "w", encoding="utf-8") as fp:
                fp.write(new_content)
            xml_modified += 1

print(f"Updated {xml_modified} quest_definition files.")

# 5. Fix quest_data.xml
with open(QUEST_DATA_PATH, "r", encoding="utf-8") as fp:
    qd_content = fp.read()

qd_modified = 0

def qd_replacer(match):
    global qd_modified
    prefix = match.group(1) # <quest id="
    qid = match.group(2)    # \d+
    quote = match.group(3)  # "
    attrs = match.group(4)  # rest of attributes

    if qid in tasks_to_fix:
        expected_id = tasks_to_fix[qid]["expected_id"]
        m_nid = re.search(r'\bnameId="([^"]*)"', attrs)
        if m_nid:
            if m_nid.group(1) != expected_id:
                new_attrs = attrs[:m_nid.start(1)] + expected_id + attrs[m_nid.end(1):]
                qd_modified += 1
                return f"{prefix}{qid}{quote}{new_attrs}>"
        else:
            new_attrs = f' nameId="{expected_id}"' + attrs
            qd_modified += 1
            return f"{prefix}{qid}{quote}{new_attrs}>"

    return match.group(0)

new_qd_content = re.sub(r'(<quest\b[^>]*\bid=")(\d+)(")([^>]*)>', qd_replacer, qd_content)

if qd_modified > 0:
    with open(QUEST_DATA_PATH, "w", encoding="utf-8") as fp:
        fp.write(new_qd_content)

print(f"Updated {qd_modified} entries in quest_data.xml.")
