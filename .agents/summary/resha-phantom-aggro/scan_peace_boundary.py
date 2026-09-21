# -*- coding: utf-8 -*-
"""按当前要塞状态(1221=BALAUR/PEACE)重算 400010000 附近 NPC 的感知边界。

输入：玩家 GPS (来自 log/adminaudit.log 的 //gps)。
输出：95m 已知列表内所有 NPC + 3D 距离 + 有效感知范围 + 进入仇恨所需的垂直下降量。
"""
import glob, math, os, re, sys
import xml.etree.ElementTree as ET

ROOT = '/Users/mc/IdeaProjects/AionEmu-test'
PX, PY, PZ = 1548.9895, 1004.6653, 3021.8057   # //gps 14:23:02
KNOWN_DIST = 95.0                              # VisibleObject.VisibilityDistance / maxZvisibleDistance

# ---------- 1) npc 模板 ----------
tpl = {}
for path in glob.glob(f'{ROOT}/src/main/resources/aion/data/static_data/npcs/*.xml'):
    try:
        for ev, el in ET.iterparse(path, events=('end',)):
            if el.tag == 'npc_template':
                br = el.find('bound_radius')
                tpl[int(el.attrib['npc_id'])] = dict(
                    name=el.attrib.get('name', ''), name_id=el.attrib.get('name_id', ''),
                    ai=el.attrib.get('ai', ''), sensory=float(el.attrib.get('sensory_range', '0') or 0),
                    tribe=el.attrib.get('tribe', ''), type=el.attrib.get('type', ''),
                    npctype=el.attrib.get('npc_type', ''), race=el.attrib.get('race', ''),
                    level=el.attrib.get('level', ''),
                    bound_front=float(br.attrib.get('front', '0') or 0) if br is not None else 0.0,
                    bound_side=float(br.attrib.get('side', '0') or 0) if br is not None else 0.0)
                el.clear()
    except Exception as e:
        print('tpl fail', path, e, file=sys.stderr)

# ---------- 2) 零售 AI 定义 ----------
retail = {}
for path in glob.glob(f'{ROOT}/src/main/resources/aion/definitions/compact/ai/npc-ai-parts/*.xml'):
    text = open(path, encoding='utf-8', errors='ignore').read()
    for m in re.finditer(r'<npc id="(\d+)"([^>]*)/?>', text):
        attrs = m.group(2)
        g = lambda k, d='': (re.search(k + r'="([^"]*)"', attrs).group(1) if re.search(k + r'="([^"]*)"', attrs) else d)
        retail[int(m.group(1))] = dict(ai=g('ai'), sens=g('sensory_range'), short=g('sensory_range_short'),
                                       angle=g('sensory_angle'), scale=g('model_scale_percent', '100'),
                                       chase=g('max_chase_time'), pff=g('react_to_pathfind_fail'))

# ---------- 3) 刷怪点（按 BALAUR/PEACE 过滤） ----------
rows = []
files = glob.glob(f'{ROOT}/src/main/resources/aion/data/static_data/spawns/**/400010000*.xml', recursive=True)
for path in files:
    is_siege_file = os.sep + 'Sieges' + os.sep in path
    root = ET.parse(path).getroot()
    for siege_spawn in root.iter('siege_spawn'):
        sid = siege_spawn.attrib.get('siege_id')
        for race_el in siege_spawn.findall('siege_race'):
            race = race_el.attrib.get('race')
            for mod_el in race_el.findall('siege_mod'):
                mod = mod_el.attrib.get('mod')
                active = (race == 'BALAUR' and mod == 'PEACE')
                for spawn in mod_el.iter('spawn'):
                    nid = spawn.attrib.get('npc_id')
                    if not nid:
                        continue
                    for spot in spawn.findall('spot'):
                        rows.append((int(nid), float(spot.attrib['x']), float(spot.attrib['y']), float(spot.attrib['z']),
                                     f'Sieges#{sid}/{race}/{mod}', active))
    if not is_siege_file:
        for spawn in root.iter('spawn'):
            nid = spawn.attrib.get('npc_id')
            if not nid or spawn.find('spot') is None:
                continue
            for spot in spawn.findall('spot'):
                rows.append((int(nid), float(spot.attrib['x']), float(spot.attrib['y']), float(spot.attrib['z']),
                             os.path.basename(os.path.dirname(path)), True))

# ---------- 4) 计算 ----------
out = []
for nid, x, y, z, where, active in rows:
    if not active:
        continue
    d2 = math.hypot(x - PX, y - PY)
    if d2 > 150:
        continue
    dz = z - PZ
    d3 = math.hypot(d2, dz)
    t = tpl.get(nid, {})
    r = retail.get(nid, {})
    base = t.get('sensory', 0.0)
    try:
        rs = float(r.get('sens') or 0)
    except ValueError:
        rs = 0.0
    if rs > 0:
        base = rs
    try:
        scale = float(r.get('scale') or 100)
    except ValueError:
        scale = 100.0
    bound_off = max(t.get('bound_front', 0.0), t.get('bound_side', 0.0)) * max(0.0, scale) / 200.0
    eff = 0.0 if base <= 0 else min(100.0, base + bound_off)
    known = d2 <= KNOWN_DIST and abs(dz) <= KNOWN_DIST
    out.append(dict(npc=nid, x=x, y=y, z=z, where=where, d2=d2, dz=dz, d3=d3, base=base, eff=eff,
                    gap=d3 - eff, known=known, t=t, r=r, bound_off=bound_off))

out.sort(key=lambda e: (not e['known'], e['gap']))
print(f"玩家 GPS: X={PX} Y={PY} Z={PZ}")
print(f"过滤条件: 要塞 1221 = BALAUR -> 仅 siege_race=BALAUR & mod=PEACE 生效 (+非要塞刷怪文件)")
print(f"已知列表判定: 2D<=95 且 |dz|<=95\n")
hdr = f"{'npc':>7} {'2d':>7} {'dz':>7} {'3d':>7} {'sens':>5} {'eff':>6} {'gap':>7} {'known':>5}  {'ai':<14} {'retailAI':<34} name **[tribe/type] file"
print(hdr)
for e in out:
    if not e['known']:
        continue
    print(f"{e['npc']:7d} {e['d2']:7.1f} {e['dz']:+7.1f} {e['d3']:7.1f} {e['base']:5.0f} {e['eff']:6.1f} {e['gap']:+7.1f} {'Y':>5}  "
          f"{str(e['t'].get('ai','')):<14} {str(e['r'].get('ai','')):<34} {e['t'].get('name','')} [{e['t'].get('tribe','')}/{e['t'].get('type','')}] name_id={e['t'].get('name_id','')} {e['where']}")

print("\n=== 95m 已知列表内、但在感知范围外且**下降后可进入感知**的对象（按所需下降量排序）===")
vert = []
for e in out:
    if not e['known'] or e['eff'] <= 0 or e['gap'] <= 0:
        continue
    d2, dz, S = e['d2'], e['dz'], e['eff']
    if S <= d2:
        continue
    target_dz = -math.sqrt(S * S - d2 * d2) if dz < 0 else math.sqrt(S * S - d2 * d2)
    delta = abs(target_dz - dz)     # 需要向该方向移动的垂直距离
    vert.append((delta, e))
vert.sort()
for delta, e in vert[:30]:
    print(f"下降/上升 {delta:5.1f}m -> npc {e['npc']:7d} 2d={e['d2']:6.1f} dz={e['dz']:+6.1f} 3d={e['d3']:6.1f} eff={e['eff']:5.1f}  "
          f"{str(e['r'].get('ai') or e['t'].get('ai','')):<34} {e['t'].get('name','')} name_id={e['t'].get('name_id','')}")

print(f"\n已知列表内对象总数: {sum(1 for e in out if e['known'])} / 150m 内候选: {len(out)}")
