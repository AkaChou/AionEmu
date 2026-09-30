#!/usr/bin/env python3
"""真端模板表 <-> 本仓库 typed quest XML 只读对账。

输入（都不修改）:
  真端：<真端根>/Map/XML/
        Quest_SimpleHunt/SerialHunt/Talk/CollectItem/UseItem/ItemPlay/CombineTask.xml,
        data_driven_quest.xml, quest.xml, npcs.xml, Items.xml
  本仓库：src/main/resources/aion/data/static_data/quest/definitions/quests/*.xml

输出（本目录）:
  reconciliation.tsv      逐任务对账结果
  reconciliation-summary.md  汇总（由 report 脚本或人工引用）

判定项:
  TARGETS   真端 monster/object/item 名单 -> id 是否都能在本仓库击杀/物品目标里找到
  REWARD    真端 reward_npc_name -> id 是否命中本仓库 npc-complete / reward 路线 NPC
  ACQUIRE   真端 acquired_npc_name / value0_acquire_ -> id 是否命中本仓库 npc-start / dialog NPC
  PREREQ    真端 con_quest 是否命中本仓库 quests-finished
  STEPS     真端击杀/收集步数 vs 本仓库结构估算
"""
from __future__ import annotations
import os

import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

RETAIL = Path(f"{REPO.parent / '58Server'}/Map/XML")
REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
OURS = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
OUT = Path(__file__).resolve().parent

TEMPLATES = [
    ("SimpleHunt", "Quest_SimpleHunt.xml"),
    ("SimpleSerialHunt", "Quest_SimpleSerialHunt.xml"),
    ("SimpleTalk", "Quest_SimpleTalk.xml"),
    ("SimpleCollectItem", "Quest_SimpleCollectItem.xml"),
    ("SimpleUseItem", "Quest_SimpleUseItem.xml"),
    ("SimpleItemPlay", "Quest_SimpleItemPlay.xml"),
    ("CombineTask", "Quest_CombineTask.xml"),
]


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-16", errors="replace")


def strip_doctype(text: str) -> str:
    idx = text.find("]>")
    return text[idx + 2:] if idx > 0 else text


def load_name_map(path: Path, tag: str) -> dict[str, set[int]]:
    body = strip_doctype(read_text(path))
    pairs = re.findall(r"<id>(\d+)</id>\s*<name>([^<]+)</name>", body)
    out: dict[str, set[int]] = {}
    for i, name in pairs:
        out.setdefault(name.strip().lower(), set()).add(int(i))
    return out


def load_retail_templates() -> dict[int, dict]:
    out: dict[int, dict] = {}
    for template, filename in TEMPLATES:
        body = strip_doctype(read_text(RETAIL / filename))
        for m in re.finditer(r'<id\s+id="(\d+)">(.*?)</id>', body, re.S):
            qid, block = int(m.group(1)), m.group(2)
            fields: dict[str, list[str]] = {}
            for fm in re.finditer(r"<([a-z0-9_]+)>\s*([^<]*?)\s*</\1>", block):
                fields.setdefault(fm.group(1), []).append(fm.group(2).strip())
            out.setdefault(qid, {"template": template, "fields": fields})
    # data_driven: <quest_data_driven><id>N</id> ... </quest_data_driven>
    body = strip_doctype(read_text(RETAIL / "data_driven_quest.xml"))
    for m in re.finditer(r"<quest_data_driven>(.*?)</quest_data_driven>", body, re.S):
        block = m.group(1)
        idm = re.search(r"<id>(\d+)</id>", block)
        if not idm:
            continue
        qid = int(idm.group(1))
        fields: dict[str, list[str]] = {}
        for fm in re.finditer(r"<([a-z0-9_]+)>\s*([^<]*?)\s*</\1>", block):
            fields.setdefault(fm.group(1), []).append(fm.group(2).strip())
        fields["_raw"] = [block]
        out.setdefault(qid, {"template": "DataDriven", "fields": fields})
    return out


def load_retail_quest_meta() -> dict[int, dict]:
    body = strip_doctype(read_text(RETAIL / "quest.xml"))
    meta: dict[int, dict] = {}
    for m in re.finditer(r"<quest>(.*?)</quest>", body, re.S):
        block = m.group(1)
        idm = re.search(r"<id>(\d+)</id>", block)
        if not idm:
            continue
        fields: dict[str, list[str]] = {}
        for fm in re.finditer(r"<([a-z0-9_]+)>\s*([^<]*?)\s*</\1>", block):
            fields.setdefault(fm.group(1), []).append(fm.group(2).strip())
        meta[int(idm.group(1))] = fields
    return meta


def tokens(value: str) -> list[str]:
    return [t for t in re.split(r"[\s,;]+", value) if t]


def parse_our_quest(path: Path) -> dict:
    root = ET.parse(path).getroot()
    sig: dict = {
        "kill_ids": set(),
        "npc_ids": set(),
        "kill_steps": 0,
        "dialog_ids": set(),
        "complete_ids": set(),
        "start_ids": set(),
        "item_ids": set(),
        "prereq": set(),
        "blocks": set(),
        "statuses": [],
        "var0": set(),
        "start_nodes": 0,
    }
    for node in root.findall("./nodes/node"):
        label = node.get("label", "")
        status = node.get("status", "")
        sig["statuses"].append(status)
        var = node.find("var")
        if var is not None and var.get("name") == "var0":
            sig["var0"].add(int(var.get("value")))
        if status == "START":
            sig["start_nodes"] += 1
    for pre in root.findall("./metadata/prerequisites/quest"):
        value = pre.get("id", "")
        if value.isdigit():
            sig["prereq"].add(int(value))
    for el in root.iter():
        tag = el.tag
        for value in tokens(el.get("npc-id") or "") + tokens(el.get("npc-ids") or ""):
            if value.isdigit():
                sig["npc_ids"].add(int(value))
        if el.get("item-id", "").isdigit():
            sig["item_ids"].add(int(el.get("item-id")))
        if tag in {"kill-chain", "counter", "counter-grid", "kill-routes", "npc-start", "npc-report", "npc-item-report", "npc-complete"}:
            sig["blocks"].add(tag)
        if tag == "npc-complete" and el.get("npc-id"):
            sig["complete_ids"].add(int(el.get("npc-id")))
        if tag == "npc-start" and el.get("npc-id"):
            sig["start_ids"].add(int(el.get("npc-id")))
        if tag in {"dialog", "npc-report", "npc-item-report"} and el.get("npc-id"):
            sig["dialog_ids"].add(int(el.get("npc-id")))
        if tag == "kill-npc":
            ids = el.get("npc-id") or ""
            idset = el.get("npc-ids") or ""
            for value in [ids] + tokens(idset):
                if value.isdigit():
                    sig["kill_ids"].add(int(value))
        if tag == "kill-chain":
            nodes = tokens(el.get("nodes") or "")
            sig["kill_steps"] += max(0, len(nodes) - 1)
        if tag == "counter" and el.get("required", "").isdigit():
            sig["kill_steps"] += int(el.get("required"))
        if tag == "counter-grid":
            for dim in el.findall("dimension"):
                for value in tokens(dim.get("npc-ids") or ""):
                    if value.isdigit():
                        sig["kill_ids"].add(int(value))
                if dim.get("required", "").isdigit():
                    sig["kill_steps"] += int(dim.get("required"))
        if tag == "kill-routes":
            for value in tokens(el.get("npc-ids") or ""):
                if value.isdigit():
                    sig["kill_ids"].add(int(value))
            sig["kill_steps"] += len(tokens(el.get("npc-ids") or ""))
        if tag == "npc-item-report" and el.get("required", "").isdigit():
            sig["kill_steps"] += int(el.get("required"))
        if tag in {"give-item", "remove-item", "has-item"} and el.get("item-id"):
            sig["item_ids"].add(int(el.get("item-id")))
        if tag == "quests-finished" and el.get("quest-ids"):
            for value in tokens(el.get("quest-ids")):
                if value.isdigit():
                    sig["prereq"].add(int(value))
        if tag == "condition" and (el.get("type") or "") == "finished" and (el.get("quest-id") or "").isdigit():
            sig["prereq"].add(int(el.get("quest-id")))
    # 非块内联 kill 转换
    for tr in root.findall("./transitions/transition"):
        if tr.find("./event/kill-npc") is not None:
            sig["kill_steps"] += 1
    return sig


def name_to_ids(value: str, npc_map: dict[str, set[int]], item_map: dict[str, set[int]]) -> tuple[set[int], set[str]]:
    ids: set[int] = set()
    missing: set[str] = set()
    for name in tokens(value):
        key = name.strip().lower()
        if not key or key.isdigit():
            continue
        if key.startswith(("haction", "relative", "absolute", "str_")) or key in {"movie", "cutscene"}:
            continue
        if key in npc_map:
            ids |= npc_map[key]
        elif key in item_map:
            ids |= item_map[key]
        else:
            missing.add(name)
    return ids, missing


def parse_hunt(value: str, npc_map, item_map) -> tuple[set[int], set[str], int]:
    """解析 Hunt 值串：'Name_a, Name_b 5; Name_c 3;' -> (ids, missing, steps)。"""
    ids: set[int] = set()
    missing: set[str] = set()
    steps = 0
    for group in value.split(";"):
        group = group.strip()
        if not group:
            continue
        parts = [p.strip() for p in group.split(",") if p.strip()]
        count = 0
        if parts:
            tail = re.match(r"^(.*?)\s+(\d+)$", parts[-1])
            if tail:
                parts[-1] = tail.group(1).strip()
                count = int(tail.group(2))
        for name in parts:
            found, miss = name_to_ids(name, npc_map, item_map)
            ids |= found
            missing |= miss
        steps += count
    return ids, missing, steps


def retail_expectations(entry: dict, meta: dict, npc_map, item_map) -> dict:
    t = entry["template"]
    f = entry["fields"]
    exp = {
        "target_ids": set(),
        "target_missing": set(),
        "reward_ids": set(),
        "reward_missing": set(),
        "acquire_ids": set(),
        "acquire_missing": set(),
        "prereq": set(),
        "steps": None,
        "categories": set(),
    }
    def add_npc(key, bucket):
        for value in f.get(key, []):
            ids, missing = name_to_ids(value, npc_map, item_map)
            exp[bucket].update(ids)
            exp[bucket.replace("_ids", "_missing")].update(missing)

    def add_monsters(prefix):
        steps = 0
        for idx in range(1, 6):
            names = f.get(f"{prefix}monster{idx}") or f.get(f"{prefix}monster_{idx}") or []
            counts = f.get(f"{prefix}count{idx}") or f.get(f"{prefix}count_{idx}") or []
            for value in names:
                ids, missing = name_to_ids(value, npc_map, item_map)
                exp["target_ids"].update(ids)
                exp["target_missing"].update(missing)
            for value in counts:
                if value.isdigit():
                    steps += int(value)
        return steps

    if t == "SimpleHunt":
        exp["steps"] = add_monsters("")
        add_npc("reward_npc_name", "reward_ids")
        add_npc("acquired_npc_name", "acquire_ids")
    elif t == "SimpleSerialHunt":
        for idx in range(1, 6):
            names = f.get(f"monster_{['first','second','third','fourth','fifth'][idx-1]}") or []
            counts = f.get(f"count_{['first','second','third','fourth','fifth'][idx-1]}") or []
            for value in names:
                ids, missing = name_to_ids(value, npc_map, item_map)
                exp["target_ids"].update(ids)
                exp["target_missing"].update(missing)
            exp["steps"] = (exp["steps"] or 0) + sum(int(v) for v in counts if v.isdigit())
        add_npc("reward_npc_name", "reward_ids")
        add_npc("acquired_npc_name", "acquire_ids")
        for idx in range(1, 6):
            add_npc(f"talk_npc{idx}", "acquire_ids")
    elif t in {"SimpleTalk", "SimpleItemPlay", "SimpleUseItem"}:
        add_npc("reward_npc_name", "reward_ids")
        add_npc("acquired_npc_name", "acquire_ids")
        for idx in range(1, 6):
            add_npc(f"talk_npc{idx}", "acquire_ids")
        for key in ("use_item_name", "give_item"):
            for value in f.get(key, []):
                ids, missing = name_to_ids(value, npc_map, item_map)
                exp["target_ids"].update(ids)
                exp["target_missing"].update(missing)
    elif t == "SimpleCollectItem":
        add_npc("reward_npc_name", "reward_ids")
        add_npc("acquired_npc_name", "acquire_ids")
        for key in ("object1", "object2", "object3"):
            for value in f.get(key, []):
                ids, missing = name_to_ids(value, npc_map, item_map)
                exp["target_ids"].update(ids)
                exp["target_missing"].update(missing)
    elif t == "CombineTask":
        add_npc("task_npc", "acquire_ids")
        for key in ("product", "give_component1", "give_component2", "give_component3"):
            for value in f.get(key, []):
                ids, missing = name_to_ids(value, npc_map, item_map)
                exp["target_ids"].update(ids)
                exp["target_missing"].update(missing)
    else:  # DataDriven
        for value in f.get("value0_acquire_", []):
            ids, missing = name_to_ids(value, npc_map, item_map)
            exp["acquire_ids"].update(ids)
            exp["acquire_missing"].update(missing)
        for value in f.get("reward_npc_name", []):
            ids, missing = name_to_ids(value, npc_map, item_map)
            exp["reward_ids"].update(ids)
            exp["reward_missing"].update(missing)
        steps = 0
        for dm in re.finditer(r"<data>(.*?)</data>", (f.get("_raw") or [""])[0], re.S):
            block = dm.group(1)
            catm = re.search(r"<category_progress_>([^<]*)</category_progress_>", block)
            cat = catm.group(1).strip() if catm else ""
            exp["categories"].add(cat)
            values = re.findall(r"<value\d+_progress_>([^<]*)</value\d+_progress_>", block)
            for value in values:
                if cat.lower().startswith(("hunt", "pvp")):
                    ids, missing, cnt = parse_hunt(value, npc_map, item_map)
                    exp["target_ids"].update(ids)
                    exp["target_missing"].update(missing)
                    steps += cnt
                else:
                    ids, missing = name_to_ids(value, npc_map, item_map)
                    exp["target_ids"].update(ids)
                    exp["target_missing"].update(missing)
        exp["steps"] = steps or None
    for value in meta.get("collect_item1", []) + meta.get("collect_item2", []):
        ids, _ = name_to_ids(value, npc_map, item_map)
        exp["target_ids"].update(ids)
    # 前置以真端 quest.xml 的 finished_quest_condN 为准（模板表的 con_quest 不是前置字段）。
    for key, values in meta.items():
        if key.startswith("finished_quest_cond"):
            for value in values:
                for chunk in re.split(r"[,\s]+", value.strip()):
                    chunk = chunk.split(":")[0]
                    if re.fullmatch(r"[Qq]?\d+", chunk or ""):
                        exp["prereq"].add(int(re.sub(r"[^0-9]", "", chunk)))
    return exp


def main() -> int:
    npc_map = load_name_map(RETAIL / "npcs.xml", "npc")
    item_map = load_name_map(RETAIL / "Items.xml", "item")
    templates = load_retail_templates()
    meta = load_retail_quest_meta()
    rows = []
    stats = Counter()
    ours_files = sorted(p for p in OURS.glob("*.xml") if p.stem.isdigit())
    for path in ours_files:
        qid = int(path.stem)
        entry = templates.get(qid)
        sig = parse_our_quest(path)
        our_meta = meta.get(qid, {})
        if entry is None:
            stats["NO_TEMPLATE"] += 1
            rows.append({
                "quest": qid, "template": "-", "verdict": "NO_TEMPLATE",
                "detail": "", "our_kill": len(sig["kill_ids"]), "our_steps": sig["kill_steps"],
                "retail_steps": "", "targets_missing": "", "reward": "", "acquire": "",
            })
            continue
        exp = retail_expectations(entry, our_meta, npc_map, item_map)
        checks = []
        our_universe = sig["kill_ids"] | sig["item_ids"] | sig["npc_ids"]
        if exp["target_ids"]:
            if not exp["target_ids"] <= our_universe:
                checks.append("TARGETS")
        if exp["reward_ids"] and not exp["reward_ids"] <= sig["complete_ids"] | sig["dialog_ids"]:
            checks.append("REWARD")
        if exp["acquire_ids"] and not exp["acquire_ids"] <= sig["start_ids"] | sig["dialog_ids"]:
            checks.append("ACQUIRE")
        if exp["prereq"] and not exp["prereq"] <= sig["prereq"]:
            checks.append("PREREQ")
        if exp["steps"] and sig["kill_steps"] and exp["steps"] != sig["kill_steps"]:
            checks.append("STEPS")
        verdict = "ALIGNED" if not checks else "DIFF_" + "_".join(checks)
        stats[verdict] += 1
        stats[f"TPL_{entry['template']}"] += 1
        rows.append({
            "quest": qid,
            "template": entry["template"],
            "verdict": verdict,
            "detail": ";".join(checks),
            "our_kill": len(sig["kill_ids"]),
            "our_steps": sig["kill_steps"],
            "retail_steps": exp["steps"] or "",
            "targets_missing": " ".join(sorted(exp["target_missing"])),
            "reward": " ".join(str(i) for i in sorted(exp["reward_ids"])),
            "acquire": " ".join(str(i) for i in sorted(exp["acquire_ids"])),
        })

    tsv = OUT / "reconciliation.tsv"
    with tsv.open("w", encoding="utf-8") as fh:
        cols = ["quest", "template", "verdict", "detail", "our_kill", "our_steps", "retail_steps", "targets_missing", "reward", "acquire"]
        fh.write("\t".join(cols).rstrip() + "\n")
        for r in rows:
            fh.write("\t".join(str(r[c]) for c in cols).rstrip() + "\n")

    print("our definitions:", len(ours_files))
    print("retail template entries:", len(templates))
    for key in sorted(stats):
        print(f"{key}: {stats[key]}")
    print("tsv:", tsv)
    return 0


if __name__ == "__main__":
    sys.exit(main())
