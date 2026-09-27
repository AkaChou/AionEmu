#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""从真端家族表 XML 抽取任务 ID，产出"首列 = quest_id"的 TSV 供其它探针复用。

家族表在 `src/main/resources/aion/data/static_data/quest_retail/Quest_<Family>.xml`，
任务行统一是 `<id id="NNNN">`。

用法：
    python3 -B m5b2b_family_ids.py [--retail-dir <dir>] [--out-dir <dir>] [--only SimpleTalk ...]
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
BASE = REPO_ROOT / ".agents/summary/scriptdll-quest-driver"
DEFAULT_RETAIL_DIR = REPO_ROOT / "src/main/resources/aion/data/static_data/quest_retail"
# 任务行：<id id="1103"> / Quest row: <id id="1103">
QUEST_ID = re.compile(r'<id\s+id="(\d+)"')


def extract(quest_table: Path) -> list[int]:
	"""抽取该表全部任务 ID（保持文件顺序，去重）。 / Extract quest ids from a retail family table."""
	seen: dict[int, None] = {}
	for match in QUEST_ID.finditer(quest_table.read_text(encoding="utf-8", errors="replace")):
		seen[int(match.group(1))] = None
	return list(seen)


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("--retail-dir", type=Path, default=DEFAULT_RETAIL_DIR)
	parser.add_argument("--out-dir", type=Path, default=BASE)
	parser.add_argument("--only", nargs="*", default=None, help="只处理这些家族（如 SimpleTalk CombineTask）")
	args = parser.parse_args(argv)

	families = sorted(path.stem.removeprefix("Quest_") for path in args.retail_dir.glob("Quest_*.xml"))
	if args.only:
		wanted = {name.removeprefix("Quest_") for name in args.only}
		families = [name for name in families if name in wanted]
	if not families:
		print("no family table matched / 未匹配到家族表", file=sys.stderr)
		return 1

	for family in families:
		table = args.retail_dir / f"Quest_{family}.xml"
		ids = extract(table)
		out = args.out_dir / f"m5b2b-family-ids-{family}.tsv"
		out.write_text("quest_id\n" + "\n".join(str(q) for q in ids) + "\n", encoding="utf-8")
		print(f"{family:<20} {len(ids):>5} ids -> {out.name}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
