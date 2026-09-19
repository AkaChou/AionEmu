#!/usr/bin/env python3
"""审计 legacy 接取 owner 在 quest XML 中的保留情况。

Audit whether legacy quest handlers' NPC start owners survive in the migrated quest XML.

分类 / Categories:
  A_QUEST_SELECT_UNHANDLED : 旧 handler 有 addOnQuestStart(owner)，但 XML 在 NONE 状态没有该 NPC 的
                             QUEST_SELECT/FINISH_DIALOG 响应 -> 客户端点任务行会循环重发（18300 症状）。
  B_ACCEPT_ACTION_MISSING  : 有响应页但没有 QUEST_ACCEPT_* 接取动作。
  C_AUTO_START_ONLY        : 该 NPC 在 NONE 状态完全不可达，只能靠 enter-zone/enter-world/level-up 等自动接取。

用法 / Usage:
    python3 .agents/summary/quest-enter-zone-start-owner/audit_enter_zone_start_owner.py --legacy-rev 51b4cb971
"""
from __future__ import annotations

import argparse
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

QUEST_DIR = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
HANDLER_PREFIX = "src/main/java/com/aionemu/gameserver/quest/handlers/"
SELECT_ACTIONS = {"QUEST_SELECT", "FINISH_DIALOG", "ASK_QUEST_ACCEPT"}
ACCEPT_ACTIONS = {"QUEST_ACCEPT_1", "QUEST_ACCEPT_SIMPLE", "QUEST_ACCEPT_SIMPLE_1"}
AUTO_START_EVENTS = {"enter-zone", "enter-world", "level-up", "use-item", "event-quest-refresh",
                     "monster-hunt", "at-distance", "use-object"}
QUEST_ID_RE = re.compile(r"questId\s*=\s*(\d{3,6})\s*;")
SUPER_RE = re.compile(r"super\s*\(\s*(\d{3,6})\s*\)")
START_OWNER_RE = re.compile(
    r"registerQuestNpc\s*\(\s*(\d+)\s*\)\s*\.\s*addOnQuestStart\s*\(", re.S)


def git(*args: str) -> str:
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout


def legacy_start_owners(rev: str) -> dict[int, tuple[set[int], str]]:
    files = [line for line in git("ls-tree", "-r", f"{rev}^", "--name-only", HANDLER_PREFIX).splitlines()
             if line.endswith(".java")]
    owners: dict[int, tuple[set[int], str]] = {}
    for path in files:
        source = git("show", f"{rev}^:{path}")
        match = SUPER_RE.search(source) or QUEST_ID_RE.search(source)
        if not match:
            continue
        quest_id = int(match.group(1))
        found = {int(value) for value in START_OWNER_RE.findall(source)}
        if found:
            owners.setdefault(quest_id, (set(), path.split("/")[-1]))[0].update(found)
    return owners


def actions_of(payload: ET.Element) -> set[str]:
    raw = payload.get("action") or ""
    raw += " " + (payload.get("actions") or "")
    return {token.strip() for token in raw.replace(",", " ").split() if token.strip()}


def analyse(xml: Path) -> dict[str, object]:
    root = ET.parse(xml).getroot()
    nodes = {node.get("label"): (node.get("status") or "").strip() for node in root.findall("./nodes/node")}
    none_labels = {label for label, status in nodes.items() if status == "NONE"}
    block_owners: set[int] = set()
    for block in root.findall("./transitions/dialog"):
        if block.get("type") == "NPC_START" and block.get("npc-id"):
            block_owners.add(int(block.get("npc-id")))
    select_owners: set[int] = set()
    accept_owners: set[int] = set()
    auto_start = False
    for transition in root.findall("./transitions/transition"):
        if transition.get("source") not in none_labels:
            continue
        event = transition.find("event")
        if event is None or not len(event):
            continue
        payload = event[0]
        if payload.tag in AUTO_START_EVENTS and nodes.get(transition.get("target")) == "START":
            auto_start = True
        if payload.tag == "dialog" and payload.get("type") == "TALK_TO_NPC" and payload.get("npc-id"):
            npc = int(payload.get("npc-id"))
            actions = actions_of(payload)
            if actions & SELECT_ACTIONS:
                select_owners.add(npc)
            if actions & ACCEPT_ACTIONS and nodes.get(transition.get("target")) == "START":
                accept_owners.add(npc)
    return {"block_owners": block_owners, "select_owners": select_owners,
            "accept_owners": accept_owners, "auto_start": auto_start}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--legacy-rev", default="51b4cb971")
    args = parser.parse_args()

    legacy = legacy_start_owners(args.legacy_rev)
    findings: dict[str, list[str]] = {"A": [], "B": [], "C": []}
    for quest_id, (owners, handler) in sorted(legacy.items()):
        xml = QUEST_DIR / f"{quest_id}.xml"
        if not xml.exists():
            continue
        data = analyse(xml)
        reachable = owners & (data["block_owners"] | data["select_owners"])  # type: ignore[operator]
        accept = owners & (data["block_owners"] | data["accept_owners"])  # type: ignore[operator]
        if not reachable:
            bucket = "C" if data["auto_start"] else "A"
            findings[bucket].append(f"{quest_id}: owners={sorted(owners)} handler={handler}")
        elif not accept:
            findings["B"].append(f"{quest_id}: owners={sorted(owners)} handler={handler}")
    print(f"legacy handlers with addOnQuestStart: {len(legacy)}")
    for key, label in (("A", "A_QUEST_SELECT_UNHANDLED (硬缺陷/无法接取)"),
                       ("B", "B_ACCEPT_ACTION_MISSING (可查看不可接取)"),
                       ("C", "C_AUTO_START_ONLY (被自动接取绕过)")):
        print(f"{label}: {len(findings[key])}")
        for line in findings[key]:
            print("  - " + line)
    print("remaining_suspicious=" + str(len(findings["A"]) + len(findings["B"]) + len(findings["C"])))


if __name__ == "__main__":
    main()
