#!/usr/bin/env python3
"""审计接取携带道具：真端 inventory_item_name 与生产 inventory-items 的差异。

Audit the accept-time carried-item contract. Aion 5.8 distinguishes:

* ``inventory_item_nameN``: the item the player must already carry to accept;
* ``check_itemN``: the item checked/progressed later in the quest.

The private ``quest_data.xml`` migration previously conflated both fields into
``inventory-items``.  The typed engine treats ``inventory-items`` as a
start-eligibility gate, so copying a ``check_item`` there makes a normal quest
unacceptable (the 30721/Sedative case).

The script regenerates the retail-backed test baseline and writes the remaining
opposite-axis gaps (retail inventory_item_name absent from production).
"""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ITEMS = ROOT / "src/main/resources/aion/data/static_data/items"
QUESTS = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
RETAIL = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml")
BASELINE = ROOT / "src/test/resources/quest/quest-inventory-start-item-retail-contract.tsv"
GAPS = ROOT / ".agents/summary/quest-inventory-start-item/inventory-start-item-gaps.tsv"


def item_ids_by_dev_name() -> dict[str, int]:
    result: dict[str, int] = {}
    for path in ITEMS.rglob("*.xml"):
        for match in re.finditer(r"<item_template\b[^>]*>", path.read_text(errors="ignore")):
            body = match.group(0)
            item_id = re.search(r'\bid="(\d+)"', body)
            name = re.search(r'name_desc="([^"]*)"', body)
            if item_id and name:
                result.setdefault(name.group(1).lower(), int(item_id.group(1)))
    return result


def retail_inventory_names() -> dict[int, list[str]]:
    result: dict[int, list[str]] = {}
    for block in re.findall(r"<quest>(.*?)</quest>",
            RETAIL.read_text(encoding="utf-8", errors="ignore"), re.S):
        quest_id = re.search(r"<id>(\d+)</id>", block)
        if not quest_id:
            continue
        names = [value.strip().split()[0]
                 for field, value in re.findall(r"<([a-z_0-9]+)>([^<]*)</\1>", block)
                 if field.startswith("inventory_item_name") and value.strip()]
        if names:
            result[int(quest_id.group(1))] = names
    return result


def production_inventory_items() -> tuple[set[int], dict[int, set[int]]]:
    quest_ids: set[int] = set()
    result: dict[int, set[int]] = {}
    for path in QUESTS.glob("*.xml"):
        quest_ids.add(int(path.stem))
        root = ET.parse(path).getroot()
        item_ids = {int(element.get("id")) for element in
            root.findall("./metadata/inventory-items/item") if element.get("id")}
        if item_ids:
            result[int(path.stem)] = item_ids
    return quest_ids, result


def main() -> None:
    name_to_id = item_ids_by_dev_name()
    rows: list[tuple[int, list[int]]] = []
    unresolved: list[tuple[int, str]] = []
    for quest_id, names in sorted(retail_inventory_names().items()):
        ids = []
        complete = True
        for name in names:
            item_id = name_to_id.get(name.lower())
            if item_id is None:
                unresolved.append((quest_id, name))
                complete = False
            else:
                ids.append(item_id)
        if complete:
            rows.append((quest_id, sorted(set(ids))))

    BASELINE.write_text(
        "# questId\tretailInventoryItemNameIds\n"
        + "\n".join(f"{quest_id}\t{','.join(map(str, ids))}" for quest_id, ids in rows) + "\n",
        encoding="utf-8")

    production_quest_ids, production = production_inventory_items()
    baseline = dict(rows)
    extra = []
    for quest_id, item_ids in sorted(production.items()):
        unexpected = sorted(item_ids - set(baseline.get(quest_id, [])))
        if unexpected:
            extra.append((quest_id, unexpected))

    missing_file = []
    missing_gate = []
    for quest_id, item_ids in sorted(baseline.items()):
        absent = sorted(set(item_ids) - production.get(quest_id, set()))
        if not absent:
            continue
        row = (quest_id, absent)
        if quest_id not in production_quest_ids:
            missing_file.append(row)
        else:
            missing_gate.append(row)

    GAPS.write_text(
        "# questId\tstatus\tretailInventoryItemNameIds\n"
        + "\n".join(f"{quest_id}\tNO_QUEST_XML\t{','.join(map(str, item_ids))}"
                     for quest_id, item_ids in missing_file)
        + "\n"
        + "\n".join(f"{quest_id}\tXML_MISSING_GATE\t{','.join(map(str, item_ids))}"
                     for quest_id, item_ids in missing_gate) + "\n",
        encoding="utf-8")

    print(f"retail quests with inventory_item_name: {len(retail_inventory_names())}")
    print(f"resolved baseline rows: {len(rows)}; unresolved names: {len(unresolved)}")
    print(f"production quests with inventory-items: {len(production)}")
    print(f"extra inventory-items not backed by retail inventory_item_name: {len(extra)}")
    for quest_id, item_ids in extra:
        print(f"  - {quest_id}: {item_ids}")
    print(f"retail inventory_item_name with no production XML: {len(missing_file)}")
    print(f"retail inventory_item_name missing from an existing XML: {len(missing_gate)} -> {GAPS.name}")
    if unresolved:
        print("unresolved names:")
        for quest_id, name in unresolved:
            print(f"  - {quest_id}: {name}")


if __name__ == "__main__":
    main()
