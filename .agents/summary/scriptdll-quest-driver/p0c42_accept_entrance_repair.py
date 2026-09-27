#!/usr/bin/env python3
"""P0c-42：1323 接取入口修复（登记表外科手术）。

判例（真端 + 客户端双证）：
  真端 `Quest_SimpleTalk.xml` 1323 = `acquired_npc_name=LF2_Lost_JewelBox` + `give_item=ITEM_QUEST_1323A 1`
  + `talk_npc1=Tree_NoMove_Lodas` + `reward_npc_name=Justachys`（接取者是箱子对象 730032）。
  客户端 `QUEST_Q1323.html`：page 1011 `select1`（order 1，按钮 1007，字面「打开盖子看看。」）
  → page 4 `ask_quest_accept`（order 2，按钮 1002 接受 / 1003 拒绝）→ page 1003/1004 结果页。
  退役 XML（git 历史）对照：有 `use-item 182201309 -> SHOW_ASK_QUEST_ACCEPT_WINDOW`（用吊坠开敬接窗）
  与 `TALK_TO_NPC 730032 USE_OBJECT -> give-item`（箱子给吊坠）两形，但**没有** select1 页。

缺口（P0c-42 探针实测）：登记表把接取侧写成 4 条 `Q` 记录（无 NPC 的通用 QUEST_ACTION），
且 `R 1 USE_OBJECT` 丢了下发页 ⇒ 客户端页 **1011 与 4 都从未被下发**（`CLIENT_PAGE_UNREACHED`），
玩家无法打开接取窗 ⇒ 任务无法接取。审计的 owner 匹配规则（`sameDialogOwner`）要求"下发页的路由"
与"页按钮的路由"同 owner，因此接取侧必须绑到箱子（730032），形状逐字取自同族已采纳兄弟行（3001/21136）。

机械：就地替换 4 条 Q 记录为箱子绑定的 R 记录 + 新增 1 条 ASK_QUEST_ACCEPT + 给 R1 补下发页，
随后按文件出现序重编 R seq。所有前置行逐字断言（fail-closed），双副本（main + target/classes）同写。

用法：`python3 -B p0c42_accept_entrance_repair.py`（dry-run）/ `... --apply`。
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
COPIES = [
	REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv",
	REPO / "target/classes/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv",
]
QUEST = "1323"
BOX = "730032"

# 前置行（逐字，fail-closed）：现行 1323 的 4 条 Q + 1 条待补下发页的 R1。
BEFORE = {
	f"{QUEST}\tQ\t4\tQUEST_REFUSE_1\tunaccepted\tunaccepted\t-\t-\tCLOSE\t-":
		f"{QUEST}\tR\t4\t{BOX}\tQUEST_REFUSE_1\tunaccepted\tunaccepted\t-\t-\t"
		"DIALOG:SHOW_QUEST_PAGE:QUEST_REFUSE_1\tRETAIL_MATCH\tQUEST_REFUSE_1=CLIENT\t-",
	f"{QUEST}\tQ\t5\tQUEST_ACCEPT_1\tunaccepted\tstarted\tSTART_ELIGIBLE\t-\t"
	"SYNC:VISIBILITY_REFRESH;DIALOG:SHOW_QUEST_PAGE:QUEST_ACCEPT_1\t-":
		f"{QUEST}\tR\t3\t{BOX}\tQUEST_ACCEPT_1\tunaccepted\tstarted\tSTART_ELIGIBLE\t-\t"
		"SYNC:VISIBILITY_REFRESH;DIALOG:SHOW_QUEST_PAGE:QUEST_ACCEPT_1\tRETAIL_MATCH\t"
		"QUEST_ACCEPT_1=CLIENT\t-",
	f"{QUEST}\tQ\t6\tFINISH_DIALOG\tunaccepted\tunaccepted\t-\t-\tCLOSE\t-":
		f"{QUEST}\tR\t5\t{BOX}\tFINISH_DIALOG\tunaccepted\tunaccepted\t-\t-\tCLOSE\tRETAIL_MATCH\t-\t-",
	f"{QUEST}\tQ\t7\tFINISH_DIALOG\tstarted\tstarted\t-\t-\tCLOSE\t-":
		f"{QUEST}\tR\t6\t{BOX}\tFINISH_DIALOG\tstarted\tstarted\t-\t-\tCLOSE\tRETAIL_MATCH\t-\t-",
	f"{QUEST}\tR\t1\t{BOX}\tUSE_OBJECT\tunaccepted\tunaccepted\t-\tGIVE_ITEM:182201309:1\t-\t"
	"RETAIL_MATCH\t-\t-":
		f"{QUEST}\tR\t1\t{BOX}\tUSE_OBJECT\tunaccepted\tunaccepted\t-\tGIVE_ITEM:182201309:1\t"
		"DIALOG:SHOW_QUEST_PAGE:SELECT1\tRETAIL_MATCH\tSELECT1=CLIENT\t-",
}
# 新增行：箱子页 1011 的按钮 1007（「打开盖子看看」）→ 接取窗（page 4）。
NEW_ROW = (f"{QUEST}\tR\t2\t{BOX}\tASK_QUEST_ACCEPT\tunaccepted\tunaccepted\t-\t-\t"
	"DIALOG:SHOW_QUEST_PAGE:SHOW_ASK_QUEST_ACCEPT_WINDOW\tRETAIL_MATCH\t"
	"SHOW_ASK_QUEST_ACCEPT_WINDOW=GLOBAL\t-")
NEW_AFTER = f"{QUEST}\tR\t1\t{BOX}\tUSE_OBJECT\t"


def main() -> int:
	apply = "--apply" in sys.argv
	baselines = [path.read_bytes() for path in COPIES]
	if any(payload != baselines[0] for payload in baselines[1:]):
		print("ABORT: registry copies diverge — lane storm, retry later")
		return 1
	text = baselines[0].decode("utf-8")
	lines = text.splitlines(keepends=True)

	quest_rows = [line for line in lines if line.split("\t", 1)[0] == QUEST]
	if len(quest_rows) != 20:
		print(f"ABORT: expected 20 rows for {QUEST}, found {len(quest_rows)}")
		return 1
	for before in BEFORE:
		if not any(line.rstrip("\n") == before for line in lines):
			print(f"ABORT: pre-condition row missing or drifted:\n  {before}")
			return 1
	if any(line.rstrip("\n") == NEW_ROW for line in lines):
		print("ABORT: repair already applied")
		return 1

	patched = []
	inserted = 0
	for line in lines:
		stripped = line.rstrip("\n")
		if stripped in BEFORE:
			replacement = BEFORE[stripped]
			patched.append(replacement + ("\n" if line.endswith("\n") else ""))
			if stripped.startswith(NEW_AFTER):
				patched.append(NEW_ROW + ("\n" if line.endswith("\n") else ""))
				inserted += 1
		else:
			patched.append(line)
	if inserted != 1:
		print(f"ABORT: new row anchored on the wrong row (inserted={inserted})")
		return 1

	# R seq 重编（按文件出现序；loader 顺序无关，但保持文件可读且与生成器同口径）。
	seq = 0
	for index, line in enumerate(patched):
		parts = line.rstrip("\n").split("\t")
		if parts[0] == QUEST and parts[1] == "R":
			seq += 1
			parts[2] = str(seq)
			patched[index] = "\t".join(parts) + ("\n" if line.endswith("\n") else "")
	if seq != 14:
		print(f"ABORT: expected 14 R rows for {QUEST} after repair, renumbered {seq}")
		return 1

	import difflib
	opcodes = difflib.SequenceMatcher(None, lines, patched, autojunk=False).get_opcodes()
	replaced = [op for op in opcodes if op[0] == "replace"]
	inserted_ops = [op for op in opcodes if op[0] == "insert"]
	replaced_rows = sum(op[2] - op[1] for op in replaced)
	produced_rows = sum(op[4] - op[3] for op in replaced)
	# 预期：旧侧 13 行改写（4 条 Q→R + R1..R9 段）+ 新侧 14 行（多出 1 条 ASK_QUEST_ACCEPT）。
	if replaced_rows != 13 or produced_rows != 14:
		print(f"ABORT: unexpected edit shape — replaced_rows={replaced_rows} "
			f"replace_blocks={len(replaced)} produced_rows={produced_rows}")
		return 1
	print(f"PLAN: {QUEST} accept side rebound to box {BOX}: 13 rows rewritten "
		f"(4 Q->R + R1 page push + 8 seq renumbers), 1 row inserted (ASK_QUEST_ACCEPT), "
		f"R rows renumbered to {seq}; replace at={[op[1] + 1 for op in replaced]}")
	if not apply:
		print("DRY-RUN: no file written (pass --apply to execute)")
		return 0

	payload = "".join(patched).encode("utf-8")
	for path in COPIES:
		path.write_bytes(payload)
	print(f"APPLIED: {len(COPIES)} copies; run the probe + chain gate next "
		"(fingerprint for 1323 must be re-frozen)")
	return 0


if __name__ == "__main__":
	sys.exit(main())
