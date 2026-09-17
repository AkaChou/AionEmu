#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3：职业权限（class axis）修复脚本（幂等，可复算）。

定性结论（2026-09-18，见 GOAL_PROGRESS.zh-CN.md 阶段 P3）：
- FIX_ADD   生产缺 <classes>（通配）而真端限定子集 -> 按真端写入（导师任务族、
  Kaliga 武器收集族、Dark Poeta/守护者英雄分组任务）。
- FIX_REPLACE 生产声明过宽 -> 按真端收窄（Crying Spear/Judge Dagger 族多 TEMPLAR、
  19074 枪星导师任务多 AETHERTECH、14031/24031 机甲星使命过宽）。
- FIX_APPEND 生产子集缺新职业 -> 补齐（1466 族缺 AETHERTECH、11076 缺 AETHERTECH+MUSE）。
- EVIDENCE_BLOCKED 不修改：24050~24054（真端缺 MUSE 疑似笔误）、3910、4931。
- INTENTIONAL 不修改：3121/30350（密码之刃=技匠武器适配）、30237（新枪矛=战士武器适配）。

所有目标 classes 只写 alive 进阶职业（任务 min-level>=17，base 死条目无行为差异），
按 PlayerClass id 顺序输出。
"""
import os
import re

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")

ORDER = ["WARRIOR", "GLADIATOR", "TEMPLAR", "SCOUT", "ASSASSIN", "RANGER",
         "MAGE", "SORCERER", "SPIRIT_MASTER", "PRIEST", "CLERIC", "CHANTER",
         "TECHNIST", "GUNSLINGER", "AETHERTECH", "MUSE", "SONGWEAVER"]

KALIGA = {
    "18618": ["ASSASSIN", "GLADIATOR", "RANGER", "TEMPLAR"],
    "18619": ["GLADIATOR", "TEMPLAR"],
    "18620": ["ASSASSIN", "GLADIATOR", "RANGER"],
    "18621": ["GLADIATOR"],
    "18622": ["ASSASSIN", "GLADIATOR", "RANGER"],
    "18623": ["CHANTER", "CLERIC", "GLADIATOR", "TEMPLAR"],
    "18624": ["CHANTER", "CLERIC"],
    "18625": ["SORCERER", "SPIRIT_MASTER"],
    "18626": ["SORCERER", "SPIRIT_MASTER"],
    "18627": ["CHANTER", "CLERIC", "GLADIATOR", "TEMPLAR"],
}
for base in list(KALIGA):
    KALIGA[str(int(base) + 10000)] = KALIGA[base]

GUN = {"18643": ["GUNSLINGER"], "18644": ["GUNSLINGER"],
       "18645": ["SONGWEAVER"], "18648": ["AETHERTECH"]}
for base in list(GUN):
    GUN[str(int(base) + 10000)] = GUN[base]

DARK_POETA = {
    "80219": ["ASSASSIN", "GLADIATOR", "GUNSLINGER", "RANGER", "TEMPLAR"],
    "80220": ["AETHERTECH", "CHANTER", "CLERIC", "SONGWEAVER", "SORCERER", "SPIRIT_MASTER"],
    "80316": ["ASSASSIN", "CHANTER", "GLADIATOR", "RANGER", "TEMPLAR"],
    "80317": ["CLERIC", "SORCERER", "SPIRIT_MASTER"],
}
DARK_POETA["80228"] = DARK_POETA["80219"]
DARK_POETA["80229"] = DARK_POETA["80220"]
DARK_POETA["80322"] = DARK_POETA["80316"]
DARK_POETA["80323"] = DARK_POETA["80317"]

PRECEPTOR = {
    "3928": ["CLERIC"], "3929": ["CHANTER"],
    "4922": ["GLADIATOR"], "4926": ["SORCERER"], "4928": ["CLERIC"],
    "4927": ["SPIRIT_MASTER"], "4929": ["CHANTER"],
}

FIX_ADD = {}
FIX_ADD.update(PRECEPTOR)
FIX_ADD.update(KALIGA)
FIX_ADD.update(GUN)
FIX_ADD.update(DARK_POETA)

FIX_REPLACE = {
    "18614": ["GLADIATOR"],
    "18630": ["ASSASSIN", "GLADIATOR", "RANGER"],
    "18634": ["GLADIATOR"],
    "28614": ["GLADIATOR"],
    "28630": ["ASSASSIN", "GLADIATOR", "RANGER"],
    "28634": ["GLADIATOR"],
    "19074": ["GUNSLINGER"],
    "14031": ["AETHERTECH"],
    "24031": ["AETHERTECH"],
}

FIX_APPEND = {
    "1466": ["AETHERTECH"],
    "1496": ["AETHERTECH"],
    "1497": ["AETHERTECH"],
    "1498": ["AETHERTECH"],
    "2696": ["AETHERTECH"],
    "11076": ["AETHERTECH", "MUSE"],
}

CLASSES_RE = re.compile(r"\n[ \t]*<classes>.*?</classes>", re.S)
RACES_CLOSE = "</races>"


def classes_block(ids):
    body = "\n".join(f'      <class id="{i}"/>' for i in ids)
    return f"\n    <classes>\n{body}\n    </classes>"


def process(qid, target_ids, append=False):
    path = os.path.join(PROD_DIR, qid + ".xml")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    current = re.search(r"<classes>(.*?)</classes>", text, re.S)
    current_ids = re.findall(r'<class id="([^"]+)"', current.group(1)) if current else []
    if append:
        # FIX_APPEND：在现有声明上补缺，不能整块替换。
        merged = sorted(set(current_ids) | set(target_ids), key=ORDER.index)
    else:
        merged = sorted(target_ids, key=ORDER.index)
    if sorted(current_ids, key=ORDER.index) == merged:
        print(f"{qid}: already {merged}")
        return
    block = classes_block(merged)
    if current:
        text = CLASSES_RE.sub(block, text, count=1)
    else:
        idx = text.find(RACES_CLOSE)
        if idx < 0:
            raise SystemExit(f"{qid}: no <races> anchor for classes insertion")
        insert_at = idx + len(RACES_CLOSE)
        text = text[:insert_at] + block + text[insert_at:]
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)
    print(f"{qid}: classes -> {merged}")


def main():
    for qid, ids in {**FIX_ADD, **FIX_REPLACE}.items():
        process(qid, ids)
    for qid, ids in FIX_APPEND.items():
        process(qid, ids, append=True)


if __name__ == "__main__":
    main()
