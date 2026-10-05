#!/usr/bin/env python3
# 报告页分型对账：relaySteps>=1 且双页（select2+select5）的任务 = 修复影响面。
# 逐件从客户端 HTML 抓 select2/select5 的按钮动作，验证 select2 非报告页、select5 是报告页。
import re, glob, os, sys

RETAIL = "src/main/resources/aion/data/static_data/quest/retail"
HTML_DIR = "/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs"
FAMILIES = ["Quest_SimpleTalk.xml", "Quest_SimpleItemPlay.xml", "Quest_SimpleUseItem.xml",
            "Quest_SimpleCollectItem.xml", "Quest_SimpleHunt.xml", "Quest_SimpleSerialHunt.xml"]

def relay_steps(path):
    """任务 → talk_npc 数。"""
    text = open(path, encoding='utf-8').read()
    out = {}
    for m in re.finditer(r'<id id="(\d+)">(.*?)</id>', text, re.S):
        qid = int(m.group(1)); body = m.group(2)
        talks = re.findall(r'<talk_npc\d+>', body)
        out[qid] = len(talks)
    return out

def html_pages(qid):
    f = os.path.join(HTML_DIR, f"QUEST_Q{qid}.html")
    if not os.path.exists(f):
        return None
    t = open(f, encoding='utf-8').read()
    pages = {}
    for m in re.finditer(r'<HtmlPage name="([^"]+)">(.*?)</HtmlPage>', t, re.S):
        acts = re.findall(r'href="([^"]+)"', m.group(2))
        pages[m.group(1)] = acts
    return pages

allq = {}
for fam in FAMILIES:
    for qid, steps in relay_steps(os.path.join(RETAIL, fam)).items():
        allq.setdefault(qid, []).append((fam, steps))

print(f"任务总数（六族并集）: {len(allq)}")
multi = {q: v for q, v in allq.items() if len(v) > 1}
if multi:
    print(f"多族重复: {multi}")

# 报告页调用方四族（Talk/CollectItem/UseItem/DD-零步）；影响面判定按"有中继"
affected, safe_double = [], []
for qid, fams in sorted(allq.items()):
    fam, steps = fams[0]
    if fam not in ("Quest_SimpleTalk.xml", "Quest_SimpleUseItem.xml", "Quest_SimpleCollectItem.xml"):
        continue
    pages = html_pages(qid)
    if pages is None:
        continue
    has2, has5 = "select2" in pages, "select5" in pages
    old = 1352 if has2 else (2375 if has5 else None)
    new = (2375 if has5 else None) if (steps >= 1 and has2) else old
    # 新判定：steps>=1 且 has2 → 跳过 select2 取 select5（有）；否则同旧
    if steps >= 1 and has2 and has5:
        affected.append((qid, fam.replace('Quest_Simple','').replace('.xml',''), steps,
                         pages.get("select2"), pages.get("select5")))
    elif steps >= 1 and has2 and not has5:
        safe_double.append((qid, fam, steps, pages.get("select2")))

print(f"\n== 影响面：有中继 + select2&select5 双页（报告页 1352→2375）: {len(affected)} 件 ==")
for qid, fam, steps, a2, a5 in affected:
    print(f"  {qid:>7} [{fam}] steps={steps} select2={a2} select5={a5}")

print(f"\n== 有中继 + 仅 select2（无 select5；新规则跳过 select2 → -1 一步直达）: {len(safe_double)} 件 ==")
for qid, fam, steps, a2 in safe_double:
    print(f"  {qid:>7} [{fam}] steps={steps} select2={a2}")
