#!/usr/bin/env python3
"""审计“任何已建模路径都无法接取”的 quest XML。

Audit quest definitions that expose no modelled accept path from NONE state.

判定 / Rule:
  一个任务若在 NONE 状态没有任何 (a) 对话接取响应(QUEST_SELECT/FINISH_DIALOG/ASK_QUEST_ACCEPT/QUEST_ACCEPT_*)、
  (b) 自动接取事件(enter-zone/enter-world/level-up/use-item/at-distance/event-quest-refresh)、
  (c) 其它任务通过 start-quest 动作授予，
  则客户端在任务列表点击该任务行时会停留在原页（18300 类循环）。
"""
from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

BASE = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
SELECT_ACTIONS = {"QUEST_SELECT", "FINISH_DIALOG", "ASK_QUEST_ACCEPT", "QUEST_ACCEPT_1",
                  "QUEST_ACCEPT_SIMPLE", "QUEST_ACCEPT_SIMPLE_1", "QUEST_REFUSE_1",
                  "QUEST_REFUSE_2", "QUEST_REFUSE_SIMPLE"}
AUTO_EVENTS = {"enter-zone", "enter-world", "level-up", "use-item", "at-distance",
               "event-quest-refresh", "use-object", "kill-npc", "monster-hunt", "movie-end",
               "skill", "item-use"}


def actions_of(payload: ET.Element) -> set[str]:
    raw = (payload.get("action") or "") + " " + (payload.get("actions") or "")
    return {token for token in raw.replace(",", " ").split() if token}


def main() -> None:
    granted_by_other_quest: set[str] = set()
    documents: dict[str, ET.Element] = {}
    for path in sorted(BASE.glob("*.xml")):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as error:
            print("PARSE_FAIL", path.name, error)
            continue
        documents[path.stem] = root
        for action in root.iter("start-quest"):
            quest = action.get("quest-id") or action.get("questId") or action.get("id")
            if quest:
                granted_by_other_quest.add(quest.strip())

    unreachable: list[tuple[str, str]] = []
    for quest_id, root in documents.items():
        nodes = {node.get("label"): (node.get("status") or "").strip()
                 for node in root.findall("./nodes/node")}
        none_labels = {label for label, status in nodes.items() if status == "NONE"}
        if not none_labels:
            continue
        reachable = False
        for block in root.findall("./transitions/dialog"):
            if block.get("type") in {"NPC_START"} and block.get("source") in none_labels:
                reachable = True
        for transition in root.findall("./transitions/transition"):
            if transition.get("source") not in none_labels:
                continue
            event = transition.find("event")
            if event is None or not len(event):
                continue
            payload = event[0]
            if payload.tag in AUTO_EVENTS:
                reachable = True
            if payload.tag == "dialog" and payload.get("type") == "TALK_TO_NPC" \
                    and actions_of(payload) & SELECT_ACTIONS:
                reachable = True
        if not reachable and quest_id not in granted_by_other_quest:
            statuses = ",".join(sorted({status for status in nodes.values() if status != "NONE"}))
            unreachable.append((quest_id, statuses))

    print(f"quest files: {len(documents)}")
    print(f"granted by other quests' start-quest action: {len(granted_by_other_quest)}")
    print(f"no modelled accept path from NONE: {len(unreachable)}")
    for quest_id, statuses in unreachable:
        print(f"  - {quest_id} other_statuses={statuses}")


if __name__ == "__main__":
    main()
