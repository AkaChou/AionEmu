#!/usr/bin/env python3
# 普查2：20002(SIMPLE) 检查按钮任务的失败页声明（select6）与按钮页 id；以及 39 对照。
import os, re, collections
RETAIL = "src/main/resources/aion/data/static_data/quest/retail"
HTML_ROOTS = ["/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs"]
FAMILIES = ["Quest_SimpleTalk.xml", "Quest_SimpleCollectItem.xml", "Quest_SimpleUseItem.xml",
            "Quest_SimpleHunt.xml"]
index = {}
for root in HTML_ROOTS:
    for dp, _, files in os.walk(root):
        for fn in files:
            m = re.fullmatch(r'(?i)quest_q(\d+)\.html', fn)
            if m:
                index[int(m.group(1))] = os.path.join(dp, fn)

def qids(p):
    t = open(p, encoding='utf-8').read()
    return sorted({int(m.group(1)) for m in re.finditer(r'<id id="(\d+)">', t)})

def pages(qid):
    f = index.get(qid)
    if not f: return None
    t = open(f, encoding='utf-8', errors='replace').read()
    return {m.group(1): re.findall(r'<Act[^>]*href="([^"]+)"', m.group(2))
            for m in re.finditer(r'<HtmlPage name="([^"]+)"[^>]*>(.*?)</HtmlPage>', t, re.S)}

CHK, SIM = "HACTION_CHECK_USER_HAS_QUEST_ITEM", "HACTION_CHECK_USER_HAS_QUEST_ITEM_SIMPLE"
stat = collections.Counter()
missing_html = []
for fam in FAMILIES:
    for qid in qids(os.path.join(RETAIL, fam)):
        pg = pages(qid)
        if pg is None:
            missing_html.append((fam, qid)); continue
        for name, acts in pg.items():
            kind = "SIMPLE" if SIM in acts else ("PLAIN39" if CHK in acts else None)
            if not kind: continue
            stat[(fam, kind, name, "select6" in pg)] += 1
            # 记录按钮页名（应恒为 select5），以及该任务是否声明失败页 select6
            break

print("family / kind / 按钮页名 / 是否声明select6 / 数量")
for k in sorted(stat):
    print(f"{k[0]:30s} {k[1]:8s} {k[2]:20s} select6={k[3]!s:5s} {stat[k]}")
print("\n无客户端页:", len(missing_html))
