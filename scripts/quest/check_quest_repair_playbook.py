#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Mapping, Sequence


PATTERN_ROW = re.compile(
    r"^\| `(?P<pattern>[A-Z][A-Z0-9_]+)` \| (?P<symptoms>.+) \| (?P<fingerprint>.+) \| "
    r"(?P<checkpoint>.+) \| (?P<proof>.+) \|$",
    re.MULTILINE,
)
INDEX_COMMIT = re.compile(r"^\| `(?P<commit>[0-9a-f]{7,40})` \|", re.MULTILINE)
DETAIL_COMMIT = re.compile(r"^- commit：`(?P<commit>[0-9a-f]{7,40})`。$", re.MULTILINE)
PROOF_COMMIT = re.compile(r"`(?P<commit>[0-9a-f]{7,40})`")
PROOF_TEST = re.compile(
    r"`(?P<test_class>[A-Za-z][A-Za-z0-9_]*Test)#(?P<test_method>[A-Za-z_$][A-Za-z0-9_$]*)`"
)
DETAIL_HEADING = re.compile(r"^#{2,3} 8\.(?P<number>[1-9][0-9]*) .+$", re.MULTILINE)
DETAIL_PATTERN = re.compile(r"^- Pattern ID：(?P<patterns>.+)。$", re.MULTILINE)
BACKTICK_PATTERN = re.compile(r"`(?P<pattern>[A-Z][A-Z0-9_]+)`")
PLAYBOOK_PATHS = (
    Path("docs/quest/QUEST_REPAIR_PLAYBOOK.zh-CN.md"),
    Path("docs/quest/repair-playbook/PATTERNS.zh-CN.md"),
    Path("docs/quest/repair-playbook/CASES.zh-CN.md"),
)
PLAYBOOK_SHARD_GLOBS = ("docs/quest/repair-playbook/cases/*.md",)


@dataclass(frozen=True)
class PatternEntry:
    pattern_id: str
    proof: str


@dataclass(frozen=True)
class ValidationResult:
    errors: tuple[str, ...]
    pattern_count: int
    representative_commit_count: int
    representative_test_count: int
    detailed_case_count: int


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate quest repair Playbook pattern coverage and references.")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    return parser.parse_args()


def resolve_commit(root: Path, reference: str) -> str | None:
    result = subprocess.run(
        ["git", "rev-parse", "--verify", f"{reference}^{{commit}}"],
        cwd=root,
        check=False,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip() if result.returncode == 0 else None


def load_playbook(root: Path) -> str:
    missing = [str(path) for path in PLAYBOOK_PATHS if not (root / path).is_file()]
    if missing:
        raise SystemExit("quest repair Playbook files are missing:\n- " + "\n- ".join(missing))
    required_paths = [root / path for path in PLAYBOOK_PATHS]
    shard_paths = [path for pattern in PLAYBOOK_SHARD_GLOBS for path in sorted(root.glob(pattern))]
    return "\n".join(path.read_text(encoding="utf-8") for path in required_paths + shard_paths)


def load_test_sources(root: Path) -> dict[str, tuple[str, ...]]:
    test_sources: dict[str, list[str]] = {}
    for path in (root / "src/test/java").rglob("*Test.java"):
        test_sources.setdefault(path.stem, []).append(path.read_text(encoding="utf-8"))
    return {test_class: tuple(sources) for test_class, sources in test_sources.items()}


def defines_test_method(source: str, method_name: str) -> bool:
    return re.search(rf"\bvoid\s+{re.escape(method_name)}\s*\(", source) is not None


def detail_sections(source: str) -> list[tuple[str, str]]:
    headings = list(DETAIL_HEADING.finditer(source))
    sections: list[tuple[str, str]] = []
    for index, heading in enumerate(headings):
        end = headings[index + 1].start() if index + 1 < len(headings) else source.find("\n## 9.", heading.end())
        if end < 0:
            end = len(source)
        sections.append((heading.group("number"), source[heading.start():end]))
    return sections


def validate_details(source: str, declared_patterns: set[str], errors: list[str]) -> None:
    required = ("代表任务", "根因", "修复层", "修改文件", "验证命令和结果", "复用边界", "commit")
    sections = detail_sections(source)
    if not sections:
        errors.append("no detailed cases found under section 8")
        return
    for number, section in sections:
        for label in required:
            if not re.search(rf"^- {re.escape(label)}：", section, re.MULTILINE):
                errors.append(f"case 8.{number} is missing field {label}")
        if not re.search(r"^- 玩家(?:可见)?症状：", section, re.MULTILINE):
            errors.append(f"case 8.{number} is missing player-visible symptom")
        pattern_line = DETAIL_PATTERN.search(section)
        if pattern_line is None:
            errors.append(f"case 8.{number} is missing Pattern ID")
            continue
        patterns = set(BACKTICK_PATTERN.findall(pattern_line.group("patterns")))
        if not patterns:
            errors.append(f"case 8.{number} has an empty Pattern ID field")
        for pattern_id in sorted(patterns - declared_patterns):
            errors.append(f"case 8.{number} references undeclared pattern {pattern_id}")


def validate_playbook(
    source: str,
    test_sources: Mapping[str, Sequence[str]],
    commit_resolver: Callable[[str], str | None],
) -> ValidationResult:
    errors: list[str] = []

    pattern_matches = list(PATTERN_ROW.finditer(source))
    entries = [PatternEntry(match.group("pattern"), match.group("proof")) for match in pattern_matches]
    declared_patterns = {entry.pattern_id for entry in entries}
    if len(declared_patterns) != len(entries):
        duplicates = sorted({entry.pattern_id for entry in entries if sum(
            candidate.pattern_id == entry.pattern_id for candidate in entries
        ) > 1})
        errors.append("duplicate Pattern ID: " + ", ".join(duplicates))

    proof_commits: set[str] = set()
    proof_tests: set[str] = set()
    for entry in entries:
        commit_refs = PROOF_COMMIT.findall(entry.proof)
        test_refs = list(PROOF_TEST.finditer(entry.proof))
        if not commit_refs:
            errors.append(f"pattern {entry.pattern_id} has no representative commit")
        if not test_refs:
            errors.append(f"pattern {entry.pattern_id} has no representative test method")
        for reference in commit_refs:
            resolved = commit_resolver(reference)
            if resolved is None:
                errors.append(f"pattern {entry.pattern_id} references unknown commit {reference}")
            else:
                proof_commits.add(resolved)
        for test_ref in test_refs:
            test_class = test_ref.group("test_class")
            test_method = test_ref.group("test_method")
            qualified_name = f"{test_class}#{test_method}"
            proof_tests.add(qualified_name)
            class_sources = test_sources.get(test_class)
            if class_sources is None:
                errors.append(f"pattern {entry.pattern_id} references missing test class {test_class}")
            elif not any(defines_test_method(test_source, test_method) for test_source in class_sources):
                errors.append(f"pattern {entry.pattern_id} references missing test method {qualified_name}")

    representative_refs = set(INDEX_COMMIT.findall(source)) | set(DETAIL_COMMIT.findall(source))
    representative_commits: set[str] = set()
    for reference in sorted(representative_refs):
        resolved = commit_resolver(reference)
        if resolved is None:
            errors.append(f"Playbook references unknown representative commit {reference}")
        else:
            representative_commits.add(resolved)
    for commit in sorted(representative_commits - proof_commits):
        errors.append(f"representative commit has no Pattern fingerprint: {commit[:12]}")

    validate_details(source, declared_patterns, errors)
    return ValidationResult(
        errors=tuple(errors),
        pattern_count=len(entries),
        representative_commit_count=len(representative_commits),
        representative_test_count=len(proof_tests),
        detailed_case_count=len(detail_sections(source)),
    )


def main() -> None:
    args = parse_args()
    root = args.root.resolve()
    result = validate_playbook(
        load_playbook(root),
        load_test_sources(root),
        lambda reference: resolve_commit(root, reference),
    )
    if result.errors:
        raise SystemExit("quest repair Playbook validation failed:\n- " + "\n- ".join(result.errors))
    print(
        f"PLAYBOOK_PATTERNS={result.pattern_count} "
        f"REPRESENTATIVE_COMMITS={result.representative_commit_count} "
        f"REPRESENTATIVE_TESTS={result.representative_test_count} "
        f"DETAILED_CASES={result.detailed_case_count}"
    )


if __name__ == "__main__":
    main()
