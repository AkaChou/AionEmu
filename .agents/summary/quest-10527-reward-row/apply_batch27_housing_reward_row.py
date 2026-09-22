#!/usr/bin/env python3
"""批次 27：住宅回收箱任务 18805/28805 的领奖行投影与旧存档自愈。

族级判据（QE-051 + 本批新证据）：
- 客户端 quest_summary 都是 3 行，末行（行 2）是领奖行
  （18805 = STR_DIC_N_Shugo_housing_rec，28805 = STR_DIC_N_Shugo_housing_drec）；
- legacy handler `_18805Going_Thrifting` / `_28805SomethingOld_SomethingNew`：
  回收箱（730522/730525）的 STEP_TO_2 走 `defaultCloseDialog(env, 1, 2)`，把 step 推到 2；
  回到旧货商主人（830660/830661、830662/830663 共用分支）的 SELECT_REWARD 才
  `changeQuestStep(env, 2, 2, true)` —— 旧引擎只置 REWARD、step 停在 2；
- 客户端行 2 只在领奖态可见，所以 reward 投影必须是 2（当前被写成 1，并且
  s1 -> reward 的 transition 又把 var0 显式写回 1，导致领奖阶段任务书停在“阅读回收箱说明”那行）。

落点（两侧同形）：
1. reward 节点投影 var0 1 -> 2；
2. 删除 s1 -> reward 交接 transition 里的 set-variable var0=1（改由 target 投影生效）；
3. 新增无 source 的 ENTER_WORLD 恢复边：REWARD && var0=1 -> reward（仅
   LEVEL_AND_VISIBILITY_REFRESH），纠正旧投影留下的 1 号存档；正规态 var0=2 不再被重放。

参考模板：quests/18809.xml（同族 QE-051 领奖行合同：stage1 -> reward(2) + REWARD/var0=0 自愈边）。

Batch 27: reward-row projection and stale-save recovery for the housing recycle quests 18805/28805.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

QUEST_DIR = (Path(__file__).resolve().parents[3]
             / "src/main/resources/aion/data/static_data/quest_definition/quests")

CONTRACTS = {
    18805: {
        "reward_npc": 830520,
        "starter": 830070,
        "box": 730522,
        "title": "Going Thrifting",
        "npc_key": "STR_DIC_N_Shugo_housing_rec",
        "legacy": "_18805Going_Thrifting",
    },
    28805: {
        "reward_npc": 830521,
        "starter": 830154,
        "box": 730525,
        "title": "Something Old, Something New",
        "npc_key": "STR_DIC_N_Shugo_housing_drec",
        "legacy": "_28805SomethingOld_SomethingNew",
    },
}

REWARD_NODE_STALE = ('    <node label="reward" status="REWARD">\n'
                     '      <var name="var0" value="1"/>\n'
                     '    </node>\n')
REWARD_NODE_FIXED = ('    <node label="reward" status="REWARD">\n'
                     '      <var name="var0" value="2"/>\n'
                     '    </node>\n')


def recovery_edge(quest_id: int) -> str:
    c = CONTRACTS[quest_id]
    return (
        f'    <!-- QE-051 领奖行合同：客户端 quest_q{quest_id}.html 的 quest_summary 共 3 行，末行（行 2）是领奖行\n'
        f'         （{c["npc_key"]}，任务内 NPC {c["reward_npc"]}）；legacy `{c["legacy"]}` 在回收箱 730{str(c["box"])[3:]} 的\n'
        '         STEP_TO_2 之后把 step 推到 2，回到旧货商主人的 SELECT_REWARD 才置 REWARD（step 停 2）。\n'
        '         旧投影停在 1，进入世界时纠正为 2 并下发状态包。\n'
        f'         Reward row contract (QE-051): the last of the 3 journal rows is the reward row; saves persisted\n'
        '         at var0=1 are repaired to 2 on enter-world, matching the legacy step chain. -->\n'
        '    <transition target="reward">\n'
        '      <event>\n'
        '        <enter-world/>\n'
        '      </event>\n'
        '      <conditions>\n'
        '        <status-is status="REWARD"/>\n'
        '        <variable-is field="var0" value="1"/>\n'
        '      </conditions>\n'
        '      <actions>\n'
        '        <set-variable field="var0" value="2"/>\n'
        '      </actions>\n'
        '      <after-commit>\n'
        '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        '      </after-commit>\n'
        '    </transition>\n')


def rewrite(quest_id: int, text: str) -> str:
    if REWARD_NODE_FIXED in text and '        <variable-is field="var0" value="1"/>\n' in text:
        return text
    if REWARD_NODE_STALE not in text:
        raise SystemExit(f"BATCH27_REWARD_NODE_ANCHOR_MISSING quest={quest_id}")
    text = text.replace(REWARD_NODE_STALE, REWARD_NODE_FIXED, 1)

    pattern = re.compile(r'(    <transition source="s1" target="reward">\n)(.*?)(    </transition>\n)', re.S)
    match = pattern.search(text)
    if match is None:
        raise SystemExit(f"BATCH27_HANDOVER_ANCHOR_MISSING quest={quest_id}")
    body = match.group(2)
    cleaned = re.sub(r'      <actions>\n        <set-variable field="var0" value="1"/>\n      </actions>\n', '', body)
    if cleaned == body:
        raise SystemExit(f"BATCH27_STALE_WRITE_ANCHOR_MISSING quest={quest_id}")
    text = text[:match.start(2)] + cleaned + text[match.end(2):]

    transitions_anchor = '  <transitions>\n'
    if text.count(transitions_anchor) != 1:
        raise SystemExit(f"BATCH27_TRANSITIONS_ANCHOR_MISSING quest={quest_id}")
    text = text.replace(transitions_anchor, transitions_anchor + recovery_edge(quest_id), 1)
    return text


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply the batch-27 housing reward-row repair.")
    parser.add_argument("--check", action="store_true", help="只校验，不写文件 / verify only")
    args = parser.parse_args()

    quests = sorted(CONTRACTS)
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
            print(f"BATCH27_PENDING quests={pending}")
            return 1
        print(f"BATCH27_OK quests={quests} already-applied")
        return 0
    if pending:
        print(f"BATCH27_APPLIED quests={pending}")
    else:
        print(f"BATCH27_OK quests={quests} already-applied")
    return 0


if __name__ == "__main__":
    sys.exit(main())
