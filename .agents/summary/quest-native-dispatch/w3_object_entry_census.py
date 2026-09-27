#!/usr/bin/env python3
# W3 事实冻结（只读）：物件入口接取变体的候选行集普查。
# 判据（与待落谓词同轴）：非系统 ∧ 无 NPC_START 块 ∧ acquired 解析唯一 ∧ 该 NPC 上有
#   (a) unaccepted 源、USE_OBJECT 动作、下发 SELECT1 入口页的入口自环
#   (b) 同一 NPC 上 ASK_QUEST_ACCEPT（下发接取窗页）
#   (c) unaccepted→started 的 QUEST_ACCEPT* 提交记录
import sys
from collections import defaultdict
base = "src/main/resources/aion/data/static_data/quest_retail/"
S = base + "quest_client_talk_chain_steps.tsv"
rows = defaultdict(list)
for line in open(S, encoding="utf-8"):
    if line.startswith("#") or not line.strip():
        continue
    f = line.rstrip("\n").split("\t")
    rows[int(f[0])].append(f)

# 真端表：acquired_npc_name / talk_npc1
import re
retail = {}
xml = open(base + "Quest_SimpleTalk.xml", encoding="utf-8").read()
for m in re.finditer(r'<id id="(\d+)">(.*?)</id>', xml, re.S):
    qid = int(m.group(1)); body = m.group(2)
    def g(tag):
        mm = re.search(r"<%s>(.*?)</%s>" % (tag, tag), body)
        return mm.group(1).strip() if mm else None
    retail[qid] = dict(acquired=g("acquired_npc_name"), reward=g("reward_npc_name"),
                       give=g("give_item"), item_check=g("item_check"))

cands = []
for qid, recs in sorted(rows.items()):
    if qid not in retail:
        continue
    blocks = {r[1] for r in recs}
    if "NPC_START" in blocks:
        continue
    ent = [r for r in recs if r[1] == "R" and "USE_OBJECT" == r[4] and r[5] == "unaccepted"]
    if not ent:
        continue
    for e in ent:
        npc = int(e[3])
        same = [r for r in recs if r[1] == "R" and int(r[3]) == npc]
        entry_page = [t for t in e[10].split(";") if t.startswith("DIALOG:SHOW_QUEST_PAGE:")]
        relay = [r for r in same if r[4] == "ASK_QUEST_ACCEPT"]
        commit = [r for r in same if r[4].startswith("QUEST_ACCEPT") and r[5] == "unaccepted" and r[6] == "started"]
        cands.append((qid, npc, e[9] or "-", "|".join(entry_page), len(relay), len(commit),
                      retail[qid]["acquired"], retail[qid]["give"]))
print("quest\tnpc\tentry_actions\tentry_page\trelay\tcommit\tacquired_name\tgive_item")
for c in cands:
    print("\t".join(str(x) for x in c))
ids = sorted({c[0] for c in cands})
print("\n候选行集 n=%d: %s" % (len(ids), ids))
# 反向：有 USE_OBJECT 记录但无 unaccepted 源者
others = sorted({q for q, recs in rows.items()
                 if any(r[1] == "R" and r[4] == "USE_OBJECT" and r[5] != "unaccepted" for r in recs)})
print("（对照）USE_OBJECT 记录非 unaccepted 源的行 n=%d: %s" % (len(others), others[:40]))
