#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""M5-b2 断面：SimpleCollectItem 真端表任务的生产 XML 形状普查。

输出 TSV: quest_id, verdict, reports, completes, transitions, var0, pages, actions, npcs
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from m5a2_simple_collect_item_name_resolution import family_ids, retail_rows  # noqa: E402

REPO = Path("/Users/mc/IdeaProjects/AionEmu-test")
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"


def collect(qid):
    path = QUESTS / f"{qid}.xml"
    if not path.exists():
        return None
    root = ET.parse(path).getroot()
    pages, actions, npcs, var0 = set(), set(), set(), set()
    for node in root.findall("./nodes/node"):
        for var in node.findall("var"):
            if var.get("name") == "var0":
                var0.add(var.get("value"))
    for t in root.iter("transition"):
        ev = t.find("event")
        if ev is not None:
            for d in ev.findall("dialog"):
                if d.get("action"):
                    actions.add(d.get("action"))
                if d.get("actions"):
                    actions.update(d.get("actions").split())
                if d.get("npc-id"):
                    npcs.add(d.get("npc-id"))
            for ca in ev.findall("can-act"):
                npcs.add(ca.get("template-id"))
        for ac in t.findall("after-commit"):
            for d in ac.findall("dialog"):
                if d.get("page"):
                    pages.add(d.get("page"))
                if d.get("action"):
                    actions.add(d.get("action"))
    reports = list(root.iter("npc-item-report"))
    completes = list(root.iter("npc-complete"))
    for r in reports:
        npcs.add(r.get("npc-id"))
    for c in completes:
        npcs.add(c.get("npc-id"))
    return {
        "reports": len(reports),
        "completes": len(completes),
        "transitions": len(list(root.iter("transition"))),
        "var0": sorted(var0, key=lambda v: int(v or 0)),
        "pages": sorted(pages),
        "actions": sorted(actions),
        "npcs": sorted(npcs),
    }


def main():
    rows = retail_rows()
    ids = family_ids()
    print("quest_id\treports\tcompletes\ttransitions\tvar0\tpages\tactions\tnpcs")
    for qid in ids:
        info = collect(qid)
        if info is None:
            print(f"{qid}\tMISSING_XML")
            continue
        print("\t".join([
            str(qid), str(info["reports"]), str(info["completes"]), str(info["transitions"]),
            "|".join(info["var0"]), "|".join(info["pages"]), "|".join(info["actions"]),
            "|".join(info["npcs"]),
        ]))


if __name__ == "__main__":
    main()
