#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""核验"类别哨兵接取名"在真端能否由 `npcfactions_quest.xml` 表达（阵营 + 星期发放）。

背景：家族表里 `acquired_npc_name = _faction_` 不是 NPC，而是"由阵营日常系统发放"的类别哨兵。
真端 `Map/XML/npcfactions_quest.xml` 给出 (quest_id -> 阵营名, 星期位)，本工具做覆盖率核验。

用法：
    python3 -B m5b2b_faction_grant_coverage.py [--retail-xml <dir>] [--out <tsv>]
"""
from __future__ import annotations

import argparse
import re
import sys
from collections import Counter
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
BASE = REPO_ROOT / ".agents/summary/scriptdll-quest-driver"
DEFAULT_RETAIL_XML = Path("/Users/mc/IdeaProjects/58Server/Map/XML")
DEFAULT_OUT = BASE / "m5b2b-faction-grant-coverage.tsv"
FAMILY_TABLES = (
	"Quest_SimpleCollectItem.xml",
	"Quest_SimpleHunt.xml",
	"Quest_SimpleItemPlay.xml",
	"Quest_SimpleSerialHunt.xml",
	"Quest_SimpleTalk.xml",
	"Quest_SimpleUseItem.xml",
	"Quest_CombineTask.xml",
	"data_driven_quest.xml",
)
STATE_BY_DAY = ("mon", "tue", "wed", "thu", "fri", "sat", "sun")
ROW = re.compile(r'<id id="(\d+)">(.*?)</id>', re.S)
ROW_DD = re.compile(r'<quest_id quest_id="(\d+)">(.*?)</quest_id>', re.S)
ACQUIRED = re.compile(r"<acquired_npc_name>([^<]*)</acquired_npc_name>")
FACTION_NAME = re.compile(r"<npcfaction_name>([^<]*)</npcfaction_name>")


def load_npcfactions(path: Path) -> dict[int, tuple[str, str]]:
	"""读真端 npcfactions_quest.xml（UTF-16）。 / Load the retail faction-daily grant table."""
	text = path.read_text(encoding="utf-16", errors="replace")
	grants: dict[int, tuple[str, str]] = {}
	for quest_id, body in ROW_DD.findall(text):
		name = FACTION_NAME.search(body)
		days = "".join("1" if f"<{day}>1</{day}>" in body else "0" for day in STATE_BY_DAY)
		grants[int(quest_id)] = (name.group(1) if name else "?", days)
	return grants


def family_rows(path: Path) -> list[tuple[int, str]]:
	"""读家族表 (quest_id, acquired_npc_name)。 / Load (quest_id, acquired_npc_name) rows."""
	text = path.read_text(encoding="utf-8", errors="replace")
	rows = ROW_DD.findall(text) if "quest_id" in text[:4000] else ROW.findall(text)
	out: list[tuple[int, str]] = []
	for quest_id, body in rows:
		match = ACQUIRED.search(body)
		if match:
			out.append((int(quest_id), match.group(1)))
	return out


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("--retail-xml", type=Path, default=DEFAULT_RETAIL_XML)
	parser.add_argument("--retail-dir", type=Path,
		default=REPO_ROOT / "src/main/resources/aion/data/static_data/quest_retail")
	parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
	args = parser.parse_args(argv)

	grants = load_npcfactions(args.retail_xml / "npcfactions_quest.xml")
	print(f"npcfactions_quest.xml 条目 / grant rows: {len(grants)}")

	lines = ["family\trows\tfaction_sentinel_rows\tnpcfaction_covered\tdisabled_all_zero\tuncovered_ids"]
	total_sentinel = 0
	total_covered = 0
	for table in FAMILY_TABLES:
		path = args.retail_dir / table
		if not path.is_file():
			print(f"WARN missing {table}", file=sys.stderr)
			continue
		rows = family_rows(path)
		sentinel = [quest for quest, acquired in rows if acquired in ("_faction_", "_challengetask_", "_area_")]
		faction_only = [quest for quest, acquired in rows if acquired == "_faction_"]
		covered = [quest for quest in faction_only if quest in grants]
		disabled = [quest for quest in covered if set(grants[quest][1]) == {"0"}]
		uncovered = sorted(set(faction_only) - set(covered))
		total_sentinel += len(faction_only)
		total_covered += len(covered)
		family = table.removesuffix(".xml")
		print(f"{family:<22} rows={len(rows):>5}  _faction_={len(faction_only):>3}  "
			f"covered={len(covered):>3}  disabled={len(disabled):>3}  uncovered={len(uncovered):>3}")
		lines.append("\t".join(
			[family, str(len(rows)), str(len(sentinel)), str(len(covered)), str(len(disabled)),
				",".join(str(q) for q in uncovered[:20])]))

	print(f"\n合计 _faction_ 行 / total faction rows: {total_sentinel}；可由 npcfactions 覆盖: {total_covered}")
	days = Counter(mask for _, mask in grants.values())
	print("真端星期位分布（mon..sun） / weekday masks in retail:")
	for mask, count in days.most_common(8):
		print(f"  {mask}  {count}")
	args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
	print(f"wrote {args.out}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
