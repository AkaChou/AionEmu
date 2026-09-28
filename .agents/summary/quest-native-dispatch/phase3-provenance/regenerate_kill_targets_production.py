#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""G3 单源校验：候选击杀目标生产表冻结（P4c 后无测试夹具副本）。

P4c 之前的链条：quest-15546 车道生成器 → 测试夹具 → 本脚本提升到生产表；
P4c 单源化后夹具退役、`QuestIlumaNorsvoldKillTargetCoverageTest` 直读生产表，
本脚本只保留**只读冻结校验**：

  --check  校验生产表存在且 sha256 == provenance-pins.tsv 的 G3 artifact pin，
           并要求 pin 已单源化（source 列为 "-"）；
  --apply  已关闭——没有可提升的源。fail-closed，改动必须直接落生产表并同步 pin。

用法 / Usage:
  python3 -B regenerate_kill_targets_production.py --check
  python3 -B regenerate_kill_targets_production.py --apply   # 预期失败（exit 2）

Before P4c the chain promoted a byte-identical test fixture into the production table.
After single-sourcing, this tool is a read-only freeze check; --apply is closed.
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[4]
PINS = Path(__file__).resolve().parent / "provenance-pins.tsv"
PROD = REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_client_kill_targets.tsv"


def sha256(p: Path) -> str:
	return hashlib.sha256(p.read_bytes()).hexdigest()


def pinned() -> tuple[str, str]:
	for line in PINS.read_text(encoding="utf-8").splitlines():
		if line.startswith("G3\t"):
			parts = line.split("\t")
			return parts[2], parts[4]
	raise SystemExit("G3 pin row not found")


def check() -> int:
	artifact_sha, source_sha = pinned()
	fails = []
	if not PROD.is_file():
		fails.append(f"production missing: {PROD}")
	else:
		actual = sha256(PROD)
		if actual != artifact_sha:
			fails.append(f"production sha drift: pinned={artifact_sha} actual={actual}")
	if source_sha != "-":
		fails.append("G3 pin is not single-sourced (source_sha256 != '-'); apply the P4c pin update")
	if fails:
		print("G3_CHECK_FAILED")
		print("\n".join(fails))
		return 1
	print("G3_CHECK_OK (single source, pinned sha match)")
	return 0


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument("--check", action="store_true", help="只读冻结校验（默认）")
	ap.add_argument("--apply", action="store_true", help="P4c 后关闭：无提升源")
	args = ap.parse_args()
	if args.apply:
		print("G3_APPLY_DISABLED (P4c single-sourcing: no fixture to promote; edit the production table and update the pin)", file=sys.stderr)
		return 2
	return check()


if __name__ == "__main__":
	sys.exit(main())
