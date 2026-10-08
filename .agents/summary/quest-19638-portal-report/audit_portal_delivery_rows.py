#!/usr/bin/env python3
"""审计：交付 NPC（reward_npc_name）为 portal_dialog 传送门 NPC 的 DataDriven 行。

Audit: DataDriven rows whose delivery npc (reward_npc_name) is a portal_dialog portal npc.

背景 / Why: 传送门类 AI（PortalDialogAI2 / Specialize01PortalAI2）的开门页判定只读
QuestEngine 的 NPC 注册表（getQuestNpc(npcId).getOnTalkEvent()）；这些行的交付面只有
在进表时才可达。2026-10-08 实机 19638/799022 即此类（见同目录 README.zh-CN.md）。

口径与边界 / Method and limits:
  - name 解析只做 name ∪ name_desc 精确唯一（与 NativeNpcNameResolver 的 fail-closed 口径一致，
    但不含别名表 retail-npc-name-aliases.xml 与名组表 retail-quest-ai-name-groups.xml）：
    MISSING 行不代表无缺口，PORTAL 命中可靠（单命中且落在 portal_dialog 集合）。
  - specialize_portal（Specialize01PortalAI2）同读该注册表，本脚本未纳入其 NPC 集合。

用法 / Usage: python3 .agents/summary/quest-19638-portal-report/audit_portal_delivery_rows.py
"""

import collections
import glob
import re

ROOT = "."
DD_TABLE = ROOT + "/src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml"
PORTAL_XML = ROOT + "/src/main/resources/aion/data/static_data/portals/portal_template2.xml"
NPC_SHARDS = ROOT + "/src/main/resources/aion/data/static_data/npcs/npc_template_*.xml"


def portal_dialog_npc_ids():
    text = open(PORTAL_XML, encoding="utf-8").read()
    return {int(m) for m in re.findall(r'<portal_dialog npc_id="(\d+)"', text)}


def npc_name_index():
    names = collections.defaultdict(set)
    tag = re.compile(r"<npc_template\b([^>]*)>")
    attrs = re.compile(r'\b(npc_id|name|name_desc)="([^"]*)"')
    for path in glob.glob(NPC_SHARDS):
        for match in tag.finditer(open(path, encoding="utf-8", errors="ignore").read()):
            found = dict(attrs.findall(match.group(1)))
            npc_id = found.get("npc_id")
            if not npc_id:
                continue
            for key in (found.get("name_desc"), found.get("name")):
                if key:
                    names[key.strip().lower()].add(int(npc_id))
    return names


def rows():
    text = open(DD_TABLE, encoding="utf-8").read()
    for block in re.finditer(r"<quest_data_driven>(.*?)</quest_data_driven>", text, re.S):
        body = block.group(1)
        qid = re.search(r"<id>(\d+)</id>", body)
        reward = re.search(r"<reward_npc_name>([^<]*)</reward_npc_name>", body)
        acquire = re.search(r"<category_acquire_>([^<]*)</category_acquire_>", body)
        if qid:
            yield (int(qid.group(1)),
                   reward.group(1).strip() if reward else "",
                   acquire.group(1).strip() if acquire else "")


def main():
    portals = portal_dialog_npc_ids()
    names = npc_name_index()
    seen = collections.Counter()
    hits = []
    for quest_id, reward, acquire in rows():
        if not reward:
            continue
        ids = names.get(reward.lower())
        if ids is None:
            hits.append((quest_id, reward, acquire, "MISSING", []))
            continue
        portal_ids = sorted(ids & portals)
        if portal_ids:
            hits.append((quest_id, reward, acquire, "PORTAL", sorted(ids)))
    print("portal_dialog npc:", len(portals))
    for hit in hits:
        key = (hit[1], tuple(hit[4]))
        if hit[3] == "PORTAL" or seen[key] == 0:
            print("  quest %-6d reward=%-40s acquire=%-10s %s %s" % hit)
        seen[key] += 1
    portal_rows = [h for h in hits if h[3] == "PORTAL"]
    print("PORTAL rows:", len(portal_rows), "MISSING rows:",
          len([h for h in hits if h[3] == "MISSING"]))


if __name__ == "__main__":
    main()
