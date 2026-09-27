#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-52 归因工具：对拍两份 T3 日志的失败方法集合，算本片（24 行退役）的机械 delta。

T3 日志里的 `[ERROR]   Class.method:line msg` 汇总段是唯一的失败清单；本工具只做集合算术，
不解释失败原因。用法：`python3 -B p0c52_t3_attribution.py <baseline.log> <candidate.log>`。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROW = re.compile(r"^\[ERROR\]\s+([A-Za-z0-9_]+)\.([A-Za-z0-9_$]+):(\d+)\s*(.*)$")


def failures(path: Path) -> dict[str, str]:
	result: dict[str, str] = {}
	for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
		match = ROW.match(line)
		if match:
			key = f"{match.group(1)}.{match.group(2)}"
			result.setdefault(key, f"L{match.group(3)} {match.group(4).strip()[:120]}")
	return result


def main() -> int:
	if len(sys.argv) != 3:
		print("usage: p0c52_t3_attribution.py <baseline.log> <candidate.log>")
		return 2
	baseline = failures(Path(sys.argv[1]))
	candidate = failures(Path(sys.argv[2]))
	added = sorted(set(candidate) - set(baseline))
	removed = sorted(set(baseline) - set(candidate))
	print(f"baseline={len(baseline)} candidate={len(candidate)}")
	print(f"ADDED x{len(added)} (in candidate only):")
	for key in added:
		print(f"  + {key}  {candidate[key]}")
	print(f"REMOVED x{len(removed)} (in baseline only):")
	for key in removed:
		print(f"  - {key}  {baseline[key]}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
