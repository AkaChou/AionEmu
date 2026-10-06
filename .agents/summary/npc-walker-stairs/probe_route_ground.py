#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""离线复刻 GeoMap.getZ（网格面 ∪ 地形图，取 [refZ-100, refZ+2] 带内最高面），
对 LF1A_NPCPath_Ermona 全程与四个跟随者的航点做地面剖面，量化
「航点 Z 取上一步坐标地面」在斜坡/台阶上的误差。

数据源：src/main/resources/aion/geo/210030000.geo.gz + models.mesh + 210030000.png
格式复刻自 GeoWorldLoader / Terrain / GeoMap.getZ（只读）。
"""
import gzip
import struct
import zlib
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
GEO = os.path.join(ROOT, "src/main/resources/aion/geo")
WORLD = 210030000
PHYSICAL = 1
# 感兴趣区域（Ermona 路线走廊，留 25m 边距）
ROI = (1625.0, 1435.0, 1720.0, 1540.0)

ROUTE = {
    1: (1659.969971, 1470.829956, 124.100006),
    2: (1665.554688, 1465.856323, 124.100006),
    3: (1674.187012, 1463.226929, 124.100006),
    4: (1682.106323, 1466.245239, 124.100006),
    5: (1683.682739, 1473.610352, 124.100006),
    6: (1683.656738, 1481.128418, 124.100006),
    7: (1689.688354, 1488.808350, 124.100006),
    8: (1688.986694, 1496.266235, 124.100006),
    9: (1683.711060, 1502.411987, 124.100006),
    10: (1674.959473, 1503.821289, 124.100006),
    11: (1665.865845, 1502.906860, 124.100006),
    12: (1658.484131, 1504.036743, 124.100006),
    13: (1657.537598, 1504.802979, 124.100006),
    14: (1655.742065, 1497.651001, 124.100006),
    15: (1654.734497, 1489.943359, 124.100006),
    16: (1653.685059, 1480.657227, 124.100006),
    17: (1659.401611, 1471.428467, 124.100006),
}
# offsetsx / offsetsy（spawn 文件 walker_template）
OFFX = [0, -1, 1, 0, 0]
OFFY = [0, -2, -3, -6, -8]


# ---------- geo 放置物 / 网格 ----------
def parse_placements(path):
    with gzip.open(path, "rb") as f:
        data = f.read()
    pos, total = 0, len(data)
    while pos < total:
        (nl,) = struct.unpack_from(">H", data, pos)
        pos += 2
        name = data[pos:pos + nl].decode("utf-8", "replace")
        pos += nl
        loc = struct.unpack_from(">3f", data, pos)
        pos += 12
        matrix = struct.unpack_from(">9f", data, pos)
        pos += 36
        scale = struct.unpack_from(">3f", data, pos)
        pos += 12
        pos += 4
        yield name, loc, matrix, scale


def load_world_meshes():
    """放置物 → 世界三角面 → 网格索引。"""
    placements = list(parse_placements(os.path.join(GEO, "%d.geo.gz" % WORLD)))
    names = {name.lower(): True for name, *_ in placements}
    by_name = {}
    with open(os.path.join(GEO, "models.mesh"), "rb") as f:
        data = f.read()
    pos, total = 0, len(data)
    while pos < total:
        (nl,) = struct.unpack_from(">H", data, pos)
        pos += 2
        name = data[pos:pos + nl].decode("utf-8", "replace")
        pos += nl
        (mc,) = struct.unpack_from(">B", data, pos)
        pos += 1
        lower = name.lower()
        want = lower in names
        mods = []
        for _ in range(mc):
            (vc,) = struct.unpack_from(">H", data, pos)
            pos += 2
            nf = vc * 3
            verts = struct.unpack_from(">%df" % nf, data, pos)
            pos += 4 * nf
            (tc,) = struct.unpack_from(">H", data, pos)
            pos += 2
            isz = data[pos]
            pos += 1
            ni = tc * 3
            if isz == 1:
                idx = struct.unpack_from(">%dB" % ni, data, pos)
                pos += ni
            else:
                idx = struct.unpack_from(">%dH" % ni, data, pos)
                pos += 2 * ni
            intentions = data[pos + 1]
            pos += 2
            if want and (intentions & PHYSICAL):
                mods.append((verts, idx))
        if want:
            by_name[lower] = mods

    def rotate(m, v):
        x, y, z = v
        return (m[0] * x + m[1] * y + m[2] * z,
                m[3] * x + m[4] * y + m[5] * z,
                m[6] * x + m[7] * y + m[8] * z)

    grid = {}
    cell = 2.0
    x0, y0, x1, y1 = ROI
    count = 0
    for name, loc, matrix, scale in placements:
        mods = by_name.get(name.lower())
        if not mods:
            continue
        for verts, idx in mods:
            world = []
            for i in range(0, len(verts), 3):
                v = (verts[i] * scale[0], verts[i + 1] * scale[1], verts[i + 2] * scale[2])
                r = rotate(matrix, v)
                world.append((r[0] + loc[0], r[1] + loc[1], r[2] + loc[2]))
            for t in range(0, len(idx), 3):
                a, b, c = world[idx[t]], world[idx[t + 1]], world[idx[t + 2]]
                if max(a[0], b[0], c[0]) < x0 or min(a[0], b[0], c[0]) > x1:
                    continue
                if max(a[1], b[1], c[1]) < y0 or min(a[1], b[1], c[1]) > y1:
                    continue
                if abs((b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])) < 1e-9:
                    continue
                count += 1
                cx0 = int((min(a[0], b[0], c[0]) - x0) // cell)
                cx1 = int((max(a[0], b[0], c[0]) - x0) // cell)
                cy0 = int((min(a[1], b[1], c[1]) - y0) // cell)
                cy1 = int((max(a[1], b[1], c[1]) - y0) // cell)
                for gx in range(cx0, cx1 + 1):
                    for gy in range(cy0, cy1 + 1):
                        grid.setdefault((gx, gy), []).append((a, b, c))
    return grid, count


# ---------- 地形 PNG ----------
def load_terrain():
    path = os.path.join(GEO, "%d.png" % WORLD)
    d = open(path, "rb").read()
    pos = 8
    w = h = bitdepth = colortype = None
    idat = b""
    while pos < len(d):
        ln, typ = struct.unpack_from(">I4s", d, pos)
        pos += 8
        chunk = d[pos:pos + ln]
        pos += ln + 4
        if typ == b"IHDR":
            w, h, bitdepth, colortype = struct.unpack_from(">IIBB", chunk, 0)
        elif typ == b"IDAT":
            idat += chunk
        elif typ == b"IEND":
            break
    assert bitdepth == 16 and colortype == 0, (bitdepth, colortype)
    raw = zlib.decompress(idat)
    bpp = 2
    stride = w * bpp
    out = bytearray(h * stride)
    prev = bytearray(stride)
    p = 0
    for row in range(h):
        ft = raw[p]
        p += 1
        line = bytearray(raw[p:p + stride])
        p += stride
        if ft == 1:
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i - bpp]) & 0xFF
        elif ft == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif ft == 3:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif ft == 4:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                b = prev[i]
                c = prev[i - bpp] if i >= bpp else 0
                pa = abs(b - c)
                pb = abs(a - c)
                pc = abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        out[row * stride:(row + 1) * stride] = line
        prev = line
    # 复刻 Terrain.getZ(xIndex,yIndex)：越界 NaN、边界 0、样本 -1(0xFFFF) 空洞 NaN；
    # 高度 = u16 * 2048 / 65536 = u16/32（碰撞面语义，与 pathHeight 的 &0xfffc 不同）。
    # 索引存储复刻 readHeightData: heightmap[y + x*H] = pixel(row=x, col=y)。
    def hgrid(xi, yi):
        if xi < 0 or yi < 0 or xi > h or yi > w:
            return None
        if xi == 0 or yi == 0 or xi == h or yi == w:
            return 0.0
        o = xi * stride + yi * bpp  # row = xi, col = yi
        v = (out[o] << 8) | out[o + 1]
        if v == 0xFFFF:
            return None
        return v / 32.0

    def terrain_hit(px, py, lo, hi):
        """复刻 collideNearXY 的垂直射线单元三角检测；返回带内最高的地形交点 z 或 None。"""
        x_n, y_w = int(px / 2), int(py / 2)
        z2 = hgrid(x_n, y_w + 1)
        if z2 is None:
            return None
        z3 = hgrid(x_n + 1, y_w)
        if z3 is None:
            return None
        z1 = hgrid(x_n, y_w)
        z4 = hgrid(x_n + 1, y_w + 1)
        x_nw, y_nw = x_n * 2, y_w * 2
        tri = []
        if z1 is not None:
            tri.append(((x_nw, y_nw, z1), (x_nw, y_nw + 2, z2), (x_nw + 2, y_nw, z3)))
        if z4 is not None:
            tri.append(((x_nw + 2, y_nw + 2, z4), (x_nw, y_nw + 2, z2), (x_nw + 2, y_nw, z3)))
        best = None
        for a, b, c in tri:
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            l1 = ((b[1] - c[1]) * (px - c[0]) + (c[0] - b[0]) * (py - c[1])) / den
            l2 = ((c[1] - a[1]) * (px - c[0]) + (a[0] - c[0]) * (py - c[1])) / den
            l3 = 1.0 - l1 - l2
            if l1 < -1e-6 or l2 < -1e-6 or l3 < -1e-6:
                continue
            z = l1 * a[2] + l2 * b[2] + l3 * c[2]
            if lo <= z <= hi and (best is None or z > best):
                best = z
        return best

    return terrain_hit


# ---------- 地面采样 ----------
def make_ground(grid, terrain_z):
    CELL = 2.0
    x0, y0 = ROI[0], ROI[1]

    def surface_z(px, py, ref_z):
        band_lo, band_hi = ref_z - 100.0, ref_z + 2.0
        best = None
        cell = (int((px - x0) // CELL), int((py - y0) // CELL))
        for a, b, c in grid.get(cell, ()):
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            l1 = ((b[1] - c[1]) * (px - c[0]) + (c[0] - b[0]) * (py - c[1])) / den
            l2 = ((c[1] - a[1]) * (px - c[0]) + (a[0] - c[0]) * (py - c[1])) / den
            l3 = 1.0 - l1 - l2
            if l1 < -1e-6 or l2 < -1e-6 or l3 < -1e-6:
                continue
            z = l1 * a[2] + l2 * b[2] + l3 * c[2]
            if band_lo <= z <= band_hi and (best is None or z > best):
                best = z
        tz = terrain_z(px, py, band_lo, band_hi)
        if tz is not None and (best is None or tz > best):
            best = tz
        return best

    return surface_z


def sgn(v):
    return (v > 0) - (v < 0)


def glp(origin, destination, sagittal, coronal):
    """复刻 WalkerGroup.getLinePoint（2026-10-06 已用实机目标点验证）。"""
    ox, oy = origin
    cx, cy = destination
    dir_s, dir_c = sgn(cx - ox), sgn(cy - oy)
    if oy - cy == 0:
        return (ox + dir_c * coronal, oy - dir_s * sagittal)
    if ox - cx == 0:
        return (ox + dir_c * sagittal, oy + dir_c * coronal)
    slope = (ox - cx) / (oy - cy)
    d = abs(sagittal) / ((1 + slope * slope) ** 0.5)
    if sagittal * dir_c < 0:
        result = (ox - d, oy + d * slope)
    else:
        result = (ox + d, oy - d * slope)
    if coronal != 0:
        rs = glp(origin, destination, sgn(sagittal) * abs(coronal) if sagittal != 0 else abs(coronal), 0)
        ddx, ddy = abs(ox - rs[0]), abs(oy - rs[1])
        if coronal < 0:
            if dir_s < 0 and dir_c < 0: result = (result[0] + ddy, result[1] + ddx)
            elif dir_s > 0 and dir_c > 0: result = (result[0] - ddy, result[1] - ddx)
            elif dir_s < 0 and dir_c > 0: result = (result[0] + ddy, result[1] - ddx)
            elif dir_s > 0 and dir_c < 0: result = (result[0] - ddy, result[1] + ddx)
        else:
            if dir_s < 0 and dir_c < 0: result = (result[0] - ddy, result[1] - ddx)
            elif dir_s > 0 and dir_c > 0: result = (result[0] + ddy, result[1] + ddx)
            elif dir_s < 0 and dir_c > 0: result = (result[0] - ddy, result[1] + ddx)
            elif dir_s > 0 and dir_c < 0: result = (result[0] + ddy, result[1] - ddx)
    return result


def main():
    print("loading meshes ...", flush=True)
    grid, tri_count = load_world_meshes()
    print("ROI triangles: %d" % tri_count, flush=True)
    print("loading terrain ...", flush=True)
    terrain_z = load_terrain()
    ground = make_ground(grid, terrain_z)

    def route_ground(step):
        x, y, z = ROUTE[step]
        return ground(x, y, z - 1)

    print("\n== 路线点地面（resolveRouteStepZ 同带 [z-101, z+1]... 实为 [z-1-100, z-1+2]） ==")
    for k in sorted(ROUTE):
        x, y, z = ROUTE[k]
        g = route_ground(k)
        print("step %2d (%8.2f,%8.2f) routeZ=%7.2f ground=%s" % (k, x, y, z, "None" if g is None else "%.3f" % g))

    print("\n== 跟随者航点 Z 误差（旧=上一步坐标地面；新=航点自身地面） ==")
    print("（offset=offsetsy 米；err=旧-新，>0 表示旧值偏高=客户端悬空，<0 偏低=入地）")
    for idx in range(1, 5):
        print("\n-- animal#%d offsets=(%d,%d)" % (idx, OFFX[idx], OFFY[idx]))
        for k in sorted(ROUTE):
            prev = k - 1 if k > 1 else 17
            p = ROUTE[prev]
            c = ROUTE[k]
            wx, wy = glp((p[0], p[1]), (c[0], c[1]), OFFX[idx], OFFY[idx])
            old = route_ground(prev)          # 旧实现：上一步坐标处地面
            new = ground(wx, wy, p[2] - 1)    # 新实现：航点自身地面（同一射线带）
            if old is None or new is None:
                print("  step->%2d wp=(%8.2f,%8.2f)  old=%s new=%s" % (k, wx, wy, old, new))
                continue
            err = old - new
            flag = "  <<<" if abs(err) >= 0.25 else ""
            print("  step->%2d wp=(%8.2f,%8.2f) 旧=%8.3f 新=%8.3f err=%+7.3f%s" % (k, wx, wy, old, new, err, flag))


if __name__ == "__main__":
    main()
