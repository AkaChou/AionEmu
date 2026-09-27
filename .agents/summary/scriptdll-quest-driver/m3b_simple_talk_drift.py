#!/usr/bin/env python3
"""M3-b：SimpleTalk「真端合成 vs XML vs 客户端任务书」三方漂移登记。

用法（先由 Java 门禁导出分类，再用本脚本落登记）：
	mvn -o -q -Dtest=RetailSimpleTalkGateTest -Dretail.talk.equivOut=/tmp/talk-drift.txt test
	python3 -B .agents/summary/scriptdll-quest-driver/m3b_simple_talk_drift.py --classification /tmp/talk-drift.txt --write-registry

口径变更（2026-09-23，用户指令）：quest-definition XML 不再是准入金标准——它自身带有大量历史错误，
不能用作等价审计的判据。真端模板表 + quest.xml 元数据 + ScriptDLL64 语义是唯一权威；XML 只在
「真端无法表达」时保留。本脚本因此**只做登记**：把 XML 与真端合成器的差异、以及客户端任务书/
客户端脚本能提供的线索记录成 TSV，供人工判定谁正确，不再作为通过/失败判据。

三方数据源：
- 真端合成器：Java 侧 RetailSimpleTalkDefinitionCompiler（每轮由 Maven 门禁导出分类，见 pending TSV）
- 生产 XML：quest_definition/quests/<id>.xml（reward 投影、var0 值域、可见行集合）
- 客户端：data_unpacked/Dialogs/QUEST_Q<id>.html 的 quest_summary 行清单 +
  Quest_unpacked/quest_monster.csv 的 Progress(SECTION_n<X; SECTION_5==0) 客户端进度声明

用法：
	python3 -B .agents/summary/scriptdll-quest-driver/m3b_simple_talk_drift.py
输出：
	retail-simple-talk-drift.tsv（逐任务登记）
	stdout（分类 × 客户端形状 × XML 投影的交叉表）
"""

from __future__ import annotations

import argparse
import csv
import importlib.util
import re
import sys
from collections import Counter
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
REGISTRY = REPO / "src/test/resources/quest/retail-simple-talk-drift.tsv"
VARIANT_REGISTRY = HERE / "retail-simple-talk-client-variant.tsv"
# 真端推导覆盖的客户端交付按钮（其余按钮 = 真端表无法表达的客户端变体，保留 XML 降级）。
COVERED_REPORT_ACTIONS = {
	"HACTION_SELECT_QUEST_REWARD": "SELECT_QUEST_REWARD",
	"HACTION_CHECK_USER_HAS_QUEST_ITEM": "CHECK_USER_HAS_QUEST_ITEM",
	"HACTION_CHECK_USER_HAS_QUEST_ITEM_SIMPLE": "CHECK_USER_HAS_QUEST_ITEM_SIMPLE",
}
ITEM_CHECK_ACTIONS = {"HACTION_CHECK_USER_HAS_QUEST_ITEM", "HACTION_CHECK_USER_HAS_QUEST_ITEM_SIMPLE"}
RETAIL_TALK_TABLE = REPO / "src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml"
AUDIT = REPO / ".agents/summary/quest-10527-reward-row/audit_reward_row_vs_client_steps.py"
UNPACK = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked")
DIALOGS = Path("/Users/mc/PycharmProjects/unpak/data_unpacked/Dialogs")
OUTPUT = HERE / "retail-simple-talk-drift.tsv"


def load_audit_module():
	"""复用既有审计脚本的读取器（只调用纯函数，不触发它的 main()）。"""
	spec = importlib.util.spec_from_file_location("reward_row_audit", AUDIT)
	module = importlib.util.module_from_spec(spec)
	spec.loader.exec_module(module)
	return module


def load_classification(path: Path) -> dict[int, str]:
	"""读 Java 门禁导出的分类（唯一源头是 RetailSimpleTalkGateTest）。"""
	rows: dict[int, str] = {}
	for line in path.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			continue
		quest_id, classification = line.split("\t")[:2]
		rows[int(quest_id)] = classification
	return rows


def load_client_monster_rows() -> dict[int, list[str]]:
	rows: dict[int, list[str]] = {}
	with (UNPACK / "quest_monster.csv").open(encoding="utf-8", errors="replace") as handle:
		for raw in csv.DictReader(handle):
			row = {(k.strip() if isinstance(k, str) else k): (v.strip() if isinstance(v, str) else v)
				for k, v in raw.items()}
			quest_id = row["questId"]
			if not quest_id.isdigit():
				continue
			rows.setdefault(int(quest_id), []).append(
				f"{row.get('questProgress', '')}|{row.get('sourceType', '')}")
	return rows


def client_steps(dialogs_index: dict[int, Path], quest_id: int) -> list[str] | None:
	path = dialogs_index.get(quest_id)
	if path is None:
		return None
	text = path.read_text(encoding="utf-8", errors="ignore")
	page = re.search(r'<HtmlPage name="quest_summary">(.*?)</HtmlPage>', text, re.S)
	if page is None:
		return None
	return [re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", step)).strip()
		for step in re.findall(r"<step>(.*?)</step>", page.group(1), re.S)]


def client_report_action(dialogs: dict[int, Path], quest_id: int) -> tuple[str, bool]:
	"""客户端 select5 页的交付按钮与是否存在 select6 失败页。"""
	path = dialogs.get(quest_id)
	if path is None:
		return "NO_CLIENT_FILE", False
	text = path.read_text(encoding="utf-8", errors="ignore")
	pages: dict[str, str] = {}
	for match in re.finditer(r'<HtmlPage\b[^>]*\bname="([^"]+)"[^>]*>(.*?)</HtmlPage>', text, re.S):
		pages.setdefault(match.group(1).lower(), match.group(2))
	select5 = pages.get("select5")
	if select5 is None:
		return "NO_SELECT5_PAGE", "select6" in pages
	actions = re.findall(r"(HACTION_[A-Z0-9_]+)", select5)
	return (",".join(actions) if actions else "NO_ACTION"), "select6" in pages


def main() -> int:
	parser = argparse.ArgumentParser()
	parser.add_argument("--classification", required=True, help="Java 门禁导出的分类 TSV")
	parser.add_argument("--write-registry", action="store_true", help="落盘测试作用域漂移登记")
	args = parser.parse_args()
	audit = load_audit_module()
	classification = load_classification(Path(args.classification))
	monsters = load_client_monster_rows()
	dialogs = audit.client_index()
	cross: Counter[tuple[str, str, str, str, str]] = Counter()
	written = 0
	with OUTPUT.open("w", encoding="utf-8", newline="") as handle:
		writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
		writer.writerow(["quest_id", "classification", "client_rows", "client_last_row_text",
			"client_report_action", "client_has_select6", "client_script_progress", "xml_reward_var0",
			"xml_state_var0", "xml_var0_max", "xml_row_state_verdict", "xml_visible_shape",
			"client_section0"])
		for quest_id in sorted(classification):
			summary = audit.definition_summary(quest_id)
			if summary is None:
				continue
			steps = client_steps(dialogs, quest_id)
			row_count = len(steps) if steps is not None else 0
			last_row = row_count - 1 if steps else None
			reward_var0 = summary["reward"].get("var0")
			row_verdict, rows_without, out_of_range = audit.row_state_verdict(
				row_count if steps is not None else 0, last_row if steps is not None else None,
				summary["visible_rows"])
			shape = audit.visible_shape(row_count if steps is not None else 0, summary["visible_rows"])
			progress = " ".join(sorted({entry.split("|")[0] for entry in monsters.get(quest_id, [])}))
			report_action, has_select6 = client_report_action(dialogs, quest_id)
			writer.writerow([quest_id, classification[quest_id], row_count if steps is not None else "",
				(steps[-1] if steps else ""), report_action, "yes" if has_select6 else "no", progress, reward_var0,
				" ".join(str(value) for value in summary["visible_rows"]), summary["var0_max"],
				row_verdict, shape, audit.client_section0(quest_id)])
			written += 1
			code = classification[quest_id]
			head = "REJECTED" if code.startswith("REJECTED:") else code
			cross[(head, report_action, f"reward={reward_var0}", f"script={'yes' if progress else 'no'}",
				"sel6" if has_select6 else "-")] += 1
	print(f"登记 {written} 行 -> {OUTPUT}")
	table_item_check = {
		int(match.group(1))
		for match in re.finditer(r'<id id="(\d+)">(.*?)</id>', RETAIL_TALK_TABLE.read_text(encoding="utf-8"),
			re.S)
		if "<item_check>" in match.group(2)
	}
	variants = []
	for row in csv.DictReader(OUTPUT.open(encoding="utf-8"), delimiter="\t"):
		if row["classification"].startswith("REJECTED:"):
			continue
		action = row["client_report_action"]
		quest_id = int(row["quest_id"])
		gap = action not in COVERED_REPORT_ACTIONS
		if (quest_id in table_item_check) != (action in ITEM_CHECK_ACTIONS):
			# 真端表 item_check 与客户端可见交付按钮必须一致，否则该行只能保留 XML 降级。
			gap = True
		if gap:
			variants.append((row["quest_id"], action, "CLIENT_REPORT_VARIANT"))
	if args.write_registry:
		with VARIANT_REGISTRY.open("w", encoding="utf-8", newline="\n") as handle:
			handle.write("# SimpleTalk 客户端交付变体（真端表无法表达 → 保留 XML 降级）\n")
			handle.write("# quest_id\tclient_report_action\treason\n")
			for quest_id, action, reason in variants:
				handle.write(f"{quest_id}\t{action}\t{reason}\n")
		print(f"客户端变体 -> {VARIANT_REGISTRY}（{len(variants)} 行）")
		header = ("# SimpleTalk 真端合成 vs 历史 XML 漂移登记（quest_id, classification）\n"
			"# 生成：RetailSimpleTalkGateTest -Dretail.talk.equivOut=<path> + m3b_simple_talk_drift.py\n"
			"# 语义：登记不是退役门槛（2026-09-23 口径：XML 带历史错误，真端优先）；\n"
			"#       本表锁住分类，任何静默漂移都会让门禁 driftVersusLegacyXmlIsRegistered 失败。\n"
			"# classification: EQUIVALENT | DIFF:NODE_PROJECTION | DIFF:TRANSITION_SET | REJECTED:<稳定码>\n")
		with REGISTRY.open("w", encoding="utf-8", newline="\n") as handle:
			handle.write(header)
			for quest_id in sorted(classification):
				handle.write(f"{quest_id}\t{classification[quest_id]}\n")
		print(f"漂移登记 -> {REGISTRY}（{len(classification)} 行）")
	print("\n分类 × 客户端交付按钮 × XML reward 投影 × 客户端脚本：")
	for key, count in sorted(cross.items(), key=lambda item: (-item[1], item[0])):
		print(f"  {count:5d}  {key[0]:<32} {key[1]:<40} {key[2]:<11} {key[3]:<10} {key[4]}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
