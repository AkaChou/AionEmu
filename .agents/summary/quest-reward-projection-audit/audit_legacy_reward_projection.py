#!/usr/bin/env python3
"""审计所有历史 legacy handler 的 REWARD 进入方式，与当前任务 XML 的 reward 投影比对。

背景：旧 `QuestHandler.changeQuestStep(env, from, to, true)` 只把状态置为 REWARD，
不会写入 `to`；因此迁移到 XML 时 reward 节点必须投影为 `from`，并补一条
`REWARD && var0 == to` 的无 source `enter-world` 恢复边（见 f6aff952a、15300/25300 修复）。

用法：
    python3 .agents/summary/quest-reward-projection-audit/audit_legacy_reward_projection.py
输出：
    report.tsv（逐任务判定）与 stdout 汇总。
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
HANDLER_DIR = "src/main/java/com/aionemu/gameserver/quest/handlers"
QUESTS_DIR = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

REWARD_STEP_RE = re.compile(
    r"changeQuestStep\s*\(\s*env\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*true\s*\)"
)
QUEST_ID_RE = re.compile(r"questId\s*=\s*(\d+)")
FILE_ID_RE = re.compile(r"_(\d{3,6})")


def deleted_handler_paths() -> dict[str, str]:
    """返回 path -> 最近一次删除它的 commit。"""
    out = subprocess.run(
        ["git", "log", "--all", "--diff-filter=D", "--pretty=format:commit %H", "--name-only",
         "--", HANDLER_DIR],
        cwd=REPO, capture_output=True, text=True, check=True,
    ).stdout
    paths: dict[str, str] = {}
    commit = None
    for line in out.splitlines():
        if line.startswith("commit "):
            commit = line.split()[1]
        elif line.strip() and commit:
            # git log 由新到旧，首次出现的删除提交即为最近一次删除。
            paths.setdefault(line.strip(), commit)
    return paths


def batch_contents(specs: list[str]) -> list[str | None]:
    """按 `<rev>:<path>` 批量读取 blob 内容（单进程 cat-file --batch）。"""
    payload = ("\n".join(specs) + "\n").encode()
    out = subprocess.run(["git", "cat-file", "--batch"], cwd=REPO,
                         input=payload, capture_output=True, check=True).stdout
    contents: list[str | None] = []
    pos = 0
    while pos < len(out):
        nl = out.index(b"\n", pos)
        header = out[pos:nl].decode()
        pos = nl + 1
        if header.endswith(" missing"):
            contents.append(None)
            continue
        _, _, size = header.split()
        size = int(size)
        contents.append(out[pos:pos + size].decode("utf-8", "replace"))
        pos += size + 1
    return contents


def reward_projection(quest_id: int) -> tuple[str, str, str]:
    """读取当前 XML：reward 投影 var0、恢复边 var0 集合、START->reward 的 set-variable。"""
    path = QUESTS_DIR / f"{quest_id}.xml"
    if not path.exists():
        return "NO_XML", "", ""
    tree = ET.parse(path)
    root = tree.getroot()
    reward_nodes = [n for n in root.iter("node") if n.get("status") == "REWARD"]
    if len(reward_nodes) != 1:
        return f"MULTI_REWARD_NODE({len(reward_nodes)})", "", ""
    reward_label = reward_nodes[0].get("label")
    var0 = None
    for var in reward_nodes[0].iter("var"):
        if var.get("name") == "var0":
            var0 = var.get("value")
    recovery: list[str] = []
    handover_writes: list[str] = []
    for transition in root.iter("transition"):
        conditions = transition.find("conditions")
        cond_status = cond_var = None
        if conditions is not None:
            status_el = conditions.find("status-is")
            var_el = conditions.find("variable-is")
            if status_el is not None:
                cond_status = status_el.get("status")
            if var_el is not None and var_el.get("field") == "var0":
                cond_var = var_el.get("value")
        if (transition.get("source") is None and transition.get("target") == reward_label
                and cond_status == "REWARD" and cond_var is not None):
            recovery.append(cond_var)
        if transition.get("target") == reward_label and transition.get("source") is not None:
            actions = transition.find("actions")
            if actions is not None:
                for set_var in actions.iter("set-variable"):
                    if set_var.get("field") == "var0":
                        handover_writes.append(set_var.get("value") or "?")
    return var0 or "NONE", ",".join(sorted(set(recovery))), ",".join(handover_writes)


def main() -> int:
    paths = deleted_handler_paths()
    specs = [f"{commit}^:{path}" for path, commit in paths.items()]
    contents = batch_contents(specs)

    rows = []
    skipped_ambiguous = []
    for (path, commit), content in zip(paths.items(), contents):
        if content is None or "QuestHandler" not in content:
            continue
        entries = REWARD_STEP_RE.findall(content)
        if not entries:
            continue
        ids = set(int(m) for m in QUEST_ID_RE.findall(content))
        file_id = FILE_ID_RE.search(os.path.basename(path))
        if file_id:
            ids.add(int(file_id.group(1)))
        ids = sorted(ids)
        if len(ids) != 1:
            skipped_ambiguous.append((path, ids))
            continue
        quest_id = ids[0]
        froms = sorted({int(a) for a, _ in entries})
        tos = sorted({int(b) for _, b in entries})
        if len(froms) != 1 or len(tos) != 1:
            skipped_ambiguous.append((path, [quest_id], froms, tos))
            continue
        expected_from, expected_to = froms[0], tos[0]
        var0, recovery, handover = reward_projection(quest_id)
        if var0 == "NO_XML":
            verdict = "NO_XML"
        elif expected_from == expected_to:
            verdict = "SELF_REWARD_NO_STEP"
        elif var0 != str(expected_from):
            verdict = "MISMATCH_PROJECTION"
        elif str(expected_to) not in recovery.split(","):
            verdict = "MISSING_RECOVERY_EDGE"
        elif handover:
            verdict = "HANDOVER_REWRITES_STEP"
        else:
            verdict = "OK"
        rows.append((quest_id, path, expected_from, expected_to, var0, recovery, handover, verdict))

    report = Path(__file__).with_name("report.tsv")
    with report.open("w", encoding="utf-8") as handle:
        handle.write("quest_id\thandler_path\texpected_from\texpected_to\txml_var0\trecovery_vars\thandover_writes\tverdict\n")
        for row in sorted(rows):
            handle.write("\t".join(str(cell) for cell in row) + "\n")

    counts: dict[str, int] = {}
    for row in rows:
        counts[row[-1]] = counts.get(row[-1], 0) + 1
    print(f"handlers with reward entry: {len(rows)}; skipped(ambiguous ids): {len(skipped_ambiguous)}")
    for verdict, count in sorted(counts.items()):
        print(f"  {verdict}: {count}")
    print("\nnon-OK rows:")
    for row in sorted(rows):
        if row[-1] != "OK":
            print("  " + "\t".join(str(cell) for cell in row))
    if skipped_ambiguous:
        print("\nskipped (needs manual triage):")
        for item in skipped_ambiguous:
            print("  " + str(item))
    print(f"\nreport: {report}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
