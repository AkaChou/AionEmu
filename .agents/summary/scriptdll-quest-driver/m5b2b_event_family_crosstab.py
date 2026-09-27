#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按真端家族 × ScriptDLL64 事件码做交叉表，用家族差异反推事件码语义。

方法：事件码语义无法从单个任务看出来，但**家族之间的差异**能看出来：
- 击杀族（SimpleHunt）若某事件码 100% 出现、采集族 0% 出现，该事件码就是"击杀"。
预期输入：`m5b2b-quest-event-census.tsv`（由 `m5b2b_handler_slot_scan.py --all --tsv` 生成）。

用法：
    python3 -B m5b2b_event_family_crosstab.py [--out <tsv>]
"""
from __future__ import annotations

import argparse
import re
import sys
from collections import Counter
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
CENSUS = REPO_ROOT / ".agents/summary/scriptdll-quest-driver/m5b2b-quest-event-census.tsv"
RETAIL_DIR = REPO_ROOT / "src/main/resources/aion/data/static_data/quest_retail"
DEFAULT_OUT = REPO_ROOT / ".agents/summary/scriptdll-quest-driver/m5b2b-event-family-crosstab.tsv"
# 家族名 → 真端表文件 / Family name to retail table file
FAMILIES = {
	"SimpleHunt": "Quest_SimpleHunt.xml",
	"SimpleCollectItem": "Quest_SimpleCollectItem.xml",
	"SimpleTalk": "Quest_SimpleTalk.xml",
	"CombineTask": "Quest_CombineTask.xml",
	"SimpleUseItem": "Quest_SimpleUseItem.xml",
	"SimpleItemPlay": "Quest_SimpleItemPlay.xml",
	"SimpleSerialHunt": "Quest_SimpleSerialHunt.xml",
}
# 任务 ID 属性（真端表口径）/ Quest id attribute used by the retail tables
QUEST_ID_PATTERN = re.compile(r'\b(?:quest_id|id)="(\d{3,6})"')


def load_census() -> dict[int, set[str]]:
	"""读事件码普查结果，忽略 quest-id 绑定行。 / Load the census, skipping quest-id bind rows."""
	events: dict[int, set[str]] = {}
	for line in CENSUS.read_text(encoding="utf-8", errors="replace").splitlines()[1:]:
		parts = line.split("\t")
		if len(parts) < 4 or parts[3].startswith("qid:") or not parts[0].isdigit():
			continue
		events.setdefault(int(parts[0]), set()).add(parts[3])
	return events


def load_family_ids(file_name: str) -> set[int]:
	"""从真端家族表提取任务 ID。 / Extract quest ids from a retail family table."""
	path = RETAIL_DIR / file_name
	if not path.is_file():
		return set()
	text = path.read_text(encoding="utf-8", errors="replace")
	return {int(match) for match in QUEST_ID_PATTERN.findall(text)}


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
	args = parser.parse_args(argv)

	events = load_census()
	print(f"census: {len(events)} quests with event registrations / 普查任务对象 {len(events)}")
	rows: list[tuple[str, int, int, str, int, float]] = []
	summary: dict[str, dict[str, float]] = {}
	for family, file_name in FAMILIES.items():
		ids = sorted(load_family_ids(file_name))
		registered = [quest for quest in ids if quest in events]
		counts: Counter[str] = Counter()
		for quest in registered:
			for event in events[quest]:
				counts[event] += 1
		total = len(ids)
		summary[family] = {
			event: count / len(registered) if registered else 0.0
			for event, count in counts.items()
		}
		for event, count in sorted(counts.items(), key=lambda item: -item[1]):
			rows.append(
				(family, total, len(registered), event, count, count / len(registered) if registered else 0.0)
			)

	# 组合所有家族里出现过的事件码 / Union of every event index seen in these families
	all_events = sorted({row[3] for row in rows}, key=lambda token: int(token, 16))
	header = ["family", "family_size", "registered", "event_idx", "count", "ratio"]
	print("\t".join(header))
	for row in rows:
		print("\t".join([row[0], str(row[1]), str(row[2]), row[3], str(row[4]), f"{row[5]:.3f}"]))
	args.out.write_text(
		"\n".join(
			["\t".join(header)]
			+ ["\t".join([r[0], str(r[1]), str(r[2]), r[3], str(r[4]), f"{r[5]:.3f}"]) for r in rows]
		)
		+ "\n",
		encoding="utf-8",
	)

	print("\n=== 家族覆盖率（行=事件码，列=家族）/ Coverage matrix ===")
	print("event  " + "".join(f"{family[:9]:>11s}" for family in FAMILIES))
	for event in all_events:
		rough = f"{event:6s} "
		print(rough + "".join(f"{summary[f].get(event, 0.0):11.2f}" for f in FAMILIES))
	print(f"\nwrote {args.out}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
