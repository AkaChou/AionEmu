#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 P4 数值奖励合同基线 TSV（可复算，只读外部数据）。

输出：src/test/resources/quest/quest-reward-value-retail-contract.tsv
  行集 = 生产任务；列 = 真端档位 1 数值奖励（字段缺失写 RETAIL_UNSET，门禁跳过）：
    quest_id / retail_exp1 / retail_gold1 / retail_abyss_point1 / retail_glory_point1
比对语义：真端字段存在即权威（含 0=真端明确无此奖励）；
AP 的服务端 4 倍版本倍率族（11279~11286/21281~21288/18849/18850/28849/28850）
由门禁例外清单放行。
"""
import os
import xml.etree.ElementTree as ET

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
RETAIL_QUEST_XML = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")
OUT = os.path.join(
    REPO, "src/test/resources/quest/quest-reward-value-retail-contract.tsv")

FIELDS = {"exp": "reward_exp1", "gold": "reward_gold1",
          "ap": "reward_abyss_point1", "gp": "reward_glory_point1"}


def main():
    retail = {}
    for _, elem in ET.iterparse(RETAIL_QUEST_XML, events=("end",)):
        if elem.tag == "quest":
            qid = None
            fields = {}
            for child in elem:
                if child.tag == "id":
                    qid = (child.text or "").strip()
                elif child.tag in FIELDS.values():
                    fields[child.tag] = (child.text or "").strip()
            if qid is not None and qid.isdigit():
                retail[qid] = fields
            elem.clear()

    rows = 0
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("# Aion 5.8 retail quest tier-1 numeric reward contract snapshot\n")
        fh.write("# source: Quest_unpacked/quest.xml (regenerate: "
                 ".agents/summary/quest-systemic-goal/build_reward_value_contract_tsv.py)\n")
        fh.write("# RETAIL_UNSET means the retail field is absent (retail not configured; "
                 "skip comparison). Existing values (including 0) are authoritative.\n")
        fh.write("quest_id\tretail_exp1\tretail_gold1\tretail_abyss_point1\t"
                 "retail_glory_point1\n")
        for qid in sorted(retail, key=int):
            if not os.path.exists(os.path.join(PROD_DIR, qid + ".xml")):
                continue
            fields = retail[qid]
            vals = ["RETAIL_UNSET" if FIELDS[k] not in fields else fields[FIELDS[k]]
                    for k in ("exp", "gold", "ap", "gp")]
            fh.write(f"{qid}\t" + "\t".join(vals) + "\n")
            rows += 1
    print(f"reward value contract rows ({rows}) -> {OUT}")


if __name__ == "__main__":
    main()
