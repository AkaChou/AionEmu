"""静态审计：events_config.xml 的 <active> 名单里，有哪些活动当前处于时间窗内。

静默只读；用于评估 gameserver.event.service.enable 开关的影响面。
"""
import re
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

PATH = "src/main/resources/aion/data/static_data/events_config/events_config.xml"
NOW = datetime.now(timezone.utc)

tree = ET.parse(PATH)
root = tree.getroot()
active_text = (root.findtext("active") or "").strip()
active = {n.strip() for n in active_text.split(";") if n.strip()}

rows = []
for event in root.iter("event"):
    name = event.get("name")
    start = datetime.fromisoformat(event.get("start"))
    end = datetime.fromisoformat(event.get("end"))
    in_window = start <= NOW < end
    listed = name in active
    if listed:
        spawn_npcs = []
        spawns = event.find("spawns")
        if spawns is not None:
            spawn_npcs = [int(s.get("npc_id")) for s in spawns.iter("spawn")]
        rows.append((name, start.date().isoformat(), end.date().isoformat(), in_window,
                     len(spawn_npcs), 833671 in spawn_npcs))

listed_rows = rows
print(f"NOW(UTC) = {NOW:%Y-%m-%d %H:%M}")
print(f"<active> 名单条目 = {len(active)}；匹配到模板的活动 = {len(listed_rows)}")
print()
live = [r for r in listed_rows if r[3]]
expired = [r for r in listed_rows if not r[3]]
print(f"当前时间窗内（会被 EventService 启动）= {len(live)}")
for name, s, e, _, n, has80787npc in live:
    mark = "  <== 80787 家族 NPC" if has80787npc else ""
    print(f"  {name:38s} {s} ~ {e}  spawn_npc={n}{mark}")
print()
print(f"时间窗已过（仅保留在名单、不会启动）= {len(expired)}")
for name, s, e, _, n, has80787npc in expired:
    mark = "  <== 80787 家族 NPC" if has80787npc else ""
    print(f"  {name:38s} {s} ~ {e}  spawn_npc={n}{mark}")

missing = active - {r[0] for r in listed_rows}
if missing:
    print()
    print("名单中但 events_config.xml 无同名模板（无效果）：")
    for name in sorted(missing):
        print("  " + name)
