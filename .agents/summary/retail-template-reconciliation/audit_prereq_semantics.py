#!/usr/bin/env python3
"""PREREQ 语义等价审计（只读）。

R = 真端 DNF: OR over branches(AND ids)
O = 仓库: (AND prerequisites) AND (OR over groups(AND conditions))
未移植任务固定为 false（仓库既有策略：不引用未移植任务）。
真值表枚举判定 R=>O（仓库是否更严/漏前置）与 O=>R（仓库是否更松/缺前置）。
"""
from __future__ import annotations
import os
import re, sys, itertools
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

RETAIL = Path(f"{REPO.parent / '58Server'}/Map/XML/quest.xml")
QUESTS = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
HERE = Path(__file__).resolve().parent
ported = {int(p.stem) for p in QUESTS.glob("*.xml") if p.stem.isdigit()}

def retail_dnf():
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
    sc = [(c.get("type").lower(), int(c.get("quest-id"))) for c in root.findall("./metadata/start-conditions/condition") if (c.get("quest-id") or "").isdigit()]
    groups = [[(c.get("type").lower(), int(c.get("quest-id"))) for c in g.findall("condition") if (c.get("quest-id") or "").isdigit()]
              for g in root.findall("./metadata/start-condition-groups/group")]
    pre_sc = [(t, i) for t, i in sc if t == "finished" and i in pre]
    extra_sc = [(t, i) for t, i in sc if not (t == "finished" and i in pre)]
    return pre, extra_sc, groups

def lit(cond_type, qid, assign):
    v = assign[qid]
    if cond_type == "finished":
        return v
    if cond_type in ("unfinished", "noacquired"):
        return not v
    if cond_type == "acquired":
        return v          # 近似：acquired 视为已接取，按“已开始”真值
    return None           # equipped 等无法在任务图内判定

def evaluate(pre, sc, groups, assign):
    for i in pre:
        if not assign[i]:
            return False
    for t, i in sc:
        r = lit(t, i, assign)
        if r is None:
            return None
        if not r:
            return False
    if groups:
        ok = False
        for g in groups:
            good = True
            for t, i in g:
                r = lit(t, i, assign)
                if r is None:
                    return None
                if not r:
                    good = False
                    break
            if good:
                ok = True
                break
        if not ok:
            return False
    return True

def main():
    dnf = retail_dnf()
    stricter, looser, unknown, ok = [], [], [], 0
    for path in sorted(QUESTS.glob("*.xml")):
        if not path.stem.isdigit():
            continue
        qid = int(path.stem)
        branches = dnf.get(qid)
        if not branches:
            continue
        pre, sc, groups = repo_form(path)
        vars_ = sorted({i for b in branches for i in b} | set(pre) | {i for _, i in sc} | {i for g in groups for _, i in g})
        if len(vars_) > 12:
            unknown.append((qid, f"too many vars {len(vars_)}"))
            continue
        disp = []
        for values in itertools.product([False, True], repeat=len(vars_)):
            assign = dict(zip(vars_, values))
            for v in vars_:
                if v not in ported:
                    assign[v] = False     # 未移植任务：不可完成
            r = any(all(assign[i] for i in b) for b in branches)
            o = evaluate(pre, sc, groups, assign)
            if o is None:
                disp = None
                break
            if r and not o:
                disp.append(("REPO_STRICTER", dict(assign)))
            elif o and not r:
                disp.append(("REPO_LOOSER", dict(assign)))
        if disp is None:
            unknown.append((qid, "unstatted condition type"))
            continue
        if not disp:
            ok += 1
            continue
        kinds = {d[0] for d in disp}
        sample = disp[0][1]
        row = {
            "quest": qid,
            "kind": "+".join(sorted(kinds)),
            "retail_dnf": ";".join(",".join(map(str, b)) for b in branches),
            "ours": f"pre={pre} sc={sc} groups={groups}",
            "witness": " ".join(f"{k}={'T' if v else 'F'}" for k, v in sorted(sample.items())),
            "n_witness": len(disp),
        }
        (stricter if "REPO_STRICTER" in kinds else looser).append(row)
    out = HERE / "prereq-semantic-audit.tsv"
    rows = stricter + looser
    with out.open("w", encoding="utf-8") as fh:
        cols = ["quest", "kind", "retail_dnf", "ours", "witness", "n_witness"]
        fh.write("\t".join(cols).rstrip() + "\n")
        for r in rows:
            fh.write("\t".join(str(r[c]) for c in cols).rstrip() + "\n")
    print(f"equivalent={ok} stricter={len(stricter)} looser={len(looser)} unknown={len(unknown)}")
    for r in stricter:
        print(f"  STRICTER {r['quest']:<6} retail={r['retail_dnf']:<24} ours={r['ours']}")
    for r in looser:
        print(f"  LOOSER   {r['quest']:<6} retail={r['retail_dnf']:<24} ours={r['ours']}")
    for q, why in unknown:
        print(f"  UNKNOWN  {q}: {why}")
    print("->", out)

if __name__ == "__main__":
    main()
