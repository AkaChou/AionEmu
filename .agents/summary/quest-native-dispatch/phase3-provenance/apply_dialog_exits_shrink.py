#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P2b：按 P2a 普查裁定删除 dialog_exits 的 104 个阶梯 token（只缩行、不删表）。

前置（fail-closed，任一不满足即退出 1）：
  - census-dialog-exits-v2.tsv 存在且 delete_candidates=104、tokens 仅 SELECT1_1/SELECT1_1_1；
  - 普查无 talk-ladder-live 行；
  - 目标表 sha256 == 快照 .before 的 sha256。

用法：
  python3 -B apply_dialog_exits_shrink.py --check   # 只校验/预演
  python3 -B apply_dialog_exits_shrink.py --apply   # 写回生产表 + 落 .after 快照
"""
from __future__ import annotations

import argparse
import hashlib
import shutil
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
TABLE = REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv"
CENSUS = HERE / "census-dialog-exits-v2.tsv"
SNAPSHOTS = REPO / ".agents/summary/quest-native-dispatch/retired-tsv"
BEFORE = SNAPSHOTS / "quest_client_dialog_exits.tsv.rows-20260928.before"
AFTER = SNAPSHOTS / "quest_client_dialog_exits.tsv.rows-20260928.after"
REMOVED = HERE / "dialog-exits-removed-tokens-20260928.tsv"
LADDER = {"SELECT1_1", "SELECT1_1_1"}


def sha256(p: Path) -> str:
	return hashlib.sha256(p.read_bytes()).hexdigest()


def main() -> int:
	ap = argparse.ArgumentParser()
	ap.add_argument("--check", action="store_true", help="只校验/预演（默认）")
	ap.add_argument("--apply", action="store_true")
	args = ap.parse_args()

	if not CENSUS.is_file():
		print("census missing; run census_dialog_exits_v2.py first")
		return 1
	# 1) 普查前置
	deletes: dict[int, list[tuple[str, str]]] = {}
	live = 0
	for line in CENSUS.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			continue
		p = line.split("\t")
		qid, klass, verdicts = int(p[0]), p[3], p[5]
		if klass == "talk-ladder-live":
			live += 1
		for item in verdicts.split(" | "):
			if not item.strip():
				continue
			token, rest = item.split("=", 1)
			if rest.startswith("删"):
				ev = rest.split("[", 1)[1].split("]", 1)[0]
				deletes.setdefault(qid, []).append((token, ev))
	flat = [t for v in deletes.values() for t, _ in v]
	if len(flat) != 104 or set(flat) - LADDER or live:
		print(f"precondition failed: deletes={len(flat)} tokens={sorted(set(flat))} live={live}")
		return 1
	if not BEFORE.is_file() or sha256(BEFORE) != sha256(TABLE):
		print("target table differs from .before snapshot; re-snapshot first")
		return 1

	# 2) 预演/写回
	out_lines, removed_rows = [], []
	before_tokens = after_tokens = 0
	for line in TABLE.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			out_lines.append(line)
			continue
		p = line.split("\t")
		qid = int(p[0])
		tokens = p[1].split() if len(p) > 1 else []
		before_tokens += len(tokens)
		drop = {t for t, _ in deletes.get(qid, [])}
		keep = [t for t in tokens if t not in drop]
		after_tokens += len(keep)
		for t, ev in deletes.get(qid, []):
			if t in tokens:
				removed_rows.append(f"{qid}\t{t}\t{ev}")
		out_lines.append(f"{qid}\t{' '.join(keep)}")
	if before_tokens != 443 or after_tokens != 339:
		print(f"token count mismatch: before={before_tokens} after={after_tokens}")
		return 1
	if not args.apply:
		print(f"CHECK_OK deletes=104 rows={len(deletes)} tokens 443->339 live=0")
		return 0
	TABLE.write_text("\n".join(out_lines) + "\n", encoding="utf-8")
	shutil.copyfile(TABLE, AFTER)
	REMOVED.write_text("# quest_id\ttoken\tclient_evidence\n" + "\n".join(sorted(removed_rows)) + "\n",
		encoding="utf-8")
	print(f"APPLIED tokens 443->339 rows={len(deletes)}")
	print(f"after sha256={sha256(TABLE)}")
	print(f"snapshots before={sha256(BEFORE)} after={sha256(AFTER)}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
