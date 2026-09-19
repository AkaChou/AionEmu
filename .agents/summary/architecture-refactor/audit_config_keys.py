#!/usr/bin/env python3
"""只读审计：@Property 键的重复、死字段与"文件里没有的键"。

Read-only audit of the @Property keys: duplicates, dead fields and keys that no properties file
defines.
"""
from __future__ import annotations

import collections
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "src/main/java"
CONFIG_DIR = ROOT / "src/main/resources/aion/config"

FIELD_RE = re.compile(
    r"@Property\s*\(([^)]*)\)\s*\n\s*(?:public|private|protected)?\s*(?:static\s+)?"
    r"([\w<>\[\],\.\s]+?)\s+(\w+)\s*(?:=|;)"
)
CLASS_RE = re.compile(r"^(?:public\s+)?(?:final\s+)?class\s+(\w+)", re.M)
REF_RE = re.compile(r"\b(\w+)\.([A-Z][A-Z0-9_]*)\b")


def main() -> None:
    files = sorted(SRC.rglob("*.java"))
    texts = {p: p.read_text(encoding="utf-8") for p in files}

    keymap: dict[str, list[tuple[str, str]]] = collections.defaultdict(list)
    owner_of: dict[tuple[str, str], Path] = {}
    for path, text in texts.items():
        owner = CLASS_RE.search(text)
        for match in FIELD_RE.finditer(text):
            key = re.search(r'key\s*=\s*"([^"]*)"', match.group(1))
            if not key:
                continue
            cls = owner.group(1) if owner else path.stem
            keymap[key.group(1)].append((cls, match.group(3)))
            owner_of[(cls, match.group(3))] = path

    # 引用索引：字段 -> 引用它的文件集合
    refs: dict[tuple[str, str], set[Path]] = collections.defaultdict(set)
    for path, text in texts.items():
        for cls, field in REF_RE.findall(text):
            if (cls, field) in owner_of:
                refs[(cls, field)].add(path)

    dups = {k: v for k, v in keymap.items() if len(v) > 1}
    print(f"@Property 键总数: {len(keymap)}；重复键: {len(dups)}")
    for key, users in sorted(dups.items()):
        print(f"  重复 {key} -> {users}")

    dead = [f"{cls}.{field}" for (cls, field), path in owner_of.items()
            if refs.get((cls, field), set()) - {path} == set()]
    print(f"\n本类外无引用（疑似死字段）: {len(dead)}")
    for name in sorted(dead):
        print("  ", name)

    file_keys: set[str] = set()
    for props in CONFIG_DIR.rglob("*.properties"):
        for line in props.read_text(encoding="utf-8", errors="ignore").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            file_keys.add(line.split("=", 1)[0].strip())
    missing = sorted(k for k in keymap if k not in file_keys)
    print(f"\n配置文件里没有对应行的键（仅用默认值）: {len(missing)}")
    for key in missing:
        print("  ", key, "->", keymap[key])

    out = ROOT / ".agents/summary/architecture-refactor/static_config_key_audit.md"
    with out.open("w", encoding="utf-8") as fh:
        fh.write("# @Property 键审计 / @Property key audit\n\n")
        fh.write("生成者: `.agents/summary/architecture-refactor/audit_config_keys.py`\n\n")
        fh.write(f"- 键总数: {len(keymap)}\n- 重复键: {len(dups)}\n")
        fh.write(f"- 本类外无引用（疑似死字段）: {len(dead)}\n")
        fh.write(f"- 配置文件里没有对应行的键: {len(missing)}\n\n")
        fh.write("## 重复键\n\n| 键 | 绑定位置 |\n|---|---|\n")
        for key, users in sorted(dups.items()):
            fh.write(f"| `{key}` | {', '.join(f'{c}.{f}' for c, f in users)} |\n")
        fh.write("\n## 疑似死字段\n\n")
        for name in sorted(dead):
            fh.write(f"- `{name}`\n")
        fh.write("\n## 仅使用默认值的键\n\n")
        for key in missing:
            fh.write(f"- `{key}` -> {keymap[key]}\n")
    print(f"\nwrote {out.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
