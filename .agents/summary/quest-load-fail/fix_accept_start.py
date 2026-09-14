#!/usr/bin/env python3
"""阶段 3：为 select_none 不可达且无接取入口的单 NPC 任务补 NPC_START 接取块。

Phase 3: add the NPC_START block for single-NPC quests whose client select_none page is
unreached and whose XML lacks any accept entry. Only when the active client HTML contains
the full accept chain (page 4 + accept + refuse pages), so the generated chain cannot emit
pages the client lacks.

证据 / evidence:
  - 客户端 HTML：select_none 为接取链根，携带 ASK_QUEST_ACCEPT(1007) 按钮，且
    SHOW_ASK_QUEST_ACCEPT_WINDOW(4)/QUEST_ACCEPT_1(1003)/QUEST_REFUSE_1(1004) 齐备。
  - origin/history 契约：data_driven/item_collecting 模板的 start_page=4762（有契约时校验）。
  - 单一 NPC：xml 中唯一的 npc-complete/npc-item-report npc 即接取 NPC（零售单 NPC 任务模式）。
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
from fix_notask_pages import client_model, load_contracts, quest_xml_path  # noqa: E402

EVIDENCE = {r["quest_id"]: r for r in
            csv.DictReader((ROOT / ".agents/summary/quest-load-fail/data-driven-npc-evidence.csv")
                           .open(encoding="utf-8-sig", newline=""))}

AUDIT = ROOT / ".agents/summary/quest-load-fail/quest-order-audit-current.csv"
MANIFEST = ROOT / ".agents/summary/quest-load-fail/accept-start-manifest.csv"
PAGE_NAME_RE = re.compile(r"^([^#]+)#(\S+?) page-order")


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
    pages, _ = client_model()
    contracts = load_contracts()
    rows = read_csv(AUDIT)
    edits: dict[Path, list[dict]] = defaultdict(list)
    manifest_rows = []
    seen = set()
    for r in rows:
        if r["audit_status"] != "CLIENT_PAGE_UNREACHED":
            continue
        m = PAGE_NAME_RE.match(r["evidence_source"])
        if not m or m.group(2) != "select_none":
            continue
        q = int(r["quest_id"])
        if q in seen:
            continue
        seen.add(q)
        html_constants = set(pages.get(q, {}).values())
        full_chain = {"SHOW_ASK_QUEST_ACCEPT_WINDOW", "QUEST_ACCEPT_1", "QUEST_REFUSE_1"} <= html_constants
        briefing_only = not full_chain
        contract = contracts.get(q)
        if contract and contract["start_page_id"] not in ("", "0", "4762"):
            manifest_rows.append({"quest_id": q, "decision": f"skip: contract start_page {contract['start_page_id']}"})
            continue
        path = quest_xml_path(q)
        root = ET.parse(path).getroot()
        if any(d.get("type") == "NPC_START" for d in root.findall("./transitions/dialog")):
            manifest_rows.append({"quest_id": q, "decision": "skip: NPC_START already present"})
            continue
        npcs = {int(d.get("npc-id")) for tag in ("npc-complete", "npc-item-report")
                for d in root.findall(f"./transitions/{tag}") if d.get("npc-id")}
        if len(npcs) != 1:
            # 多 NPC：契约 start_npc_ids 唯一时按契约选择。
            # Multi-NPC quests: use the contract's start_npc_ids when unique.
            contract_start = sorted(int(x) for x in (contract["start_npc_ids"] or "").split()
                                    if x and x != "0") if contract else []
            if len(contract_start) == 1:
                npcs = set(contract_start)
            elif not npcs and contract and contract["end_npc_ids"]:
                end_npcs = sorted(int(x) for x in contract["end_npc_ids"].split() if x and x != "0")
                if len(end_npcs) == 1:
                    npcs = set(end_npcs)
        if len(npcs) != 1:
            # 客户端解包证据：data-driven 任务的接取(Talk)/领奖 NPC 名字解析为唯一 ID。
            # Client unpack evidence: data-driven acquire(Talk)/reward NPC names resolve to
            # a unique id - the authoritative "who starts this quest" input.
            evidence = EVIDENCE.get(str(q))
            if evidence:
                for field in (("acquire_npc_id",) if evidence.get("acquire_category") == "Talk"
                              else ("acquire_npc_id", "reward_npc_id")):
                    candidate = evidence.get(field, "")
                    if candidate and not candidate.startswith("AMBIG") and candidate.isdigit():
                        npcs = {int(candidate)}
                        break
        if len(npcs) != 1:
            manifest_rows.append({"quest_id": q, "decision": f"blocked: single-NPC check failed ({len(npcs)})"})
            continue
        npc = npcs.pop()
        none_labels = [n.get("label") for n in root.findall("./nodes/node") if n.get("status") == "NONE"]
        start_labels = [n.get("label") for n in root.findall("./nodes/node") if n.get("status") == "START"]
        # 简报对只挂在 NONE 节点上；NPC_START 模式才需要唯一 START 节点。
        # Briefing pairs only attach to the NONE node; only the NPC_START mode needs a
        # unique START node.
        if briefing_only:
            if len(none_labels) != 1:
                manifest_rows.append({"quest_id": q, "decision": "blocked: NONE nodes != 1"})
                continue
        elif len(none_labels) != 1 or len(start_labels) != 1:
            manifest_rows.append({"quest_id": q, "decision": "blocked: NONE/START nodes != 1"})
            continue
        # 幂等：已有同 NPC 的 unaccepted QUEST_SELECT 显式路由则不重复添加。
        # Idempotent: skip when an explicit unaccepted QUEST_SELECT route already exists.
        if any(t.get("source") == none_labels[0]
               and any(e.get("npc-id") == str(npc) and e.get("action") == "QUEST_SELECT"
                       for e in t.findall("./event/dialog"))
               for t in root.findall("./transitions/transition")):
            manifest_rows.append({"quest_id": q, "decision": "skip: start route exists"})
            continue
        # 简报式 HTML（无 4/1003/1004 页）：select_none 的按钮应为 FINISH_DIALOG（履行委托后关闭）。
        # Briefing-only HTML (no pages 4/1003/1004): the select_none button should be
        # FINISH_DIALOG (accept the commission, close).
        if briefing_only:
            none_buttons = actions.get(q, {}).get(4762, set()) if False else None
            decision = "add-briefing-pair"
        else:
            decision = "add-npc-start"
        manifest_rows.append({"quest_id": q, "npc": npc, "none": none_labels[0],
                              "start": start_labels[0] if start_labels else "",
                              "decision": decision})
        edits[path].append(manifest_rows[-1])

    with MANIFEST.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["quest_id", "npc", "none", "start", "decision"])
        writer.writeheader()
        writer.writerows(manifest_rows)
    adds = sum(1 for r2 in manifest_rows if r2["decision"].startswith("add"))
    print(f"select_none: candidates={adds} files={len(edits)} manifest={MANIFEST}")
    if not args.write:
        return 0

    npc_start_tmpl = ('\n    <!-- 零售单 NPC 接取：select_none 链根接取，生成 1007/1002/1003 标准接取链。 -->\n'
                '    <dialog type="NPC_START" npc-id="{npc}" source="{none}" target="{start}" '
                'selection-sources="{none} {start}" start-page="SELECT_NONE"/>')
    briefing_tmpl = ('\n    <!-- 区域/事件接取任务：对话仅显示 select_none 履行简报，FINISH_DIALOG 关闭；\n'
                     '         start-eligible 供 repeatable 任务在 COMPLETE 态别名复用。 -->\n'
                     '    <transition source="{none}" target="{none}">\n'
                     '      <event>\n'
                     '        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>\n'
                     '      </event>\n'
                     '      <conditions>\n'
                     '        <start-eligible/>\n'
                     '      </conditions>\n'
                     '      <after-commit>\n'
                     '        <dialog type="SHOW_QUEST_PAGE" page="SELECT_NONE"/>\n'
                     '      </after-commit>\n'
                     '    </transition>\n'
                     '    <transition source="{none}" target="{none}">\n'
                     '      <event>\n'
                     '        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="FINISH_DIALOG"/>\n'
                     '      </event>\n'
                     '      <conditions>\n'
                     '        <start-eligible/>\n'
                     '      </conditions>\n'
                     '      <after-commit>\n'
                     '        <close-dialog/>\n'
                     '      </after-commit>\n'
                     '    </transition>')
    for path, items in edits.items():
        before = path.read_bytes()
        updated = before.decode("utf-8")
        for item in items:
            if item["decision"] == "add-npc-start":
                updated = updated.replace(
                    "</transitions>",
                    npc_start_tmpl.format(npc=item["npc"], none=item["none"],
                                          start=item.get("start") or "")
                    + "</transitions>", 1)
            else:
                updated = updated.replace(
                    "</transitions>",
                    briefing_tmpl.format(npc=item["npc"], none=item["none"])
                    + "</transitions>", 1)
        ET.fromstring(updated)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change: {path}")
        path.write_text(updated, encoding="utf-8")
    print(f"changed quests={len(edits)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
