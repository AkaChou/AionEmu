#!/usr/bin/env python3
"""Extract registered ScriptDLL NPC-dialog transports from retail evidence."""

import argparse
import hashlib
import json
import re
import struct
import tempfile
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path


def load_world_ids(file: Path) -> dict[str, int]:
    world_ids = {}
    for entry in ET.parse(file).getroot().iter("data"):
        name = (entry.text or "").strip().casefold()
        if not name or not entry.get("id"):
            continue
        world_id = int(entry.get("id"))
        if name in world_ids and world_ids[name] != world_id:
            raise ValueError(f"conflicting world id for {name}")
        world_ids[name] = world_id
    return world_ids


REGISTRATION = re.compile(
    r"FUN_180cb2ab0\([^;]*?&(?P<vtable>PTR_vftable_[0-9a-f]+)\s*,\s*0x1b\s*,\s*"
    r"(?P<callback>FUN_[0-9a-f]+)\s*,",
)
FUNCTION_MARKER = re.compile(r"(?m)^// @(?P<address>[0-9a-f]+)\s+(?P<name>FUN_[0-9a-f]+)\s+->")
TRANSPORT_CALL = re.compile(
    r"(?P<raw>\(\*\*\(code \*\*\)\([^;]*?\+\s*(?P<offset>0x2d0|0x2d8|0x2e0)\)\)"
    r"\((?P<args>[^;]+)\);)",
)
VIRTUAL_CALL = re.compile(
    r"(?P<raw>\(\*\*\(code \*\*\)\([^;]*?\+\s*(?P<offset>0x[0-9a-f]+|\d+)\)\)"
    r"\((?P<args>[^;]+)\);)",
    re.IGNORECASE,
)
DIRECT_CALL = re.compile(r"(?P<raw>(?P<name>FUN_[0-9a-f]+)\s*\((?P<args>[^;\n]*)\);)")
INTEGER = re.compile(r"-?(?:0x[0-9a-f]+|\d+)$", re.IGNORECASE)
WIDE_STRING = re.compile(r'L"([^"]+)"$')

PORTAL_SERVICE_REQUIREMENTS = {
    "0a02711fcd9bbf1e": {},
    "0eff6fbaa0598942": {},
    "9b29ef7f2a1f2510": {"race": "ASMODIANS"},
    "a563e194f3586215": {"race": "ASMODIANS", "min_level": 65, "max_level": 100},
    "bbb59816bae2e848": {"race": "ELYOS", "min_level": 65, "max_level": 100},
    "c0693a58b0486877": {},
    "c773c543096b3491": {},
    "e4ab7c479ad41c01": {"min_level": 40, "max_level": 100},
    "f949be6f7be4bac9": {"race": "ELYOS"},
}

RETAIL_TRANSPORT_DOMAIN_TYPES = {
    ("IDNovice_Elevator_Lever_Up", "FUN_180c78400", "0eff6fbaa0598942", "0x2d0"): "LIFT",
}


def source_label(path: Path, root: Path, prefix: str = "") -> str:
    relative = path.resolve().relative_to(root.resolve()).as_posix()
    return f"{prefix}/{relative}" if prefix else relative


def marked_blocks(text: str):
    markers = list(FUNCTION_MARKER.finditer(text))
    for index, marker in enumerate(markers):
        end = markers[index + 1].start() if index + 1 < len(markers) else len(text)
        yield marker, text[marker.start():end]


def script_names(path: Path) -> dict[str, str]:
    text = path.read_text(encoding="utf-8")
    result = {}
    for _marker, block in marked_blocks(text):
        vtable = re.search(r"(PTR_vftable_[0-9a-f]+)\s*=", block)
        name = re.search(r'L"([^"]+)"', block)
        if vtable is None or name is None:
            continue
        previous = result.setdefault(vtable.group(1), name.group(1))
        if previous != name.group(1):
            raise ValueError(f"conflicting script name for {vtable.group(1)}")
    return result


def registrations(path: Path, names: dict[str, str], source_root: Path) -> list[dict[str, object]]:
    text = path.read_text(encoding="utf-8")
    result = []
    for match in REGISTRATION.finditer(text):
        vtable = match.group("vtable")
        if vtable not in names:
            raise ValueError(f"missing script name for registered {vtable}")
        result.append({
            "script_name": names[vtable],
            "vtable": vtable,
            "callback": match.group("callback"),
            "registration_source": {
                "path": source_label(path, source_root),
                "line": text.count("\n", 0, match.start()) + 1,
            },
        })
    if not result:
        raise ValueError(f"no registered NPC dialog callbacks in {path}")
    return result


def callback_blocks(directory: Path, wanted: set[str], source_root: Path) -> dict[str, dict[str, object]]:
    result = {}
    for path in sorted(directory.glob("fun_*.cpp")):
        text = path.read_text(encoding="utf-8")
        if not any(name in text for name in wanted - result.keys()):
            continue
        for marker, block in marked_blocks(text):
            name = marker.group("name")
            if name not in wanted:
                continue
            if name in result:
                raise ValueError(f"duplicate callback body for {name}")
            result[name] = {
                "body": block,
                "path": source_label(path, source_root),
                "line": text.count("\n", 0, marker.start()) + 1,
            }
    return result


def npc_templates(path: Path, wanted: set[str], xml_root: Path) -> dict[str, list[dict[str, object]]]:
    result = defaultdict(list)
    for _event, node in ET.iterparse(path, events=("end",)):
        if node.tag != "npc":
            continue
        ai_name = (node.findtext("ai_name") or "").strip()
        if ai_name.casefold() in wanted:
            result[ai_name.casefold()].append({
                "id": int(node.findtext("id") or "0"),
                "name": (node.findtext("name") or "").strip(),
                "ai_name": ai_name,
                "source": source_label(path, xml_root, "XML"),
            })
        node.clear()
    return {name: sorted(rows, key=lambda row: (row["id"], row["name"])) for name, rows in result.items()}


def world_evidence(worlds_directory: Path, world_ids: dict[str, int], wanted_names: set[str]):
    spawns = defaultdict(list)
    aliases = defaultdict(dict)
    for path in sorted(worlds_directory.glob("*/world.xml")):
        world_name = path.parent.name
        world_id = world_ids.get(world_name.casefold())
        source = f"Worlds/{world_name}/world.xml"
        stack = []
        for event, node in ET.iterparse(path, events=("start", "end")):
            if event == "start":
                stack.append(node.tag)
                continue
            if node.tag == "npc":
                name = (node.findtext("name") or "").strip()
                if name.casefold() in wanted_names:
                    pos = node.find("pos")
                    missing = []
                    if world_id is None:
                        missing.append("world_id")
                    if pos is None or not all((pos.findtext(key) or "").strip() for key in ("x", "y", "z")):
                        missing.append("coordinates")
                    spawns[name.casefold()].append({
                        "complete": not missing,
                        "world_name": world_name,
                        "world_id": world_id,
                        "x": (pos.findtext("x") or "").strip() if pos is not None else "",
                        "y": (pos.findtext("y") or "").strip() if pos is not None else "",
                        "z": (pos.findtext("z") or "").strip() if pos is not None else "",
                        "dir": (node.findtext("dir") or "").strip(),
                        "object_type": (node.findtext("editor_classname") or "Npc").strip(),
                        "conditional": "condition_info" in stack,
                        "spawn_page": dict(node.find("spawn_page").attrib) if node.find("spawn_page") is not None else {},
                        "source": source,
                        "missing": missing,
                    })
                node.clear()
            elif node.tag == "location_alias":
                name = (node.findtext("name") or "").strip()
                if name and world_id is not None:
                    points = []
                    for point in node.findall("points/data"):
                        values = {key: (point.findtext(key) or "").strip() for key in ("x", "y", "z", "dir")}
                        if all(values.values()):
                            points.append(values)
                    if points:
                        key = name.casefold()
                        if key in aliases[world_id] and aliases[world_id][key]["points"] != points:
                            raise ValueError(f"conflicting alias {name} in world {world_id}")
                        aliases[world_id][key] = {"name": name, "points": points, "source": source}
                node.clear()
            elif node.tag == "territory":
                node.clear()
            stack.pop()
    for values in spawns.values():
        values.sort(key=lambda row: (row["world_id"] or 0, row["source"], row["x"], row["y"], row["z"]))
    return dict(spawns), dict(aliases)


def split_args(value: str) -> list[str]:
    result, start, depth, quoted = [], 0, 0, False
    for index, char in enumerate(value):
        if char == '"' and (index == 0 or value[index - 1] != "\\"):
            quoted = not quoted
        elif not quoted and char == "(":
            depth += 1
        elif not quoted and char == ")":
            depth -= 1
        elif not quoted and char == "," and depth == 0:
            result.append(value[start:index].strip())
            start = index + 1
    result.append(value[start:].strip())
    return result


def integer(value: str) -> int | None:
    value = value.strip()
    return int(value, 0) if INTEGER.fullmatch(value) else None


def float32(value: str) -> float | None:
    value = value.strip().rstrip("fFdD")
    if re.fullmatch(r"0x[0-9a-f]{1,8}", value, re.IGNORECASE):
        return struct.unpack(">f", int(value, 16).to_bytes(4, "big"))[0]
    try:
        return float(value)
    except ValueError:
        return None


def matching_brace(text: str, opening: int) -> int | None:
    depth = 0
    for index in range(opening, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return index
    return None


def matching_parenthesis(text: str, opening: int) -> int | None:
    depth = 0
    quoted = False
    for index in range(opening, len(text)):
        char = text[index]
        if char == '"' and (index == 0 or text[index - 1] != "\\"):
            quoted = not quoted
        elif not quoted and char == "(":
            depth += 1
        elif not quoted and char == ")":
            depth -= 1
            if depth == 0:
                return index
    return None


def source_line(callback: dict[str, object], position: int) -> int:
    return int(callback["line"]) + str(callback["body"]).count("\n", 0, position)


def callback_predicates(callback: dict[str, object]) -> list[dict[str, object]]:
    body = str(callback["body"])
    result = []
    for match in re.finditer(r"\b(if|switch)\s*\(", body):
        opening = body.find("(", match.start())
        closing = matching_parenthesis(body, opening)
        if closing is None:
            raise ValueError(f"unterminated callback predicate in {callback['path']}:{callback['line']}")
        expression = " ".join(body[opening + 1:closing].split())
        signature = re.sub(r"\b[A-Za-z_]\w*\b", "_", expression)
        result.append({
            "kind": match.group(1).upper(),
            "expression": expression,
            "signature": "".join(signature.split()),
            "source": {"path": callback["path"], "line": source_line(callback, match.start())},
        })
    return result


def callback_operations(callback: dict[str, object]) -> list[dict[str, object]]:
    body = str(callback["body"])
    operations = []
    matches = [(match.start(), "VIRTUAL", match) for match in VIRTUAL_CALL.finditer(body)]
    matches.extend((match.start(), "DIRECT", match) for match in DIRECT_CALL.finditer(body))
    for position, call_type, match in sorted(matches, key=lambda row: row[0]):
        line_start = body.rfind("\n", 0, position) + 1
        assigned = re.search(r"=\s*$", body[line_start:position]) is not None
        if call_type == "VIRTUAL":
            target = f"0x{int(match.group('offset'), 0):x}"
            kind = "TRANSPORT" if target in {"0x2d0", "0x2d8", "0x2e0"} else "READ" if assigned else "CALL"
        else:
            target = match.group("name")
            kind = "READ" if assigned else "CALL"
        operations.append({
            "kind": kind,
            "call_type": call_type,
            "target": target,
            "raw": " ".join(match.group("raw").split()),
            "source": {"path": callback["path"], "line": source_line(callback, position)},
        })
    return operations


def callback_features(callback: dict[str, object]) -> tuple[dict[str, object], dict[str, object]]:
    predicates = callback_predicates(callback)
    operations = callback_operations(callback)
    signature = {
        "predicates": [f"{predicate['kind']}:{predicate['signature']}" for predicate in predicates],
        "operations": [f"{operation['kind']}:{operation['call_type']}:{operation['target']}"
                       for operation in operations],
    }
    shape_id = hashlib.sha256(json.dumps(signature, sort_keys=True).encode()).hexdigest()[:16]
    return {"shape_id": shape_id, "predicates": predicates, "operations": operations}, signature


def portal_service_projection(status: str, shape_id: str | None) -> dict[str, object]:
    if status == "NOT_TRANSPORT":
        return {"status": "NOT_APPLICABLE", "requirements": {}}
    if status != "ROUTE_PROVEN":
        return {"status": "REJECT_ROUTE_NOT_PROVEN", "requirements": {}}
    requirements = PORTAL_SERVICE_REQUIREMENTS.get(shape_id)
    if requirements is None:
        return {"status": "REJECT_UNMODELED_CALLBACK_SHAPE", "requirements": {}}
    return {"status": "EXPRESSIBLE", "requirements": requirements}


def transport_domain_type(script_name: str, callback: str, shape_id: str, api_offset: str) -> str:
    return RETAIL_TRANSPORT_DOMAIN_TYPES.get((script_name, callback, shape_id, api_offset), "UNCLASSIFIED")


def containing_blocks(body: str, position: int) -> tuple[int, ...]:
    blocks = []
    for match in re.finditer(r"[{}]", body[:position]):
        if match.group() == "{":
            blocks.append(match.start())
        else:
            blocks.pop()
    return tuple(blocks)


def dialog_variables(body: str, position: int) -> set[str]:
    variables = set()
    target_blocks = containing_blocks(body, position)
    for match in re.finditer(r"(?m)^\s*(\w+)\s*=\s*(.*?)\s*;", body[:position]):
        assignment_blocks = containing_blocks(body, match.start())
        if target_blocks[:len(assignment_blocks)] != assignment_blocks:
            continue
        left, right = match.groups()
        source = re.fullmatch(r"\*?(\w+)", right)
        if "(param_2 + 0x28)" in right or source and source.group(1) in variables:
            variables.add(left)
        else:
            variables.discard(left)
    return variables


def event_for_call(body: str, call_position: int) -> dict[str, object] | None:
    conditions = []
    pattern = re.compile(
        r"if\s*\(\s*(?:(?P<left>\*?\w+)\s*(?P<op>==|!=)\s*(?P<right>-?(?:0x[0-9a-f]+|\d+))|"
        r"(?P<reverse>-?(?:0x[0-9a-f]+|\d+))\s*(?P<reverse_op>==|!=)\s*(?P<reverse_var>\*?\w+))\s*\)",
        re.IGNORECASE,
    )
    for match in pattern.finditer(body, 0, call_position):
        variable = (match.group("left") or match.group("reverse_var")).lstrip("*")
        if variable not in dialog_variables(body, match.start()):
            continue
        operation = match.group("op") or match.group("reverse_op")
        value = integer(match.group("right") or match.group("reverse"))
        opening = body.find("{", match.end())
        if opening < 0 or body[match.end():opening].strip():
            continue
        closing = matching_brace(body, opening)
        if closing is None:
            continue
        if operation == "==" and opening < call_position < closing:
            conditions.append((opening, value, match.group(0)))
        elif operation == "!=" and closing < call_position and "return;" in body[opening:closing]:
            between = body[closing + 1:call_position]
            if "else" not in between:
                conditions.append((closing, value, match.group(0)))

    for switch in re.finditer(r"switch\s*\(\s*(\*?\w+)\s*\)\s*\{", body[:call_position]):
        if switch.group(1).lstrip("*") not in dialog_variables(body, switch.start()):
            continue
        closing = matching_brace(body, body.find("{", switch.start()))
        if closing is None or call_position > closing:
            continue
        cases = list(re.finditer(r"(?m)^\s*case\s+(-?(?:0x[0-9a-f]+|\d+))\s*:",
                                 body[switch.end():call_position], re.IGNORECASE))
        if cases:
            case = cases[-1]
            conditions.append((switch.end() + case.start(), integer(case.group(1)), case.group(0).strip()))

    if not conditions:
        return None
    _position, value, evidence = max(conditions, key=lambda row: row[0])
    return {"dialog": value, "evidence": evidence}


def parsed_calls(callback: dict[str, object]) -> list[dict[str, object]]:
    body = callback["body"]
    current_world = set(re.findall(
        r"(?m)^\s*(\w+)\s*=\s*\(\*\*\(code \*\*\)\([^;]*?\+\s*0x338\)\)\([^;]+\);", body,
    ))
    calls = []
    for match in TRANSPORT_CALL.finditer(body):
        offset = match.group("offset")
        args = split_args(match.group("args"))
        reasons = []
        destination = None
        transport_type = {
            "0x2d0": "COORDINATES",
            "0x2d8": "WORLD_ALIAS",
            "0x2e0": "CURRENT_WORLD_ALIAS",
        }[offset]
        expected = {"0x2d0": 7, "0x2d8": 4, "0x2e0": 3}[offset]
        if len(args) != expected:
            reasons.append("unsupported_argument_shape")
        elif offset == "0x2d0":
            world = "CURRENT" if args[1] in current_world else integer(args[1])
            coordinates = [float32(value) for value in args[2:5]]
            direction = integer(args[5])
            mode = integer(args[6])
            if world is None:
                reasons.append("dynamic_world")
            if any(value is None for value in coordinates):
                reasons.append("dynamic_coordinates")
            if direction is None:
                reasons.append("dynamic_direction")
            if mode is None:
                reasons.append("dynamic_mode")
            destination = {
                "kind": "COORDINATES",
                "world": world,
                "x": coordinates[0],
                "y": coordinates[1],
                "z": coordinates[2],
                "dir": direction,
                "mode": mode,
            }
        else:
            world = "CURRENT" if offset == "0x2e0" or args[1] in current_world else integer(args[1])
            alias_arg = args[1] if offset == "0x2e0" else args[2]
            alias = WIDE_STRING.fullmatch(alias_arg)
            mode = integer(args[2] if offset == "0x2e0" else args[3])
            if world is None:
                reasons.append("dynamic_world")
            if alias is None:
                reasons.append("dynamic_alias")
            if mode is None:
                reasons.append("dynamic_mode")
            destination = {
                "kind": "ALIAS",
                "world": world,
                "alias": alias.group(1) if alias else None,
                "mode": mode,
            }
        event = event_for_call(body, match.start())
        if event is None:
            reasons.append("missing_dialog_event")
        calls.append({
            "api_offset": offset,
            "transport_type": transport_type,
            "source": {
                "path": callback["path"],
                "line": callback["line"] + body.count("\n", 0, match.start()),
            },
            "raw_call": " ".join(match.group("raw").split()),
            "event": event,
            "destination": destination,
            "reasons": reasons,
        })
    return calls


def resolved_destination(spec: dict[str, object] | None, start: dict[str, object],
                         aliases: dict[int, dict[str, dict[str, object]]]):
    if spec is None:
        return None, ["missing_destination"]
    world_id = start["world_id"] if spec["world"] == "CURRENT" else spec["world"]
    if world_id is None:
        return None, ["missing_destination_world"]
    if spec["kind"] == "COORDINATES":
        return {
            "complete": True,
            "world_id": world_id,
            "x": spec["x"], "y": spec["y"], "z": spec["z"], "dir": spec["dir"],
            "source": "ScriptDLL64 callback",
        }, []
    alias = aliases.get(world_id, {}).get(str(spec["alias"]).casefold())
    if alias is None:
        return {"complete": False, "world_id": world_id, "alias": spec["alias"]}, ["missing_alias"]
    return {
        "complete": True,
        "world_id": world_id,
        "alias": alias["name"],
        "points": alias["points"],
        "source": alias["source"],
    }, []


def build(script_root: Path, registrations_file: Path, script_names_file: Path, callbacks_directory: Path,
          xml_root: Path, npc_source: Path, worlds_directory: Path, world_ids_file: Path) -> dict[str, object]:
    names = script_names(script_names_file)
    rows = registrations(registrations_file, names, script_root)
    callbacks = callback_blocks(callbacks_directory, {row["callback"] for row in rows}, script_root)
    templates = npc_templates(npc_source, {row["script_name"].casefold() for row in rows}, xml_root)
    wanted_names = {template["name"].casefold() for values in templates.values() for template in values}
    spawns, aliases = world_evidence(worlds_directory, load_world_ids(world_ids_file), wanted_names)
    status_counts = Counter()
    reason_counts = Counter()
    shape_counts = Counter()
    proven_shape_counts = Counter()
    portal_projection_counts = Counter()
    shape_signatures = {}
    call_count = route_count = portal_route_count = 0

    for row in rows:
        callback = callbacks.get(row["callback"])
        row_templates = templates.get(row["script_name"].casefold(), [])
        starts = []
        for template in row_templates:
            for spawn in spawns.get(template["name"].casefold(), []):
                starts.append({"npc_id": template["id"], "npc_name": template["name"], **spawn})
        row["npc_templates"] = row_templates
        row["starts"] = starts
        row["callback_source"] = None if callback is None else {"path": callback["path"], "line": callback["line"]}
        if callback is None:
            row["callback_features"] = None
        else:
            features, signature = callback_features(callback)
            row["callback_features"] = features
            shape_counts[features["shape_id"]] += 1
            shape_signatures[features["shape_id"]] = signature
        calls = [] if callback is None else parsed_calls(callback)
        for call in calls:
            call["domain_type"] = transport_domain_type(
                row["script_name"], row["callback"], row["callback_features"]["shape_id"], call["api_offset"])
        row["calls"] = calls
        reasons = []
        if callback is None:
            reasons.append("missing_callback_body")
        if calls and not row_templates:
            reasons.append("missing_npc_binding")
        if calls and row_templates and not starts:
            reasons.append("missing_start_endpoint")
        for call in calls:
            call_count += 1
            routes = []
            for start in starts:
                destination, route_reasons = resolved_destination(call["destination"], start, aliases)
                combined = sorted(set(call["reasons"] + start["missing"] + route_reasons))
                routes.append({
                    "status": "ENDPOINT_PROVEN" if not combined else "ENDPOINT_REJECTED",
                    "start": start,
                    "destination": destination,
                    "reasons": combined,
                })
                route_count += not combined
            call["routes"] = routes
            if call["reasons"]:
                reasons.extend(call["reasons"])
            if starts and any(route["status"] != "ENDPOINT_PROVEN" for route in routes):
                reasons.append("unresolved_route")
        if not calls and not reasons:
            status = "NOT_TRANSPORT"
        elif calls and not reasons and all(call["routes"] for call in calls):
            status = "ROUTE_PROVEN"
        else:
            status = "ROUTE_REJECTED"
        row["status"] = status
        row["reasons"] = sorted(set(reasons))
        projection = portal_service_projection(
            status, None if row["callback_features"] is None else row["callback_features"]["shape_id"])
        row["portal_service_projection"] = projection
        portal_projection_counts[projection["status"]] += 1
        if projection["status"] == "EXPRESSIBLE":
            portal_route_count += sum(route["status"] == "ENDPOINT_PROVEN"
                                      for call in calls for route in call["routes"])
        if callback is not None and status == "ROUTE_PROVEN":
            proven_shape_counts[row["callback_features"]["shape_id"]] += 1
        status_counts[status] += 1
        reason_counts.update(row["reasons"])

    rows.sort(key=lambda row: (row["script_name"].casefold(), row["callback"], row["vtable"]))
    return {
        "version": 5,
        "provenance": {
            "kind": "RETAIL_SOURCE_MATRIX",
            "authoritative_retail_evidence": True,
            "proof_scope": [
                "callback registration", "NPC binding", "static start", "dialog event",
                "transport API shape", "destination endpoint", "raw callback predicates",
                "callback read/call operation inventory", "normalized callback structure",
                "PortalService requirements for audited callback structures",
                "audited transport domain types",
            ],
            "excluded_semantics": [
                "unaudited predicate semantic interpretation", "non-transport call semantic interpretation",
                "unaudited transport domain types", "runtime consumer selection",
            ],
            "registrations": source_label(registrations_file, script_root),
            "script_names": source_label(script_names_file, script_root),
            "callbacks": source_label(callbacks_directory, script_root) + "/fun_*.cpp",
            "npc_definitions": source_label(npc_source, xml_root, "XML"),
            "world_definitions": "Worlds/*/world.xml",
            "world_ids": source_label(world_ids_file, xml_root, "XML"),
            "transport_api_offsets": {
                "0x2d0": "world and coordinates",
                "0x2d8": "world and location alias",
                "0x2e0": "current-world location alias",
            },
            "transport_domain_type_rules": [{
                "script_name": script_name,
                "callback": callback,
                "callback_shape": shape_id,
                "api_offset": api_offset,
                "domain_type": domain_type,
            } for (script_name, callback, shape_id, api_offset), domain_type
                in sorted(RETAIL_TRANSPORT_DOMAIN_TYPES.items())],
            "portal_service_semantics": {
                "player_field_offsets": {
                    "0x60": {
                        "semantic": "race",
                        "values": {"0": "ELYOS", "1": "ASMODIANS"},
                        "evidence": ["fun/fun_723.cpp:656-658", "fun/fun_723.cpp:801-803"],
                    },
                    "0x70": {
                        "semantic": "level",
                        "evidence": ["fun/fun_723.cpp:2462-2466", "fun/fun_723.cpp:3174-3178"],
                    },
                },
                "terminal_calls": {
                    "0x188": "reject branch",
                    "0x5d8": "finish dialog branch",
                    "FUN_180c51a60": "unmatched dialog fallback",
                },
            },
        },
        "summary": {
            "registrations": len(rows),
            "unique_callbacks": len({row["callback"] for row in rows}),
            "transport_calls": call_count,
            "endpoint_proven_routes": route_count,
            "portal_service_expressible_routes": portal_route_count,
            "callback_shapes": len(shape_signatures),
            "route_proven_callback_shapes": len(proven_shape_counts),
            "by_status": dict(sorted(status_counts.items())),
            "rejected_by_reason": dict(sorted(reason_counts.items())),
            "portal_service_projection_by_status": dict(sorted(portal_projection_counts.items())),
        },
        "callback_shapes": [{
            "id": shape_id,
            "registrations": shape_counts[shape_id],
            "route_proven_registrations": proven_shape_counts[shape_id],
            "signature": shape_signatures[shape_id],
        } for shape_id in sorted(shape_signatures)],
        "registrations": rows,
    }


def render(report: dict[str, object]) -> str:
    return json.dumps(report, ensure_ascii=False, indent=2) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("script_root", type=Path)
    parser.add_argument("registrations", type=Path)
    parser.add_argument("script_names", type=Path)
    parser.add_argument("callbacks", type=Path)
    parser.add_argument("xml_root", type=Path)
    parser.add_argument("npc_source", type=Path)
    parser.add_argument("worlds", type=Path)
    parser.add_argument("world_ids", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    content = render(build(args.script_root, args.registrations, args.script_names, args.callbacks,
                           args.xml_root, args.npc_source, args.worlds, args.world_ids))
    if args.check:
        with tempfile.TemporaryDirectory() as directory:
            generated = Path(directory) / args.output.name
            generated.write_text(content, encoding="utf-8")
            if not args.output.is_file() or args.output.read_bytes() != generated.read_bytes():
                raise SystemExit(f"stale retail ScriptDLL transport matrix: {args.output}")
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(content, encoding="utf-8")
    report = json.loads(content)
    print(f"{'verified' if args.check else 'generated'} {report['summary']['registrations']} registered callbacks")
    print(f"proven {report['summary']['endpoint_proven_routes']} ScriptDLL transport endpoints")


if __name__ == "__main__":
    main()
