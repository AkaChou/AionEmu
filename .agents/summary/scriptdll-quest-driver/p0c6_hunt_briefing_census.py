#!/usr/bin/env python3
"""P0c-6 普查：SimpleHunt 网格族 `talk_npc1` 行的对话框轴证据表。

对真端表里 47 个带 `talk_npc1` 的 SimpleHunt 行，逐行汇总四路证据：

  1. 真端表：acquired/talk/reward 三个名字与计数器槽位；
  2. 客户端任务书 `quest_q<id>.html`：`select2`/`select2_1`/`select2_2`/`select5` 页与
     `HACTION_SELECT2_1`/`SELECT2_2`/`SETPRO1` 按钮（简报链的强第二证据）；
  3. 客户端 `quest_monster.csv`：击杀行门控（`SECTION_n<k` 与 `SECTION_5==0` 占位）；
  4. 生产 quest-definition XML：现有路由里对"简报链"的表达（task/talk NPC 的
     QUEST_SELECT→SELECT2、SELECT2_1/SELECT2_2、SETPRO1）。

输出 TSV 供逐行裁定；脚本只读，不改任何资源。
Census of the four evidence sources for the retail `talk_npc1` SimpleHunt rows.
"""
from __future__ import annotations
import os

import pathlib
import re
import sys

REPO = pathlib.Path(__file__).resolve().parents[3]
TABLE = REPO / "src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml"
RETENTION = REPO / "src/test/resources/quest/retail-xml-retention.tsv"
QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
UNPAK = pathlib.Path(f"{REPO.parent / 'PycharmProjects' / 'unpak'}")
DIALOGS = UNPAK / "data_unpacked/Dialogs"
MONSTER = UNPAK / "Quest_unpacked/quest_monster.csv"

ID_BLOCK = re.compile(r'<id id="(\d+)">([\s\S]*?)</id>')
ACTION = re.compile(r'HACTION_([A-Z0-9_]+)')
OUT = pathlib.Path(__file__).resolve().parent / "p0c6-hunt-briefing-census.tsv"


def field(block: str, tag: str) -> str:
	"""取一个真端字段（首个子标签文本）。 / Reads one retail field."""
	m = re.search(r"<%s>(.*?)</%s>" % (tag, tag), block, re.S)
	return m.group(1).strip() if m else ""


def table_rows() -> dict[int, dict]:
	text = TABLE.read_text(encoding="utf-8")
	rows: dict[int, dict] = {}
	for match in ID_BLOCK.finditer(text):
		quest_id, block = int(match.group(1)), match.group(2)
		if "talk_npc1" not in block:
			continue
		slots = []
		for slot in range(1, 9):
			count = field(block, "count%d" % slot)
			if count:
				slots.append("c%d=%s" % (slot, count))
		rows[quest_id] = {
			"acquired": field(block, "acquired_npc_name"),
			"talk": field(block, "talk_npc1"),
			"reward": field(block, "reward_npc_name"),
			"slots": ",".join(slots),
		}
	return rows


def retention_owners() -> dict[int, tuple[str, str, str]]:
	rows: dict[int, tuple[str, str, str]] = {}
	for line in RETENTION.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			continue
		parts = line.split("\t")
		if len(parts) > 3:
			rows[int(parts[0])] = (parts[1], parts[2], parts[3])
	return rows


def dialog_index() -> dict[int, pathlib.Path]:
	"""客户端任务书索引：quest_id → HTML 路径（大小写与子目录都不固定）。"""
	index: dict[int, pathlib.Path] = {}
	for path in DIALOGS.rglob("*.html"):
		match = re.fullmatch(r"quest_q(\d+)\.html", path.name, re.I)
		if match is not None:
			index.setdefault(int(match.group(1)), path)
	return index


def client_pages(quest_id: int, index: dict[int, pathlib.Path]) -> tuple[list[str], list[str], str]:
	"""客户端任务书：页集合、HACTION 集合、任务书步的 visible 槽位。"""
	path = index.get(quest_id)
	if path is None:
		return [], [], ""
	text = path.read_text(encoding="utf-8", errors="replace")
	pages = re.findall(r'<HtmlPage name="([^"]+)"', text)
	actions = sorted(set(ACTION.findall(text)))
	summary = re.search(r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', text, re.S | re.I)
	slots: list[str] = []
	if summary:
		for step in re.findall(r"<step>(.*?)</step>", summary.group(1), re.S | re.I):
			first = re.search(r'visible="\[%(\d+)\]"', step)
			slots.append(first.group(1) if first else "?")
	return pages, actions, ",".join(slots)


def client_gates(quest_id: int) -> str:
	gates = []
	for line in MONSTER.read_text(encoding="utf-8", errors="replace").splitlines():
		parts = line.split(",")
		if len(parts) >= 2 and parts[0].strip().isdigit() and int(parts[0].strip()) == quest_id:
			gates.append(parts[1].strip())
	return " | ".join(gates)


def xml_routes(quest_id: int) -> dict:
	"""生产 XML 里简报链的表达（任务 NPC 的页路由 / SETPRO1）。"""
	path = QUESTS / ("%d.xml" % quest_id)
	if not path.is_file():
		return {"present": False, "select2": "", "chain": "", "setpro1": "", "npc_start": ""}
	text = path.read_text(encoding="utf-8")
	routes = re.findall(r"<transition[^>]*>([\s\S]*?)</transition>", text)
	page_routes: list[str] = []
	select2_all: list[str] = []
	setpro1: list[str] = []
	for route in routes:
		npc = re.search(r'npc-id="(\d+)"', route)
		action = re.search(r'action="([A-Z0-9_]+)"', route)
		page = re.search(r'page="([A-Z0-9_]+)"', route)
		if npc and action:
			item = "%s:%s" % (npc.group(1), action.group(1))
			page_routes.append(item)
			if action.group(1) == "QUEST_SELECT" and page and page.group(1).startswith("SELECT2"):
				select2_all.append(item + "->" + page.group(1))
			if action.group(1).startswith("SELECT2_") and page:
				select2_all.append(item + "->" + page.group(1))
			if action.group(1) == "SETPRO1":
				setpro1.append(item)
	shorthand = re.findall(r'<dialog type="NPC_START"[^>]*npc-id="(\d+)"', text)
	return {
		"present": True,
		"select2": " ".join(sorted(set(select2_all))),
		"chain": " ".join(sorted(set(page_routes))),
		"setpro1": " ".join(sorted(set(setpro1))),
		"npc_start": " ".join(sorted(set(shorthand))),
	}


def main() -> int:
	rows = table_rows()
	owners = retention_owners()
	index = dialog_index()
	header = [
		"quest_id", "owner", "family", "reason", "acquired", "talk_npc1", "reward",
		"talk==reward", "talk==acquired", "slots",
		"client_pages", "client_actions", "summary_slots", "client_gates",
		"xml_present", "xml_select2", "xml_setpro1", "xml_npc_start",
	]
	lines = ["\t".join(header)]
	for quest_id in sorted(rows):
		row = rows[quest_id]
		owner = owners.get(quest_id, ("-", "-", "-"))
		pages, actions, slots = client_pages(quest_id, index)
		xml = xml_routes(quest_id)
		brief_actions = [a for a in actions if a.startswith("SELECT2_") or a == "SETPRO1"]
		lines.append("\t".join([
			str(quest_id), owner[0], owner[1], owner[2],
			row["acquired"], row["talk"], row["reward"],
			"Y" if row["talk"] == row["reward"] else "N",
			"Y" if row["talk"] == row["acquired"] else "N",
			row["slots"],
			"|".join(pages), "|".join(brief_actions), slots, client_gates(quest_id),
			"Y" if xml["present"] else "N", xml["select2"], xml["setpro1"], xml["npc_start"],
		]))
	OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
	print("wrote %s rows=%d" % (OUT, len(rows)))
	for quest_id in sorted(rows):
		owner = owners.get(quest_id, ("-",))
		if owner[0] == "XML_RETENTION":
			print(_summary_line(quest_id, rows[quest_id], owners, index))
	return 0


def _summary_line(quest_id: int, row: dict, owners: dict, index: dict) -> str:
	pages, actions, slots = client_pages(quest_id, index)
	brief = [a for a in actions if a.startswith("SELECT2_") or a == "SETPRO1"]
	return "  %d owner=%s reason=%s talk=%s reward=%s 客户端简报按钮=%s 摘要槽=%s" % (
		quest_id, owners.get(quest_id, ("-",))[0], owners.get(quest_id, ("-", "-", "-"))[2],
		row["talk"], row["reward"], ",".join(brief) or "无", slots or "无")


if __name__ == "__main__":
	sys.exit(main())
