#!/usr/bin/env python3
"""Generate the stage/recovery evidence matrix for production instances."""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path

import generate_retail_instance_door_matrix as door_matrix
import generate_retail_instance_ownership_matrix as ownership_matrix


COVERAGE = Path("src/main/resources/aion/definitions/compact/instance/coverage.xml")
CONDITION_SPAWNS = Path("src/main/resources/aion/definitions/compact/ai/condition-spawns.xml")
NPC_AI = Path("src/main/resources/aion/definitions/compact/ai/npc-ai.xml")
INSTANCE_SPAWNS = Path("src/main/resources/aion/data/static_data/spawns/Instances")
HANDLERS = Path("src/main/java/com/aionemu/gameserver/instance/handlers/scripts")
LEGACY_AI = Path("src/main/java/com/aionemu/gameserver/ai")
NPC_TEMPLATES = Path("src/main/resources/aion/data/static_data/npcs/npc_template.xml")
REPORT = Path("docs/RETAIL_INSTANCE_STAGE_RECOVERY_MATRIX.json")
DISABLED_WORLD = 300260000
INSTANCE_ID = re.compile(r"\b\d{9}\b")
CLASS = re.compile(r"\b(?:public\s+)?(?:abstract\s+)?class\s+(\w+)(?:\s+extends\s+(\w+))?")
METHOD = re.compile(r"(?:public|protected|private)\s+(?:static\s+)?[\w<>\[\]?]+\s+(\w+)\s*\(")
SPAWN = re.compile(r"\bspawn(?:PartyMember)?\(\s*(\d{6})\s*,")
STATE_CALL = re.compile(r"\b(?:runtimeState|getRuntimeState)\(\)\.(\w+)\s*\(")
STATE_LITERAL = re.compile(r"\b(?:runtimeState|getRuntimeState)\(\)\.\w+\s*\(\s*\"([^\"]+)\"")
STATE_PREFIX = re.compile(r"\b(?:STATE|STATE_PREFIX)\s*=\s*\"([^\"]+)\"")
DEADLINE = re.compile(r"\bscheduleDeadline\(\s*\"([^\"]+)\"")
STAGE_CALLBACKS = {
    "onInstanceCreate", "onPlayerLogin", "onPlayerLogOut", "onEnterInstance", "onLeaveInstance",
    "onEnterZone", "onLeaveZone", "onOpenDoor", "onPlayMovieEnd", "onReviveEvent", "onDie",
    "onChangeStage", "getStage", "onDropRegistered", "handleUseItemFinish", "onRetailPortalAction",
}
PATTERN_STAGE_OPERATIONS = {
    "set_condition_spawn_variable", "set_condition_spawn_variable_to_world", "change_world_scene_status",
}
PATTERN_TIMER_OPERATIONS = {"add_battle_timer", "set_idle_timer"}


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def production_worlds(root: Path) -> dict[int, dict[str, object]]:
    worlds = {}
    for node in ET.parse(root / COVERAGE).getroot().findall("world"):
        world_id = int(node.get("id", "0"))
        if world_id == DISABLED_WORLD:
            continue
        owners = ownership_matrix.dimension_owners(node.get("dimension_owners", ""), world_id)
        worlds[world_id] = {
            "local_name": node.get("local_name", ""),
            "retail_name": node.get("retail_name", ""),
            "classification": node.get("classification", ""),
            "declared_stage_owner": owners["stage"],
            "declared_recovery_owner": owners["recovery"],
        }
    if len(worlds) != 139:
        raise ValueError(f"expected 139 production worlds, found {len(worlds)}")
    return worlds


def literal_calls(text: str, method: str) -> list[dict[str, object]]:
    result = []
    call = re.compile(rf"RetailConditionSpawnEngine\.{method}\s*\((.*?)\);", re.DOTALL)
    for match in call.finditer(text):
        values = re.findall(r'\"([^\"]+)\"', match.group(1))
        result.append({
            "mechanism": method,
            "variable": values[0] if values else "",
            "target_world_id": int(match.group(1).split(",", 1)[0].strip())
            if method == "setVariableToWorld" and match.group(1).split(",", 1)[0].strip().isdigit() else None,
            "line": line_number(text, match.start()),
        })
    return result


@dataclass(frozen=True)
class JavaClass:
    name: str
    parent: str
    source: str
    world_ids: frozenset[int]
    features: dict[str, object]


def java_class(path: Path, root: Path) -> JavaClass | None:
    text = path.read_text(encoding="utf-8")
    declaration = CLASS.search(text)
    if declaration is None:
        return None
    world_ids = set()
    for annotation in re.findall(r"@InstanceID\s*\((.*?)\)", text, re.DOTALL):
        world_ids.update(int(value) for value in INSTANCE_ID.findall(annotation))
    methods = sorted({match.group(1) for match in METHOD.finditer(text)} & STAGE_CALLBACKS)
    condition_writes = literal_calls(text, "setVariable") + literal_calls(text, "setVariableToWorld")
    for row in condition_writes:
        row["source"] = str(path.relative_to(root))
    state_calls = Counter(match.group(1) for match in STATE_CALL.finditer(text))
    deadlines = [{
        "name": match.group(1),
        "line": line_number(text, match.start()),
        "source": str(path.relative_to(root)),
    } for match in DEADLINE.finditer(text)]
    features = {
        "callbacks": methods,
        "condition_writes": condition_writes,
        "stage_types": sorted(set(re.findall(r"\bStageType\.([A-Z0-9_]+)", text))),
        "score_states": sorted(set(re.findall(r"\bInstanceScoreType\.([A-Z0-9_]+)", text))),
        "state_calls": dict(sorted(state_calls.items())),
        "state_keys": sorted(set(STATE_LITERAL.findall(text))),
        "state_prefixes": sorted(set(STATE_PREFIX.findall(text))),
        "deadlines": deadlines,
        "volatile_schedules": text.count("GameThreadPoolServices.threadPoolManager().schedule"),
        "future_fields": len(re.findall(r"\bFuture\s*<", text)),
        "spawn_npc_ids": sorted({int(value) for value in SPAWN.findall(text)}),
    }
    return JavaClass(declaration.group(1), declaration.group(2) or "", str(path.relative_to(root)),
                     frozenset(world_ids), features)


def merge_features(lineage: list[JavaClass]) -> dict[str, object]:
    callbacks = set()
    condition_writes = []
    stage_types = set()
    score_states = set()
    state_calls = Counter()
    state_keys = set()
    state_prefixes = set()
    deadlines = []
    spawn_npc_ids = set()
    volatile_schedules = 0
    future_fields = 0
    for item in lineage:
        features = item.features
        callbacks.update(features["callbacks"])
        condition_writes.extend(features["condition_writes"])
        stage_types.update(features["stage_types"])
        score_states.update(features["score_states"])
        state_calls.update(features["state_calls"])
        state_keys.update(features["state_keys"])
        state_prefixes.update(features["state_prefixes"])
        deadlines.extend(features["deadlines"])
        spawn_npc_ids.update(features["spawn_npc_ids"])
        volatile_schedules += int(features["volatile_schedules"])
        future_fields += int(features["future_fields"])
    return {
        "lineage": [item.name for item in lineage],
        "sources": [item.source for item in lineage],
        "callbacks": sorted(callbacks),
        "condition_writes": sorted(condition_writes,
                                   key=lambda row: (str(row["source"]), int(row["line"]), str(row["mechanism"]))),
        "stage_types": sorted(stage_types),
        "score_states": sorted(score_states),
        "state_calls": dict(sorted(state_calls.items())),
        "state_keys": sorted(state_keys),
        "state_prefixes": sorted(state_prefixes),
        "deadlines": sorted(deadlines, key=lambda row: (str(row["source"]), int(row["line"]))),
        "volatile_schedules": volatile_schedules,
        "future_fields": future_fields,
        "spawn_npc_ids": sorted(spawn_npc_ids),
    }


def handler_models(root: Path, worlds: set[int]) -> tuple[dict[int, dict[str, object]], dict[int, set[int]]]:
    classes = {}
    for path in sorted((root / HANDLERS).rglob("*.java")):
        item = java_class(path, root)
        if item is not None:
            classes[item.name] = item
    _owners, _controls, denials = door_matrix.handlers(root, worlds)
    result = {}
    for item in classes.values():
        if not item.world_ids:
            continue
        lineage = [item]
        seen = {item.name}
        parent = item.parent
        while parent in classes and parent not in seen:
            seen.add(parent)
            ancestor = classes[parent]
            lineage.append(ancestor)
            parent = ancestor.parent
        model = merge_features(lineage)
        model["handler"] = item.source
        for world_id in item.world_ids & worlds:
            if world_id in result:
                raise ValueError(f"duplicate instance handlers for {world_id}")
            result[world_id] = model
    return result, denials


def expression_variables(expression: str) -> set[str]:
    return {value.casefold() for value in re.findall(r"[A-Za-z_][A-Za-z0-9_]*", expression)
            if value.casefold() not in {"true", "false", "and", "or"}}


def condition_models(root: Path, worlds: set[int]) -> dict[int, dict[str, object]]:
    result = {}
    for world in ET.parse(root / CONDITION_SPAWNS).getroot().findall("world"):
        world_id = int(world.get("id", "0"))
        if world_id not in worlds:
            continue
        declared = {node.get("name", "").casefold(): node.get("name", "") for node in world.findall("variable")}
        consumers: dict[str, dict[str, object]] = defaultdict(lambda: {"condition_ids": [], "sources": set()})
        sources = set()
        timed_npcs = 0
        conditions = world.findall("condition")
        for condition in conditions:
            source = condition.get("source", "")
            if source:
                sources.add(source)
            for variable in expression_variables(condition.get("expression", "")):
                consumers[variable]["condition_ids"].append(int(condition.get("id", "0")))
                if source:
                    consumers[variable]["sources"].add(source)
            timed_npcs += sum(any(int(npc.get(field, "0")) > 0 for field in
                                  ("initial_delay", "initial_delay_extra", "life", "respawn_time"))
                              for npc in condition.findall(".//npc"))
        names = sorted(set(declared) | set(consumers))
        result[world_id] = {
            "world_name": world.get("name", ""),
            "condition_count": len(conditions),
            "timed_npc_count": timed_npcs,
            "sources": sorted(sources),
            "variables": [{
                "name": declared.get(name, name),
                "declared": name in declared,
                "condition_ids": sorted(set(consumers[name]["condition_ids"])),
                "sources": sorted(consumers[name]["sources"]),
            } for name in names],
        }
    return result


def leaf_fields(node: ET.Element) -> dict[str, str]:
    return {leaf.tag: (leaf.text or "").strip() for leaf in node.iter() if leaf is not node and len(leaf) == 0}


def pattern_models(root: Path) -> tuple[dict[str, dict[str, object]], dict[str, set[int]], dict[int, dict[str, str]]]:
    patterns = {}
    for path in sorted((root / door_matrix.AI_DIRECTORY).glob("npcaipatterns*.xml")):
        for pattern in ET.parse(path).getroot().iter("npc_ai_pattern"):
            name = pattern.findtext("name", "").strip()
            operations = []
            spawns = []
            events = pattern.find("event_handlers")
            if events is not None:
                for event in events:
                    for rule in event.findall("pattern"):
                        priority = int(rule.findtext("priority", "0"))
                        for container_name in ("conditions", "actions"):
                            container = rule.find(container_name)
                            if container is None:
                                continue
                            for operation in container:
                                if operation.tag == "spawn":
                                    npc_name = operation.findtext("npc_nameid", "").strip()
                                    if npc_name:
                                        spawns.append({
                                            "event": event.tag,
                                            "priority": priority,
                                            "npc_name": npc_name,
                                        })
                                if operation.tag in PATTERN_STAGE_OPERATIONS | PATTERN_TIMER_OPERATIONS:
                                    operations.append({
                                        "event": event.tag,
                                        "priority": priority,
                                        "container": container_name,
                                        "operation": operation.tag,
                                        "fields": leaf_fields(operation),
                                    })
            if operations or spawns:
                patterns[name.casefold()] = {
                    "name": name,
                    "source": str(path.relative_to(root)),
                    "operations": operations,
                    "spawns": spawns,
                }

    mappings = {}
    names: dict[str, set[int]] = defaultdict(set)
    for node in ET.parse(root / NPC_AI).getroot().findall("npc"):
        npc_id = int(node.get("id", "0"))
        ai = node.get("ai", "")
        key = ai.casefold()
        pattern = patterns.get(key) or patterns.get(door_matrix.VERSION_SUFFIX.sub("", key))
        names[node.get("name", "").casefold()].add(npc_id)
        if pattern is not None:
            mappings[npc_id] = {
                "npc_name": node.get("name", ""),
                "mapped_pattern": ai,
                "pattern_key": pattern["name"].casefold(),
            }
    return patterns, names, mappings


def direct_reachability(root: Path, worlds: set[int], handlers: dict[int, dict[str, object]]) -> dict[tuple[int, int], set[str]]:
    result: dict[tuple[int, int], set[str]] = defaultdict(set)
    for path in sorted((root / INSTANCE_SPAWNS).glob("*.xml")):
        for spawn_map in ET.parse(path).getroot().iter("spawn_map"):
            world_id = int(spawn_map.get("map_id", "0"))
            if world_id not in worlds:
                continue
            for spawn in spawn_map.iter("spawn"):
                result[(world_id, int(spawn.get("npc_id", "0")))].add(f"static:{path.relative_to(root)}")
    for world in ET.parse(root / CONDITION_SPAWNS).getroot().findall("world"):
        world_id = int(world.get("id", "0"))
        if world_id not in worlds:
            continue
        for condition in world.findall("condition"):
            source = condition.get("source", str(CONDITION_SPAWNS))
            for npc in condition.findall(".//npc"):
                result[(world_id, int(npc.get("id", "0")))].add(f"condition:{source}")
    for world_id, handler in handlers.items():
        for npc_id in handler["spawn_npc_ids"]:
            result[(world_id, int(npc_id))].add(f"handler:{handler['handler']}")
    return result


def expand_pattern_spawns(reachable: dict[tuple[int, int], set[str]], patterns: dict[str, dict[str, object]],
                          names: dict[str, set[int]], mappings: dict[int, dict[str, str]]) -> list[dict[str, object]]:
    unresolved = []
    seen_gaps = set()
    changed = True
    while changed:
        changed = False
        for (world_id, npc_id), _sources in list(reachable.items()):
            mapping = mappings.get(npc_id)
            if mapping is None:
                continue
            pattern = patterns[mapping["pattern_key"]]
            for spawn in pattern["spawns"]:
                candidates = names.get(str(spawn["npc_name"]).casefold(), set())
                if len(candidates) != 1:
                    key = (world_id, pattern["name"], spawn["event"], spawn["npc_name"])
                    if key not in seen_gaps:
                        seen_gaps.add(key)
                        unresolved.append({
                            "world_id": world_id,
                            "pattern": pattern["name"],
                            "pattern_source": pattern["source"],
                            **spawn,
                            "candidate_npc_ids": sorted(candidates),
                            "status": "REJECT_MISSING_PATTERN_SPAWN" if not candidates
                            else "REJECT_AMBIGUOUS_PATTERN_SPAWN",
                        })
                    continue
                target = next(iter(candidates))
                key = (world_id, target)
                source = f"pattern:{pattern['name']}:{spawn['event']}"
                if source not in reachable[key]:
                    reachable[key].add(source)
                    changed = True
    return sorted(unresolved, key=lambda row: (int(row["world_id"]), str(row["pattern"]), str(row["npc_name"])))


def legacy_stage_ais(root: Path) -> dict[str, dict[str, object]]:
    result = {}
    for path in sorted((root / LEGACY_AI).rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        ai_name = re.search(r'@AIName\("([^\"]+)"\)', text)
        if ai_name is None:
            continue
        writes = literal_calls(text, "setVariable") + literal_calls(text, "setVariableToWorld")
        scene_changes = len(re.findall(r"\.onChangeStage\s*\(", text))
        if not writes and not scene_changes:
            continue
        for row in writes:
            row["source"] = str(path.relative_to(root))
        key = ai_name.group(1).casefold()
        if key in result:
            raise ValueError(f"duplicate legacy stage AI name: {ai_name.group(1)}")
        result[key] = {
            "ai": ai_name.group(1),
            "source": str(path.relative_to(root)),
            "condition_writes": writes,
            "scene_changes": scene_changes,
        }
    return result


def legacy_npc_mappings(root: Path, ais: dict[str, dict[str, object]]) -> dict[int, dict[str, object]]:
    result = {}
    for _event, node in ET.iterparse(root / NPC_TEMPLATES, events=("end",)):
        if node.tag == "npc_template":
            owner = ais.get(node.get("ai", "").casefold())
            if owner is not None:
                result[int(node.get("npc_id", "0"))] = {
                    "npc_name": node.get("name", ""),
                    "ai": owner["ai"],
                    "owner": owner,
                }
        node.clear()
    return result


def stage_classification(world: dict[str, object]) -> str:
    owner = str(world["declared_stage_owner"])
    handler = world["handler_model"]
    patterns = [binding for binding in world["pattern_bindings"] if binding["selection"] == "ACTIVE"]
    has_pattern_stage = any(operation["operation"] in PATTERN_STAGE_OPERATIONS
                            for binding in patterns for operation in binding["operations"])
    has_handler_stage = bool(handler and (handler["condition_writes"] or handler["stage_types"]
                                          or handler["score_states"] or "onChangeStage" in handler["callbacks"]
                                          or "getStage" in handler["callbacks"]))
    if owner == "HANDLER":
        if not handler:
            return "REJECT_MISSING_HANDLER"
        if has_handler_stage and handler["condition_writes"] and not (handler["stage_types"] or handler["score_states"]):
            return "HANDLER_CONDITION_BRIDGE"
        return "HANDLER_STATE_MACHINE" if has_handler_stage else "HANDLER_EVENT_FLOW"
    if owner == "RETAIL_PATTERN":
        return "RETAIL_PATTERN_MODELED" if has_pattern_stage else "REJECT_MISSING_PATTERN_STAGE_SIGNAL"
    if owner == "NOT_APPLICABLE":
        return "NOT_APPLICABLE" if not has_handler_stage and not has_pattern_stage else "DECLARED_NOT_APPLICABLE_CONTRADICTION"
    return "EXTERNAL_OWNER_WITH_STATIC_EVIDENCE" if has_pattern_stage or world["condition_model"] else "EXTERNAL_OWNER"


def recovery_classification(world: dict[str, object]) -> str:
    owner = str(world["declared_recovery_owner"])
    handler = world["handler_model"]
    persistent = bool(handler and (handler["state_calls"] or handler["deadlines"]))
    volatile = bool(handler and (handler["volatile_schedules"] or handler["future_fields"]))
    pattern_timers = any(operation["operation"] in PATTERN_TIMER_OPERATIONS
                         for binding in world["pattern_bindings"] if binding["selection"] == "ACTIVE"
                         for operation in binding["operations"])
    if owner == "HANDLER":
        if not handler:
            return "REJECT_MISSING_HANDLER"
        if volatile:
            return "REJECT_VOLATILE_HANDLER_STATE"
        if persistent:
            return "HANDLER_USES_PERSISTENT_STATE"
        return "REJECT_UNMODELED_HANDLER_RECOVERY"
    if owner == "STATELESS":
        return "DECLARED_STATELESS_CONTRADICTION" if persistent or volatile or pattern_timers else "STATELESS_RUNTIME_SHAPE"
    if owner == "RETAIL_DATA":
        return "PERSISTENT_RETAIL_RUNTIME" if world["condition_model"] or pattern_timers else "REJECT_MISSING_RETAIL_RECOVERY_SIGNAL"
    return "EXTERNAL_RECOVERY_OWNER"


def conversion_status(world: dict[str, object]) -> str:
    if world["stage_classification"].startswith("REJECT_") or world["recovery_classification"].startswith("REJECT_"):
        return "REJECT_EVIDENCE_GAP"
    if world["declared_stage_owner"] == "HANDLER" or world["declared_recovery_owner"] == "HANDLER":
        return "RETAIN_HANDLER"
    return "ALREADY_EXTERNAL_OWNER"


def evidence_gaps(world: dict[str, object]) -> list[str]:
    gaps = []
    if world["declared_stage_owner"] == "HANDLER":
        gaps.append("scriptdll_stage_semantics_not_compiled")
    if world["stage_classification"] == "REJECT_MISSING_PATTERN_STAGE_SIGNAL":
        gaps.append("missing_retail_pattern_stage_signal")
    if world["recovery_classification"] == "REJECT_VOLATILE_HANDLER_STATE":
        gaps.append("volatile_handler_schedule")
    if world["recovery_classification"] == "REJECT_UNMODELED_HANDLER_RECOVERY":
        gaps.append("missing_handler_recovery_state_model")
    if world["recovery_classification"] == "DECLARED_STATELESS_CONTRADICTION":
        gaps.append("declared_stateless_has_runtime_state")
    condition = world["condition_model"]
    if condition:
        gaps.extend(f"missing_condition_variable_producer:{variable['name']}" for variable in condition["variables"]
                    if variable["producer_status"] != "RESOLVED")
    gaps.extend(f"{row['status'].casefold()}:{row['pattern']}:{row['npc_name']}"
                for row in world["pattern_spawn_gaps"])
    return sorted(set(gaps))


def deletion_conditions(world: dict[str, object]) -> list[str]:
    conditions = []
    if world["declared_stage_owner"] == "HANDLER":
        conditions.append("compile every handler stage trigger and transition from retail evidence into existing consumers")
    if world["declared_recovery_owner"] == "HANDLER":
        conditions.append("persist and restore every stage state and absolute deadline before removing handler recovery")
    if any(gap == "volatile_handler_schedule" for gap in world["evidence_gaps"]):
        conditions.append("replace volatile handler schedules with InstanceDeadlineScheduler")
    if any(gap.startswith("missing_condition_variable_producer:") for gap in world["evidence_gaps"]):
        conditions.append("resolve every consumed condition variable to a reachable retail or retained runtime producer")
    if world["pattern_spawn_gaps"]:
        conditions.append("resolve every transitive Pattern spawn target")
    return conditions


def attach_batches(matrix: list[dict[str, object]]) -> list[dict[str, object]]:
    grouped: dict[str, list[dict[str, object]]] = defaultdict(list)
    signatures = {}
    for world in matrix:
        handler = world["handler_model"]
        signature = {
            "declared_stage_owner": world["declared_stage_owner"],
            "declared_recovery_owner": world["declared_recovery_owner"],
            "stage_classification": world["stage_classification"],
            "recovery_classification": world["recovery_classification"],
            "conversion_status": world["conversion_status"],
            "has_condition_model": world["condition_model"] is not None,
            "handler_callbacks": [] if handler is None else handler["callbacks"],
            "has_pattern_stage": any(operation["operation"] in PATTERN_STAGE_OPERATIONS
                                     for binding in world["pattern_bindings"]
                                     for operation in binding["operations"]),
        }
        key = json.dumps(signature, sort_keys=True, separators=(",", ":"))
        signatures[key] = signature
        grouped[key].append(world)
    batches = []
    for index, key in enumerate(sorted(grouped), 1):
        batch_id = f"B{index:03d}"
        group = grouped[key]
        for world in group:
            world["batch_id"] = batch_id
        batches.append({
            "id": batch_id,
            "count": len(group),
            "signature": signatures[key],
            "world_ids": [world["world_id"] for world in group],
        })
    return batches


def build(root: Path) -> dict[str, object]:
    world_data = production_worlds(root)
    world_ids = set(world_data)
    handlers, denials = handler_models(root, world_ids)
    conditions = condition_models(root, world_ids)
    patterns, names, mappings = pattern_models(root)
    reachable = direct_reachability(root, world_ids, handlers)
    spawn_gaps = expand_pattern_spawns(reachable, patterns, names, mappings)
    legacy_ais = legacy_stage_ais(root)
    legacy_mappings = legacy_npc_mappings(root, legacy_ais)
    retail_npcs = door_matrix.retail_mapped_npcs(root)

    pattern_by_world: dict[int, list[dict[str, object]]] = defaultdict(list)
    legacy_by_world: dict[int, list[dict[str, object]]] = defaultdict(list)
    for (world_id, npc_id), sources in sorted(reachable.items()):
        mapping = mappings.get(npc_id)
        if mapping is not None:
            pattern = patterns[mapping["pattern_key"]]
            if pattern["operations"]:
                pattern_by_world[world_id].append({
                    "npc_id": npc_id,
                    "npc_name": mapping["npc_name"],
                    "mapped_pattern": mapping["mapped_pattern"],
                    "pattern": pattern["name"],
                    "pattern_key": mapping["pattern_key"],
                    "pattern_source": pattern["source"],
                    "reachability_sources": sorted(sources),
                    "selection": "REJECT_HANDLER_PATTERN" if npc_id in denials[world_id] else "ACTIVE",
                    "operations": pattern["operations"],
                })
        legacy = legacy_mappings.get(npc_id)
        if legacy is not None:
            legacy_by_world[world_id].append({
                "npc_id": npc_id,
                "npc_name": legacy["npc_name"],
                "ai": legacy["ai"],
                "reachability_sources": sorted(sources),
                "selection": "LEGACY_AI_OWNER" if npc_id in denials[world_id] or npc_id not in retail_npcs
                else "ACTIVE_ON_RETAIL_PATTERN_FALLBACK",
                **legacy["owner"],
            })

    cross_world_producers: dict[int, dict[str, list[dict[str, object]]]] = defaultdict(lambda: defaultdict(list))
    for source_world_id, handler in handlers.items():
        for write in handler["condition_writes"]:
            target = write["target_world_id"]
            if write["mechanism"] == "setVariableToWorld" and target in world_ids and write["variable"]:
                cross_world_producers[int(target)][str(write["variable"]).casefold()].append({
                    "owner": "HANDLER", "source_world_id": source_world_id, **write,
                })
    for source_world_id, bindings in pattern_by_world.items():
        for binding in bindings:
            if binding["selection"] != "ACTIVE":
                continue
            for operation in binding["operations"]:
                if operation["operation"] != "set_condition_spawn_variable_to_world":
                    continue
                target = operation["fields"].get("worldid", "")
                variable = operation["fields"].get("string", "")
                if target.isdigit() and int(target) in world_ids and variable:
                    cross_world_producers[int(target)][variable.casefold()].append({
                        "owner": "RETAIL_PATTERN", "source_world_id": source_world_id,
                        "npc_id": binding["npc_id"], "pattern": binding["pattern"], **operation,
                    })
    for source_world_id, bindings in legacy_by_world.items():
        for binding in bindings:
            if binding["selection"] != "LEGACY_AI_OWNER":
                continue
            for write in binding["condition_writes"]:
                target = write["target_world_id"]
                if write["mechanism"] == "setVariableToWorld" and target in world_ids and write["variable"]:
                    cross_world_producers[int(target)][str(write["variable"]).casefold()].append({
                        "owner": "LEGACY_AI", "source_world_id": source_world_id,
                        "npc_id": binding["npc_id"], "ai": binding["ai"], **write,
                    })

    matrix = []
    for world_id, metadata in sorted(world_data.items()):
        condition = conditions.get(world_id)
        handler = handlers.get(world_id)
        pattern_bindings = sorted(pattern_by_world.get(world_id, []),
                                  key=lambda row: (int(row["npc_id"]), str(row["pattern"])))
        legacy_bindings = sorted(legacy_by_world.get(world_id, []), key=lambda row: int(row["npc_id"]))
        producers: dict[str, list[dict[str, object]]] = defaultdict(list)
        for variable, rows in cross_world_producers.get(world_id, {}).items():
            producers[variable].extend(rows)
        if handler:
            for write in handler["condition_writes"]:
                if write["variable"] and write["target_world_id"] in {None, world_id}:
                    producers[str(write["variable"]).casefold()].append({"owner": "HANDLER", **write})
        for binding in pattern_bindings:
            if binding["selection"] != "ACTIVE":
                continue
            for operation in binding["operations"]:
                if operation["operation"] == "set_condition_spawn_variable":
                    variable = operation["fields"].get("string", "")
                    if variable:
                        producers[variable.casefold()].append({
                            "owner": "RETAIL_PATTERN", "npc_id": binding["npc_id"],
                            "pattern": binding["pattern"], **operation,
                        })
        for binding in legacy_bindings:
            if binding["selection"] != "LEGACY_AI_OWNER":
                continue
            for write in binding["condition_writes"]:
                if write["variable"] and write["target_world_id"] in {None, world_id}:
                    producers[str(write["variable"]).casefold()].append({
                        "owner": "LEGACY_AI", "npc_id": binding["npc_id"], "ai": binding["ai"], **write,
                    })
        if condition:
            for variable in condition["variables"]:
                variable["producers"] = sorted(producers.get(str(variable["name"]).casefold(), []),
                                                key=lambda row: (str(row["owner"]), int(row.get("npc_id", 0)),
                                                                 str(row.get("source", "")), int(row.get("line", 0))))
                variable["producer_status"] = "RESOLVED" if variable["producers"] else "REJECT_MISSING_PRODUCER"

        world = {
            "world_id": world_id,
            **metadata,
            "handler": "" if handler is None else handler["handler"],
            "handler_model": handler,
            "condition_model": condition,
            "pattern_bindings": pattern_bindings,
            "legacy_bindings": legacy_bindings,
            "pattern_spawn_gaps": [row for row in spawn_gaps if int(row["world_id"]) == world_id],
        }
        world["stage_classification"] = stage_classification(world)
        world["recovery_classification"] = recovery_classification(world)
        world["conversion_status"] = conversion_status(world)
        world["evidence_gaps"] = evidence_gaps(world)
        world["deletion_conditions"] = deletion_conditions(world)
        matrix.append(world)

    batches = attach_batches(matrix)
    summary = {
        "production_worlds": len(matrix),
        "batches": len(batches),
        "registered_handler_worlds": sum(bool(world["handler"]) for world in matrix),
        "condition_worlds": sum(world["condition_model"] is not None for world in matrix),
        "condition_variables": sum(len(world["condition_model"]["variables"]) for world in matrix
                                   if world["condition_model"]),
        "condition_variables_missing_producers": sum(
            variable["producer_status"] != "RESOLVED" for world in matrix if world["condition_model"]
            for variable in world["condition_model"]["variables"]),
        "pattern_bindings": sum(len(world["pattern_bindings"]) for world in matrix),
        "pattern_stage_bindings": sum(any(operation["operation"] in PATTERN_STAGE_OPERATIONS
                                          for operation in binding["operations"])
                                      for world in matrix for binding in world["pattern_bindings"]),
        "pattern_timer_bindings": sum(any(operation["operation"] in PATTERN_TIMER_OPERATIONS
                                          for operation in binding["operations"])
                                      for world in matrix for binding in world["pattern_bindings"]),
        "legacy_stage_bindings": sum(len(world["legacy_bindings"]) for world in matrix),
        "pattern_spawn_gaps": len(spawn_gaps),
        "declared_stage_owners": dict(sorted(Counter(str(world["declared_stage_owner"]) for world in matrix).items())),
        "declared_recovery_owners": dict(sorted(Counter(str(world["declared_recovery_owner"]) for world in matrix).items())),
        "stage_classifications": dict(sorted(Counter(str(world["stage_classification"]) for world in matrix).items())),
        "recovery_classifications": dict(sorted(Counter(str(world["recovery_classification"]) for world in matrix).items())),
        "conversion_statuses": dict(sorted(Counter(str(world["conversion_status"]) for world in matrix).items())),
    }
    used_patterns = sorted({str(binding["pattern_key"]) for world in matrix for binding in world["pattern_bindings"]})
    pattern_catalog = [{
        "pattern": patterns[key]["name"],
        "source": patterns[key]["source"],
        "operations": patterns[key]["operations"],
    } for key in used_patterns]
    for world in matrix:
        for binding in world["pattern_bindings"]:
            binding.pop("pattern_key")
            binding.pop("operations")
    return {
        "version": 1,
        "provenance": {
            "kind": "RUNTIME_AUDIT_PROJECTION",
            "authoritative_retail_evidence": False,
            "production_world_enumeration": str(COVERAGE),
            "condition_projection": str(CONDITION_SPAWNS),
            "pattern_projection": "src/main/resources/aion/definitions/compact/ai/npcaipatterns*.xml",
            "uncompiled_scriptdll_authority": "58Server/server58-source/MainServer_ScriptDLL64",
            "runtime_state": "src/main/java/com/aionemu/gameserver/model/instance/InstanceRuntimeState.java",
            "deadline_scheduler": "src/main/java/com/aionemu/gameserver/services/instance/InstanceDeadlineScheduler.java",
        },
        "summary": summary,
        "pattern_models": pattern_catalog,
        "batches": batches,
        "worlds": matrix,
    }


def render(report: dict[str, object]) -> str:
    return json.dumps(report, ensure_ascii=False, indent=2) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    output = args.output or root / REPORT
    content = render(build(root))
    if args.check:
        if not output.is_file() or output.read_text(encoding="utf-8") != content:
            raise SystemExit(f"stale instance stage/recovery matrix: {output}")
        print(f"instance stage/recovery matrix is current: {output}")
        return
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(content, encoding="utf-8")
    print(json.dumps(json.loads(content)["summary"], ensure_ascii=False, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (ET.ParseError, OSError, ValueError) as error:
        print(error, file=sys.stderr)
        raise SystemExit(1) from error
