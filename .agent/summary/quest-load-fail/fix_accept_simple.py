#!/usr/bin/env python3
"""为接取简报页(select_none 4762)的 QUEST_ACCEPT_SIMPLE/QUEST_REFUSE_SIMPLE 按钮补标准路由。

Adds the standard simple-accept/refuse routes for quests whose briefing page
(select_none 4762) carries QUEST_ACCEPT_SIMPLE/QUEST_REFUSE_SIMPLE buttons.
Reads the accept-start manifest for (quest, npc, none, start) wiring; idempotent.
"""
from __future__ import annotations

import csv
import hashlib
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / ".agent/summary/quest-load-fail"))
from fix_notask_pages import client_model, quest_xml_path  # noqa: E402

MANIFEST_IN = ROOT / ".agent/summary/quest-load-fail/accept-start-manifest.csv"
MANIFEST_OUT = ROOT / ".agent/summary/quest-load-fail/accept-simple-manifest.csv"

ACCEPT_TMPL = (
    '\n    <transition source="{none}" target="{start}">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_ACCEPT_SIMPLE"/>\n'
    '      </event>\n'
    '      <conditions>\n'
    '        <start-eligible/>\n'
    '      </conditions>\n'
    '      <after-commit>\n'
    '        <sync-quest-state mode="VISIBILITY_REFRESH"/>\n'
    '        <close-dialog/>\n'
    '      </after-commit>\n'
    '    </transition>')
REFUSE_TMPL = (
    '\n    <transition source="{none}" target="{none}">\n'
    '      <event>\n'
    '        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_REFUSE_SIMPLE"/>\n'
    '      </event>\n'
    '      <after-commit>\n'
    '        <close-dialog/>\n'
    '      </after-commit>\n'
    '    </transition>')


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def main() -> int:
    pages, actions = client_model()
    edits: dict[Path, list[tuple]] = defaultdict(list)
    rows = []
    seen = set()
    for row in csv.DictReader(MANIFEST_IN.open(encoding="utf-8-sig", newline="")):
        if row["decision"] not in ("add-briefing-pair", "add-npc-start"):
            continue
        q, npc, none, start = (int(row["quest_id"]), row["npc"], row["none"],
                               row.get("start") or row["none"])
        key = (q, npc)
        if key in seen:
            continue
        seen.add(key)
        btns = actions.get(q, {}).get(4762, set())
        need = [a for a in ("QUEST_ACCEPT_SIMPLE", "QUEST_REFUSE_SIMPLE") if a in btns]
        if not need:
            continue
        path = quest_xml_path(q)
        root = ET.parse(path).getroot()
        missing = []
        for action in need:
            exists = any(
                t.get("source") == none
                and any(e.get("npc-id") == npc and e.get("action") == action
                        for e in t.findall("./event/dialog"))
                for t in root.findall("./transitions/transition"))
            if not exists:
                missing.append(action)
        if not missing:
            continue
        rows.append({"quest_id": q, "npc": npc, "none": none, "start": start,
                     "missing": " ".join(missing)})
        edits[path].append((npc, none, start, missing))

    with MANIFEST_OUT.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["quest_id", "npc", "none", "start", "missing"])
        writer.writeheader()
        writer.writerows(rows)
    print(f"quests needing simple-accept routes: {len(edits)}")
    if "--write" not in sys.argv:
        return 0
    for path, items in edits.items():
        before = path.read_bytes()
        s = before.decode("utf-8")
        for npc, none, start, missing in items:
            block = ""
            if "QUEST_ACCEPT_SIMPLE" in missing:
                block += ACCEPT_TMPL.format(npc=npc, none=none, start=start)
            if "QUEST_REFUSE_SIMPLE" in missing:
                block += REFUSE_TMPL.format(npc=npc, none=none)
            s = s.replace("</transitions>", block + "</transitions>", 1)
        ET.fromstring(s)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change: {path}")
        path.write_text(s, encoding="utf-8")
    print(f"patched files: {len(edits)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
