#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P4 第三批：extended-rewards（最后一轮追加奖励）对齐修复（幂等，自校验）。

权威数据：tier-axis-audit.tsv 的 EXT_DIFF 行（真端 reward_gold_ext /
reward_item_ext_1 / selectable_reward_item_ext_N 字段）。

修复动作：
- ADD_GOLD   生产 ext 容器缺 reward_gold_ext 对应的 GOLD 行 -> 补
- ADD_MISSING 生产无 ext 容器或缺道具声明 -> 按 retail_items+retail_gold 建全
- SHRINK     生产 ext 容器多出真端没有的道具 -> 删（80040/80041 多 125040131、
             80116 族职业件平铺缩到真端单件）
- MOVE_FROM_REWARDS  若该道具同时存在于 metadata rewards 平铺（每轮发放）
             与真端 ext（最后一轮追加）语义冲突 -> 从平铺删除并收缩
             fixed-reward-indices
- 2368 的 TITLE 77 vs 真端 dark_title27：title 名称无模板表映射，
  EVIDENCE_BLOCKED，保留生产 TITLE 不动。
脚本末尾对全部修改文件做 ET.parse 自校验。
"""
import os
import re
import xml.etree.ElementTree as ET

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
AUDIT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "tier-axis-audit.tsv")
PROD_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/quest_definition/quests")

# XSD 顺序：rewards/reward-groups 在 extended-rewards 之前，锚点按此优先
ANCHORS = ["</rewards>", "</group>", "</work-items>", "</inventory-items>",
           "</items>", "</repeat>", "</races>"]
RETAIL_QUEST_XML = "/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml"
RETAIL_ITEMS_DIR = "/Users/mc/PycharmProjects/unpak/Items_unpacked"
ITEM_DIR = os.path.join(
    REPO, "src/main/resources/aion/data/static_data/items/item")


def parse_detail(detail):
    pm = re.search(r"prod=\[(.*?)\] retail_items=(\[[^\]]*\]) retail_gold=(\w+)", detail)
    prod_part, items_part, gold = pm.group(1), pm.group(2), pm.group(3)
    prod_items = [(int(i), int(a)) for i, a in
                  re.findall(r"'(?:ITEM|SELECTABLE_ITEM)', (\d+), (\d+)\)", prod_part)]
    prod_gold = re.search(r"\('GOLD', 0, (\d+)\)", prod_part)
    prod_gold = int(prod_gold.group(1)) if prod_gold else None
    retail_items = [(int(i), int(a)) for i, a in
                    re.findall(r"\((\d+), (\d+)\)", items_part)]
    retail_gold = None if gold == "None" else int(gold)
    return prod_items, prod_gold, retail_items, retail_gold


def rewards_container(text):
    return re.search(r"(<rewards>\n|<group>\n)(.*?)(\n[ \t]*</rewards>|\n[ \t]*</group>)",
                     text, re.S)


def remove_from_rewards(text, qid, iid, amount):
    c = rewards_container(text)
    if not c:
        return text, False
    body = c.group(2)
    kept, removed = [], False
    for line in body.split("\n"):
        m = re.match(r'[ \t]*<reward kind="ITEM" id="(\d+)"\s+amount="(\d+)"', line)
        if m and int(m.group(1)) == iid and int(m.group(2)) == amount and not removed:
            removed = True
            continue
        kept.append(line)
    if not removed:
        return text, False
    total = len(kept)

    def clamp(match):
        idxs = [int(x) for x in match.group(1).split()]
        clamped = " ".join(str(i) for i in idxs if i < total)
        return f'fixed-reward-indices="{clamped}" '

    tail = text[c.end(2):]
    tail = re.sub(r'fixed-reward-indices="([^"]*)" ', clamp, tail)
    return text[:c.start(2)] + "\n".join(kept) + tail, True


def ensure_container(text, qid):
    """返回 (text, body_start, body_end)：ext 容器奖励区的切片索引。"""
    open_tag = "<extended-rewards>"
    close_tag = "</extended-rewards>"
    start = text.find(open_tag)
    if start >= 0:
        body_start = start + len(open_tag) + 1
        end = text.find(close_tag, body_start)
        if end < 0:
            raise SystemExit(f"{qid}: extended-rewards close tag missing")
        return text, body_start, end
    for anchor in ANCHORS:
        idx = text.find(anchor)
        if idx >= 0:
            insert_at = idx + len(anchor)
            block = "\n    <extended-rewards>\n    </extended-rewards>"
            text = text[:insert_at] + block + text[insert_at:]
            start = text.find(open_tag, insert_at)
            body_start = start + len(open_tag) + 1
            end = text.find(close_tag, body_start)
            return text, body_start, end
    raise SystemExit(f"{qid}: no anchor for extended-rewards container")


def ext_lines(items, gold):
    lines = []
    if gold is not None:
        lines.append(f'      <reward kind="GOLD" id="0" amount="{gold}"/>')
    for kind, iid, amount in items:
        lines.append(f'      <reward kind="{kind}" id="{iid}" amount="{amount}"/>')
    return lines


def process(qid, retail_items, retail_gold, retail_title):
    path = os.path.join(PROD_DIR, qid + ".xml")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    changed = False

    # MOVE_FROM_REWARDS：真端 ext 道具若在 metadata rewards 平铺（每轮发放），
    # 与 ext 的最后一轮追加语义冲突 -> 从平铺删除
    for kind, iid, amount in retail_items:
        if kind != "ITEM":
            continue
        text, removed = remove_from_rewards(text, qid, iid, amount)
        if removed:
            changed = True
            print(f"{qid}: moved item {iid}x{amount} out of tier-1 rewards")

    text, body_start, body_end = ensure_container(text, qid)
    body = text[body_start:body_end]
    cur_items = [(int(m.group(1)), int(m.group(2)))
                 for m in re.finditer(
                     r'<reward kind="ITEM" id="(\d+)"\s+amount="(\d+)"', body)]
    cur_gold = re.search(r'<reward kind="GOLD" id="0"\s+amount="(\d+)"', body)
    cur_gold = int(cur_gold.group(1)) if cur_gold else None
    cur_selectable = re.findall(r'<reward kind="SELECTABLE_ITEM" id="(\d+)"', body)

    want_items = sorted(retail_items)
    want_gold = retail_gold
    if sorted(cur_items) != want_items or cur_gold != want_gold:
        # TITLE 声明保留（如 2368：title_ext 名称无模板表映射，EVIDENCE_BLOCKED）
        title_line = re.search(
            r'[ \t]*<reward kind="TITLE"[^>]*/>[ \t]*\n?', body)
        lines = ext_lines(want_items, want_gold)
        if title_line:
            lines.append(title_line.group(0).rstrip("\n"))
        new_body = ("\n" + "\n".join(lines) + "\n") if lines else "\n"
        text = text[:body_start] + new_body + text[body_end:]
        changed = True
        print(f"{qid}: ext container rewritten -> items={want_items} gold={want_gold}")
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(text)
    else:
        print(f"{qid}: ext already aligned")

    if changed:
        ET.parse(path)  # 自校验


def build_item_map():
    mapping = {}
    for fn in sorted(os.listdir(ITEM_DIR)):
        if fn.endswith(".xml"):
            for _, elem in ET.iterparse(os.path.join(ITEM_DIR, fn), events=("start",)):
                if elem.tag == "item_template":
                    nd = elem.get("name_desc")
                    iid = elem.get("id")
                    if nd and iid and nd not in mapping:
                        mapping[nd] = int(iid)
                    elem.clear()
    for fn in sorted(os.listdir(RETAIL_ITEMS_DIR)):
        if fn.startswith("client_items") and fn.endswith(".xml"):
            with open(os.path.join(RETAIL_ITEMS_DIR, fn), "rb") as stream:
                raw = stream.read()
            start = raw.find(b"<?xml")
            if start < 0:
                continue
            for elem in ET.fromstring(raw[start:]).iter("client_item"):
                name = iid = None
                for c in elem:
                    if c.tag == "id":
                        iid = (c.text or "").strip()
                    elif c.tag == "name":
                        name = (c.text or "").strip()
                if name and iid and iid.isdigit() and name not in mapping:
                    mapping[name] = int(iid)
    return mapping




def parse_retail_ext():
    """直接解析真端 ext 三字段（保留 kind：SELECTABLE 件不降级为 ITEM）。"""
    import xml.etree.ElementTree as ET
    retail = {}
    for _, elem in ET.iterparse(RETAIL_QUEST_XML, events=("end",)):
        if elem.tag != "quest":
            continue
        qid = None
        fields = {}
        for child in elem:
            tag = child.tag
            if tag == "id":
                qid = (child.text or "").strip()
            elif tag in ("reward_gold_ext", "reward_item_ext_1", "reward_title_ext") \
                    or re.match(r"^selectable_reward_item_ext_\d+$", tag):
                fields[tag] = (child.text or "").strip()
        if qid is not None and qid.isdigit() and fields:
            retail[qid] = fields
        elem.clear()
    return retail


def main():
    import xml.etree.ElementTree as ET
    item_map = build_item_map()
    retail_ext = parse_retail_ext()
    for qid in sorted(retail_ext, key=int):
        fields = retail_ext[qid]
        items = []
        gold = None
        title = None
        for key, val in fields.items():
            if not val:
                continue
            if key == "reward_gold_ext":
                gold = int(val)
            elif key == "reward_title_ext":
                title = val
            else:
                parts = val.rsplit(" ", 1)
                iid = item_map.get(parts[0])
                if iid is None:
                    print(f"{qid}: unmapped ext name {parts[0]}, skipped")
                    continue
                cnt = int(parts[1]) if len(parts) == 2 else 1
                kind = "SELECTABLE_ITEM" if key.startswith("selectable_") else "ITEM"
                items.append((kind, iid, cnt))
        items.sort()
        process(qid, items, gold, title)


if __name__ == "__main__":
    main()
