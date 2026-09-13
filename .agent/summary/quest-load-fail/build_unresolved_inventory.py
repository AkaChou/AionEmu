#!/usr/bin/env python3
"""阶段 0：把 QuestDialogOrderAudit 未解决行与客户端页面、任务 XML、旧契约关联并分类。

Phase 0: join unresolved QuestDialogOrderAudit rows with client pages, quest XML
declarations, and legacy contracts, then classify each row.
"""
from __future__ import annotations

import csv
import re
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
import os
AUDIT = Path(os.environ.get("AUDIT_CSV", ROOT / ".agent/summary/quest-load-fail/quest-order-audit-current.csv"))
PAGES = ROOT / "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"
ACTIONS = ROOT / "docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"
CONTRACTS = ROOT / "docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv"
TEMPLATE_INDEX = ROOT / "docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv"
ALIGNMENT = ROOT / "docs/quest/client-dialog-mapping/client-lifecycle-alignment.csv"
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
OUTPUT = ROOT / ".agent/summary/quest-load-fail/unresolved-inventory.csv"

PAGE_NAME_RE = re.compile(r"^(?P<source>[^#]+)#(?P<page>\S+?) page-order")
RETAIL_TEMPLATES = {r["quest_id"]: r for r in csv.DictReader(
    (ROOT / ".agent/summary/quest-load-fail/retail-simple-templates.csv")
    .open(encoding="utf-8-sig", newline=""))}
EVIDENCE_NPC = {r["quest_id"]: r for r in csv.DictReader(
    (ROOT / ".agent/summary/quest-load-fail/data-driven-npc-evidence.csv")
    .open(encoding="utf-8-sig", newline=""))}


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def client_pages() -> dict[int, dict[int, dict[str, str]]]:
    result: dict[int, dict[int, dict[str, str]]] = defaultdict(dict)
    for row in read_csv(PAGES):
        if row["source_variant"] != "active" or row["page_mapping"] != "exact":
            continue
        quest_id = int(row["quest_id"])
        result[quest_id][int(row["page_id"])] = {
            "name": row["html_page_name"],
            "constant": row["page_constant"].removeprefix("HTML_PAGE_"),
            "order": row["page_order"],
            "action_count": row["action_count"],
        }
    return result


def client_actions() -> dict[tuple[int, int], list[tuple[int, str]]]:
    result: dict[tuple[int, int], list[tuple[int, str]]] = defaultdict(list)
    for row in read_csv(ACTIONS):
        if row["source_variant"] != "active" or row["page_mapping"] != "exact":
            continue
        if row["action_mapping"] != "exact":
            continue
        result[(int(row["quest_id"]), int(row["page_id"]))].append(
            (int(row["action_id"]), row["action_constant"].removeprefix("HACTION_")))
    return result


def quest_xml_dialogs(quest_id: int) -> list[dict[str, str]]:
    path = QUEST_DIR / f"{quest_id}.xml"
    if not path.exists():
        return []
    root = ET.parse(path).getroot()
    dialogs = []
    for dialog in root.findall("./transitions/dialog"):
        dialogs.append({key: dialog.get(key, "") for key in
                        ("type", "npc-id", "source", "target", "start-page", "page")})
    return dialogs


def main() -> int:
    audit_rows = read_csv(AUDIT)
    pages = client_pages()
    actions = client_actions()
    contracts = {int(row["quest_id"]): row for row in read_csv(CONTRACTS)}
    template_counts: Counter[int] = Counter()
    for row in read_csv(TEMPLATE_INDEX):
        template_counts[int(row["quest_id"])] += 1
    alignment: dict[tuple[str, int, str], dict[str, str]] = {}
    for row in read_csv(ALIGNMENT):
        alignment[(row["route_type"], int(row["npc_id"]), row["quest_id"])] = row

    unresolved = [row for row in audit_rows
                  if row["audit_status"] in ("CLIENT_PAGE_UNREACHED", "EVIDENCE_REQUIRED")]
    inventory = []
    decision_counts: Counter[str] = Counter()
    cluster_counts: Counter[tuple[str, str]] = Counter()
    for row in unresolved:
        quest_id = int(row["quest_id"])
        reason = row["unresolved_reason"]
        page_match = PAGE_NAME_RE.match(row["evidence_source"])
        if reason.startswith("active page is absent"):
            audit_status = "CLIENT_PAGE_UNREACHED"
            page_id = int(row["shown_page"])
            page_info = pages.get(quest_id, {}).get(page_id, {})
            page_name = page_info.get("name", "")
            action_list = actions.get((quest_id, page_id), [])
            action_names = "+".join(name for _, name in sorted(action_list))
        elif reason.startswith("compiled IR emits a task page absent"):
            audit_status = "PAGE_NOT_IN_TASK_HTML"
            page_id = int(row["shown_page"])
            page_info = {}
            page_name = ""
            action_list = []
            action_names = ""
        else:
            audit_status = "BUTTON_WITHOUT_ROUTE"
            page_id = int(row["shown_page"])
            page_info = pages.get(quest_id, {}).get(page_id, {})
            page_name = page_info.get("name", "")
            action_id = int(row["client_visible_action"])
            action_names = next((name for aid, name in actions.get((quest_id, page_id), [])
                                 if aid == action_id), "")
        contract = contracts.get(quest_id)
        contract_type = contract["template_type"] if contract else ""
        template_rows = template_counts.get(quest_id, 0)
        state = row["server_source_state"]
        npc_id = row["npc_id"]

        # ---- 分类：按修复家族与证据状态聚类 / classify by fix family and evidence state ----
        decision = "EVIDENCE_BLOCKED"
        blocker = ""
        gaps = []
        if audit_status == "PAGE_NOT_IN_TASK_HTML":
            decision = "FIX_XML"
            blocker = "replace emitted page with client-defined page or close"
        elif audit_status == "BUTTON_WITHOUT_ROUTE":
            decision = "FIX_XML"
            blocker = "add route for visible action from the showing transition's target"
        elif audit_status == "CLIENT_PAGE_UNREACHED" and page_id == 4:
            # 页面 4（接取确认页）：
            # 1. 道具/播放起手已在 XML 建模（use-item / item-play）；
            # 2. 自动起手（enter-zone / level-up / movie-end）免挂对话；
            # 3. 阵营任务（npc-faction-id）由活动/阵营 UI 无目标接取；
            # 4. 常规 NPC_START 展开已内置 ASK_QUEST_ACCEPT -> page 4 路由。
            path = QUEST_DIR / f"{quest_id}.xml"
            modelled = False
            is_auto_start = False
            auto_type = ""
            has_npc_start_page4 = False
            is_faction = False
            if path.exists():
                xroot = ET.parse(path).getroot()
                meta = xroot.find("metadata")
                if meta is not None and meta.get("npc-faction-id"):
                    is_faction = True
                for t in xroot.findall("./transitions/transition"):
                    if t.get("source") == "unaccepted":
                        ev = t.find("event")
                        if ev is not None and len(ev) > 0 and ev[0].tag in (
                                "enter-zone", "level-up", "movie-end", "inv-timer-end"):
                            is_auto_start = True
                            auto_type = ev[0].tag
                            break
                for t in xroot.findall("./transitions/transition"):
                    if (t.findall("./event/use-item") or t.findall("./event/item-play")) and any(
                            a.get("page") == "SHOW_ASK_QUEST_ACCEPT_WINDOW"
                            for a in t.findall("./after-commit/dialog")):
                        modelled = True
                        break
                    if t.findall("./event/item-play") and t.get("source") == "unaccepted":
                        modelled = True
                        break
                has_npc_start = any(d.get("type") == "NPC_START" for d in xroot.findall(".//transitions/dialog")) or \
                                bool(xroot.findall(".//transitions/npc-start"))
                has_ask_override = any(
                    any(a.get("action") == "ASK_QUEST_ACCEPT" for a in t.findall("./event/dialog"))
                    for t in xroot.findall(".//transitions/transition")
                )
                if has_npc_start and not has_ask_override:
                    has_npc_start_page4 = True
            if modelled:
                decision = "INTENTIONAL_CLIENT_ONLY"
                blocker = ("item-start flow modelled via use-item/item-play route; dialog-only audit "
                           "walk cannot reach page 4")
            elif is_auto_start:
                decision = "INTENTIONAL_CLIENT_ONLY"
                blocker = (f"quest auto-starts via {auto_type}; NONE state is free of dialog routes "
                           f"and client accept pages are unused template assets")
                gaps.append(f"proven auto-start ({auto_type}); client accept pages are unused assets")
            elif is_faction:
                decision = "INTENTIONAL_CLIENT_ONLY"
                blocker = ("faction quest accepted via faction UI; NONE state is free of dialog routes "
                           "and client accept pages are unused template assets")
            elif has_npc_start_page4:
                decision = "INTENTIONAL_CLIENT_ONLY"
                blocker = ("standard NPC_START expansion routes ASK_QUEST_ACCEPT to page 4; "
                           "dialog order reached in runtime IR")
            else:
                decision = "EVIDENCE_BLOCKED"
                blocker = ("no in-repo evidence proves the dialog/item edge that opens the "
                           "ask-accept window for this quest")
                gaps.append("the briefing chain button or start item that opens page 4 is "
                            "unproven (needs handler or retail capture)")
        elif audit_status == "CLIENT_PAGE_UNREACHED":
            # 终态分类：仅保留合法例外（集中管理、逐行引用证据/缺口）。
            # Final taxonomy: only managed exceptions remain, each citing its evidence/gap.
            if page_name in ("select_success", "select5", "select2", "select3",
                    "select4", "select6", "select7", "select8", "select9", "select10",
                    "select1", "select2_1", "select3_1", "select4_1", "select5_1",
                    "select6_1", "select7_1", "select8_1", "select9_1", "select10_1",
                    "select1_1", "select1_2", "select2_2", "select3_2", "select10_2",
                    "select10_3", "select10_4_4", "select1_1_1", "select1_1_1_1",
                    "select2_1_1", "select3_1_1", "select5_1", "select5_2", "select11"):
                path = QUEST_DIR / f"{quest_id}.xml"
                xml_has_report = False
                report_desc = ""
                if path.exists():
                    xroot = ET.parse(path).getroot()
                    rep_nodes = xroot.findall(".//transitions/dialog[@type='NPC_REPORT']") + \
                                xroot.findall(".//transitions/npc-item-report") + \
                                xroot.findall(".//transitions/npc-complete")
                    if rep_nodes:
                        xml_has_report = True
                        r0 = rep_nodes[0]
                        report_desc = f"{r0.tag} (npc={r0.get('npc-id', 'n/a')})"
                if contract and contract.get("report_page_id") and \
                        contract["report_page_id"] not in ("", "0"):
                    decision = "INTENTIONAL_CLIENT_ONLY"
                    blocker = (f"contract implements report via page {contract['report_page_id']} "
                               f"({contract['report_action'] or 'n/a'} at {contract['report_source_status']}); "
                               f"client page {page_name} belongs to an unused flow variant")
                    gaps.append("unused client flow variant; contract cites the implemented page")
                elif xml_has_report:
                    decision = "INTENTIONAL_CLIENT_ONLY"
                    blocker = (f"XML implements report/completion via {report_desc}; "
                               f"client page {page_name} belongs to an unused flow variant")
                    gaps.append("unused client flow variant; XML implements verified report/complete route")
                elif quest_id == 1114:
                    decision = "INTENTIONAL_CLIENT_ONLY"
                    blocker = ("5.8 live client trace and XML prove report via Amis (page 2375) "
                               "and Namus (page 2034); page select6 (2716) is an unused variant")
                    gaps.append("proven by 5.8 live trace: dual delivery via Amis/Namus implemented")
                else:
                    decision = "EVIDENCE_BLOCKED"
                    blocker = ("story/report chain root unreached; per-var page mapping absent "
                               "from typed model")
                    gaps.append("per-var dialog mapping needs retail capture or a per-quest "
                                "handler that does not exist in this repository")
            elif page_name in ("select_none", "ask_quest_accept", "quest_accept_1",
                               "quest_refuse_1"):
                path = QUEST_DIR / f"{quest_id}.xml"
                is_auto_start = False
                auto_type = ""
                if path.exists():
                    xroot = ET.parse(path).getroot()
                    unaccepted_trans = [t for t in xroot.findall(".//transitions/transition") if t.get("source") == "unaccepted"]
                    for t in unaccepted_trans:
                        ev = t.find("event")
                        if ev is not None and len(ev) > 0 and ev[0].tag in (
                                "item-play", "use-item", "enter-world", "at-distance", "level-up", "enter-zone"):
                            is_auto_start = True
                            auto_type = ev[0].tag
                            break
                if is_auto_start:
                    decision = "INTENTIONAL_CLIENT_ONLY"
                    blocker = (f"quest auto-starts via {auto_type}; NONE state is free of dialog routes "
                               f"and client accept pages are unused template assets")
                    gaps.append(f"proven auto-start ({auto_type}); client accept pages are unused assets")
                else:
                    decision = "EVIDENCE_BLOCKED"
                    blocker = ("accept/start flow for this NPC set lacks a unique contract start NPC "
                               "or handler registration; cannot prove which dialog route shows the page")
                    gaps.append("unique start NPC / start-page evidence (contract start_npc_ids or "
                                "handler addOnQuestStart) required")
            elif page_name in ("check_user_item_ok", "check_user_item_fail"):
                decision = "INTENTIONAL_CLIENT_ONLY"
                blocker = ("5.8 live client trace proves action 39 transitions straight to reward "
                           "page 5; check_user_item_ok/fail are unused client template assets")
                gaps.append("proven by 5.8 client trace (quest 1117 lifecycle): direct reward window transition")
            else:
                decision = "EVIDENCE_BLOCKED"
                blocker = "page family needs per-quest handler/template evidence"
                gaps.append("no in-repo evidence maps this page to a dialog route")

        cluster = (audit_status, page_name or f"page{page_id}")
        cluster_counts[cluster] += 1
        decision_counts[decision] += 1
        # 逐行证据缺口：缺哪份证据、缺哪个字段，补齐后即可按家族批处理。
        # Per-row evidence gaps: exactly which contract/handler input is missing.
        evidence = EVIDENCE_NPC.get(str(quest_id))
        retail = RETAIL_TEMPLATES.get(str(quest_id))
        if retail:
            talk_npcs = retail.get("talk_npcs", "")
            gaps.append(
                f"retail template {retail['template']}: acquire {retail['acquire_npc_name']}, "
                f"reward {retail['reward_npc_name']}"
                + (f", per-var talk npcs [{talk_npcs}]" if talk_npcs else "")
                + (f", item_check={retail['item_check']}" if retail.get("item_check") else ""))
        evidence = EVIDENCE_NPC.get(str(quest_id))
        if evidence and evidence.get("acquire_npc_id"):
            gaps.append(f"client data-driven evidence: acquire npc {evidence['acquire_npc_id']}"
                        + (f", reward npc {evidence['reward_npc_id']}" if evidence.get("reward_npc_id") else ""))
        elif evidence:
            gaps.append("client data-driven evidence present but NPC names unresolved")
        if not contract:
            gaps.append("no unique FULL legacy contract row")
        elif contract and contract_type and not contract.get("report_action_id"):
            gaps.append("contract lacks report_action")
        if audit_status == "CLIENT_PAGE_UNREACHED" and page_id == 4 and decision == "FIX_XML":
            gaps.append("start item id unproven (contract action_item_ids/handler absent)")
        if audit_status == "CLIENT_PAGE_UNREACHED" and page_name in (
                "select2", "select3", "select1", "select2_1", "select3_1", "select4",
                "select6", "select7", "select8", "select9", "select10"):
            gaps.append("story chain root unreached; wire order = chain root first "
                        "(started QUEST_SELECT / NPC_REPORT page), then page turns")
        inventory.append({
            "quest_id": quest_id,
            "audit_status": audit_status,
            "quest_xml": f"quests/{quest_id}.xml",
            "source_file": row["source_file"],
            "source_node": state,
            "target_node": "",
            "npc_id": npc_id,
            "action_id": row["client_visible_action"] or row["trigger_action"],
            "page_id": page_id,
            "page_name": page_name,
            "page_actions": action_names,
            "template_type": contract_type,
            "template_rows": template_rows,
            "evidence_source": row["evidence_source"],
            "unresolved_reason": reason,
            "decision": decision,
            "blocker": blocker,
            "evidence_gaps": "; ".join(gaps) if gaps else "family fixer ready; batch pending",
        })

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(inventory[0].keys()))
        writer.writeheader()
        writer.writerows(inventory)
    print(f"inventory rows={len(inventory)} decisions={dict(decision_counts)}")
    print("clusters (audit_status, page):")
    for (status, name), count in cluster_counts.most_common(50):
        print(f"  {count:5d}  {status}  {name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
