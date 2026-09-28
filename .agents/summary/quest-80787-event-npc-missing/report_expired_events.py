"""列出 <active> 名单中当前不在时间窗内的活动，并区分"已过期"与"未开始"。

只读静态审计；数据源 src/main/resources/aion/data/static_data/events_config/events_config.xml。
"""
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

PATH = "src/main/resources/aion/data/static_data/events_config/events_config.xml"
NOW = datetime.now(timezone.utc)

root = ET.parse(PATH).getroot()
active = {n.strip() for n in (root.findtext("active") or "").split(";") if n.strip()}

expired, future = [], []
for ev in root.iter("event"):
    name = ev.get("name")
    if name not in active:
        continue
    start = datetime.fromisoformat(ev.get("start"))
    end = datetime.fromisoformat(ev.get("end"))
    spawns = ev.find("spawns")
    npc_ids = [s.get("npc_id") for s in spawns.iter("spawn")] if spawns is not None else []
    spots = sum(len(s.findall("spot")) for s in spawns.iter("spawn")) if spawns is not None else 0
    row = (name, start, end, len(npc_ids), spots, ev.find("quests") is not None,
           ev.find("inventory_drop") is not None, ev.find("event_drops") is not None,
           ev.find("surveys") is not None)
    if end <= NOW:
        expired.append(row)
    elif start > NOW:
        future.append(row)

def dump(title, rows):
    print(f"== {title}（{len(rows)}） ==")
    print(f"{'活动名':34s} {'时间窗':25s} {'刷怪':>10s}  其他载荷")
    for name, s, e, npcs, spots, q, inv, drop, survey in sorted(rows, key=lambda r: (r[2], r[0])):
        extra = []
        if q: extra.append("quests")
        if inv: extra.append("inventory_drop")
        if drop: extra.append("event_drops")
        if survey: extra.append("surveys")
        print(f"{name:34s} {s:%Y-%m-%d}~{e:%Y-%m-%d}  {npcs:>3d}模板/{spots:>3d}点  " + ",".join(extra))
    print()

print(f"NOW(UTC) = {NOW:%Y-%m-%d %H:%M}\n")
dump("已过期（end <= now）", expired)
dump("尚未开始（start > now）", future)
