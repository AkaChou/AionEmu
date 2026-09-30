#!/usr/bin/env python3
# 批 1 修正：10 行采纳后仍被编译器拒绝（击杀目标别名 token 本服不可解析，属批 2 击杀轴）
# ⇒ 回退出采纳集：采纳类删条目、decisions 删行、retention 回 XML_RETENTION/ADJUDICATED、
#   恢复 XML + catalog 条目（fail-closed：门先红，回退后转绿并留痕）。
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

import os
import re
import subprocess

ROOT = f"{REPO}"
ADOPT_CLASS = f"{ROOT}/src/main/java/com/aionemu/gameserver/questEngine/retail/RetailChallengeAcquireAdoptions.java"
DECISIONS = f"{ROOT}/src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv"
RET_MAIN = f"{ROOT}/src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
RET_TEST = f"{ROOT}/src/test/resources/quest/retail-xml-retention.tsv"
CATALOG = f"{ROOT}/src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"
XML_DIR = f"{ROOT}/src/main/resources/aion/data/static_data/quest_definition/quests"

REVERT = [17011, 17015, 17016, 17017, 17018, 27011, 27015, 27016, 27017, 27018]

# 采纳类删条目
text = open(ADOPT_CLASS, encoding="utf-8").read()
for q in REVERT:
    pat = re.compile(r"\n\t\t\tMap\.entry\(%d, \d+\)," % q)
    text, n = pat.subn("", text)
    assert n == 1, (q, n)
open(ADOPT_CLASS, "w", encoding="utf-8").write(text)
print("adoption class: -10 entries")

# decisions 删 10 行
lines = open(DECISIONS, encoding="utf-8").read().splitlines()
kept = [l for l in lines if not (l.split("\t")[0].isdigit() and int(l.split("\t")[0]) in REVERT)]
assert len(lines) - len(kept) == 10, (len(lines), len(kept))
open(DECISIONS, "w", encoding="utf-8").write("\n".join(kept) + "\n")
print("decisions: -10")

# retention 回退（双副本同断言）
def edit(path):
    out, changed = [], 0
    for line in open(path, encoding="utf-8").read().splitlines():
        parts = line.split("\t")
        if parts[0].isdigit() and int(parts[0]) in REVERT:
            assert parts[1] == "RETAIL_TABLE" and parts[3] == "OK", line
            parts[1] = "XML_RETENTION"
            parts[3] = "ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH"
            parts[4] = ("b1-acquire-sentinel/b1_forensics.tsv challenge-adopter deferred: acquire "
                        "evidence complete (delivery npc + client NPC_START) but kill targets "
                        "(WorldRaid_*/quest-boss aliases) unresolved server-side - flip deferred "
                        "to the kill-target axis (batch 2)")
            changed += 1
        out.append("\t".join(parts))
    open(path, "w", encoding="utf-8").write("\n".join(out) + "\n")
    return changed

assert edit(RET_MAIN) == 10
assert edit(RET_TEST) == 10
print("retention: 10 reverted (both copies)")

# 恢复 XML + catalog（从 HEAD 恢复）
for q in REVERT:
    subprocess.run(["git", "checkout", "HEAD", "--", f"src/main/resources/aion/data/static_data/"
                   f"quest_definition/quests/{q}.xml"], cwd=ROOT, check=True)
    t = f"{ROOT}/target/classes/aion/data/static_data/quest_definition/quests/{q}.xml"
    import shutil, os
    if os.path.exists(f"{XML_DIR}/{q}.xml") and not os.path.exists(t):
        shutil.copy(f"{XML_DIR}/{q}.xml", t)
cat = open(CATALOG, encoding="utf-8").read()
added = 0
for q in REVERT:
    entry = (f'\n  <definition id="{q}" resource="aion/data/static_data/quest_definition/'
             f'quests/{q}.xml" mode="EXECUTABLE" />')
    if f'id="{q}"' not in cat:
        # 锚定插回：按 id 排序插到其后继 definition 前（无后继则插到收尾标签前）
        m = re.search(r'\n\s*<definition id="(\d+)"', cat)
        successors = sorted(int(m2.group(1)) for m2 in re.finditer(r'<definition id="(\d+)"', cat)
                            if int(m2.group(1)) > q)
        if successors:
            nxt = successors[0]
            cat = re.sub(r'(\n\s*<definition id="%d")' % nxt, entry + r"\1", cat, count=1)
        else:
            cat = cat.replace("</quest_definitions>", entry + "\n</quest_definitions>")
        added += 1
open(CATALOG, "w", encoding="utf-8").write(cat)
print(f"catalog: +{added}, xml restored: {len(REVERT)}")
print("OK")
