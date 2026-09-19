#!/usr/bin/env python3
"""审计击杀计数任务的 SECTION 合同（row-index family / counter-chain family）。

客户端（Aion 5.8 `Quest_unpacked/quest_monster.csv` + `Dialogs/quest_q*.html`）里 `SECTION_n`
有两个家族，2026-09-19 的残余复核确认二者必须分开判定：

1. **ROW_INDEX 家族**：存在 `SECTION_0==S` 门控。`SECTION_0` 是任务说明行索引，
   `SECTION_1..` 是当前行计数；最后一击杀满后必须把 `SECTION_0` 推进到报告行 `S+1`，
   否则服务端进入 `REWARD` 而客户端任务说明停在击杀行（15001/15041 症状）。
2. **COUNTER_CHAIN 家族**：没有 `SECTION_0==S`。`SECTION_0..N` 是*逐行*计数器，
   门控形如 `SECTION_k<1; SECTION_{k-1}==1`（链式：前一行打掉后一行才可见）或
   `SECTION_k<N; SECTION_5==0`（单行狩猎 + 阶段标志）。此处行推进由计数器本身驱动，
   不能再要求把行索引塞进 `SECTION_0` —— 那样反而把计数槽换成行号（QE-012 反例）。

审计同时解析 `<transition>`、`<kill-routes>`、`<counter>`、`<counter-grid>`、`<kill-chain>`
五类击杀载体，并把客户端怪物名经 npcs 表解析成 npc id 后与路线逐一对齐。
"""
from __future__ import annotations

import argparse
import csv
import re
import sys
from collections import defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = REPO_ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
CATALOG = REPO_ROOT / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"

SECTION_LT_RE = re.compile(r"SECTION_(\d+)\s*<\s*(\d+)")
SECTION_EQ_RE = re.compile(r"SECTION_(\d+)\s*==\s*(\d+)")


def executable_ids() -> set[int]:
    text = CATALOG.read_text(encoding="utf-8")
    return {int(m.group(1)) for m in re.finditer(r'<definition id="(\d+)"[^>]*mode="EXECUTABLE"', text)}


def client_records(csv_path: Path) -> dict[int, list[dict]]:
    """每任务的客户端进度行（保留原始门控/计数条件与怪物名）。"""
    records: dict[int, list[dict]] = defaultdict(list)
    with csv_path.open(encoding="utf-8-sig") as handle:
        for row in csv.DictReader(handle, skipinitialspace=True):
            progress = row["questProgress"] or ""
            if "SECTION_" not in progress:
                continue
            monsters = [tok.strip().lower() for tok in (row["monsters_gathers_npcs_list"] or "").split(",")
                        if tok.strip()]
            records[int(row["questId"])].append({
                "progress": progress,
                "gates": [(int(k), int(v)) for k, v in SECTION_EQ_RE.findall(progress)],
                "counters": {int(k): int(v) for k, v in SECTION_LT_RE.findall(progress)},
                "monsters": monsters,
            })
    return records


def load_npc_index(npcs_root: Path | None) -> dict[str, int]:
    """name_desc（小写）-> npc_id，用于把客户端怪物名对齐到服务端路线。"""
    index: dict[str, int] = {}
    if npcs_root is None or not npcs_root.is_dir():
        return index
    for path in sorted(npcs_root.glob("npc_template_*.xml")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for match in re.finditer(r"<npc_template\b[^>]*>", text):
            head = match.group(0)
            name = re.search(r'name_desc="([^"]+)"', head)
            npc_id = re.search(r'npc_id="(\d+)"', head)
            if name and npc_id:
                index.setdefault(name.group(1).lower(), int(npc_id.group(1)))
    return index


def html_steps(quest: int, dialog_root: Path | None):
    if dialog_root is None:
        return None, None
    for directory in sorted(p for p in dialog_root.glob("*") if p.is_dir()):
        path = directory / f"quest_q{quest}.html"
        if path.exists():
            text = path.read_text(encoding="utf-8", errors="ignore")
            summary = re.search(r'name="quest_summary".*?</HtmlPage>', text, re.S)
            if summary is None:
                return path, 0
            return path, len(re.findall(r"<step>", summary.group(0)))
    return None, None


def parse_node_vars(text: str) -> dict[str, dict]:
    nodes: dict[str, dict] = {}
    # 自闭合节点（<node .../>）必须显式识别：否则不跨节点的正则会把下一个
    # 节点的 <var> 张冠李戴给自闭合节点（13758/19631 家族的 reward 投影曾因此丢失）。
    # Self-closing nodes need an explicit alternative or the cross-node regex would
    # misattribute the next node's <var> children (13758/19631 family reward projection).
    for match in re.finditer(r'<node label="(\w+)"[^>]*?(?:/>|>(.*?)</node>)', text, re.S):
        nodes[match.group(1)] = {
            "status": (re.search(r'status="(\w+)"', match.group(0)) or [None, None])[1],
            "vars": {mm.group(1): int(mm.group(2))
                     for mm in re.finditer(r'<var name="(\w+)" value="(\d+)"', match.group(2) or "")},
        }
    return nodes


def parse_progress(text: str) -> dict[str, dict]:
    fields: dict[str, dict] = {}
    block = re.search(r"<progress>(.*?)</progress>", text, re.S)
    if block is None:
        return fields
    for match in re.finditer(r"<bit-field\b[^>]*/>", block.group(1)):
        head = match.group(0)
        name = re.search(r'name="(\w+)"', head)
        offset = re.search(r'offset="(\d+)"', head)
        maximum = re.search(r'max="(\d+)"', head)
        if name and offset:
            fields[name.group(1)] = {
                "offset": int(offset.group(1)),
                "max": int(maximum.group(1)) if maximum else None,
            }
    return fields


def kill_ids(block: str) -> set[int]:
    ids: set[int] = set()
    for token in re.findall(r'<kill-npc npc-ids?="([^"]+)"', block):
        for part in token.split():
            if part.isdigit():
                ids.add(int(part))
    return ids


def parse_transitions(text: str):
    """显式 <transition> 块 + 4 类块语法（kill-routes/counter/counter-grid/kill-chain）。"""
    out = []
    for block in re.findall(r"<transition\b.*?</transition>", text, re.S):
        head = re.match(r"<transition\b[^>]*>", block).group(0)
        source = re.search(r'source="([^"]+)"', head)
        target = re.search(r'target="([^"]+)"', head)
        priority = re.search(r'priority="([^"]+)"', head)
        actions = [("set", mm.group(1), int(mm.group(2)))
                   for mm in re.finditer(r'<set-variable field="(\w+)" value="(\d+)"', block)]
        actions += [("increment", mm.group(1), int(mm.group(2)))
                    for mm in re.finditer(r'<increment-variable field="(\w+)" delta="(\d+)"', block)]
        npc_ids = kill_ids(block)
        out.append({
            "kind": "transition",
            "source": source.group(1) if source else None,
            "target": target.group(1) if target else None,
            "priority": int(priority.group(1)) if priority else None,
            "kill": bool(npc_ids),
            "npc_ids": npc_ids,
            "actions": actions,
            "refresh": 'mode="LEVEL_AND_VISIBILITY_REFRESH"' in block,
            "enter_world": "<enter-world/>" in block,
        })
    for match in re.finditer(r"<kill-routes\b[^>]*/>", text):
        head = match.group(0)
        source = re.search(r'source="([^"]+)"', head)
        target = re.search(r'target="([^"]+)"', head)
        npc_ids = {int(tok) for tok in (re.search(r'npc-ids="([^"]+)"', head).group(1).split()
                                        if re.search(r'npc-ids="([^"]+)"', head) else [])}
        out.append({
            "kind": "kill-routes",
            "source": source.group(1) if source else None,
            "target": target.group(1) if target else None,
            "priority": None,
            "kill": bool(npc_ids),
            "npc_ids": npc_ids,
            "actions": [],
            "refresh": False,
            "enter_world": False,
        })
    for block in re.findall(r"<counter\b.*?</counter>", text, re.S):
        head = re.match(r"<counter\b[^>]*>", block).group(0)
        source = re.search(r'source="([^"]+)"', head)
        target = re.search(r'target="([^"]+)"', head)
        field = re.search(r'field="(\w+)"', head)
        required = re.search(r'required="(\d+)"', head)
        npc_ids = kill_ids(block)
        out.append({
            "kind": "counter",
            "source": source.group(1) if source else None,
            "target": target.group(1) if target else None,
            "priority": None,
            "kill": bool(npc_ids),
            "npc_ids": npc_ids,
            "actions": [("increment", field.group(1), 1)] if field else [],
            "refresh": False,
            "enter_world": False,
            "counter_field": field.group(1) if field else None,
            "counter_required": int(required.group(1)) if required else None,
        })
    for block in re.findall(r"<counter-grid\b.*?</counter-grid>", text, re.S):
        for match in re.finditer(r'<dimension\b[^>]*/>', block):
            head = match.group(0)
            field = re.search(r'field="(\w+)"', head)
            required = re.search(r'required="(\d+)"', head)
            npc_ids = {int(tok) for tok in (re.search(r'npc-ids="([^"]+)"', head).group(1).split()
                                            if re.search(r'npc-ids="([^"]+)"', head) else [])}
            out.append({
                "kind": "counter-grid",
                "source": None,
                "target": None,
                "priority": None,
                "kill": bool(npc_ids),
                "npc_ids": npc_ids,
                "actions": [("set", field.group(1), int(required.group(1)))] if field and required else [],
                "refresh": False,
                "enter_world": False,
                "counter_field": field.group(1) if field else None,
                "counter_required": int(required.group(1)) if required else None,
            })
    for block in re.findall(r"<kill-chain\b.*?</kill-chain>", text, re.S):
        head = re.match(r"<kill-chain\b[^>]*>", block).group(0)
        nodes = (re.search(r'nodes="([^"]+)"', head).group(1).split()
                 if re.search(r'nodes="([^"]+)"', head) else [])
        npc_ids = kill_ids(block)
        out.append({
            "kind": "kill-chain",
            "source": nodes[0] if nodes else None,
            "target": nodes[-1] if nodes else None,
            "priority": None,
            "kill": bool(npc_ids),
            "npc_ids": npc_ids,
            "actions": [],
            "refresh": False,
            "enter_world": False,
            "chain_nodes": nodes,
        })
    return out


def route_touched_fields(route: dict) -> dict[str, int]:
    touched: dict[str, int] = {}
    for kind, field, amount in route["actions"]:
        if kind == "set":
            touched[field] = amount
        else:
            touched[field] = touched.get(field, 0) + amount
    return touched


def classify_shapes(records: list[dict]) -> dict:
    """判定客户端 SECTION 家族（row-index / counter-chain）。

    判别式：`SECTION_0` 若出现在 `<N` 计数条件里，它就是计数器本身（1102/18911/24153/24155 家族，
    链式或单行狩猎）；只有 `SECTION_0` 从不出现在 `<N` 里、且出现 `SECTION_0==S` 门控时，
    它才是任务说明行索引（15001/15101/25304/14252 家族）。
    """
    gates = {gate for record in records for gate in record["gates"]}
    counters: dict[int, int] = {}
    for record in records:
        for section, threshold in record["counters"].items():
            counters[section] = max(counters.get(section, 0), threshold)
    section_zero_is_counter = 0 in counters
    row_gates = sorted(value for section, value in gates if section == 0)
    stage_gates = sorted((section, value) for section, value in gates if section != 0)
    if row_gates and not section_zero_is_counter:
        stage = max(row_gates)
        return {
            "shape": "ROW_INDEX",
            "stage": stage,
            "report_row": stage + 1,
            "counters": counters,
            "stage_gates": stage_gates,
        }
    return {
        "shape": "COUNTER_CHAIN",
        "stage": None,
        "report_row": None,
        "counters": counters,
        "stage_gates": stage_gates,
    }


def audit_quest(quest: int, records: list[dict], npc_index: dict[str, int],
                html_step_count: int | None) -> dict:
    text = (QUEST_DIR / f"{quest}.xml").read_text(encoding="utf-8")
    nodes = parse_node_vars(text)
    fields = parse_progress(text)
    transitions = parse_transitions(text)
    shape = classify_shapes(records)
    if shape["shape"] == "ROW_INDEX" and html_step_count:
        # 客户端任务说明的最后一行才是报告行（25304/25604 这类击杀行后面还有中间行）。
        shape["report_row"] = html_step_count - 1
    counters = shape["counters"]
    reward_vars = (nodes.get("reward", {}) or {}).get("vars") or {}

    kill_routes = [route for route in transitions if route["kill"]]
    monster_ids: dict[int, set[int]] = {}
    unresolved: set[str] = set()
    for record in records:
        for section, _threshold in record["counters"].items():
            for name in record["monsters"]:
                npc_id = npc_index.get(name)
                if npc_id is None:
                    unresolved.add(name)
                else:
                    monster_ids.setdefault(section, set()).add(npc_id)

    result = {
        "quest": quest,
        "shape": shape["shape"],
        "stage": shape["stage"],
        "report_row": shape["report_row"],
        "counters": ";".join(f"SECTION_{k}<{v}" for k, v in sorted(counters.items())),
        "stage_gates": ";".join(f"SECTION_{k}=={v}" for k, v in shape["stage_gates"]),
        "reward_projection": ";".join(f"{k}={v}" for k, v in sorted(reward_vars.items())),
        "kill_route_kinds": ";".join(sorted({route["kind"] for route in kill_routes})),
        "kill_route_targets": ";".join(sorted({route["target"] for route in kill_routes if route["target"]})),
        "migration_repair": any(route["source"] is None and route["target"] == "reward"
                                and route["enter_world"] for route in transitions),
        "field_alignment_problems": "",
        "monster_alignment_problems": "",
        "noop_kill_routes": "",
        "unresolved_monsters": "",
        "verdict": "",
    }

    problems = []
    for section in sorted(set(counters) | {section for section, _ in shape["stage_gates"]}):
        field = fields.get(f"var{section}")
        if field is None:
            if section in counters:
                problems.append(f"var{section} missing")
            continue
        if field["offset"] != 6 * section:
            problems.append(f"var{section}@{field['offset']}!=SECTION_{section}")
    result["field_alignment_problems"] = ",".join(problems)

    def route_fields(route: dict) -> dict[str, int]:
        """路线实际写入的字段：事务动作 + 目标节点投影（kill-chain / 网格节点靠投影推进）。"""
        touched = dict(route_touched_fields(route))
        target = nodes.get(route.get("target") or "", {})
        for field, value in (target.get("vars") or {}).items():
            touched.setdefault(field, value)
        return touched

    monster_problems = []
    # 计数归属按“客户端同一条 count 记录里的怪名集合”整组核对：XML 一条路线可以
    # 覆盖多组（13758/15546 家族把同一路线拆成多条计数记录），单条路线也可能带
    # 多个 npc-id，因此按 npc 集合相交做覆盖判定，而不是要求逐组精确相等。
    # Coverage is checked group by group against the client count record; a single XML
    # route may cover several client groups, so intersect rather than require equality.
    for section, npc_ids in sorted(monster_ids.items()):
        expect = f"var{section}" if shape["shape"] == "COUNTER_CHAIN" else "var0"
        covering = [route for route in kill_routes if route["npc_ids"] & npc_ids]
        if not covering:
            monster_problems.append(f"{sorted(npc_ids)} has no kill route")
            continue
        owners = {route.get("counter_field") for route in covering}
        touched = {field for route in covering for field in route_fields(route)}
        if expect not in owners and expect not in touched:
            monster_problems.append(f"{sorted(npc_ids)} not counted on {expect}")
    result["monster_alignment_problems"] = ",".join(monster_problems)
    result["unresolved_monsters"] = ",".join(sorted(unresolved))

    real_kills = [route for route in kill_routes if route["kind"] != "kill-routes"]
    if any(route["kind"] == "kill-routes" for route in kill_routes) and not real_kills:
        result["noop_kill_routes"] = "kill-routes-only"

    if shape["shape"] == "ROW_INDEX":
        stage = shape["stage"]
        report_row = shape["report_row"]
        # 节点投影缺 var0 等价于投影 0（bit-field 已声明，未写即保持 0），
        # 不能因为 <node/> 自闭合就把第 0 行的承载节点漏掉。
        # A missing var0 projection means 0 (the bit-field defaults to 0), so a
        # self-closing <node/> still carries stage 0.
        stage_nodes = [label for label, node in nodes.items()
                       if node["status"] == "START" and node["vars"].get("var0", 0) == stage]
        # 行索引是否落地有两种等价写法：
        #   1. reward 投影写 var0 == 报告行（planner 的跨节点补投影路径）；
        #   2. reward 投影不写 var0，但存在一条进入 reward 的转换在事务动作里
        #      显式 set var0 == 报告行（planner 不会覆盖动作已触及的字段）。
        # Row index closure has two equivalent spellings: the reward projection
        # carries var0 == report row, or an incoming reward transition explicitly
        # sets var0 == report row while the reward projection leaves it untouched.
        stage_kills = [route for route in real_kills if route["source"] in stage_nodes]
        reward_edge_sets_report_row = any(
            route["target"] == "reward"
            and route_fields(route).get("var0") == report_row
            and "var0" not in reward_vars
            for route in transitions)
        closed = (bool(stage_nodes) and not monster_problems
                  and (reward_vars.get("var0") == report_row
                       or reward_edge_sets_report_row))
        if closed:
            result["verdict"] = ("ROW_INDEX_CLOSED" if reward_vars.get("var0") == report_row
                                 else "ROW_INDEX_EDGE_CLOSED")
        return result

    saturated = all(reward_vars.get(f"var{section}") == threshold
                    for section, threshold in counters.items())
    report_route = any(route["target"] == "reward" and not route["kill"] for route in transitions)
    ok = (not problems and not monster_problems and (saturated or report_route))
    result["verdict"] = "COUNTER_CHAIN_OK" if ok else "COUNTER_CHAIN_GAP"
    return result


def legacy_writes(quest: int, legacy_root: Path | None):
    if legacy_root is None:
        return None, []
    hits = sorted(legacy_root.rglob(f"_{quest}*.java"))
    if not hits:
        return None, []
    text = "\n".join(path.read_text(encoding="utf-8", errors="ignore") for path in hits)
    writes = re.findall(r"setQuestVarById\(\s*0\s*,\s*([^)]+)\)", text)
    writes += re.findall(r"setQuestVar\(\s*0\s*,\s*([^)]+)\)", text)
    writes += re.findall(r"setQuestVar\(\s*([^,()]+)\s*\)", text)
    for current, following in re.findall(r"changeQuestStep\(\s*[^,]+,\s*([^,()]+),\s*([^,()]+)", text):
        current, following = current.strip(), following.strip()
        if current == following and current.isdigit():
            continue
        writes.append(following)
    return hits[0].name, writes


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--client-quest-csv", required=True, type=Path)
    parser.add_argument("--client-dialog-root", type=Path)
    parser.add_argument("--legacy-handler-root", type=Path)
    parser.add_argument("--npcs-root", type=Path,
                        default=REPO_ROOT / "src/main/resources/aion/data/static_data/npcs")
    parser.add_argument("--out-csv", type=Path)
    args = parser.parse_args()

    executables = executable_ids()
    records = client_records(args.client_quest_csv)
    npc_index = load_npc_index(args.npcs_root)
    closed_counts = defaultdict(int)
    closed_ids: dict[str, list[int]] = {}
    rows = []
    for quest, quest_records in sorted(records.items()):
        if quest not in executables:
            continue
        path, steps = html_steps(quest, args.client_dialog_root)
        info = audit_quest(quest, quest_records, npc_index, steps)
        if info.get("verdict") in ("ROW_INDEX_CLOSED", "ROW_INDEX_EDGE_CLOSED"):
            closed_counts[info["verdict"]] += 1
            closed_ids.setdefault(info["verdict"], []).append(quest)
            continue
        legacy_name, writes = legacy_writes(quest, args.legacy_handler_root)
        verdict = info.get("verdict")
        if info["shape"] == "ROW_INDEX":
            if legacy_name and any(w.strip().isdigit() and int(w.strip()) > 0 for w in writes):
                verdict = "SAME_CLASS_CONFIRMED"
            elif legacy_name:
                verdict = "REVIEW_LEGACY_NO_VAR0"
            else:
                verdict = "REVIEW_NO_LEGACY"
        elif not verdict:
            verdict = "REVIEW_NO_LEGACY"
        row = dict(info)
        row["verdict"] = verdict
        row["html_steps"] = steps
        row["html_file"] = path.name if path else ""
        row["legacy_handler"] = legacy_name or ""
        row["legacy_var0_writes"] = ";".join(writes)
        rows.append(row)

    counts = defaultdict(int)
    for row in rows:
        counts[row["verdict"]] += 1
    print(f"executable quests: {len(executables)}")
    for verdict in sorted(closed_counts):
        ids = sorted(set(closed_ids[verdict]))
        print(f"{verdict} (closed, {len(ids)}): {' '.join(str(i) for i in ids)}")
    print(f"residual rows: {len(rows)} -> {dict(sorted(counts.items()))}")
    for verdict in sorted(counts):
        ids = [row["quest"] for row in rows if row["verdict"] == verdict]
        print(f"{verdict} ({len(ids)}): {' '.join(str(i) for i in sorted(set(ids)))}")

    if args.out_csv:
        args.out_csv.parent.mkdir(parents=True, exist_ok=True)
        with args.out_csv.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
            writer.writeheader()
            writer.writerows(rows)
        print(f"wrote {args.out_csv}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
