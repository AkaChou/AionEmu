#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""审计：接取对象 ≠ 交付对象（reward_npc_name）的任务行，及其接取对象的 AI。

动机（2026-10-08，quest 19640）：
  传送门类 AI（portal_dialog / specialize_portal）的开门页轴 PortalDialogAI2#checkDialog 只读
  QuestEngine 的 NPC 对话注册表：任一相关任务处于 REWARD 就给奖励窗页 5。接取对象也在注册表
  （接取面注册），因此「接取对象 ≠ 交付对象」的行在接取对象上会被开出奖励窗，而领奖动作
  （8..22）在服务端只注册在交付对象上 ⇒ 死按钮 / 动作被 AI 回显为页 ⇒ 客户端 load fail。
  本脚本枚举同型机型的全量范围（不止 DD 表：其余六族表同结构）。

输出：按 (接取名, 交付名, 接取NPC.ai) 分组的行清单，附接取对象 AI 统计。
用法：python3 .agents/summary/quest-19640-reward-window-recovery/audit_acquire_reward_npc_gap.py
"""

from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path
from xml.etree import ElementTree

REPO = Path(__file__).resolve()
while not (REPO / "pom.xml").exists():
    REPO = REPO.parent
    if REPO.parent == REPO:
        sys.exit("找不到仓库根（含 pom.xml）")

RETAIL = REPO / "src/main/resources/aion/data/static_data/quest/retail"
NPCS = REPO / "src/main/resources/aion/data/static_data/npcs"

# 传送门类 AI：开门页轴只读 QuestEngine 注册表（PortalDialogAI2 / Specialize01PortalAI2 的 checkDialog）。
PORTAL_AIS = {"portal_dialog", "specialize_portal"}


def npc_name_map() -> dict[str, list[tuple[int, str]]]:
    """name_desc -> [(npc_id, ai)]（大小写不敏感；一个名字可对应多模板）。"""
    result: dict[str, list[tuple[int, str]]] = defaultdict(list)
    for path in sorted(NPCS.glob("npc_template_*.xml")):
        try:
            root = ElementTree.parse(path).getroot()
        except ElementTree.ParseError as exc:
            print(f"[warn] 跳过不可解析文件 {path.name}: {exc}", file=sys.stderr)
            continue
        for tpl in root.iter("npc_template"):
            name = tpl.get("name_desc") or tpl.get("name") or ""
            ai = tpl.get("ai") or ""
            npc_id = tpl.get("npc_id") or ""
            if not name or not npc_id.isdigit():
                continue
            result[name.lower()].append((int(npc_id), ai))
    return result


def add(rows: list[tuple[str, str, str, str]], family: str, quest_id: str,
        acquire: str, reward: str) -> None:
    acquire = (acquire or "").strip()
    reward = (reward or "").strip()
    if acquire and reward and acquire.lower() != reward.lower():
        rows.append((family, quest_id, acquire, reward))


def scan_tables(rows: list[tuple[str, str, str, str]]) -> None:
    # DD 表：value0_acquire_ / reward_npc_name
    dd = ElementTree.parse(RETAIL / "data_driven_quest.xml").getroot()
    for row in dd.iter("quest_data_driven"):
        qid = (row.findtext("id") or "").strip()
        add(rows, "DataDriven", qid, row.findtext("value0_acquire_") or "",
            row.findtext("reward_npc_name") or "")

    # 其余族表：acquired_npc_name / reward_npc_name（SimpleHunt / SimpleTalk / CollectItem / ...）
    for path in sorted(RETAIL.glob("Quest_*.xml")):
        if path.name.endswith(".xsd"):
            continue
        root = ElementTree.parse(path).getroot()
        for node in root:
            qid = (node.get("id") or node.findtext("id") or "").strip()
            acquire = node.findtext("acquired_npc_name") or node.findtext("acquire_npc_name") or ""
            reward = node.findtext("reward_npc_name") or ""
            add(rows, path.stem.replace("Quest_", ""), qid, acquire, reward)


def main() -> None:
    names = npc_name_map()
    rows: list[tuple[str, str, str, str]] = []
    scan_tables(rows)

    unresolved = 0
    by_ai = defaultdict(list)
    for family, qid, acquire, reward in rows:
        templates = names.get(acquire.lower(), [])
        if not templates:
            unresolved += 1
            continue
        ais = sorted({ai for _, ai in templates if ai})
        key = ",".join(ais) if ais else "(no-ai)"
        by_ai[key].append((family, qid, acquire, reward, templates))

    print(f"接取≠交付 行总数: {len(rows)}（接取名未解析: {unresolved}）")
    print("按接取对象 AI 分组：")
    for ai in sorted(by_ai, key=lambda k: (k not in PORTAL_AIS, k)):
        group = by_ai[ai]
        flag = "  <== 传送门类（同一缺口）" if ai in PORTAL_AIS else ""
        print(f"  {ai}: {len(group)} 行{flag}")

    print("\n传送门类接取对象的完整清单：")
    for ai in sorted(PORTAL_AIS):
        group = by_ai.get(ai, [])
        for family, qid, acquire, reward, templates in group:
            ids = ",".join(str(i) for i, _ in templates)
            print(f"  [{family}] quest {qid}: 接取 {acquire}({ids}) -> 交付 {reward}")


if __name__ == "__main__":
    main()
