#!/usr/bin/env python3
"""批次 34：Rentus Base 营救 Paios 族（30504 / 30554）的三行阶梯重建。

族判据（2026-09-22）：
- 客户端 quest_q30504/quest_q30554.html 的 quest_summary 三行、可见槽位 `%0/%3/%6`（标准「槽位 = 3 × 状态号」）：
  行 0「找到并救出 Paios(799536)」、行 1「和 Paios 对话」、行 2「和 Lition(205438) 对话」；quest_complete
  文案「把派奥斯的死讯告诉李迪安」与页文本（select_success 是 Lition 的「我一直在等你。派奥斯怎么样了？」、
  select_quest_reward1 是 Lition 的悲恸）一致指向“最后向 Lition 报告”。
- 客户端 NPC 表：Lition = 205438，Paios = 799536；柱子物件 701098 = `IDYun_Column_Q30504`，static spawn 在
  300280000 Rentus_base 与 300620000 Occupied_Rentus_Base（这两个实例世界同时刷 Lition/Paios）。
- retail `zz_retail_simple_quests.xml`：`start_type="TALK" start_ids="205438" end_npc_ids="799536"
  reset_world_id="300280000"`，单步 `ACTION action_ids="701098"`；领奖合同 `31 -> DEFAULT_SUCCESS(10002)`、
  `1009 -> REWARD`、奖励页 `5 -> SHOW_SELECT_QUEST_REWARD_WINDOW1`。
- 迁移前既无 Java handler 也无脚本（`git ls-tree 7e9f0316c^` 无匹配），定义由 retail/数据驱动迁移生成：它把
  三行塌陷成「started 传 SET_SUCCEED(205438) 直达 reward」——而 205438 的客户端页里根本没有 SET_SUCCEED 按钮
  ——reward 投影抄成 0，`var0` 只有 1 bit（max=1）连领奖行都放不下，柱子物件 701098 从未接进 IR，
  玩家无法完成行 0。
- 领奖 owner 取 205438(Lition)：客户端末行点名的 NPC（QE-052）与页文本、quest_complete 三方一致；retail 的
  `end_npc_ids=799536` 与客户端末行冲突，按客户端口径收敛并登记为 PENDING_CLIENT 复测点（报告 §三十八之四）。
- 实作细节：本任务同一个 NPC(205438) 既是接取 NPC 又是领奖 NPC，`<dialog type="NPC_REPORT">` 与 NPC_START 展开
  会撞 AMBIGUOUS_TRANSITION，因此 REWARD 态入口写成显式 `reward + QUEST_SELECT -> DEFAULT_SUCCESS`（与同链
  30503 同形）。

用法 / Usage:
    python3 .agents/summary/quest-10527-reward-row/apply_batch34_rentus_base_row_ladder.py --check
    python3 .agents/summary/quest-10527-reward-row/apply_batch34_rentus_base_row_ladder.py --apply
"""

from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

DOCUMENTS: dict[int, str] = {}

DOCUMENTS[30504] = '<?xml version="1.0" encoding="UTF-8"?>\n<quest-definition id="30504" version="1">\n  <metadata name="[Group] The Search For Paios" display-name-id="1135497" min-level="57" max-level="2147483647" category="QUEST" cannot-share="true">\n    <races>\n      <race id="ELYOS" />\n    </races>\n    <reward-groups>\n      <group>\n        <reward kind="GOLD" id="0" amount="299160" />\n        <reward kind="EXP" id="0" amount="6907092" />\n        <reward kind="SELECTABLE_ITEM" id="164000066" amount="15" />\n        <reward kind="SELECTABLE_ITEM" id="164000121" amount="15" />\n        <reward kind="SELECTABLE_ITEM" id="164000070" amount="15" />\n      </group>\n    </reward-groups>\n    <start-condition-groups>\n      <group>\n        <condition type="finished" quest-id="30503" />\n      </group>\n    </start-condition-groups>\n  </metadata>\n  <progress>\n    <bit-field name="var0" offset="0" width="3" min="0" max="7" persistence="PERSISTENT" scope="LOCAL"/>\n  </progress>\n  <nodes>\n    <node label="unaccepted" status="NONE">\n      <var name="var0" value="0"/>\n    </node>\n    <node label="started" status="START">\n      <var name="var0" value="0"/>\n    </node>\n    <node label="s1" status="START">\n      <var name="var0" value="1"/>\n    </node>\n    <node label="reward" status="REWARD">\n      <var name="var0" value="2"/>\n    </node>\n    <node label="complete" status="COMPLETE">\n      <var name="var0" value="0"/>\n    </node>\n  </nodes>\n  <transitions>\n    <!-- QE-051 三行阶梯（批次 34）：客户端 quest_summary 三行（可见槽位 %0/%3/%6）——行 0“找到并救出\n         Paios(799536)”（挪开柱子物件 701098）、行 1“和 Paios 对话”（客户端 select2 页，按钮 SET_SUCCEED）、\n         行 2“和 Lition(205438) 对话”（客户端 select_success 页，按钮 SELECT_QUEST_REWARD）。retail 只登记一个\n         ACTION step（action_ids=701098），领奖合同是 REWARD 态 31 到 DEFAULT_SUCCESS(10002)、1009 到奖励窗口；\n         旧定义把 0 到 1 到 2 的三行塌陷成「started 传 SET_SUCCEED(205438) 直达 reward」——而 205438 的客户端页\n         里根本没有 SET_SUCCEED 按钮——reward 投影抄成 0，而且 progress 只有 1 bit（max=1）连领奖行都放不下，\n         柱子物件 701098 也从未接进 IR，玩家无法完成行 0。\n         Row ladder repair (batch 34): the client owns three rows (pull the column off Paios, hear his dying\n         words, report to Lition), while the migrated definition collapsed them into one SET_SUCCEED jump and\n         never wired the column object at all. -->\n    <transition target="reward">\n      <event>\n        <enter-world/>\n      </event>\n      <conditions>\n        <status-is status="REWARD"/>\n        <variable-is field="var0" value="0"/>\n      </conditions>\n      <actions>\n        <set-variable field="var0" value="2"/>\n      </actions>\n      <after-commit>\n        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n      </after-commit>\n    </transition>\n    <transition target="reward">\n      <event>\n        <enter-world/>\n      </event>\n      <conditions>\n        <status-is status="REWARD"/>\n        <variable-is field="var0" value="1"/>\n      </conditions>\n      <actions>\n        <set-variable field="var0" value="2"/>\n      </actions>\n      <after-commit>\n        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n      </after-commit>\n    </transition>\n    <!-- 接取：Lition 205438 委托营救被压在柱子下的 Paios。 / Accept from Lition. -->\n    <dialog type="NPC_START" npc-id="205438" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>\n    <!-- 行 0：挪开柱子物件。 / Row 0: use the column object. -->\n    <transition source="started" target="s1">\n      <event>\n        <dialog type="TALK_TO_NPC" npc-id="701098" action="USE_OBJECT"/>\n      </event>\n      <after-commit>\n        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n      </after-commit>\n    </transition>\n    <!-- 行 1：Paios 的遗言（select2）——按钮 SET_SUCCEED 收入领奖行。 / Row 1: Paios\' dying words. -->\n    <transition source="s1" target="s1">\n      <event>\n        <dialog type="TALK_TO_NPC" npc-id="799536" action="QUEST_SELECT"/>\n      </event>\n      <after-commit>\n        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>\n      </after-commit>\n    </transition>\n    <transition source="s1" target="reward">\n      <event>\n        <dialog type="TALK_TO_NPC" npc-id="799536" action="SET_SUCCEED"/>\n      </event>\n      <after-commit>\n        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n        <close-dialog/>\n      </after-commit>\n    </transition>\n    <!-- 行 2：向 Lition 报告 Paios 的死讯并领奖（合同 31 到 DEFAULT_SUCCESS，1009 到奖励窗口由 npc-complete\n         的 preview 展开；不得再显式声明 SELECT_QUEST_REWARD 自环，否则 AMBIGUOUS_TRANSITION）。\n         Row 2: report back to Lition and claim. -->\n    <transition source="reward" target="reward">\n      <event>\n        <dialog type="TALK_TO_NPC" npc-id="205438" action="QUEST_SELECT"/>\n      </event>\n      <after-commit>\n        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>\n      </after-commit>\n    </transition>\n    <npc-complete npc-id="205438" source="reward" target="complete" fixed-reward-indices="0 1" complete-reward-index="0" finish="SELECTION_DIALOG">\n      <choice action="SELECTED_QUEST_REWARD1" reward-index="2"/>\n      <choice action="SELECTED_QUEST_REWARD2" reward-index="3"/>\n      <choice action="SELECTED_QUEST_REWARD3" reward-index="4"/>\n      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>\n    </npc-complete>\n  </transitions>\n</quest-definition>\n'

DOCUMENTS[30554] = '<?xml version="1.0" encoding="UTF-8"?>\n<quest-definition id="30554" version="1">\n  <metadata name="[Group] Saving Private Paios" display-name-id="1135511" min-level="57" max-level="2147483647" category="QUEST" cannot-share="true">\n    <races>\n      <race id="ASMODIANS" />\n    </races>\n    <reward-groups>\n      <group>\n        <reward kind="GOLD" id="0" amount="897480" />\n        <reward kind="EXP" id="0" amount="6907092" />\n        <reward kind="SELECTABLE_ITEM" id="164000066" amount="15" />\n        <reward kind="SELECTABLE_ITEM" id="164000121" amount="15" />\n        <reward kind="SELECTABLE_ITEM" id="164000070" amount="15" />\n      </group>\n    </reward-groups>\n    <start-condition-groups>\n      <group>\n        <condition type="finished" quest-id="30553" />\n      </group>\n    </start-condition-groups>\n  </metadata>\n  <progress>\n    <bit-field name="var0" offset="0" width="3" min="0" max="7" persistence="PERSISTENT" scope="LOCAL"/>\n  </progress>\n  <nodes>\n    <node label="unaccepted" status="NONE">\n      <var name="var0" value="0"/>\n    </node>\n    <node label="started" status="START">\n      <var name="var0" value="0"/>\n    </node>\n    <node label="s1" status="START">\n      <var name="var0" value="1"/>\n    </node>\n    <node label="reward" status="REWARD">\n      <var name="var0" value="2"/>\n    </node>\n    <node label="complete" status="COMPLETE">\n      <var name="var0" value="0"/>\n    </node>\n  </nodes>\n  <transitions>\n    <!-- QE-051 三行阶梯（批次 34）：客户端 quest_summary 三行（可见槽位 %0/%3/%6）——行 0“找到并救出\n         Paios(799536)”（挪开柱子物件 701098）、行 1“和 Paios 对话”（客户端 select2 页，按钮 SET_SUCCEED）、\n         行 2“和 Lition(205438) 对话”（客户端 select_success 页，按钮 SELECT_QUEST_REWARD）。retail 只登记一个\n         ACTION step（action_ids=701098），领奖合同是 REWARD 态 31 到 DEFAULT_SUCCESS(10002)、1009 到奖励窗口；\n         旧定义把 0 到 1 到 2 的三行塌陷成「started 传 SET_SUCCEED(205438) 直达 reward」——而 205438 的客户端页\n         里根本没有 SET_SUCCEED 按钮——reward 投影抄成 0，而且 progress 只有 1 bit（max=1）连领奖行都放不下，\n         柱子物件 701098 也从未接进 IR，玩家无法完成行 0。\n         Row ladder repair (batch 34): the client owns three rows (pull the column off Paios, hear his dying\n         words, report to Lition), while the migrated definition collapsed them into one SET_SUCCEED jump and\n         never wired the column object at all. -->\n    <transition target="reward">\n      <event>\n        <enter-world/>\n      </event>\n      <conditions>\n        <status-is status="REWARD"/>\n        <variable-is field="var0" value="0"/>\n      </conditions>\n      <actions>\n        <set-variable field="var0" value="2"/>\n      </actions>\n      <after-commit>\n        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n      </after-commit>\n    </transition>\n    <transition target="reward">\n      <event>\n        <enter-world/>\n      </event>\n      <conditions>\n        <status-is status="REWARD"/>\n        <variable-is field="var0" value="1"/>\n      </conditions>\n      <actions>\n        <set-variable field="var0" value="2"/>\n      </actions>\n      <after-commit>\n        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n      </after-commit>\n    </transition>\n    <!-- 接取：Lition 205438 委托营救被压在柱子下的 Paios。 / Accept from Lition. -->\n    <dialog type="NPC_START" npc-id="205438" source="unaccepted" target="started" selection-sources="unaccepted started" start-page="SELECT_NONE"/>\n    <!-- 行 0：挪开柱子物件。 / Row 0: use the column object. -->\n    <transition source="started" target="s1">\n      <event>\n        <dialog type="TALK_TO_NPC" npc-id="701098" action="USE_OBJECT"/>\n      </event>\n      <after-commit>\n        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n      </after-commit>\n    </transition>\n    <!-- 行 1：Paios 的遗言（select2）——按钮 SET_SUCCEED 收入领奖行。 / Row 1: Paios\' dying words. -->\n    <transition source="s1" target="s1">\n      <event>\n        <dialog type="TALK_TO_NPC" npc-id="799536" action="QUEST_SELECT"/>\n      </event>\n      <after-commit>\n        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>\n      </after-commit>\n    </transition>\n    <transition source="s1" target="reward">\n      <event>\n        <dialog type="TALK_TO_NPC" npc-id="799536" action="SET_SUCCEED"/>\n      </event>\n      <after-commit>\n        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n        <close-dialog/>\n      </after-commit>\n    </transition>\n    <!-- 行 2：向 Lition 报告 Paios 的死讯并领奖（合同 31 到 DEFAULT_SUCCESS，1009 到奖励窗口由 npc-complete\n         的 preview 展开；不得再显式声明 SELECT_QUEST_REWARD 自环，否则 AMBIGUOUS_TRANSITION）。\n         Row 2: report back to Lition and claim. -->\n    <transition source="reward" target="reward">\n      <event>\n        <dialog type="TALK_TO_NPC" npc-id="205438" action="QUEST_SELECT"/>\n      </event>\n      <after-commit>\n        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>\n      </after-commit>\n    </transition>\n    <npc-complete npc-id="205438" source="reward" target="complete" fixed-reward-indices="0 1" complete-reward-index="0" finish="SELECTION_DIALOG">\n      <choice action="SELECTED_QUEST_REWARD1" reward-index="2"/>\n      <choice action="SELECTED_QUEST_REWARD2" reward-index="3"/>\n      <choice action="SELECTED_QUEST_REWARD3" reward-index="4"/>\n      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>\n    </npc-complete>\n  </transitions>\n</quest-definition>\n'



def main(argv: list[str]) -> int:
    apply = "--apply" in argv
    check = "--check" in argv or not apply
    status = 0
    for quest_id, document in DOCUMENTS.items():
        path = QUESTS / f"{quest_id}.xml"
        current = path.read_text(encoding="utf-8")
        if check:
            if current == document:
                print(f"BATCH34_OK {quest_id} already-applied")
            else:
                print(f"BATCH34_PENDING {quest_id} differs from target document")
                status = 1
            continue
        if current == document:
            print(f"BATCH34_OK {quest_id} already-applied")
            continue
        path.write_text(document, encoding="utf-8")
        print(f"BATCH34_APPLIED {quest_id}")
    return status


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
