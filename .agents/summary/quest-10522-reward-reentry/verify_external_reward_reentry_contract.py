#!/usr/bin/env python3
"""静态校验“引擎外推进 REWARD”任务的领奖合同（不依赖 Maven）。

`ExternalRewardAdvanceReentryContractTest` 在编译后的 IR 上断言同样四件事；本脚本用 XML 源文本做等价只读
检查，便于在没有构建授权时先确认 XML 侧合同：

  1. reward 节点投影 var0 == 基线中的写入方步数；
  2. 每个完成 NPC 都有 `reward -> reward` + `TALK_TO_NPC(QUEST_SELECT 31)` -> `SHOW_QUEST_PAGE(DEFAULT_SUCCESS)`；
  3. `started -> reward` 事务不写 `<set-variable field="var0">`；
  4. enter-world 自愈边的 status-is/variable-is 条件与基线 staleRewardSteps 顺序一致。

用法：python3 .agents/summary/quest-10522-reward-reentry/verify_external_reward_reentry_contract.py
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUEST_DIR = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
BASELINE = REPO / "src/test/resources/quest/external-reward-advance-baseline.tsv"

TRANSITION_RE = re.compile(r"<transition\b(?P<head>[^>]*)>(?P<body>.*?)</transition>", re.S)
NODE_RE = re.compile(r'<node label="reward"[^>]*>(?P<body>.*?)</node>', re.S)


def read_baseline() -> list[dict]:
    rows = []
    for line in BASELINE.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        quest_id, writer, step, projection, npcs, stale = (line.split("\t") + [""] * 6)[:6]
        rows.append({
            "questId": int(quest_id),
            "writer": writer,
            "step": int(step),
            "projection": int(projection),
            "npcs": [int(token) for token in npcs.split()],
            "stale": [] if stale.strip() in ("", "-") else [int(token) for token in stale.split()],
        })
    return rows


def reward_projection(text: str) -> int:
    body = NODE_RE.search(text)
    if body is None:
        raise AssertionError("missing reward node")
    match = re.search(r'<var name="var0" value="(\d+)"/>', body.group("body"))
    if match is None:
        raise AssertionError("missing reward var0 projection")
    return int(match.group(1))


def reward_entry_npcs(text: str) -> list[int]:
    found = []
    for block in TRANSITION_RE.finditer(text):
        head, body = block.group("head"), block.group("body")
        if 'source="reward"' not in head or 'target="reward"' not in head:
            continue
        npc = re.search(r'<dialog type="TALK_TO_NPC" npc-id="(\d+)" action="QUEST_SELECT"/>', body)
        if npc and 'page="DEFAULT_SUCCESS"' in body:
            found.append(int(npc.group(1)))
    return found


def recovery_values(text: str) -> list[int]:
    values = []
    for block in TRANSITION_RE.finditer(text):
        head, body = block.group("head"), block.group("body")
        if "source=" in head or 'target="reward"' not in head:
            continue
        if "<enter-world/>" not in body or '<status-is status="REWARD"/>' not in body:
            continue
        conditions = re.findall(r"<(status-is|variable-is)\b[^>]*/>", body)
        if conditions != ["status-is", "variable-is"]:
            raise AssertionError(f"unexpected recovery condition order: {conditions}")
        match = re.search(r'<variable-is field="var0" value="(\d+)"/>', body)
        if match is None:
            raise AssertionError("recovery route without var0 condition")
        values.append(int(match.group(1)))
    return values


def started_to_reward_writes_var0(text: str) -> bool:
    for block in TRANSITION_RE.finditer(text):
        head, body = block.group("head"), block.group("body")
        if 'source="started"' not in head or 'target="reward"' not in head:
            continue
        if re.search(r'<set-variable field="var0"\b', body):
            return True
    return False


def main() -> int:
    failures = []
    for row in read_baseline():
        quest_id = row["questId"]
        text = (QUEST_DIR / f"{quest_id}.xml").read_text(encoding="utf-8")
        if row["step"] != row["projection"]:
            failures.append(f"{quest_id}: baseline rows disagree on writer step/projection")
        projection = reward_projection(text)
        if projection != row["step"]:
            failures.append(f"{quest_id}: reward projection {projection} != writer step {row['step']}")
        entry_npcs = reward_entry_npcs(text)
        for npc in row["npcs"]:
            if npc not in entry_npcs:
                failures.append(f"{quest_id}: NPC {npc} has no reward-state entry page")
        for npc in entry_npcs:
            if npc not in row["npcs"]:
                failures.append(f"{quest_id}: entry page for unexpected NPC {npc}")
        if started_to_reward_writes_var0(text):
            failures.append(f"{quest_id}: started -> reward still rewrites var0")
        recovery = recovery_values(text)
        if recovery != row["stale"]:
            failures.append(f"{quest_id}: recovery {recovery} != baseline stale steps {row['stale']}")
        print(f'OK {quest_id}: projection={projection} entry-npcs={entry_npcs} recovery={recovery}')

    if failures:
        print("\nFAILED / 失败：")
        for failure in failures:
            print("  " + failure)
        return 1
    print(f"\nstatic contract verified for {len(read_baseline())} quests / 静态合同校验通过")
    return 0


if __name__ == "__main__":
    sys.exit(main())
