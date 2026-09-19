#!/usr/bin/env python3
"""Reconstruct the exact file list the throwable codemod rewrote, from HEAD content.

以 HEAD 内容重放分类逻辑，得到本次改写命中的文件清单，便于精确暂存。
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import codemod_throwable as cm  # noqa: E402
from audit_log_args import (  # noqa: E402
    BUNDLES,
    I18N_OPEN,
    LOG_OPEN,
    ROOT,
    STRING_LITERAL,
    load_bundle,
    match_paren,
    placeholders,
    split_top_level,
    strip_comments_preserving_offsets,
)


def collect(rel: str, source: str) -> list[dict]:
    stripped = strip_comments_preserving_offsets(source)
    log_spans = []
    for m in LOG_OPEN.finditer(stripped):
        open_idx = stripped.index("(", m.start())
        end = match_paren(stripped, open_idx)
        if end < 0:
            continue
        log_spans.append({"open": open_idx, "end": end, "level": m.group(2)})
    rows = []
    for m in I18N_OPEN.finditer(stripped):
        open_idx = stripped.index("(", m.start())
        end = match_paren(stripped, open_idx)
        if end < 0:
            continue
        args = split_top_level(stripped[open_idx + 1 : end - 1])
        if len(args) < 2:
            continue
        literal = STRING_LITERAL.fullmatch(args[0].strip())
        owner = None
        for span in log_spans:
            if span["open"] < m.start() and end <= span["end"]:
                if owner is None or span["end"] - span["open"] < owner["end"] - owner["open"]:
                    owner = span
        tail = stripped[end : owner["end"] - 1] if owner else ""
        extra = [a.strip() for a in split_top_level(tail) if a.strip()] if owner else []
        rows.append(
            {
                "file": rel,
                "line": source.count("\n", 0, m.start()) + 1,
                "level": owner["level"] if owner else "",
                "key": literal.group(1) if literal else "",
                "key_expr": args[0].strip(),
                "i18n_args": [a.strip() for a in args[1:]],
                "log_extra": extra,
                "i18n_call_start": m.start(),
                "i18n_call_end": end,
                "in_log": owner is not None,
            }
        )
    return rows


bundles = {name: load_bundle(path) for name, path in BUNDLES.items()}
touched = []
for rel in subprocess.run(["git", "ls-files", "src/main/java"], capture_output=True, text=True).stdout.split():
    if not rel.endswith(".java"):
        continue
    source = subprocess.run(["git", "show", f"HEAD:{rel}"], capture_output=True, text=True).stdout
    rated = []
    for row in collect(rel, source):
        en = bundles["en"].get(row["key"])
        zh = bundles["zh"].get(row["key"])
        if not row["in_log"] or not row["key"] or en is None or zh is None:
            continue
        used = max(placeholders(en), default=-1) + 1
        trailing = [a for a in row["i18n_args"] if cm.is_throwable(a)]
        extra_throwable = bool(row["log_extra"]) and cm.is_throwable(row["log_extra"][-1])
        plain = [a for a in row["i18n_args"] if not cm.is_throwable(a)]
        if len(trailing) > 1:
            cat = "multi-throwable"
        elif trailing and extra_throwable:
            cat = "B-throwable-also-logged"
        elif trailing and used == len(row["i18n_args"]) and used > len(plain):
            cat = "E-throwable-only-inline"
        elif trailing and used < len(row["i18n_args"]):
            cat = "A-stack-trace-lost"
        elif trailing:
            cat = "A-stack-trace-lost"
        elif used < len(plain):
            cat = "C-plain-arg-dropped"
        else:
            cat = "ok"
        row["category"] = cat
        rated.append(row)
    if any(cm.decide(row) is not None for row in rated):
        touched.append(rel)

extra = [
    "src/main/resources/messages.properties",
    "src/main/resources/messages_zh_CN.properties",
    "src/test/java/com/aionemu/boot/i18n/LocalizedLogArgumentsTest.java",
]
Path(ROOT / ".agents/summary/i18n-log-args/touched-files.txt").write_text("\n".join(touched + extra) + "\n")
print("java files rewritten:", len(touched))
print("total paths:", len(touched) + len(extra))
