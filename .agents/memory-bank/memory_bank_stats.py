#!/usr/bin/env python3
"""Report memory-bank Pattern statistics: status, domain, validation and freshness.

The report is derived from Pattern metadata only; it never edits cards. Use `--json` for
machine consumption and `--fail-over-days N` to turn the freshness check into a gate.

Run:
    python3 -B .agents/memory-bank/memory_bank_stats.py
    python3 -B .agents/memory-bank/memory_bank_stats.py --json
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from datetime import date
from pathlib import Path

from memory_bank import OPTIONAL_FIELDS, iter_pattern_entries


VALIDATION_KINDS = ("static", "focused-test", "production-gate", "runtime", "client")
SUMMARY_REF = re.compile(r"\.agents/summary/[A-Za-z0-9._/-]+")
COMMIT_REF = re.compile(r"\bcommit[ =]([0-9a-f]{7,40})\b")
SOURCE_REF = re.compile(
    r"[A-Za-z0-9_@./-]+\.(?:java|xml|md|py|tsv|csv|json|properties|xsd|yml|yaml)\b"
)
FRESH_DAYS = 30
AGING_DAYS = 90


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Report memory-bank Pattern statistics.")
    parser.add_argument("--json", action="store_true", help="emit machine-readable JSON")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--today", help="override today's date (YYYY-MM-DD) for reproducible runs")
    parser.add_argument(
        "--fail-over-days",
        type=int,
        help="exit 1 when a CONFIRMED entry was last verified more than N days ago",
    )
    return parser.parse_args()


def parse_day(value: str) -> date | None:
    try:
        return date.fromisoformat(value)
    except ValueError:
        return None


def entry_stats(entry, today: date) -> dict[str, object]:
    metadata = entry.metadata
    verified = parse_day(metadata.get("last_verified", ""))
    age = (today - verified).days if verified else None
    evidence = metadata.get("evidence", "")
    validation = metadata.get("validation", "")
    return {
        "pattern_id": entry.pattern_id,
        "domain": entry.card.stem,
        "status": metadata.get("status", "UNKNOWN"),
        "last_verified": metadata.get("last_verified", ""),
        "age_days": age,
        "validation_kinds": [kind for kind in VALIDATION_KINDS if kind in validation],
        "summary_refs": len(set(SUMMARY_REF.findall(evidence))),
        "commit_refs": len(set(COMMIT_REF.findall(evidence))),
        "source_refs": len(set(SOURCE_REF.findall(evidence))),
        "related": len(re.findall(r"\b[A-Z][A-Z0-9]+-\d{3}\b", entry.section)) - 1,
        "has_keywords": bool(metadata.get(OPTIONAL_FIELDS[0], "")),
    }


def build_report(root: Path, today: date) -> dict[str, object]:
    bank = root / ".agents/memory-bank"
    entries = [entry_stats(entry, today) for entry in iter_pattern_entries(bank / "patterns")]
    known = [entry for entry in entries if entry["age_days"] is not None]
    stale = sorted(
        (entry for entry in known if entry["age_days"] > AGING_DAYS),
        key=lambda item: -int(item["age_days"]),
    )
    aging = sorted(
        (entry for entry in known if FRESH_DAYS < entry["age_days"] <= AGING_DAYS),
        key=lambda item: -int(item["age_days"]),
    )
    return {
        "generated": today.isoformat(),
        "total": len(entries),
        "status": dict(sorted(Counter(entry["status"] for entry in entries).items())),
        "domains": dict(sorted(Counter(entry["domain"] for entry in entries).items())),
        "validation": {
            kind: sum(1 for entry in entries if kind in entry["validation_kinds"])
            for kind in VALIDATION_KINDS
        },
        "freshness": {
            "unknown": len(entries) - len(known),
            "fresh": sum(1 for entry in known if entry["age_days"] <= FRESH_DAYS),
            "aging": [entry["pattern_id"] for entry in aging],
            "stale": [entry["pattern_id"] for entry in stale],
        },
        "evidence": {
            "with_summary_ref": sum(1 for entry in entries if entry["summary_refs"]),
            "with_commit_ref": sum(1 for entry in entries if entry["commit_refs"]),
            "with_source_ref": sum(1 for entry in entries if entry["source_refs"]),
        },
        "keywords": sum(1 for entry in entries if entry["has_keywords"]),
        "entries": entries,
    }


def render_text(report: dict[str, object]) -> str:
    lines = [
        f"MEMORY_BANK_STATS DATE={report['generated']} PATTERNS={report['total']}",
        f"STATUS {report['status']}",
        f"DOMAINS {report['domains']}",
        f"VALIDATION {report['validation']}",
        f"EVIDENCE {report['evidence']}",
        f"KEYWORDS {report['keywords']}/{report['total']}",
    ]
    freshness = report["freshness"]
    lines.append(
        f"FRESHNESS unknown={freshness['unknown']} fresh={freshness['fresh']} "
        f"aging={len(freshness['aging'])} stale={len(freshness['stale'])}"
    )
    if freshness["aging"]:
        lines.append("AGING " + ", ".join(freshness["aging"]))
    if freshness["stale"]:
        lines.append("STALE " + ", ".join(freshness["stale"]))
    return "\n".join(lines)


def main() -> None:
    args = parse_args()
    today = parse_day(args.today) if args.today else date.today()
    if today is None:
        raise SystemExit("--today must be YYYY-MM-DD")
    report = build_report(args.root.resolve(), today)
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    else:
        print(render_text(report))

    if args.fail_over_days is not None:
        too_old = [
            entry["pattern_id"]
            for entry in report["entries"]
            if entry["status"] == "CONFIRMED"
            and entry["age_days"] is not None
            and entry["age_days"] > args.fail_over_days
        ]
        if too_old:
            print(f"MEMORY_BANK_FRESHNESS_FAILED {', '.join(sorted(too_old))}")
            raise SystemExit(1)
        print(f"MEMORY_BANK_FRESHNESS_OK THRESHOLD={args.fail_over_days}")


if __name__ == "__main__":
    main()
