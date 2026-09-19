#!/usr/bin/env python3
"""
Statically verifies quest 10506 XML definition for corridor portal routes and schema validity.
"""

import sys
from pathlib import Path
import lxml.etree as etree

def main():
    root_dir = Path(__file__).resolve().parents[3]
    schema_path = root_dir / "src/main/resources/aion/data/static_data/quest_definition/quest_definition.xsd"
    xml_path = root_dir / "src/main/resources/aion/data/static_data/quest_definition/quests/10506.xml"
    aion_xml_path = root_dir / "aion/data/static_data/quest_definition/quests/10506.xml"

    # 1. Verify schema
    with open(schema_path, "rb") as sf:
        schema_root = etree.XML(sf.read())
    schema = etree.XMLSchema(schema_root)

    with open(xml_path, "rb") as xf:
        doc = etree.parse(xf)
    schema.assertValid(doc)
    print("[PASS] src/.../10506.xml is valid against quest_definition.xsd")

    with open(aion_xml_path, "rb") as axf:
        adoc = etree.parse(axf)
    schema.assertValid(adoc)
    print("[PASS] aion/.../10506.xml is valid against quest_definition.xsd")

    # 2. Check sync between src and aion
    assert xml_path.read_text(encoding="utf-8") == aion_xml_path.read_text(encoding="utf-8"), "src and aion mismatch"
    print("[PASS] src and aion 10506.xml are byte-identical")

    # 3. Check transitions
    root = doc.getroot()
    transitions = root.findall(".//transition")

    routes = []
    for t in transitions:
        src = t.get("source")
        tgt = t.get("target")
        evt_el = t.find("event")
        if evt_el is not None and len(evt_el) > 0:
            child = evt_el[0]
            routes.append((src, tgt, child.tag, dict(child.attrib)))

    # Ensure s2 has no 702666 or 702667
    s2_portals = [r for r in routes if r[0] == "s2" and (
        r[3].get("npc-id") in ("702666", "702667") or r[3].get("template-id") in ("702666", "702667")
    )]
    assert len(s2_portals) == 0, f"s2 should not contain portals, found: {s2_portals}"
    print("[PASS] s2 has no premature portal routes")

    # Ensure s4 has 702666 and 702667 (talk and can-act)
    s4_talk_entrance = [r for r in routes if r[0] == "s4" and r[2] == "dialog" and r[3].get("npc-id") == "702666"]
    s4_talk_exit = [r for r in routes if r[0] == "s4" and r[2] == "dialog" and r[3].get("npc-id") == "702667"]
    s4_can_entrance = [r for r in routes if r[0] == "s4" and r[2] == "can-act" and r[3].get("template-id") == "702666"]
    s4_can_exit = [r for r in routes if r[0] == "s4" and r[2] == "can-act" and r[3].get("template-id") == "702667"]
    assert len(s4_talk_entrance) == 1 and len(s4_talk_exit) == 1 and len(s4_can_entrance) == 1 and len(s4_can_exit) == 1
    print("[PASS] s4 has complete portal entrance and exit routes")

    # Ensure s5 has 702666 and 702667 (talk and can-act)
    s5_talk_entrance = [r for r in routes if r[0] == "s5" and r[2] == "dialog" and r[3].get("npc-id") == "702666"]
    s5_talk_exit = [r for r in routes if r[0] == "s5" and r[2] == "dialog" and r[3].get("npc-id") == "702667"]
    s5_can_entrance = [r for r in routes if r[0] == "s5" and r[2] == "can-act" and r[3].get("template-id") == "702666"]
    s5_can_exit = [r for r in routes if r[0] == "s5" and r[2] == "can-act" and r[3].get("template-id") == "702667"]
    assert len(s5_talk_entrance) == 1 and len(s5_talk_exit) == 1 and len(s5_can_entrance) == 1 and len(s5_can_exit) == 1
    print("[PASS] s5 has complete portal entrance and exit routes")

    print("[ALL CHECKS PASSED]")

if __name__ == "__main__":
    main()
