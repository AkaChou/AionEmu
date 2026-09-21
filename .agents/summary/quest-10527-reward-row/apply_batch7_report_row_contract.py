#!/usr/bin/env python3
"""批次 7：QE-051 领奖行合同——“2/3 行报告·交付型”末行投影修复（21 个任务）。

证据（每个任务逐条核对，详见 batch7-evidence.tsv）：
1. 客户端解包 `data_unpacked/Dialogs/**/quest_q<id>.html` 的 `quest_summary` 行清单（权威行数）。
2. 服务端 typed 定义里 reward 节点投影停在更早的行号 → 客户端最后一行的 START/REWARD 状态缺口
   （审计 verdict：MISSING_LAST_ROW / ROW_BEHIND）。
3. 末行 NPC 必须是该任务领奖路线上的 NPC；天/魔镜像同形时作为独立旁证。

三组：
- A 组（18 个成长任务 19672..19694 / 29672..29694）：客户端 2 行（收集成长货币 → 向成长支援教官报告），
  定义里只有唯一 NPC 806698（天 LC1_L_grow_npc_Rena_01）/806700（魔 DC1_D_grow_npc_Melrania_01），
  即 accept 与领奖共用 NPC；reward 投影 0 → 1，并补 enter-world 自愈边。
- B 组（3210）：客户端 3 行（救出 Shulack_01 → 除掉头目 → 和 Shugo_Shulack_02 对话），
  定义已有 k1(var0=1)/k2(var0=2) 状态但 reward 停在 0；镜像 4210 的 reward 已是 2 且带 enter-world 自愈边。
  reward 投影 0 → 2，并补 0 → 2、1 → 2 两条 enter-world 自愈边（旧 handler 的存档 var0 可能停在 0/1）。
- C 组（18036/28036）：客户端 2 行（和德拉坎战士对话 → 向 Demades/Latkel 报告）。旧定义的
  `started(SETPRO1)` 事务把 var0 写成 1，但 `started` 节点投影仍是 0，而 `var0>=1` 的两条事务以
  `started` 为 source——按 QuestMutationPlanner.matchesSourceNode 的“投影变量必须全等”语义，
  这些事务永远匹配不到，任务在交出物品后卡死。修复：新增 `s1`(START, var0=1) 节点、把 SETPRO1 事务
  指到 s1、把 var0>=1 的两条事务改为以 s1 为 source，reward 投影 0 → 1 并补 enter-world 自愈边。

用法：
    python3 .agents/summary/quest-10527-reward-row/apply_batch7_report_row_contract.py            # 应用
    python3 .agents/summary/quest-10527-reward-row/apply_batch7_report_row_contract.py --check      # 只校验
"""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
UNPACK = Path("/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs")
EVIDENCE = Path(__file__).resolve().parent / "batch7-evidence.tsv"

# (quest_id, client_rows, last_row, reward_npc_id, reward_npc_name, last_row_text)
GROUP_A = [
    (19672, 2, 1, [0], 806698, "LC1_L_grow_npc_Rena_01", "向成长支援教官报告"),
    (19677, 2, 1, [0], 806698, "LC1_L_grow_npc_Rena_01", "向成长支援教官报告"),
    (19684, 2, 1, [0], 806698, "LC1_L_grow_npc_Rena_01", "向成长支援教官报告"),
    (19685, 2, 1, [0], 806698, "LC1_L_grow_npc_Rena_01", "向成长支援教官报告"),
    (19686, 2, 1, [0], 806698, "LC1_L_grow_npc_Rena_01", "向成长支援教官报告"),
    (19687, 2, 1, [0], 806698, "LC1_L_grow_npc_Rena_01", "向成长支援教官报告"),
    (19688, 2, 1, [0], 806698, "LC1_L_grow_npc_Rena_01", "向成长支援教官报告"),
    (19689, 2, 1, [0], 806698, "LC1_L_grow_npc_Rena_01", "向成长支援教官报告"),
    (19694, 2, 1, [0], 806698, "LC1_L_grow_npc_Rena_01", "向成长支援教官报告"),
    (29672, 2, 1, [0], 806700, "DC1_D_grow_npc_Melrania_01", "向成长支援教官报告"),
    (29677, 2, 1, [0], 806700, "DC1_D_grow_npc_Melrania_01", "向成长支援教官报告"),
    (29684, 2, 1, [0], 806700, "DC1_D_grow_npc_Melrania_01", "向成长支援教官报告"),
    (29685, 2, 1, [0], 806700, "DC1_D_grow_npc_Melrania_01", "向成长支援教官报告"),
    (29686, 2, 1, [0], 806700, "DC1_D_grow_npc_Melrania_01", "向成长支援教官报告"),
    (29687, 2, 1, [0], 806700, "DC1_D_grow_npc_Melrania_01", "向成长支援教官报告"),
    (29688, 2, 1, [0], 806700, "DC1_D_grow_npc_Melrania_01", "向成长支援教官报告"),
    (29689, 2, 1, [0], 806700, "DC1_D_grow_npc_Melrania_01", "向成长支援教官报告"),
    (29694, 2, 1, [0], 806700, "DC1_D_grow_npc_Melrania_01", "向成长支援教官报告"),
]

# (quest_id, client_rows, last_row, stale_rows, reward_npc_id, reward_npc_name, last_row_text)
GROUP_B = [
    (3210, 3, 2, [0, 1], 798331, "Shugo_Shulack_02", "和 Shugo_Shulack_02 对话"),
]

# (quest_id, client_rows, last_row, stale_row, reward_npc_id, reward_npc_name, last_row_text, accept_npc)
GROUP_C = [
    (18036, 2, 1, 0, 801281, "LDF5b_Demades_E", "向 LDF5b_Demades_E 报告", 802008),
    (28036, 2, 1, 0, 801280, "LDF5b_Latkel_E", "向 LDF5b_Latkel_E 报告", 802015),
]


def quest_path(quest_id: int) -> Path:
    return QUESTS / f"{quest_id}.xml"


def client_summary_rows(quest_id: int) -> list[str]:
    """返回客户端 quest_summary 的行文本（大小写不敏感地定位 quest_q<id>.html / QUEST_Q<id>.html）。"""
    pattern = re.compile(rf"(?i)^quest_q{quest_id}\.html$")
    for candidate in UNPACK.rglob("*.html"):
        if not pattern.match(candidate.name):
            continue
        text = candidate.read_text(encoding="utf-8", errors="ignore")
        match = re.search(r'<HtmlPage name="quest_summary".*?</HtmlPage>', text, re.S | re.I)
        if not match:
            continue
        steps = re.findall(r"<step>(.*?)</step>", match.group(0), re.S | re.I)
        return [re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", step)).strip() for step in steps]
    return []


def recovery_block(quest_id: int, client_rows: int, stale_row: int, last_row: int,
                   npc_id: int, npc_name: str) -> str:
    return (
        f"    <!-- QE-051 领奖行合同：客户端 quest_q{quest_id}.html 的 quest_summary 共 {client_rows} 行，"
        f"末行是领奖行\n"
        f"         （{npc_name}，NPC id {npc_id}，对应该任务的领奖 NPC）；旧投影停在 {stale_row}，"
        f"进入世界时纠正为 {last_row} 并下发状态包。\n"
        f"         Reward row contract (QE-051): the last of the {client_rows} quest_q{quest_id} journal rows is\n"
        f"         the reward row ({npc_name}, npc id {npc_id}, the reward NPC of this quest); saves persisted\n"
        f"         at {stale_row} are repaired to {last_row} on enter-world. -->\n"
        f'    <transition target="reward">\n'
        f"      <event>\n"
        f"        <enter-world/>\n"
        f"      </event>\n"
        f"      <conditions>\n"
        f'        <status-is status="REWARD"/>\n'
        f'        <variable-is field="var0" value="{stale_row}"/>\n'
        f"      </conditions>\n"
        f"      <actions>\n"
        f'        <set-variable field="var0" value="{last_row}"/>\n'
        f"      </actions>\n"
        f"      <after-commit>\n"
        f'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        f"      </after-commit>\n"
        f"    </transition>\n"
    )


def set_reward_row(text: str, quest_id: int, last_row: int) -> str:
    """把 reward 节点里的 var0 投影改成末行号（只动 reward 节点那一行）。"""
    block = re.search(r'    <node label="reward" status="REWARD">.*?    </node>\n', text, re.S)
    if not block:
        raise SystemExit(f"quest {quest_id}: reward node block not found")
    current = re.search(r'<var name="var0" value="(\d+)"/>', block.group(0))
    if current and current.group(1) == str(last_row):
        return text
    updated = re.sub(r'(<var name="var0" value=")(\d+)("/>)', rf"\g<1>{last_row}\g<3>", block.group(0))
    if updated == block.group(0):
        raise SystemExit(f"quest {quest_id}: reward var0 projection already changed or missing")
    return text[:block.start()] + updated + text[block.end():]


def insert_recovery(text: str, quest_id: int, block: str) -> str:
    marker = f"    <!-- QE-051 领奖行合同：客户端 quest_q{quest_id}.html"
    if marker in text:
        return text
    anchor = "  <transitions>\n"
    if text.count(anchor) != 1:
        raise SystemExit(f"quest {quest_id}: <transitions> anchor not unique")
    return text.replace(anchor, anchor + block, 1)


def apply_group_a_or_b(entry) -> str:
    quest_id = entry[0]
    client_rows, last_row = entry[1], entry[2]
    stale_rows = entry[3] if isinstance(entry[3], list) else [entry[3]]
    npc_id, npc_name = entry[4], entry[5]
    path = quest_path(quest_id)
    text = path.read_text(encoding="utf-8")
    if f"    <!-- QE-051 领奖行合同：客户端 quest_q{quest_id}.html" in text:
        return f"{quest_id}: already applied"
    text = set_reward_row(text, quest_id, last_row)
    block = "".join(
        recovery_block(quest_id, client_rows, stale, last_row, npc_id, npc_name)
        for stale in stale_rows)
    text = insert_recovery(text, quest_id, block)
    path.write_text(text, encoding="utf-8")
    return f"{quest_id}: reward var0 -> {last_row}, recovery from {stale_rows}"


def apply_group_c(entry) -> str:
    quest_id, client_rows, last_row, stale_row, npc_id, npc_name, _text, accept_npc = entry
    path = quest_path(quest_id)
    text = path.read_text(encoding="utf-8")
    if f"    <!-- QE-051 领奖行合同：客户端 quest_q{quest_id}.html" in text:
        return f"{quest_id}: already applied"
    if '    <node label="s1" status="START">' not in text:
        started = re.search(r'    <node label="started" status="START">.*?    </node>\n', text, re.S)
        if not started:
            raise SystemExit(f"quest {quest_id}: started node block not found")
        node = ('    <node label="s1" status="START">\n'
                '      <var name="var0" value="1"/>\n'
                '    </node>\n')
        text = text[:started.end()] + node + text[started.end():]
    text = set_reward_row(text, quest_id, last_row)

    # 交出物品的事务从 started 改指新的 s1 节点（目标投影 var0=1 与动作一致）。
    hand_over = re.search(
        rf'    <transition source="started" target="started">\n'
        rf'      <event>\n'
        rf'        <dialog type="TALK_TO_NPC" npc-id="{accept_npc}" action="SETPRO1"/>\n'
        rf'      </event>', text)
    if not hand_over:
        raise SystemExit(f"quest {quest_id}: SETPRO1 transition not found")
    text = (text[:hand_over.start()]
            + hand_over.group(0).replace('source="started" target="started"',
                                         'source="started" target="s1"', 1)
            + text[hand_over.end():])

    # var0>=1 的两条事务（报告 NPC 的 QUEST_SELECT 与 SELECT_QUEST_REWARD）改以 s1 为 source；
    # QUEST_SELECT 的目标节点也一起从 started 改为 s1（目标投影 var0=1）。
    for action, old_target, new_target in (("QUEST_SELECT", "started", "s1"),
                                           ("SELECT_QUEST_REWARD", "reward", "reward")):
        match = re.search(
            rf'    <transition source="started" target="{old_target}">\n'
            rf'      <event>\n'
            rf'        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="{action}"/>\n'
            rf'      </event>\n'
            rf'      <conditions>\n'
            rf'        <variable-at-least field="var0" value="1"/>\n'
            rf'      </conditions>', text)
        if not match:
            raise SystemExit(f"quest {quest_id}: var0>=1 transition for {action} not found")
        replaced = match.group(0).replace(
            f'target="{old_target}"', f'target="{new_target}"', 1).replace(
            'source="started"', 'source="s1"', 1)
        text = text[:match.start()] + replaced + text[match.end():]

    text = insert_recovery(text, quest_id,
                           recovery_block(quest_id, client_rows, stale_row, last_row, npc_id, npc_name))
    path.write_text(text, encoding="utf-8")
    return f"{quest_id}: s1 node + retargeted hand-over/report routes, reward var0 -> {last_row}"


def verify() -> list[str]:
    problems: list[str] = []
    for entry in GROUP_A:
        quest_id, last_row = entry[0], entry[2]
        roots = ET.parse(quest_path(quest_id)).getroot()
        reward = [n for n in roots.findall("./nodes/node") if n.get("status") == "REWARD"][0]
        actual = [v.get("value") for v in reward.findall("var")]
        if actual[0] != str(last_row):
            problems.append(f"{quest_id}: reward var0 {actual[0]} != {last_row}")
        if not roots.findall('./transitions/transition[@target="reward"]/event/enter-world'):
            problems.append(f"{quest_id}: missing enter-world recovery edge")
        rows = client_summary_rows(quest_id)
        if len(rows) != entry[1]:
            problems.append(f"{quest_id}: client rows {len(rows)} != {entry[1]}")
    for entry in GROUP_B:
        quest_id, last_row, stale_rows = entry[0], entry[2], entry[3]
        roots = ET.parse(quest_path(quest_id)).getroot()
        reward = [n for n in roots.findall("./nodes/node") if n.get("status") == "REWARD"][0]
        if [v.get("value") for v in reward.findall("var")][0] != str(last_row):
            problems.append(f"{quest_id}: reward var0 != {last_row}")
        recoveries = [t for t in roots.findall("./transitions/transition")
                      if t.get("target") == "reward" and t.get("source") is None]
        got = sorted(t.find("./conditions/variable-is").get("value") for t in recoveries)
        if got != sorted(str(value) for value in stale_rows):
            problems.append(f"{quest_id}: recovery edges {got} != {stale_rows}")
    for entry in GROUP_C:
        quest_id, last_row = entry[0], entry[2]
        roots = ET.parse(quest_path(quest_id)).getroot()
        labels = [n.get("label") for n in roots.findall("./nodes/node")]
        if "s1" not in labels:
            problems.append(f"{quest_id}: missing s1 node")
        reward = [n for n in roots.findall("./nodes/node") if n.get("status") == "REWARD"][0]
        if [v.get("value") for v in reward.findall("var")][0] != str(last_row):
            problems.append(f"{quest_id}: reward var0 != {last_row}")
        sources = {(t.get("source"), t.get("target")) for t in roots.findall("./transitions/transition")}
        for shape in (("started", "s1"), ("s1", "s1"), ("s1", "reward")):
            if shape not in sources:
                problems.append(f"{quest_id}: missing transition {shape[0]} -> {shape[1]}")
    return problems


def write_evidence() -> None:
    rows = []
    for entry in GROUP_A:
        quest_id, client_rows, last_row, stale_rows, npc_id, npc_name, last_text = entry
        rows.append((quest_id, "A", client_rows, last_row, ",".join(map(str, stale_rows)),
                     npc_id, npc_name, last_text))
    for entry in GROUP_B:
        quest_id, client_rows, last_row, stale_rows, npc_id, npc_name, last_text = entry
        rows.append((quest_id, "B", client_rows, last_row, ",".join(map(str, stale_rows)),
                     npc_id, npc_name, last_text))
    for entry in GROUP_C:
        quest_id, client_rows, last_row, stale_row, npc_id, npc_name, last_text, _accept = entry
        rows.append((quest_id, "C", client_rows, last_row, stale_row, npc_id, npc_name, last_text))
    with EVIDENCE.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t")
        writer.writerow(["quest_id", "group", "client_rows", "reward_row", "stale_rows",
                         "reward_npc_id", "reward_npc_name", "last_row_text", "client_rows_observed",
                         "client_last_row_text"])
        for row in rows:
            observed = client_summary_rows(row[0])
            writer.writerow([*row, len(observed), observed[-1] if observed else ""])


def main() -> int:
    check_only = "--check" in sys.argv
    write_evidence()
    if not check_only:
        for entry in GROUP_A:
            print(apply_group_a_or_b(entry))
        for entry in GROUP_B:
            print(apply_group_a_or_b(entry))
        for entry in GROUP_C:
            print(apply_group_c(entry))
    problems = verify()
    if problems:
        print("BATCH7_VERIFY_FAILED")
        for problem in problems:
            print("  -", problem)
        return 1
    print(f"BATCH7_VERIFY_OK quests={len(GROUP_A) + len(GROUP_B) + len(GROUP_C)}"
          f" (A={len(GROUP_A)} B={len(GROUP_B)} C={len(GROUP_C)})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
