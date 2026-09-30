#!/usr/bin/env python3
"""Phase 3 只读勘测：22 张注册 TSV 的读取点普查。
对每张表产出：直接路径引用（src 全树）、清单 note 点名的消费者类、
消费者类在 src/main 的调用点、测试侧引用。只读，不改任何文件。
Read-only Phase 3 census: for each registered TSV, find direct path references,
the manifest-named consumer classes, their src/main call sites, and test-side references."""
import os
import re
import subprocess
from pathlib import Path

ROOT = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
OUT = ROOT / ".agents/summary/quest-native-dispatch/phase3-recon/census-readers.tsv"

MANIFEST = ROOT / "src/main/resources/aion/data/static_data/quest_retail/quest-retail-tsv-manifest.tsv"
rows = []
for line in MANIFEST.read_text().splitlines():
    if line.startswith("#") or not line.strip() or line.startswith("file\t"):
        continue
    parts = line.split("\t")
    rows.append((parts[0], parts[1], parts[2]))

# 消费者类名词表：从清单 note 里抽 CamelCase 词（≥2 个大写字母视为类名候选）
def classes_in_note(note: str) -> list[str]:
    names = re.findall(r"\b[A-Z][a-zA-Z0-9]+\b", note)
    seen, out = set(), []
    for n in names:
        if sum(1 for c in n if c.isupper()) >= 2 and n not in seen:
            seen.add(n)
            out.append(n)
    return out

def grep(pattern: str, pathspec: list[str]) -> list[str]:
    cmd = ["grep", "-rln", "--include=*.java", "-e", pattern, *pathspec]
    proc = subprocess.run(cmd, capture_output=True, text=True, cwd=ROOT)
    return sorted(proc.stdout.splitlines())

header = ["tsv", "role", "status", "direct_refs_main", "direct_refs_test",
          "note_classes", "class_refs_main", "class_refs_test"]
lines = ["\t".join(header)]
for tsv, role, status in rows:
    direct_main = grep(tsv, ["src/main"])
    direct_test = grep(tsv, ["src/test"])
    note = next((l.split("\t")[3] for l in MANIFEST.read_text().splitlines()
                 if l.startswith(tsv + "\t")), "")
    classes = classes_in_note(note)
    cls_main, cls_test = [], []
    for cls in classes:
        hits_m = grep(r"\b" + cls + r"\b", ["src/main"])
        hits_t = grep(r"\b" + cls + r"\b", ["src/test"])
        if hits_m:
            cls_main.append(cls)
        if hits_t:
            cls_test.append(cls)
    lines.append("\t".join([tsv, role, status,
                            ";".join(Path(p).name for p in direct_main),
                            ";".join(Path(p).name for p in direct_test),
                            ";".join(classes), ";".join(cls_main), ";".join(cls_test)]))
OUT.write_text("\n".join(lines) + "\n")
print(f"wrote {OUT} ({len(rows)} tsvs)")
