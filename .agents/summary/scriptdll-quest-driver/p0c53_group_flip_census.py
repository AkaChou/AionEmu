#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-53：组表扩域采纳批的逐行裁定表（只读生成证据，不写生产）。

对每个"由 REJECTED 变 ADOPTED"（以及被改写拒绝码）的 DD 行，找出它**靠哪个对话名组**过关，
并附上该组的四源证据（客户端块 id / 成员名 / 成员 id / title_id / 遗留接取流），供报告与
退役清单使用。

用法：`python3 -B p0c53_group_flip_census.py [--out <tsv>]`
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(Path(__file__).resolve().parent))

import p0c52_quest_ai_name_groups as gen  # noqa: E402

DRIFT = REPO / "src/test/resources/quest/retail-data-driven-drift.tsv"
DRIFT_KEYS = ("ADOPTED", "REJECTED:")


def classification(path: Path) -> dict[str, str]:
	out: dict[str, str] = {}
	for line in path.read_text(encoding="utf-8").splitlines():
		if not line or line.startswith("#"):
			continue
		parts = line.split("\t")
		if len(parts) > 1:
			out[parts[0]] = parts[1]
	return out


def group_table() -> dict[str, list[str]]:
	out: dict[str, list[str]] = {}
	text = (REPO / "src/main/resources/aion/data/static_data/quest_retail/retail-quest-ai-name-groups.tsv"
		).read_text(encoding="utf-8")
	for line in text.splitlines():
		if not line or line.startswith("#"):
			continue
		name, members = line.split("\t")
		out[name] = members.split(",")
	return out


def dd_fields() -> dict[str, tuple[str, str, str, list[str]]]:
	"""quest id -> (acquire category, acquire param, reward name, step categories)."""
	out: dict[str, tuple[str, str, str, list[str]]] = {}
	text = (REPO / "src/main/resources/aion/data/static_data/quest_retail/data_driven_quest.xml"
		).read_text(encoding="utf-8", errors="replace")
	for block in re.findall(r"<quest_data_driven>.*?</quest_data_driven>", text, re.S):
		qid = re.search(r"<id>(\d+)</id>", block)
		if not qid:
			continue
		cat = re.search(r"<category_acquire_>([^<]*)</category_acquire_>", block)
		value = re.search(r"<value0_acquire_>([^<]*)</value0_acquire_>", block)
		rew = re.search(r"<reward_npc_name>([^<]*)</reward_npc_name>", block)
		categories = re.findall(r"<category_progress_>([^<]*)</category_progress_>", block)
		out[qid.group(1)] = (cat.group(1) if cat else "", (value.group(1) if value else "").strip(),
			(rew.group(1) if rew else "").strip(), [c.lower() for c in categories])
	return out


def main() -> int:
	parser = argparse.ArgumentParser()
	parser.add_argument("--dump", required=True, help="post-change classification dump (equivOut)")
	parser.add_argument("--out", default=None)
	args = parser.parse_args()
	baseline = {line.split("\t")[0]: line.split("\t")[1]
		for line in DRIFT.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")}
	post = classification(Path(args.dump))
	groups = group_table()
	blocks = gen.client_quest_ai_blocks()
	templates = gen.server_templates()
	fields = dd_fields()

	rows = []
	for quest_id in sorted(post, key=int):
		now, before = post[quest_id], baseline.get(quest_id, "")
		if now == before:
			continue
		cat, acquire, reward, steps = fields.get(quest_id, ("", "", "", []))
		hits = []
		for name in (acquire, reward):
			if name in groups:
				hits.append(("acquire" if name == acquire else "reward", name))
		# 组名也可能出现在进度步字段上（真端把采集/交付对象写在同一列）。
		detail = []
		for side, name in hits:
			members = groups[name]
			ids = sorted(templates[member][0] for member in members)
			accept, flow = gen.legacy_dialogs([quest_id])
			detail.append(f"{side}:{name}")
			rows.append("\t".join([
				quest_id, before, now, side, name, cat, ",".join(steps),
				",".join(str(i) for i in sorted(blocks.get(name, set()))),
				",".join(members), ",".join(str(i) for i in ids),
				",".join(sorted({templates[m][1] for m in members})),
				",".join(str(i) for i in accept), ",".join(str(i) for i in flow)]))
		if not hits:
			rows.append("\t".join([quest_id, before, now, "none", "", cat, ",".join(steps),
				"", "", "", "", "", ""]))
	header = ("quest_id\tbefore\tafter\tside\tgroup\tacquire_category\tsteps\tclient_block_ids\t"
		"members\tmember_ids\ttitle_ids\tlegacy_accept\tlegacy_flow")
	output = header + "\n" + "\n".join(rows) + "\n"
	if args.out:
		Path(args.out).write_text(output, encoding="utf-8")
		print(f"wrote {args.out} rows={len(rows)}")
	transitions: dict[str, int] = {}
	for quest_id in sorted(post, key=int):
		before, now = baseline.get(quest_id, ""), post[quest_id]
		if before != now:
			key = f"{before or '-'} -> {now}"
			transitions[key] = transitions.get(key, 0) + 1
	for key in sorted(transitions, key=lambda k: -transitions[k]):
		print(f"  {transitions[key]:3d}  {key}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
