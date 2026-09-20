#!/usr/bin/env python3
"""审计“大师服（[Master Server]）专属刷怪”与任务击杀目标可达性。

Audits master-server-only spawn categories and quest kill-target reachability.

用法 / Usage:
    python3 .agents/summary/quest-conquest-offering-reachability/audit_master_server_spawn_coverage.py

退出码 / Exit code: 0 = 全部检查通过, 1 = 存在缺口。
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SPAWNS = ROOT / "src/main/resources/aion/data/static_data/spawns"
QUESTS = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
SCHEDULE = ROOT / "src/main/resources/aion/config/schedule/conquest_schedule.xml"

# 玩家可玩世界 ↔ 大师服镜像世界 / playable worlds against their master-server mirrors
WORLD_PAIRS = [("Inggison", 210050000, 210130000), ("Gelkmaros", 220070000, 220140000)]
REQUIRED_KILLS = 10


def quest_kill_targets(quest_id: int) -> set[int]:
    text = (QUESTS / f"{quest_id}.xml").read_text(encoding="utf-8")
    return {int(token) for group in re.findall(r'npc-ids="([^"]+)"', text) for token in group.split()}


def conquest_spawns(file_name: str, conquest_id: int) -> tuple[set[int], set[int], int]:
    """返回 (world ids, npc ids, spot 数) —— 仅 CONQUEST 状态。"""
    root = ET.parse(SPAWNS / "Conquest" / file_name).getroot()
    worlds: set[int] = set()
    npcs: set[int] = set()
    spots = 0
    for spawn_map in root.iter("spawn_map"):
        worlds.add(int(spawn_map.get("map_id")))
        for conquest in spawn_map.findall("conquest_spawn"):
            if int(conquest.get("id")) != conquest_id:
                continue
            for state in conquest.findall("conquest_type"):
                if state.get("ostate") != "CONQUEST":
                    continue
                for spawn in state.findall("spawn"):
                    npcs.add(int(spawn.get("npc_id")))
                    spots += len(spawn.findall("spot"))
    return worlds, npcs, spots


def scheduled_conquest_ids() -> set[int]:
    root = ET.parse(SCHEDULE).getroot()
    return {int(c.get("id")) for c in root.findall("conquest") if c.findall("offeringTime")}


def audit_categories() -> list[str]:
    """列出只在镜像世界声明刷怪的类别。"""
    findings = []
    print(f"{'spawn category':<32}{'Inggison live/master':>26}{'Gelkmaros live/master':>28}")
    for directory in sorted(p.name for p in SPAWNS.iterdir() if p.is_dir()):
        counts = []
        for _, live, master in WORLD_PAIRS:
            counts.append((len(list((SPAWNS / directory).glob(f"*{live}*"))),
                           len(list((SPAWNS / directory).glob(f"*{master}*")))))
        if counts == [(0, 0), (0, 0)]:
            continue
        master_only = any(live == 0 and master > 0 for live, master in counts)
        print(f"{directory:<32}{str(counts[0]):>26}{str(counts[1]):>28}"
              f"{'  <== master-only' if master_only else ''}")
        if master_only:
            findings.append(directory)
    return findings


def audit_quest_reachability() -> list[str]:
    """校验 15321/25321 第 3 段目标在可玩世界按活动时段可达。"""
    problems = []
    scheduled = scheduled_conquest_ids()
    for quest_id, file_name, master_name, conquest_id, world in (
            (15321, "210050000_Inggison.xml", "210130000_Inggison [Master Server].xml", 1, 210050000),
            (25321, "220070000_Gelkmaros.xml", "220140000_Gelkmaros [Master Server].xml", 2, 220070000)):
        worlds, npcs, spots = conquest_spawns(file_name, conquest_id)
        _, master_npcs, _ = conquest_spawns(master_name, conquest_id)
        targets = quest_kill_targets(quest_id)
        checks = {
            "worlds": worlds == {world},
            "every event npc is a quest target": npcs <= targets,
            "mirror superset": npcs <= master_npcs,
            f"at least {REQUIRED_KILLS} npc kinds": len(npcs) >= REQUIRED_KILLS,
            f"at least {REQUIRED_KILLS} spots": spots >= REQUIRED_KILLS,
            "conquest scheduled": conquest_id in scheduled,
        }
        print(f"\n{quest_id} <- {file_name}: worlds={sorted(worlds)} npcs={len(npcs)} spots={spots}")
        for label, ok in checks.items():
            print(f"   [{'OK' if ok else 'FAIL'}] {label}")
            if not ok:
                problems.append(f"{quest_id}: {label}")
    return problems


def main() -> int:
    master_only = audit_categories()
    problems = audit_quest_reachability()
    print("\n== 只在镜像世界声明刷怪的类别 / master-only categories ==")
    print("   " + (", ".join(master_only) if master_only else "none"))
    print("== 结论 / verdict ==")
    if problems:
        print("   FAIL: " + "; ".join(problems))
        return 1
    print("   OK: 15321/25321 第 3 段目标在可玩世界按活动时段可达 / stage 3 reachable in the playable worlds")
    return 0


if __name__ == "__main__":
    sys.exit(main())
