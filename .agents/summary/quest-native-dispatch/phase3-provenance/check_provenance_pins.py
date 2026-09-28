#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""G1–G4 血缘冻结校验器（只读，零 Maven）。

读 provenance-pins.tsv：
  ① 每行 artifact_sha256 必须匹配当前文件；
  ② source_sha256 != "-" 且源存在时必须匹配（外部源缺失 → SKIP 并告警）；
  ③ G3：artifact 与 source 必须逐字节相同（fixture→production 提升的等价证明）；
  ④ G4：production 与测试侧登记表的 quest_id 集必须互斥（表头书面口径的机检版）。

用法：python3 -B check_provenance_pins.py
退出码：0 = 全部通过（允许 SKIP）；1 = 有 FAIL。
"""
from __future__ import annotations

import hashlib
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[4]
PINS = Path(__file__).resolve().parent / "provenance-pins.tsv"


def sha256(path: Path) -> str:
	return hashlib.sha256(path.read_bytes()).hexdigest()


def quest_ids(path: Path) -> set[str]:
	ids = set()
	for line in path.read_text(encoding="utf-8").splitlines():
		if line.strip() and not line.startswith("#"):
			ids.add(line.split("\t")[0])
	return ids


def main() -> int:
	fails: list[str] = []
	skips: list[str] = []
	checks = 0
	rows = [l.split("\t") for l in PINS.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")]
	header, rows = rows[0], rows[1:]
	assert header[:3] == ["id", "artifact", "artifact_sha256"], header
	for rid, artifact, artifact_sha, source, source_sha, regeneration, invariant in rows:
		apath = REPO / artifact
		checks += 1
		if not apath.is_file():
			fails.append(f"{rid}: artifact missing {artifact}")
			continue
		actual = sha256(apath)
		if actual != artifact_sha:
			fails.append(f"{rid}: artifact sha drift {artifact}\n  pinned={artifact_sha}\n  actual={actual}")
		spath = None
		if source_sha != "-" and source != "-":
			spath = Path(source)
			if not spath.is_absolute():
				spath = REPO / source
		if spath is not None:
			if not spath.is_file():
				skips.append(f"{rid}: source absent (external) {source}")
			else:
				checks += 1
				actual_src = sha256(spath)
				if actual_src != source_sha:
					fails.append(f"{rid}: source sha drift {source}\n  pinned={source_sha}\n  actual={actual_src}")
				if rid == "G3" and apath.read_bytes() != spath.read_bytes():
					fails.append(f"{rid}: artifact != fixture bytes")
		if rid == "G4" and spath is not None and spath.is_file():
			checks += 1
			overlap = quest_ids(apath) & quest_ids(spath)
			if overlap:
				fails.append(f"{rid}: production/test-side quest_id sets overlap: {sorted(overlap)}")
	print(f"PROVENANCE_PINS checks={checks} skipped={len(skips)} failed={len(fails)}")
	for s in skips:
		print("SKIP", s)
	for f in fails:
		print("FAIL", f)
	if fails:
		print("PROVENANCE_PINS_FAILED")
		return 1
	print("PROVENANCE_PINS_OK")
	return 0


if __name__ == "__main__":
	sys.exit(main())
