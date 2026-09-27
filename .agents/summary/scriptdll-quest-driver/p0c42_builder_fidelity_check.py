#!/usr/bin/env python3
"""P0c-42：生成器忠实性检查（登记表是生成物 ⇒ 人工修复必须可由生成器复现）。

判据：把生成器的 `OUT` 重定向到临时文件后跑全量生成，输出必须与仓库里的
`quest_client_talk_chain_steps.tsv` **逐字节相同**；同时核对生成器会重写的 8 张裁定表
在本次运行前后**零漂移**（生成器确定性）。

用法：`python3 -B p0c42_builder_fidelity_check.py [--keep <regen 路径>]`
退出码 0 = 逐字节相同；1 = 有差异（打印 diff 前 40 行）。
"""
from __future__ import annotations

import importlib.util
import subprocess
import sys
from pathlib import Path

TOPIC = Path(__file__).resolve().parent
REPO = TOPIC.parents[2]
BUILDER = TOPIC / "build_quest_client_talk_chain_steps.py"
REGISTRY = REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv"
# P0c-45：口径升级为"生成器读到的**全部**输入表"（grep `HERE / '*.tsv'` 全提取，19 张），
# 不再只覆盖 8 张——干跑/保真必须证明对每一张输入表都零写入。
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


def main() -> int:
	positional = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
	regen = Path(positional[0]) if positional else Path("/tmp/p0c42-fidelity-regen.tsv")
	before = {name: (TOPIC / name).read_bytes() for name in DECISION_TABLES
		if (TOPIC / name).exists()}

	module = load_builder()
	module.OUT = regen
	module.main()

	if regen.read_bytes() != REGISTRY.read_bytes():
		print("FIDELITY_FAIL: 生成器输出与登记表不同")
		subprocess.run(["diff", str(regen), str(REGISTRY)], check=False)
		return 1
	print(f"FIDELITY_OK: 生成器重跑输出与登记表逐字节相同（{REGISTRY.stat().st_size} 字节）")
	md5 = subprocess.run(["md5", "-q", str(REGISTRY)], capture_output=True, text=True,
		check=False).stdout.strip()
	print(f"REGISTRY_MD5 {md5}")
	for name, payload in before.items():
		if (TOPIC / name).read_bytes() != payload:
			print(f"DECISION_TABLE_DRIFT: {name}")
			return 1
	print(f"DECISION_TABLES_STABLE: {len(before)} 张裁定表零漂移")
	return 0


if __name__ == "__main__":
	sys.exit(main())
