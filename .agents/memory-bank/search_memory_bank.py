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


LATIN_TOKEN = re.compile(r"[a-z0-9_@.-]+")
CJK_RUN = re.compile(r"[\u3400-\u9fff]+")
# Fields an agent actually searches by, scored above the surrounding narrative prose.
HEADLINE_FIELDS = ("symptom", "first_check", "root_cause")
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


def main() -> None:
    args = parse_args()
    terms = tokenize(" ".join(args.query))
    if not terms:
        raise SystemExit("query must contain at least one searchable token")
    if args.limit < 1:
        raise SystemExit("--limit must be positive")

    entries = []
    for entry in iter_pattern_entries(args.root / ".agents/memory-bank/patterns"):
        counts = build_index(entry.section, entry.pattern_id)
        matched = [term for term in terms if term in counts]
        if (args.all and len(matched) != len(terms)) or not matched:
            continue
        # Coverage: how much of the query the card explains, so a one-word overlap on a long
        # card cannot outrank a card that matches most of the query.
        coverage = len(set(matched)) / len(set(terms))
        raw = sum(counts[term] for term in set(matched))
        score = raw / (len(entry.section) ** LENGTH_NORMALIZATION) * (1 + coverage)
        entries.append((round(score, 4), coverage, entry))

    entries.sort(key=lambda item: (-item[0], -item[1], item[2].pattern_id))
    if not entries:
        print("MEMORY_BANK_NO_MATCH")
        return

    print(f"MEMORY_BANK_MATCHES={min(len(entries), args.limit)}")
    for score, coverage, entry in entries[: args.limit]:
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
