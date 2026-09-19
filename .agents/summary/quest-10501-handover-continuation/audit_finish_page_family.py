#!/usr/bin/env python3
"""枚举下发客户端确认页 CHECK_USER_ITEM_OK(10000) 的任务，并按该任务客户端 HTML 中

该页唯一按钮的动作分类：1009 自洽领奖、1008 客户端本地关闭（死端形态）、其他客户端任务动作
（故事翻页，需要链根）。
Enumerates quests that display the client confirmation page CHECK_USER_ITEM_OK(10000) and classifies
them by the button action the quest's own client HTML declares for that page: 1009 self-contained
reward claim, 1008 client-local close (dead-end shape), other client quest actions (story page turns
that need a chain root).

输入 / Inputs:
  src/main/resources/aion/data/static_data/quest_definition/quests/*.xml
  docs/quest/client-dialog-mapping/quest-dialog-action-details.csv
  src/main/java/com/aionemu/gameserver/questEngine/definition/QuestDialogAction.java

用法 / Usage:
  python3 .agents/summary/quest-10501-handover-continuation/audit_finish_page_family.py \
    > .agents/summary/quest-10501-handover-continuation/client-finish-page-family.csv
"""
from __future__ import annotations

import csv
import glob
import os
import re
import sys
from collections import Counter

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
QUEST_XML_DIR = os.path.join(REPO, "src", "main", "resources", "aion", "data", "static_data",
                            "quest_definition", "quests")
DETAILS = os.path.join(REPO, "docs", "quest", "client-dialog-mapping",
                       "quest-dialog-action-details.csv")
ACTIONS = os.path.join(REPO, "src", "main", "java", "com", "aionemu", "gameserver", "questEngine",
                       "definition", "QuestDialogAction.java")

FINISH_DIALOG_ID = 1008
SELECT_REWARD_ID = 1009
PAGE_ID = "10000"


def quest_actions() -> dict[int, str]:
    """QuestDialogAction.java 中服务端已建模的任务动作 id -> 常量名。"""
    text = open(ACTIONS, encoding="utf-8").read()
    found = {}
    for name, raw in re.findall(r"^\t([A-Z][A-Z0-9_]*)\((-?\d+)\)", text, flags=re.MULTILINE):
        found[int(raw)] = name
    return found


def classify(action_ids: set[int], known: dict[int, str]) -> str:
    if action_ids == {SELECT_REWARD_ID}:
        return "SELF_CONTAINED_REWARD_ACTION"
    if action_ids == {FINISH_DIALOG_ID}:
        return "CLIENT_LOCAL_CLOSE"
    if action_ids and all(action in known for action in action_ids):
        return "STORY_PAGE_TURN"
    return "UNKNOWN_ACTION"


def main() -> int:
    known = quest_actions()
    rows_by_quest_page: dict[tuple[str, str], list[dict]] = {}
    with open(DETAILS, encoding="utf-8-sig", newline="") as handle:
        for row in csv.DictReader(handle):
            rows_by_quest_page.setdefault((row["quest_id"], row["page_id"]), []).append(row)

    out = []
    counts = Counter()
    for path in sorted(glob.glob(os.path.join(QUEST_XML_DIR, "*.xml"))):
        quest_id = os.path.splitext(os.path.basename(path))[0]
        text = open(path, encoding="utf-8").read()
        if 'page="CHECK_USER_ITEM_OK"' not in text:
            continue
        client_rows = rows_by_quest_page.get((quest_id, PAGE_ID), [])
        actions = {int(row["action_id"]) for row in client_rows if row["action_id"]}
        classification = classify(actions, known) if client_rows else "NO_CLIENT_MAPPING"
        counts[classification] += 1
        out.append({
            "quest_id": quest_id,
            "displayed_page": PAGE_ID,
            "client_action_ids": " ".join(str(value) for value in sorted(actions)),
            "client_action_constants": " ".join(sorted({row["action_constant"] for row in client_rows})),
            "button_text_zh": " | ".join(sorted({row["button_text_zh"] for row in client_rows})),
            "classification": classification,
            "xml_file": os.path.relpath(path, REPO),
        })

    writer = csv.DictWriter(sys.stdout, fieldnames=list(out[0].keys()))
    writer.writeheader()
    writer.writerows(out)
    print(f"# quests displaying CHECK_USER_ITEM_OK: {len(out)}", file=sys.stderr)
    for name, value in counts.most_common():
        print(f"#   {name}: {value}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
