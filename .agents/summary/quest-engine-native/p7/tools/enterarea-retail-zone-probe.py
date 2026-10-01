#!/usr/bin/env python3
"""P7 步 2 步 c 探针：DD `enterarea` 别名的**真端区来源**解析（原生车道数据面，无台账直读）。

真端事实（<真端根>/Map/Worlds/<world>/world{,_M,_N}.xml）：
  · `<npc><name>X</name><sensory_area><points>…` —— 感官区。真端进区处理 `FUN_180c47bf0` 用
    `FUN_1810798b0`（0x1003F 名哈希）把**当前步别名**与进区区名比对 ⇒ 绑定语义 = **同名**。
  · `<questscript_area><name>X</name><quest>id,…</quest><points_info>` —— 任务脚本区。真端数据自带
    `<quest>` 绑定 ⇒ 绑定语义 = **quest 集**（与世界文件里的区名无需逐字相同）。
  · `<item_use_area><name>X</name><points_info>` —— 用物区（同形多边形）。

解析级联（逐行，命中即止）：
  R1 真端区**同名**（感官区 NPC 名 / 任务脚本区名 / 用物区名，逐字）。
  R2 真端感官区**世界前缀同名**（DD 别名 = `<worldDir>_` + 世界文件里的区名）。
  R3 任务脚本区**按 `<quest>` 绑定**（别名 quest 集 ⊆ 区 quest 集，且区在世界目录命中）。
  R4 fail-closed：真端世界的世界文件全无该区定义 ⇒ `NO_RETAIL_AREA`（不注册、不猜几何）。

mapid 来源 = 仓内既存的客户端派生映射 `<仓库根>/src/main/resources/aion/definitions/compact/id-mappings.xml`
（`id/worldid.xml`：世界目录名小写 → mapid）。

用法 / usage:
  python3 -B enterarea-retail-zone-probe.py                 # 只计算并写 TSV/JSON
  python3 -B enterarea-retail-zone-probe.py --check         # 只校验已落盘产物是否与现算一致
  python3 -B enterarea-retail-zone-probe.py --emit-zones    # 生成 zones_retail_enterarea.xml
"""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve()


def find_repo(start: pathlib.Path) -> pathlib.Path:
	for candidate in [start, *start.parents]:
		if (candidate / "pom.xml").is_file():
			return candidate
	raise SystemExit("cannot locate <仓库根> (pom.xml) from " + str(start))


REPO = find_repo(HERE)
RETAIL = REPO.parent / "58Server"
WORLDS = RETAIL / "Map" / "Worlds"
ID_MAPPINGS = REPO / "src/main/resources/aion/definitions/compact/id-mappings.xml"
DD_TABLE = REPO / "src/main/resources/aion/data/static_data/quest/retail/data_driven_quest.xml"
PRESENCE = REPO / ".agents/summary/quest-engine-native/p7/dd-client-presence.tsv"
ZONES_DIR = REPO / "src/main/resources/aion/data/static_data/zones"
ZONE_OUT = ZONES_DIR / "zones_retail_enterarea.xml"
TSV_OUT = HERE.parent / "qe-enterarea-retail-zone-resolution.tsv"
JSON_OUT = HERE.parent / "qe-enterarea-retail-zone-resolution.json"

WORLD_FILES = ("world.xml", "world_M.xml", "world_N.xml")
# 几何同时出现在 world.xml 与 world_N.xml 时优先取「更全」的那份：N > M > 基准。
FILE_RANK = {"world_N.xml": 0, "world_M.xml": 1, "world.xml": 2}


def read_world_text(path: pathlib.Path) -> str:
	raw = path.read_bytes()
	if raw[:2] in (b"\xff\xfe", b"\xfe\xff"):
		return raw.decode("utf-16", errors="replace")
	return raw.decode("utf-8", errors="replace")


def load_world_ids() -> dict[str, str]:
	text = ID_MAPPINGS.read_text("utf-8", errors="replace")
	match = re.search(r'<static_document[^>]*name="id/worldid\.xml"[^>]*>(.*?)</static_document>', text, re.S)
	if not match:
		raise SystemExit("id/worldid.xml not found in " + str(ID_MAPPINGS))
	pairs = re.findall(r'<data\s+id="(\d+)"(?:\s+[^>]*)?>([^<]*)</data>', match.group(1))
	return {name.strip().lower(): mapid for mapid, name in pairs}


def parse_rings(block: str) -> list[tuple[list[tuple[str, str]], str, str]]:
	"""<points>…</points> + <bottom>/<top> → [(points, bottom, top)]，points 内为 (x, y)。"""
	rings = []
	for points in re.findall(r"<points>(.*?)</points>", block, re.S):
		coords = re.findall(r"<x>([-\d.]+)</x>\s*<y>([-\d.]+)</y>", points)
		if not coords:
			continue
		tops = re.findall(r"<top>([-\d.]+)</top>", block)
		bottoms = re.findall(r"<bottom>([-\d.]+)</bottom>", block)
		rings.append((coords, bottoms[0] if bottoms else "", tops[0] if tops else ""))
	return rings


def load_retail_areas() -> list[dict]:
	"""真端世界文件里的区定义：按 (kind, name, world_dir) 归并**全部胞**。

	同一感官区在真端世界文件里可能是**多个同名 `<npc>`**（每胞一条，多胞区 = 多胞），因此必须按名字归并，
	只看第一条会把 6 胞区读成 1 胞。同名区同时出现在 `world{,_M,_N}.xml` 时取「N > M > 基准」且实测
	三份文件的胞集合逐字相同（328/328，探针自带断言）。"""
	merged: dict[tuple[str, str, str], dict] = {}
	for world_dir in sorted(p for p in WORLDS.iterdir() if p.is_dir()):
		for rank, file_name in enumerate(("world_N.xml", "world_M.xml", "world.xml")):
			path = world_dir / file_name
			if not path.is_file():
				continue
			text = read_world_text(path)
			if "<sensory_area>" not in text and "<questscript_area>" not in text and "<item_use_area>" not in text:
				continue
			for block in re.finditer(r"<npc\b[^>]*>(.*?)</npc>", text, re.S):
				body = block.group(1)
				if "<sensory_area>" not in body:
					continue
				name = re.search(r"<name>([^<]+)</name>", body)
				area = re.search(r"<sensory_area>(.*?)</sensory_area>", body, re.S)
				if name and area:
					absorb(merged, "sensory_area", name.group(1).strip(), world_dir.name, file_name, rank,
						parse_rings(area.group(1)), [])
			for kind in ("questscript_area", "item_use_area"):
				for block in re.finditer(r"<%s\b[^>]*>(.*?)</%s>" % (kind, kind), text, re.S):
					body = block.group(1)
					name = re.search(r"<name>([^<]+)</name>", body)
					if not name:
						continue
					quests = re.search(r"<quest>([^<]*)</quest>", body)
					absorb(merged, kind, name.group(1).strip(), world_dir.name, file_name, rank,
						parse_rings(body), [q.strip() for q in quests.group(1).split(",") if q.strip()] if quests else [])
	return sorted(merged.values(), key=lambda a: (a["name"], a["world_dir"], a["kind"]))


def absorb(merged: dict, kind: str, name: str, world_dir: str, file_name: str, rank: int,
		rings: list, quests: list) -> None:
	key = (kind, name, world_dir)
	entry = merged.get(key)
	if entry is None:
		merged[key] = {"kind": kind, "name": name, "world_dir": world_dir, "file": file_name,
			"rank": rank, "rings": list(rings), "quests": list(quests)}
		return
	if rank < entry["rank"]:
		entry.update({"file": file_name, "rank": rank, "rings": list(rings), "quests": list(quests)})
		return
	if rank > entry["rank"]:
		return
	known = {canonical_cell(cell) for cell in entry["rings"]}
	for cell in rings:
		if canonical_cell(cell) not in known:
			entry["rings"].append(cell)
			known.add(canonical_cell(cell))
	for quest in quests:
		if quest not in entry["quests"]:
			entry["quests"].append(quest)


def canonical_cell(cell: tuple) -> str:
	points, bottom, top = cell
	return "%s|%s|%s" % (bottom, top, ",".join("%s %s" % (x, y) for x, y in points))


def load_switch_aliases() -> dict[str, dict]:
	"""切换集里 `EnterArea` 的两个轴：progress（进程步）与 acquire（接取 kind）。

	两轴的别名集合**互斥**（progress 105 / acquire 15，并集 120）：progress 由进区 handler 消费，
	acquire 由接取面消费（另批）。本工具两轴都裁定，但只给 progress 轴发区数据。
	"""
	lines = [line for line in PRESENCE.read_text("utf-8").splitlines() if line and not line.startswith("#")]
	header = lines[0].split("\t")
	rows = [dict(zip(header, line.split("\t"))) for line in lines[1:]]
	switch = {int(r["quest_id"]) for r in rows
		if r["verdict"] == "CLIENT_PRESENT" and r["retention_owner"] == "RETAIL_TABLE"}
	text = DD_TABLE.read_text("utf-8", errors="replace")
	entries = {}
	for entry in re.findall(r"<quest_data_driven>(.*?)</quest_data_driven>", text, re.S):
		match = re.search(r"<id>(\d+)</id>", entry)
		if match:
			entries[int(match.group(1))] = entry
	aliases: dict[str, dict] = {}
	for quest_id in sorted(switch):
		entry = entries.get(quest_id, "")
		for block in re.findall(r"<data>(.*?)</data>", entry, re.S):
			if "<category_progress_>EnterArea</category_progress_>" in block:
				value = re.search(r"<value0_progress_>([^<]*)</value0_progress_>", block)
				if value and value.group(1).strip():
					record(aliases, value.group(1).strip(), "progress", quest_id)
		if "<category_acquire_>EnterArea</category_acquire_>" in entry:
			value = re.search(r"<value0_acquire_>([^<]*)</value0_acquire_>", entry)
			if value and value.group(1).strip():
				record(aliases, value.group(1).strip(), "acquire", quest_id)
	return aliases


def record(aliases: dict[str, dict], alias: str, axis: str, quest_id: int) -> None:
	entry = aliases.setdefault(alias, {"axes": set(), "quests": set()})
	entry["axes"].add(axis)
	entry["quests"].add(quest_id)


def load_registered_zones() -> dict[str, dict]:
	zones = {}
	for path in sorted(ZONES_DIR.glob("zones_*.xml")):
		if path == ZONE_OUT:
			# 本工具自己的产物不计入「既存区」：否则第二次运行会把全部别名当成已登记，写出空表。
			# The tool's own output is not part of the pre-existing registry (keeps emission idempotent).
			continue
		text = path.read_text("utf-8", errors="replace")
		for block in re.finditer(r"<zone\b[^>]*name=\"([^\"]+)\"[^>]*>(.*?)</zone>", text, re.S):
			name = block.group(1)
			mapid = re.search(r'<zone\b[^>]*mapid="(\d+)"', block.group(0))
			zones[name.upper()] = {"name": name, "mapid": mapid.group(1) if mapid else "",
				"file": path.name, "cells": block.group(2).count("<points")}
	return zones


def digest(rings: list[tuple[list[tuple[str, str]], str, str]]) -> str:
	canonical = ";".join(
		"%s|%s|%s" % (bottom, top, ",".join("%s %s" % (x, y) for x, y in points))
		for points, bottom, top in rings)
	return hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:16]


def resolve(alias: str, quests: list[int], areas: list[dict]) -> dict:
	exact = [area for area in areas if area["name"] == alias]
	if exact:
		return pick(exact, "R1_SAME_NAME", quests)
	# 任务脚本区：真端数据自带 <quest> 绑定，绑定优先于名字形（`QuestArea_Q13965` 绑 13975 系列，
	# `QuestArea_Q13965a` 才绑 13965 系列 ⇒ 只按名字形取会错绑）。
	# Questscript areas carry an explicit retail <quest> binding; binding wins over name shape.
	quest_set = {str(q) for q in quests}
	bound = [area for area in areas
		if area["kind"] == "questscript_area" and quest_set & set(area["quests"])
		and (alias.upper().startswith(area["world_dir"].upper() + "_")
			or area["name"].upper().split("_")[0] in alias.upper().split("_")[0])]
	if bound:
		# 绑定必须完全覆盖别名 quest 集，否则只是部分相交 ⇒ 不取。
		full = [area for area in bound if quest_set <= set(area["quests"])]
		return pick(full or bound, "R3_QUESTSCRIPT_QUEST_BINDING", quests)
	prefixed = []
	for area in areas:
		prefix = area["world_dir"].upper() + "_"
		if alias.upper().startswith(prefix) and alias[len(prefix):] == area["name"]:
			prefixed.append(area)
	if prefixed:
		return pick(prefixed, "R2_WORLD_PREFIX_SAME_NAME", quests)
	return {"status": "NO_RETAIL_AREA", "rule": "R4_FAIL_CLOSED", "kind": "", "world_dir": "",
		"mapid": "", "file": "", "rings": [], "quests": [], "candidates": 0}


def normalize_zone_name(alias: str) -> set:
	"""别名 → 遗留区名候选（不含 mapid 后缀）：大写 + `SENSORYAREA`→`SENSORY_AREA` + `AtoB`→`A_TO_B`
	+ 数字后尾字母拆分。仅用于与仓内既存区做**几何交叉核对**，不作运行时解析依据。"""
	up = alias.upper()
	variants = {up, up.replace("SENSORYAREA", "SENSORY_AREA"), up.replace("QUESTAREA", "QUEST_AREA")}
	for value in list(variants):
		variants.add(re.sub(r"([A-Z])TO([A-Z])(?=_|$)", r"\1_TO_\2", value))
		variants.add(re.sub(r"([0-9])([A-Z])(?=_|$)", r"\1_\2", value))
		variants.add(re.sub(r"([0-9])([A-Z])([A-Z])(?=_|$)", r"\1_\2\3", value))
	return variants


def legacy_zone_for(alias: str, registered: dict[str, dict]) -> dict | None:
	for variant in normalize_zone_name(alias):
		for name, info in registered.items():
			base = re.sub(r"_\d{9}$", "", name)
			if name == variant or base == variant:
				return info
	return None


def twin_alias(alias: str) -> str:
	"""LF6↔DF6 / 1xxxx↔2xxxx 镜像别名（真端副本的世界数据侧完整性核对用）。"""
	if not alias.upper().startswith("LF"):
		return ""
	mirror = "DF" + alias[2:]
	match = re.match(r"(DF[0-9]+_\w+_Q)([12])(\d+)(.*)$", mirror)
	if not match:
		return mirror
	return "%s%s%s%s" % (match.group(1), "2" if match.group(2) == "1" else "1", match.group(3), match.group(4))


def pick(candidates: list[dict], rule: str, quests: list[int]) -> dict:
	candidates = sorted(candidates, key=lambda a: (a["rank"], -len(a["rings"])))
	best = candidates[0]
	quest_set = {str(q) for q in quests}
	return {"status": "OK", "rule": rule, "kind": best["kind"], "world_dir": best["world_dir"],
		"mapid": "", "file": best["file"], "rings": best["rings"], "candidates": len(candidates),
		"quests": best["quests"], "binding_ok": (quest_set <= set(best["quests"])) if best["quests"] else None}


def main() -> int:
	parser = argparse.ArgumentParser()
	parser.add_argument("--check", action="store_true", help="只校验产物与现算一致")
	parser.add_argument("--emit-zones", action="store_true", help="生成 zones_retail_enterarea.xml")
	args = parser.parse_args()

	world_ids = load_world_ids()
	areas = load_retail_areas()
	aliases = load_switch_aliases()
	registered = load_registered_zones()

	rows = []
	for alias in sorted(aliases):
		axes = sorted(aliases[alias]["axes"])
		quests = sorted(aliases[alias]["quests"])
		result = resolve(alias, quests, areas)
		if result["status"] == "OK":
			result["mapid"] = world_ids.get(result["world_dir"].lower(), "?")
			if result["mapid"] == "?":
				result["status"] = "NO_MAPID"
		reuse = registered.get(alias.upper())
		legacy = legacy_zone_for(alias, registered)
		legacy_note = ""
		if legacy and result["rings"]:
			legacy_note = "%s:cells=%d/%d" % (legacy["file"], len(result["rings"]), legacy["cells"])
		elif legacy:
			legacy_note = "%s:cells=0/%d" % (legacy["file"], legacy["cells"])
		mirror = twin_alias(alias)
		twin = next((area for area in areas if area["name"] == mirror), None)
		rows.append({
			"alias": alias,
			"axis": "+".join(axes),
			"quests": ",".join(str(q) for q in quests),
			"status": result["status"],
			"rule": result["rule"],
			"kind": result["kind"],
			"world_dir": result["world_dir"],
			"mapid": result["mapid"],
			"cells": len(result["rings"]),
			"points_digest": digest(result["rings"]) if result["rings"] else "",
			"zone_name": alias,
			"zone_registered": reuse["name"] if reuse else "",
			"binding_ok": result.get("binding_ok"),
			"legacy_cross_check": legacy_note,
			"twin_evidence": ("%s:%s" % (twin["world_dir"], twin["kind"])) if twin else "",
			"source": "%s:%s" % (result["file"], result["kind"]) if result["kind"] else (result["rule"]),
			"rings": result["rings"],
		})

	ok = [r for r in rows if r["status"] == "OK"]
	missing = [r for r in rows if r["status"] != "OK"]
	progress = [r for r in rows if r["axis"] == "progress"]
	acquire = [r for r in rows if r["axis"] == "acquire"]
	progress_ok = [r for r in progress if r["status"] == "OK"]
	acquire_ok = [r for r in acquire if r["status"] == "OK"]
	summary = {
		"switch_set_aliases": len(rows),
		"progress_axis": {"total": len(progress), "resolved": len(progress_ok),
			"no_retail_area": len(progress) - len(progress_ok)},
		"acquire_axis": {"total": len(acquire), "resolved": len(acquire_ok),
			"no_retail_area": len(acquire) - len(acquire_ok)},
		"resolved": len(ok),
		"no_retail_area": len(missing),
		"kinds": dict(collections.Counter(r["kind"] for r in ok)),
		"rules": dict(collections.Counter(r["rule"] for r in ok)),
		"world_dirs": dict(collections.Counter(r["world_dir"] for r in ok)),
		"cells_total": sum(r["cells"] for r in ok),
		"progress_cells_total": sum(r["cells"] for r in progress_ok),
		"already_registered_same_name": sum(1 for r in rows if r["zone_registered"]),
		"binding_mismatch": [r["alias"] for r in ok if r["kind"] == "questscript_area" and r["binding_ok"] is False],
		"legacy_cross_check_mismatch": [r["alias"] for r in ok if r["legacy_cross_check"]
			and r["legacy_cross_check"].split("cells=")[1].split("/")[0] != r["legacy_cross_check"].split("cells=")[1].split("/")[1].split(":")[0]],
		"twin_evidence_present": [r["alias"] for r in missing if r["twin_evidence"]],
		"progress_missing_aliases": [r["alias"] for r in progress if r["status"] != "OK"],
		"acquire_missing_aliases": [r["alias"] for r in acquire if r["status"] != "OK"],
		"missing_aliases": [r["alias"] for r in missing],
	}

	columns = ("alias", "axis", "quests", "status", "rule", "kind", "world_dir", "mapid", "cells", "points_digest",
		"zone_name", "zone_registered", "legacy_cross_check", "twin_evidence", "source")
	tsv = ["\t".join(columns)]
	tsv += ["\t".join(str(r[key]) for key in columns) for r in rows]
	tsv_text = "\n".join(tsv) + "\n"
	json_text = json.dumps(summary, ensure_ascii=False, indent=2) + "\n"

	if args.check:
		ok_check = TSV_OUT.read_text("utf-8") == tsv_text and JSON_OUT.read_text("utf-8") == json_text
		print("CHECK", "OK" if ok_check else "STALE")
		return 0 if ok_check else 1
	TSV_OUT.write_text(tsv_text, "utf-8")
	JSON_OUT.write_text(json_text, "utf-8")
	print(json.dumps(summary, ensure_ascii=False, indent=2))

	if args.emit_zones:
		# 步 f 起两轴都发区：acquire 轴（接取 kind 6 的同名区树）与 progress 轴互斥，几何同源。
		# Since step f both axes emit zones: the acquire axis (kind-6 same-name tree) is
		# alias-disjoint from the progress axis and shares the same retail geometry source.
		emit_zone_file(ok)
	return 0


def emit_zone_file(rows: list[dict]) -> None:
	registered = load_registered_zones()
	lines = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
		'<!--',
		'\tDD EnterArea 原生区注册表：一个 zone 元素 = 一个真端区（名字逐字取自 DD 别名与真端世界文件）。',
		'\t生成工具：.agents/summary/quest-engine-native/p7/tools/enterarea-retail-zone-probe.py（emit-zones 模式）。',
		'\t坐标与 top/bottom 逐字取自 真端根 Map/Worlds/<world>/world{,_M,_N}.xml 的 sensory_area、',
		'\tquestscript_area、item_use_area 段；mapid 取自 id-mappings.xml（客户端派生）。禁止手改。',
		'-->',
		'<zones>']
	written = 0
	for row in rows:
		if row["zone_registered"]:
			continue
		lines.append('\t<zone mapid="%s" name="%s" area_type="POLYGON" zone_type="SUB">'
			% (row["mapid"], row["zone_name"]))
		for points, bottom, top in row["rings"]:
			lines.append('\t\t<points bottom="%s" top="%s">' % (bottom, top))
			for x, y in points:
				lines.append('\t\t\t<point x="%s" y="%s"/>' % (x, y))
			lines.append('\t\t</points>')
		lines.append('\t</zone>')
		written += 1
	lines.append('</zones>')
	ZONE_OUT.write_text("\n".join(lines) + "\n", "utf-8")
	print("emitted zones:", written, "->", ZONE_OUT.relative_to(REPO))


if __name__ == "__main__":
	sys.exit(main())
