#!/usr/bin/env python3
"""Phase 4-1 对账：真端 Quest_SimpleHunt.xml ↔ 本仓库 quest-definition。

链路：
  真端表 countN/monsterN ──(name_desc 索引)──► npc_ids
  本仓库 <progress><bit-field width="6" offset="6*(N-1)"> + <counter-grid><dimension required/npc-ids>

输出：simple-hunt-reconciliation.tsv + 控制台汇总。
用法：python3 reconcile_simple_hunt.py [--rebuild-index] [--limit N]
"""
from __future__ import annotations

import argparse
import csv
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

BASE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(BASE, "../../.."))
RETAIL_TABLE = f"{os.environ.get('AION_RETAIL_ROOT', os.path.expanduser('~/IdeaProjects/58Server'))}/Map/XML/Quest_SimpleHunt.xml"
NPC_DIR = os.path.join(REPO, "src/main/resources/aion/data/static_data/npcs")
QUEST_DIR = os.path.join(REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")
CLIENT_DIALOGS = f"{os.environ.get('AION_UNPACK_ROOT', os.path.expanduser('~/PycharmProjects/unpak'))}/data_unpacked/Dialogs"
CLIENT_TABLES = f"{os.environ.get('AION_UNPACK_ROOT', os.path.expanduser('~/PycharmProjects/unpak'))}/Quest_unpacked"
CSV_GUARD = re.compile(r"SECTION_(\d+)<(\d+)")
INDEX_TSV = os.path.join(BASE, "npc_name_index.tsv")
OUT_TSV = os.path.join(BASE, "simple-hunt-reconciliation.tsv")

NPC_TEMPLATE_RE = re.compile(r'<npc_template\b[^>]*?name_desc="([^"]+)"[^>]*?npc_id="(\d+)"')
NPC_TEMPLATE_RE_ALT = re.compile(r'<npc_template\b[^>]*?npc_id="(\d+)"[^>]*?name_desc="([^"]+)"')


def read_text(path):
    raw = open(path, "rb").read()
    enc = "utf-16" if raw[:2] in (b"\xff\xfe", b"\xfe\xff") else "utf-8"
    return raw.decode(enc, errors="replace")


def build_index():
    index = defaultdict(set)
    for name in sorted(os.listdir(NPC_DIR)):
        if not name.endswith(".xml"):
            continue
        text = read_text(os.path.join(NPC_DIR, name))
        for m in NPC_TEMPLATE_RE.finditer(text):
            index[m.group(1)].add(int(m.group(2)))
        for m in NPC_TEMPLATE_RE_ALT.finditer(text):
            index[m.group(2)].add(int(m.group(1)))
    with open(INDEX_TSV, "w") as fh:
        fh.write("name_desc\tnpc_ids\n")
        for key in sorted(index):
            fh.write(f"{key}\t{' '.join(str(v) for v in sorted(index[key]))}\n")
    return index


def load_index(rebuild):
    if rebuild or not os.path.exists(INDEX_TSV):
        return build_index()
    index = {}
    with open(INDEX_TSV, newline="") as fh:
        for row in csv.reader(fh, delimiter="\t"):
            if len(row) == 2 and row[0] != "name_desc":
                index[row[0]] = {int(v) for v in row[1].split()}
    return index


def parse_retail():
    text = read_text(RETAIL_TABLE)
    body = text.split("]>", 1)[1] if "]>" in text else text
    root = ET.fromstring(body)
    out = {}
    for node in root:
        qid = node.get("id")
        if not qid or not qid.isdigit():
            continue
        counters = []
        for n in range(1, 7):
            count = node.findtext(f"count{n}")
            monsters = node.findtext(f"monster{n}")
            if count is None and monsters is None:
                continue
            counters.append({
                "n": n,
                "count": int(count) if (count or "").strip().isdigit() else None,
                "monsters": [m.strip() for m in (monsters or "").split(",") if m.strip()],
            })
        out[int(qid)] = {
            "counters": counters,
            "acquired": (node.findtext("acquired_npc_name") or "").strip(),
            "reward": (node.findtext("reward_npc_name") or "").strip(),
            "con_quest": (node.findtext("con_quest") or "").strip(),
        }
    return out


def parse_repo(qid):
    path = os.path.join(QUEST_DIR, f"{qid}.xml")
    if not os.path.exists(path):
        return None
    root = ET.parse(path).getroot()
    fields = []
    for field in root.findall("./progress/bit-field"):
        fields.append({
            "name": field.get("name"),
            "offset": int(field.get("offset", "0")),
            "width": int(field.get("width", "1")),
            "max": field.get("max"),
        })
    dims = []
    for dim in root.findall("./transitions/counter-grid/dimension"):
        dims.append({
            "field": dim.get("field"),
            "required": int(dim.get("required", "0")),
            "npc_ids": [int(x) for x in (dim.get("npc-ids") or "").split() if x.isdigit()],
            "source_order": dim.get("source-order") or "",
        })
    kill_chain = None
    chain = root.find("./transitions/kill-chain")
    if chain is not None:
        kill_chain = [n for n in (chain.get("nodes") or "").split() if n]
    return {"fields": fields, "dims": dims, "kill_chain": kill_chain}


def load_client_csv():
    """quest_monster.csv + quest_script_monster.csv：quest → {counter_index(1起): (limit, ids)}"""
    out = defaultdict(dict)
    for fname in ("quest_monster.csv", "quest_script_monster.csv"):
        path = os.path.join(CLIENT_TABLES, fname)
        if not os.path.exists(path):
            continue
        with open(path, newline="", encoding="utf-8", errors="replace") as fh:
            for row in csv.DictReader(fh, skipinitialspace=True):
                qid = (row.get("questId") or "").strip()
                if not qid.isdigit():
                    continue
                guard = row.get("questProgress") or row.get("progress") or ""
                m = CSV_GUARD.search(guard)
                if not m:
                    continue
                section, limit = int(m.group(1)), int(m.group(2))
                if section == 5:
                    continue
                names = [x.strip() for x in (row.get("monsters_gathers_npcs_list") or "").split(",") if x.strip()]
                slot = out[int(qid)].setdefault(section, {"limit": limit, "names": set()})
                slot["limit"] = max(slot["limit"], limit)
                slot["names"].update(names)
    return out


def client_evidence(qid):
    """客户端摘要证据：<step> 行数与击杀行的计数显示 ([%n]/N)。"""
    path = os.path.join(CLIENT_DIALOGS, f"QUEST_Q{qid}.html")
    if not os.path.exists(path):
        return None
    src = open(path, encoding="utf-8", errors="replace").read()
    m = re.search(r'<HtmlPage name="quest_summary">.*?</HtmlPage>', src, re.S)
    if not m:
        return {"rows": None, "counters": []}
    body = m.group(0)
    rows = len(re.findall(r"<step>", body))
    counters = [f"{a}/{b}" for a, b in re.findall(r"\[%(\d+)\]/(\d+)", body)]
    return {"rows": rows, "counters": counters}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--rebuild-index", action="store_true")
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--emit-contract", default="", help="输出 JUnit 门禁用 TSV")
    args = ap.parse_args()

    index = load_index(args.rebuild_index)
    retail = parse_retail()
    csv_expect = load_client_csv()
    contract_rows = []

    lower_index = {k.lower(): v for k, v in index.items()}

    def resolve(names):
        ids, missing = set(), []
        for name in names:
            hit = index.get(name) or lower_index.get(name.lower())
            if hit:
                ids |= hit
            else:
                missing.append(name)
        return ids, missing
    dupes = {k: v for k, v in index.items() if len(v) > 1}
    print(f"NPC 名索引: {len(index)} 个 name_desc（多 id 名字 {len(dupes)} 个）")
    print(f"真端 SimpleHunt 行: {len(retail)}")

    rows = []
    stats = defaultdict(int)
    counter = 0
    for qid, info in sorted(retail.items()):
        repo = parse_repo(qid)
        if repo is None:
            stats["NO_REPO_XML"] += 1
            continue
        counter += 1
        if args.limit and counter > args.limit:
            break
        status = []
        sixbit = {f["offset"]: f for f in repo["fields"] if f["width"] == 6}
        dims_by_field = {}
        for d in repo["dims"]:
            dims_by_field.setdefault(d["field"], []).append(d)
        for c in info["counters"]:
            n = c["n"]
            resolved, unresolved = resolve(c["monsters"])
            csv_slot = csv_expect.get(qid, {}).get(n - 1)
            csv_ids, csv_unresolved = resolve(csv_slot["names"]) if csv_slot else (set(), [])
            csv_limit = csv_slot["limit"] if csv_slot else None
            if unresolved:
                status.append(f"UNRESOLVED_NAME:n{n}:{','.join(unresolved)}")
                stats["UNRESOLVED_NAME"] += 1
                contract_rows.append((qid, n, "UNRESOLVED_NAME", c["count"] or 0, [], []))
                continue
            offset = 6 * (n - 1)
            field = sixbit.get(offset)
            if field is None:
                wider = [f for f in repo["fields"]
                         if f["offset"] <= offset < f["offset"] + f["width"] and f["width"] > 6]
                if wider and c["count"] is not None and c["count"] > 63:
                    status.append(f"WIDE_FIELD_FOR_COUNT_GT_63:n{n}:{wider[0]['name']}w{wider[0]['width']}max{wider[0]['max']}")
                    stats["WIDE_FIELD_FOR_COUNT_GT_63"] += 1
                    contract_rows.append((qid, n, "WIDE_FIELD", c["count"], sorted(resolved),
                                          sorted(csv_ids - resolved)))
                else:
                    status.append(f"NO_FIELD:n{n}@off{offset}")
                    stats["NO_FIELD"] += 1
                    contract_rows.append((qid, n, "NO_FIELD", c["count"], sorted(resolved),
                                          sorted(csv_ids - resolved)))
                continue
            dims = dims_by_field.get(field["name"], [])
            if not dims:
                if repo["kill_chain"]:
                    kills = max(len(repo["kill_chain"]) - 1, 0)
                    if c["count"] is not None and kills == c["count"]:
                        status.append(f"EQUIVALENT_KILL_CHAIN:n{n}:{kills}杀")
                        stats["EQUIVALENT_KILL_CHAIN"] += 1
                        contract_rows.append((qid, n, "KILL_CHAIN", c["count"], sorted(resolved),
                                              sorted(csv_ids - resolved)))
                    else:
                        status.append(f"CHAIN_COUNT_MISMATCH:n{n}:链{kills}杀≠表{c['count']}")
                        stats["CHAIN_COUNT_MISMATCH"] += 1
                elif any(f["offset"] == offset for f in repo["fields"]):
                    status.append(f"FIELD_WITHOUT_KILL_MODEL:n{n}")
                    stats["FIELD_WITHOUT_KILL_MODEL"] += 1
                    contract_rows.append((qid, n, "FIELD_NO_KILL_MODEL", c["count"], sorted(resolved),
                                          sorted(csv_ids - resolved)))
                else:
                    status.append(f"NO_DIMENSION:n{n}")
                    stats["NO_DIMENSION"] += 1
                    contract_rows.append((qid, n, "NO_FIELD", c["count"], sorted(resolved),
                                          sorted(csv_ids - resolved)))
                continue
            allowed = resolved | csv_ids
            model = "MISMATCH"
            matched = None
            for d in dims:
                ids = set(d["npc_ids"])
                if d["required"] == c["count"] and resolved <= ids <= allowed:
                    matched = d
                    break
            if matched is None:
                best = min(dims, key=lambda d: abs(d["required"] - (c["count"] or 0)))
                repo_ids = set(best["npc_ids"])
                if repo_ids == csv_ids and csv_ids != resolved:
                    status.append(f"CSV_ALIGNED:n{n}:limit_repo={best['required']}/csv={csv_limit}/retail={c['count']}")
                    stats["CSV_ALIGNED"] += 1
                    continue
                if repo_ids == (resolved | csv_ids) and csv_ids:
                    status.append(f"UNION_RETAIL_CSV:n{n}")
                    stats["UNION_RETAIL_CSV"] += 1
                    continue
                missing = sorted(resolved - set(best["npc_ids"]))
                extra = sorted(set(best["npc_ids"]) - resolved)
                detail = []
                if best["required"] != c["count"]:
                    detail.append(f"count {best['required']}≠{c['count']}")
                if missing:
                    detail.append(f"missing={','.join(map(str, missing))}")
                if extra:
                    detail.append(f"extra={','.join(map(str, extra))}")
                status.append(f"MISMATCH:n{n}:" + ";".join(detail))
                stats["MISMATCH"] += 1
                contract_rows.append((qid, n, "PENDING_MISMATCH", c["count"] or 0, sorted(resolved),
                                      sorted(csv_ids - resolved)))
                continue
            stats["MATCH"] += 1
            contract_rows.append((qid, n, "COUNTER_GRID", c["count"], sorted(resolved), sorted(csv_ids - resolved)))
        ev = client_evidence(qid)
        rows.append({
            "quest_id": qid,
            "client_rows": "" if ev is None else (ev["rows"] if ev["rows"] is not None else "no-summary"),
            "client_counters": "" if ev is None else " ".join(ev["counters"]),
            "retail_counters": "; ".join(
                f"n{c['n']}:{c['count']}x[{','.join(c['monsters'])}]" for c in info["counters"]),
            "repo_fields6": "; ".join(f"{f['name']}@off{f['offset']}" for f in repo["fields"] if f["width"] == 6),
            "repo_dims": "; ".join(
                f"{d['field']}:{d['required']}x[{','.join(map(str, d['npc_ids']))}]" for d in repo["dims"]),
            "status": "OK" if not status else " | ".join(status),
        })
    with open(OUT_TSV, "w", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=["quest_id", "client_rows", "client_counters", "retail_counters",
                                                "repo_fields6", "repo_dims", "status"], delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)

    if args.emit_contract:
        with open(args.emit_contract, "w", newline="") as fh:
            fh.write("# Aion 5.8 真端 Quest_SimpleHunt.xml 击杀计数合同快照\n")
            fh.write("# model: COUNTER_GRID=6位字段+dimension；KILL_CHAIN=串行链；其余=未按计数器建模（另案）\n")
            fh.write("quest_id\tcounter_index\tmodel\trequired\tnpc_ids\textra_ids\n")
            for qid, n, model, required, ids, extra in sorted(contract_rows):
                id_cell = " ".join(map(str, ids)) or "-"
                extra_cell = " ".join(map(str, extra)) or "-"
                fh.write(f"{qid}\t{n}\t{model}\t{required}\t{id_cell}\t{extra_cell}\n")
        print(f"合同快照已写出: {len(contract_rows)} 行")
    print(f"对账任务: {counter}")
    for key in sorted(stats):
        print(f"  {key:<16} {stats[key]}")
    bad = [r for r in rows if r["status"] != "OK"]
    print(f"完全一致: {sum(1 for r in rows if r['status'] == 'OK')} / {len(rows)}")
    print("--- 前 15 个不一致 ---")
    for r in bad[:15]:
        print(f"  {r['quest_id']}: {r['status']} || retail={r['retail_counters']} || repo={r['repo_dims']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
