#!/usr/bin/env python3
"""全库审计：typed 任务定义的状态行（START/REWARD var0）与 Aion 5.8 客户端任务书行索引（quest_summary）是否错位。

背景：
- 客户端任务书行索引读 SECTION_0（= var0，见 memory-bank QE-012）；10527 使用 182216075 后
  被错误投影成 REWARD/var0=14（应为 15，与魔族镜像 20527 一致），任务书于是停在上一行。
- 用户判据（QE-051）：“客户端 quest_summary 的每一行都必须有一个 START/REWARD 状态”，
  缺口行在游戏里永远不会被高亮；`var0 >= 行数` 的状态落在客户端不存在的行号上。
- QE-045 例外：15300/25300 等任务的客户端验收值是“进入 REWARD 前的 packed step”，
  reward 投影固定为 0/旧 step，不能用“最后一行”机械套用（见 QE045_LOCKED）。

客户端解包目录默认 /Users/mc/PycharmProjects/unpak（可用 AION_UNPACK_ROOT 覆盖）：
- data_unpacked/Dialogs/quest_q<id>.html 的 quest_summary 行清单（行索引权威）
- Quest_unpacked/quest_script_monster.csv 的 Progress(SECTION_0==N ...) 客户端脚本声明（var0 是否行索引的旁证）

用法：
    python3 .agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py [quest_id ...]
输出：
    audit-output.tsv（全库逐任务判定）
    audit-missing-last-row.tsv（“只缺最后一行”的候选清单，供分批核对）
"""

from __future__ import annotations

import csv
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS_DIR = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
UNPACK_ROOT = Path(os.environ.get("AION_UNPACK_ROOT", "/Users/mc/PycharmProjects/unpak"))
DIALOG_DIR = UNPACK_ROOT / "data_unpacked/Dialogs"
SCRIPT_CSV = UNPACK_ROOT / "Quest_unpacked/quest_script_monster.csv"
NPC_NAME_FILE = UNPACK_ROOT / "npcs_unpacked/client_npcs_npc.xml"
OUTPUT = Path(__file__).resolve().parent / "audit-output.tsv"
MISSING_LAST_OUTPUT = Path(__file__).resolve().parent / "audit-missing-last-row.tsv"
CANDIDATE_OUTPUT = Path(__file__).resolve().parent / "audit-qe051-candidates.tsv"

# 已被 QE-045 回归锁按“进入 REWARD 前的 packed step”固定的任务；本审计与它们结论冲突时需客户端复测。
# 已核实的“var0 不是任务书行号”例外（审计会判 ROW_WITHOUT_STATE，但不是缺陷）：
# - 30203 / 30303（贝希蒙迪尔寺院组队任务）：var0..var3 是 4 只守护者的击杀标志位，
#   旧 handler `_30203GroupHalttheCeremony` 用 setQuestVarById(0..3, 1) 逐个置位、集齐后
#   setStatus(REWARD)（领奖态就是 var0..3=1，与当前 reward 投影一致）；客户端 quest_script 也是
#   Progress(SECTION_0<1)…Progress(SECTION_3<1) 的四个标志位声明，任务书 3 行由 SECTION 标志位
#   与 visible 槽位驱动，不能按“行号 = var0”改投影（改了会打断其余三只的击杀路线）。
# Verified "var0 is not the journal row index" exceptions (audited as ROW_WITHOUT_STATE, not defects):
# - 30203 / 30303: var0..var3 are four guardian kill flags (legacy _30203GroupHalttheCeremony sets each
#   via setQuestVarById, then setStatus(REWARD) with var0..3=1 matching the current reward projection);
#   the client script declares Progress(SECTION_0<1)..Progress(SECTION_3<1), so the three journal rows are
#   driven by those flags/visible slots rather than by var0.
VAR0_FLAG_EXCEPTIONS = {30203, 30303, 13918, 23918, 24112, 30600, 30610}
# 追加登记（批次 26，2026-09-22）：24112（单槽 Named：SECTION_0<1 且 SECTION_5==0）与
# 30600/30610（双层 Named/Boss：SECTION_0<1;SECTION_5==0 + SECTION_1<1;SECTION_0==1）的 var0/var1
# 都是**逐行计数器**而不是行号：客户端任务书第二/第三行由 `SECTION_n<1` 饱和驱动（行 2/行 3 在计数满格后
# 自动亮起），因此本审计的行号口径会把 24112 判成 MISSING_LAST_ROW、把 30600/30610 判成 MISSING_TAIL_ROWS；
# 权威口径是 .agents/summary/quest-15001-multicounter-step/audit_section0_report_row_closure.py 的
# COUNTER_CHAIN_OK（reward 投影 = 计数饱和值 + 存在 report 路由），门禁是 CounterChainBriefingStageContractTest。
# 若把 reward 投影机械改成“末行行号”（24112=2、30600/30610=3），客户端行 2 的门控 SECTION_0==1 会失效、
# 任务书反而退回上一行，属于 QE-053 明确禁止的反向修正。
# Batch 26 registration: 24112's var0 and 30600/30610's var0/var1 are per-row counters, not the journal row
# index, so this row-index lens reports MISSING_LAST_ROW / MISSING_TAIL_ROWS; the authoritative lens is the
# section0 audit's COUNTER_CHAIN_OK verdict.
# 追加登记（批次 21，2026-09-22）：13918 / 23918 与 30203/30303 同族 —— 客户端 quest_monster.csv 的
# SECTION_0..4 是五个链式 0/1 计数槽（行 n 只在 SECTION_n<1 且 SECTION_{n-1}==1 时可见），
# var0 不是行号，所以本审计的行号口径会判 ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE；
# 该族的权威口径是 .agents/summary/quest-15001-multicounter-step/audit_section0_report_row_closure.py
# 的 COUNTER_CHAIN 判定（两侧 COUNTER_CHAIN_OK：字段落在 6n、每只精锐兵只推自己那一槽、领奖投影全 1）。
# Batch 21 registration: 13918/23918 are the same shape as 30203/30303 -- the client chains five 0/1
# SECTION slots, so var0 is a counter rather than the journal row index and this row-index lens reports
# ROW_BEHIND/ROW_WITHOUT_STATE; the authoritative lens is the COUNTER_CHAIN verdict of the section0 audit.

# 追加登记（批次 25，2026-09-22）：2842（魔族）/ 1841（天族）这一对镜像的客户端门控是
# Progress(SECTION_0<39; SECTION_5==0) —— 单行狩猎的击杀计数，不是行阶梯：var0 是 0..39 的击杀数，
# 任务书的第二个可见行（“和 Herz / Sakmis 对话”）由计数满格与领奖态驱动，不承载行号状态。
# 领奖投影必须是饱和值 39（1841 早已是 39，批次 25 把 2842 从 0 对齐到 39 —— 旧值 0 会让
# QuestMutationPlanner#matchesSourceNode 的逐字段全等把领奖态存档挡在所有 reward 路由之外）。
# 因此本审计的行号口径会判 ROW_AHEAD / STATES_BEYOND_ROWS（2842 = BOTH_MISALIGNED、1841 = STATE_OUT_OF_RANGE），
# 权威口径是 .agents/summary/quest-15001-multicounter-step/audit_section0_report_row_closure.py 的
# COUNTER_CHAIN_OK（reward 投影计数饱和 + 存在 report 路由），门禁是 CounterChainTripletContractTest。
# Batch 25 registration: the mirror pair 2842/1841 gates on Progress(SECTION_0<39; SECTION_5==0), a
# single-row hunt counter, so var0 is a 0..39 kill count rather than the journal row index; the reward
# projection must carry the saturated 39 (1841 already did; batch 25 aligned 2842 from 0 to 39, because 0
# blocked every reward route through the all-fields-equal source-node match). The row-index lens therefore
# reports ROW_AHEAD/STATES_BEYOND_ROWS for both sides; the authoritative lens is the section0 audit's
# COUNTER_CHAIN_OK verdict and CounterChainTripletContractTest.
COUNTER_SATURATED_REWARD_ROWS = {1841, 2842}

# 已核实的“客户端空行”例外：客户端 HTML 里存在与相邻行共用 visible 槽位的空 <p>，审计会把它当成一行
# （client_rows 比真实状态数多 1）。10530（天族）第 8 行是空的 `<p visible="[%24]"></font></p>`，
# 第 9 行“倾听 Jucleas 的故事”同样是 `[%24]`，第 10 行才是 `[%27]`；魔族镜像 20530 没有这个空行，
# 两阵营的“槽位 = 3 × 逻辑行号”完全一致，行数多的那一行只是客户端数据里的空段落。批次 4 已把
# 10530 的 reward 对齐到镜像的 9，这里只登记证据、不改判定（否则会按“末行行号 10”误修）。
# Verified "client blank row" exception: the client HTML contains an empty <p> that shares its visible
# slot with the following row, so the audit counts one row too many. Elyos 10530 row 8 is an empty
# `<p visible="[%24]"></font></p>` and row 9 ("listen to Jucleas") also uses [%24] while row 10 uses [%27];
# the Asmodian mirror 20530 has no such blank row and both sides agree on slot == 3 * logical row index.
# Batch 4 already aligned 10530's reward to the mirror's 9; this entry only records the evidence so the
# "last row index 10" reading is not mechanically repaired later. The verdict itself is unchanged.
DUPLICATE_VISIBLE_SLOT_BLANK_ROWS = {10530}

# 已核实的“整段空槽位”例外（批次 24，2026-09-22）：客户端 quest_summary 固定渲染若干 <step>，但每一步的
# 可见文本都是空白（序幕 1000/2000 的 4 个空槽只挂了 [%collectitem] 占位符，quest.xml 里既没有 collect_item
# 也没有任何 NPC），本审计会把空槽当成“没有状态的客户端行”，判成 NO_REWARD_ROW / MISSING_TAIL_ROWS /
# ROW_WITHOUT_STATE。这些任务没有可以点亮的任务书行，按行号补阶梯只会造出永远不显示的节点；判定保持不变，
# 这里只登记证据（扫描脚本 .agents/summary/quest-10527-reward-row/audit_blank_journal_slots.py，
# 明细 blank-journal-slots.tsv）：
#   ALL_BLANK（全部 step 为空，12 个）：1000/2000（序幕 enter-zone + movie -> complete，无 REWARD 节点，
#     由 BlankJournalSlotBoundaryContractTest 锁定）；16984/26984（服务端暂无 <nodes>，属“无状态”族）；
#     3959/4963/18706/18744/20015/28706/28744/29706（客户端有任务书但服务端没有定义文件，属“缺定义”族）。
#   TRAILING_BLANK（末尾 step 为空）：1400 —— 行 0 是“除掉作恶的特洛尔和托尔金 (/7)”，var0/var1 是 8x4 的
#     击杀计数组合（35 个节点），reward 投影 var0=7/var1=3 是计数饱和值，所以本审计的行号口径会判
#     ROW_AHEAD / STATES_BEYOND_ROWS / STATE_OUT_OF_RANGE，空槽不承载任何目标。
# Verified "blank journal slots" exceptions (batch 24): the client quest_summary renders fixed <step> slots whose
# visible text is entirely blank (the prologue 1000/2000 rows only carry a [%collectitem] placeholder while their
# quest.xml declares neither collect_item nor any NPC), so this row-index lens reports NO_REWARD_ROW /
# MISSING_TAIL_ROWS / ROW_WITHOUT_STATE. Those quests have no journal row to light up; the verdicts are unchanged
# and this entry only records the evidence. 1400 is the trailing-blank case: row 0 is the /7 kill counter and
# var0/var1 are its 8x4 combination, so its saturated reward projection is a counter value rather than a row index.
BLANK_JOURNAL_SLOT_EXCEPTIONS = {
    1000, 2000, 3959, 4963, 16984, 18706, 18744, 20015, 26984, 28706, 28744, 29706, 1400,
}

QE045_LOCKED = {2393, 3722, 4722, 11149, 13965, 14010, 14015, 14020, 14040, 14050,
                15674, 23965, 24010, 24020, 24040, 24050, 25674, 30057, 30158, 30208}

# 已核实的“reward 投影 = legacy 落盘 step，行号口径算出的末行索引不是权威值”例外（QE-054，批次 27 登记证据、
# 批次 28 补进本脚本；判定保持不变，只是登记，防止后续把这几行当成 MISSING_LAST_ROW 真缺陷批量改）：
# - 15300/25300（Taking Arms / A Bloody Battle with Beritra）：legacy `changeQuestStep(env, 13, 14, true)`
#   只置 REWARD、step 停在 13，已由 2026-09-19/20 用户真机全程验收（含领奖）并由
#   Quest15300And25300RewardProjectionTest 锁定；
# - 10100/20100（Kahrun Intrigue / Ghost Of A Bygone Age）：legacy `useQuestItem(env, item, 4, 4, true)`
#   落盘 step=4，由 Quest10100And20100ItemUseRemovalTest 锁定；同系列的 10101/10110 投影等于末行索引，
#   说明同一系列内两种口径并存，必须逐任务看 legacy，禁止按行号或任务号区间批量替换。
# Verified "reward projection == legacy persisted step, the last row index is not authoritative" exceptions
# (QE-054): registered evidence only, the verdicts stay unchanged. 15300/25300 stop at the pre-REWARD step 13
# (client-accepted 2026-09-19/20), 10100/20100 persist step 4 through useQuestItem(..., 4, 4, true).
LEGACY_STEP_EXCEPTION = {15300, 25300, 10100, 20100}

# “和 X 对话 / 向 X 报告 / 去 X 那里”这一类末行 = 客户端领奖行（中/韩双语关键词）。
DIALOG_ROW_RE = re.compile(r"对话|报告|见面|交谈|转达|传达|询问|汇报|告诉|通知|迎接|确认|拜访|交给|交付|递交|转交|归还|送达|대화|보고|만나")

CLIENT_INDEX: dict[int, Path] | None = None
CLIENT_SECTION0: dict[int, str] | None = None
CLIENT_NPC_NAMES: dict[int, str] | None = None


def client_npc_names() -> dict[int, str]:
    """客户端 npcs_unpacked/client_npcs_npc.xml 的 NPC id -> 客户端名（例：804698 -> LF5_Nubes_E）。

    用途：任务书末行“和[%dic:STR_DIC_N_XXX]对话”里的 XXX 必须能对上任务里真正出场的 NPC，
    这是“末行确实是领奖行”的独立证据（不依赖服务端投影）。
    """
    global CLIENT_NPC_NAMES
    if CLIENT_NPC_NAMES is None:
        names: dict[int, str] = {}
        if NPC_NAME_FILE.exists():
            text = NPC_NAME_FILE.read_text(encoding="utf-8", errors="ignore")
            for match in re.finditer(r"<npc_client>\s*<id>(\d+)</id>\s*<name>([^<]*)</name>", text):
                names[int(match.group(1))] = match.group(2)
        CLIENT_NPC_NAMES = names
    return CLIENT_NPC_NAMES


def client_index() -> dict[int, Path]:
    """单次遍历客户端 Dialogs 目录，建立任务号 -> quest_q<id>.html 索引。

    解包目录里同时存在 `quest_q<id>.html`（小写，6430 个）与 `QUEST_Q<id>.html`（大写，2695 个，
    多为仅大写存在的任务）；索引必须大小写不敏感，否则这些任务会被误判成“无客户端任务书”。
    同名大小写同时存在时优先选含 quest_summary 页面的一份。
    """
    global CLIENT_INDEX
    if CLIENT_INDEX is None:
        pattern = re.compile(r"(?i)^quest_q(\d+)\.html$")
        candidates: dict[int, list[Path]] = {}
        if DIALOG_DIR.exists():
            for dirpath, _dirnames, filenames in os.walk(DIALOG_DIR):
                for filename in filenames:
                    match = pattern.match(filename)
                    if match:
                        candidates.setdefault(int(match.group(1)), []).append(
                            Path(dirpath) / filename)
        index: dict[int, Path] = {}
        for quest_id, paths in candidates.items():
            # 优先非 unused/ 目录、其次小写名（range 子目录里的现行副本）
            paths.sort(key=lambda path: ("unused" in path.parts,
                                         path.name.startswith("QUEST")))
            with_page = [path for path in paths
                         if '<HtmlPage name="quest_summary">' in path.read_text(
                             encoding="utf-8", errors="ignore")]
            index[quest_id] = (with_page or paths)[0]
        CLIENT_INDEX = index
    return CLIENT_INDEX


def client_rows(quest_id: int) -> list[str] | None:
    """客户端 quest_summary 的行清单（去标签文本）；无页面返回 None。"""
    path = client_index().get(quest_id)
    if path is None:
        return None
    text = path.read_text(encoding="utf-8", errors="ignore")
    page = re.search(r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', text, re.S)
    if page is None:
        return None
    return [re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", step)).strip()
            for step in re.findall(r"<step>(.*?)</step>", page.group(1), re.S)]


def client_section0(quest_id: int) -> str:
    """客户端 quest_script_monster.csv 里该任务的 SECTION_0 声明（var0 是否行索引的旁证）。"""
    global CLIENT_SECTION0
    if CLIENT_SECTION0 is None:
        index: dict[int, set[str]] = {}
        if SCRIPT_CSV.exists():
            with SCRIPT_CSV.open(encoding="utf-8-sig", errors="replace") as handle:
                for record in csv.DictReader(handle):
                    raw = (record.get("questId") or "").strip()
                    progress = (record.get("progress") or "").strip()
                    if not raw.isdigit() or "SECTION_0" not in progress:
                        continue
                    index.setdefault(int(raw), set()).add(progress)
        CLIENT_SECTION0 = {quest_id: " | ".join(sorted(patterns))
                           for quest_id, patterns in index.items()}
    return CLIENT_SECTION0.get(quest_id, "")


def definition_summary(quest_id: int) -> dict[str, object] | None:
    path = QUESTS_DIR / f"{quest_id}.xml"
    if not path.exists():
        return None
    raw = path.read_text(encoding="utf-8")
    quest_npcs = {int(value) for value in re.findall(r'npc-id="(\d+)"', raw)}
    root = ET.fromstring(raw)
    nodes_element = root.find("nodes")
    nodes: dict[str, dict[str, int]] = {}
    statuses: dict[str, str] = {}
    for node in (nodes_element if nodes_element is not None else []):
        nodes[node.get("label")] = {var.get("name"): int(var.get("value"))
                                    for var in node.findall("var")}
        statuses[node.get("label")] = node.get("status")
    progress_element = root.find("progress")
    var0_max = None
    for field in (progress_element if progress_element is not None else []):
        if field.get("name") == "var0" and field.get("max") is not None:
            var0_max = int(field.get("max"))
    visible_rows = sorted({variables.get("var0") for label, variables in nodes.items()
                           if statuses[label] in ("START", "REWARD")
                           and variables.get("var0") is not None})
    starts = [(label, variables.get("var0")) for label, variables in nodes.items()
              if label.startswith("s")]
    last_start = starts[-1] if starts else (None, None)
    handovers: list[str] = []
    writes: list[str] = []
    recovery = False
    increments_var0 = False
    transitions = root.find("transitions")
    for transition in (transitions if transitions is not None else []):
        actions = transition.find("actions")
        for action in (actions if actions is not None else []):
            if action.tag == "increment-variable" and action.get("field") == "var0":
                increments_var0 = True
        if transition.get("target") != "reward":
            continue
        source = transition.get("source")
        event_element = transition.find("event")
        events = " ".join(element.tag for element in
                          (event_element if event_element is not None else []))
        writes_here = [f"{action.get('field')}={action.get('value')}"
                       for action in (actions if actions is not None else [])
                       if action.tag == "set-variable"]
        if source is None:
            conditions = transition.find("conditions")
            if any(condition.tag == "status-is" and condition.get("status") == "REWARD"
                   for condition in (conditions if conditions is not None else [])):
                recovery = True
            continue
        handovers.append(f"{source}->reward[{events}]")
        writes.extend(writes_here)
    return {
        "reward": nodes.get("reward", {}),
        "last_start": last_start,
        "handovers": handovers,
        "handover_writes": writes,
        "recovery": recovery,
        "visible_rows": visible_rows,
        "increments_var0": increments_var0,
        "var0_max": var0_max,
        "quest_npcs": quest_npcs,
    }


def verdict(reward_var0: int | None, last_row: int | None) -> str:
    if reward_var0 is None:
        return "NO_REWARD_ROW"
    if last_row is None:
        return "NO_CLIENT_HTML"
    if reward_var0 == last_row:
        return "ROW_ALIGNED"
    return "ROW_BEHIND" if reward_var0 < last_row else "ROW_AHEAD"


def row_state_verdict(row_count: int, last_row: int | None,
                      visible_rows: list[int]) -> tuple[str, list[int], list[int]]:
    """按“每个客户端任务书行都应有一个 START/REWARD 状态”校验行 ↔ var0 映射。"""
    if last_row is None:
        return "NO_CLIENT_HTML", [], []
    rows_without_state = [row for row in range(row_count) if row not in visible_rows]
    states_out_of_range = [value for value in visible_rows if value >= row_count]
    if rows_without_state and states_out_of_range:
        return "BOTH_MISALIGNED", rows_without_state, states_out_of_range
    if rows_without_state:
        return "ROW_WITHOUT_STATE", rows_without_state, states_out_of_range
    if states_out_of_range:
        return "STATE_OUT_OF_RANGE", rows_without_state, states_out_of_range
    return "ROW_STATE_ALIGNED", rows_without_state, states_out_of_range


def visible_shape(row_count: int, visible_rows: list[int]) -> str:
    """把可见状态集合归类成可批量处置的形状。"""
    if row_count == 0:
        return "NO_CLIENT_HTML"
    states = sorted(visible_rows)
    if not states:
        return "NO_STATE"
    if states == list(range(row_count)):
        return "ALIGNED"
    if states == list(range(row_count - 1)):
        return "MISSING_LAST_ROW"
    if states and states == list(range(len(states))) and states[-1] < row_count - 1:
        return "MISSING_TAIL_ROWS"
    if states and states[-1] >= row_count:
        return "STATES_BEYOND_ROWS"
    if states:
        return "INTERIOR_GAP"
    return "NO_STATE"


def mirror_of(quest_id: int) -> int | None:
    """天/魔镜像任务号（1xxxx <-> 2xxxx）；其余返回 None。"""
    if 10000 <= quest_id < 20000:
        return quest_id + 10000
    if 20000 <= quest_id < 30000:
        return quest_id - 10000
    return None


def main() -> int:
    requested = sorted(int(value) for value in sys.argv[1:] if value.isdigit())
    quest_ids = requested or sorted(int(path.stem) for path in QUESTS_DIR.glob("*.xml")
                                    if path.stem.isdigit())
    rows = []
    for quest_id in quest_ids:
        summary = definition_summary(quest_id)
        if summary is None:
            continue
        steps = client_rows(quest_id)
        reward = summary["reward"]
        last_start = summary["last_start"]
        reward_var0 = reward.get("var0")
        last_row = len(steps) - 1 if steps else None
        visible_rows = summary["visible_rows"]
        row_verdict, rows_without_state, states_out_of_range = row_state_verdict(
            len(steps) if steps else 0, last_row, visible_rows)
        shape = visible_shape(len(steps) if steps else 0, visible_rows)
        mirror = mirror_of(quest_id)
        rows.append({
            "quest_id": quest_id,
            "reward_var0": reward_var0,
            "last_start_node": last_start[0],
            "last_start_var0": last_start[1],
            "handovers": " ".join(summary["handovers"]),
            "handover_writes": ",".join(summary["handover_writes"]),
            "recovery": summary["recovery"],
            "var0_increment": summary["increments_var0"],
            "client_rows": len(steps) if steps else 0,
            "client_last_row": last_row,
            "last_row_text": (steps[-1] if steps else ""),
            "last_row_kind": ("DIALOG" if steps and DIALOG_ROW_RE.search(steps[-1])
                              else ("OTHER" if steps else "")),
            "verdict": verdict(reward_var0, last_row),
            "visible_state_var0": " ".join(str(value) for value in visible_rows),
            "shape": shape,
            "row_state_verdict": row_verdict,
            "rows_without_state": " ".join(str(row) for row in rows_without_state),
            "states_out_of_range": " ".join(str(value) for value in states_out_of_range),
            "qe045_locked": quest_id in QE045_LOCKED,
            "client_section0": client_section0(quest_id),
            "var0_max": summary["var0_max"],
            "last_row_npc_key": " ".join(re.findall(r"STR_DIC_N_([A-Za-z0-9_]+)", steps[-1])) if steps else "",
            "last_row_npc_matches_quest": bool(steps and any(
                name.lower() == key.lower()
                for key in re.findall(r"STR_DIC_N_([A-Za-z0-9_]+)", steps[-1])
                for npc_id, name in client_npc_names().items()
                if npc_id in summary["quest_npcs"])),
            "reward_row_within_field_max": (summary["var0_max"] is None or reward_var0 is None
                                            or reward_var0 <= summary["var0_max"]),
            "last_row_within_field_max": (summary["var0_max"] is None or last_row is None
                                          or last_row <= summary["var0_max"]),
            "client_file": (client_index().get(quest_id).name
                            if client_index().get(quest_id) else ""),
        })

    columns = ["quest_id", "reward_var0", "last_start_node", "last_start_var0", "handovers",
               "handover_writes", "recovery", "var0_increment", "client_rows", "client_last_row",
               "last_row_kind", "verdict", "shape", "row_state_verdict", "visible_state_var0",
               "rows_without_state", "states_out_of_range", "qe045_locked", "client_section0",
               "last_row_text", "client_file", "var0_max", "reward_row_within_field_max",
               "last_row_within_field_max", "last_row_npc_key", "last_row_npc_matches_quest"]
    with OUTPUT.open("w", encoding="utf-8") as out:
        out.write("\t".join(columns) + "\n")
        for row in rows:
            out.write("\t".join(str(row[column]) for column in columns) + "\n")

    index = {row["quest_id"]: row for row in rows}

    def count(key: str, values=None) -> dict[str, int]:
        """按 key 计数；values 可选，用于先按同一行的 key 值过滤。"""
        counted: dict[str, int] = {}
        for row in rows:
            if values is not None and row[key] not in values:
                continue
            counted[row[key]] = counted.get(row[key], 0) + 1
        return counted

    def shape_of_verdict(name: str) -> dict[str, int]:
        counted: dict[str, int] = {}
        for row in rows:
            if row["verdict"] != name:
                continue
            counted[row["shape"]] = counted.get(row["shape"], 0) + 1
        return counted

    print(f"quests={len(rows)} client_dir={'OK' if DIALOG_DIR.exists() else 'MISSING'} "
          f"script_csv={'OK' if SCRIPT_CSV.exists() else 'MISSING'}")

    print("\n[1] reward 投影 vs 客户端最后一行：")
    for name, value in sorted(count("verdict").items()):
        print(f"  {name}: {value}")

    print("\n[2] 行 ↔ 状态（“每一行都要有 START/REWARD 状态”）：")
    for name, value in sorted(count("row_state_verdict").items()):
        print(f"  {name}: {value}")

    covered = sum(1 for row in rows if row["shape"] != "NO_CLIENT_HTML")
    upper = sum(1 for row in rows if row["client_file"].startswith("QUEST"))
    print(f"\n[3] 形状聚类（客户端 quest_summary 覆盖 {covered} / {len(rows)} 个任务；"
          f"其中大写命名副本 {upper} 个）：")
    shape_counts = count("shape")
    for name, value in sorted(shape_counts.items(), key=lambda item: -item[1]):
        print(f"  {name}: {value}")

    print("\n[4] 领奖行指标 × 形状：")
    for name in ("ROW_ALIGNED", "ROW_BEHIND", "ROW_AHEAD", "NO_REWARD_ROW", "NO_CLIENT_HTML"):
        shapes = shape_of_verdict(name)
        detail = " ".join(f"{key}={value}" for key, value in
                          sorted(shapes.items(), key=lambda item: -item[1]))
        print(f"  {name}: {sum(shapes.values())} ({detail or '-'})")

    missing = [row for row in rows if row["shape"] == "MISSING_LAST_ROW"]
    print(f"\n[5] 只缺最后一行（MISSING_LAST_ROW）={len(missing)}：")
    print(f"  末行是对话/报告/见面：{sum(1 for row in missing if row['last_row_kind'] == 'DIALOG')}"
          f"；末行是其它目标：{sum(1 for row in missing if row['last_row_kind'] == 'OTHER')}")
    print(f"  其中 QE-045 锁（reward 保持旧 packed step）："
          f"{sum(1 for row in missing if row['qe045_locked'])}")
    print(f"  其中已登记 QE-054 legacy 落盘 step 例外（禁止按末行索引改）："
          f"{sorted(row['quest_id'] for row in missing if int(row['quest_id']) in LEGACY_STEP_EXCEPTION)}")
    mirrored_missing = [row for row in missing
                        if (mirror := index.get(mirror_of(row["quest_id"])))
                        and mirror["client_rows"] == row["client_rows"]
                        and mirror["shape"] == "MISSING_LAST_ROW"]
    aligned_mirror = [(row, index[mirror_of(row["quest_id"])]) for row in missing
                      if (mirror := index.get(mirror_of(row["quest_id"])))
                      and mirror["client_rows"] == row["client_rows"]
                      and mirror["row_state_verdict"] == "ROW_STATE_ALIGNED"]
    print(f"  镜像同样缺末行（结构同型）：{len(mirrored_missing)}；"
          f"镜像已对齐（单侧缺陷嫌疑）：{len(aligned_mirror)}")
    for row, mirror in aligned_mirror[:20]:
        print(f"    {row['quest_id']} (reward={row['reward_var0']}) vs 镜像 "
              f"{mirror['quest_id']} (reward={mirror['reward_var0']}, 对齐)")
    direct = [row for row in missing
              if row["last_row_kind"] == "DIALOG" and not row["qe045_locked"]
              and row["verdict"] in ("ROW_BEHIND", "ROW_AHEAD")]
    singles = [row for row in direct
               if (mirror := index.get(mirror_of(row["quest_id"])))
               and mirror["client_rows"] == row["client_rows"]
               and mirror["row_state_verdict"] == "ROW_STATE_ALIGNED"
               and mirror["verdict"] == "ROW_ALIGNED"]
    print(f"  末行是领奖行且 reward 投影不是末行的候选：{len(direct)}"
          f"（QE-051 模板 = 投影改末行 + 无 source enter-world 恢复边；明细 audit-qe051-candidates.tsv）")
    print(f"  其中镜像一侧行数相同、行/状态全对齐且 reward 投影=其领奖行"
          f"（单侧缺陷、可直接当模板）：{len(singles)}")

    candidate_order = ["quest_id", "client_rows", "reward_var0", "last_start_node",
                       "last_start_var0", "recovery", "client_file", "client_section0",
                       "last_row_text"]
    with CANDIDATE_OUTPUT.open("w", encoding="utf-8") as out:
        out.write("\t".join(candidate_order + ["mirror_id", "mirror_shape",
                                                "mirror_reward_var0", "mirror_last_row_text",
                                                "mirror_is_single_side"]) + "\n")
        for row in direct:
            mirror = index.get(mirror_of(row["quest_id"]))
            # 强证据：镜像一侧行/状态全对齐且 reward 投影就是其领奖行（可直接当模板）
            single = bool(mirror and mirror["client_rows"] == row["client_rows"]
                          and mirror["row_state_verdict"] == "ROW_STATE_ALIGNED"
                          and mirror["verdict"] == "ROW_ALIGNED")
            out.write("\t".join(str(row[column]) for column in candidate_order) + "\t" + "\t".join([
                str(mirror["quest_id"]) if mirror else "",
                mirror["shape"] if mirror else "",
                str(mirror["reward_var0"]) if mirror else "",
                mirror["last_row_text"] if mirror else "",
                "YES" if single else "NO"]) + "\n")

    order = ["quest_id", "client_rows", "last_row_kind", "last_row_text", "reward_var0",
             "last_start_node", "last_start_var0", "handover_writes", "recovery", "qe045_locked",
             "verdict", "row_state_verdict", "client_section0"]
    with MISSING_LAST_OUTPUT.open("w", encoding="utf-8") as out:
        out.write("\t".join(order + ["mirror_id", "mirror_shape", "mirror_reward_var0",
                                     "mirror_last_row_text"]) + "\n")
        for row in missing:
            mirror = index.get(mirror_of(row["quest_id"]))
            out.write("\t".join(str(row[column]) for column in order) + "\t" + "\t".join([
                str(mirror["quest_id"]) if mirror else "",
                mirror["shape"] if mirror else "",
                str(mirror["reward_var0"]) if mirror else "",
                mirror["last_row_text"] if mirror else ""]) + "\n")

    beyond = [row for row in rows if row["shape"] == "STATES_BEYOND_ROWS"]
    print(f"\n[6] 状态越过客户端行数（STATES_BEYOND_ROWS）={len(beyond)}：")
    print(f"  其中 var0 被 increment-variable 累加（更像计数槽）："
          f"{sum(1 for row in beyond if row['var0_increment'])}")
    print(f"  其中客户端脚本声明了 SECTION_0 判定（更像行索引）："
          f"{sum(1 for row in beyond if row['client_section0'])}")
    print(f"  其中客户端只声明 1 行：{sum(1 for row in beyond if row['client_rows'] == 1)}；"
          f"2 行：{sum(1 for row in beyond if row['client_rows'] == 2)}")
    print("  样例：" + "; ".join(
        f"{row['quest_id']}(rows={row['client_rows']},states={row['visible_state_var0']})"
        for row in beyond[:8]))

    print("\n[7] 缺中间/尾部多行（INTERIOR_GAP / MISSING_TAIL_ROWS）：")
    for shape in ("MISSING_TAIL_ROWS", "INTERIOR_GAP", "NO_STATE"):
        subset = [row for row in rows if row["shape"] == shape]
        print(f"  {shape}: {len(subset)}；样例：" + "; ".join(
            f"{row['quest_id']}(rows={row['client_rows']},缺={row['rows_without_state'] or '-'})"
            for row in subset[:6]))

    print("\n[8] 客户端 quest_script 声明 SECTION_0 的任务（行索引的强旁证）：")
    evidence = [row for row in rows if row["client_section0"]]
    print(f"  共 {len(evidence)} 个；其中行↔状态已对齐 "
          f"{sum(1 for row in evidence if row['row_state_verdict'] == 'ROW_STATE_ALIGNED')}，"
          f"错位 {sum(1 for row in evidence if row['row_state_verdict'] != 'ROW_STATE_ALIGNED')}")
    for row in evidence:
        if row["row_state_verdict"] != "ROW_STATE_ALIGNED":
            print(f"    {row['quest_id']} rows={row['client_rows']} "
                  f"states={row['visible_state_var0'] or '-'} {row['row_state_verdict']} "
                  f"缺={row['rows_without_state'] or '-'} 越={row['states_out_of_range'] or '-'} "
                  f"script={row['client_section0'][:70]}")

    over_max = [row for row in rows if not row["reward_row_within_field_max"]]
    print(f"\n[7b] progress 字段上限（reward 投影 > 声明 max 会直接编译失败）={len(over_max)}：")
    for row in over_max[:20]:
        print(f"    {row['quest_id']} reward={row['reward_var0']} var0 max={row['var0_max']} "
              f"rows={row['client_rows']}")
    needs_raise = [row for row in rows
                   if row["shape"] == "MISSING_LAST_ROW" and not row["last_row_within_field_max"]]
    print(f"  若把口径改成客户端领奖行、需要同时抬高 max 的 MISSING_LAST_ROW 任务："
          f"{len(needs_raise)}（样例：" + ", ".join(
              f"{row['quest_id']}({row['var0_max']}->{row['client_last_row']})"
              for row in needs_raise[:8]) + "）")

    dialog_candidates = [row for row in rows
                         if row["shape"] == "MISSING_LAST_ROW" and row["last_row_kind"] == "DIALOG"]
    matched = [row for row in dialog_candidates if row["last_row_npc_matches_quest"]]
    print(f"\n[5b] 末行 NPC 名字证据（quest_summary 末行的 STR_DIC_N_XXX 能对上任务内 NPC 的客户端名）：")
    print(f"  MISSING_LAST_ROW 且末行是对话行：{len(dialog_candidates)}；"
          f"其中 NPC 名字对上：{len(matched)}；对不上：{len(dialog_candidates) - len(matched)}")
    print("  NPC 对不上的样例：" + "; ".join(
        f"{row['quest_id']}({row['last_row_npc_key'] or '无N键'})"
        for row in dialog_candidates if not row["last_row_npc_matches_quest"])[:400])

    print("\n[9] archdaeva chain 10520-10530 / 20520-20530：")
    for row in rows:
        if 10520 <= row["quest_id"] <= 10530 or 20520 <= row["quest_id"] <= 20530:
            print(f"  {row['quest_id']} rows={row['client_rows']} "
                  f"state_var0={row['visible_state_var0'] or '-'} "
                  f"reward={row['reward_var0']} {row['row_state_verdict']} "
                  f"missing={row['rows_without_state'] or '-'} "
                  f"out_of_range={row['states_out_of_range'] or '-'} "
                  f"writes={row['handover_writes'] or '-'} recovery={row['recovery']}")

    if requested:
        print("\n[10] 单任务明细：")
        for row in rows:
            for key in columns:
                print(f"  {key} = {row[key]}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
