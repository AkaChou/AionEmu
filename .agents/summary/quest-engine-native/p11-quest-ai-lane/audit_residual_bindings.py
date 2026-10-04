#!/usr/bin/env python3
"""D1 残余裁决：Quest-AI 集外对话位 / 真端无注册任务 的多轴对拍。

残余面（口径与 measure_binding_gate.py 相同）：
  A. **OUTSIDE_SURFACE**：某任务的 `<dialog>`/`<npc-complete>` 引用**全部**落在全局 Quest-AI
     npc id 集之外（67 件）——真端 Quest-AI 面不覆盖这些对话位；
  B. **UNREGISTERED_TASK**：真端 `FUN_180cb5920` 注册面查无此 quest id，但 XML 带 Quest-AI
     集内引用（18 件）。

四条独立轴（缺一不可，任一轴命中即不是「凭空对话位」）：
  1. **NPC 轴**：XML 对话位是否存在于真端 npcs.xml；有无 `quest_ai_name`（无 = 通用任务对话 NPC）。
  2. **遗留/客户端合同轴**：`legacy-quest-dialog-contracts.csv`（本仓 origin/history compact quest
     scripts 提取；sha256 85016518d757e07f829c5a92fa8189053f65d9db865b1ee34690079486b6adb7，
     与 quest_client_handin_npc_sets.xml 同源）的 start ∪ end ∪ progress NPC 集。
  3. **真端 DD 轴**：`data_driven_quest.xml` 行的 `value0_acquire_` / `reward_npc_name`（名字，按
     npcs.xml name/quest_ai_name 大小写不敏感解析为 npc id）。
  4. **客户端轴**：`quest-dialog-pages.csv`（Aion 5.8 客户端 Quest.pak HTML，active 变体）是否有该
     任务的对话页。
  5. **quest-id 直驱脚本轴**：`retail-xml-retention.xml` 的 `reason=SCRIPTED` +
     `evidence=registry=FUN_...`（真端按 quest id 注册的直驱脚本口，QE-136 已列明的「首参注册口」族）。

输出：TSV（逐件全轴明细）+ 分类计数。
用法：python3 audit_residual_bindings.py [--contracts <csv>] [--client-pages <csv>] [--tsv <out>]
"""
import argparse
import csv
import importlib.util
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
HERE = Path(__file__).resolve().parent
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
RETAIL = REPO / "src/main/resources/aion/data/static_data/quest/retail"
NPCS = Path("/Users/mc/IdeaProjects/58Server/Map/XML/npcs.xml")
CLIENT_DIR = Path("/Users/mc/IdeaProjects/AionEmu-headless/data/client-dialog-mapping")
DEFAULT_CONTRACTS = CLIENT_DIR / "legacy-quest-dialog-contracts.csv"
DEFAULT_CLIENT_PAGES = CLIENT_DIR / "quest-dialog-pages.csv"
DEFAULT_TSV = HERE / "d1-residual-adjudication.tsv"


def load_measure():
    spec = importlib.util.spec_from_file_location("measure_binding_gate", HERE / "measure_binding_gate.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def load_npcs():
    """npc id → {name, quest_ai_name}；name/quest_ai_name 折叠索引 → id 集。"""
    txt = NPCS.read_bytes().decode("utf-16")
    info, by_name, by_qa = {}, defaultdict(set), defaultdict(set)
    cur = None
    for line in txt.splitlines():
        s = line.strip()
        if s.startswith("<id>") and s.endswith("</id>"):
            cur = int(s[4:-5])
            info.setdefault(cur, {})
        elif cur is None:
            continue
        elif s.startswith("<name>") and s.endswith("</name>"):
            info[cur]["name"] = s[6:-7]
            by_name[s[6:-7].casefold()].add(cur)
        elif s.startswith("<quest_ai_name>") and s.endswith("</quest_ai_name>"):
            info[cur]["qa"] = s[15:-16]
            by_qa[s[15:-16].casefold()].add(cur)
    return info, by_name, by_qa


def load_contracts(path: Path):
    out = {}
    with path.open(encoding="utf-8-sig", newline="") as handle:
        for row in csv.DictReader(handle):
            if not (row.get("quest_id") or "").strip().isdigit():
                continue
            sets = {c: {int(t) for t in (row.get(c) or "").split() if t.isdigit()}
                    for c in ("start_npc_ids", "end_npc_ids", "progress_npc_ids")}
            out[int(row["quest_id"])] = {
                "ids": set().union(*sets.values()),
                "template": row.get("template_type", ""),
                "resource": row.get("source_resource", ""),
            }
    return out


def load_client_pages(path: Path):
    out = defaultdict(set)
    with path.open(encoding="utf-8-sig", newline="") as handle:
        for row in csv.DictReader(handle):
            q = (row.get("quest_id") or "").strip()
            if q.isdigit() and row.get("source_variant") == "active":
                out[int(q)].add(row.get("html_page_name", ""))
    return out


def load_dd_rows():
    """DD 行 id → (接取名集, 交付名集)。"""
    text = (RETAIL / "data_driven_quest.xml").read_text(encoding="utf-8")
    out = {}
    for block in re.findall(r"<quest_data_driven>(.*?)</quest_data_driven>", text, re.S):
        qid = int(re.search(r"<id>(\d+)</id>", block).group(1))
        acquire = {v.strip() for v in re.findall(r"<value0_acquire_>([^<]*)</value0_acquire_>", block) if v.strip()}
        reward = {v.strip() for v in re.findall(r"<reward_npc_name>([^<]*)</reward_npc_name>", block) if v.strip()}
        out[qid] = (acquire, reward)
    return out


def load_owners():
    text = (RETAIL / "retail-xml-retention.xml").read_text(encoding="utf-8")
    out = {}
    for block in re.findall(r"<quest>(.*?)</quest>", text, re.S):
        qid = int(re.search(r"<quest_id>(\d+)</quest_id>", block).group(1))
        owner = re.search(r"<owner>([^<]*)</owner>", block)
        reason = re.search(r"<reason>([^<]*)</reason>", block)
        evidence = re.search(r"<evidence>([^<]*)</evidence>", block)
        out[qid] = (owner.group(1) if owner else "", reason.group(1) if reason else "",
                    evidence.group(1) if evidence else "")
    return out


def resolve(names, by_name, by_qa):
    ids = set()
    for name in names:
        ids |= by_name.get(name.casefold(), set())
        ids |= by_qa.get(name.casefold(), set())
    return ids


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--contracts", type=Path, default=DEFAULT_CONTRACTS)
    parser.add_argument("--client-pages", type=Path, default=DEFAULT_CLIENT_PAGES)
    parser.add_argument("--tsv", type=Path, default=DEFAULT_TSV)
    args = parser.parse_args()

    measure = load_measure()
    result = measure.audit()
    global_ai, by_quest = result["global_ai"], result["by_quest"]
    npc_info, by_name, by_qa = load_npcs()
    contracts = load_contracts(args.contracts)
    client_pages = load_client_pages(args.client_pages)
    dd_rows = load_dd_rows()
    owners = load_owners()
    ref_pat = re.compile(measure.REF_PATTERN)

    rows = []
    for f in sorted(QUESTS.glob("*.xml")):
        quest = int(f.stem)
        refs = {int(x) for x in ref_pat.findall(f.read_text(encoding="utf-8"))}
        if not refs:
            continue
        on_surface = refs & global_ai
        if not on_surface:
            kind = "OUTSIDE_SURFACE"
        elif not by_quest.get(quest):
            kind = "UNREGISTERED_TASK"
        else:
            continue

        # 轴 1：NPC 存在性 / quest_ai_name
        missing = sorted(n for n in refs if n not in npc_info)
        qa_names = {npc_info[n].get("qa") for n in refs if n in npc_info and "qa" in npc_info[n]}
        plain = sorted(n for n in refs if n in npc_info and "qa" not in npc_info[n])
        if missing:
            npc_axis = "NPC_MISSING"
        elif plain and qa_names:
            npc_axis = "MIXED_AI_DECLARED_AND_PLAIN"
        elif plain:
            npc_axis = "ALL_PLAIN_NPC"
        else:
            npc_axis = "ALL_AI_DECLARED"

        # 轴 2：遗留/客户端合同
        contract = contracts.get(quest)
        declared = contract["ids"] if contract else set()
        if contract is None:
            legacy = "NO_ROW"
        elif refs <= declared:
            legacy = "COVERED"
        elif declared & refs:
            legacy = "PARTIAL"
        else:
            legacy = "CONFLICT"

        # 轴 3：真端 DD 行（接取/交付名 → npc id）
        acquire, reward = dd_rows.get(quest, (set(), set()))
        dd_ids = resolve(acquire, by_name, by_qa) | resolve(reward, by_name, by_qa)
        if not dd_rows.get(quest):
            dd_axis = "NO_DD_ROW"
        elif refs & dd_ids:
            dd_axis = "DD_MATCH"
        else:
            dd_axis = "DD_MISMATCH"

        # 轴 4：客户端 HTML 页
        pages = client_pages.get(quest, set())
        client_axis = f"PAGES={len(pages)}" if pages else "NO_CLIENT_PAGES"

        owner, reason, evidence = owners.get(quest, ("", "", ""))
        scripted = reason == "SCRIPTED" and "registry=" in evidence

        verdict = "UNRESOLVED"
        if legacy == "COVERED":
            verdict = "LEGACY_BACKED"
        elif dd_axis == "DD_MATCH":
            verdict = "DD_BACKED"
        elif npc_axis == "ALL_PLAIN_NPC" and client_axis != "NO_CLIENT_PAGES":
            verdict = "PLAIN_NPC_WITH_CLIENT_PAGES"
        elif scripted:
            verdict = "SCRIPTED_REGISTRY"
        elif reason.startswith("ADJUDICATED:") and dd_axis == "DD_MISMATCH":
            verdict = "DD_REWARD_DEFERRED"
        elif client_axis != "NO_CLIENT_PAGES":
            verdict = "CLIENT_PAGES_ONLY"

        rows.append(dict(quest=quest, kind=kind, verdict=verdict, npc_axis=npc_axis, legacy=legacy,
                         dd=dd_axis, client=client_axis, owner=owner, reason=reason, evidence=evidence,
                         xml=sorted(refs), legacy_ids=sorted(declared), dd_ids=sorted(dd_ids),
                         missing=missing, qa=sorted(x for x in qa_names if x),
                         plain=plain, pages=len(pages)))

    header = ["quest_id", "kind", "verdict", "npc_axis", "legacy", "dd_axis", "client_pages", "owner",
              "reason", "retail_evidence", "xml_dialog_npcs", "legacy_contract_npcs", "dd_resolved_npcs",
              "npc_missing", "npc_quest_ai_names", "npc_plain"]
    args.tsv.write_text("\t".join(header) + "\n" + "\n".join("\t".join(str(r[k]) for k in (
        "quest", "kind", "verdict", "npc_axis", "legacy", "dd", "client", "owner", "reason", "evidence",
        "xml", "legacy_ids", "dd_ids", "missing", "qa", "plain")) for r in rows) + "\n", encoding="utf-8")

    print(f"残余任务 {len(rows)} 件（OUTSIDE_SURFACE ∪ UNREGISTERED_TASK = 67 ∪ 18）\n")
    print("判定分布：")
    for (kind, verdict), n in sorted(Counter((r["kind"], r["verdict"]) for r in rows).items()):
        print(f"  {kind:17s} {verdict:28s} {n}")
    print("\n轴分布：")
    for axis in ("npc_axis", "legacy", "dd", "client"):
        counts = Counter(r[axis].split("=")[0] if axis == "client" else r[axis] for r in rows)
        print(f"  {axis:9s} {dict(sorted(counts.items()))}")
    print(f"\nwrote {args.tsv.relative_to(REPO)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
