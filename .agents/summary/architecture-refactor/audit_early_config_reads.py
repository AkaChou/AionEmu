#!/usr/bin/env python3
"""只读审计：Spring Bean 在"构造期"读取静态配置字段的位置。

背景：所有 Bean 都在 AionServiceLauncher(ApplicationRunner) 调用 Config.load() 之前完成构造。
因此 Bean 的构造器、字段初始化器、@PostConstruct 里读取 XxxConfig.FIELD，拿到的是加载前的
占位值（0 / null / 字段初始值），而不是配置文件里的值。

Read-only audit of static config reads that happen during bean construction. Every bean is built
before AionServiceLauncher (ApplicationRunner) runs Config.load(), so a constructor, field
initializer or @PostConstruct reading XxxConfig.FIELD observes the pre-load placeholder value
(0 / null) instead of the configured value.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "src/main/java"

BEAN_ANNOTATIONS = (
    "@Component", "@Service", "@Repository", "@Controller", "@RestController",
    "@Configuration", "@RestControllerAdvice", "@ControllerAdvice", "@Aspect",
)
PROPERTY_FIELD_RE = re.compile(r"@Property\s*\(")


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*[\s\S]*?\*/", lambda m: "\n" * m.group(0).count("\n"), text)
    return re.sub(r"//[^\n]*", "", text)


def config_classes(files: list[Path]) -> dict[str, Path]:
    found: dict[str, Path] = {}
    for path in files:
        text = path.read_text(encoding="utf-8")
        if not PROPERTY_FIELD_RE.search(text):
            continue
        for name in re.findall(r"^(?:public\s+)?(?:final\s+)?class\s+(\w+)", text, re.M):
            found.setdefault(name, path)
    return found


def member_ranges(text: str, class_name: str) -> list[tuple[str, int, int]]:
    """返回 (kind, start_line, end_line)，kind ∈ constructor / postconstruct / field / method。"""
    lines = text.split("\n")
    members: list[tuple[str, int, int]] = []
    depth = 0
    pending_postconstruct = False
    index = 0
    while index < len(lines):
        raw = lines[index]
        stripped = raw.strip()
        if depth == 1:
            if stripped.startswith("@PostConstruct"):
                pending_postconstruct = True
            ctor = re.match(rf"(?:public|private|protected)?\s*{class_name}\s*\(", stripped)
            method = re.match(r"(?:public|private|protected|static|final|synchronized|\s)*[\w<>\[\],\.\s]+\s+(\w+)\s*\($", stripped)
            field = (not stripped.startswith(("@", "//", "*", "}"))
                     and "(" not in stripped and "=" in stripped and stripped.endswith(";"))
            if ctor or method or field:
                # 找到成员体结束行
                depth_start = depth
                end = index
                inner = 0
                for probe in range(index, len(lines)):
                    inner += lines[probe].count("{") - lines[probe].count("}")
                    if lines[probe].rstrip().endswith(";"):
                        end = probe
                        break
                    if inner <= 0 and probe > index and "{" in "".join(lines[index:probe + 1]):
                        end = probe
                        break
                if ctor:
                    kind = "constructor"
                elif field:
                    kind = "field"
                elif pending_postconstruct:
                    kind = "postconstruct"
                else:
                    kind = "method"
                members.append((kind, index + 1, end + 1))
                pending_postconstruct = False
        depth += raw.count("{") - raw.count("}")
        index += 1
    return members


def main() -> None:
    files = sorted(SRC.rglob("*.java"))
    configs = config_classes(files)
    print(f"静态 @Property 配置类: {len(configs)}")

    findings: list[tuple[str, str, str, str, int, str]] = []
    for path in files:
        raw = path.read_text(encoding="utf-8")
        text = strip_comments(raw)
        bean = any(a in text for a in BEAN_ANNOTATIONS)
        if not bean:
            continue
        class_name = re.search(r"^(?:public\s+)?(?:final\s+)?class\s+(\w+)", text, re.M)
        if not class_name:
            continue
        cls = class_name.group(1)
        hits = []
        for line_no, line in enumerate(text.split("\n"), 1):
            for match in re.finditer(r"\b(\w+)\.([A-Z][A-Z0-9_]*)\b", line):
                if match.group(1) in configs:
                    hits.append((line_no, match.group(1), match.group(2), line.strip()))
        if not hits:
            continue
        ranges = member_ranges(text, cls)
        for line_no, cfg, field, snippet in hits:
            kind = "class-body"
            for member_kind, start, end in ranges:
                if start <= line_no <= end:
                    kind = member_kind
                    break
            findings.append((str(path.relative_to(ROOT)), cls, f"{cfg}.{field}", kind, line_no, snippet[:90]))

    early = [f for f in findings if f[3] in ("constructor", "field", "postconstruct", "class-body")]
    print(f"Bean 内静态配置读取点: {len(findings)}；其中构造期读取: {len(early)}")
    print()
    print("| 文件 | Bean | 读取 | 位置 | 行 |")
    print("|---|---|---|---|---|")
    for path, cls, read, kind, line_no, _ in early:
        print(f"| `{path.split('/')[-1]}` | `{cls}` | `{read}` | {kind} | {line_no} |")

    out = ROOT / ".agents/summary/architecture-refactor/early_config_reads_audit.md"
    with out.open("w", encoding="utf-8") as fh:
        fh.write("# Bean 构造期静态配置读取审计 / Early static config reads inside beans\n\n")
        fh.write("生成者: `.agents/summary/architecture-refactor/audit_early_config_reads.py`\n\n")
        fh.write("判定：Bean 的构造器 / 字段初始化器 / `@PostConstruct` 在 `Config.load()` 之前执行，\n")
        fh.write("此处读到的是加载前的占位值。\n\n")
        fh.write("| 文件 | Bean | 读取字段 | 位置 | 行号 | 代码 |\n|---|---|---|---|---|---|\n")
        for path, cls, read, kind, line_no, snippet in early:
            fh.write(f"| `{path}` | `{cls}` | `{read}` | {kind} | {line_no} | `{snippet}` |\n")
        fh.write("\n## 运行期读取（无需处理，仅登记）\n\n")
        fh.write("| 文件 | Bean | 读取字段 | 位置 | 行号 |\n|---|---|---|---|---|\n")
        for path, cls, read, kind, line_no, _ in findings:
            if kind not in ("constructor", "field", "postconstruct", "class-body"):
                fh.write(f"| `{path}` | `{cls}` | `{read}` | {kind} | {line_no} |\n")
    print(f"\nwrote {out.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
