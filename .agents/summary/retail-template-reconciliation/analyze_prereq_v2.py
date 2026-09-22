#!/usr/bin/env python3
"""PREREQ 批次复核（只读）：合并 prerequisites + start-conditions 的 AND 集合 A，
与真端 DNF 分支 B 比对，输出精确语义差异与建议动作。"""
from __future__ import annotations
import re, csv, sys
import xml.etree.ElementTree as ET
from pathlib import Path

RETAIL = Path("/Users/mc/IdeaProjects/58Server/Map/XML/quest.xml")
QUESTS = Path("/Users/mc/IdeaProjects/AionEmu-test/src/main/resources/aion/data/static_data/quest_definition/quests")
HERE = Path(__file__).resolve().parent

def retail_blocks() -> dict[int, dict]:
    text = RETAIL.read_text(encoding="utf-16", errors="replace")
    body = text[text.find("]>") + 2:]
    out = {}
    for m in re.finditer(r"<quest>(.*?)</quest>", body, re.S):
        block = m.group(1)
        idm = re.search(r"<id>(\d+)</id>", block)
        if not idm:
            continue
        fields = {}
        for key, value in re.findall(r"<([a-z_0-9]+)>([^<]*)</\1>", block):
            fields.setdefault(key, []).append(value.strip())
        out[int(idm.group(1))] = fields
    return out

def retail_conds(fields: dict) -> list[list[int]]:
    conds = []
    for k in ("finished_quest_cond1", "finished_quest_cond2", "finished_quest_cond3", "finished_quest_cond4"):
        for v in fields.get(k, []):
            ids = []
            for chunk in re.split(r"[,\s]+", v):
                chunk = chunk.split(":")[0]
                if re.fullmatch(r"[Qq]?\d+", chunk or ""):
                    n = int(re.sub(r"\D", "", chunk))
                    if n:
                        ids.append(n)
            if ids:
                conds.append(ids)
    return conds

def repo_form(path: Path) -> dict:
    root = ET.parse(path).getroot()
    pre = [int(q.get("id")) for q in root.findall("./metadata/prerequisites/quest") if (q.get("id") or "").isdigit()]
    sc = [(c.get("type"), int(c.get("quest-id"))) for c in root.findall("./metadata/start-conditions/condition")
          if (c.get("quest-id") or "").isdigit()]
    groups = [[(c.get("type"), int(c.get("quest-id"))) for c in g.findall("condition") if (c.get("quest-id") or "").isdigit()]
              for g in root.findall("./metadata/start-condition-groups/group")]
    return {"pre": pre, "sc": sc, "groups": groups}

def main() -> None:
    ids = [int(r.split("\t")[0]) for r in (HERE / "prereq-plan.tsv").read_text(encoding="utf-8").splitlines()[1:]]
    rb = retail_blocks()
    rows = []
    for qid in sorted(ids):
        f = rb.get(qid, {})
        conds = retail_conds(f)
        path = QUESTS / f"{qid}.xml"
        cur = repo_form(path)
        A = set(cur["pre"]) | {i for t, i in cur["sc"] if t == "finished"}
        nonfin = [(t, i) for t, i in cur["sc"] if t != "finished"]
        B = [set(b) for b in conds]
        union = set().union(*B) if B else set()
        extras = sorted(A - union)
        common = sorted(set.intersection(*B)) if B else []
        superset_of_all = all(b <= A for b in B) if B else False
        missing_from_A = sorted(union - A)
        cover = all(any(A <= b for b in B) for _ in (0,)) if B else False
        rows.append({
            "quest": qid, "name": (f.get("name") or [""])[0][:28],
            "race": (f.get("race") or [""])[0], "lvl": (f.get("min_level") or [""])[0],
            "A": ",".join(map(str, sorted(A))) or "-", "groups_cur": cur["groups"],
            "B": ";".join(",".join(map(str, b)) for b in conds),
            "common": ",".join(map(str, common)) or "-",
            "extras_not_in_retail": ",".join(map(str, extras)) or "-",
            "missing": ",".join(map(str, missing_from_A)) or "-",
            "A_implies_a_branch": cover,
            "A_subset_every_branch": all(A <= b for b in B) if B else False,
            "all_branches_implied_by_A": superset_of_all,
            "nonfinished_start_conditions": nonfin,
        })
    with (HERE / "prereq-review-v2.tsv").open("w", encoding="utf-8") as fh:
        cols = list(rows[0].keys())
        fh.write("\t".join(cols).rstrip() + "\n")
        for r in rows:
            fh.write("\t".join(str(r[c]) for c in cols).rstrip() + "\n")
    for r in rows:
        print(f"{r['quest']:<6} A=[{r['A']:<14}] B={r['B']:<22} common={r['common']:<12} extras={r['extras_not_in_retail']:<10} miss={r['missing']:<10} sub_all={r['A_subset_every_branch']} sup_all={r['all_branches_implied_by_A']} name={r['name']}")
    print("wrote", HERE / "prereq-review-v2.tsv")

if __name__ == "__main__":
    main()
