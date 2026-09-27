#!/usr/bin/env python3
"""生成客户端对话出口登记表（真端合成器用来补"真端模板表没有列"的对话续页）。

数据源：仓库内的客户端映射 `docs/quest/client-dialog-mapping/quest-dialog-pages.csv`
（`source_variant=active` 且 `page_mapping=exact`，由客户端 HTML 解包派生，随仓库版本冻结）。

为什么需要它：真端 `Quest_SimpleTalk.xml` 只声明 acquired/reward NPC 与物品/过场轴，**没有页链列**；
但客户端 5.8 的任务页里，一部分任务的 `select1` 带 `HACTION_SELECT1_1` 续页按钮（剧情续页），
服务端必须给出对应路由，否则按钮无路由（客户端契约门禁 BUTTON_WITHOUT_ROUTE）。

登记表语义：quest_id → 该任务需要的客户端对话出口集合（当前只有 SELECT1_1）。
	SELECT_NONE_1 select_none 页有 select_none_1 续页 → 需要 `SELECT_NONE_1 -> SHOW SELECT_NONE_1` 路由
	             （遗留 15478 形；接取/拒绝按钮在该续页上）。
	SELECT1_1    接取 NPC 的 select1 页有 select1_1 续页 → 需要 `SELECT1_1 -> SHOW SELECT1_1` 路由。
	SELECT1_1_1  select1_1 页还有 select1_1_1 续页 → 需要 `SELECT1_1_1 -> SHOW SELECT1_1_1` 路由。
	SELECT6      报告 NPC 的交付检查有 select6 失败页 → 缺物品时下发 SELECT6；没有该页时下发关窗。

用法：
	python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_dialog_exits.py
输出：
	src/main/resources/aion/data/static_data/quest_retail/quest_client_dialog_exits.tsv
"""

from __future__ import annotations

import csv
from collections import defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
PAGES = REPO / "docs/quest/client-dialog-mapping/quest-dialog-pages.csv"
ACTIONS = REPO / "docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"
OUT = (REPO / "src/main/resources/aion/data/static_data/quest_retail"
	"/quest_client_dialog_exits.tsv")


def load_pages() -> dict[int, set[str]]:
	"""active/exact 客户端页名集合（按任务）。"""
	pages: dict[int, set[str]] = defaultdict(set)
	with PAGES.open(encoding="utf-8-sig") as handle:
		for row in csv.DictReader(handle):
			if row.get("source_variant") != "active" or row.get("page_mapping") != "exact":
				continue
			quest_id = row.get("quest_id", "").strip()
			if quest_id.isdigit():
				pages[int(quest_id)].add((row.get("html_page_name") or "").strip().lower())
	return pages


def load_actions() -> dict[int, dict[str, set[int]]]:
	"""读取 active/exact 页面按钮。 / Load active, exact page button actions."""
	actions: dict[int, dict[str, set[int]]] = defaultdict(lambda: defaultdict(set))
	with ACTIONS.open(encoding="utf-8-sig", newline="") as handle:
		for row in csv.DictReader(handle):
			if row.get("source_variant") != "active" or row.get("page_mapping") != "exact":
				continue
			quest_id = row.get("quest_id", "").strip()
			action_id = row.get("action_id", "").strip()
			if quest_id.isdigit() and action_id.isdigit():
				actions[int(quest_id)][row.get("html_page_name", "").lower()].add(int(action_id))
	return actions


def exits_for(names: set[str], page_actions: dict[str, set[int]]) -> list[str]:
	"""页名集合 → 服务端必须提供的对话出口。"""
	exits: list[str] = []
	if "select_none_1" in names:
		# select_none 页唯一按钮 SELECT_NONE_1(4763) 是服务端要发路由的续页（遗留 15478 形
		# unaccepted→unaccepted SELECT_NONE_1 → SHOW SELECT_NONE_1）；接取/拒绝按钮落在该页。
		exits.append("SELECT_NONE_1")
	if "select1_1" in names:
		exits.append("SELECT1_1")
	if "select1_1_1" in names:
		exits.append("SELECT1_1_1")
	if "select6" in names:
		exits.append("SELECT6")
	if 1353 in page_actions.get("select2", set()):
		exits.append("SELECT2_CONTINUE")
	if 39 in page_actions.get("select5", set()):
		exits.append("SELECT5_CHECK")
	if 20002 in page_actions.get("select5", set()):
		exits.append("SELECT5_CHECK_SIMPLE")
	return exits


def main() -> int:
	pages = load_pages()
	page_actions = load_actions()
	rows = [(quest_id, exits_for(names, page_actions.get(quest_id, {})))
		for quest_id, names in sorted(pages.items())]
	rows = [(quest_id, exits) for quest_id, exits in rows if exits]
	header = (
		"# 客户端对话出口登记表（quest_id, exits）——从 docs/quest/client-dialog-mapping 派生\n"
		"# 生成：python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_dialog_exits.py\n"
		"# exits 为空格分隔的出口标记；真端合成器据此补真端模板表没有列出的对话续页路由。\n"
		"# 标记：SELECT_NONE_1 = select_none 续页（4763，接取/拒绝按钮所在页）；"
		"SELECT1_1 / SELECT1_1_1 = 接取续页；SELECT2_CONTINUE = SELECT2 按钮 1353；"
		"SELECT6 = 交付失败页；"
		"SELECT5_CHECK / SELECT5_CHECK_SIMPLE = SELECT5 按钮 39 / 20002。\n"
	)
	OUT.write_text(header + "".join(f"{quest_id}\t{' '.join(exits)}\n" for quest_id, exits in rows),
		encoding="utf-8")
	print(f"客户端对话出口登记 -> {OUT}（{len(rows)} 行）")
	for token in ("SELECT_NONE_1", "SELECT1_1", "SELECT1_1_1", "SELECT2_CONTINUE", "SELECT6",
			"SELECT5_CHECK", "SELECT5_CHECK_SIMPLE"):
		print(f"  {token}: {sum(1 for _, exits in rows if token in exits)}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
