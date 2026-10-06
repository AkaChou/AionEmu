#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""反解 LF1A_NPCPath_Ermona 编队观测目标：忠实复刻 WalkerGroup.getLinePoint 后按路线步对暴力匹配。"""
import re, itertools

ROUTE = [
    (1, 1659.969971, 1470.829956, 124.100006),
    (2, 1665.554688, 1465.856323, 124.100006),
    (3, 1674.187012, 1463.226929, 124.100006),
    (4, 1682.106323, 1466.245239, 124.100006),
    (5, 1683.682739, 1473.610352, 124.100006),
    (6, 1683.656738, 1481.128418, 124.100006),
    (7, 1689.688354, 1488.808350, 124.100006),
    (8, 1688.986694, 1496.266235, 124.100006),
    (9, 1683.711060, 1502.411987, 124.100006),
    (10, 1674.959473, 1503.821289, 124.100006),
    (11, 1665.865845, 1502.906860, 124.100006),
    (12, 1658.484131, 1504.036743, 124.100006),
    (13, 1657.537598, 1504.802979, 124.100006),
    (14, 1655.742065, 1497.651001, 124.100006),
    (15, 1654.734497, 1489.943359, 124.100006),
    (16, 1653.685059, 1480.657227, 124.100006),
    (17, 1659.401611, 1471.428467, 124.100006),
]

def sgn(v):
    return (v > 0) - (v < 0)

def get_line_point(origin, destination, sagittal, coronal):
    ox, oy = origin
    dx_, dy_ = destination
    dir_sag, dir_cor = sgn(dx_ - ox), sgn(dy_ - oy)
    if oy - dy_ == 0:
        return (ox + dir_cor * coronal, oy - dir_sag * sagittal)
    if ox - dx_ == 0:
        return (ox + dir_cor * sagittal, oy + dir_cor * coronal)
    slope = (ox - dx_) / (oy - dy_)
    d = abs(sagittal) / ((1 + slope * slope) ** 0.5)
    if sagittal * dir_cor < 0:
        result = (ox - d, oy + d * slope)
    else:
        result = (ox + d, oy - d * slope)
    if coronal != 0:
        rs_sag = sgn(sagittal) * abs(coronal) if sagittal != 0 else 0
        rx, ry = get_line_point(origin, destination, rs_sag, 0)
        ddx, ddy = abs(ox - rx), abs(oy - ry)
        if coronal < 0:
            if dir_sag < 0 and dir_cor < 0: result = (result[0] + ddy, result[1] + ddx)
            elif dir_sag > 0 and dir_cor > 0: result = (result[0] - ddy, result[1] - ddx)
            elif dir_sag < 0 and dir_cor > 0: result = (result[0] + ddy, result[1] - ddx)
            elif dir_sag > 0 and dir_cor < 0: result = (result[0] - ddy, result[1] + ddx)
        else:
            if dir_sag < 0 and dir_cor < 0: result = (result[0] - ddy, result[1] - ddx)
            elif dir_sag > 0 and dir_cor > 0: result = (result[0] + ddy, result[1] + ddx)
            elif dir_sag < 0 and dir_cor > 0: result = (result[0] - ddy, result[1] + ddx)
            elif dir_sag > 0 and dir_cor < 0: result = (result[0] + ddy, result[1] - ddx)
    return result

# 观测目标（来自 aidebug.log 16:55:59-17:00:28 的目标切换）
OBSERVED = [
    (1673.7738, 1501.6964), (1653.8928, 1477.2296), (1654.0049, 1476.1517),
    (1657.9003, 1468.1925), (1666.7083, 1460.3754), (1680.4319, 1458.4224),
    (1683.7139, 1465.6105), (1678.7223, 1474.8362), (1690.4386, 1480.8446),
    (1694.1993, 1490.1989), (1691.6104, 1501.1408), (1682.9231, 1504.6163),
    (1664.6970, 1499.0020),
]

# 模板偏移：offsetsx="0,-1,1,0,0" offsetsy="0,-2,-3,-6,-8"
OFFX = [0, -1, 1, 0, 0]
OFFY = [0, -2, -3, -6, -8]

print("== 观测目标与路线步/偏移的匹配（容差 0.6m） ==")
for (tx, ty) in OBSERVED:
    hits = []
    n = len(ROUTE)
    for i in range(n):
        s1 = ROUTE[i]                # cur
        s2 = ROUTE[(i - 1) % n]      # prev
        for idx in range(5):
            for (sag, cor) in [(OFFX[idx], OFFY[idx]), (OFFY[idx], OFFX[idx])]:
                px, py = get_line_point((s2[1], s2[2]), (s1[1], s1[2]), sag, cor)
                if ((px - tx) ** 2 + (py - ty) ** 2) ** 0.5 <= 0.6:
                    hits.append((s2[0], s1[0], idx, sag, cor, px, py))
    print("tgt(%.1f, %.1f):" % (tx, ty), "NO MATCH" if not hits else "")
    for h in hits[:6]:
        print("   prevStep=%d curStep=%d idx=%d shift=(%d,%d) -> (%.2f, %.2f)" % h)
