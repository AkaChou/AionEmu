#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-53 侦察（只读）：`RETAIL_ACQUIRE_NPC_UNRESOLVED` 余下 38 行的形状与候选名字通道。

输出三列证据：DD 行的接取类别/参数、步骤类别、以及该接取名在 npc 模板里的**形状家族**
（精确 / 前缀展开候选 / 阵营前缀候选 / 家族代表 无 / 完全不存在）。据此把 38 行分成
"变体通道可解"与"真端无此名（KEEP）"两堆，再决定是否值得动编译器。

用法：`python3 -B p0c53_acquire_variant_census.py [--out <tsv>]`
"""
from __future__ import annotations

import argparse
import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
DRIFT = REPO / "src/test/resources/quest/retail-data-driven-drift.tsv"
DD = REPO / "src/main/resources/aion/data/static_data/quest_retail/data_driven_quest.xml"
NPC_GLOB = "src/main/resources/aion/data/static_data/npcs/npc_template_*.xml"
CODE = "REJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED"


def unresolved_rows() -> list[tuple[str, str]]:
	rows = []
	for line in DRIFT.read_text(encoding="utf-8").splitlines():
		if not line or line.startswith("#"):
			continue
		parts = line.split("\t")
		if len(parts) > 1 and parts[1] == CODE:
			rows.append((parts[0], parts[2] if len(parts) > 2 else ""))
	return rows


def dd_rows() -> dict[str, tuple[str, str, list[str]]]:
	"""quest id -> (acquire category, acquire param, step categories)。"""
	text = DD.read_text(encoding="utf-8")
	body = text[text.find("<quest_data_drivens>"):]
	rows: dict[str, tuple[str, str, list[str]]] = {}
	for block in re.findall(r"<quest_data_driven>(.*?)</quest_data_driven>", body, re.S):
		match = re.search(r"<id>(\d+)</id>", block)
		if not match:
			continue
		category = re.search(r"<category_acquire_>([^<]*)</category_acquire_>", block)
		param = re.search(r"<value0_acquire_>([^<]*)</value0_acquire_>", block)
		categories = re.findall(r"<category_progress_>([^<]*)</category_progress_>", block)
		rows[match.group(1)] = (category.group(1) if category else "",
			(param.group(1).strip() if param else ""), categories)
	return rows


def template_names() -> list[str]:
	names: list[str] = []
	for path in sorted(REPO.glob(NPC_GLOB)):
		text = path.read_text(encoding="utf-8", errors="replace")
		names.extend(re.findall(r'name_desc="([^"]+)"', text))
	return names


def shapes(name: str, names: list[str]) -> tuple[str, str]:
	lower = name.lower()
	exact = [candidate for candidate in names if candidate.lower() == lower]
	if exact:
		return "EXACT", exact[0]
	prefix = [candidate for candidate in names if candidate.lower().startswith(lower + "_")]
	if prefix:
		return "PREFIX_EXPAND", ",".join(sorted(prefix)[:6])
	race = [candidate for candidate in names
		if candidate.lower().endswith("_" + lower) or ("_" + lower + "_") in candidate.lower()]
	if race:
		return "INFIX", ",".join(sorted(race)[:6])
	family = [candidate for candidate in names if lower in candidate.lower()]
	if family:
		return "CONTAINS", ",".join(sorted(family)[:6])
	return "ABSENT", ""


def main() -> int:
	parser = argparse.ArgumentParser()
	parser.add_argument("--out", default=None)
	args = parser.parse_args()
	rows = unresolved_rows()
	dd = dd_rows()
	names = template_names()
	lines = ["quest_id\tacquire_category\tacquire_param\tsteps\tshape\tcandidates"]
	summary: dict[str, int] = {}
	for quest_id, detail in rows:
		entry = dd.get(quest_id)
		category, param = (entry[0], entry[1]) if entry else ("?", detail.split(" -> ")[0])
		steps = "|".join(entry[2]) if entry else "?"
		shape, candidates = shapes(param, names)
		summary[shape] = summary.get(shape, 0) + 1
		lines.append(f"{quest_id}\t{category}\t{param}\t{steps}\t{shape}\t{candidates}")
	output = "\n".join(lines) + "\n"
	if args.out:
		Path(args.out).write_text(output, encoding="utf-8")
		print(f"wrote {args.out} rows={len(rows)}")
	print("shape histogram:", summary)
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
