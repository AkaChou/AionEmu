#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""P0c-53 侦察（只读）：真端对话名组通道的**可扩域**测量。

P0c-52 落地的组表只收渲染成守备队的 `LDF4_Advance_Village_Guard_*` 家族（生成器里一条
`GROUP_RE` 硬过滤）。本脚本把过滤去掉，测量「DD 表真的引用、且客户端声明为对话名组」的
全部名字及其四源一致情况，用来裁定 `RETAIL_ACQUIRE_NPC_UNRESOLVED` 余下行里有多少是
**组通道未扩域**（而不是名字真缺失）。

四源（与生成器同口径）：客户端 npc 块 `<quest_ai_name>` 的成员 id 集、客户端词典正文
`STR_DIC_E_<名>` 的成员名列表、服务端 npc 模板（成员共享同一 title_id）、遗留生产 XML 的
`NPC_START` id 集（该组名下所有任务合并）。

用法：`python3 -B p0c53_group_scope_census.py [--out <tsv>]`
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(Path(__file__).resolve().parent))

import p0c52_quest_ai_name_groups as gen  # noqa: E402  （复用四源读取函数）

DRIFT = REPO / "src/test/resources/quest/retail-data-driven-drift.tsv"
UNRESOLVED_CODES = ("REJECTED:RETAIL_ACQUIRE_NPC_UNRESOLVED", "REJECTED:RETAIL_REWARD_NPC_UNRESOLVED")


def all_client_groups() -> dict[str, set[int]]:
	"""全部 `quest_ai_name` 声明（去掉 GROUP_RE 过滤，去掉空值）。"""
	out: dict[str, set[int]] = {}
	cur_id = cur = None
	with gen.CLIENT_NPC.open("r", encoding="utf-8", errors="replace") as handle:
		for raw in handle:
			line = raw.strip()
			if line == "<npc_client>":
				cur_id = cur = None
			elif line.startswith("<id>") and cur_id is None:
				cur_id = line[4:-5]
			elif line.startswith("<quest_ai_name>"):
				cur = line[len("<quest_ai_name>"):-len("</quest_ai_name>")].strip()
			elif line == "</npc_client>":
				if cur and cur_id:
					out.setdefault(cur, set()).add(int(cur_id))
				cur_id = cur = None
	return out


def all_client_dic_members() -> dict[str, list[str]]:
	"""全部 `STR_DIC_E_<名>` 正文里的 `STR_DIC_N_<成员>` 引用（去掉 GROUP_RE 过滤）。"""
	out: dict[str, list[str]] = {}
	pending = None
	with gen.CLIENT_DIC.open("r", encoding="utf-8", errors="replace") as handle:
		for raw in handle:
			line = raw.strip()
			hit = re.match(r"<name>STR_DIC_E_([A-Za-z0-9_]+)</name>", line)
			if hit:
				pending = hit.group(1)
				continue
			if pending and line.startswith("<body>"):
				body = line[len("<body>"):-len("</body>")]
				out[pending] = re.findall(r"STR_DIC_N_([A-Za-z0-9_]+)", body)
				pending = None
	return out


def dd_references() -> tuple[dict[str, list[str]], dict[str, list[str]]]:
	"""DD 表引用的接取名 / 交付名 → 任务 id 列表（去别名，含 `NPC_` 前缀原样）。"""
	text = (REPO / "src/main/resources/aion/data/static_data/quest_retail/data_driven_quest.xml"
		).read_text(encoding="utf-8", errors="replace")
	acq: dict[str, list[str]] = {}
	reward: dict[str, list[str]] = {}
	for block in re.findall(r"<quest_data_driven>.*?</quest_data_driven>", text, re.S):
		qid = re.search(r"<id>(\d+)</id>", block)
		if not qid:
			continue
		hit = re.search(r"<category_acquire_>([^<]*)</category_acquire_>", block)
		value = re.search(r"<value0_acquire_>([^<]*)</value0_acquire_>", block)
		rew = re.search(r"<reward_npc_name>([^<]*)</reward_npc_name>", block)
		if value and value.group(1).strip() and (not hit or hit.group(1).lower() == "talk"):
			acq.setdefault(value.group(1).strip(), []).append(qid.group(1))
		if rew and rew.group(1).strip():
			reward.setdefault(rew.group(1).strip(), []).append(qid.group(1))
	return acq, reward


def unresolved_rows() -> dict[str, str]:
	"""当前登记为接取/交付名未解析的任务 → 名字。"""
	out: dict[str, str] = {}
	for line in DRIFT.read_text(encoding="utf-8").splitlines():
		if not line or line.startswith("#"):
			continue
		parts = line.split("\t")
		if len(parts) > 2 and parts[1] in UNRESOLVED_CODES:
			out[parts[0]] = parts[2].split(" -> ")[0]
	return out


def main() -> int:
	parser = argparse.ArgumentParser()
	parser.add_argument("--out", default=None)
	args = parser.parse_args()
	groups = all_client_groups()
	dic = all_client_dic_members()
	templates = gen.server_templates()
	acq, reward = dd_references()
	unresolved = unresolved_rows()

	referenced: dict[str, set[str]] = {}
	for name, ids in acq.items():
		referenced.setdefault(name, set()).update(ids)
	for name, ids in reward.items():
		referenced.setdefault(name, set()).update(ids)

	lines = ["name\tused_by\tdd_quests\tunresolved_quests\tclient_block_ids\tdic_members\t"
		"member_ids\ttitle_ids\tlegacy_npc_start\tverdict\treason"]
	summary: dict[str, int] = {}
	hits: list[tuple[str, str]] = []
	for name in sorted(referenced):
		if name not in groups and name not in dic:
			continue
		members = dic.get(name, [])
		block_ids = sorted(groups.get(name, set()))
		member_ids: list[int] = []
		title_ids: set[str] = set()
		reason = ""
		for member in members:
			hit = templates.get(member)
			if hit is None:
				reason = f"member {member} has no server template"
				continue
			member_ids.append(hit[0])
			title_ids.add(hit[1])
		member_ids.sort()
		quest_ids = sorted(referenced[name])
		legacy = sorted(gen.legacy_npc_start(quest_ids))
		if not reason:
			if not members:
				reason = "no dictionary member list"
			elif block_ids != member_ids:
				reason = f"client block ids {block_ids} != dic member ids {member_ids}"
			elif len(title_ids) != 1:
				reason = f"members span title_ids {sorted(title_ids)}"
			elif legacy and legacy != member_ids:
				reason = f"legacy NPC_START {legacy} != dic member ids {member_ids}"
		verdict = "PASS" if not reason else "FAIL"
		summary[verdict] = summary.get(verdict, 0) + 1
		unres = [q for q in quest_ids if q in unresolved and unresolved[q] == name]
		if verdict == "PASS" and unres:
			hits.append((name, ",".join(unres)))
		used = []
		if name in acq:
			used.append("acquire")
		if name in reward:
			used.append("reward")
		lines.append("\t".join([
			name, "+".join(used), ",".join(quest_ids), ",".join(unres),
			",".join(str(i) for i in block_ids), ",".join(members),
			",".join(str(i) for i in member_ids), ",".join(sorted(title_ids)),
			",".join(str(i) for i in legacy), verdict, reason]))
	output = "\n".join(lines) + "\n"
	if args.out:
		Path(args.out).write_text(output, encoding="utf-8")
		print(f"wrote {args.out} rows={len(lines) - 1}")
	print("declared group names:", len(groups), "| dic names:", len(dic))
	print("dd-referenced names:", len(referenced), "| table candidates:", len(lines) - 1)
	print("verdict histogram:", summary)
	print("unresolved rows unlockable by widening:", len(hits))
	for name, quests in hits:
		print(f"   {name} -> {quests}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
