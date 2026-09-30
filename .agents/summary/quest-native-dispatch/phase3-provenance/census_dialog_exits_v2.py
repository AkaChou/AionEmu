#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P2a：dialog_exits 行级可达性普查 v2（只读，2026-09-28）。

与批 0 R1 的差异：
  ① 按 Phase 2 后的新编译路径重算：阶梯 token（SELECT1_1/SELECT1_1_1）只在
     RetailSimpleTalkDefinitionCompiler:1212/1214 读取，而该 else 分支要求
     `has NPC_START blocks ∧ !systemGrant ∧ ¬all(acceptSourcesWithinSegment)`；
     本脚本复刻该谓词（含 servesFinishDialog）。
  ② 新增客户端证据列：按 build_quest_client_dialog_exits.py 的同一映射
     （quest-dialog-pages.csv / quest-dialog-action-details.csv）反查每个 token 的证据；
     无客户端证据的 token 一律判"留"（保守，不因"看起来没人用"而删）。
输出：census-dialog-exits-v2.tsv（逐行）+ 控制台汇总。
"""
from __future__ import annotations
import os

import csv
import re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}")
RETAIL = ROOT / "src/main/resources/aion/data/static_data/quest_retail"
RETENTION = RETAIL / "retail-xml-retention.tsv"
SIMPLETALK = RETAIL / "Quest_SimpleTalk.xml"
CHAIN = RETAIL / "quest_client_talk_chain_steps.tsv"
EXITS = RETAIL / "quest_client_dialog_exits.tsv"
PAGES = ROOT / "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"
ACTIONS = ROOT / "docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"
OUT = Path(__file__).resolve().parent / "census-dialog-exits-v2.tsv"

LADDER = {"SELECT1_1", "SELECT1_1_1"}
BODY = {"SELECT2_CONTINUE", "SELECT5_CHECK", "SELECT5_CHECK_SIMPLE", "SELECT6"}
DD_ONLY = {"SELECT_NONE_1"}

# ---- 读取点守卫：requires( 必须仍是 12 处 / 3 文件（P2a 声称的静态面） ----
EXPECTED_REQUIRES = {
    "RetailSimpleTalkDefinitionCompiler.java": 10,
    "RetailDataDrivenDefinitionCompiler.java": 1,
    "RetailDataDrivenCollectCompiler.java": 1,
}
requires_counts = {}
for name in EXPECTED_REQUIRES:
    path = ROOT / "src/main/java/com/aionemu/gameserver/questEngine/retail" / name
    requires_counts[name] = len(re.findall(r"\brequires\(", path.read_text(encoding="utf-8")))
assert requires_counts == EXPECTED_REQUIRES, f"requires() 读取点漂移: {requires_counts}"

# ---- 输入解析 ----
family, owner = {}, {}
for line in RETENTION.read_text(encoding="utf-8").splitlines():
    if line.startswith("#") or not line.strip():
        continue
    parts = line.split("\t")
    if len(parts) >= 4 and parts[0].isdigit():
        family[int(parts[0])], owner[int(parts[0])] = parts[2], parts[1]

st_acquired, st_talknpcs = {}, set()
cur = None
for line in SIMPLETALK.read_text(encoding="utf-8").splitlines():
    m = re.match(r'\s*<id id="(\d+)">', line)
    if m:
        cur = int(m.group(1)); continue
    if cur is None:
        continue
    m = re.match(r"\s*<acquired_npc_name>(.*)</acquired_npc_name>", line)
    if m:
        st_acquired[cur] = m.group(1).strip(); continue
    if re.match(r"\s*<talk_npc\d+>", line):
        st_talknpcs.add(cur)

def is_sentinel(name: str | None) -> bool:
    return bool(name) and len(name) > 2 and name.startswith("_") and name.endswith("_")

blocks = defaultdict(list)   # qid -> [(npc_id, source, target, extra)]
routes = defaultdict(list)   # qid -> [(npc_id, action, source)]
for line in CHAIN.read_text(encoding="utf-8").splitlines():
    if line.startswith("#") or not line.strip():
        continue
    p = line.split("\t")
    qid = int(p[0])
    if p[1] == "B" and len(p) >= 7:
        blocks[qid].append((p[2], p[3], p[4], p[5], p[6]))  # label, npc, source, target, extra
    elif p[1] == "R" and len(p) >= 8:
        routes[qid].append((p[3], p[4], p[5]))

def serves_finish_dialog(qid: int, npc: str, source: str) -> bool:
    return any(n == npc and a == "FINISH_DIALOG" and s == source for n, a, s in routes[qid])

def accept_sources_within_segment(qid: int, block) -> bool:
    npc, target, extra = block[1], block[3], block[4]
    encoded = extra.split("|", -1)[0]
    for source in encoded.strip().split():
        if not source or source == "-":
            continue
        if source != "unaccepted" and source != target and serves_finish_dialog(qid, npc, source):
            return False
    return True

# 客户端证据（与 build_quest_client_dialog_exits.py 同映射）
pages = defaultdict(dict)          # qid -> page_name -> "source_file:sha8"
with PAGES.open(encoding="utf-8-sig") as fh:
    for row in csv.DictReader(fh):
        if row.get("source_variant") != "active" or row.get("page_mapping") != "exact":
            continue
        q = (row.get("quest_id") or "").strip()
        if q.isdigit():
            src = f"{(row.get('source_file') or '?')}:{(row.get('source_sha256') or '?')[:8]}"
            pages[int(q)][(row.get("html_page_name") or "").strip().lower()] = src
page_actions = defaultdict(lambda: defaultdict(dict))  # qid -> page -> action_id -> src
with ACTIONS.open(encoding="utf-8-sig", newline="") as fh:
    for row in csv.DictReader(fh):
        if row.get("source_variant") != "active" or row.get("page_mapping") != "exact":
            continue
        q = (row.get("quest_id") or "").strip()
        a = (row.get("action_id") or "").strip()
        if q.isdigit() and a.isdigit():
            src = f"{(row.get('source_file') or '?')}:{(row.get('source_sha256') or '?')[:8]}"
            page_actions[int(q)][(row.get("html_page_name") or "").lower()][int(a)] = src

def evidence_for(qid: int) -> dict[str, str]:
    ev: dict[str, str] = {}
    names, acts = pages.get(qid, {}), page_actions.get(qid, {})
    if "select_none_1" in names: ev["SELECT_NONE_1"] = f"pages:select_none_1@{names['select_none_1']}"
    if "select1_1" in names: ev["SELECT1_1"] = f"pages:select1_1@{names['select1_1']}"
    if "select1_1_1" in names: ev["SELECT1_1_1"] = f"pages:select1_1_1@{names['select1_1_1']}"
    if "select6" in names: ev["SELECT6"] = f"pages:select6@{names['select6']}"
    if 1353 in acts.get("select2", {}): ev["SELECT2_CONTINUE"] = f"actions:select2#1353@{acts['select2'][1353]}"
    if 39 in acts.get("select5", {}): ev["SELECT5_CHECK"] = f"actions:select5#39@{acts['select5'][39]}"
    if 20002 in acts.get("select5", {}): ev["SELECT5_CHECK_SIMPLE"] = f"actions:select5#20002@{acts['select5'][20002]}"
    return ev

# ---- 逐行普查 ----
report, verdicts, delete_candidates = [], Counter(), []
for line in EXITS.read_text(encoding="utf-8").splitlines():
    if line.startswith("#") or not line.strip():
        continue
    p = line.split("\t")
    qid = int(p[0])
    tokens = p[1].split() if len(p) > 1 else []
    fam, own = family.get(qid), owner.get(qid)
    ev = evidence_for(qid)
    start_blocks = [b for b in blocks.get(qid, []) if b[0] == "NPC_START"]
    has_start_blocks = bool(start_blocks)
    system_grant = is_sentinel(st_acquired.get(qid))
    single_step = qid in st_acquired and qid not in st_talknpcs
    if fam in ("DataDriven", "SimpleCollectItem"):
        klass = "dd-collect"
    elif fam == "SimpleTalk" and (qid in st_acquired or qid in st_talknpcs or has_start_blocks):
        if single_step:
            klass = "talk-single-step"
        elif system_grant:
            klass = "talk-system-grant"
        elif not has_start_blocks:
            klass = "talk-no-start-block"
        else:
            all_within = all(accept_sources_within_segment(qid, b) for b in start_blocks)
            klass = "talk-ladder-dead" if all_within else "talk-ladder-live"
    elif fam in ("SimpleHunt", "SimpleSerialHunt", "SimpleUseItem", "SimpleItemPlay", "CombineTask", "-", None):
        klass = f"family-{fam}"
    else:
        klass = "conservative-keep"

    row_verdicts = []
    for t in tokens:
        if t in LADDER:
            if klass == "talk-ladder-live":
                v, why = "留", "else 分支可达（acceptSourcesWithinSegment=false）"
            elif klass in ("talk-ladder-dead", "talk-system-grant", "talk-no-start-block", "talk-single-step"):
                v, why = "删", f"{klass}：:1212/1214 不可达"
            else:
                v, why = "判不了", f"class={klass}（保守保留）"
        elif t in BODY:
            v, why = ("留", "buildChain 体级 1411/1438/1465 无条件读") if klass.startswith("talk-") \
                else ("判不了", f"class={klass}（保守保留）")
        elif t in DD_ONLY:
            v, why = ("留", "DD:163 / collect:64 读取") if klass == "dd-collect" \
                else ("判不了", f"class={klass}（保守保留）")
        else:
            v, why = "判不了", "未知 token（保守保留）"
        if v == "删" and t not in ev:
            v, why = "判不了", why + "；客户端证据缺失（保守保留）"
        evp = ev.get(t, "-")
        if v == "删":
            delete_candidates.append((qid, t))
        verdicts[(t, v)] += 1
        row_verdicts.append(f"{t}={v}[{evp}]{why}")
    report.append(f"{qid}\t{own}\t{fam}\t{klass}\t{' '.join(tokens)}\t{' | '.join(row_verdicts)}")

OUT.write_text(
    "# quest_id\towner\tfamily\tclass\ttokens\tverdicts（token=裁定[客户端证据]理由）\n"
    + "\n".join(report) + "\n", encoding="utf-8")
print(f"rows={len(report)} delete_candidates={len(delete_candidates)}")
print("verdict_by_token:", {k: v for k, v in sorted(verdicts.items())})
print("delete rows:", len({q for q, _ in delete_candidates}))
print("delete tokens:", dict(Counter(t for _, t in delete_candidates)))
class_cnt = Counter(r.split("\t")[3] for r in report)
print("classes:", dict(class_cnt))
