#!/usr/bin/env python3
"""Validate the memory-bank routing and lifecycle metadata.

This is intentionally a structural checker. It does not infer root causes,
validate runtime behavior, or promote summary evidence into a Pattern.
"""

from __future__ import annotations

import argparse
import re
from collections import defaultdict
from pathlib import Path

from memory_bank import ENTRY_FIELDS, PATTERN_HEADING, PatternEntry, iter_pattern_entries
from sync_memory_bank import render_symptom_index


PATTERN_ID = re.compile(r"\b[A-Z][A-Z0-9]+-\d{3}\b")
HEADING_ID = PATTERN_HEADING
MARKDOWN_LINK = re.compile(r"\[[^\]]+\]\((?P<target>[^)\s]+)(?:\s+[^)]*)?\)")

REQUIRED_CARD_FIELDS = ("Pattern IDs:", "card_status:", "scope:", "last_reviewed:")
REQUIRED_ACTIVE_FIELDS = ("last_updated:", "status:", "scope:", "owner:", "expires:")
REQUIRED_ARCHIVE_FIELDS = (
    "status:",
    "scope:",
    "source:",
    "last_verified:",
    "replacement:",
    "read_by_default:",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate memory-bank structure and routing.")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    return parser.parse_args()


def read_text(path: Path, errors: list[str]) -> str:
    if not path.is_file():
        errors.append(f"missing file: {path}")
        return ""
    return path.read_text(encoding="utf-8")


def missing_fields(source: str, fields: tuple[str, ...]) -> list[str]:
    return [field for field in fields if field not in source]


def validate_entry_metadata(
    entry: PatternEntry, errors: list[str]
) -> dict[str, str]:
    path = entry.card
    pattern_id = entry.pattern_id
    metadata = entry.metadata
    if entry.metadata_blocks == 0:
        errors.append(f"{path}:{entry.line}: Pattern {pattern_id} has no pattern-metadata block")
        return {}
    if entry.metadata_blocks > 1:
        errors.append(f"{path}:{entry.line}: Pattern {pattern_id} has multiple pattern-metadata blocks")
    for field in ENTRY_FIELDS:
        if not metadata.get(field):
            errors.append(f"{path}: Pattern {pattern_id} missing entry field {field}")

    status = metadata.get("status")
    if status and status not in {"CONFIRMED", "PROVISIONAL", "SUPERSEDED"}:
        errors.append(f"{path}: Pattern {pattern_id} has invalid status {status}")
    for field in ("first_seen", "last_verified"):
        value = metadata.get(field, "")
        if value and value != "unknown" and not re.fullmatch(r"\d{4}-\d{2}-\d{2}", value):
            errors.append(f"{path}: Pattern {pattern_id} has invalid {field} {value}")

    replacement = metadata.get("superseded_by", "")
    if replacement != "none" and not PATTERN_ID.fullmatch(replacement):
        errors.append(f"{path}: Pattern {pattern_id} has invalid superseded_by {replacement}")
    if status == "SUPERSEDED" and replacement == "none":
        errors.append(f"{path}: Pattern {pattern_id} is SUPERSEDED without superseded_by")
    if status != "SUPERSEDED" and replacement not in {"", "none"}:
        errors.append(f"{path}: Pattern {pattern_id} has replacement but is not SUPERSEDED")
    return metadata


def validate_pattern_cards(
    pattern_dir: Path, errors: list[str]
) -> tuple[dict[str, list[str]], dict[str, dict[str, str]]]:
    occurrences: dict[str, list[str]] = defaultdict(list)
    entries: dict[str, dict[str, str]] = {}
    paths = sorted(pattern_dir.glob("*.md"))
    parsed_entries = list(iter_pattern_entries(pattern_dir))
    entries_by_card: dict[Path, list[PatternEntry]] = defaultdict(list)
    for entry in parsed_entries:
        entries_by_card[entry.card].append(entry)
    if not paths:
        errors.append(f"no pattern cards found under {pattern_dir}")
    for path in paths:
        source = read_text(path, errors)
        missing = missing_fields(source, REQUIRED_CARD_FIELDS)
        for field in missing:
            errors.append(f"{path}: missing card field {field}")
        card_entries = entries_by_card.get(path, [])
        if not card_entries:
            errors.append(f"{path}: no Pattern ID heading found")
        for entry in card_entries:
            occurrences[entry.pattern_id].append(f"{entry.card}:{entry.line}")
            entries[entry.pattern_id] = validate_entry_metadata(entry, errors)
    return occurrences, entries


def validate_archive(archive_dir: Path, errors: list[str]) -> int:
    entries = [path for path in sorted(archive_dir.glob("*.md")) if path.name != "README.md"]
    for path in entries:
        source = read_text(path, errors)
        for field in REQUIRED_ARCHIVE_FIELDS:
            if field not in source:
                errors.append(f"{path}: missing archive field {field}")
    return len(entries)


def validate_links(paths: tuple[Path, ...], errors: list[str]) -> int:
    checked = 0
    for path in paths:
        source = read_text(path, errors)
        for match in MARKDOWN_LINK.finditer(source):
            target = match.group("target")
            if target.startswith(("http://", "https://", "mailto:")):
                continue
            target = target.split("#", 1)[0]
            if not target:
                continue
            checked += 1
            resolved = (path.parent / target).resolve()
            if not resolved.exists():
                line = source.count("\n", 0, match.start()) + 1
                errors.append(f"{path}:{line}: broken link {target}")
    return checked


def validate_summary_compatibility(root: Path, errors: list[str]) -> None:
    canonical = root / ".agents/summary"
    legacy = root / ".agent/summary"
    if not canonical.is_dir():
        errors.append(f"missing canonical summary directory: {canonical}")
    if not legacy.is_symlink():
        errors.append(f"legacy summary path must be a symlink: {legacy}")
    elif legacy.resolve() != canonical.resolve():
        errors.append(f"legacy summary symlink does not target {canonical}: {legacy}")


def validate(root: Path) -> tuple[tuple[str, ...], int, int, int, int, int]:
    bank = root / ".agents/memory-bank"
    errors: list[str] = []
    router_path = bank / "systemPatterns.md"
    symptom_path = bank / "symptom-index.md"
    active_path = bank / "activeContext.md"
    router = read_text(router_path, errors)
    symptom_index = read_text(symptom_path, errors)
    active = read_text(active_path, errors)
    validate_summary_compatibility(root, errors)

    for field in ("role: router-only", "status: ACTIVE", "last_reviewed:"):
        if field not in router:
            errors.append(f"{router_path}: missing router field {field}")
    for field in REQUIRED_ACTIVE_FIELDS:
        if field not in active:
            errors.append(f"{active_path}: missing active-context field {field}")

    router_ids = set(PATTERN_ID.findall(router))
    symptom_ids = set(PATTERN_ID.findall(symptom_index))
    occurrences, entries = validate_pattern_cards(bank / "patterns", errors)
    pattern_ids = set(occurrences)

    for pattern_id, locations in sorted(occurrences.items()):
        if len(locations) > 1:
            errors.append(f"duplicate Pattern ID {pattern_id}: " + ", ".join(locations))
    for pattern_id in sorted(router_ids - pattern_ids):
        errors.append(f"router references Pattern ID without a tagged card heading: {pattern_id}")
    for pattern_id in sorted(pattern_ids - router_ids):
        errors.append(f"pattern card is not routed by systemPatterns.md: {pattern_id}")
    for pattern_id in sorted(router_ids - symptom_ids):
        errors.append(f"symptom-index.md is missing Pattern ID: {pattern_id}")
    for pattern_id in sorted(symptom_ids - router_ids):
        errors.append(f"symptom-index.md contains unrouted Pattern ID: {pattern_id}")
    for pattern_id, metadata in sorted(entries.items()):
        if metadata.get("status") == "CONFIRMED" and not metadata.get("evidence"):
            errors.append(f"Pattern {pattern_id} is CONFIRMED without evidence")
    expected_symptom_index = render_symptom_index(list(iter_pattern_entries(bank / "patterns")))
    if symptom_index != expected_symptom_index:
        errors.append(f"{symptom_path}: stale generated index; run sync_memory_bank.py")

    governance_paths = (
        root / "AGENTS.md",
        bank / "README.md",
        router_path,
        active_path,
        symptom_path,
        bank / "archive/README.md",
    )
    links_checked = validate_links(governance_paths, errors)
    archive_count = validate_archive(bank / "archive", errors)
    return tuple(errors), len(router_ids), len(pattern_ids), len(symptom_ids), links_checked, archive_count


def main() -> None:
    args = parse_args()
    result = validate(args.root.resolve())
    errors, router_count, pattern_count, symptom_count, links_checked, archive_count = result
    if errors:
        print("MEMORY_BANK_CHECK_FAILED")
        for error in errors:
            print(f"- {error}")
        raise SystemExit(1)
    print(
        "MEMORY_BANK_OK "
        f"ROUTER_IDS={router_count} "
        f"PATTERNS={pattern_count} "
        f"SYMPTOM_INDEX={symptom_count} "
        f"LINKS={links_checked} "
        f"ARCHIVE_ENTRIES={archive_count}"
    )


if __name__ == "__main__":
    main()
