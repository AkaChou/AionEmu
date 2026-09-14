# -*- coding: utf-8 -*-
"""最终判定：保留真端(retail, resolve_z=true)、删除非真端(legacy)。
Final rule: keep the retail spot (resolve_z="true"), delete the non-retail (legacy) one.

判定一个 legacy spot L 应删除，当且仅当在同一 <spawn> 块内存在真端 spot R 满足：
  1) L 在 a5e274fd0 提交前已存在（非真端旧点）
  2) L 在提交后仍存在（= 漏删残留）
  3) R 由 a5e274fd0 引入（在该提交版本中存在、在其父版本中不存在）
  4) L 与 R 的平面距离 dxy <= DXY_CAP（同一点位；z 由真端重新投影，不作判据）

Note: dz is NOT used as a criterion — retail z is re-projected from terrain, so the commit's
own successful replacements show dz up to 701. dxy is the reliable "same point" signal.
"""
import os, re, math, subprocess, sys
from collections import Counter

COMMIT = "a5e274fd0"
ROOT   = "src/main/resources/aion/data/static_data/spawns"
DXY_CAP = 2.0

SPAWN_RE = re.compile(r"<spawn\b([^>]*)>(.*?)</spawn>", re.S)
SPOT_RE  = re.compile(r"<spot\b([^>]*?)/?>", re.S)
ATTR_RE  = re.compile(r'([\w_]+)\s*=\s*"([^"]*)"')

def attrs(s): return dict(ATTR_RE.findall(s))
def fnum(d, k):
    try: return float(d[k])
    except (KeyError, ValueError): return None
def key(a, nd=2):
    return (round(a["x"], nd), round(a["y"], nd), round(a["z"] or 0.0, nd))

def parse(text):
    out = []
    for m in SPAWN_RE.finditer(text):
        sa = attrs(m.group(1)); base = m.start(2)
        spots = []
        for sm in SPOT_RE.finditer(m.group(2)):
            a = attrs(sm.group(1))
            x, y, z = fnum(a, "x"), fnum(a, "y"), fnum(a, "z")
            if x is None or y is None: continue
            spots.append({"x": x, "y": y, "z": z,
                          "rz": a.get("resolve_z", "false") == "true",
                          "raw": sm.group(0).strip(),
                          "line": text[:base + sm.start()].count("\n") + 1})
        out.append({"npc": sa.get("npc_id"), "spots": spots,
                    "line": text[:m.start()].count("\n") + 1})
    return out

def git_show(rev, path):
    return subprocess.run(["git", "show", "%s:%s" % (rev, path)],
                          capture_output=True, text=True).stdout

changed = [c for c in subprocess.run(["git", "show", "--name-only", "--pretty=format:", COMMIT],
           capture_output=True, text=True).stdout.split()
           if c.startswith(ROOT) and c.endswith(".xml")]

deletions, skipped = [], []
for path in changed:
    after  = open(path, encoding="utf-8", errors="replace").read()
    parent = git_show(COMMIT + "^", path)
    commitv = git_show(COMMIT, path)
    if not parent or not commitv: continue
    p_spots = Counter(key(s) for b in parse(parent)  for s in b["spots"])
    c_spots = Counter(key(s) for b in parse(commitv) for s in b["spots"])
    for ab in parse(after):
        leg = [s for s in ab["spots"] if not s["rz"]]
        ret = [s for s in ab["spots"] if s["rz"]]
        for L in leg:
            if p_spots[key(L)] <= 0:            # 非提交前旧点 -> 不是漏删
                skipped.append(("legacy-not-preexisting", path, L)); continue
            hit = None
            for R in ret:
                if c_spots[key(R)] <= 0:        # 真端点非该提交引入 -> 不属本次同步
                    continue
                if p_spots[key(R)] > 0:         # 真端点提交前已存在 -> 非新增
                    continue
                d = math.hypot(L["x"] - R["x"], L["y"] - R["y"])
                if d <= DXY_CAP:
                    hit = (R, d); break
            if hit:
                deletions.append({"file": path, "npc": ab["npc"], "spawn_line": ab["line"],
                                  "L": L, "R": hit[0], "dxy": round(hit[1], 3),
                                  "dz": round(abs((L["z"] or 0.0) - (hit[0]["z"] or 0.0)), 3)})
            else:
                skipped.append(("no-coincident-retail", path, L))

print("待删除（非真端 legacy spot）: %d" % len(deletions))
print("文件数: %d" % len({d['file'] for d in deletions}))
print("跳过: %d" % len(skipped))
ds = sorted(d["dxy"] for d in deletions)
if ds:
    print("\ndxy 分布: min=%.3f p25=%.3f p50=%.3f p75=%.3f p90=%.3f max=%.3f"
          % (ds[0], ds[len(ds)//4], ds[len(ds)//2], ds[3*len(ds)//4],
             ds[int(len(ds)*0.9)], ds[-1]))
    for lo, hi in [(0,0.2),(0.2,0.5),(0.5,1.0),(1.0,1.5),(1.5,2.01)]:
        print("   [%.1f,%.1f): %d" % (lo, hi, sum(1 for d in ds if lo <= d < hi)))
dzs = sorted(d["dz"] for d in deletions)
print("dz  分布: min=%.3f p50=%.3f p90=%.3f max=%.3f  (仅供参考，非判据)" %
      (dzs[0], dzs[len(dzs)//2], dzs[int(len(dzs)*0.9)], dzs[-1]))
# 用户报告的 3 个
print("\n用户报告 NPC:")
for npc in ("278628", "278630", "278633"):
    for d in deletions:
        if d["npc"] == npc:
            print("   npc=%s dxy=%s  L=%s" % (npc, d["dxy"], d["L"]["raw"]))

import json
with open(".agents/summary/spawn-duplicate-spots/deletions.json", "w", encoding="utf-8") as f:
    json.dump([{k: v for k, v in d.items() if k != "L"} |
               {"line": d["L"]["line"], "raw": d["L"]["raw"]} for d in deletions], f,
              ensure_ascii=False, indent=1)
print("\nwritten deletions.json")
