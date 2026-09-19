#!/usr/bin/env python3
"""审计击杀计数任务的 SECTION_0 报告行闭环 (step index closure audit).

客户端合同（Aion 5.8 `Quest_unpacked/quest_monster.csv` + `Dialogs/quest_q*.html`）：
同一个任务说明行 S 可以并行门控多个计数器 `SECTION_n<N`；当最后一击杀满后，
服务端必须把 `SECTION_0`（任务说明行索引）推进到报告行 S+1，否则服务端虽然进入
`REWARD`，客户端任务说明仍停在击杀行。

判定使用 QuestMutationPlanner 的真实语义：先应用 transition actions，随后目标节点
投影只覆盖未被动作触及的字段。因此“最后一次击杀”后的 var0 =
显式 set/increment 值，否则为 reward/目标节点投影，否则保持原值。

用法：
  python3 audit_section0_report_row_closure.py \
      --client-quest-csv /path/to/quest_monster.csv \
      --client-dialog-root /path/to/Dialogs \
      --legacy-handler-root /path/to/quest/handlers     # 可选
"""
from __future__ import annotations

import argparse
import csv
import re
import sys
from collections import defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = REPO_ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
CATALOG = REPO_ROOT / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"

SECTION_RE = re.compile(r"SECTION_(\d+)\s*<\s*(\d+)")
STAGE_RE = re.compile(r"SECTION_0\s*==\s*(\d+)")


def executable_ids() -> set[int]:
    text = CATALOG.read_text(encoding="utf-8")
    return {int(m.group(1)) for m in re.finditer(r'<definition id="(\d+)"[^>]*mode="EXECUTABLE"', text)}


def client_counters(csv_path: Path):
    out = defaultdict(lambda: defaultdict(dict))
    with csv_path.open(encoding="utf-8-sig") as handle:
        for row in csv.DictReader(handle, skipinitialspace=True):
            quest = int(row["questId"])
            progress = row["questProgress"] or ""
            if "SECTION_" not in progress:
                continue
            stage_match = STAGE_RE.search(progress)
            stage = int(stage_match.group(1)) if stage_match else 0
            for section, threshold in SECTION_RE.findall(progress):
                if section != "0":
                    out[quest][stage][int(section)] = int(threshold)
    return out


def html_steps(quest: int, dialog_root: Path | None):
    if dialog_root is None:
        return None, None
    for directory in sorted(p for p in dialog_root.glob("*") if p.is_dir()):
        path = directory / f"quest_q{quest}.html"
        if path.exists():
            text = path.read_text(encoding="utf-8", errors="ignore")
            summary = re.search(r'name="quest_summary".*?</HtmlPage>', text, re.S)
            if summary is None:
                return path, 0
            return path, len(re.findall(r"<step>", summary.group(0)))
    return None, None


def parse_node(text: str, label: str):
    match = re.search(rf'<node label="{label}"[^>]*>(.*?)</node>', text, re.S)
    if match is None:
        return None
    return {
        "status": (re.search(r'status="(\w+)"', match.group(0)) or [None, None])[1],
        "vars": {m.group(1): int(m.group(2))
                 for m in re.finditer(r'<var name="(\w+)" value="(\d+)"', match.group(1))},
    }


def parse_transitions(text: str):
    out = []
    for block in re.findall(r"<transition\b.*?</transition>", text, re.S):
        head = re.match(r"<transition\b[^>]*>", block).group(0)
        source = re.search(r'source="([^"]+)"', head)
        target = re.search(r'target="([^"]+)"', head)
        priority = re.search(r'priority="([^"]+)"', head)
        actions = [("set", mm.group(1), int(mm.group(2)))
                   for mm in re.finditer(r'<set-variable field="(\w+)" value="(\d+)"', block)]
        actions += [("increment", mm.group(1), int(mm.group(2)))
                    for mm in re.finditer(r'<increment-variable field="(\w+)" delta="(\d+)"', block)]
        out.append({
            "source": source.group(1) if source else None,
            "target": target.group(1) if target else None,
            "priority": int(priority.group(1)) if priority else None,
            "kill": "<kill-npc" in block,
            "actions": actions,
            "refresh": 'mode="LEVEL_AND_VISIBILITY_REFRESH"' in block,
            "enter_world": "<enter-world/>" in block,
        })
    return out


def analyze(quest: int, stage: int):
    path = QUEST_DIR / f"{quest}.xml"
    if not path.exists():
        return None
    text = path.read_text(encoding="utf-8")
    nodes = {label: parse_node(text, label)
             for label in ("unaccepted", "started", "ready", "reward", "complete")}
    transitions = parse_transitions(text)

    kill_var0 = []
    for transition in transitions:
        if transition["source"] != "started" or not transition["kill"]:
            continue
        value = stage
        touched = False
        for kind, field, amount in transition["actions"]:
            if field == "var0":
                value = amount if kind == "set" else value + amount
                touched = True
        target = nodes.get(transition["target"])
        if not touched and target and "var0" in target["vars"]:
            value = target["vars"]["var0"]
        kill_var0.append((transition["target"], value))

    later = [amount for transition in transitions
             if transition["source"] in (None, "started", "ready", "reward")
             and transition["target"] == "reward" and not transition["kill"]
             for kind, field, amount in transition["actions"]
             if field == "var0" and kind == "set"]
    reward = nodes.get("reward") or {"vars": {}}
    reward_var0 = reward["vars"].get("var0")
    candidates = [value for _, value in kill_var0]
    if reward_var0 is not None:
        candidates.append(reward_var0)
    candidates += later
    return {
        "reward_var0": reward_var0,
        "kill_targets": sorted({target for target, _ in kill_var0}),
        "kill_var0": sorted({value for _, value in kill_var0}),
        "best": max(candidates) if candidates else None,
        "migration_repair": any(transition["source"] is None and transition["target"] == "reward"
                                and transition["enter_world"] for transition in transitions),
    }


def legacy_writes(quest: int, legacy_root: Path | None):
    if legacy_root is None:
        return None, []
    hits = sorted(legacy_root.rglob(f"_{quest}*.java"))
    if not hits:
        return None, []
    text = "\n".join(path.read_text(encoding="utf-8", errors="ignore") for path in hits)
    writes = re.findall(r"setQuestVarById\(\s*0\s*,\s*([^)]+)\)", text)
    writes += re.findall(r"setQuestVar\(\s*0\s*,\s*([^)]+)\)", text)
    return hits[0].name, writes


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--client-quest-csv", required=True, type=Path)
    parser.add_argument("--client-dialog-root", type=Path)
    parser.add_argument("--legacy-handler-root", type=Path)
    parser.add_argument("--out-csv", type=Path)
    args = parser.parse_args()

    executables = executable_ids()
    counters = client_counters(args.client_quest_csv)
    rows = []
    for quest, stages in sorted(counters.items()):
        if quest not in executables:
            continue
        for stage, sections in sorted(stages.items()):
            if not sections:
                continue
            info = analyze(quest, stage)
            if info is None:
                continue
            path, steps = html_steps(quest, args.client_dialog_root)
            if steps is not None and steps <= stage + 1:
                continue  # 客户端任务说明没有报告行，无需推进行索引
            legacy_name, writes = legacy_writes(quest, args.legacy_handler_root)
            closed = info["best"] is not None and info["best"] >= stage + 1
            if closed:
                continue
            if legacy_name and any(re.sub(r"\D", "", w) not in ("", "0") for w in writes):
                verdict = "SAME_CLASS_CONFIRMED"
            elif legacy_name:
                verdict = "REVIEW_LEGACY_NO_VAR0"
            else:
                verdict = "REVIEW_NO_LEGACY"
            rows.append({
                "quest": quest,
                "stage": stage,
                "counters": ";".join(f"SECTION_{k}<{v}" for k, v in sorted(sections.items())),
                "reward_projection_var0": info["reward_var0"],
                "kill_route_var0": ";".join(str(v) for v in info["kill_var0"]),
                "kill_route_targets": ";".join(info["kill_targets"]),
                "migration_repair_present": info["migration_repair"],
                "html_steps": steps,
                "html_file": path.name if path else "",
                "legacy_handler": legacy_name or "",
                "legacy_var0_writes": ";".join(writes),
                "verdict": verdict,
            })

    counts = defaultdict(int)
    for row in rows:
        counts[row["verdict"]] += 1
    print(f"executable quests: {len(executables)}")
    print(f"section0 report-row closure candidates: {len(rows)} -> {dict(sorted(counts.items()))}")
    for verdict in ("SAME_CLASS_CONFIRMED", "REVIEW_LEGACY_NO_VAR0", "REVIEW_NO_LEGACY"):
        ids = [row["quest"] for row in rows if row["verdict"] == verdict]
        print(f"{verdict} ({len(ids)}): {' '.join(str(i) for i in sorted(set(ids)))}")

    if args.out_csv:
        args.out_csv.parent.mkdir(parents=True, exist_ok=True)
        with args.out_csv.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
            writer.writeheader()
            writer.writerows(rows)
        print(f"wrote {args.out_csv}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
