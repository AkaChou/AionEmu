#!/usr/bin/env python3
"""阶段 2：修复 PAGE_NOT_IN_TASK_HTML 行（分家族批处理，先 dry-run 出清单再 --write）。

Phase 2: fix PAGE_NOT_IN_TASK_HTML rows by family (dry-run manifest first, --write to apply).

家族 / families:
  report   : NPC_REPORT 的 page 指向客户端报告根（缺省 DEFAULT_SUCCESS 全局协议页）。
  start    : NPC_START 的 start-page 指向客户端接取链根；契约证明非接取 NPC 时删除该入口。
  explicit : 显式 transition 的 SHOW 页面不在客户端 HTML 中（改为 close 或删除路由）。
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / ".agent/summary/quest"))

AUDIT = ROOT / ".agent/summary/quest-load-fail/quest-order-audit-current.csv"
PAGES = ROOT / "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"
ACTIONS = ROOT / "docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"
CONTRACTS = ROOT / "docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv"
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
MANIFEST = ROOT / ".agent/summary/quest-load-fail/notask-fix-manifest.csv"

PATH_RE = re.compile(r"^(?P<src>\S*) \+ (?P<owner>NPC \d+|QUEST_ACTION) \+ (?P<action>\S+) -> (?P<tgt>\S*) \+ page (?P<page>\d+)$")
ENUM_RE = re.compile(r"^\t([A-Z0-9_]+)\((\d+)\),", re.MULTILINE)

# 全局协议页：HtmlPages.xml 解析，不属于单个任务 HTML。
# Global protocol pages: resolved by HtmlPages.xml, not owned by one quest HTML.
GLOBAL_PROTOCOL = {10000: "CHECK_USER_ITEM_OK", 10001: "CHECK_USER_ITEM_FAIL",
                   10002: "DEFAULT_SUCCESS"}


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def page_symbols() -> dict[int, str]:
    text = (ROOT / "src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogPage.java").read_text()
    return {int(num): name for name, num in ENUM_RE.findall(text)}


def client_model():
    pages: dict[int, dict[int, str]] = defaultdict(dict)
    for row in read_csv(PAGES):
        if row["source_variant"] == "active" and row["page_mapping"] == "exact":
            pages[int(row["quest_id"])][int(row["page_id"])] = row["page_constant"].removeprefix("HTML_PAGE_")
    actions: dict[int, dict[int, set[str]]] = defaultdict(lambda: defaultdict(set))
    for row in read_csv(ACTIONS):
        if (row["source_variant"] == "active" and row["page_mapping"] == "exact"
                and row["action_mapping"] == "exact"):
            actions[int(row["quest_id"])][int(row["page_id"])].add(row["action_constant"].removeprefix("HACTION_"))
    return pages, actions


def chain_roots(q: int, pages, actions):
    """返回 (接取链根 page_id, 报告链根 page_id)；不唯一时为 None。

    Returns (start chain root page id, report chain root page id); None when not unique.
    """
    html = pages.get(q, {})
    acts = actions.get(q, {})
    by_name = {name: pid for pid, name in html.items()}
    preds: dict[int, set[int]] = defaultdict(set)
    for pid, pa in acts.items():
        for a in pa:
            target = by_name.get(a)
            if target is not None and target != pid:
                preds[target].add(pid)
            if a == "ASK_QUEST_ACCEPT":
                preds[4].add(pid)

    def closure(seeds: set[int]) -> set[int]:
        seen = set(seeds)
        pending = list(seeds)
        while pending:
            cur = pending.pop()
            for prev in preds.get(cur, ()):
                if prev not in seen:
                    seen.add(prev)
                    pending.append(prev)
        return seen

    interactive = {pid for pid, pa in acts.items() if pa}
    accepting = {pid for pid, pa in acts.items() if pa & {"QUEST_ACCEPT_1", "QUEST_ACCEPT_SIMPLE"}}
    reporting = {pid for pid, pa in acts.items() if "SELECT_QUEST_REWARD" in pa}

    start = None
    if accepting:
        closure_set = closure(accepting) & interactive
        roots = [pid for pid in closure_set if not (preds.get(pid, set()) & closure_set)]
        if len(roots) == 1:
            start = roots[0]
    report = None
    if reporting:
        closure_set = closure(reporting) - closure(accepting) if accepting else closure(reporting)
        roots = [pid for pid in closure_set if not (preds.get(pid, set()) & closure_set)]
        if len(roots) == 1:
            report = roots[0]
    return start, report


def load_contracts():
    grouped: dict[int, list[dict[str, str]]] = defaultdict(list)
    for row in read_csv(CONTRACTS):
        if row["contract_scope"] == "FULL":
            grouped[int(row["quest_id"])].append(row)
    return {q: rs[0] for q, rs in grouped.items() if len(rs) == 1}


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--family", choices=("report", "start", "explicit"), required=True)
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def collect(family: str):
    symbols = page_symbols()
    pages, actions = client_model()
    contracts = load_contracts()
    rows = read_csv(AUDIT)
    plan = []
    seen_rows: set[tuple] = set()
    for r in rows:
        if not r["unresolved_reason"].startswith("compiled IR emits a task page absent"):
            continue
        m = PATH_RE.match(r["actual_path"])
        if not m:
            continue
        q = int(r["quest_id"])
        npc = int(r["npc_id"]) if r["npc_id"] else 0
        action = m.group("action")
        emitted = int(m.group("page"))
        src = m.group("src")
        key = (q, npc, src, action, emitted)
        if key in seen_rows:
            continue
        seen_rows.add(key)
        if family == "report" and action == "31" and emitted in (2375, 1352):
            kind = "npc_report" if emitted == 1011 else "report"
            plan.append({"quest_id": q, "npc": npc, "src": src, "action": action,
                         "emitted": emitted, "kind": kind})
        elif family == "start" and action == "31" and emitted == 1011:
            plan.append({"quest_id": q, "npc": npc, "src": src, "action": action,
                         "emitted": emitted, "kind": "npc_start"})
        elif family == "explicit" and not (action == "31" and emitted in (1011, 2375, 1352)):
            plan.append({"quest_id": q, "npc": npc, "src": src, "action": action,
                         "emitted": emitted, "kind": "explicit"})
    # attach client/contract context
    roots_cache: dict[int, tuple] = {}
    for item in plan:
        q = item["quest_id"]
        if q not in roots_cache:
            roots_cache[q] = chain_roots(q, pages, actions)
        start_root, report_root = roots_cache[q]
        item["start_root"] = start_root
        item["report_root"] = report_root
        item["html_has_page4"] = 4 in pages.get(q, {})
        contract = contracts.get(q)
        item["in_start"] = bool(contract and str(item["npc"]) in (contract["start_npc_ids"] or "").split())
        item["in_end"] = bool(contract and str(item["npc"]) in (contract["end_npc_ids"] or "").split())
        item["has_contract"] = contract is not None
        item["html_has_emitted"] = item["emitted"] in pages.get(q, {})
    return plan


def quest_xml_path(q: int) -> Path:
    return QUEST_DIR / f"{q}.xml"


def decide_report(item) -> str:
    return f'page="DEFAULT_SUCCESS"'


def main() -> int:
    args = parse_args()
    plan = collect(args.family)
    print(f"{args.family}: {len(plan)} rows to consider")

    edits: dict[Path, list[dict]] = defaultdict(list)
    blocked: list[dict] = []
    decided: set[tuple] = set()
    for item in plan:
        q, npc = item["quest_id"], item["npc"]
        path = quest_xml_path(q)
        root = ET.parse(path).getroot()
        decision = None
        if item["kind"] in ("npc_report", "report"):
            dialog = next((d for d in root.findall("./transitions/dialog")
                           if d.get("type") == "NPC_REPORT" and d.get("npc-id") == str(npc)
                           and d.get("page") in ("SELECT5", "SELECT2") and d.get("source") == item["src"]), None)
            explicit = None
            if dialog is None:
                for t in root.findall("./transitions/transition"):
                    if t.get("source") != item["src"]:
                        continue
                    events = [e for e in t.findall("./event/dialog")
                              if e.get("npc-id") == str(npc) and e.get("action") == "QUEST_SELECT"]
                    shows = [a for a in t.findall("./after-commit/dialog")
                             if a.get("page") in ("SELECT5", "SELECT2")]
                    if events and shows:
                        explicit = t
                        break
            if dialog is not None:
                decision = {"op": "set-report-page"}
            elif explicit is not None:
                decision = {"op": "set-explicit-report-page",
                            "old_page": explicit.findall("./after-commit/dialog")[0].get("page")}
            else:
                item["why"] = "no matching NPC_REPORT dialog or explicit route"; blocked.append(item)
                continue
        elif item["kind"] == "npc_start":
            dialog = next((d for d in root.findall("./transitions/dialog")
                           if d.get("type") == "NPC_START" and d.get("npc-id") == str(npc)
                           and d.get("start-page") == "SELECT1"), None)
            explicit = next((t for t in root.findall("./transitions/transition")
                             if any(e.get("npc-id") == str(npc) and e.get("action") == "QUEST_SELECT"
                                    for e in t.findall("./event/dialog"))
                             and any(a.get("page") == "SELECT1" for a in t.findall("./after-commit/dialog"))
                             and t.get("source") == item["src"]), None)
            if dialog is not None:
                if item["has_contract"] and not item["in_start"]:
                    decision = {"op": "remove-npc-start"}
                elif item["start_root"]:
                    symbol = page_symbols().get(item["start_root"])
                    if symbol is None:
                        item["why"] = f"start root {item['start_root']} not a typed symbol"; blocked.append(item)
                        continue
                    decision = {"op": "set-start-page", "symbol": symbol}
                elif item["html_has_page4"]:
                    # 客户端唯一 NONE 态入口是接取窗口页 4（传单/自动接取类任务）。
                    # The client's only NONE-state entry is the ask-accept window page 4
                    # (flyer/auto-accept quests).
                    decision = {"op": "set-start-page", "symbol": "SHOW_ASK_QUEST_ACCEPT_WINDOW"}
                else:
                    decision = {"op": "remove-npc-start"}
            elif explicit is not None:
                if item["has_contract"] and not item["in_start"]:
                    # 契约证明该 NPC 不是接取 NPC：删除无依据的接取入口。
                    # The contract proves this NPC is not a starter: drop the unfounded entry.
                    decision = {"op": "remove-explicit-start"}
                elif item["start_root"]:
                    symbol = page_symbols().get(item["start_root"])
                    if symbol is None:
                        item["why"] = "start root not a typed symbol"; blocked.append(item)
                        continue
                    decision = {"op": "set-explicit-start-page", "symbol": symbol}
                elif item["html_has_page4"]:
                    decision = {"op": "set-explicit-start-page", "symbol": "SHOW_ASK_QUEST_ACCEPT_WINDOW"}
                else:
                    decision = {"op": "remove-explicit-start"}
            else:
                item["why"] = "emitting start route not found"; blocked.append(item)
                continue
        else:  # explicit family
            decision = {"op": "inspect"}  # handled manually after review
        if decision["op"] == "inspect":
            item["why"] = "explicit family needs manual review"; blocked.append(item)
            continue
        item["path"] = str(path.relative_to(ROOT))
        item["decision"] = decision["op"]
        item["detail"] = {k: v for k, v in decision.items() if k != "op"}
        decided.add((q, npc))
        edits[path].append(item)

    # 别名行：同一路由在 repeatable 任务下会从多个状态触发审计；同 (quest, npc) 已有决策时跳过。
    # Alias rows: one route fires multiple audit rows across states in repeatable quests; skip
    # when the same (quest, npc) already has a decision.
    kept_blocked = []
    for item in blocked:
        if ((item["quest_id"], item["npc"]) in decided
                and item.get("why") == "emitting start route not found"):
            item["decision"] = "skip-alias"
        else:
            kept_blocked.append(item)
    blocked = kept_blocked

    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    with MANIFEST.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=[
            "quest_id", "npc", "src", "action", "emitted", "kind", "decision",
            "start_root", "report_root", "has_contract", "in_start", "in_end", "why", "detail", "path"])
        writer.writeheader()
        for item in plan:
            writer.writerow({k: item.get(k, "") for k in writer.fieldnames})
    print(f"plan={len(plan)} applied_groups={len(edits)} blocked={len(blocked)} manifest={MANIFEST}")

    if not args.write:
        return 0
    changed = 0
    for path, items in edits.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        updated = apply_edits(path, items, source)
        ET.fromstring(updated)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change: {path}")
        path.write_text(updated, encoding="utf-8")
        changed += len(items)
    print(f"changed rows={changed} files={len(edits)}")
    return 0


def apply_edits(path: Path, items, source: str) -> str:
    for item in items:
        npc = str(item["npc"])
        if item["decision"] == "set-report-page":
            # 仅替换该 NPC 的 NPC_REPORT page 属性
            pat = re.compile(r'(<dialog\b[^>]*type="NPC_REPORT"[^>]*npc-id="%s"[^>]*page=")(SELECT5|SELECT2)("[^>]*/>)' % re.escape(npc))
            new_source, count = pat.subn(r'\g<1>DEFAULT_SUCCESS\g<3>', source)
            if count == 0:
                raise RuntimeError(f"{path}: report dialog not found for npc {npc}")
            source = new_source
        elif item["decision"] == "set-explicit-report-page":
            old = item["detail"]["old_page"]
            removed = False
            for match in re.finditer(r'\n    <transition source="%s" target="[^"]*">.*?\n    </transition>' % re.escape(item["src"]), source, re.DOTALL):
                block = match.group(0)
                if f'npc-id="{npc}" action="QUEST_SELECT"' in block and f'page="{old}"' in block:
                    new_block = block.replace(f'page="{old}"', 'page="DEFAULT_SUCCESS"', 1)
                    source = source.replace(block, new_block, 1)
                    removed = True
                    break
            if not removed:
                raise RuntimeError(f"{path}: explicit report route not patched for npc {npc}")
        elif item["decision"] == "set-start-page":
            pat = re.compile(r'(<dialog\b[^>]*type="NPC_START"[^>]*npc-id="%s"[^>]*start-page=")SELECT1("[^>]*/>)' % re.escape(npc))
            new_source, count = pat.subn(r'\g<1>%s\g<2>' % item["detail"]["symbol"], source)
            if count == 0:
                raise RuntimeError(f"{path}: start dialog not found for npc {npc}")
            source = new_source
        elif item["decision"] == "remove-npc-start":
            pat = re.compile(r'\n    <dialog\b[^>]*type="NPC_START"[^>]*npc-id="%s"[^>]*start-page="SELECT1"[^>]*>\s*(?:<accept-actions>.*?</accept-actions>\s*)?</dialog>\n' % re.escape(npc), re.DOTALL)
            pat_selfclose = re.compile(r'\n    <dialog\b[^>]*type="NPC_START"[^>]*npc-id="%s"[^>]*start-page="SELECT1"[^>]*/>\n' % re.escape(npc))
            new_source, c1 = pat.subn("\n", source)
            if c1:
                source = new_source
            else:
                new_source, c2 = pat_selfclose.subn("\n", source)
                if not c2:
                    raise RuntimeError(f"{path}: npc-start not removed for {npc}")
                source = new_source
        elif item["decision"] == "set-explicit-start-page":
            # 显式 QUEST_SELECT -> SHOW SELECT1 路由改指向客户端链根/接取窗口页（按 source 区分块）。
            symbol = item["detail"]["symbol"]
            replaced = False
            for match in re.finditer(r'\n    <transition source="%s" target="[^"]*">.*?\n    </transition>' % re.escape(item["src"]), source, re.DOTALL):
                block = match.group(0)
                if f'npc-id="{npc}" action="QUEST_SELECT"' in block and 'page="SELECT1"' in block:
                    source = source.replace(block, block.replace('page="SELECT1"', f'page="{symbol}"', 1), 1)
                    replaced = True
                    break
            if not replaced:
                raise RuntimeError(f"{path}: explicit SELECT1 show not found for npc {npc}")
        elif item["decision"] == "remove-explicit-start":
            pat = re.compile(r'\n    <transition source="%s" target="%s">\n(?:.*?\n)*?    </transition>' % (re.escape(item["src"]), re.escape(item["src"])), re.DOTALL)
            # 精确匹配包含该 NPC QUEST_SELECT 与 SHOW SELECT1 的 transition
            blocks = pat.findall(source)
            removed = False
            for match in re.finditer(r'\n    <transition source="%s" target="%s">.*?\n    </transition>' % (re.escape(item["src"]), re.escape(item["src"])), source, re.DOTALL):
                block = match.group(0)
                if f'npc-id="{npc}" action="QUEST_SELECT"' in block and 'page="SELECT1"' in block:
                    source = source.replace(block, "", 1)
                    removed = True
                    break
            if not removed:
                raise RuntimeError(f"{path}: explicit start route not removed for npc {npc}")
    return source


if __name__ == "__main__":
    raise SystemExit(main())
