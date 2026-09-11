#!/usr/bin/env python3
from __future__ import annotations

import csv
import re
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
AUDIT = ROOT / ".agent/summary/quest-load-fail/quest-order-audit-current.csv"
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"


ROUTES = {
    "1007": ("ASK_QUEST_ACCEPT", "SHOW_ASK_QUEST_ACCEPT_WINDOW"),
    "1002": ("QUEST_ACCEPT_1", "QUEST_ACCEPT_1"),
    "1003": ("QUEST_REFUSE_1", "QUEST_REFUSE_1"),
}


def render(source: str, npc_id: str, action: str, page: str) -> str:
    return (
        f'    <transition source="{source}" target="{source}">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="{action}"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        f'        <dialog type="SHOW_QUEST_PAGE" page="{page}"/>\n'
        '      </after-commit>\n'
        '    </transition>\n'
    )


def render_close(source: str, npc_id: str) -> str:
    return (
        f'    <transition source="{source}" target="{source}">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="FINISH_DIALOG"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        '        <close-dialog/>\n'
        '      </after-commit>\n'
        '    </transition>\n'
    )


def has_transition(document: ET.Element, source: str, npc_id: str, action: str) -> bool:
    for transition in document.findall("./transitions/transition"):
        if transition.get("source") != source:
            continue
        for event in transition.findall("./event/dialog"):
            if event.get("npc-id") == npc_id and event.get("action") == action:
                return True
    return False


def main() -> int:
    with AUDIT.open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))

    grouped: dict[tuple[int, str, str], set[str]] = {}
    for row in rows:
        if (not row["unresolved_reason"].startswith("visible client action has no route")
                or row["client_visible_action"] not in {"1007", "1002", "1003"}
                or not row["npc_id"]):
            continue
        key = (int(row["quest_id"]), row["server_source_state"], row["npc_id"])
        grouped.setdefault(key, set()).add(row["client_visible_action"])

    changed = 0
    for (quest_id, source, npc_id), actions in sorted(grouped.items()):
        path = QUEST_DIR / f"{quest_id}.xml"
        text = path.read_text(encoding="utf-8")
        document = ET.fromstring(text)
        additions = ""
        for visible in ("1007", "1002", "1003"):
            if visible not in actions:
                continue
            action, page = ROUTES[visible]
            if not has_transition(document, source, npc_id, action):
                additions += render(source, npc_id, action, page)
        if not has_transition(document, source, npc_id, "FINISH_DIALOG"):
            additions += render_close(source, npc_id)
        if not additions:
            continue
        closing = text.rfind("</transitions>")
        if closing < 0:
            raise RuntimeError(f"missing transitions closing tag: {path}")
        updated = text[:closing] + additions + text[closing:]
        ET.fromstring(updated)
        path.write_text(updated, encoding="utf-8")
        changed += 1

    print(f"insertions={changed} candidates={len(grouped)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
