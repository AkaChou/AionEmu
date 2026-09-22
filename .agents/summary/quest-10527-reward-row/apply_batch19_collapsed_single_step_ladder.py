#!/usr/bin/env python3
"""批次 19：单步塌陷对（Elyos 存根）补回客户端 quest_summary 行状态阶梯。

族级判据（与批次 18 同源，但落点不同）：

* 同形镜像对 `q` / `q±10000` 的客户端 quest_summary **行数相同**、末行 NPC 都能在任务内对上；
* 镜像那一侧已经有完整的 `0..N-1` START/REWARD 状态阶梯（audit: ROW_ALIGNED / ROW_STATE_ALIGNED）；
* 落后那一侧被迁移塌陷成“单步交接直接置 REWARD”，只剩行 0 一个状态
  （audit: ROW_BEHIND / MISSING_TAIL_ROWS / ROW_WITHOUT_STATE）。

这种情况**不能只改 reward 投影**（改了会指向一个客户端不存在、服务端也没有状态的行），
必须按客户端对话页 `<Act href="HACTION_*">` 给出的动作阶梯把中间行补回来。

本批成员（Elyos 存根 / Asmodian 镜像已完整）：

* 15000（镜像 25000，4 行）：行 0 交回背囊 → 行 1 和米利亚德对话（接过修理工具）→
  行 2 在人工奥德生成器附近使用修理工具 → 行 3 向米利亚德报告（领奖行）。
  客户端页动作：`HACTION_CHECK_USER_HAS_QUEST_ITEM` → `HACTION_SELECT2` → `HACTION_SETPRO2`
  → 使用工作物品 182215662 → `HACTION_SELECT_QUEST_REWARD`。
* 15670（镜像 25670，4 行）：行 0 与赫梅洛斯（806093）对话 → 行 1 调查 4 处痕迹后交出证据 →
  行 2 调查第五处痕迹（731793）→ 行 3 向伊利西亚（806114）报告（领奖行）。
  客户端页动作：`HACTION_SETPRO1` → `HACTION_CHECK_USER_HAS_QUEST_ITEM` → `HACTION_SET_SUCCEED`
  → `HACTION_SELECT_QUEST_REWARD`；镜像 25670 的结构逐条对应（Blizzne 806105 / FOBJ 731794 /
  Reinhard 806116）。

两个任务都补无 source 的 `REWARD && var0==0 -> 领奖行` enter-world 自愈边：迁移前的塌陷定义把
“交完物品”的玩家直接写成 `REWARD + var0=0`，改完阶梯后这个存档匹配不到 reward 节点的投影，
必须自愈到领奖行，否则连奖励对话都点不出来（QE-051 旧存档自愈边同形）。

用法：
  python3 apply_batch19_collapsed_single_step_ladder.py --check   # 只校验（改动前应 FAIL，改完后 OK）
  python3 apply_batch19_collapsed_single_step_ladder.py           # 应用改动，幂等
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

MARKER = "QE-051 领奖行（批次 19，单步塌陷对）"

NODES_15000 = """  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="s1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="s2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="3"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>"""

TRANSITIONS_15000 = """  <transitions>
    <!-- %(marker)s / Elyos 存根 15000）：客户端 quest_q15000.html 的 quest_summary 共 4 行
         （行 0 交回被抢走的背囊 → 行 1 和米利亚德对话 → 行 2 在人工奥德生成器附近使用修理工具 →
         行 3 向米利亚德报告），与镜像 25000（4 行、reward=3）同形；迁移前的 Elyos handler 只实现了
         “交背囊直接置 REWARD”，把行 1/行 2 与领奖行一起丢掉，客户端页 select1 → select2 → select2_1
         依次给出 HACTION_CHECK_USER_HAS_QUEST_ITEM / HACTION_SELECT2 / HACTION_SETPRO2
         （“从米利亚德那里接过修理工具”），补回的阶梯即照这四页动作落地。
         Collapsed single-step shard (Elyos stub): the client journal declares 4 rows and the mirror 25000
         already projects row 3; the client dialog pages give the exact action ladder. -->
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
    <dialog type="NPC_START" npc-id="804874" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <!-- 行 0：从振翼布奇手里抢回背囊后交给米利亚德；Row 0: hand the recovered bag back to Milliard. -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804874" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804874" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="182215661" count="1"/>
      </conditions>
      <actions>
        <remove-item item-id="182215661" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>
      </after-commit>
    </transition>
    <transition source="started" target="started" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804874" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>
      </after-commit>
    </transition>
    <!-- 行 1：check_user_item_ok 页的 SELECT2(1352) 打开 select2 页，select2 页的 SELECT2_1(1353) 打开
         select2_1 页，最后 SETPRO2(10001) 接过修理工具（客户端页按钮链，由 QuestClientContractGateTest 校验）。
         Row 1 button chain: CHECK_USER_ITEM_OK -> SELECT2 -> select2 -> SELECT2_1 -> select2_1 -> SETPRO2 -> row 2. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804874" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804874" action="SELECT2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804874" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s2" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804874" action="SETPRO2"/>
      </event>
      <actions>
        <give-item item-id="182215662" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 2：在人工奥德生成器附近使用修理工具（工作物品 182215662）后进入领奖行 3。
         Row 2: use the repair tool near the aether generator, which advances into reward row 3. -->
    <transition source="s2" target="reward">
      <event>
        <use-item item-id="182215662"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
    <!-- 行 3（领奖行）：向米利亚德报告；领奖窗口由 npc-complete 的 preview 路由（USE_OBJECT / SELECT_QUEST_REWARD）承接。
         Row 3 (reward row): report to Milliard; the reward window stays on the npc-complete preview route. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804874" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="804874" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>

    <npc-complete npc-id="804874" source="reward" target="complete" fixed-reward-indices="0 1 2 3" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>"""

NODES_15670 = """  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="s1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="s2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="3"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>"""

TRANSITIONS_15670 = """  <transitions>
    <!-- %(marker)s / Elyos 存根 15670）：客户端 quest_q15670.html 的 quest_summary 共 4 行
         （行 0 与赫梅洛斯 806093 对话 → 行 1 调查 4 处痕迹后交出证据 → 行 2 调查第五处痕迹 731793 →
         行 3 向伊利西亚 806114 报告），与镜像 25670（4 行、reward=3）同形：镜像把行 0/行 1 放在
         布里兹内 806105、行 2 放在 FOBJ 731794、行 3 放在莱茵哈特 806116，本任务按其结构换回天族 id。
         客户端页动作依次是 HACTION_SETPRO1 / HACTION_CHECK_USER_HAS_QUEST_ITEM / HACTION_SET_SUCCEED /
         HACTION_SELECT_QUEST_REWARD；迁移前的塌陷定义把 4 件证据收在领奖 NPC 伊利西亚身上并直接置 REWARD。
         Collapsed single-step shard (Elyos stub): mirror 25670 already has the full 4-row ladder; the client
         pages give the same action sequence with the Elyos NPC/item/object ids. -->
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
    <dialog type="NPC_START" npc-id="806114" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>
    <!-- 行 0：与五色湿地侦察哨所的赫梅洛斯（806093）对话；Row 0: talk to Hemellos (806093). -->
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="806093" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="806093" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <!-- 行 1：调查 4 处痕迹（703434-703437）取证后交给赫梅洛斯。
         Row 1: investigate the four traces (703434-703437) and hand the evidence to Hemellos. -->
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="806093" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s2" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="806093" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="182216189" count="1"/>
        <has-item item-id="182216190" count="1"/>
        <has-item item-id="182216191" count="1"/>
        <has-item item-id="182216192" count="1"/>
      </conditions>
      <actions>
        <remove-item item-id="182216189" count="1"/>
        <remove-item item-id="182216190" count="1"/>
        <remove-item item-id="182216191" count="1"/>
        <remove-item item-id="182216192" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_OK"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="806093" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="806093" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="703434"/>
      </event>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="703435"/>
      </event>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="703436"/>
      </event>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="703437"/>
      </event>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <can-act template-id="703434" action-type="ACTION_ITEM_USE"/>
      </event>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <can-act template-id="703435" action-type="ACTION_ITEM_USE"/>
      </event>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <can-act template-id="703436" action-type="ACTION_ITEM_USE"/>
      </event>
    </transition>
    <transition source="s1" target="s1">
      <event>
        <can-act template-id="703437" action-type="ACTION_ITEM_USE"/>
      </event>
    </transition>
    <!-- 行 2：调查第五处痕迹（731793）后进入领奖行 3。
         Row 2: investigate the fifth trace (731793), which advances into reward row 3. -->
    <transition source="s2" target="s2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="731793" action="USE_OBJECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="s2" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="731793" action="SET_SUCCEED"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s2" target="s2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="806093" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="s2" target="s2">
      <event>
        <can-act template-id="731793" action-type="ACTION_ITEM_USE"/>
      </event>
    </transition>
    <!-- 行 3（领奖行）：向伊利西亚军团长（806114）报告；领奖窗口由 npc-complete 的 preview 路由承接。
         Row 3 (reward row): report to Ilisia (806114); the reward window stays on the npc-complete preview route. -->
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="806114" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="806114" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>

    <npc-complete npc-id="806114" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>"""

# 15670 的 4 处痕迹采集属于行 1（s1, var0=1），drops 的 collecting-step 必须同步（校验器
# QuestInteractionObjectValidator#validateCatalogDrops 用 source 节点 var0 比对 collecting-step）。
DROPS_15670 = {
    '<drop npc-id="703434" item-id="182216189" chance="100" each-member="true" collecting-step="0"/>':
        '<drop npc-id="703434" item-id="182216189" chance="100" each-member="true" collecting-step="1"/>',
    '<drop npc-id="703435" item-id="182216190" chance="100" each-member="true" collecting-step="0"/>':
        '<drop npc-id="703435" item-id="182216190" chance="100" each-member="true" collecting-step="1"/>',
    '<drop npc-id="703436" item-id="182216191" chance="100" each-member="true" collecting-step="0"/>':
        '<drop npc-id="703436" item-id="182216191" chance="100" each-member="true" collecting-step="1"/>',
    '<drop npc-id="703437" item-id="182216192" chance="100" each-member="true" collecting-step="0"/>':
        '<drop npc-id="703437" item-id="182216192" chance="100" each-member="true" collecting-step="1"/>',
}

TASKS = {
    "15000": (NODES_15000, TRANSITIONS_15000 % {"marker": MARKER}, {}),
    "15670": (NODES_15670, TRANSITIONS_15670 % {"marker": MARKER}, DROPS_15670),
}


def rewrite(text: str, nodes: str, transitions: str, drops: dict[str, str]) -> str:
    out = re.sub(r"  <nodes>.*?</nodes>", lambda _: nodes, text, count=1, flags=re.S)
    out = re.sub(r"  <transitions>.*?</transitions>", lambda _: transitions, out, count=1, flags=re.S)
    for old, new in drops.items():
        out = out.replace(old, new)
    return out


def main() -> int:
    check = "--check" in sys.argv
    problems = []
    for quest_id, (nodes, transitions, drops) in sorted(TASKS.items()):
        path = QUESTS / f"{quest_id}.xml"
        text = path.read_text(encoding="utf-8")
        expected = rewrite(text, nodes, transitions, drops)
        if text == expected:
            print(f"BATCH19_OK quest={quest_id} already-applied")
            continue
        if check:
            problems.append(f"{quest_id}: needs batch19 ladder rewrite")
            print(f"BATCH19_PENDING quest={quest_id}")
            continue
        path.write_text(expected, encoding="utf-8")
        print(f"BATCH19_APPLIED quest={quest_id}")
    if problems:
        print("BATCH19_CHECK_FAILED " + "; ".join(problems))
        return 1
    print("BATCH19_VERIFY_OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
