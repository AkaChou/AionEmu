#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-51 / 续片 25：漂移登记表 25052 改 ADOPTED（手术式，只改本批行）。

来源 = `-Dretail.dataDriven.equivOut` 实测 dump（分类权威）。守卫：
  1. 目标行当前必须是 `REJECTED:RETAIL_STEP_UNSUPPORTED`（本批翻转前的登记值）；
  2. 只改这 2 行，其余行逐字节保留（并行车道的 20035 漂移不许顺手动）；
  3. 双副本（src/test + target/test-classes）一致落地；.agents 快照副本按需同步。

用法：`python3 -B p0c50_update_drift_rows.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
REGISTRY = (
	REPO / "src/test/resources/quest/retail-data-driven-drift.tsv",
	REPO / "target/test-classes/quest/retail-data-driven-drift.tsv",
)
SNAPSHOT = REPO / ".agents/summary/scriptdll-quest-driver/dd-drift-fresh.tsv"
FLIP_IDS = ("25052",)
OLD_PREFIX = "REJECTED:RETAIL_FOBJ_COLLECT_UNSUPPORTED"


def main() -> int:
	apply = "--apply" in sys.argv
	args = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(args) != 1:
		print("usage: p0c50_update_drift_rows.py <dump.tsv> [--apply]")
		return 2
	dump = {}
	for line in Path(args[0]).read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			continue
		parts = line.split("\t")
		dump[parts[0]] = parts[1]
	for quest_id in FLIP_IDS:
		if dump.get(quest_id) != "ADOPTED":
			print(f"ABORT: dump says {quest_id} = {dump.get(quest_id)}, expected ADOPTED")
			return 1

	for path in REGISTRY:
		lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
		changed = []
		for index, line in enumerate(lines):
			if line.startswith("#") or not line.strip():
				continue
			parts = line.split("\t")
			if parts[0] in FLIP_IDS:
				if not parts[1].startswith(OLD_PREFIX):
					print(f"ABORT: {path.name}: {parts[0]} is not {OLD_PREFIX}: {line.strip()}")
					return 1
				lines[index] = f"{parts[0]}\tADOPTED\n"
				changed.append(parts[0])
		if sorted(changed, key=int) != sorted(FLIP_IDS, key=int):
			print(f"ABORT: {path.name}: expected {sorted(FLIP_IDS)}, changed {sorted(changed)}")
			return 1
		delta = sum(1 for old, new in zip(path.read_text(encoding="utf-8").splitlines(keepends=True),
			lines) if old != new)
		if delta != len(FLIP_IDS):
			print(f"ABORT: {path.name}: {delta} lines changed, expected {len(FLIP_IDS)}")
			return 1
		if apply:
			path.write_text("".join(lines), encoding="utf-8")
		print(f"{path.relative_to(REPO)}: {len(FLIP_IDS)} rows -> ADOPTED"
			f"{'' if apply else ' (dry-run)'}")

	if SNAPSHOT.exists():
		lines = SNAPSHOT.read_text(encoding="utf-8").splitlines(keepends=True)
		changed = []
		for index, line in enumerate(lines):
			parts = line.split("\t")
			if not line.startswith("#") and line.strip() and parts[0] in FLIP_IDS:
				lines[index] = f"{parts[0]}\tADOPTED\n"
				changed.append(parts[0])
		if sorted(changed, key=int) != sorted(FLIP_IDS, key=int):
			print(f"ABORT: snapshot: expected {sorted(FLIP_IDS)}, changed {sorted(changed)}")
			return 1
		if apply:
			SNAPSHOT.write_text("".join(lines), encoding="utf-8")
		print(f"{SNAPSHOT.name}: {len(changed)} snapshot rows -> ADOPTED"
			f"{'' if apply else ' (dry-run)'}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
