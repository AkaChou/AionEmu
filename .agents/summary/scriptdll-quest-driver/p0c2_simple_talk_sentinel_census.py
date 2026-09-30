#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-2 普查：真端 Quest_SimpleTalk.xml 的类别哨兵行能否被系统发放形状承接。

判据（每行都要成立才可迁移）：
  1. 行在"生产宇宙"内（catalog 或 保留清单 owner=RETAIL_TABLE）；
  2. 报告 NPC 名唯一可解析（合成器的 REWARD 前置条件）；
  3. 无 talk_npcN 链、无 give/remove 物品轴、无 cutsceneid1（否则仍被拒绝）；
  4. 真端 quest.xml 有 npcfaction_name 且能在合成器的 NPC_FACTIONS 映射里落地（阵营轮换键）；
  5. 生产 npc_factions_quest.xml 的星期位（全 0 = 真端不发放）。

输出：p0c2-simple-talk-sentinel-census.tsv
"""
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

import io
import os
import re
import sys

ROOT = f"{REPO}"
RETAIL = os.path.join(ROOT, "src/main/resources/aion/data/static_data/quest_retail")
NPC_DIR = os.path.join(ROOT, "src/main/resources/aion/data/static_data/npcs")
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "p0c2-simple-talk-sentinel-census.tsv")

FIELD_TAGS = ["acquired_npc_name", "reward_npc_name", "talk_npc1", "talk_npc2", "talk_npc3",
              "give_item", "give_item1", "remove_item1", "item_check", "cutsceneid1", "con_quest"]


def text_of(block, tag):
    m = re.search(r"<%s>(.*?)</%s>" % (tag, tag), block, re.S)
    if not m:
        return None
    v = m.group(1).strip()
    return v or None


def read(path):
    return io.open(path, encoding="utf-8", errors="replace").read()


def parse_simple_talk():
    s = read(os.path.join(RETAIL, "Quest_SimpleTalk.xml"))
    rows = {}
    for m in re.finditer(r'<id\s+id="(\d+)"[^>]*>(.*?)</id>', s, re.S):
        qid = int(m.group(1))
        block = m.group(2)
        rows[qid] = {tag: text_of(block, tag) for tag in FIELD_TAGS}
    return rows


def parse_retail_quests():
    """真端 quest.xml → {id: {npcfaction_name: ...}}（只抽需要字段，文件 11MB）。"""
    s = read(os.path.join(RETAIL, "quest.xml"))
    out = {}
    for m in re.finditer(r"<quest(?:\s[^>]*)?>\s*(.*?)</quest>", s, re.S):
        block = m.group(1)
        qid = re.search(r"<id>(\d+)</id>", block)
        if not qid:
            continue
        out[int(qid.group(1))] = {
            "npcfaction_name": text_of(block, "npcfaction_name"),
        }
    return out


def npc_name_index():
    """name_desc → npc_id 集合（含 NPC_ 前缀别名，与 RetailNpcNameIndex 同口径）。"""
    idx = {}
    for name in sorted(os.listdir(NPC_DIR)):
        if not name.startswith("npc_template_") or not name.endswith(".xml"):
            continue
        s = read(os.path.join(NPC_DIR, name))
        for m in re.finditer(r"<npc_template\b[^>]*>", s):
            tag = m.group(0)
            ids = re.search(r'npc_id="(\d+)"', tag)
            desc = re.search(r'name_desc="([^"]*)"', tag)
            if not ids or not desc:
                continue
            key = desc.group(1).strip().lower()
            idx.setdefault(key, set()).add(int(ids.group(1)))
            if key.startswith("npc_"):
                idx.setdefault(key[4:], set()).add(int(ids.group(1)))
    return idx


def resolve(index, name):
    if not name:
        return set()
    raw = name.strip()
    if raw.isdigit():
        return {int(raw)}
    return index.get(raw.lower(), set())


def universe():
    s = read(os.path.join(ROOT, "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"))
    ids = {int(x) for x in re.findall(r'<definition id="(\d+)"', s)}
    retention = os.path.join(RETAIL, "retail-xml-retention.tsv")
    for line in io.open(retention, encoding="utf-8", errors="replace"):
        if line.startswith("#") or not line.strip():
            continue
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 2 and parts[1] == "RETAIL_TABLE":
            ids.add(int(parts[0]))
    return ids


def weekday_masks():
    s = read(os.path.join(ROOT, "src/main/resources/aion/data/static_data/npc_factions/npc_factions_quest.xml"))
    out = {}
    for m in re.finditer(r"<npc_faction_quest\b[^>]*/>", s):
        tag = m.group(0)
        qid = int(re.search(r'quest_id="(\d+)"', tag).group(1))
        mask = "".join(re.search(r'%s="(\d)"' % d, tag).group(1) for d in
                       ("mon", "tue", "wed", "thu", "fri", "sat", "sun"))
        out[qid] = mask
    return out


def npc_factions_map():
    """从 RetailQuestMetadataCompiler 抽 NPC_FACTIONS 映射（真相只有一份，避免复制漂移）。"""
    s = read(os.path.join(ROOT, "src/main/java/com/aionemu/gameserver/questEngine/retail/RetailQuestMetadataCompiler.java"))
    block = re.search(r"NPC_FACTIONS = Map\.ofEntries\((.*?)\);", s, re.S).group(1)
    return {k: int(v) for k, v in re.findall(r'Map\.entry\("([^"]+)",\s*(\d+)\)', block)}


def drift_registry():
    out = {}
    for line in io.open(os.path.join(ROOT, "src/test/resources/quest/retail-simple-talk-drift.tsv"),
                        encoding="utf-8", errors="replace"):
        if line.startswith("#") or not line.strip():
            continue
        parts = line.rstrip("\n").split("\t")
        out[int(parts[0])] = parts[1]
    return out


def sentinel_of(name):
    if not name:
        return None
    raw = name.strip()
    if len(raw) > 2 and raw.startswith("_") and raw.endswith("_"):
        return raw[1:-1].lower()
    return None


def main():
    talk = parse_simple_talk()
    retail = parse_retail_quests()
    index = npc_name_index()
    world_ids = universe()
    masks = weekday_masks()
    factions = npc_factions_map()
    drift = drift_registry()

    rows = []
    for qid, row in sorted(talk.items()):
        sentinel = sentinel_of(row["acquired_npc_name"])
        if sentinel is None:
            continue
        reward_ids = resolve(index, row["reward_npc_name"])
        faction_name = (retail.get(qid) or {}).get("npcfaction_name")
        rows.append({
            "quest_id": qid,
            "sentinel": sentinel,
            "in_universe": "1" if qid in world_ids else "0",
            "reward_npc_name": row["reward_npc_name"] or "",
            "reward_ids": ",".join(str(x) for x in sorted(reward_ids)) or "-",
            "talk_chain": ",".join(row[t] for t in ("talk_npc1", "talk_npc2", "talk_npc3") if row[t]) or "-",
            "item_axes": ("give" if (row["give_item"] or row["give_item1"]) else "")
                          + ("remove" if row["remove_item1"] else "") or "-",
            "item_check": row["item_check"] or "0",
            "cutscene": row["cutsceneid1"] or "-",
            "npcfaction_name": faction_name or "-",
            "faction_id": str(factions.get((faction_name or "").strip(), 0)),
            "weekday_mask": masks.get(qid, "-"),
            "drift": drift.get(qid, "-"),
        })

    cols = ["quest_id", "sentinel", "in_universe", "reward_npc_name", "reward_ids", "talk_chain",
            "item_axes", "item_check", "cutscene", "npcfaction_name", "faction_id", "weekday_mask", "drift"]
    with io.open(OUT, "w", encoding="utf-8") as fh:
        fh.write("# P0c-2 SimpleTalk 类别哨兵普查（生成脚本 p0c2_simple_talk_sentinel_census.py）\n")
        fh.write("\t".join(cols) + "\n")
        for r in rows:
            fh.write("\t".join(str(r[c]) for c in cols) + "\n")

    # ---------------- 汇总 ----------------
    def count(pred, subset=None):
        return sum(1 for r in (subset if subset is not None else rows) if pred(r))

    print("哨兵行总数:", len(rows))
    for sentinel in sorted({r["sentinel"] for r in rows}):
        subset = [r for r in rows if r["sentinel"] == sentinel]
        print("\n== %s: %d 行 ==" % (sentinel, len(subset)))
        print("  宇宙内:", count(lambda r: r["in_universe"] == "1", subset))
        print("  报告 NPC 唯一:", count(lambda r: "," in r["reward_ids"] or r["reward_ids"] != "-"
                                       and "AMBIG" not in r["reward_ids"] and len(r["reward_ids"].split(",")) == 1, subset))
        print("  报告 NPC 未解析:", count(lambda r: r["reward_ids"] == "-", subset))
        print("  报告 NPC 多解:", count(lambda r: len(r["reward_ids"].split(",")) > 1 and r["reward_ids"] != "-", subset))
        print("  有 talk 链:", count(lambda r: r["talk_chain"] != "-", subset))
        print("  有物品轴:", count(lambda r: r["item_axes"] != "-", subset))
        print("  item_check=1:", count(lambda r: r["item_check"] not in ("0", "-", ""), subset))
        print("  有过场:", count(lambda r: r["cutscene"] != "-", subset))
        print("  真端有 npcfaction_name:", count(lambda r: r["npcfaction_name"] != "-", subset))
        print("  NPC_FACTIONS 落地:", count(lambda r: r["faction_id"] != "0", subset))
        print("  星期位全 0:", count(lambda r: set(r["weekday_mask"]) == {"0"}, subset))
        print("  星期位未登记:", count(lambda r: r["weekday_mask"] == "-", subset))
        in_world = [r for r in subset if r["in_universe"] == "1"]
        print("  [宇宙内] 干净行(无链/无物品/无过场/报告唯一):",
              count(lambda r: r["talk_chain"] == "-" and r["item_axes"] == "-" and r["cutscene"] == "-"
                    and r["reward_ids"] not in ("-",) and "," not in r["reward_ids"], in_world))
        print("  [宇宙内] 漂移分布:", {c: count(lambda r: r["drift"] == c, in_world)
                                    for c in sorted({r["drift"] for r in in_world})})
    print("\n输出:", OUT)


if __name__ == "__main__":
    sys.exit(main())
