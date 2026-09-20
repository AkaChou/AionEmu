#!/usr/bin/env python3
"""复用既有审计脚本，输出每个候选点的最高物理面与容差带。

Reuses the existing audit script to print the highest physical surface at every
candidate spawn, so the 1m/2m surface tolerance bands can be re-derived.

Usage:
  python3 .agents/summary/spawn-z-audit/audit_surface_delta.py \
    > .agents/summary/spawn-z-audit/audit_surface_delta_full.txt
"""
import importlib.util
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
AUDIT = os.path.join(ROOT, ".agents", "summary", "inggison-somation-rock-z", "audit_rocks_vs_spawns.py")
WORLD_REPORT = os.path.join(ROOT, ".agents", "summary", "inggison-somation-rock-z", "audit_full_output.txt")


def load_worlds():
    """从上一轮审计报告中提取 world 列表并按出现顺序去重。

    Loads the world list from the previous audit report, preserving first-seen order.
    """
    worlds = []
    with open(WORLD_REPORT, encoding="utf-8") as report:
        for line in report:
            match = re.match(r"world (\d+):", line)
            if match:
                world = int(match.group(1))
                if world not in worlds:
                    worlds.append(world)
    return worlds


def main():
    spec = importlib.util.spec_from_file_location("audit_rocks_vs_spawns", AUDIT)
    audit = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(audit)
    audit.DELTA = 1e9
    sys.argv = ["audit_surface_delta"] + [str(world) for world in load_worlds()]
    audit.main()


if __name__ == "__main__":
    main()
