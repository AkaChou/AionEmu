#!/usr/bin/env python3
"""PREREQ 可达分支对账（只读）。

真端 finished_quest_condN 是 OR 分支，分支内逗号 = AND。
仓库既有策略（commit 043426b47）：不引用未移植任务（死分支），
因此只把「分支内所有任务都已移植」的分支纳入期望集合。
输出：期望集合未被仓库覆盖的任务（真缺陷）、仓库多出的引用（漂移）。
"""
from __future__ import annotations
import os
import re, csv
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

RETAIL = Path(f"{REPO.parent / '58Server'}/Map/XML/quest.xml")
QUESTS = Path("src/main/resources/aion/data/static_data/quest/definitions/quests")
HERE = Path(__file__).resolve().parent
ported = {int(p.stem) for p in QUESTS.glob("*.xml") if p.stem.isdigit()}

def retail_dnf() -> dict[int, list[list[int]]]:
    text = RETAIL.read_text(encoding="utf-16", errors="replace")
    body = text[text.find("]>") + 2:]
    out = {}
    for m in re.finditer(r"<quest>(.*?)</quest>", body, re.S):
        b = m.group(1)
        i = re.search(r"<id>(\d+)</id>", b)
        if not i:
            continue
        branches = []
        for _k, v in re.findall(r"<(finished_quest_cond\d)>([^<]*)</\1>", b):
            ids = []
            for chunk in re.split(r"[,\s]+", v.strip()):
                chunk = chunk.split(":")[0]
                if re.fullmatch(r"[Qq]?\d+", chunk or ""):
                    n = int(re.sub(r"\D", "", chunk))
                    if n:
                        ids.append(n)
            if ids:
                branches.append(ids)
        if branches:
            out[int(i.group(1))] = branches
    return out

def repo_form(path: Path):
    root = ET.parse(path).getroot()
    pre = [int(q.get("id")) for q in root.findall("./metadata/prerequisites/quest") if (q.get("id") or "").isdigit()]
    sc = [(c.get("type"), int(c.get("quest-id"))) for c in root.findall("./metadata/start-conditions/condition") if (c.get("quest-id") or "").isdigit()]
    groups = [[(c.get("type"), int(c.get("quest-id"))) for c in g.findall("condition") if (c.get("quest-id") or "").isdigit()]
              for g in root.findall("./metadata/start-condition-groups/group")]
    return pre, sc, groups

def flat(form):
    pre, sc, groups = form
    return {i for i in pre} | {i for t, i in sc if t.lower() == "finished"} | {i for g in groups for t, i in g if t.lower() == "finished"}

def main() -> None:
    dnf = retail_dnf()
    rows = []
    for path in sorted(QUESTS.glob("*.xml")):
        if not path.stem.isdigit():
            continue
        qid = int(path.stem)
        branches = dnf.get(qid)
        if not branches:
            continue
        reachable = [b for b in branches if all(i in ported for i in b)]
        unreachable = [b for b in branches if not all(i in ported for i in b)]
        expected = {i for b in reachable for i in b}
        our = flat(repo_form(path))
        missing = sorted(expected - our)
        extra = sorted(our - {i for b in branches for i in b})
        if missing or extra:
            rows.append({
                "quest": qid,
                "retail_dnf": ";".join(",".join(map(str, b)) for b in branches),
                "reachable": ";".join(",".join(map(str, b)) for b in reachable) or "-",
                "unreachable_branches": ";".join(",".join(map(str, b)) for b in unreachable) or "-",
                "ours": ",".join(map(str, sorted(our))) or "-",
                "missing": " ".join(map(str, missing)) or "-",
                "extra_drift": " ".join(map(str, extra)) or "-",
            })
    out = HERE / "prereq-reachable-audit.tsv"
    cols = list(rows[0].keys())
    with out.open("w", encoding="utf-8") as fh:
        fh.write("\t".join(cols).rstrip() + "\n")
        for r in rows:
            fh.write("\t".join(str(r[c]) for c in cols).rstrip() + "\n")
    for r in rows:
        print(f"{r['quest']:<6} missing=[{r['missing']:<12}] extra=[{r['extra_drift']:<8}] reachable={r['reachable']:<18} unreachable={r['unreachable_branches']}")
    print(f"rows={len(rows)} -> {out}")

if __name__ == "__main__":
    main()
