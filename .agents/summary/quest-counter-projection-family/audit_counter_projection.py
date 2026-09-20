"""自环计数投影锁：全量审计 + 修复前后行为对照。

运行期内核（QuestMutationPlanner#matchesSourceNode）用 source 节点的投影与当前
packed 变量做**全等**匹配：只要自环计数器把某个被投影的字段写到别的值，下一次
同源事件就再也匹配不到该 source，任务停在第一只怪（NO_MATCH）。

本脚本做两件事：
1. 扫描生产 quest_definition/quests/*.xml，报告"自环 + increment 一个被 source
   投影钉住的字段"这一结构性缺陷（修复前 15321/25608/27510，修复后应为 0）。
2. 用与 planner 相同的匹配规则模拟连续击杀，给出修复前（把计数字段强行钉回
   0）与修复后的完成所需击杀数，证明症状与修复方向一致。
"""
from __future__ import annotations

import glob
import os
import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass

QUEST_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"


@dataclass(frozen=True)
class Transition:
    source: str | None
    target: str
    priority: int
    event: tuple[str, tuple[int, ...]]
    conditions: list[tuple[str, str, int]]
    actions: list[tuple[str, str, int]]

    def touches(self, field: str) -> bool:
        return any(action[1] == field for action in self.actions)


def load(quest_id: int) -> tuple[dict[str, dict[str, int]], list[Transition]]:
    root = ET.parse(os.path.join(QUEST_DIR, f"{quest_id}.xml")).getroot()
    nodes: dict[str, dict[str, int]] = {}
    for node in root.findall("nodes/node"):
        nodes[node.get("label")] = {v.get("name"): int(v.get("value")) for v in node.findall("var")}
    transitions: list[Transition] = []
    for t in root.findall("transitions/transition"):
        event = t.find("event")
        if event is None:
            continue
        child = event[0]
        ids: tuple[int, ...] = ()
        if child.tag == "kill-npc":
            if child.get("npc-id"):
                ids = (int(child.get("npc-id")),)
            else:
                ids = tuple(sorted(int(token) for token in (child.get("npc-ids") or "").split()))
        conditions = [
            ((c.tag, c.get("field"), int(c.get("value"))))
            for c in t.findall("conditions/*")
            if c.get("field")
        ]
        actions = []
        for a in t.findall("actions/*"):
            if a.get("field") is None:
                continue
            value = int(a.get("value") or a.get("delta") or 0)
            actions.append((a.tag, a.get("field"), value))
        transitions.append(
            Transition(
                t.get("source"),
                t.get("target"),
                int(t.get("priority") or 0),
                (child.tag, ids),
                conditions,
                actions,
            )
        )
    return nodes, transitions


def conditions_hold(state: dict[str, int], conditions: list[tuple[str, str, int]]) -> bool:
    for tag, field, value in conditions:
        current = state.get(field, 0)
        if tag == "variable-below" and not current < value:
            return False
        if tag == "variable-at-least" and not current >= value:
            return False
        if tag == "variable-is" and current != value:
            return False
    return True


def matches(nodes, transition: Transition, state: dict[str, int], npc_id: int) -> bool:
    if transition.source is None:
        return False
    projection = nodes.get(transition.source)
    if projection is None:
        return False
    for field, value in projection.items():
        if state.get(field, 0) != value:
            return False
    if not conditions_hold(state, transition.conditions):
        return False
    tag, ids = transition.event
    return tag == "kill-npc" and npc_id in ids


def apply(nodes, transition: Transition, state: dict[str, int]) -> dict[str, int]:
    next_state = dict(state)
    touched = set()
    for tag, field, value in transition.actions:
        if tag == "set-variable":
            next_state[field] = value
        elif tag == "increment-variable":
            next_state[field] = next_state.get(field, 0) + value
        touched.add(field)
    for field, value in nodes[transition.target].items():
        if field not in touched:
            next_state[field] = value
    return next_state


def simulate(quest_id: int, start_node: str, plan: list[tuple[int, int]], pin_counter: str | None = None):
    """按 NPC 轮转派发击杀；返回 (每个击杀是否被处理, 最终状态)。"""
    nodes, transitions = load(quest_id)
    if pin_counter:
        for label, projection in nodes.items():
            if label == start_node:
                projection[pin_counter] = 0
    state = {field: 0 for field in ("var0", "var1", "var2")}
    state.update(nodes[start_node])
    handled = []
    for npc_id, count in plan:
        for _ in range(count):
            ordered = sorted(transitions, key=lambda t: t.priority)
            match = next(
                (t for t in ordered
                 if t.event[0] == "kill-npc" and npc_id in t.event[1] and matches(nodes, t, state, npc_id)),
                None,
            )
            if match is None:
                handled.append(False)
                continue
            state = apply(nodes, match, state)
            handled.append(True)
    return handled, state


if __name__ == "__main__":
    print("== 1. 全量结构性扫描：自环 + increment 被投影字段 ==")
    offenders = []
    for path in sorted(glob.glob(os.path.join(QUEST_DIR, "*.xml"))):
        quest_id = int(os.path.basename(path)[:-4])
        nodes, transitions = load(quest_id)
        for transition in transitions:
            if transition.source is None or transition.source != transition.target:
                continue
            projection = nodes.get(transition.source, {})
            for tag, field, _ in transition.actions:
                if tag == "increment-variable" and field in projection:
                    offenders.append((quest_id, transition.source, field))
    print("   violations:", len(offenders))
    for row in offenders:
        print("   ", row)

    print("== 2. 修复前后行为对照 ==")
    cases = [
        (15321, "s1", [(235829, 30)], "var1", 30),
        (25608, "step2", [(241235, 10)], "var1", 10),
        (27510, "s3", [(244454, 10), (244490, 1)], "var1", 11),
    ]
    for quest_id, start_node, plan, counter, expected in cases:
        fixed, final = simulate(quest_id, start_node, plan)
        broken, _ = simulate(quest_id, start_node, plan, pin_counter=counter)
        print(f"   {quest_id}: 修复后处理 {sum(fixed)}/{len(fixed)} 次击杀，最终 {final}")
        print(f"   {quest_id}: 修复前处理 {sum(broken)}/{len(broken)} 次击杀（第二只起停止计数）")
        assert all(fixed), f"quest {quest_id} still stalls"
        assert sum(fixed) == expected, (quest_id, sum(fixed))
        assert not all(broken), f"quest {quest_id} negative control did not stall"
    print("OK")
