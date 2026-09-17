#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3：真端 class_permitted 与生产 <classes> 全库审计（只读）。

映射与语义：
- 真端职业 token -> 服务端 PlayerClass id（Goal 提供的映射，逐条复核）。
- base-class 语义：任务 min-level >= 10（转职后）时，真端 base token（warrior/scout/
  mage/cleric/engineer/artist）实际含义是其两条进阶线；min-level < 10 时必须包含
  base class 本身（WARRIOR/SCOUT/MAGE/PRIEST/TECHNIST/MUSE）。
- 生产 classes 元素缺省 = 全职业通配（与真端 17 token 全集等价）。
- 镜像拆分：同族 1xxxx/2xxxx 各自阵营任务分别声明，真端单任务本身也是单阵营的，
  class 轴不涉及阵营拆分，但存在同族天/魔任务分别限定不同职业子集的有意设计。

输出分类：EQUIVALENT / BASE_CLASS_EXPANSION / MIRROR_VARIANT / PROD_WIDER /
PROD_NARROWER / UNMAPPED_TOKEN / RETAIL_MISSING。
"""
import os
import xml.etree.ElementTree as ET

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
RETAIL_QUEST_XML = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "class-axis-audit.tsv")

TOKEN_MAP = {
    "warrior": "WARRIOR", "fighter": "GLADIATOR", "knight": "TEMPLAR",
    "scout": "SCOUT", "assassin": "ASSASSIN", "ranger": "RANGER",
    "mage": "MAGE", "wizard": "SORCERER", "elementallist": "SPIRIT_MASTER",
    "cleric": "PRIEST", "priest": "CLERIC", "chanter": "CHANTER",
    "engineer": "TECHNIST", "gunner": "GUNSLINGER", "rider": "AETHERTECH",
    "artist": "MUSE", "bard": "SONGWEAVER",
}
# base token -> (base class, [advanced classes])
BASE_CLASSES = {
    "warrior": ("WARRIOR", ["GLADIATOR", "TEMPLAR"]),
    "scout": ("SCOUT", ["ASSASSIN", "RANGER"]),
    "mage": ("MAGE", ["SORCERER", "SPIRIT_MASTER"]),
    "cleric": ("PRIEST", ["CLERIC", "CHANTER"]),
    "engineer": ("TECHNIST", ["GUNSLINGER", "AETHERTECH"]),
    "artist": ("MUSE", ["SONGWEAVER"]),
}
ALL_CLASSES = set(TOKEN_MAP.values())

FULL_SET_TOKENS = "warrior scout mage cleric engineer artist fighter knight assassin ranger wizard elementallist chanter priest gunner bard rider"


def retail_classes():
    out = {}
    for _, elem in ET.iterparse(RETAIL_QUEST_XML, events=("end",)):
        if elem.tag != "quest":
            continue
        qid = None
        cls = ""
        mn = ""
        for child in elem:
            if child.tag == "id":
                qid = (child.text or "").strip()
            elif child.tag == "class_permitted":
                cls = (child.text or "").strip()
            elif child.tag == "minlevel_permitted":
                mn = (child.text or "").strip()
        if qid is not None and qid.isdigit():
            out[qid] = (cls, mn)
        elem.clear()
    return out


def production_classes():
    out = {}
    for fn in sorted(os.listdir(PROD_DIR)):
        if not fn.endswith(".xml"):
            continue
        qid = fn[:-4]
        tree = ET.parse(os.path.join(PROD_DIR, fn))
        meta = tree.getroot().find("metadata")
        classes = {c.get("id") for c in meta.findall("./classes/class")} if meta is not None else set()
        out[qid] = classes
    return out


def main():
    retail = retail_classes()
    prod = production_classes()
    rows = []
    counts = {}
    for qid in sorted(prod, key=int):
        pset = prod[qid]
        r = retail.get(qid)
        if r is None:
            cls, mn = "", ""
            note = "RETAIL_MISSING"
            rows.append((qid, "RETAIL_MISSING", ",".join(sorted(pset)), "", note))
            counts[note] = counts.get(note, 0) + 1
            continue
        cls, mn = r
        tokens = cls.split()
        if "class_permitted" not in " ".join([]):  # no-op guard
            pass
        unmapped = [t for t in tokens if t not in TOKEN_MAP]
        if unmapped:
            rows.append((qid, "UNMAPPED_TOKEN", ",".join(sorted(pset)), cls,
                         "unmapped=" + ",".join(unmapped)))
            counts["UNMAPPED_TOKEN"] = counts.get("UNMAPPED_TOKEN", 0) + 1
            continue
        if not tokens or set(tokens) == set(FULL_SET_TOKENS.split()):
            rset = ALL_CLASSES  # 通配
            note = ""
        else:
            rset = set()
            note = ""
            base_expanded = False
            for t in tokens:
                mapped = TOKEN_MAP[t]
                if t in BASE_CLASSES and mn != "999" and mn.isdigit() and int(mn) >= 10:
                    # 转职后接取：base token 语义为其进阶线
                    rset.update(BASE_CLASSES[t][1])
                    base_expanded = True
                else:
                    rset.add(mapped)
            if base_expanded:
                note = "base-expanded"
        if pset == rset:
            cat = "EQUIVALENT"
        elif not pset:
            cat = "PROD_WILDCARD"
        elif rset - pset and pset - rset:
            cat = "MISMATCH"
        elif rset - pset:
            cat = "PROD_NARROWER"
        else:
            cat = "PROD_WIDER"
        rows.append((qid, cat, ",".join(sorted(pset)), " ".join(tokens), note))
        counts[cat] = counts.get(cat, 0) + 1

    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("quest_id\tcategory\tproduction_classes\tretail_tokens\tnote\n")
        for row in rows:
            fh.write("\t".join(row) + "\n")
    print("category counts:", counts)


if __name__ == "__main__":
    main()
