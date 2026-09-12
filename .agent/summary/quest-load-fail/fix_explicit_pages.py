#!/usr/bin/env python3
"""阶段 2 收尾：显式路由 SHOW 的页面不在客户端 HTML 时，按规则改 close / 指回客户端页 / 删除。

Phase 2 finish: for explicit routes whose SHOW page is absent from the active client HTML,
either close the dialog, re-point to the client-evidenced page, or drop the stale route.
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
sys.path.insert(0, str(ROOT / ".agent/summary/quest-load-fail"))
from fix_notask_pages import client_model, quest_xml_path  # noqa: E402

AUDIT = ROOT / ".agent/summary/quest-load-fail/quest-order-audit-current.csv"
MANIFEST = ROOT / ".agent/summary/quest-load-fail/explicit-fix-manifest.csv"

PATH_RE = re.compile(r"^(?P<src>\S*) \+ (?P<owner>NPC \d+|QUEST_ACTION) \+ (?P<action>\S+) -> (?P<tgt>\S*) \+ page (?P<page>\d+)$")

# 事件 (action, shown_page) -> 修复动作。
# (action, shown_page) -> fix operation.
OPS = {
    ("1002", "1003"): "close",
    ("1003", "1004"): "close",
    ("20000", "1003"): "close",
    ("1352", "1352"): "close",
    ("1693", "1693"): "close",
    ("31", "1693"): {"op": "repoint", "symbol": "SELECT5"},
    ("39", "2120"): {"op": "repoint", "symbol": "CHECK_USER_ITEM_FAIL"},
    ("31", "4762"): "remove",
}


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


# 审计动作号 -> XML 事件动作符号（页名动作与页符号同名）。
# Audit action id -> XML event action symbol (page-named actions share the page symbol).
ACTION_SYMBOLS = {
    "31": ["QUEST_SELECT"],
    "39": ["CHECK_USER_HAS_QUEST_ITEM"],
    "1002": ["QUEST_ACCEPT_1"],
    "1003": ["QUEST_REFUSE_1"],
    "20000": ["QUEST_ACCEPT_SIMPLE"],
    "1352": ["SELECT2"],
    "1693": ["SELECT3"],
}


# 审计页号 -> XML 页符号。
# Audit page id -> XML page symbol.
PAGE_SYMBOLS = {
    "1003": "QUEST_ACCEPT_1",
    "1004": "QUEST_REFUSE_1",
    "1352": "SELECT2",
    "1693": "SELECT3",
    "2120": "SELECT4_2",
    "4762": "SELECT_NONE",
}


def find_block(source: str, item: dict) -> str | None:
    npc = str(item["npc"])
    symbols = ACTION_SYMBOLS.get(item["action"], [item["action"]])
    page_symbol = PAGE_SYMBOLS.get(item["show_page"], item["show_page"])

    def scan(pattern: str) -> str | None:
        for match in re.finditer(pattern, source, re.DOTALL):
            block = match.group(0)
            for symbol in symbols:
                event_match = (f'npc-id="{npc}" action="{symbol}"' if item["npc"]
                               else f'action="{symbol}"')
                if event_match in block and f'page="{page_symbol}"' in block:
                    return block
        return None

    # 先按声明的 source 匹配；repeatable 别名行再全局回退；缩进不敏感。
    # Match the declared source first; repeat-alias rows fall back to a global scan;
    # indentation-insensitive.
    block = scan(r'\n[ \t]*<transition source="%s" target="[^"]*"[^>]*>.*?\n[ \t]*</transition>' % re.escape(item["src"]))
    return block or scan(r'\n[ \t]*<transition source="[^"]*" target="[^"]*"[^>]*>.*?\n[ \t]*</transition>')



OVERRIDE_TMPL = (
    "\n    <transition source=\"{src}\" target=\"{target}\">\n"
    "      <event>\n"
    "        <dialog type=\"TALK_TO_NPC\" npc-id=\"{npc}\" action=\"{action_symbol}\"/>\n"
    "      </event>\n"
    "      <conditions>\n"
    "        <start-eligible/>\n"
    "      </conditions>\n"
    "      <after-commit>\n"
    "{extra}"
    "        <close-dialog/>\n"
    "      </after-commit>\n"
    "    </transition>"
)


def add_override(source: str, item: dict, decision: str) -> str:
    """为 NPC_START 生成路由补显式覆盖：accept 保留接取状态变更，refuse 原地关闭。

    Adds an explicit override for a generated NPC_START route: accept keeps the state
    change, refuse stays in place; both close instead of showing a missing page.
    """
    action_symbol = ACTION_SYMBOLS[item["action"]][0]
    root = ET.fromstring(source)
    target = item["src"]
    extra = ""
    if item["action"] == "1002":
        dialog = next(d for d in root.findall("./transitions/dialog")
                      if d.get("type") == "NPC_START" and d.get("npc-id") == str(item["npc"]))
        target = dialog.get("target")
        extra = '        <sync-quest-state mode="VISIBILITY_REFRESH"/>\n'
    block = OVERRIDE_TMPL.format(src=item["src"], target=target, npc=item["npc"],
                                 action_symbol=action_symbol, extra=extra)
    return source.replace("</transitions>", block + "</transitions>", 1)


def main() -> int:
    args = parse_args()
    pages, _ = client_model()
    rows = read_csv(AUDIT)
    edits: dict[Path, list[dict]] = defaultdict(list)
    manifest_rows = []
    seen: set[tuple] = set()
    for r in rows:
        if (r["audit_status"] != "EVIDENCE_REQUIRED"):
            continue
        m = PATH_RE.match(r["actual_path"])
        if not m:
            continue
        page_reason = r["unresolved_reason"].startswith("compiled IR emits")
        button_reason = r["unresolved_reason"].startswith("visible client action has no route")
        if page_reason:
            action, page_id = m.group("action"), m.group("page")
        elif (button_reason and m.group("action") == "31" and m.group("page") == "4762"
                and r["client_visible_action"] == "1007"):
            # 旧客户端简报页（1011）的 started 态残留：5.8 已无对应页，删除整条路由。
            # Stale started-state briefing route from the old client page 1011: no 5.8 page
            # exists, so drop the route (dialog closes by default).
            action, page_id = m.group("action"), m.group("page")
            key = (r["quest_id"], r["npc_id"], m.group("src"), action, page_id)
            if key in seen:
                continue
            seen.add(key)
            q = int(r["quest_id"])
            item = {"quest_id": q, "src": m.group("src"), "npc": int(r["npc_id"] or 0),
                    "action": action, "show_page": "SELECT_NONE"}
            manifest_rows.append({**item, "decision": "remove"})
            edits[quest_xml_path(q)].append(item)
            continue
        else:
            continue
        action, page_id = m.group("action"), m.group("page")
        key = (r["quest_id"], r["npc_id"], m.group("src"), action, page_id)
        if key in seen:
            continue
        seen.add(key)
        op = OPS.get((action, page_id))
        if op is None:
            manifest_rows.append({"quest_id": r["quest_id"], "src": m.group("src"), "npc": r["npc_id"],
                                  "action": action, "show_page": page_id, "decision": "blocked: no rule"})
            continue
        q = int(r["quest_id"])
        item = {"quest_id": q, "src": m.group("src"), "npc": int(r["npc_id"] or 0),
                "action": action, "show_page": page_id}
        if op == "close":
            # 客户端没有该响应页：保留条件与事务动作，响应改为关闭对话。
            # The client has no such response page: keep conditions/actions and close instead.
            decision = "close"
        elif isinstance(op, dict) and op["op"] == "repoint":
            decision = f"repoint:{op['symbol']}"
        else:
            decision = "remove"
        manifest_rows.append({**item, "decision": decision})
        edits[quest_xml_path(q)].append(item)

    with MANIFEST.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["quest_id", "src", "npc", "action", "show_page", "decision"])
        writer.writeheader()
        writer.writerows(manifest_rows)
    print(f"candidates={len(manifest_rows)} files={len(edits)} manifest={MANIFEST}")
    if not args.write:
        return 0

    changed = 0
    for path, items in edits.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        for item in items:
            decision = next(row["decision"] for row in manifest_rows
                            if row["quest_id"] == item["quest_id"]
                            and row["src"] == item["src"] and row["action"] == item["action"]
                            and row["show_page"] == item["show_page"])
            block = find_block(source, item)
            if block is None:
                root = ET.fromstring(source)
                has_generated = any(
                    d.get("type") == "NPC_START" and d.get("npc-id") == str(item["npc"])
                    and d.get("source") == item["src"]
                    for d in root.findall("./transitions/dialog"))
                if not has_generated:
                    # 幂等重跑：前次部分写入已修复；审计结果兜底验证。
                    # Idempotent re-run: a prior partial write already fixed this row;
                    # the audit verifies the outcome.
                    print(f"skip (already applied): {path.name} {item}")
                    continue
                if item["action"] in ("1002", "1003"):
                    # 生成式 NPC_START 路由：用显式路由覆盖，保留状态变更，响应改为关闭。
                    # Generated NPC_START route: override explicitly, keep the state change
                    # and close instead of showing the page the client lacks.
                    source = add_override(source, item, decision)
                    continue
                print(f"skip (already applied or not found): {path.name} {item}")
                continue
            page_symbol = PAGE_SYMBOLS.get(item["show_page"], item["show_page"])
            if decision == "close":
                new_block = block.replace(
                    f'<dialog type="SHOW_QUEST_PAGE" page="{page_symbol}"/>',
                    "<close-dialog/>", 1)
            elif decision.startswith("repoint:"):
                symbol = decision.split(":", 1)[1]
                new_block = block.replace(
                    f'<dialog type="SHOW_QUEST_PAGE" page="{page_symbol}"/>',
                    f'<dialog type="SHOW_QUEST_PAGE" page="{symbol}"/>', 1)
            else:
                new_block = ""
            source = source.replace(block, new_block, 1)
        ET.fromstring(source)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change: {path}")
        path.write_text(source, encoding="utf-8")
        changed += len(items)
    print(f"changed rows={changed}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
