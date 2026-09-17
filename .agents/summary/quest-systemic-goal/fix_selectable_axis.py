#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P4 第二批 A：生产可选奖励池过宽（SEL_EXTRA）缩池修复（幂等）。

按真端 selectable_reward_item1_N 映射集合删除生产多出的 SELECTABLE_ITEM：
  - metadata 档位 1 容器删除对应声明行；
  - npc-complete 删除指向被删项的 choice 行，并按删除后的平铺顺序重算
    保留 choice 的 reward-index；
  - fixed-reward-indices 断言只指向非 SELECTABLE 项且位于被删项之前（不受位移影响）。
清单（missing/extra 见 reward-axis-audit.tsv 的 SEL_EXTRA 行）：
  1687/2677 缩 8 件第二套防具；4017 缩 102000900；3946 缩 152200370；
  30222/30223/30227/30232/30234/30322/30323/30327/30332/30334 缩 186000098。
"""
import os
import re

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")

DROPS = {
    "1687": [112100747, 112300744, 112500732, 112600743, 112301411,
             114100825, 114300851, 114500797, 114600754],
    "2677": [112100748, 112300745, 112500733, 112600744, 114100826,
             114300852, 114500798, 114600755],
    "4017": [102000900],
    "3946": [152200370],
}
for qid in ("30222", "30223", "30227", "30232", "30234",
            "30322", "30323", "30327", "30332", "30334"):
    DROPS[qid] = [186000098]

REWARD_LINE = re.compile(
    r'[ \t]*<reward kind="(SELECTABLE_ITEM)" id="(\d+)" amount="[^"]*"\s*/>[ \t]*\n?')


def process(qid, drops):
    path = os.path.join(PROD_DIR, qid + ".xml")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()

    container = re.search(r"(<rewards>\n|<group>\n)(.*?)(\n[ \t]*</rewards>|\n[ \t]*</group>)",
                          text, re.S)
    if not container:
        raise SystemExit(f"{qid}: no rewards container")
    body = container.group(2)
    lines = body.split("\n")

    def selectable_id(line):
        m = re.match(r'[ \t]*<reward kind="SELECTABLE_ITEM" id="(\d+)"', line)
        return int(m.group(1)) if m else None

    kept_lines = []
    removed = []
    for line in lines:
        sid = selectable_id(line)
        if sid in drops:
            removed.append((sid, line))
        else:
            kept_lines.append(line)

    if not removed:
        print(f"{qid}: nothing to remove")
        return

    # 重算平铺索引（metadata 容器内所有 reward 行的顺序即 npc-complete 索引空间）
    new_body = "\n".join(kept_lines)
    new_flat_ids = []
    for line in new_body.split("\n"):
        m = re.match(r'[ \t]*<reward kind="([^"]+)" id="(\d+)"', line)
        if m:
            new_flat_ids.append((m.group(1), int(m.group(2))))

    # 重写 npc-complete：删除 drop choice，重算保留 choice 的 reward-index
    old_flat = []
    for line in body.split("\n"):
        m = re.match(r'[ \t]*<reward kind="([^"]+)" id="(\d+)"', line)
        if m:
            old_flat.append((m.group(1), int(m.group(2))))

    def remap_choice(match):
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
        new_idx = next(i for i, (k, x) in enumerate(new_flat_ids)
                       if k == "SELECTABLE_ITEM" and x == iid)
        return re.sub(r'reward-index="\d+"', f'reward-index="{new_idx}"', line)

    text_new = text[:container.start(2)] + new_body + text[container.end(2):]
    # 删除指向被删项的 choice 行（含行尾换行），并重映射保留项
    text_new = re.sub(r'[ \t]*<choice [^>]*reward-index="\d+"[^>]*/>[ \t]*\n?',
                      remap_choice, text_new)

    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text_new)
    print(f"{qid}: removed {sorted(i for i, _ in removed)}")


def main():
    for qid, drops in DROPS.items():
        process(qid, drops)


if __name__ == "__main__":
    main()
