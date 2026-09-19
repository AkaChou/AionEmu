#!/usr/bin/env python3
"""Retire the dual-source SingletonHolder fallback for every remaining class (v2).

Handles holder-name variants (SingletonHolder/NewSingletonHolder/qualified), resolvedInstance
caches and tab/space indentation. Usage: retire_all_fallbacks_v2.py [--apply] <file> ...
"""
import json
import re
import sys
from pathlib import Path

DOC = """{i}/**
{i} * 获取实例：必须由 Spring 提供（{{@link #setInstanceProvider(ObjectProvider)}}）。
{i} * Returns the instance, which must be supplied by Spring.
{i} *
{i} * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
{i} * The legacy static fallback is retired: a missing provider now fails fast instead of silently
{i} * creating a second instance outside the container.</p>
{i} *
{i} * @return 由 Spring 提供的实例 / the Spring-provided instance
{i} * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
{i} *         when no provider or bean is available
{i} */
"""


def transform(path, apply):
    p = Path(path)
    cls = p.stem
    text = p.read_text(encoding="utf-8")
    lines = text.splitlines()

    decl = next(i for i, l in enumerate(lines) if "getInstance()" in l and "static" in l and "{" in l)
    sig = lines[decl].rstrip()
    assert sig.endswith("{"), f"{path}: unexpected declaration"
    sig = sig[:-1].rstrip()
    indent = re.match(r"[ \t]*", lines[decl]).group(0)
    unit = "\t" if indent.startswith("\t") else "    "

    j = decl - 1
    while j >= 0 and lines[j].strip() == "":
        j -= 1
    if j >= 0 and lines[j].strip() == "*/":
        k = j
        while lines[k].strip() != "/**":
            k -= 1
        doc_start = k
    else:
        doc_start = decl  # 无 javadoc 时直接插入 / insert the new javadoc when absent

    bal = 0
    for i in range(decl, min(len(lines), decl + 40)):
        bal += lines[i].count("{") - lines[i].count("}")
        if bal == 0:
            method_end = i
            break

    body = "\n".join(lines[decl:method_end + 1])
    m = re.search(r"return (?:[\w$]+\.)?(\w+)\.(?:INSTANCE|instance|_instance);", body)
    assert m, f"{path}: holder name not found"
    holder = m.group(1)
    cached = "resolvedInstance" in text

    doc = DOC.format(i=indent).splitlines()
    if cached:
        method = [
            f"{indent}{sig.strip()} {{",
            f"{indent}{unit}{cls} resolved = resolvedInstance;",
            f"{indent}{unit}if (resolved != null) {{",
            f"{indent}{unit}{unit}return resolved;",
            f"{indent}{unit}}}",
            f"{indent}{unit}ObjectProvider<{cls}> provider = instanceProvider;",
            f"{indent}{unit}resolved = provider == null ? null : provider.getIfAvailable();",
            f"{indent}{unit}if (resolved == null) {{",
            f'{indent}{unit}{unit}throw new IllegalStateException("{cls} 未由 Spring 提供："',
            f'{indent}{unit}{unit}{unit}+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")',
            f'{indent}{unit}{unit}{unit}+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");',
            f"{indent}{unit}}}",
            f"{indent}{unit}resolvedInstance = resolved;",
            f"{indent}{unit}return resolved;",
            f"{indent}}}",
        ]
    else:
        method = [
            f"{indent}{sig.strip()} {{",
            f"{indent}{unit}ObjectProvider<{cls}> provider = instanceProvider;",
            f"{indent}{unit}{cls} provided = provider == null ? null : provider.getIfAvailable();",
            f"{indent}{unit}if (provided == null) {{",
            f'{indent}{unit}{unit}throw new IllegalStateException("{cls} 未由 Spring 提供："',
            f'{indent}{unit}{unit}{unit}+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")',
            f'{indent}{unit}{unit}{unit}+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");',
            f"{indent}{unit}}}",
            f"{indent}{unit}return provided;",
            f"{indent}}}",
        ]

    h = next(i for i, l in enumerate(lines) if f"class {holder}" in l)
    hs = h
    while hs - 1 >= 0 and (lines[hs - 1].strip() == "" or
                           lines[hs - 1].lstrip().startswith(("/**", "*", "*/", "@"))):
        hs -= 1
    bal = 0
    for i in range(h, min(len(lines), h + 30)):
        bal += lines[i].count("{") - lines[i].count("}")
        if bal == 0:
            holder_end = i
            break

    if holder_end > method_end:
        out = lines[:doc_start] + doc + method + lines[method_end + 1:hs] + lines[holder_end + 1:]
    else:
        out = lines[:hs] + lines[holder_end + 1:doc_start] + doc + method + lines[method_end + 1:]

    cleaned = []
    for l in out:
        if l.strip() == "" and cleaned[-2:] == ["", ""]:
            continue
        cleaned.append(l)
    result = "\n".join(cleaned).rstrip("\n") + "\n"
    if apply:
        p.write_text(result, encoding="utf-8")
        print(f"APPLIED {path} (holder={holder} cached={cached})")
    else:
        print(f"DRY {path} holder={holder} cached={cached} indent={'tab' if unit == chr(9) else 'spaces'}")


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if a != "--apply"]
    apply = "--apply" in sys.argv
    for a in args:
        transform(a, apply)
