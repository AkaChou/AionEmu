#!/usr/bin/env python3
"""QE-023 门禁的静态等价实现：全库前置有向图自环/环路 + 悬空引用检查（只读）。"""
from __future__ import annotations
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

QUESTS = Path("src/main/resources/aion/data/static_data/quest/definitions/quests")

def graph():
    have = {int(p.stem) for p in QUESTS.glob("*.xml") if p.stem.isdigit()}
    edges: dict[int, list[int]] = {}
    dangling = []
    for p in QUESTS.glob("*.xml"):
        if not p.stem.isdigit():
            continue
        qid = int(p.stem)
        root = ET.parse(p).getroot()
        reqs = [int(x.get("id")) for x in root.findall("./metadata/prerequisites/quest") if (x.get("id") or "").isdigit()]
        reqs += [int(c.get("quest-id")) for c in root.findall("./metadata/start-conditions/condition")
                 if (c.get("type") or "").lower() == "finished" and (c.get("quest-id") or "").isdigit()]
        for g in root.findall("./metadata/start-condition-groups/group"):
            reqs += [int(c.get("quest-id")) for c in g.findall("condition")
                     if (c.get("type") or "").lower() == "finished" and (c.get("quest-id") or "").isdigit()]
        edges[qid] = reqs
        for r in reqs:
            if r not in have:
                dangling.append((qid, r))
            if r == qid:
                dangling.append((qid, "SELF"))
    return have, edges, dangling

def main():
    have, edges, dangling = graph()
    state: dict[int, int] = {}
    cycles: list[list[int]] = []
    def dfs(cur, path):
        state[cur] = 1
        path.append(cur)
        for nxt in edges.get(cur, []):
            if nxt not in have:
                continue
            st = state.get(nxt, 0)
            if st == 1:
                i = path.index(nxt)
                cycles.append(path[i:] + [nxt])
            elif st == 0:
                dfs(nxt, path)
        path.pop()
        state[cur] = 2
    for q in sorted(have):
        if state.get(q, 0) == 0:
            dfs(q, [])
    print(f"quests={len(have)} self_or_dangling={len(dangling)} cycles={len(cycles)}")
    for d in dangling[:20]:
        print("  dangling/self:", d)
    for c in cycles[:10]:
        print("  cycle:", c)
    return 1 if cycles or dangling else 0

if __name__ == "__main__":
    sys.exit(main())
