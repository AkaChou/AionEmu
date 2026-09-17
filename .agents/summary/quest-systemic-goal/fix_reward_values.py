#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P4 第一批：奖励数值轴零散差异修复（幂等，可复算）。

依据 reward-axis-audit.tsv 中字段明确（真端字段存在）的数值差异行：
- EXP_DIFF / GOLD_DIFF / GLORY_DIFF：生产 <rewards> 中对应 kind 的 amount 对齐真端；
  生产缺失则插入；真端为 0 则删除。
- AP_DIFF：跳过精确 4.0 倍行（11279~11286/21281~21288/18849/18850/28849/28850 族，
  服务端 5.8 欧比斯 AP 版本倍率，intentional）；其余对齐。
- EXP_ZERO_RETAIL：真端字段存在且=0（真端明确无经验奖励），删除生产 EXP 声明。

不动 ITEM_DIFF / SELECTABLE_DIFF（多分支平铺表达需逐任务取证，留下批）。
"""
import os
import re

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
AUDIT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "reward-axis-audit.tsv")
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")

KIND_FIELD = {"GOLD_DIFF": ("reward_gold1", "GOLD"),
              "AP_DIFF": ("reward_abyss_point1", "AP"),
              "GLORY_DIFF": ("reward_glory_point1", "GP")}
CATEGORY = {"EXP_DIFF": "EXP", "EXP_ZERO_RETAIL": "EXP",
            "GOLD_DIFF": "GOLD", "AP_DIFF": "AP", "GLORY_DIFF": "GP"}


def is_intentional_ap_x4(detail):
    m = re.match(r"prod=(\d+) retail=(\d+)", detail)
    if not m:
        return False
    prod, retail = int(m.group(1)), int(m.group(2))
    return retail != 0 and prod == retail * 4


def load_rows():
    rows = []
    with open(AUDIT, encoding="utf-8") as fh:
        next(fh)
        for line in fh:
            cols = line.rstrip("\n").split("\t")
            if len(cols) >= 3 and cols[1] in CATEGORY:
                rows.append((cols[0], cols[1], cols[2]))
    return rows


def parse_detail(detail, category=None):
    m = re.match(r"prod=(-?\d+) retail=(\d+)", detail)
    if m:
        return (int(m.group(1)), int(m.group(2)))
    # EXP_ZERO_RETAIL 的 detail 只有 prod=N：真端字段存在且为 0（明确无奖励）
    m = re.match(r"prod=(-?\d+)", detail)
    if m and category == "EXP_ZERO_RETAIL":
        return (int(m.group(1)), 0)
    return (None, None)


def process(qid, kind, retail_value):
    path = os.path.join(PROD_DIR, qid + ".xml")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    m = re.search(r"(<rewards>|<group>)(.*?)(</rewards>|</group>)", text, re.S)
    if not m:
        raise SystemExit(f"{qid}: no rewards container")
    body = m.group(2)
    line_re = re.compile(
        r'\n[ \t]*<reward kind="%s" id="[^"]*" amount="[^"]*"\s*/>' % kind)
    existing = line_re.search(body)
    if retail_value == 0:
        if not existing:
            print(f"{qid}: {kind} already absent")
            return
        body = line_re.sub("", body, count=1)
        action = "removed"
    elif existing:
        old = re.search(r'amount="([^"]*)"', existing.group(0)).group(1)
        if int(old) == retail_value:
            print(f"{qid}: {kind} already {retail_value}")
            return
        body = line_re.sub(
            '\n      <reward kind="%s" id="0" amount="%d"/>' % (kind, retail_value),
            body, count=1)
        action = f"{old} -> {retail_value}"
    else:
        # 容器尾部追加：头部插入会使 npc-complete 的 choice/complete-reward-index 位移
        reward_line_re = re.compile(r'\n[ \t]*<reward kind="[^"]*"[^>]*/>[ \t]*')
        last = None
        for last in reward_line_re.finditer(body):
            pass
        insert_at = (last.end() if last else 0)
        body = body[:insert_at] + '\n      <reward kind="%s" id="0" amount="%d"/>' % (
            kind, retail_value) + body[insert_at:]
        action = f"appended {retail_value}"
    text = text[:m.start(2)] + body + text[m.end(2):]
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)
    print(f"{qid}: {kind} {action}")


def main():
    applied = set()
    for qid, category, detail in load_rows():
        if category == "AP_DIFF" and is_intentional_ap_x4(detail):
            continue
        prod_value, retail_value = parse_detail(detail, category)
        if retail_value is None:
            continue
        kind = CATEGORY[category]
        key = (qid, kind)
        if key in applied:
            continue
        applied.add(key)
        process(qid, kind, retail_value)


if __name__ == "__main__":
    main()
