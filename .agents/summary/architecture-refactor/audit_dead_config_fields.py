#!/usr/bin/env python3
"""只读审计：@Property 字段的引用范围（外部引用 / 仅类内引用 / 无引用）。

Read-only audit of @Property field reference scope: external references, in-class-only usage, or
no usage at all.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "src/main/java"

FIELD_RE = re.compile(
    r"@Property\s*\([^)]*\)\s*\n\s*(?:public|private|protected)?\s*(?:static\s+)?"
    r"[\w<>\[\],\.\s]+?\s+(\w+)\s*(?:=|;)"
)
CLASS_RE = re.compile(r"^(?:public\s+)?(?:final\s+)?class\s+(\w+)", re.M)
REF_RE = re.compile(r"\b(\w+)\.([A-Za-z_]\w*)\b")
BARE_RE_TEMPLATE = r"\b{}\b"


def main() -> None:
    texts = {p: p.read_text(encoding="utf-8") for p in sorted(SRC.rglob("*.java"))}
    owners: dict[tuple[str, str], Path] = {}
    for path, text in texts.items():
        owner = CLASS_RE.search(text)
        if not owner or not re.search(r"@Property\s*\(", text):
            continue
        for match in FIELD_RE.finditer(text):
            owners[(owner.group(1), match.group(1))] = path

    external: dict[tuple[str, str], set[str]] = {}
    for path, text in texts.items():
        for cls, field in REF_RE.findall(text):
            key = (cls, field)
            if key in owners and owners[key] != path:
                external.setdefault(key, set()).add(path.name)

    internal_only, unused = [], []
    for (cls, field), path in sorted(owners.items()):
        if external.get((cls, field)):
            continue
        body = texts[path]
        declaration = re.search(rf"@Property\s*\([^)]*\)\s*\n\s*.*?\b{field}\b", body)
        stripped = body
        if declaration:
            stripped = body[:declaration.start()] + body[declaration.end():]
        if re.search(BARE_RE_TEMPLATE.format(field), stripped):
            internal_only.append(f"{cls}.{field}")
        else:
            unused.append(f"{cls}.{field}")

    print(f"@Property 字段总数: {len(owners)}")
    print(f"类外无引用、但类内使用: {len(internal_only)}")
    for f in internal_only:
        print("   内用", f)
    print(f"\n类外无引用且类内也无引用（死字段）: {len(unused)}")
    for f in unused:
        print("   死", f)

    out = ROOT / ".agents/summary/architecture-refactor/static_config_field_scope_audit.md"
    with out.open("w", encoding="utf-8") as fh:
        fh.write("# @Property 字段引用范围审计 / @Property field reference scope audit\n\n")
        fh.write("生成者: `.agents/summary/architecture-refactor/audit_dead_config_fields.py`\n\n")
        fh.write(f"- 字段总数: {len(owners)}\n")
        fh.write(f"- 类外无引用、类内使用: {len(internal_only)}\n")
        fh.write(f"- 类外无引用且类内也无引用: {len(unused)}\n\n")
        fh.write("## 仅类内使用\n\n")
        for f in internal_only:
            fh.write(f"- `{f}`\n")
        fh.write("\n## 完全无引用（死字段）\n\n")
        for f in unused:
            fh.write(f"- `{f}`\n")
    print(f"\nwrote {out.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
