#!/usr/bin/env python3
"""批次 21：精锐兵族 13918 / 23918 的链式 0/1 阶梯 + 领奖 owner 收敛 + 奖励索引补齐。

族级判据（Aion 5.8 客户端解包为唯一权威）：

* 两侧 `Dialogs/10000_19999/quest_q13918.html` 与 `Dialogs/20000_29999/quest_q23918.html` 的
  `quest_summary` 都是 6 行：行 0..4 依次消灭五只特殊精锐兵（每行 `([%n]/1)`），
  行 5 = 向 `STR_DIC_N_LDF4_Advance_Elger_E`(802350) / `STR_DIC_N_LDF4_Advance_Helgund_E`(802353) 报告。
* `Quest_unpacked/quest_monster.csv` 两侧逐行同形，门控是**链式**的：
  `SECTION_0<1; SECTION_5==0` / `SECTION_1<1; SECTION_0==1` / `SECTION_2<1; SECTION_1==1` /
  `SECTION_3<1; SECTION_2==1` / `SECTION_4<1; SECTION_3==1`。
  每槽都是 0/1 计数器、行 n 只有前一槽 ==1 时才可见 → COUNTER_CHAIN 家族
  （判别式见 `.agents/summary/quest-15001-multicounter-step/2026-09-19-section0-report-row-sweep.zh-CN.md`）。
* 客户端怪物名经 `npcs/npc_template_*.xml` 的 `name_desc` 对齐到服务端 npc id：
  Elyos 13918 = 235321/235322/235323/235324/235325；
  Asmodian 23918 = 235559/235560/235561/235326/235327。
* 接取 NPC 由客户端 `select1`(接受页) 与 `client_npcs_npc.xml` 对齐：13918 = 802328、23918 = 802347。

旧模型缺陷（两侧都在本批收口）：

* 13918 是 step 链（`started/k1..k5` 投影 `var0=1..5`，领奖投影 `var0=5,var1..3=1`）：客户端行 2 要求
  `SECTION_1==1`，而 var1 只在领奖投影里才为 1，因此第 3 只之后再没有任何任务书行能亮；同一模型还缺
  var4（客户端 `SECTION_4<1` 计数槽没有位域），`audit_section0_report_row_closure.py` 判 `COUNTER_CHAIN_GAP`。
* 23918 是 c44c50bd0 建的 5 维组合网格（32 节点自由顺序计数）：乱序击杀会写出客户端无法显示的状态
  （行 n 只在 `SECTION_{n-1}==1` 时可见），同一提交把 `fixed-reward-indices="0 1 2"` 误改成 `0 1`
  （丢掉 metadata 第三条 ITEM 169405255×6），领奖 owner 也仍是接取 NPC（QE-052）。

落点：两侧同形重建
`started(0)/k1(1)/k2(1,1)/k3(1,1,1)/k4(1,1,1,1)/k5(全 1)/reward(全 1)/complete(0)` 的串行阶梯，
每只精锐兵只推进自己那一格（`PACKET_ONLY`）；接取 owner 留在接取 NPC，报告与领奖 owner 收敛到行 5 NPC；
`fixed-reward-indices` 补回 `0 1 2`。13918 额外补旧 step 存档的迁移边（`var0=2..5 → k2..k5`，
领奖态 `var0>=2 → 全 1`）；两侧都补“领奖态计数未落齐（sum<5）”的迁移边。

用法：
  python3 apply_batch21_chain_elite_ladder.py --check   # 只校验（改动前应 FAIL，改完后 OK）
  python3 apply_batch21_chain_elite_ladder.py           # 应用改动，幂等
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

CONTRACTS = {
    "13918": {
        "mirror": "23918",
        "accept_npc": 802328,
        "report_npc": 802350,
        "report_key": "LDF4_Advance_Elger_E",
        "report_dic": "STR_DIC_N_LDF4_Advance_Elger_E",
        "kills": (235321, 235322, 235323, 235324, 235325),
        "legacy_step_model": True,
    },
    "23918": {
        "mirror": "13918",
        "accept_npc": 802347,
        "report_npc": 802353,
        "report_key": "LDF4_Advance_Helgund_E",
        "report_dic": "STR_DIC_N_LDF4_Advance_Helgund_E",
        "kills": (235559, 235560, 235561, 235326, 235327),
        "legacy_step_model": False,
    },
}

ALL_FIELDS = ("var0", "var1", "var2", "var3", "var4")

PROGRESS = """  <progress>
    <!-- 客户端 quest_monster.csv 的五行是链式 0/1 计数槽：行 0 = SECTION_0<1 且 SECTION_5==0，
         行 n = SECTION_n<1 且 SECTION_(n-1)==1。var0..var4 就是这五个槽（各占 SECTION_n = 6n 位），
         每只精锐兵只把自己那一槽推到 1，行随之沉下、下一行亮起；var5 不声明（SECTION_5 必须保持 0）。
         位域保留 6 bit（而不是 1 bit）：旧 step 模型把 var0 写成 0..5，只有 6 bit 才能读到并迁移这些存档，
         新模型自身只写 0/1（节点投影与自愈动作都由门禁锁死）。
         Client quest_monster.csv chains five 0/1 counter slots (row n shows while SECTION_n<1 and
         SECTION_(n-1)==1); var0..var4 are exactly those slots, so each elite owns one counter at 6n and
         SECTION_5 (never declared) stays 0. The 6-bit width is kept so legacy step-model saves
         (var0 = 2..5) stay readable for the migration edges below; the new model only ever writes 0/1. -->
    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
    <bit-field name="var1" offset="6" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
    <bit-field name="var2" offset="12" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
    <bit-field name="var3" offset="18" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
    <bit-field name="var4" offset="24" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>"""


def node(label: str, status: str, ones: int) -> str:
    body = "".join(f'      <var name="{name}" value="{1 if index < ones else 0}"/>\n'
                   for index, name in enumerate(ALL_FIELDS))
    return f'    <node label="{label}" status="{status}">\n{body}    </node>'


NODES = "  <nodes>\n" + "\n".join((
    node("unaccepted", "NONE", 0),
    node("started", "START", 0),
    node("k1", "START", 1),
    node("k2", "START", 2),
    node("k3", "START", 3),
    node("k4", "START", 4),
    node("k5", "START", 5),
    node("reward", "REWARD", 5),
    node("complete", "COMPLETE", 0),
)) + "\n  </nodes>"

SET_ALL = "".join(f'        <set-variable field="{name}" value="1"/>\n' for name in ALL_FIELDS)
SET_FIRST = "".join(f'        <set-variable field="{name}" value="1"/>\n' for name in ALL_FIELDS[:4])

HEAL_COMMENT = """    <!-- 迁移自愈 / Migration repair：旧领奖存档匹配不到本轮的五槽全 1 领奖投影
         （QuestMutationPlanner#matchesSourceNode 逐字段全等），登录/切图或点报告 NPC 时补正；
         旧 step 模型领奖投影是 var0=5,var1..3=1，旧 4 维网格领奖投影只落到 var0..var3。 -->
"""


def heal_enter_world(conditions: str, actions: str, target: str) -> str:
    return f"""    <transition target="{target}">
      <event>
        <enter-world/>
      </event>
      <conditions>
{conditions}      </conditions>
      <actions>
{actions}      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
"""


def heal_talk(npc: int, conditions: str, actions: str, target: str) -> str:
    return f"""    <transition target="{target}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>
      </event>
      <conditions>
{conditions}      </conditions>
      <actions>
{actions}      </actions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
"""


def heals(contract: dict) -> str:
    npc = contract["report_npc"]
    text = HEAL_COMMENT
    if contract["legacy_step_model"]:
        # 旧 step 模型：var0 = 已击杀数（0..5）。无损映射到本轮阶梯，避免把已完成 5 杀的存档打回起点。
        text += """    <!-- 旧 step 模型（var0 = 已击杀数 0..5）的无损迁移：var0=2..5 分别落到 k2..k5。
         Legacy step-model saves (var0 = kills done) map losslessly onto k2..k5. -->
"""
        for done in (5, 4, 3, 2):
            actions = "".join(f'        <set-variable field="{name}" value="1"/>\n'
                              for name in ALL_FIELDS[:done])
            conditions = (f'        <status-is status="START"/>\n'
                          f'        <variable-is field="var0" value="{done}"/>\n')
            text += heal_enter_world(conditions, actions, f"k{done}")
    text += heal_enter_world('        <status-is status="REWARD"/>\n'
                             '        <variable-at-least field="var0" value="2"/>\n', SET_ALL, "reward")
    text += heal_talk(npc, '        <status-is status="REWARD"/>\n'
                           '        <variable-at-least field="var0" value="2"/>\n', SET_ALL, "reward")
    text += heal_enter_world('        <status-is status="REWARD"/>\n'
                             '        <variable-sum-below fields="var0 var1 var2 var3 var4" value="5"/>\n',
                             SET_ALL, "reward")
    text += heal_talk(npc, '        <status-is status="REWARD"/>\n'
                           '        <variable-sum-below fields="var0 var1 var2 var3 var4" value="5"/>\n',
                      SET_ALL, "reward")
    return text


KILL_ROWS = (
    "行 0：LDF4_Advance_B_Killer_Dr_65_Ae / _01_65_Ae",
    "行 1：LDF4_Advance_B_Killer_Dr_65_Ah / _01_65_Ah",
    "行 2：LDF4_Advance_B1_Killer_Dr_65_Ah / _01_65_Ah",
    "行 3：LDF4_Advance_B_Killer_Da_65_Al / _Li_65_Al",
    "行 4：LDF4_Advance_B1_Killer_Da_65_Al / _Li_65_Al",
)


def kills(contract: dict) -> str:
    text = ""
    for index, npc in enumerate(contract["kills"]):
        source = "started" if index == 0 else f"k{index}"
        target = f"k{index + 1}"
        text += f"""    <!-- 第 {index + 1} 只（{KILL_ROWS[index]}）：只推进 SECTION_{index}，
         行 {index} 计数满 1 后下沉，行 {index + 1}（SECTION_{index + 1}<1 且 SECTION_{index}==1）亮起。 -->
    <transition source="{source}" target="{target}">
      <event>
        <kill-npc npc-id="{npc}"/>
      </event>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
"""
    return text


def transitions(contract: dict, quest_id: str) -> str:
    accept = contract["accept_npc"]
    report = contract["report_npc"]
    return f"""  <transitions>
    <!-- QE-051 领奖行合同 / QE-051 reward-row contract：客户端 quest_q{quest_id}.html 的 quest_summary 共 6 行
         （行 0..4 = 依次消灭五只特殊精锐兵，行 5 = 向 {contract['report_dic']}（{report}）报告）。
         quest_monster.csv 的门控是链式的，因此服务端必须是串行 0/1 阶梯：乱序击杀不计数，
         客户端任务书任何时刻都只亮“当前该打的那一只”。接取 owner = {accept}（行 0 前的接受页），
         报告与领奖 owner = {report}（行 5 的 NPC，QE-052）。
         The client chained SECTION gates make this a strictly serial 0/1 ladder: out-of-order kills do not
         count, so the journal only ever highlights the elite the player still owes. Offer owner stays
         {accept}; report and completion owner is the row-5 NPC {report}. -->
{heals(contract)}    <dialog type="NPC_START" npc-id="{accept}" source="unaccepted" target="started" selection-sources="unaccepted k5" start-page="SELECT1"/>
{kills(contract)}    <dialog type="NPC_REPORT" npc-id="{report}" source="k5" target="reward" page="SELECT2"/>
    <npc-complete npc-id="{report}" source="reward" target="complete" fixed-reward-indices="0 1 2" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>"""


KILLS_HEAD = "    <kills>\n"


def ensure_kills(text: str, contract: dict) -> str:
    """metadata <kills> 只是展示性狩猎步骤（QuestKillCounterRetailGateTest），但两侧必须同形：
    13918 已声明五只精锐兵、23918 在 c44c50bd0 重建时丢了整块，这里补回同样的五条。"""
    if "<kills>" in text:
        return text
    block = KILLS_HEAD + "".join(
        f'      <kill sequence="{index + 1}">\n        <npc id="{npc}"/>\n      </kill>\n'
        for index, npc in enumerate(contract["kills"])) + "    </kills>\n"
    return text.replace("    </rewards>\n", "    </rewards>\n" + block, 1)


def rewrite(text: str, quest_id: str) -> str:
    contract = CONTRACTS[quest_id]
    out = ensure_kills(text, contract)
    out = re.sub(r"  <progress>.*?</progress>", lambda _: PROGRESS, out, count=1, flags=re.S)
    out = re.sub(r"  <nodes>.*?</nodes>", lambda _: NODES, out, count=1, flags=re.S)
    return re.sub(r"  <transitions>.*?</transitions>",
                  lambda _: transitions(contract, quest_id), out, count=1, flags=re.S)


def main() -> int:
    check = "--check" in sys.argv
    problems = []
    for quest_id in sorted(CONTRACTS):
        path = QUESTS / f"{quest_id}.xml"
        text = path.read_text(encoding="utf-8")
        expected = rewrite(text, quest_id)
        if text == expected:
            print(f"BATCH21_OK quest={quest_id} already-applied")
            continue
        if check:
            problems.append(f"{quest_id}: needs batch21 chain-elite ladder rewrite")
            print(f"BATCH21_PENDING quest={quest_id}")
            continue
        path.write_text(expected, encoding="utf-8")
        print(f"BATCH21_APPLIED quest={quest_id}")
    if problems:
        print("BATCH21_CHECK_FAILED " + "; ".join(problems))
        return 1
    print("BATCH21_VERIFY_OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
