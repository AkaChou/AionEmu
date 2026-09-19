#!/usr/bin/env python3
"""Retire dual-source SingletonHolder fallbacks for a list of service classes.

Usage: retire_singleton_fallback.py <file> [<file> ...] [--apply]
Without --apply it only prints a dry-run summary (canary mode).
"""
import sys
from pathlib import Path

NEW_DOC = """\t/**
\t * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
\t * Returns the instance, which must be supplied by Spring.
\t *
\t * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
\t * The legacy static fallback is retired: a missing provider now fails fast instead of silently
\t * creating a second instance outside the container.</p>
\t *
\t * @return 由 Spring 提供的实例 / the Spring-provided instance
\t * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
\t */
"""


def new_method_lines(signature, cls, cached):
    lines = ["\t" + signature.strip() + " {"]
    if cached:
        lines += [
            f"\t\t{cls} resolved = resolvedInstance;",
            "\t\tif (resolved != null) {",
            "\t\t\treturn resolved;",
            "\t\t}",
        ]
    lines += [f"\t\tObjectProvider<{cls}> provider = instanceProvider;"]
    if cached:
        lines += [f"\t\tresolved = provider == null ? null : provider.getIfAvailable();",
                  "\t\tif (resolved == null) {",
                  f'\t\t\tthrow new IllegalStateException("{cls} 未由 Spring 提供："',
                  '\t\t\t\t+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")',
                  '\t\t\t\t+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");',
                  "\t\t}",
                  "\t\tresolvedInstance = resolved;",
                  "\t\treturn resolved;"]
    else:
        lines += [
            f"\t\t{cls} provided = provider == null ? null : provider.getIfAvailable();",
            "\t\tif (provided == null) {",
            f'\t\t\tthrow new IllegalStateException("{cls} 未由 Spring 提供："',
            '\t\t\t\t+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")',
            '\t\t\t\t+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");',
            "\t\t}",
            "\t\treturn provided;",
        ]
    lines.append("\t}")
    return lines


def transform(path, apply):
    p = Path(path)
    cls = p.stem
    text = p.read_text(encoding="utf-8")
    lines = text.splitlines()

    decl = next(i for i, l in enumerate(lines) if "getInstance()" in l and "static" in l and "{" in l)
    signature = lines[decl].rstrip()
    assert signature.endswith("{"), f"{path}: unexpected getInstance declaration"
    signature = signature[:-1].rstrip()

    j = decl - 1
    while lines[j].strip() == "":
        j -= 1
    assert lines[j].strip() == "*/", f"{path}: no javadoc above getInstance"
    doc_end = j
    k = j
    while lines[k].strip() != "/**":
        k -= 1
    doc_start = k

    balance = 0
    i = decl
    while True:
        balance += lines[i].count("{") - lines[i].count("}")
        if balance == 0:
            method_end = i
            break
        i += 1

    cached = "resolvedInstance" in text
    new_block = NEW_DOC.splitlines() + new_method_lines(signature, cls, cached)

    h = next(i for i, l in enumerate(lines) if "class SingletonHolder" in l)
    hs = h
    while hs - 1 >= 0 and (lines[hs - 1].lstrip().startswith(("/**", "*", "*/", "@"))
                           or lines[hs - 1].strip() == ""):
        hs -= 1
    balance = 0
    i = h
    while True:
        balance += lines[i].count("{") - lines[i].count("}")
        if balance == 0:
            holder_end = i
            break
        i += 1

    if holder_end > method_end:
        out = lines[:doc_start] + new_block + lines[method_end + 1:hs] + lines[holder_end + 1:]
    else:
        out = lines[:hs] + lines[holder_end + 1:doc_start] + new_block + lines[method_end + 1:]

    # tidy: collapse 3+ blank lines
    cleaned = []
    for l in out:
        if l.strip() == "" and cleaned[-2:] == ["", ""]:
            continue
        cleaned.append(l)
    result = "\n".join(cleaned) + "\n"

    if apply:
        p.write_text(result, encoding="utf-8")
        print(f"APPLIED {path}")
    else:
        print(f"DRY-RUN {path}: cached={cached} method_end={method_end + 1} holder={hs + 1}-{holder_end + 1}")


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if a != "--apply"]
    apply = "--apply" in sys.argv
    for a in args:
        transform(a, apply)
