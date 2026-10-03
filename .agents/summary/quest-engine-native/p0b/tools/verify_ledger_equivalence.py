#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""quest/retail 台账 TSV↔XML 等价性对拍器（2026-10-03 台账 XML 化批，S3 证据件）。

口径 / Equivalence rule:
- before 侧 = `git show HEAD:<tsv>`（落定基线；HEAD 缺失时回退读工作树），按同一行切分规则
  重建规范行（列值 strip；可选尾列缺席/空白 = None）；注释行同时取出。
- after 侧 = XML 解析（xml.etree），行元素 = 根的直接子元素按文档序；列值 = 列子元素文本
  strip（可选尾列缺席/空白 = None；显式反转义由解析器完成）。
- 判定 = 行数相等 ∧ 逐行元组全等 ∧ 全部注释行（改写后）在 XML 文本中出现；retention 另查
  两副本逐字节相等。输出 `p0b/ledger-xml/ledger-equivalence.tsv`（before/after sha256 + 判定）。

`--self-test`：故意改 1 个字符必须红（对拍器自证)，注释丢失检查同理。自测失败即整体失败。

Verify that every ledger's canonical rows survive the TSV-to-XML conversion byte-for-byte
(columns rebuilt from both sides, optional trailing column = None), that every comment line
survives as text, and that the retention copies stay identical. `--self-test` proves the
comparator goes red on a one-character mutation.
"""

from __future__ import annotations

import hashlib
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

from convert_ledgers_to_xml import REPO, TABLES, Table, apply_substitutions, parse_source  # noqa: E402

EVIDENCE_DIR = REPO / ".agents/summary/quest-engine-native/p0b/ledger-xml"
EVIDENCE_TSV = EVIDENCE_DIR / "ledger-equivalence.tsv"


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def before_bytes(table: Table) -> bytes:
    """before 侧字节：HEAD 优先，缺失回退工作树。 / Before-side bytes: HEAD first, worktree fallback."""
    relative = str(table.source.relative_to(REPO))
    completed = subprocess.run(["git", "show", f"HEAD:{relative}"], cwd=REPO,
                               capture_output=True)
    if completed.returncode == 0:
        return completed.stdout
    if table.source.is_file():
        return table.source.read_bytes()
    raise SystemExit(f"{table.stem}: neither HEAD nor the working tree has {relative}")


def canonical_rows(table: Table, text: str) -> tuple[list[tuple], list[str]]:
    """规范行与注释行。 / Canonical rows and comment lines."""
    header, entries = parse_source(table, text)
    interleaved = [payload for kind, payload in entries if kind == "comment"]
    comments = apply_substitutions(header + interleaved, table)
    rows = [tuple(payload) for kind, payload in entries if kind == "row"]
    return rows, comments


def xml_rows(table: Table, path: Path) -> list[tuple]:
    """XML 侧规范行（保文档序；尾可选列缺席/空白 = None）。 / Canonical rows from the XML side."""
    root = ET.parse(path).getroot()
    if root.tag != table.root:
        raise SystemExit(f"{table.stem}: unexpected root <{root.tag}> in {path}")
    rows: list[tuple] = []
    for child in root:
        if child.tag != table.row:
            raise SystemExit(f"{table.stem}: unexpected row element <{child.tag}> in {path}")
        values: list[str | None] = []
        seen: set[str] = set()
        for column in table.columns:
            node = child.find(column.tag)
            if node is not None:
                if column.tag in seen:
                    raise SystemExit(f"{table.stem}: duplicate <{column.tag}> in {path}")
                seen.add(column.tag)
                value = (node.text or "").strip()
                if column.optional and value == "":
                    value = None
            else:
                if not column.optional:
                    raise SystemExit(f"{table.stem}: missing <{column.tag}> in {path}")
                value = None
            values.append(value)
        for extra in child:
            if extra.tag not in {column.tag for column in table.columns}:
                raise SystemExit(f"{table.stem}: unexpected column <{extra.tag}> in {path}")
        rows.append(tuple(values))
    return rows


def first_difference(before: list[tuple], after: list[tuple]) -> str | None:
    """首个差异描述；无差异返回 None。 / First difference description or None."""
    if len(before) != len(after):
        return f"row count {len(before)} != {len(after)}"
    for index, (left, right) in enumerate(zip(before, after), 1):
        if left != right:
            return f"row {index}: {left!r} != {right!r}"
    return None


def comment_hits(table: Table, comments: list[str], xml_text: str) -> list[str]:
    """未能在 XML 文本中找到的注释行。 / Comment lines missing from the XML text."""
    return [line for line in comments if line and line not in xml_text]


def check_table(table: Table, xml_text: str | None = None) -> tuple[str, str, str, int, str]:
    """对拍一张表；返回（before sha、after sha、判定、行数、消息）。 / Verify one table."""
    raw = before_bytes(table)
    text = raw.decode("utf-8")
    before, comments = canonical_rows(table, text)
    main_target = table.targets[0]
    if xml_text is None:
        xml_text = main_target.read_text(encoding="utf-8")
    after = xml_rows(table, main_target)
    difference = first_difference(before, after)
    if difference:
        return sha256_bytes(raw), sha256_bytes(main_target.read_bytes()), "DIFFERENT", len(before), difference
    missing = comment_hits(table, comments, xml_text)
    if missing:
        return sha256_bytes(raw), sha256_bytes(main_target.read_bytes()), "COMMENT_LOST", len(before), missing[0]
    if len(table.targets) > 1:
        first = main_target.read_bytes()
        for other in table.targets[1:]:
            if other.read_bytes() != first:
                return (sha256_bytes(raw), sha256_bytes(first), "COPY_DRIFT", len(before),
                        f"{other} differs byte-wise from {main_target}")
    return sha256_bytes(raw), sha256_bytes(main_target.read_bytes()), "IDENTICAL", len(before), ""


def self_test() -> int:
    """对拍器自证：改 1 个字符、丢 1 行注释都必须被检出。 / Comparator self-test must go red on mutation."""
    problems: list[str] = []
    table = TABLES[-1]
    rows, comments = canonical_rows(table, before_bytes(table).decode("utf-8"))
    mutated = [list(row) for row in rows]
    mutated[0][0] = (mutated[0][0] or "") + "X"
    if first_difference(rows, [tuple(row) for row in mutated]) is None:
        problems.append("value mutation not detected")
    if first_difference(rows, rows[:-1]) is None:
        problems.append("row-count mutation not detected")
    xml_text = table.targets[0].read_text(encoding="utf-8") if table.targets[0].is_file() else None
    if xml_text is not None:
        missing_real = comment_hits(table, comments, xml_text)
        if missing_real:
            problems.append(f"control comment missing from XML: {missing_real[0]}")
        if not comment_hits(table, ["definitely-absent-comment-line"], xml_text):
            problems.append("absent comment not detected")
    if problems:
        print("SELF-TEST FAILED: " + "; ".join(problems), file=sys.stderr)
        return 1
    print("self-test OK (value mutation / row-count mutation / comment checks all red as expected)")
    return 0


def main(argv: list[str]) -> int:
    status = self_test()
    if status != 0:
        return status
    lines = ["table\trows\tbefore_sha256\tafter_sha256\tverdict\tnote"]
    failures = 0
    for table in TABLES:
        before_sha, after_sha, verdict, rows, message = check_table(table)
        lines.append(f"{table.stem}\t{rows}\t{before_sha}\t{after_sha}\t{verdict}\t{message}")
        marker = "OK  " if verdict == "IDENTICAL" else "FAIL"
        print(f"{marker} {table.stem}: rows={rows} verdict={verdict} {message}")
        if verdict != "IDENTICAL":
            failures += 1
    EVIDENCE_DIR.mkdir(parents=True, exist_ok=True)
    EVIDENCE_TSV.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"evidence: {EVIDENCE_TSV.relative_to(REPO)}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
