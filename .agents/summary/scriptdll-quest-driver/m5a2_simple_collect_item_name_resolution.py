#!/usr/bin/env python3
"""M5-a2：SimpleCollectItem 名称解析勘察（NPC / 对象名 → 本服 id）。

对象名（如 `LF1_Cherubim_pouch`）在本服就是 `npc_template`（`tribe=FIELD_OBJECT_*`），
因此与接取/报告 NPC 共用同一张 `name_desc → npc_id` 索引。

输出：retail-simple-collect-item-name-resolution.tsv（逐任务：接取/报告 NPC、对象、解析结果与拒绝码）。
"""
from __future__ import annotations

import pathlib
import re
from collections import Counter, defaultdict

REPO = pathlib.Path(__file__).resolve().parents[3]
SD = REPO / "src/main/resources/aion/data/static_data"
RETAIL = SD / "quest_retail/Quest_SimpleCollectItem.xml"
RETENTION = SD / "quest_retail/retail-xml-retention.tsv"
OUT = pathlib.Path(__file__).resolve().parent / "retail-simple-collect-item-name-resolution.tsv"


def name_index():
    names: dict[str, set[int]] = defaultdict(set)
    for path in (SD / "npcs").glob("npc_template_*.xml"):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for tag in re.findall(r"<npc_template\b[^>]*>", text):
            name = re.search(r'name_desc="([^"]*)"', tag)
            npc_id = re.search(r'npc_id="(\d+)"', tag)
            if name and npc_id:
                names[name.group(1).lower()].add(int(npc_id.group(1)))
    return names


def retail_rows():
    text = RETAIL.read_text(encoding="utf-8")
    rows = {}
    for quest_id, body in re.findall(r'<id id="(\d+)">(.*?)</id>', text, re.DOTALL):
        fields = {k: v.strip() for k, v in re.findall(r"<(\w+)>(.*?)</\1>", body, re.DOTALL)}
        rows[int(quest_id)] = fields
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


def classify(names, raw):
    ids = names.get((raw or "").lower(), set())
    if len(ids) == 1:
        return "OK", next(iter(ids))
    if not ids:
        kind = "_SENTINEL" if raw and raw.startswith("_") and raw.endswith("_") else "_UNRESOLVED"
        return kind, ""
    return "_AMBIGUOUS", ",".join(str(x) for x in sorted(ids))


def main() -> int:
    names = name_index()
    rows = retail_rows()
    ids = family_ids()
    stats = Counter()
    lines = ["# SimpleCollectItem 名称解析（quest_id, verdict, rejection, acquired, acquired_id, reward, "
             "reward_id, objects, object_ids）"]
    for quest_id in ids:
        row = rows.get(quest_id)
        if row is None:
            stats["NO_RETAIL_ROW"] += 1
            lines.append(f"{quest_id}\tNO_RETAIL_ROW\tNO_RETAIL_ROW\t\t\t\t\t\t")
            continue
        acquired = row.get("acquired_npc_name", "")
        reward = row.get("reward_npc_name", "")
        objects = [value for key, value in sorted(row.items()) if key.startswith("object")]
        acquired_kind, acquired_id = classify(names, acquired)
        reward_kind, reward_id = classify(names, reward)
        object_kinds = [classify(names, obj) for obj in objects]
        rejection = ""
        if acquired_kind != "OK":
            rejection = "RETAIL_ACQUIRE_NPC" + acquired_kind
        elif reward_kind != "OK":
            rejection = "RETAIL_REWARD_NPC" + reward_kind
        elif any(kind != "OK" for kind, _ in object_kinds):
            rejection = "RETAIL_COLLECT_OBJECT" + next(kind for kind, _ in object_kinds if kind != "OK")
        verdict = "DRIVABLE" if not rejection else "REJECTED"
        stats[verdict] += 1
        stats[rejection or "OK"] += 1
        lines.append("\t".join([
            str(quest_id), verdict, rejection, acquired, str(acquired_id), reward, str(reward_id),
            ",".join(objects), ",".join(str(v) for _, v in object_kinds)]))
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"family={len(ids)} -> {OUT.name}")
    print("stats:", dict(stats))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
