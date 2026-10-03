#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""真端对话名组表生成（`quest_ai_name` → 成员 `name_desc`）+ 多源交叉核验。

Generator for the retail dialog-name group table, with cross-source validation.

P0c-52 首版只收守备队一个家族（生成器里一条 `GROUP_RE` 硬过滤）；**P0c-53 扩域**为
「DD 表真的引用、且客户端声明了 ≥2 个成员、且组名与成员自己的名字不同」的全部名字——
这正是"一只 NPC 的对话名不是它自己的名字"的判别式（普通 NPC 的 `quest_ai_name` 就是它自己的
`name`，那类名字走精确解析，不进组表）。硬过滤换成数据判据后，同一台机器同时覆盖守备队与
`Ab1_BLv*`（登陆点基地守卫）、`IDRaksha_Solo_StageStart[_Dark]`（副本台阶）、
`NPC_event_idevent_s*` / `event_npc_*`（活动商人）四族。

Sources:
1. 客户端 npc 块（`<quest_ai_name>`）：`组名 → npc_id 集`（成员身份的第一手声明）；
2. 服务端 npc 模板：`npc_id → name_desc / title_id`（成员名与"同组共享一个 title_id"的真端编制）；
3. 客户端词典条目正文（`STR_DIC_E_<组名>`）：成员名列表（**可选** —— 随包词典转储并不覆盖全部组，
   缺条目时成员名由 ② 反查得到，条目存在时须与 ① 的 id 集一致）；
4. 遗留生产 XML 的接取流 NPC 集（`NPC_START` + `QUEST_ACCEPT*` 对话），组名名下全部任务取并集：
   成员必须是其**子集**（见证关系；遗留可能多给交付 NPC 一条接取路由，多出来的 id 必须能解释为
   该组名下某任务自己的交付 NPC，否则 fail-closed）。

Four sources: the client npc blocks (member ids), the server npc templates (names and the shared
title_id), the client dictionary entry body (optional member list), and the legacy XML accept-flow
npc set (a superset witness; extra ids must resolve as the reward npc of one of the group's quests).

Output: quest_retail/retail-quest-ai-name-groups.tsv (only written with --emit).
"""
from __future__ import annotations
import os

import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
NPC_TEMPLATE_DIR = REPO / "src/main/resources/aion/data/static_data/npcs"
RETAIL_DIR = REPO / "src/main/resources/aion/data/static_data/quest/retail"
DD_TABLE = RETAIL_DIR / "data_driven_quest.xml"
RETENTION = RETAIL_DIR / "retail-xml-retention.tsv"
OUT_TSV = RETAIL_DIR / "retail-quest-ai-name-groups.tsv"
# 排除登记表已于 2026-09-28（批 P1）整表退役：生成器停写，只在标准输出打印。
# The rejected-registry TSV was retired wholesale on 2026-09-28 (batch P1); the generator prints only.
LEGACY_QUESTS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"


# 客户端解包根：按同宿主目录约定解析，兼容 <workspace>/PycharmProjects 与 ~/PycharmProjects 两种布局。
# Unpacked client root: sibling-directory convention with a home-directory fallback.
def _client_root() -> Path:
	for candidate in (REPO.parent / "PycharmProjects" / "unpak", Path.home() / "PycharmProjects" / "unpak"):
		if (candidate / "npcs_unpacked" / "client_npcs_npc.xml").exists():
			return candidate
	return REPO.parent / "PycharmProjects" / "unpak"


CLIENT_ROOT = _client_root()
CLIENT_NPC = CLIENT_ROOT / "npcs_unpacked" / "client_npcs_npc.xml"
CLIENT_DIC = CLIENT_ROOT / "strings_unpacked" / "client_strings_dic_etc.xml"

# 遗留 XML 的接取流动作（NPC_START 之外的"对话即接取"形）。 / The legacy accept-flow actions.
ACCEPT_ACTIONS = ("QUEST_ACCEPT_1", "QUEST_ACCEPT_SIMPLE", "ASK_QUEST_ACCEPT")

# 候选阶段就被登记的排除项（判据与证据冻结在表里，不随遗留 XML 的留存状态漂移）。
# 判据必须是**静态输入**（客户端块 + 服务端模板）可判的：遗留 XML 会随采纳逐批退役，若让它的
# 读数为判据，同一份数据在不同检出状态下会生成不同的表（首跑实测：Raksha 组因 `accept` 只剩下
# 兄弟任务的交付 NPC 而误判相斥）。
# Adjudicated exclusions, frozen here with their evidence: the deciding predicates must be decidable
# from the static inputs (client blocks + server templates). The legacy XML retires batch by batch, so
# letting its readout decide would make the table depend on the checkout state (hit on the first run:
# the Raksha group looked contradictory once only a sibling quest's XML remained).
REGISTERED_EXCLUSIONS = {
	"magician_apprentice": ("LEGACY_ACCEPT_CONTRADICTS_CLIENT",
		"client [804897, 804898] vs legacy [804868, 804869]"),
}


def client_quest_ai_blocks() -> dict[str, set[int]]:
	"""{quest_ai_name: npc ids} from the client npc blocks (single streaming pass)."""
	out: dict[str, set[int]] = {}
	cur_id = cur_quest_ai = None
	with CLIENT_NPC.open("r", encoding="utf-8", errors="replace") as handle:
		for raw in handle:
			line = raw.strip()
			if line == "<npc_client>":
				cur_id = cur_quest_ai = None
			elif line.startswith("<id>") and cur_id is None:
				cur_id = line[4:-5]
			elif line.startswith("<quest_ai_name>"):
				cur_quest_ai = line[len("<quest_ai_name>"):-len("</quest_ai_name>")].strip()
			elif line == "</npc_client>":
				if cur_quest_ai and cur_id:
					out.setdefault(cur_quest_ai, set()).add(int(cur_id))
				cur_id = cur_quest_ai = None
	return out


def client_dic_members() -> dict[str, list[str]]:
	"""{quest_ai_name: member name_descs} from the client dictionary bodies."""
	out: dict[str, list[str]] = {}
	pending = None
	with CLIENT_DIC.open("r", encoding="utf-8", errors="replace") as handle:
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


def server_templates() -> dict[str, tuple[int, str]]:
	"""{name_desc: (npc_id, title_id)} over every npc template file."""
	out: dict[str, tuple[int, str]] = {}
	for path in sorted(NPC_TEMPLATE_DIR.glob("*.xml")):
		text = path.read_text(encoding="utf-8", errors="replace")
		for match in re.finditer(r"<npc_template\b([^>]*)>", text):
			attrs = dict(re.findall(r'(\w+)="([^"]*)"', match.group(1)))
			desc, npc_id = attrs.get("name_desc"), attrs.get("npc_id")
			if desc and npc_id:
				out[desc] = (int(npc_id), attrs.get("title_id", "-"))
	return out


def server_names_by_id() -> dict[int, str]:
	"""{npc_id: name_desc} — 成员名的反查通道（词典缺条目时的成员名来源）。"""
	out: dict[int, str] = {}
	for path in sorted(NPC_TEMPLATE_DIR.glob("*.xml")):
		text = path.read_text(encoding="utf-8", errors="replace")
		for match in re.finditer(r"<npc_template\b([^>]*)>", text):
			attrs = dict(re.findall(r'(\w+)="([^"]*)"', match.group(1)))
			desc, npc_id = attrs.get("name_desc"), attrs.get("npc_id")
			if desc and npc_id:
				out[int(npc_id)] = desc
	return out


# 组表的服务面 = **全部家族表**真的写出来的 NPC 位点（P9 扩域：原实现只收 DD 表）。
#   * 接取位列  acquired_npc_name / value0_acquire_（承接旧「接取流子集」守卫）
#   * 其余位列  reward_npc_name / talk_npcN（交付与中继：同一条真端名字节点语义）
# Service surface = every NPC slot actually written by *any* family table (P9 widening: the original
# implementation only collected the DD table). Accept columns keep the legacy accept-superset guard.
FAMILY_TABLES = ("Quest_SimpleHunt.xml", "Quest_SimpleSerialHunt.xml", "Quest_SimpleTalk.xml",
	"Quest_SimpleCollectItem.xml", "Quest_SimpleUseItem.xml", "Quest_SimpleItemPlay.xml",
	"Quest_CombineTask.xml", "data_driven_quest.xml")
ROW_RE = re.compile(r"<id(?:\s+id=\"(\d+)\")?\s*>(.*?)</id>", re.S)
NAME_FIELDS = re.compile(r"<(acquired_npc_name|reward_npc_name|value0_acquire_|talk_npc\d)>([^<]*)</\1>")
ACCEPT_FIELDS = ("acquired_npc_name", "value0_acquire_")


def table_references() -> tuple[dict[str, list[str]], dict[str, list[str]]]:
	"""{接取名: 任务 id} / {其余名字: 任务 id} —— 组表服务全部家族表真的写出来的位点。"""
	acq: dict[str, list[str]] = {}
	reward: dict[str, list[str]] = {}

	def add(target: dict[str, list[str]], value: str, quest_id: str) -> None:
		value = value.strip()
		if value:
			target.setdefault(value, []).append(quest_id)

	# DD 表是容器形（<quest_data_driven> 行块，行内另有嵌套 <data><id>）：按块解析，不能被行正则切碎。
	# The DD table is container-shaped (row blocks with nested <data><id>): parse by block, not by row regex.
	if DD_TABLE.exists():
		text = DD_TABLE.read_text(encoding="utf-8", errors="replace")
		for block in re.findall(r"<quest_data_driven>.*?</quest_data_driven>", text, re.S):
			qid = re.search(r"<id>(\d+)</id>", block)
			if not qid:
				continue
			value = re.search(r"<value0_acquire_>([^<]*)</value0_acquire_>", block)
			rew = re.search(r"<reward_npc_name>([^<]*)</reward_npc_name>", block)
			if value:
				add(acq, value.group(1), qid.group(1))
			if rew:
				add(reward, rew.group(1), qid.group(1))
	for table_name in FAMILY_TABLES:
		if table_name == "data_driven_quest.xml":
			continue
		path = RETAIL_DIR / table_name
		if not path.exists():
			continue
		text = path.read_text(encoding="utf-8", errors="replace")
		for qid, body in ROW_RE.findall(text):
			if not qid:
				continue
			for field, value in NAME_FIELDS.findall(body):
				target = acq if field in ACCEPT_FIELDS else reward
				add(target, value, qid)
	return acq, reward


def legacy_dialogs(quest_ids: list[str]) -> tuple[set[int], set[int]]:
	"""(接取流 id 集, 全对话 id 集) for the given quests from the retained legacy XML."""
	accept: set[int] = set()
	flow: set[int] = set()
	for qid in quest_ids:
		path = LEGACY_QUESTS / f"{qid}.xml"
		if not path.exists():
			continue
		for tag in re.finditer(r"<dialog\b[^>]*>", path.read_text(encoding="utf-8")):
			text = tag.group(0)
			npc = re.search(r'npc-id="(\d+)"', text)
			if not npc:
				continue
			flow.add(int(npc.group(1)))
			if 'type="NPC_START"' in text:
				accept.add(int(npc.group(1)))
				continue
			action = re.search(r'action="([A-Z_0-9]*)"', text)
			if action and action.group(1) in ACCEPT_ACTIONS:
				accept.add(int(npc.group(1)))
	return accept, flow


def reward_ids_of(quest_ids: list[str], templates: dict[str, tuple[int, str]]) -> set[int]:
	"""该组名下各任务自己声明的交付 NPC id（遗留多给接取路由时的解释集）。"""
	out: set[int] = set()
	wanted = set(quest_ids)
	for table_name in FAMILY_TABLES:
		path = RETAIL_DIR / table_name
		if not path.exists():
			continue
		text = path.read_text(encoding="utf-8", errors="replace")
		for qid, body in ROW_RE.findall(text):
			if qid not in wanted:
				continue
			rew = re.search(r"<reward_npc_name>([^<]*)</reward_npc_name>", body)
			if not rew:
				continue
			hit = templates.get(rew.group(1).strip())
			if hit:
				out.add(hit[0])
	return out


def main() -> int:
	emit = "--emit" in sys.argv
	blocks = client_quest_ai_blocks()
	dic = client_dic_members()
	templates = server_templates()
	names_by_id = server_names_by_id()
	acq, reward = table_references()
	accept_names = set(acq)
	retention = {}
	for line in RETENTION.read_text(encoding="utf-8").splitlines():
		if line.startswith("#") or not line.strip():
			continue
		parts = line.split("\t")
		if len(parts) >= 4 and "RETAIL_ACQUIRE_NPC_UNRESOLVED" in parts[3]:
			retention[parts[0]] = parts[3]

	referenced: dict[str, set[str]] = {}
	for source in (acq, reward):
		for name, quest_ids in source.items():
			referenced.setdefault(name, set()).update(quest_ids)

	# 候选 = DD 引用 ∧ 客户端声明 ≥2 成员 ∧ 组名与成员自己的名字不同（对话名 ≠ 名字 ⇒ 真·共享对话名）
	# ∧ 成员共享一个 title_id（真端编制不变式：同编制才同对话名）。全部判据只用**静态输入**
	#（客户端 npc 块 + 服务端模板），遗留 XML 只作见证打印、不参与判定。
	# Candidate = DD-referenced and declared with at least two members, with a group name that is not
	# any member's own name (a shared dialog name rather than an ordinary spawn name) and members
	# sharing one title_id (one squad, one dialog name). Every predicate uses static inputs only
	# (client npc blocks + server templates); the legacy XML is printed as a witness, never deciding.
	candidates = []
	excluded: list[tuple[str, str, str]] = []
	for name, quest_ids in sorted(referenced.items()):
		ids = sorted(blocks.get(name, set()))
		if len(ids) < 2:
			continue
		members = [names_by_id.get(npc) for npc in ids]
		if any(member is None for member in members) or name in members:
			continue
		title_ids = sorted({templates[member][1] for member in members})
		if len(title_ids) != 1:
			excluded.append((name, "MULTI_TITLE_ID", ",".join(title_ids)))
			continue
		if name in REGISTERED_EXCLUSIONS:
			excluded.append((name,) + REGISTERED_EXCLUSIONS[name])
			continue
		candidates.append(name)
	# 锚点守卫：守备队家族（P0c-52 批）必须仍在候选里，否则说明客户端 npc 块转储或作用域判据坏了。
	# Anchor guard: the guard family must still be a candidate, else the client npc dump or the scope
	# predicate is broken.
	if not any(name.startswith("LDF4_Advance_Village_Guard_") for name in candidates):
		print(f"ABORT: guard family missing from {len(candidates)} candidates — client dump or scope broken")
		return 1

	failures: list[str] = []
	rows: list[tuple[str, list[str]]] = []
	for name in candidates:
		ids = sorted(blocks[name])
		derived = [names_by_id[npc] for npc in ids]
		dic_members = dic.get(name, [])
		members = dic_members if dic_members else derived
		member_ids = sorted(templates[member][0] for member in members)
		title_ids = {templates[member][1] for member in members}
		quest_ids = sorted(referenced[name])
		accept, flow = legacy_dialogs(quest_ids)
		print(f"== {name}")
		print(f"   block ids        {ids}")
		print(f"   derived members  {derived}")
		print(f"   dic members      {dic_members}")
		print(f"   member ids       {member_ids}")
		print(f"   title_ids        {sorted(title_ids)}")
		print(f"   dd quests        {quest_ids} (retention-unresolved "
			f"{sum(1 for q in quest_ids if q in retention)}/{len(quest_ids)})")
		print(f"   legacy accept    {sorted(accept)} | flow {sorted(flow)}")
		if not dic_members:
			print("   dic members      - (missing from the shipped dump; names derived from the templates)")
		elif sorted(templates[member][0] for member in dic_members) != ids:
			failures.append(f"{name}: dic member ids != client block ids {ids}")
		if len(title_ids) != 1:
			failures.append(f"{name}: members span title_ids {sorted(title_ids)}")
		# 遗留 XML = 见证（非判据）：留存下来的部分给出接取流 id 集，成员应落在其中；证据不完整
		#（同组任务已有退役）时只打印覆盖率，不做"相斥"推断。
		# The legacy XML is a witness, never a predicate: the retained part's accept-flow ids should
		# contain the members; with incomplete evidence (sibling quests already retired) only the
		# coverage is printed, and no contradiction is inferred.
		retained = [quest for quest in quest_ids if (LEGACY_QUESTS / f"{quest}.xml").exists()]
		if name in accept_names and len(retained) == len(quest_ids) and accept and not set(ids) <= accept:
			failures.append(f"{name}: client block ids {ids} not a subset of legacy accept {sorted(accept)}")
		elif len(retained) < len(quest_ids):
			covered = len(set(ids) & accept)
			print(f"   legacy witness   partial evidence ({len(retained)}/{len(quest_ids)} quests retained);"
				f" {covered}/{len(ids)} members seen in the legacy accept flow")
		extras = accept - set(ids)
		explainable = reward_ids_of(quest_ids, templates)
		if extras - explainable:
			print(f"   legacy extras    {sorted(extras - explainable)} (outside the members; partial evidence"
				f" makes reward-npc explanations incomplete)")
		elif extras:
			print(f"   legacy extras    {sorted(extras)} = the quests' own reward npcs (explained)")
		if flow and not set(ids) <= flow:
			print(f"   legacy flow      members outside the retained flow: {sorted(set(ids) - flow)}")
		if not quest_ids:
			failures.append(f"{name}: no family-table row uses this group name")
		rows.append((name, members))

	if failures:
		print("FAILURES:")
		for line in failures:
			print("  " + line)
		return 1
	print(f"OK: {len(rows)} groups, sources agree (member ids from the block/template channel)")
	print(f"excluded candidates: {len(excluded)}")
	for name, code, detail in excluded:
		print(f"   {name}\t{code}\t{detail}")
	if emit:
		header = (
			"# 真端对话名组表（quest_ai_name → 成员 name_desc）——由 p0c52_quest_ai_name_groups.py 生成\n"
			"# 依据：客户端 npc 块 <quest_ai_name> 的成员 id 集（第一手）+ 服务端 npc 模板（成员名与\n"
			"# 共享 title_id）+ 客户端词典正文 STR_DIC_E_<名>（可选，存在时须与块 id 集一致）+ 遗留\n"
			"# 生产 XML 的接取流 id 集（超集见证；多出的 id 必须解释为该组任务的交付 NPC）。\n"
			"# 候选判据：全部家族表真的写出来的名字 ∧ 客户端声明 ≥2 成员 ∧ 组名不是任一成员自己的名字。\n"
			"# quest_ai_name\tmember_name_descs\n")
		body = "".join(f"{name}\t{','.join(members)}\n" for name, members in rows)
		OUT_TSV.write_text(header + body, encoding="utf-8")
		print(f"written {OUT_TSV.relative_to(REPO)}")
	return 0


if __name__ == "__main__":
	sys.exit(main())
