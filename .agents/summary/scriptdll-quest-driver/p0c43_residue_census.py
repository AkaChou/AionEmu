#!/usr/bin/env python3
"""P0c-43 残留普查：`XML_NPC_AXIS`（报告/完成流绑错 NPC + 阶梯缺失）在全登记表里还剩几行。

**判据必须是投影式的，不能按节点标签**：登记表的阶段节点标签不统一（判例 24123 用 `s1/s2`，
1394 用 `v1/v2`，1537 用 `k1..k3`，其他族还有 `steps1`/`equipped96`/`reward30`/`instance95`），
按 `s\\d+` 匹配会把完整阶梯误报成缺失（本文档首版即犯此错，stdout 末行给出误报面复核）。

判据（只认客户端背书的缺陷，逐任务裁定依据，不作批量套用依据）：
  轴 1 `REPORT_NPC_MISMATCH` = 指向 reward 节点的 R 行所绑 NPC 集（或 `B NPC_REPORT` 的 npc）
       不含真端 `reward_npc_name` 解析 id ⇒ 报告/完成流绑错 NPC（真端声明报告在别人身上）；
  轴 2 `LADDER_MISSING` = 客户端任务书行数 == K+1 且 K>1（**客户端背书多行**）而登记表 START 行的
       var0 投影不足 K 段（QE-051 投影判据；单行任务书不构成缺陷）。
其余事实列只作留证。输出 `p0c43-residue-census.tsv` + stdout 汇总。只读。
"""
from __future__ import annotations

import collections
import re
from pathlib import Path

TOPIC = Path(__file__).resolve().parent
REPO = TOPIC.parents[2]
REGISTRY = REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_client_talk_chain_steps.tsv"
RETAIL = REPO / "src/main/resources/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml"
NAME_INDEX = TOPIC / "npc_name_index.tsv"
SUMMARY = REPO / "src/main/resources/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv"
OUT = TOPIC / "p0c43-residue-census.tsv"


def read_kv_ints(path: Path) -> dict[int, int]:
	out = {}
	for line in path.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			continue
		parts = line.split("\t")
		out[int(parts[0])] = int(parts[1])
	return out


def main() -> int:
	npc_ids = {}
	for line in NAME_INDEX.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or line.startswith("name_desc") or not line.strip():
			continue
		parts = line.split("\t")
		npc_ids[parts[0]] = parts[1].split("|")
	retail = {int(m.group(1)): m.group(2) for m in re.finditer(
		r'<id id="(\d+)">(.*?)</id>', RETAIL.read_text(encoding="utf-8"), re.S)}
	summary_rows = read_kv_ints(SUMMARY)

	blocks = collections.defaultdict(list)
	report_block_npc = {}
	nodes = collections.defaultdict(set)
	report_side_npc = collections.defaultdict(set)
	block_npcs = collections.defaultdict(set)
	acquired_npc = {}
	# 报告侧动作（真端 reward NPC 持有者）：交付推进 / 门控 / 交付消费。阶段推进动作
	# （SETPRO*/USE_OBJECT/续页按钮）不在此列——阶段 NPC 持有"推进到 reward"的路由是合法形状
	# （判例 4052：第三段 talk NPC 的 SETPRO 直接 target=reward）。
	# 注意：`QUEST_SELECT` 是**每个链上 NPC 的通用对话事件**（含接取 NPC），不算报告侧动作；
	# 报告/交付侧动作只有交付推进、门控与交付消费三类。
	REPORT_ACTIONS = {"SELECT_QUEST_REWARD", "SET_SUCCEED",
		"CHECK_USER_HAS_QUEST_ITEM", "CHECK_USER_HAS_QUEST_ITEM_SIMPLE"}
	for line in REGISTRY.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			continue
		row = line.split("\t")
		qid = int(row[0])
		blocks[qid].append(row)
		if row[1] == "B":
			if row[2] == "NPC_REPORT":
				report_block_npc[qid] = row[3]
			elif row[2] == "NPC_START":
				acquired_npc[qid] = row[3]
			block_npcs[qid].add(row[3])
		elif row[1] == "N":
			# N 记录的 var0 列可能是 '-'（无进度投影的节点）——按 0 计，不参与阶梯投影。
			var0 = int(row[4]) if row[4].isdigit() else 0
			nodes[qid].add((row[2], row[3], var0))
		elif row[1] == "R":
			block_npcs[qid].add(row[3])
			if row[4] in REPORT_ACTIONS:
				report_side_npc[qid].add(row[3])
		elif row[1] in ("I",):
			block_npcs[qid].add(row[2])

	rows_out = []
	multi_stage = []
	for qid in sorted(set(nodes) | set(report_block_npc)):
		body = retail.get(qid, "")
		if "<talk_npc1>" not in body or "<reward_npc_name>" not in body:
			continue
		reward_name = re.search(r"<reward_npc_name>([^<]*)</reward_npc_name>", body).group(1).strip()
		ids = npc_ids.get(reward_name, [])
		reward_id = ids[0] if len(ids) == 1 and ids[0].isdigit() else None
		k_stages = len([k for k in range(1, 64) if "<talk_npc%d>" % k in body])
		# 投影式阶梯长度（标签无关，QE-051：reward 与末段 sK 同 var0 ⇒ 末段 var0 可落在 REWARD 节点）。
		ladder = sorted({v for _, status, v in nodes.get(qid, set())
			if (status == "START" and v >= 1) or status == "REWARD"})
		journal = summary_rows.get(qid, -1)
		bound = set(report_side_npc.get(qid, set()))
		flags = []
		if reward_id:
			if report_block_npc.get(qid) and report_block_npc[qid] != reward_id:
				flags.append("REPORT_BLOCK_NPC:%s!=真端%s" % (report_block_npc[qid], reward_id))
			if reward_id not in block_npcs.get(qid, set()):
				# 判例 24123 形：真端声明的报告 NPC 在块内**完全未接线**（动作名无关判据）。
				flags.append("REWARD_NPC_UNWIRED:真端%s 不在块内 NPC 集%s(接取%s)"
					% (reward_id, sorted(block_npcs.get(qid, set())), acquired_npc.get(qid, "-")))
			elif bound and len(bound) > 1:
				flags.append("REPORT_SIDE_MIXED:报告侧动作绑%s⊃真端%s" % (sorted(bound), reward_id))
		if journal == k_stages + 1 and k_stages > 1 and len(ladder) < k_stages:
			flags.append("LADDER_MISSING:投影%s<K=%d(任务书%d行)" % (ladder or "[]", k_stages, journal))
		if flags:
			rows_out.append((qid, k_stages, reward_name, reward_id or "?", sorted(bound),
				ladder, journal, ";".join(flags)))
		if k_stages > 1:
			multi_stage.append((qid, k_stages, ladder))

	OUT.write_text(
		"# P0c-43 残留普查：登记表内真端多阶段行中，报告 NPC 错绑或阶梯投影缺失的行（只认客户端背书）\n"
		"# 判据 报告侧 NPC 轴 = B NPC_REPORT ≠ 真端 reward_npc_name，或无块且报告侧动作全绑他人；\n"
		"#      LADDER_MISSING = 客户端任务书行数 == K+1 且 K>1 而 START 行 var0 投影 < K（QE-051）\n"
		"# quest_id\tK\tretail_reward_npc\treward_id\tregistry_bound_npcs\tladder_projection"
		"\tclient_rows\tflags\n"
		+ "\n".join("\t".join(str(c) for c in row) for row in rows_out) + ("\n" if rows_out else ""),
		encoding="utf-8")
	mismatch = [r[0] for r in rows_out if "REWARD_NPC_UNWIRED" in r[7]]
	ladder_missing = [r[0] for r in rows_out if "LADDER_MISSING" in r[7]]
	both = [r[0] for r in rows_out if "REPORT_" in r[7] and "LADDER_MISSING" in r[7]]
	print("登记表内真端多阶段行 %d；残留行 %d" % (len(multi_stage), len(rows_out)))
	print("真端报告 NPC 未接线 %d 行 %s" % (len(mismatch), mismatch))
	print("LADDER_MISSING %d 行 %s" % (len(ladder_missing), ladder_missing))
	print("同签名（报告错绑 + 客户端背书的阶梯缺失）%d 行 %s" % (len(both), both))
	print("OVERFLAG 复核：K>1 的 %d 行里投影阶梯完整（>=K 段）者 %d 行 ⇒ 按 `s\\d+` 标签判据的误报面"
		% (len(multi_stage), sum(1 for _, k, l in multi_stage if len(l) >= k)))
	print("24123 是否仍在残留表：%s" % ("是（回归！）" if 24123 in {r[0] for r in rows_out} else "否"))
	print("输出 -> %s" % OUT)
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
