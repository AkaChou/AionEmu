#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-48：talk+collect 段登记表增量落地（PVP 链批：80846/80847 + 9640）。

输入 = 修正后的生成器干跑输出（`--out=` 到本主题目录，`dd mixes=148 / 88 rows`）。本片只落地
**本批 + PVP 词汇直接产物**三行（80846/80847 = pvp+collectitem 采集段；9640 = pvp+talk 行，
家族外、生产惰性），刻意**不带入** 20035/20501 两行——它们在 TALK_HUNT_CHAIN_DEFERRED 桶里属
并行车道的在修改行，登记表保持现状以免抢跑（登记即证据对齐，登记表与生成器的三行差异在此登记）。

落地对象：src/main/resources 与 target/classes 两份副本（逐字节一致守卫）。

Writes the slice's three rows (80846/80847 pvp+collectitem collect stages, 9640 pvp+talk outside the
family) from the corrected generator's dry run into both registry copies, deliberately withholding
the two lane-owned rows (20035/20501, in the parallel lane's TALK_HUNT_CHAIN_DEFERRED bucket) so the
lane's in-flight reshaping is not pre-empted.

用法 / usage: python3 -B p0c48_add_pvp_collect_stage_rows.py [--check]
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
DRY_RUN = REPO / ".agents/summary/scriptdll-quest-driver/p0c48-dry-run-talk-collect-chain-pages.tsv"
BASELINE = REPO / ".agents/summary/scriptdll-quest-driver/p0c48-pre-regen-talk-collect-chain-pages.tsv"
TARGETS = (
	REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_collect_chain_pages.tsv",
	REPO / "target/classes/aion/data/static_data/quest_retail/quest_client_talk_collect_chain_pages.tsv",
)
ADDED = ("9640", "80846", "80847")
WITHHELD = ("20035", "20501")


def main() -> int:
	check_only = "--check" in sys.argv[1:]
	baseline = BASELINE.read_text(encoding="utf-8").splitlines(keepends=True)
	dry = DRY_RUN.read_text(encoding="utf-8").splitlines(keepends=True)
	dry_ids = {line.split("\t", 1)[0] for line in dry if line.strip() and not line.startswith("#")}
	base_ids = {line.split("\t", 1)[0] for line in baseline if line.strip() and not line.startswith("#")}
	added = sorted(dry_ids - base_ids)
	removed = sorted(base_ids - dry_ids)
	expected = sorted(set(ADDED) | set(WITHHELD))
	if added != expected or removed:
		print(f"ABORT: generator delta moved — added={added} removed={removed}")
		return 1
	for withheld in WITHHELD:
		if withheld in dry_ids and withheld not in base_ids:
			print(f"withheld lane row stays out: {withheld}")
	filtered = "".join(line for line in dry
		if line.startswith("#") or line.split("\t", 1)[0] not in WITHHELD)
	for path in TARGETS:
		text = path.read_text(encoding="utf-8")
		if text != "".join(baseline):
			print(f"ABORT: {path} differs from the recorded baseline (lane storm, retry later)")
			return 1
		if not check_only:
			path.write_text(filtered, encoding="utf-8")
		print(f"{path.relative_to(REPO)}: {len(baseline)} -> {len(filtered.splitlines())} rows"
			f"{' (check only)' if check_only else ''}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
