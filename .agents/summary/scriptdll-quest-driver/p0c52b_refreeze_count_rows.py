#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-52b：计数轴改判 2 行的 DD IR 指纹重冻（改值 2 行，增删 0 行）。

背景：单段 hunt 行的计数轴对齐判据从「怪名集合一致」放宽为「单计数器 + 单条客户端进度行」
（真端名单含改名/幽灵模板的 13841 族按名对不上，服务端读数 60/60/55 vs 客户端 40）。
判据放宽后真端 IR 计数步长随之改判的行只有 13841/23841 两行（13845/13849/23845/23849 的
IR 本来就是客户端计数 40，指纹不动——判据放宽对它们是无操作），其余行一个字节都不许动。

机械（与 p0c52_fp_flip.py 同口径）：
  1. 读 `-Dretail.dataDriven.fpOut` 实测 dump（真值来源）；
  2. 基线数据行与 dump 的行集合完全一致（本片不增不删）；
  3. 除 CORRECTED 2 行外，其余行逐字节相同（并行车道的 FOREIGN 行保持基线值）；
  4. CORRECTED 2 行必须真的改值（否则说明对齐没生效，脚本失败）；
  5. 双副本落地（src/test/resources + target/test-classes）。

用法：`python3 -B p0c52b_refreeze_count_rows.py <dump.tsv>`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FROZEN = (
	REPO / "src/test/resources/quest/retail-data-driven-ir-fingerprints.tsv",
	REPO / "target/test-classes/quest/retail-data-driven-ir-fingerprints.tsv",
)
CORRECTED = ("13841", "23841")
# 并行车道的在改行此前为 15042/16821/16823/26821/26823；其车道已在 17:2x 自行重冻，
# 现在与本 dump 逐字节一致（脚本断言：除 CORRECTED 外不得有任何行漂移）。
# The parallel lane's rows used to drift; that lane has since re-frozen them, so they now agree with
# this dump and the script requires zero out-of-scope drift.
FOREIGN: tuple[str, ...] = ()


def data_lines(text: str) -> list[str]:
	return [line for line in text.splitlines(keepends=True)
		if not line.startswith("#") and line.strip()]


def main() -> int:
	apply = "--apply" in sys.argv
	args = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(args) != 1:
		print("usage: p0c52b_refreeze_count_rows.py <dump.tsv> [--apply]")
		return 2
	dump = data_lines(Path(args[0]).read_text(encoding="utf-8"))
	dump_map = {line.split("\t")[0]: line for line in dump}
	baseline_text = FROZEN[0].read_text(encoding="utf-8")
	baseline = data_lines(baseline_text)

	if set(dump_map) != {line.split("\t")[0] for line in baseline}:
		print("ABORT: dump row set moved (insertions/deletions are not part of this slice)")
		return 1

	patched: list[str] = []
	changed: list[str] = []
	drifted: list[str] = []
	for line in baseline:
		key = line.split("\t")[0]
		if key in CORRECTED:
			if dump_map[key] == line:
				print(f"ABORT: corrected row {key} carries the baseline value (alignment silent)")
				return 1
			patched.append(dump_map[key])
			changed.append(key)
			continue
		if dump_map[key] != line:
			if key in FOREIGN:
				drifted.append(key)
				patched.append(line)
				continue
			print(f"ABORT: out-of-scope row {key} drifted")
			return 1
		patched.append(line)
	if sorted(changed, key=int) != sorted(CORRECTED, key=int):
		print(f"ABORT: corrected set mismatch: {sorted(changed, key=int)}")
		return 1
	if sorted(drifted, key=int) != sorted(FOREIGN, key=int):
		print(f"ABORT: foreign drift set moved: {sorted(drifted, key=int)}")
		return 1
	output = "".join(line for line in baseline_text.splitlines(keepends=True)
		if line.startswith("#") or not line.strip()) + "".join(patched)
	print(f"PLAN: rows {len(baseline)} -> {len(patched)}; re-frozen={sorted(changed, key=int)}; "
		f"foreign kept at baseline={sorted(drifted, key=int)}")
	if not apply:
		print("DRY-RUN: no file written (pass --apply to execute)")
		return 0
	for path in FROZEN:
		if path.exists():
			path.write_text(output, encoding="utf-8")
			print(f"written: {path.relative_to(REPO)}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
