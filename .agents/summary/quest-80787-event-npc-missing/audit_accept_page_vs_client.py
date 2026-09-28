"""审计：已采纳(ADOPTED)的 DD 任务里，有多少任务的客户端 HTML 缺 ask_quest_accept(页 4)。

背景：DD 接取流 canonicalAcceptFlow 硬编码 QUEST_SELECT → SHOW_ASK_QUEST_ACCEPT_WINDOW(4)；
若该任务的客户端 HTML 只有 select_none(4762) 而没有 ask_quest_accept(4)，客户端加载该页会 load fail。

数据源（均为仓库内/客户端解包，只读）：
  src/test/resources/quest/retail-data-driven-drift.tsv          —— ADOPTED 名单
  src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv —— 客户端页索引（由 5.8 客户端 HTML 生成）
"""
import collections

DRIFT = "src/test/resources/quest/retail-data-driven-drift.tsv"
CONTRACT = "src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv"

adopted = []
for line in open(DRIFT, encoding="utf-8"):
    if line.startswith("#") or not line.strip():
        continue
    parts = line.rstrip("\n").split("\t")
    if len(parts) >= 2 and parts[1] == "ADOPTED":
        adopted.append(int(parts[0]))

pages = collections.defaultdict(set)
names = collections.defaultdict(dict)
for line in open(CONTRACT, encoding="utf-8"):
    if line.startswith("#") or not line.strip():
        continue
    qid, pid, pname = line.rstrip("\n").split("\t")
    pages[int(qid)].add(int(pid))
    names[int(qid)][int(pid)] = pname

has_contract = [q for q in adopted if q in pages]
missing4 = [q for q in has_contract if 4 not in pages[q]]
has4 = [q for q in has_contract if 4 in pages[q]]
has4762 = [q for q in missing4 if 4762 in pages[q]]
no_contract = [q for q in adopted if q not in pages]

print(f"ADOPTED 任务数 = {len(adopted)}")
print(f"  有客户端页索引 = {len(has_contract)}；无索引 = {len(no_contract)}")
print(f"  客户页含 ask_quest_accept(4) = {len(has4)}")
print(f"  客户页缺 4 = {len(missing4)}，其中含 select_none(4762) = {len(has4762)}")
print()
print("缺页 4 且含 select_none 的任务（按 id）：")
print("  " + ", ".join(map(str, sorted(has4762))))
print()
print("缺页 4 且不含 select_none 的任务（需逐个看客户端实际入口页）：")
rest = sorted(set(missing4) - set(has4762))
print("  " + ", ".join(map(str, rest)))
