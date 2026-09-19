#!/usr/bin/env python3
"""SECTION_0 报告行 sweep（同族批量修复脚本）。

把「同一个 SECTION_0 说明行上并行门控击杀计数、最后一击直接进入 REWARD」的任务
统一补齐任务说明行索引：

1. `reward` 节点投影 `var0 = S+1`（报告行）；
2. `started -> started` 击杀自环显式 `set var0 = S`（自愈脏行索引）；
3. `started -> reward` 的 priority 0 终击路线显式 `set var0 = S+1`，并保证
   `LEVEL_AND_VISIBILITY_REFRESH`；
4. 追加无 source 的 `enter-world` 迁移修复路线（`status-is REWARD` +
   `variable-below var0 S+1` -> `set var0=S+1`）。

只处理结构完全同型的任务：`started` 节点投影 `var0=S`、击杀自环不改写 var0、
终击路线只指向 `reward`。结构不同的任务（多阶段、定制节点、非击杀完成）会被
列为 SKIP，交由人工判定。

用法：
  python3 apply_section0_report_row_sweep.py --candidates <csv> [--apply]
"""
from __future__ import annotations

import argparse
import csv
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = REPO_ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
MIGRATION_MARKER = "<!-- SECTION_0 migration repair: legacy REWARD saves -->"
NODE_RE = re.compile(r'<node label="(\w+)"[^>]*>.*?</node>', re.S)
TRANSITION_RE = re.compile(r"<transition\b.*?</transition>", re.S)


def node_block(text: str, label: str):
    for match in re.finditer(r'<node label="(\w+)"[^>]*>.*?</node>', text, re.S):
        if match.group(1) == label:
            return match.group(0)
    return None


def stage_node_label(text: str, stage: int):
    """承载客户端计数阶段的行节点：优先 started，其次任意投影 var0==stage 的 START 节点。"""
    candidates = []
    for match in re.finditer(r'<node label="(\w+)"[^>]*>(.*?)</node>', text, re.S):
        label, body = match.group(1), match.group(2)
        status = re.search(r'status="(\w+)"', match.group(0))
        variables = node_vars(match.group(0))
        if status and status.group(1) == "START" and variables.get("var0") == stage:
            candidates.append(label)
    if "started" in candidates:
        return "started"
    return candidates[0] if candidates else None


def node_vars(block: str):
    return {m.group(1): int(m.group(2))
            for m in re.finditer(r'<var name="(\w+)" value="(\d+)"', block)}


def transition_blocks(text: str):
    return list(re.finditer(r"<transition\b.*?</transition>", text, re.S))


def transition_head(block: str):
    return re.match(r"<transition\b[^>]*>", block).group(0)


def attr(head: str, name: str):
    m = re.search(rf'{name}="([^"]+)"', head)
    return m.group(1) if m else None


def var0_writes(block: str):
    writes = [int(m.group(1)) for m in re.finditer(r'<set-variable field="var0" value="(\d+)"', block)]
    writes += [f"inc{m.group(1)}" for m in re.finditer(r'<increment-variable field="var0" delta="(\d+)"', block)]
    return writes


def block_indent(block: str, position: int):
    line_start = block.rfind("\n", 0, position) + 1
    return re.match(r"[ \t]*", block[line_start:]).group(0)


def insert_first_action(block: str, action: str, quest: int):
    match = re.search(r"<actions>\s*\n(\s*)(?=\S)", block)
    if match is not None:
        return block[:match.end()] + action + "\n" + match.group(1) + block[match.end():]
    match = re.search(r"<actions\s*/>", block)
    if match is not None:
        indent = block_indent(block, match.start()) + "  "
        replacement = f"<actions>\n{indent}{action}\n{block_indent(block, match.start())}</actions>"
        return block[:match.start()] + replacement + block[match.end():]
    match = re.search(r"<after-commit>|<\/transition>", block)
    if match is None:
        raise ValueError(f"quest {quest}: cannot locate an insertion point for <actions>")
    indent = block_indent(block, match.start())
    replacement = f"<actions>\n{indent}  {action}\n{indent}</actions>\n{indent}"
    return block[:match.start()] + replacement + block[match.start():]


def check_preconditions(text: str, stage: int, quest: int):
    reward = node_block(text, "reward")
    started = node_block(text, "started")
    if reward is None or started is None:
        return "missing started/reward node"
    stage_label = stage_node_label(text, stage)
    if stage_label is None:
        return f"no START node projects SECTION_0={stage}"
    if MIGRATION_MARKER in text:
        return "migration route already present"
    completing = []
    for match in transition_blocks(text):
        block = match.group(0)
        head = transition_head(block)
        source, target = attr(head, "source"), attr(head, "target")
        if source != stage_label:
            continue
        if "<kill-npc" not in block:
            continue
        if target not in ("started", "reward"):
            return f"kill route targets custom node {target}"
        writes = var0_writes(block)
        if target == "started" and writes:
            return f"kill self-loop already writes var0 ({writes})"
        if target == "reward":
            if writes:
                return f"completing kill route already writes var0 ({writes})"
            completing.append(block)
    if not completing:
        return "no started->reward kill route"
    return None


def patch_quest(quest: int, stage: int, text: str):
    report = stage + 1
    node_label = stage_node_label(text, stage)
    reward = node_block(text, "reward")
    if re.search(r'<var name="var0" value="\d+"\s*/>', reward):
        new_reward = re.sub(r'(<var name="var0" value=")\d+(")', rf"\g<1>{report}\g<2>", reward, count=1)
    else:
        opening = re.search(r'<node label="reward"[^>]*>\s*\n', reward)
        if opening is None:
            raise ValueError(f"quest {quest}: reward node is not pretty-printed")
        new_reward = (reward[:opening.end()]
                      + f'      <var name="var0" value="{report}"/>\n'
                      + reward[opening.end():])
    text = text.replace(reward, new_reward, 1)

    def patch_transition(match: re.Match[str]):
        block = match.group(0)
        head = transition_head(block)
        if attr(head, "source") != node_label or "<kill-npc" not in block:
            return block
        target = attr(head, "target")
        if target == node_label:
            return insert_first_action(block, f'<set-variable field="var0" value="{stage}"/>', quest)
        if target == "reward":
            block = insert_first_action(block, f'<set-variable field="var0" value="{report}"/>', quest)
            block = block.replace('<sync-quest-state mode="PACKET_ONLY"/>',
                                  '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>')
            return block
        return block

    text = TRANSITION_RE.sub(patch_transition, text)

    opening = re.search(r"<transitions[^>]*>\s*\n", text)
    if opening is None:
        raise ValueError(f"quest {quest}: transitions block is not pretty-printed")
    repair = (
        f'    {MIGRATION_MARKER}\n'
        '    <transition target="reward">\n'
        '      <event>\n'
        '        <enter-world/>\n'
        '      </event>\n'
        '      <conditions>\n'
        '        <status-is status="REWARD"/>\n'
        f'        <variable-below field="var0" value="{report}"/>\n'
        '      </conditions>\n'
        '      <actions>\n'
        f'        <set-variable field="var0" value="{report}"/>\n'
        '      </actions>\n'
        '      <after-commit>\n'
        '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        '      </after-commit>\n'
        '    </transition>\n'
    )
    return text[:opening.end()] + repair + text[opening.end():]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--candidates", required=True, type=Path)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--verdicts", default="SAME_CLASS_CONFIRMED",
                        help="参与 sweep 的审计判定，逗号分隔（默认仅 SAME_CLASS_CONFIRMED）")
    args = parser.parse_args()

    verdicts = {value.strip() for value in args.verdicts.split(",") if value.strip()}
    rows = [row for row in csv.DictReader(args.candidates.open(encoding="utf-8"))
            if row["verdict"] in verdicts]
    applied, skipped = [], []
    for row in sorted(rows, key=lambda r: int(r["quest"])):
        quest, stage = int(row["quest"]), int(row["stage"])
        path = QUEST_DIR / f"{quest}.xml"
        text = path.read_text(encoding="utf-8")
        reason = check_preconditions(text, stage, quest)
        if reason:
            skipped.append((quest, stage, reason))
            continue
        updated = patch_quest(quest, stage, text)
        if updated != text and args.apply:
            path.write_text(updated, encoding="utf-8")
        applied.append((quest, stage))
    mode = "APPLIED" if args.apply else "DRY-RUN"
    print(f"[{mode}] patchable quests: {len(applied)}")
    print(f"[{mode}] skipped quests: {len(skipped)}")
    for quest, stage, reason in skipped:
        print(f"  SKIP {quest} stage={stage}: {reason}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
