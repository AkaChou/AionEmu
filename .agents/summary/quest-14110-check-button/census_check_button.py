#!/usr/bin/env python3
# 普查：真端六族（+DD）任务中，客户端任务页的"交付检查按钮"编码是 39 还是 20002。
# Census: which encoding (HACTION_CHECK_USER_HAS_QUEST_ITEM=39 / _SIMPLE=20002) the client task page uses.
import os, re, glob, collections

RETAIL = "src/main/resources/aion/data/static_data/quest/retail"
HTML_ROOTS = ["/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs"]
FAMILIES = ["Quest_SimpleTalk.xml", "Quest_SimpleCollectItem.xml", "Quest_SimpleUseItem.xml",
            "Quest_SimpleItemPlay.xml", "Quest_SimpleHunt.xml", "Quest_SimpleSerialHunt.xml",
            "Quest_CombineTask.xml", "data_driven_quest.xml"]

# 建立 任务id -> 客户端 html 路径（大小写不敏感）
index = {}
for root in HTML_ROOTS:
    for dirpath, _, files in os.walk(root):
        for fn in files:
            m = re.fullmatch(r'(?i)quest_q(\d+)\.html', fn)
            if m:
                index[int(m.group(1))] = os.path.join(dirpath, fn)
print(f"客户端任务页索引: {len(index)} 件")

def quest_ids(path):
    text = open(path, encoding='utf-8').read()
    return sorted({int(m.group(1)) for m in re.finditer(r'<id id="(\d+)">', text)})

def pages_of(qid):
    f = index.get(qid)
    if not f:
        return None
    t = open(f, encoding='utf-8', errors='replace').read()
    out = {}
    for m in re.finditer(r'<HtmlPage name="([^"]+)"[^>]*>(.*?)</HtmlPage>', t, re.S):
        acts = re.findall(r'<Act[^>]*href="([^"]+)"', m.group(2))
        out[m.group(1)] = acts
    return out

CHECK = "HACTION_CHECK_USER_HAS_QUEST_ITEM"
SIMPLE = "HACTION_CHECK_USER_HAS_QUEST_ITEM_SIMPLE"

summary = collections.Counter()
detail = collections.defaultdict(list)
for fam in FAMILIES:
    p = os.path.join(RETAIL, fam)
    if not os.path.exists(p):
        continue
    for qid in quest_ids(p):
        pages = pages_of(qid)
        if pages is None:
            summary[(fam, "no-html")] += 1
            continue
        kinds = set()
        for name, acts in pages.items():
            if SIMPLE in acts:
                kinds.add(("SIMPLE", name))
            elif CHECK in acts:
                kinds.add(("PLAIN39", name))
        if not kinds:
            summary[(fam, "no-check-button")] += 1
        else:
            for kind, name in kinds:
                summary[(fam, kind)] += 1
                detail[(fam, kind)].append((qid, name))

print("\n== 各家族统计 ==")
for k in sorted(summary, key=lambda x: (x[0], x[1])):
    print(f"{k[0]:32s} {k[1]:18s} {summary[k]}")

print("\n== SIMPLE(20002) 明细（族, 任务id, 按钮所在页）==")
for (fam, kind), rows in sorted(detail.items()):
    if kind == "SIMPLE":
        print(f"-- {fam}: {len(rows)} 件")
        print("   " + ", ".join(f"{q}@{p}" for q, p in rows[:60]) + (" ..." if len(rows) > 60 else ""))
