#!/usr/bin/env python3
"""批次 52：18301/28301（阿图拉姆空中要塞「监视水晶球」双子）行阶梯重建脚本。

背景（report §五十六）：
- 客户端 quest_summary 各 3 行（槽位 0/3/6）：行 0 = 破坏兵站区 7 个监视水晶球（([%2]/7)）、
  行 1 = 击毁胡根后取得 H-Core（动力装置）、行 2 = 把动力装置交给 Hariken 领奖；
- 客户端 quest_script_monster.csv 对 idstation_fobj_dr_cm_01 声明 Progress(0~6)，即七个水晶球
  占 step 0..6，第七个把 SECTION_0 推到 7；H-Core（730374）由副本脚本在 Weapon Hugen
  死亡时生成（AturamSkyFortressInstance#onDie 217371）；
- 迁移前 handler `_18301MyPrec_H_ious`/`_28301Power_On`：水晶球 var<7 每次 +1、var==7 才在
  H-Core 处拾取装置并置 REWARD，领奖在 Hariken。天族 18301 被塌陷成 started -> reward 且
  reward 投影 0（审计 ROW_BEHIND | MISSING_TAIL_ROWS | ROW_WITHOUT_STATE），魔族 28301 有计数
  阶梯但三个 NPC 都能发装置；本批把两侧收敛成同形。

用法 / Usage: python3 apply_batch52_aturam_crystal_ladder.py [--apply|--check]
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"

CRYSTALS = "702656 730373"
H_CORE = 730374
HARIKEN = 799530

SPECS = {
    18301: {
        "item": 182212100,
        "item_zh": "quest_18301a = 182212100",
        "item_en": "the Elyos work item 182212100 (quest_18301a)",
        "row0_zh": ("    <!-- 行 0：兵站区域 7 个监视水晶球（普通副本 702656 / 活动副本 730373，"
                    "同族 18314/28314 的 id 组合），\n"
                    "         客户端 quest_script 声明 Progress(0~6)，正好覆盖 0..6 七个 step；"
                    "第 7 个把 var0 推到 7。\n"
                    "         Row 0: the seven spy crystals (702656 in the normal instance, "
                    "730373 in the event instance);\n"
                    "         the client script declares Progress(0~6), so seven kills own steps 0..6 "
                    "and the last one opens k7. -->"),
        "row1_zh": ("    <!-- 行 1：击毁守护兵胡根后掉落的 H-Core（730374）。legacy 只在 var==7 时开放拾取页，\n"
                    "         取走装置即落入行 2（领奖行），发本任务的工作物品 {item_zh}。\n"
                    "         Row 1: the H-Core (730374) dropped by Weapon Hugen; the legacy handler only "
                    "opens the pickup\n"
                    "         pages at var==7 and grants {item_en} when it is taken. -->"),
        "row2_zh": ("    <!-- 行 2：把动力装置交给 Hariken 领奖（客户端 select_success(10002) 的按钮\n"
                    "         HACTION_SELECT_QUEST_REWARD(1009) 由 npc-complete 的 preview 路由打开\n"
                    "         select_quest_reward1(5)）。\n"
                    "         Row 2: hand the device to Hariken; select_success(10002) -> "
                    "HACTION_SELECT_QUEST_REWARD(1009)\n"
                    "         opens the reward window select_quest_reward1(5). -->"),
        "heal_zh": ("    <!-- 自愈边：迁移期 reward 投影停在 0，领奖态旧存档进世界时补到水晶计数/领奖行 7。\n"
                    "         Heal edge: the migrated REWARD projection stopped at 0, so stale reward "
                    "saves move to step 7. -->"),
    },
    28301: {
        "item": 182212110,
        "item_zh": "182212110",
        "item_en": "this faction's work item 182212110",
        "row0_zh": ("    <!-- 行 0：兵站区域 7 个监视水晶球（普通副本 702656 / 活动副本 730373），"
                    "客户端 quest_script 声明\n"
                    "         Progress(0~6)，第七个把 SECTION_0 推到 7。与天族镜像 18301 同形。\n"
                    "         Row 0: the seven spy crystals; the client script declares Progress(0~6), "
                    "so seven kills own steps 0..6.\n"
                    "         Mirrors the Elyos half 18301. -->"),
        "row1_zh": ("    <!-- 行 1：击毁守护兵胡根后掉落的 H-Core（730374），legacy 只在 var==7 时开放拾取页，\n"
                    "         取走装置即落入行 2（领奖行），发本任务的工作物品 {item_zh}。\n"
                    "         Row 1: the H-Core (730374) dropped by Weapon Hugen; the legacy handler only "
                    "opens the pickup\n"
                    "         pages at var==7 and grants {item_en} when it is taken. -->"),
        "row2_zh": ("    <!-- 行 2：把动力装置交给 Hariken 领奖（客户端 select_success(10002) 的按钮 "
                    "HACTION_SELECT_QUEST_REWARD(1009)\n"
                    "         由 npc-complete 的 preview 路由打开 select_quest_reward1(5)）。"
                    "行 2：claim row（领奖 owner 只留 Hariken）。\n"
                    "         Row 2: hand the device to Hariken; the 1009 button is served by the "
                    "npc-complete preview routes. -->"),
        "heal_zh": ("    <!-- 自愈边：迁移期 reward 投影停在 0 的旧存档进世界时补到水晶计数/领奖行 7。\n"
                    "         Heal edge: stale REWARD/var0&lt;7 saves move to step 7 on enter-world. -->"),
    },
}

NODES = '''  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="k1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="k2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="k3" status="START">
      <var name="var0" value="3"/>
    </node>
    <node label="k4" status="START">
      <var name="var0" value="4"/>
    </node>
    <node label="k5" status="START">
      <var name="var0" value="5"/>
    </node>
    <node label="k6" status="START">
      <var name="var0" value="6"/>
    </node>
    <node label="k7" status="START">
      <var name="var0" value="7"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="7"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
'''

TRANSITIONS = '''  <transitions>
    <!-- 接取：Hariken（799530），客户端页 select_none(4762) -> ask_quest_accept(4) -> quest_accept_1(1003)。
         Accept at Hariken (799530); the client pages are select_none/ask_quest_accept/quest_accept_1. -->
    <dialog type="NPC_START" npc-id="799530" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
{row0}
    <kill-chain nodes="started k1 k2 k3 k4 k5 k6 k7">
      <event>
        <kill-npc npc-ids="{crystals}"/>
      </event>
    </kill-chain>
{row1}
    <transition source="k7" target="k7">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{h_core}" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="k7" target="k7">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{h_core}" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="k7" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{h_core}" action="SETPRO2"/>
      </event>
      <actions>
        <give-item item-id="{item}" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
{row2}
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{hariken}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="{hariken}" source="reward" target="complete" fixed-reward-indices="0" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="1"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="2"/>
      <choice action="SELECTED_QUEST_REWARD3" reward-index="3"/>
      <choice action="SELECTED_QUEST_REWARD4" reward-index="4"/>
      <choice action="SELECTED_QUEST_REWARD5" reward-index="5"/>
      <choice action="SELECTED_QUEST_REWARD6" reward-index="6"/>
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
{heal}
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-below field="var0" value="7"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="7"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
  </transitions>
</quest-definition>
'''


def expected_body(quest_id: int) -> str:
    spec = SPECS[quest_id]
    return NODES + TRANSITIONS.format(
        row0=spec["row0_zh"],
        row1=spec["row1_zh"].format(item_zh=spec["item_zh"], item_en=spec["item_en"]),
        row2=spec["row2_zh"],
        heal=spec["heal_zh"],
        crystals=CRYSTALS,
        h_core=H_CORE,
        hariken=HARIKEN,
        item=spec["item"],
    )


def head(path: Path) -> str:
    raw = path.read_text(encoding="utf-8")
    index = raw.index("  <nodes>")
    return raw[:index]


def main() -> int:
    apply = "--apply" in sys.argv
    check = "--check" in sys.argv
    if not apply and not check:
        print("usage: apply_batch52_aturam_crystal_ladder.py [--apply|--check]")
        return 2
    status = 0
    for quest_id in sorted(SPECS):
        path = QUESTS / f"{quest_id}.xml"
        expected = head(path) + expected_body(quest_id)
        actual = path.read_text(encoding="utf-8")
        if apply:
            if actual != expected:
                path.write_text(expected, encoding="utf-8")
                print(f"BATCH52_APPLIED quest={quest_id}")
            else:
                print(f"BATCH52_ALREADY_APPLIED quest={quest_id}")
        if check:
            ok = actual == expected
            print(f"BATCH52_CHECK {'OK' if ok else 'MISMATCH'} quest={quest_id}")
            status = status or (0 if ok else 1)
    return status


if __name__ == "__main__":
    raise SystemExit(main())
