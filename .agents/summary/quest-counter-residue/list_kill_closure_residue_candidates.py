#!/usr/bin/env python3
"""列出「击杀收口残留本阶段计数器」候选任务（QE-044 规则 A 的击杀子集）。

List kill-closure counter-residue candidates: a kill transition that leaves the
client-declared counting stage while keeping the stage-local counter non-zero
(``increment-variable`` or a non-zero ``set-variable``) and whose target node does
not declare that field. Such a closure packs the step as ``(varC<<6*C)|T`` and the
client stops requesting the quest dialog for step ``T``.

默认只列出「收口累加残留」（``increment-variable``）：跨节点累加式收口一定把累计值
带进目标阶段，是 15400/25400/15604/16821/26821 同形态的确诊口径。
``--include-sets`` 额外列出「收口赋值残留」（``set-variable`` 非 0），该形态包含
QE-045 允许的领奖投影与合法标记，必须逐任务取证后才能判定，不作为批量修复输入。

Usage: python3 list_kill_closure_residue_candidates.py [--include-sets] [--verbose]
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


def load_contracts() -> dict[int, dict[int, int]]:
    contracts: dict[int, dict[int, int]] = {}
    for raw in CLIENT_MONSTER_CSV.read_text(encoding="utf-8", errors="replace").splitlines():
        cells = raw.split(",")
        if len(cells) < 2 or not cells[0].strip().isdigit():
            continue
        match = PROGRESS_RE.search(cells[1])
        if not match:
            continue
        step_section, stage, counter_section, _limit = (int(g) for g in match.groups())
        if step_section != 0 or counter_section == 0:
            continue
        contracts.setdefault(int(cells[0]), {})[stage] = counter_section
    return contracts


def main() -> int:
    verbose = "--verbose" in sys.argv
    include_sets = "--include-sets" in sys.argv
    contracts = load_contracts()
    rows: list[str] = []
    for path in sorted(QUEST_DIR.glob("*.xml")):
        match = re.fullmatch(r"(\d+)\.xml", path.name)
        if not match:
            continue
        quest_id = int(match.group(1))
        stage_counters = contracts.get(quest_id)
        if not stage_counters:
            continue
        root = ET.parse(path).getroot()
        nodes = {}
        for node in root.findall("./nodes/node"):
            nodes[node.get("label")] = {v.get("name"): int(v.get("value")) for v in node.findall("var")}
        for transition in root.findall("./transitions/transition"):
            event = transition.find("event/*")
            if event is None or event.tag != "kill-npc":
                continue
            source, target = transition.get("source"), transition.get("target")
            if source is None or target is None or source == target:
                continue
            source_stage = nodes.get(source, {}).get("var0")
            counter = stage_counters.get(source_stage) if source_stage is not None else None
            if counter is None:
                continue
            field = f"var{counter}"
            if field in nodes.get(target, {}):
                continue
            residue = None
            for action in transition.findall("./actions/*"):
                if action.get("field") != field:
                    continue
                if action.tag == "increment-variable":
                    residue = f"increment {action.get('delta', '1')}"
                elif (include_sets and action.tag == "set-variable"
                      and action.get("value") not in (None, "0")):
                    residue = f"set {action.get('value')}"
            if residue is None:
                continue
            npcs = event.get("npc-id") or event.get("npc-ids") or "-"
            rows.append(f"{quest_id}\t{source}(stage {source_stage})->{target}\t{field}\t{residue}\tkill-npc {npcs}")
    print("\n".join(rows))
    print(f"# candidates={len(rows)}", file=sys.stderr)
    if verbose:
        print("\n".join(rows), file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
