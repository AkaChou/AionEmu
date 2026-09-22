#!/usr/bin/env python3
"""批次 26：简报阶段（SECTION_5 标志位）+ Named/Boss 双层计数族收口 / Batch 26 briefing flag + named/boss ladders.

背景 / Why
---------
`audit_section0_report_row_closure.py` 最后剩下的 3 个 `COUNTER_CHAIN_GAP`：

* 24112（No Laissez-faire for Lepharists，魔族，Altgard）：客户端只有一条链式记录
  `Progress(SECTION_0<1; SECTION_5==0)`（消灭 LehparAsChD_15_An 210510，任务书行 1），
  任务书 3 行 = 行 0「去 DF1A 入口见见 Brodir」/ 行 1「消灭头目 ([%11]/1)」/ 行 2「告诉 Brodir」。
  旧 handler `_24112NoLaissezfaireforLepharists` 证明 `SECTION_5` 是**简报标志位**：
  `ACCEPT_QUEST_SIMPLE` 时 `setQuestVarById(5, 1)`（行 0 亮、行 1 被 `SECTION_5==0` 挡住），
  与 Brodir 对话的 `STEP_TO_1` 里 `setQuestVarById(5, 0); setQuestVarById(0, 0)`（行 1 亮），
  击杀 210510 时 `setQuestVarById(0, 1)`，最后 `SELECT_REWARD` 置 REWARD（var0=1、var5=0）。
  当前定义把 reward 投影写成 var0=0，而击杀路线已经把 var0 推到 1 —— 领奖态存档既不匹配 `reward`
  节点（投影 0）也不匹配任何路线（`started` 投影 0），玩家击杀后在 Brodir 处卡死。

* 30600 / 30610（[Group] Fight Of The Navigators / The Good News, And Bad）：客户端各有两条链式记录
  `SECTION_0<1; SECTION_5==0`（核控制室 Named 指挥官 219256/219257）与
  `SECTION_1<1; SECTION_0==1`（舰长室 IDDreadgion_03_DrakanWi_Boss_Ah 219264），任务书 4 行
  = 行 0「和 Linocus / Aluna 对话」/ 行 1「消灭 Named」/ 行 2「消灭舰长」/ 行 3「向 Hejitor / Astella 报告」。
  当前定义只有一个 `var0`（步骤号 0/1/2）：`var1` 缺失 → 客户端行 2 的计数永远是 0/1；
  还有两条**无守卫**的 `started --SETPRO1--> reward` 直跳（可以在接取后立刻领奖）。
  领奖 owner 被写成 205842（Ancanus）/205864（Udvi）——这两个 NPC 在静态 spawn 与实例/AI 代码里
  **都没有任何出场点**（旧 handler 的遗留），而任务书行 0/行 3 点名的 800324 Linocus / 800325 Hejitor
  在 210070000 Cygnea、800326 Aluna / 800327 Astella 在 220080000 Enshar 都有静态 spawn，
  客户端 quest_complete 文本也写明“（Astella/Hejitor）说…让我潜入…所以我去找（Aluna/Linocus）听取详细情况”。
  因此 owner 收敛到任务书 NPC，接取与报告都落在同一名 NPC（Hejitor / Astella），简报 NPC 是 Linocus / Aluna。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch26_briefing_and_named_ladders.py [--check]
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

QUEST_DIR = (Path(__file__).resolve().parents[3]
             / "src/main/resources/aion/data/static_data/quest_definition/quests")

VAR0_FIELD = ('    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" '
              'scope="LOCAL"/>\n')
VAR1_FIELD = ('    <bit-field name="var1" offset="6" width="6" min="0" max="63" persistence="PERSISTENT" '
              'scope="LOCAL"/>\n')
VAR5_FIELD = ('    <bit-field name="var5" offset="30" width="2" min="0" max="1" persistence="PERSISTENT" '
              'scope="LOCAL"/>\n')


def progress_block(two_slots: bool) -> str:
    """progress 块：var0[/var1] 计数槽 + var5 简报标志位。"""
    if two_slots:
        chain_zh = ('         行 1 = SECTION_0<1 且 SECTION_5==0（核控制室的 Named 指挥官），\n'
                    '         行 2 = SECTION_1<1 且 SECTION_0==1（舰长室的舰长）。\n')
        chain_en = ('         Client quest_monster.csv chains 0/1 counter slots: row 1 shows while SECTION_0<1\n'
                    '         with the briefing flag clear, row 2 while SECTION_1<1 and SECTION_0==1.\n')
        slots_zh, slots_en = 'var0/var1', 'var0/var1'
        fields = VAR0_FIELD + VAR1_FIELD
    else:
        chain_zh = '         行 1 = SECTION_0<1 且 SECTION_5==0（头目苏马尔浑）。\n'
        chain_en = ('         Client quest_monster.csv chains a single 0/1 counter slot: row 1 shows while\n'
                    '         SECTION_0<1 with the briefing flag clear.\n')
        slots_zh, slots_en = 'var0', 'var0'
        fields = VAR0_FIELD
    return ('  <progress>\n'
            '    <!-- 客户端 quest_monster.csv 的行是链式 0/1 计数槽：\n'
            + chain_zh
            + f'         {slots_zh} 就是这些计数槽（各占 SECTION_n = 6n 位），var5（SECTION_5）是简报标志位：\n'
              '         接取时置 1（任务书亮“去见简报 NPC”那一行），与简报 NPC 对话后清 0，击杀计数行才出现；\n'
              '         客户端 script 的 SECTION_5==0 门控即由此满足。\n'
            + chain_en
            + f'         {slots_en} are those slots at 6n and var5 is the briefing flag, raised on accept and\n'
              "         cleared by the briefing talk so the client's SECTION_5==0 gate matches the journal. -->\n"
            + fields + VAR5_FIELD + "  </progress>\n")


def node(label: str, status: str, variables: dict[str, int]) -> str:
    body = "".join(f'      <var name="{name}" value="{value}"/>\n' for name, value in variables.items())
    return f'    <node label="{label}" status="{status}">\n{body}    </node>\n'


def accept_row(npc: int, page: str = "SELECT1") -> str:
    return (f'    <dialog type="NPC_START" npc-id="{npc}" source="unaccepted" target="started" '
            f'selection-sources="unaccepted" start-page="{page}"/>\n')


def briefing_row(brief_npc: int, brief_name: str) -> str:
    """行 0 的两段式对话：QUEST_SELECT 只回应客户端 talk 并展示 select2 页，页面上的
    “结束对话”按钮（HACTION_SETPRO1 = 10000）才清 var5 并推进到 briefed。

    Row-0 dialog is two-step: QUEST_SELECT only answers the client talk and opens the select2 page;
    the visible “end dialog” button (HACTION_SETPRO1 = 10000) clears var5 and advances to briefed.
    The split is required because the client select2 page exposes exactly one visible action (10000)."""
    return (f'    <!-- 行 0a：向 {brief_name} {brief_npc} 对话，展示简报页 select2（状态不变，标志位保持）。\n'
            '         Row 0a: talking to the briefing NPC opens the select2 page without touching the flag. -->\n'
            '    <transition source="started" target="started">\n'
            '      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{brief_npc}" action="QUEST_SELECT"/>\n'
            '      </event>\n'
            '      <after-commit>\n'
            '        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>\n'
            '      </after-commit>\n'
            '    </transition>\n'
            f'    <!-- 行 0b：点击简报页的“结束对话”（SETPRO1=10000）后清 var5，'
            '让击杀计数行（SECTION_5==0）亮起。\n'
            '         Row 0b: the visible end-dialog button clears var5 so the SECTION_5==0 kill row becomes visible. -->\n'
            '    <transition source="started" target="briefed">\n'
            '      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{brief_npc}" action="SETPRO1"/>\n'
            '      </event>\n'
            '      <after-commit>\n'
            '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
            '        <close-dialog/>\n'
            '      </after-commit>\n'
            '    </transition>\n')


def kill_step(source: str, target: str, npc_ids: str, comment: str) -> str:
    event = (f'        <kill-npc npc-ids="{npc_ids}"/>\n' if " " in npc_ids
             else f'        <kill-npc npc-id="{npc_ids}"/>\n')
    return (comment
            + f'    <transition source="{source}" target="{target}">\n'
            '      <event>\n'
            + event
            + '      </event>\n'
            '      <after-commit>\n'
            '        <sync-quest-state mode="PACKET_ONLY"/>\n'
            '      </after-commit>\n'
            '    </transition>\n')


def npc_report(npc: int, source: str, comment: str) -> str:
    return (comment
            + f'    <dialog type="NPC_REPORT" npc-id="{npc}" source="{source}" target="reward" page="SELECT5"/>\n')


def npc_complete(npc: int, indices: str) -> str:
    return (f'    <npc-complete npc-id="{npc}" source="reward" target="complete" '
            f'fixed-reward-indices="{indices}" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" '
            'complete-reward-index="0" finish="SELECTION_DIALOG">\n'
            '      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>\n'
            '    </npc-complete>\n')


def cond(tag: str, field: str, value: str) -> str:
    """一条带缩进的 transition 条件行 / one indented condition line."""
    return f'        <{tag} field="{field}" value="{value}"/>\n'


def migration_edge(target: str, status: str, conditions: list[str],
                   sets: list[tuple[str, str]]) -> str:
    body = f'        <status-is status="{status}"/>\n' + "".join(conditions)
    actions = "".join(f'        <set-variable field="{field}" value="{value}"/>\n' for field, value in sets)
    return (f'    <transition target="{target}">\n'
            '      <event>\n'
            '        <enter-world/>\n'
            '      </event>\n'
            '      <conditions>\n'
            + body
            + '      </conditions>\n'
            '      <actions>\n'
            + actions
            + '      </actions>\n'
            '      <after-commit>\n'
            '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
            '      </after-commit>\n'
            '    </transition>\n')


LEPHARIST_NODES = ('  <nodes>\n'
                   + node("unaccepted", "NONE", {"var0": 0, "var5": 0})
                   + node("started", "START", {"var0": 0, "var5": 1})
                   + node("briefed", "START", {"var0": 0, "var5": 0})
                   + node("killed", "START", {"var0": 1, "var5": 0})
                   + node("reward", "REWARD", {"var0": 1, "var5": 0})
                   + node("complete", "COMPLETE", {"var0": 0, "var5": 0})
                   + '  </nodes>\n')


DREDGION_NODES = ('  <nodes>\n'
                  + node("unaccepted", "NONE", {"var0": 0, "var1": 0, "var5": 0})
                  + node("started", "START", {"var0": 0, "var1": 0, "var5": 1})
                  + node("briefed", "START", {"var0": 0, "var1": 0, "var5": 0})
                  + node("k1", "START", {"var0": 1, "var1": 0, "var5": 0})
                  + node("k2", "START", {"var0": 1, "var1": 1, "var5": 0})
                  + node("reward", "REWARD", {"var0": 1, "var1": 1, "var5": 0})
                  + node("complete", "COMPLETE", {"var0": 0, "var1": 0, "var5": 0})
                  + '  </nodes>\n')


def lepharist_transitions() -> str:
    return ('  <transitions>\n'
            '    <!-- 接取：Nokir 203631；目标节点 started 的投影把 var5 置 1（行 0「去见 Brodir」由此点亮）。\n'
            '         Accept from Nokir; the started projection raises the briefing flag var5. -->\n'
            + accept_row(203631)
            + briefing_row(832821, "Brodir")
            + kill_step("briefed", "killed", "210510",
                        '    <!-- 行 1：消灭 LehparAsChD_15_An 210510，只推自己那一槽（SECTION_0）。\n'
                        '         Row 1: the named kill advances only its own slot. -->\n')
            + npc_report(832821, "killed",
                         '    <!-- 行 2：向 Brodir 报告（客户端 select5）；只有击杀完成后才有这条入口，\n'
                         '         旧定义的 started->reward 直跳已删除。\n'
                         '         Row 2: report to Brodir; the entry exists only after the kill. -->\n')
            + npc_complete(832821, "0 1 2")
            + '    <!-- 旧存档迁移 1：接取后没听简报就先杀怪的存档（var0=1 且 var5=1）归一化到 killed。\n'
              '         Legacy saves that killed the named mob before the briefing normalize onto killed. -->\n'
            + migration_edge("killed", "START",
                             [cond("variable-is", "var0", "1"), cond("variable-is", "var5", "1")],
                             [("var5", "0"), ("var0", "1")])
            + '    <!-- 旧存档迁移 2：旧定义（reward 投影 var0=0）留下的领奖态存档补齐到饱和投影 (1,0)。\n'
              '         Reward saves left by the previous definition are completed to the saturated (1,0). -->\n'
            + migration_edge("reward", "REWARD", [cond("variable-below", "var0", "1")],
                             [("var0", "1"), ("var5", "0")])
            + '  </transitions>\n')


def dredgion_transitions(accept_npc: int, brief_npc: int, brief_name: str, report_npc: int,
                         report_name: str, indices: str) -> str:
    return ('  <transitions>\n'
            '    <!-- 接取与报告都落在任务书点名的 NPC（行 0 的简报 NPC 与行 3 的报告 NPC）；'
            '目标节点 started 的投影把 var5 置 1。\n'
            '         Accept and report both use the journal NPCs; the started projection raises var5. -->\n'
            + accept_row(accept_npc)
            + briefing_row(brief_npc, brief_name)
            + kill_step("briefed", "k1", "219256 219257",
                        '    <!-- 行 1：核控制室 Named 指挥官 219256/219257 任一 1 只，只推 SECTION_0。\n'
                        '         Row 1: either named commander advances only SECTION_0. -->\n')
            + kill_step("k1", "k2", "219264",
                        '    <!-- 行 2：舰长室 IDDreadgion_03_DrakanWi_Boss_Ah 219264，只推 SECTION_1'
                        '（客户端行 2 的门控是 SECTION_1<1 且 SECTION_0==1）。\n'
                        '         Row 2: the boss advances only SECTION_1. -->\n')
            + npc_report(report_npc, "k2",
                         f'    <!-- 行 3：向 {report_name} {report_npc} 报告（客户端 select5）并领奖；'
                         '旧定义的无守卫 SETPRO1 直跳已删除。\n'
                         f'         Row 3: report to {report_name} and claim the reward. -->\n')
            + npc_complete(report_npc, indices)
            + '    <!-- 旧存档迁移 1：旧“步骤号”定义打完舰长写 var0=2，START 存档映射到 k2。\n'
              '         The legacy step model wrote var0=2 after the boss kill; START var0=2 maps onto k2. -->\n'
            + migration_edge("k2", "START", [cond("variable-is", "var0", "2")],
                             [("var0", "1"), ("var1", "1"), ("var5", "0")])
            + '    <!-- 旧存档迁移 2：旧 REWARD 投影 var0=2（var1 从未写过）补齐到饱和投影 (1,1)。\n'
              '         The old REWARD projection var0=2 is completed to the saturated (1,1). -->\n'
            + migration_edge("reward", "REWARD", [cond("variable-is", "var0", "2")],
                             [("var0", "1"), ("var1", "1"), ("var5", "0")])
            + '  </transitions>\n')


BLOCKS = {
    24112: {"progress": progress_block(False), "nodes": LEPHARIST_NODES,
            "transitions": lepharist_transitions()},
    30600: {"progress": progress_block(True), "nodes": DREDGION_NODES,
            "transitions": dredgion_transitions(800325, 800324, "Linocus", 800325, "Hejitor", "0 1 2 3")},
    30610: {"progress": progress_block(True), "nodes": DREDGION_NODES,
            "transitions": dredgion_transitions(800327, 800326, "Aluna", 800327, "Astella", "0 1 2 3")},
}

# 幂等标记：每个任务收口后的独有片段 / Idempotence markers, one per quest.
MARKERS = {
    24112: ('<bit-field name="var5" offset="30"', 'npc-id="832821" action="SETPRO1"'),
    30600: ('<bit-field name="var1" offset="6"', 'npc-id="800324" action="SETPRO1"'),
    30610: ('<bit-field name="var1" offset="6"', 'npc-id="800326" action="SETPRO1"'),
}


def replace_block(text: str, tag: str, new_block: str) -> str:
    pattern = re.compile(rf"  <{tag}>.*?\n[ ]*</{tag}>\n", re.S)
    if len(pattern.findall(text)) != 1:
        raise SystemExit(f"BATCH26_BLOCK_ANCHOR_MISSING tag={tag}")
    return pattern.sub(lambda _match: new_block, text, count=1)


def rewrite(quest_id: int, text: str) -> str:
    if all(marker in text for marker in MARKERS[quest_id]):
        return text
    for tag in ("progress", "nodes", "transitions"):
        text = replace_block(text, tag, BLOCKS[quest_id][tag])
    return text


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply the batch-26 briefing-flag ladders.")
    parser.add_argument("--check", action="store_true", help="只校验，不写文件 / verify only")
    args = parser.parse_args()

    quests = sorted(BLOCKS)
    pending = []
    for quest_id in quests:
        path = QUEST_DIR / f"{quest_id}.xml"
        if not path.exists():
            raise SystemExit(f"missing {path}")
        original = path.read_text(encoding="utf-8")
        target = rewrite(quest_id, original)
        if target != original:
            pending.append(quest_id)
            if not args.check:
                path.write_text(target, encoding="utf-8")
    if args.check:
        if pending:
            print(f"BATCH26_PENDING quests={pending}")
            return 1
        print(f"BATCH26_OK quests={quests} already-applied")
        return 0
    if pending:
        print(f"BATCH26_APPLIED quests={pending}")
    else:
        print(f"BATCH26_OK quests={quests} already-applied")
    return 0


if __name__ == "__main__":
    sys.exit(main())
