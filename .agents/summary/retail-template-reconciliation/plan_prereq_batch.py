#!/usr/bin/env python3
"""PREREQ 批次计划：真端 finished_quest_condN -> 本仓库前置写法（只读，产出计划 TSV）。

规则（QE-021 + 仓库既有约定）:
  - 每个 finished_quest_condN 是 OR 分支；
  - 同一 cond 字段内逗号分隔的多个任务 = 该分支内 AND；
  - 单 cond（可含多 id）-> <metadata><prerequisites><quest id=.../></prerequisites>
  - 多 cond -> <metadata><start-condition-groups><group><condition type="finished" quest-id=.../>...</group>...
"""
from __future__ import annotations
import os

import csv
import re
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

RETAIL_QUEST_XML = Path(f"{REPO.parent / '58Server'}/Map/XML/quest.xml")
QUESTS = Path(f"{REPO}/src/main/resources/aion/data/static_data/quest_definition/quests")
HERE = Path(__file__).resolve().parent


def parse_retail_conds() -> dict[int, list[list[int]]]:
    text = RETAIL_QUEST_XML.read_text(encoding="utf-16", errors="replace")
    body = text[text.find("]>") + 2:]
    out: dict[int, list[list[int]]] = {}
    for m in re.finditer(r"<quest>(.*?)</quest>", body, re.S):
        block = m.group(1)
        idm = re.search(r"<id>(\d+)</id>", block)
        if not idm:
            continue
        conds: list[list[int]] = []
        for _key, value in re.findall(r"<(finished_quest_cond\d)>([^<]*)</\1>", block):
            ids = []
            for chunk in re.split(r"[,\s]+", value.strip()):
                chunk = chunk.split(":")[0]
                if re.fullmatch(r"[Qq]?\d+", chunk or "") and int(re.sub(r"\D", "", chunk)) != 0:
                    ids.append(int(re.sub(r"\D", "", chunk)))
            if ids:
                conds.append(ids)
        if conds:
            out[int(idm.group(1))] = conds
    return out


def current_form(path: Path) -> dict:
    root = ET.parse(path).getroot()
    pre = [int(q.get("id")) for q in root.findall("./metadata/prerequisites/quest") if (q.get("id") or "").isdigit()]
    groups = []
    for g in root.findall("./metadata/start-condition-groups/group"):
        fin = [int(c.get("quest-id")) for c in g.findall("condition")
               if (c.get("type") or "") == "finished" and (c.get("quest-id") or "").isdigit()]
        groups.append(fin)
    return {"prerequisites": pre, "groups": groups}


def build_snippet(conds: list[list[int]]) -> str:
    if len(conds) == 1:
        lines = ["    <prerequisites>"]
        lines += [f'      <quest id="{i}"/>' for i in conds[0]]
        lines.append("    </prerequisites>")
        return "\n".join(lines)
    lines = ["    <start-condition-groups>"]
    for ids in conds:
        lines.append("      <group>")
        lines += [f'        <condition type="finished" quest-id="{i}"/>' for i in ids]
        lines.append("      </group>")
    lines.append("    </start-condition-groups>")
    return "\n".join(lines)


def main() -> int:
    retail = parse_retail_conds()
    diff = [int(r["quest"]) for r in csv.DictReader((HERE / "prereq-mismatch.tsv").open(encoding="utf-8"), delimiter="\t")]
    rows = []
    for qid in sorted(diff):
        conds = retail.get(qid)
        path = QUESTS / f"{qid}.xml"
        if conds is None or not path.exists():
            rows.append({"quest": qid, "action": "NO_EVIDENCE_OR_FILE"})
            continue
        cur = current_form(path)
        flat_retail = {i for ids in conds for i in ids}
        flat_ours = set(cur["prerequisites"]) | {i for g in cur["groups"] for i in g}
        missing = sorted(flat_retail - flat_ours)
        extra = sorted(flat_ours - flat_retail)
        if not missing:
            action = "ALREADY_SATISFIED"
        elif cur["prerequisites"] or cur["groups"]:
            action = "CONFLICT_MERGE_MANUAL"
        elif len(conds) == 1:
            action = "ADD_PREREQUISITES"
        else:
            action = "ADD_START_CONDITION_GROUPS"
        rows.append({
            "quest": qid,
            "conds": ";".join(",".join(str(i) for i in ids) for ids in conds),
            "action": action,
            "missing": " ".join(str(i) for i in missing),
            "extra": " ".join(str(i) for i in extra),
            "current": f"prereq={cur['prerequisites']} groups={cur['groups']}",
            "proposed": build_snippet(conds).replace("\n", " | "),
        })
    out = HERE / "prereq-plan.tsv"
    cols = ["quest", "conds", "action", "missing", "extra", "current", "proposed"]
    with out.open("w", encoding="utf-8") as fh:
        fh.write("\t".join(cols).rstrip() + "\n")
        for r in rows:
            fh.write("\t".join(str(r.get(c, "")) for c in cols).rstrip() + "\n")
    summary: dict[str, int] = {}
    for r in rows:
        summary[r["action"]] = summary.get(r["action"], 0) + 1
    print("plan rows:", len(rows), summary)
    print("plan:", out)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
