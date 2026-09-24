"""对比 item_template 各种候选编码的体积与解析成本。
Compare candidate encodings for item_template by size and parse cost.
用法: python3 bench_encodings.py <shard.xml>
"""
import glob
import io
import json
import re
import sys
import time
import gzip
import xml.etree.ElementTree as ET

shard = sys.argv[1] if len(sys.argv) > 1 else sorted(
    glob.glob("src/main/resources/aion/data/static_data/items/item/item_template_*.xml"))[-1]

# ---- 收集：路径键表 + 每行 (idx,value) 对 ----
keys, rows = {}, []
raw_bytes = open(shard, "rb").read()
for _ev, el in ET.iterparse(io.BytesIO(raw_bytes), events=("end",)):
    if el.tag != "item_template":
        continue
    pairs = []
    stack = [(el, "")]
    while stack:
        e, path = stack.pop()
        for k, v in e.attrib.items():
            p = path + "@" + k
            pairs.append((keys.setdefault(p, len(keys)), v))
        for c in reversed(list(e)):
            stack.append((c, path + "/" + c.tag))
    rows.append(pairs)
    el.clear()
inv = [None] * len(keys)
for k, i in keys.items():
    inv[i] = k

def gz(b):
    out = io.BytesIO()
    with gzip.GzipFile(fileobj=out, mode="wb", compresslevel=6, mtime=0) as g:
        g.write(b)
    return out.getvalue()

raw = len(raw_bytes)
keytab = "\t".join(inv)

# A. 变长 TSV: 首行键表, 每行 "idx:value" 用 tab 分隔
tsv = keytab + "\n" + "\n".join("\t".join(f"{i}:{v}" for i, v in r) for r in rows)
tsv_b = tsv.encode()

# B. 稀疏 JSONL (键名索引 + 值)
jsonl = json.dumps(inv, separators=(",", ":"), ensure_ascii=False) + "\n" + "\n".join(
    json.dumps(sum(([i, v] for i, v in r), []), separators=(",", ":"), ensure_ascii=False) for r in rows)
jsonl_b = jsonl.encode()

# C. 值字典 TSV
vals, vrows = {}, []
for r in rows:
    vrows.append([(i, vals.setdefault(v, len(vals))) for i, v in r])
vtab = "\t".join(vals.keys())
cdict = keytab + "\n" + vtab + "\n" + "\n".join(
    "\t".join(f"{i}:{j}" for i, j in r) for r in vrows)
cdict_b = cdict.encode()

print(f"shard                  : {shard.split('/')[-1]}")
print(f"records                : {len(rows):,}   distinct keys: {len(keys)}")
print(f"XML raw                : {raw:>12,}")
print(f"XML gzip-6             : {len(gz(raw_bytes)):>12,}  {len(gz(raw_bytes))/raw:6.1%}")
print()
for name, b in (("TSV var-len", tsv_b), ("JSONL sparse", jsonl_b), ("TSV + value-dict", cdict_b)):
    print(f"{name:<22} : {len(b):>12,}  {len(b)/raw:6.1%}    gz {len(gz(b)):>10,}  {len(gz(b))/raw:6.1%}")
print()

# ---- 解析成本（python 参考值，非 JVM 绝对值）----
t0 = time.time()
for l in tsv.split("\n")[1:]:
    [x.split(":", 1) for x in l.split("\t")]
t_tsv = time.time() - t0
t0 = time.time()
for l in jsonl.split("\n")[1:]:
    json.loads(l)
t_json = time.time() - t0
t0 = time.time()
for _ev, el in ET.iterparse(io.BytesIO(raw_bytes), events=("end",)):
    if el.tag == "item_template":
        _ = list(el)
        el.clear()
t_xml = time.time() - t0
print(f"parse  TSV      : {t_tsv:.2f}s   ({t_xml/t_tsv:.1f}x faster than XML)")
print(f"parse  JSONL    : {t_json:.2f}s   ({t_xml/t_json:.1f}x faster than XML)")
print(f"parse  XML(ET)  : {t_xml:.2f}s")

# ---- 值字符集安全性检查（决定 TSV 是否需要转义）----
import collections
bad = collections.Counter()
charset = collections.Counter()
for _r in rows:
    for _i, v in _r:
        if "\t" in v: bad["TAB"] += 1
        if "\n" in v or "\r" in v: bad["LF/CR"] += 1
        if v == "": bad["EMPTY"] += 1
        for ch in v:
            if not (ch.isalnum() or ch in " .,_-'()[]{}!@#$%^&*+=/\\|<>?~`\""):
                charset[ch] += 1
print()
print("=== 值安全字符集检查 ===")
print(f"TAB/LF/空值出现次数 : {dict(bad) if bad else 'none'}")
print(f"非 ASCII 或特殊字符 : {dict(charset) if charset else 'none (纯 ASCII)'}")
