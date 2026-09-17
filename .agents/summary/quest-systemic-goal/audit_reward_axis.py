#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P4：奖励结算（reward axis）第一档全库审计（只读）。

比对维度（真端 quest.xml 档位 1 vs 生产 metadata rewards）：
- reward_exp1            <-> <reward kind="EXP">
- reward_gold1           <-> <reward kind="GOLD">
- reward_abyss_point1    <-> <reward kind="AP">
- reward_glory_point1    <-> <reward kind="GLORY">
- reward_title1          <-> <reward kind="TITLE">
- reward_item1_N         <-> <reward kind="ITEM">（真端值为 "<name_desc> <count>"，
                             经 items_template 的 name_desc -> id 映射）
- selectable_reward_item1_N <-> <reward kind="SELECTABLE_ITEM">

输出：
  reward-axis-audit.tsv  逐任务差异行（仅列存在差异的任务）
  stdout                 分类计数与 EXP 比值分布（判断是否存在系统性倍率）
"""
import os
import re
import xml.etree.ElementTree as ET

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
RETAIL_QUEST_XML = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"
ITEM_DIR = os.path.join(REPO, "src/main/resources/aion/data/static_data/items/item")
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "reward-axis-audit.tsv")


RETAIL_ITEMS_DIR = "/Users/mc/PycharmProjects/unpak/Items_unpacked"


def build_item_map():
    """name_desc -> id（生产 item_templates 权威映射），真端 client_items name -> id 兜底。"""
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
    fallback = 0
    for fn in sorted(os.listdir(RETAIL_ITEMS_DIR)):
        if not (fn.startswith("client_items") and fn.endswith(".xml")):
            continue
        path = os.path.join(RETAIL_ITEMS_DIR, fn)
        try:
            with open(path, "rb") as stream:
                raw = stream.read()
        except OSError:
            continue
        ascii_start = raw.find(b"<?xml")
        if ascii_start < 0:
            continue
        root = ET.fromstring(raw[ascii_start:])
        for elem in root.iter("client_item"):
            name = None
            iid = None
            for c in elem:
                if c.tag == "id":
                    iid = (c.text or "").strip()
                elif c.tag == "name":
                    name = (c.text or "").strip()
            if name and iid and iid.isdigit() and name not in mapping:
                mapping[name] = int(iid)
                fallback += 1
    print(f"retail client_items fallback entries: {fallback}")
    return mapping


def parse_retail_rewards():
    out = {}
    for _, elem in ET.iterparse(RETAIL_QUEST_XML, events=("end",)):
        if elem.tag != "quest":
            continue
        qid = None
        fields = {}
        for child in elem:
            tag = child.tag
            if tag == "id":
                qid = (child.text or "").strip()
            elif re.match(r"^(reward_exp1|reward_gold1|reward_abyss_point1|"
                          r"reward_glory_point1|reward_title1)$", tag):
                fields[tag] = (child.text or "0").strip()
            elif re.match(r"^(reward_item1|selectable_reward_item1)_\d+$", tag):
                fields[tag] = (child.text or "").strip()
        if qid is not None and qid.isdigit():
            out[qid] = fields
        elem.clear()
    return out


def parse_prod_rewards():
    out = {}
    for fn in sorted(os.listdir(PROD_DIR)):
        if not fn.endswith(".xml"):
            continue
        qid = fn[:-4]
        try:
            meta = ET.parse(os.path.join(PROD_DIR, fn)).getroot().find("metadata")
        except ET.ParseError:
            continue
        rewards = {}
        items = []
        selectable = []
        # 档位 1 = 平铺 <rewards>；多档任务取第一个 <group>（档位 1），
        # 与真端 reward_*1 字段对应，其余档位对应真端 _2/_3 字段（本批不做）。
        groups = meta.findall("./reward-groups/group")
        reward_nodes = meta.findall("./rewards/reward") if not groups \
            else groups[0].findall("reward")
        for r in reward_nodes:
            kind = r.get("kind")
            if kind == "ITEM":
                items.append((int(r.get("id")), int(r.get("amount"))))
            elif kind == "SELECTABLE_ITEM":
                selectable.append((int(r.get("id")), int(r.get("amount"))))
            else:
                rewards[kind] = int(r.get("amount"))
        out[qid] = (rewards, items, selectable)
    return out


def main():
    print("building item name_desc->id map ...")
    item_map = build_item_map()
    print(f"item map size: {len(item_map)}")

    retail = parse_retail_rewards()
    prod = parse_prod_rewards()
    print(f"retail={len(retail)} prod={len(prod)}")

    exp_ratios = []
    rows = []
    counts = {}

    def note(cat, qid, detail):
        rows.append((qid, cat, detail))
        counts[cat] = counts.get(cat, 0) + 1

    for qid in sorted(prod, key=int):
        r = retail.get(qid)
        if r is None:
            continue
        prewards, pitems, pselect = prod[qid]
        def retail_number(key):
            # 字段缺失 = 真端未配置（生产自建奖励属服务端设计，不比对）；字段存在才具权威
            if key not in r:
                return None
            return int(r[key] or 0)

        def numeric_diff(key, prod_value, category):
            rv = retail_number(key)
            if rv is None:
                return
            if rv != prod_value:
                note(category, qid, f"prod={prod_value} retail={rv}")

        # EXP
        rexpv = retail_number("reward_exp1")
        pexp = prewards.get("EXP", 0)
        if rexpv is not None:
            if rexpv and pexp and pexp != rexpv:
                ratio = pexp / rexpv
                exp_ratios.append((qid, ratio))
                note("EXP_DIFF", qid, f"prod={pexp} retail={rexpv} ratio={ratio:.3f}")
            elif rexpv and not pexp:
                note("EXP_MISSING_PROD", qid, f"retail={rexpv}")
            elif pexp and rexpv == 0:
                note("EXP_ZERO_RETAIL", qid, f"prod={pexp}")
        numeric_diff("reward_gold1", prewards.get("GOLD", 0), "GOLD_DIFF")
        numeric_diff("reward_abyss_point1", prewards.get("AP", 0), "AP_DIFF")
        numeric_diff("reward_glory_point1", prewards.get("GP", 0), "GLORY_DIFF")
        # Title：真端 reward_title1 为名称字符串（如 light_title04），本机解包数据无
        # title 模板表可映射到数字 id，整轴 EVIDENCE_BLOCKED，不做逐任务比对。
        rtitle = r.get("reward_title1", "")
        ptitle = prewards.get("TITLE", 0)
        if rtitle and not ptitle:
            note("TITLE_EVIDENCE_BLOCKED", qid, f"retail={rtitle} prod_title_absent")
        elif rtitle and ptitle:
            note("TITLE_EVIDENCE_BLOCKED", qid, f"retail={rtitle} prod={ptitle}")
        # fixed items
        ritems = []
        unmapped = []
        for key, val in r.items():
            m = re.match(r"^reward_item1_(\d+)$", key)
            if not m or not val:
                continue
            parts = val.rsplit(" ", 1)
            name = parts[0]
            cnt = int(parts[1]) if len(parts) == 2 else 1
            iid = item_map.get(name)
            if iid is None:
                unmapped.append(name)
            else:
                ritems.append((iid, cnt))
        ritems.sort()
        if unmapped:
            note("ITEM_UNMAPPED", qid, "names=" + ",".join(sorted(set(unmapped))))
        elif sorted(pitems) != ritems:
            note("ITEM_DIFF", qid,
                 f"prod={sorted(pitems)} retail={sorted(ritems)}")
        # selectable count comparison (per-slot count of items)
        rsel = []
        for key, val in r.items():
            m = re.match(r"^selectable_reward_item1_(\d+)$", key)
            if not m or not val:
                continue
            parts = val.rsplit(" ", 1)
            name = parts[0]
            cnt = int(parts[1]) if len(parts) == 2 else 1
            iid = item_map.get(name)
            if iid is not None:
                rsel.append((iid, cnt))
        rsel.sort()
        if rsel and sorted(pselect) != rsel:
            note("SELECTABLE_DIFF", qid,
                 f"prod_n={len(pselect)} retail_n={len(rsel)}")

    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("quest_id\tcategory\tdetail\n")
        for row in rows:
            fh.write("\t".join(row) + "\n")
    print("category counts:", counts)

    if exp_ratios:
        from collections import Counter
        buckets = Counter(round(r, 2) for _, r in exp_ratios)
        print("EXP ratio distribution (top 12):", buckets.most_common(12))


if __name__ == "__main__":
    main()
