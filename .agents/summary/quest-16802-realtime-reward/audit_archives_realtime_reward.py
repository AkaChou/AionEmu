#!/usr/bin/env python3
"""审计永恒档案馆击杀任务的「实时奖励」收口合同 / Audit the real-time reward closure of the Archives hunts.

检查项 / Checks:
1. 击杀收口：最后一击是否直接把任务推进到 REWARD 节点（QE-018）。
   Kill closure: the final kill enters the REWARD node directly.
2. 计数阶段：计数期间 SECTION_0(var0) 是否保持 0（客户端 quest_monster 门控）。
   Counting stage: SECTION_0 (var0) stays 0 while counting.
3. 报告页：DEFAULT_SUCCESS 只在领奖状态下发。
   Report page: DEFAULT_SUCCESS is emitted from the reward status only.
4. 上线自愈：是否存在无 source 的 enter-world 迁移路线（旧 k1/k2 脏存档）。
   Login self-heal: source-less enter-world migration routes for legacy k1/k2 saves.

用法 / Usage:
    python3 audit_archives_realtime_reward.py [quests-dir]
"""
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

DEFAULT_QUESTS_DIR = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
FAMILY = (16801, 26801, 16802, 26802, 16803, 26803, 16804, 26804)


def node_statuses(transitions_root, nodes_root):
    statuses = {}
    for node in nodes_root.findall("node"):
        statuses[node.get("label")] = (node.get("status"), {
            var.get("name"): int(var.get("value")) for var in node.findall("var")})
    return statuses


def kill_routes(transitions_root):
    routes = []
    for transition in transitions_root.findall("transition"):
        event = transition.find("event")
        if event is None or event.find("kill-npc") is None:
            continue
        routes.append(transition)
    return routes


def talk_routes(transitions_root, action_name):
    routes = []
    for transition in transitions_root.findall("transition"):
        event = transition.find("event")
        if event is None:
            continue
        dialog = event.find("dialog")
        if dialog is None or dialog.get("type") != "TALK_TO_NPC":
            continue
        raw = dialog.get("action") or dialog.get("actions") or ""
        if action_name in raw.split():
            routes.append(transition)
    return routes


def page_of(transition):
    after = transition.find("after-commit")
    if after is None:
        return None
    for dialog in after.findall("dialog"):
        if dialog.get("type") == "SHOW_QUEST_PAGE":
            return dialog.get("page")
    return None


def self_heal_routes(transitions_root):
    routes = []
    for transition in transitions_root.findall("transition"):
        if transition.get("source") is not None:
            continue
        event = transition.find("event")
        if event is None or event.find("enter-world") is None:
            continue
        routes.append(transition)
    return routes


def audit(quest_id, quests_dir):
    root = ET.parse(quests_dir / f"{quest_id}.xml").getroot()
    nodes_root = root.find("nodes")
    transitions_root = root.find("transitions")
    statuses = node_statuses(transitions_root, nodes_root)

    closure = []
    for route in kill_routes(transitions_root):
        target_status = statuses.get(route.get("target"), ("?", {}))[0]
        if target_status == "REWARD":
            closure.append(f"{route.get('source')}->{route.get('target')}({target_status},p={route.get('priority')})")

    counting_flags = sorted({statuses.get(route.get("target"), ("?", {}))[1].get("var0")
                             for route in kill_routes(transitions_root)
                             if statuses.get(route.get("target"), ("?", {}))[0] == "START"},
                            key=lambda value: (value is None, value))

    report_pages = {}
    for route in talk_routes(transitions_root, "QUEST_SELECT"):
        source_status = statuses.get(route.get("source"), ("?", {}))[0]
        report_pages.setdefault(source_status, set()).add(page_of(route))
    preview_states = sorted({statuses.get(route.get("source"), ("?", {}))[0]
                             for route in talk_routes(transitions_root, "SELECT_QUEST_REWARD")})
    heal = [f"{route.get('target')}(p={route.get('priority')})" for route in self_heal_routes(transitions_root)]

    return {
        "quest": quest_id,
        "kill_closure": closure,
        "counting_var0": counting_flags,
        "report_page_states": {status: sorted(pages) for status, pages in report_pages.items()},
        "reward_preview_states": preview_states,
        "self_heal": heal,
    }


def main():
    quests_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_QUESTS_DIR
    for quest_id in FAMILY:
        row = audit(quest_id, quests_dir)
        print(f"=== {row['quest']} ===")
        print(f"  击杀收口 kill closure      : {', '.join(row['kill_closure']) or 'NONE (最后一击不进入 REWARD)'}")
        print(f"  计数阶段 var0 投影         : {row['counting_var0']}")
        print(f"  QUEST_SELECT 报告页状态    : {row['report_page_states']}")
        print(f"  1009 预览路由来源状态      : {row['reward_preview_states']}")
        print(f"  上线自愈 self-heal         : {', '.join(row['self_heal']) or 'NONE'}")


if __name__ == "__main__":
    main()
