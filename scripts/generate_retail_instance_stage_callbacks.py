#!/usr/bin/env python3
"""Build the retail 0x1a NPC interaction callback matrix for production instances."""

from __future__ import annotations

import argparse
import json
import re
import tempfile
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path

from generate_retail_script_transports import (
    VIRTUAL_CALL,
    callback_blocks,
    callback_features,
    integer,
    load_world_ids,
    matching_brace,
    npc_templates,
    registrations,
    script_names,
    source_label,
    split_args,
)


EVENT_CODE = 0x1A
REPORT = Path("docs/RETAIL_INSTANCE_STAGE_CALLBACK_SOURCE_MATRIX.json")
VARIABLE = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")
WIDE_STRING = re.compile(r'L"([^"]*)"$')
OPERATION_FAMILIES = {
    "0x188": "DIALOG_RESULT",
    "0x1d0": "ITEM_CONSUME",
    "0x210": "LEAVE_INSTANCE",
    "0x2a8": "MESSAGE",
    "0x2d0": "TRANSPORT",
    "0x2d8": "TRANSPORT",
    "0x2e0": "TRANSPORT",
    "0x300": "ITEM_COUNT_READ",
    "0x3a0": "MESSAGE",
    "0x578": "NAMED_VARIABLE_WRITE",
    "0x590": "INSTANCE_ACTION",
}
OPERATION_DIMENSIONS = {"0x210": "exit"}
LEAVE_INSTANCE_EVIDENCE = [
    {
        "source": "MainServer_ScriptDLL64/fun/fun_730.cpp:681",
        "meaning": "FUN_180ca2d20 invokes the IUserImp virtual slot at +0x210",
    },
    {
        "source": "MainServer/Server64.exe:.rdata 0x1411768d8+0x210",
        "meaning": "the IUserImp +0x210 slot resolves to FUN_1404dc6a0",
    },
    {
        "source": "MainServer_Server64/fun/fun_050.cpp:8247",
        "meaning": "FUN_1404dc6a0 calls the +0x230 LeaveInstance implementation with flag 0",
    },
    {
        "source": "MainServer_Server64/classes/Account/IUserImp.cpp:1411",
        "meaning": "IUserImp::LeaveInstance delegates the flag to User::LeaveInstance",
    },
    {
        "source": "MainServer_Server64/classes/Account/User.cpp:110991",
        "meaning": "User::LeaveInstance(0) requests the dynamic-world exit before moving the user",
    },
    {
        "source": "MainServer_Server64/classes/World/DynamicWorld.cpp:3462",
        "meaning": "DynamicWorld uses its exit alias when present and otherwise copies the player's saved location",
    },
]
LEAVE_INSTANCE_SOURCE_CHECKS = {
    Path("fun/fun_050.cpp"): (
        "void FUN_1404dc6a0",
        "(*param_1 + 0x230))(param_1,0);",
    ),
    Path("classes/Account/IUserImp.cpp"): (
        "void IUserImp_LeaveInstance",
        "User_LeaveInstance(*(int64_t *)(param_1 + 8),param_2);",
    ),
    Path("classes/Account/User.cpp"): (
        "void User_LeaveInstance",
        "if (param_2 != '\\0')",
        "WorldDb_GetDynamicWorld()",
        "*plVar9 + 0x1c8",
        "*in_RCX + 0x150",
    ),
    Path("classes/World/DynamicWorld.cpp"): (
        "bool DynamicWorld_ExitPointLocAlias",
        "*param_1 + 0x1b8",
        "if (_Src == (wchar_t *)0x0)",
        "*(uint64_t *)(param_2 + 0x43d8)",
        "wcsncpy_s",
    ),
}


def validate_leave_instance_semantics(script_root: Path) -> None:
    server_root = script_root.parent / "MainServer_Server64"
    for relative, tokens in LEAVE_INSTANCE_SOURCE_CHECKS.items():
        path = server_root / relative
        source = path.read_text(encoding="utf-8")
        missing = [token for token in tokens if token not in source]
        if missing:
            raise ValueError(f"stale leave-instance semantic evidence in {relative}: {missing}")


def production_worlds(path: Path, world_ids_path: Path) -> dict[int, dict[str, str]]:
    retail_names = {}
    for node in ET.parse(world_ids_path).getroot():
        world_id = int(node.get("id", "0"))
        name = (node.text or "").strip()
        if world_id <= 0 or not name or world_id in retail_names:
            raise ValueError(f"invalid retail world mapping for {world_id}")
        retail_names[world_id] = name
    result = {}
    for node in ET.parse(path).getroot().findall("map"):
        if node.get("instance") != "true":
            continue
        world_id = int(node.get("id", "0"))
        retail_name = retail_names.get(world_id)
        if world_id <= 0 or not retail_name or world_id in result:
            raise ValueError(f"invalid production instance world {world_id}")
        result[world_id] = {
            "local_name": node.get("name", ""),
            "retail_name": retail_name,
        }
    if len(result) != 139:
        raise ValueError(f"expected 139 production instance worlds, found {len(result)}")
    return result


def expression_variables(expression: str) -> set[str]:
    return {name.casefold() for name in VARIABLE.findall(expression)
            if name.casefold() not in {"and", "or", "true", "false"}}


def retail_instance_evidence(worlds_directory: Path, world_ids: dict[str, int],
                             production_ids: set[int], wanted_names: set[str]):
    spawns: dict[str, list[dict[str, object]]] = defaultdict(list)
    conditions: dict[int, list[dict[str, str]]] = defaultdict(list)
    for directory in sorted(path for path in worlds_directory.iterdir() if path.is_dir()):
        world_id = world_ids.get(directory.name.casefold())
        if world_id not in production_ids:
            continue
        for filename in ("world.xml", "world_N.xml"):
            path = directory / filename
            if not path.is_file():
                continue
            source = f"Worlds/{directory.name}/{filename}"
            stack: list[ET.Element] = []
            for event, node in ET.iterparse(path, events=("start", "end")):
                if event == "start":
                    stack.append(node)
                    continue
                if node.tag == "extcondition":
                    expression = (node.text or "").strip()
                    if expression:
                        conditions[world_id].append({"expression": expression, "source": source})
                elif node.tag == "npc":
                    name = (node.findtext("name") or "").strip()
                    if name.casefold() in wanted_names:
                        condition = ""
                        for ancestor in reversed(stack[:-1]):
                            if ancestor.tag == "condition_info":
                                condition = (ancestor.findtext("condition/extcondition") or "").strip()
                                break
                        pos = node.find("pos")
                        spawns[name.casefold()].append({
                            "world_id": world_id,
                            "world_name": directory.name,
                            "source": source,
                            "spawn_type": "CONDITIONAL" if condition else "STATIC",
                            "condition": condition,
                            "x": (pos.findtext("x") or "").strip() if pos is not None else "",
                            "y": (pos.findtext("y") or "").strip() if pos is not None else "",
                            "z": (pos.findtext("z") or "").strip() if pos is not None else "",
                            "dir": (node.findtext("dir") or "").strip(),
                        })
                if node.tag in {"npc", "territory", "condition_info"}:
                    node.clear()
                stack.pop()
    for name, rows in spawns.items():
        unique = {json.dumps(row, sort_keys=True): row for row in rows}
        spawns[name] = sorted(unique.values(), key=lambda row: (
            int(row["world_id"]), str(row["source"]), str(row["condition"]),
            str(row["x"]), str(row["y"]), str(row["z"])))
    for world_id, rows in conditions.items():
        unique = {(row["expression"], row["source"]): row for row in rows}
        conditions[world_id] = [unique[key] for key in sorted(unique)]
    return dict(spawns), dict(conditions)


def instance_exit_models(retail_root: Path, worlds_directory: Path, world_ids: dict[str, int],
                         production_ids: set[int]) -> dict[int, dict[str, object]]:
    source = retail_root / "China/instance_cooltime.xml"
    models = {
        world_id: {
            "model": "UNMODELED",
            "status": "REJECT_MISSING_INSTANCE_RULE",
            "source": source_label(source, retail_root, "XML"),
            "endpoints": [],
        }
        for world_id in production_ids
    }
    wanted_aliases: dict[str, set[str]] = defaultdict(set)
    rules = {}
    for node in ET.parse(source).getroot().findall("instance_cooltime"):
        name = (node.findtext("name") or "").strip()
        world_id = world_ids.get(name.casefold())
        if world_id not in production_ids:
            continue
        if world_id in rules:
            raise ValueError(f"duplicate instance exit rule for world {world_id}")
        rules[world_id] = node
        endpoints = []
        partial = False
        for slot in (1, 2):
            exit_world = (node.findtext(f"exit_world_{slot}") or "").strip()
            alias = (node.findtext(f"exit_alias_{slot}") or "").strip()
            if bool(exit_world) != bool(alias):
                partial = True
                continue
            if not exit_world:
                continue
            destination_world_id = world_ids.get(exit_world.casefold())
            endpoints.append({
                "slot": slot,
                "world_name": exit_world,
                "world_id": destination_world_id,
                "alias": alias,
                "points": [],
                "source": "",
            })
            wanted_aliases[exit_world.casefold()].add(alias.casefold())
        if partial:
            models[world_id] = {
                "model": "UNMODELED",
                "status": "REJECT_INCOMPLETE_INSTANCE_RULE",
                "source": source_label(source, retail_root, "XML"),
                "rule_id": int((node.findtext("id") or "0").strip()),
                "endpoints": endpoints,
            }
        elif endpoints:
            models[world_id] = {
                "model": "INSTANCE_RULE_ALIAS",
                "status": "RESOLVED",
                "source": source_label(source, retail_root, "XML"),
                "rule_id": int((node.findtext("id") or "0").strip()),
                "endpoints": endpoints,
            }
        else:
            models[world_id] = {
                "model": "PLAYER_PREVIOUS_LOCATION",
                "status": "RESOLVED",
                "source": source_label(source, retail_root, "XML"),
                "rule_id": int((node.findtext("id") or "0").strip()),
                "endpoints": [],
            }

    folders = {path.name.casefold(): path for path in worlds_directory.iterdir() if path.is_dir()}
    aliases = {}
    for world_name, wanted in wanted_aliases.items():
        folder = folders.get(world_name)
        if folder is None:
            continue
        for filename in ("world.xml",):
            path = folder / filename
            if not path.is_file():
                continue
            for node in ET.parse(path).getroot().findall("location_alias_list/location_alias"):
                name = (node.findtext("name") or "").strip()
                key = name.casefold()
                if key not in wanted:
                    continue
                points = []
                for point in node.findall("points/data"):
                    values = {field: (point.findtext(field) or "").strip() for field in ("x", "y", "z", "dir")}
                    if not all(values.values()):
                        raise ValueError(f"incomplete instance exit alias point {folder.name}/{name}")
                    points.append(values)
                if not points:
                    raise ValueError(f"empty instance exit alias {folder.name}/{name}")
                alias_key = (world_name, key)
                value = {"points": points, "source": f"Worlds/{folder.name}/{filename}"}
                if alias_key in aliases and aliases[alias_key] != value:
                    raise ValueError(f"conflicting instance exit alias {folder.name}/{name}")
                aliases[alias_key] = value

    for model in models.values():
        if model["model"] != "INSTANCE_RULE_ALIAS" or model["status"] != "RESOLVED":
            continue
        for endpoint in model["endpoints"]:
            evidence = aliases.get((str(endpoint["world_name"]).casefold(), str(endpoint["alias"]).casefold()))
            if endpoint["world_id"] is None:
                model["status"] = "REJECT_MISSING_EXIT_WORLD"
            elif evidence is None:
                model["status"] = "REJECT_MISSING_EXIT_ALIAS"
            else:
                endpoint.update(evidence)
    return models


def literal_arguments(arguments: list[str]) -> list[dict[str, object]]:
    result = []
    for index, value in enumerate(arguments):
        number = integer(value)
        string = WIDE_STRING.fullmatch(value)
        if number is not None:
            result.append({"index": index, "type": "INTEGER", "value": number})
        elif string is not None:
            result.append({"index": index, "type": "STRING", "value": string.group(1)})
    return result


def assigned_integer_values(body: str, expression: str) -> list[int]:
    if not re.fullmatch(r"[A-Za-z_]\w*", expression):
        return []
    pattern = re.compile(rf"\b{re.escape(expression)}\s*=\s*(-?(?:0x[0-9a-f]+|\d+))\s*;", re.IGNORECASE)
    return sorted({int(match.group(1), 0) for match in pattern.finditer(body)})


def assigned_item_gate_messages(body: str, expression: str) -> dict[str, int]:
    if not re.fullmatch(r"[A-Za-z_]\w*", expression):
        return {}
    assignment = re.compile(
        rf"\b{re.escape(expression)}\s*=\s*(-?(?:0x[0-9a-f]+|\d+))\s*;", re.IGNORECASE)
    matches = [(match.start(), int(match.group(1), 0)) for match in assignment.finditer(body)]
    if len(matches) != 2:
        return {}
    for opening in (match.start() for match in re.finditer(r"\{", body)):
        closing = matching_brace(body, opening)
        if closing is None:
            continue
        block = body[opening:closing]
        if "+ 0x1d0" not in block or "+ 0x578" not in block:
            continue
        success = [value for position, value in matches if opening < position < closing]
        failure = [value for position, value in matches if not opening < position < closing]
        if len(success) == 1 and len(failure) == 1:
            return {"failure_message_id": failure[0], "success_message_id": success[0]}
    return {}


def parsed_operation(operation: dict[str, object], callback: dict[str, object]) -> dict[str, object]:
    row = dict(operation)
    family = OPERATION_FAMILIES.get(str(operation["target"]), "UNMODELED")
    row["family"] = family
    row["dimension"] = OPERATION_DIMENSIONS.get(str(operation["target"]))
    row["semantic_status"] = "RESOLVED" if family != "UNMODELED" else "REJECT_UNMODELED_OPERATION"
    call = VIRTUAL_CALL.fullmatch(str(operation["raw"]))
    arguments = split_args(call.group("args")) if call is not None else []
    row["literal_arguments"] = literal_arguments(arguments)
    parsed = {}
    if family == "NAMED_VARIABLE_WRITE" and len(arguments) >= 3:
        name = WIDE_STRING.fullmatch(arguments[1])
        value = integer(arguments[2])
        if name is not None:
            parsed["variable"] = name.group(1)
        if value is not None:
            parsed["value"] = value
    elif family in {"ITEM_COUNT_READ", "ITEM_CONSUME"} and len(arguments) >= 2:
        item_id = integer(arguments[1])
        if item_id is not None:
            parsed["item_id"] = item_id
        if family == "ITEM_CONSUME" and len(arguments) >= 3:
            count = integer(arguments[2])
            if count is not None:
                parsed["count"] = count
    elif family == "INSTANCE_ACTION" and len(arguments) >= 3:
        action = WIDE_STRING.fullmatch(arguments[2])
        if action is not None:
            parsed["action"] = action.group(1)
    elif family == "LEAVE_INSTANCE":
        parsed["flag"] = False
        row["semantic_evidence"] = LEAVE_INSTANCE_EVIDENCE
    elif family in {"MESSAGE", "DIALOG_RESULT"} and len(arguments) >= 2:
        message = integer(arguments[1])
        if message is not None:
            parsed["message_ids"] = [message]
        else:
            values = assigned_integer_values(str(callback["body"]), arguments[1])
            if values:
                parsed["message_ids"] = values
                parsed.update(assigned_item_gate_messages(str(callback["body"]), arguments[1]))
    row["parsed"] = parsed
    row["parse_status"] = "PARSED" if parsed else "RAW_ONLY" if family != "UNMODELED" else "UNMODELED"
    return row


def registration_source_status(row: dict[str, object], callback: dict[str, object] | None,
                               templates: list[dict[str, object]]) -> str:
    if row["registration_binding"] == "DYNAMIC":
        return "REJECT_DYNAMIC_REGISTRATION"
    if row["script_name"] is None:
        return "REJECT_MISSING_SCRIPT_NAME"
    if callback is None:
        return "REJECT_MISSING_CALLBACK_BODY"
    if not templates:
        return "REJECT_MISSING_NPC_BINDING"
    return "SOURCE_MODELED"


def runtime_item_gate_variables(path: Path) -> dict[int, dict[str, object]]:
    result = {}
    for node in ET.parse(path).getroot().findall("item_gate_variable"):
        npc_id = int(node.get("npc_id", "0"))
        row = {
            "world_id": int(node.get("world_id", "0")),
            "item_id": int(node.get("item_id", "0")),
            "item_count": int(node.get("item_count", "1")),
            "variable": node.get("variable", ""),
            "value": int(node.get("value", "0")),
            "failure_message_id": int(node.get("failure_message_id", "0")),
            "success_message_id": int(node.get("success_message_id", "0")),
        }
        if npc_id <= 0 or npc_id in result:
            raise ValueError(f"invalid duplicate runtime item gate NPC {npc_id}")
        result[npc_id] = row
    return result


def runtime_npc_ai(path: Path, wanted: set[int]) -> dict[int, str]:
    result = {}
    for _event, node in ET.iterparse(path, events=("end",)):
        if node.tag == "npc_template":
            npc_id = int(node.get("npc_id", "0"))
            if npc_id in wanted:
                result[npc_id] = node.get("ai", "")
        node.clear()
    return result


def item_gate_variable_candidate(row: dict[str, object], catalog: dict[str, object],
                                 condition_variables: dict[int, set[str]],
                                 runtime_entries: dict[int, dict[str, object]],
                                 runtime_ai: dict[int, str]) -> dict[str, object] | None:
    operations = catalog["operations"]
    if [operation["target"] for operation in operations] != ["0x300", "0x1d0", "0x578", "0x3a0"]:
        return None
    parsed = {str(operation["family"]): operation["parsed"] for operation in operations}
    world_ids = sorted({int(binding["world_id"]) for binding in row["production_instance_bindings"]})
    npc_ids = sorted({int(binding["npc_id"]) for binding in row["production_instance_bindings"]})
    reasons = []
    item_id = parsed["ITEM_COUNT_READ"].get("item_id")
    consume_item_id = parsed["ITEM_CONSUME"].get("item_id")
    item_count = parsed["ITEM_CONSUME"].get("count")
    variable = parsed["NAMED_VARIABLE_WRITE"].get("variable")
    value = parsed["NAMED_VARIABLE_WRITE"].get("value")
    failure_message_id = parsed["MESSAGE"].get("failure_message_id")
    success_message_id = parsed["MESSAGE"].get("success_message_id")
    if len(world_ids) != 1:
        reasons.append("multiple_runtime_worlds")
    if item_id is None or item_id != consume_item_id or item_count is None:
        reasons.append("unclosed_item_transaction")
    if variable is None or value is None:
        reasons.append("unclosed_variable_write")
    if failure_message_id is None or success_message_id is None:
        reasons.append("unclosed_message_branches")
    if variable is not None and any(variable.casefold() not in condition_variables.get(world_id, set())
                                    for world_id in world_ids):
        reasons.append("variable_not_consumed_by_retail_condition")
    expected = None
    if not reasons:
        expected = {
            "world_id": world_ids[0],
            "item_id": item_id,
            "item_count": item_count,
            "variable": variable,
            "value": value,
            "failure_message_id": failure_message_id,
            "success_message_id": success_message_id,
        }
        runtime_values = [runtime_entries.get(npc_id) for npc_id in npc_ids]
        if any(runtime_ai.get(npc_id) != "useitem" for npc_id in npc_ids):
            reasons.append("runtime_consumer_not_useitem")
        elif all(value == expected for value in runtime_values):
            status = "ALREADY_DATA_DRIVEN"
        elif all(value is None for value in runtime_values):
            status = "CONVERSION_READY"
        else:
            reasons.append("runtime_data_mismatch")
    if reasons:
        status = "REJECT_EVIDENCE_GAP"
    return {
        "family": "ITEM_GATE_VARIABLE",
        "script_name": row["script_name"],
        "callback": row["callback"],
        "shape_id": catalog["shape_id"],
        "world_ids": world_ids,
        "npc_ids": npc_ids,
        "expected_runtime": expected,
        "status": status,
        "reasons": sorted(reasons),
    }


def build(script_root: Path, retail_root: Path, aionemu_root: Path) -> dict[str, object]:
    validate_leave_instance_semantics(script_root)
    fun = script_root / "fun"
    names_path = script_root / "classes/NPC/IAIScriptNpcImp.cpp"
    npc_path = retail_root / "China/npcs.xml"
    world_ids_path = retail_root / "China/ID/WorldId.xml"
    worlds_directory = retail_root.parent / "Worlds"
    world_maps_path = aionemu_root / "src/main/resources/aion/data/static_data/world_maps.xml"

    worlds = production_worlds(world_maps_path, world_ids_path)
    names = script_names(names_path)
    rows = registrations(fun, names, script_root, EVENT_CODE)
    callbacks = callback_blocks(fun, {str(row["callback"]) for row in rows}, script_root)
    templates = npc_templates(
        npc_path, {str(row["script_name"]).casefold() for row in rows if row["script_name"]}, retail_root)
    wanted_names = {str(template["name"]).casefold() for values in templates.values() for template in values}
    world_ids = load_world_ids(world_ids_path)
    spawns, conditions = retail_instance_evidence(worlds_directory, world_ids, set(worlds), wanted_names)
    exit_models = instance_exit_models(retail_root, worlds_directory, world_ids, set(worlds))
    condition_variables = {
        world_id: {variable for row in entries for variable in expression_variables(row["expression"])}
        for world_id, entries in conditions.items()
    }

    callback_catalog = {}
    shape_signatures = {}
    callback_shape_counts = Counter()
    for name, callback in callbacks.items():
        features, signature = callback_features(callback)
        operations = [parsed_operation(operation, callback) for operation in features["operations"]]
        callback_catalog[name] = {
            "source": {"path": callback["path"], "line": callback["line"]},
            "shape_id": features["shape_id"],
            "predicates": features["predicates"],
            "operations": operations,
        }
        shape_signatures[features["shape_id"]] = signature
        callback_shape_counts[features["shape_id"]] += 1

    world_links: dict[int, list[dict[str, object]]] = defaultdict(list)
    source_statuses = Counter()
    scope_statuses = Counter()
    instance_statuses = Counter()
    operation_targets = Counter()
    operation_families = Counter()
    instance_callbacks = set()
    instance_scripts = set()
    instance_bindings = 0
    for row in rows:
        script_key = str(row["script_name"]).casefold() if row["script_name"] else ""
        row_templates = templates.get(script_key, [])
        bindings = []
        for template in row_templates:
            for spawn in spawns.get(str(template["name"]).casefold(), []):
                bindings.append({"npc_id": template["id"], "npc_name": template["name"], **spawn})
        bindings.sort(key=lambda binding: (
            int(binding["world_id"]), int(binding["npc_id"]), str(binding["source"]),
            str(binding["condition"]), str(binding["x"]), str(binding["y"]), str(binding["z"])))
        callback = callbacks.get(str(row["callback"]))
        source_status = registration_source_status(row, callback, row_templates)
        scope_status = "PRODUCTION_INSTANCE_REACHABLE" if bindings else "OUTSIDE_PRODUCTION_INSTANCE_CLOSURE"
        instance_status = source_status if bindings and source_status != "SOURCE_MODELED" else (
            "INSTANCE_SOURCE_MODELED" if bindings else "NOT_APPLICABLE")
        row["npc_templates"] = row_templates
        row["production_instance_bindings"] = bindings
        row["source_status"] = source_status
        row["scope_status"] = scope_status
        row["instance_status"] = instance_status
        source_statuses[source_status] += 1
        scope_statuses[scope_status] += 1
        instance_statuses[instance_status] += 1
        if not bindings:
            continue
        instance_bindings += len(bindings)
        instance_callbacks.add(str(row["callback"]))
        if row["script_name"]:
            instance_scripts.add(str(row["script_name"]))
        catalog = callback_catalog.get(str(row["callback"]))
        if catalog is not None:
            operation_targets.update(str(operation["target"]) for operation in catalog["operations"])
            operation_families.update(str(operation["family"]) for operation in catalog["operations"])
        for world_id in sorted({int(binding["world_id"]) for binding in bindings}):
            world_bindings = [binding for binding in bindings if int(binding["world_id"]) == world_id]
            writes = []
            if catalog is not None:
                for operation in catalog["operations"]:
                    variable = operation["parsed"].get("variable")
                    if variable:
                        writes.append({
                            "variable": variable,
                            "value": operation["parsed"].get("value"),
                            "consumed_by_retail_condition": variable.casefold() in condition_variables.get(world_id, set()),
                            "source": operation["source"],
                        })
            operations = [] if catalog is None else catalog["operations"]
            leave_operations = [operation for operation in operations if operation["family"] == "LEAVE_INSTANCE"]
            link = {
                "script_name": row["script_name"],
                "callback": row["callback"],
                "shape_id": None if catalog is None else catalog["shape_id"],
                "npc_ids": sorted({int(binding["npc_id"]) for binding in world_bindings}),
                "spawn_count": len(world_bindings),
                "spawn_sources": sorted({str(binding["source"]) for binding in world_bindings}),
                "start_bindings": world_bindings,
                "condition_variable_writes": writes,
                "instance_status": instance_status,
            }
            if leave_operations:
                link["leave_instance"] = {
                    "operations": leave_operations,
                    "endpoint": exit_models[world_id],
                }
            world_links[world_id].append(link)

    rows.sort(key=lambda row: (
        (str(row["script_name"]) if row["script_name"] else "").casefold(),
        str(row["callback"]), str(row["vtable"]), str(row["registration_source"]["path"]),
        int(row["registration_source"]["line"])))
    batches = []
    by_shape: dict[str, list[dict[str, object]]] = defaultdict(list)
    for world_id, links in world_links.items():
        for link in links:
            if link["shape_id"]:
                by_shape[str(link["shape_id"])].append({"world_id": world_id, **link})
    for shape_id, links in sorted(by_shape.items()):
        batches.append({
            "shape_id": shape_id,
            "world_ids": sorted({int(link["world_id"]) for link in links}),
            "scripts": sorted({str(link["script_name"]) for link in links if link["script_name"]}),
            "callbacks": sorted({str(link["callback"]) for link in links}),
            "npc_ids": sorted({npc_id for link in links for npc_id in link["npc_ids"]}),
        })
    world_matrix = [{
        "world_id": world_id,
        **metadata,
        "condition_sources": conditions.get(world_id, []),
        "callback_bindings": sorted(world_links.get(world_id, []), key=lambda link: (
            (str(link["script_name"]) if link["script_name"] else "").casefold(), str(link["callback"]))),
    } for world_id, metadata in sorted(worlds.items())]

    runtime_entries = runtime_item_gate_variables(
        aionemu_root / "src/main/resources/aion/definitions/compact/script-npcs.xml")
    runtime_ai = runtime_npc_ai(
        aionemu_root / "src/main/resources/aion/data/static_data/npcs/npc_template.xml",
        {int(binding["npc_id"]) for row in rows for binding in row["production_instance_bindings"]})
    conversion_candidates = []
    for row in rows:
        catalog = callback_catalog.get(str(row["callback"]))
        if catalog is None or row["instance_status"] != "INSTANCE_SOURCE_MODELED":
            continue
        candidate = item_gate_variable_candidate(
            row, catalog, condition_variables, runtime_entries, runtime_ai)
        if candidate is not None:
            conversion_candidates.append(candidate)
    candidate_statuses = Counter(candidate["status"] for candidate in conversion_candidates)

    return {
        "version": 2,
        "provenance": {
            "kind": "RETAIL_SOURCE_MATRIX",
            "authoritative_retail_evidence": True,
            "proof_scope": [
                "0x1a callback registration", "NPC script binding", "production instance spawn reachability",
                "raw callback predicates", "callback operation inventory", "literal operation arguments",
                "named variable writes", "item count and consume calls", "message call identifiers",
                "leave-instance operation semantics", "instance-rule exit aliases", "previous-location fallback",
            ],
            "excluded_semantics": [
                "unmapped virtual call semantics", "branch ordering and transaction semantics",
                "runtime consumer selection", "conversion readiness", "restart recovery behavior",
            ],
            "registrations": "fun/fun_*.cpp",
            "script_names": "classes/NPC/IAIScriptNpcImp.cpp",
            "callbacks": "fun/fun_*.cpp",
            "npc_definitions": source_label(npc_path, retail_root, "XML"),
            "world_definitions": "Worlds/*/world.xml|world_N.xml",
            "world_ids": source_label(world_ids_path, retail_root, "XML"),
            "production_world_enumeration": str(world_maps_path.relative_to(aionemu_root)),
            "main_server_semantics": "MainServer_Server64",
            "operation_families": dict(sorted(OPERATION_FAMILIES.items())),
            "leave_instance_semantic_evidence": LEAVE_INSTANCE_EVIDENCE,
        },
        "summary": {
            "production_worlds": len(worlds),
            "registrations": len(rows),
            "registrations_by_binding": dict(sorted(Counter(str(row["registration_binding"]) for row in rows).items())),
            "unique_callbacks": len({str(row["callback"]) for row in rows}),
            "callback_bodies": len(callback_catalog),
            "missing_callback_bodies": sorted({str(row["callback"]) for row in rows} - set(callback_catalog)),
            "callback_shapes": len(shape_signatures),
            "source_statuses": dict(sorted(source_statuses.items())),
            "scope_statuses": dict(sorted(scope_statuses.items())),
            "instance_statuses": dict(sorted(instance_statuses.items())),
            "instance_worlds_with_callbacks": sum(bool(world["callback_bindings"]) for world in world_matrix),
            "instance_bindings": instance_bindings,
            "instance_scripts": len(instance_scripts),
            "instance_callbacks": len(instance_callbacks),
            "instance_batches": len(batches),
            "instance_operation_targets": dict(sorted(operation_targets.items())),
            "instance_operation_families": dict(sorted(operation_families.items())),
            "leave_instance_worlds": sum(
                "leave_instance" in link for links in world_links.values() for link in links),
            "leave_instance_endpoint_models": dict(sorted(Counter(
                str(link["leave_instance"]["endpoint"]["model"])
                for links in world_links.values() for link in links if "leave_instance" in link).items())),
            "leave_instance_endpoint_statuses": dict(sorted(Counter(
                str(link["leave_instance"]["endpoint"]["status"])
                for links in world_links.values() for link in links if "leave_instance" in link).items())),
            "conversion_candidates": len(conversion_candidates),
            "conversion_candidate_statuses": dict(sorted(candidate_statuses.items())),
        },
        "callback_shapes": [{
            "id": shape_id,
            "callbacks": callback_shape_counts[shape_id],
            "signature": shape_signatures[shape_id],
        } for shape_id in sorted(shape_signatures)],
        "callback_catalog": [{"callback": name, **callback_catalog[name]} for name in sorted(callback_catalog)],
        "instance_batches": batches,
        "conversion_candidates": conversion_candidates,
        "worlds": world_matrix,
        "registrations": rows,
    }


def render(report: dict[str, object]) -> str:
    return json.dumps(report, ensure_ascii=False, indent=2) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("script_root", type=Path)
    parser.add_argument("retail_root", type=Path)
    parser.add_argument("aionemu_root", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    content = render(build(args.script_root.resolve(), args.retail_root.resolve(), args.aionemu_root.resolve()))
    if args.check:
        with tempfile.TemporaryDirectory() as directory:
            generated = Path(directory) / args.output.name
            generated.write_text(content, encoding="utf-8")
            if not args.output.is_file() or args.output.read_bytes() != generated.read_bytes():
                raise SystemExit(f"stale retail instance stage callback matrix: {args.output}")
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(content, encoding="utf-8")
    summary = json.loads(content)["summary"]
    print(f"{'verified' if args.check else 'generated'} {summary['registrations']} registered 0x1a callbacks")
    print(f"modeled {summary['instance_bindings']} production instance bindings")


if __name__ == "__main__":
    main()
