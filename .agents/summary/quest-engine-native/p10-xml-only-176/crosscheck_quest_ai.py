#!/usr/bin/env python3
"""交叉核对：真端 npcs.xml 的 quest_ai_name ↔ ScriptDLL IOneQuestScriptNpc 注册 ↔ 本仓 XML 的 NPC 引用。

用法：python3 crosscheck_quest_ai.py <quest_id> [<quest_id> ...]
"""
import io
import re
import sys
from pathlib import Path

DLL = Path("/Users/mc/IdeaProjects/58Server/server58/MainServer_ScriptDLL64/ScriptDLL64.c")
NPCS = Path("/Users/mc/IdeaProjects/58Server/Map/XML/npcs.xml")
XMLDIR = Path("/Users/mc/IdeaProjects/AionEmu-test/src/main/resources/aion/data/static_data/quest/definitions/quests")


def parse_registry():
    src = DLL.read_text(encoding="latin-1", errors="replace")
    by_id = {}
    for call in re.findall(r"FUN_180cb5920\(([^;\n]*)\);", src):
        parts = [p.strip() for p in call.rsplit(",", 2)]
        if len(parts) != 3:
            continue
        name_m = re.search(r'L"([^"]+)"', parts[1])
        name = name_m.group(1) if name_m else parts[1]
        last = parts[2]
        value = int(last, 16) if last.startswith("0x") else int(last)
        by_id.setdefault(value, set()).add(name)
    return by_id


def parse_npcs():
    data = NPCS.read_bytes()
    txt = data.decode("utf-16") if data[:2] in (b"\xff\xfe", b"\xfe\xff") else data.decode("utf-8", "replace")
    lines = txt.splitlines()
    cur = None
    npc = {}
    for line in lines:
        s = line.strip()
        if s.startswith("<id>") and s.endswith("</id>"):
            cur = int(s[4:-5])
            npc.setdefault(cur, {})
        elif cur is not None and s.startswith("<name>") and s.endswith("</name>"):
            npc[cur]["name"] = s[6:-7]
        elif cur is not None and s.startswith("<quest_ai_name>") and s.endswith("</quest_ai_name>"):
            npc[cur]["quest_ai"] = s[15:-16]
    return npc


def main():
    ids = [int(x) for x in sys.argv[1:]]
    registry = parse_registry()
    npcs = parse_npcs()
    by_name = {}
    for npc_id, info in npcs.items():
        if "quest_ai" in info:
            by_name.setdefault(info["quest_ai"], []).append(npc_id)
    for quest_id in ids:
        xml = XMLDIR / f"{quest_id}.xml"
        npc_refs = []
        if xml.exists():
            npc_refs = sorted({int(m) for m in re.findall(r'npc-id="(\d+)"', xml.read_text(encoding="utf-8"))})
        names = sorted(registry.get(quest_id, set()))
        print(f"=== quest {quest_id} ===")
        print(f"  ScriptDLL 注册名: {names}")
        print(f"  本仓 XML NPC 引用: {npc_refs}")
        for npc_id in npc_refs:
            info = npcs.get(npc_id, {})
            print(f"    npc {npc_id}: name={info.get('name')} quest_ai_name={info.get('quest_ai')} "
                  f"→ 注册命中: {info.get('quest_ai') in names}")
        # 反向：注册名对应哪些 npc
        for nm in names:
            print(f"    注册名 {nm} → npc 候选 {by_name.get(nm, [])[:6]}")


if __name__ == "__main__":
    main()
