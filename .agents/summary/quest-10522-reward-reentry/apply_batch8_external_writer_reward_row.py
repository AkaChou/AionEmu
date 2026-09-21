#!/usr/bin/env python3
"""批次 8：QE-046 引擎外写入者补齐领奖行（8 个任务）。

背景：QE-051 要求 reward 节点投影 = 客户端 quest_summary 的领奖行；QE-046 又要求 reward 投影 =
引擎外写入方留下的 packed step。对“2 行任务（进行行 + 和 NPC 对话/报告行）”两者只有在写入方
同时写行号时才同时成立——本批次把写入方改成写 var0=1（与已修好的 15545/25545 模板一致），
再把 reward 投影推到 1，并把旧存档（REWARD/var0=0）的无 source enter-world 自愈边改成 0。

写入方：
- CM_CREATIVITY_POINTS#checkQuestCompletion（10522/20522）
- CoalescenceService#updateQuestsOnCoalescenceComplete（15542/25542）
- RiftOrbAI2#forQuest（30211/30213/30311/30313）

用法：
    python3 .agents/summary/quest-10522-reward-reentry/apply_batch8_external_writer_reward_row.py [--check]
"""

from __future__ import annotations

import re
import sys
from pathlib import Path
from xml.etree import ElementTree as ET

REPO = Path(__file__).resolve().parents[3]
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

# (quest_id, 客户端文件, 末行文本, 领奖 NPC id, 写入方文件, 定位锚点)
CONTRACTS = [
    (10522, "quest_q10522.html", "和代理人 Weatha 对话", 806075,
     "src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_CREATIVITY_POINTS.java",
     "10522"),
    (20522, "quest_q20522.html", "和代理人 Feregran 对话（客户端字典写作 LF6_Weatha_E）", 806079,
     "src/main/java/com/aionemu/gameserver/network/aion/clientpackets/CM_CREATIVITY_POINTS.java",
     "20522"),
    (15542, "quest_q15542.html", "和 LF6_Felen_E 对话", 806074,
     "src/main/java/com/aionemu/gameserver/services/item/CoalescenceService.java", "15542"),
    (25542, "quest_q25542.html", "和 DF6_Edorin_E 对话", 806078,
     "src/main/java/com/aionemu/gameserver/services/item/CoalescenceService.java", "25542"),
    (30211, "quest_q30211.html", "和 Pilomenes 对话", 798941,
     "src/main/java/com/aionemu/gameserver/ai/instance/beshmundirTemple/RiftOrbAI2.java", "30211"),
    (30213, "quest_q30213.html", "和 Cainus 对话", 798926,
     "src/main/java/com/aionemu/gameserver/ai/instance/beshmundirTemple/RiftOrbAI2.java", "30213"),
    (30311, "quest_q30311.html", "和 Herka 对话", 799322,
     "src/main/java/com/aionemu/gameserver/ai/instance/beshmundirTemple/RiftOrbAI2.java", "30311"),
    (30313, "quest_q30313.html", "和 Hler 对话", 799225,
     "src/main/java/com/aionemu/gameserver/ai/instance/beshmundirTemple/RiftOrbAI2.java", "30313"),
]

RECOVERY_MARKER_EN = "Reward row contract (QE-051/QE-046)"


def comment(quest_id, client_file, last_text, npc_id, client_rows=2) -> str:
    return (
        f"    <!-- QE-046/QE-051 领奖行合同：客户端 {client_file} 的 quest_summary 共 {client_rows} 行，"
        f"末行是领奖行\n"
        f"         （{last_text}，NPC id {npc_id}）；写入方在该行推进 var0=1 后置 REWARD，"
        f"旧存档 REWARD/var0=0 由本边按 reward 投影自愈。\n"
        f"         Reward row contract (QE-051/QE-046): the last of the {client_rows} {client_file} journal\n"
        f"         rows is the reward row ({last_text}, npc id {npc_id}); the engine-external writer now leaves\n"
        f"         var0=1 before REWARD, and this edge repairs saves persisted at REWARD/var0=0. -->\n"
    )


def recovery_block(quest_id, client_file, last_text, npc_id) -> str:
    return comment(quest_id, client_file, last_text, npc_id) + (
        '    <transition target="reward">\n'
        "      <event>\n"
        "        <enter-world/>\n"
        "      </event>\n"
        "      <conditions>\n"
        '        <status-is status="REWARD"/>\n'
        '        <variable-is field="var0" value="0"/>\n'
        "      </conditions>\n"
        "      <after-commit>\n"
        '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        "      </after-commit>\n"
        "    </transition>\n"
    )


def patch_xml(quest_id, client_file, last_text, npc_id) -> str:
    path = QUESTS / f"{quest_id}.xml"
    text = path.read_text(encoding="utf-8")
    if RECOVERY_MARKER_EN in text and 'value="0"/>\n      </conditions>' in text:
        return f"{quest_id}: already applied"
    # 1) reward 投影 0 -> 1
    block = re.search(r'    <node label="reward" status="REWARD">.*?    </node>\n', text, re.S)
    if not block:
        raise SystemExit(f"quest {quest_id}: reward node not found")
    updated = re.sub(r'(<var name="var0" value=")(\d+)("/>)', r"\g<1>1\g<3>", block.group(0))
    text = text[:block.start()] + updated + text[block.end():]
    # 2) 已有自愈边（10522/20522）：把紧邻其上的旧注释换成新注释，并把条件 1 -> 0；
    #    其余任务插入新边（含注释，无 actions）。
    anchor_head = '    <transition target="reward">\n      <event>\n        <enter-world/>'
    if anchor_head in text:
        index = text.index(anchor_head)
        end = text.index("    </transition>\n", index) + len("    </transition>\n")
        block = text[index:end]
        if 'value="1"' not in block:
            return f"{quest_id}: already applied"
        comment_start = text.rfind("    <!--", 0, index)
        prefix = text[:comment_start] if comment_start != -1 else text[:index]
        replaced_block = block.replace('<variable-is field="var0" value="1"/>',
                                       '<variable-is field="var0" value="0"/>')
        text = prefix + comment(quest_id, client_file, last_text, npc_id) + replaced_block + text[end:]
    else:
        anchor = "  <transitions>\n"
        if text.count(anchor) != 1:
            raise SystemExit(f"quest {quest_id}: <transitions> anchor not unique")
        text = text.replace(anchor, anchor + recovery_block(quest_id, client_file, last_text, npc_id), 1)
    path.write_text(text, encoding="utf-8")
    return f"{quest_id}: reward var0 -> 1 + enter-world recovery from var0=0"


def patch_writer(quest_id, writer_rel) -> str:
    path = REPO / writer_rel
    text = path.read_text(encoding="utf-8")
    # 在“该任务的 REWARD 推进”块里，把 setStatus(REWARD) 之前插入行号写入（幂等：块内已有则跳过）。
    if "RiftOrbAI2" in writer_rel:
        marker = ("        if (qs != null && qs.getStatus() == QuestStatus.START) {\n"
                  "            qs.setStatus(QuestStatus.REWARD);")
        if marker not in text:
            if "setQuestVarById(0, 1)" in text:
                return f"{writer_rel}: already applied"
            raise SystemExit(f"{writer_rel}: REWARD block not found")
        replacement = (
            "        if (qs != null && qs.getStatus() == QuestStatus.START) {\n"
            "            // QE-046/QE-051 领奖行：写入方把 var0 推进到末行（和 NPC 对话）再置 REWARD，"
            "与 reward 节点投影一致。\n"
            "            // QE-046/QE-051 reward row: the writer advances var0 to the last journal row"
            " (talk to the NPC) before REWARD so it matches the reward projection.\n"
            "            qs.setQuestVarById(0, 1);\n"
            "            qs.setStatus(QuestStatus.REWARD);")
        text = text.replace(marker, replacement, 1)
    else:
        pattern = re.compile(
            rf"(hasQuest\({quest_id}\).*?if \(qs != null && qs\.getStatus\(\) == QuestStatus\.START\) \{{\n)"
            r"((?:\s*//[^\n]*\n)*)(\s+)qs\.setStatus\(QuestStatus\.REWARD\);", re.S)
        m = pattern.search(text)
        if not m:
            raise SystemExit(f"{writer_rel}: REWARD block for {quest_id} not found")
        if "setQuestVarById" in m.group(0):
            return f"{writer_rel}: already applied for quest {quest_id}"
        indent = m.group(3)
        insert = (f"{indent}// QE-046/QE-051 领奖行：写入方把 var0 推进到末行（和代理人对话）再置 REWARD，"
                  f"与 reward 节点投影一致。\n"
                  f"{indent}// QE-046/QE-051 reward row: the writer advances var0 to the last journal row"
                  f" (talk to the agent) before REWARD so it matches the reward projection.\n"
                  f"{indent}qs.setQuestVarById(0, 1);\n")
        text = text[:m.end(1)] + insert + text[m.end(1):]
    path.write_text(text, encoding="utf-8")
    return f"{writer_rel}: setQuestVarById(0, 1) inserted for quest {quest_id}"


def verify() -> list[str]:
    problems = []
    for quest_id, client_file, last_text, npc_id, writer_rel, _anchor in CONTRACTS:
        root = ET.parse(QUESTS / f"{quest_id}.xml").getroot()
        reward = [n for n in root.findall("./nodes/node") if n.get("status") == "REWARD"][0]
        value = [v.get("value") for v in reward.findall("var")][0]
        if value != "1":
            problems.append(f"{quest_id}: reward var0 {value} != 1")
        recoveries = [t for t in root.findall("./transitions/transition")
                      if t.get("source") is None and t.get("target") == "reward"]
        conds = {c.get("value") for r in recoveries for c in r.findall("./conditions/variable-is")}
        if conds != {"0"} or len(recoveries) != 1:
            problems.append(f"{quest_id}: recovery edges {len(recoveries)} conditions {conds}")
        if any(t.findall("./actions/*") for t in recoveries):
            problems.append(f"{quest_id}: recovery edge must not declare actions")
        writer = (REPO / writer_rel).read_text(encoding="utf-8")
        if "RiftOrbAI2" in writer_rel:
            if writer.count("setQuestVarById") != 1:
                problems.append(f"{quest_id}: writer {writer_rel} setQuestVarById count "
                                f"{writer.count('setQuestVarById')} != 1")
        else:
            pattern = re.compile(
                rf"hasQuest\({quest_id}\).*?qs\.setStatus\(QuestStatus\.REWARD\);", re.S)
            block = pattern.search(writer)
            if not block or "setQuestVarById(0, 1)" not in block.group(0):
                problems.append(f"{quest_id}: writer {writer_rel} missing setQuestVarById(0, 1)")
    return problems


def main() -> int:
    if "--check" not in sys.argv:
        patched_writers = set()
        for quest_id, client_file, last_text, npc_id, writer_rel, _anchor in CONTRACTS:
            print(patch_xml(quest_id, client_file, last_text, npc_id))
            # RiftOrbAI2 的 forQuest 用一个循环推进 4 个任务，只插入一次。
            if "RiftOrbAI2" in writer_rel and writer_rel in patched_writers:
                continue
            print(patch_writer(quest_id, writer_rel))
            patched_writers.add(writer_rel)
    problems = verify()
    if problems:
        print("BATCH8_VERIFY_FAILED")
        for problem in problems:
            print("  -", problem)
        return 1
    print(f"BATCH8_VERIFY_OK quests={len(CONTRACTS)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
