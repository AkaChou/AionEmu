#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-58 挑战任务哨兵决策表：从普查 TSV + 静态源生成逐行裁定（只读生成，产出 TSV）。

判据（全静态；遗留 XML 只作见证列）：
  * 真端 DD 表：`category_acquire_ = Talk` ∧ `value0_acquire_ = _challengetask_`（**哨兵**，不是 NPC 名）
    ∧ `reward_npc_name` 唯一解析（服务端 npc 模板 name_desc → npc_id）；
  * 客户端 npc 块（外部 `client_npcs_npc.xml`）：该 npc 的 `<quest_ai_name>` 与真端 reward 名**同名**
    （客户端自己声明这个对话路由名归它）——接取与交付落在同一 NPC 上的客户端侧背书；
  * 客户端登记：入口页（select_none 4762）+ 任务书行 + 击杀进度行 + 按钮签名（校验接取页存在）；
  * 遗留 XML 见证：`NPC_START npc-id=<同一 id> start-page=SELECT_NONE`（接取）+ 同 id `npc-complete`。

裁定：接取人回退为真端 reward 名解析出的唯一 npc（accept-at-reward-npc）。
用法 / Usage: `python3 -B p0c58_challenge_sentinel_decisions.py [--out <tsv>]`
"""
from __future__ import annotations
import os

import argparse
import csv
import glob
from collections import Counter
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
NPC_DIR = REPO / "src/main/resources/aion/data/static_data/npcs"
RETAIL = REPO / "src/main/resources/aion/data/static_data/quest_retail"
CLIENT_NPC = Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/npcs_unpacked/client_npcs_npc.xml")
CLIENT_ACTIONS = REPO / "docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"
CENSUS = HERE / "p0c56-item-acquire-census.tsv"
DEFAULT_OUT = HERE / "p0c58-challenge-sentinel-decisions.tsv"
SENTINEL = "_challengetask_"

TABLE_COLUMNS = ("quest_id", "acquire_cat", "acquire_param", "dd_reward", "reward_npc_id", "client_quest_ai_name",
	"client_entry_page", "client_summary_rows", "client_hunt_progress_rows", "client_buttons",
	"legacy_npc_starts", "legacy_nodes", "conclusion", "basis")


def census_rows() -> list[dict[str, str]]:
	lines = [line for line in CENSUS.read_text(encoding="utf-8").splitlines() if not line.startswith("#")]
	header = lines[0].split("\t")
	return [dict(zip(header, line.split("\t"))) for line in lines[1:] if line.strip()]


def npc_ids_by_name() -> dict[str, set[int]]:
	out: dict[str, set[int]] = {}
	for path in sorted(glob.glob(str(NPC_DIR / "*.xml"))):
		text = Path(path).read_text(encoding="utf-8", errors="replace")
		for match in re.finditer(r"<npc_template\b([^>]*)>", text):
			attrs = dict(re.findall(r'(\w+)="([^"]*)"', match.group(1)))
			name, npc_id = attrs.get("name_desc"), attrs.get("npc_id")
			if name and npc_id:
				out.setdefault(name.lower(), set()).add(int(npc_id))
	return out


def client_npc_blocks() -> dict[int, str]:
	"""客户端 npc 块：npc id → `quest_ai_name`（外部客户端数据，缺文件则空表）。"""
	out: dict[int, str] = {}
	if not CLIENT_NPC.exists():
		return out
	text = CLIENT_NPC.read_text(encoding="utf-8", errors="replace")
	for block in re.finditer(r"<npc_client>(.*?)</npc_client>", text, re.S):
		body = block.group(1)
		npc_id = re.search(r"<id>(\d+)</id>", body)
		ai_name = re.search(r"<quest_ai_name>([^<]*)</quest_ai_name>", body)
		if npc_id and ai_name:
			out[int(npc_id.group(1))] = ai_name.group(1).strip()
	return out


def tsv_map(name: str) -> dict[str, str]:
	out = {}
	for line in (RETAIL / name).read_text(encoding="utf-8").splitlines():
		if not line.strip() or line.startswith("#"):
			continue
		parts = line.split("\t")
		out[parts[0]] = "|".join(parts[1:])
	return out


def client_buttons() -> dict[str, str]:
	out: dict[str, list[str]] = {}
	with CLIENT_ACTIONS.open(encoding="utf-8-sig", newline="") as handle:
		for row in csv.DictReader(handle):
			if row["source_variant"] != "active" or row["page_mapping"] != "exact":
				continue
			if not row["action_id"].isdigit():
				continue
			out.setdefault(row["quest_id"], []).append(
				f"{row['html_page_name'].lower()}#{row['action_id']}:{row['action_constant']}")
	return {quest: " | ".join(sorted(set(keys))) for quest, keys in out.items()}


def main() -> int:
	parser = argparse.ArgumentParser()
	parser.add_argument("--out", default=str(DEFAULT_OUT))
	args = parser.parse_args()
	npc_ids = npc_ids_by_name()
	client_ai = client_npc_blocks()
	entry_pages = tsv_map("quest_client_entry_pages.tsv")
	summary_rows = tsv_map("quest_client_summary_rows.tsv")
	hunt_rows = tsv_map("quest_client_hunt_progress_rows.tsv")
	buttons = client_buttons()

	rows = [row for row in census_rows() if row["acquire_param"].strip() == SENTINEL]
	lines = [
		"# P0c-58 / 续片 30 决策表：挑战任务哨兵 6 行采纳（接取人 = 真端交付 NPC 本人）",
		"# 判据 = DD(Talk ∧ value0_acquire_=_challengetask_ ∧ reward 名唯一解析) ∧ 客户端 npc 块 quest_ai_name == reward 名 ∧ 客户端接取页登记",
		"# 机械 = 哨兵回退为 reward 名 → 既有 hunt/pvp 合成器（accept-at-reward-npc；遗留见证同形：NPC_START + npc-complete 同 id）",
		"# basis=DD_CHALLENGE_SENTINEL_ACCEPT_AT_REWARD_NPC",
		"\t".join(TABLE_COLUMNS),
	]
	conclusion: Counter = Counter()
	for row in sorted(rows, key=lambda r: int(r["quest_id"])):
		reward = row["dd_reward"].strip()
		ids = sorted(npc_ids.get(reward.lower(), set()))
		quest_id = int(row["quest_id"])
		ai_name = ",".join(client_ai.get(i, "-") for i in ids) if ids else "-"
		ok = len(ids) == 1 and ai_name.lower() == reward.lower() and row["acquire_cat"].lower() == "talk"
		verdict = "ADOPT" if ok else "DEFER:evidence-incomplete"
		conclusion[verdict] += 1
		lines.append("\t".join((
			row["quest_id"], row["acquire_cat"], row["acquire_param"], reward,
			str(ids[0]) if len(ids) == 1 else ",".join(str(i) for i in ids) or "-",
			ai_name,
			entry_pages.get(row["quest_id"], "-"),
			summary_rows.get(row["quest_id"], "-"),
			hunt_rows.get(row["quest_id"], "-"),
			buttons.get(row["quest_id"], "-"),
			row["xml_npc_starts"], row["xml_nodes"],
			verdict,
			"DD_CHALLENGE_SENTINEL_ACCEPT_AT_REWARD_NPC",
		)))
	Path(args.out).write_text("\n".join(lines) + "\n", encoding="utf-8")
	print(f"wrote {args.out} rows={len(rows)} " + " ".join(f"{k}={v}" for k, v in sorted(conclusion.items())))
	return 0


if __name__ == "__main__":
	sys.exit(main())
