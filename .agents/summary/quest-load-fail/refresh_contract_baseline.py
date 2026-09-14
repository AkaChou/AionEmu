#!/usr/bin/env python3
from __future__ import annotations

import csv
import sys
from collections import Counter
from pathlib import Path


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: refresh_contract_baseline.py <audit.csv> <baseline.tsv>")

    audit_path = Path(sys.argv[1])
    baseline_path = Path(sys.argv[2])
    fingerprints: set[str] = set()
    counts: Counter[str] = Counter()
    with audit_path.open(encoding="utf-8-sig", newline="") as source:
        for row in csv.DictReader(source):
            reason = row["unresolved_reason"]
            if reason.startswith("compiled IR emits a task page absent from the active client page index"):
                failure_type = "PAGE_NOT_IN_TASK_HTML"
            elif reason.startswith("visible client action has no route"):
                failure_type = "BUTTON_WITHOUT_ROUTE"
            else:
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
    baseline_path.write_text(
        "\n".join(header + sorted(fingerprints)) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
