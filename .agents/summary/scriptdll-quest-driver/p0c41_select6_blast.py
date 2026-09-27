#!/usr/bin/env python3
"""P0c-41：select6 关闭出口闭包的爆炸半径表（修复前/后两轮探针日志的机器对拍）。

输入：修复前日志 + 修复后日志（P0c41Select6CloseExitProbeTest 输出）。
输出：p0c41-select6-close-exit-blast.tsv（逐 quest 的 scoped 缺口 + 前后计数 + 是否链式登记）。

判据：修复后 scoped 集合必须等于修复前集合减去「闭合集合」；闭合集合必须逐元素等于
链式登记行（`quest_client_talk_chain_steps.tsv`）与 scoped 的交集——否则说明补丁溢出到别的族。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
CHAIN_STEPS = REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv"
OUT = HERE / "p0c41-select6-close-exit-blast.tsv"

ROW = re.compile(r"^\s*SCOPED quest=(\d+) source=(\S+) npc=(\d+) dialogId=(\S+) clientHasPage=(\w+)")
COUNT = re.compile(r"SELECT6 pushers=(\d+) missing close exit=(\d+) missing WITH select6 exit=(\d+)")
STATUSES = re.compile(r"P0C41 statuses=(\{.*?\})")
FATAL = re.compile(r"^FATAL count=(\d+)$", re.M)


def parse(path: Path):
	rows, counts, statuses, fatal = [], None, None, None
	text = path.read_text(encoding="utf-8")
	for line in text.splitlines():
		m = ROW.match(line)
		if m:
			rows.append(tuple(m.groups()))
		m = COUNT.search(line)
		if m:
			counts = tuple(int(x) for x in m.groups())
		m = STATUSES.search(line)
		if m:
			statuses = m.group(1)
		m = FATAL.search(line)
		if m:
			fatal = int(m.group(1))
	if counts is None or statuses is None or fatal is None:
		raise SystemExit(f"ABORT: log incomplete: {path}")
	return rows, counts, statuses, fatal


def main() -> int:
	if len(sys.argv) != 3:
		print("usage: p0c41_select6_blast.py <before.log> <after.log>")
		return 2
	before, bcounts, bstatus, bfatal = parse(Path(sys.argv[1]))
	after, acounts, astatus, afatal = parse(Path(sys.argv[2]))

	chain_ids = {line.split("\t", 1)[0] for line in CHAIN_STEPS.read_text(encoding="utf-8").splitlines()
		if line and not line.startswith("#")}
	before_set, after_set = set(before), set(after)
	closed = sorted(before_set - after_set)
	introduced = sorted(after_set - before_set)
	chain_closed = sorted({row[0] for row in closed if row[0] in chain_ids})
	other_closed = sorted({row[0] for row in closed if row[0] not in chain_ids})

	lines = [
		"# P0c-41 select6 关闭出口闭包的爆炸半径（修复前/后探针日志机器对拍）",
		"# 口径：SCOPED = 该任务在客户端出口登记表有 SELECT6，且 IR 里某节点下发 2716 失败页但没有",
		"#        同节点、同 NPC 的 FINISH_DIALOG(1008) 关闭路由（探针按每 push 一行打印；本表按",
		"#        (quest, source, npc, dialogId) 去重，故去重行数 < missing_with_select6 的 push 计数）。",
		f"# before: pushers={bcounts[0]} missing_close={bcounts[1]} missing_with_select6={bcounts[2]} "
			f"statuses={bstatus} fatal={bfatal}",
		f"# after : pushers={acounts[0]} missing_close={acounts[1]} missing_with_select6={acounts[2]} "
			f"statuses={astatus} fatal={afatal}",
		f"# 闭包集合（修复后消失的 scoped 行）：{len(closed)} 键 / 涉及 {len({r[0] for r in closed})} 任务；"
			f"其中链式登记行 {chain_closed}，非链式行 {other_closed}",
		f"# 新增 scoped 行（溢出守卫，必须为空）：{introduced}",
		"kind\tquest_id\tsource\tnpc\tdialogId\tclient_has_page\tchain_registered",
	]
	for row in sorted(before_set):
		verdict = "CLOSED" if row not in after_set else "STILL_OPEN"
		lines.append("\t".join([verdict, row[0], row[1], row[2], row[3], row[4],
			"yes" if row[0] in chain_ids else "no"]))
	OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")

	ok = (not introduced and set(chain_closed) == {row[0] for row in closed}
		and len(closed) == len(before_set) - len(after_set) and afatal == 0)
	print(f"before scoped={len(before_set)} after scoped={len(after_set)} closed={len(closed)} "
		f"introduced={len(introduced)} chain_closed={chain_closed} other_closed={other_closed}")
	print(f"fatal {bfatal} -> {afatal}; wrote {OUT}")
	print("OK: closure set == chain rows, no overflow" if ok else "CHECK: unexpected closure set")
	return 0 if ok else 1


if __name__ == "__main__":
	sys.exit(main())
