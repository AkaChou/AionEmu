#!/usr/bin/env python3
"""Generate the complete static portal/lift ownership matrix for production instances."""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path


REPORT = Path("docs/RETAIL_INSTANCE_PORTAL_MATRIX.json")
START_AUDIT = Path("docs/RETAIL_INSTANCE_PORTAL_START_AUDIT.json")
PATTERN_AUDIT = Path("docs/RETAIL_INSTANCE_PORTAL_PATTERN_AUDIT.json")
DYNAMIC_AUDIT = Path("docs/RETAIL_INSTANCE_PORTAL_DYNAMIC_AUDIT.json")
TRANSPORT_SOURCE = Path("docs/RETAIL_INSTANCE_TRANSPORT_SOURCE_MATRIX.json")
NUMBER = re.compile(r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[fFdD])?")
SPAWN_CALL = re.compile(
    rf"\bspawn\(\s*(\d{{6}})\s*,\s*({NUMBER.pattern})\s*,\s*({NUMBER.pattern})\s*,\s*({NUMBER.pattern})"
    r"(?:\s*,\s*\(byte\)\s*(\d+)(?:\s*,\s*(\d+))?)?"
)


def production_worlds(root: Path) -> dict[int, dict[str, str]]:
    path = root / "src/main/resources/aion/definitions/compact/instance/coverage.xml"
    worlds = {}
    for node in ET.parse(path).getroot().findall("world"):
        world_id = int(node.get("id"))
        if world_id == 300260000:
            continue
        worlds[world_id] = {
            "local_name": node.get("local_name", ""),
            "retail_name": node.get("retail_name", ""),
            "classification": node.get("classification", ""),
        }
    return worlds


def add_point(points: dict[tuple[int, int], list[dict[str, str]]], world_id: int, npc_id: int,
              node: ET.Element, source: str, spawn_type: str) -> None:
    point = {key: node.get(key, "") for key in ("x", "y", "z")}
    point["heading"] = node.get("h", node.get("heading", ""))
    point["source"] = source
    point["spawn_type"] = spawn_type
    points[(world_id, npc_id)].append(point)


def spawn_points(root: Path, worlds: set[int], route_npcs: set[int]) -> dict[tuple[int, int], list[dict[str, str]]]:
    points: dict[tuple[int, int], list[dict[str, str]]] = defaultdict(list)
    directory = root / "src/main/resources/aion/data/static_data/spawns"
    for path in sorted(directory.rglob("*.xml")):
        spawn_type = path.relative_to(directory).parts[0]
        document = ET.parse(path).getroot()
        for spawn_map in document.iter("spawn_map"):
            world_id = int(spawn_map.get("map_id", "0"))
            for spawn in spawn_map.iter("spawn"):
                npc_id = int(spawn.get("npc_id", "0"))
                if world_id not in worlds and npc_id not in route_npcs:
                    continue
                for spot in spawn.findall("spot"):
                    add_point(points, world_id, npc_id, spot, f"static:{path.relative_to(root)}", spawn_type)

    path = root / "src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"
    for world in ET.parse(path).getroot().findall("world"):
        world_id = int(world.get("id", "0"))
        for condition in world.findall("condition"):
            source = condition.get("source", "")
            for npc in condition.findall(".//npc"):
                npc_id = int(npc.get("id", "0"))
                if world_id in worlds or npc_id in route_npcs:
                    add_point(points, world_id, npc_id, npc, f"condition:{source}", "condition-spawns")

    for key, values in points.items():
        points[key] = [dict(items) for items in sorted({tuple(sorted(value.items())) for value in values})]
    return points


def npc_templates(root: Path, wanted: set[int]) -> dict[int, dict[str, str]]:
    result = {}
    path = root / "src/main/resources/aion/data/static_data/npcs/npc_template.xml"
    for _event, node in ET.iterparse(path, events=("end",)):
        if node.tag != "npc_template":
            continue
        npc_id = int(node.get("npc_id", "0"))
        if npc_id in wanted:
            result[npc_id] = {
                "name": node.get("name", ""),
                "retail_name": node.get("name_desc", ""),
                "ai": node.get("ai", "").casefold(),
            }
        node.clear()
    return result


def portal_locations(root: Path) -> dict[int, dict[str, object]]:
    path = root / "src/main/resources/aion/data/static_data/portals/portal_loc.xml"
    result = {}
    for node in ET.parse(path).getroot().findall("portal_loc"):
        loc_id = int(node.get("loc_id", "0"))
        complete = all(node.get(key) is not None for key in ("world_id", "x", "y", "z"))
        result[loc_id] = {
            "complete": complete,
            "world_id": int(node.get("world_id", "0")),
            "x": node.get("x", ""),
            "y": node.get("y", ""),
            "z": node.get("z", ""),
            "heading": node.get("h", ""),
        }
    return result


def path_semantics(path: ET.Element) -> dict[str, object]:
    requirements = []
    for requirement in path.findall("portal_req"):
        requirements.append({
            "attributes": dict(sorted(requirement.attrib.items())),
            "quests": [dict(sorted(node.attrib.items())) for node in requirement.findall("quest_req")],
            "items": [dict(sorted(node.attrib.items())) for node in requirement.findall("item_req")],
        })
    return {
        "attributes": dict(sorted(path.attrib.items())),
        "requirements": requirements,
        "animation": path.get("animation", "FIRE_ANIMATION"),
    }


def portal_paths(root: Path) -> dict[int, list[dict[str, object]]]:
    locations = portal_locations(root)
    aliases = location_aliases(root)
    result: dict[int, list[dict[str, object]]] = defaultdict(list)
    path = root / "src/main/resources/aion/data/static_data/portals/portal_template2.xml"
    document = ET.parse(path).getroot()
    for kind in ("portal_use", "portal_dialog"):
        for owner in document.findall(kind):
            npc_id = int(owner.get("npc_id", "0"))
            for route in owner.findall("portal_path"):
                loc_id = int(route.get("loc_id", "0"))
                destination = locations.get(loc_id, {"complete": False})
                alias_name = route.get("destination_alias")
                if alias_name and destination.get("complete"):
                    points = aliases.get((destination["world_id"], alias_name.casefold()))
                    destination = {
                        "complete": bool(points),
                        "world_id": destination["world_id"],
                        "alias": alias_name,
                        "points": points or [],
                        "source": "src/main/resources/aion/definitions/compact/ai/ai-location-aliases.xml",
                    }
                result[npc_id].append({
                    "mechanism": kind,
                    "owner": "PortalAI2/PortalService" if kind == "portal_use" else "DialogService/PortalService",
                    "loc_id": loc_id,
                    "destination": destination,
                    "semantics": path_semantics(route),
                    "conversion": "ALREADY_DATA_DRIVEN" if destination["complete"] else "REJECT_MISSING_DESTINATION",
                })
    return result


def teleporter_paths(root: Path) -> dict[int, list[dict[str, object]]]:
    location_file = root / "src/main/resources/aion/data/static_data/teleport_location.xml"
    locations = {}
    for node in ET.parse(location_file).getroot().findall("teleloc_template"):
        loc_id = int(node.get("loc_id", "0"))
        complete = all(node.get(key) is not None for key in ("mapid", "posX", "posY", "posZ"))
        locations[loc_id] = {
            "complete": complete,
            "world_id": int(node.get("mapid", "0")),
            "x": node.get("posX", ""),
            "y": node.get("posY", ""),
            "z": node.get("posZ", ""),
            "heading": node.get("heading", ""),
        }

    result: dict[int, list[dict[str, object]]] = defaultdict(list)
    path = root / "src/main/resources/aion/data/static_data/npc_teleporter.xml"
    for template in ET.parse(path).getroot().findall("teleporter_template"):
        npc_ids = [int(value) for value in template.get("npc_ids", "").split() if int(value) > 0]
        for route in template.findall("locations/telelocation"):
            loc_id = int(route.get("loc_id", "0"))
            destination = locations.get(loc_id, {"complete": False})
            row = {
                "mechanism": "teleporter",
                "owner": "TeleporterData/TeleportService2",
                "loc_id": loc_id,
                "destination": destination,
                "semantics": {
                    "attributes": dict(sorted(route.attrib.items())),
                    "teleport_id": template.get("teleportId", ""),
                    "animation": "TELEPORTER_PROTOCOL",
                },
                "conversion": "ALREADY_DATA_DRIVEN" if destination["complete"] else "REJECT_MISSING_DESTINATION",
            }
            for npc_id in npc_ids:
                result[npc_id].append(row)
    return result


def retail_start_audit(root: Path) -> dict[int, dict[str, object]]:
    document = json.loads((root / START_AUDIT).read_text(encoding="utf-8"))
    if document.get("missing_condition_npc_ids"):
        raise ValueError("retail portal start audit has missing condition NPCs")
    condition_entries: dict[int, list[dict[str, object]]] = defaultdict(list)
    for entry in document["condition_entries"]:
        condition_entries[int(entry["npc_id"])].append(entry)
    result = {}
    for npc_id, entries in condition_entries.items():
        result[npc_id] = {
            "audit": str(START_AUDIT),
            "classification": "CONDITION_INFO_LIST",
            "world_ids": sorted({int(entry["world_id"]) for entry in entries}),
            "sources": sorted({str(entry["source"]) for entry in entries}),
            "unsupported_reasons": sorted({
                str(reason) for entry in entries for reason in entry["unsupported_reasons"]
            }),
            "conversion": "REJECT_UNMODELED_CONDITION_TRIGGER",
        }
    for npc_id in document["no_retail_start_npc_ids"]:
        npc_id = int(npc_id)
        if npc_id in result:
            raise ValueError(f"conflicting retail start audit for NPC {npc_id}")
        result[npc_id] = {
            "audit": str(START_AUDIT),
            "classification": "NO_RETAIL_WORLD_SPAWN",
            "conversion": "REJECT_NO_RETAIL_START",
        }
    return result


def retail_pattern_audit(root: Path) -> dict[int, dict[str, object]]:
    document = json.loads((root / PATTERN_AUDIT).read_text(encoding="utf-8"))
    result = {int(entry["npc_id"]): entry for entry in document["entries"]}
    if len(result) != len(document["entries"]):
        raise ValueError("duplicate NPC in retail portal pattern audit")
    return result


def dynamic_route_audit(root: Path) -> tuple[
        dict[tuple[str, int, int], dict[str, object]], dict[tuple[str, int], dict[str, object]]]:
    document = json.loads((root / DYNAMIC_AUDIT).read_text(encoding="utf-8"))
    legacy = {}
    handlers = {}
    summary = Counter()
    for entry in document["legacy_ai"]:
        for binding in entry["bindings"]:
            key = (str(entry["owner"]), int(binding["world_id"]), int(binding["npc_id"]))
            if key in legacy:
                raise ValueError(f"duplicate legacy dynamic portal audit binding {key}")
            legacy[key] = entry
            summary[str(entry["conversion"])] += 1
    for entry in document["handlers"]:
        key = (str(entry["owner"]), int(entry["world_id"]))
        if key in handlers:
            raise ValueError(f"duplicate handler dynamic portal audit binding {key}")
        handlers[key] = entry
        summary[str(entry["conversion"])] += len(entry["routes"])
    if dict(sorted(summary.items())) != document["summary"]:
        raise ValueError("stale retail dynamic portal audit summary")
    return legacy, handlers


def location_aliases(root: Path) -> dict[tuple[int, str], list[dict[str, str]]]:
    path = root / "src/main/resources/aion/definitions/compact/ai/ai-location-aliases.xml"
    result = {}
    for node in ET.parse(path).getroot().findall("alias"):
        key = (int(node.get("world_id", "0")), node.get("name", "").casefold())
        points = [{field: point.get(field, "") for field in ("x", "y", "z", "dir")}
                  for point in node.findall("point")]
        if key in result or not points:
            raise ValueError(f"invalid retail location alias {key}")
        result[key] = points
    return result


def retail_pattern_routes(npc_id: int, world_id: int, evidence: dict[str, object] | None,
                          aliases: dict[tuple[int, str], list[dict[str, str]]]) -> list[dict[str, object]]:
    if evidence is None or evidence["status"] != "SUPPORTED_RETAIL_PATTERN":
        return []
    routes = []
    for action in evidence["teleport_actions"]:
        alias = str(action["alias"])
        points = aliases.get((world_id, alias.casefold()))
        if not points:
            raise ValueError(f"missing retail teleport alias {world_id}:{alias} for NPC {npc_id}")
        routes.append({
            "mechanism": "retail_pattern_alias",
            "owner": f'RetailPatternAI2:{evidence["mapped_pattern"]}',
            "destination": {
                "complete": True,
                "world_id": world_id,
                "alias": alias,
                "points": points,
            },
            "semantics": {
                "event": action["event"],
                "conditions": action["conditions"],
                "target": action["target"],
                "showfx": action["showfx"],
                "action": "teleport_target_alias",
            },
            "pattern_evidence": {
                "audit": str(PATTERN_AUDIT),
                "pattern_source": evidence["pattern_source"],
            },
            "conversion": "ALREADY_DATA_DRIVEN",
        })
    if not routes:
        raise ValueError(f"supported retail teleport Pattern has no route for NPC {npc_id}")
    return routes


def split_arguments(value: str) -> list[str]:
    arguments = []
    depth = 0
    start = 0
    for index, char in enumerate(value):
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
        elif char == "," and depth == 0:
            arguments.append(value[start:index].strip())
            start = index + 1
    arguments.append(value[start:].strip())
    return arguments


def invocation_arguments(text: str, offset: int) -> tuple[str, int]:
    start = offset + len("TeleportService2.teleportTo(")
    depth = 1
    for index in range(start, len(text)):
        if text[index] == "(":
            depth += 1
        elif text[index] == ")":
            depth -= 1
            if depth == 0:
                return text[start:index], index + 1
    raise ValueError("unterminated TeleportService2.teleportTo call")


def literal(value: str) -> str | None:
    value = re.sub(r"^\s*\([A-Za-z]+\)\s*", "", value.strip())
    return value.rstrip("fFdD") if NUMBER.fullmatch(value) else None


def coordinate_index(arguments: list[str]) -> int | None:
    if len(arguments) == 8:
        candidates = (3,)
    elif len(arguments) == 7:
        candidates = (2,) if "TeleportAnimation." in arguments[-1] else (3,)
    elif len(arguments) == 6:
        candidates = (2,) if arguments[-1].lstrip().startswith("(byte)") else (3, 2)
    else:
        candidates = (2, 3)
    for candidate in candidates:
        if len(arguments) > candidate + 2 and all(literal(value) is not None
                                                   for value in arguments[candidate:candidate + 3]):
            return candidate
    return None


def expression_kind(value: str, current_pattern: str) -> str:
    if literal(value) is not None:
        return "FIXED"
    if re.search(current_pattern, value):
        return "CURRENT"
    return "EXPRESSION"


def teleport_semantics(arguments: list[str], coordinates: int | None) -> dict[str, object]:
    if coordinates is None:
        return {
            "api": "teleportTo",
            "arguments": len(arguments),
            "instance": "UNRESOLVED",
            "heading": "UNRESOLVED",
            "animation": "UNRESOLVED",
        }
    explicit_instance = coordinates == 3
    heading_index = coordinates + 3
    animation_index = heading_index + 1
    instance = (expression_kind(arguments[2], r"(?:getInstanceId\s*\(|\binstanceId\b)")
                if explicit_instance else "SERVICE_DEFAULT")
    heading = (expression_kind(arguments[heading_index], r"getHeading\s*\(")
               if len(arguments) > heading_index else "PLAYER_DEFAULT")
    animation = arguments[animation_index].strip() if len(arguments) > animation_index else "BEAM_ANIMATION"
    return {
        "api": "teleportTo",
        "arguments": len(arguments),
        "instance": instance,
        "heading": heading,
        "animation": animation,
    }


def case_values(text: str, offset: int, digits: int) -> list[int]:
    window = text[max(0, offset - 8000):offset]
    matches = list(re.finditer(r"case\s+([^:\n]+?)(?:->|:)", window))
    for match in reversed(matches):
        values = [int(value) for value in re.findall(rf"\b\d{{{digits}}}\b", match.group(1))]
        if values:
            return values
    return []


def teleport_calls(text: str) -> list[dict[str, object]]:
    calls = []
    needle = "TeleportService2.teleportTo("
    offset = 0
    while (offset := text.find(needle, offset)) >= 0:
        content, end = invocation_arguments(text, offset)
        arguments = split_arguments(content)
        world_expression = arguments[1] if len(arguments) > 1 else ""
        world_literal = literal(world_expression)
        coordinate_offset = coordinate_index(arguments)
        destination = {
            "complete": coordinate_offset is not None,
            "world_expression": world_expression,
            "world_id": int(world_literal) if world_literal and len(world_literal.lstrip("-")) == 9 else None,
            "x": literal(arguments[coordinate_offset]) if coordinate_offset is not None else "",
            "y": literal(arguments[coordinate_offset + 1]) if coordinate_offset is not None else "",
            "z": literal(arguments[coordinate_offset + 2]) if coordinate_offset is not None else "",
            "heading": literal(arguments[coordinate_offset + 3])
            if coordinate_offset is not None and len(arguments) > coordinate_offset + 3 else "",
        }
        calls.append({
            "destination": destination,
            "raw": " ".join(content.split()),
            "teleport": teleport_semantics(arguments, coordinate_offset),
            "source_worlds": case_values(text, offset, 9),
            "source_npcs": case_values(text, offset, 6),
        })
        offset = end
    return calls


def java_owners(root: Path) -> tuple[dict[str, dict[str, object]], list[dict[str, object]]]:
    ai = {}
    handlers = []
    java_root = root / "src/main/java/com/aionemu/gameserver"
    for path in sorted(java_root.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        if "TeleportService2.teleportTo(" not in text:
            continue
        calls = teleport_calls(text)
        relative = str(path.relative_to(root))
        ai_name = re.search(r'@AIName\("([^"]+)"\)', text)
        if ai_name:
            ai[ai_name.group(1).casefold()] = {"file": relative, "calls": calls}
        instance_ids = set()
        for annotation in re.findall(r"@InstanceID\s*\((.*?)\)", text, re.DOTALL):
            instance_ids.update(int(value) for value in re.findall(r"\b\d{9}\b", annotation))
        if instance_ids:
            handlers.append({"file": relative, "world_ids": sorted(instance_ids), "calls": calls})
    return ai, handlers


def handler_spawn_points(root: Path, wanted_npcs: set[int]
                         ) -> dict[tuple[int, int], list[dict[str, str]]]:
    points: dict[tuple[int, int], list[dict[str, str]]] = defaultdict(list)
    directory = root / "src/main/java/com/aionemu/gameserver/instance/handlers/scripts"
    for path in sorted(directory.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        world_ids = set()
        for annotation in re.findall(r"@InstanceID\s*\((.*?)\)", text, re.DOTALL):
            world_ids.update(int(value) for value in re.findall(r"\b\d{9}\b", annotation))
        if not world_ids:
            continue
        source = f"handler:{path.relative_to(root)}"
        for match in SPAWN_CALL.finditer(text):
            npc_id = int(match.group(1))
            if npc_id not in wanted_npcs:
                continue
            point = {
                "x": literal(match.group(2)) or "",
                "y": literal(match.group(3)) or "",
                "z": literal(match.group(4)) or "",
                "heading": match.group(5) or "",
                "entity_id": match.group(6) or "",
                "source": source,
                "spawn_type": "handler-dynamic",
            }
            for world_id in world_ids:
                points[(world_id, npc_id)].append(point)
    for key, values in points.items():
        points[key] = [dict(items) for items in sorted({tuple(sorted(value.items())) for value in values})]
    return points


def legacy_route(owner: dict[str, object], call: dict[str, object], evidence: dict[str, object] | None,
                 dynamic_evidence: dict[str, object] | None) -> dict[str, object]:
    destination = dict(call["destination"])
    if evidence is not None and dynamic_evidence is not None:
        raise ValueError(f"conflicting portal audits for {owner['file']}")
    if dynamic_evidence is not None:
        if destination["complete"] or call["raw"] != dynamic_evidence["raw_call"]:
            raise ValueError(f"stale runtime destination audit for {owner['file']}")
        destination = {
            "complete": True,
            "world_id": None,
            "runtime_model": dynamic_evidence["destination_model"],
        }
        conversion = str(dynamic_evidence["conversion"])
    elif not destination["complete"]:
        conversion = "REJECT_DYNAMIC_DESTINATION"
    elif evidence is None:
        conversion = "REJECT_MISSING_RETAIL_EVIDENCE"
    elif evidence["status"] == "REJECT_INCOMPLETE_RETAIL_PATTERN":
        conversion = "REJECT_INCOMPLETE_RETAIL_PATTERN"
    elif evidence["status"] == "REJECT_NO_RETAIL_PATTERN":
        conversion = "REJECT_NO_RETAIL_PATTERN"
    else:
        raise ValueError(f"legacy route retained for supported retail Pattern NPC {evidence['npc_id']}")
    route = {
        "mechanism": "legacy_ai",
        "owner": owner["file"],
        "destination": destination,
        "semantics": {
            "raw_call": call["raw"],
            "teleport": dynamic_evidence["teleport"] if dynamic_evidence is not None else call["teleport"],
        },
        "conversion": conversion,
    }
    if dynamic_evidence is not None:
        route["semantics"]["trigger"] = dynamic_evidence["trigger"]
        route["dynamic_evidence"] = {
            "audit": str(DYNAMIC_AUDIT),
            "source": dynamic_evidence["source"],
            "reason": dynamic_evidence["reason"],
        }
    if evidence is not None:
        route["pattern_evidence"] = {
            "audit": str(PATTERN_AUDIT),
            "mapped_pattern": evidence["mapped_pattern"],
            "status": evidence["status"],
            "reason": evidence.get("reason", ""),
        }
    return route


def audited_handler_routes(handler: dict[str, object], evidence: dict[str, object]) -> list[dict[str, object]]:
    if [call["raw"] for call in handler["calls"]] != [evidence["raw_call"]]:
        raise ValueError(f"stale handler dynamic portal audit for {handler['file']}")
    routes = []
    for audited in evidence["routes"]:
        destination = dict(audited["destination"])
        if not destination.get("complete") or destination.get("world_id") is None:
            raise ValueError(f"incomplete audited handler destination for {handler['file']}")
        routes.append({
            "mechanism": "handler",
            "owner": handler["file"],
            "destination": destination,
            "semantics": {
                "raw_call": evidence["raw_call"],
                "teleport": evidence["teleport"],
                "trigger": evidence["trigger"],
                "condition": audited["condition"],
            },
            "dynamic_evidence": {
                "audit": str(DYNAMIC_AUDIT),
                "source": evidence["source"],
                "reason": evidence["reason"],
            },
            "conversion": evidence["conversion"],
        })
    return routes


def destination_kind(world_id: int | None, complete: bool, worlds: set[int]) -> str:
    if not complete:
        return "MISSING"
    if world_id is None:
        return "DYNAMIC"
    return "INSTANCE_STATIC" if world_id in worlds else "WORLD_STATIC"


def start_kind(route: dict[str, object], worlds: set[int]) -> str:
    if route.get("start_dynamic"):
        return "DYNAMIC"
    points = route.get("start_points", [])
    world_id = route.get("start_world_id")
    if not points or world_id is None:
        return "MISSING"
    scope = "INSTANCE" if world_id in worlds else "WORLD"
    static = any(point["spawn_type"] in {"Instances", "Npcs"} for point in points)
    return f"{scope}_{'STATIC' if static else 'CONDITIONAL'}"


def route_allows_world(route: dict[str, object], world_id: int) -> bool:
    source_world_id = int(route.get("semantics", {}).get("attributes", {}).get("source_world_id", "0"))
    return source_world_id == 0 or source_world_id == world_id


def source_transport_routes(root: Path) -> dict[tuple[int, int, int], list[dict[str, object]]]:
    document = json.loads((root / TRANSPORT_SOURCE).read_text(encoding="utf-8"))
    provenance = document.get("provenance", {})
    if (document.get("version") != 5 or provenance.get("kind") != "RETAIL_SOURCE_MATRIX"
            or not provenance.get("authoritative_retail_evidence")):
        raise ValueError("invalid retail ScriptDLL transport source matrix")
    grouped: dict[tuple[int, int, int], dict[str, dict[str, object]]] = defaultdict(dict)
    for registration in document["registrations"]:
        callback_features = registration.get("callback_features")
        if registration["calls"] and (not callback_features or not callback_features.get("shape_id")
                                      or "predicates" not in callback_features
                                      or "operations" not in callback_features):
            raise ValueError(f"ScriptDLL transport without callback features: {registration['script_name']}")
        projection = registration.get("portal_service_projection")
        if not projection or projection.get("status") not in {
                "EXPRESSIBLE", "NOT_APPLICABLE", "REJECT_ROUTE_NOT_PROVEN", "REJECT_UNMODELED_CALLBACK_SHAPE"}:
            raise ValueError(f"ScriptDLL transport without PortalService projection: {registration['script_name']}")
        for call in registration["calls"]:
            if call.get("domain_type") not in {"LIFT", "UNCLASSIFIED"}:
                raise ValueError(f"ScriptDLL transport without audited domain type: {registration['script_name']}")
            event = call.get("event")
            dialog = -1 if event is None else int(event["dialog"])
            for route in call["routes"]:
                if route["status"] != "ENDPOINT_PROVEN":
                    continue
                start = route["start"]
                start_evidence = {
                    "world_id": start["world_id"],
                    "npc_id": start["npc_id"],
                    "x": start["x"],
                    "y": start["y"],
                    "z": start["z"],
                    "dir": start["dir"],
                    "object_type": start["object_type"],
                    "source": start["source"],
                }
                evidence = {
                    "matrix": str(TRANSPORT_SOURCE),
                    "script_name": registration["script_name"],
                    "callback": registration["callback"],
                    "registration_status": registration["status"],
                    "registration_reasons": registration["reasons"],
                    "registration_source": registration["registration_source"],
                    "callback_source": call["source"],
                    "callback_features": callback_features,
                    "api_offset": call["api_offset"],
                    "transport_type": call["transport_type"],
                    "domain_type": call["domain_type"],
                    "event": event,
                    "portal_service_projection": projection,
                    "starts": [start_evidence],
                    "destination": route["destination"],
                }
                key = (int(start["npc_id"]), int(start["world_id"]), dialog)
                identity = json.dumps({
                    "callback": registration["callback"],
                    "destination": route["destination"],
                    "projection": projection,
                }, sort_keys=True)
                previous = grouped[key].setdefault(identity, evidence)
                if previous is not evidence:
                    previous["starts"].append(start_evidence)
    result = {}
    for key, bindings in grouped.items():
        result[key] = []
        for evidence in bindings.values():
            evidence["starts"] = sorted({json.dumps(start, sort_keys=True) for start in evidence["starts"]})
            evidence["starts"] = [json.loads(start) for start in evidence["starts"]]
            result[key].append(evidence)
    return result


def same_destination(runtime: dict[str, object], retail: dict[str, object]) -> bool:
    if runtime.get("world_id") != retail.get("world_id"):
        return False
    runtime_points = runtime.get("points")
    retail_points = retail.get("points")
    if runtime_points is not None:
        if retail_points is None or len(runtime_points) != len(retail_points):
            return False
        return all(any(same_destination_point(runtime_point, retail_point)
                       for runtime_point in runtime_points) for retail_point in retail_points)
    return any(same_destination_point(runtime, point) for point in retail_points or [retail])


def same_destination_point(runtime: dict[str, object], retail: dict[str, object]) -> bool:
    if not all(key in runtime and retail.get(key) is not None for key in ("x", "y", "z")):
        return False
    if any(abs(float(runtime[key]) - float(retail[key])) > 0.001 for key in ("x", "y", "z")):
        return False
    if runtime.get("dir") is not None:
        return retail.get("dir") is not None and abs(float(runtime["dir"]) - float(retail["dir"])) <= 0.001
    heading = runtime.get("heading")
    direction = retail.get("dir")
    return heading not in {None, ""} and direction is not None \
        and int(float(heading)) == int(float(direction) * 120 / 360) % 120


def portal_service_requirements(route: dict[str, object]) -> dict[str, object]:
    semantics = route.get("semantics", {})
    attributes = semantics.get("attributes", {})
    result = {}
    race = attributes.get("race")
    if race and race != "PC_ALL":
        result["race"] = race
    requirements = semantics.get("requirements", [])
    if requirements:
        requirement = requirements[0].get("attributes", {})
        if int(requirement.get("min_level", "0")) > 0:
            result["min_level"] = int(requirement["min_level"])
        if "max_level" in requirement:
            result["max_level"] = int(requirement["max_level"])
    return result


def same_requirements(runtime: dict[str, object], retail: dict[str, object]) -> bool:
    return portal_service_requirements(runtime) == retail["portal_service_projection"]["requirements"]


def source_transport_evidence(route: dict[str, object],
                              sources: dict[tuple[int, int, int], list[dict[str, object]]]
                              ) -> dict[str, object] | None:
    if route["mechanism"] != "portal_dialog" or route.get("npc_id") is None or route.get("start_world_id") is None:
        return None
    dialog = route.get("semantics", {}).get("attributes", {}).get("dialog")
    if dialog is None:
        return None
    matches = [evidence for evidence in sources.get((int(route["npc_id"]), int(route["start_world_id"]), int(dialog)), [])
               if evidence["registration_status"] == "ROUTE_PROVEN"
               and evidence["portal_service_projection"]["status"] == "EXPRESSIBLE"
               and same_destination(route["destination"], evidence["destination"])
               and same_requirements(route, evidence)]
    if len(matches) > 1:
        raise ValueError(f"ambiguous ScriptDLL transport evidence for NPC {route['npc_id']}")
    return matches[0] if matches else None


def same_starts(points: list[dict[str, object]], retail: list[dict[str, object]]) -> bool:
    return all(any(all(key in point and abs(float(point[key]) - float(start[key])) <= 0.001
                       for key in ("x", "y", "z")) for point in points) for start in retail)


def script_transport_candidates(
        matrix: list[dict[str, object]],
        sources: dict[tuple[int, int, int], list[dict[str, object]]],
        templates: dict[int, dict[str, str]],
        points: dict[tuple[int, int], list[dict[str, str]]],
) -> list[dict[str, object]]:
    runtime_by_key: dict[tuple[int, int, int], list[dict[str, object]]] = defaultdict(list)
    for world in matrix:
        for route in world["routes"]:
            if route["mechanism"] != "portal_dialog" or route.get("start_world_id") is None:
                continue
            dialog = route.get("semantics", {}).get("attributes", {}).get("dialog")
            if dialog is None:
                continue
            runtime_by_key[(int(route["npc_id"]), int(route["start_world_id"]), int(dialog))].append(route)

    candidates = []
    for key, evidence_rows in sources.items():
        npc_id, world_id, dialog = key
        runtime_routes = runtime_by_key.get(key, [])
        for evidence in evidence_rows:
            shape_id = str(evidence["callback_features"]["shape_id"])
            event_dialog = None if evidence["event"] is None else int(evidence["event"]["dialog"])
            projection = evidence["portal_service_projection"]
            runtime_points = points.get((world_id, npc_id), [])
            start_status = ("MISSING" if not runtime_points else "MATCH"
                            if same_starts(runtime_points, evidence["starts"]) else "MISMATCH")
            exact = [route for route in runtime_routes
                     if same_destination(route["destination"], evidence["destination"])
                     and same_requirements(route, evidence)]
            ai = templates.get(npc_id, {}).get("ai", "")
            if projection["status"] != "EXPRESSIBLE":
                status = projection["status"]
                reason = "retail callback is not fully expressible by PortalService"
            elif not runtime_points:
                status = "REJECT_MISSING_RUNTIME_START"
                reason = "retail NPC start is absent from the runtime spawn index"
            elif ai != "portal_dialog":
                status = "REJECT_RUNTIME_CONSUMER"
                reason = f"runtime NPC AI is {ai or 'unset'}, not portal_dialog"
            elif exact:
                status = "ALREADY_DATA_DRIVEN_RETAIL_PROVEN"
                reason = "destination and PortalService requirements match retail evidence"
            else:
                status = "CONVERSION_READY"
                reason = "retail callback is expressible by the existing PortalService consumer"
            candidates.append({
                "npc_id": npc_id,
                "start_world_id": world_id,
                "dialog": event_dialog,
                "callback_shape": shape_id,
                "script_name": evidence["script_name"],
                "callback": evidence["callback"],
                "transport_type": evidence["transport_type"],
                "domain_type": evidence["domain_type"],
                "starts": evidence["starts"],
                "destination": evidence["destination"],
                "requirements": projection["requirements"],
                "runtime_ai": ai,
                "runtime_start_status": start_status,
                "runtime_routes": [{
                    "loc_id": route.get("loc_id"),
                    "destination": route["destination"],
                    "requirements": portal_service_requirements(route),
                    "conversion": route["conversion"],
                } for route in runtime_routes],
                "status": status,
                "reason": reason,
                "source": {
                    "matrix": str(TRANSPORT_SOURCE),
                    "registration": evidence["registration_source"],
                    "callback": evidence["callback_source"],
                },
            })
    candidates.sort(key=lambda row: (
        row["npc_id"], row["start_world_id"], -1 if row["dialog"] is None else row["dialog"], row["script_name"],
        json.dumps(row["destination"], sort_keys=True),
    ))
    return candidates


def transport_type(route: dict[str, object], evidence: dict[str, object] | None) -> str:
    if evidence is not None:
        if evidence["domain_type"] != "UNCLASSIFIED":
            return str(evidence["domain_type"])
        return "SCRIPT_DIALOG_" + str(evidence["transport_type"])
    mechanism = str(route["mechanism"])
    if mechanism in {"portal_use", "portal_dialog", "teleporter", "retail_pattern_alias"}:
        return mechanism.upper()
    runtime_model = route["destination"].get("runtime_model", {})
    return {
        "RELATIVE_CURRENT_POSITION": "RELATIVE_TELEPORT",
        "HOUSING_ADDRESS": "ADDRESS_TELEPORT",
    }.get(runtime_model.get("kind"), "SCRIPT_TELEPORT")


def transport_type_source(route: dict[str, object], evidence: dict[str, object] | None) -> str:
    if evidence is not None:
        return str(TRANSPORT_SOURCE)
    return {
        "portal_use": "src/main/resources/aion/data/static_data/portals/portal_template2.xml",
        "portal_dialog": "src/main/resources/aion/data/static_data/portals/portal_template2.xml",
        "teleporter": "src/main/resources/aion/data/static_data/npc_teleporter.xml",
        "retail_pattern_alias": str(PATTERN_AUDIT),
    }.get(str(route["mechanism"]), str(route["owner"]))


def runtime_consumer(route: dict[str, object]) -> str:
    return {
        "portal_use": "PortalService",
        "portal_dialog": "PortalService",
        "teleporter": "TeleporterData/TeleportService2",
        "retail_pattern_alias": "RetailPatternAI2",
        "legacy_ai": "LEGACY_AI",
        "handler": "INSTANCE_HANDLER",
    }[str(route["mechanism"])]


def endpoint_status(route: dict[str, object], worlds: set[int]) -> str:
    start = start_kind(route, worlds)
    destination = destination_kind(route["destination"].get("world_id"),
                                   bool(route["destination"].get("complete")), worlds)
    return f"{start}_TO_{destination}"


def requirement_shape(requirements: list[dict[str, object]]) -> list[dict[str, object]]:
    return [{
        "attributes": sorted(requirement["attributes"]),
        "quests": sorted(sorted(quest) for quest in requirement["quests"]),
        "items": sorted(sorted(item) for item in requirement["items"]),
    } for requirement in requirements]


def route_signature(route: dict[str, object], worlds: set[int]) -> dict[str, object]:
    semantics = route["semantics"]
    mechanism = route["mechanism"]
    signature: dict[str, object] = {
        "mechanism": mechanism,
        "transport_type": route["transport_type"],
        "endpoint_status": route["endpoint_status"],
        "runtime_consumer": route["runtime_consumer"],
        "start": start_kind(route, worlds),
        "destination": destination_kind(route["destination"].get("world_id"),
                                         bool(route["destination"].get("complete")), worlds),
        "conversion": route["conversion"],
    }
    if route.get("start_owner"):
        signature["start_owner"] = "HANDLER"
    if mechanism in {"portal_use", "portal_dialog"}:
        signature["semantics"] = {
            "attributes": sorted(key for key in semantics["attributes"] if key != "loc_id"),
            "requirements": requirement_shape(semantics["requirements"]),
            "animation": semantics["animation"],
        }
    elif mechanism == "teleporter":
        signature["semantics"] = {
            "attributes": sorted(key for key in semantics["attributes"] if key != "loc_id"),
            "teleport_id": "SET" if semantics["teleport_id"] else "EMPTY",
            "animation": semantics["animation"],
        }
    elif mechanism == "retail_pattern_alias":
        signature["semantics"] = {
            "event": semantics["event"],
            "conditions": semantics["conditions"],
            "target": semantics["target"],
            "showfx": semantics["showfx"],
            "action": semantics["action"],
        }
    else:
        signature["semantics"] = dict(semantics.get("teleport", {}))
        if "trigger" in semantics:
            signature["semantics"]["trigger"] = semantics["trigger"]
    return signature


def attach_batches(matrix: list[dict[str, object]], worlds: set[int]) -> list[dict[str, object]]:
    grouped: dict[str, list[dict[str, object]]] = defaultdict(list)
    signatures = {}
    for world in matrix:
        for route in world["routes"]:
            signature = route_signature(route, worlds)
            key = json.dumps(signature, sort_keys=True, separators=(",", ":"))
            signatures[key] = signature
            grouped[key].append(route)
    batches = []
    for index, key in enumerate(sorted(grouped), 1):
        batch_id = f"B{index:03d}"
        routes = grouped[key]
        for route in routes:
            route["batch_id"] = batch_id
        batches.append({
            "id": batch_id,
            "count": len(routes),
            "signature": signatures[key],
            "owners": sorted({str(route["owner"]) for route in routes}),
        })
    return batches


def build(root: Path) -> dict[str, object]:
    world_data = production_worlds(root)
    world_ids = set(world_data)
    portals = portal_paths(root)
    teleporters = teleporter_paths(root)
    route_definitions: dict[int, list[dict[str, object]]] = defaultdict(list)
    for definitions in (portals, teleporters):
        for npc_id, routes in definitions.items():
            route_definitions[npc_id].extend(routes)
    points = spawn_points(root, world_ids, set(route_definitions))
    dynamic_points = handler_spawn_points(root, set(route_definitions))
    templates = npc_templates(root, {npc_id for _world_id, npc_id in points})
    ai_owners, handlers = java_owners(root)
    start_audit = retail_start_audit(root)
    pattern_audit = retail_pattern_audit(root)
    dynamic_legacy_audit, dynamic_handler_audit = dynamic_route_audit(root)
    transport_sources = source_transport_routes(root)
    aliases = location_aliases(root)
    audited_missing_starts = set()
    audited_legacy_npcs = set()
    audited_dynamic_legacy = set()
    audited_dynamic_handlers = set()
    external_routes: dict[int, list[dict[str, object]]] = defaultdict(list)
    dynamic_routes: dict[int, list[dict[str, object]]] = defaultdict(list)
    points_by_npc: dict[int, list[tuple[int, list[dict[str, str]]]]] = defaultdict(list)
    for (start_world, npc_id), start_points in points.items():
        points_by_npc[npc_id].append((start_world, start_points))
    dynamic_points_by_npc: dict[int, list[tuple[int, list[dict[str, str]]]]] = defaultdict(list)
    for (start_world, npc_id), start_points in dynamic_points.items():
        dynamic_points_by_npc[npc_id].append((start_world, start_points))
    for npc_id, definitions in sorted(route_definitions.items()):
        starts = points_by_npc.get(npc_id, [])
        dynamic_starts = dynamic_points_by_npc.get(npc_id, [])
        external_starts = [(world_id, start_points) for world_id, start_points in starts
                           if world_id not in world_data]
        for definition in definitions:
            for start_world, start_points in dynamic_starts:
                if start_world in world_data and route_allows_world(definition, start_world):
                    dynamic_routes[start_world].append({
                        "npc_id": npc_id,
                        "start_world_id": start_world,
                        "start_points": start_points,
                        "start_dynamic": True,
                        "start_owner": start_points[0]["source"],
                        "association": "START",
                        **definition,
                    })
            destination = definition["destination"]
            destination_world = destination.get("world_id")
            if not destination.get("complete") or destination_world not in world_data:
                continue
            for start_world, start_points in external_starts:
                if not route_allows_world(definition, start_world):
                    continue
                external_routes[destination_world].append({
                    "npc_id": npc_id,
                    "start_world_id": start_world,
                    "start_points": start_points,
                    "association": "DESTINATION",
                    **definition,
                })
            if not starts and not dynamic_starts:
                evidence = start_audit.get(npc_id)
                if evidence is None:
                    raise ValueError(f"missing retail start audit for NPC {npc_id}")
                audited_missing_starts.add(npc_id)
                external_routes[destination_world].append({
                    "npc_id": npc_id,
                    "start_world_id": None,
                    "start_points": [],
                    "association": "DESTINATION",
                    **definition,
                    "conversion": evidence["conversion"],
                    "start_evidence": {key: value for key, value in evidence.items() if key != "conversion"},
                })

    if audited_missing_starts != set(start_audit):
        raise ValueError(f"stale retail start audit NPCs: {sorted(set(start_audit) - audited_missing_starts)}")

    matrix = []
    route_counts = Counter()
    conversion_counts = Counter()
    association_counts = Counter()
    start_endpoint_counts = Counter()
    destination_endpoint_counts = Counter()
    transport_type_counts = Counter()
    type_status_counts = Counter()
    endpoint_status_counts = Counter()
    runtime_consumer_counts = Counter()
    retail_transport_evidence_count = 0
    for world_id, metadata in sorted(world_data.items()):
        starts = []
        routes = []
        for (start_world, npc_id), start_points in sorted(points.items()):
            if start_world != world_id:
                continue
            template = templates.get(npc_id, {"name": "", "retail_name": "", "ai": ""})
            pattern_evidence = pattern_audit.get(npc_id)
            pattern_data_routes = retail_pattern_routes(npc_id, world_id, pattern_evidence, aliases)
            data_routes = [route for route in portals.get(npc_id, []) + teleporters.get(npc_id, [])
                           if route_allows_world(route, world_id)] + pattern_data_routes
            owner = ai_owners.get(template["ai"])
            if not data_routes and owner is None:
                continue
            starts.append({"npc_id": npc_id, **template, "points": start_points})
            for route in data_routes:
                routes.append({"npc_id": npc_id, "start_world_id": world_id, "start_points": start_points,
                               "association": "START", **route})
            if owner is not None:
                if pattern_evidence is not None:
                    audited_legacy_npcs.add(npc_id)
                if pattern_data_routes:
                    continue
                dynamic_key = (owner["file"], world_id, npc_id)
                dynamic_evidence = dynamic_legacy_audit.get(dynamic_key)
                for call in owner["calls"]:
                    if call["source_worlds"] and world_id not in call["source_worlds"]:
                        continue
                    if call["source_npcs"] and npc_id not in call["source_npcs"]:
                        continue
                    if dynamic_evidence is not None:
                        audited_dynamic_legacy.add(dynamic_key)
                    routes.append({"npc_id": npc_id, "start_world_id": world_id, "start_points": start_points,
                                   "association": "START",
                                   **legacy_route(owner, call, pattern_evidence, dynamic_evidence)})

        for handler in handlers:
            if world_id not in handler["world_ids"]:
                continue
            dynamic_key = (handler["file"], world_id)
            dynamic_evidence = dynamic_handler_audit.get(dynamic_key)
            if dynamic_evidence is not None:
                audited_dynamic_handlers.add(dynamic_key)
                for route in audited_handler_routes(handler, dynamic_evidence):
                    routes.append({
                        "npc_id": None,
                        "start_world_id": world_id,
                        "start_points": [],
                        "start_dynamic": True,
                        "association": "START",
                        **route,
                    })
                continue
            for call in handler["calls"]:
                destination = dict(call["destination"])
                routes.append({
                    "npc_id": None,
                    "start_world_id": world_id,
                    "start_points": [],
                    "start_dynamic": True,
                    "association": "START",
                    "mechanism": "handler",
                    "owner": handler["file"],
                    "destination": destination,
                    "semantics": {"raw_call": call["raw"], "teleport": call["teleport"],
                                  "trigger": "UNMODELED"},
                    "conversion": "REJECT_DYNAMIC_TRIGGER",
                })

        routes.extend(external_routes.get(world_id, []))
        routes.extend(dynamic_routes.get(world_id, []))

        routes.sort(key=lambda row: (str(row["npc_id"]), row["mechanism"], row["owner"],
                                     json.dumps(row["destination"], sort_keys=True)))
        for route in routes:
            evidence = source_transport_evidence(route, transport_sources)
            if evidence is not None:
                route["retail_transport_evidence"] = evidence
                retail_transport_evidence_count += 1
            route["transport_type"] = transport_type(route, evidence)
            route["type_source"] = transport_type_source(route, evidence)
            route["type_status"] = "RETAIL_PROVEN" if evidence is not None else "RUNTIME_MODELED"
            route["endpoint_status"] = endpoint_status(route, world_ids)
            route["runtime_consumer"] = runtime_consumer(route)
            route_counts[route["mechanism"]] += 1
            conversion_counts[route["conversion"]] += 1
            association_counts[route["association"]] += 1
            start_endpoint_counts[start_kind(route, world_ids)] += 1
            destination_endpoint_counts[destination_kind(route["destination"].get("world_id"),
                                                          bool(route["destination"].get("complete")),
                                                          world_ids)] += 1
            transport_type_counts[route["transport_type"]] += 1
            type_status_counts[route["type_status"]] += 1
            endpoint_status_counts[route["endpoint_status"]] += 1
            runtime_consumer_counts[route["runtime_consumer"]] += 1
        matrix.append({"world_id": world_id, **metadata, "starts": starts, "routes": routes})

    if audited_legacy_npcs != set(pattern_audit):
        raise ValueError(f"stale retail pattern audit NPCs: {sorted(set(pattern_audit) - audited_legacy_npcs)}")
    if audited_dynamic_legacy != set(dynamic_legacy_audit):
        raise ValueError(f"stale runtime destination audit bindings: {sorted(set(dynamic_legacy_audit) - audited_dynamic_legacy)}")
    if audited_dynamic_handlers != set(dynamic_handler_audit):
        raise ValueError(f"stale runtime handler audit bindings: {sorted(set(dynamic_handler_audit) - audited_dynamic_handlers)}")

    batches = attach_batches(matrix, world_ids)
    transport_candidates = script_transport_candidates(matrix, transport_sources, templates, points)
    candidate_counts = Counter(candidate["status"] for candidate in transport_candidates)
    candidate_start_counts = Counter(candidate["runtime_start_status"] for candidate in transport_candidates)
    summary = {
        "production_worlds": len(matrix),
        "worlds_with_routes": sum(bool(world["routes"]) for world in matrix),
        "start_npcs": sum(len(world["starts"]) for world in matrix),
        "dynamic_start_npcs": len(dynamic_points),
        "routes": sum(route_counts.values()),
        "batches": len(batches),
        "routes_by_association": dict(sorted(association_counts.items())),
        "routes_by_mechanism": dict(sorted(route_counts.items())),
        "routes_by_conversion": dict(sorted(conversion_counts.items())),
        "routes_by_start_endpoint": dict(sorted(start_endpoint_counts.items())),
        "routes_by_destination_endpoint": dict(sorted(destination_endpoint_counts.items())),
        "routes_by_transport_type": dict(sorted(transport_type_counts.items())),
        "routes_by_type_status": dict(sorted(type_status_counts.items())),
        "routes_by_endpoint_status": dict(sorted(endpoint_status_counts.items())),
        "routes_by_runtime_consumer": dict(sorted(runtime_consumer_counts.items())),
        "retail_transport_evidence": retail_transport_evidence_count,
        "script_transport_candidates": len(transport_candidates),
        "script_transport_candidates_by_status": dict(sorted(candidate_counts.items())),
        "script_transport_candidates_by_start_status": dict(sorted(candidate_start_counts.items())),
    }
    return {
        "version": 8,
        "provenance": {
            "kind": "RUNTIME_AUDIT_PROJECTION",
            "authoritative_retail_evidence": False,
            "retail_source_matrices": {
                "direct_portals": "docs/RETAIL_DIRECT_PORTAL_SOURCE_MATRIX.json",
                "script_transports": str(TRANSPORT_SOURCE),
            },
        },
        "summary": summary,
        "script_transport_candidates": transport_candidates,
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
            raise SystemExit(f"stale instance portal matrix: {output}")
        print(f"instance portal matrix is current: {output}")
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
