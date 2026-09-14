# -*- coding: utf-8 -*-
"""历史取证判据：对每个「legacy 与真端平面重合」的对 (L,R)，
找到引入 R 的提交，检查该提交的父版本中是否已存在 L。
  父版本已存在 L  →  该提交在 L 旁边新增了重合的 R = 制造重复 → 应删 L
  父版本不含 L    →  L 与 R 同批引入，非"漏删"       → 不删
"""
import json, re, math, subprocess
from collections import defaultdict

FINAL = ".agents/summary/spawn-duplicate-spots/final.json"
SPAWN_RE = re.compile(r"<spawn\b([^>]*)>(.*?)</spawn>", re.S)
SPOT_RE  = re.compile(r"<spot\b([^>]*?)/?>", re.S)
ATTR_RE  = re.compile(r'([\w_]+)\s*=\s*"([^"]*)"')

def attrs(s): return dict(ATTR_RE.findall(s))
def fnum(d,k):
    try: return float(d[k])
    except (KeyError,ValueError): return None

def blocks(text):
    out=[]
    for m in SPAWN_RE.finditer(text):
        sa=attrs(m.group(1)); base=m.start(2)
        sp=[]
        for sm in SPOT_RE.finditer(m.group(2)):
            a=attrs(sm.group(1)); x,y=fnum(a,"x"),fnum(a,"y")
            if x is None or y is None: continue
            sp.append({"x":x,"y":y,"rz":a.get("resolve_z","false")=="true",
                       "raw":sm.group(0).strip(),
                       "line":text[:base+sm.start()].count("\n")+1})
        out.append({"npc":sa.get("npc_id"),"spots":sp})
    return out

def git(args):
    return subprocess.run(["git"]+args, capture_output=True, text=True).stdout

pairs = json.load(open(FINAL, encoding="utf-8"))
print("待验证对: %d" % len(pairs))
confirmed=[]; notdup=[]; unresolved=[]
for i, r in enumerate(pairs):
    f=r["file"]; npc=r["npc"]
    mx = re.search(r'x="([^"]*)"', r["retraw"]); my = re.search(r'y="([^"]*)"', r["retraw"])
    lx = re.search(r'x="([^"]*)"', r["raw"]);     ly = re.search(r'y="([^"]*)"', r["raw"])
    if not (mx and my and lx and ly): unresolved.append(r); continue
    try:
        Lx, Ly = float(lx.group(1)), float(ly.group(1))
    except ValueError:
        unresolved.append(r); continue
    # 引入 R 的提交（x 属性串出现次数变化的提交，最早的那个）
    out = git(["log","--format=%h","-S",mx.group(0),"--",f]).split()
    if not out: unresolved.append(r); continue
    intro = out[-1]
    parent = git(["show","%s^:%s"%(intro,f)])
    if not parent: unresolved.append(r); continue
    # 父版本中同 npc 块内是否存在与 L 平面重合的 legacy 点
    hit=False
    for b in blocks(parent):
        if b["npc"]!=npc: continue
        for s in b["spots"]:
            if s["rz"]: continue
            if math.hypot(s["x"]-Lx, s["y"]-Ly) <= 2.0: hit=True; break
        if hit: break
    r2=dict(r); r2["intro"]=intro
    (confirmed if hit else notdup).append(r2)
    if (i+1)%20==0: print("  ...%d/%d" % (i+1,len(pairs)))

print()
print("确认（引入 R 时 L 已存在 = 制造重复）: %d" % len(confirmed))
print("非漏删（L 与 R 同批引入）:            %d" % len(notdup))
print("无法判定:                              %d" % len(unresolved))
print("文件数(确认): %d" % len({r['file'] for r in confirmed}))
byc=defaultdict(int)
for r in confirmed: byc[r["intro"]]+=1
print("\n按引入提交分组:")
for k,v in sorted(byc.items(), key=lambda x:-x[1])[:12]:
    d=git(["log","-1","--format=%ad %s","--date=short",k]).strip()
    print("   %-10s %4d  %s" % (k, v, d[:62]))
json.dump(confirmed, open(".agents/summary/spawn-duplicate-spots/confirmed.json","w",encoding="utf-8"),
          ensure_ascii=False, indent=1)
print("\n非漏删样例（应排除）:")
for r in notdup[:8]:
    print("   %s npc=%s dxy=%s" % (r["file"].split("/")[-1], r["npc"], r["dxy"]))
    print("      L %s" % r["raw"])
    print("      R %s" % r["retraw"])
