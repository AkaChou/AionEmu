# -*- coding: utf-8 -*-
"""最终判定（平面坐标，不含 z）：
保留真端点（resolve_z="true"），删除同 <spawn> 块内与它平面重合的非真端 legacy 点。
Final rule (planar XY, z excluded): keep the retail spot, delete the coincident legacy one.

平面重合阈值 DXY=2.0；z 不作判据（真端 z 由地形重投影，天然与旧值不同）。
"""
import os, re, math, json
from collections import defaultdict

ROOT = "src/main/resources/aion/data/static_data/spawns"
DXY = 2.0
SPAWN_RE = re.compile(r"<spawn\b([^>]*)>(.*?)</spawn>", re.S)
SPOT_RE  = re.compile(r"<spot\b([^>]*?)/?>", re.S)
ATTR_RE  = re.compile(r'([\w_]+)\s*=\s*"([^"]*)"')

def attrs(s): return dict(ATTR_RE.findall(s))
def fnum(d, k):
    try: return float(d[k])
    except (KeyError, ValueError): return None

def parse(path):
    text = open(path, encoding="utf-8", errors="replace").read()
    out = []
    for m in SPAWN_RE.finditer(text):
        sa = attrs(m.group(1)); base = m.start(2)
        spots = []
        for sm in SPOT_RE.finditer(m.group(2)):
            a = attrs(sm.group(1))
            x, y = fnum(a, "x"), fnum(a, "y")
            if x is None or y is None: continue
            spots.append({"x": x, "y": y, "z": fnum(a, "z"),
                          "rz": a.get("resolve_z", "false") == "true",
                          "a": a, "raw": sm.group(0).strip(),
                          "line": text[:base + sm.start()].count("\n") + 1})
        out.append({"npc": sa.get("npc_id"), "spots": spots, "attrs": sa,
                    "line": text[:m.start()].count("\n") + 1})
    return out

results = []
for dirpath, _, names in os.walk(ROOT):
    for fn in sorted(names):
        if not fn.endswith(".xml"): continue
        path = os.path.join(dirpath, fn).replace("\\", "/")
        for blk in parse(path):
            leg = [s for s in blk["spots"] if not s["rz"]]
            ret = [s for s in blk["spots"] if s["rz"]]
            if not leg or not ret: continue
            for L in leg:
                best = None
                for R in ret:
                    d = math.hypot(L["x"] - R["x"], L["y"] - R["y"])
                    if best is None or d < best[0]: best = (d, R)
                if best and best[0] <= DXY:
                    results.append({"file": path, "npc": blk["npc"], "spawn_line": blk["line"],
                                    "nleg": len(leg), "nret": len(ret), "nspots": len(blk["spots"]),
                                    "dxy": round(best[0], 3),
                                    "line": L["line"], "raw": L["raw"],
                                    "retline": best[1]["line"], "retraw": best[1]["raw"]})

print("待删除的 legacy 点: %d" % len(results))
print("文件数: %d" % len({r['file'] for r in results}))
print("块数: %d" % len({(r['file'], r['spawn_line']) for r in results}))
ds = sorted(r["dxy"] for r in results)
for lo, hi in [(0,0.2),(0.2,0.5),(0.5,1.0),(1.0,1.5),(1.5,2.01)]:
    print("   dxy [%.1f,%.1f): %d" % (lo, hi, sum(1 for d in ds if lo <= d < hi)))

by = defaultdict(list)
for r in results: by[r["file"]].append(r)
with open(".agents/summary/spawn-duplicate-spots/final.txt", "w", encoding="utf-8") as out:
    for f in sorted(by):
        out.write("\n=== %s (%d) ===\n" % (f, len(by[f])))
        for r in sorted(by[f], key=lambda z: z["line"]):
            out.write("  npc=%-8s spawnL%-6d nspots=%d(leg%d/ret%d) dxy=%s\n"
                      % (r["npc"], r["spawn_line"], r["nspots"], r["nleg"], r["nret"], r["dxy"]))
            out.write("     DEL   L%-6d %s\n" % (r["line"], r["raw"]))
            out.write("     keep  L%-6d %s\n" % (r["retline"], r["retraw"]))
json.dump(results, open(".agents/summary/spawn-duplicate-spots/final.json","w",encoding="utf-8"),
          ensure_ascii=False, indent=1)
print("\n用户报告的 3 个:")
for npc in ("278628","278630","278633"):
    for r in results:
        if r["npc"]==npc: print("   npc=%s dxy=%s spawnL%d" % (npc, r["dxy"], r["spawn_line"]))
