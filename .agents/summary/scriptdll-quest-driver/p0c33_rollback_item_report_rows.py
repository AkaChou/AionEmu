#!/usr/bin/env python3
"""P0c-33：24202/80320 回退到 XML（npc-item-report 门通道缺失，窗口合同门两行新指纹）。

证据链（本切片实测）：
  * 客户端 HTML 声明检查按钮：24202 select5(2375) HACTION_CHECK_USER_HAS_QUEST_ITEM_SIMPLE(20002)；
    80320 select5(2375) HACTION_CHECK_USER_HAS_QUEST_ITEM(39)。
  * 遗留 XML 以 `<npc-item-report source="started" target="reward" item-id=... required=...>` 表达该门；
    XML 编译器展开为 39/20002 成功/失败对。
  * 链登记表（build_quest_client_talk_chain_steps.py 转写）**未转写**该元素，链编译因此无门路由：
    真端定义 started 态只有 QUEST_SELECT/10255/FINISH_DIALOG。
  * 后果：QuestClientContractGateTest 新增两条 BUTTON_WITHOUT_ROUTE（页 2375 的检查按钮无路由），
    且在 HEAD 版 XML 定义上跑同一审计为 0 条（探针 RetailXmlEraContractProbeTest 直证）。

回退范围（只两行）：清单四副本（whipsaw 守卫）→ XML ×2 恢复（main + target/classes）→
目录行回插（main + target/classes，保持 id 序）→ 链 IR 指纹删两行（main + target 副本）。
drift 登记行在回退后由活体分类重算补齐（本脚本只改清单/目录/XML/指纹）。
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]

MANIFEST_MAIN = REPO / "src/main/resources/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
MANIFEST_TEST = REPO / "src/test/resources/quest/retail-xml-retention.tsv"
MANIFEST_TARGET_MAIN = REPO / "target/classes/aion/data/static_data/quest_retail/retail-xml-retention.tsv"
MANIFEST_TARGET_TEST = REPO / "target/test-classes/quest/retail-xml-retention.tsv"
CATALOG_MAIN = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quest_definition_catalog.xml"
CATALOG_TARGET = REPO / "target/classes/aion/data/static_data/quest/definitions/quest_definition_catalog.xml"
QUESTS_MAIN = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
QUESTS_TARGET = REPO / "target/classes/aion/data/static_data/quest/definitions/quests"
FINGERPRINTS = REPO / "src/test/resources/quest/retail-simple-talk-chain-ir-fingerprints.tsv"
FINGERPRINTS_TARGET = REPO / "target/test-classes/quest/retail-simple-talk-chain-ir-fingerprints.tsv"

ROLLBACK_IDS = ("24202", "80320")
RETIRED_ROW_MARKER = "\tRETAIL_TABLE\tSimpleTalk\tOK\tretired-xml-in-git-history p0c32-accept-entrance-decisions.tsv"
XML_ROW = ("{qid}\tXML_RETENTION\tSimpleTalk\tSEMANTIC_GAP:RETAIL_TALK_CHAIN\t"
	"p0c33-item-report-rollback-decisions.tsv basis=ITEM_CHECK_GATE_NOT_TRANSCRIBED")
CATALOG_ROW = ('  <definition id="{qid}" '
	'resource="aion/data/static_data/quest/definitions/quests/{qid}.xml" mode="EXECUTABLE" />\n')


def abort(message: str) -> int:
	print(f"ABORT: {message}")
	return 1


def main() -> int:
	baselines = [path.read_bytes() for path in
		(MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST)]
	if any(bytes_ != baselines[0] for bytes_ in baselines[1:]):
		return abort("manifest copies diverge — lane storm, retry later")

	lines = baselines[0].decode("utf-8").splitlines(keepends=True)
	matched = []
	for index, line in enumerate(lines):
		qid = line.split("\t", 1)[0]
		if qid in ROLLBACK_IDS:
			if RETIRED_ROW_MARKER not in line:
				return abort(f"quest {qid} is not the p0c32 retired row: {line.strip()}")
			lines[index] = XML_ROW.format(qid=qid) + "\n"
			matched.append(qid)
	if sorted(matched) != sorted(ROLLBACK_IDS):
		return abort(f"expected rows {sorted(ROLLBACK_IDS)}, matched {sorted(matched)}")
	patched = "".join(lines).encode("utf-8")
	changed = sum(1 for old, new in zip(baselines[0].decode("utf-8").splitlines(),
		patched.decode("utf-8").splitlines()) if old != new)
	if changed != len(ROLLBACK_IDS):
		return abort(f"whipsaw guard — {changed} lines changed, expected {len(ROLLBACK_IDS)}")

	# XML 恢复：以 HEAD 内容回写 main + target 两份副本（回退必须同时回拷 target，P0c-17 判例）。
	# Restore the XMLs from HEAD into both copies; a rollback must refill target too.
	restored = {}
	for qid in ROLLBACK_IDS:
		path = f"src/main/resources/aion/data/static_data/quest/definitions/quests/{qid}.xml"
		text = subprocess.run(["git", "show", f"HEAD:{path}"], cwd=REPO, check=True,
			capture_output=True).stdout.decode("utf-8")
		if "<quest-definition" not in text:
			return abort(f"unexpected HEAD content for quest {qid}")
		restored[qid] = text
		for quests_dir in (QUESTS_MAIN, QUESTS_TARGET):
			existing = quests_dir / f"{qid}.xml"
			if existing.exists() and existing.read_text(encoding="utf-8") != text:
				return abort(f"working copy {existing} exists with different content")
			existing.write_text(text, encoding="utf-8")

	# 目录行回插（main + target，保持 id 升序位置）。
	# Re-insert the catalog rows in id order in both copies.
	for catalog in (CATALOG_MAIN, CATALOG_TARGET):
		text = catalog.read_text(encoding="utf-8")
		entries = list(re.finditer(r'  <definition id="(\d+)"[^\n]*\n', text))
		for qid in ROLLBACK_IDS:
			row = CATALOG_ROW.format(qid=qid)
			if row in text:
				return abort(f"catalog already contains {qid} in {catalog}")
			insert_at = None
			for entry in entries:
				if int(entry.group(1)) > int(qid):
					insert_at = entry.start()
					break
			if insert_at is None:
				return abort(f"no insertion point for {qid} in {catalog}")
			text = text[:insert_at] + row + text[insert_at:]
			entries = list(re.finditer(r'  <definition id="(\d+)"[^\n]*\n', text))
		catalog.write_text(text, encoding="utf-8")

	# 链 IR 指纹删两行（main + target 副本必须逐字节一致）。
	# Drop the two frozen fingerprints from both copies.
	fp_baselines = [path.read_bytes() for path in (FINGERPRINTS, FINGERPRINTS_TARGET)]
	if fp_baselines[0] != fp_baselines[1]:
		return abort("fingerprint copies diverge")
	fp_lines = fp_baselines[0].decode("utf-8").splitlines(keepends=True)
	kept = [line for line in fp_lines
		if line.split("\t", 1)[0] not in ROLLBACK_IDS or line.startswith("#")]
	if len(fp_lines) - len(kept) != len(ROLLBACK_IDS):
		return abort(f"expected {len(ROLLBACK_IDS)} fingerprint lines, "
			f"dropped {len(fp_lines) - len(kept)}")
	fp_patched = "".join(kept).encode("utf-8")
	for path in (FINGERPRINTS, FINGERPRINTS_TARGET):
		path.write_bytes(fp_patched)

	for path in (MANIFEST_MAIN, MANIFEST_TEST, MANIFEST_TARGET_MAIN, MANIFEST_TARGET_TEST):
		path.write_bytes(patched)
	print(f"rollback={ROLLBACK_IDS} manifest×4 catalog×2 xml×2 fingerprints×2; "
		"run verify_retirement.py then the family/contract gates")
	return 0


if __name__ == "__main__":
	sys.exit(main())
