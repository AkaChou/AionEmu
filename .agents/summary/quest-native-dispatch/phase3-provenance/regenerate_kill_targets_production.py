#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""G3 再生成通道：测试夹具 → 生产击杀目标表（fail-closed 提升）。

链条：
  1) 兄弟车道生成器 .agents/summary/quest-15546-kill-progress/generate_kill_target_contract_tsv.py
     重建 src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv（本脚本不代跑，只消费）；
  2) 本脚本把夹具提升到生产路径（--check 只校验；--apply 写入），并对照 provenance-pins.tsv 的 sha。

用法：
  python3 -B regenerate_kill_targets_production.py --check   # 校验（默认）
  python3 -B regenerate_kill_targets_production.py --apply   # 校验通过后写入生产表
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[4]
PINS = Path(__file__).resolve().parent / "provenance-pins.tsv"
FIXTURE = REPO / "src/test/resources/quest/iluma-norsvold-kill-target-contract.tsv"
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
	if not FIXTURE.is_file():
		fails.append(f"fixture missing: {FIXTURE}")
	if not PROD.is_file():
		fails.append(f"production missing: {PROD}")
	if fails:
		print("\n".join(fails))
		return 1
	if sha256(FIXTURE) != source_sha:
		fails.append(f"fixture sha drift: pinned={source_sha} actual={sha256(FIXTURE)}")
	if sha256(PROD) != artifact_sha:
		fails.append(f"production sha drift: pinned={artifact_sha} actual={sha256(PROD)}")
	if FIXTURE.read_bytes() != PROD.read_bytes():
		fails.append("fixture != production bytes")
	if fails:
		print("G3_CHECK_FAILED")
		print("\n".join(fails))
		return 1
	print("G3_CHECK_OK (fixture == production, pinned sha match)")
	return 0


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument("--check", action="store_true", help="只校验（默认行为；显式可用）")
	ap.add_argument("--apply", action="store_true", help="校验通过后写入生产表")
	args = ap.parse_args()
	rc = check()
	if rc or not args.apply:
		return rc
	data = FIXTURE.read_bytes()
	if PROD.read_bytes() == data:
		print("G3_APPLY_NOOP (already identical)")
		return 0
	PROD.write_bytes(data)
	print(f"G3_APPLY_WROTE {PROD.relative_to(REPO)} sha256={sha256(PROD)}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
