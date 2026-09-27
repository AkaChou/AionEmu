#!/usr/bin/env python3
"""P0c-43：爆炸半径（pre 登记表 vs 已安装登记表）的逐任务块对拍，可独立复现。

输入：`p0c43-registry-pre.tsv`（改道落地前的登记表快照，随本目录留档）+ 仓库里已安装的登记表。
输出：stdout（HEADERS/CHANGED/ADDED/REMOVED + 24123 块的 unified diff），并落盘两块：
`p0c43-24123-registry-pre.tsv` / `p0c43-24123-registry-post.tsv`。
退出码 0 = 变化面恰为 {24123}。
"""
from __future__ import annotations

import collections
import difflib
import sys
from pathlib import Path

TOPIC = Path(__file__).resolve().parent
REPO = TOPIC.parents[2]
PRE = TOPIC / "p0c43-registry-pre.tsv"
POST = REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv"
EXPECT_CHANGED = [24123]


def blocks(path: Path):
	groups = collections.OrderedDict()
	headers = 0
	for line in path.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			headers += 1
			continue
		groups.setdefault(int(line.split("\t")[0]), []).append(line)
	return groups, headers


def main() -> int:
	pre, pre_headers = blocks(PRE)
	post, post_headers = blocks(POST)
	changed = sorted(q for q in set(pre) & set(post) if pre[q] != post[q])
	added = sorted(set(post) - set(pre))
	removed = sorted(set(pre) - set(post))
	print("HEADERS %d -> %d" % (pre_headers, post_headers))
	print("CHANGED %s" % changed)
	print("ADDED %s" % added)
	print("REMOVED %s" % removed)
	print("TOTAL_BLOCKS %d -> %d" % (len(pre), len(post)))
	for qid in changed:
		(TOPIC / ("p0c43-%d-registry-pre.tsv" % qid)).write_text(
			"\n".join(pre[qid]) + "\n", encoding="utf-8")
		(TOPIC / ("p0c43-%d-registry-post.tsv" % qid)).write_text(
			"\n".join(post[qid]) + "\n", encoding="utf-8")
		print("--- 任务块差异 %d（%d 行 -> %d 行）" % (qid, len(pre[qid]), len(post[qid])))
		for line in difflib.unified_diff(pre[qid], post[qid], lineterm="", n=0):
			if line.startswith(("---", "+++")):
				continue
			print("    " + line)
	if changed != EXPECT_CHANGED or added or removed:
		print("BLAST_UNEXPECTED: 期望变化面恰为 %s" % EXPECT_CHANGED)
		return 1
	print("BLAST_OK: 变化面恰为 %s（无新增/无删除任务块）" % EXPECT_CHANGED)
	return 0


if __name__ == "__main__":
	sys.exit(main())
