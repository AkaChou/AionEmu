#!/usr/bin/env python3
"""诊断 PAGE_NOT_IN_TASK_HTML 各簇：NPC_REPORT 簇与 start-page 簇的客户端证据。

Diagnose PAGE_NOT_IN_TASK_HTML clusters: client evidence for NPC_REPORT and
start-page families.
"""
from __future__ import annotations

import csv
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
AUDIT = ROOT / ".agents/summary/quest-load-fail/quest-order-audit-current.csv"
PAGES = ROOT / "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"
ACTIONS = ROOT / "docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"
CONTRACTS = ROOT / "docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv"
PATH_RE = re.compile(r"^(?P<src>\S*) \+ (?P<owner>NPC \d+|QUEST_ACTION) \+ (?P<action>\S+) -> (?P<tgt>\S*) \+ page (?P<page>\d+)$")


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def main() -> int:
    rows = read_csv(AUDIT)
    notask = [r for r in rows if r["unresolved_reason"].startswith("compiled IR emits a task page absent")]

    pages: dict[int, dict[int, str]] = defaultdict(dict)
    for row in read_csv(PAGES):
        if row["source_variant"] == "active" and row["page_mapping"] == "exact":
            pages[int(row["quest_id"])][int(row["page_id"])] = row["page_constant"].removeprefix("HTML_PAGE_")

    actions: dict[int, dict[int, set[str]]] = defaultdict(lambda: defaultdict(set))
    for row in read_csv(ACTIONS):
        if row["source_variant"] == "active" and row["page_mapping"] == "exact" and row["action_mapping"] == "exact":
            actions[int(row["quest_id"])][int(row["page_id"])].add(row["action_constant"].removeprefix("HACTION_"))

    contracts: dict[int, dict[str, str]] = {}
    grouped: dict[int, list[dict[str, str]]] = defaultdict(list)
    for row in read_csv(CONTRACTS):
        if row["contract_scope"] == "FULL":
            grouped[int(row["quest_id"])].append(row)
    contracts = {q: rs[0] for q, rs in grouped.items() if len(rs) == 1}

    def report_pages(q: int) -> list[int]:
        return sorted(pid for pid, acts in actions.get(q, {}).items() if "SELECT_QUEST_REWARD" in acts and acts)

    # --- NPC_REPORT clusters (action 31 -> page 2375/1352) ---
    report_cluster = [r for r in notask
                      if (m := PATH_RE.match(r["actual_path"])) and m.group("action") == "31"
                      and m.group("page") in ("2375", "1352")]
    shape = Counter()
    for r in report_cluster:
        q = int(r["quest_id"])
        contract = contracts.get(q)
        npc = int(r["npc_id"])
        reps = report_pages(q)
        html = pages.get(q, {})
        has_success = 10002 in html
        key = (
            "report_page_success" if reps == [10002] else
            "report_page_other" if len(reps) == 1 else
            "report_multi" if len(reps) > 1 else "report_none",
            "html10002" if has_success else "no10002",
            "contract_end" if contract and str(npc) in (contract["end_npc_ids"] or "").split()
            else "contract_start" if contract and str(npc) in (contract["start_npc_ids"] or "").split()
            else "no_contract" if contract is None else "contract_other",
        )
        shape[key] += 1
    print("NPC_REPORT cluster shapes:")
    for k, v in shape.most_common(20):
        print(f"  {v:4d}  {k}")

    # --- start-page cluster (action 31 -> page 1011) ---
    start_cluster = [r for r in notask
                     if (m := PATH_RE.match(r["actual_path"])) and m.group("action") == "31"
                     and m.group("page") == "1011"]
    start_shape = Counter()
    examples: dict[tuple, list[int]] = defaultdict(list)
    for r in start_cluster:
        q = int(r["quest_id"])
        contract = contracts.get(q)
        npc = int(r["npc_id"])
        html = pages.get(q, {})
        # chain root heuristic: page that has an action chain leading to 4 (ask accept) but no
        # inbound page-turn edge; compute via aligner-like walk on action constants
        preds: set[int] = set()
        for pid, acts in actions.get(q, {}).items():
            for a in acts:
                for pid2, name2 in html.items():
                    if name2 == a:
                        preds.add(pid2)
        roots = [pid for pid in html if pid not in preds and actions.get(q, {}).get(pid)]
        interactive_roots = [pid for pid in roots if pid not in (9, 1008, 12, 11, 5, 6, 7)]
        key = (
            "contract_start" if contract and str(npc) in (contract["start_npc_ids"] or "").split()
            else "contract_end_only" if contract and str(npc) in (contract["end_npc_ids"] or "").split()
            else "no_contract" if contract is None else "contract_other",
            "unique_interactive_root" if len(interactive_roots) == 1
            else "multi_roots" if len(interactive_roots) > 1 else "no_interactive_root",
        )
        start_shape[key] += 1
        if len(examples[key]) < 5:
            examples[key].append((q, npc, [html.get(x, x) for x in interactive_roots]))
    print("\nstart-page cluster shapes:")
    for k, v in start_shape.most_common(20):
        print(f"  {v:4d}  {k}  e.g. {examples[k][:3]}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
