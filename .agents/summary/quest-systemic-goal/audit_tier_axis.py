#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P4 第三批：多档奖励（档位 2/3）与扩展奖励审计（只读）。

真端档位字段：reward_{exp,gold,abyss_point,glory_point}{1,2,3}、
reward_item{1,2,3}_N、selectable_reward_item{1,2,3}_N。
生产档位容器：<reward-groups><group>...</group></reward-groups> 的第 t 个 <group>
对应真端档位 t；无 reward-groups 的任务只有档位 1。
扩展奖励：reward_item_ext_1（名称 数量）vs metadata <extended-rewards>。

输出分类：
  TIER2/3_NUMERIC_DIFF   档位数值不一致
  TIER2/3_ITEM_DIFF      档位道具不一致
  TIER_MISSING_PROD      真端有档位字段、生产组数不足
  TIER_EXTRA_PROD        生产组数多于真端档位字段数（需逐一定性）
  EXT_DIFF               扩展奖励不一致
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
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "tier-axis-audit.tsv")

NUMERIC_FIELDS = ["reward_exp", "reward_gold", "reward_abyss_point", "reward_glory_point"]

AP_RELIC_EXCEPTION_QIDS = {
    "11279", "11280", "11281", "11282", "11283", "11284", "11285", "11286",
    "21281", "21282", "21283", "21284", "21285", "21286", "21287", "21288",
    "18849", "18850", "28849", "28850",
}



def build_item_map():
    mapping = {}
    for fn in sorted(os.listdir(ITEM_DIR)):
        if fn.endswith(".xml"):
            for _, elem in ET.iterparse(os.path.join(ITEM_DIR, fn), events=("start",)):
                if elem.tag == "item_template":
                    nd = elem.get("name_desc")
                    iid = elem.get("id")
                    if nd and iid and nd not in mapping:
                        mapping[nd] = int(iid)
                    elem.clear()
    for fn in sorted(os.listdir(RETAIL_ITEMS_DIR)):
        if fn.startswith("client_items") and fn.endswith(".xml"):
            with open(os.path.join(RETAIL_ITEMS_DIR, fn), "rb") as stream:
                raw = stream.read()
            start = raw.find(b"<?xml")
            if start < 0:
                continue
            for elem in ET.fromstring(raw[start:]).iter("client_item"):
                name = iid = None
                for c in elem:
                    if c.tag == "id":
                        iid = (c.text or "").strip()
                    elif c.tag == "name":
                        name = (c.text or "").strip()
                if name and iid and iid.isdigit() and name not in mapping:
                    mapping[name] = int(iid)
    return mapping


def parse_retail():
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
            elif re.match(r"^(reward_(exp|gold|abyss_point|glory_point)[123]|"
                          r"reward_item[123]_\d+|selectable_reward_item[123]_\d+|"
                          r"reward_item_ext_1|reward_gold_ext|reward_title_ext|"
                          r"selectable_reward_item_ext_\d+)$", tag):
                fields[tag] = (child.text or "").strip()
        if qid is not None and qid.isdigit():
            out[qid] = fields
        elem.clear()
    return out


def parse_prod():
    out = {}
    for fn in sorted(os.listdir(PROD_DIR)):
        if not fn.endswith(".xml"):
            continue
        qid = fn[:-4]
        try:
            meta = ET.parse(os.path.join(PROD_DIR, fn)).getroot().find("metadata")
        except ET.ParseError:
            continue
        groups = []
        for g in meta.findall("./reward-groups/group"):
            rows = []
            for r in g.findall("reward"):
                rows.append((r.get("kind"), int(r.get("id")), int(r.get("amount"))))
            groups.append(rows)
        if not groups:
            rows = []
            for r in meta.findall("./rewards/reward"):
                rows.append((r.get("kind"), int(r.get("id")), int(r.get("amount"))))
            groups.append(rows)
        ext = [(r.get("kind"), int(r.get("id")), int(r.get("amount")))
               for r in meta.findall("./extended-rewards/reward")]
        if not ext:
            # 多档 extended 形态：组 1 即最后一轮追加奖励
            for g in meta.findall("./extended-reward-groups/group"):
                ext.extend((r.get("kind"), int(r.get("id")), int(r.get("amount")))
                           for r in g.findall("reward"))
        out[qid] = (groups, ext)
    return out


def main():
    item_map = build_item_map()
    retail = parse_retail()
    prod = parse_prod()
    print(f"item map={len(item_map)} retail={len(retail)} prod={len(prod)}")

    rows = []
    counts = {}

    def note(cat, qid, detail):
        rows.append((qid, cat, detail))
        counts[cat] = counts.get(cat, 0) + 1

    for qid in sorted(prod, key=int):
        r = retail.get(qid)
        if r is None:
            continue
        groups, ext = prod[qid]
        # 真端每个档位的存在性与内容
        retail_tiers = {}
        for t in (1, 2, 3):
            present = any(f"reward_{f}{t}" in r or f"reward_item{t}_" in r
                          or f"selectable_reward_item{t}_" in r
                          for f in ("exp", "gold", "abyss_point", "glory_point"))
            if not present:
                continue
            numeric = {}
            items = []
            unmapped = False
            for f in ("exp", "gold", "abyss_point", "glory_point"):
                key = f"reward_{f}{t}"
                if key in r and r[key]:
                    numeric[f] = int(r[key])
            for key, val in r.items():
                m = re.match(rf"^(reward|selectable_reward)_item{t}_(\d+)$", key)
                if key.startswith("selectable_reward_item_ext_"):
                    continue
                if not m or not val:
                    continue
                parts = val.rsplit(" ", 1)
                iid = item_map.get(parts[0])
                cnt = int(parts[1]) if len(parts) == 2 else 1
                if iid is None:
                    unmapped = True
                    break
                items.append(("SELECTABLE_ITEM" if m.group(1) == "selectable_reward"
                              else "ITEM", iid, cnt))
            retail_tiers[t] = (numeric, sorted(items), unmapped)

        # 档位 2/3 比对（档位 1 已由既有门禁覆盖）
        for t in (2, 3):
            if t not in retail_tiers:
                continue
            numeric, items, unmapped = retail_tiers[t]
            # 全 0 数值且无道具的档位 = 真端空档（无实质奖励），生产单档表达合理
            if not any(v != 0 for v in numeric.values()) and not items:
                continue
            if unmapped:
                note(f"TIER{t}_UNMAPPED", qid, "names unmappable")
                continue
            if qid in AP_RELIC_EXCEPTION_QIDS:
                # 遗物兑换 20 条：服务端 4 倍 AP 特殊倍率组，平铺 grant-selected-reward（intentional 例外）
                continue
            if (t - 1) >= len(groups):
                # 若真端该档与档位 1 完全相同（5.8 统一成长箱/数值道具机械复制），生产单组表达完全等价
                if 1 in retail_tiers:
                    num1, items1, _ = retail_tiers[1]
                    norm_num = {k: v for k, v in numeric.items() if v != 0}
                    norm_num1 = {k: v for k, v in num1.items() if v != 0}
                    if norm_num == norm_num1 and items == items1:
                        continue
                note(f"TIER{t}_MISSING_PROD", qid,
                     f"prod_groups={len(groups)} retail_numeric={numeric} items={items}")
                continue
            prod_rows = groups[t - 1]
            prod_numeric = {k: next((amt for kind, _, amt in prod_rows if kind == v), 0)
                            for k, v in (("exp", "EXP"), ("gold", "GOLD"),
                                         ("abyss_point", "AP"), ("glory_point", "GP"))}
            ret_numeric = {k: numeric.get(k, 0)
                           for k in ("exp", "gold", "abyss_point", "glory_point")}
            prod_items = sorted((kind, iid, amt) for kind, iid, amt in prod_rows
                                if kind in ("ITEM", "SELECTABLE_ITEM"))
            diff_fields = {k: (prod_numeric[k], ret_numeric[k])
                           for k in prod_numeric if prod_numeric[k] != ret_numeric[k]}
            if diff_fields:
                note(f"TIER{t}_NUMERIC_DIFF", qid,
                     f"diffs={diff_fields}")
            if items != prod_items:
                note(f"TIER{t}_ITEM_DIFF", qid,
                     f"prod={prod_items} retail={items}")
        if len(groups) > 3 and len(retail_tiers) <= 1:
            note("TIER_EXTRA_PROD", qid, f"prod_groups={len(groups)}")

        # 扩展奖励：真端 ext 模型 = item_ext_1 + gold_ext + title_ext
        # （title_ext 为名称字符串，无法映射数字 id，仅校验生产是否有 TITLE 声明）
        ext_field = r.get("reward_item_ext_1")
        gold_ext = r.get("reward_gold_ext")
        title_ext = r.get("reward_title_ext")
        ext_selectable = sorted(
            (int(k.rsplit("_", 1)[1]), v) for k, v in r.items()
            if k.startswith("selectable_reward_item_ext_") and v)
        if ext_field or gold_ext or title_ext or ext_selectable:
            expected = []
            unmapped = False
            if ext_field:
                parts = ext_field.rsplit(" ", 1)
                iid = item_map.get(parts[0])
                cnt = int(parts[1]) if len(parts) == 2 else 1
                if iid is None:
                    unmapped = True
                else:
                    expected.append(("ITEM", iid, cnt))
            for _, val in ext_selectable:
                parts = val.rsplit(" ", 1)
                iid = item_map.get(parts[0])
                cnt = int(parts[1]) if len(parts) == 2 else 1
                if iid is None:
                    unmapped = True
                else:
                    expected.append(("SELECTABLE_ITEM", iid, cnt))
            if gold_ext:
                try:
                    expected.append(("GOLD", 0, int(gold_ext)))
                except ValueError:
                    pass
            if unmapped:
                note("EXT_UNMAPPED", qid, f"name={ext_field}")
            else:
                prod_items = sorted((iid, amt) for kind, iid, amt in ext
                                    if kind not in ("GOLD", "TITLE"))
                exp_items = sorted((iid, amt) for k, iid, amt in expected
                                   if k in ("ITEM", "SELECTABLE_ITEM"))
                prod_gold = next((amt for kind, iid, amt in ext if kind == "GOLD"), None)
                exp_gold = next((amt for k, iid, amt in expected if k == "GOLD"), None)
                prod_title = next((iid for kind, iid, amt in ext if kind == "TITLE"), None)
                items_differ = prod_items != exp_items
                gold_differ = (prod_gold != exp_gold) and not (
                    prod_gold is None and exp_gold is None)
                title_differ = bool(title_ext) and prod_title is None
                if items_differ or gold_differ or title_differ:
                    note("EXT_DIFF", qid,
                         f"prod={sorted(ext)} "
                         f"retail_items={exp_items} retail_gold={exp_gold} "
                         f"retail_title={title_ext}")

    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("quest_id\tcategory\tdetail\n")
        for row in rows:
            fh.write("\t".join(row) + "\n")
    print("category counts:", counts)


if __name__ == "__main__":
    main()
