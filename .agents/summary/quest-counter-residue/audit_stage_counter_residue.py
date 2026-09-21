#!/usr/bin/env python3
"""审计：阶段推进离开/进入客户端声明的击杀计数阶段时的局部计数器契约（QE-044）。

Audit the local-counter contract for stage advances that leave or enter a
client-declared counting stage (QE-044).

判定规则 / Rules:
  * 证据 1：客户端 ``quest_monster.csv`` 的 ``Progress(SECTION_0==S; SECTION_C<L)``
    声明「阶段 S 用 SECTION_C 计数，客户端上限 L」。
  * 证据 2：服务端 progress bit-field 必须把 SECTION_C 放在 offset 6*C（QE-012）。
  * 规则 A（离场残留）：从计数阶段 S 推进到 T != S 的转换，如果既不显式
    ``set-variable varC = 0``，目标节点投影也没有声明 varC，那么计数器会静默
    带入 T，使打包步数变成 ``(varC<<6*C) | T``，客户端 ``progress == T``
    判定失败（NPC 对白 / 区域 / 影片门控不触发）。
  * 规则 B（入场脏值）：推进进入计数阶段 T 且 T 在契约中用 SECTION_C 计数时，
    若计数器既未清零、目标投影也未声明，则新计数从脏值起算。
  * 例外：目标节点显式声明该字段（如 15546 领奖行需要展示 4/4 计数）视为
    有意的显示契约，不报。
"""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
CLIENT_MONSTER_CSV = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_monster.csv")

PROGRESS_RE = re.compile(r"Progress\(SECTION_(\d+)==(\d+);\s*SECTION_(\d+)<(\d+)\)")


def load_client_counting_contract(path: Path) -> dict[int, dict[int, tuple[int, int]]]:
    """返回 {quest_id: {stage: (counter_section, limit)}}。"""
    contracts: dict[int, dict[int, tuple[int, int]]] = {}
    if not path.exists():
        return contracts
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        cells = raw.split(",")
        if len(cells) < 2 or not cells[0].strip().isdigit():
            continue
        match = PROGRESS_RE.search(cells[1])
        if not match:
            continue
        step_section, stage, counter_section, limit = (int(g) for g in match.groups())
        if step_section != 0 or counter_section == 0:
            continue
        contracts.setdefault(int(cells[0]), {})[stage] = (counter_section, limit)
    return contracts


def parse_definition(path: Path):
    root = ET.parse(path).getroot()
    fields: dict[str, dict[str, int]] = {}
    progress = root.find("progress")
    if progress is not None:
        for field in progress.findall("bit-field"):
            name = field.get("name")
            if name:
                fields[name] = {"offset": int(field.get("offset", "0")),
                                "width": int(field.get("width", "1"))}
    nodes: dict[str, dict[str, int]] = {}
    for node in root.findall("./nodes/node"):
        label = node.get("label")
        if label:
            nodes[label] = {var.get("name"): int(var.get("value")) for var in node.findall("var")}
    transitions = []
    for transition in root.findall("./transitions/transition"):
        transitions.append({
            "source": transition.get("source"),
            "target": transition.get("target"),
            "event": transition.find("event/*"),
            "actions": [(a.tag, dict(a.attrib)) for a in transition.findall("./actions/*")],
        })
    return fields, nodes, transitions


def describe_event(event) -> str:
    if event is None:
        return "-"
    if event.tag in {"dialog", "kill-npc"}:
        key = "npc-id" if event.get("npc-id") else "npc-ids"
        return f"{event.tag} {event.get(key, '-')} {event.get('action', '')}".strip()
    return event.tag


def touched_counter(transition, field_name: str) -> bool:
    return any(tag in {"set-variable", "increment-variable"} and attrib.get("field") == field_name
               for tag, attrib in transition["actions"])


def main() -> int:
    contracts = load_client_counting_contract(CLIENT_MONSTER_CSV)
    rows: list[str] = []
    audited_quests = 0
    for path in sorted(QUEST_DIR.glob("*.xml")):
        match = re.fullmatch(r"(\d+)\.xml", path.name)
        if not match:
            continue
        quest_id = int(match.group(1))
        contract = contracts.get(quest_id)
        if not contract:
            continue
        audited_quests += 1
        fields, nodes, transitions = parse_definition(path)
        for stage, (counter_section, limit) in sorted(contract.items()):
            field_name = f"var{counter_section}"
            field = fields.get(field_name)
            if field is None:
                rows.append(f"{quest_id}\tMISSING_FIELD\tstage={stage} {field_name} limit={limit} not declared")
                continue
            if field["offset"] != 6 * counter_section:
                rows.append(f"{quest_id}\tSECTION_OFFSET_MISMATCH\tstage={stage} {field_name} "
                            f"offset={field['offset']} expected={6 * counter_section}")
                continue
            for transition in transitions:
                source_vars = nodes.get(transition["source"] or "", {})
                target_vars = nodes.get(transition["target"] or "", {})
                if "var0" not in source_vars or "var0" not in target_vars:
                    continue
                source_stage, target_stage = source_vars["var0"], target_vars["var0"]
                if source_stage != stage or target_stage == stage:
                    continue
                if field_name in target_vars or touched_counter(transition, field_name):
                    continue
                entry = (f"{quest_id}\tRESIDUAL_COUNTER\tstage={source_stage}->{target_stage} "
                         f"{field_name}<{limit} event={describe_event(transition['event'])} "
                         f"source={transition['source']} target={transition['target']}")
                rows.append(entry)
            # 规则 B：进入该计数阶段时计数器必须为空（目标阶段即本阶段）
            target_of_counting_stage = stage
            for transition in transitions:
                target_vars = nodes.get(transition["target"] or "", {})
                source_vars = nodes.get(transition["source"] or "", {})
                if "var0" not in source_vars or "var0" not in target_vars:
                    continue
                if target_vars["var0"] != target_of_counting_stage or source_vars["var0"] == target_of_counting_stage:
                    continue
                if field_name in target_vars or touched_counter(transition, field_name):
                    continue
                rows.append(f"{quest_id}\tDIRTY_COUNTING_ENTRY\tstage={source_vars['var0']}->{stage} "
                            f"{field_name}<{limit} event={describe_event(transition['event'])} "
                            f"source={transition['source']} target={transition['target']}")
    for row in rows:
        print(row)
    print(f"# audited_quests={audited_quests} findings={len(rows)}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
