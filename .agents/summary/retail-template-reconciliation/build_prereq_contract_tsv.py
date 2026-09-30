#!/usr/bin/env python3
"""生成 PREREQ 真端合同基线：src/test/resources/quest/quest-prerequisite-retail-contract.tsv

数据源：${AION_RETAIL_ROOT:-$HOME/IdeaProjects/58Server}/Map/XML/quest.xml（真端 5.8 服务端任务表，UTF-16LE）。
列：quest_id <TAB> 分支列表（分支内逗号=AND，分号=OR）。
运行：python3 -B .agents/summary/retail-template-reconciliation/build_prereq_contract_tsv.py
"""
from __future__ import annotations
import os
import re
from pathlib import Path

RETAIL = Path(f"{os.environ.get('AION_RETAIL_ROOT', os.path.expanduser('~/IdeaProjects/58Server'))}/Map/XML/quest.xml")
OUT = Path("src/test/resources/quest/quest-prerequisite-retail-contract.tsv")


def main() -> int:
    text = RETAIL.read_text(encoding="utf-16", errors="replace")
    body = text[text.find("]>") + 2:]
    rows: list[tuple[int, str]] = []
    for m in re.finditer(r"<quest>(.*?)</quest>", body, re.S):
        block = m.group(1)
        idm = re.search(r"<id>(\d+)</id>", block)
        if not idm:
            continue
        branches = []
        for _key, value in re.findall(r"<(finished_quest_cond\d)>([^<]*)</\1>", block):
            ids = []
            for chunk in re.split(r"[,\s]+", value.strip()):
                chunk = chunk.split(":")[0]
                if re.fullmatch(r"[Qq]?\d+", chunk or ""):
                    n = int(re.sub(r"\D", "", chunk))
                    if n:
                        ids.append(n)
            if ids:
                branches.append(ids)
        if branches:
            rows.append((int(idm.group(1)), ";".join(",".join(map(str, b)) for b in branches)))
    rows.sort()
    with OUT.open("w", encoding="utf-8", newline="\n") as fh:
        fh.write("# Aion 5.8 真端 quest.xml finished_quest_condN 快照：分支内逗号=AND，分号=OR\n")
        fh.write("# 由 .agents/summary/retail-template-reconciliation/build_prereq_contract_tsv.py 生成\n")
        fh.write("quest_id\tfinished_quest_branches\n")
        for qid, branch in rows:
            fh.write(f"{qid}\t{branch}\n")
    print(f"rows={len(rows)} -> {OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
