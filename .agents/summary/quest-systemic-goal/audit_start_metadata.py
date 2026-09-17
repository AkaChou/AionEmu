#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
真端 start-metadata 全库审计脚本（只读）。

用途：把真端 quest.xml 的 start 元数据（minlevel_permitted / maxlevel_permitted /
race_permitted / class_permitted / gender_permitted / max_repeat_count）与生产
quest_definition/quests/*.xml 的 metadata 逐任务比对，输出 TSV 差异清单。

语义约定：
- 真端 maxlevel_permitted 的 0 / 999 为「无上限」sentinel；998 语义待证（视为 sentinel 候选记录）。
- 生产 max-level="2147483647" 表示无上限。
- 真端 race_permitted: pc_light=ELYOS, pc_dark=ASMODIANS；两者同时=双阵营。
- 真端 class_permitted 的 base-class token 与服务端 PlayerClass 的映射关系在
  class-token-mapping.tsv 中单独维护（P3 阶段使用，本脚本仅输出原始 token 集合）。

输入（只读）：
  /Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml
  src/main/resources/aion/data/static_data/quest_definition/quests/*.xml
输出：
  .agents/summary/quest-systemic-goal/start-metadata-diff.tsv
  （列：quest_id, field, production, retail, note）
"""
import os
import sys
import xml.etree.ElementTree as ET

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
RETAIL_QUEST_XML = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")
OUT_TSV = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "start-metadata-diff.tsv")

UNLIMITED_PROD = "2147483647"
# 真端 sentinel：0=字段默认无上限；998/999=不可达大值（等效无上限，999 亦用于占位任务）
RETAIL_NO_LIMIT = {"0", "998", "999"}
# 生产侧 999 同样是不可达大值，等效无上限
PROD_NO_LIMIT = {UNLIMITED_PROD, "999", ""}


def parse_retail():
    """解析真端 quest.xml，返回 {id: dict(field=value)}。"""
    quests = {}
    for _, elem in ET.iterparse(RETAIL_QUEST_XML, events=("end",)):
        if elem.tag != "quest":
            continue
        qid = None
        fields = {}
        for child in elem:
            if child.tag == "id":
                qid = (child.text or "").strip()
            elif child.tag in (
                    "minlevel_permitted", "maxlevel_permitted", "client_level",
                    "gender_permitted", "class_permitted", "race_permitted",
                    "max_repeat_count"):
                fields[child.tag] = (child.text or "").strip()
        if qid is not None and qid.isdigit():
            quests[qid] = fields
        elem.clear()
    return quests


def parse_production():
    """解析生产 quest XML metadata，返回 {id: dict(field=value)}。"""
    quests = {}
    for fn in sorted(os.listdir(PROD_DIR)):
        if not fn.endswith(".xml"):
            continue
        qid = fn[:-4]
        try:
            tree = ET.parse(os.path.join(PROD_DIR, fn))
        except ET.ParseError as exc:
            print(f"PARSE_ERROR {qid}: {exc}", file=sys.stderr)
            continue
        meta = tree.getroot().find("metadata")
        if meta is None:
            continue
        fields = {
            "min-level": meta.get("min-level", ""),
            "max-level": meta.get("max-level", ""),
        }
        races = [r.get("id") for r in meta.findall("./races/race")]
        fields["races"] = " ".join(races) if races else ""
        classes = [c.get("id") for c in meta.findall("./classes/class")]
        fields["classes"] = " ".join(classes) if classes else ""
        gender = meta.find("gender")
        fields["gender"] = gender.get("id", "") if gender is not None else ""
        repeat = meta.find("repeat")
        fields["repeat"] = (repeat.get("max-count", "")
                            if repeat is not None else "")
        quests[qid] = fields
        elem_clear(tree)
    return quests


def elem_clear(tree):
    tree.getroot().clear()


def normalize_retail_maxlevel(value):
    """真端 maxlevel_permitted -> 归一化结果（unlimited 保留 sentinel 说明）。"""
    if value in RETAIL_NO_LIMIT:
        return "UNLIMITED"
    return value


def normalize_prod_maxlevel(value):
    if value in PROD_NO_LIMIT:
        return "UNLIMITED"
    return value


def main():
    retail = parse_retail()
    prod = parse_production()
    print(f"retail quests: {len(retail)}  production quests: {len(prod)}")

    rows = []
    for qid in sorted(prod, key=lambda x: int(x)):
        p = prod[qid]
        r = retail.get(qid)
        if r is None:
            rows.append((qid, "existence", "PRESENT", "MISSING_IN_RETAIL",
                         "production-only quest"))
            continue
        # min-level
        if "minlevel_permitted" in r:
            rmin = r["minlevel_permitted"]
            if rmin != "999" and p["min-level"] != rmin:
                rows.append((qid, "min-level", p["min-level"], rmin,
                             "minlevel_permitted mismatch (retail 999=treated as no data)"))
        # max-level (sentinel-aware)
        if "maxlevel_permitted" in r:
            rmax = normalize_retail_maxlevel(r["maxlevel_permitted"])
            pmax = normalize_prod_maxlevel(p["max-level"])
            if rmax != pmax:
                rows.append((qid, "max-level", pmax, rmax,
                             f"retail raw={r['maxlevel_permitted']}"))
        # faction (PC_ALL == ELYOS ASMODIANS 等价表达)
        race_map = {"pc_light": "ELYOS", "pc_dark": "ASMODIANS"}
        rr = " ".join(race_map.get(t, t)
                      for t in r.get("race_permitted", "").split())
        if rr == "ELYOS ASMODIANS" and p["races"] == "PC_ALL":
            rr = "PC_ALL"
        if rr != p["races"]:
            rows.append((qid, "races", p["races"], rr,
                         "race_permitted mismatch"))
        # gender
        rg = r.get("gender_permitted", "")
        rg_norm = {"all": ""}.get(rg, rg.upper())
        if rg_norm != p["gender"]:
            rows.append((qid, "gender", p["gender"], rg_norm,
                         "gender_permitted mismatch"))
        # repeat
        rrep = r.get("max_repeat_count", "")
        if rrep and p["repeat"] and rrep != p["repeat"]:
            rows.append((qid, "max_repeat_count", p["repeat"], rrep,
                         "max_repeat_count mismatch"))
        # class tokens (raw, for P3; production classes emitted verbatim)
        rcls = r.get("class_permitted", "")
        if rcls:
            rows.append((qid, "class-tokens-retail", "", rcls, "raw retail tokens"))

    with open(OUT_TSV, "w", encoding="utf-8") as fh:
        fh.write("quest_id\tfield\tproduction\tretail\tnote\n")
        for row in rows:
            fh.write("\t".join(row) + "\n")
    print(f"wrote {len(rows)} rows -> {OUT_TSV}")


if __name__ == "__main__":
    main()
