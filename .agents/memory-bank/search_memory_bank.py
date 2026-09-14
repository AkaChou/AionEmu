#!/usr/bin/env python3
"""Search Pattern metadata and evidence by symptom, keyword or Pattern ID."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

from memory_bank import iter_pattern_entries


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Search memory-bank Pattern entries.")
    parser.add_argument("query", nargs="+", help="symptom, keyword or Pattern ID")
    parser.add_argument("--all", action="store_true", help="require every query token to match")
    parser.add_argument("--limit", type=int, default=10, help="maximum results, default: 10")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    return parser.parse_args()


def tokenize(value: str) -> list[str]:
    return re.findall(r"[a-z0-9_@.-]+|[\u3400-\u9fff]+", value.casefold())


def main() -> None:
    args = parse_args()
    terms = tokenize(" ".join(args.query))
    if not terms:
        raise SystemExit("query must contain at least one searchable token")
    if args.limit < 1:
        raise SystemExit("--limit must be positive")

    entries = []
    for entry in iter_pattern_entries(args.root / ".agents/memory-bank/patterns"):
        searchable = f"{entry.pattern_id} {entry.section}".casefold()
        matched = [term for term in terms if term in searchable]
        if (args.all and len(matched) != len(terms)) or not matched:
            continue
        score = sum(searchable.count(term) for term in set(matched))
        if " ".join(args.query).casefold() in searchable:
            score += len(terms) * 2
        entries.append((score, entry))

    entries.sort(key=lambda item: (-item[0], item[1].pattern_id))
    if not entries:
        print("MEMORY_BANK_NO_MATCH")
        return

    print(f"MEMORY_BANK_MATCHES={min(len(entries), args.limit)}")
    for score, entry in entries[: args.limit]:
        metadata = entry.metadata
        print(f"[{entry.pattern_id}] {metadata.get('status', 'UNKNOWN')} {entry.card}:{entry.line} score={score}")
        print(f"  symptom: {metadata.get('symptom', '(metadata missing)')}")
        print(f"  first_check: {metadata.get('first_check', '(metadata missing)')}")
        print(f"  evidence: {metadata.get('evidence', '(metadata missing)')}")


if __name__ == "__main__":
    main()
