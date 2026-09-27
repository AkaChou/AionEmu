#!/usr/bin/env python3
"""生成客户端简报链登记表（quest_client_briefing_chains.tsv）。

数据源：仓库内的客户端映射
  docs/quest/client-dialog-mapping/quest-dialog-pages.csv        （页 → page_id）
  docs/quest/client-dialog-mapping/quest-dialog-action-details.csv（页 → 按钮 → action_id）
（`source_variant=active` 且 `*_mapping=exact`，由客户端 HTML 解包派生，随仓库版本冻结）

为什么需要它：真端 `Quest_SimpleHunt.xml` 有 `talk_npc1`（简报 NPC）列，但没有页链列；
客户端 5.8 的任务书里，带简报的任务在 `select2` 页上有 `HACTION_SELECT2_1` /
`HACTION_SELECT2_1_1` / `HACTION_SELECT2_2` 续页按钮，末页按钮是 `HACTION_SETPRO1` /
`HACTION_SETPRO2`（清简报标志位）。服务端必须为链上每个按钮给出路由，否则按钮无路由
（客户端契约门禁 BUTTON_WITHOUT_ROUTE），且击杀行会被客服端 `SECTION_5==0` 门控一直挡住。

链语义：从 `select2` 页开始，沿"页 i 的第一个按钮 → 与按钮同名的下一页"前进，直到按钮没有
同名下一页（终结点，通常是 SETPRO1/SETPRO2）。登记行格式：

  quest_id \t entry_page \t action_id:page_id [action_id:page_id ...]

`entry_page` 是入口页 id（服务端 QUEST_SELECT 展示的页，客户端即 select2）；`action_id` 是客户端
按钮的 HACTION id（与 AionEmu 的 QuestDialogAction 取值同源，如 10000=SETPRO1、10001=SETPRO2）；
`page_id=0` 表示终结点（该按钮不展示任务页，只清简报标志位并关窗）。
47 个真端 `talk_npc1` 行的链终结点全部是 SETPRO1/SETPRO2（45 + 2）。

用法：
  python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_briefing_chains.py
输出：
  src/main/resources/aion/data/static_data/quest_retail/quest_client_briefing_chains.tsv
"""

from __future__ import annotations

import csv
from collections import defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
MAPPING = REPO / "docs/quest/client-dialog-mapping"
PAGES = MAPPING / "quest-dialog-pages.csv"
DETAILS = MAPPING / "quest-dialog-action-details.csv"
OUT = (REPO / "src/main/resources/aion/data/static_data/quest_retail"
	"/quest_client_briefing_chains.tsv")

ENTRY_PAGE = "select2"
MAX_HOPS = 8


def load_pages() -> dict[int, dict[str, int]]:
	"""quest_id → {页名(小写): page_id}（active/exact）。"""
	pages: dict[int, dict[str, int]] = defaultdict(dict)
	with PAGES.open(encoding="utf-8-sig") as handle:
		for row in csv.DictReader(handle):
			if row.get("source_variant") != "active" or row.get("page_mapping") != "exact":
				continue
			quest_id = (row.get("quest_id") or "").strip()
			page_id = (row.get("page_id") or "").strip()
			if quest_id.isdigit() and page_id.isdigit():
				pages[int(quest_id)][(row.get("html_page_name") or "").strip().lower()] = int(page_id)
	return pages


def load_actions() -> dict[int, dict[int, list[tuple[int, str]]]]:
	"""quest_id → {page_id: [(action_id, action_constant)]}（按 select_index 排序）。"""
	actions: dict[int, dict[int, list[tuple[int, int, str]]]] = defaultdict(lambda: defaultdict(list))
	with DETAILS.open(encoding="utf-8-sig") as handle:
		for row in csv.DictReader(handle):
			if row.get("source_variant") != "active" or row.get("action_mapping") != "exact":
				continue
			quest_id = (row.get("quest_id") or "").strip()
			page_id = (row.get("page_id") or "").strip()
			action_id = (row.get("action_id") or "").strip()
			if not (quest_id.isdigit() and page_id.isdigit() and action_id.isdigit()):
				continue
			index = (row.get("select_index") or "0").strip() or "0"
			actions[int(quest_id)][int(page_id)].append(
				(int(index) if index.isdigit() else 0, int(action_id),
					(row.get("action_constant") or "").strip()))
	result: dict[int, dict[int, list[tuple[int, str]]]] = {}
	for quest_id, by_page in actions.items():
		result[quest_id] = {
			page_id: [(action_id, constant) for _, action_id, constant in sorted(entries)]
			for page_id, entries in by_page.items()
		}
	return result


def chain_for(pages: dict[str, int], actions: dict[int, list[tuple[int, str]]]) -> tuple[int, list[str]] | None:
	"""客户端简报链 → (entry_page, ["action:page", ...])；无 select2 页或链为空时返回 None。"""
	entry_page = pages.get(ENTRY_PAGE)
	if entry_page is None:
		return None
	hops: list[str] = []
	page_name: str | None = ENTRY_PAGE
	seen: set[str] = set()
	while page_name is not None and len(hops) <= MAX_HOPS:
		if page_name in seen:
			break
		seen.add(page_name)
		buttons = actions.get(pages.get(page_name, -1), [])
		if not buttons:
			break
		action_id, constant = buttons[0]
		next_name = constant.removeprefix("HACTION_").lower()
		next_page = pages.get(next_name)
		if next_page is None:
			hops.append("%d:0" % action_id)
			break
		hops.append("%d:%d" % (action_id, next_page))
		page_name = next_name
	return (entry_page, hops) if hops else None


def main() -> int:
	pages = load_pages()
	actions = load_actions()
	rows: list[tuple[int, int, list[str]]] = []
	for quest_id in sorted(pages):
		chain = chain_for(pages[quest_id], actions.get(quest_id, {}))
		if chain is not None:
			rows.append((quest_id, chain[0], chain[1]))
	header = (
		"# 客户端简报链登记表（quest_id, entry_page, hops）——从 docs/quest/client-dialog-mapping 派生\n"
		"# 生成：python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_briefing_chains.py\n"
		"# entry_page = 入口页（服务端 QUEST_SELECT 展示的 select2 页）；hops 为空格分隔的\n"
		"# action_id:page_id（客户端按钮链）；page_id=0 表示终结点（清简报标志位并关窗）。\n"
		"# 消费方：真端 SimpleHunt 的 talk_npc1 简报接线（其余家族的 select2 链由各自合成器处理）。\n"
	)
	OUT.write_text(header + "".join(
		"%d\t%d\t%s\n" % (quest_id, entry_page, " ".join(hops))
		for quest_id, entry_page, hops in rows), encoding="utf-8")
	print("客户端简报链登记 -> %s（%d 行）" % (OUT, len(rows)))
	terminals: dict[str, int] = defaultdict(int)
	for _, _, hops in rows:
		terminals[hops[-1].split(":")[0] + ("" if hops[-1].endswith(":0") else "(有页)")] += 1
	print("  终结点按钮分布:", dict(sorted(terminals.items(), key=lambda kv: -kv[1])[:8]))
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
