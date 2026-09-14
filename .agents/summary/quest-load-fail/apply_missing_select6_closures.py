#!/usr/bin/env python3
from __future__ import annotations

import csv
import re
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
AUDIT = ROOT / ".agents/summary/quest-load-fail/quest-order-audit-current.csv"
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
TRANSITION_PATTERN = re.compile(r"<transition\b[^>]*>.*?</transition>", re.DOTALL)


def main() -> int:
    with AUDIT.open(encoding="utf-8-sig", newline="") as stream:
        rows = [
            row for row in csv.DictReader(stream)
            if row["unresolved_reason"].startswith(
                "compiled IR emits a task page absent from the active client page index")
            and row["shown_page"] == "2716"
            and row["trigger_action"] == "39"
        ]

    grouped: dict[int, list[dict[str, str]]] = {}
    for row in rows:
        grouped.setdefault(int(row["quest_id"]), []).append(row)

    changed = 0
    unresolved: list[str] = []
    for quest_id, quest_rows in sorted(grouped.items()):
        path = QUEST_DIR / f"{quest_id}.xml"
        text = path.read_text(encoding="utf-8")
        replacements: list[tuple[int, int, str]] = []
        for match in TRANSITION_PATTERN.finditer(text):
            block = ET.fromstring(match.group(0))
            if block.get("source") != "started":
                continue
            for row in quest_rows:
                event = block.find("./event/dialog")
                page = block.find("./after-commit/dialog[@type='SHOW_QUEST_PAGE']")
                if (event is not None and page is not None
                        and event.get("npc-id") == row["npc_id"]
                        and event.get("action") == "CHECK_USER_HAS_QUEST_ITEM"
                        and page.get("page") == "SELECT6"):
                    updated = match.group(0).replace(
                        '<dialog type="SHOW_QUEST_PAGE" page="SELECT6"/>',
                        "<close-dialog/>",
                        1,
                    )
                    replacements.append((match.start(), match.end(), updated))
        if not replacements:
            unresolved.extend(row["actual_path"] for row in quest_rows)
            continue
        for start, end, updated in sorted(replacements, reverse=True):
            text = text[:start] + updated + text[end:]
        ET.fromstring(text)
        path.write_text(text, encoding="utf-8")
        changed += len(replacements)

    print(f"changed={changed} quests={len(grouped)} unresolved={len(unresolved)}")
    for value in unresolved:
        print("UNRESOLVED", value)
    return 1 if unresolved else 0


if __name__ == "__main__":
    raise SystemExit(main())
