#!/usr/bin/env python3
"""全库扫描：legacy 进入 REWARD 时用到的 packed step（QE-045/QE-051 的交叉证据源）。

口径：
- 在迁移前 commit `7e9f0316c^` 的 quest handler 树里抓 `useQuestItem(env, item, step, nextStep, true, ...)`、
  `defaultCloseDialog(env, step, nextStep, true, ...)`、`checkQuestItems(env, step, nextStep, true, ...)`、
  `checkQuestItemsSimple(...)`、`changeQuestStep(env, step, nextStep, true)`
  （签名见同 commit 的 `questEngine/handlers/QuestHandler.java`）；
- 与当前 typed 定义的 `reward` 投影、客户端 quest_summary 行数对比，输出：
  * `LEGACY_NEXTSTEP_DROPPED`：当前投影 == legacy step 且 legacy nextStep == 客户端末行
    （迁移把 nextStep 丢了，需要把投影改到 nextStep + 补自愈边）；
  * `ALIGNED_TO_NEXTSTEP`：当前投影 == legacy nextStep（迁移正确）；
  * `STEP_EQUALS_NEXTSTEP`：legacy 本身 step == nextStep（REWARD 保持进入前的 packed step，属 QE-045 语义）；
  * `OTHER`：其余（多阶段/自定义节点/被 QE045_LOCKED 收紧的基线等）。

用法：python3 .agents/summary/quest-10527-reward-row/audit_legacy_reward_entry_steps.py
输出：legacy-reward-entry-scan.tsv（逐任务一行，含 legacy 调用、当前投影、客户端行数、判定）
"""

from __future__ import annotations

import csv
import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
LEGACY_REV = "7e9f0316c^"
# 已被专门门禁钉住的 QE-045 基线：reward 投影保留进入 REWARD 前的 packed step，不按“客户端末行”改。
# Test-locked QE-045 baseline: these keep the pre-REWARD packed step and must not be pushed to the last row.
TEST_LOCKED = {15300, 25300}
OUTPUT = Path(__file__).resolve().parent / "legacy-reward-entry-scan.tsv"
AUDIT_OUTPUT = Path(__file__).resolve().parent / "audit-output.tsv"

CALL_PATTERNS = [
    ("useQuestItem", re.compile(r"useQuestItem\(env, *[^,]+(?:, *[^,]+)*?, *(\d+), *(\d+), *true")),
    ("defaultCloseDialog", re.compile(r"defaultCloseDialog\(env, *(\d+), *(\d+), *true")),
    ("checkQuestItems", re.compile(r"checkQuestItems\(env, *(\d+), *(\d+), *true")),
    ("checkQuestItemsSimple", re.compile(r"checkQuestItemsSimple\(env, *(\d+), *(\d+), *true")),
    ("changeQuestStep", re.compile(r"changeQuestStep\(env, *(\d+), *(\d+), *true")),
]
GREP_PATTERN = (r"useQuestItem\(env|defaultCloseDialog\(env, *[0-9]+, *[0-9]+, *true|"
                r"checkQuestItems\(env|checkQuestItemsSimple\(env|"
                r"changeQuestStep\(env, *[0-9]+, *[0-9]+, *true")
FILE_PATTERN = re.compile(r"handlers/(?:[a-z_]+/)?_(\d{4,6})[A-Za-z_0-9]*\.java")


def legacy_calls() -> dict[int, set[tuple[str, int, int]]]:
    result = subprocess.run(
        ["git", "grep", "-n", "-E", GREP_PATTERN, LEGACY_REV,
         "--", "src/main/java/com/aionemu/gameserver/quest/handlers/*"],
        cwd=REPO, capture_output=True, text=True, check=True)
    calls: dict[int, set[tuple[str, int, int]]] = {}
    for line in result.stdout.splitlines():
        file_match = FILE_PATTERN.search(line)
        if file_match is None:
            continue
        quest_id = int(file_match.group(1))
        for helper, pattern in CALL_PATTERNS:
            for match in pattern.finditer(line):
                calls.setdefault(quest_id, set()).add(
                    (helper, int(match.group(1)), int(match.group(2))))
    return calls


def audit_rows() -> dict[int, dict[str, str]]:
    with AUDIT_OUTPUT.open(encoding="utf-8") as handle:
        return {int(row["quest_id"]): row for row in csv.DictReader(handle, delimiter="\t")}


def classify(quest_id: int, calls: set[tuple[str, int, int]], row: dict[str, str] | None) -> str:
    if row is None or not row["reward_var0"].isdigit() or not row["client_rows"].isdigit():
        return "NO_AUDIT_DATA"
    reward, rows = int(row["reward_var0"]), int(row["client_rows"])
    if row["qe045_locked"] == "True":
        return "QE045_LOCKED"
    if quest_id in TEST_LOCKED:
        return "TEST_LOCKED"
    if any(step != next_step and reward == step and rows - 1 == next_step
           for _helper, step, next_step in calls):
        return "LEGACY_NEXTSTEP_DROPPED"
    if any(step != next_step and reward == next_step for _helper, step, next_step in calls):
        return "ALIGNED_TO_NEXTSTEP"
    if all(step == next_step for _helper, step, next_step in calls):
        return "STEP_EQUALS_NEXTSTEP"
    return "OTHER"


def main() -> int:
    calls = legacy_calls()
    rows = audit_rows()
    counts: dict[str, int] = {}
    with OUTPUT.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(["quest_id", "legacy_calls", "reward_var0", "client_rows", "verdict",
                         "row_state_verdict", "classification"])
        for quest_id in sorted(calls):
            calls_for_quest = sorted(calls[quest_id])
            row = rows.get(quest_id)
            classification = classify(quest_id, set(calls_for_quest), row)
            counts[classification] = counts.get(classification, 0) + 1
            writer.writerow([
                quest_id,
                "; ".join(f"{helper}({step}->{next_step})" for helper, step, next_step in calls_for_quest),
                row["reward_var0"] if row else "",
                row["client_rows"] if row else "",
                row["verdict"] if row else "",
                row["row_state_verdict"] if row else "",
                classification,
            ])
    print(f"legacy 进入 REWARD 的 handler 任务数：{len(calls)}")
    for key in sorted(counts):
        print(f"  {key}: {counts[key]}")
    dropped = [q for q, c in sorted(calls.items())
               if classify(q, c, rows.get(q)) == "LEGACY_NEXTSTEP_DROPPED"]
    print(f"  LEGACY_NEXTSTEP_DROPPED 明细：{dropped}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
