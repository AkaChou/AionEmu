#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 P4 第二批固定道具/可选奖励合同基线 TSV（可复算，只读外部数据）。

输出：src/test/resources/quest/quest-item-selectable-retail-contract.tsv
  行集 = 真端 reward_item1_N / selectable_reward_item1_N 全部道具名可映射的生产任务。
  列：
    quest_id
    retail_fixed_items    真端固定道具 id:amount 列表（分号分隔，按 id 排序）
    retail_selectable_ids 真端可选道具 id 列表（逗号分隔，按 id 排序）
道具名映射：生产 item_templates 的 name_desc -> id，真端 client_items name -> id 兜底。
无法全部映射的行不入基线（EVIDENCE_BLOCKED，见台账）。
"""
import os
import re
import xml.etree.ElementTree as ET

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
RETAIL_QUEST_XML = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"
RETAIL_ITEMS_DIR = "/Users/mc/PycharmProjects/unpak/Items_unpacked"
ITEM_DIR = os.path.join(REPO, "src/main/resources/aion/data/static_data/items/item")
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")
OUT = os.path.join(
    REPO, "src/test/resources/quest/quest-item-selectable-retail-contract.tsv")


def build_item_map():
    mapping = {}
    for fn in sorted(os.listdir(ITEM_DIR)):
        if not fn.endswith(".xml"):
            continue
        for _, elem in ET.iterparse(os.path.join(ITEM_DIR, fn), events=("start",)):
            if elem.tag == "item_template":
                nd = elem.get("name_desc")
                iid = elem.get("id")
                if nd and iid and nd not in mapping:
                    mapping[nd] = int(iid)
                elem.clear()
    for fn in sorted(os.listdir(RETAIL_ITEMS_DIR)):
        if not (fn.startswith("client_items") and fn.endswith(".xml")):
            continue
        path = os.path.join(RETAIL_ITEMS_DIR, fn)
        with open(path, "rb") as stream:
            raw = stream.read()
        start = raw.find(b"<?xml")
        if start < 0:
            continue
        root = ET.fromstring(raw[start:])
        for elem in root.iter("client_item"):
            name = iid = None
            for c in elem:
                if c.tag == "id":
                    iid = (c.text or "").strip()
                elif c.tag == "name":
                    name = (c.text or "").strip()
            if name and iid and iid.isdigit() and name not in mapping:
                mapping[name] = int(iid)
    return mapping


def main():
    item_map = build_item_map()
    print(f"item map size: {len(item_map)}")

    retail = {}
    for _, elem in ET.iterparse(RETAIL_QUEST_XML, events=("end",)):
        if elem.tag != "quest":
            continue
        qid = None
        fields = {}
        for child in elem:
            tag = child.tag
            if tag == "id":
                qid = (child.text or "").strip()
            elif re.match(r"^(reward_item1|selectable_reward_item1)_\d+$", tag):
                fields[tag] = (child.text or "").strip()
        if qid is not None and qid.isdigit():
            retail[qid] = fields
        elem.clear()

    rows = 0
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("# Aion 5.8 retail quest fixed/selectable item contract snapshot\n")
        fh.write("# source: Quest_unpacked/quest.xml (regenerate: "
                 ".agents/summary/quest-systemic-goal/build_item_selectable_contract_tsv.py)\n")
        fh.write("# rows: production quests whose retail item names are all mappable; "
                 "rows with unmapped names are EVIDENCE_BLOCKED and excluded.\n")
        fh.write("quest_id\tretail_fixed_items\tretail_selectable_ids\n")
        for qid in sorted(retail, key=int):
            if not os.path.exists(os.path.join(PROD_DIR, qid + ".xml")):
                continue
            fields = retail[qid]
            fixed = []
            selectable = []
            unmapped = False
            for key, val in fields.items():
                if not val:
                    continue
                parts = val.rsplit(" ", 1)
                name = parts[0]
                cnt = int(parts[1]) if len(parts) == 2 else 1
                iid = item_map.get(name)
                if iid is None:
                    unmapped = True
                    break
                if key.startswith("reward_item1_"):
                    fixed.append(f"{iid}:{cnt}")
                else:
                    selectable.append(str(iid))
            if unmapped or (not fixed and not selectable):
                continue
            has_fixed_field = any(k.startswith("reward_item1_") for k in fields)
            fixed_out = ";".join(sorted(fixed)) if has_fixed_field else "RETAIL_UNSET"
            sel_out = ",".join(sorted(set(selectable))) if selectable else ""
            if not sel_out and not has_fixed_field:
                continue
            fh.write(f"{qid}\t{fixed_out}\t{sel_out}\n")
            rows += 1
    print(f"item/selectable contract rows ({rows}) -> {OUT}")


if __name__ == "__main__":
    main()
