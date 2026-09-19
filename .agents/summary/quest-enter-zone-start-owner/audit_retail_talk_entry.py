#!/usr/bin/env python3
"""用 retail 对话合同审计“任务列表行入口”是否存在于当前 XML。

Audit that every retail TALK-start NPC answers the client quest-list row action in the current XML.

规则 / Rule:
  对 legacy-quest-dialog-contracts.csv 中 start_type=TALK、report_open_action=QUEST_SELECT 的任务，
  其 start_npc_ids 中的 NPC 必须在 quest XML 的 NONE 状态具备入口动作
  （QUEST_SELECT 31 / FINISH_DIALOG 1008 / ASK_QUEST_ACCEPT 1007 / QUEST_ACCEPT_1 1002 / QUEST_ACCEPT_SIMPLE）
  或 NPC_START 块；否则客户端在第 10 页点任务行会得到 DialogService 的 page=10 回退（18300 症状）。
"""
from __future__ import annotations

import csv
import xml.etree.ElementTree as ET
from pathlib import Path

BASE = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
CONTRACTS = Path("docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv")
ENTRY = {"QUEST_SELECT", "FINISH_DIALOG", "ASK_QUEST_ACCEPT", "QUEST_ACCEPT_1",
         "QUEST_ACCEPT_SIMPLE", "QUEST_ACCEPT_SIMPLE_1", "SELECT_NONE_1"}


def actions_of(payload: ET.Element) -> set[str]:
    raw = (payload.get("action") or "") + " " + (payload.get("actions") or "")
    return {token for token in raw.replace(",", " ").split() if token}


def entry_npcs(root: ET.Element) -> dict[int, set[str]]:
    nodes = {node.get("label"): (node.get("status") or "").strip()
             for node in root.findall("./nodes/node")}
    none_labels = {label for label, status in nodes.items() if status == "NONE"}
    result: dict[int, set[str]] = {}
    for block in root.findall("./transitions/dialog"):
        if block.get("source") in none_labels and block.get("type") == "NPC_START" and block.get("npc-id"):
            result.setdefault(int(block.get("npc-id")), set()).add("NPC_START")
    for transition in root.findall("./transitions/transition"):
        if transition.get("source") not in none_labels:
            continue
        event = transition.find("event")
        if event is None or not len(event):
            continue
        payload = event[0]
        if payload.tag != "dialog" or payload.get("type") != "TALK_TO_NPC" or not payload.get("npc-id"):
            continue
        result.setdefault(int(payload.get("npc-id")), set()).update(actions_of(payload))
    return result


def main() -> None:
    rows = []
    with CONTRACTS.open(encoding="utf-8-sig") as handle:
        for row in csv.DictReader(handle):
            if row.get("start_type") != "TALK":
                continue
            if row.get("report_open_action") != "QUEST_SELECT":
                continue
            npcs = [int(value) for value in (row.get("start_npc_ids") or "").split() if value.isdigit()]
            if not npcs:
                continue
            rows.append((int(row["quest_id"]), npcs, row.get("start_page_id"), row.get("start_page")))

    missing_file = 0
    hits = []
    for quest_id, npcs, page_id, page in rows:
        path = BASE / f"{quest_id}.xml"
        if not path.exists():
            missing_file += 1
            continue
        modelled = entry_npcs(ET.parse(path).getroot())
        for npc in npcs:
            actions = modelled.get(npc, set())
            if actions & ENTRY or "NPC_START" in actions:
                continue
            hits.append((quest_id, npc, sorted(actions), page_id, page))

    print(f"retail TALK contracts: {len(rows)} (quest XML missing: {missing_file})")
    print(f"start NPCs without list-row entry in current XML: {len(hits)}")
    for quest_id, npc, actions, page_id, page in hits:
        print(f"  - {quest_id} npc={npc} none_actions={actions} retail_start_page={page_id}/{page}")


if __name__ == "__main__":
    main()
