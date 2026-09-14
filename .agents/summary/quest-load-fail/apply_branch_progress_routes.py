#!/usr/bin/env python3
from __future__ import annotations

import csv
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
AUDIT = ROOT / ".agents/summary/quest-load-fail/quest-order-audit-current.csv"
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
QUEST_IDS = {2367, 2411, 2448, 2922}
ROUTES = {
    "10009": ("SETPRO10", "10", "SELECT2"),
    "10019": ("SETPRO20", "20", "SELECT3"),
}


def render(source: str, npc_id: str, action: str, value: str, page: str) -> str:
    return (
        f'    <transition source="{source}" target="{source}">\n'
        '      <event>\n'
        f'        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="{action}"/>\n'
        '      </event>\n'
        '      <actions>\n'
        f'        <set-variable field="var0" value="{value}"/>\n'
        '      </actions>\n'
        '      <after-commit>\n'
        '        <sync-quest-state mode="PACKET_ONLY"/>\n'
        f'        <dialog type="SHOW_QUEST_PAGE" page="{page}"/>\n'
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
                or row["client_visible_action"] not in ROUTES
                or not row["npc_id"]):
            continue
        quest_id = int(row["quest_id"])
        if quest_id not in QUEST_IDS:
            continue
        key = (quest_id, row["server_source_state"], row["npc_id"])
        grouped.setdefault(key, set()).add(row["client_visible_action"])

    changed = 0
    for (quest_id, source, npc_id), visible_actions in sorted(grouped.items()):
        path = QUEST_DIR / f"{quest_id}.xml"
        text = path.read_text(encoding="utf-8")
        document = ET.fromstring(text)
        additions = ""
        for visible in sorted(visible_actions):
            action, value, page = ROUTES[visible]
            if not has_transition(document, source, npc_id, action):
                additions += render(source, npc_id, action, value, page)
        if not additions:
            continue
        closing = text.rfind("</transitions>")
        if closing < 0:
            raise RuntimeError(f"missing transitions closing tag: {path}")
        updated = text[:closing] + additions + text[closing:]
        ET.fromstring(updated)
        path.write_text(updated, encoding="utf-8")
        changed += 1

    print(f"changed_files={changed} candidates={len(grouped)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
