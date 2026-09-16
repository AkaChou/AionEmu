#!/usr/bin/env python3
"""离线复现 PathData.projectPoint，统计"PATH 投影失败"的刷点（只读）。

复现对象：
  PathService.projectGroundPoint(worldId, x, y, refZ)
    -> PathData.MapData.projectPoint(x, y, refZ, terrain)
    -> findNode(gridX, gridY, refZ) -> Sector.find(..., maxVerticalDelta = 0.7f)

失败（返回 null）的含义：SpawnEngine.projectedSpawnZ 会继续走 terrain 兜底；
如果该 world 没有 terrain（TERRAIN_DISABLED_MAPS / 无 PNG），就只能退回 XML 里的 z。

用法：python3 .agents/summary/spawn-z-audit/audit_path_projection.py
输出：stdout + 同目录 path_projection.csv / path_projection_report.md
"""
import csv
import gzip
import hashlib
import importlib.util
import math
import os
import struct
import sys
from collections import defaultdict

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location(
    "spawn_gaps", os.path.join(HERE, "audit_projection_gaps.py"))
gaps = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(gaps)

GEO_DIR = gaps.GEO_DIR
PATH_DIR = os.path.join(GEO_DIR, "path")
INDEX_MAGIC = 0x58504941
INDEX_VERSION = 1
PATH_VERSION_MAJOR = 6
MAX_VERTICAL_DELTA = 0.7


def payload_size(sector_type):
    return {0: 4, 2: 0, 4: 4096, 6: 128, 8: 264, 10: 568, 12: 1028, 14: 2052}[sector_type & ~1]


class PathReader:
    """按 PathData.java 的格式读取一个 world 的 PATH 数据。"""

    def __init__(self, world_id):
        self.world_id = world_id
        with open(os.path.join(PATH_DIR, "%d.idx" % world_id), "rb") as f:
            raw = f.read()
        if len(raw) < 84:
            raise ValueError("index too small")
        (magic, version, width, height, cols, rows, node_off, node_size,
         portal_off, portal_count) = struct.unpack_from("<10i", raw, 0)
        if magic != INDEX_MAGIC or version != INDEX_VERSION:
            raise ValueError("bad index header")
        expected_size = struct.unpack_from("<q", raw, 40)[0]
        digest = raw[48:80]
        block_count = struct.unpack_from("<i", raw, 80)[0]
        if portal_count < 0 or block_count != cols * rows or len(raw) != 84 + block_count * 4:
            raise ValueError("bad index dimensions")
        self.width, self.height = width, height
        self.block_columns, self.block_rows = cols, rows
        self.node_table_offset, self.node_table_size = node_off, node_size
        self.block_offsets = list(struct.unpack_from("<%di" % block_count, raw, 84))
        with gzip.open(os.path.join(PATH_DIR, "%d.path.gz" % world_id), "rb") as f:
            data = f.read()
        if len(data) != expected_size:
            raise ValueError("path size mismatch: %d != %d" % (len(data), expected_size))
        if hashlib.sha256(data).digest() != digest:
            raise ValueError("path sha256 mismatch")
        if (struct.unpack_from("<i", data, 16)[0] >> 16) != PATH_VERSION_MAJOR:
            raise ValueError("unsupported path version")
        self.data = data
        self._blocks = {}

    # ---------- block / sector ----------
    def _read_block(self, block_id):
        cursor = self.block_offsets[block_id]
        end = self.block_offsets[block_id + 1] if block_id + 1 < len(self.block_offsets) else len(self.data)
        count = self.data[cursor]
        cursor += 1
        sectors = [None] * count
        for file_order in range(count):
            layer = count - file_order - 1
            sector_type = self.data[cursor]
            cursor += 1
            if sector_type == 16:
                base = struct.unpack_from("<i", self.data, cursor)[0]
                nodes = struct.unpack_from("<h", self.data, cursor + 4)[0]
                cursor += 6
                sectors[layer] = (16, base, nodes, -1)
                continue
            if sector_type > 15:
                raise ValueError("bad sector type %d" % sector_type)
            cursor += 1  # boundaryMask
            cursor += 16  # 4 boundaries
            payload = cursor
            cursor += payload_size(sector_type)
            links = -1
            if sector_type & 1:
                links = cursor
                cursor += 512
            sectors[layer] = (sector_type, payload, links, -1)
        if cursor != end:
            # 自我校验：解析长度必须与 index 中的 block 边界一致
            self._sector_mismatch = getattr(self, "_sector_mismatch", 0) + 1
        return sectors

    def _block(self, block_id):
        cached = self._blocks.get(block_id)
        if cached is None:
            cached = self._read_block(block_id)
            self._blocks[block_id] = cached
        return cached

    def _block_id(self, grid_x, grid_y):
        if grid_x < 0 or grid_y < 0 or grid_x >= self.width * 2 or grid_y >= self.height * 2:
            return -1
        column = grid_x >> 5
        row = grid_y >> 5
        if column >= self.block_columns or row >= self.block_rows:
            return -1
        return row * self.block_columns + column

    # ---------- height ----------
    def _height_at(self, sector_type, payload, grid_x, grid_y, terrain):
        local_x = grid_x & 31
        local_y = grid_y & 31
        cell = local_y * 32 + local_x
        t = sector_type & ~1
        if t == 0:
            return struct.unpack_from("<i", self.data, payload)[0] / 100.0
        if t == 2:
            return terrain(grid_x * 0.5 + 0.25, grid_y * 0.5 + 0.25)
        if t == 4:
            value = struct.unpack_from("<i", self.data, payload + cell * 4)[0]
            return math.nan if value == 2147483647 else value / 100.0
        if t == 6:
            byte = self.data[payload + local_y * 4 + local_x // 8]
            if (byte >> (local_x & 7)) & 1:
                return math.nan
            return terrain(grid_x * 0.5 + 0.25, grid_y * 0.5 + 0.25)
        if t == 8:
            byte = self.data[payload + 8 + local_y * 8 + local_x // 4]
            code = (byte >> ((local_x & 3) * 2)) & 3
            if code < 2:
                return struct.unpack_from("<i", self.data, payload + code * 4)[0] / 100.0
            if code == 2:
                return terrain(grid_x * 0.5 + 0.25, grid_y * 0.5 + 0.25)
            return math.nan
        if t == 10:
            byte = self.data[payload + 56 + local_y * 16 + local_x // 2]
            code = (byte & 0xF) if (local_x & 1) == 0 else (byte >> 4)
            if code < 14:
                return struct.unpack_from("<i", self.data, payload + code * 4)[0] / 100.0
            if code == 14:
                return terrain(grid_x * 0.5 + 0.25, grid_y * 0.5 + 0.25)
            return math.nan
        if t == 12:
            value = self.data[payload + 4 + cell]
            if value < 0xFE:
                return (struct.unpack_from("<i", self.data, payload)[0] + value) / 100.0
            if value == 0xFE:
                return terrain(grid_x * 0.5 + 0.25, grid_y * 0.5 + 0.25)
            return math.nan
        if t == 14:
            value = struct.unpack_from("<H", self.data, payload + 4 + cell * 2)[0]
            if value < 0xFFFE:
                return (struct.unpack_from("<i", self.data, payload)[0] + value) / 100.0
            if value == 0xFFFE:
                return terrain(grid_x * 0.5 + 0.25, grid_y * 0.5 + 0.25)
            return math.nan
        return math.nan

    # ---------- nodes ----------
    def _simple_node(self, block_id, sector_type, payload, grid_x, grid_y, terrain):
        if sector_type == 16 or self._block_id(grid_x, grid_y) != block_id:
            return None
        z = self._height_at(sector_type, payload, grid_x, grid_y, terrain)
        return None if not math.isfinite(z) else z

    def _complex_offsets(self, base, count):
        offsets = []
        cursor = base
        for _ in range(count):
            offsets.append(cursor)
            descriptor = self.data[self.node_table_offset + cursor + 8]
            cursor += 9
            for direction in range(4):
                mode = (descriptor >> (direction * 2)) & 3
                cursor += 2 if mode == 1 else (4 if mode == 2 else 0)
        return offsets

    def _complex_node(self, offset):
        if offset < 0 or offset + 9 > self.node_table_size:
            return None
        position = self.node_table_offset + offset
        z = struct.unpack_from("<i", self.data, position)[0] / 100.0
        x = struct.unpack_from("<H", self.data, position + 4)[0]
        y = struct.unpack_from("<H", self.data, position + 6)[0]
        return (x, y, z)

    def project(self, x, y, z, terrain):
        """返回投影到的 z；失败返回 None（与 Java projectPoint 一致）。"""
        grid_x = int(x * 2)
        grid_y = int(y * 2)
        block_id = self._block_id(grid_x, grid_y)
        if block_id < 0:
            return None
        for sector in self._block(block_id):
            if sector is None:
                continue
            if sector[0] != 16:
                node_z = self._simple_node(block_id, sector[0], sector[1], grid_x, grid_y, terrain)
                if node_z is not None and abs(node_z - z) < MAX_VERTICAL_DELTA:
                    return node_z
                continue
            best, difference = None, MAX_VERTICAL_DELTA
            for offset in self._complex_offsets(sector[1], sector[2]):
                node = self._complex_node(offset)
                if node is not None and node[0] == grid_x and node[1] == grid_y:
                    current = abs(node[2] - z)
                    if current < difference:
                        best, difference = node[2], current
            if best is not None:
                return best
        return None


class TerrainProvider:
    """复现 Terrain.getPathHeight（只在有 terrain 的 world 上启用，用于校验）。"""

    def __init__(self, world_id):
        image = Image.open(os.path.join(GEO_DIR, "%d.png" % world_id))
        self.pixels = image.load()
        self.hm_x_size, self.hm_y_size = image.size

    def _sample(self, x, y):
        if x < 0 or y < 0 or x >= self.hm_x_size or y >= self.hm_y_size:
            return math.nan
        raw = self.pixels[y, x]  # 与 readHeightData 的转置取样一致
        if raw == 0xFFFF:
            return math.nan
        return (raw & 0xFFFC) / 32.0

    def __call__(self, world_x, world_y):
        sample_x, sample_y = world_x / 2.0, world_y / 2.0
        x0, y0 = math.floor(sample_x), math.floor(sample_y)
        if x0 < 0 or y0 < 0 or x0 + 1 >= self.hm_x_size or y0 + 1 >= self.hm_y_size:
            return math.nan
        fx, fy = sample_x - x0, sample_y - y0
        top = self._sample(x0, y0) * (1 - fx) + self._sample(x0 + 1, y0) * fx
        bottom = self._sample(x0, y0 + 1) * (1 - fx) + self._sample(x0 + 1, y0 + 1) * fx
        return top * (1 - fy) + bottom * fy


def nan_terrain(_x, _y):
    return math.nan


def validate(per_world, npc_speed):
    """在 Poeta(210010000，有 terrain) 上对照已知结论，确认复现正确。"""
    print("== 校验：Poeta 210010000（有 terrain）")
    reader = PathReader(210010000)
    terrain = TerrainProvider(210010000)
    spots = per_world.get(210010000, [])
    mobile = [s for s in spots if not s["fly"] and npc_speed.get(s["npc_id"], 1.0) > 0]
    ok = sum(1 for s in mobile if reader.project(s["x"], s["y"], s["z"], terrain) is not None)
    print("   可移动刷点 %d，PATH 投影成功 %d（%.1f%%）" % (len(mobile), ok, 100.0 * ok / max(1, len(mobile))))
    probes = [
        ("retail z=120.0（commit 1ab8aaeb3 之后）", 636.376282, 854.420715, 120.0, None),
        ("legacy z=104.629166（地面值）", 549.623718, 1198.683960, 104.629166, 104.629166),
    ]
    for label, x, y, z, _expected in probes:
        result = reader.project(x, y, z, terrain)
        print("   %-38s project(%.3f, %.3f, %.3f) -> %s" % (
            label, x, y, z, "null" if result is None else "z=%.4f" % result))
    print("   block 解析长度不一致的 block 数: %d" % getattr(reader, "_sector_mismatch", 0))
    return reader


def main():
    disabled = gaps.parse_terrain_disabled_maps()
    geo, terrain_raw, path = gaps.collect_geo_sources()
    terrain_worlds = {w for w in terrain_raw if w not in disabled}
    npc_speed = gaps.collect_npc_speeds()
    per_world = gaps.collect_spots()
    active = gaps.active_world_ids()

    validate(per_world, npc_speed)

    targets = sorted(w for w in per_world
                     if w in active and w in geo and w in path and w not in terrain_worlds)
    print()
    print("== 目标：%d 个'有 geo+PATH、无 terrain'的已加载 world" % len(targets))

    rows = []
    samples = []
    for world_id in targets:
        mobile = [s for s in per_world[world_id]
                  if not s["fly"] and npc_speed.get(s["npc_id"], 1.0) > 0 and not s["resolve_z"]]
        if not mobile:
            continue
        reader = PathReader(world_id)
        failed = []
        for spot in mobile:
            if reader.project(spot["x"], spot["y"], spot["z"], nan_terrain) is None:
                failed.append(spot)
        rows.append((world_id, len(mobile), len(failed)))
        for spot in failed[:3]:
            samples.append((world_id, spot["npc_id"], spot["x"], spot["y"], spot["z"], spot["file"]))
        print("   %-10d 依赖PATH的点 %5d  投影失败 %5d (%.1f%%)" % (
            world_id, len(mobile), len(failed), 100.0 * len(failed) / len(mobile)))
        del reader

    total = sum(r[1] for r in rows)
    failed_total = sum(r[2] for r in rows)
    print()
    print("合计：依赖 PATH 的可移动刷点 %d，其中投影失败 %d（%.1f%%）"
          % (total, failed_total, 100.0 * failed_total / max(1, total)))

    with open(os.path.join(HERE, "path_projection.csv"), "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["world_id", "path_dependent_mobile_spots", "projection_failed"])
        for row in sorted(rows, key=lambda r: -r[2]):
            writer.writerow(row)

    with open(os.path.join(HERE, "path_projection_report.md"), "w", encoding="utf-8") as f:
        f.write("# PATH 投影失败统计（离线复现 PathData.projectPoint）\n\n")
        f.write("范围：已加载世界中**没有 terrain**、依赖 PATH 兜底的可移动非飞行刷点"
                "（`resolve_z` 点走 geo，不计入）。\n\n")
        f.write("- 依赖 PATH 的刷点总数：%d\n- PATH 投影失败（会退回 XML z）：%d（%.1f%%）\n\n"
                % (total, failed_total, 100.0 * failed_total / max(1, total)))
        f.write("| world_id | 依赖 PATH 的点 | 投影失败 | 失败率 |\n|---|---|---|---|\n")
        for row in sorted(rows, key=lambda r: -r[2]):
            f.write("| %d | %d | %d | %.1f%% |\n" % (
                row[0], row[1], row[2], 100.0 * row[2] / max(1, row[1])))
        f.write("\n## 失败样例\n\n| world_id | npc_id | x | y | z | 文件 |\n|---|---|---|---|---|---|\n")
        for s in samples[:60]:
            f.write("| %d | %d | %.3f | %.3f | %.3f | %s |\n" % s)
    print("已写出: path_projection.csv / path_projection_report.md")
    return 0


if __name__ == "__main__":
    sys.exit(main())
