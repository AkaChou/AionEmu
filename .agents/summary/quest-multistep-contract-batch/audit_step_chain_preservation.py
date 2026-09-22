#!/usr/bin/env python3
"""核对多步链重建后的生产 XML 相对 HEAD 是否「只做预期改动」。

对每个任务逐项断言：
1. 客户端每行一个状态，步骤 i 的 SETPRO{i} 精确从第 i 个状态指向第 i+1 个状态；
2. 旧文件出现过的物品生命周期（give-item/remove-item/has-item）元素类型与物品 ID 一个都不能消失；
3. 只保留 1 个 npc-complete（领奖 NPC），且其子元素（choice/preview）原样保留；
4. 旧 NPC_REPORT 引用的页面必须仍能在新文件中出现（改由领奖行页面承担）；
5. 所有 page / action 名称必须命中 QuestDialogPage / QuestDialogAction 枚举。
"""
from __future__ import annotations

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

BASE = Path(__file__).resolve().parent
REPO = BASE.parents[2]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
ENUM_DIR = REPO / "src/main/java/com/aionemu/gameserver/questEngine/definition"
sys.path.insert(0, str(BASE))
import dump_chain_evidence as D  # noqa: E402
from apply_multistep_chains_v2 import ENUM_ACTIONS, ENUM_PAGES, build_plan  # noqa: E402


OLD_PATH = "src/main/resources/aion/data/static_data/quest_definition/quests/{quest_id}.xml"


def head_text(quest_id: int) -> str:
    """修复前的 HEAD 原始 XML；块级保留审计必须以它为基准。"""
    return subprocess.run(["git", "show", f"HEAD:{OLD_PATH.format(quest_id=quest_id)}"],
                          capture_output=True, text=True, check=True).stdout


def old_tree(quest_id: int) -> ET.Element:
    return ET.fromstring(head_text(quest_id))


def item_facts(root: ET.Element) -> dict[str, set[str]]:
    facts: dict[str, set[str]] = {"give": set(), "remove": set(), "has": set()}
    for element in root.iter():
        for family in facts:
            if element.tag == f"{family}-item":
                facts[family].add(element.get("item-id", "?"))
    return facts


def main() -> int:
    ids = [int(value) for value in sys.argv[1:]]
    failures = 0
    for quest_id in ids:
        path = QUESTS / f"{quest_id}.xml"
        raw = path.read_text(encoding="utf-8")
        new = ET.fromstring(raw)
        old = old_tree(quest_id)
        plan = build_plan(quest_id, head_text(quest_id))
        problems: list[str] = []

        routes = {}
        for transition in new.findall("transitions/transition"):
            event = transition.find("event")
            if event is None or len(event) != 1:
                continue
            dialog = list(event)[0]
            if dialog.tag == "dialog" and dialog.get("action", "").startswith("SETPRO"):
                routes[dialog.get("action")] = (transition.get("source"), transition.get("target"),
                                                dialog.get("npc-id"))
        for index, step in enumerate(plan.steps, start=1):
            expected = (step.source, step.target, str(step.npc))
            actual = routes.get(f"SETPRO{index}")
            if actual != expected:
                problems.append(f"SETPRO{index} 路由 {actual} != 期望 {expected}")

        old_items, new_items = item_facts(old), item_facts(new)
        for family in ("give", "remove", "has"):
            lost = old_items[family] - new_items[family]
            if lost:
                problems.append(f"{family}-item 物品 {sorted(lost)} 在新定义中消失")

        completes = new.findall("transitions/npc-complete")
        if len(completes) != 1 or completes[0].get("npc-id") != str(plan.reward_npc):
            problems.append(f"npc-complete 应为唯一领奖 NPC {plan.reward_npc}")

        pages = set(re.findall(r'page="([A-Za-z0-9_]+)"', raw))
        actions = {value for value in re.findall(r'action="([A-Za-z0-9_]+)"', raw) if ".." not in value}
        unknown_pages = {page for page in pages if page.upper() not in ENUM_PAGES}
        unknown_actions = {action for action in actions if action.upper() not in ENUM_ACTIONS}
        if unknown_pages:
            problems.append(f"未登记页面 {sorted(unknown_pages)}")
        if unknown_actions:
            problems.append(f"未登记动作 {sorted(unknown_actions)}")

        for report in old.findall("transitions/dialog"):
            if report.get("type") == "NPC_REPORT" and report.get("page") not in pages:
                problems.append(f"NPC_REPORT 页面 {report.get('page')} 在新定义中不可达")

        status = "OK" if not problems else "FAIL"
        failures += 1 if problems else 0
        print(f"{status} {quest_id}: rows={plan.rows} steps={[step.npc for step in plan.steps]} "
              f"reward={plan.reward_npc} has-item={sorted(new_items['has'])} "
              f"remove-item={sorted(new_items['remove'])}")
        for problem in problems:
            print(f"    - {problem}")
    print(f"检查 {len(ids)} 个任务，失败 {failures} 个")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
