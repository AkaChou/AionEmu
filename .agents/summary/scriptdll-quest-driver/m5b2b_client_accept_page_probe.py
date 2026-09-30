#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""客户端口径复核：任务书 HTML 里有没有标准 `select1`（NPC 对话接取页）。

与另两条独立证据对齐：
1. 真端表 `legacy-quest-dialog-template-index.csv` 的 `start_npc_ids` 是否为哨兵 `0`；
2. DLL 事件签名是否含 `{0x1c,0x1d,0x26}` 三元组；
3. 客户端 `QUEST_Q<id>.html` 是否有 `select1` 页（本脚本）。

用法：
    python3 -B m5b2b_client_accept_page_probe.py [--ids 35007 1103 ...] [--out <tsv>]
"""
from __future__ import annotations
import os

import argparse
import re
import sys
from collections import Counter
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

REPO_ROOT = Path(__file__).resolve().parents[3]
BASE = REPO_ROOT / ".agents/summary/scriptdll-quest-driver"
CENSUS = BASE / "m5b2b-quest-event-census.tsv"
CONTRACT_INDEX = REPO_ROOT / "docs/quest/client-dialog-mapping/legacy-quest-dialog-template-index.csv"
DEFAULT_FAMILY = REPO_ROOT / "src/test/resources/quest/retail-simple-collect-item-drift.tsv"
DEFAULT_OUT = BASE / "m5b2b-client-accept-page-vs-sentinel.tsv"
# 解包后的客户端对话页目录 / Unpacked client dialog page root
CLIENT_DIALOG_ROOT = Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}/data_unpacked/Dialogs")
# `<HtmlPage name="...">` 提取 / HtmlPage name extraction
HTML_PAGE = re.compile(r'<HtmlPage\s+name="([^"]+)"')
TRIPLET = ("0x1c", "0x1d", "0x26")


# 客户端 HTML 索引缓存：小写文件名 -> 路径 / Client html index cache: lowercased name -> path
_HTML_INDEX: dict[str, Path] | None = None


def build_html_index() -> dict[str, Path]:
	"""一次性索引全部客户端任务书 HTML（避免逐任务扫目录）。 / Index every client quest html once."""
	global _HTML_INDEX
	if _HTML_INDEX is None:
		index: dict[str, Path] = {}
		for path in CLIENT_DIALOG_ROOT.rglob("*.html"):
			name = path.name.lower()
			if name.startswith("quest_q"):
				index.setdefault(name, path)
		_HTML_INDEX = index
	return _HTML_INDEX


def find_client_html(quest_id: int) -> Path | None:
	"""按任务 ID 找客户端对话 HTML（大小写两种命名都可能存在）。 / Locate the client dialog html."""
	index = build_html_index()
	return index.get(f"quest_q{quest_id}.html")


def load_events() -> dict[int, set[str]]:
	"""读事件码普查表。 / Load the event census."""
	events: dict[int, set[str]] = {}
	for line in CENSUS.read_text(encoding="utf-8", errors="replace").splitlines()[1:]:
		parts = line.split("\t")
		if len(parts) < 4 or parts[3].startswith("qid:") or not parts[0].isdigit():
			continue
		events.setdefault(int(parts[0]), set()).add(parts[3])
	return events


def load_contract_index() -> dict[int, list[str]]:
	"""读真端对话契约索引。 / Load the retail dialog contract index."""
	index: dict[int, list[str]] = {}
	for line in CONTRACT_INDEX.read_text(encoding="utf-8", errors="replace").splitlines()[1:]:
		columns = line.split(",")
		if columns and columns[0].isdigit():
			index[int(columns[0])] = columns
	return index


def load_ids(path: Path) -> list[int]:
	"""读家族任务 ID。 / Load family quest ids."""
	ids: set[int] = set()
	for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
		if not line.strip() or line.startswith("#"):
			continue
		token = line.split("\t")[0].strip()
		if token.isdigit():
			ids.add(int(token))
	return sorted(ids)


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("ids", nargs="*", type=int)
	parser.add_argument("--family-src", type=Path, default=DEFAULT_FAMILY)
	parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
	args = parser.parse_args(argv)

	events = load_events()
	index = load_contract_index()
	quest_ids = args.ids or load_ids(args.family_src)
	matrix: Counter[tuple[str, bool]] = Counter()
	rows: list[tuple[str, ...]] = []
	no_contract = 0
	for quest in quest_ids:
		path = find_client_html(quest)
		pages = HTML_PAGE.findall(path.read_text(encoding="utf-8", errors="replace")) if path else []
		lowered = {page.lower() for page in pages}
		has_select1 = any(page.startswith("select1") for page in lowered)
		has_ask_accept = any("ask_quest_accept" in page for page in lowered)
		record = index.get(quest)
		start_ids = record[4].strip() if record else "?"
		# 三态：哨兵 / 真实 NPC / 无契约行（"?" 不能当真实 NPC 计数）
		# Three states: sentinel / real npc / no contract row ("?" must not be counted as real).
		if record is None:
			start_state = "no-contract"
			no_contract += 1
		elif start_ids in ("", "0"):
			start_state = "sentinel"
		else:
			start_state = "real"
		triplet = set(TRIPLET) <= events.get(quest, set())
		registered = quest in events
		if path:
			matrix[(start_state, has_select1)] += 1
		rows.append(
			(
				str(quest),
				start_ids,
				"triplet" if triplet else ("no-triplet" if registered else "no-registration"),
				"select1" if has_select1 else ("ask_quest_accept" if has_ask_accept else "no-accept-page"),
				path.name if path else "MISSING",
				",".join(sorted(pages)[:6]),
			)
		)

	print("(start_state, has_select1) over the family / 二维表：")
	print(f"  sentinel & select1    : {matrix[('sentinel', True)]}")
	print(f"  sentinel & !select1   : {matrix[('sentinel', False)]}")
	print(f"  real     & select1    : {matrix[('real', True)]}")
	print(f"  real     & !select1   : {matrix[('real', False)]}")
	print(f"  no-contract rows      : {no_contract}")
	print(f"  rows WITHOUT client html: {sum(1 for row in rows if row[4] == 'MISSING')}")
	print("(triplet_status, accept style) / 交叉表：")
	cross = Counter((row[2], row[3]) for row in rows if row[4] != "MISSING")
	for key in sorted(cross):
		print(f"  {key[0]:<16} {key[1]:<18} {cross[key]}")

	header = ["quest_id", "contract_start_npc", "triplet_status", "client_accept_style", "client_html", "html_pages"]
	args.out.write_text(
		"\n".join(["\t".join(header)] + ["\t".join(row) for row in rows]) + "\n", encoding="utf-8"
	)
	print(f"wrote {args.out}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
