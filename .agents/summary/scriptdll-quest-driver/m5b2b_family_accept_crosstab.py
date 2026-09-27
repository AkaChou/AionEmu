#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把各家族的 `m5b2b-client-accept-page-<Family>.tsv` 汇总成一张跨家族交叉表。

判据三元：DLL 生命周期三元组 {0x1c,0x1d,0x26} 有无 × 客户端接取页样式（select1 / ask_quest_accept / 无 html）。

用法：
    python3 -B m5b2b_family_accept_crosstab.py [--base <dir>] [--out <tsv>]
"""
from __future__ import annotations

import argparse
import sys
from collections import Counter
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
BASE = REPO_ROOT / ".agents/summary/scriptdll-quest-driver"
DEFAULT_OUT = BASE / "m5b2b-family-accept-crosstab.tsv"
FAMILIES = (
	"CombineTask",
	"SimpleCollectItem",
	"SimpleHunt",
	"SimpleItemPlay",
	"SimpleSerialHunt",
	"SimpleTalk",
	"SimpleUseItem",
)


def load(path: Path) -> list[list[str]]:
	"""读单家族客户端接取页 TSV。 / Load one family's client accept page TSV."""
	lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
	return [line.split("\t") for line in lines[1:] if line.strip()]


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("--base", type=Path, default=BASE)
	parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
	args = parser.parse_args(argv)

	grand: Counter[tuple[str, str, str]] = Counter()
	out_rows: list[str] = ["family\trows\ttriplet_status\tclient_accept_style\tcount"]
	for family in FAMILIES:
		path = args.base / f"m5b2b-client-accept-page-{family}.tsv"
		if not path.is_file():
			print(f"WARN missing {path.name}", file=sys.stderr)
			continue
		rows = load(path)
		counter: Counter[tuple[str, str, str]] = Counter()
		for row in rows:
			style = row[3] if row[4] != "MISSING" else "NO_HTML"
			counter[(row[2], style)] += 1
			if row[4] != "MISSING":
				grand[(row[2], style)] += 1
		print(f"### {family}  rows={len(rows)}")
		for (triplet, style), count in sorted(counter.items()):
			print(f"  {triplet:<16} {style:<18} {count}")
			out_rows.append(f"{family}\t{len(rows)}\t{triplet}\t{style}\t{count}")
	print("\n=== 跨家族合计（不含 NO_HTML） / grand total (NO_HTML excluded) ===")
	for (triplet, style), count in sorted(grand.items()):
		print(f"  {triplet:<16} {style:<18} {count}")
		out_rows.append(f"ALL\t-\t{triplet}\t{style}\t{count}")

	args.out.write_text("\n".join(out_rows) + "\n", encoding="utf-8")
	print(f"\nwrote {args.out}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
