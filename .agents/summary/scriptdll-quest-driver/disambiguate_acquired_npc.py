#!/usr/bin/env python3
"""M2-c 批次 2 分析：真端表接取/报告 NPC 名无法唯一解析时如何消歧。

背景（M2-c 批次 2 实测）：`RetailSimpleHuntFamilyGateTest` 的 179 个拒绝里有 145 个是
`acquired_npc_name must resolve exactly one npc`，且错误信息里集合为**空**——
即真端表给的名字在本仓 npc 模板里根本不存在，而不是"多 id 需要选一个"。

对比对象（只读证据）：
  * 真端表 `Quest_SimpleHunt.xml`（`<id id="X">`，任务号是**属性**）的
    acquired_npc_name / reward_npc_name；
  * 本仓库 npc 模板（name_desc / name / name_id → npc_id）；
  * 生产 XML 的 NPC_START / NPC_REPORT / NPC_COMPLETE / TALK_TO_NPC 实际 npc-id
    （**仅用于评测，不进入实现**）。

输出：哨兵名分组 × XML NPC 集合的对应关系，以及名字归一化规则的命中率。
"""
from __future__ import annotations

import collections
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
PROD = REPO / "src/main/resources/aion/data/static_data"
QUESTS = PROD / "quest/definitions/quests"
RETAIL_TABLE = PROD / "quest_retail/Quest_SimpleHunt.xml"
NPCS = PROD / "npcs"

TEMPLATE = re.compile(r"<npc_template\b[^>]*?>")
ATTR = re.compile(r'(\w[\w-]*)="([^"]*)"')

REJECTS_DEFAULT = Path("/tmp/hunt-family5.txt")
REJECT_MARK = "acquired_npc_name must resolve"


def npc_index():
    """name_desc/name/name_id → npc_id 三套索引 + npc_id → 属性。"""
    by_desc: dict[str, set[int]] = collections.defaultdict(set)
    by_name: dict[str, set[int]] = collections.defaultdict(set)
    by_name_id: dict[str, set[int]] = collections.defaultdict(set)
    attrs: dict[int, dict[str, str]] = {}
    for path in sorted(NPCS.glob("npc_template_*.xml")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for match in TEMPLATE.finditer(text):
            values = dict(ATTR.findall(match.group(0)))
            raw = values.get("npc_id")
            if not raw:
                continue
            npc = int(raw)
            attrs[npc] = values
            if values.get("name_desc"):
                by_desc[values["name_desc"].lower()].add(npc)
            if values.get("name"):
                by_name[values["name"].lower()].add(npc)
            if values.get("name_id"):
                by_name_id[values["name_id"]].add(npc)
    return by_desc, by_name, by_name_id, attrs


def spawned() -> set[int]:
    """实际刷出的 npc_id 集合（含 spot 的 spawn 段）。"""
    result: set[int] = set()
    for path in (PROD / "spawns").rglob("*.xml"):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for match in re.finditer(r'<spawn npc_id="(\d+)"[^>]*>(.*?)</spawn>', text, re.S):
            if "<spot" in match.group(2):
                result.add(int(match.group(1)))
    return result


def retail_rows() -> dict[int, dict[str, str]]:
    """真端表行：任务号取 `<id id="X">` 的**属性**（不是子元素）。"""
    rows: dict[int, dict[str, str]] = {}
    root = ET.parse(RETAIL_TABLE).getroot()
    for entry in root.iter("id"):
        raw = entry.get("id")
        if not raw:
            continue
        rows[int(raw)] = {
            "acquired": (entry.findtext("acquired_npc_name") or "").strip(),
            "reward": (entry.findtext("reward_npc_name") or "").strip(),
        }
    return rows


def xml_npc_roles(quest_id: int) -> dict[str, list[int]]:
    """生产 XML 里各类对话块的 npc-id（仅评测用）。"""
    path = QUESTS / f"{quest_id}.xml"
    if not path.exists():
        return {}
    text = path.read_text(encoding="utf-8")
    roles: dict[str, list[int]] = {}
    for tag, label in (("NPC_START", "start"), ("NPC_REPORT", "report"),
                       ("NPC_COMPLETE", "complete"), ("TALK_TO_NPC", "talk")):
        pattern = rf'<dialog type="{tag}"[^>]*npc-id="(\d+)"'
        found = sorted({int(x) for x in re.findall(pattern, text)})
        if found:
            roles[label] = found
    return roles


def rejected_quests(path: Path) -> list[int]:
    return [int(line.split("\t")[0]) for line in path.read_text().splitlines()
            if line[:1].isdigit() and REJECT_MARK in line]


def main() -> int:
    rejects_path = Path(sys.argv[1]) if len(sys.argv) > 1 else REJECTS_DEFAULT
    by_desc, by_name, by_name_id, attrs = npc_index()
    live = spawned()
    retail = retail_rows()
    rejects = rejected_quests(rejects_path)
    print(f"rejects={len(rejects)}  retail_rows={len(retail)}  live_npcs={len(live)}")
    print(f"index: name_desc={len(by_desc)} name={len(by_name)} name_id={len(by_name_id)}")

    groups: dict[str, list[int]] = collections.defaultdict(list)
    detail = []
    for quest_id in rejects:
        row = retail.get(quest_id, {})
        acquired = row.get("acquired", "<NO-ROW>")
        reward = row.get("reward", "")
        groups[acquired].append(quest_id)
        roles = xml_npc_roles(quest_id)
        detail.append((quest_id, acquired, reward, roles))

    print("\n# 接取名分组（哨兵名占绝大多数）")
    for name, ids in sorted(groups.items(), key=lambda kv: -len(kv[1])):
        head = ",".join(str(i) for i in ids[:6])
        print(f"  {len(ids):4d}  {name!r:20s} e.g. {head}")

    sentinel = re.compile(r"^_.*_$")
    print("\n# 哨兵分组 → 生产 XML 的 NPC 角色集合（评测：能否用单张哨兵表驱动）")
    for name, ids in sorted(groups.items(), key=lambda kv: -len(kv[1])):
        if not sentinel.match(name):
            continue
        per_role: dict[str, collections.Counter] = collections.defaultdict(collections.Counter)
        missing = 0
        for quest_id in ids:
            roles = xml_npc_roles(quest_id)
            if not roles:
                missing += 1
                continue
            for role in ("start", "report", "complete", "talk"):
                per_role[role][tuple(roles.get(role, []))] += 1
        print(f"  [{name}] quests={len(ids)} 无任何对话块={missing}")
        for role, counter in per_role.items():
            for combo, count in counter.most_common(5):
                print(f"      {role:9s} {count:4d}× {list(combo)}")
        print(f"      并集={sorted({n for q in ids for n in xml_npc_roles(q).get('start', [])})}  (start)")

    print("\n# 非哨兵样本明细")
    for quest_id, acquired, reward, roles in detail:
        if sentinel.match(acquired):
            continue
        desc = by_desc.get(acquired.lower(), set())
        named = by_name.get(acquired.lower(), set())
        prefixed = by_desc.get(("npc_" + acquired).lower(), set())
        print(f"  {quest_id}  acquired={acquired!r} reward={reward!r}")
        print(f"      name_desc={sorted(desc)} name={sorted(named)} 'NPC_'+name_desc={sorted(prefixed)}")
        print(f"      xml={roles}")

    print("\n# 规则命中率（仅对能给出 XML 接取 id 的任务）")
    hits = collections.Counter()
    for quest_id, acquired, reward, roles in detail:
        target = (roles.get("start") or roles.get("talk") or roles.get("report") or [])
        if sentinel.match(acquired) or len(target) != 1:
            continue
        npc = target[0]
        hits["total"] += 1
        template = attrs.get(npc, {})
        hits["name_desc相等"] += template.get("name_desc", "").lower() == acquired.lower()
        hits["name相等"] += template.get("name", "").lower() == acquired.lower()
        hits["去掉NPC_前缀后相等"] += template.get("name_desc", "").lower().removeprefix("npc_") == acquired.lower()
        hits["去掉NPC_前缀后name相等"] += template.get("name", "").lower().removeprefix("npc_") == acquired.lower()
        hits["npc在live"] += npc in live
    print(" ", dict(hits))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
