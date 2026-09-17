#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P4 第二批 B/C：可选奖励收尾修正（幂等）。

1) 删除发放真端不存在道具的显式 SELECTED_QUEST_REWARD 分支（有 NOREWARD 兜底，
   删除后客户端不可达的分支不再残留发放合同）：
   2677 x8（第二套帽子分支）、30327 x1（186000098）、1942 x1、19015 x1（第 3 分支）。
2) 1540：缩掉 npc-complete choice 与 metadata 声明中的 110300939（补漏）。
3) 2641：metadata 补真端 5 件布/皮/锁/板上衣 SELECTABLE 声明
   （npc-complete 的 SELECTED_QUEST_REWARD1..NOREWARD 通配自动按索引发放）。
"""
import os
import re

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")

# 任务 -> (需删除的可选项 id 集合)（用于识别显式分支）
BRANCH_DROPS = {
    "2677": {112100748, 112300745, 112500733, 112600744,
             114100826, 114300852, 114500798, 114600755},
    "30327": {186000098},
    "1942": {152200370},
    "19015": {152201792},
}

CHOICE_DROPS = {"1540": {110300939}}

# 2641 真端 selectable 5 件（布/皮/锁/锁变体/板 上衣）
SELECTABLE_ADD = {
    "2641": [110101202, 110301137, 110501107, 110551027, 110601089],
}


def delete_branches(qid, drops):
    path = os.path.join(PROD_DIR, qid + ".xml")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    removed = 0
    for tr_match in list(re.finditer(
            r"[ \t]*<transition[^>]*>.*?</transition>\n", text, re.S)):
        block = tr_match.group(0)
        if "SELECTED_QUEST_REWARD" not in block:
            continue
        ids = {int(m) for m in re.findall(
            r'<grant-reward kind="ITEM" id="(\d+)"', block)}
        if ids & drops:
            text = text.replace(block, "", 1)
            removed += 1
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)
    print(f"{qid}: removed {removed} explicit branch(es)")


def shrink_choice(qid, drops):
    path = os.path.join(PROD_DIR, qid + ".xml")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    container = re.search(r"(<rewards>\n)(.*?)(\n[ \t]*</rewards>)", text, re.S)
    if not container:
        raise SystemExit(f"{qid}: no rewards container")
    body = container.group(2)
    kept, removed = [], []
    for line in body.split("\n"):
        m = re.match(r'[ \t]*<reward kind="SELECTABLE_ITEM" id="(\d+)"', line)
        (removed if m and int(m.group(1)) in drops else kept).append(line)
    if not removed:
        print(f"{qid}: choice shrink already applied")
        return
    new_body = "\n".join(kept)
    old_flat, new_flat = [], []
    for src, acc in ((body.split("\n"), old_flat), (kept, new_flat)):
        for line in src:
            m = re.match(r'[ \t]*<reward kind="([^"]+)" id="(\d+)"', line)
            if m:
                acc.append((m.group(1), int(m.group(2))))

    def remap(match):
        line = match.group(0)
        idx_m = re.search(r'reward-index="(\d+)"', line)
        if not idx_m:
            return line
        old_idx = int(idx_m.group(1))
        if old_idx >= len(old_flat):
            return line
        kind, iid = old_flat[old_idx]
        if kind != "SELECTABLE_ITEM" or iid in drops:
            return ""
        new_idx = next(i for i, (k, x) in enumerate(new_flat)
                       if k == "SELECTABLE_ITEM" and x == iid)
        return re.sub(r'reward-index="\d+"', f'reward-index="{new_idx}"', line)

    text = text[:container.start(2)] + new_body + text[container.end(2):]
    text = re.sub(r'[ \t]*<choice [^>]*reward-index="\d+"[^>]*/>[ \t]*\n?',
                  remap, text)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)
    print(f"{qid}: removed selectable lines: {len(removed)}")


def add_selectable(qid, ids):
    path = os.path.join(PROD_DIR, qid + ".xml")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    if any(f'id="{iid}"' in text and "SELECTABLE_ITEM" in text
           for iid in ids):
        already = all(f'SELECTABLE_ITEM" id="{iid}"' in text for iid in ids)
        if already:
            print(f"{qid}: selectable already present")
            return
    rows = "\n".join(
        f'      <reward kind="SELECTABLE_ITEM" id="{iid}" amount="1"/>'
        for iid in ids)
    anchor = re.search(r'(\n[ \t]*</rewards>)', text)
    if not anchor:
        raise SystemExit(f"{qid}: no </rewards> anchor")
    text = text[:anchor.start()] + "\n" + rows + text[anchor.start():]
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)
    print(f"{qid}: appended {len(ids)} selectable items")


def main():
    for qid, drops in BRANCH_DROPS.items():
        delete_branches(qid, drops)
    for qid, drops in CHOICE_DROPS.items():
        shrink_choice(qid, drops)
    for qid, ids in SELECTABLE_ADD.items():
        add_selectable(qid, ids)


if __name__ == "__main__":
    main()
