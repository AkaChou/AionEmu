#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P4 第二批 C：metadata 档位 1 容器内完全重复的奖励声明去重（幂等）。

2303/2367/2411/2448/3088 的 <rewards> 中同一 (kind,id,amount) 声明出现 2~3 次，
且 npc-complete 的 fixed-reward-indices 把重复项全部列为完成发放项——完成时重复
发放（2303 发 48 瓶 vs 真端 24；3088 发 12 个 vs 真端 4）。真端 reward_item1_N
只有一组。修复=容器去重 + 同步缩写所有 npc-complete 的 fixed-reward-indices。
"""
import os
import re

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")

TARGETS = ["2303", "2367", "2411", "2448", "3088"]


def dedupe(text):
    container = re.search(r"(<rewards>\n)(.*?)(\n[ \t]*</rewards>)", text, re.S)
    if not container:
        raise SystemExit("no rewards container")
    body = container.group(2)
    seen = set()
    kept = []
    removed = 0
    for line in body.split("\n"):
        m = re.match(r'[ \t]*<reward kind="([^"]+)" id="(\d+)" amount="([^"]+)"', line)
        if m:
            key = tuple(m.groups())
            if key in seen:
                removed += 1
                continue
            seen.add(key)
        kept.append(line)
    return text[:container.start(2)] + "\n".join(kept) + text[container.end(2):], removed


def shrink_fixed(text):
    """fixed-reward-indices 压缩到平铺奖励总数内。"""
    def fix(match):
        head, indices, tail = match.group(1), match.group(2), match.group(3)
        return f"{head}{indices}{tail}"
    container = re.search(r"(<rewards>\n)(.*?)(\n[ \t]*</rewards>)", text, re.S)
    total = len([l for l in container.group(2).split("\n")
                 if re.match(r'[ \t]*<reward kind="', l)])

    def clamp(match):
        idxs = [int(x) for x in match.group(1).split()]
        clamped = " ".join(str(i) for i in idxs if i < total)
        return f'fixed-reward-indices="{clamped}" '

    return re.sub(r'fixed-reward-indices="([^"]*)" ', clamp, text), total


def main():
    for qid in TARGETS:
        path = os.path.join(PROD_DIR, qid + ".xml")
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        text, removed = dedupe(text)
        if not removed:
            print(f"{qid}: already deduped")
            continue
        text, total = shrink_fixed(text)
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(text)
        print(f"{qid}: removed {removed} duplicate line(s), fixed indices clamped "
              f"to <{total}")


if __name__ == "__main__":
    main()
