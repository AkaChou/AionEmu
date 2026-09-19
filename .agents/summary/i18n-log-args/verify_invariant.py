#!/usr/bin/env python3
"""Verify the localization invariant: I18n.get args must match the template placeholders.

用法 / Usage: python3 .agents/summary/i18n-log-args/verify_invariant.py
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_log_args import (  # noqa: E402
    BUNDLES,
    I18N_OPEN,
    ROOT,
    SRC,
    STRING_LITERAL,
    load_bundle,
    match_paren,
    placeholders,
    split_top_level,
    strip_comments_preserving_offsets,
)

bundles = {name: load_bundle(path) for name, path in BUNDLES.items()}
THROWABLE = re.compile(r"^(?:e|ex|exc|error|t|th|thr|throwable|cause|exception|var\d+)$")
violations = []
checked = 0
for path in sorted(SRC.rglob("*.java")):
    source = path.read_text(encoding="utf-8")
    stripped = strip_comments_preserving_offsets(source)
    for m in I18N_OPEN.finditer(stripped):
        inner_start = stripped.index("(", m.start()) + 1
        end = match_paren(stripped, inner_start - 1)
        args = split_top_level(stripped[inner_start : end - 1])
        literal = STRING_LITERAL.fullmatch(args[0].strip())
        if not literal:
            continue
        key = literal.group(1)
        if key not in bundles["en"] or key not in bundles["zh"]:
            violations.append((f"{path.relative_to(ROOT)}:{source.count(chr(10), 0, m.start()) + 1}", key, "missing key"))
            continue
        passed = len(args) - 1
        for name, bundle in bundles.items():
            want = max(placeholders(bundle[key]), default=-1) + 1
            checked += 1
            if want != passed:
                violations.append(
                    (
                        f"{path.relative_to(ROOT)}:{source.count(chr(10), 0, m.start()) + 1}",
                        key,
                        f"{name} template wants {want} args, call passes {passed}",
                    )
                )
        for arg in args[1:]:
            if THROWABLE.match(arg.strip()) or (arg.strip().startswith("new ") and "Exception" in arg):
                violations.append(
                    (
                        f"{path.relative_to(ROOT)}:{source.count(chr(10), 0, m.start()) + 1}",
                        key,
                        f"throwable passed to I18n.get: {arg.strip()}",
                    )
                )
print("call sites checked:", checked // 2)
print("violations:", len(violations))
for where, key, why in violations:
    print(f"  {where}  {key}: {why}")
sys.exit(1 if violations else 0)
