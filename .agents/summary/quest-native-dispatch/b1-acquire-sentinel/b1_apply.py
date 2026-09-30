#!/usr/bin/env python3
# 批 1 数据面编辑（确定性、带断言计数）：
#   34 行 flip（retention → RETAIL_TABLE/OK）+ 149 行裁定（ADJUDICATED:<码>）+ 9 行码对齐（drift 权威码）
#   + decisions 登记 34 行 + rejects fixture 删 34 行 + catalog 删 34 条 + 删 34 个 XML。
import re
import os
import subprocess

ROOT = f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}"
RET_MAIN = f"{ROOT}/src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
RET_TEST = f"{ROOT}/src/test/resources/quest/retail-xml-retention.tsv"
DECISIONS = f"{ROOT}/src/test/resources/quest/retail-simple-hunt-adjudicated-decisions.tsv"
REJECTS = f"{ROOT}/src/test/resources/quest/retail-simplehunt-compiler-rejects.tsv"
CATALOG = f"{ROOT}/src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"
XML_DIR = f"{ROOT}/src/main/resources/aion/data/static_data/quest_definition/quests"
TARGET_XML_DIR = f"{ROOT}/target/classes/aion/data/static_data/quest_definition/quests"

adopt = {}
for line in open("/tmp/b1-adopt34.tsv"):
    q, n = line.split()
    adopt[int(q)] = int(n)
assert len(adopt) == 34, len(adopt)

# NPC 名（证据列用）：从 forensics 提取 reward 名
reward_name = {}
for line in open("/tmp/b1-forensics.tsv"):
    f = line.rstrip("\n").split("\t")
    if f[2] == "RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH":
        m = re.match(r"reward=(.*) reward_res=", f[6] if len(f) > 6 else f[-1])
        # 列布局：0 qid 1 fam 2 code 3 acq= 4 acq_res 5 reward= 6 reward_res 7 area 8 client
# 重新按真实列解析（dd 行不同）——hunt 行：3=acq= 4=acq_res 5=reward=<name> 6=reward_res=... 7=area_bound 8=client
for line in open("/tmp/b1-forensics.tsv"):
    f = line.rstrip("\n").split("\t")
    if len(f) >= 6 and f[2] == "RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH":
        m = re.match(r"reward=(.*)", f[5])
        if m:
            reward_name[int(f[0])] = m.group(1)

def edit_retention(path):
    lines = open(path, encoding="utf-8").read().splitlines()
    out, flips, adjs, realigns = [], 0, 0, 0
    for line in lines:
        parts = line.split("\t")
        if len(parts) >= 5 and parts[0].isdigit():
            qid = int(parts[0])
            code = parts[3]
            if code == "SEMANTIC_GAP:RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH":
                if qid in adopt:
                    parts[1], parts[3] = "RETAIL_TABLE", "OK"
                    parts[4] = (f"retail-simple-hunt-adjudicated-decisions.tsv ADOPT_RETAIL "
                                f"CHALLENGE_TASK_NPC_DELIVERY npc={adopt[qid]}")
                    flips += 1
                else:
                    parts[3] = "ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH"
                    parts[4] = ("b1-acquire-sentinel/b1_forensics.tsv challenge grant: no server "
                                f"accept entry; delivery npc {reward_name.get(qid, '?')} unresolved "
                                "in npc index")
                    adjs += 1
            elif code == "SEMANTIC_GAP:RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING":
                parts[3] = "ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL_AREA_PENDING"
                parts[4] = ("b1-acquire-sentinel/b1_forensics.tsv area grant: no quest_area binding "
                            "in ai-areas.xml (retail world files do not wire it)")
                adjs += 1
            elif code == "SEMANTIC_GAP:RETAIL_ACQUIRE_GRANT_UNSUPPORTED":
                # 以冻结 drift 表为准：9 行真实码已漂移到 talk 链族，8 行 EnterArea 留在本批裁定。
                drift = DRIFT_CODES.get(qid)
                if drift and drift.startswith("REJECTED:RETAIL_TALK_"):
                    parts[3] = "SEMANTIC_GAP:" + drift.split(":", 1)[1]
                    parts[4] = ("b1-acquire-sentinel/b1_forensics.tsv retention-lag fix: code "
                                f"realigned to frozen drift {drift} (talk-chain family, batch 3)")
                    realigns += 1
                else:
                    parts[3] = "ADJUDICATED:RETAIL_ACQUIRE_GRANT_UNSUPPORTED"
                    parts[4] = ("b1-acquire-sentinel/b1_forensics.tsv enter-area acquire: no "
                                "quest_area binding (or collect-progress shape unsupported by the "
                                "area grant edge)")
                    adjs += 1
            elif code == "SEMANTIC_GAP:RETAIL_ACQUIRE_NPC_UNRESOLVED":
                parts[3] = "ADJUDICATED:RETAIL_ACQUIRE_NPC_UNRESOLVED"
                parts[4] = ("b1-acquire-sentinel/b1_forensics.tsv acquire npc unresolved: name "
                            "absent from this server's npc_name_index")
                adjs += 1
            elif code == "SEMANTIC_GAP:RETAIL_ACQUIRE_NPC_SENTINEL":
                parts[3] = "ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL"
                parts[4] = ("b1-acquire-sentinel/b1_forensics.tsv faction sentinel with reward npc "
                            "unresolved (no deliverable owner on this server)")
                adjs += 1
            out.append("\t".join(parts))
        else:
            out.append(line)
    open(path, "w", encoding="utf-8").write("\n".join(out) + "\n")
    return flips, adjs, realigns

# drift 权威码（9 行滞后修正用）
DRIFT_CODES = {}
for line in open(f"{ROOT}/src/test/resources/quest/retail-data-driven-drift.tsv"):
    if line.startswith("#"):
        continue
    parts = line.rstrip("\n").split("\t")
    if len(parts) >= 2 and parts[0].isdigit():
        DRIFT_CODES[int(parts[0])] = parts[1]

flips = adjs = realigns = 0
for path in (RET_MAIN, RET_TEST):
    f2, a2, r2 = edit_retention(path)
    if path == RET_MAIN:
        flips, adjs, realigns = f2, a2, r2
    else:
        assert (flips, adjs, realigns) == (f2, a2, r2), "two retention copies diverged"
print(f"retention: flips={flips} adjudicated={adjs} realigned={realigns}")
assert (flips, adjs, realigns) == (34, 149, 9), (flips, adjs, realigns)

# decisions += 34
dec = open(DECISIONS, encoding="utf-8").read().rstrip("\n")
rows = []
for qid in sorted(adopt):
    npc = adopt[qid]
    rows.append(f"{qid}\tADOPT_RETAIL\tCHALLENGE_TASK_NPC_DELIVERY\t-\t"
                f"挑战哨兵接取=交付 NPC 本人：真端 reward_npc_name 唯一解析到 {npc}，"
                f"客户端 lifecycle 在同 NPC 登记 NPC_START 接取边（NONE→START）；"
                f"归一化名贯到下游（RetailSimpleHuntPlan.bind + RetailChallengeAcquireAdoptions）")
assert len(rows) == 34
open(DECISIONS, "w", encoding="utf-8").write(dec + "\n" + "\n".join(rows) + "\n")
print("decisions: +34")

# rejects fixture -= 34
rej_lines = open(REJECTS, encoding="utf-8").read().splitlines()
kept = [l for l in rej_lines if not (l.split("\t")[0].isdigit() and int(l.split("\t")[0]) in adopt
                                     and l.split("\t")[1] == "RETAIL_ACQUIRE_NPC_SENTINEL_NO_GRANT_PATH")]
removed = len(rej_lines) - len(kept)
assert removed == 34, removed
open(REJECTS, "w", encoding="utf-8").write("\n".join(kept) + "\n")
print("rejects fixture: -34")

# catalog -= 34 + XML 删除 + target 孤副本清理
cat = open(CATALOG, encoding="utf-8").read()
removed_cat = 0
for qid in adopt:
    pat = re.compile(r'\n\s*<definition id="%d" [^>]*/>' % qid)
    cat, n = pat.subn("", cat)
    removed_cat += n
    assert n == 1, (qid, n)
open(CATALOG, "w", encoding="utf-8").write(cat)
print("catalog: -34")

deleted = 0
for qid in adopt:
    p = f"{XML_DIR}/{qid}.xml"
    if os.path.exists(p):
        os.remove(p)
        deleted += 1
    t = f"{TARGET_XML_DIR}/{qid}.xml"
    if os.path.exists(t):
        os.remove(t)
print(f"xml deleted: {deleted} (target orphans cleaned where present)")
assert deleted == 34
print("OK")
