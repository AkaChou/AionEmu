"""为 71 个过期活动生成中文对照表（中文名只用仓库内可溯源来源，缺则标注"直译"）。

来源优先级：
1. 活动挂载任务的中文名 —— docs/QUEST_CATALOG.zh-CN.md（由 5.8 中文客户端任务串生成）
2. 活动 NPC 的中文名 —— docs/aion-game-terms-en-zh.md（由客户端 client_strings_npc.xml 生成）
3. 无客户端依据时留空，由人工直译，并在列中标注
"""
import re
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

EVENTS = "src/main/resources/aion/data/static_data/events_config/events_config.xml"
CATALOG = "docs/QUEST_CATALOG.zh-CN.md"
GLOSSARY = "docs/aion-game-terms-en-zh.md"
NPCS = ["src/main/resources/aion/data/static_data/npcs/npc_template_%s.xml" % r for r in
        ("200000_216188", "216189_235748", "235749_247606", "247607_270057",
         "270058_286320", "286321_800030", "800031_834289", "834290_885645")]

NOW = datetime.now(timezone.utc)

# 任务 id -> 中文名
quest_zh = {}
for line in open(CATALOG, encoding="utf-8"):
    if not line.startswith("|"):
        continue
    cols = [c.strip() for c in line.split("|")]
    if len(cols) > 5 and cols[1].isdigit():
        quest_zh[int(cols[1])] = cols[4]

# NPC 英文名 -> 中文名
npc_zh = {}
for line in open(GLOSSARY, encoding="utf-8"):
    if not line.startswith("|"):
        continue
    cols = [c.strip() for c in line.split("|")]
    if len(cols) > 4 and cols[1] and cols[2]:
        npc_zh.setdefault(cols[1], cols[2])

# npc id -> 模板英文名
npc_name = {}
for path in NPCS:
    for m in re.finditer(r'<npc_template\b[^>]*npc_id="(\d+)"[^>]*>', open(path, encoding="utf-8").read()):
        tag = m.group(0)
        nid = int(m.group(1))
        nm = re.search(r'\bname="([^"]*)"', tag)
        npc_name[nid] = nm.group(1) if nm else ""

root = ET.parse(EVENTS).getroot()
active = {n.strip() for n in (root.findtext("active") or "").split(";") if n.strip()}

out = []
for ev in root.iter("event"):
    name = ev.get("name")
    if name not in active:
        continue
    end = datetime.fromisoformat(ev.get("end"))
    if end > NOW:
        continue
    q = ev.find("quests")
    qids = []
    if q is not None:
        for tag in ("startable", "maintainable"):
            for el in q.iter(tag):
                qids += [int(x) for x in (el.text or "").split(";") if x.strip().isdigit()]
    spawns = ev.find("spawns")
    npc_ids = []
    if spawns is not None:
        for s in spawns.iter("spawn"):
            nid = int(s.get("npc_id"))
            if nid not in npc_ids:
                npc_ids.append(nid)
    spots = len(list(spawns.iter("spot"))) if spawns is not None else 0
    out.append((name, datetime.fromisoformat(ev.get("start")), end, spots, qids, npc_ids))

print(f"过期活动 {len(out)} 个\n")
for name, s, e, spots, qids, npc_ids in sorted(out, key=lambda r: (r[2], r[0])):
    qz = [quest_zh[qi] for qi in qids if qi in quest_zh]
    nz = [npc_zh.get(npc_name.get(n, ""), "") for n in npc_ids[:3]]
    nz = [x for x in nz if x]
    print(f"{name}\t{s:%Y-%m-%d}~{e:%Y-%m-%d}\t{spots}点")
    print(f"    任务中文名: {' / '.join(qz[:4]) if qz else '（无任务载荷）'}")
    print(f"    NPC ({','.join(map(str, npc_ids[:3]))}): {' / '.join(nz) if nz else '（无中文名）'}")
