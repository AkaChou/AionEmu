#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P1/P2 start-metadata 修复脚本（幂等，可复算）。

P1：9 个 min-level 真端对齐（1648, 2231, 2641, 19000..19003, 25407, 25408）。
P2：27 个 max-level 真端对齐：
  - 19 条补真端上限（4711, 4712, 4722, 15665, 15666, 18833, 18835, 25665, 25666,
    29672..29676, 29684..29688）
  - 4 条 82 -> 65（80621, 80622, 80643, 80644）
  - 5 条移除族内不对称残留 cap -> 无上限（27525, 50074, 80945, 80946, 80878）

只重写目标文件的 min-level / max-level 属性值，其他字节保持不变。
重跑时若值已是目标值则跳过（幂等）。
"""
import os
import re

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")

MIN_FIXES = {
    "1648": "42", "2231": "12", "2641": "41",
    "19000": "50", "19001": "50", "19002": "50", "19003": "50",
    "25407": "68", "25408": "68",
}
MAX_FIXES = {
    "4711": "50", "4712": "50", "4722": "55",
    "15665": "67", "15666": "67",
    "18833": "23", "18835": "26",
    "25665": "67", "25666": "67",
    "29672": "55", "29673": "19", "29674": "30", "29675": "40", "29676": "48",
    "29684": "19", "29685": "29", "29686": "39", "29687": "49", "29688": "65",
    "80621": "65", "80622": "65", "80643": "65", "80644": "65",
    # 真端无上限：2147483647 为生产无上限表达
    "27525": "2147483647", "50074": "2147483647",
    "80945": "2147483647", "80946": "2147483647", "80878": "2147483647",
}


def fix(qid, attr, value):
    path = os.path.join(PROD_DIR, qid + ".xml")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    pattern = re.compile(r'(%s=")[^"]*(")' % attr)
    m = pattern.search(text)
    if not m:
        print(f"{qid}: NO {attr} ATTRIBUTE")
        return
    if m.group(0) == f'{attr}="{value}"':
        print(f"{qid}: already {attr}={value}")
        return
    text = pattern.sub(r"\g<1>%s\g<2>" % value, text, count=1)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)
    print(f"{qid}: {attr} -> {value}")


def main():
    for qid, v in MIN_FIXES.items():
        fix(qid, "min-level", v)
    for qid, v in MAX_FIXES.items():
        fix(qid, "max-level", v)


if __name__ == "__main__":
    main()
