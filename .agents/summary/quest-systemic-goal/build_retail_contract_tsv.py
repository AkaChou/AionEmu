#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从真端 quest.xml 生成 start-metadata 门禁基线 TSV（可复算，只读外部数据）。

输出 1：src/test/resources/quest/quest-start-metadata-retail-contract.tsv
  行集 = 生产目录中存在的全部任务。
  列：
    quest_id               生产任务 ID
    retail_min_level       真端 minlevel_permitted；真端 999（占位任务）写 RETAIL_PLACEHOLDER
    retail_max_level       真端 maxlevel_permitted；0/998/999 归一为 UNLIMITED
    retail_faction         真端 race_permitted 归一：pc_light=ELYOS, pc_dark=ASMODIANS,
                           两者皆有或生产 PC_ALL 等价表达 -> PC_ALL
    retail_gender          真端 gender_permitted 归一：all -> ALL
    retail_max_repeat      真端 max_repeat_count
输出 2：src/test/resources/quest/quest-start-metadata-retail-cap-exceptions.tsv
  有意封顶例外清单：生产 max-level 与真端不一致、但经定性为服务端系统性版本封顶
  （整族一致 max=82）的任务。仅接受 82 这一唯一封顶值，任何其他值必须逐条重新定性。
"""
import os
import xml.etree.ElementTree as ET

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
RETAIL_QUEST_XML = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")
OUT_CONTRACT = os.path.join(
    REPO, "src/test/resources/quest/quest-start-metadata-retail-contract.tsv")
OUT_CAP = os.path.join(
    REPO, "src/test/resources/quest/quest-start-metadata-retail-cap-exceptions.tsv")

# 经 2026-09-18 全库定性：服务端有意版本封顶值（整族一致 82，见
# .agents/summary/quest-systemic-goal/GOAL_PROGRESS.zh-CN.md 阶段 P2）
INTENTIONAL_CAP_VALUES = {"82"}


def parse_retail():
    quests = {}
    for _, elem in ET.iterparse(RETAIL_QUEST_XML, events=("end",)):
        if elem.tag != "quest":
            continue
        qid = None
        fields = {}
        for child in elem:
            if child.tag == "id":
                qid = (child.text or "").strip()
            elif child.tag in ("minlevel_permitted", "maxlevel_permitted",
                               "race_permitted", "gender_permitted",
                               "max_repeat_count"):
                fields[child.tag] = (child.text or "").strip()
        if qid is not None and qid.isdigit():
            quests[qid] = fields
        elem.clear()
    return quests


def prod_ids():
    return {fn[:-4] for fn in os.listdir(PROD_DIR) if fn.endswith(".xml")}


def norm_faction(race_permitted, prod_race=""):
    tokens = set(race_permitted.split())
    has_light = "pc_light" in tokens
    has_dark = "pc_dark" in tokens
    if prod_race == "PC_ALL" or (has_light and has_dark):
        return "PC_ALL"
    if has_light:
        return "ELYOS"
    if has_dark:
        return "ASMODIANS"
    return "UNKNOWN"


def norm_retail_max(v):
    return "UNLIMITED" if v in ("0", "998", "999", "") else v


def main():
    retail = parse_retail()
    pids = prod_ids()
    print(f"retail={len(retail)} prod={len(pids)}")

    cap_rows = []
    with open(OUT_CONTRACT, "w", encoding="utf-8") as fh:
        fh.write("# Aion 5.8 retail quest start-metadata contract snapshot\n")
        fh.write("# source: Quest_unpacked/quest.xml (regenerate: "
                 ".agents/summary/quest-systemic-goal/build_retail_contract_tsv.py)\n")
        fh.write("# retail_min_level=RETAIL_PLACEHOLDER means the retail quest is a "
                 "placeholder (minlevel_permitted=999); the min-level check is skipped.\n")
        fh.write("# retail_max_level: retail 0/998/999 are normalized to UNLIMITED.\n")
        fh.write("# retail_gender: retail gender_permitted normalized (all -> ALL).\n")
        fh.write("# retail_max_repeat: retail max_repeat_count.\n")
        fh.write("quest_id\tretail_min_level\tretail_max_level\tretail_faction\tretail_gender\tretail_max_repeat\n")
        for qid in sorted(pids, key=int):
            r = retail.get(qid)
            if r is None:
                continue
            rmin = r.get("minlevel_permitted", "")
            rmin_out = "RETAIL_PLACEHOLDER" if rmin == "999" else rmin
            rmax = norm_retail_max(r.get("maxlevel_permitted", ""))
            fac = norm_faction(r.get("race_permitted", ""))
            gender = {"all": "ALL"}.get(r.get("gender_permitted", "all"),
                                        r.get("gender_permitted", "all").upper())
            repeat = r.get("max_repeat_count", "1")
            fh.write(f"{qid}\t{rmin_out}\t{rmax}\t{fac}\t{gender}\t{repeat}\n")

    # 有意封顶例外 = 生产 max 为封顶值 82 且真端为 UNLIMITED 的行（逐行留档）
    for qid in sorted(pids, key=int):
        r = retail.get(qid)
        if r is None:
            continue
        prod_max = None
        with open(os.path.join(PROD_DIR, qid + ".xml"), encoding="utf-8") as pf:
            head = pf.read(900)
        import re
        m = re.search(r'max-level="([^"]+)"', head)
        if m:
            prod_max = m.group(1)
        rmax = norm_retail_max(r.get("maxlevel_permitted", ""))
        if rmax == "UNLIMITED" and prod_max in INTENTIONAL_CAP_VALUES:
            cap_rows.append(qid)

    with open(OUT_CAP, "w", encoding="utf-8") as fh:
        fh.write("# Intentional server-side level cap exceptions (quest-systemic-goal P2)\n")
        fh.write("# Production max-level=82 while retail has no limit; family-consistent "
                 "server cap convention (global config cap=83, 5.8 players cap at 66).\n")
        fh.write("quest_id\tproduction_max_level\tnote\n")
        for qid in cap_rows:
            fh.write(f"{qid}\t82\tintentional server cap (family-consistent)\n")
    print(f"contract rows -> {OUT_CONTRACT}")
    print(f"cap exception rows ({len(cap_rows)}) -> {OUT_CAP}")


if __name__ == "__main__":
    main()
