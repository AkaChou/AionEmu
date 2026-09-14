#!/usr/bin/env python3
"""Validate the memory-bank routing and lifecycle metadata.

This is intentionally a structural checker. It does not infer root causes,
validate runtime behavior, or promote summary evidence into a Pattern.
"""

from __future__ import annotations

import argparse
import re
import subprocess
from collections import defaultdict
from pathlib import Path

from memory_bank import (
    ENTRY_FIELDS,
    PATTERN_HEADING,
    REFERENCE_FIELDS,
    PatternEntry,
    iter_pattern_entries,
)
from sync_memory_bank import render_symptom_index


PATTERN_ID = re.compile(r"\b[A-Z][A-Z0-9]+-\d{3}\b")
HEADING_ID = PATTERN_HEADING
MARKDOWN_LINK = re.compile(r"\[[^\]]+\]\((?P<target>[^)\s]+)(?:\s+[^)]*)?\)")

# Source extensions that make a bare filename (no directory separator) a checkable reference.
# "DAOManager.init" and "Maven/JDK baseline notes" are descriptive text, not references, and
# are excluded because their trailing token never carries one of these extensions.
EVIDENCE_SOURCE_EXTENSIONS = frozenset({"java", "xml", "csv", "md", "json", "properties", "xsd", "yml", "yaml"})
_SOURCE_EXT_ALTERNATION = "|".join(sorted(EVIDENCE_SOURCE_EXTENSIONS))
# Evidence references: a repository path optionally pinned to a line number. Either a path with
# at least one separator ("quests/1900.xml") or a bare source filename ("quest_data.xml").
EVIDENCE_REFERENCE = re.compile(
    r"(?P<path>"
    r"[A-Za-z0-9_@.-]+/[A-Za-z0-9_@./-]*\.[A-Za-z0-9]+"
    rf"|[A-Za-z0-9_@.-]+\.(?:{_SOURCE_EXT_ALTERNATION})\b"
    r")(?::(?P<line>\d+))?"
)
# git commit references inside an evidence field: "commit 97fcba667".
EVIDENCE_COMMIT = re.compile(r"\bcommit[ =](?P<sha>[0-9a-f]{7,40})\b")
# Runtime artifacts that are gitignored and legitimately absent from a fresh checkout.
EVIDENCE_ENV_ALLOWLIST = ("target/", "aion/", "log/")

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
) -> tuple[dict[str, list[str]], dict[str, dict[str, str]], dict[str, Path]]:
    occurrences: dict[str, list[str]] = defaultdict(list)
    entries: dict[str, dict[str, str]] = {}
    cards: dict[str, Path] = {}
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
            cards[entry.pattern_id] = entry.card
    return occurrences, entries, cards


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


def resolve_commit(root: Path, reference: str) -> str | None:
    result = subprocess.run(
        ["git", "rev-parse", "--verify", f"{reference}^{{commit}}"],
        cwd=root,
        check=False,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip() if result.returncode == 0 else None


class EvidenceResolver:
    """Resolve evidence path references against the repository, with a suffix-match fallback.

    Cards cite source files by a stable suffix ("quests/1900.xml") rather than the full package
    path, so an exact-miss falls back to a suffix search over the tracked source roots. The index
    is built once and reused for every reference.
    """

    SEARCH_ROOTS = ("src", "docs", ".agents", "scripts")

    def __init__(self, root: Path) -> None:
        self.root = root
        self._index: dict[str, list[Path]] | None = None

    def _files(self) -> dict[str, list[Path]]:
        if self._index is None:
            index: dict[str, list[Path]] = {}
            for name in self.SEARCH_ROOTS:
                base = self.root / name
                if not base.is_dir():
                    continue
                for path in base.rglob("*"):
                    if path.is_file():
                        index.setdefault(str(path.relative_to(self.root)), []).append(path)
            self._index = index
        return self._index

    def resolve(self, card: Path, reference: str) -> Path | None:
        if reference.startswith(EVIDENCE_ENV_ALLOWLIST):
            return None
        # Card-relative references such as "archive/foo.md" resolve against the card's directory.
        for base in (self.root, card.parent):
            candidate = base / reference
            if candidate.exists():
                return candidate
        suffix = "/" + reference
        matches = [p for rel, paths in self._files().items() if rel.endswith(suffix) for p in paths]
        if len(matches) == 1:
            return matches[0]
        return None


def validate_evidence_references(
    entries: dict[str, dict[str, str]],
    cards: dict[str, Path],
    resolver: EvidenceResolver,
    root: Path,
    errors: list[str],
) -> int:
    checked = 0
    for pattern_id, metadata in sorted(entries.items()):
        card = cards.get(pattern_id, root)
        for field_name in REFERENCE_FIELDS:
            evidence = metadata.get(field_name, "")
            if not evidence:
                continue
            for match in EVIDENCE_REFERENCE.finditer(evidence):
                reference = match.group("path")
                if reference.startswith(EVIDENCE_ENV_ALLOWLIST):
                    continue
                checked += 1
                resolved = resolver.resolve(card, reference)
                if resolved is None:
                    errors.append(
                        f"Pattern {pattern_id} {field_name} references missing path {reference}"
                    )
                    continue
                line_ref = match.group("line")
                if line_ref and resolved.is_file():
                    total = len(resolved.read_text(encoding="utf-8", errors="replace").splitlines())
                    if int(line_ref) > total:
                        errors.append(
                            f"Pattern {pattern_id} {field_name} line {reference}:{line_ref} "
                            f"exceeds {total} lines"
                        )
            for match in EVIDENCE_COMMIT.finditer(evidence):
                checked += 1
                if resolve_commit(root, match.group("sha")) is None:
                    errors.append(
                        f"Pattern {pattern_id} {field_name} references unknown commit "
                        f"{match.group('sha')}"
                    )
    return checked


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
    occurrences, entries, cards = validate_pattern_cards(bank / "patterns", errors)
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
    evidence_checked = validate_evidence_references(
        entries, cards, EvidenceResolver(root), root, errors
    )
    archive_count = validate_archive(bank / "archive", errors)
    return (
        tuple(errors),
        len(router_ids),
        len(pattern_ids),
        len(symptom_ids),
        links_checked,
        evidence_checked,
        archive_count,
    )


def main() -> None:
    args = parse_args()
    result = validate(args.root.resolve())
    errors, router_count, pattern_count, symptom_count, links_checked, evidence_checked, archive_count = result
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
        f"EVIDENCE_REFS={evidence_checked} "
        f"ARCHIVE_ENTRIES={archive_count}"
    )


if __name__ == "__main__":
    main()
