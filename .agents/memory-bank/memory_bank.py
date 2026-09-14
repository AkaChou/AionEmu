"""Shared parser for Pattern metadata used by memory-bank maintenance tools."""

from __future__ import annotations

from dataclasses import dataclass
import re
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
