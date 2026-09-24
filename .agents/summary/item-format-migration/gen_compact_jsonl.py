#!/usr/bin/env python3
"""从 item_template XML 分片生成紧凑 JSONL（键表 + 稀疏索引对），供性能探针使用。

Generate compact JSONL (key table + sparse index pairs) from an item_template XML shard
for the performance probe.

格式 / Format:
  第 1 行   {"_keys":[<路径>, ...]}                路径键表，全分片只出现一次
  第 2..N 行 [<键索引>,"<值>",<键索引>,"<值>", ...] 只写出现的字段；同一键重复即列表多元素

路径 / Paths:
  ""            根 ItemTemplate 的 @XmlAttribute        -> "@id"
  嵌套元素属性    -> "/weapon_stats@parry"
  多态列表元素    -> "/actions/craftlearn@skillid"（用元素名区分具体类型，不含序号）

用法 / Usage:
  python3 gen_compact_jsonl.py <shard.xml> <out.jsonl>
"""
import io
import json
import sys
import xml.etree.ElementTree as ET

# 无属性元素的存在标记：真实值从不为空字符串。
ELEMENT_ONLY = ""


def collect(root):
    """深度优先收集 (路径, 值) 对，按文档顺序输出。

    同名兄弟元素在路径里带序号（`/modifiers/add[2]@bonus`）：同一个父节点下若有多个同名子元素，
    路径必须区分它们，否则解析器无法还原列表元素归属——属性集不同时（例如部分 <add> 没有
    bonus），任何"属性名回绕"式的推断都会把它们错并到同一个元素。

    Depth-first collect (path, value) pairs in document order.

    Sibling elements sharing a tag carry an index in the path (`/modifiers/add[2]@bonus`):
    when one parent holds several same-tag children the path must tell them apart, otherwise the
    reader cannot restore list membership. With differing attribute sets (some <add> lack bonus)
    every "attribute name wrapped around" heuristic merges them into one element.
    """
    pairs = []
    stack = [(root, "")]
    while stack:
        node, path = stack.pop()
        if path and not node.attrib:
            # 无属性元素（如 <read/>）必须显式记录存在性：路径→值 编码否则完全看不到它，
            # 解析端会漏建该对象（实测 actions 列表因此少一项）。空值即存在标记——真实值从不为空。
            # An attribute-less element (e.g. <read/>) must record its existence explicitly: a
            # path->value encoding is otherwise blind to it and the reader skips the object entirely
            # (measured: the actions list came up one entry short). The empty string marks existence
            # because real values are never empty.
            pairs.append((path, ELEMENT_ONLY))
        for k, v in node.attrib.items():
            pairs.append((path + "@" + k, v))
        totals = {}
        for child in node:
            totals[child.tag] = totals.get(child.tag, 0) + 1
        seen = {}
        for child in reversed(list(node)):
            seen[child.tag] = seen.get(child.tag, 0) + 1
            index = totals[child.tag] - seen[child.tag]
            suffix = f"[{index}]" if totals[child.tag] > 1 else ""
            stack.append((child, path + "/" + child.tag + suffix))
    return pairs


def main():
    shard, out_path = sys.argv[1], sys.argv[2]
    keys = {}
    rows = []
    with open(shard, "rb") as fh:
        for _event, el in ET.iterparse(fh, events=("end",)):
            if el.tag != "item_template":
                continue
            row = []
            for path, value in collect(el):
                row.append((keys.setdefault(path, len(keys)), value))
            rows.append(row)
            el.clear()

    inv = [None] * len(keys)
    for name, idx in keys.items():
        inv[idx] = name

    with io.open(out_path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(json.dumps({"_keys": inv}, separators=(",", ":"), ensure_ascii=False))
        fh.write("\n")
        for row in rows:
            flat = []
            for idx, value in row:
                flat.append(idx)
                flat.append(value)
            fh.write(json.dumps(flat, separators=(",", ":"), ensure_ascii=False))
            fh.write("\n")

    print(f"records={len(rows)} keys={len(inv)} out={out_path}")


if __name__ == "__main__":
    main()
