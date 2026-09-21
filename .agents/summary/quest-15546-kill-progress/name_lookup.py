#!/usr/bin/env python3
"""按 NPC 模板名查询模板 ID 与静态刷新位置。"""
from __future__ import annotations
import re, sys, pathlib, collections

ROOT = pathlib.Path(__file__).resolve().parents[3]
SD = ROOT / "src/main/resources/aion/data/static_data"
names: dict[str, list[int]] = collections.defaultdict(list)
for f in (SD / "npcs").glob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    for m in re.finditer(r'npc_template\b[^>]*?npc_id="(\d+)"[^>]*?>', t):
        n = re.search(r'name_desc="([^"]*)"', m.group(0))
        if n:
            names[n.group(1).lower()].append(int(m.group(1)))
spawned: dict[int, set[str]] = collections.defaultdict(set)
for f in (SD / "spawns").rglob("*.xml"):
    t = f.read_text(errors="ignore", encoding="utf-8")
    for x in re.findall(r'npc_id="(\d+)"', t):
        spawned[int(x)].add(f.stem)

for want in sys.argv[1:]:
    key = want.lower()
    ids = names.get(key, [])
    if not ids:
        # 允许前缀匹配（客户端名与模板名大小写/后缀略有差异）
        ids = sorted({i for n, v in names.items() if n.startswith(key) for i in v})
    print(f"{want}: " + ", ".join(f"{i}{sorted(spawned[i]) if i in spawned else 'UNSPAWNED'}" for i in ids))
