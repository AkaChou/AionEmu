#!/usr/bin/env python3
"""生产任务掉落契约与真端 quest.xml 的“按怪物加权”结构化比对（名称无关）。

Retail quest.xml names quest items and monsters, so ids cannot be resolved offline.
This audit compares the retail contract per monster instead: for every monster that
can drop the quest item, which chance applies, and how many distinct quest items are
dropped overall. Re-grouping the same monsters into different production drop rows is
therefore treated as equivalent, while a missing/extra monster or a different chance
is reported.
"""
from __future__ import annotations

import re
from collections import Counter
from pathlib import Path
from xml.etree import ElementTree as ET

RETAIL = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml")
QUEST_DIR = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
DROP_FIELD = re.compile(r"^drop_(monster|item|prob)_(\d+)$")


def retail_contract() -> dict[int, dict[str, object]]:
    root = ET.parse(RETAIL).getroot()
    out: dict[int, dict[str, object]] = {}
    for quest in root.findall("quest"):
        raw = quest.findtext("id")
        if not raw:
            continue
        qid = int(raw)
        groups: dict[int, dict[str, str]] = {}
        for child in quest:
            m = DROP_FIELD.match(child.tag)
            if not m:
                continue
            text = (child.text or "").strip()
            if text:
                groups.setdefault(int(m.group(2)), {})[m.group(1)] = text
        drops = [g for g in groups.values() if g.get("monster") and g.get("item")]
        if not drops:
            continue
        monster_chances: Counter[int] = Counter()
        items: set[str] = set()
        for group in drops:
            chance = int(group.get("prob") or "0")
            items.add(group["item"])
            for _ in group["monster"].split():
                monster_chances[chance] += 1
        out[qid] = {"monster_chances": monster_chances, "items": len(items)}
    return out


def production_contract(root: ET.Element) -> dict[str, object]:
    monster_chances: Counter[int] = Counter()
    items: set[str] = set()
    for d in root.iter("drop"):
        items.add(d.get("item-id"))
        monster_chances[int(d.get("chance") or "0")] += 1
    return {"monster_chances": monster_chances, "items": len(items)}


def main() -> None:
    retail = retail_contract()
    findings: list[str] = []
    scanned = 0
    for path in sorted(QUEST_DIR.glob("*.xml"), key=lambda p: int(p.stem) if p.stem.isdigit() else 0):
        if not path.stem.isdigit():
            continue
        qid = int(path.stem)
        contract = retail.get(qid)
        if contract is None:
            continue
        scanned += 1
        prod = production_contract(ET.parse(path).getroot())
        problems = []
        if prod["monster_chances"] != contract["monster_chances"]:
            problems.append("chances retail=%s prod=%s" % (dict(contract["monster_chances"]), dict(prod["monster_chances"])))
        if prod["items"] != contract["items"]:
            problems.append(f"items retail={contract['items']} prod={prod['items']}")
        if problems:
            findings.append(f"{qid}\t" + "; ".join(problems))
    print(f"QUESTS_WITH_RETAIL_DROPS={scanned}")
    print(f"FINDINGS={len(findings)}")
    for f in findings:
        print(f)


if __name__ == "__main__":
    main()
