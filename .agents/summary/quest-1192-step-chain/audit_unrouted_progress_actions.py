#!/usr/bin/env python3
"""全库审计：客户端任务对话页上的「推进类按钮动作」是否都能在 typed 任务定义里找到路由。

背景（2026-09-21，quest 1192）：
- 客户端 QUEST_Q1192.html 为三个步骤各定义了一条对话链：select2/select2_1（HACTION_SETPRO1）、
  select3/select3_1（HACTION_SELECT3_1 → HACTION_SETPRO2）、select5（HACTION_SELECT_QUEST_REWARD），
  且 quest_summary 声明了三行步骤；
- 迁移后的 1192.xml 只有一个 started(var0=0) 进行中状态，把 SETPRO1 同时挂在 203701/203833 上并直接进 reward，
  SETPRO2 / SELECT3_1 完全没有路由，select3 / select3_1 / select5 从未被引用 —— 玩家跟拉比临托斯说完话就能跳过
  第 2、3 步直接领奖（wiki 侧表现为三步共用同一条 //quest set 1192 START 0）。

判定：客户端 HTML 里出现的推进类动作（HACTION_SETPRO<n>、HACTION_SELECT<n>_<m>、HACTION_SELECT_QUEST_REWARD）
必须在任务 XML 的显式 action 里出现（NPC_START / npc-report / npc-complete 会隐式生成 QUEST_SELECT、
SELECT_QUEST_REWARD、FINISH_DIALOG 等通用动作，已计入白名单）。缺任一动作 = 候选缺陷，需人工核对。

用法：python3 audit_unrouted_progress_actions.py [quest_id ...]
输出：unrouted-progress-actions.tsv
"""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS_DIR = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
UNPACK = Path("/Users/mc/PycharmProjects/unpak")
DIALOG_DIR = UNPACK / "data_unpacked/Dialogs"
OUTPUT = Path(__file__).resolve().parent / "unrouted-progress-actions.tsv"

PROGRESS_ACTION = re.compile(r"^HACTION_(SETPRO\d+|SELECT\d+_\d+(?:_\d+)*|SELECT_QUEST_REWARD)$")
# 由简写块隐式生成、不必显式声明动作的通用路由
IMPLICIT_ACTIONS = {
    "SELECT_QUEST_REWARD",  # npc-report / npc-complete 会生成
    "USE_OBJECT",
    "QUEST_SELECT",
    "ASK_QUEST_ACCEPT",
    "QUEST_ACCEPT_1",
    "QUEST_ACCEPT_SIMPLE",
    "QUEST_REFUSE_1",
    "QUEST_REFUSE_2",
    "QUEST_REFUSE_SIMPLE",
    "FINISH_DIALOG",
}


def client_html(quest_id: int) -> Path | None:
    for name in (f"QUEST_Q{quest_id}.html", f"quest_q{quest_id}.html"):
        path = DIALOG_DIR / name
        if path.exists():
            return path
    return None


def client_rows(path: Path) -> int:
    text = path.read_text(encoding="utf-8", errors="replace")
    block = re.search(r"<steps>(.*?)</steps>", text, re.S)
    return len(re.findall(r"<step>", block.group(1))) if block else 0


def client_progress_actions(path: Path) -> set[str]:
    text = path.read_text(encoding="utf-8", errors="replace")
    names = re.findall(r"HACTION_([A-Z0-9_]+)", text)
    return {name for name in names if PROGRESS_ACTION.match("HACTION_" + name)}


def xml_actions(path: Path) -> set[str]:
    """XML 中显式路由的动作。dialog 既支持 action="X" 也支持 actions="X Y"（多动作同边），
    后者若漏解析会把已路由的动作误报为无路由（2026-09-21 复核 4338 时发现）。"""
    text = path.read_text(encoding="utf-8")
    declared = set(re.findall(r'\baction="([A-Z0-9_]+)"', text))
    for group in re.findall(r'\bactions="([^"]+)"', text):
        declared.update(token for token in group.split() if re.fullmatch(r"[A-Z0-9_]+", token))
    return declared


def main() -> int:
    requested = sorted(int(value) for value in sys.argv[1:] if value.isdigit())
    quest_ids = requested or sorted(int(p.stem) for p in QUESTS_DIR.glob("*.xml") if p.stem.isdigit())
    rows = []
    for quest_id in quest_ids:
        xml_path = QUESTS_DIR / f"{quest_id}.xml"
        html_path = client_html(quest_id)
        if not xml_path.exists() or html_path is None:
            continue
        declared = xml_actions(xml_path)
        missing = sorted(action for action in client_progress_actions(html_path)
                         if action not in declared and action not in IMPLICIT_ACTIONS)
        if not missing:
            continue
        rows.append({
            "quest_id": quest_id,
            "client_rows": client_rows(html_path),
            "missing_progress_actions": " ".join(missing),
            "declared_actions": " ".join(sorted(declared)),
            "client_file": html_path.name,
        })
    with OUTPUT.open("w", encoding="utf-8") as out:
        writer = csv.DictWriter(out, fieldnames=["quest_id", "client_rows", "missing_progress_actions",
                                                 "declared_actions", "client_file"], delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)
    multi = [row for row in rows if row["client_rows"] >= 2]
    print(f"候选任务（客户端推进动作在 XML 中无路由）：{len(rows)}；其中客户端步骤 >= 2 行：{len(multi)}")
    for row in multi[:25]:
        print(f"  {row['quest_id']}\trows={row['client_rows']}\tmissing={row['missing_progress_actions']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
