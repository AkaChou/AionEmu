#!/usr/bin/env python3
"""只读前置审计，保留分支和结局；差异不等于缺陷。
Read-only prerequisite audit preserving branches and outcomes; differences are not defects.
"""
import argparse
import csv
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
KINDS = ("finished", "unfinished", "noacquired", "acquired")


def canonical(groups):
    groups = {frozenset(group) for group in groups}
    return sorted(sorted(group) for group in groups if not any(other < group for other in groups))


def retail_contract(quest, names):
    finished, common, unresolved = [], [], []
    for child in quest:
        match = re.fullmatch(r"(finished|unfinished|noacquired|acquired)_quest_cond\d+", child.tag)
        if not match or not (child.text or "").strip():
            continue
        kind = match[1]
        group = []
        for token in child.text.split(","):
            parts = token.strip().split(":")
            qid = names.get(parts[0].lower())
            if qid is None:
                unresolved.append(token.strip())
                continue
            # 客户端结局从 1 起，服务端 reward-mode 从 0 起。
            # Client outcome indices are one-based; server reward modes are zero-based.
            mode = int(parts[1]) - 1 if len(parts) == 2 else 0
            group.append((kind, qid, mode))
        if kind == "finished":
            finished.append(group)
        else:
            common.extend(group)
    return canonical([group + common for group in (finished or [[]])]), unresolved


def production_contract(meta):
    common = [("finished", int(q.attrib["id"]), 0) for q in meta.findall("./prerequisites/quest")]
    groups = meta.findall("./start-condition-groups/group")
    if not groups:
        shorthand = meta.find("start-conditions")
        groups = [shorthand] if shorthand is not None else []
    return canonical([common + [(c.attrib["type"], int(c.attrib["quest-id"]),
                               int(c.get("reward-mode", "0")))
                              for c in group if c.get("type") in KINDS]
                      for group in groups] or [common])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--retail", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path(__file__).with_name("prerequisite-axis-audit.tsv"))
    args = parser.parse_args()
    retail = {}
    for _, quest in ET.iterparse(args.retail, events=("end",)):
        if quest.tag == "quest":
            retail[int(quest.findtext("id"))] = quest
    names = {q.findtext("name").lower(): qid for qid, q in retail.items()}
    names.update({f"q{qid}": qid for qid in retail})
    prod = {int(p.stem): ET.parse(p).getroot().find("metadata")
            for p in (ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests").glob("*.xml")}
    counts = {}
    with args.output.open("w", encoding="utf-8", newline="") as out:
        writer = csv.writer(out, delimiter="\t", lineterminator="\n")
        writer.writerow(["quest_id", "classification", "retail_dnf", "production_dnf", "absent_positive_refs", "unresolved_tokens"])
        for qid, meta in sorted(prod.items()):
            if qid not in retail:
                category, expected, unresolved = "NO_CLIENT_RECORD", [], []
            else:
                expected, unresolved = retail_contract(retail[qid], names)
                category = "TOKEN_UNRESOLVED" if unresolved else "MATCH"
            actual = production_contract(meta)
            absent = sorted({ref for group in expected for kind, ref, mode in group
                             if kind in ("finished", "acquired") and ref not in prod})
            if category == "MATCH" and expected != actual:
                category = "REVIEW_ABSENT_REFERENCE" if absent else "REVIEW_CONTRACT"
            counts[category] = counts.get(category, 0) + 1
            if category != "MATCH":
                writer.writerow([qid, category, json.dumps(expected), json.dumps(actual),
                                 json.dumps(absent), json.dumps(unresolved)])
    print(json.dumps({"production": len(prod), "counts": counts}, ensure_ascii=False))


if __name__ == "__main__":
    main()
