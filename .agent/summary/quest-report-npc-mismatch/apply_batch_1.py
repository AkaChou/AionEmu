import re
import os

XML_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"

def read_xml(qid):
    with open(f"{XML_DIR}/{qid}.xml", "r", encoding="utf-8") as f:
        return f.read()

def write_xml(qid, content):
    with open(f"{XML_DIR}/{qid}.xml", "w", encoding="utf-8") as f:
        f.write(content)

# 1. Simple ID replacements:
simple_replaces = {
    "2561": ("295178", "204753"),
    "2632": ("204799", "832820"),
    "3714": ("203844", "279045"),
    "3716": ("203844", "279045"),
    "18033": ("801281", "801037"),
    "30614": ("800327", "800326"),
    "80216": ("831024", "831025"),
    "80225": ("831027", "831026"),
}

for qid, (old_id, new_id) in simple_replaces.items():
    content = read_xml(qid)
    content = content.replace(f'npc-id="{old_id}"', f'npc-id="{new_id}"')
    write_xml(qid, content)
    print(f"Updated {qid}: {old_id} -> {new_id}")

# 2. Quests where NPC_REPORT and npc-complete were on start NPC instead of end NPC:
report_replaces = {
    "2485": ("203331", "204407"),
    "11139": ("799075", "798979"),
    "11455": ("799070", "798946"),
    "13917": ("802350", "802328"),
    "17550": ("806134", "806789"),
    "18250": ("806134", "798604"),
    "18251": ("806729", "798604"),
    "28601": ("204702", "205234"),
}

for qid, (start_id, end_id) in report_replaces.items():
    content = read_xml(qid)
    # replace report and complete
    content = re.sub(rf'<dialog type="NPC_REPORT" npc-id="{start_id}"', rf'<dialog type="NPC_REPORT" npc-id="{end_id}"', content)
    content = re.sub(rf'<npc-complete npc-id="{start_id}"', rf'<npc-complete npc-id="{end_id}"', content)
    # clean up selection-sources="unaccepted started" to "unaccepted"
    content = content.replace('selection-sources="unaccepted started"', 'selection-sources="unaccepted"')
    write_xml(qid, content)
    print(f"Updated {qid}: report/complete {start_id} -> {end_id}")

