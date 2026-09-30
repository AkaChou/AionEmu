#!/usr/bin/env python3
"""批 7+8：retention 双副本 reason 前缀翻转 SEMANTIC_GAP:<code> → ADJUDICATED:<code>。
零 owner 变化、零 evidence 结构变化（evidence 列整体替换为裁定说明）。
失败即抛 AssertionError，双副本不一致也抛。"""
import os
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

ROOT = REPO
COPIES = [
    ROOT / "src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv",
    ROOT / "src/test/resources/quest/retail-xml-retention.tsv",
]
RENAME_FILES = [Path("/tmp/b7-renames.tsv"), Path("/tmp/b8-renames.tsv")]

renames = {}
for path in RENAME_FILES:
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line:
            continue
        qid, reason, evidence = line.split("\t")
        if qid in renames:
            raise AssertionError(f"duplicate rename for {qid}")
        renames[qid] = (reason, evidence)

applied = 0
for copy in COPIES:
    lines = copy.read_text().splitlines()
    out = []
    seen = set()
    for line in lines:
        if line.startswith("#") or not line.strip():
            out.append(line)
            continue
        parts = line.split("\t")
        if parts[0] in renames:
            reason, evidence = renames[parts[0]]
            if parts[3] != "SEMANTIC_GAP:" + reason.split(":", 1)[1]:
                raise AssertionError(
                    f"{copy.name}:{parts[0]} 旧码不符: {parts[3]} != SEMANTIC_GAP:{reason}")
            parts[3] = reason
            parts[4] = evidence
            seen.add(parts[0])
            applied += 1
        out.append("\t".join(parts))
    missing = set(renames) - seen
    if missing:
        raise AssertionError(f"{copy.name} 未命中: {sorted(missing)}")
    copy.write_text("\n".join(out) + "\n")

expected = len(renames) * len(COPIES)
assert applied == expected, f"applied={applied} != expected={expected}"
print(f"OK applied={applied} ({len(renames)} rows x {len(COPIES)} copies)")
