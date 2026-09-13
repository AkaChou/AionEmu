import xml.etree.ElementTree as ET

XML_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"

two_npc_quests = {
    "3329": ("203909", "203956"),
    "13702": ("802350", "802352"),
    "13705": ("802331", "802333"),
    "13963": ("835218", "835217"),
    "13964": ("835218", "835217"),
    "16837": ("806564", "806568"),
    "16900": ("203901", "203965"),
    "16901": ("203901", "203989"),
    "16902": ("204500", "204612"),
    "16903": ("204500", "204656"),
    "16979": ("802025", "801762"),
    "16986": ("804862", "804864"),
    "16988": ("801953", "804865"),
    "18208": ("205316", "205309"),
    "18832": ("830365", "830001"),
    "24112": ("203631", "832821"),
    "24151": ("204715", "204801"),
    "2654": ("204775", "204655"),
    "26979": ("802026", "801764"),
    "28832": ("830532", "830085"),
}

for qid, (start_npc, end_npc) in two_npc_quests.items():
    path = f"{XML_DIR}/{qid}.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    trans = root.find("transitions")
    
    # 1. remove NPC_START on end_npc
    for d in list(trans.findall('dialog[@type="NPC_START"]')):
        nid = d.get('npc-id')
        if nid == end_npc:
            trans.remove(d)
        elif nid == start_npc:
            d.set('selection-sources', 'unaccepted')
            
    # 2. remove NPC_REPORT on start_npc
    for d in list(trans.findall('dialog[@type="NPC_REPORT"]')):
        nid = d.get('npc-id')
        if nid == start_npc:
            trans.remove(d)
            
    # 3. remove npc-complete on start_npc
    for d in list(trans.findall('npc-complete')):
        nid = d.get('npc-id')
        if nid == start_npc:
            trans.remove(d)

    # 4. In 24151: remove duplicate NPC_REPORT on end_npc if multiple
    seen_rep = set()
    for d in list(trans.findall('dialog[@type="NPC_REPORT"]')):
        nid = d.get('npc-id')
        page = d.get('page')
        key = (nid, page)
        if key in seen_rep:
            trans.remove(d)
        else:
            seen_rep.add(key)

    # 5. remove stray transitions jumping to reward from start_npc
    for t in list(trans.findall('transition')):
        d = t.find('.//dialog')
        if d is not None:
            nid = d.get('npc-id')
            if nid == start_npc and t.get('target') == 'reward' and t.get('source') != 'unaccepted':
                trans.remove(t)
            elif nid == end_npc and t.get('source') == 'unaccepted':
                trans.remove(t)
            elif nid == end_npc and t.get('action') == 'QUEST_SELECT' and t.get('target') != 'reward':
                trans.remove(t)

    tree.write(path, encoding="utf-8", xml_declaration=True)
    print(f"Cleaned 2-NPC {qid}: start={start_npc}, end={end_npc}")

# Multi-start 18826, 28826:
def clean_multi_start(qid, start_npcs, end_npc):
    path = f"{XML_DIR}/{qid}.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    trans = root.find("transitions")
    for d in list(trans.findall('dialog[@type="NPC_START"]')):
        nid = d.get('npc-id')
        if nid == end_npc:
            trans.remove(d)
        elif nid in start_npcs:
            d.set('selection-sources', 'unaccepted')
    for d in list(trans.findall('dialog[@type="NPC_REPORT"]')):
        nid = d.get('npc-id')
        if nid != end_npc:
            trans.remove(d)
    for d in list(trans.findall('npc-complete')):
        nid = d.get('npc-id')
        if nid != end_npc:
            trans.remove(d)
    tree.write(path, encoding="utf-8", xml_declaration=True)
    print(f"Cleaned multi-start {qid}")

clean_multi_start("18826", ["830660", "830661"], "730522")
clean_multi_start("28826", ["830662", "830663"], "730525")
