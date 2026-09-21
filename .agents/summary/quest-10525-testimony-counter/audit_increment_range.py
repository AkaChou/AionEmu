#!/usr/bin/env python3
"""Audit `increment-variable` actions against the declared progress field range.

Rules reported for every production quest definition:

* OUT_OF_RANGE - the increment provably leaves the declared bit-field range.
* EXACT_COUNTER_UNBOUNDED - the quest consumes the field with an exact match
  (`variable-is`, i.e. a counter that must land on one specific value) and the
  increment has no upper bound that keeps `value + delta` inside the range.
  This is the defect class behind quest 10525 (reported runtime crash) and
  10529/20529 (stranded boss-kill step).
* UNBOUNDED - no upper bound at all, but the field is not exact-matched.
  Visibility only: most of these change step (target node != source node) or are
  `variable-at-least` overflow arms whose counting ends with the step.

Upper bounds considered: `variable-below F V` (F <= V-1), `variable-is F V`
(F == V) and a source-node projection pinning F to a constant.
"""
from __future__ import annotations

import argparse
import csv
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

DEFAULT_QUESTS = Path("src/main/resources/aion/data/static_data/quest_definition/quests")


def scan(quest_file: Path) -> list[dict]:
    root = ET.parse(quest_file).getroot()
    quest_id = root.get("id")
    progress = root.find("progress")
    maxima = {bit.get("name"): int(bit.get("max")) for bit in progress.findall("bit-field")} \
        if progress is not None else {}
    projections = {
        node.get("label"): {var.get("name"): int(var.get("value")) for var in node.findall("var")}
        for node in root.findall("./nodes/node")
    }
    exact_fields = {
        condition.get("field")
        for condition in root.findall("./transitions/transition/conditions/variable-is")
    }
    rows: list[dict] = []
    for transition in root.findall("./transitions/transition"):
        conditions = transition.findall("./conditions/*")
        for action in transition.findall("./actions/increment-variable"):
            field = action.get("field")
            delta = int(action.get("delta"))
            bounds = [int(c.get("value")) - 1 for c in conditions
                      if c.tag == "variable-below" and c.get("field") == field]
            bounds += [int(c.get("value")) for c in conditions
                       if c.tag == "variable-is" and c.get("field") == field]
            pinned = projections.get(transition.get("source"), {}).get(field)
            if pinned is not None:
                bounds.append(pinned)
            bound = min(bounds) if bounds else None
            maximum = maxima.get(field)
            if maximum is not None and bound is not None and bound + delta > maximum:
                verdict = "OUT_OF_RANGE"
            elif bound is None or maximum is None:
                verdict = "EXACT_COUNTER_UNBOUNDED" if field in exact_fields else "UNBOUNDED"
            else:
                verdict = "OK"
            rows.append({
                "quest": quest_id,
                "file": quest_file.name,
                "field": field,
                "delta": delta,
                "source": transition.get("source"),
                "target": transition.get("target"),
                "priority": transition.get("priority", ""),
                "declared_max": "" if maximum is None else maximum,
                "pre_bound": "" if bound is None else bound,
                "exact_matched": field in exact_fields,
                "verdict": verdict,
                "conditions": ",".join(
                    f"{c.tag}:{c.get('field')}={c.get('value')}" for c in conditions),
            })
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--quests", type=Path, default=DEFAULT_QUESTS)
    parser.add_argument("--csv", type=Path, default=None)
    args = parser.parse_args()

    rows: list[dict] = []
    for quest_file in sorted(args.quests.glob("*.xml")):
        rows.extend(scan(quest_file))

    by_verdict: dict[str, list[dict]] = defaultdict(list)
    for row in rows:
        by_verdict[row["verdict"]].append(row)

    print(f"quests scanned    : {len(list(args.quests.glob('*.xml')))}")
    print(f"increment actions : {len(rows)}")
    for verdict in ("OUT_OF_RANGE", "EXACT_COUNTER_UNBOUNDED", "UNBOUNDED", "OK"):
        print(f"{verdict:24}: {len(by_verdict[verdict])}")
    for verdict in ("OUT_OF_RANGE", "EXACT_COUNTER_UNBOUNDED"):
        quests = sorted({row["quest"] for row in by_verdict[verdict]})
        print(f"{verdict:24}quests: {' '.join(quests) if quests else '(none)'}")

    if args.csv:
        with args.csv.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
            writer.writeheader()
            writer.writerows(rows)
        print(f"wrote {args.csv}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
