#!/usr/bin/env python3
"""Phase 4-2 对账：真端 Quest_SimpleTalk.xml / data_driven_quest.xml ↔ 本仓库定义。

SimpleTalk：acquired/reward/talk_npcN 解析成 npc_id 后，必须出现在本仓库定义的转移引用里。
DataDriven：每个 progress 步骤的 value0（数字=id，名字=name_desc→id）必须在本仓库定义里出现。
输出：simple-talk-reconciliation.tsv、datadriven-step-reconciliation.tsv + 控制台汇总。
"""
from __future__ import annotations

import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

BASE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(BASE, "../../.."))
RETAIL = f"{REPO.parent / '58Server'}/Map/XML"
QUEST_DIR = os.path.join(REPO, "src/main/resources/aion/data/static_data/quest/definitions/quests")
INDEX_TSV = os.path.join(BASE, "npc_name_index.tsv")


def read_text(path):
    raw = open(path, "rb").read()
    enc = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    return raw.decode(enc, errors="replace")


def load_index():
    idx, low = {}, {}
    with open(INDEX_TSV) as fh:
        next(fh)
        for line in fh:
            parts = line.rstrip("\n").split("\t")
            if len(parts) == 2:
                idx[parts[0]] = {int(v) for v in parts[1].split()}
                low[parts[0].lower()] = idx[parts[0]]
    return idx, low


def parse_retail(path, root_tag):
    text = read_text(path)
    try:
        return ET.fromstring(text)  # 保留内部 DTD 实体定义
    except ET.ParseError:
        body = text.split("]>", 1)[1] if "]>" in text else text
        return ET.fromstring(body)


def repo_ids(qid):
    path = os.path.join(QUEST_DIR, f"{qid}.xml")
    if not os.path.exists(path):
        return None
    text = open(path, encoding="utf-8").read()
    ids = set()
    for attr in ("npc-id", "npc-ids", "object-id", "object-ids", "target-id", "target-ids"):
        for raw in re.findall(rf'{attr}="([^"]+)"', text):
            ids |= {int(v) for v in raw.split() if v.isdigit()}
    return ids


def resolve(names, idx, low):
    ids, missing = set(), []
    for name in names:
        if not name:
            continue
        if name.isdigit():
            ids.add(int(name))
            continue
        hit = idx.get(name) or low.get(name.lower())
        if hit:
            ids |= hit
        else:
            missing.append(name)
    return ids, missing


def main() -> int:
    idx, low = load_index()
    print(f"NPC 名索引: {len(idx)}")

    # ---------- SimpleTalk ----------
    talk = parse_retail(os.path.join(RETAIL, "Quest_SimpleTalk.xml"), "quest_simpletalks")
    rows, stats = [], defaultdict(int)
    for node in talk:
        qid = (node.get("id") or "").strip()
        if not qid.isdigit():
            continue
        qid = int(qid)
        roles = {"acquired": node.findtext("acquired_npc_name") or "",
                 "reward": node.findtext("reward_npc_name") or ""}
        for n in range(1, 5):
            value = node.findtext(f"talk_npc{n}")
            if value:
                roles[f"talk{n}"] = value
        repo = repo_ids(qid)
        if repo is None:
            stats["NO_REPO_XML"] += 1
            continue
        missing_roles, unresolved = [], []
        for role, name in roles.items():
            ids, bad = resolve([name], idx, low)
            unresolved += bad
            if ids and not (ids & repo):
                missing_roles.append(f"{role}={name}->{sorted(ids)}")
        if unresolved:
            stats["UNRESOLVED_NAME"] += 1
        elif missing_roles:
            stats["MISSING_IN_DEFINITION"] += 1
        else:
            stats["OK"] += 1
        rows.append((qid, ";".join(f"{k}={v}" for k, v in roles.items()), ";".join(missing_roles)))
    with open(os.path.join(BASE, "simple-talk-reconciliation.tsv"), "w") as fh:
        fh.write("quest_id\troles\tmissing_roles\n")
        for qid, roles, missing in rows:
            fh.write(f"{qid}\t{roles}\t{missing}\n")
    print("SimpleTalk 对账:", dict(stats))

    # ---------- DataDriven ----------
    dd = parse_retail(os.path.join(RETAIL, "data_driven_quest.xml"), "quest_data_drivens")
    dd_rows, dd_stats = [], defaultdict(int)
    for node in dd:
        qid_text = node.findtext("id") or ""
        if not qid_text.strip().isdigit():
            continue
        qid = int(qid_text.strip())
        repo = repo_ids(qid)
        if repo is None:
            dd_stats["NO_REPO_XML"] += 1
            continue
        steps, matched = 0, 0
        details = []
        for data in node.findall("./progress_info/data"):
            category = (data.findtext("category_progress_") or "").strip()
            target = (data.findtext("value0_progress_") or "").strip()
            if not category and not target:
                continue
            steps += 1
            ids, _missing = resolve([target], idx, low)
            if ids and (ids & repo):
                matched += 1
            else:
                details.append(f"{category}:{target}")
        if steps == 0:
            dd_stats["NO_STEPS"] += 1
        elif matched == steps:
            dd_stats["ALL_STEPS_PRESENT"] += 1
        elif matched:
            dd_stats["PARTIAL"] += 1
        else:
            dd_stats["NONE"] += 1
        dd_rows.append((qid, steps, matched, "; ".join(details[:4])))
    with open(os.path.join(BASE, "datadriven-step-reconciliation.tsv"), "w") as fh:
        fh.write("quest_id\tsteps\tmatched\tmissing_examples\n")
        for row in dd_rows:
            fh.write("\t".join(str(x) for x in row) + "\n")
    print("DataDriven 对账:", dict(dd_stats))
    return 0


if __name__ == "__main__":
    sys.exit(main())
