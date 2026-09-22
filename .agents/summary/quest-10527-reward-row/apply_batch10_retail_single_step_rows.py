#!/usr/bin/env python3
"""批次 10：QE-051 领奖行合同——retail“单步任务”族的 reward 投影收口（17 个任务）。

证据（逐条写入 batch10-evidence.tsv）：
1. retail 源定义 `origin/history:src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml`
   里这 17 个任务都是 `data_driven_quest` 的**单 step** 结构（start + 1 个 step），
   即“拿任务 → 完成一个交付/收集动作 → 领奖”的两段式；客户端 `quest_summary` 正好 2 行。
2. 同族 249 个 retail 单步任务里 184 个已经是 `reward var0=1`（末行）且形状统一
   （`unaccepted/started(0) + reward(1) + complete(0)` 加一条无 source 的 `enter-world` 自愈边），
   本批 17 个的 `reward var0=0` 属族内落后项（审计 verdict：MISSING_LAST_ROW / ROW_BEHIND）。
3. 客户端解包 `quest_q<id>.html` 的 `quest_summary` 末行就是领奖/交付行
   （交给/送给/报告/再次对话），与本任务的领奖 NPC 一致。

两组：
- A 组（12 个，末行**完全没有** START/REWARD 状态）：1527、1528、1725、2135、2247、2266、3087、4020、
  21455、26838、80735、80736。reward 投影 0 → 1 后两行都有状态。
- B 组（5 个，末行已有中间行状态（var0=1），但 reward 仍停在行 0，领奖态在任务书里停在上一行）：
  1963、1964、16838、16977、18035。同样把 reward 投影改成 1。
- 两组都补一条无 source 的 `enter-world` 自愈边：`status=REWARD && var0==0 → set var0=1`，
  否则旧存档（REWARD/var0=0）会因 `QuestMutationPlanner#matchesSourceNode` 的“投影变量必须全等”
  语义匹配不到任何领奖路由。

用法：
    python3 .agents/summary/quest-10527-reward-row/apply_batch10_retail_single_step_rows.py            # 应用
    python3 .agents/summary/quest-10527-reward-row/apply_batch10_retail_single_step_rows.py --check    # 只校验
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
EVIDENCE = Path(__file__).resolve().parent / "batch10-evidence.tsv"

CLIENT_ROWS = 2
REWARD_ROW = 1
STALE_ROW = 0

# (quest_id, group, 领奖/交付 NPC id, 客户端末行 NPC 名或文案[, 备注])
# npc_id 为 None 表示“末行 NPC 尚未在 typed 定义里承担领奖路线”，仅在注释里记录客户端名与待核实项。
REPAIRS = [
    (1527, "A", 205229, "Raninia"),
    (1528, "A", 204583, "Senemonea"),
    (1725, "A", 278590, "Dactyl"),
    (2135, "A", 203532, "DF1_Fisher"),
    (2247, "A", 203645, "NPC_Gerger_3"),
    (2266, "A", 203654, "Aurtri"),
    (3087, "A", 798144, "Talos"),
    (4020, "A", 205120, "Blume"),
    # retail end_npc_ids=799244（server name_desc=Unset，与客户端 STR_DIC_N_Unset 一致），
    # typed 定义当前把领奖/completion 放在起始 NPC 799404 上 —— NPC 归属待单独核实（本批只收口行投影）。
    (21455, "A", None, "Unset", "retail end_npc_ids=799244，typed 定义当前用 799404，NPC 归属待单独核实"),
    (26838, "A", 806575, "IDEternity_03_Jarik01_E"),
    (80735, "A", 833544, "未完成的术古罗伯"),
    (80736, "A", 833546, "未完成的术古罗伯"),
    (1963, "B", 203726, "Polyidus"),
    (1964, "B", 203726, "Polyidus"),
    (16838, "B", 806566, "IDEternity_03_Ostia01_E"),
    (16977, "B", 801762, "LDF5_Under_Timarchus_E"),
    (18035, "B", 800527, "LF5_Brontte_E"),
    # C 组：镜像（19002）已对齐的单侧缺陷，旧 handler 直接给出“领奖时 var0=1”的语义证据。
    # Group C: single-sided defect whose Elyos mirror 19002 is already aligned, with the legacy handler
    # spelling out the "var0=1 at reward" semantics.
    (29002, "C", 204099, "Resberg"),
]


def quest_path(quest_id: int) -> Path:
    return QUESTS / f"{quest_id}.xml"


def client_summary_rows(quest_id: int) -> list[str]:
    """客户端 quest_summary 行清单（大小写不敏感定位 quest_q<id>.html / QUEST_Q<id>.html）。"""
    pattern = re.compile(rf"(?i)^quest_q{quest_id}\.html$")
    for candidate in sorted(UNPACK.rglob("*.html")):
        if not pattern.match(candidate.name):
            continue
        text = candidate.read_text(encoding="utf-8", errors="ignore")
        match = re.search(r'<HtmlPage name="quest_summary".*?</HtmlPage>', text, re.S | re.I)
        if not match:
            continue
        steps = re.findall(r"<step>(.*?)</step>", match.group(0), re.S | re.I)
        return [re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", step)).strip() for step in steps]
    return []


def recovery_block(quest_id: int, npc_id: int | None, npc_name: str, group: str,
                   note: str | None = None) -> str:
    tail = ("末行当前没有任何 START/REWARD 状态" if group == "A"
            else ("末行已有中间行状态但 reward 投影仍停在行 0，领奖态在任务书里停在上一行"
                  if group == "B" else "对侧镜像已对齐，本侧投影停在行 0"))
    family_note = ("同族 249 个 retail 单步任务中 184 个已是 reward var0=1，旧投影停在 "
                   f"{STALE_ROW} 属族内落后项" if group in ("A", "B")
                   else "旧 handler _29002ExpertAethertappersTest 在 204099 的 STEP_TO_1 写 "
                        f"setQuestVarById(0, {REWARD_ROW})、在 204257 领奖时只 setStatus(REWARD)"
                        f"（var0 保持 {REWARD_ROW}），镜像 19002 的投影也已是 {REWARD_ROW}，"
                        f"本侧停在 {STALE_ROW} 属单侧缺陷")
    npc_part = npc_name + (f"，NPC id {npc_id}" if npc_id is not None
                           else "（typed 定义的领奖 NPC 归属待单独核实）")
    note_part = f"\n         {note}。" if note else ""
    return (
        f"    <!-- QE-051 领奖行合同：客户端 quest_q{quest_id}.html\n"
        f"         quest_summary 共 {CLIENT_ROWS} 行，末行是领奖/交付行（{npc_part}）；{tail}。{note_part}\n"
        f"         {family_note}；\n"
        f"         进入世界时把旧存档纠正为 {REWARD_ROW} 并下发状态包。\n"
        f"         Reward row contract (QE-051): the last of the {CLIENT_ROWS}\n"
        f"         quest_q{quest_id} journal rows is the reward row ({npc_name}"
        f"{', npc id %d' % npc_id if npc_id is not None else ''}); the old\n"
        f"         projection stopped at {STALE_ROW}."
        + (" 184 of 249 sibling retail single-step quests already project"
           f" {REWARD_ROW}." if group in ("A", "B") else
           f" The mirror 19002 already projects {REWARD_ROW}.")
        + f" Saves persisted at {STALE_ROW} are repaired on enter-world. -->\n"
        f'    <transition target="reward">\n'
        f"      <event>\n"
        f"        <enter-world/>\n"
        f"      </event>\n"
        f"      <conditions>\n"
        f'        <status-is status="REWARD"/>\n'
        f'        <variable-is field="var0" value="{STALE_ROW}"/>\n'
        f"      </conditions>\n"
        f"      <actions>\n"
        f'        <set-variable field="var0" value="{REWARD_ROW}"/>\n'
        f"      </actions>\n"
        f"      <after-commit>\n"
        f'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        f"      </after-commit>\n"
        f"    </transition>\n"
    )


def set_reward_row(text: str, quest_id: int, last_row: int) -> str:
    """把 reward 节点的 var0 投影改成领奖行号（只动 reward 节点块）。"""
    block = re.search(r'    <node label="reward" status="REWARD">.*?    </node>\n', text, re.S)
    if not block:
        raise SystemExit(f"quest {quest_id}: reward node block not found")
    current = re.search(r'<var name="var0" value="(\d+)"\s*/>', block.group(0))
    if current is None:
        raise SystemExit(f"quest {quest_id}: reward var0 projection missing")
    if current.group(1) == str(last_row):
        return text
    updated = re.sub(r'(<var name="var0" value=")(\d+)("\s*/>)', rf"\g<1>{last_row}\g<3>", block.group(0))
    return text[:block.start()] + updated + text[block.end():]


def insert_recovery(text: str, quest_id: int, block: str) -> str:
    marker = f"    <!-- QE-051 领奖行合同：retail 单步任务"
    if marker in text:
        return text
    anchor = "  <transitions>\n"
    if text.count(anchor) != 1:
        raise SystemExit(f"quest {quest_id}: <transitions> anchor not unique")
    return text.replace(anchor, anchor + block, 1)


def apply_repair(entry) -> str:
    quest_id, group, npc_id, npc_name, *rest = entry
    note = rest[0] if rest else None
    path = quest_path(quest_id)
    text = path.read_text(encoding="utf-8")
    if "    <!-- QE-051 领奖行合同：retail 单步任务" in text:
        return f"{quest_id}: already applied"
    if 'set-variable field="var0"' in text:
        raise SystemExit(f"quest {quest_id}: unexpected var0 write, review before applying")
    text = set_reward_row(text, quest_id, REWARD_ROW)
    text = insert_recovery(text, quest_id, recovery_block(quest_id, npc_id, npc_name, group, note))
    path.write_text(text, encoding="utf-8")
    return f"{quest_id} (group {group}): reward var0 {STALE_ROW} -> {REWARD_ROW}, recovery edge added"


def verify() -> list[str]:
    problems: list[str] = []
    for quest_id, group, npc_id, npc_name, *rest in REPAIRS:
        root = ET.parse(quest_path(quest_id)).getroot()
        reward = [n for n in root.findall("./nodes/node") if n.get("status") == "REWARD"]
        if len(reward) != 1:
            problems.append(f"{quest_id}: reward node count {len(reward)} != 1")
            continue
        var0 = reward[0].find("./var[@name='var0']")
        if var0 is None or var0.get("value") != str(REWARD_ROW):
            problems.append(f"{quest_id}: reward var0 {None if var0 is None else var0.get('value')} != {REWARD_ROW}")
        recoveries = [t for t in root.findall("./transitions/transition")
                      if t.get("target") == "reward" and t.get("source") is None]
        if len(recoveries) != 1:
            problems.append(f"{quest_id}: recovery edge count {len(recoveries)} != 1")
        else:
            edge = recoveries[0]
            if edge.find("./event/enter-world") is None:
                problems.append(f"{quest_id}: recovery edge is not enter-world")
            status = edge.find("./conditions/status-is")
            stale = edge.find("./conditions/variable-is")
            action = edge.find("./actions/set-variable")
            if status is None or status.get("status") != "REWARD":
                problems.append(f"{quest_id}: recovery status-is != REWARD")
            if stale is None or stale.get("field") != "var0" or stale.get("value") != str(STALE_ROW):
                problems.append(f"{quest_id}: recovery stale row != {STALE_ROW}")
            if action is None or action.get("field") != "var0" or action.get("value") != str(REWARD_ROW):
                problems.append(f"{quest_id}: recovery action != var0={REWARD_ROW}")
            sync = edge.find("./after-commit/sync-quest-state")
            if sync is None or sync.get("mode") != "LEVEL_AND_VISIBILITY_REFRESH":
                problems.append(f"{quest_id}: recovery after-commit != LEVEL_AND_VISIBILITY_REFRESH")
        npcs = {int(value) for value in re.findall(r'npc-id="(\d+)"', quest_path(quest_id).read_text(encoding="utf-8"))}
        if npc_id is not None and npc_id not in npcs:
            problems.append(f"{quest_id}: reported reward npc {npc_id} not present in definition")
        rows = client_summary_rows(quest_id)
        if len(rows) != CLIENT_ROWS:
            problems.append(f"{quest_id}: client rows {len(rows)} != {CLIENT_ROWS}")
    return problems


def write_evidence() -> None:
    with EVIDENCE.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t")
        writer.writerow(["quest_id", "group", "client_rows", "reward_row", "stale_row",
                         "reward_npc_id", "reward_npc_name", "client_rows_observed",
                         "client_last_row_text", "fixed_shape_before"])
        for quest_id, group, npc_id, npc_name, *rest in REPAIRS:
            observed = client_summary_rows(quest_id)
            writer.writerow([quest_id, group, CLIENT_ROWS, REWARD_ROW, STALE_ROW,
                             "" if npc_id is None else npc_id, npc_name,
                             len(observed), observed[-1] if observed else "",
                             {"A": "末行无状态", "B": "末行有状态/reward停行0"}.get(
                                 group, "镜像已对齐的单侧缺陷")])


def main() -> int:
    check_only = "--check" in sys.argv
    write_evidence()
    if not check_only:
        for entry in REPAIRS:
            print(apply_repair(entry))
    problems = verify()
    if problems:
        print("BATCH10_VERIFY_FAILED")
        for problem in problems:
            print("  -", problem)
        return 1
    groups = {group: sum(1 for row in REPAIRS if row[1] == group) for group in ("A", "B", "C")}
    print(f"BATCH10_VERIFY_OK quests={len(REPAIRS)} "
          f"(A={groups['A']} B={groups['B']} C={groups['C']})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
