import xml.etree.ElementTree as ET

XML_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"

def clean_clones(qid, start_npc, end_npc):
    path = f"{XML_DIR}/{qid}.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    trans = root.find("transitions")
    
    # 1. remove NPC_START not matching start_npc
    for d in list(trans.findall('dialog[@type="NPC_START"]')):
        nid = d.get('npc-id')
        if nid != start_npc:
            trans.remove(d)
        else:
            d.set('selection-sources', 'unaccepted')
            
    # 2. remove NPC_REPORT not matching end_npc
    for d in list(trans.findall('dialog[@type="NPC_REPORT"]')):
        nid = d.get('npc-id')
        if nid != end_npc:
            trans.remove(d)
            
    # 3. remove npc-complete not matching end_npc
    for d in list(trans.findall('npc-complete')):
        nid = d.get('npc-id')
        if nid != end_npc:
            trans.remove(d)

    # 4. remove stray transitions jumping to reward from non-end npc
    for t in list(trans.findall('transition')):
        d = t.find('.//dialog')
        if d is not None:
            nid = d.get('npc-id')
            if nid != end_npc and t.get('target') == 'reward' and t.get('source') != 'unaccepted':
                trans.remove(t)

    # Write back
    tree.write(path, encoding="utf-8", xml_declaration=True)
    print(f"Cleaned {qid}: start={start_npc}, end={end_npc}")

clean_clones("3977", "204656", "204656")
clean_clones("14200", "798155", "798155")
clean_clones("80601", "831831", "831831")
clean_clones("80606", "831832", "831832")
clean_clones("3319", "798050", "798050")
clean_clones("4501", "204728", "204728")
