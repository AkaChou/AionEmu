#!/usr/bin/env python3
"""普查退役 XML 语料中 SETPROn 推进行的 after-commit 形态（close-dialog vs 回页 10）。

Census of SETPROn advance after-commits in the retired quest XML corpus:
close-dialog vs SHOW_SELECTION_PAGE(page 10).
"""
import re, subprocess, sys
from collections import Counter

REF = "4ede058c0~1"
BASE = "src/main/resources/aion/data/static_data/quest_definition/quests/"

names = subprocess.run(["git", "ls-tree", "-r", "--name-only", REF, BASE],
                       capture_output=True, text=True, check=True).stdout.split()
rows = []
for path in names:
    if not path.endswith(".xml"):
        continue
    qid = re.search(r"/(\d+)\.xml$", path).group(1)
    text = subprocess.run(["git", "show", f"{REF}:{path}"],
                          capture_output=True, text=True, check=True).stdout
    for m in re.finditer(r"<transition\b.*?</transition>", text, re.S):
        block = m.group(0)
        if not re.search(r'action="SETPRO\d+"', block):
            continue
        after = re.search(r"<after-commit>(.*?)</after-commit>", block, re.S)
        tail = after.group(1) if after else ""
        ev = re.search(r'action="(SETPRO\d+)"', block).group(1)
        npc = re.search(r'npc-id="(\d+)"', block)
        kinds = []
        if "close-dialog" in tail:
            kinds.append("close-dialog")
        for p in re.findall(r'page="([A-Z0-9_]+)"', tail):
            kinds.append(p)
        if "sync-quest-state" in tail:
            kinds.append("sync")
        rows.append((qid, ev, npc.group(1) if npc else "?", "+".join(kinds) or "EMPTY"))

c = Counter(r[3] for r in rows)
print("total SETPRO transitions:", len(rows))
for k, v in c.most_common():
    print(f"  {v:5d}  {k}")
print("\n-- rows with page tail (page 10 family) --")
for r in rows:
    if "SELECT" in r[3] and "close-dialog" not in r[3]:
        print("  ", r)
print("\n-- close-dialog rows --")
cd = [r for r in rows if "close-dialog" in r[3]]
print("  count:", len(cd))
for r in cd[:40]:
    print("  ", r)
