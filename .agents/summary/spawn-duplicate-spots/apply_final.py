# -*- coding: utf-8 -*-
"""删除非真端 legacy spot，保留真端（resolve_z="true"）。
Delete the non-retail legacy spot; keep the retail one (resolve_z="true").
"""
import json, sys, os
from collections import defaultdict

DRY = "--dry-run" in sys.argv
lst = json.load(open(".agents/summary/spawn-duplicate-spots/apply_list.json", encoding="utf-8"))
by = defaultdict(list)
for r in lst: by[r["file"]].append(r)

total = 0; refused = 0; mismatch = 0
for path, items in sorted(by.items()):
    lines = open(path, encoding="utf-8").read().split("\n")
    for it in items:
        i = it["line"] - 1
        if i < 0 or i >= len(lines):
            print("!! 行号越界 %s L%d" % (path, it["line"])); refused += 1; continue
        actual = lines[i].strip()
        if actual != it["raw"].strip():
            print("!! 行内容不匹配 %s L%d\n   期望 %s\n   实际 %s" % (path, it["line"], it["raw"], actual))
            mismatch += 1; continue
        if "resolve_z" in actual:
            print("!! 拒绝删除真端 %s L%d" % (path, it["line"])); refused += 1; continue
        lines[i] = None
        total += 1
    if not DRY:
        open(path, "w", encoding="utf-8").write("\n".join(l for l in lines if l is not None))

print("\n%s删除 %d 个 legacy spot（拒绝 %d，内容不匹配 %d）"
      % ("[DRY-RUN] " if DRY else "已", total, refused, mismatch))
