import xml.etree.ElementTree as ET

XML_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"

def clean_quest(qid, start_npcs, end_npcs):
    path = f"{XML_DIR}/{qid}.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    trans = root.find("transitions")
    
    # 1. remove NPC_START not in start_npcs
    for d in list(trans.findall('dialog[@type="NPC_START"]')):
        nid = d.get('npc-id')
        if nid not in start_npcs:
            trans.remove(d)
        else:
            d.set('selection-sources', 'unaccepted')
            
    # 2. remove NPC_REPORT not in end_npcs
    for d in list(trans.findall('dialog[@type="NPC_REPORT"]')):
        nid = d.get('npc-id')
        if nid not in end_npcs:
            trans.remove(d)
            
    # 3. remove npc-complete not in end_npcs
    for d in list(trans.findall('npc-complete')):
        nid = d.get('npc-id')
        if nid not in end_npcs:
            trans.remove(d)

    # 4. remove stray transitions jumping to reward from start_npcs
    for t in list(trans.findall('transition')):
        d = t.find('.//dialog')
        if d is not None:
            nid = d.get('npc-id')
            if nid in start_npcs and t.get('target') == 'reward' and t.get('source') != 'unaccepted':
                trans.remove(t)
            elif nid in start_npcs and t.get('action') == 'SELECT2_1':
                trans.remove(t)

    tree.write(path, encoding="utf-8", xml_declaration=True)
    print(f"Cleaned {qid}")

# Apply to the 11 quests:
clean_quest("13700", ["804699"], ["802350"])
clean_quest("13701", ["798926"], ["802350"])
clean_quest("18940", ["802431"], ["802383"])
clean_quest("23700", ["804719"], ["802353"])
clean_quest("23701", ["799225"], ["802353"])
clean_quest("28940", ["802433"], ["802384"])
clean_quest("39003", ["800500"], ["800504"])
clean_quest("49003", ["800502"], ["800505"])
clean_quest("18737", ["804707"], ["206378", "206379", "206380"])
clean_quest("28737", ["804732"], ["206395", "206396", "206397"])
clean_quest("13962", ["835218"], ["835217"])

