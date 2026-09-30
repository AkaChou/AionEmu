#!/usr/bin/env python3
# dialog_exits 行级缩表普查（批 0 R1，quest-native-dispatch 车道）。
# 判据（静态可达性，证据 = 编译器调用点逐处核对）：
#   requires( 全仓 12 处 / 3 文件，测试零调用：
#     RetailSimpleTalkDefinitionCompiler :1212/:1214  SELECT1_1/SELECT1_1_1 —— 仅「链行 ∧ 非 systemGrant ∧
#                                     有 NPC_START 块 ∧ 非 canonical 接取」分支可达（canonical/无块/系统发放行到不了）
#     RetailSimpleTalkDefinitionCompiler :1411  SELECT2_CONTINUE；:1438/:1439 SELECT5_CHECK/SIMPLE；
#                                     :1465 SELECT6 —— buildChain 体级无条件读（全部链行保留，交接包明令禁删）
#     RetailSimpleTalkDefinitionCompiler :1701/:1702/:1727/:1733 —— reportFlowChain（legacy 交付）同上保留
#     RetailDataDrivenDefinitionCompiler :163  SELECT_NONE_1 —— DD 行唯一读点
#     RetailDataDrivenCollectCompiler    :64   SELECT_NONE_1 —— 采集行唯一读点
#   singleStep 行（Quest_SimpleTalk 无 talk_npcN）走 build( 不带 exits 参数 → 全 token 不可达；
#   precheck 先于 build/buildChain → 被拒行不读表。
# 保守原则：判不了的行一律保留（少删安全，快照判据兜底）。
# Census for the row-level shrink of quest_client_dialog_exits.tsv (batch 0 R1).
# Row census per (quest x token) reachability; conservative: undecided rows keep tokens.
import os
import re
import sys
from collections import Counter

ROOT = f"{os.environ.get('AION_REPO_ROOT', os.path.expanduser('~/IdeaProjects/AionEmu-test'))}"
RETENTION = f"{ROOT}/src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
SIMPLETALK = f"{ROOT}/src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml"
CHAIN = f"{ROOT}/src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv"
EXITS = f"{ROOT}/src/main/resources/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv"
OUT = f"{ROOT}/.agents/summary/quest-native-dispatch/r1-dialog-exits-shrink/dialog_exits.shrunk.tsv"
REPORT = f"{ROOT}/.agents/summary/quest-native-dispatch/r1-dialog-exits-shrink/census-report.tsv"

# retention: quest_id, owner, family, reason, evidence
family = {}
owner = {}
with open(RETENTION, encoding="utf-8") as fh:
    for line in fh:
        if line.startswith("#"):
            continue
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 4 and parts[0].isdigit():
            family[int(parts[0])] = parts[2]
            owner[int(parts[0])] = parts[1]

# SimpleTalk template: <id id="N"> children acquired_npc_name / talk_npc1..
st_acquired = {}
st_talknpcs = {}
cur = None
with open(SIMPLETALK, encoding="utf-8") as fh:
    for line in fh:
        m = re.match(r'\s*<id id="(\d+)">', line)
        if m:
            cur = int(m.group(1))
            continue
        if cur is not None:
            m = re.match(r"\s*<acquired_npc_name>(.*)</acquired_npc_name>", line)
            if m:
                st_acquired[cur] = m.group(1).strip()
                continue
            m = re.match(r"\s*<talk_npc\d+>", line)
            if m:
                st_talknpcs[cur] = True

# chain registry: NPC_START block presence per quest id
start_blocks = set()
with open(CHAIN, encoding="utf-8") as fh:
    for line in fh:
        if line.startswith("#"):
            continue
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 3 and parts[1] == "B" and parts[2] == "NPC_START":
            start_blocks.add(int(parts[0]))


def is_sentinel(name: str) -> bool:
    # RetailGrantKind.of 的哨兵形状测试：_..._（长度 > 2）。
    # Same sentinel shape test as RetailGrantKind.of.
    return name is not None and len(name) > 2 and name.startswith("_") and name.endswith("_")


BODY_CHAIN_TOKENS = {"SELECT2_CONTINUE", "SELECT5_CHECK", "SELECT5_CHECK_SIMPLE", "SELECT6"}
LADDER_TOKENS = {"SELECT1_1", "SELECT1_1_1"}

removed = Counter()
kept = Counter()
classes = Counter()
report = []
out_lines = []
with open(EXITS, encoding="utf-8") as fh:
    for line in fh:
        if line.startswith("#") or line.strip() == "":
            out_lines.append(line.rstrip("\n"))
            continue
        parts = line.rstrip("\n").split("\t")
        qid = int(parts[0])
        tokens = [t for t in (parts[1].split() if len(parts) > 1 else []) if t]
        fam = family.get(qid)
        own = owner.get(qid)
        if fam in ("DataDriven", "SimpleCollectItem"):
            # DD/采集行唯一读点 = SELECT_NONE_1；其余 token 无消费者。
            # The only reader for DD/collect rows is SELECT_NONE_1.
            keep = [t for t in tokens if t == "SELECT_NONE_1"]
            klass = "dd-collect"
        elif fam == "SimpleTalk" and qid in st_acquired | st_talknpcs:
            if not st_talknpcs.get(qid):
                # singleStep 行走 build(（无 exits 参数）→ 全 token 不可达。
                # singleStep rows compile via build( which never receives exits.
                keep = []
                klass = "talk-single-step"
            elif is_sentinel(st_acquired.get(qid, "")):
                # systemGrant 链行：:1151 分支直接回放，:1182 接取块分支不可达 → 阶梯 token 死；
                # 体级四 token（SELECT2/SELECT5/SELECT6 族）按交接包禁令保留。
                # systemGrant chains skip the NPC_START branch; body-level four stay.
                keep = [t for t in tokens if t not in LADDER_TOKENS]
                klass = "talk-chain-system-grant"
            elif qid not in start_blocks:
                # 无 NPC_START 块的链行到不了 :1212 → 阶梯 token 死。
                # Chain rows without an NPC_START block never reach the ladder reader.
                keep = [t for t in tokens if t not in LADDER_TOKENS]
                klass = "talk-chain-no-start-block"
            else:
                # 有 START 块的非系统链行：无法静态排除 canonical 接取（S2 策略 A 已把接取梯
                # 记录从注册表过滤但块仍在）→ 保守保留全部 token。
                # Conservative: legacy-ladder candidates keep all tokens.
                keep = list(tokens)
                klass = "talk-chain-keep-all"
        elif fam in ("SimpleHunt", "SimpleSerialHunt", "SimpleUseItem", "SimpleItemPlay",
                     "CombineTask", "-", None):
            # 这些家族的编译器零 requires( 调用；'-' = 无真端表行（XML owner 从 XML 编译）。
            # These families never call requires(; '-' = no retail row at all.
            keep = []
            klass = f"family-{fam}"
        else:
            # 未知组合（XML_RETENTION 的 SimpleTalk 行等）：保守保留。
            # Unknown combination: conservative keep.
            keep = list(tokens)
            klass = "conservative-keep"
        removed_token_count = len(tokens) - len(keep)
        classes[klass] += 1
        for t in tokens:
            kept[t] += 1
        for t in tokens:
            if t not in keep:
                removed[t] += 1
        report.append(f"{qid}\t{own}\t{fam}\t{klass}\t{len(tokens)}\t{len(keep)}\t"
                      f"{' '.join(sorted(set(tokens) - set(keep)))}")
        out_lines.append(f"{qid}\t{' '.join(keep)}")

with open(OUT, "w", encoding="utf-8") as fh:
    fh.write("\n".join(out_lines) + "\n")
with open(REPORT, "w", encoding="utf-8") as fh:
    fh.write("# quest_id\towner\tfamily\tclass\ttokens_before\ttokens_after\tremoved\n")
    fh.write("\n".join(report) + "\n")

total_rows = sum(classes.values())
total_removed = sum(removed.values())
print(f"rows={total_rows} removed_tokens={total_removed}")
print("classes:", dict(classes))
print("removed_by_token:", dict(removed))
print("kept_by_token:", dict(kept))
