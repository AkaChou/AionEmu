#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-56 物品轴普查：`ACQUIRE_NPC_UNRESOLVED` 21 行的六源形状（只读，产出 TSV）。

六源 = ①分类 dump（桶成员）②真端 DD 表（接取类别/参数、进度类别、交付名）
③真端 quest.xml（works/reqs/drops/removes/交付名/category1）④遗留 XML（接取形见证：
`<use-item>` 目标、无主 `QUEST_ACTION`、接取段 NPC、节点集、奖励段 NPC）
⑤客户端登记（入口页 / 任务书行 / 信件页）⑥物品名索引（符号 → 物品 id）。

Inventory census (read-only) over the six sources; emits one TSV row per quest.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
SUMMARY = REPO / ".agents/summary/scriptdll-quest-driver"
RETAIL = REPO / "src/main/resources/aion/data/static_data/quest_retail"
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"

OUT = SUMMARY / "p0c56-item-acquire-census.tsv"
CLASSIFICATION = SUMMARY / "p0c56-classification-now.tsv"
BUCKET = "REJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED"


def bucket_ids() -> list[int]:
    ids = []
    for line in CLASSIFICATION.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        parts = line.split("\t")
        if parts[1] == BUCKET:
            ids.append(int(parts[0]))
    return sorted(ids)


def dd_entries() -> dict[int, dict[str, object]]:
    text = (RETAIL / "data_driven_quest.xml").read_text(encoding="utf-8")
    out: dict[int, dict[str, object]] = {}
    for block in re.findall(r"<quest_data_driven>(.*?)</quest_data_driven>", text, re.S):
        def one(name: str) -> str | None:
            m = re.search(rf"<{name}>(.*?)</{name}>", block, re.S)
            return m.group(1).strip() if m else None

        progress = [
            (one_of(data, "category_progress_"), one_of(data, "value0_progress_"))
            for data in re.findall(r"<data>(.*?)</data>", block, re.S)
        ]
        out[int(one("id"))] = {
            "acquire_category": one("category_acquire_"),
            "acquire_param": one("value0_acquire_"),
            "acquire_param2": one("value1_acquire_"),
            "reward_npc": one("reward_npc_name"),
            "progress": [p for p in progress if p[0]],
        }
    return out


def one_of(block: str, name: str) -> str | None:
    m = re.search(rf"<{name}>(.*?)</{name}>", block, re.S)
    return m.group(1).strip() if m else None


def quest_xml_entries() -> dict[int, dict[str, object]]:
    text = (RETAIL / "quest.xml").read_text(encoding="utf-8")
    out: dict[int, dict[str, object]] = {}
    for block in re.findall(r"\t<quest>(.*?)\t</quest>", text, re.S):
        def one(name: str) -> str | None:
            m = re.search(rf"<{name}>(.*?)</{name}>", block, re.S)
            return m.group(1).strip() if m else None

        def all_of(name: str) -> list[str]:
            return [m.strip() for m in re.findall(rf"<{name}>(.*?)</{name}>", block, re.S)]

        qid = one("id")
        if qid is None:
            continue
        out[int(qid)] = {
            "work_items": [v for v in all_of("quest_work_item[0-9]+") if v],
            "check_items": [v for v in all_of("check_item[0-9_]+") if v and v != "none"],
            "collect_items": [v for v in all_of("collect_item[0-9_]+") if v and v != "none"],
            "drops": [v for v in all_of("drop_monster[0-9_]+") if v and v != "none"],
            "remove_items": [v for v in all_of("remove_item[0-9_]+") if v and v != "none"],
            "reward_npc": one("reward_npc_name"),
            "category1": one("category1"),
            "max_repeat": one("max_repeat_count"),
        }
    return out


def legacy_shape(quest_id: int) -> dict[str, object]:
    path = QUESTS / f"{quest_id}.xml"
    if not path.exists():
        return {"present": False}
    text = path.read_text(encoding="utf-8")
    use_items = re.findall(r"<use-item item-id=\"(\d+)\"", text)
    targetless = re.findall(r"<dialog type=\"QUEST_ACTION\" action=\"([A-Z_0-9]+)\"", text)
    npc_starts = re.findall(
        r"<dialog type=\"NPC_START\" npc-id=\"(\d+)\"[^>]*start-page=\"([A-Z_0-9]+)\"", text)
    transition_blocks = re.findall(r"<transition source=\"([a-zA-Z0-9_]+)\" target=\"([a-zA-Z0-9_]+)\">(.*?)</transition>", text, re.S)
    accept_npcs: set[str] = set()
    accept_actions: list[str] = []
    for source, target, body in transition_blocks:
        if source != "unaccepted":
            continue
        npcs = re.findall(r"<dialog type=\"TALK_TO_NPC\" npc-id=\"(\d+)\"", body)
        actions = re.findall(r"action[s]?=\"([^\"]+)\"", body)
        if npcs:
            accept_npcs.update(npcs)
            accept_actions.extend(actions)
    reward_npcs = sorted({n for n in re.findall(r"<dialog type=\"TALK_TO_NPC\" npc-id=\"(\d+)\"", text)})
    nodes = re.findall(r"<node label=\"([a-zA-Z0-9_]+)\" status=\"([A-Z]+)\"", text)
    return {
        "present": True,
        "use_items": use_items,
        "npc_starts": [f"{npc}:{page}" for npc, page in npc_starts],
        "targetless_actions": sorted(set(targetless)),
        "accept_npcs": sorted(accept_npcs),
        "accept_actions": sorted(set(accept_actions)),
        "npc_ids": reward_npcs,
        "nodes": [f"{label}:{status}" for label, status in nodes],
        "has_started_node": any(label == "started" for label, _ in nodes),
    }


def tsv_index(name: str) -> list[list[str]]:
    rows = []
    for line in (RETAIL / name).read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        rows.append(line.split("\t"))
    return rows


def item_lookup() -> dict[str, str]:
    out = {}
    for line in (SUMMARY / "item_name_index.tsv").read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        parts = line.split("\t")
        out[parts[0]] = parts[1]
    return out


def client_registries() -> dict[str, dict[int, str]]:
    out = {}
    for name in ("quest_client_entry_pages.tsv", "quest_client_summary_rows.tsv",
                 "quest_client_talk_pages.tsv", "quest_client_reward_npcs.tsv"):
        table: dict[int, str] = {}
        for parts in tsv_index(name):
            table[int(parts[0])] = "|".join(parts[1:])
        out[name] = table
    return out


def main() -> int:
    ids = bucket_ids()
    dd = dd_entries()
    meta = quest_xml_entries()
    items = item_lookup()
    client = client_registries()
    lines = ["# P0c-56 物品轴普查（ACQUIRE_NPC_UNRESOLVED 21 行 × 六源）",
             "# 生成：p0c56_item_acquire_census.py（只读）",
             "\t".join(("quest_id", "acquire_cat", "acquire_param", "param2", "item_symbol", "item_id",
                        "progress", "dd_reward", "meta_reward", "work_items", "check_items", "collect_items",
                        "drops", "remove_items", "category1", "max_repeat", "xml_use_items",
                        "xml_npc_starts",
                        "xml_targetless", "xml_accept_npcs", "xml_accept_actions", "xml_npc_ids", "xml_nodes",
                        "entry_page", "summary_rows", "talk_pages", "client_reward_npcs"))]
    for quest_id in ids:
        row = dd.get(quest_id, {})
        meta_row = meta.get(quest_id, {})
        xml = legacy_shape(quest_id)
        param = row.get("acquire_param") or ""
        symbol = param.strip().split()[0].lower() if param else ""
        item_id = items.get(symbol, "")
        progress = ";".join(f"{cat}:{val}" for cat, val in row.get("progress", [])) or "-"
        lines.append("\t".join((
            str(quest_id),
            str(row.get("acquire_category")),
            str(param),
            str(row.get("acquire_param2") or "-"),
            symbol or "-",
            item_id or "-",
            progress,
            str(row.get("reward_npc")),
            str(meta_row.get("reward_npc") or "-"),
            ",".join(meta_row.get("work_items", [])) or "-",
            ",".join(meta_row.get("check_items", [])) or "-",
            ",".join(meta_row.get("collect_items", [])) or "-",
            ",".join(meta_row.get("drops", [])) or "-",
            ",".join(meta_row.get("remove_items", [])) or "-",
            str(meta_row.get("category1") or "-"),
            str(meta_row.get("max_repeat") or "-"),
            ",".join(xml.get("use_items", [])) or "-",
            ",".join(xml.get("npc_starts", [])) or "-",
            ",".join(xml.get("targetless_actions", [])) or "-",
            ",".join(xml.get("accept_npcs", [])) or "-",
            ",".join(xml.get("accept_actions", [])) or "-",
            ",".join(xml.get("npc_ids", [])) or "-",
            ",".join(xml.get("nodes", [])) or "-",
            client["quest_client_entry_pages.tsv"].get(quest_id, "-"),
            client["quest_client_summary_rows.tsv"].get(quest_id, "-"),
            client["quest_client_talk_pages.tsv"].get(quest_id, "-"),
            client["quest_client_reward_npcs.tsv"].get(quest_id, "-"),
        )))
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {OUT} rows={len(ids)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
