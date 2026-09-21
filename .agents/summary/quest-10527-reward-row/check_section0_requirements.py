#!/usr/bin/env python3
"""把客户端 quest_script 的 SECTION_0 约束与服务端会产生/写入的 SECTION_0 取值对齐。

判据（对“SECTION_0 是阶段/行索引计数器”的任务族成立）：
- 客户端 `Progress(...; SECTION_0==N ...)` / `SECTION_0<N` 声明是对该任务某一步“可见/生效”的硬约束；
  服务端必须能产出满足该约束的 packed SECTION_0（状态投影、`set-variable`、`increment-variable` 都算）。
- 服务端产不出的约束 = 该步在客户端永远不会生效（任务书该行永远不亮/计数不刷新），属缺陷。
- 反过来服务端产出的值多于客户端声明的值并不一定是缺陷（多行/多阶段同时可见的模型）。

输出 TSV：quest_id / rows / script_predicates / required / produced / verdict / note

用法：
    python3 .agents/summary/quest-10527-reward-row/check_section0_requirements.py [quest_id ...]
"""

from __future__ import annotations

import importlib.util
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("audit", HERE / "audit_reward_row_vs_client_steps.py")
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


def predicates(quest_id: int) -> list[str]:
    raw = audit.client_section0(quest_id)
    return [part.strip() for part in raw.split("|") if part.strip()] if raw else []


def section0_constraints(quest_id: int) -> list[tuple[str, int]]:
    """该任务所有声明里的 SECTION_0 约束（operator, value）。/ SECTION_0 constraints of the quest."""
    found: list[tuple[str, int]] = []
    for predicate in predicates(quest_id):
        for operator, value in re.findall(r"SECTION_0\s*(==|<=|>=|<|>)\s*(\d+)", predicate):
            found.append((operator, int(value)))
    return found


def layout_head(quest_id: int) -> tuple[str, int, int] | None:
    """progress 里 offset=0 的字段（= SECTION_0），返回 (name, width, max)。"""
    path = audit.QUESTS_DIR / f"{quest_id}.xml"
    if not path.exists():
        return None
    root = ET.fromstring(path.read_text(encoding="utf-8"))
    progress = root.find("progress")
    if progress is None:
        return None
    best = None
    for field in progress:
        if field.get("offset") != "0":
            continue
        name = field.get("name")
        width = int(field.get("width", "0"))
        maximum = int(field.get("max", "0")) if field.get("max") else (2 ** width - 1)
        if best is None or width > best[1]:
            best = (name, width, maximum)
    return best


def produced_values(quest_id: int) -> tuple[set[int], set[int]]:
    """返回 (状态投影产生的 SECTION_0 值, 动作写入的值)。"""
    path = audit.QUESTS_DIR / f"{quest_id}.xml"
    root = ET.fromstring(path.read_text(encoding="utf-8"))
    head = layout_head(quest_id)
    if head is None:
        return set(), set()
    name = head[0]
    projections: set[int] = set()
    nodes = root.find("nodes")
    for node in (nodes if nodes is not None else []):
        for var in node.findall("var"):
            if var.get("name") == name:
                projections.add(int(var.get("value")))
    writes: set[int] = set()
    transitions = root.find("transitions")
    for transition in (transitions if transitions is not None else []):
        for container in ("actions",):
            for action in transition.findall(container):
                if action.get("field") != name:
                    continue
                if action.tag == "set-variable" and action.get("value") is not None:
                    writes.add(int(action.get("value")))
                elif action.tag == "increment-variable" and action.get("value") is not None:
                    for base in list(projections) + list(writes):
                        writes.add(base + int(action.get("value")))
    return projections, writes


def satisfied(operator: str, limit: int, values: set[int]) -> bool:
    return any({
        "==": value == limit,
        "<": value < limit,
        "<=": value <= limit,
        ">": value > limit,
        ">=": value >= limit,
    }[operator] for value in values)


def main() -> None:
    requested = [int(value) for value in sys.argv[1:] if value.isdigit()]
    if not requested:
        audit.client_section0(0)  # 触发客户端脚本索引构建 / build the client script index
        requested = sorted((audit.CLIENT_SECTION0 or {}).keys())
    print("quest\trows\tfield\tsection0_values\tscript\tunsatisfied\tverdict")
    for quest_id in requested:
        if not (audit.QUESTS_DIR / f"{quest_id}.xml").exists():
            print(f"{quest_id}\t0\t-\t[]\t{audit.client_section0(quest_id)}\t-\tNO_DEFINITION")
            continue
        rows = audit.client_rows(quest_id) or []
        head = layout_head(quest_id)
        constraints = section0_constraints(quest_id)
        projections, writes = produced_values(quest_id)
        values = projections | writes
        unsatisfied = [f"SECTION_0{operator}{limit}" for operator, limit in constraints
                       if not satisfied(operator, limit, values)]
        verdict = "NO_SECTION0_FIELD" if head is None else (
            "OK" if not unsatisfied else "UNREACHABLE")
        print(f"{quest_id}\t{len(rows)}\t{head[0] if head else '-'}\t{sorted(values)}\t"
              f"{audit.client_section0(quest_id)}\t{' '.join(unsatisfied) or '-'}\t{verdict}")


if __name__ == "__main__":
    main()
