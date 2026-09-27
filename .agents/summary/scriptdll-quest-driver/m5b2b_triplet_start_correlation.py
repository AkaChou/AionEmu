#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""用真端 DLL 的事件码签名去复核"合成器拒绝"的批次是否**本来就是另一类任务**。

思路：漂移登记里的 `REJECTED:RETAIL_ACQUIRE_NPC_SENTINEL` 意味着真端接取 NPC 是哨兵（值 0）。
如果这批任务在 DLL 里也**没有** `{0x1c,0x1d,0x26}` 三元组，那么"接取不走 NPC 对话"就有两条独立证据，
说明它们属于不同形状（不是解析缺口），逐条处置口径也就不同。

用法：
    python3 -B m5b2b_triplet_start_correlation.py [--family-src <drift.tsv>] [--out <tsv>]
"""
from __future__ import annotations

import argparse
import sys
from collections import Counter, defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
BASE = REPO_ROOT / ".agents/summary/scriptdll-quest-driver"
CENSUS = BASE / "m5b2b-quest-event-census.tsv"
CONTRACT_INDEX = REPO_ROOT / "docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv"
DEFAULT_FAMILY = REPO_ROOT / "src/test/resources/quest/retail-simple-collect-item-drift.tsv"
DEFAULT_OUT = BASE / "m5b2b-triplet-vs-sentinel-start.tsv"
# 生命周期三元组 / Lifecycle triplet
TRIPLET = ("0x1c", "0x1d", "0x26")


def load_events() -> dict[int, set[str]]:
	"""读事件码普查表。 / Load the event census."""
	events: dict[int, set[str]] = defaultdict(set)
	for line in CENSUS.read_text(encoding="utf-8", errors="replace").splitlines()[1:]:
		parts = line.split("\t")
		if len(parts) < 4 or parts[3].startswith("qid:") or not parts[0].isdigit():
			continue
		events[int(parts[0])].add(parts[3])
	return events


def load_contract_index() -> dict[int, list[str]]:
	"""读真端对话契约索引（含 start_npc_ids / end_npc_ids）。 / Load the retail dialog contract index."""
	index: dict[int, list[str]] = {}
	for line in CONTRACT_INDEX.read_text(encoding="utf-8", errors="replace").splitlines()[1:]:
		columns = line.split(",")
		if columns and columns[0].isdigit():
			index[int(columns[0])] = columns
	return index


def load_family_ids(path: Path) -> list[int]:
	"""读家族任务 ID（首列为 ID 的 TSV）。 / Load family quest ids from the first column."""
	ids: set[int] = set()
	for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
		if not line.strip() or line.startswith("#"):
			continue
		token = line.split("\t")[0].strip()
		if token.isdigit():
			ids.add(int(token))
	return sorted(ids)


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("--family-src", type=Path, default=DEFAULT_FAMILY)
	parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
	args = parser.parse_args(argv)

	events = load_events()
	index = load_contract_index()
	family = load_family_ids(args.family_src)
	print(f"family quests / 家族任务: {len(family)}（来源 {args.family_src.name}）")

	matrix: Counter[tuple[bool, bool]] = Counter()
	rows: list[tuple[int, str, str, str]] = []
	for quest in family:
		record = index.get(quest)
		if record is None:
			rows.append((quest, "?", "?".join(sorted(events.get(quest, set()))), "no-contract-row"))
			continue
		start_ids = record[4].strip()
		start_is_sentinel = start_ids in ("", "0")
		has_triplet = set(TRIPLET) <= events.get(quest, set())
		matrix[(has_triplet, start_is_sentinel)] += 1
		rows.append(
			(
				quest,
				"sentinel" if start_is_sentinel else start_ids,
				"+".join(sorted(events.get(quest, set()), key=lambda token: int(token, 16))) or "-",
				"triplet" if has_triplet else "no-triplet",
			)
		)

	print("\n2x2 / 二维表 (has_triplet, start_npc_is_sentinel) over the family:")
	print(f"  triplet  & sentinel start : {matrix[(True, True)]}")
	print(f"  triplet  & real start     : {matrix[(True, False)]}")
	print(f"  no-trip  & sentinel start : {matrix[(False, True)]}")
	print(f"  no-trip  & real start     : {matrix[(False, False)]}")

	header = ["quest_id", "contract_start_npc", "event_signature", "triplet_status"]
	lines = ["\t".join(header)] + ["\t".join(str(cell) for cell in row) for row in rows]
	args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
	print(f"\nwrote {args.out}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
