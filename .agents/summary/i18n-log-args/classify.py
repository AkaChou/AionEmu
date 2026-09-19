#!/usr/bin/env python3
"""Classify every localized log call that passes arguments into I18n.get."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_log_args import (  # noqa: E402
    BUNDLES,
    LOG_OPEN,
    ROOT,
    SRC,
    I18N_OPEN,
    STRING_LITERAL,
    load_bundle,
    match_paren,
    placeholders,
    split_top_level,
    strip_comments_preserving_offsets,
)

THROWABLE = re.compile(r"^(?:e|ex|exc|error|t|th|thr|throwable|cause|exception|var\d+)$")


def is_throwable(expr: str) -> bool:
    expr = expr.strip()
    if expr.startswith("new ") and ("Exception" in expr or "Error" in expr or "Throwable" in expr):
        return True
    return bool(THROWABLE.match(expr))


def collect(path: Path):
    source = path.read_text(encoding="utf-8")
    stripped = strip_comments_preserving_offsets(source)
    log_spans = []
    for m in LOG_OPEN.finditer(stripped):
        open_idx = stripped.index("(", m.start())
        end = match_paren(stripped, open_idx)
        if end < 0:
            continue
        inner = stripped[open_idx + 1 : end - 1]
        log_spans.append(
            {
                "start": m.start(),
                "open": open_idx,
                "end": end,
                "level": m.group(2),
                "args": split_top_level(inner),
                "inner_start": open_idx + 1,
            }
        )
    out = []
    for m in I18N_OPEN.finditer(stripped):
        open_idx = stripped.index("(", m.start())
        end = match_paren(stripped, open_idx)
        if end < 0:
            continue
        inner = stripped[open_idx + 1 : end - 1]
        args = split_top_level(inner)
        if len(args) < 2:
            continue
        owner = None
        for span in log_spans:
            if span["open"] < m.start() and end <= span["end"]:
                if owner is None or span["end"] - span["open"] < owner["end"] - owner["open"]:
                    owner = span
        i18n_args = [a.strip() for a in args[1:]]
        log_extra = []
        if owner:
            # top-level args of the log call that follow this I18n.get call
            tail = stripped[end : owner["end"] - 1]
            log_extra = [a.strip() for a in split_top_level(tail) if a.strip()]
        literal = STRING_LITERAL.fullmatch(args[0].strip())
        out.append(
            {
                "file": path.relative_to(ROOT).as_posix(),
                "line": source.count("\n", 0, m.start()) + 1,
                "level": owner["level"] if owner else "",
                "key": literal.group(1) if literal else "",
                "key_expr": args[0].strip(),
                "i18n_args": i18n_args,
                "log_extra": log_extra,
                "i18n_call_start": m.start(),
                "i18n_call_end": end,
                "in_log": owner is not None,
            }
        )
    return out


def main() -> int:
    bundles = {name: load_bundle(path) for name, path in BUNDLES.items()}
    rows = []
    for path in sorted(SRC.rglob("*.java")):
        rows.extend(collect(path))

    buckets: dict[str, list] = {}
    for row in rows:
        en = bundles["en"].get(row["key"])
        zh = bundles["zh"].get(row["key"])
        if not row["in_log"]:
            cat = "not-a-log-call"
        elif not row["key"]:
            cat = "dynamic-key"
        elif en is None or zh is None:
            cat = "missing-key"
        else:
            used = max(placeholders(en), default=-1) + 1
            trailing = [a for a in row["i18n_args"] if is_throwable(a)]
            extra_throwable = bool(row["log_extra"]) and is_throwable(row["log_extra"][-1])
            plain = [a for a in row["i18n_args"] if not is_throwable(a)]
            row["used"] = used
            row["plain"] = plain
            row["trailing_throwable"] = trailing
            row["log_extra_throwable"] = extra_throwable
            if len(trailing) > 1:
                cat = "multi-throwable"
            elif trailing and extra_throwable:
                cat = "B-throwable-also-logged" + ("" if used == len(plain) else "+template-mismatch")
            elif trailing and used == len(row["i18n_args"]) and used > len(plain):
                # template renders the throwable inline, so the logger never sees it
                cat = "E-throwable-only-inline"
            elif trailing and used < len(row["i18n_args"]):
                cat = "A-stack-trace-lost" + ("+template-mismatch" if used < len(plain) else "")
            elif trailing:
                cat = "A-stack-trace-lost"
            elif used < len(plain):
                cat = "C-plain-arg-dropped"
            elif used > len(plain):
                cat = "D-template-wants-more"
            else:
                cat = "ok"
        row["category"] = cat
        buckets.setdefault(cat, []).append(row)

    for cat in sorted(buckets):
        print(f"{cat:36s} sites={len(buckets[cat]):4d} keys={len({r['key'] for r in buckets[cat]}):4d}")
    out = ROOT / ".agents/summary/i18n-log-args/classification.json"
    out.write_text(json.dumps(rows, ensure_ascii=False, indent=1), encoding="utf-8")
    print("wrote", out.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
