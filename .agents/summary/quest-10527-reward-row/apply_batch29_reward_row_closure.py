#!/usr/bin/env python3
"""批次 29：剩余 MISSING_LAST_ROW 逐族收口 —— 有 legacy 依据且投影不一致的 6 个真缺陷。

族级判据（QE-054，沿用批次 27/28）：reward 投影必须等于 legacy 进入 REWARD 时真正落盘的 step。
`changeQuestStep(env, step, nextStep, reward=true)`（含 defaultCloseDialog / checkQuestItems /
checkItemExistence / useQuestItem / useQuestObject / defaultFollowEndEvent / defaultOnKillEvent 的
reward 分支）只置 REWARD、**不写 nextStep**，所以落盘值恒等于调用时的 `step`（= 进入前的 var0）；
显式 `qs.setQuestVar(N)` / `changeQuestStep(..., false)` / `setQuestVarById(0, N)` 之后的
`setStatus(REWARD)` 才把 var0 写成 N。逐任务取证见 batch29-triage.tsv 与 batch29-evidence.tsv。

本批修复（6 个，客户端末行槽位都是 3 × 行号的标准形态）：

1. 1876 / 2876（Taranis / Votan Emergency Orders，镜像对）
   - 客户端 3 行：0 = 和 Sakmis/Lisya 对话、1 = 和 Ascalon/Semotor 对话、2 = 向 Taranis/Votan 报告（领奖行）；
   - legacy `changeQuestStep(env, 1, 2, false)` **写 2** 之后 `qs.setStatus(REWARD)` → 落盘 2；
   - 旧 XML reward 投影写成 1（s1 -> reward 由 target 投影生效）→ 领奖时任务书停在行 1；
   - 本批 reward 投影 1 -> 2，补 ENTER_WORLD 自愈边 REWARD && var0=1 -> 2。

2. 14123（The Shadow Of Vengeance）
   - 客户端 3 行（槽位 9/24 表示行 1/2 由计数驱动）：0 = 和 Dionera 对话、1 = 消灭 hippolyta、
     2 = 向 Dionera 报告（领奖行）；
   - legacy `defaultOnKillEvent(env, 206360, 0, 1)` 把 var0 写成 1，`SELECT_REWARD` 只 setStatus
     → 落盘 1；旧 XML 的 4 条 report -> reward 交接又显式 `<set-variable var0=0>`，领奖态被打回行 0；
   - 本批 reward 投影 0 -> 1、4 处 set-variable 0 -> 1，补自愈边 REWARD && var0=0 -> 1。

3. 2600（Humongous Malek）
   - 客户端 3 行：0 = 和 Shugo 对话、1 = 杀守护者、捡原石交给 Shugo、2 = 重新和 Shugo 对话（领奖行）；
   - legacy 只在 `var0 == 1` 时响应 `SELECT_REWARD` 并 setStatus(REWARD) → 落盘 1；
   - 旧 XML reward 投影写成 0 → 领奖时任务书停在行 0；本批 0 -> 1 + 自愈边。

4. 11010（Angel To The Wounded）
   - 客户端 4 行：0 = Naiting、1 = Lionel、2 = 调查 Supply_Box、3 = 和 Naiting 对话（领奖行）；
   - legacy `defaultCloseDialog(env, 2, 3)` 写 3，随后 799071 的 SELECT_REWARD 只 setStatus → 落盘 3；
   - 旧 XML 只有 started(0)/stage1(1)/stage2(2) 且 reward 投影写成 0 → 领奖时任务书停在行 0；
   - 本批 reward 投影 0 -> 3（stage2 -> reward 由 target 投影生效）+ 自愈边 0 -> 3。

5. 1466（Respect For Deltras）
   - 客户端 2 行：0 = 燃放奥德爆竹、1 = 向 Valerius 报告（领奖行）；
   - legacy 有两条进入 REWARD 的路径：道具 `setStatus(REWARD)`（var0 停在 0）与 203903 的
     `qs.setQuestVar(2)` + `setStatus(REWARD)`（var0=2 越出客户端 2 行的 0..1 范围）；
   - 旧 XML reward 节点没有 var0（默认 0），交接又显式写 `var0=2` → 领奖窗口打开时任务书空白；
   - 本批统一落盘 1：reward 补 `var0=1`、203903 交接 2 -> 1、道具交接补 `<set-variable var0=1>`，
     补两条自愈边 REWARD && var0=0 -> 1 与 REWARD && var0=2 -> 1。

Batch 29: close the remaining MISSING_LAST_ROW family that has a legacy REWARD-entry write
whose persisted step differs from the XML reward projection (1876/2876/14123/2600/11010/1466).
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

QUEST_DIR = (Path(__file__).resolve().parents[3]
             / "src/main/resources/aion/data/static_data/quest_definition/quests")

# --- 1876 / 2876：reward 投影 1 -> 2，补 ENTER_WORLD 自愈边 ------------------------------------
OLD_1876_REWARD = '''    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
'''
NEW_1876_REWARD = '''    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>
'''

RECOVERY_1_TO_2 = '''
    <!-- QE-054 自愈边：旧投影 1 的存档进入世界时纠正为 2 并下发状态包。 -->
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
    </transition>
  '''

# --- 2600：reward 投影 0 -> 1 + 自愈边 --------------------------------------------------------
OLD_2600_REWARD = '''    <node label="reward" status="REWARD">
      <var name="var0" value="0"/>
    </node>
'''
NEW_2600_REWARD = '''    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
'''

RECOVERY_0_TO_1 = '''
    <!-- QE-054 自愈边：旧投影 0 的存档进入世界时纠正为 1 并下发状态包。 -->
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
  '''

# --- 14123：reward 投影 0 -> 1，4 处交接 set-variable 0 -> 1，自愈边 0 -> 1 -------------------
OLD_14123_HANDOVER = '''      <actions>
        <set-variable field="var0" value="0"/>
      </actions>
'''
NEW_14123_HANDOVER = '''      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
'''

# --- 11010：reward 投影 0 -> 3 + 自愈边 ------------------------------------------------------
OLD_11010_REWARD = '''    <node label="reward" status="REWARD">
      <var name="var0" value="0"/>
    </node>
'''
NEW_11010_REWARD = '''    <node label="reward" status="REWARD">
      <var name="var0" value="3"/>
    </node>
'''

RECOVERY_0_TO_3 = '''
    <!-- QE-054 自愈边：旧投影 0 的存档进入世界时纠正为 3（末行 / 领奖行）并下发状态包。 -->
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
  '''

# --- 1466：reward 补 var0=1、交接 2 -> 1、道具交接补 set-variable 1、两条自愈边 ----------------
OLD_1466_REWARD = '''    <node label="reward" status="REWARD"/>
'''
NEW_1466_REWARD = '''    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
'''

OLD_1466_ITEM_ACTIONS = '''      <conditions>
        <zone-is zone="EXECUTION_GROUND_OF_DELTRAS_220020000"/>
      </conditions>
      <actions>
        <remove-item item-id="182201385" count="1"/>
      </actions>
'''
NEW_1466_ITEM_ACTIONS = '''      <conditions>
        <zone-is zone="EXECUTION_GROUND_OF_DELTRAS_220020000"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
        <remove-item item-id="182201385" count="1"/>
      </actions>
'''

OLD_1466_HANDOVER = '''      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
'''
NEW_1466_HANDOVER = '''      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
'''

RECOVERY_1466 = '''
    <!-- QE-054 自愈边：旧存档（道具路径停在 0、对话路径写成越界的 2）进入世界时统一纠正为 1。 -->
    <transition target="reward">
      <event>
        <enter-world/>
      </event>
      <conditions>
        <status-is status="REWARD"/>
        <variable-is field="var0" value="0"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
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
        <variable-is field="var0" value="2"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
      </after-commit>
    </transition>
  '''

CLOSE_MARKER = '\n</quest-definition>\n'


def read(quest_id: int) -> str:
    return (QUEST_DIR / f"{quest_id}.xml").read_text(encoding="utf-8")


def write(quest_id: int, text: str) -> None:
    (QUEST_DIR / f"{quest_id}.xml").write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 occurrence, found {count}")
    return text.replace(old, new, 1)


def replace_n(text: str, old: str, new: str, expected: int, label: str) -> str:
    count = text.count(old)
    if count != expected:
        raise SystemExit(f"{label}: expected {expected} occurrences, found {count}")
    return text.replace(old, new)


TRANSITIONS_CLOSE = "</transitions>"


def append_before_close(text: str, block: str, label: str) -> str:
    """把自愈边插到 </transitions> 之前（transition 必须位于 transitions 元素内）。"""
    marker_line = block.strip().splitlines()[0]
    if marker_line in text:
        raise SystemExit(f"{label}: recovery edge already present")
    index = text.rfind(TRANSITIONS_CLOSE)
    if index < 0:
        raise SystemExit(f"{label}: missing {TRANSITIONS_CLOSE}")
    head = text[:index].rstrip(" \t")
    return head + block + text[index:]


def fix_1876() -> None:
    text = read(1876)
    text = replace_once(text, OLD_1876_REWARD, NEW_1876_REWARD, "1876 reward projection")
    text = append_before_close(text, RECOVERY_1_TO_2, "1876 recovery")
    write(1876, text)


def fix_2876() -> None:
    text = read(2876)
    text = replace_once(text, OLD_1876_REWARD, NEW_1876_REWARD, "2876 reward projection")
    text = append_before_close(text, RECOVERY_1_TO_2, "2876 recovery")
    write(2876, text)


def fix_2600() -> None:
    text = read(2600)
    text = replace_once(text, OLD_2600_REWARD, NEW_2600_REWARD, "2600 reward projection")
    text = append_before_close(text, RECOVERY_0_TO_1, "2600 recovery")
    write(2600, text)


def fix_14123() -> None:
    text = read(14123)
    text = replace_once(text, OLD_2600_REWARD, NEW_2600_REWARD, "14123 reward projection")
    text = replace_n(text, OLD_14123_HANDOVER, NEW_14123_HANDOVER, 4, "14123 handover")
    text = append_before_close(text, RECOVERY_0_TO_1, "14123 recovery")
    write(14123, text)


def fix_11010() -> None:
    text = read(11010)
    text = replace_once(text, OLD_11010_REWARD, NEW_11010_REWARD, "11010 reward projection")
    text = append_before_close(text, RECOVERY_0_TO_3, "11010 recovery")
    write(11010, text)


def fix_1466() -> None:
    text = read(1466)
    text = replace_once(text, OLD_1466_REWARD, NEW_1466_REWARD, "1466 reward projection")
    text = replace_once(text, OLD_1466_ITEM_ACTIONS, NEW_1466_ITEM_ACTIONS, "1466 item handover")
    text = replace_once(text, OLD_1466_HANDOVER, NEW_1466_HANDOVER, "1466 npc handover")
    text = append_before_close(text, RECOVERY_1466, "1466 recovery")
    write(1466, text)


APPLIERS = {
    "1876": fix_1876,
    "2876": fix_2876,
    "2600": fix_2600,
    "14123": fix_14123,
    "11010": fix_11010,
    "1466": fix_1466,
}


def already_applied(quest_id: int) -> bool:
    text = read(quest_id)
    if quest_id in (1876, 2876):
        return '<node label="reward" status="REWARD">\n      <var name="var0" value="2"/>\n    </node>' in text and RECOVERY_1_TO_2 in text
    if quest_id in (14123, 2600):
        return '<node label="reward" status="REWARD">\n      <var name="var0" value="1"/>\n    </node>' in text and RECOVERY_0_TO_1 in text
    if quest_id == 11010:
        return '<node label="reward" status="REWARD">\n      <var name="var0" value="3"/>\n    </node>' in text and RECOVERY_0_TO_3 in text
    if quest_id == 1466:
        return '<node label="reward" status="REWARD">\n      <var name="var0" value="1"/>\n    </node>' in text and RECOVERY_1466 in text
    return False


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true",
                        help="只校验当前状态（幂等检查），不做修改")
    args = parser.parse_args()

    if args.check:
        pending = [q for q in APPLIERS if not already_applied(int(q))]
        if pending:
            print(f"BATCH29_CHECK_PENDING {sorted(pending)}")
            return 1
        print("BATCH29_OK all 6 quests already-applied")
        return 0

    pending = [q for q in APPLIERS if not already_applied(int(q))]
    if not pending:
        print("BATCH29_OK all 6 quests already-applied")
        return 0
    for quest_id in sorted(pending):
        APPLIERS[quest_id]()
        print(f"BATCH29_APPLIED {quest_id}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
