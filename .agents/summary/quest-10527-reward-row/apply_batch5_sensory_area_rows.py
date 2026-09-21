#!/usr/bin/env python3
"""批次 5：修正 15550/25550 与 15551-15554/25551-25554（伊卢玛/诺斯沃尔德“导览”与“感应区坐骑”任务）。

背景与证据：
- 客户端 quest_summary 行清单（`/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs`）：
  * 15550/25550 共 3 行：和 Volter/Svanhild 对话、和 Ador/Conrto 对话、和 Aquaris/Vadorei 对话（领奖行）。
  * 15551-15554 与魔镜像共 3 行：使用 A->X 吸引物、使用 X->A 吸引物、和 Aquaris/Vadorei 对话（领奖行）。
- 旧 handler（origin/history；删除它们的迁移提交是 79bc5d3a6）：
  * 15550/25550：Volter/Svanhild `STEP_TO_1` 把 var0 0->1；Ador/Conrto `STEP_TO_2` 把 var0 1->2；
    Aquaris/Vadorei 的 `SELECT_REWARD` 置 REWARD，因此领奖态 = 最后一行的 var0=2。
  * 15551-15554/25551-25554：`onEnterZoneEvent` 在 `<ID>_A_TO_X` 感应区把 var0 0->1，
    在 `<ID>_X_TO_A` 感应区把 var0 1->2 并置 `QuestStatus.REWARD`。
- 迁移提交 79bc5d3a6 只保留了“进入感应区自动接取（unaccepted -> started）”，把上面的行推进整条丢了：
  中间行没有 START 状态、领奖态投影停在倒数第二行（审计 verdict：MISSING_LAST_ROW + ROW_WITHOUT_STATE）。

本脚本的改动（全部为断言式替换，任一处不匹配即报错退出）：
1. Group A（15550/25550）：reward 投影 1 -> 2、`s1 -> reward` 交接写 var0=2、补 REWARD/var0=1 的 enter-world 自愈边。
2. Group B（8 个任务）：var0 位段 width 1->2 / max 1->2；新增 START 节点 `s1`(var0=1) 并把 reward 推到 2；
   保留原有的两条 `unaccepted -> started` 感应区自动接取；恢复两条感应区行推进
   （`started -> s1` 用 A_TO_X 的 PACKET_ONLY；`s1 -> reward` 用 X_TO_A 的 LEVEL_AND_VISIBILITY_REFRESH）；
   `started -> reward` 交接写 var0=2；补 REWARD/var0=1 的 enter-world 自愈边。

用法（对 HEAD 版本运行可复现当前落库结果）：
    python3 .agents/summary/quest-10527-reward-row/apply_batch5_sensory_area_rows.py
"""

from __future__ import annotations

from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"


class GroupA:
    """15550/25550：三段 NPC 对话链（第 3 段是领奖行）。 / Three talk rows, the third one is the reward row."""

    def __init__(self, quest_id: int, handover_npc: int, reward_npc_id: int, first_npc: str,
            second_npc: str, reward_npc: str) -> None:
        self.quest_id = quest_id
        self.handover_npc = handover_npc
        self.reward_npc_id = reward_npc_id
        self.first_npc = first_npc
        self.second_npc = second_npc
        self.reward_npc = reward_npc


class GroupB:
    """15551-15554/25551-25554：两段感应区坐骑之后回到领奖 NPC。 / Two sensory-area rides then the reward NPC."""

    def __init__(self, quest_id: int, handler: str, zone_in: str, zone_back: str, npc_id: int,
            npc_name: str, first_ride: str, second_ride: str) -> None:
        self.quest_id = quest_id
        self.handler = handler
        self.zone_in = zone_in
        self.zone_back = zone_back
        self.npc_id = npc_id
        self.npc_name = npc_name
        self.first_ride = first_ride
        self.second_ride = second_ride


GROUP_A = [
    GroupA(15550, 806134, 806089, "LF6_Volter_E", "LF6_Ador_E", "LF6_Aquaris_E"),
    GroupA(25550, 806135, 806101, "DF6_Svanhild_E", "DF6_Conrto_E", "DF6_Vadorei_E"),
]

GROUP_B = [
    GroupB(15551, "_15551Giddyup_Starturtle",
           "LF6_SENSORY_AREA_Q15551_A_TO_B_210100000", "LF6_SENSORY_AREA_Q15551_B_TO_A_210100000",
           806089, "LF6_Aquaris_E", "LF6_A2_B1_FAttraction", "LF6_B1_A2_FAttraction"),
    GroupB(15552, "_15552Surfing_The_Ancient_Well",
           "LF6_SENSORY_AREA_Q15552_A_TO_D_210100000", "LF6_SENSORY_AREA_Q15552_D_TO_A_210100000",
           806089, "LF6_Aquaris_E", "LF6_A1_D_FAttraction", "LF6_D_A1_FAttraction"),
    GroupB(15553, "_15553Ride_An_Iluman_Butterfly",
           "LF6_SENSORY_AREA_Q15553_A_TO_F_210100000", "LF6_SENSORY_AREA_Q15553_F_TO_A_210100000",
           806089, "LF6_Aquaris_E", "LF6_A1_F1_FAttraction", "LF6_F1_A1_FAttraction"),
    GroupB(15554, "_15554A_Rickety_Ride",
           "LF6_SENSORY_AREA_Q15554_A_TO_H_210100000", "LF6_SENSORY_AREA_Q15554_H_TO_A_210100000",
           806089, "LF6_Aquaris_E", "LF6_A2_H_FAttraction", "LF6_H_A2_FAttraction"),
    GroupB(25551, "_25551Springleaf_Shortcut",
           "DF6_SENSORY_AREA_Q25551_A_TO_B_220110000", "DF6_SENSORY_AREA_Q25551_B_TO_A_220110000",
           806101, "DF6_Vadorei_E", "DF6_A2_B1_FAttraction", "DF6_B1_A2_FAttraction"),
    GroupB(25552, "_25552Pull_The_Lever",
           "DF6_SENSORY_AREA_Q25552_A_TO_D_220110000", "DF6_SENSORY_AREA_Q25552_D_TO_A_220110000",
           806101, "DF6_Vadorei_E", "DF6_A1_D_FAttraction", "DF6_D_A1_FAttraction"),
    GroupB(25553, "_25553Butterfly_March",
           "DF6_SENSORY_AREA_Q25553_A_TO_F_220110000", "DF6_SENSORY_AREA_Q25553_F_TO_A_220110000",
           806101, "DF6_Vadorei_E", "DF6_A1_F1_FAttraction", "DF6_F1_A1_FAttraction"),
    GroupB(25554, "_25554Reed_Patch_Rush",
           "DF6_SENSORY_AREA_Q25554_A_TO_H_220110000", "DF6_SENSORY_AREA_Q25554_H_TO_A_220110000",
           806101, "DF6_Vadorei_E", "DF6_A2_H_FAttraction", "DF6_H_A2_FAttraction"),
]


def replace_once(text: str, before: str, after: str, quest_id: int, label: str) -> str:
    count = text.count(before)
    if count != 1:
        raise SystemExit(f"quest {quest_id}: {label} expected exactly 1 match, found {count}")
    return text.replace(before, after, 1)


def comment_a(spec: GroupA) -> str:
    """Group A 的领奖行合同注释。/ Reward-row contract comment for group A."""
    return f"""    <!-- QE-051 领奖行合同：客户端 quest_q{spec.quest_id}.html 的 quest_summary 共 3 行——第 1 行“和
         {spec.first_npc} 对话”、第 2 行“和 {spec.second_npc} 对话”、第 3 行“和 {spec.reward_npc} 对话”（领奖 NPC {spec.reward_npc_id}）
         才是领奖行；旧投影把 REWARD 压在第 2 行（var0=1）。
         Reward row contract (QE-051): quest_q{spec.quest_id}.html lists three journal rows and only the third one
         ("talk to {spec.reward_npc}", reward NPC {spec.reward_npc_id}) is the reward row; the migrated projection parked
         REWARD on row 2 (var0=1), so the reward state must project the last row (var0=2). -->"""


def comment_b_reward(spec: GroupB) -> str:
    """Group B 的领奖行合同注释。/ Reward-row contract comment for group B."""
    return f"""    <!-- QE-051 领奖行合同：客户端 quest_q{spec.quest_id}.html 的 quest_summary 共 3 行——第 1 行“使用
         {spec.first_ride}”、第 2 行“使用 {spec.second_ride}”、第 3 行“和 {spec.npc_name} 对话”（领奖行）；
         旧投影把 REWARD 压在第 2 行（var0=1）。
         Reward row contract (QE-051): the three quest_q{spec.quest_id} journal rows are "use {spec.first_ride}",
         "use {spec.second_ride}" and "talk to {spec.npc_name}" (the reward row); the migrated projection
         parked REWARD on row 2 (var0=1), so the reward state must project the last row (var0=2). -->"""


def comment_b_advance(spec: GroupB) -> str:
    """Group B 的感应区行推进注释。/ Sensor row-advance comment for group B."""
    return f"""    <!-- 感应区行推进：旧 handler `{spec.handler}.onEnterZoneEvent` 在 A_TO_X 感应区把 var0 0 -> 1、
         在 X_TO_A 感应区把 var0 1 -> 2 并置 REWARD（两条路线正是客户端第 1、2 行的收口点）；
         迁移只保留了上面的自动接取，把两条推进整条丢了，于是第 2 行没有任何状态、领奖态停在 var0=1。
         Legacy onEnterZoneEvent row advance: entering the A_TO_X sensor set var0 0 -> 1 and entering the
         X_TO_A sensor set var0 1 -> 2 plus REWARD — the completion points of the two ride rows.
         The migration kept only the auto-accept above and dropped both advances, leaving row 2 without a
         state and the reward projection at var0=1. -->"""


def patch_group_a(spec: GroupA) -> None:
    quest_id = spec.quest_id
    path = QUESTS / f"{quest_id}.xml"
    text = path.read_text(encoding="utf-8")

    # 1. reward 投影推到客户端第 3 行（0 基的 2）。
    text = replace_once(text, """    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>""", """    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>""", quest_id, "reward projection")

    # 2. 进入领奖的交接路线写入与目标投影一致的领奖行。
    text = replace_once(text, f"""    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{spec.handover_npc}" action="SELECT_QUEST_REWARD"/>
      </event>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>""", f"""    <transition source="s1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{spec.handover_npc}" action="SELECT_QUEST_REWARD"/>
      </event>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>""", quest_id, "s1 -> reward hand-over")

    # 3. 旧存档自愈边 + 合同注释。
    text = replace_once(text, "  <transitions>", f"""  <transitions>
{comment_a(spec)}
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="1"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>""", quest_id, "recovery edge")

    path.write_text(text, encoding="utf-8")
    print(f"group A quest {quest_id}: reward row 1 -> 2, hand-over write 2, recovery edge added")


def patch_group_b(spec: GroupB) -> None:
    quest_id = spec.quest_id
    path = QUESTS / f"{quest_id}.xml"
    text = path.read_text(encoding="utf-8")

    # 1. var0 需要容纳到 2（位段 1 bit -> 2 bit）。
    text = replace_once(text,
        '<bit-field name="var0" offset="0" width="1" min="0" max="1" persistence="PERSISTENT" scope="LOCAL"/>',
        '<bit-field name="var0" offset="0" width="2" min="0" max="2" persistence="PERSISTENT" scope="LOCAL"/>',
        quest_id, "progress width/max")

    # 2. 新增中间行状态 s1 并把领奖行推到 2。
    text = replace_once(text, """    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>""", """    <node label="s1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>""", quest_id, "s1 node + reward projection")

    # 3. 领奖交接写 var0=2。
    text = replace_once(text, f"""    <transition source="started" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{spec.npc_id}" action="SELECT_QUEST_REWARD"/>
      </event>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>""", f"""    <transition source="started" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{spec.npc_id}" action="SELECT_QUEST_REWARD"/>
      </event>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>""", quest_id, "started -> reward hand-over")

    # 4. 旧存档自愈边（旧投影停在 1）。
    text = replace_once(text, "  <transitions>", f"""  <transitions>
{comment_b_reward(spec)}
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="1"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>""", quest_id, "recovery edge")

    # 5. 恢复 legacy handler 的两条感应区行推进（迁移只保留了自动接取）。
    anchor = f"""    <transition source="unaccepted" target="started">
      <event>
        <enter-zone zone="{spec.zone_back}"/>
      </event>
      <conditions>
        <start-eligible/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>"""
    text = replace_once(text, anchor, anchor + f"""
{comment_b_advance(spec)}
    <transition source="started" target="s1">
      <event>
        <enter-zone zone="{spec.zone_in}"/>
      </event>
      <conditions>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
    <transition source="s1" target="reward">
      <event>
        <enter-zone zone="{spec.zone_back}"/>
      </event>
      <conditions>
        <variable-is field="var0" value="1"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>""", quest_id, "zone row advance routes")

    path.write_text(text, encoding="utf-8")
    print(f"group B quest {quest_id}: s1 row added, reward row 1 -> 2, zone advances restored, recovery edge added")


def main() -> None:
    for spec in GROUP_A:
        patch_group_a(spec)
    for spec in GROUP_B:
        patch_group_b(spec)


if __name__ == "__main__":
    main()
