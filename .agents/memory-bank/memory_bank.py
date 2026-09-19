"""Shared parser for Pattern metadata used by memory-bank maintenance tools."""

from __future__ import annotations

from dataclasses import dataclass
import hashlib
import json
import re
from collections.abc import Iterable
from pathlib import Path


PATTERN_ID = re.compile(r"[A-Z][A-Z0-9]+-\d{3}")
PATTERN_HEADING = re.compile(
    r"^#{2,6}\s+.*?\[(?P<pattern>[A-Z][A-Z0-9]+-\d{3})\]",
    re.MULTILINE,
)
ANY_HEADING = re.compile(r"^#{2,6}\s+", re.MULTILINE)
METADATA_BLOCK = re.compile(
    r"<!--\s*pattern-metadata\s*\n(?P<body>.*?)\n\s*-->",
    re.DOTALL,
)

ENTRY_FIELDS = (
    "status",
    "scope",
    "first_seen",
    "last_verified",
    "symptom",
    "root_cause",
    "fix_or_guardrail",
    "evidence",
    "validation",
    "boundaries",
    "superseded_by",
    "first_check",
)

# Optional metadata: cards may add these to widen retrieval recall without changing the contract.
OPTIONAL_FIELDS = ("keywords",)
OPTIONAL_FIELD_LIMIT = 300

# Optional fields whose path and commit references are validated the same way as `evidence`.
REFERENCE_FIELDS = ("evidence", "see_also")

# Any Pattern ID mentioned inside a section is treated as a related card for retrieval.
PATTERN_ID_REFERENCE = re.compile(r"\b[A-Z][A-Z0-9]+-\d{3}\b")
HEADING_MARKER = re.compile(r"^#{2,6}\s+")
RELATED_ID_LIMIT = 20


@dataclass(frozen=True)
class PatternEntry:
    pattern_id: str
    card: Path
    line: int
    section: str
    metadata: dict[str, str]
    metadata_blocks: int


def parse_metadata(block: str) -> dict[str, str]:
    metadata: dict[str, str] = {}
    for raw_line in block.splitlines():
        line = raw_line.strip()
        if not line or ":" not in line:
            continue
        key, value = line.split(":", 1)
        metadata[key.strip()] = value.strip()
    return metadata


def iter_pattern_entries(pattern_dir: Path):
    for card in sorted(pattern_dir.glob("*.md")):
        source = card.read_text(encoding="utf-8")
        pattern_headings = list(PATTERN_HEADING.finditer(source))
        all_headings = list(ANY_HEADING.finditer(source))
        for match in pattern_headings:
            next_headings = [heading for heading in all_headings if heading.start() > match.start()]
            section_end = next_headings[0].start() if next_headings else len(source)
            section = source[match.start() : section_end]
            blocks = list(METADATA_BLOCK.finditer(section))
            metadata = parse_metadata(blocks[0].group("body")) if blocks else {}
            yield PatternEntry(
                pattern_id=match.group("pattern"),
                card=card,
                line=source.count("\n", 0, match.start()) + 1,
                section=section,
                metadata=metadata,
                metadata_blocks=len(blocks),
            )


def section_heading(section: str) -> str:
    """Return one section heading text without the leading Markdown hashes."""
    first_line = section.splitlines()[0] if section else ""
    return HEADING_MARKER.sub("", first_line).strip()


def section_sha256(section: str) -> str:
    """Return a stable content hash for one Pattern section."""
    return hashlib.sha256(section.encode("utf-8")).hexdigest()


def entry_record(entry: PatternEntry, root: Path) -> dict[str, object]:
    """Build the machine-readable record shared by index.jsonl and search output.

    The record carries the routing metadata plus the exact file/line span of the section, so an
    agent can expand a single Pattern instead of reading a whole domain card.
    """
    section = entry.section
    try:
        card = entry.card.relative_to(root).as_posix()
    except ValueError:
        card = entry.card.as_posix()
    related = sorted(
        {match.group(0) for match in PATTERN_ID_REFERENCE.finditer(section)} - {entry.pattern_id}
    )
    record: dict[str, object] = {
        "pattern_id": entry.pattern_id,
        "title": section_heading(section),
        "domain": entry.card.stem,
        "card": card,
        "line": entry.line,
        "end_line": entry.line + section.rstrip("\n").count("\n"),
        "section_bytes": len(section.encode("utf-8")),
        "section_sha256": section_sha256(section),
        "related": related[:RELATED_ID_LIMIT],
    }
    for field in ENTRY_FIELDS:
        record[field] = entry.metadata.get(field, "")
    for field in OPTIONAL_FIELDS:
        record[field] = entry.metadata.get(field, "")
    return record


def render_index_jsonl(entries: Iterable[PatternEntry], root: Path) -> str:
    """Render one compact JSON object per Pattern entry, sorted by Pattern ID.

    The first line is a self-describing meta record; every following line is a Pattern record.
    """
    ordered = sorted(entries, key=lambda item: item.pattern_id)
    meta = {
        "record": "meta",
        "schema": 1,
        "generator": "sync_memory_bank.py",
        "entry_count": len(ordered),
        "fields": [
            "pattern_id",
            "title",
            "domain",
            "card",
            "line",
            "end_line",
            "section_bytes",
            "section_sha256",
            "related",
            *ENTRY_FIELDS,
            *OPTIONAL_FIELDS,
        ],
    }
    records = [meta]
    for entry in ordered:
        record = entry_record(entry, root)
        record["record"] = "pattern"
        records.append(record)
    if not ordered:
        return ""
    return (
        "\n".join(
            json.dumps(record, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
            for record in records
        )
        + "\n"
    )
