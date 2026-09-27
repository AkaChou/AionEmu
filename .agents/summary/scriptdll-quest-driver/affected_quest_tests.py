#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
affected_quest_tests.py - Quest impact test selector / 任务影响测试选择器

Given quest ids, scan every src/test/java/**/*Test.java (and *Tests.java when
present) for digit-bounded mentions, then emit ready-to-use Surefire -Dtest=
selectors. / 给定任务 ID，扫描 src/test/java 下所有 *Test.java（以及实际存在的
*Tests.java）中的数字边界引用，并生成可直接交给 Surefire 的 -Dtest= 选择器。

The digit boundary avoids 2504 matching 125045, and intentionally still matches
identifier forms such as QUEST_2504. / 数字边界可避免 2504 误命中 125045，同时有意
保留 QUEST_2504 这类标识符写法。

Usage / 用法:
	python3 -B .agents/summary/scriptdll-quest-driver/affected_quest_tests.py 2585 1136 2237 14150
	python3 -B .agents/summary/scriptdll-quest-driver/affected_quest_tests.py --json 2585 1136
	python3 -B .agents/summary/scriptdll-quest-driver/affected_quest_tests.py --wide-gates 2585

Exit codes / 退出码:
	0 success / 成功
	2 invalid arguments or missing test root / 参数非法或测试目录不存在
	3 a fixed T1 gate class was not found under the test root / 固定 T1 门禁类未在测试根目录解析到
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import time
from pathlib import Path

# Repository root derived from this file: .agents/summary/<topic>/<script>.py / 由文件位置反推仓库根目录
REPO_ROOT = Path(__file__).resolve().parents[3]
# Default test source root / 默认测试源码根目录
DEFAULT_TEST_ROOT = REPO_ROOT / "src/test/java"
# Hard performance budget required by the driver / 本工具要求的性能上限
PERFORMANCE_LIMIT_SECONDS = 5.0
# Canonical T1 gate classes; every entry must resolve on disk or the run is reported as incomplete.
# 固定 T1 门禁类；任一条目无法在磁盘解析时，报告必须标记为不完整。
T1_GATE_CLASSES = (
	"RetailSimpleCollectItemGateTest",
	"RetailDataDrivenGateTest",
	"RetailSimpleSerialHuntGateTest",
	"QuestInteractionObjectContractGateTest",
	"QuestProductionStartupGateTest",
	"RetailOwnershipGateTest",
	"RetailQuestCatalogTest",
	"RetailQuestDriverOverlayTest",
	"RetailSystemGrantDispatchTest",
	"QuestClientContractGateTest",
	"ProductionCatalogWhitelistVerificationTest",
	"QuestDefinitionCatalogManifestTest",
	"RetailSimpleHuntFamilyGateTest",
	"RetailSimpleTalkGateTest",
	"RetailSimpleTalkChainGateTest",
	"RetailNonIrAxisGateTest",
	"RetailSimpleItemPlayGateTest",
	"QuestItemPlayGrantGateTest",
	# P0c-52 真端化/新增常设门：击杀计数合同（生产视图）、单段 hunt 计数轴、对话名组通道。
	# P0c-52 permanent gates: the kill-counter contract on the production view, the single-stage hunt
	# count axis and the dialog-name group channel.
	"QuestKillCounterRetailGateTest",
	"RetailHuntClientCountGateTest",
	"RetailQuestAiNameGroupGateTest",
	# 真端驱动原生生命周期黑盒契约门（quest-native-dispatch P0-1）：接取→进度→交付→结算。
	# Native retail lifecycle black-box contract gate (quest-native-dispatch P0-1).
	"RetailQuestContractTest",
	# P0-2 DD 尾片：防漂移门禁（TSV 清单冻结）。
	# Anti-drift gate: the retail TSV manifest is frozen.
	"RetailTsvManifestGateTest",
	"RetailBriefingChainEvidenceGateTest",
)
# Class-name tokens used only inside the quest engine test package by --wide-gates.
# 仅供 --wide-gates 在任务引擎测试包内使用的类名关键词。
GATE_CLASS_KEYWORDS = (
	"Gate",
	"Contract",
	"Catalog",
	"Manifest",
	"Whitelist",
	"Ownership",
	"Overlay",
)
# Wide gate discovery never leaves this package prefix, so unrelated gameplay gates stay out.
# 宽口径门禁发现不越过该包前缀，避免误捕与本目标无关的玩法门禁。
GATE_SCAN_PACKAGE_PREFIX = "com.aionemu.gameserver.questEngine."
# Java package declaration, used to recover the real FQCN / Java package 声明，用于恢复真实 FQCN
PACKAGE_PATTERN = re.compile(
	r"^[ \t]*package[ \t]+([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*)[ \t]*;",
	re.MULTILINE,
)


def parse_args(argv: list[str]) -> tuple[list[str], argparse.Namespace]:
	"""Parse CLI arguments and normalize quest ids. / 解析命令行参数并规范化任务 ID。"""
	parser = argparse.ArgumentParser(
		description=(
			"Find test classes affected by quest ids and build Surefire -Dtest= selectors. / "
			"反查受任务 ID 影响的测试类并生成 Surefire -Dtest= 选择器。"
		),
	)
	parser.add_argument(
		"quest_ids",
		nargs="+",
		metavar="QUEST_ID",
		help="Numeric quest id(s), for example 2585 1136. / 数字任务 ID，例如 2585 1136。",
	)
	parser.add_argument(
		"--json",
		action="store_true",
		help="Emit machine-readable JSON. / 输出机器可读 JSON。",
	)
	parser.add_argument(
		"--wide-gates",
		action="store_true",
		help=(
			"Add every quest-engine test class whose simple name matches the gate keywords "
			"on top of the fixed T1 set. / 在固定 T1 集合之外，追加任务引擎包内所有类名命中门禁关键词的测试类。"
		),
	)
	parser.add_argument(
		"--test-root",
		type=Path,
		default=DEFAULT_TEST_ROOT,
		help=f"Test source root, default {DEFAULT_TEST_ROOT}. / 测试源码根目录，默认 {DEFAULT_TEST_ROOT}。",
	)
	args = parser.parse_args(argv)
	args.test_root = args.test_root.expanduser().resolve()
	try:
		quest_ids = normalize_quest_ids(args.quest_ids)
	except ValueError as exc:
		parser.error(str(exc))
	return quest_ids, args


def normalize_quest_ids(raw_ids: list[str]) -> list[str]:
	"""Normalize ids to canonical decimal strings, de-duplicated in input order. / 将 ID 规范化为十进制字符串并按输入顺序去重。"""
	normalized: list[str] = []
	seen: set[str] = set()
	for raw_id in raw_ids:
		token = raw_id.strip()
		if not token.isdigit():
			raise ValueError(f"quest id must be numeric: {raw_id!r} / 任务 ID 必须为数字: {raw_id!r}")
		canonical = str(int(token))
		if canonical not in seen:
			seen.add(canonical)
			normalized.append(canonical)
	if not normalized:
		raise ValueError("at least one quest id is required / 至少需要一个任务 ID")
	return normalized


def iter_test_files(test_root: Path) -> list[Path]:
	"""Return sorted *Test.java and *Tests.java sources. / 返回排序后的 *Test.java 与 *Tests.java 源文件。"""
	if not test_root.is_dir():
		raise FileNotFoundError(f"test root does not exist: {test_root} / 测试根目录不存在: {test_root}")
	files = [
		path
		for path in test_root.rglob("*.java")
		if path.name.endswith("Test.java") or path.name.endswith("Tests.java")
	]
	return sorted(files, key=lambda path: str(path))


def derive_fqcn(path: Path, test_root: Path, source: str) -> str:
	"""Derive the test class FQCN from package declaration or relative path. / 从 package 声明或相对路径推导测试类 FQCN。"""
	class_name = path.name[: -len(".java")]
	match = PACKAGE_PATTERN.search(source)
	if match:
		return f"{match.group(1)}.{class_name}"
	relative = path.relative_to(test_root).with_suffix("")
	return ".".join(relative.parts)


def build_quest_pattern(quest_ids: list[str]) -> re.Pattern[str]:
	"""Compile one alternation with digit boundaries for all ids. / 为全部 ID 编译带数字边界的单一交替正则。"""
	alternatives = sorted(quest_ids, key=lambda value: (-len(value), value))
	body = "|".join(re.escape(value) for value in alternatives)
	return re.compile(rf"(?<!\d)({body})(?!\d)")


def collect_gate_classes(all_fqcns: list[str], wide_gates: bool) -> tuple[list[str], list[str]]:
	"""Resolve the fixed T1 gates and, in wide mode, the quest-engine keyword gates.
	/ 解析固定 T1 门禁；宽口径模式下再补齐任务引擎包内的关键词门禁。"""
	sorted_fqcns = sorted(all_fqcns)
	by_simple_name: dict[str, str] = {
		fqcn.rsplit(".", 1)[-1]: fqcn for fqcn in sorted_fqcns
	}
	missing_t1 = [name for name in T1_GATE_CLASSES if name not in by_simple_name]
	gates = {by_simple_name[name] for name in T1_GATE_CLASSES if name in by_simple_name}
	if wide_gates:
		for fqcn in sorted_fqcns:
			if not fqcn.startswith(GATE_SCAN_PACKAGE_PREFIX):
				continue
			simple_name = fqcn.rsplit(".", 1)[-1].lower()
			if any(keyword.lower() in simple_name for keyword in GATE_CLASS_KEYWORDS):
				gates.add(fqcn)
	return sorted(gates), missing_t1


def scan(test_root: Path, quest_ids: list[str], wide_gates: bool = False) -> dict:
	"""Pair each matching test class with the quest ids it mentions, and collect gates. / 统计每个命中类提及的任务 ID，并收集全局门禁类。"""
	pattern = build_quest_pattern(quest_ids)
	class_hits: dict[str, set[str]] = {}
	quest_hits: dict[str, set[str]] = {quest_id: set() for quest_id in quest_ids}
	files = iter_test_files(test_root)
	all_fqcns: list[str] = []
	for path in files:
		source = path.read_text(encoding="utf-8", errors="replace")
		fqcn = derive_fqcn(path, test_root, source)
		all_fqcns.append(fqcn)
		matched = {match.group(1) for match in pattern.finditer(source)}
		if matched:
			class_hits.setdefault(fqcn, set()).update(matched)
			for quest_id in matched:
				quest_hits[quest_id].add(fqcn)
	gate_classes, missing_t1 = collect_gate_classes(all_fqcns, wide_gates)
	return {
		"files_scanned": len(files),
		"class_hits": class_hits,
		"quest_hits": quest_hits,
		"gate_classes": gate_classes,
		"missing_t1_gates": missing_t1,
	}


def build_selector(classes: list[str]) -> str:
	"""Build a Surefire selector; an empty set stays shell-friendly. / 生成 Surefire 选择器；空集合也保持可直接复制。"""
	return "-Dtest=" + ",".join(classes)


def build_report(
	test_root: Path,
	quest_ids: list[str],
	scan_result: dict,
	elapsed_seconds: float,
	wide_gates: bool = False,
) -> dict:
	"""Shape the final report for text or JSON output. / 组织最终报告，供文本或 JSON 输出使用。"""
	class_hits: dict[str, set[str]] = scan_result["class_hits"]
	quest_hits: dict[str, set[str]] = scan_result["quest_hits"]
	affected_classes = sorted(class_hits)
	unmatched_quest_ids = [quest_id for quest_id in quest_ids if not quest_hits[quest_id]]
	gate_classes = list(scan_result["gate_classes"])
	missing_t1_gates = list(scan_result["missing_t1_gates"])
	combined_classes = sorted(set(affected_classes) | set(gate_classes))
	selectors = {
		"affected": build_selector(affected_classes),
		"gates": build_selector(gate_classes),
		"combined": build_selector(combined_classes),
	}
	return {
		"repo_root": str(REPO_ROOT),
		"test_root": str(test_root),
		"quest_ids": quest_ids,
		"scanned_test_classes": scan_result["files_scanned"],
		"affected_classes": affected_classes,
		"class_hits": {
			fqcn: [quest_id for quest_id in quest_ids if quest_id in class_hits[fqcn]]
			for fqcn in affected_classes
		},
		"quest_hits": {
			quest_id: sorted(quest_hits[quest_id])
			for quest_id in quest_ids
		},
		"unmatched_quest_ids": unmatched_quest_ids,
		"gate_classes": gate_classes,
		"gate_mode": "t1+quest-engine-keywords" if wide_gates else "t1-fixed",
		"t1_gate_classes": list(T1_GATE_CLASSES),
		"missing_t1_gates": missing_t1_gates,
		"gate_keywords": list(GATE_CLASS_KEYWORDS),
		"selectors": selectors,
		"selector_lengths": {name: len(selector) for name, selector in selectors.items()},
		"elapsed_seconds": round(elapsed_seconds, 6),
		"performance_limit_seconds": PERFORMANCE_LIMIT_SECONDS,
		"performance_passed": elapsed_seconds <= PERFORMANCE_LIMIT_SECONDS,
	}


def render_text(report: dict) -> str:
	"""Render the human-readable report. / 渲染人类可读报告。"""
	lines: list[str] = []
	lines.append("Aion quest test impact report / Aion 任务测试影响报告")
	lines.append("=" * 72)
	lines.append(f"Repo root            / 仓库根目录 : {report['repo_root']}")
	lines.append(f"Test root            / 测试根目录 : {report['test_root']}")
	lines.append(f"Scanned classes      / 扫描类数   : {report['scanned_test_classes']}")
	lines.append(f"Quest ids            / 任务 ID    : {', '.join(report['quest_ids'])}")
	lines.append("")
	lines.append(f"Affected test classes / 命中测试类 ({len(report['affected_classes'])}):")
	if report["affected_classes"]:
		for fqcn in report["affected_classes"]:
			ids = ", ".join(report["class_hits"][fqcn])
			lines.append(f"  {fqcn} -> {ids}")
	else:
		lines.append("  (none / 无)")
	lines.append("")
	lines.append(f"Unmatched quest ids / 未被任何测试类提名的 ID ({len(report['unmatched_quest_ids'])}):")
	if report["unmatched_quest_ids"]:
		for quest_id in report["unmatched_quest_ids"]:
			lines.append(f"  {quest_id}")
	else:
		lines.append("  (none / 无)")
	lines.append("")
	lines.append(
		f"Global gate classes / 全局门禁类 ({len(report['gate_classes'])}, mode: "
		+ report["gate_mode"]
		+ "):"
	)
	if report["gate_classes"]:
		for fqcn in report["gate_classes"]:
			lines.append(f"  {fqcn}")
	else:
		lines.append("  (none / 无)")
	if report["missing_t1_gates"]:
		lines.append(
			"  WARN unresolved T1 gates / 未解析的 T1 门禁: "
			+ ", ".join(report["missing_t1_gates"])
		)
	lines.append("")
	lines.append("Surefire selectors / Surefire 选择器:")
	lines.append(
		f"  affected / 命中类   : {report['selectors']['affected']}"
		f"  [length={report['selector_lengths']['affected']}]"
	)
	lines.append(
		f"  gates    / 门禁类   : {report['selectors']['gates']}"
		f"  [length={report['selector_lengths']['gates']}]"
	)
	lines.append(
		f"  combined / 合并类   : {report['selectors']['combined']}"
		f"  [length={report['selector_lengths']['combined']}]"
	)
	lines.append("")
	status = "PASS" if report["performance_passed"] else "FAIL"
	lines.append(
		f"Elapsed / 耗时: {report['elapsed_seconds']:.3f}s "
		f"(limit / 上限 {report['performance_limit_seconds']:.3f}s, {status})"
	)
	return "\n".join(lines)


def main(argv: list[str]) -> int:
	"""CLI entry point. / 命令行入口。"""
	quest_ids, args = parse_args(argv)
	started_at = time.perf_counter()
	try:
		scan_result = scan(args.test_root, quest_ids, wide_gates=args.wide_gates)
	except FileNotFoundError as exc:
		print(str(exc), file=sys.stderr)
		return 2
	elapsed_seconds = time.perf_counter() - started_at
	report = build_report(
		args.test_root, quest_ids, scan_result, elapsed_seconds, wide_gates=args.wide_gates
	)
	if args.json:
		print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=False))
	else:
		print(render_text(report))
	if report["missing_t1_gates"]:
		return 3
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
