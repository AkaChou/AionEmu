#!/usr/bin/env python3
"""生成客户端报告页登记表（quest_client_report_pages.tsv）。

数据源：仓库内的客户端映射
  docs/quest/client-dialog-mapping/quest-dialog-action-details.csv
（`source_variant=active` 且 `action_mapping=exact`）

为什么需要它：真端 `Quest_SimpleHunt.xml` 有 `reward_npc_name`，但**没有"报告页"列**；客户端 5.8 的
报告页是"承载 `HACTION_SELECT_QUEST_REWARD`(1009) 按钮的那一页"——普通击杀任务在 `select2`
（页 1352），带简报的任务在 `select5`（页 2375，`select2` 被简报链占用）。服务端报告路由必须展示
客户端契约里的那一页，否则同一页号会被两条链共用（简报链 + 报告链），客户端契约门禁会判定
"可见按钮无路由"（BUTTON_WITHOUT_ROUTE）。

登记行格式：

  quest_id \t report_page

未入表的任务由合成器回落 `QuestDialogPage.SELECT2`（历史规范形）。

用法：
  python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_report_pages.py
输出：
  src/main/resources/aion/data/static_data/quest_retail/quest_client_report_pages.tsv
"""

from __future__ import annotations

import csv
from collections import defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
DETAILS = REPO / "docs/quest/client-dialog-mapping/quest-dialog-action-details.csv"
OUT = (REPO / "src/main/resources/aion/data/static_data/quest_retail"
	"/quest_client_report_pages.tsv")

REPORT_ACTION = 1009


def load_report_pages() -> dict[int, set[int]]:
	"""quest_id → {承载 1009 按钮的页 id}（active/exact）。"""
	pages: dict[int, set[int]] = defaultdict(set)
	with DETAILS.open(encoding="utf-8-sig") as handle:
		for row in csv.DictReader(handle):
			if row.get("source_variant") != "active" or row.get("action_mapping") != "exact":
				continue
			quest_id = (row.get("quest_id") or "").strip()
			page_id = (row.get("page_id") or "").strip()
			action_id = (row.get("action_id") or "").strip()
			if quest_id.isdigit() and page_id.isdigit() and action_id == str(REPORT_ACTION):
				pages[int(quest_id)].add(int(page_id))
	return pages


def main() -> int:
	pages = load_report_pages()
	# 同一任务在两页都有 1009 按钮时（450 个任务，全部在 SimpleHunt 族之外）无法由本表判定报告页，
	# 不入表：消费方回落历史规范页 SELECT2，细分留给后续切片。
	ambiguous = {quest_id for quest_id, ids in pages.items() if len(ids) > 1}
	rows = [(quest_id, sorted(ids)[0]) for quest_id, ids in sorted(pages.items()) if len(ids) == 1]
	header = (
		"# 客户端报告页登记表（quest_id, report_page）——从 docs/quest/client-dialog-mapping 派生\n"
		"# 生成：python3 -B .agents/summary/scriptdll-quest-driver/build_quest_client_report_pages.py\n"
		"# report_page = 承载 HACTION_SELECT_QUEST_REWARD(1009) 的页 id（普通行 1352=select2，\n"
		"# 简报行 2375=select5）；未入表（含同页多解任务）由合成器回落 QuestDialogPage.SELECT2。\n"
	)
	OUT.write_text(header + "".join(
		"%d\t%d\n" % (quest_id, page_id) for quest_id, page_id in rows), encoding="utf-8")
	print("客户端报告页登记 -> %s（%d 行，多页未入表 %d 个）" % (OUT, len(rows), len(ambiguous)))
	counts: dict[int, int] = defaultdict(int)
	for _, page_id in rows:
		counts[page_id] += 1
	print("  页分布 top:", dict(sorted(counts.items(), key=lambda kv: -kv[1])[:6]))
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
