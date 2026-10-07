#!/usr/bin/env python3
"""领奖行批次错误修复（QE-054 步号轴权威）：40 个 XML 任务的 reward 投影回归权威值。

修复内容（对每个任务）：
1. reward 节点 var0：批次末行索引 → 真端/legacy 权威值；
2. 批次自愈边反转：条件 var0==权威 → set 批次值 改为 条件 var0==批次值 → set 权威值
   （型 B：条件 var0==0 的旧档直修边——动作改权威值，并新增批次坏档回滚边）；
3. 自愈边前的旧「领奖行合同」注释块替换为 QE-054 权威说明（双语）。

用法：
    python3 .agents/summary/quest-step-axis-fullscan/apply_reward_axis_fix.py [--dry]
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"

# (questId, 批次值 rewardRow, 权威值 auth, 证据摘要)
FIXLIST = [
    (1626, 7, 6, "legacy useQuestObject(6,6,true) 落盘 6；真端 SetProgress 1..6"),
    (1636, 4, 3, "legacy useQuestItem(3,3,true) 落盘 3；真端 1|2|3"),
    (2122, 2, 1, "legacy defaultCloseDialog(1,1,true) 落盘 1；真端 1"),
    (2208, 2, 1, "legacy 击杀后 setQuestVar(1)；真端 1"),
    (2284, 3, 2, "真端 1|2 无 3；staleRow=2"),
    (2333, 3, 2, "legacy 护送 1→2、完成进 REWARD 轴保持 2；真端 1|2"),
    (2436, 2, 1, "legacy defaultFollowEndEvent(1,1,true) 落盘 1；真端 0|1"),
    (2620, 2, 1, "legacy setStatus(REWARD) 不写 var（var0=1）；staleRow=1"),
    (3056, 2, 1, "真端 SetProgress=1 且 0x110(1,1)；legacy 计数递进后轴 1"),
    (1319, 8, 7, "legacy 七次递进落 7 后 setStatus(REWARD)；真端 1..7"),
    (1900, 4, 3, "legacy defaultCloseDialog(3,4,true) 落盘 3；真端 1|2|3"),
    (3082, 3, 2, "legacy useQuestObject(2,2,true) 落盘 2；真端 1|2(提取不全)"),
    (3200, 4, 3, "真端 3；itemUseArea 审计已证 use-item 链轴 3"),
    (3721, 3, 2, "真端 2；镜像 4721 同"),
    (4038, 3, 2, "legacy changeQuestStep(2,3,true) 落盘 2；真端 2"),
    (4502, 3, 2, "legacy checkQuestItems(2,2,true) 落盘 2；真端 2"),
    (4721, 3, 2, "真端 2；镜像 3721 同"),
    (4939, 5, 4, "legacy defaultCloseDialog(4,4,true) 落盘 4；真端 4"),
    (4943, 4, 3, "legacy defaultCloseDialog(3,3,true) 落盘 3；真端 2|3"),
    (11116, 3, 2, "真端 SetProgress(11116,2) 直证；legacy var+1 与真端矛盾按真端"),
    (11076, 4, 3, "legacy defaultCloseDialog(3,4,true) 落盘 3；真端 1|2|3"),
    (14046, 7, 6, "legacy defaultCloseDialog(6,6,true) 落盘 6；真端 3|5 无 7"),
    (14051, 4, 3, "legacy STEP_TO_4 仅 setStatus(REWARD) 不写 var（=3）；真端 1|3"),
    (14153, 6, 5, "legacy setQuestVarById(0,5)+REWARD 落盘 5；真端 3 无 6"),
    (10530, 9, 8, "legacy checkQuestItems(8,9,true) 落盘 8；真端无 9"),
    (20530, 9, 8, "镜像 10530 对称 + 真端 0x100 不写轴全局语义"),
    (21114, 5, 4, "legacy defaultOnKillEvent(216563,4,true) 落盘 4；真端 1|3|4"),
    (24022, 8, 7, "legacy defaultCloseDialog(7,7,true) 落盘 7；真端 2|3|5|6|7"),
    (24023, 4, 3, "真端 1|2|3 无 4；staleRow=3"),
    (24024, 5, 4, "legacy defaultOnKillEvent(212861,4,true) 落盘 4；真端 1|3|4"),
    (24025, 4, 3, "legacy defaultCloseDialog(3,3,true) 落盘 3；真端 3"),
    (24030, 9, 8, "真端 2|4|5|6|8 无 9；staleRow=8"),
    (24046, 7, 6, "legacy defaultCloseDialog(6,6,true) 落盘 6；真端 4|6 无 7"),
    (24051, 6, 5, "legacy changeQuestStep(5,5,true) 落盘 5；真端 1|3|5"),
    (30111, 2, 1, "真端 1 无 2；staleRow=1"),
    (30227, 3, 2, "legacy 击杀 var+1→2 后 REWARD；真端 1|2"),
    (30327, 3, 2, "镜像 30227 同；真端 1|2"),
    (30217, 3, 2, "legacy setVar(2) 后 setStatus(REWARD) 落盘 2；staleRow=2"),
    (30317, 3, 2, "镜像 30217 同"),
    (1921, 4, 3, "legacy defaultCloseDialog(3,3,true) 落盘 3；staleRow=3"),
]

COMMENT_ZH = ("QE-054 步号轴权威：领奖投影 = 真端/legacy 推进值 {auth}（真端 0x100 状态推进不写轴，"
              "客户端 REWARD 态按 [%N] 行门槛自行显示报告行）；{evidence}。"
              "曾被领奖行批次误抬为 {bad}，本边把坏档回滚到 {auth}。")
COMMENT_EN = ("Step-axis authority (QE-054): the reward projection is the retail/legacy progress "
              "value {auth} (retail 0x100 state advances never touch the axis; the client shows the "
              "report row via its own [%N] gate); {evidence}. The reward-row batch once mislifted it "
              "to {bad} — this edge rolls bad saves back to {auth}.")

# 旧注释块识别（紧邻自愈边、含这些关键词）
OLD_COMMENT_KEYS = ("领奖行", "QE-051", "旧存档修复", "reward row", "Reward row", "Reward-row")


def build_comment(auth: int, bad: int, evidence: str) -> str:
    zh = COMMENT_ZH.format(auth=auth, bad=bad, evidence=evidence)
    en = COMMENT_EN.format(auth=auth, bad=bad, evidence=evidence)
    return f"    <!-- {zh}\n         {en} -->"


def fix_file(path: Path, bad: int, auth: int, evidence: str, dry: bool) -> list[str]:
    text = path.read_text(encoding="utf-8")
    log = []

    # 1) reward 节点 var0（var0 之后可能还有其它 var，不要求紧跟 </node>）
    pat_node = re.compile(r'(<node label="reward" status="REWARD">\s*<var name="var0" value=")('
                          + str(bad) + r')(")')
    def node_repl(m):
        log.append(f"reward node: {bad} -> {auth}")
        return m.group(1) + str(auth) + m.group(3)
    new_text, n = pat_node.subn(node_repl, text, count=1)
    if n != 1:
        return [f"SKIP {path.name}: reward node var0={bad} not found"]
    text = new_text

    # 2) 批次自愈边：定位 target="reward" 且 enter-world 的 transition 块
    edge_pat = re.compile(
        r'    <transition target="reward">\n'
        r'(      <event>\n        <enter-world/>\n      </event>\n'
        r'      <conditions>\n'
        r'        <status-is status="REWARD"/>\n'
        r'        <variable-is field="var0" value=")(\d+)("/>\n'
        r'      </conditions>\n'
        r'      <actions>\n'
        r'        <set-variable field="var0" value=")(\d+)("/>\n'
        r'      </actions>\n'
        r'      <after-commit>\n'
        r'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        r'      </after-commit>\n'
        r'    </transition>)')
    m = edge_pat.search(text)
    if not m:
        return [f"SKIP {path.name}: batch healing edge not found"] + log
    cond_val, act_val = int(m.group(2)), int(m.group(4))
    if cond_val == auth and act_val == bad:
        # 型 A：条件=权威 动作=批次值 → 反转
        new_edge = m.group(0).replace(f'value="{auth}"', f'value="{bad}"', 1)
        new_edge = new_edge.replace(f'<set-variable field="var0" value="{bad}"/>',
                                    f'<set-variable field="var0" value="{auth}"/>', 1)
        text = text[:m.start()] + new_edge + text[m.end():]
        log.append(f"edge A reversed: cond {auth}->{bad}, act {bad}->{auth}")
    elif cond_val == 0 and act_val == bad:
        # 型 B：条件=0（迁移期旧档直修）→ 动作改权威；另加批次坏档回滚边
        new_edge = m.group(0).replace(f'<set-variable field="var0" value="{bad}"/>',
                                      f'<set-variable field="var0" value="{auth}"/>', 1)
        rollback = (f'    <transition target="reward">\n'
                    f'      <event>\n        <enter-world/>\n      </event>\n'
                    f'      <conditions>\n'
                    f'        <status-is status="REWARD"/>\n'
                    f'        <variable-is field="var0" value="{bad}"/>\n'
                    f'      </conditions>\n'
                    f'      <actions>\n'
                    f'        <set-variable field="var0" value="{auth}"/>\n'
                    f'      </actions>\n'
                    f'      <after-commit>\n'
                    f'        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
                    f'      </after-commit>\n'
                    f'    </transition>')
        text = text[:m.start()] + new_edge + "\n" + rollback + text[m.end():]
        log.append(f"edge B: act {bad}->{auth}; rollback edge {bad}->{auth} added")
    else:
        return [f"SKIP {path.name}: unexpected edge cond={cond_val} act={act_val}"] + log

    # 3) 旧注释块 → QE-054 模板（定位自愈边之前的紧邻注释）
    edge_pos = text.find('    <transition target="reward">\n      <event>\n        <enter-world/>')
    before = text[:edge_pos]
    # 向前找最后一个完整注释块
    comment_pat = re.compile(r'(    <!--(?:[^-]|-[^-]|--[^>])*?-->\n(?:[ \t]*\n)?)')
    last = None
    for cm in comment_pat.finditer(before):
        if any(k in cm.group(1) for k in OLD_COMMENT_KEYS):
            last = cm
    if last:
        text = text[:last.start()] + build_comment(auth, bad, evidence) + "\n" + text[last.end():]
        log.append("comment block replaced with QE-054 template")
    else:
        # 无紧邻旧注释：在自愈边前插入新注释
        text = (text[:edge_pos] + build_comment(auth, bad, evidence) + "\n\n" + text[edge_pos:])
        log.append("QE-054 comment inserted (no old comment found)")

    if not dry:
        path.write_text(text, encoding="utf-8")
    return log


def main() -> int:
    dry = "--dry" in sys.argv
    for qid, bad, auth, evidence in FIXLIST:
        path = QUESTS / f"{qid}.xml"
        if not path.is_file():
            print(f"{qid}: NO XML (skip)")
            continue
        print(f"== {qid} (batch {bad} -> auth {auth})")
        for line in fix_file(path, bad, auth, evidence, dry):
            print(f"   {line}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
