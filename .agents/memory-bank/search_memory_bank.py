#!/usr/bin/env python3
"""Search Pattern metadata and evidence by symptom, keyword or Pattern ID.

Two agent-facing modes are supported:

- ranked search over every Pattern section (``--json`` for machine consumption);
- exact expansion of a single Pattern via ``--id``, which returns only that section.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

from memory_bank import entry_record, iter_pattern_entries


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Search memory-bank Pattern entries.")
    parser.add_argument("query", nargs="*", help="symptom, keyword or Pattern ID")
    parser.add_argument(
        "--id",
        dest="pattern_id",
        help="print one Pattern entry by ID instead of searching, for example QE-013",
    )
    parser.add_argument("--json", action="store_true", help="emit machine-readable JSON")
    parser.add_argument(
        "--status",
        help="only include these statuses, comma separated (CONFIRMED,PROVISIONAL,SUPERSEDED)",
    )
    parser.add_argument("--all", action="store_true", help="require every query token to match")
    parser.add_argument("--limit", type=int, default=10, help="maximum results, default: 10")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    return parser.parse_args()


VALID_STATUSES = ("CONFIRMED", "PROVISIONAL", "SUPERSEDED")


LATIN_TOKEN = re.compile(r"[a-z0-9_@.-]+")
CJK_RUN = re.compile(r"[\u3400-\u9fff]+")
# Fields an agent actually searches by, scored above the surrounding narrative prose.
# `keywords` carries aliases, user phrasing and log keywords so recall does not depend on the
# card's own vocabulary.
HEADLINE_FIELDS = ("keywords", "symptom", "first_check", "root_cause")
FIELD_WEIGHT = 4
# Normalize by section length so a long card cannot outrank a precise short one on raw frequency.
LENGTH_NORMALIZATION = 0.5


def tokenize(value: str) -> list[str]:
    """Tokenize a query or document, emitting CJK bigrams alongside Latin words.

    A whole CJK run as a single token would only match as a contiguous substring, so a natural
    query like "\u4efb\u52a1\u8fdb\u5ea6\u4e0d\u63a8\u8fdb" would miss a symptom field written as "\u4efb\u52a1\u8ffd\u8e2a UI \u4e0d\u63a8\u8fdb".
    Bigrams let partially-overlapping Chinese queries still score.
    """
    folded = value.casefold()
    tokens = LATIN_TOKEN.findall(folded)
    for run in CJK_RUN.findall(folded):
        if len(run) == 1:
            tokens.append(run)
        else:
            tokens.extend(run[index:index + 2] for index in range(len(run) - 1))
    return tokens


def build_index(section: str, pattern_id: str) -> dict[str, int]:
    """Return per-token counts for one Pattern section, weighting the headline metadata fields."""
    counts: dict[str, int] = {}
    for field in HEADLINE_FIELDS:
        value = re.search(rf"^{field}: (.+)$", section, re.MULTILINE)
        if value:
            for token in tokenize(value.group(1)):
                counts[token] = counts.get(token, 0) + FIELD_WEIGHT
    for token in tokenize(section):
        counts[token] = counts.get(token, 0) + 1
    for token in tokenize(pattern_id):
        counts[token] = counts.get(token, 0) + FIELD_WEIGHT
    return counts


def parse_statuses(value: str | None) -> set[str]:
    """Return the requested status filter; an empty set means no filtering."""
    if not value:
        return set()
    statuses = {item.strip().upper() for item in value.split(",") if item.strip()}
    invalid = statuses - set(VALID_STATUSES)
    if invalid:
        raise SystemExit(f"unknown --status: {', '.join(sorted(invalid))}")
    return statuses


def print_entry(root: Path, entries, pattern_id: str, as_json: bool) -> None:
    """Print exactly one Pattern section, or fail when the ID is unknown or ambiguous."""
    wanted = pattern_id.strip().upper()
    matches = [entry for entry in entries if entry.pattern_id == wanted]
    if len(matches) != 1:
        if as_json:
            print(json.dumps({"pattern_id": wanted, "found": False}, ensure_ascii=False))
        else:
            print(f"MEMORY_BANK_NO_MATCH ID={wanted}")
        raise SystemExit(1)
    entry = matches[0]
    record = entry_record(entry, root)
    if as_json:
        record["section"] = entry.section
        print(json.dumps(record, ensure_ascii=False, indent=2, sort_keys=True))
        return
    print(
        f"[{entry.pattern_id}] {entry.metadata.get('status', 'UNKNOWN')} "
        f"{record['card']}:{record['line']}-{record['end_line']}"
    )
    print()
    print(entry.section.rstrip("\n"))


def main() -> None:
    args = parse_args()
    root = args.root.resolve()
    statuses = parse_statuses(args.status)
    entries = list(iter_pattern_entries(root / ".agents/memory-bank/patterns"))

    if args.pattern_id:
        print_entry(root, entries, args.pattern_id, args.json)
        return

    if not args.query:
        raise SystemExit("provide a query or --id <PATTERN_ID>")
    if args.limit < 1:
        raise SystemExit("--limit must be positive")
    terms = tokenize(" ".join(args.query))
    if not terms:
        raise SystemExit("query must contain at least one searchable token")

    matches = []
    for entry in entries:
        if statuses and entry.metadata.get("status", "") not in statuses:
            continue
        counts = build_index(entry.section, entry.pattern_id)
        matched = [term for term in terms if term in counts]
        if (args.all and len(matched) != len(terms)) or not matched:
            continue
        # Coverage: how much of the query the card explains, so a one-word overlap on a long
        # card cannot outrank a card that matches most of the query.
        coverage = len(set(matched)) / len(set(terms))
        raw = sum(counts[term] for term in set(matched))
        score = raw / (len(entry.section) ** LENGTH_NORMALIZATION) * (1 + coverage)
        matches.append((round(score, 4), coverage, entry))

    matches.sort(key=lambda item: (-item[0], -item[1], item[2].pattern_id))
    if not matches:
        if args.json:
            print(
                json.dumps(
                    {"query": " ".join(args.query), "count": 0, "total_matches": 0, "matches": []},
                    ensure_ascii=False,
                )
            )
        else:
            print("MEMORY_BANK_NO_MATCH")
        return

    selected = matches[: args.limit]
    if args.json:
        payload = {
            "query": " ".join(args.query),
            "count": len(selected),
            "total_matches": len(matches),
            "matches": [
                {**entry_record(entry, root), "score": score, "coverage": round(coverage, 4)}
                for score, coverage, entry in selected
            ],
        }
        print(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True))
        return

    print(f"MEMORY_BANK_MATCHES={len(selected)}")
    for score, coverage, entry in selected:
        metadata = entry.metadata
        print(
            f"[{entry.pattern_id}] {metadata.get('status', 'UNKNOWN')} {entry.card}:{entry.line} "
            f"score={score} coverage={coverage:.0%}"
        )
        print(f"  symptom: {metadata.get('symptom', '(metadata missing)')}")
        print(f"  first_check: {metadata.get('first_check', '(metadata missing)')}")
        print(f"  evidence: {metadata.get('evidence', '(metadata missing)')}")


if __name__ == "__main__":
    main()
