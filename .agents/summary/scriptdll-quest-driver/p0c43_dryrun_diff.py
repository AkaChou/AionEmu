#!/usr/bin/env python3
"""P0c-43：生成器改道干跑比对（不安装）——只看"输出的哪些任务块变了"。

用法：`python3 -B p0c43_dryrun_diff.py [--expect 24123]`
退出码 0 = 变化面 == 期望集合；1 = 有意外变化（或期望未出现）。
"""
from __future__ import annotations

import collections
import difflib
import importlib.util
import subprocess
import sys
from pathlib import Path

TOPIC = Path(__file__).resolve().parent
REPO = TOPIC.parents[2]
BUILDER = TOPIC / "build_quest_client_talk_chain_steps.py"
REGISTRY = REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv"
# P0c-45：口径升级为"生成器读到的**全部**输入表"（grep `HERE / '*.tsv'` 全提取），
# 不再只覆盖 8 张——干跑必须证明对每一张输入表都零写入。
DECISION_TABLES = (
	"item_name_index.tsv",
	"npc_name_index.tsv",
	"p0c10e-talk-chain-census.tsv",
	"p0c10f-chain-no-routes.tsv",
	"p0c10f-talk-chain-decisions.tsv",
	"p0c10h-chain-axis-mismatch.tsv",
	"p0c10h-chain-deferred-compound.tsv",
	"p0c10h-chain-unverified-pages.tsv",
	"p0c10i-canonical-gaps.tsv",
	"p0c10j-deferred-decisions.tsv",
	"p0c11-collect-gate-decisions.tsv",
	"p0c34-chain-reward-row-divergence.tsv",
	"p0c34-chain-reward-row-overrides.tsv",
	"p0c34-item-report-crosscheck.tsv",
	"p0c35-progress-row-overrides.tsv",
	"p0c38-canonical-item-channel.tsv",
	"p0c42-accept-entrance-decisions.tsv",
	"p0c43-canonical-resynthesis.tsv",
	"p0c45-extra-owner-decisions.tsv",
	"p0c46-role-narrowing-decisions.tsv",
	"p0c47-stage-window-spread.tsv",
	"p0c54-stage-page-decisions.tsv",
	"p0c55-stage-leg-decisions.tsv",
	"p0c57-accept-entrance-decisions.tsv",
)


def load_builder():
	spec = importlib.util.spec_from_file_location("chain_steps_builder", BUILDER)
	module = importlib.util.module_from_spec(spec)
	spec.loader.exec_module(module)
	return module


def blocks(path: Path):
	"""按 quest_id 切块（跳过 # 头），返回 {qid: [原始行]} 与头行数。"""
	groups = collections.OrderedDict()
	headers = 0
	for line in path.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			headers += 1
			continue
		groups.setdefault(int(line.split("\t")[0]), []).append(line)
	return groups, headers


def main() -> int:
	expect = set()
	if "--expect" in sys.argv:
		expect = {int(a) for a in sys.argv[sys.argv.index("--expect") + 1].split(",")}
	regen = Path("/tmp/p0c43-dryrun.tsv")
	before_tables = {name: (TOPIC / name).read_bytes() for name in DECISION_TABLES
		if (TOPIC / name).exists()}

	module = load_builder()
	module.OUT = regen
	module.main()

	gold, gold_headers = blocks(REGISTRY)
	new, new_headers = blocks(regen)
	changed = sorted(q for q in set(gold) & set(new) if gold[q] != new[q])
	added = sorted(set(new) - set(gold))
	removed = sorted(set(gold) - set(new))
	print("HEADERS %d -> %d" % (gold_headers, new_headers))
	print("CHANGED %s" % changed)
	print("ADDED %s" % added)
	print("REMOVED %s" % removed)
	for qid in changed:
		print("--- 任务块差异 %d（%d 行 -> %d 行）" % (qid, len(gold[qid]), len(new[qid])))
		for line in difflib.unified_diff(gold[qid], new[qid], lineterm="", n=0):
			if line.startswith(("---", "+++")):
				continue
			print("    " + line)
	for name, payload in before_tables.items():
		if (TOPIC / name).read_bytes() != payload:
			print("DECISION_TABLE_DRIFT: %s" % name)
			return 1
	print("DECISION_TABLES_STABLE: %d 张裁定表零漂移" % len(before_tables))
	if expect and (set(changed) != expect or added or removed):
		print("DRYRUN_UNEXPECTED: 期望变化面 %s" % sorted(expect))
		return 1
	print("DRYRUN_OK: 变化面 == %s" % sorted(expect or set(changed)))
	md5 = subprocess.run(["md5", "-q", str(regen)], capture_output=True, text=True,
		check=False).stdout.strip()
	print("REGEN %s (%s, %d 字节)" % (regen, md5, regen.stat().st_size))
	return 0


if __name__ == "__main__":
	sys.exit(main())
