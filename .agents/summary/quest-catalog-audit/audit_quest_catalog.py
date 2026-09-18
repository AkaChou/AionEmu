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
        sid_txt = sid.text.strip() if sid.text else ""
        sname_txt = sname.text.strip() if sname.text else ""
        sbody_txt = sbody.text if sbody is not None and sbody.text else ""
        name_to_string[sname_txt] = (sid_txt, sbody_txt)
        id_to_string[sid_txt] = (sname_txt, sbody_txt)

client_quest_path = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"
tree_quest = ET.parse(client_quest_path)
client_quests = {}
for q in tree_quest.getroot().findall("quest"):
    qid = q.find("id")
    if qid is not None and qid.text:
        qid_txt = qid.text.strip()
        desc = q.find("desc")
        desc_txt = desc.text.strip() if desc is not None and desc.text else ""
        client_quests[qid_txt] = {
            "desc": desc_txt,
        }

catalog_path = "docs/QUEST_CATALOG.zh-CN.md"
catalog_rows = {}
with open(catalog_path, "r", encoding="utf-8") as fp:
    for line in fp:
        m = re.match(r"^\|\s*(\d+)\s*\|\s*([^|]+)\|\s*([^|]+)\|\s*([^|]+)\|", line)
        if m:
            catalog_rows[m.group(1)] = {
                "level": m.group(2).strip(),
                "name_en": m.group(3).strip(),
                "name_zh": m.group(4).strip(),
                "line": line
            }

prod_dir = "src/main/resources/aion/data/static_data/quest_definition/quests"
mismatches = []

for fn in sorted(os.listdir(prod_dir), key=lambda x: int(x[:-4]) if x[:-4].isdigit() else 999999):
    if not fn.endswith(".xml"):
        continue
    qid = fn[:-4]
    filepath = os.path.join(prod_dir, fn)
    with open(filepath, "r", encoding="utf-8") as fp:
        content = fp.read(1000)

    m = re.search(r'<metadata\s+name="([^"]*)"\s+display-name-id="([^"]*)"', content)
    if not m:
        m = re.search(r'<metadata\s+display-name-id="([^"]*)"\s+name="([^"]*)"', content)
        if m:
            d_id, d_name = m.group(1), m.group(2)
        else:
            d_id, d_name = None, None
    else:
        d_name, d_id = m.group(1), m.group(2)

    cat = catalog_rows.get(qid, {})
    cat_zh = cat.get("name_zh", "")

    if qid in client_quests:
        desc = client_quests[qid]["desc"]
        if desc in name_to_string:
            c_sid, c_zh = name_to_string[desc]
            if d_id != c_sid or cat_zh != c_zh:
                mismatches.append({
                    "qid": qid,
                    "server_id": d_id,
                    "client_id": c_sid,
                    "server_en": d_name,
                    "catalog_zh": cat_zh,
                    "client_zh": c_zh,
                    "desc": desc
                })

print(f"Total mismatching quests: {len(mismatches)}")
name_different = [m for m in mismatches if m["catalog_zh"] != m["client_zh"]]
print(f"Total where catalog_zh != client_zh: {len(name_different)}")
for m in name_different:
    print(f"QID {m['qid']}: catalog='{m['catalog_zh']}' vs client='{m['client_zh']}' (server_id={m['server_id']}, client_id={m['client_id']})")
