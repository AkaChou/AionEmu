#!/usr/bin/env python3
# W3 异径独立复核（只读）：不依赖 Java 守卫，直接用数据文件复算物件梯的四条判据。
#   ① 物件 NPC 730032 的对话面键集（源:动作 id）
#   ② 入口记录页下发 == SELECT1；中转记录页下发 == SHOW_ASK_QUEST_ACCEPT_WINDOW
#   ③ 入口自环载荷 == 真端 give_item（work item 首项 × 符号计数）
#   ④ 提交记录（QUEST_ACCEPT*，unaccepted→started）存在且条件为 START_ELIGIBLE
import re, sys
D = "src/main/resources/aion/data/static_data/quest_retail/"
ACT = {"USE_OBJECT": -1, "QUEST_SELECT": 31, "ASK_QUEST_ACCEPT": 1007, "QUEST_ACCEPT_1": 1002,
       "QUEST_ACCEPT_SIMPLE": 20000, "QUEST_REFUSE_1": 1003, "QUEST_REFUSE_2": 1004,
       "QUEST_REFUSE_SIMPLE": 20001, "FINISH_DIALOG": 1008}
QID, NPC = 1323, 730032
rows = [l.rstrip("\n").split("\t") for l in open(D + "quest_client_talk_chain_steps.tsv", encoding="utf-8")
        if not l.startswith("#") and l.strip()]
face = [(r[5], r[4]) for r in rows if int(r[0]) == QID and r[1] == "R" and int(r[3]) == NPC]
keys = sorted({(s, ACT.get(a, a)) for s, a in face})
expect = sorted({("started", 1008), ("unaccepted", -1), ("unaccepted", 1002), ("unaccepted", 1003),
                 ("unaccepted", 1007), ("unaccepted", 1008)})
print("① 物件对话面键集:", keys)
print("   与编译期合同相等:", keys == expect)
entry = [r for r in rows if int(r[0]) == QID and r[1] == "R" and int(r[3]) == NPC and r[4] == "USE_OBJECT"][0]
relay = [r for r in rows if int(r[0]) == QID and r[1] == "R" and int(r[3]) == NPC and r[4] == "ASK_QUEST_ACCEPT"][0]
pag = lambda r: sorted(t.split(":")[-1] for t in r[9].split(";") if t.startswith("DIALOG:SHOW_QUEST_PAGE:"))
print("② 入口页:", pag(entry), "== ['SELECT1']:", pag(entry) == ["SELECT1"])
print("   中转页:", pag(relay), "== ['SHOW_ASK_QUEST_ACCEPT_WINDOW']:", pag(relay) == ["SHOW_ASK_QUEST_ACCEPT_WINDOW"])
xml = open(D + "Quest_SimpleTalk.xml", encoding="utf-8").read()
m = re.search(r'<id id="%d">(.*?)</id>' % QID, xml, re.S)
give = re.search(r"<give_item>(.*?)</give_item>", m.group(1)).group(1).strip()
qdata = open("src/main/resources/aion/data/static_data/quest_data/quest_data.xml", encoding="utf-8").read()
wq = re.search(r'<quest id="%d".*?</quest>' % QID, qdata, re.S).group(0)
item = re.search(r'<quest_work_item item_id="(\d+)"', wq).group(1)
count = give.split()[1] if len(give.split()) > 1 else "1"
want = "GIVE_ITEM:%s:%s" % (item, count)
print("③ 真端 give_item:", give, "→ work item", item, "; 入口自环载荷:", entry[8], "; 相等:", entry[8] == want)
commits = [r for r in rows if int(r[0]) == QID and r[1] == "R" and int(r[3]) == NPC
           and r[4].startswith("QUEST_ACCEPT") and r[5] == "unaccepted" and r[6] == "started"]
print("④ 提交记录:", [(c[4], c[7]) for c in commits], "; 恰一条且 START_ELIGIBLE:",
      len(commits) == 1 and commits[0][7] == "START_ELIGIBLE")
ok = keys == expect and pag(entry) == ["SELECT1"] and pag(relay) == ["SHOW_ASK_QUEST_ACCEPT_WINDOW"] \
     and entry[8] == want and len(commits) == 1 and commits[0][7] == "START_ELIGIBLE"
print("INDEPENDENT_CHECK", "PASS" if ok else "FAIL")
sys.exit(0 if ok else 1)
