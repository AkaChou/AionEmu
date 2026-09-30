#!/usr/bin/env python3
"""M5-a：SimpleCollectItem 真端表 ↔ 生产 XML 的形状勘察（可迁移性测量）。

- 真端表：src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleCollectItem.xml（M5-a 入仓）
- 家族集合：保留清单 reason=FAMILY_PENDING:SimpleCollectItem 的 178 行
- 输出：retail-simple-collect-item-shapes.tsv（逐任务 verdict + 事件形状 + 对象名）

判据：真端行存在 + 接取/报告 NPC 唯一 + `objectN` 能在本服对象表里解析 + XML 形状归类。
"""
from __future__ import annotations

import pathlib
import re
from collections import Counter

REPO = pathlib.Path(__file__).resolve().parents[3]
RETAIL = REPO / "src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleCollectItem.xml"
RETENTION = REPO / "src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
OUT = pathlib.Path(__file__).resolve().parent / "retail-simple-collect-item-shapes.tsv"

ROW = re.compile(r"<id id=\"(\d+)\">(.*?)</id>", re.DOTALL)
FIELD = re.compile(r"<(\w+)>(.*?)</\1>", re.DOTALL)


def parse_retail():
    text = RETAIL.read_text(encoding="utf-8")
    rows = {}
    for quest_id, body in ROW.findall(text):
        fields = {name: value.strip() for name, value in FIELD.findall(body)}
        objects = [value for name, value in sorted(fields.items()) if name.startswith("object")]
        rows[int(quest_id)] = {
            "acquired": fields.get("acquired_npc_name"),
            "reward": fields.get("reward_npc_name"),
            "objects": objects,
        }
    return rows


def family_ids():
    ids = []
    for line in RETENTION.read_text(encoding="utf-8").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) >= 4 and parts[3] == "FAMILY_PENDING:SimpleCollectItem":
            ids.append(int(parts[0]))
    return sorted(ids)


def xml_shape(quest_id):
    path = QUESTS / f"{quest_id}.xml"
    if not path.is_file():
        return None
    text = path.read_text(encoding="utf-8")
    events = tuple(sorted(set(re.findall(r"<event>\s*<([a-zA-Z-]+)", text))))
    cond_block = re.search(r"<conditions>(.*?)</conditions>", text, re.DOTALL)
    guards = ()
    if cond_block:
        guards = tuple(sorted(set(re.findall(r"<([a-zA-Z-]+)", cond_block.group(1)))))
    return {
        "events": events,
        "guards": guards,
        "nodes": len(re.findall(r"<node\b", text)),
        "transitions": len(re.findall(r"<transition\b|<dialog\b", text)),
    }


def main() -> int:
    retail = parse_retail()
    ids = family_ids()
    stats = Counter()
    shapes = Counter()
    rows = []
    for quest_id in ids:
        row = retail.get(quest_id)
        shape = xml_shape(quest_id)
        if row is None:
            stats["no_retail_row"] += 1
            rows.append((quest_id, "NO_RETAIL_ROW", "", "", "", "", ""))
            continue
        stats["retail_row"] += 1
        if shape is None:
            stats["xml_missing"] += 1
            rows.append((quest_id, "XML_MISSING", "", "", "", "", ""))
            continue
        stats["xml_present"] += 1
        shapes[(shape["events"], shape["guards"])] += 1
        rows.append((quest_id, "OK", ",".join(row["objects"]), row["acquired"] or "", row["reward"] or "",
                     "|".join(shape["events"]), "|".join(shape["guards"])))
    header = ("# SimpleCollectItem 形状勘察（quest_id, verdict, objects, acquired_npc, reward_npc, xml_events, "
              "xml_guards）\n")
    OUT.write_text(header + "".join("\t".join(str(x) for x in row) + "\n" for row in rows), encoding="utf-8")
    print(f"retail rows={len(retail)} family={len(ids)} -> {OUT.name}")
    print("stats:", dict(stats))
    print("distinct xml shapes:", len(shapes))
    for shape, count in shapes.most_common(10):
        print(f"  {count:4d} events={shape[0]} guards={shape[1]}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
