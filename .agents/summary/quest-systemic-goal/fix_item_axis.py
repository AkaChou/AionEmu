#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P4 第二批 D：固定奖励道具零散错配修复（幂等）。

按真端 reward_item1_N 映射集合对齐生产的固定道具声明：
- REPLACE 换道具/改数量：1648、2332、2585、2611、25082
- REMOVE 删生产多发的固定道具：18310、18606、50029、51029（均为尾部项，
  同步收缩 npc-complete fixed-reward-indices 中越界引用）
- APPEND 补真端固定道具：2962、15230、15231、15232、25230、28915、80333
2345 为分支合并形态（10 处分支引用），不在本脚本范围，逐任务取证。
"""
import os
import re

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")

REPLACE = {
    # qid: (old_id, old_amount, new_id, new_amount)
    "1648": (186000004, 3, 186000005, 4),
    "2332": (186000007, 6, 186000008, 6),
    "2585": (186000010, 1, 186000010, 4),
    "2611": (186000010, 1, 186000010, 20),
    "25082": (162000050, 14, 162000124, 14),
}
REMOVE = {
    "18310": [170170038],
    "18606": [190000016],
    "50029": [186000178],
    "51029": [186000178],
}
APPEND = {
    "2962": [(162000027, 6)],
    "15230": [(188053902, 1)],
    "15231": [(188053901, 1)],
    "15232": [(188053900, 1)],
    "25230": [(188053900, 1)],
    "28915": [(188052569, 1)],
    "80333": [(188052557, 1)],
}


def container(text):
    # 平铺 <rewards> 或多档 <group>（档位 1）两种容器
    return re.search(r"(<rewards>\n|<group>\n)(.*?)(\n[ \t]*</rewards>|\n[ \t]*</group>)",
                     text, re.S)


def process(qid):
    path = os.path.join(PROD_DIR, qid + ".xml")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    changed = False

    if qid in REPLACE:
        old_id, old_amount, new_id, new_amount = REPLACE[qid]
        line_re = re.compile(
            r'([ \t]*<reward kind="ITEM" id=")%s(" amount=")%s("\s*/>)'
            % (old_id, old_amount))
        loose = re.compile(
            r'([ \t]*<reward kind="ITEM" id=")%s(" amount=")%s("\s*/>)' % (old_id, old_amount))
        if loose.search(text):
            text = loose.sub(
                lambda m: f"{m.group(1)}{new_id}{m.group(2)}{new_amount}{m.group(3)}",
                text, count=1)
            changed = True
            print(f"{qid}: replaced {old_id}x{old_amount} -> {new_id}x{new_amount}")
        elif re.search(r'id="%s"\s+amount="%s"' % (new_id, new_amount), text):
            print(f"{qid}: already replaced")
        else:
            raise SystemExit(f"{qid}: expected line {old_id}x{old_amount} not found")

    if qid in REMOVE:
        drops = REMOVE[qid]
        c = container(text)
        body = c.group(2)
        kept, removed = [], []
        for line in body.split("\n"):
            m = re.match(r'[ \t]*<reward kind="ITEM" id="(\d+)"', line)
            if m and int(m.group(1)) in drops:
                removed.append(int(m.group(1)))
            else:
                kept.append(line)
        if removed:
            total = len(kept)

            def clamp(match):
                idxs = [int(x) for x in match.group(1).split()]
                clamped = " ".join(str(i) for i in idxs if i < total)
                return f'fixed-reward-indices="{clamped}" '

            tail = text[c.end(2):]
            tail = re.sub(r'fixed-reward-indices="([^"]*)" ', clamp, tail)
            text = text[:c.start(2)] + "\n".join(kept) + tail
            changed = True
            print(f"{qid}: removed {sorted(set(removed))}")
        else:
            print(f"{qid}: remove already applied")

    if qid in APPEND:
        c = container(text)
        if c is None:
            # 无 rewards 容器：按 XSD 顺序在 </items> / </repeat> / </races> 后新建
            insert_at = None
            rows = "".join(
                f'\n    <rewards>\n' + "".join(
                    f'      <reward kind="ITEM" id="{iid}" amount="{amount}"/>\n'
                    for iid, amount in APPEND[qid]) + '    </rewards>'
                for _ in [0])
            anchor = None
            for tag in ("</items>", "</repeat>", "</races>"):
                anchor = text.find(tag)
                if anchor >= 0:
                    insert_at = anchor + len(tag)
                    break
            if insert_at is None:
                raise SystemExit(f"{qid}: no anchor for rewards container")
            text = text[:insert_at] + rows + text[insert_at:]
            changed = True
            print(f"{qid}: created rewards container with {APPEND[qid]}")
        else:
            body = c.group(2)
            missing = []
            for iid, amount in APPEND[qid]:
                if not re.search(r'<reward kind="ITEM" id="%s"\s+amount="%s"' % (iid, amount), body):
                    missing.append((iid, amount))
            if missing:
                rows = "\n".join(
                    f'      <reward kind="ITEM" id="{iid}" amount="{amount}"/>'
                    for iid, amount in missing)
                text = text[:c.end(1)] + rows + "\n" + text[c.end(1):]
                changed = True
                print(f"{qid}: appended {missing}")
            else:
                print(f"{qid}: append already applied")

    if changed:
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(text)


def main():
    for qid in sorted(set(REPLACE) | set(REMOVE) | set(APPEND), key=int):
        process(qid)


if __name__ == "__main__":
    main()
