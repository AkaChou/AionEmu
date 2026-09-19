#!/usr/bin/env python3
"""审计 NONE 状态是否有“任务列表行入口”动作（18300 指纹）。

Audit whether each NPC modelled in NONE state answers the client quest-list row action.

规则 / Rule:
  某个 NPC 在 NONE 状态已经出现对话路由（说明该任务在该 NPC 处被建模），但该 NPC 的动作集合里没有任何
  任务列表行入口动作（QUEST_SELECT 31 / FINISH_DIALOG 1008 / ASK_QUEST_ACCEPT 1007 / QUEST_ACCEPT_*），
  则客户端在第 10 页点击该任务行时服务端无应答，会重复下发同一页（18300 症状）。
"""
from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

BASE = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
ENTRY_ACTIONS = {"QUEST_SELECT", "FINISH_DIALOG", "ASK_QUEST_ACCEPT", "SELECT_NONE_1",
                 "QUEST_ACCEPT_1", "QUEST_ACCEPT_SIMPLE", "QUEST_ACCEPT_SIMPLE_1"}


def actions_of(payload: ET.Element) -> set[str]:
    raw = (payload.get("action") or "") + " " + (payload.get("actions") or "")
    return {token for token in raw.replace(",", " ").split() if token}


def main() -> None:
    hits: list[tuple[str, int, set[str]]] = []
    for path in sorted(BASE.glob("*.xml")):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        nodes = {node.get("label"): (node.get("status") or "").strip()
                 for node in root.findall("./nodes/node")}
        none_labels = {label for label, status in nodes.items() if status == "NONE"}
        if not none_labels:
            continue
        npc_actions: dict[int, set[str]] = {}
        for block in root.findall("./transitions/dialog"):
            if block.get("source") not in none_labels or not block.get("npc-id"):
                continue
            npc = int(block.get("npc-id"))
            actions = npc_actions.setdefault(npc, set())
            actions.add("NPC_START_BLOCK" if block.get("type") == "NPC_START" else str(block.get("type")))
        for transition in root.findall("./transitions/transition"):
            if transition.get("source") not in none_labels:
                continue
            event = transition.find("event")
            if event is None or not len(event):
                continue
            payload = event[0]
            if payload.tag != "dialog" or payload.get("type") != "TALK_TO_NPC" or not payload.get("npc-id"):
                continue
            npc = int(payload.get("npc-id"))
            npc_actions.setdefault(npc, set()).update(actions_of(payload))
        for npc, actions in npc_actions.items():
            if "NPC_START_BLOCK" in actions:
                continue
            if actions & ENTRY_ACTIONS:
                continue
            if not actions:
                continue
            hits.append((path.stem, npc, actions))
    print(f"none-state NPCs without list-row entry action: {len(hits)}")
    for quest_id, npc, actions in hits:
        print(f"  - quest={quest_id} npc={npc} none_actions={sorted(actions)}")


if __name__ == "__main__":
    main()
