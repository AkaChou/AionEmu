#!/usr/bin/env python3
"""Generate the static-door ownership matrix for production instances."""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path


COVERAGE = Path("src/main/resources/aion/definitions/compact/instance/coverage.xml")
STATIC_DOORS = Path("src/main/resources/aion/data/static_data/staticdoors/staticdoor_templates.xml")
INSTANCE_SPAWNS = Path("src/main/resources/aion/data/static_data/spawns/Instances")
AI_DIRECTORY = Path("src/main/resources/aion/definitions/compact/ai")
HANDLERS = Path("src/main/java/com/aionemu/gameserver/instance/handlers/scripts")
NPC_TEMPLATES = Path("src/main/resources/aion/data/static_data/npcs/npc_template.xml")
REPORT = Path("docs/RETAIL_INSTANCE_DOOR_MATRIX.json")
RETAIL_DOORS = Path("docs/RETAIL_STATIC_DOOR_SOURCE_MATRIX.json")
RETAIL_DOOR_REFERENCES = Path("docs/RETAIL_STATIC_DOOR_REFERENCE_GRAPH.json")
VERSION_SUFFIX = re.compile(r"_ver\d+$", re.IGNORECASE)
INSTANCE_ID = re.compile(r"\b\d{9}\b")
SPAWN_CALL = re.compile(r"\bspawn\(\s*(\d{6})\s*,")
SET_DOOR = re.compile(r"\bsetDoorState\s*\(\s*(.+?)\s*,\s*(true|false)\s*\)")
OPEN_DOOR = re.compile(r"\bopenDoor\s*\(\s*([^()]+?)\s*\)")
MAP_SET_OPEN = re.compile(
    r"(?:\bdoors|instance\.getDoors\(\))\.get\(\s*([^)]+?)\s*\)\.setOpen\(\s*(true|false)\s*\)"
)
BULK_SET_OPEN = re.compile(r"\bdoor\.setOpen\(\s*(true|false)\s*\)")
LEGACY_SET_OPEN = re.compile(r"getDoors\(\)\.get\(\s*(\d+)\s*\)\.setOpen\(\s*(true|false)\s*\)")
LEGACY_SET_DOOR_STATE = re.compile(
    r"getInstanceHandler\(\)\.setDoorState\(\s*(\d+)\s*,\s*(true|false)\s*\)"
)


def production_worlds(root: Path) -> dict[int, dict[str, str]]:
    worlds = {}
    for node in ET.parse(root / COVERAGE).getroot().findall("world"):
        world_id = int(node.get("id", "0"))
        if world_id == 300260000:
            continue
        owners = {part.split(":", 1)[0]: part.split(":", 1)[1]
                  for part in node.get("dimension_owners", "").split(",")}
        worlds[world_id] = {
            "local_name": node.get("local_name", ""),
            "retail_name": node.get("retail_name", ""),
            "declared_owner": owners["door"],
        }
    if len(worlds) != 139:
        raise ValueError(f"expected 139 production worlds, found {len(worlds)}")
    return worlds


def state_flags(raw: str) -> list[str]:
    value = int(raw, 16) if raw.lower().startswith("0x") else int(raw)
    return [name for bit, name in ((1, "OPENED"), (2, "CLICKABLE"), (4, "CLOSEABLE"), (8, "ONEWAY"))
            if value & bit]


def static_doors(root: Path, worlds: set[int]) -> dict[int, list[dict[str, object]]]:
    result: dict[int, list[dict[str, object]]] = defaultdict(list)
    for world in ET.parse(root / STATIC_DOORS).getroot().findall("world"):
        world_id = int(world.get("world", "0"))
        if world_id not in worlds:
            continue
        physical_ids = set()
        for node in world.findall("staticdoor"):
            if node.get("type", "DOOR") != "DOOR":
                continue
            door_id = int(node.get("doorid", "0"))
            if door_id in physical_ids:
                raise ValueError(f"duplicate static door {world_id}:{door_id}")
            physical_ids.add(door_id)
            retail_id = int(node.get("retailid", "0"))
            raw_state = node.get("state", "0")
            result[world_id].append({
                "door_id": door_id,
                "retail_id": retail_id or None,
                "lookup_ids": [door_id] + ([retail_id] if retail_id else []),
                "_x": node.get("x"),
                "_y": node.get("y"),
                "_z": node.get("z"),
                "key_id": int(node.get("keyid", "0")),
                "initial_state": raw_state,
                "initial_flags": state_flags(raw_state),
                "mesh": node.get("mesh", ""),
                "handler_controls": [],
                "pattern_controls": [],
                "legacy_controls": [],
                "service_controls": [],
            })
        result[world_id].sort(key=lambda door: int(door["door_id"]))
    return result


def retail_static_doors(root: Path, worlds: set[int]) -> dict[int, dict[str, object]]:
    document = json.loads((root / RETAIL_DOORS).read_text(encoding="utf-8"))
    provenance = document.get("provenance", {})
    if (document.get("version") != 1 or provenance.get("kind") != "RETAIL_SOURCE_MATRIX"
            or not provenance.get("authoritative_retail_evidence")):
        raise ValueError("invalid retail static-door source matrix")
    result = {}
    for world in document["worlds"]:
        world_id = world.get("world_id")
        if world_id not in worlds:
            continue
        if world_id in result:
            raise ValueError(f"duplicate retail static-door world {world_id}")
        if world["duplicate_editor_ids"]:
            raise ValueError(f"duplicate retail static-door ids in world {world_id}")
        if any(door["status"] != "PROVEN" for door in world["doors"]):
            raise ValueError(f"incomplete retail static-door evidence in world {world_id}")
        result[int(world_id)] = world
    if set(result) != worlds:
        raise ValueError(f"missing retail static-door worlds: {sorted(worlds - set(result))}")
    return result


def retail_control_references(root: Path, worlds: dict[int, dict[str, object]]) -> dict[tuple[object, ...], dict[str, object]]:
    document = json.loads((root / RETAIL_DOOR_REFERENCES).read_text(encoding="utf-8"))
    summary = document.get("summary", {})
    authority = document.get("authority", {})
    if (document.get("version") != 4 or document.get("projection") != "static_door"
            or authority.get("data_root") != "58Server-new/Map"
            or summary.get("unresolved") or summary.get("ambiguous")):
        raise ValueError("invalid retail static-door reference graph")
    world_names = {str(world["world_name"]).casefold() for world in worlds.values()}
    grouped: dict[tuple[object, ...], list[dict[str, object]]] = defaultdict(list)
    for reference in document["references"]:
        world_name = str(reference["consumer"]["world"])
        if world_name.casefold() not in world_names:
            continue
        raw_id = str(reference["raw"])
        method = str(reference["method"])
        if reference.get("kind") != "static_door" or reference.get("status") not in {"RESOLVED", "REJECTED"}:
            raise ValueError(f"invalid retail door reference: {reference}")
        if not raw_id.isdigit() or not method.isdigit():
            raise ValueError(f"invalid retail door control fields: {reference}")
        target_ids = sorted(int(target["id"]) for target in reference["targets"])
        if any(int(target["control_id"]) != int(raw_id) for target in reference["targets"]):
            raise ValueError(f"retail door control mismatch: {reference}")
        for npc_id in reference["npc_ids"]:
            key = (world_name.casefold(), int(npc_id), str(reference["pattern"]).casefold(),
                   int(raw_id), int(method))
            grouped[key].append({
                "status": reference["status"],
                "target_ids": target_ids,
                "source": reference["source"],
                "reference": reference["consumer"]["id"],
            })
    result = {}
    for key, references in grouped.items():
        signatures = {(reference["status"], tuple(reference["target_ids"])) for reference in references}
        if len(signatures) != 1:
            raise ValueError(f"conflicting retail door references for {key}: {references}")
        status, target_ids = signatures.pop()
        result[key] = {
            "status": status,
            "target_ids": list(target_ids),
            "sources": sorted({reference["source"] for reference in references}),
            "references": sorted({reference["reference"] for reference in references}),
        }
    return result


def same_point(runtime: dict[str, object], retail: dict[str, object]) -> bool:
    return all(runtime.get(f"_{key}") not in {None, ""}
               and abs(float(runtime[f"_{key}"]) - float(retail[key])) <= 0.01 for key in ("x", "y", "z"))


def retail_flags(door: dict[str, object]) -> list[str]:
    return [name for name, field in (("OPENED", "opened"), ("CLICKABLE", "clickable"),
                                     ("CLOSEABLE", "closeable")) if door[field]]


def associate_retail_doors(runtime: list[dict[str, object]], retail: list[dict[str, object]]
                           ) -> tuple[str, list[dict[str, object]]]:
    index = lookup(runtime)
    matched = set()
    associations = []
    for door in retail:
        candidates = index.get(int(door["editor_id"]), [])
        exact = [candidate for candidate in candidates if same_point(candidate, door)]
        if len(exact) == 1:
            candidate = exact[0]
            matched.add(int(candidate["door_id"]))
            expected_flags = retail_flags(door)
            state_status = ("MATCH" if sorted(flag for flag in candidate["initial_flags"] if flag != "ONEWAY")
                            == sorted(expected_flags) else "MISMATCH")
            status = "MATCH"
        elif len(exact) > 1:
            candidate = None
            state_status = "NOT_COMPARED"
            status = "AMBIGUOUS_RUNTIME"
        elif candidates:
            candidate = None
            state_status = "NOT_COMPARED"
            status = "COORDINATE_MISMATCH"
        else:
            candidate = None
            state_status = "NOT_COMPARED"
            status = "MISSING_RUNTIME"
        associations.append({
            "retail_editor_id": door["editor_id"],
            "runtime_door_id": None if candidate is None else candidate["door_id"],
            "status": status,
            "initial_state_status": state_status,
        })
    associations.extend({
        "retail_editor_id": None,
        "runtime_door_id": door["door_id"],
        "status": "RUNTIME_ONLY",
        "initial_state_status": "NOT_COMPARED",
    } for door in runtime if int(door["door_id"]) not in matched)
    if not associations:
        source_status = "NOT_APPLICABLE"
    elif any(row["status"] != "MATCH" for row in associations):
        source_status = "DEFINITION_MISMATCH"
    elif any(row["initial_state_status"] != "MATCH" for row in associations):
        source_status = "INITIAL_STATE_MISMATCH"
    else:
        source_status = "MATCH"
    return source_status, associations


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def method_start(text: str, offset: int) -> int:
    candidates = [match.start() for match in re.finditer(
        r"(?:public|protected|private)\s+(?:static\s+)?[\w<>\[\]]+\s+\w+\s*\([^)]*\)\s*\{", text[:offset])]
    return candidates[-1] if candidates else 0


def resolved_ids(text: str, offset: int, expression: str) -> list[int]:
    expression = expression.strip()
    if expression.isdigit():
        return [int(expression)]
    start = method_start(text, offset)
    window = text[start:offset]
    arrays = list(re.finditer(r"new\s+int\[\]\s*\{([^}]*)\}", window, re.DOTALL))
    if arrays and expression in {"door", "doorId"}:
        values = [int(value) for value in re.findall(r"\b\d+\b", arrays[-1].group(1))]
        if values:
            return values
    if expression == "doorId":
        values = [int(value) for value in re.findall(r"\bdoorId\s*!=\s*(\d+)\b", window)]
        if values:
            return sorted(set(values))
    cases = [int(value) for value in re.findall(r"\bcase\s+(\d+)\s*(?:->|:)", window[-1200:])]
    return cases[-1:] if cases else []


def handler_calls(path: Path, root: Path) -> tuple[str, str, set[int], list[dict[str, object]], set[int]]:
    text = path.read_text(encoding="utf-8")
    declaration = re.search(r"\bclass\s+(\w+)(?:\s+extends\s+(\w+))?", text)
    if declaration is None:
        return "", "", set(), [], set()
    class_name = declaration.group(1)
    parent = declaration.group(2) or ""
    world_ids = set()
    for annotation in re.findall(r"@InstanceID\s*\((.*?)\)", text, re.DOTALL):
        world_ids.update(int(value) for value in INSTANCE_ID.findall(annotation))
    source = str(path.relative_to(root))
    calls = []

    def add(match: re.Match[str], mechanism: str, state: bool, expression: str) -> None:
        ids = resolved_ids(text, match.start(), expression)
        calls.append({
            "owner_type": "HANDLER",
            "mechanism": mechanism,
            "state": "OPEN" if state else "CLOSED",
            "target_expression": expression.strip(),
            "target_ids": ids,
            "recovery": "PERSISTED_RUNTIME_STATE" if mechanism in {"setDoorState", "openDoor_helper"}
            else "RUNTIME_ONLY",
            "source": source,
            "line": line_number(text, match.start()),
        })

    for match in SET_DOOR.finditer(text):
        expression = match.group(1).strip()
        if expression == "doorId" and re.search(r"\bvoid\s+openDoor\s*\(\s*int\s+doorId\s*\)",
                                                   text[method_start(text, match.start()):match.start()]):
            continue
        add(match, "setDoorState", match.group(2) == "true", expression)
    for match in OPEN_DOOR.finditer(text):
        expression = match.group(1).strip()
        if expression.startswith("int "):
            continue
        add(match, "openDoor_helper", True, expression)
    for match in MAP_SET_OPEN.finditer(text):
        add(match, "direct_setOpen", match.group(2) == "true", match.group(1))
    if "instance.getDoors().values()" in text:
        for match in BULK_SET_OPEN.finditer(text):
            calls.append({
                "owner_type": "HANDLER",
                "mechanism": "bulk_setOpen",
                "state": "OPEN" if match.group(1) == "true" else "CLOSED",
                "target_expression": "ALL_STATIC_DOORS",
                "target_ids": [],
                "recovery": "RUNTIME_ONLY",
                "source": source,
                "line": line_number(text, match.start()),
            })
    denied = set()
    for match in re.finditer(r"case\s+([\d,\s]+)\s*->\s*false", text):
        denied.update(int(value) for value in re.findall(r"\d+", match.group(1)))
    return class_name, parent, world_ids, calls, denied


def handlers(root: Path, worlds: set[int]) -> tuple[dict[int, str], dict[int, list[dict[str, object]]], dict[int, set[int]]]:
    owners = {}
    controls: dict[int, list[dict[str, object]]] = defaultdict(list)
    denials: dict[int, set[int]] = defaultdict(set)
    classes = {}
    for path in sorted((root / HANDLERS).rglob("*.java")):
        class_name, parent, world_ids, calls, denied = handler_calls(path, root)
        if class_name:
            classes[class_name] = (path, parent, world_ids, calls, denied)
    for class_name, (path, parent, world_ids, own_calls, own_denied) in classes.items():
        if not world_ids:
            continue
        source = str(path.relative_to(root))
        calls = list(own_calls)
        denied = set(own_denied)
        seen = {class_name}
        while parent in classes and parent not in seen:
            seen.add(parent)
            _path, parent, _world_ids, inherited_calls, inherited_denied = classes[parent]
            calls.extend(inherited_calls)
            denied.update(inherited_denied)
        for world_id in world_ids & worlds:
            if world_id in owners:
                raise ValueError(f"duplicate instance handlers for {world_id}: {owners[world_id]}, {source}")
            owners[world_id] = source
            controls[world_id].extend(calls)
            denials[world_id].update(denied)
    return owners, controls, denials


def legacy_door_ais(root: Path) -> dict[str, dict[str, object]]:
    result = {}
    directory = root / "src/main/java/com/aionemu/gameserver/ai/instance"
    for path in sorted(directory.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        calls = []
        for pattern, mechanism, recovery in (
            (LEGACY_SET_OPEN, "legacy_ai_setOpen", "RUNTIME_ONLY"),
            (LEGACY_SET_DOOR_STATE, "legacy_ai_setDoorState", "PERSISTED_RUNTIME_STATE"),
        ):
            for match in pattern.finditer(text):
                calls.append({
                    "owner_type": "LEGACY_AI",
                    "mechanism": mechanism,
                    "state": "OPEN" if match.group(2) == "true" else "CLOSED",
                    "target_id": int(match.group(1)),
                    "recovery": recovery,
                    "source": str(path.relative_to(root)),
                    "line": line_number(text, match.start()),
                })
        calls.sort(key=lambda call: int(call["line"]))
        if not calls:
            continue
        ai_name = re.search(r'@AIName\("([^"]+)"\)', text)
        if ai_name is None:
            raise ValueError(f"legacy door AI has no @AIName: {path}")
        key = ai_name.group(1).casefold()
        if key in result:
            raise ValueError(f"duplicate legacy door AI name: {ai_name.group(1)}")
        result[key] = {"ai": ai_name.group(1), "calls": calls}
    return result


def legacy_npc_mappings(root: Path, ais: dict[str, dict[str, object]]) -> dict[int, dict[str, object]]:
    result = {}
    for _event, node in ET.iterparse(root / NPC_TEMPLATES, events=("end",)):
        if node.tag == "npc_template":
            owner = ais.get(node.get("ai", "").casefold())
            if owner is not None:
                result[int(node.get("npc_id", "0"))] = {
                    "name": node.get("name", ""),
                    "ai": owner["ai"],
                    "owner": owner,
                }
        node.clear()
    return result


def retail_mapped_npcs(root: Path) -> set[int]:
    return {int(node.get("id", "0"))
            for node in ET.parse(root / AI_DIRECTORY / "npc-ai.xml").getroot().findall("npc")
            if node.get("ai", "")}


def compact_conditions(node: ET.Element | None) -> list[str]:
    if node is None:
        return []
    return [f"{leaf.tag}={leaf.text.strip() if leaf.text else ''}" for leaf in node.iter() if len(leaf) == 0]


def control_patterns(root: Path) -> dict[str, dict[str, object]]:
    result = {}
    for path in sorted((root / AI_DIRECTORY).glob("npcaipatterns*.xml")):
        for pattern in ET.parse(path).getroot().iter("npc_ai_pattern"):
            name = pattern.findtext("name", "").strip()
            actions = []
            events = pattern.find("event_handlers")
            if events is not None:
                for event in events:
                    for rule in event.findall("pattern"):
                        for action in rule.findall("actions/control_door"):
                            door_id = int(action.findtext("id", "0"))
                            method = int(action.findtext("method", "0"))
                            actions.append({
                                "event": event.tag,
                                "priority": int(rule.findtext("priority", "0")),
                                "conditions": compact_conditions(rule.find("conditions")),
                                "target_id": door_id,
                                "method": method,
                                "state": "OPEN" if method == 1 else "CLOSED",
                            })
            if actions:
                result[name.casefold()] = {
                    "name": name,
                    "source": str(path.relative_to(root)),
                    "actions": actions,
                }
    return result


def npc_mappings(root: Path, patterns: dict[str, dict[str, object]]) -> dict[int, dict[str, object]]:
    result = {}
    for node in ET.parse(root / AI_DIRECTORY / "npc-ai.xml").getroot().findall("npc"):
        ai = node.get("ai", "")
        key = ai.casefold()
        pattern = patterns.get(key) or patterns.get(VERSION_SUFFIX.sub("", key))
        if pattern is not None:
            result[int(node.get("id", "0"))] = {"name": node.get("name", ""), "ai": ai, "pattern": pattern}
    return result


def reachable_npcs(root: Path, worlds: set[int], wanted: set[int]) -> dict[tuple[int, int], list[str]]:
    result: dict[tuple[int, int], set[str]] = defaultdict(set)
    for path in sorted((root / INSTANCE_SPAWNS).glob("*.xml")):
        for spawn_map in ET.parse(path).getroot().iter("spawn_map"):
            world_id = int(spawn_map.get("map_id", "0"))
            if world_id not in worlds:
                continue
            for spawn in spawn_map.iter("spawn"):
                npc_id = int(spawn.get("npc_id", "0"))
                if npc_id in wanted:
                    result[(world_id, npc_id)].add(f"static:{path.relative_to(root)}")
    path = root / AI_DIRECTORY / "condition-spawns.xml"
    for world in ET.parse(path).getroot().findall("world"):
        world_id = int(world.get("id", "0"))
        if world_id not in worlds:
            continue
        for npc in world.findall(".//npc"):
            npc_id = int(npc.get("id", "0"))
            if npc_id in wanted:
                result[(world_id, npc_id)].add(f"condition:{npc.get('source', '') or path.relative_to(root)}")
    for handler in sorted((root / HANDLERS).rglob("*.java")):
        text = handler.read_text(encoding="utf-8")
        world_ids = set()
        for annotation in re.findall(r"@InstanceID\s*\((.*?)\)", text, re.DOTALL):
            world_ids.update(int(value) for value in INSTANCE_ID.findall(annotation))
        for match in SPAWN_CALL.finditer(text):
            npc_id = int(match.group(1))
            if npc_id in wanted:
                for world_id in world_ids & worlds:
                    result[(world_id, npc_id)].add(f"handler:{handler.relative_to(root)}")
    return {key: sorted(values) for key, values in result.items()}


def lookup(doors: list[dict[str, object]]) -> dict[int, list[dict[str, object]]]:
    result: dict[int, list[dict[str, object]]] = defaultdict(list)
    for door in doors:
        result[int(door["door_id"])].append(door)
    return result


def retail_lookup(doors: list[dict[str, object]]) -> dict[int, list[dict[str, object]]]:
    result: dict[int, list[dict[str, object]]] = defaultdict(list)
    for door in doors:
        if door["retail_id"] is not None:
            result[int(door["retail_id"])].append(door)
    return result


def target_status(index: dict[int, list[dict[str, object]]], target_id: int) -> str:
    count = len(index.get(target_id, []))
    return "RESOLVED" if count == 1 else "MISSING" if count == 0 else "AMBIGUOUS"


def classify(doors: list[dict[str, object]], handler_controls: list[dict[str, object]],
             pattern_controls: list[dict[str, object]], legacy_controls: list[dict[str, object]],
             service_controls: list[dict[str, object]], unresolved: list[dict[str, object]]) -> tuple[str, str]:
    if not doors:
        owner = "REJECT_UNRESOLVED_DOOR_TARGET" if unresolved else "NO_STATIC_DOORS"
        return owner, "REJECT_UNRESOLVED_DOOR_TARGET" if unresolved else "NOT_APPLICABLE"
    has_handler = bool(handler_controls)
    has_pattern = bool(pattern_controls)
    has_legacy = bool(legacy_controls)
    has_service = bool(service_controls)
    if unresolved:
        owner = "REJECT_UNRESOLVED_DOOR_TARGET"
    elif sum((has_handler, has_pattern, has_legacy, has_service)) > 1:
        owner = "MIXED_CONTROL_OWNERS"
    elif has_handler:
        owner = "HANDLER_EVENT_BRIDGE"
    elif has_pattern:
        owner = "PATTERN_CONTROLLED"
    elif has_legacy:
        owner = "LEGACY_AI_CONTROLLED"
    elif has_service:
        owner = "SERVICE_CONTROLLED"
    else:
        owner = "STATIC_OR_KEY_OWNED"
    recovery = ("REJECT_RUNTIME_ONLY_LEGACY_STATE"
                if any(call["recovery"] == "RUNTIME_ONLY" for call in legacy_controls)
                else "REJECT_RUNTIME_ONLY_HANDLER_STATE"
                if any(call["recovery"] == "RUNTIME_ONLY" for call in handler_controls)
                else "REJECT_UNRESOLVED_DOOR_TARGET" if unresolved
                else "DERIVED_SERVICE_STATE" if has_service
                else "PERSISTED_RUNTIME_STATE" if has_handler or has_pattern or has_legacy else "INITIAL_TEMPLATE")
    return owner, recovery


def suggested_owner(classification: str, declared_owner: str) -> str:
    if classification in {"MIXED_CONTROL_OWNERS", "REJECT_UNRESOLVED_DOOR_TARGET"}:
        return declared_owner
    if declared_owner in {"EVENT_AI", "HOUSING_SERVICE", "NON_PRODUCTION", "SCRIPT_QUEST",
                          "TOURNAMENT_SERVICE"}:
        return declared_owner
    return {
        "NO_STATIC_DOORS": "NOT_APPLICABLE",
        "STATIC_OR_KEY_OWNED": "RETAIL_DATA",
        "PATTERN_CONTROLLED": "RETAIL_PATTERN",
        "HANDLER_EVENT_BRIDGE": "HANDLER",
        "LEGACY_AI_CONTROLLED": "SCRIPT_AI",
        "SERVICE_CONTROLLED": "TOURNAMENT_SERVICE",
    }[classification]


def attach_batches(matrix: list[dict[str, object]]) -> list[dict[str, object]]:
    grouped: dict[str, list[dict[str, object]]] = defaultdict(list)
    signatures = {}
    for world in matrix:
        signature = {
            "declared_owner": world["declared_owner"],
            "classification": world["classification"],
            "recovery": world["recovery"],
            "has_static_doors": bool(world["doors"]),
            "handler_mechanisms": sorted({call["mechanism"] for call in world["handler_controls"]}),
            "has_pattern_controls": bool(world["pattern_controls"]),
            "has_legacy_controls": bool(world["legacy_controls"]),
            "has_service_controls": bool(world["service_controls"]),
            "retail_source_status": world["retail_source_status"],
        }
        key = json.dumps(signature, sort_keys=True, separators=(",", ":"))
        signatures[key] = signature
        grouped[key].append(world)
    batches = []
    for index, key in enumerate(sorted(grouped), 1):
        batch_id = f"B{index:03d}"
        batch_worlds = grouped[key]
        for world in batch_worlds:
            world["batch_id"] = batch_id
        batches.append({
            "id": batch_id,
            "count": len(batch_worlds),
            "signature": signatures[key],
            "world_ids": [world["world_id"] for world in batch_worlds],
        })
    return batches


def build(root: Path) -> dict[str, object]:
    world_data = production_worlds(root)
    world_ids = set(world_data)
    doors_by_world = static_doors(root, world_ids)
    retail_doors_by_world = retail_static_doors(root, world_ids)
    retail_references = retail_control_references(root, retail_doors_by_world)
    handler_owners, controls_by_world, pattern_denials = handlers(root, world_ids)
    patterns = control_patterns(root)
    mappings = npc_mappings(root, patterns)
    legacy_ais = legacy_door_ais(root)
    legacy_mappings = legacy_npc_mappings(root, legacy_ais)
    retail_npcs = retail_mapped_npcs(root)
    bindings = reachable_npcs(root, world_ids, set(mappings) | set(legacy_mappings))
    pattern_by_world: dict[int, list[dict[str, object]]] = defaultdict(list)
    for (world_id, npc_id), sources in sorted(bindings.items()):
        if npc_id not in mappings:
            continue
        mapping = mappings[npc_id]
        pattern = mapping["pattern"]
        for action in pattern["actions"]:
            pattern_by_world[world_id].append({
                "npc_id": npc_id,
                "npc_name": mapping["name"],
                "mapped_pattern": mapping["ai"],
                "pattern": pattern["name"],
                "pattern_source": pattern["source"],
                "reachability_sources": sources,
                "selection": "REJECT_HANDLER_PATTERN" if npc_id in pattern_denials[world_id]
                else "RUNTIME_GATED_RETAIL_PATTERN",
                "owner_type": "PATTERN",
                **action,
                "recovery": "PERSISTED_RUNTIME_STATE",
            })

    legacy_by_world: dict[int, list[dict[str, object]]] = defaultdict(list)
    for (world_id, npc_id), sources in sorted(bindings.items()):
        mapping = legacy_mappings.get(npc_id)
        if mapping is None:
            continue
        selection = ("LEGACY_AI_OWNER" if npc_id in pattern_denials[world_id] or npc_id not in retail_npcs
                     else "ACTIVE_ON_RETAIL_PATTERN_FALLBACK")
        for call in mapping["owner"]["calls"]:
            legacy_by_world[world_id].append({
                "npc_id": npc_id,
                "npc_name": mapping["name"],
                "ai": mapping["ai"],
                "reachability_sources": sources,
                "selection": selection,
                **call,
            })

    matrix = []
    classification_counts = Counter()
    recovery_counts = Counter()
    target_counts = Counter()
    mechanism_counts = Counter()
    owner_counts = Counter()
    key_counts = Counter()
    declared_counts = Counter()
    suggested_counts = Counter()
    ownership_mismatches = 0
    unresolved_total = 0
    source_status_counts = Counter()
    association_counts = Counter()
    initial_state_counts = Counter()
    for world_id, metadata in sorted(world_data.items()):
        doors = doors_by_world.get(world_id, [])
        retail_world = retail_doors_by_world.get(world_id, {"source": "", "doors": []})
        retail_source_status, door_associations = associate_retail_doors(doors, retail_world["doors"])
        for door in doors:
            for key in ("_x", "_y", "_z"):
                door.pop(key)
        source_status_counts[retail_source_status] += 1
        association_counts.update(row["status"] for row in door_associations)
        initial_state_counts.update(row["initial_state_status"] for row in door_associations)
        index = lookup(doors)
        retail_index = retail_lookup(doors)
        unresolved = []
        handler_controls = controls_by_world.get(world_id, [])
        expanded_handler = []
        for call in handler_controls:
            row = dict(call)
            if call["mechanism"] == "bulk_setOpen":
                row["target_status"] = "ALL_RESOLVED" if doors else "MISSING"
                matched = doors
            elif call["target_ids"]:
                statuses = {target_status(index, int(target_id)) for target_id in call["target_ids"]}
                row["target_status"] = next(iter(statuses)) if len(statuses) == 1 else "MIXED"
                matched = [door for target_id in call["target_ids"] for door in index.get(int(target_id), [])]
            else:
                row["target_status"] = "RUNTIME_EXPRESSION"
                matched = []
            target_counts[row["target_status"]] += 1
            mechanism_counts[row["mechanism"]] += 1
            owner_counts[row["owner_type"]] += 1
            if row["target_status"] in {"MISSING", "AMBIGUOUS", "MIXED"}:
                unresolved.append(row)
            for door in matched:
                door["handler_controls"].append({key: value for key, value in row.items() if key != "source"})
            expanded_handler.append(row)

        pattern_controls = []
        for control in pattern_by_world.get(world_id, []):
            row = dict(control)
            reference_key = (str(retail_world["world_name"]).casefold(), int(row["npc_id"]),
                             str(row["pattern"]).casefold(), int(row["target_id"]), int(row["method"]))
            reference = retail_references.get(reference_key)
            row["retail_reference"] = reference
            matched = retail_index.get(int(row["target_id"]), [])
            if reference is None:
                row["target_status"] = "MISSING_RETAIL_REFERENCE"
                matched = []
            elif reference["status"] == "REJECTED":
                row["target_status"] = "REJECTED_RETAIL_TARGET_ABSENT"
                matched = []
            elif sorted(int(door["door_id"]) for door in matched) != reference["target_ids"]:
                row["target_status"] = "RUNTIME_GROUP_MISMATCH"
                row["runtime_target_ids"] = sorted(int(door["door_id"]) for door in matched)
                matched = []
            else:
                row["target_status"] = "RESOLVED_SET"
            target_counts[row["target_status"]] += 1
            owner_counts[row["owner_type"]] += 1
            if row["target_status"] != "RESOLVED_SET":
                unresolved.append(row)
            else:
                for door in matched:
                    door["pattern_controls"].append({
                        key: value for key, value in row.items() if key not in {"pattern_source", "reachability_sources"}
                    })
            pattern_controls.append(row)

        legacy_controls = []
        for control in legacy_by_world.get(world_id, []):
            row = dict(control)
            row["target_status"] = target_status(index, int(row["target_id"]))
            target_counts[row["target_status"]] += 1
            owner_counts[row["owner_type"]] += 1
            if row["target_status"] != "RESOLVED":
                unresolved.append(row)
            else:
                index[int(row["target_id"])][0]["legacy_controls"].append({
                    key: value for key, value in row.items() if key not in {"source", "reachability_sources"}
                })
            legacy_controls.append(row)

        service_controls = []
        if metadata["declared_owner"] == "TOURNAMENT_SERVICE" and doors:
            row = {
                "owner_type": "SERVICE",
                "mechanism": "TournamentService.setDoors",
                "state": "RUNTIME_MATCH_STATE",
                "target_status": "ALL_RESOLVED",
                "recovery": "DERIVED_TOURNAMENT_SESSION",
                "source": "src/main/java/com/aionemu/gameserver/services/instance/TournamentService.java",
                "line": 721,
            }
            service_controls.append(row)
            target_counts[row["target_status"]] += 1
            owner_counts[row["owner_type"]] += 1
            for door in doors:
                door["service_controls"].append({key: value for key, value in row.items() if key != "source"})

        classification, recovery = classify(doors, expanded_handler, pattern_controls, legacy_controls,
                                            service_controls, unresolved)
        classification_counts[classification] += 1
        recovery_counts[recovery] += 1
        declared_counts[metadata["declared_owner"]] += 1
        owner = suggested_owner(classification, metadata["declared_owner"])
        suggested_counts[owner] += 1
        ownership_mismatches += owner != metadata["declared_owner"]
        unresolved_total += len(unresolved)
        for door in doors:
            key_id = int(door["key_id"])
            key_counts["LOCKED"] += key_id == 1
            key_counts["ITEM_KEY"] += key_id > 1
            key_counts["NO_KEY"] += key_id == 0
        matrix.append({
            "world_id": world_id,
            **metadata,
            "handler": handler_owners.get(world_id, ""),
            "classification": classification,
            "suggested_owner": owner,
            "recovery": recovery,
            "retail_source_status": retail_source_status,
            "retail_source": str(retail_world["source"]),
            "door_source_matches": sum(row["status"] == "MATCH" and row["initial_state_status"] == "MATCH"
                                       for row in door_associations),
            "door_source_mismatches": [row for row in door_associations
                                       if row["status"] != "MATCH" or row["initial_state_status"] != "MATCH"],
            "doors": doors,
            "handler_controls": expanded_handler,
            "pattern_controls": pattern_controls,
            "legacy_controls": legacy_controls,
            "service_controls": service_controls,
            "unresolved_controls": unresolved,
        })

    batches = attach_batches(matrix)
    summary = {
        "production_worlds": len(matrix),
        "static_door_worlds": sum(bool(world["doors"]) for world in matrix),
        "static_doors": sum(len(world["doors"]) for world in matrix),
        "batches": len(batches),
        "handler_control_calls": sum(len(world["handler_controls"]) for world in matrix),
        "pattern_control_bindings": sum(len(world["pattern_controls"]) for world in matrix),
        "legacy_control_bindings": sum(len(world["legacy_controls"]) for world in matrix),
        "service_control_bindings": sum(len(world["service_controls"]) for world in matrix),
        "unresolved_controls": unresolved_total,
        "ownership_mismatches": ownership_mismatches,
        "worlds_by_retail_source_status": dict(sorted(source_status_counts.items())),
        "door_source_associations": dict(sorted(association_counts.items())),
        "initial_state_comparisons": dict(sorted(initial_state_counts.items())),
        "worlds_by_classification": dict(sorted(classification_counts.items())),
        "worlds_by_recovery": dict(sorted(recovery_counts.items())),
        "declared_owners": dict(sorted(declared_counts.items())),
        "suggested_owners": dict(sorted(suggested_counts.items())),
        "handler_calls_by_mechanism": dict(sorted(mechanism_counts.items())),
        "controls_by_owner": dict(sorted(owner_counts.items())),
        "controls_by_target_status": dict(sorted(target_counts.items())),
        "doors_by_key_semantics": dict(sorted(key_counts.items())),
    }
    return {
        "version": 3,
        "provenance": {
            "kind": "RUNTIME_AUDIT_PROJECTION",
            "authoritative_retail_evidence": False,
            "retail_source_matrix": str(RETAIL_DOORS),
            "retail_reference_graph": str(RETAIL_DOOR_REFERENCES),
        },
        "summary": summary,
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
            raise SystemExit(f"stale instance door matrix: {output}")
        print(f"instance door matrix is current: {output}")
        return
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(content, encoding="utf-8")
    print(json.dumps(json.loads(content)["summary"], ensure_ascii=False, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ET.ParseError, ValueError) as error:
        print(error, file=sys.stderr)
        raise SystemExit(1) from error
