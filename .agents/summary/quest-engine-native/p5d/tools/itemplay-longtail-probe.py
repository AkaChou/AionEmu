#!/usr/bin/env python3
"""SimpleItemPlay 长尾逐行事实探针（只读真端表 + 本仓退役/保留清单）。

输出每一行的：questId / use_item / give_item / talk_npcK / give_itemK / remove_itemK /
con_quest / item_check / cutsceneid1 / cs1_haction，并标注退役集与 XML 保留集归属。
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[5]
TABLE = ROOT / "src/main/resources/aion/data/static_data/quest/retail/Quest_SimpleItemPlay.xml"
RETIRED = ROOT / "src/main/java/com/aionemu/gameserver/questEngine/definition/RetiredQuestIds.java"
RETENTION = ROOT / "src/test/resources/quest/retail-xml-retention.tsv"

COLUMNS = ["use_item_name", "give_item", "talk_npc1", "talk_npc2", "give_item1", "give_item2",
           "remove_item1", "remove_item2", "con_quest", "item_check", "cutsceneid1", "cs1_haction"]


def retired_ids() -> set[int]:
    text = RETIRED.read_text(encoding="utf-8")
    return {int(m) for m in re.findall(r"\b(\d{3,6})\b", text.split("contains")[0] + text)}


def retention_rows() -> dict[int, str]:
    out = {}
    for line in RETENTION.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        cells = line.split("\t")
        if len(cells) >= 2 and cells[0].isdigit():
            out[int(cells[0])] = cells[1] if len(cells) > 1 else ""
    return out


def main() -> int:
    root = ET.parse(TABLE).getroot()
    rows = [e for e in root if e.tag == "id"]
    retired = retired_ids()
    retention = retention_rows()
    long_tail, routed_like = [], []
    for e in rows:
        qid = int(e.get("id"))
        vals = {}
        for col in COLUMNS:
            el = e.find(col)
            vals[col] = (el.text or "").strip() if el is not None and (el.text or "").strip() else ""
        declares = [c for c in COLUMNS if vals[c]]
        tail = any(vals[c] for c in ("talk_npc1", "talk_npc2", "give_item1", "give_item2",
                                     "remove_item1", "remove_item2", "cutsceneid1", "cs1_haction")) \
            or vals["item_check"] == "1"
        rec = (qid, declares, vals, qid in retired, retention.get(qid, ""))
        (long_tail if tail else routed_like).append(rec)

    print(f"# rows={len(rows)} long_tail={len(long_tail)} plain={len(routed_like)}")
    print("# questId\tretired\tretention\tdeclared-长尾列\tuse_item\tgive_item")
    for group, label in ((routed_like, "PLAIN"), (long_tail, "LONGTAIL")):
        for qid, declares, vals, ret, keep in group:
            tail_cols = [c for c in declares if c not in ("use_item_name", "give_item")]
            print(f"{label}\t{qid}\t{'R' if ret else '-'}\t{keep or '-'}\t{'|'.join(tail_cols) or '-'}\t"
                  f"{vals['use_item_name'] or '-'}\t{vals['give_item'] or '-'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
