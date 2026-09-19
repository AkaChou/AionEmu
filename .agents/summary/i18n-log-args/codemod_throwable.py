#!/usr/bin/env python3
"""Move throwables out of I18n.get(...) and onto the enclosing logging call.

规则 / Rule: 异常必须作为日志调用的最后一个参数传递，才能打印堆栈；
I18n.get 只接收模板能消费的普通参数。

用法 / Usage: python3 .agents/summary/i18n-log-args/codemod_throwable.py [--write]
"""
from __future__ import annotations

import json
import re
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_log_args import (  # noqa: E402
    ROOT,
    STRING_LITERAL,
    match_paren,
    split_top_level,
    strip_comments_preserving_offsets,
)

CLASSIFICATION = ROOT / ".agents/summary/i18n-log-args/classification.json"
THROWABLE = re.compile(r"^(?:e|ex|exc|error|t|th|thr|throwable|cause|exception|var\d+)$")
GENERIC = {"A-stack-trace-lost", "E-throwable-only-inline", "B-throwable-also-logged"}
# 手动裁决：模板无法消费的非异常参数 / manual decisions for args the template cannot consume.
OVERRIDES = {
    ("src/main/java/com/aionemu/gameserver/dao/impl/ItemStoneListDAO.java", 249): {"keep": 0, "append": "rollbackEx"},
    ("src/main/java/com/aionemu/gameserver/dataholders/WalkerData.java", 119): {"keep": 1, "append": "e1.getCause()"},
    ("src/main/java/com/aionemu/gameserver/dataholders/WalkerData.java", 133): {"keep": 1, "append": "e.getCause()"},
    ("src/main/java/com/aionemu/gameserver/dataholders/ZoneData.java", 158): {"keep": 1, "append": "e1.getCause()"},
    ("src/main/java/com/aionemu/gameserver/dataholders/ZoneData.java", 172): {"keep": 1, "append": "e.getCause()"},
    ("src/main/java/com/aionemu/gameserver/dataholders/loadingutils/XmlDataLoader.java", 342): {"keep": 0, "append": None},
    ("src/main/java/com/aionemu/gameserver/world/zone/ZoneService.java", 195): {"keep": 0, "append": "e"},
}


def is_throwable(expr: str) -> bool:
    expr = expr.strip()
    if expr.startswith("new ") and re.search(r"(Exception|Error|Throwable)", expr):
        return True
    return bool(THROWABLE.match(expr))


def comma_offsets(inner: str) -> list[int]:
    offsets, depth, i, n = [], 0, 0, len(inner)
    while i < n:
        c = inner[i]
        if c == '"':
            i += 1
            while i < n:
                if inner[i] == "\\":
                    i += 2
                    continue
                if inner[i] == '"':
                    break
                i += 1
        elif c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == "," and depth == 0:
            offsets.append(i)
        i += 1
    return offsets


def decide(row: dict) -> dict | None:
    key = (row["file"], row["line"])
    if key in OVERRIDES:
        return dict(OVERRIDES[key])
    if row["category"] == "multi-throwable" or row["category"] in GENERIC:
        args = row["i18n_args"]
        keep = len(args)
        while keep > 0 and is_throwable(args[keep - 1]):
            keep -= 1
        if keep == len(args):
            return None
        append = args[-1] if row["category"] != "B-throwable-also-logged" else None
        return {"keep": keep, "append": append}
    return None


def main() -> int:
    write = "--write" in sys.argv
    rows = json.loads(CLASSIFICATION.read_text(encoding="utf-8"))
    by_file: dict[str, list[dict]] = defaultdict(list)
    for row in rows:
        if decide(row) is not None:
            by_file[row["file"]].append(row)

    planned = skipped = 0
    for rel, file_rows in sorted(by_file.items()):
        path = ROOT / rel
        source = path.read_text(encoding="utf-8")
        stripped = strip_comments_preserving_offsets(source)
        edits = []
        for row in sorted(file_rows, key=lambda r: -r["i18n_call_start"]):
            start, end = row["i18n_call_start"], row["i18n_call_end"]
            inner_start = stripped.index("(", start) + 1
            original_inner = source[inner_start : end - 1]
            stripped_inner = stripped[inner_start : end - 1]
            args = split_top_level(stripped_inner)
            keep = decide(row)["keep"]
            append = decide(row)["append"]
            if len(args) != len(row["i18n_args"]) + 1:
                print(f"SKIP {rel}:{row['line']} arg-count mismatch")
                skipped += 1
                continue
            if len(split_top_level(original_inner)) != len(args):
                print(f"SKIP {rel}:{row['line']} comment inside args")
                skipped += 1
                continue
            commas = comma_offsets(stripped_inner)
            if keep == len(row["i18n_args"]):
                new_inner = original_inner
            else:
                new_inner = original_inner[: commas[keep]].rstrip()
            prefix = source[start:inner_start]
            replacement = prefix + new_inner + ")"
            if append:
                replacement += ", " + append
            edits.append((start, end, replacement))
            planned += 1
        if not edits:
            continue
        new_source = source
        for start, end, replacement in edits:
            new_source = new_source[:start] + replacement + new_source[end:]
        if new_source == source:
            print(f"UNCHANGED {rel}")
        if write:
            path.write_text(new_source, encoding="utf-8")
    print(f"planned rewrites: {planned}  skipped: {skipped}  files: {len(by_file)}  write={write}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
