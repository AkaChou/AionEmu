#!/usr/bin/env python3
"""第二批领奖投影修复（QE-054）：6 个非 CONTRACTS 来源任务（triage 全量扫描候选）。

形态：
- 3711/4711：三条值自愈边（0/1/2→3）动作改权威 2，另加回滚边 3→2；
- 11031/11032/11033：无自愈边，reward 3→2 并新增回滚边 3→2（1345 先例）；
- 28602：单边 3→4 反转为 4→3（镜像 18602 的 kill 链权威 3）。

用法：
    python3 .agents/summary/quest-step-axis-fullscan/apply_reward_axis_fix2.py [--dry]
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"

COMMENT_ZH = ("QE-054 步号轴权威：领奖投影 = 真端/legacy 推进值 {auth}（真端 0x100 状态推进不写轴，"
              "客户端 REWARD 态按 [%N] 行门槛自行显示报告行）；{evidence}。"
              "本边把非权威存档归一到 {auth}。")
COMMENT_EN = ("Step-axis authority (QE-054): the reward projection is the retail/legacy progress "
              "value {auth} (retail 0x100 state advances never touch the axis; the client shows the "
              "report row via its own [%N] gate); per the legacy/retail progress evidence on the "
              "Chinese line. This edge collapses off-axis saves onto {auth}.")


def edge(cond_expr: str, act: int) -> str:
    return (f'    <transition target="reward">\n'
            f'      <event>\n        <enter-world/>\n      </event>\n'
            f'      <conditions>\n'
            f'        <status-is status="REWARD"/>\n'
            f'        {cond_expr}\n'
            f'      </conditions>\n'
            f'      <actions>\n'
            f'        <set-variable field="var0" value="{act}"/>\n'
            f'      </actions>\n'
            f'      <after-commit>\n'
            f'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
            f'      </after-commit>\n'
            f'    </transition>')


def fix(qid: int, bad: int, auth: int, evidence: str, mode: str, dry: bool) -> None:
    path = QUESTS / f"{qid}.xml"
    text = path.read_text(encoding="utf-8")
    print(f"== {qid} (batch {bad} -> auth {auth}, mode {mode})")
    # 1) reward 节点值
    text, n = re.subn(r'(<node label="reward" status="REWARD">\s*<var name="var0" value=")' + str(bad) + r'(")',
                      lambda m: m.group(1) + str(auth) + m.group(2), text, count=1)
    if n != 1:
        print("   SKIP: reward node not matched")
        return
    comment = ("    <!-- " + COMMENT_ZH.format(auth=auth, evidence=evidence) + "\n         "
               + COMMENT_EN.format(auth=auth) + " -->")
    if mode == "triple":
        # 三条值边（0/1/2→bad）动作改 auth；块前注释替换；追加回滚边 bad→auth
        block_pat = re.compile(
            r'(    <!-- [^\n]*\n(?:[^\n]*\n)*?    -->\n)?'
            r'(    <transition target="reward">\n      <event>\n        <enter-world/>\n      </event>\n'
            r'      <conditions>\n        <status-is status="REWARD"/>\n'
            r'        <variable-is field="var0" value="[012]"/>\n      </conditions>\n'
            r'      <actions>\n        <set-variable field="var0" value=")' + str(bad) + r'("/>\n'
            r'      </actions>\n      <after-commit>\n'
            r'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n      </after-commit>\n'
            r'    </transition>\n?)+')
        m = block_pat.search(text)
        if not m:
            print("   SKIP: triple-edge block not found")
            return
        rewritten = m.group(0)
        if rewritten.startswith("    <!--"):
            head, body = rewritten.split("-->\n", 1)
            rewritten = comment + "\n" + body
        else:
            rewritten = comment + "\n" + rewritten
        rewritten = rewritten.replace(f'<set-variable field="var0" value="{bad}"/>',
                                      f'<set-variable field="var0" value="{auth}"/>')
        rewritten = rewritten.rstrip("\n") + "\n" + edge(
            f'<variable-is field="var0" value="{bad}"/>', auth) + "\n"
        text = text[:m.start()] + rewritten + text[m.end():]
    elif mode == "none":
        # 无自愈边：在 <transitions> 后插入注释 + 回滚边
        rollback = comment + "\n\n" + edge(f'<variable-is field="var0" value="{bad}"/>', auth) + "\n"
        text = text.replace("  <transitions>\n", "  <transitions>\n" + rollback, 1)
    elif mode == "single":
        # 单边 cond=auth act=bad 反转 + 注释替换
        blk = re.compile(
            r'(    <!-- [^\n]*\n(?:[^\n]*\n)*?    -->\n)?'
            r'    <transition target="reward">\n      <event>\n        <enter-world/>\n      </event>\n'
            r'      <conditions>\n        <status-is status="REWARD"/>\n'
            r'        <variable-is field="var0" value=")' + str(auth) + r'("/>\n'
            r'      </conditions>\n      <actions>\n        <set-variable field="var0" value=")' + str(bad) + r'("/>\n')
        m = blk.search(text)
        if not m:
            print("   SKIP: single edge not found")
            return
        rewritten = comment + "\n" + (
            f'    <transition target="reward">\n      <event>\n        <enter-world/>\n      </event>\n'
            f'      <conditions>\n        <status-is status="REWARD"/>\n'
            f'        <variable-is field="var0" value="{bad}"/>\n'
            f'      </conditions>\n      <actions>\n        <set-variable field="var0" value="{auth}"/>\n')
        text = text[:m.start()] + rewritten + text[m.end():]
    if not dry:
        path.write_text(text, encoding="utf-8")
    print("   ok")


def main() -> int:
    dry = "--dry" in sys.argv
    fix(3711, 3, 2, "legacy defaultOnKillEvent(214823,2,true) 落盘 2；真端集合 {2}", "triple", dry)
    fix(4711, 3, 2, "镜像 3711 同；legacy defaultOnKillEvent(214823,2,true) 落盘 2", "triple", dry)
    fix(11031, 3, 2, "legacy useQuestItem(2,3,true) 落盘 2", "none", dry)
    fix(11032, 3, 2, "legacy useQuestItem(2,3,true) 落盘 2", "none", dry)
    fix(11033, 3, 2, "legacy useQuestItem(2,3,true) 落盘 2", "none", dry)
    fix(28602, 4, 3, "镜像 18602 同；legacy defaultOnKillEvent(targetId,3,true) 落盘 3；真端 {1,2,3}", "single", dry)
    return 0


if __name__ == "__main__":
    sys.exit(main())
