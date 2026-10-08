#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""逐候选抽取 legacy handler 的「谁能置 REWARD / 谁能发结束对话」事实。

Per-candidate extraction of legacy-handler facts: which target NPC can set REWARD
and which can send the quest-end (reward) dialog.

输出 / Output: legacy-facts.tsv —— quest / target / sets_reward / sends_end_dialog / sends_page / context。
判定依赖 `if (targetId == X)` 块（旧 handler 的统一写法）与块内的
`setStatus(QuestStatus.REWARD)` / `sendQuestEndDialog` / `sendQuestDialog(env, page)` 调用。
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").exists())
LEGACY_TREE = "7e9f0316c^"

CANDIDATES = [1582, 18808, 21105, 2232, 2237, 2307, 2664, 28302, 28303, 28808,
              30211, 30213, 30311, 4004, 4012, 80707, 80833]


def legacy_body(quest_id: int) -> tuple[str | None, str]:
    tree = subprocess.run(["git", "ls-tree", "-r", "--name-only", LEGACY_TREE], cwd=REPO,
                          capture_output=True, text=True).stdout
    hits = [line for line in tree.splitlines()
            if "/quest/handlers/" in line and re.search(rf"/_{quest_id}[A-Za-z]", line)]
    if not hits:
        return None, ""
    path = hits[0]
    return path, subprocess.run(["git", "show", f"{LEGACY_TREE}:{path}"], cwd=REPO,
                                capture_output=True, text=True).stdout


def current_status_branch(lines: list[str], index: int) -> str:
    """向上找最近的 `getStatus() == QuestStatus.X` 判定（粗粒度，够用）。"""
    for j in range(index, max(-1, index - 60), -1):
        match = re.search(r"getStatus\(\)\s*==\s*QuestStatus\.(\w+)", lines[j])
        if match:
            return match.group(1)
    return "?"


def analyze(path: str, body: str) -> list[dict[str, object]]:
    lines = body.splitlines()
    facts: list[dict[str, object]] = []
    current_target: int | None = None
    current_start = 0
    for i, line in enumerate(lines):
        match = re.search(r"targetId\s*==\s*(\d+)", line)
        if match:
            if current_target is not None:
                facts.append(_summarize(lines, current_target, current_start, i))
            current_target = int(match.group(1))
            current_start = i
    if current_target is not None:
        facts.append(_summarize(lines, current_target, current_start, len(lines)))
    # 合并同一 target 的多段（旧 handler 按状态分支多次判 targetId）
    merged: dict[int, dict[str, object]] = {}
    for fact in facts:
        target = int(fact["target"])
        slot = merged.setdefault(target, {"target": target, "sets_reward": False,
                                          "sends_end_dialog": False, "pages": set(),
                                          "branches": set()})
        slot["sets_reward"] = bool(slot["sets_reward"]) or bool(fact["sets_reward"])
        slot["sends_end_dialog"] = bool(slot["sends_end_dialog"]) or bool(fact["sends_end_dialog"])
        slot["pages"] |= fact["pages"]
        slot["branches"] |= fact["branches"]
    return list(merged.values())


def _summarize(lines: list[str], target: int, start: int, end: int) -> dict[str, object]:
    chunk = lines[start:end]
    text = "\n".join(chunk)
    pages = set(re.findall(r"sendQuestDialog\(env,\s*(\d+)\)", text))
    pages |= set(re.findall(r"sendQuestDialog\(env,\s*QuestDialog\.(\w+)\)", text))
    branches = {current_status_branch(lines, start)}
    return {
        "target": target,
        "sets_reward": bool(re.search(r"setStatus\(QuestStatus\.REWARD\)", text)),
        "sends_end_dialog": "sendQuestEndDialog" in text,
        "pages": pages,
        "branches": branches,
    }


def main() -> int:
    out = Path(__file__).parent / "legacy-facts.tsv"
    with out.open("w", encoding="utf-8") as handle:
        handle.write("quest\ttarget\tsets_reward\tsends_end_dialog\tpages\tstatus_branch\tlegacy_path\n")
        for quest in CANDIDATES:
            path, body = legacy_body(quest)
            if not path:
                handle.write(f"{quest}\t-\t-\t-\t-\t-\t(missing)\n")
                continue
            for fact in analyze(path, body):
                handle.write("\t".join([
                    str(quest), str(fact["target"]),
                    "yes" if fact["sets_reward"] else "no",
                    "yes" if fact["sends_end_dialog"] else "no",
                    ",".join(sorted(map(str, fact["pages"]))) or "-",
                    ",".join(sorted(fact["branches"])),
                    path,
                ]) + "\n")
    print(f"wrote {out}")
    print(out.read_text(encoding="utf-8"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
