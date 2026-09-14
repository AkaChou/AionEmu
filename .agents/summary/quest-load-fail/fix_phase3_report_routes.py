#!/usr/bin/env python3
"""阶段 3：补零售报告协议路由，解决 select_success / select5 两族 CLIENT_PAGE_UNREACHED。

Phase 3: add the retail report-protocol routes for the select_success (report at REWARD)
and select5 (report at START) CLIENT_PAGE_UNREACHED families.

证据 / evidence:
  - 客户端 HTML：select_success(10002)/select5(2375) 是任务自身的报告页，带 1009 按钮。
  - origin/history 契约：data_driven/item_collecting/report_to 模板的 report_open_action=31、
    report_source_status=REWARD(或 START)、report_page=10002/2375。
  - 领奖 NPC：xml 中唯一 npc-complete 的 npc 即领奖/报告对象。
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

AUDIT = ROOT / ".agents/summary/quest-load-fail/quest-order-audit-current.csv"
MANIFEST = ROOT / ".agents/summary/quest-load-fail/phase3-report-manifest.csv"
PAGE_NAME_RE = re.compile(r"^(?P<source>[^#]+)#(?P<page>\S+?) page-order")

FAMILIES = {
    "reward_report": {
        "page": "select_success",
        "source": "reward",
        "show": "DEFAULT_SUCCESS",
    },
    "start_report": {
        "page": "select5",
        "source": "started",
        "show": "SELECT5",
    },
}


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--family", choices=sorted(FAMILIES), required=True)
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def npc_complete_npcs(root: ET.Element) -> list[int]:
    return sorted({int(d.get("npc-id")) for d in root.findall("./transitions/npc-complete")
                   if d.get("npc-id")})


def reward_labels(root: ET.Element) -> list[str]:
    return [n.get("label") for n in root.findall("./nodes/node") if n.get("status") == "REWARD"]


def started_labels(root: ET.Element) -> list[str]:
    return [n.get("label") for n in root.findall("./nodes/node") if n.get("status") == "START"]


def main() -> int:
    args = parse_args()
    spec = FAMILIES[args.family]
    pages, _ = client_model()
    contracts = load_contracts()
    rows = read_csv(AUDIT)
    edits: dict[Path, list[dict]] = defaultdict(list)
    manifest_rows = []
    for r in rows:
        if r["audit_status"] != "CLIENT_PAGE_UNREACHED":
            continue
        m = PAGE_NAME_RE.match(r["evidence_source"])
        if not m or m.group("page") != spec["page"]:
            continue
        q = int(r["quest_id"])
        path = quest_xml_path(q)
        root = ET.parse(path).getroot()
        if spec["page"] not in {info["name"] for info in
                                [{"name": name} for name in pages.get(q, {}).values()] if False}:
            # client_model 返回 {page_id: constant}，这里按常量名核对。
            pass
        html_constants = set(pages.get(q, {}).values())
        if spec["page"] == "select_success" and "DEFAULT_SUCCESS" not in html_constants:
            continue
        if spec["page"] == "select5" and "SELECT5" not in html_constants:
            continue
        source = spec["source"]
        source_labels = reward_labels(root) if source == "reward" else started_labels(root)
        if len(source_labels) != 1:
            manifest_rows.append({"quest_id": q, "decision": f"blocked: {source} nodes != 1"})
            continue
        label = source_labels[0]
        npcs = npc_complete_npcs(root)
        if len(npcs) != 1:
            # 多/零 npc-complete：契约 end_npc_ids 唯一或 item-report NPC 并入后唯一切换。
            # Multiple/zero npc-complete npcs: use unique contract end_npc_ids, or merge
            # npc-item-report npcs when that yields a single claimant.
            contract = contracts.get(q)
            end_npcs = sorted(int(x) for x in (contract["end_npc_ids"] or "").split()
                              if x and x != "0") if contract else []
            item_npcs = sorted({int(d.get("npc-id")) for d in root.findall("./transitions/npc-item-report")
                                if d.get("npc-id")})
            if len(end_npcs) == 1:
                npcs = set(end_npcs)
            elif len(item_npcs) == 1:
                npcs = set(item_npcs)
        if len(npcs) != 1:
            manifest_rows.append({"quest_id": q, "decision": "blocked: npc-complete npcs != 1"})
            continue
        npc = sorted(npcs)[0]
        # 已有同 (source, npc, QUEST_SELECT) 显式路由则跳过（幂等）。
        # Skip when an explicit route for the same (source, npc, QUEST_SELECT) already exists.
        exists = any(
            t.get("source") == label
            and any(e.get("npc-id") == str(npc) and e.get("action") == "QUEST_SELECT"
                    for e in t.findall("./event/dialog"))
            for t in root.findall("./transitions/transition"))
        if exists:
            manifest_rows.append({"quest_id": q, "decision": "skip: route exists", "npc": npc})
            continue
        manifest_rows.append({"quest_id": q, "npc": npc, "source": label,
                              "decision": f"add-{spec['source']}-report"})
        edits[path].append({"npc": npc, "label": label})

    with MANIFEST.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["quest_id", "npc", "source", "decision"])
        writer.writeheader()
        writer.writerows(manifest_rows)
    print(f"{args.family}: candidates={sum(1 for r2 in manifest_rows if r2['decision'].startswith('add'))} "
          f"files={len(edits)} manifest={MANIFEST}")
    if not args.write:
        return 0

    template = (
        '\n    <transition source="{label}" target="{label}">\n'
        '      <event>\n'
        '        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>\n'
        '      </event>\n'
        '      <after-commit>\n'
        '        <dialog type="SHOW_QUEST_PAGE" page="{show}"/>\n'
        '      </after-commit>\n'
        '    </transition>'
    )
    changed = 0
    for path, items in edits.items():
        before = path.read_bytes()
        updated = before.decode("utf-8")
        for item in items:
            updated = updated.replace(
                "</transitions>",
                template.format(label=item["label"], npc=item["npc"], show=spec["show"])
                + "</transitions>", 1)
        ET.fromstring(updated)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change: {path}")
        path.write_text(updated, encoding="utf-8")
        changed += len(items)
    print(f"changed rows={changed}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
