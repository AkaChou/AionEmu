#!/usr/bin/env python3
"""扫描测试源码中「仍按 typed 目录取定义」的引用，标出已退役（RETAIL_TABLE）任务。

背景：2026-09-27（4ede058c0）与 2026-10-03（b22e1e971）两批退役把大量任务从 typed XML 目录
挪到真端/native 车道；任何仍用 ProductionQuestDefinitions.definition(<id>) 或
catalog().findExecutable(<id>)（正期望）引用退役任务的测试都会以
「missing production quest definition <id>」或空 Optional 失败。

本脚本只做静态定位，不替代实际运行；命中项需逐个按 P3 重锚口径人工裁定。
在仓库根目录运行：
  python3 .agents/summary/quest-retired-test-audit/audit_retired_test_refs.py
"""
from __future__ import annotations

import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
RETENTION = REPO / "src/main/resources/aion/data/static_data/quest/retail/retail-xml-retention.xml"
TEST_ROOT = REPO / "src/test/java"

ROW = re.compile(r"<quest>\s*<quest_id>(\d+)</quest_id>\s*<owner>([A-Z_]+)</owner>", re.S)
CONST = re.compile(r"\b([A-Z0-9_]*QUEST[A-Z0-9_]*)\s*=\s*(\d+)\s*;")
DEF_LITERAL = re.compile(r"ProductionQuestDefinitions\.definition\((\w+)\)")
FIND_EXEC_LITERAL = re.compile(r"findExecutable\((\w+)\)(.{0,60})", re.S)


def main() -> int:
    retention = RETENTION.read_text(encoding="utf-8")
    owners = {int(q): o for q, o in ROW.findall(retention)}
    retired = {q for q, o in owners.items() if o == "RETAIL_TABLE"}

    hits: list[str] = []
    for path in sorted(TEST_ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        if "ProductionQuestDefinitions" not in text:
            continue
        consts = {m.group(1): int(m.group(2)) for m in CONST.finditer(text)}
        for match in DEF_LITERAL.finditer(text):
            arg = match.group(1)
            quest_id = int(arg) if arg.isdigit() else consts.get(arg)
            if quest_id in retired:
                line = text[: match.start()].count("\n") + 1
                hits.append(f"{path.relative_to(REPO)}:{line} definition({arg}={quest_id}) 已退役")
        for match in FIND_EXEC_LITERAL.finditer(text):
            arg, tail = match.group(1), match.group(2)
            quest_id = int(arg) if arg.isdigit() else consts.get(arg)
            negative = "isPresent()" not in tail or "assertFalse" not in text[
                max(0, match.start() - 200): match.start()].split("\n")[-1]
            if quest_id in retired and not negative:
                line = text[: match.start()].count("\n") + 1
                hits.append(f"{path.relative_to(REPO)}:{line} findExecutable({arg}={quest_id}) 正期望（需人工核对）")

    print(f"retired={len(retired)} xml_retention={sum(1 for o in owners.values() if o == 'XML_RETENTION')}")
    if hits:
        print("SUSPECT_REFERENCES")
        for hit in hits:
            print("-", hit)
    else:
        print("NO_SUSPECT_REFERENCES")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
