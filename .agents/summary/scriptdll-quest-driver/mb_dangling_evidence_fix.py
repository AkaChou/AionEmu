#!/usr/bin/env python3
"""Repair memory-bank evidence references that point at retired (deleted) quest XML.

背景 / Why: 退役 = 删除（内容留在 git 历史），但历史卡片的 evidence 字段仍写着已删 XML 的路径，
结构门禁因此长期报 red（P0c-35 观察 52 处 / 18 张卡）。修法：把该路径换成"已退役"指针，
并保留可解析的持久证据（retention 清单 + 报告）。

用法 / Usage: python3 -B mb_dangling_evidence_fix.py [--apply]
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MB = ROOT / ".agents" / "memory-bank"
VERIFY = Path("/tmp/mb-verify.txt")
RETENTION = MB / ".." / ".." / "src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.tsv"

LINE = re.compile(r"^(?:- )?Pattern (?P<pid>QE-\d+) (?P<field>\w+) references missing path (?P<path>\S+)$")


def retention_owners() -> dict[int, str]:
    owners: dict[int, str] = {}
    for line in RETENTION.resolve().read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        parts = line.split("\t")
        if len(parts) >= 2 and parts[0].isdigit():
            owners[int(parts[0])] = parts[1]
    return owners


def main() -> int:
    apply = "--apply" in sys.argv
    apply_flag = apply
    owners = retention_owners()
    card_of: dict[str, str] = {}
    for row in (MB / "index.jsonl").read_text(encoding="utf-8").splitlines():
        if not row.strip():
            continue
        j = json.loads(row)
        if j.get("record") != "pattern":
            continue
        card_of[j["pattern_id"]] = j["card"]

    targets: dict[tuple[str, str], list[str]] = {}
    unexpected: list[str] = []
    for raw in VERIFY.read_text(encoding="utf-8").splitlines():
        m = LINE.match(raw.strip())
        if not m:
            continue
        pid, field, path = m.group("pid"), m.group("field"), m.group("path")
        qid = int(re.search(r"(\d+)\.xml$", path).group(1))
        owner = owners.get(qid)
        if owner != "RETAIL_TABLE":
            unexpected.append("%s %s %s（retention owner=%s）" % (pid, field, path, owner))
            continue
        targets.setdefault((pid, card_of[pid]), []).append(path)

    if unexpected:
        print("UNEXPECTED（非 RETAIL_TABLE 退役行，保持原样并上报）:")
        for u in unexpected:
            print("  " + u)

    total = 0
    for (pid, card), paths in sorted(targets.items()):
        text = (ROOT / card).read_text(encoding="utf-8")
        changed = text
        for path in sorted(set(paths), key=len, reverse=True):
            qid = int(re.search(r"(\d+)\.xml$", path).group(1))
            repl = ("retail-xml-retention.tsv 的 quest %d 行"
                    "（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）" % qid)
            n = changed.count(path)
            total += n
            changed = changed.replace(path, repl)
            print("%-8s %-28s %s×%d" % (pid, path, "REPLACE" if apply_flag else "DRY", n))
        if not apply_flag or changed == text:
            continue
        # 与并发 lane 的追加写竞态防护：写入前确认文件未被改动，否则重读重做。
        # Race guard: re-read right before writing; retry if a concurrent lane appended.
        for attempt in range(4):
            now = (ROOT / card).read_text(encoding="utf-8")
            if now == text:
                (ROOT / card).write_text(changed, encoding="utf-8")
                break
            text = now
            changed = text
            for path in sorted(set(paths), key=len, reverse=True):
                qid = int(re.search(r"(\d+)\.xml$", path).group(1))
                changed = changed.replace(path, ("retail-xml-retention.tsv 的 quest %d 行"
                    "（XML 已退役并删除，内容见 git 历史；owner 记录见该清单）" % qid))
        else:
            print("RACE-RETRY-EXHAUSTED %s %s" % (pid, card))
    print("TOTAL %d replacements over %d cards; mode=%s" % (total, len(targets), "APPLY" if apply else "DRY"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
