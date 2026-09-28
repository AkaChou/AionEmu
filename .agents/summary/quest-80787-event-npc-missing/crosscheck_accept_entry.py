"""交叉验证：ADOPTED 任务的"接取入口页"→ 解析器规则 vs 迁移前 XML 事实。

B 路（迁移前 XML，逐任务经客户端验证过的形状）：
  ① 展开形：<transition source="unaccepted"> + TALK_TO_NPC action="QUEST_SELECT" → SHOW_QUEST_PAGE page=X
  ② 块形：<npc-start ... start-page="X"/>（缺省 SELECT1）
A 路（候选规则）：客户端页索引 client_dialog_contract.tsv 中按优先级挑页。
"""
import collections
import re
import subprocess

DRIFT = "src/test/resources/quest/retail-data-driven-drift.tsv"
CONTRACT = "src/main/resources/aion/definitions/quest_dialog/client_dialog_contract.tsv"
BASE = "4ede058c0^"
XML_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests/%d.xml"
PAGE_ID = {"SELECT_NONE": 4762, "SELECT_NONE_1": 4763, "SELECT1": 1011, "SELECT1_1": 1012,
           "SHOW_ASK_QUEST_ACCEPT_WINDOW": 4, "DEFAULT_SUCCESS": 10002, "SELECT5": 2375,
           "SELECT2": 1352, "SELECT_QUEST": 10, "QUEST_SUMMARY": 9}

adopted = [int(l.split("\t")[0]) for l in open(DRIFT, encoding="utf-8")
           if not l.startswith("#") and l.strip() and l.rstrip("\n").split("\t")[1].startswith("ADOPTED")]
pages = collections.defaultdict(set)
for l in open(CONTRACT, encoding="utf-8"):
    if l.startswith("#") or not l.strip():
        continue
    q, p, _ = l.rstrip("\n").split("\t")
    pages[int(q)].add(int(p))

def xml_entry(q):
    try:
        text = subprocess.run(["git", "show", f"{BASE}:{XML_DIR % q}"], capture_output=True,
                              text=True, check=True).stdout
    except subprocess.CalledProcessError:
        return None
    for m in re.finditer(r"<transition\b.*?</transition>", text, re.S):
        b = m.group(0)
        if 'source="unaccepted"' in b and 'action="QUEST_SELECT"' in b and 'TALK_TO_NPC' in b:
            sm = re.search(r'type="SHOW_QUEST_PAGE"\s+page="([A-Z0-9_]+)"', b)
            if sm:
                return PAGE_ID.get(sm.group(1), sm.group(1))
    for m in re.finditer(r"<npc-start\b[^>]*>", text):
        b = m.group(0)
        sm = re.search(r'start-page="([A-Z0-9_]+)"', b)
        return PAGE_ID.get(sm.group(1) if sm else "SELECT1", sm.group(1) if sm else "SELECT1")
    return "NO_ACCEPT_EDGE"

def resolve(q, order):
    s = pages.get(q, set())
    for pid in order:
        if pid in s:
            return pid
    return None

for order in ([4, 1011, 4762], [4, 4762, 1011], [4762, 1011, 4], [1011, 4762, 4]):
    stat = collections.Counter()
    bad = []
    for q in adopted:
        b = xml_entry(q)
        if not isinstance(b, int):
            stat[str(b)] += 1
            continue
        a = resolve(q, order)
        if a == b:
            stat["agree"] += 1
        else:
            stat["disagree"] += 1
            bad.append((q, a, b))
    print(f"优先级 {order}: agree={stat['agree']} disagree={stat['disagree']} 其他={ {k:v for k,v in stat.items() if k not in ('agree','disagree')} }")
    if order == [4, 4762, 1011]:
        print("  不一致样本（前 25）：")
        for q, a, b in bad[:25]:
            print(f"    {q}: resolver={a} xml={b} 客户页={sorted(pages.get(q, []))[:8]}")
