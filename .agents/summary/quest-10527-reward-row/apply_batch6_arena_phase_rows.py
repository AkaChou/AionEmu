#!/usr/bin/env python3
"""批次 6：恢复 18208/18209 与魔镜像 28208/28209（苍穹试炼场单人竞技场任务）击杀阶段与任务书阶段号。

证据（两份独立来源一致）：
1. Aion 5.8 客户端 quest_q18208/18209/28208/28209 的 quest_summary 各 3 行：
   第 1 行“消灭第 2 神庙训练所的最终训练对象 ([%2]/5)”、第 2 行“穿过…消灭 quest_18208a ([%5]/1)”、
   第 3 行“和 Molfus 对话”；客户端 quest_script_monster.csv 声明
   `SECTION_0==0; SECTION_1<5`（5 次 VanqJr 击杀，var1 计数）与 `SECTION_0==1; SECTION_2<1`
   （精英击杀，var2 计数，匹配 IDArena_Solo_H1_DrakanAs_noble_55_Ae 与 IDArena_Solo_H2_TempleD_Fi_55_Ae）。
2. origin/history 旧 handler（仓库内已删除，7e9f0316c 之前的 `_18208IllusionOrInfiltration`、
   `_18209ARiftInTheSpaceTwineContinuum`、`_28208ARiftAdrift`/`_28209CatchingTheRift`）：
   var0==0 时用 var1 累加 0..4（`defaultOnKillEvent(env, 217819, 0, 4, 1)`），var1==4 的第 5 次击杀
   把 var0 置 1；随后 var0==1 时击杀 218185 置 var2=1 并 `setStatus(REWARD)`。
客户端怪物表映射（Aion 5.8 客户端 npcs_unpacked/client_npcs_monster.xml）：
   217819 = IDArena_Solo_S6_VanqJr_55_An、218185 = IDArena_Solo_H1_DrakanAs_noble_55_Ae、
   218200 = IDArena_Solo_H2_TempleD_Fi_55_Ae。

当前缺陷：
- 18208/18209（天族）迁移后没有任何击杀路线（只有“对话即领奖”的捷径），客户端声明的
  SECTION_0==1 永远不可达、任务书第 2 行永远不亮，玩家不做目标也能完成。
- 28208/28209（魔族）迁移把每个击杀都当成一个 var0 阶段（k1..k7，var0=1..7），
  与客户端声明的 SECTION_0==0/==1 冲突（第一次击杀后第 1 行的计数就不再刷新），
  并且把客户端未声明的 218192（H2 TempleL）也当成必须击杀的目标。

本脚本把四份定义统一为“2 阶段 + 2 计数器”模型（var0@0 / var1@6 / var2@12），并保留各族现有的
接取与领奖 NPC 路线；reward 投影对齐客户端第 3 行（var0=2，同 2620/4210 的 QE-051 领奖行合同），
同时为旧存档补无 source 的 enter-world 修复边（避免卡死）：

用法（对 7e9f0316c 之后的当前版本运行）：
    python3 .agents/summary/quest-10527-reward-row/apply_batch6_arena_phase_rows.py
"""

from __future__ import annotations

from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

VANQ = 217819
ELITES = "218185 218200"


class Arena:
    """一个竞技场任务的目标 NPC 与接取/领奖 NPC。 / Objective and accept/report NPCs of one arena quest."""

    def __init__(self, quest_id: int, race: str, accept_npcs: list[int], report_npcs: list[int],
            accepter: int, comment: str) -> None:
        self.quest_id = quest_id
        self.race = race
        self.accept_npcs = accept_npcs
        self.report_npcs = report_npcs
        self.accepter = accepter
        self.comment = comment


ARENAS = [
    Arena(18208, "ELYOS", [205316], [205309], 205316,
          "天族单人竞技场：接取 NPC Inggril(205316)、领奖 NPC Molfus(205309)。"),
    Arena(18209, "ELYOS", [205309], [205309], 205309,
          "天族单人竞技场（衔接 18208）：接取与领奖 NPC 均为 Molfus(205309)。"),
    Arena(28208, "ASMODIANS", [205320, 205321], [205320, 205321], 205320,
          "魔族单人竞技场：接取/领奖 NPC 为 205320 与 205321（两份定义并存）。"),
    Arena(28209, "ASMODIANS", [205320, 205321], [205320, 205321], 205320,
          "魔族单人竞技场（衔接 28208）：接取/领奖 NPC 为 205320 与 205321。"),
]

HEADER_COMMENT = """    <!-- 客户端任务书三行 ↔ 服务端阶段：第 1 行“消灭最终训练对象 ([%2]/5)”= var0=0 时用 var1 计 5 次击杀，
         第 2 行“穿过…消灭 quest_18208a ([%5]/1)”= var0=1 时用 var2 计 1 次精英击杀，
         第 3 行“和 Molfus 对话”= REWARD 领奖行（var0=2，与 2620/4210 的 QE-051 领奖行合同一致）。
         客户端 quest_script 声明 `SECTION_0==0; SECTION_1<5` 与
         `SECTION_0==1; SECTION_2<1`，旧 handler 同样用 var0 0 -> 1、var1 0..4、var2 0 -> 1 并置 REWARD；
         迁移把天族侧的击杀路线整条丢掉、魔族侧改成 var0=1..7 的逐杀阶段，两边都与客户端声明冲突。
         Journal rows vs server stages: row 1 counts five {vanq} kills in var1 while var0=0, row 2 counts the
         single elite kill of {elites} in var2 while var0=1, and row 3 is the reward row (var0=2, the same
         QE-051 reward-row contract used by 2620/4210). The client script
         declares SECTION_0==0; SECTION_1<5 and SECTION_0==1; SECTION_2<1, and the legacy handlers used the
         same var0 0 -> 1, var1 0..4, var2 0 -> 1 progression before committing REWARD. -->
"""


def repair_edges() -> str:
    """旧存档收敛边：迁移版的 k1..k7（var0=1..7）映射回新的行号模型。 / Legacy-save repair edges."""
    return """    <!-- 旧存档修复：迁移版魔族把每个击杀都当成 var0 阶段（k1..k7 = var0 1..7，天族旧投影只有 var0=0），
         本版 var0 只表示任务书行（0=第 1 行、1=第 2 行、2=领奖行），因此旧 START 存档里 var0>=2 的存档
         不再匹配任何节点、旧 REWARD 存档里 var0!=2 的存档打不开领奖路线，必须在进入世界时收敛到新行号。
         - 旧 k2..k4（第一行进行中）：回第 1 行重新累计（旧版没有把击杀数写进 var1，无法恢复计数）；
         - 旧 k5..k7（第一行已杀满、含已杀精英）：进入第 2 行并补 var1=4；
         - 旧 REWARD/var0<2（天族旧投影或“对话即领奖”捷径）与 var0>=3（魔族旧 k3..k7）统一收敛到领奖行。
         Legacy-save repair: the migrated Asmodian build used one var0 stage per kill (k1..k7 = var0 1..7; the
         Elyos projection only ever held var0=0), while this version uses var0 as the journal row (0 = row 1,
         1 = row 2, 2 = reward row). Legacy START saves with var0>=2 match no node and legacy REWARD saves with
         var0!=2 cannot open the reward route, so they are collapsed onto the new row numbers on enter-world. -->
    <transition target="started">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="START"/>
        <variable-at-least field="var0" value="1"/>
        <variable-below field="var0" value="5"/>
        <variable-below field="var1" value="4"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="0"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition target="s1">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="START"/>
        <variable-at-least field="var0" value="5"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
        <set-variable field="var1" value="4"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-below field="var0" value="2"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-at-least field="var0" value="3"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>"""


def accept_routes(arena: Arena) -> str:
    """接取路线：全部走客户端简易接取页。 / Accept routes: the client simple-accept page."""
    lines = []
    for npc in arena.accept_npcs:
        lines.append(f'    <dialog type="NPC_START" npc-id="{npc}" source="unaccepted" target="started" '
                     f'selection-sources="unaccepted started" start-page="SELECT_NONE"/>')
    return "\n".join(lines)


def report_routes(arena: Arena) -> str:
    """领奖路线：REWARD 态对话打开奖励窗口，并保留迁移既有的“直接报告”捷径。 / Reward routes."""
    lines = []
    for npc in arena.report_npcs:
        lines.append(f"""    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>""")
    lines.append(f"""    <npc-complete npc-id="{arena.report_npcs[0]}" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>""")
    return "\n".join(lines)


def body(arena: Arena) -> str:
    """任务主体（progress/nodes/transitions）。 / Quest body (progress, nodes, transitions)."""
    comments = HEADER_COMMENT.replace("{vanq}", str(VANQ)).replace("{elites}", ELITES)
    return f"""  <progress>
    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
    <bit-field name="var1" offset="6" width="3" min="0" max="4" persistence="PERSISTENT" scope="LOCAL"/>
    <bit-field name="var2" offset="12" width="1" min="0" max="1" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>
  <nodes>
    <!-- 计数位不写进节点投影：投影会参与 source 匹配，自环累加后 packed 值必须仍匹配本节点。
         The counters are intentionally left out of the node projections: projections take part in source
         matching, so a self-loop that increments them must keep matching its own source node. -->
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="s1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
{comments}{repair_edges()}
{accept_routes(arena)}
    <!-- 第 1 行：5 次 {VANQ} 击杀，var1 0..4 累加，第 5 次把阶段推进到 s1（与旧 handler 一致）。
         Row 1: five {VANQ} kills accumulate var1 0..4; the fifth one advances the stage to s1. -->
    <transition source="started" target="started">
      <event>
        <kill-npc npc-id="{VANQ}"/>
      </event>
      <conditions>
        <variable-below field="var1" value="4"/>
      </conditions>
      <actions>
        <increment-variable field="var1" delta="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <kill-npc npc-id="{VANQ}"/>
      </event>
      <conditions>
        <variable-is field="var1" value="4"/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
    <!-- 第 2 行：精英击杀（H1 龙族贵族 / H2 神殿守护者）写入 var2 并直接进入领奖阶段。
         Row 2: the elite kill ({ELITES}) writes var2 and commits the reward stage. -->
    <transition source="s1" target="reward">
      <event>
        <kill-npc npc-ids="{ELITES}"/>
      </event>
      <conditions>
        <variable-is field="var2" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var2" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <!-- 迁移既有的“直接报告”捷径（retail 旧 handler 只在 REWARD 态处理领奖 NPC），本轮保留不收紧。
         The migrated talk-to-report shortcut is intentionally kept (the legacy handler only answered the
         reward NPC in REWARD state); this round does not tighten it. -->
    <dialog type="NPC_REPORT" npc-id="{arena.report_npcs[0]}" source="started" target="reward" page="DEFAULT_SUCCESS"/>
{report_routes(arena)}
  </transitions>
"""


def rewrite(arena: Arena) -> None:
    path = QUESTS / f"{arena.quest_id}.xml"
    text = path.read_text(encoding="utf-8")
    start = text.index("  <progress>")
    end = text.index("</quest-definition>")
    text = text[:start] + body(arena) + text[end:]
    path.write_text(text, encoding="utf-8")
    print(f"quest {arena.quest_id}: arena two-phase model written ({arena.comment})")


def main() -> None:
    for arena in ARENAS:
        rewrite(arena)


if __name__ == "__main__":
    main()
