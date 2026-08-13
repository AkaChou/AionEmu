#!/usr/bin/env python3
"""Fix quest XML: add the missing report chain (select_success page + report NPC) to hunt quests.

Evidence:
- quest-order-audit.csv: CLIENT_PAGE_UNREACHED on select_success (page 10002).
- legacy-quest-dialog-contracts.csv: retail report contract (report_page, end_npc_ids).
- Reference template: quests/25503.xml (NPC_REPORT source=reward target=reward page=DEFAULT_SUCCESS).

Fix E: for quests whose kill chain auto-advances started->reward but have no report dialog:
  replace the auto-advance target with a report node chain:
    <transition started -> reward (kill)  keep
    <dialog NPC_REPORT npc=end source=reward target=reward page=DEFAULT_SUCCESS/>  add if absent
  The report dialog route was already validated by QuestXmlBlockExpander (REWARD self-loop allowed).
"""
from __future__ import annotations

import argparse
import csv
import re
from pathlib import Path

DEFAULT_PAGES = "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"
DEFAULT_AUDIT = "docs/quest/client-dialog-mapping/quest-order-audit.csv"
DEFAULT_LEGACY = "docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv"
DEFAULT_QUEST_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"

REPORT_TEMPLATE = '    <dialog type="NPC_REPORT" npc-id="{npc}" source="reward" target="reward" page="DEFAULT_SUCCESS"/>\n'


def load_unreached(audit_csv: Path, page_index: Path) -> dict[int, set[str]]:
    page_names = {}
    for row in csv.DictReader(open(page_index, encoding="utf-8-sig")):
        if row["source_variant"] == "active":
            page_names[int(row["page_id"])] = row["html_page_name"]
    result: dict[int, set[str]] = {}
    for row in csv.DictReader(open(audit_csv, encoding="utf-8-sig")):
        if row["audit_status"] != "CLIENT_PAGE_UNREACHED":
            continue
        name = page_names.get(int(row["shown_page"]))
        if name:
            result.setdefault(int(row["quest_id"]), set()).add(name)
    return result


def load_legacy(legacy_csv: Path) -> dict[int, list[dict]]:
    result: dict[int, list[dict]] = {}
    for row in csv.DictReader(open(legacy_csv, encoding="utf-8-sig")):
        result.setdefault(int(row["quest_id"]), []).append(row)
    return result


def fix_e(xml_path: Path, npc_id: str) -> bool:
    """Add NPC_REPORT reward->reward DEFAULT_SUCCESS if absent."""
    text = xml_path.read_text(encoding="utf-8")
    if 'page="DEFAULT_SUCCESS"' in text:
        return False
    marker = "  </transitions>"
    if marker not in text:
        return False
    block = REPORT_TEMPLATE.format(npc=npc_id)
    text = text.replace(marker, block + marker, 1)
    xml_path.write_text(text, encoding="utf-8")
    return True


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pages", type=Path, default=Path(DEFAULT_PAGES))
    parser.add_argument("--audit", type=Path, default=Path(DEFAULT_AUDIT))
    parser.add_argument("--legacy", type=Path, default=Path(DEFAULT_LEGACY))
    parser.add_argument("--quest-dir", type=Path, default=Path(DEFAULT_QUEST_DIR))
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    unreached = load_unreached(args.audit, args.pages)
    legacy = load_legacy(args.legacy)

    fixed = []
    skipped = []
    for qid, missing in sorted(unreached.items()):
        if "select_success" not in missing:
            continue
        path = args.quest_dir / f"{qid}.xml"
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        if "NPC_REPORT" in text or "DEFAULT_SUCCESS" in text:
            continue
        # Only quests whose kill chain auto-advances to reward: report dialog belongs to the end NPC.
        if "<kill-npc" not in text or "<kill-in-world" in text:
            continue
        # legacy contract gives the end NPC id
        contract = legacy.get(qid)
        end_npc = None
        if contract:
            for row in contract:
                if row.get("end_npc_ids"):
                    end_npc = row["end_npc_ids"].split()[0]
                    break
        if not end_npc:
            skipped.append((qid, "no legacy end_npc"))
            continue
        if not args.dry_run:
            if fix_e(path, end_npc):
                fixed.append((qid, end_npc))
        else:
            fixed.append((qid, end_npc))

    print(f"Fix E (report chain): {len(fixed)}")
    for qid, npc in fixed[:10]:
        print(f"  {qid} npc={npc}")
    print(f"跳过: {len(skipped)}")
    for qid, reason in skipped[:10]:
        print(f"  {qid}: {reason}")


if __name__ == "__main__":
    main()
