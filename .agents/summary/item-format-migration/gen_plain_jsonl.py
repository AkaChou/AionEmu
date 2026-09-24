#!/usr/bin/env python3
"""生成「标准 JSONL」：键名原样保留、嵌套对象、每行一条记录，用于可读性优先的格式评估。

Generate "plain JSONL": keys kept verbatim, nested objects, one record per line, for the
readability-first format evaluation.

与 `gen_compact_jsonl.py` / `gen_dict_jsonl.py` 的区别 / Difference from the compact variants:
  那两个把键压成字典索引、值压成整数索引，体积最小但不可读；本版本不做任何压缩，
  目标是「人能读懂、能手工编辑」，代价是体积（实测为 XML 的约 1.28 倍）。

  The compact variants dictionary-encode keys and values for minimum size at the cost of
  readability; this one compresses nothing, so a human can read and hand-edit it. The price
  is size (measured ≈1.28× the XML).

形态规则（两遍扫描，从数据自动判定）/ Shape rules (two passes, inferred from the data):
  第 1 遍统计每个「结构路径」下的子标签集合与单条记录内的最大重复次数，
  第 2 遍据下表把一个元素渲染成 JSON 值：

  | 元素的子元素特征            | 渲染为                                  | 实例 |
  |---------------------------|----------------------------------------|------|
  | 子标签 ≥ 2 种（多态列表）    | 有序数组，元素带标签包装 `[{"read":{}}]`  | `/actions`（43 种）、`/modifiers`（2 种） |
  | 子标签 1 种且可重复（数组）  | 数组，元素不带标签 `[{...},{...}]`       | `/tradein_list` 的 `tradein_item` |
  | 子标签 1 种且唯一（普通对象）| 对象 `{...}`                            | `weapon_stats`、`conditions` |
  | 根元素                     | 对象（子元素是字段而非列表元素，特判）    | `item_template` |

  路径一律不含序号：同一条记录里的多个 `<add>` 结构相同，共享同一份判定。

  Paths never carry indices: several `<add>` under one record share the same shape, so they
  share one classification.

  ⚠️ 已知边界 / Known limitation: 规则把「非根的多标签子元素」一律当成多态列表的元素。
     当前数据里只有 `/actions` 与 `/modifiers` 命中，且二者在模型中都确实是 `List<...>`；
     若将来某个元素的子元素既有列表元素又有普通字段，本规则会判错，需要显式白名单。

     The rule treats every non-root multi-tag child set as list elements. Only `/actions` and
     `/modifiers` match today and both really are `List<...>` in the model; if an element ever
     mixes list elements with plain fields, this heuristic misclassifies and needs an explicit
     allow-list.

用法 / Usage:
  python3 gen_plain_jsonl.py <shard.xml> <out.jsonl>
"""
import collections
import io
import json
import re
import sys
import xml.etree.ElementTree as ET

# 安全数值转换：纯整数、无前导零（"007" 保留字符串）、≤15 位。其余原样保留字符串，
# 与 JAXB「属性一律先当 String」的语义一致。
# Safe numeric conversion: plain integers, no leading zero ("007" stays a string), ≤15 digits.
# Everything else stays a string, matching JAXB's "attributes are strings first" semantics.
NUMERIC = re.compile(r"^-?(0|[1-9]\d{0,14})$")

ROOT_TAG = "item_template"


def scalar(value):
    """按需把字符串转成 int，否则原样返回 / convert to int when safe, else keep the string."""
    return int(value) if NUMERIC.match(value) else value


def scan(node, path, child_tags, child_repeat):
    """第 1 遍：累计每个结构路径下的子标签集合与单条记录内的最大重复次数。
    Pass 1: accumulate per-path child tag sets and the max repeat count within one record."""
    counts = collections.Counter(child.tag for child in node)
    for tag, count in counts.items():
        child_tags[path].add(tag)
        key = (path, tag)
        if count > child_repeat[key]:
            child_repeat[key] = count
    for child in node:
        scan(child, path + "/" + child.tag, child_tags, child_repeat)


def classify(child_tags, child_repeat):
    """由第 1 遍的统计得出「每个路径如何渲染」/ derive the render kind for every path."""
    kinds = {}
    for path, tags in child_tags.items():
        if len(tags) >= 2:
            kinds[path] = "polymorphic"
        else:
            tag = next(iter(tags))
            kinds[path] = "array" if child_repeat[(path, tag)] > 1 else "object"
    # 根元素的子元素是字段而非列表元素；数据层面与多态列表无法区分，必须特判。
    # The root's children are fields, not list elements; indistinguishable from a polymorphic
    # list at the data level, so it is special-cased.
    kinds[""] = "object"
    return kinds


def render(node, path, kinds):
    """把一个元素渲染成 JSON 值 / render one element into a JSON value."""
    kind = kinds.get(path, "object")
    if kind == "polymorphic":
        # 元素带标签包装，保持文档顺序（JAXB 的 List<...> 有序）。
        # Elements keep their tag and document order (JAXB's List<...> is ordered).
        return [{child.tag: render(child, path + "/" + child.tag, kinds)} for child in node]
    if kind == "array":
        return [render(child, path + "/" + child.tag, kinds) for child in node]
    obj = {name: scalar(value) for name, value in node.attrib.items()}
    groups = collections.OrderedDict()
    for child in node:
        groups.setdefault(child.tag, []).append(child)
    for tag, children in groups.items():
        obj[tag] = render(children[0], path + "/" + tag, kinds)
    return obj


def collect_records(shard):
    """流式产出根元素 / stream the root elements."""
    with open(shard, "rb") as handle:
        for _event, element in ET.iterparse(handle, events=("end",)):
            if element.tag != ROOT_TAG:
                continue
            yield element
            element.clear()


def build_schema(shards):
    """在全部输入上建一份 schema。

    必须全量扫描：同一路径在不同分片里可能只出现部分子标签（例如某分片的 `/modifiers`
    只有 `add`，另一分片还有 `rate`），按单文件判定会得出不同的 kind，导致各分片的
    JSONL 形态不一致。
    Schema must be built over all inputs: one path may show only a subset of its child tags in
    a single shard (e.g. `/modifiers` with just `add` here and `add`+`rate` there), so per-file
    classification yields different kinds and inconsistent shapes across shards.
    """
    child_tags = collections.defaultdict(set)
    child_repeat = collections.defaultdict(int)
    for shard in shards:
        for element in collect_records(shard):
            scan(element, "", child_tags, child_repeat)
    return classify(child_tags, child_repeat)


def main():
    args = sys.argv[1:]
    if args and args[0] == "--schema-out":
        kinds = build_schema(args[2:])
        with io.open(args[1], "w", encoding="utf-8") as handle:
            json.dump(kinds, handle, indent=1, sort_keys=True)
        shapes = dict(collections.Counter(kinds.values()))
        print(f"schema paths={len(kinds)} shapes={shapes} out={args[1]}")
        return

    if args and args[0] == "--schema":
        with io.open(args[1], encoding="utf-8") as handle:
            kinds = json.load(handle)
        shard, out_path = args[2], args[3]
    else:
        shard, out_path = args[0], args[1]
        kinds = build_schema([shard])

    records = 0
    with io.open(out_path, "w", encoding="utf-8", newline="\n") as handle:
        for element in collect_records(shard):
            handle.write(json.dumps(render(element, "", kinds), separators=(",", ":"), ensure_ascii=False))
            handle.write("\n")
            records += 1

    shapes = collections.Counter(kinds.values())
    print(f"records={records} paths={len(kinds)} shapes={dict(shapes)} out={out_path}")


if __name__ == "__main__":
    main()
