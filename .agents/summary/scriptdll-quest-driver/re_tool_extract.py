#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按地址从反编译导出里抽出函数体（Server64 / ScriptDLL64 / NPCSvr64 通用）。

用法：
    python3 -B re_tool_extract.py <source.c> 1404db670 1404db620 ...
    python3 -B re_tool_extract.py <source.c> --grep 0x1b8
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

BODY_MARKER = re.compile(r"^/\* ([0-9a-f]{6,10}) \*/")


def index_functions(path: Path) -> tuple[list[str], dict[str, tuple[int, int]]]:
	"""返回 (行数组, 地址→行区间)。 / Return (lines, address to line span)."""
	lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
	spans: dict[str, tuple[int, int]] = {}
	current: str | None = None
	start = 0
	for index, line in enumerate(lines):
		match = BODY_MARKER.match(line)
		if match:
			if current is not None:
				spans[current] = (start, index)
			current = match.group(1)
			start = index
	if current is not None:
		spans[current] = (start, len(lines))
	return lines, spans


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("source", type=Path)
	parser.add_argument("addresses", nargs="*")
	parser.add_argument("--grep", default="")
	parser.add_argument("--max-lines", type=int, default=60)
	args = parser.parse_args(argv)

	lines, spans = index_functions(args.source)
	print(f"# {args.source.name}: {len(spans)} functions indexed", file=sys.stderr)
	if args.grep:
		pattern = re.compile(args.grep)
		hit = 0
		for address, (start, end) in spans.items():
			body = lines[start:end]
			if any(pattern.search(line) for line in body):
				hit += 1
				print(f"{address}\t{args.source.name}:{start + 1}")
		print(f"# grep hits {hit}", file=sys.stderr)
		return 0
	for address in args.addresses:
		key = address.lower().removeprefix("0x")
		span = spans.get(key)
		if span is None:
			print(f"=== {address}: NOT FOUND ===")
			continue
		start, end = span
		print(f"===== {address} ({args.source.name}:{start + 1}) =====")
		print("\n".join(lines[start: min(end, start + args.max_lines)]))
		print()
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
