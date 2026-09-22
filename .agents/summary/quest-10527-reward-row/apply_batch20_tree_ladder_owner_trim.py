#!/usr/bin/env python3
"""批次 20：焦树族 23809 / 13809 补回三棵树的行阶梯并收敛领奖 owner。

族级判据：

* 同形镜像对 `q` / `q±10000` 的客户端 `quest_summary` 行数相同（本族均 4 行）、末行都指向任务内的领奖 NPC；
* 一侧（13809）已有 `0..3` 的行阶梯但 owner 冗余（`NPC_START` / `npc-complete` 各 4 条，三棵树也兼任接取与领奖）；
* 另一侧（23809）被迁移塌陷成“三棵树各有一条 started -> reward 的 SETPRO1 直跳 + 整包交付”，
  只剩行 0 一个状态（audit: ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE，缺行 1 2 3）。

权威证据（见同目录 batch20-prework-tree-family.zh-CN.md 与附录报告）：

* 迁移前 legacy handler（`7e9f0316c^` 的 `_23809Scar_Of_The_Past.java` / `_13809Tree_Is_Company.java`，两侧逐行同形）：
  `802429`(Vidarr/23809) 与 `802427`(Caetess/13809) 接取 + 对话 + 领奖；
  `730969`/`730970`/`730971` 三棵树只做 `useQuestObject(env, 0,1 / 1,2 / 2,3, false, 0)`
  （第 5 个参数是 varNum，**不发放物品**），领奖 NPC 的 `changeQuestStep(3, 4, true)` 只置 REWARD、保留 packed step 3。
* 客户端 `Dialogs/**/quest_q23809.html` 与 `quest_q13809.html` 两侧同形：
  `select1`(accept) / `select2`(HACTION_SETPRO1) / `select3`(HACTION_SETPRO2) / `select4`(HACTION_SETPRO3) /
  `select5`(HACTION_SELECT_QUEST_REWARD)；`quest_summary` 行 0/1/2 = 调查三棵树并采集 quest_<id>a/b/c，行 3 = 向守卫报告。
* `quest_data.xml` 两侧都只声明 `<quest_work_items>`（无 quest_drop、无 collect_items）：收集物只能由任务自身发放，
  这是 13809 侧缺失 `give-item` 的判据（23809 侧已有）。

落点：

* 节点：`unaccepted(0) / started(0) / stage1(1) / stage2(2) / reward(3) / complete(0)`，progress `var0` 保持 6 位宽。
* 每棵树两条边：`USE_OBJECT` 显示本树页（SELECT2/3/4），`SETPRO1/2/3` 推进一格并发放本行收集物；
  前两格 `PACKET_ONLY`（legacy `sendUpdatePacket` 在 START 态只发 SM_QUEST_ACTION），
  第三格进入 REWARD 用 `LEVEL_AND_VISIBILITY_REFRESH`（legacy 在 REWARD/COMPLETE 额外 `updateZone` + `updateNearbyQuests`）。
* 接取与领奖 owner 都收敛到领奖 NPC（23809=802429、13809=802427），三棵树不再有 `NPC_START` / `npc-complete`（QE-052）。
* 行 3 由 `reward -- QUEST_SELECT --> reward` 显示客户端 `select5` 页；`SELECT_QUEST_REWARD` 与领奖窗口由
  `npc-complete` 的 `preview` 承接（批次 19 同形，避免 AMBIGUOUS_TRANSITION）。
* 旧存档自愈：塌陷定义把“在三棵树处一次性交付”的玩家写成 `REWARD + var0=0`，补无 source 的
  `enter-world (REWARD && var0==0 -> var0=3)` 边，与 JournalRewardRowRepairContractTest(13809, 3, 0) 合同一致。

用法：
  python3 apply_batch20_tree_ladder_owner_trim.py --check   # 只校验（改动前应 FAIL，改完后 OK）
  python3 apply_batch20_tree_ladder_owner_trim.py           # 应用改动，幂等
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

REPORT_NPC = {"23809": 802429, "13809": 802427}
ITEMS = {
    "23809": (182215493, 182215494, 182215495),
    "13809": (182215485, 182215486, 182215487),
}
TREES = (730969, 730970, 730971)
ROW_PAGES = ("SELECT2", "SELECT3", "SELECT4")
ROW_ACTIONS = ("SETPRO1", "SETPRO2", "SETPRO3")
ROW_SYNCS = ("PACKET_ONLY", "PACKET_ONLY", "LEVEL_AND_VISIBILITY_REFRESH")
TREE_NAMES = ("烧焦的树（scorched tree）", "烧剩的树（cindery tree）", "烧成灰烬的树（burnt tree）")

NODES = """  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="stage1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="stage2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="3"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>"""

HEADER = """  <transitions>
    <!-- QE-051 领奖行合同 / QE-051 reward-row contract：客户端 quest_q%(quest)s.html 的 quest_summary 共 4 行
         （行 0/1/2 = 依次调查 %(trees)s 三棵焦树并采集 %(items)s，
         行 3 = 向 %(npc_dic)s（%(npc)s）报告）。旧定义把三棵树各自接一条 started -> reward 直跳，
         行 1/行 2 没有状态；改后三棵树只推进阶梯与发放本行收集物，接取与领奖都收在 %(npc)s 上
         （QE-052 owner 收敛）。The client journal declares 4 rows and rows 1-2 were missing;
         each tree now advances exactly one row and both the offer and the completion stay on %(npc)s. -->
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="3"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <dialog type="NPC_START" npc-id="%(npc)s" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT1"/>
"""

ROW_BLOCK = """    <!-- 行 %(index)s：%(tree_name)s %(tree)s；客户端页 select%(page_index)s(%(page_id)s) 的按钮是 HACTION_%(action)s(%(action_id)s)，
         采集物 %(item)s 由本行发放（quest_data.xml 只声明 quest_work_items，三棵树无 quest_drop 来源）。
         Row %(index)s: %(tree_name)s %(tree)s; the client page carries HACTION_%(action)s and the
         collected item %(item)s is granted on this row. -->
    <transition source="%(source)s" target="%(source)s">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="%(tree)s" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="%(page)s"/>
      </after-commit>
    </transition>
    <transition source="%(source)s" target="%(target)s">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="%(tree)s" action="%(action)s"/>
      </event>
      <actions>
        <give-item item-id="%(item)s" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="%(sync)s"/>
        <close-dialog/>
      </after-commit>
    </transition>
"""

TAIL = """    <!-- 行 3（领奖行）：向 %(npc_dic)s（%(npc)s）报告；客户端页 select5(2375) 的按钮 HACTION_SELECT_QUEST_REWARD(1009)
         与领奖窗口都落在 npc-complete 的 preview 路由上，不再显式声明 SELECT_QUEST_REWARD 自环，避免
         AMBIGUOUS_TRANSITION（批次 19 教训）。
         Row 3 (reward row): report to %(npc)s; the client select5 button and the reward window stay on the
         npc-complete preview route. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="%(npc)s" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5"/>
      </after-commit>
    </transition>

    <npc-complete npc-id="%(npc)s" source="reward" target="complete" fixed-reward-indices="0 1" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="2"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="3"/>
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>"""


def transitions(quest_id: str) -> str:
    items = ITEMS[quest_id]
    npc = REPORT_NPC[quest_id]
    npc_dic = ("STR_DIC_N_LDF5_Fortress_Village_Guard01_D" if quest_id == "23809"
        else "STR_DIC_N_LDF5_Fortress_Village_Guard01_L")
    text = HEADER % {
        "quest": quest_id,
        "npc": npc,
        "npc_dic": npc_dic,
        "trees": "STR_DIC_OBJ_LDF5_Fortress_FOBJ_B1_DeadTree_a/b/c",
        "items": "STR_DIC_I_quest_" + quest_id + "a/b/c",
    }
    source = "started"
    for index, (tree, page, action, item, sync, tree_name) in enumerate(
            zip(TREES, ROW_PAGES, ROW_ACTIONS, items, ROW_SYNCS, TREE_NAMES)):
        text += "\n" + ROW_BLOCK % {
            "index": index,
            "tree": tree,
            "tree_name": tree_name,
            "page": page,
            "page_index": index + 2,
            "page_id": 1352 if index == 0 else (1693 if index == 1 else 2034),
            "action": action,
            "action_id": 10000 + index,
            "item": item,
            "source": source,
            "target": f"stage{index + 1}" if index < 2 else "reward",
            "sync": sync,
        }
        source = f"stage{index + 1}"
    text += "\n" + TAIL % {"npc": npc, "npc_dic": npc_dic}
    return text


def rewrite(text: str, quest_id: str) -> str:
    out = re.sub(r"  <nodes>.*?</nodes>", lambda _: NODES, text, count=1, flags=re.S)
    return re.sub(r"  <transitions>.*?</transitions>",
        lambda _: transitions(quest_id), out, count=1, flags=re.S)


def main() -> int:
    check = "--check" in sys.argv
    problems = []
    for quest_id in sorted(ITEMS):
        path = QUESTS / f"{quest_id}.xml"
        text = path.read_text(encoding="utf-8")
        expected = rewrite(text, quest_id)
        if text == expected:
            print(f"BATCH20_OK quest={quest_id} already-applied")
            continue
        if check:
            problems.append(f"{quest_id}: needs batch20 tree-ladder owner-trim rewrite")
            print(f"BATCH20_PENDING quest={quest_id}")
            continue
        path.write_text(expected, encoding="utf-8")
        print(f"BATCH20_APPLIED quest={quest_id}")
    if problems:
        print("BATCH20_CHECK_FAILED " + "; ".join(problems))
        return 1
    print("BATCH20_VERIFY_OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
