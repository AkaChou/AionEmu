#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""quest/retail 八张自造 TSV 台账 → XML 转换器（2026-10-03 台账 XML 化批）。

输入：工作树上的 8 个 TSV（quest/retail/）；输出：同目录同名 XML（2 空格缩进、LF、UTF-8、
TSV 的 `#` 注释逐行映射为 XML 注释；retention 另落 test 侧副本一份，同字节）。
转换是纯机械同构：每数据行 → 行元素，每列 → 列子元素，值仅做 XML 文本转义（& < >），
不做任何语义归一化；行序、列序、值逐字保持。防呆：目标已存在即报错（--force 覆盖）、
注释文本硬校验（XML 注释不得含 `--`、不得以 `-` 结尾）、控制字符校验。

Convert the eight self-made quest/retail TSV ledgers to XML (2026-10-03 ledger-XML batch).
Mechanical isomorphism only: one row element per data row, one child element per column, values
escaped (& < >) and otherwise byte-preserved; row/column order preserved. Fails fast on an
existing target (--force overrides), on illegal XML comments (`--` inside, trailing `-`) and on
control characters.

用法 / Usage:
    python3 convert_ledgers_to_xml.py [--force]

证据口径：转换前先校验 8 个 TSV 路径工作树干净（本脚本打印 git HEAD 与目标路径 git status
快照，粘进批证据文档）。
"""

from __future__ import annotations

import hashlib
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

REPO = Path(__file__).resolve().parents[5]
MAIN_DIR = "src/main/resources/aion/data/static_data/quest/retail"
TEST_RETENTION_TARGET = "src/test/resources/quest/retail-xml-retention.xml"


@dataclass(frozen=True)
class Column:
    """一列：元素名 + 是否可缺（可缺列只允许在行尾）。 / One column: element name, optional (trailing only)."""

    tag: str
    optional: bool = False


@dataclass(frozen=True)
class Table:
    """一张表：文件主干、根元素、行元素、列序、注释改写对、附加目标。 / One table definition."""

    stem: str
    root: str
    row: str
    columns: tuple[Column, ...]
    substitutions: tuple[tuple[str, str], ...] = ()
    extra_targets: tuple[str, ...] = ()

    @property
    def source(self) -> Path:
        return REPO / MAIN_DIR / f"{self.stem}.tsv"

    @property
    def targets(self) -> tuple[Path, ...]:
        return (REPO / MAIN_DIR / f"{self.stem}.xml",) + tuple(REPO / t for t in self.extra_targets)


TABLES: tuple[Table, ...] = (
    Table(
        "quest_client_handin_npc_sets", "quest_client_handin_npc_sets", "handin_npc_set",
        (Column("quest_id"), Column("npc_ids")),
        ((r"（quest_id \t npc_id,npc_id,...）", "（quest_id / npc_ids 逗号串）"),),
    ),
    Table(
        "quest_legacy_heal_rows", "quest_legacy_heal_rows", "heal_row",
        (Column("quest_id"), Column("stale_row"), Column("reward_row"), Column("evidence")),
    ),
    Table(
        "quest_name_string_ids", "quest_name_string_ids", "name_string_id",
        (Column("key"), Column("string_id")),
    ),
    Table(
        "retail-instance-entry-points", "retail_instance_entry_points", "entry_point",
        (Column("creation_id"), Column("world_id"), Column("alias"), Column("x"), Column("y"),
         Column("z"), Column("heading"), Column("resolved"), Column("source")),
        (("creationId\tworldId\tstartAlias\tx\ty\tz\theading\tresolved\tsource",
          "列：creation_id / world_id / alias / x / y / z / heading / resolved / source"),),
    ),
    Table(
        "retail-npc-name-aliases", "retail_npc_name_aliases", "npc_name_alias",
        (Column("name"), Column("npc_ids")),
        (("alias\tnpc_ids", "name / npc_ids（逗号串）"),),
    ),
    Table(
        "retail-quest-ai-name-groups", "retail_quest_ai_name_groups", "quest_ai_name_group",
        (Column("quest_ai_name"), Column("member_name_descs")),
        (("quest_ai_name\tmember_name_descs", "quest_ai_name / member_name_descs（逗号串）"),),
    ),
    Table(
        "retail-quest-string-ids", "retail_quest_string_ids", "quest_string_id",
        (Column("key"), Column("string_id"), Column("body", optional=True)),
        (("第 3 列 body = 真端 <body> 原文", "body 元素 = 真端 <body> 原文"),),
    ),
    Table(
        "retail-xml-retention", "retail_xml_retention", "quest",
        (Column("quest_id"), Column("owner"), Column("family"), Column("reason"), Column("evidence")),
        (),
        (TEST_RETENTION_TARGET,),
    ),
)


class ConversionError(Exception):
    """转换失败（fail-fast）。 / Conversion failure (fail fast)."""


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def escape(text: str) -> str:
    """XML 文本转义（只做三实体，与装载器口径一致；不用 CDATA）。 / Escape & < > only, no CDATA."""
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def check_text(text: str, where: str) -> None:
    """值文本校验：不允许 C0 控制字符（含 \r、\t 意外残留在值内）。 / Reject C0 controls in values."""
    for ch in text:
        if ord(ch) < 0x20:
            raise ConversionError(f"{where}: control character U+{ord(ch):04X} in value {text!r}")


def check_comment(text: str, where: str) -> None:
    """XML 注释文本校验：不得含 `--`、不得以 `-` 结尾。 / XML comment rules."""
    if "--" in text:
        raise ConversionError(f"{where}: comment contains '--': {text!r}")
    if text.endswith("-"):
        raise ConversionError(f"{where}: comment ends with '-': {text!r}")


def apply_substitutions(lines: list[str], table: Table) -> list[str]:
    """按配置改写注释行；每个改写对必须至少命中一次（防静默失配）。 / Apply configured rewrites, require hits."""
    result: list[str] = []
    remaining = {old: 0 for old, _ in table.substitutions}
    for line in lines:
        for old, new in table.substitutions:
            if old in line:
                line = line.replace(old, new)
                remaining[old] += 1
        result.append(line)
    for old, hits in remaining.items():
        if hits == 0:
            raise ConversionError(f"{table.stem}: substitution never matched: {old!r}")
    return result


def comment_text(line: str) -> str:
    """TSV 注释行 → 注释文本（去前缀 `#` 与一个空格）。 / TSV comment line to comment text."""
    body = line[1:]
    if body.startswith(" "):
        body = body[1:]
    return body


def parse_source(table: Table, text: str) -> tuple[list[str], list[object]]:
    """解析 TSV：返回（头部注释行、条目序列表（注释行 or 数据行值列表））。 / Parse the TSV."""
    entries: list[object] = []
    header: list[str] = []
    required = sum(1 for column in table.columns if not column.optional)
    total = len(table.columns)
    for index, raw in enumerate(text.split("\n"), 1):
        if raw == "":
            continue
        if raw.startswith("#"):
            entries.append(("comment", comment_text(raw)))
            continue
        line = raw.rstrip("\n")
        if line.strip() == "":
            continue
        cells = line.strip().split("\t")
        if not (required <= len(cells) <= total):
            raise ConversionError(
                f"{table.stem}:{index}: expected {required}..{total} columns, got {len(cells)}: {line!r}")
        values: list[str | None] = []
        for position, column in enumerate(table.columns):
            if position >= len(cells):
                values.append(None)
                continue
            value = cells[position].strip()
            check_text(value, f"{table.stem}:{index}")
            if column.optional and value == "":
                values.append(None)
            else:
                values.append(value)
        entries.append(("row", values))
    # 头部 = 从第一条注释起、直到第一条数据行前的连续注释（其余注释按位置落点）。
    while entries and entries[0][0] == "comment":
        header.append(entries.pop(0)[1])
    return header, entries


def render(table: Table, header: list[str], entries: list[object]) -> str:
    """渲染 XML 文本（2 空格缩进、LF、末行换行）。 / Render the XML text."""
    for column_index, column in enumerate(table.columns):
        if column.optional:
            for later in table.columns[column_index + 1:]:
                if not later.optional:
                    raise ConversionError(f"{table.stem}: required column after optional column")
    lines: list[str] = ['<?xml version="1.0" encoding="UTF-8"?>']
    if header:
        lines.append("<!--")
        lines.extend(header)
        lines.append("-->")
    lines.append(f"<{table.root}>")
    for entry in entries:
        kind, payload = entry
        if kind == "comment":
            single = payload.replace("\n", " ")
            lines.append(f"  <!-- {single} -->")
            continue
        row = payload
        lines.append(f"  <{table.row}>")
        for column, value in zip(table.columns, row):
            if value is None:
                continue
            lines.append(f"    <{column.tag}>{escape(value)}</{column.tag}>")
        lines.append(f"  </{table.row}>")
    lines.append(f"</{table.root}>")
    return "\n".join(lines) + "\n"


def convert(table: Table, force: bool) -> tuple[int, str, list[str]]:
    """转换一张表；返回（数据行数、before sha256、目标 sha256 列表）。 / Convert one table."""
    if not table.source.is_file():
        raise ConversionError(f"{table.stem}: missing source {table.source}")
    raw = table.source.read_bytes()
    text = raw.decode("utf-8")
    header, entries = parse_source(table, text)
    # 全注释行（头部 + 行间）统一改写并统一做命中校验；再按位置回填。
    # Rewrite every comment line (header + interleaved) with one hit-checked pass, then re-slot.
    interleaved = [payload for kind, payload in entries if kind == "comment"]
    rewritten = apply_substitutions(header + interleaved, table)
    for line in rewritten:
        check_comment(line, f"{table.stem} (comment)")
    header_len = len(header)
    header = rewritten[:header_len]
    tail = iter(rewritten[header_len:])
    entries = [(kind, next(tail)) if kind == "comment" else (kind, payload)
               for kind, payload in entries]
    xml = render(table, header, entries)
    data = xml.encode("utf-8")
    rows = sum(1 for kind, _ in entries if kind == "row")
    for target in table.targets:
        if target.exists() and not force:
            raise ConversionError(f"{table.stem}: target exists (use --force): {target}")
    for target in table.targets:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    return rows, sha256_bytes(raw), [sha256_bytes(data)]


def git(*args: str) -> str:
    completed = subprocess.run(["git", *args], cwd=REPO, capture_output=True, text=True)
    if completed.returncode != 0:
        raise ConversionError(f"git {' '.join(args)} failed: {completed.stderr.strip()}")
    return completed.stdout.strip()


def main(argv: list[str]) -> int:
    force = "--force" in argv[1:]
    print(f"repository : {REPO}")
    print(f"git HEAD   : {git('rev-parse', 'HEAD')}")
    paths = [str(table.source.relative_to(REPO)) for table in TABLES]
    status = git("status", "--porcelain", "--", *paths)
    print(f"tree status: {'CLEAN (precondition met)' if not status else status}")
    if status:
        print("WARNING: source TSVs are dirty; the recorded before-state is the working tree copy.")
    failures: list[str] = []
    for table in TABLES:
        try:
            rows, before, targets = convert(table, force)
        except ConversionError as error:
            failures.append(str(error))
            print(f"FAIL {table.stem}: {error}")
            continue
        print(f"OK   {table.stem}: rows={rows} before={before[:16]}… "
              f"after={targets[0][:16]}… targets={len(table.targets)}")
    if failures:
        print(f"\n{len(failures)} conversion(s) failed", file=sys.stderr)
        return 1
    print("\nall 8 ledgers converted")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
