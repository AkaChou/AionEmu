#!/usr/bin/env python3
"""阶段 2 跟进：报告页修正后的两类补丁。

Phase 2 follow-ups after the report-page fix:
  turnin : 报告页(10002)可见动作 1009 缺上交路由时补最小 npc-report 上交路由。
  repoint: 契约报告页(SELECT5/SELECT2)存在于客户端 HTML 时，把误改为 DEFAULT_SUCCESS 的页指回契约页。
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
from fix_notask_pages import client_model, load_contracts, quest_xml_path  # noqa: E402

AUDIT = Path("/tmp/qa-report.csv")
MANIFEST = ROOT / ".agent/summary/quest-load-fail/quest-order-audit-current.csv"
OUT = ROOT / ".agent/summary/quest-load-fail/report-turnin-manifest.csv"

PATH_RE = re.compile(r"^(?P<src>\S*) \+ (?P<owner>NPC \d+|QUEST_ACTION) \+ (?P<action>\S+) -> (?P<tgt>\S*) \+ page (?P<page>\d+)$")
TURNIN_TMPL = (
    "\n    <transition source=\"{src}\" target=\"{target}\">\n"
    "      <event>\n"
    "        <dialog type=\"TALK_TO_NPC\" npc-id=\"{npc}\" action=\"SELECT_QUEST_REWARD\"/>\n"
    "      </event>\n"
    "      <after-commit>\n"
    "        <sync-quest-state mode=\"LEVEL_AND_VISIBILITY_REFRESH\"/>\n"
    "        <dialog type=\"SHOW_QUEST_PAGE\" page=\"SHOW_SELECT_QUEST_REWARD_WINDOW1\"/>\n"
    "      </after-commit>\n"
    "    </transition>"
)


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--family", choices=("turnin", "repoint"), required=True)
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def reward_node(root: ET.Element) -> str | None:
    labels = [node.get("label") for node in root.findall("./nodes/node")
              if node.get("status") == "REWARD"]
    return labels[0] if len(labels) == 1 else None


def main() -> int:
    args = parse_args()
    pages, actions = client_model()
    contracts = load_contracts()
    edits: dict[Path, list[dict]] = defaultdict(list)
    manifest_rows = []

    if args.family == "turnin":
        rows = read_csv(AUDIT)
        seen: set[tuple] = set()
        for r in rows:
            if (r["audit_status"] != "EVIDENCE_REQUIRED"
                    or not r["unresolved_reason"].startswith("visible client action has no route")
                    or r["shown_page"] != "10002" or r["client_visible_action"] != "1009"):
                continue
            q, npc = int(r["quest_id"]), int(r["npc_id"])
            m = PATH_RE.match(r["actual_path"])
            src = m.group("src") if m else r["server_source_state"]
            key = (q, npc, src)
            if key in seen:
                continue
            seen.add(key)
            path = quest_xml_path(q)
            root = ET.parse(path).getroot()
            has_route = any(
                t.get("source") == src
                and any(e.get("npc-id") == str(npc) and e.get("action") == "SELECT_QUEST_REWARD"
                        for e in t.findall("./event/dialog"))
                for t in root.findall("./transitions/transition"))
            if has_route:
                continue
            target = reward_node(root)
            if target is None:
                manifest_rows.append({"quest_id": q, "npc": npc, "src": src,
                                      "decision": "blocked: reward node not unique"})
                continue
            manifest_rows.append({"quest_id": q, "npc": npc, "src": src,
                                  "decision": "add-turnin", "target": target})
            edits[path].append(manifest_rows[-1])

    else:  # repoint
        for r in read_csv(MANIFEST):
            if not r["unresolved_reason"].startswith("compiled IR emits a task page absent"):
                continue
            m = PATH_RE.match(r["actual_path"])
            if not (m and m.group("action") == "31" and m.group("page") in ("2375", "1352")):
                continue
            q, npc = int(r["quest_id"]), int(r["npc_id"])
            contract = contracts.get(q)
            symbol = None
            if contract:
                report_page_id = int(contract["report_page_id"] or 0)
                if report_page_id in pages.get(q, {}):
                    symbol = {2375: "SELECT5", 1352: "SELECT2"}.get(report_page_id)
            else:
                # 无契约时以客户端 HTML 为准：HTML 存在带报告按钮的 select5/select2 页则指回。
                # Without a contract the client HTML wins: re-point when the HTML itself contains
                # a select5/select2 page carrying the report buttons.
                report_buttons = {"CHECK_USER_HAS_QUEST_ITEM", "SELECT_QUEST_REWARD"}
                if 2375 in pages.get(q, {}) and actions.get(q, {}).get(2375, set()) & report_buttons:
                    symbol = "SELECT5"
                elif 1352 in pages.get(q, {}) and actions.get(q, {}).get(1352, set()) & report_buttons:
                    symbol = "SELECT2"
            if not symbol:
                continue
            path = quest_xml_path(q)
            root = ET.parse(path).getroot()
            # 已被上一批改为 DEFAULT_SUCCESS 的报告页才需要指回。
            # Only routes previously flipped to DEFAULT_SUCCESS need re-pointing.
            is_default = any(
                (d.get("page") == "DEFAULT_SUCCESS")
                or any(a.get("page") == "DEFAULT_SUCCESS" for a in t.findall("./after-commit/dialog"))
                for d, t in [(d, None) for d in root.findall("./transitions/dialog")
                             if d.get("type") == "NPC_REPORT" and d.get("npc-id") == str(npc)]
            )
            explicit_default = any(
                t.get("source") == r["server_source_state"]
                and any(e.get("npc-id") == str(npc) and e.get("action") == "QUEST_SELECT"
                        for e in t.findall("./event/dialog"))
                and any(a.get("page") == "DEFAULT_SUCCESS" for a in t.findall("./after-commit/dialog"))
                for t in root.findall("./transitions/transition"))
            if not (is_default or explicit_default):
                continue
            manifest_rows.append({"quest_id": q, "npc": npc, "src": r["server_source_state"],
                                  "decision": "repoint", "symbol": symbol,
                                  "explicit": bool(explicit_default and not is_default)})
            edits[path].append(manifest_rows[-1])

    with OUT.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["quest_id", "npc", "src", "decision", "target", "symbol", "explicit"])
        writer.writeheader()
        writer.writerows(manifest_rows)
    print(f"{args.family}: candidates={len(manifest_rows)} files={len(edits)} manifest={OUT}")
    if not args.write:
        return 0

    changed = 0
    for path, items in edits.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        for item in items:
            npc = str(item["npc"])
            if item["decision"] == "add-turnin":
                source = source.replace("</transitions>",
                                        TURNIN_TMPL.format(src=item["src"], target=item["target"], npc=npc)
                                        + "</transitions>", 1)
            elif item["decision"] == "repoint":
                if item.get("explicit"):
                    replaced = False
                    for match in re.finditer(r'\n    <transition source="%s" target="[^"]*">.*?\n    </transition>' % re.escape(item["src"]), source, re.DOTALL):
                        block = match.group(0)
                        if f'npc-id="{npc}" action="QUEST_SELECT"' in block and 'page="DEFAULT_SUCCESS"' in block:
                            source = source.replace(block, block.replace('page="DEFAULT_SUCCESS"', f'page="{item["symbol"]}"', 1), 1)
                            replaced = True
                            break
                    if not replaced:
                        raise RuntimeError(f"{path}: explicit report route not found for npc {npc}")
                else:
                    pat = re.compile(r'(<dialog\b[^>]*type="NPC_REPORT"[^>]*npc-id="%s"[^>]*page=")DEFAULT_SUCCESS("[^>]*/>)' % re.escape(npc))
                    source, count = pat.subn(r'\g<1>%s\g<2>' % item["symbol"], source)
                    if not count:
                        raise RuntimeError(f"{path}: NPC_REPORT not found for npc {npc}")
        ET.fromstring(source)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change: {path}")
        path.write_text(source, encoding="utf-8")
        changed += len(items)
    print(f"changed rows={changed}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
