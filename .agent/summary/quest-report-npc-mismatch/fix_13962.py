import xml.etree.ElementTree as ET

path = "src/main/resources/aion/data/static_data/quest_definition/quests/13962.xml"
tree = ET.parse(path)
trans = tree.getroot().find("transitions")

# Add NPC_REPORT on 835217
ET.SubElement(trans, "dialog", {
    "type": "NPC_REPORT",
    "npc-id": "835217",
    "source": "started",
    "target": "reward",
    "page": "DEFAULT_SUCCESS"
})

# Add npc-complete on 835217
comp = ET.SubElement(trans, "npc-complete", {
    "npc-id": "835217",
    "source": "reward",
    "target": "complete",
    "fixed-reward-indices": "0 1 2",
    "actions": "SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD",
    "complete-reward-index": "0",
    "finish": "SELECTION_DIALOG"
})
ET.SubElement(comp, "preview", {"actions": "USE_OBJECT SELECT_QUEST_REWARD"})

tree.write(path, encoding="utf-8", xml_declaration=True)
print("Fixed 13962")
