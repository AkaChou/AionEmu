#!/usr/bin/env python3
"""Audit localized log calls whose message template cannot consume the passed args.

用法 / Usage:
  python3 .agents/summary/i18n-log-args/audit_log_args.py [--tsv out.tsv]
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "src/main/java"
BUNDLES = {
    "en": ROOT / "src/main/resources/messages.properties",
    "zh": ROOT / "src/main/resources/messages_zh_CN.properties",
}
LOG_OPEN = re.compile(r"\b(log|logger|LOG|LOGGER)\.(trace|debug|info|warn|error|fatal)\s*\(")
I18N_OPEN = re.compile(r"\bI18n\.get\s*\(")
THROWABLE_HINT = re.compile(r"^(e|ex|exc|error|t|th|thr|throwable|cause|exception)$")
STRING_LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')


def strip_comments_preserving_offsets(text: str) -> str:
    out = list(text)
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c == '"':
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == '"':
                    i += 1
                    break
                i += 1
            continue
        if c == "'":
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == "'":
                    i += 1
                    break
                i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                out[i] = " "
                i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "*":
            out[i] = out[i + 1] = " "
            i += 2
            while i < n and not (text[i] == "*" and i + 1 < n and text[i + 1] == "/"):
                if text[i] != "\n":
                    out[i] = " "
                i += 1
            if i < n:
                out[i] = out[i + 1] = " "
                i += 2
            continue
        i += 1
    return "".join(out)


def match_paren(text: str, open_idx: int) -> int:
    """Return index just past the ')' matching text[open_idx] == '('."""
    depth = 0
    i = open_idx
    n = len(text)
    while i < n:
        c = text[i]
        if c == '"':
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == '"':
                    break
                i += 1
            i += 1
            continue
        if c == "'":
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == "'":
                    break
                i += 1
            i += 1
            continue
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    return -1


def split_top_level(args: str) -> list[str]:
    parts: list[str] = []
    depth = 0
    cur = []
    i, n = 0, len(args)
    while i < n:
        c = args[i]
        if c == '"':
            cur.append(c)
            i += 1
            while i < n:
                cur.append(args[i])
                if args[i] == "\\":
                    i += 1
                    cur.append(args[i]) if i < n else None
                    i += 1
                    continue
                if args[i] == '"':
                    i += 1
                    break
                i += 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        if c == "," and depth == 0:
            parts.append("".join(cur).strip())
            cur = []
            i += 1
            continue
        cur.append(c)
        i += 1
    if "".join(cur).strip():
        parts.append("".join(cur).strip())
    return parts


def load_bundle(path: Path) -> dict[str, str]:
    messages: dict[str, str] = {}
    lines = path.read_text(encoding="utf-8").split("\n")
    key = None
    buf = ""
    for raw in lines:
        line = raw.rstrip("\r")
        if not buf and (not line or line.lstrip().startswith("#") or line.lstrip().startswith("!")):
            continue
        if buf:
            buf += line.lstrip()
        else:
            if "=" not in line:
                continue
            key, value = line.split("=", 1)
            key, buf = key.strip(), value
        if buf.endswith("\\") and not buf.endswith("\\\\"):
            buf = buf[:-1]
            continue
        messages[key] = buf.replace("\\:", ":").replace("\\=", "=").replace("\\!", "!")
        key, buf = None, ""
    return messages


def placeholders(template: str) -> list[int]:
    unquoted = re.sub(r"''", "", template)
    return [int(m.group(1)) for m in re.finditer(r"\{(\d+)", unquoted)]


def statement_context(text: str, idx: int, window: int = 120) -> str:
    start = text.rfind(";", max(0, idx - 400), idx)
    return " ".join(text[start + 1 : idx].split())[-window:]


def collect(path: Path):
    source = path.read_text(encoding="utf-8")
    stripped = strip_comments_preserving_offsets(source)
    log_spans = []
    for m in LOG_OPEN.finditer(stripped):
        open_idx = stripped.index("(", m.start())
        end = match_paren(stripped, open_idx)
        if end < 0:
            continue
        log_spans.append((m.start(), end, m.group(2), stripped[open_idx + 1 : end - 1]))
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
        enclosing = None
        for span in log_spans:
            if span[0] <= m.start() and end <= span[1]:
                if enclosing is None or (span[1] - span[0]) < (enclosing[1] - enclosing[0]):
                    enclosing = span
        rows.append(
            {
                "file": path.relative_to(ROOT).as_posix(),
                "line": source.count("\n", 0, m.start()) + 1,
                "key": literal.group(1) if literal else "",
                "key_expr": args[0].strip(),
                "call": "I18n.get(" + ", ".join(args) + ")",
                "arg_count": len(args) - 1,
                "args": args[1:],
                "log_level": enclosing[2] if enclosing else "",
                "log_args_after": enclosing[3].split(",", 1)[1] if enclosing and "," in enclosing[3] else "",
                "in_log": enclosing is not None,
                "context": statement_context(stripped, m.start()),
            }
        )
    return rows


def main() -> int:
    bundles = {name: load_bundle(path) for name, path in BUNDLES.items()}
    rows = []
    for path in sorted(SRC.rglob("*.java")):
        rows.extend(collect(path))
    tsv_lines = [
        "file\tline\tlevel\tkey\targ_count\tused\tstatus\ten_arg\tzh_arg\ten_placeholders\tzh_placeholders\tcall"
    ]
    counts: dict[str, int] = {}
    missing = []
    for row in rows:
        en = bundles["en"].get(row["key"]) if row["key"] else None
        zh = bundles["zh"].get(row["key"]) if row["key"] else None
        en_ph = placeholders(en) if en is not None else placeholders(row["key_expr"])
        zh_ph = placeholders(zh) if zh is not None else placeholders(row["key_expr"])
        used = (max(en_ph) + 1) if en_ph else 0
        if not row["in_log"]:
            status = "not-a-log-call"
        elif not row["key"]:
            status = "dynamic-key"
        elif en is None or zh is None:
            status = "missing-key"
        elif used < row["arg_count"]:
            status = "arg-dropped"
        elif used > row["arg_count"]:
            status = "missing-arg"
        else:
            status = "ok"
        if status == "arg-dropped":
            last = row["args"][-1].strip()
            if THROWABLE_HINT.match(last.split(".")[-1]):
                status = "throwable-dropped"
        elif status == "ok" and row["args"]:
            last = row["args"][-1].strip()
            if THROWABLE_HINT.match(last.split(".")[-1]) and not row["log_args_after"].strip():
                status = "throwable-inline"
        counts[status] = counts.get(status, 0) + 1
        if status in {"arg-dropped", "throwable-dropped"}:
            missing.append(row)
        tsv_lines.append(
            "\t".join(
                [
                    row["file"],
                    str(row["line"]),
                    row["log_level"],
                    row["key"],
                    str(row["arg_count"]),
                    str(used),
                    status,
                    (en or "").replace("\t", " "),
                    (zh or "").replace("\t", " "),
                    str(en_ph),
                    str(zh_ph),
                    row["call"].replace("\t", " ").replace("\n", " "),
                ]
            )
        )
    out = ROOT / ".agents/summary/i18n-log-args/audit.tsv"
    out.write_text("\n".join(tsv_lines) + "\n", encoding="utf-8")
    print("total I18n.get-with-args call sites:", len(rows))
    for status, count in sorted(counts.items(), key=lambda kv: -kv[1]):
        print(f"  {status:18s} {count}")
    print("\nfiles with dropped args:", len({r['file'] for r in missing}))
    print("distinct keys with dropped args:", len({r["key"] for r in missing}))
    print("wrote", out.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
