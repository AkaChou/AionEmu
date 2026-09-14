#!/usr/bin/env python3
"""阶段 3：按客户端按钮图补全故事翻页链（沿袭 2c32ae1ac 的既定模式）。

Phase 3: complete the story page-turn chains following the client button graph
(the established 2c32ae1ac pattern). Each inserted route is a pure page turn
(same source/target node, no state side effects); pages whose ids are absent
from the typed enum are skipped.
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
sys.path.insert(0, str(ROOT / ".agents/summary/quest-load-fail"))
from fix_notask_pages import client_model, page_symbols, quest_xml_path  # noqa: E402

AUDIT = ROOT / ".agents/summary/quest-load-fail/quest-order-audit-current.csv"
MANIFEST = ROOT / ".agents/summary/quest-load-fail/story-turns-manifest.csv"

PATH_RE = re.compile(r"^(?P<src>\S*) \+ (?P<owner>NPC (?P<npc>\d+)|QUEST_ACTION) \+ (?P<action>\S+) -> (?P<tgt>\S*) \+ page (?P<page>\d+)$")
# 协议接取页由 NPC_START 生成链负责，不属于故事翻页族。
# Protocol accept pages belong to the NPC_START generated chain, not story turns.
PROTOCOL_PAGES = {4, 1003, 1004}


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    pages, actions = client_model()
    symbols = page_symbols()
    rows = read_csv(AUDIT)

    # 已达页面：actual_path 以 `+ page <id>` 结尾的行证明该页由该触发显示。
    # Reached pages: rows whose actual_path ends with `+ page <id>` prove the page shown.
    reach: dict[tuple[int, int], set[tuple]] = defaultdict(set)
    for r in rows:
        m = PATH_RE.match(r["actual_path"])
        if not m or not m.group("npc"):
            continue
        reach[(int(r["quest_id"]), int(m.group("page")))].add(
            (int(m.group("npc")), m.group("tgt"), m.group("action")))

    # 客户端按钮图：action 常量 -> 同名页面。
    # Client button graph: action constant -> the page with the same name.
    by_name: dict[int, dict[str, int]] = {q: {name: pid for pid, name in plist.items()}
                                          for q, plist in pages.items()}
    candidates = []
    for r in rows:
        if r["audit_status"] != "CLIENT_PAGE_UNREACHED":
            continue
        em = re.match(r"^(?P<source>[^#]+)#(?P<page_name>\S+?) page-order", r["evidence_source"])
        if not em:
            continue
        q = int(r["quest_id"])
        page_name = em.group("page_name")
        # html_page_name(select3_1) -> 页面常量(SELECT3_1)。
        # html_page_name(select3_1) -> the page constant (SELECT3_1).
        if page_name.startswith("select"):
            pname_upper = "SELECT" + page_name.upper()[6:]
        else:
            pname_upper = page_name.upper()
        page_id = by_name.get(q, {}).get(pname_upper)
        if page_id is None or page_id in PROTOCOL_PAGES:
            continue
        page_symbol = symbols.get(page_id)
        if page_symbol is None:
            continue
        # 前驱页：按钮常量 == 本页常量名。
        # Predecessor pages: those whose button constant equals this page's constant.
        preds = []
        for pid, pa in actions.get(q, {}).items():
            for a in pa:
                target = by_name.get(q, {}).get(a)
                if target == page_id:
                    preds.append((pid, a))
        if not preds:
            continue
        for pid, action_name in preds:
            for (npc, tnode, _trigger_action) in sorted(reach.get((q, pid), set())):
                candidates.append({"quest_id": q, "npc": npc, "source": tnode,
                                   "action": action_name, "page_id": page_id,
                                   "page_symbol": page_symbol, "pred": pid})

    # 去重 + 幂等：已存在同 (source, npc, action) 显式路由则跳过。
    # Deduplicate and stay idempotent: skip when an explicit route already exists.
    seen = set()
    edits: dict[Path, list[dict]] = defaultdict(list)
    manifest_rows = []
    for c in candidates:
        key = (c["quest_id"], c["npc"], c["source"], c["action"])
        if key in seen:
            continue
        seen.add(key)
        path = quest_xml_path(c["quest_id"])
        text = path.read_text()
        if re.search(r'npc-id="%s" action="%s"' % (c["npc"], c["action"]), text):
            continue
        manifest_rows.append({**c, "decision": "add-turn"})
        edits[path].append(c)

    with MANIFEST.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["quest_id", "npc", "source", "action",
                                                    "page_id", "page_symbol", "pred", "decision"])
        writer.writeheader()
        writer.writerows(manifest_rows)
    print(f"candidates={len(manifest_rows)} files={len(edits)} manifest={MANIFEST}")
    if not args.write:
        return 0

    template = (
        '\n    <transition source="{source}" target="{source}">\n'
        '      <event>\n'
        '        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="{action}"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        '        <dialog type="SHOW_QUEST_PAGE" page="{page}"/>\n'
        '      </after-commit>\n'
        '    </transition>'
    )
    for path, items in edits.items():
        before = path.read_bytes()
        updated = before.decode("utf-8")
        for item in items:
            updated = updated.replace(
                "</transitions>",
                template.format(source=item["source"], npc=item["npc"],
                                action=item["action"], page=item["page_symbol"])
                + "</transitions>", 1)
        ET.fromstring(updated)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change: {path}")
        path.write_text(updated, encoding="utf-8")
    print(f"changed rows={len(edits)} files={len(edits)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
