import xml.etree.ElementTree as ET

XML_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"

# 3329
tree = ET.parse(f"{XML_DIR}/3329.xml")
trans = tree.getroot().find("transitions")
# Add transition for SETPRO1 at 203909
t = ET.SubElement(trans, "transition", {"source": "a0b0", "target": "a0b0"})
ev = ET.SubElement(t, "event")
ET.SubElement(ev, "dialog", {"type": "TALK_TO_NPC", "npc-id": "203909", "action": "SETPRO1"})
ac = ET.SubElement(t, "after-commit")
ET.SubElement(ac, "close-dialog")
tree.write(f"{XML_DIR}/3329.xml", encoding="utf-8", xml_declaration=True)

# 16900..16903
for qid, start_npc in [("16900", "203901"), ("16901", "203901"), ("16902", "204500"), ("16903", "204500")]:
    tree = ET.parse(f"{XML_DIR}/{qid}.xml")
    trans = tree.getroot().find("transitions")
    t = ET.SubElement(trans, "transition", {"source": "started", "target": "started"})
    ev = ET.SubElement(t, "event")
    ET.SubElement(ev, "dialog", {"type": "TALK_TO_NPC", "npc-id": start_npc, "action": "SETPRO1"})
    ac = ET.SubElement(t, "after-commit")
    ET.SubElement(ac, "close-dialog")
    tree.write(f"{XML_DIR}/{qid}.xml", encoding="utf-8", xml_declaration=True)
    print(f"Added SETPRO1 close-dialog for {qid} on {start_npc}")

