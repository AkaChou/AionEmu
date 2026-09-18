import os
import re
import xml.etree.ElementTree as ET

client_strings_path = "/Users/mc/PycharmProjects/unpak/data_unpacked/Strings/client_strings_quest.xml"
tree_strings = ET.parse(client_strings_path)
id_to_string = {}
name_to_string = {}
for s in tree_strings.getroot().findall("string"):
    sid = s.find("id")
    sname = s.find("name")
    sbody = s.find("body")
    if sid is not None and sname is not None:
        name_to_string[sname.text.strip()] = (sid.text.strip(), sbody.text if sbody is not None and sbody.text else "")
        id_to_string[sid.text.strip()] = (sname.text.strip(), sbody.text if sbody is not None and sbody.text else "")

client_quest_path = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"
tree_quest = ET.parse(client_quest_path)
client_quests = {}
for q in tree_quest.getroot().findall("quest"):
    qid = q.find("id")
    if qid is not None and qid.text:
        desc = q.find("desc")
        client_quests[qid.text.strip()] = desc.text.strip() if desc is not None and desc.text else ""

prod_dir = "src/main/resources/aion/data/static_data/quest_definition/quests"
quest_data_path = "src/main/resources/aion/data/static_data/quest_data/quest_data.xml"

tree_qd = ET.parse(quest_data_path)
qd_map = {q.attrib["id"]: q.attrib.get("nameId") for q in tree_qd.getroot().findall("quest") if "id" in q.attrib}

fix_candidates = []
for fn in sorted(os.listdir(prod_dir), key=lambda x: int(x[:-4]) if x[:-4].isdigit() else 999999):
    if not fn.endswith(".xml"):
        continue
    qid = fn[:-4]
    with open(os.path.join(prod_dir, fn), "r", encoding="utf-8") as fp:
        content = fp.read(1000)
    m = re.search(r'<metadata[^>]*display-name-id="([^"]*)"', content)
    if not m:
        continue
    server_disp_id = m.group(1)
    qd_name_id = qd_map.get(qid)

    client_desc = client_quests.get(qid)
    if client_desc and client_desc in name_to_string:
        expected_id, expected_zh = name_to_string[client_desc]
        if server_disp_id != expected_id or qd_name_id != expected_id:
            curr_zh = id_to_string.get(server_disp_id, ("", "NOT_FOUND"))[1]
            fix_candidates.append((qid, server_disp_id, qd_name_id, expected_id, curr_zh, expected_zh))

print(f"Total: {len(fix_candidates)}")
for c in fix_candidates:
    print(f"QID {c[0]}: xml={c[1]}, qd={c[2]} -> expected={c[3]} | current_name='{c[4]}' -> expected_name='{c[5]}'")
