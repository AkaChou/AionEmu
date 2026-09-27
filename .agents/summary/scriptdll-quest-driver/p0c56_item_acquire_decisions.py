#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-56 物品接取轴决策表：从普查 TSV + 静态源生成逐行裁定（只读生成，产出 TSV）。

判据（静态可判定，不依赖 checkout 态遗留 XML 的存在性）：
  * 真端 DD 表 `category_acquire_ = ItemPlay`（接取参数 = 任务起始道具符号）——接取轴权威；
  * 客户端 action-details（active/exact 行）该 quest 的按钮签名——客户端确实有接取/拒绝页；
  * 道具模板 actions 子元素（queststart/read/无）——道具侧行为见证（证据列，不作否决判据：
    AionEmu 侧不消费 QuestStartAction，接取窗由服务端 SHOW_QUEST_PAGE 下发，遗留实现同形）；
  * 遗留 XML 见证（use-item 同 id + 无主 ACCEPT/REFUSE）——工作区见证列。

裁定：
  ADOPT                              ItemPlay ∧ 道具解析成功 ∧ category1≠event
  DEFER:RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED   ItemPlay ∧ category1=event（事件 NPC 接取见证冲突 + 激活轴缺口）
  DEFER:RETAIL_ITEMPLAY_ACQUIRE_ITEM_UNRESOLVED  ItemPlay ∧ 道具符号未解析
  DEFER:challenge_task-axis          Talk ∧ 接取参数 = `_challengetask_` 哨兵（后续切片）

用法 / Usage: `python3 -B p0c56_item_acquire_decisions.py [--out <tsv>]`
"""
from __future__ import annotations

import argparse
import csv
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent.parent
ITEMS = REPO / "src/main/resources/aion/data/static_data/items/item"
CLIENT_ACTIONS = REPO / "docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"
CENSUS = HERE / "p0c56-item-acquire-census.tsv"
DEFAULT_OUT = HERE / "p0c56-item-acquire-decisions.tsv"

TABLE_COLUMNS = ("quest_id", "acquire_cat", "item_symbol", "item_id", "item_actions", "client_buttons",
	"legacy_use_items", "legacy_nodes", "conclusion", "basis")


def census_rows() -> list[dict[str, str]]:
	lines = [line for line in CENSUS.read_text(encoding="utf-8").splitlines() if not line.startswith("#")]
	header = lines[0].split("\t")
	return [dict(zip(header, line.split("\t"))) for line in lines[1:] if line.strip()]


def item_actions() -> dict[str, str]:
	"""物品 id → actions 子元素摘要（queststart/read/…；空 = 模板无 actions 块）。"""
	out: dict[str, str] = {}
	for path in sorted(ITEMS.glob("*.xml")):
		text = path.read_text(encoding="utf-8")
		for block in re.findall(r"<item_template\b(.*?)</item_template>", text, re.S):
			found = re.search(r"\bid=\"(\d+)\"", block)
			if not found:
				continue
			tags = []
			action_block = re.search(r"<actions>(.*?)</actions>", block, re.S)
			if action_block:
				tags = re.findall(r"<([a-z_]+)(?:\s[^>]*)?/>", action_block.group(1))
			out[found.group(1)] = "+".join(tags) if tags else "none"
	return out


def client_buttons() -> dict[str, str]:
	"""quest_id → `page#id:HACTION_*` 签名（客户端 action-details 的 active/exact 行）。"""
	out: dict[str, list[str]] = {}
	with CLIENT_ACTIONS.open(encoding="utf-8-sig", newline="") as handle:
		for row in csv.DictReader(handle):
			if row["source_variant"] != "active" or row["page_mapping"] != "exact":
				continue
			if not row["action_id"].isdigit():
				continue
			key = f"{row['html_page_name'].lower()}#{row['action_id']}:{row['action_constant']}"
			out.setdefault(row["quest_id"], []).append(key)
	return {quest: " | ".join(sorted(set(keys))) for quest, keys in out.items()}


def decide(row: dict[str, str], actions: dict[str, str]) -> tuple[str, str]:
	category = row["acquire_cat"].lower()
	if category != "itemplay":
		if row["acquire_param"] == "_challengetask_":
			return "DEFER:challenge_task-axis", "Talk 接取 + 哨兵参数（挑战任务轴，后续切片）"
		return "DEFER:acquire-axis-unknown", f"非物品接取类别 {row['acquire_cat']}"
	if row["category1"] == "event":
		return ("DEFER:RETAIL_ITEMPLAY_ACQUIRE_EVENT_DEFERRED",
			"事件轴未落地：遗留见证为事件 NPC 接取，与道具触发冲突")
	if row["item_id"] == "-":
		return "DEFER:RETAIL_ITEMPLAY_ACQUIRE_ITEM_UNRESOLVED", "道具符号未在客户端名表解析"
	return "ADOPT", "DD_ITEMPLAY_ACQUIRE"


def main() -> int:
	parser = argparse.ArgumentParser()
	parser.add_argument("--out", default=str(DEFAULT_OUT))
	args = parser.parse_args()
	actions = item_actions()
	buttons = client_buttons()
	rows = census_rows()
	lines = [
		"# P0c-56 / 续片 29 决策表：物品接取轴（ItemPlay 非 event 12 行采纳；event 3 行 + challenge 6 行 延后）",
		"# 判据 = DD category_acquire_=ItemPlay ∧ 客户端 action-details 声明该行接取/拒绝按钮 ∧ 道具模板/遗留 XML 见证列",
		"# 机械 = itemSymbol（大小写不敏感）→ 物品 id → UseItem(item) → ShowQuestDialog(入口页) → 无主 ACCEPT_SIMPLE/REFUSE_SIMPLE",
		"# basis=DD_ITEMPLAY_ACQUIRE",
		"\t".join(TABLE_COLUMNS),
	]
	counts: dict[str, int] = {}
	for row in rows:
		conclusion, basis = decide(row, actions)
		counts[conclusion] = counts.get(conclusion, 0) + 1
		lines.append("\t".join((
			row["quest_id"],
			row["acquire_cat"],
			row["item_symbol"],
			row["item_id"],
			actions.get(row["item_id"], "-") if row["item_id"] != "-" else "-",
			buttons.get(row["quest_id"], "-"),
			row["xml_use_items"],
			row["xml_nodes"],
			conclusion,
			basis,
		)))
	Path(args.out).write_text("\n".join(lines) + "\n", encoding="utf-8")
	print(f"wrote {args.out} rows={len(rows)} " + " ".join(f"{k}={v}" for k, v in sorted(counts.items())))
	return 0


if __name__ == "__main__":
	sys.exit(main())
