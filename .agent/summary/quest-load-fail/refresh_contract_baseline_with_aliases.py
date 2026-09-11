#!/usr/bin/env python3
from __future__ import annotations

import csv
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
AUDIT = ROOT / ".agent/summary/quest-load-fail/quest-order-audit-current.csv"
BASELINE = ROOT / "src/test/resources/quest/quest-client-contract-baseline.tsv"
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"


def has_item_check_alias(quest_id: int, source: str, npc_id: str) -> bool:
    path = QUEST_DIR / f"{quest_id}.xml"
    document = ET.parse(path).getroot()
    for report in document.findall("./transitions/npc-item-report"):
        if report.get("source") == source and report.get("npc-id") == npc_id:
            return True
    for transition in document.findall("./transitions/transition"):
        if transition.get("source") != source:
            continue
        for event in transition.findall("./event/dialog"):
            if (event.get("npc-id") == npc_id
                    and event.get("action") == "CHECK_USER_HAS_QUEST_ITEM"):
                return True
    return False


def main() -> int:
    fingerprints: set[str] = set()
    counts: Counter[str] = Counter()
    with AUDIT.open(encoding="utf-8-sig", newline="") as source:
        for row in csv.DictReader(source):
            reason = row["unresolved_reason"]
            if reason.startswith("compiled IR emits a task page absent from the active client page index"):
                failure_type = "PAGE_NOT_IN_TASK_HTML"
            elif reason.startswith("visible client action has no route"):
                failure_type = "BUTTON_WITHOUT_ROUTE"
            else:
                continue
            if (failure_type == "BUTTON_WITHOUT_ROUTE"
                    and row["client_visible_action"] == "20002"
                    and has_item_check_alias(
                        int(row["quest_id"]), row["server_source_state"], row["npc_id"])):
                continue
            counts[failure_type] += 1
            fingerprints.add("\t".join([
                failure_type,
                row["quest_id"],
                row["server_source_state"],
                row["npc_id"],
                row["trigger_action"],
                row["shown_page"],
                row["client_visible_action"],
                row["actual_path"],
            ]))

    header = [
        "# Quest client contract baseline v1",
        "# Refreshed from the current production catalog and Aion 5.8 client mapping on 2026-09-11.",
        f"# PAGE_NOT_IN_TASK_HTML={counts['PAGE_NOT_IN_TASK_HTML']} "
        f"BUTTON_WITHOUT_ROUTE={counts['BUTTON_WITHOUT_ROUTE']}",
        "# failure_type\tquest_id\tsource_state\tnpc_id\ttrigger_action\tshown_page\t"
        "visible_action\tactual_path",
    ]
    BASELINE.write_text("\n".join(header + sorted(fingerprints)) + "\n", encoding="utf-8")
    print(f"fingerprints={len(fingerprints)} {dict(counts)} output={BASELINE}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
