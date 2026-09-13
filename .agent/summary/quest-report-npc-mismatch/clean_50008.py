import xml.etree.ElementTree as ET

XML_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"

for qid, start_npc, end_npc in [("50008", "831038", "831036"), ("51008", "831039", "831037")]:
    path = f"{XML_DIR}/{qid}.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    trans = root.find("transitions")
    for d in list(trans.findall('dialog[@type="NPC_START"]')):
        if d.get('npc-id') != start_npc:
            trans.remove(d)
        else:
            d.set('selection-sources', 'unaccepted')
    for d in list(trans.findall('dialog[@type="NPC_REPORT"]')):
        if d.get('npc-id') != end_npc:
            trans.remove(d)
    for d in list(trans.findall('npc-complete')):
        if d.get('npc-id') != end_npc:
            trans.remove(d)
    tree.write(path, encoding="utf-8", xml_declaration=True)
    print(f"Cleaned {qid}")
