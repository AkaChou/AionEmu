#!/usr/bin/env python3
"""P0c-40：链式 IR 指纹外科重冻（42 行改值 + 3 行新增），保留文件既有布局。

背景：`RetailSimpleTalkDefinitionCompiler.buildChain` 的 NPC_START 块路径此前缺
`acceptContinuation`（客户端 select1 续页 SELECT1_1/SELECT1_1_1），导致 42 行采纳行的
select1 页按钮（HACTION_SELECT1_1，动作 id 1012）在 IR 里无路由（客户端死按钮）。
补齐后这 42 行的 IR 变化即冻结值变化，另加本片采纳的 3 行（21460/29070/29071）新增冻结行。

机械（与 p0c39 的指纹插入同口径）：
  1. 读 `-Dretail.talkChain.fingerprintOut` 的实测 dump（真值来源）；
  2. 冻结文件按行手术——前缀（升序段）内逐行改值 + 追加新行，尾部历史块**逐字节保留**；
  3. 断言：数据行数 = dump 行数、前缀仍升序、尾部块与基线逐字节相同、改值集合与预期一致；
  4. 双副本落地（src/test/resources + target/test-classes）。

用法：`python3 -B p0c40_refreeze_fingerprints.py <dump.tsv>`（dry-run）/ `... <dump.tsv> --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
FROZEN = [
	REPO / "src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv",
	REPO / "target/test-classes/quest/retail-simple-talk-chain-ir-fingerprints.tsv",
]
EXPECTED_CHANGED = 42
NEW_IDS = ("21460", "29070", "29071")


def load_rows(path: Path) -> list[tuple[int, str]]:
	rows = []
	for line in path.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			continue
		key, value = line.split("\t")
		rows.append((int(key), value))
	return rows


def main() -> int:
	apply = "--apply" in sys.argv
	args = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	if len(args) != 1:
		print("usage: p0c40_refreeze_fingerprints.py <dump.tsv> [--apply]")
		return 2
	dump = load_rows(Path(args[0]))
	dump_map = dict(dump)
	baseline = FROZEN[0].read_text(encoding="utf-8")
	lines = baseline.splitlines(keepends=True)

	# 尾部历史块 = 唯一升序下降点之后的所有数据行（P0c-39 记录：下降在 80752->1323）。
	data_indexes = [index for index, line in enumerate(lines)
		if not line.startswith("#") and line.strip()]
	prefix_data = []
	descent = None
	for position, index in enumerate(data_indexes):
		key = int(lines[index].split("\t")[0])
		if prefix_data and key < prefix_data[-1]:
			descent = position
			break
		prefix_data.append(key)
	if descent is None:
		print("ABORT: frozen file has no ascending-prefix descent (layout changed)")
		return 1
	prefix_indexes = data_indexes[:descent]
	tail_indexes = data_indexes[descent:]
	print(f"baseline rows={len(data_indexes)} prefix={len(prefix_indexes)} tail={len(tail_indexes)} "
		f"descent_at={descent}")

	# 1) 前缀逐行改值（保留行序）。
	changed = []
	for index in prefix_indexes:
		key, value = lines[index].rstrip("\n").split("\t")
		key = int(key)
		if key in dump_map and dump_map[key] != value:
			lines[index] = f"{key}\t{dump_map[key]}\n"
			changed.append(key)
	# 2) 尾部块：只允许改值，不允许插入/删除（逐字节保留基线块）。
	for index in tail_indexes:
		key, value = lines[index].rstrip("\n").split("\t")
		if int(key) in dump_map and dump_map[int(key)] != value:
			print(f"ABORT: tail-block row {key} would change (out of scope)")
			return 1
	# 3) 新行插入前缀的升序位置。
	existing = {int(lines[index].split("\t")[0]) for index in data_indexes}
	inserted = []
	for key_text in NEW_IDS:
		key = int(key_text)
		if key in existing:
			print(f"ABORT: new row {key} already present")
			return 1
		if key not in dump_map:
			print(f"ABORT: new row {key} missing from dump (flip not applied yet?)")
			return 1
		position = None
		for offset, index in enumerate(prefix_indexes):
			if int(lines[index].split("\t")[0]) > key:
				position = index
				break
		if position is None:
			print(f"ABORT: new row {key} does not fit inside the ascending prefix")
			return 1
		lines.insert(position, f"{key}\t{dump_map[key]}\n")
		prefix_indexes = [index + 1 if index >= position else index for index in prefix_indexes]
		prefix_indexes.insert(offset, position)
		inserted.append(key)

	patched = "".join(lines)
	# 断言：行数 = dump 行数；前缀升序；尾部块逐字节相同。
	patched_data = [line for line in patched.splitlines(keepends=True)
		if not line.startswith("#") and line.strip()]
	if len(patched_data) != len(dump):
		print(f"ABORT: patched rows {len(patched_data)} != dump rows {len(dump)}")
		return 1
	prefix_keys = [int(patched_data[position].split("\t")[0]) for position in range(0, len(prefix_indexes))]
	if prefix_keys != sorted(prefix_keys):
		print("ABORT: patched prefix is not ascending")
		return 1
	baseline_tail = [lines_before for lines_before in baseline.splitlines(keepends=True)
		if not lines_before.startswith("#") and lines_before.strip()][len(data_indexes) - len(tail_indexes):]
	patched_tail = [line for line in patched.splitlines(keepends=True)
		if not line.startswith("#") and line.strip()][len(patched_data) - len(tail_indexes):]
	if baseline_tail != patched_tail:
		print("ABORT: tail block changed byte-wise")
		return 1
	if len(changed) != EXPECTED_CHANGED:
		print(f"ABORT: expected {EXPECTED_CHANGED} changed values, got {len(changed)}: {sorted(changed)}")
		return 1

	print(f"PLAN: changed={len(changed)} {sorted(changed)}; inserted={inserted}; "
		f"rows {len(data_indexes)} -> {len(patched_data)}")
	if not apply:
		print("DRY-RUN: no file written (pass --apply to execute)")
		return 0
	for path in FROZEN:
		if path.exists():
			path.write_text(patched, encoding="utf-8")
			print(f"written: {path.relative_to(REPO)}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
