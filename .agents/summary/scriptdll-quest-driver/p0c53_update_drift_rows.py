#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-53 / 续片 27：漂移登记表补丁（25 行改 ADOPTED + 2 行拒绝码前移）。

来源 = `-Dretail.dataDriven.equivOut` 实测 dump（分类权威，且为**本片改动后**的实测）。守卫：
  1. 每个目标行当前登记值必须与 dump 的"改动前"一致（ADOPTED 目标行必须尚未 ADOPTED；
     码位前移目标行必须是旧码），否则 abort；
  2. 只改本批行 + 重算头部直方图（**基线直方图已陈旧**：数据行 ADOPTED 1153 而头注写 1112，
     本片顺手把头注改为按数据行重算）；其余行逐字节保留（并行车道的在飞行不许顺手动）；
  3. 双副本（src/test + target/test-classes）一致落地。

用法：`python3 -B p0c53_update_drift_rows.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from collections import Counter
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
REGISTRY = (
	REPO / "src/test/resources/quest/retail-data-driven-drift.tsv",
	REPO / "target/test-classes/quest/retail-data-driven-drift.tsv",
)
FLIP_IDS = ("15476", "15477", "15478", "15479", "18743", "25476", "25477", "25478", "25479", "28743",
	"50068", "50069", "50072", "50073", "50074", "50088", "50089", "50090", "50091", "50092", "50094",
	"50095", "50104", "50105", "50107")
SHIFT_IDS = {"15480": "REJECTED:RETAIL_MONSTER_UNRESOLVED",
	"25480": "REJECTED:RETAIL_MONSTER_UNRESOLVED"}
FLIP_OLD_PREFIX = "REJECTED:RETAIL_"


def main() -> int:
	apply = "--apply" in sys.argv
	args = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(args) != 1:
		print("usage: p0c53_update_drift_rows.py <dump.tsv> [--apply]")
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
	for quest_id, code in SHIFT_IDS.items():
		if dump.get(quest_id) != code:
			print(f"ABORT: dump says {quest_id} = {dump.get(quest_id)}, expected {code}")
			return 1

	for path in REGISTRY:
		original = path.read_text(encoding="utf-8")
		lines = original.splitlines(keepends=True)
		body, header = [], []
		for line in lines:
			(header if line.startswith("#") else body).append(line)
		changed = []
		for index, line in enumerate(body):
			if not line.strip():
				continue
			parts = line.split("\t")
			if parts[0] in FLIP_IDS:
				if not parts[1].startswith(FLIP_OLD_PREFIX):
					print(f"ABORT: {path.name}: {parts[0]} is not a rejection row: {line.strip()}")
					return 1
				body[index] = f"{parts[0]}\tADOPTED\n"
				changed.append(parts[0])
			elif parts[0] in SHIFT_IDS:
				old_code = parts[1]
				body[index] = "\t".join([parts[0], SHIFT_IDS[parts[0]]]
					+ [part for part in parts[2:] if part.strip()]) + "\n"
				changed.append(parts[0])
				print(f"   {parts[0]}: {old_code} -> {SHIFT_IDS[parts[0]]}")
		if sorted(changed, key=int) != sorted(list(FLIP_IDS) + list(SHIFT_IDS), key=int):
			print(f"ABORT: {path.name}: expected {sorted(list(FLIP_IDS) + list(SHIFT_IDS), key=int)},"
				f" changed {sorted(changed, key=int)}")
			return 1
		# 计数键必须 strip：两列行（ADOPTED）的 parts[1] 带换行符，不 strip 会把头注写成 `# ADOPTED` + `\t1178`
		# 两行（首跑实测踩到）。
		# The histogram key must be stripped: for two-column rows parts[1] carries the newline, and an
		# unstripped key splits the header line into two (hit on the first run).
		histogram = Counter(line.split("\t")[1].strip() for line in body if line.strip())
		new_header = [f"# DataDriven 漂移登记（禁止手改；重算：-Dretail.dataDriven.equivOut=<path>）\n"]
		for kind, count in sorted(histogram.items(), key=lambda item: -item[1]):
			new_header.append(f"# {kind}\t{count}\n")
		patched = "".join(new_header + body)
		delta = sum(1 for old, new in zip(original.splitlines(), patched.splitlines()) if old != new)
		print(f"{path.relative_to(REPO)}: {len(changed)} rows patched, {delta} lines differ"
			f"{'' if apply else ' (dry-run)'}")
		if apply:
			path.write_text(patched, encoding="utf-8")
	return 0


if __name__ == "__main__":
	sys.exit(main())
