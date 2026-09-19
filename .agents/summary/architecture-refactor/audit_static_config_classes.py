#!/usr/bin/env python3
"""只读审计：静态 @Property 配置类的消费方构成。

输出每个配置类的 (字段数 / 引用类数 / 引用方中 Spring Bean 数 / 是否自身是 Bean)，
用来判断"哪些类真的有实例绑定需求"，而不是按行数机械迁移。

Read-only audit of the static @Property config classes: how many classes consume each one and how
many of those consumers are Spring beans. Used to decide whether a class genuinely needs instance
binding instead of migrating by line count.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "src/main/java"

BEAN_ANNOTATIONS = (
    "@Component", "@Service", "@Repository", "@Controller", "@RestController",
    "@Configuration", "@RestControllerAdvice", "@ControllerAdvice", "@Aspect",
    "@ConfigurationProperties",
)

PROPERTY_RE = re.compile(r"@Property\b")


def java_files() -> list[Path]:
    return sorted(SRC.rglob("*.java"))


def package_of(text: str) -> str:
    m = re.search(r"^package\s+([\w.]+);", text, re.M)
    return m.group(1) if m else ""


def collect_config_classes(files: list[Path]) -> dict[str, dict]:
    result: dict[str, dict] = {}
    for path in files:
        text = path.read_text(encoding="utf-8")
        if not PROPERTY_RE.search(text):
            continue
        cls = re.search(r"^(?:public\s+)?(?:final\s+)?class\s+(\w+)", text, re.M)
        if not cls:
            continue
        name = cls.group(1)
        pkg = package_of(text)
        result[name] = {
            "name": name,
            "path": str(path.relative_to(ROOT)),
            "package": pkg,
            "lines": text.count("\n") + 1,
            "properties": len(PROPERTY_RE.findall(text)),
            "static_fields": len(re.findall(r"@Property\b[\s\S]{0,400}?\n\s+(?:public|private|protected)?\s*static\b", text)),
            "is_bean": any(a in text for a in BEAN_ANNOTATIONS),
            "has_instance_field": bool(re.search(r"^\s+private\s+(?!static)(?!final\s+static)\w", text, re.M)),
            "refs": [],
        }
    return result


def main() -> None:
    files = java_files()
    configs = collect_config_classes(files)
    texts: dict[Path, str] = {}

    for path in files:
        text = path.read_text(encoding="utf-8")
        texts[path] = text
        for name, info in configs.items():
            if path.name == f"{name}.java" and str(path.relative_to(ROOT)) == info["path"]:
                continue
            if not re.search(rf"\b{name}\b", text):
                continue
            consumer_is_bean = any(a in text for a in BEAN_ANNOTATIONS)
            static_reads = len(re.findall(rf"\b{name}\.\w", text))
            info["refs"].append((str(path.relative_to(ROOT)), consumer_is_bean, static_reads))

    rows = []
    for info in configs.values():
        bean_refs = sum(1 for _, is_bean, _ in info["refs"] if is_bean)
        rows.append({**info, "bean_refs": bean_refs, "ref_count": len(info["refs"])})

    rows.sort(key=lambda r: (-r["bean_refs"], -r["lines"]))
    print("| # | 配置类 | 行数 | @Property 数 | 引用类数 | 其中 Bean | 自身是 Bean |")
    print("|---|---|---|---|---|---|---|")
    for idx, row in enumerate(rows, 1):
        print(f"| {idx} | `{row['name']}` | {row['lines']} | {row['properties']} | "
              f"{row['ref_count']} | {row['bean_refs']} | {'是' if row['is_bean'] else '否'} |")

    total = len(rows)
    bean_consumed = [r for r in rows if r["bean_refs"] > 0]
    pure_static = [r for r in rows if r["bean_refs"] == 0]
    print()
    print(f"配置类总数: {total}")
    print(f"存在 Bean 消费方: {len(bean_consumed)} -> {', '.join(r['name'] for r in bean_consumed)}")
    print(f"仅被非 Bean 消费: {len(pure_static)}")
    print(f"无任何引用: {len([r for r in rows if r['ref_count'] == 0])}")

    out = ROOT / ".agents/summary/architecture-refactor/static_config_audit.md"
    with out.open("w", encoding="utf-8") as fh:
        fh.write("# 静态 @Property 配置类审计 / Static @Property config class audit\n\n")
        fh.write("生成者: `.agents/summary/architecture-refactor/audit_static_config_classes.py`\n\n")
        fh.write("| 配置类 | 行数 | @Property 数 | 引用类数 | 其中 Bean 消费方 | 自身是 Bean | 消费方 |\n")
        fh.write("|---|---|---|---|---|---|---|\n")
        for row in sorted(rows, key=lambda r: r["name"]):
            consumers = ", ".join(
                f"`{p.split('/')[-1][:-5]}`{'*' if is_bean else ''}"
                for p, is_bean, _ in sorted(row["refs"])
            ) or "—"
            fh.write(f"| `{row['name']}` | {row['lines']} | {row['properties']} | {row['ref_count']} | "
                     f"{row['bean_refs']} | {'是' if row['is_bean'] else '否'} | {consumers} |\n")
        fh.write("\n`*` 表示消费方是 Spring Bean。\n")
    print(f"\nwrote {out.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
