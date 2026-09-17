#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 P3 职业合同基线 TSV（可复算，只读外部数据）。

输出：src/test/resources/quest/quest-class-retail-contract.tsv
  行集 = 真端 class_permitted 非空且非全集 token 的生产任务（全集+生产通配=等价，不入基线）。
  列：
    quest_id           生产任务 ID
    retail_min_level   真端 minlevel_permitted（999=RETAIL_PLACEHOLDER）
    retail_class_tokens 真端原始职业 token（空格分隔）
比对语义（门禁测试内实现）：
  token -> PlayerClass 直接映射；min>=10 时删除 6 个 base 职业死条目（WARRIOR/SCOUT/
  MAGE/PRIEST/TECHNIST/MUSE，转职后不存在）；生产 classes 为空 = 通配（全职业）。
"""
import os
import xml.etree.ElementTree as ET

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
RETAIL_QUEST_XML = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")
OUT = os.path.join(
    REPO, "src/test/resources/quest/quest-class-retail-contract.tsv")

FULL_TOKENS = {"warrior", "scout", "mage", "cleric", "engineer", "artist",
               "fighter", "knight", "assassin", "ranger", "wizard",
               "elementallist", "chanter", "priest", "gunner", "bard", "rider"}


def main():
    retail = {}
    for _, elem in ET.iterparse(RETAIL_QUEST_XML, events=("end",)):
        if elem.tag == "quest":
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
                retail[qid] = (cls, mn)
            elem.clear()

    rows = 0
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("# Aion 5.8 retail quest class contract snapshot\n")
        fh.write("# source: Quest_unpacked/quest.xml (regenerate: "
                 ".agents/summary/quest-systemic-goal/build_class_contract_tsv.py)\n")
        fh.write("# rows: production quests whose retail class_permitted is a non-empty "
                 "proper subset of the 17 base/advanced tokens.\n")
        fh.write("# comparison: token->PlayerClass direct mapping; base classes are dead "
                 "entries when min level >= 10.\n")
        fh.write("quest_id\tretail_min_level\tretail_class_tokens\n")
        for qid in sorted(retail, key=int):
            path = os.path.join(PROD_DIR, qid + ".xml")
            if not os.path.exists(path):
                continue
            cls, mn = retail[qid]
            tokens = set(cls.split())
            if not cls or tokens == FULL_TOKENS:
                continue
            mn_out = "RETAIL_PLACEHOLDER" if mn == "999" else mn
            fh.write(f"{qid}\t{mn_out}\t{cls}\n")
            rows += 1
    print(f"class contract rows ({rows}) -> {OUT}")


if __name__ == "__main__":
    main()
