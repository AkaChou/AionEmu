"""分析 item_template 分片的真实结构形状：字段路径、列表结构、actions 子树。
Analyze the real structural shape of item_template shards: field paths, list structures, actions subtree.
"""
import collections
import glob
import sys
import xml.etree.ElementTree as ET

SHARD_GLOB = sys.argv[1] if len(sys.argv) > 1 else \
    "src/main/resources/aion/data/static_data/items/item/item_template_*.xml"

attr_paths = collections.Counter()   # normalized "a/b@c" -> records containing it
elem_paths = collections.Counter()   # normalized "a/b"   -> records containing it
repeat_max = collections.Counter()   # normalized path -> max repeats within one record
action_tags = collections.Counter()  # action element tag -> records containing it
action_attrs = collections.Counter() # "tag@attr" -> records
actions_per_record = collections.Counter()
records = 0
recs_with_actions = 0

for f in sorted(glob.glob(SHARD_GLOB)):
    for ev, el in ET.iterparse(f, events=("end",)):
        if el.tag != "item_template":
            continue
        records += 1
        seen_attr, seen_elem = set(), set()
        per_rep = collections.Counter()

        def walk(node, path):
            for k in node.attrib:
                seen_attr.add(f"{path}@{k}")
            tagc = collections.Counter(c.tag for c in node)
            for tag, n in tagc.items():
                p = f"{path}/{tag}"
                seen_elem.add(p)
                per_rep[p] = max(per_rep[p], n)
            for c in node:
                walk(c, f"{path}/{c.tag}")

        walk(el, "")
        for p in seen_attr: attr_paths[p] += 1
        for p in seen_elem: elem_paths[p] += 1
        for p, n in per_rep.items():
            if n > repeat_max[p]:
                repeat_max[p] = n

        acts = el.find("actions")
        if acts is not None:
            recs_with_actions += 1
            kids = list(acts)
            actions_per_record[len(kids)] += 1
            for k in kids:
                action_tags[k.tag] += 1
                for a in k.attrib:
                    action_attrs[f"{k.tag}@{a}"] += 1
        el.clear()

print(f"records                : {records:,}")
print(f"records with <actions> : {recs_with_actions:,} ({recs_with_actions/records:.1%})")
print(f"distinct attr paths    : {len(attr_paths)}")
print(f"distinct elem paths    : {len(elem_paths)}")
print(f"distinct action tags   : {len(action_tags)}")
print()
print("=== list-valued paths (repeat > 1 within a record) ===")
for p, n in sorted(repeat_max.items(), key=lambda x: -x[1]):
    if n > 1:
        print(f"  max_repeat={n:3d}  {p}")
print()
print("=== actions: children per record ===")
for n, c in sorted(actions_per_record.items()):
    print(f"  {n:3d} child(ren): {c:,} records")
print()
print("=== actions child tags ===")
for t, c in action_tags.most_common():
    print(f"  {c:7d}  {t}")
print()
print("=== elements directly under item_template ===")
tops = [p for p in elem_paths if p.count("/") == 1]
for p in sorted(tops):
    print(f"  {elem_paths[p]:7d}  {p}")
