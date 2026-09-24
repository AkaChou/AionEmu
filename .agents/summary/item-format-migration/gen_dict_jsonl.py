#!/usr/bin/env python3
"""生成「键字典 + 值字典」双字典紧凑 JSONL，用于分配量优化探针。

Generate a double-dictionary (key dict + value dict) compact JSONL for the allocation probe.

与 `gen_compact_jsonl.py` 的区别 / Difference from the path-key variant:
  该版本的值以内联字符串出现，每条记录的值都要新建 String；本版本把值也放进首行字典，
  行内只剩整数索引，于是全体记录共享同一批 String 实例——既压体积也压分配量。

  The path-key variant inlines every value as a string, so each record allocates its own String.
  This variant puts values in the header dictionary too: rows carry integer indices only, so all
  records share one set of String instances, cutting both size and allocation.

格式 / Format:
  第 1 行   {"_keys":[<路径>,...],"_values":[<值>,...]}
  第 2..N 行 [<键索引>,<值索引>,<键索引>,<值索引>,...]   全为整数

用法 / Usage:
  python3 gen_dict_jsonl.py <shard.xml> <out.jsonl>
"""
import io
import json
import sys
import xml.etree.ElementTree as ET

# 无属性元素的存在标记：真实值从不为空字符串。
# Existence marker for attribute-less elements; real values are never empty.
ELEMENT_ONLY = ""


def collect(root):
    """深度优先收集 (路径, 值) 对，按文档顺序输出；同名兄弟元素的路径带序号。
    Depth-first collect (path, value) pairs in document order; same-tag siblings carry an index."""
    pairs = []
    stack = [(root, "")]
    while stack:
        node, path = stack.pop()
        if path and not node.attrib:
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
    values = {}
    rows = []
    with open(shard, "rb") as fh:
        for _event, el in ET.iterparse(fh, events=("end",)):
            if el.tag != "item_template":
                continue
            row = []
            for path, value in collect(el):
                row.append(keys.setdefault(path, len(keys)))
                row.append(values.setdefault(value, len(values)))
            rows.append(row)
            el.clear()

    inv_keys = [None] * len(keys)
    for name, idx in keys.items():
        inv_keys[idx] = name
    inv_values = [None] * len(values)
    for value, idx in values.items():
        inv_values[idx] = value

    with io.open(out_path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(json.dumps({"_keys": inv_keys, "_values": inv_values},
                            separators=(",", ":"), ensure_ascii=False))
        fh.write("\n")
        for row in rows:
            fh.write(json.dumps(row, separators=(",", ":"), ensure_ascii=False))
            fh.write("\n")

    print(f"records={len(rows)} keys={len(inv_keys)} values={len(inv_values)} out={out_path}")


if __name__ == "__main__":
    main()
