#!/usr/bin/env python3
"""Resolve ScriptDLL quest callback thunks and compile mechanical callback facts."""

import argparse
import hashlib
import json
import re
import struct
from collections import Counter, defaultdict
from pathlib import Path

try:
    from scripts.generate_retail_quest_script_index import (
        DEFAULT_DLL,
        DEFAULT_SCRIPT_ROOT,
        load_pe,
        section_for,
        sha256,
        symbol_address,
    )
    from scripts.generate_retail_script_transports import (
        VIRTUAL_CALL,
        callback_features,
        marked_blocks,
        split_args,
    )
except ModuleNotFoundError:
    from generate_retail_quest_script_index import (
        DEFAULT_DLL,
        DEFAULT_SCRIPT_ROOT,
        load_pe,
        section_for,
        sha256,
        symbol_address,
    )
    from generate_retail_script_transports import VIRTUAL_CALL, callback_features, marked_blocks, split_args


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INDEX = ROOT / "docs/RETAIL_QUEST_SCRIPT_INDEX.json"
DEFAULT_OUTPUT = ROOT / "docs/RETAIL_QUEST_CALLBACK_IR.json"
DEFAULT_SEMANTICS = ROOT / "scripts/retail-quest-callback-semantics.json"
DEFAULT_NPC_SOURCE_ROOT = Path("/Users/mc/IdeaProjects/58Server/server58/NPCServer_NPCSvr64")
DEFAULT_NPC_SERVER_BINARY = Path("/Users/mc/IdeaProjects/58Server/NPCServer/NPCSvr64.exe")
FUNCTION_MARKER = re.compile(r"(?m)^// @(?P<address>[0-9a-f]+)\s+(?P<symbol>FUN_[0-9a-f]+)\s+->")
RECOVERED_FUNCTION_MARKER = re.compile(r"(?m)^/\* (?P<address>[0-9a-f]+) \*/")
MEMORY_REFERENCE = re.compile(
    r"\*\([^)]*\)\(\s*(?P<base>param_\d+)\s*\+\s*(?P<offset>0x[0-9a-f]+|\d+)\s*\)",
    re.IGNORECASE,
)
ASSIGNMENT = re.compile(r"(?<![=!<>])=(?!=)")
DATA_REFERENCE = re.compile(r"\b(?:DAT|PTR)_[0-9a-f]+\b")
WIDE_STRING = re.compile(r'L"((?:\\.|[^"\\])*)"')
CALL_RECEIVER = re.compile(r"\)\)\((?P<receiver>[^,;)]+)")
SEMANTIC_CATEGORIES = {"EVENT", "CONDITION", "STATE", "ACTION"}
SEMANTIC_CONFIDENCE = {"PROVEN", "DERIVED"}


def load_semantics(path: Path | None, script_root: Path, npc_source_root: Path,
                   npc_server_binary: Path = DEFAULT_NPC_SERVER_BINARY) -> dict[str, object]:
    if path is None:
        return {
            "events": {}, "function_abis": {}, "functions": {}, "virtual_operations": {},
            "callback_receiver_family": None, "receiver_families": {},
            "receiver_provenance": {},
            "provenance": None, "catalog": None,
        }
    registry = json.loads(path.read_text(encoding="utf-8"))
    if registry.get("version") != 4 or registry.get("kind") != "RETAIL_QUEST_CALLBACK_SEMANTICS":
        raise ValueError(f"unsupported quest callback semantics: {path}")
    evidence_files = registry.get("evidence_files", {})
    resolved_evidence = {}
    resolved_evidence_paths = {}
    for name, evidence in evidence_files.items():
        root = evidence.get("root")
        if root == "SCRIPT_SOURCE":
            evidence_path = script_root / evidence["path"]
        elif root == "NPC_SERVER_SOURCE":
            evidence_path = npc_source_root / evidence["path"]
        elif root == "NPC_SERVER_BINARY":
            evidence_path = npc_server_binary
        elif root == "REPOSITORY":
            evidence_path = ROOT / evidence["path"]
        else:
            raise ValueError(f"unsupported evidence root {root!r} for {name}")
        if not evidence_path.is_file() or sha256(evidence_path) != evidence.get("sha256"):
            raise ValueError(f"stale semantic evidence file for {name}: {evidence_path}")
        resolved_evidence_paths[name] = evidence_path
        if evidence.get("format", "text") != "binary":
            resolved_evidence[name] = evidence_path.read_text(encoding="utf-8").splitlines()

    def validate_evidence(key: str, entry: dict[str, object]) -> None:
        evidence_rows = entry.get("evidence", [])
        if not evidence_rows:
            raise ValueError(f"missing semantic evidence for {key}")
        for evidence in evidence_rows:
            lines = resolved_evidence.get(evidence.get("file"))
            start, end = evidence.get("lines", [0, 0])
            if lines is None or not 1 <= start <= end <= len(lines):
                raise ValueError(f"invalid semantic evidence range for {key}")
            if str(evidence.get("anchor", "")) not in "\n".join(lines[start - 1:end]):
                raise ValueError(f"missing semantic evidence anchor for {key}")

    def validate_entry(key: str, entry: dict[str, object]) -> None:
        if entry.get("category") not in SEMANTIC_CATEGORIES:
            raise ValueError(f"unsupported semantic category for {key}")
        if entry.get("confidence") not in SEMANTIC_CONFIDENCE:
            raise ValueError(f"unsupported semantic confidence for {key}")
        if not re.fullmatch(r"[A-Z][A-Z0-9_]*", str(entry.get("name", ""))):
            raise ValueError(f"invalid semantic name for {key}")
        validate_evidence(key, entry)

    for key, entry in registry.get("events", {}).items():
        if str(int(key)) != key:
            raise ValueError(f"invalid event slot semantic key {key}")
        validate_entry(f"event {key}", entry)
    for key, entry in registry.get("function_abis", {}).items():
        if not re.fullmatch(r"FUN_[0-9a-f]+", key):
            raise ValueError(f"invalid function ABI key {key}")
        script_host_parameter = entry.get("script_host_parameter")
        receiver_bindings = entry.get("receiver_bindings", {})
        if (script_host_parameter is not None
                and (type(script_host_parameter) is not int or script_host_parameter < 1)):
            raise ValueError(f"invalid script host parameter for {key}")
        if not isinstance(receiver_bindings, dict) or not receiver_bindings:
            if script_host_parameter is None:
                raise ValueError(f"missing receiver ABI for {key}")
        elif any(not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", str(receiver))
                 for receiver in receiver_bindings):
            raise ValueError(f"invalid receiver binding for {key}")
        if entry.get("confidence") not in SEMANTIC_CONFIDENCE:
            raise ValueError(f"unsupported ABI confidence for {key}")
        validate_evidence(f"function ABI {key}", entry)
    receiver_families = registry.get("receiver_families", {})
    for key, entry in receiver_families.items():
        if not re.fullmatch(r"[A-Z][A-Z0-9_]*", key):
            raise ValueError(f"invalid receiver family {key}")
        if entry.get("confidence") not in SEMANTIC_CONFIDENCE:
            raise ValueError(f"unsupported receiver family confidence for {key}")
        validate_evidence(f"receiver family {key}", entry)
        vtable = entry.get("vtable", {})
        evidence_path = resolved_evidence_paths.get(vtable.get("file"))
        address = vtable.get("address")
        entries = vtable.get("entries", {})
        if (evidence_path is None or not isinstance(address, str)
                or not re.fullmatch(r"0x[0-9a-f]+", address)
                or f"0x{int(address, 0):x}" != address
                or not isinstance(entries, dict)):
            raise ValueError(f"invalid receiver family vtable for {key}")
        pe_data, image_base, sections = load_pe(evidence_path)
        indexed_entries = {}
        if vtable.get("scan"):
            for index in range(0x1000 // 8):
                offset = index * 8
                actual = struct.unpack("<Q", pe_bytes(
                    pe_data, image_base, sections, int(address, 0) + offset, 8
                ))[0]
                target_section = section_for(sections, image_base, actual)
                if target_section is None or target_section["name"] != ".text":
                    break
                indexed_entries[f"0x{offset:x}"] = {
                    "target": f"0x{actual:x}",
                    "symbol": f"FUN_{actual:x}",
                }
            if not indexed_entries:
                raise ValueError(f"empty receiver family vtable index for {key}")
        for offset, target in entries.items():
            if (not re.fullmatch(r"0x[0-9a-f]+", str(offset))
                    or f"0x{int(offset, 0):x}" != offset
                    or not re.fullmatch(r"0x[0-9a-f]+", str(target))
                    or f"0x{int(target, 0):x}" != target):
                raise ValueError(f"invalid receiver family vtable entry for {key}")
            actual = struct.unpack("<Q", pe_bytes(
                pe_data, image_base, sections, int(address, 0) + int(offset, 0), 8
            ))[0]
            if actual != int(target, 0):
                raise ValueError(
                    f"stale receiver family vtable entry for {key} {offset}: 0x{actual:x} != {target}"
                )
            indexed_entries.setdefault(offset, {"target": target, "symbol": f"FUN_{int(target, 0):x}"})
        source_file = vtable.get("source_file")
        if source_file is not None:
            lines = resolved_evidence.get(source_file)
            if lines is None:
                raise ValueError(f"invalid receiver family vtable source for {key}")
            source_text = "\n".join(lines)
            recovered_addresses = {
                int(match.group("address"), 16) for match in RECOVERED_FUNCTION_MARKER.finditer(source_text)
            }
            recovered_addresses.update(
                int(match.group("address"), 16) for match in FUNCTION_MARKER.finditer(source_text)
            )
            for offset, indexed in indexed_entries.items():
                if int(indexed["target"], 0) not in recovered_addresses:
                    raise ValueError(f"missing vtable target source for {key} {offset}")
        vtable["index"] = indexed_entries
        accessors = entry.get("accessors", {})
        if any(offset not in indexed_entries or family not in receiver_families
               for offset, family in accessors.items()):
            raise ValueError(f"invalid receiver family accessor for {key}")
    callback_receiver_family = registry.get("callback_receiver_family")
    if callback_receiver_family not in receiver_families:
        raise ValueError(f"unsupported callback receiver family {callback_receiver_family!r}")
    for key, entry in registry.get("function_abis", {}).items():
        receiver_family = entry.get("receiver_family", callback_receiver_family)
        if entry.get("script_host_parameter") is not None and receiver_family not in receiver_families:
            raise ValueError(f"unsupported script host receiver family for {key}")
        if any(family not in receiver_families for family in entry.get("receiver_bindings", {}).values()):
            raise ValueError(f"unsupported receiver binding family for {key}")
    for key, entry in registry.get("functions", {}).items():
        if not re.fullmatch(r"FUN_[0-9a-f]+", key):
            raise ValueError(f"invalid function semantic key {key}")
        validate_entry(key, entry)
    for key, entry in registry.get("virtual_operations", {}).items():
        match = re.fullmatch(r"(CALL|READ|TRANSPORT):([A-Z][A-Z0-9_]*):(0x[0-9a-f]+)", key)
        if match is None or f"0x{int(match.group(3), 0):x}" != match.group(3):
            raise ValueError(f"invalid virtual operation semantic key {key}")
        receiver = match.group(2)
        if receiver not in {*receiver_families, "OTHER"} or entry.get("receiver") != receiver:
            raise ValueError(f"unsupported virtual operation receiver for {key}")
        if receiver != "OTHER" and match.group(3) not in receiver_families[receiver]["vtable"]["index"]:
            raise ValueError(f"missing receiver family vtable proof for {key}")
        validate_entry(f"virtual operation {key}", entry)
    receiver_provenance = registry.get("receiver_provenance", {})
    for symbol, entry in receiver_provenance.get("globals", {}).items():
        if (not re.fullmatch(r"(?:DAT|PTR)_[0-9a-f]+", symbol)
                or entry.get("receiver_family") not in receiver_families):
            raise ValueError(f"invalid receiver provenance global {symbol}")
        validate_evidence(f"receiver provenance global {symbol}", entry)
    for offset, entry in receiver_provenance.get("registry_outputs", {}).items():
        if (not re.fullmatch(r"0x[0-9a-f]+", offset)
                or f"0x{int(offset, 0):x}" != offset
                or type(entry.get("output_argument")) is not int
                or entry["output_argument"] < 0
                or entry.get("receiver_family") not in receiver_families):
            raise ValueError(f"invalid receiver provenance registry output {offset}")
        validate_evidence(f"receiver provenance registry output {offset}", entry)
    for offset, entry in receiver_provenance.get("registry_inputs", {}).items():
        if (not re.fullmatch(r"0x[0-9a-f]+", offset)
                or f"0x{int(offset, 0):x}" != offset
                or type(entry.get("input_argument")) is not int
                or entry["input_argument"] < 0
                or entry.get("receiver_family") not in receiver_families):
            raise ValueError(f"invalid receiver provenance registry input {offset}")
        validate_evidence(f"receiver provenance registry input {offset}", entry)
    for binding, entry in receiver_provenance.get("context_accessors", {}).items():
        match = re.fullmatch(r"(?:(EVENT):(\d+)|(PHASE_MASK)):(0x[0-9a-f]+)", binding)
        if (match is None or entry.get("receiver_family") not in receiver_families):
            raise ValueError(f"invalid receiver provenance context accessor {binding}")
        if match.group(1) and match.group(2) not in registry.get("events", {}):
            raise ValueError(f"unknown receiver provenance event {binding}")
        validate_evidence(f"receiver provenance context accessor {binding}", entry)
    for binding, entry in receiver_provenance.get("context_receivers", {}).items():
        match = re.fullmatch(r"(?:EVENT:(\d+)|PHASE_MASK)", binding)
        if (match is None or (match.group(1) and match.group(1) not in registry.get("events", {}))
                or type(entry.get("parameter")) is not int or entry["parameter"] < 1
                or entry.get("receiver_family") not in receiver_families):
            raise ValueError(f"invalid receiver provenance context receiver {binding}")
        validate_evidence(f"receiver provenance context receiver {binding}", entry)
    return {
        "events": registry.get("events", {}),
        "function_abis": registry.get("function_abis", {}),
        "functions": registry.get("functions", {}),
        "virtual_operations": registry.get("virtual_operations", {}),
        "callback_receiver_family": callback_receiver_family,
        "receiver_families": receiver_families,
        "receiver_provenance": receiver_provenance,
        "provenance": {"path": str(path), "sha256": sha256(path)},
        "catalog": {
            "callback_receiver_family": callback_receiver_family,
            "receiver_families": receiver_families,
            "receiver_provenance": receiver_provenance,
            "events": registry.get("events", {}),
            "function_abis": registry.get("function_abis", {}),
            "functions": registry.get("functions", {}),
            "virtual_operations": registry.get("virtual_operations", {}),
        },
    }


def mapped_semantic(entry: dict[str, object] | None, reason: str, key: str) -> dict[str, object]:
    if entry is None:
        return {"status": "UNKNOWN", "reason": reason}
    return {
        "status": "MAPPED",
        "key": key,
        "name": entry["name"],
        "category": entry["category"],
        "confidence": entry["confidence"],
    }


def operation_semantic(operation: dict[str, object], semantics: dict[str, object]) -> dict[str, object]:
    if operation["call_type"] == "DIRECT":
        target = operation["target"]
        return mapped_semantic(
            semantics["functions"].get(target), "UNMAPPED_DIRECT_FUNCTION", f"FUNCTION:{target}"
        )
    target = operation["target"]
    key = f"{operation['kind']}:{operation['receiver']}:{target}"
    entry = semantics["virtual_operations"].get(key)
    semantic_key = key
    if entry is None and operation["kind"] == "READ":
        semantic_key = f"CALL:{operation['receiver']}:{target}"
        entry = semantics["virtual_operations"].get(semantic_key)
    return mapped_semantic(entry, "UNMAPPED_VIRTUAL_OPERATION", f"VTABLE:{semantic_key}")


def semantic_coverage(rows: list[dict[str, object]]) -> dict[str, object]:
    statuses = Counter(row["status"] for row in rows)
    reasons = Counter(row["reason"] for row in rows if row["status"] == "UNKNOWN")
    return {
        "total": len(rows),
        "mapped": statuses["MAPPED"],
        "unknown": statuses["UNKNOWN"],
        "unknown_reasons": dict(sorted(reasons.items())),
    }


def pe_bytes(data: bytes, image_base: int, sections: list[dict[str, int | str]],
             value: int, size: int) -> bytes:
    section = section_for(sections, image_base, value)
    if section is None:
        raise ValueError(f"address 0x{value:x} is outside the PE image")
    delta = value - image_base - int(section["virtual_address"])
    if delta < 0 or delta + size > int(section["raw_size"]):
        raise ValueError(f"address 0x{value:x} has insufficient raw PE data")
    offset = int(section["raw_offset"]) + delta
    return data[offset:offset + size]


def resolve_target(symbol: str, pe_data: bytes, image_base: int,
                   sections: list[dict[str, int | str]],
                   functions_by_address: dict[int, str]) -> dict[str, object]:
    if symbol.startswith("FUN_"):
        if symbol_address(symbol) not in functions_by_address:
            raise ValueError(f"callback function is absent from recovered source: {symbol}")
        return {"kind": "FUNCTION", "target": symbol}
    value = symbol_address(symbol)
    code = pe_bytes(pe_data, image_base, sections, value, 32)
    for offset in range(len(code) - 4):
        if code[offset] != 0xe9:
            continue
        target_address = value + offset + 5 + struct.unpack_from("<i", code, offset + 1)[0]
        target = functions_by_address.get(target_address)
        if target:
            return {"kind": "TAIL_JUMP", "target": target, "jump_instruction_offset": offset}
    indirect = code.find(b"\x48\xff\xa0")
    if indirect >= 0:
        return {
            "kind": "INDIRECT_VTABLE_TAIL",
            "vtable_offset": f"0x{struct.unpack_from('<I', code, indirect + 3)[0]:x}",
        }
    raise ValueError(f"unsupported callback thunk at {symbol}: {code.hex()}")


def state_accesses(callback: dict[str, object]) -> list[dict[str, object]]:
    body = str(callback["body"])
    result = []
    for match in MEMORY_REFERENCE.finditer(body):
        line_start = body.rfind("\n", 0, match.start()) + 1
        line_end = body.find("\n", match.end())
        if line_end < 0:
            line_end = len(body)
        line = body[line_start:line_end]
        assignment = ASSIGNMENT.search(line)
        mode = "WRITE" if assignment and match.start() - line_start < assignment.start() else "READ"
        result.append({
            "mode": mode,
            "base": match.group("base"),
            "offset": f"0x{int(match.group('offset'), 0):x}",
            "raw": " ".join(line.split()),
            "source": {
                "path": callback["path"],
                "line": int(callback["line"]) + body.count("\n", 0, match.start()),
            },
        })
    return result


def infer_receiver_bindings(callback: dict[str, object], explicit_bindings: dict[str, str],
                            binding_contexts: set[str],
                            semantics: dict[str, object]) -> dict[str, dict[str, object]]:
    body = str(callback["body"])
    rules = semantics.get("receiver_provenance", {})
    bindings: dict[str, dict[str, object]] = {}

    def bind(receiver: str, family: str, kind: str, **details: object) -> None:
        value = {"family": family, "kind": kind, **details}
        previous = bindings.get(receiver)
        if previous is not None and previous["family"] != family:
            raise ValueError(
                f"conflicting receiver provenance for {receiver} in "
                f"{callback['path']}:{callback['line']}"
            )
        if previous is None or previous["kind"] == "EXPLICIT_ABI":
            bindings[receiver] = value

    for receiver, family in explicit_bindings.items():
        bind(receiver, family, "EXPLICIT_ABI")
    for receiver, entry in rules.get("globals", {}).items():
        if re.search(rf"\b{re.escape(receiver)}\b", body):
            bind(receiver, entry["receiver_family"], "GLOBAL_INTERFACE", symbol=receiver)
    for context in binding_contexts:
        entry = rules.get("context_receivers", {}).get(context)
        if entry is not None:
            bind(f"param_{entry['parameter']}", entry["receiver_family"],
                 "CALLBACK_CONTEXT", binding=context)

    assignments = []
    for match in re.finditer(r"(?m)^\s*(?P<target>[A-Za-z_][A-Za-z0-9_]*)\s*=\s*(?P<rhs>.*?);\s*$", body):
        assignments.append((match.group("target"), " ".join(match.group("rhs").split())))
    context_aliases = {}
    for target, rhs in assignments:
        match = re.search(r"\((param_\d+)\s*\+\s*(0x[0-9a-f]+|\d+)\)\s*$", rhs)
        if match is not None and "**" not in rhs:
            context_aliases[target] = f"0x{int(match.group(2), 0):x}"

    def context_family(offset: str) -> tuple[str, list[str]] | None:
        entries = [rules.get("context_accessors", {}).get(f"{context}:{offset}")
                   for context in binding_contexts]
        if not entries or any(entry is None for entry in entries):
            return None
        families = {entry["receiver_family"] for entry in entries}
        if len(families) != 1:
            raise ValueError(
                f"conflicting callback context accessor {offset} in "
                f"{callback['path']}:{callback['line']}"
            )
        return families.pop(), sorted(binding_contexts)

    for target, rhs in assignments:
        match = re.search(r"\bparam_\d+\s*\+\s*(0x[0-9a-f]+|\d+)", rhs)
        offset = f"0x{int(match.group(1), 0):x}" if match is not None and "**" in rhs else None
        if offset is None:
            alias = next((alias for alias in context_aliases
                          if re.search(rf"\*{re.escape(alias)}\b", rhs)), None)
            offset = context_aliases.get(alias) if alias is not None else None
        if offset is not None:
            resolved = context_family(offset)
            if resolved is not None:
                family, contexts = resolved
                bind(target, family, "CALLBACK_CONTEXT_ACCESSOR",
                     accessor_offset=offset, bindings=contexts)

    for match in VIRTUAL_CALL.finditer(body):
        offset = f"0x{int(match.group('offset'), 0):x}"
        if "DAT_184720398" not in match.group("raw"):
            continue
        args = split_args(match.group("args"))
        for direction, argument_key in (("outputs", "output_argument"),
                                        ("inputs", "input_argument")):
            entry = rules.get(f"registry_{direction}", {}).get(offset)
            if entry is None or entry[argument_key] >= len(args):
                continue
            value = re.match(r"&?([A-Za-z_][A-Za-z0-9_]*)\b",
                             args[entry[argument_key]].strip())
            if value is not None:
                bind(value.group(1), entry["receiver_family"],
                     f"INTERFACE_REGISTRY_{direction[:-1].upper()}", registry_offset=offset)

    changed = True
    while changed:
        changed = False
        for target, rhs in assignments:
            if target in bindings:
                continue
            match = VIRTUAL_CALL.search(rhs + ";")
            if match is None:
                continue
            args = split_args(match.group("args"))
            source = args[0].strip() if args else ""
            source_binding = bindings.get(source)
            if source_binding is None:
                continue
            offset = f"0x{int(match.group('offset'), 0):x}"
            family = semantics.get("receiver_families", {}).get(
                source_binding["family"], {}
            ).get("accessors", {}).get(offset)
            if family is not None:
                bind(target, family, "VIRTUAL_ACCESSOR", source=source,
                     source_family=source_binding["family"], accessor_offset=offset)
                changed = True
    return bindings


def semantic_features(callback: dict[str, object],
                      receiver_bindings: dict[str, str], binding_contexts: set[str] | None = None,
                      semantics: dict[str, object] | None = None) -> tuple[dict[str, object], dict[str, object]]:
    features, old_signature = callback_features(callback)
    accesses = state_accesses(callback)
    inferred_bindings = infer_receiver_bindings(
        callback, receiver_bindings, binding_contexts or set(), semantics or {}
    )
    for operation in features["operations"]:
        if operation["call_type"] == "DIRECT":
            operation["receiver"] = None
            continue
        match = CALL_RECEIVER.search(operation["raw"])
        if match is None:
            raise ValueError(f"missing virtual receiver in {callback['path']}:{callback['line']}")
        receiver = match.group("receiver").strip()
        provenance = inferred_bindings.get(receiver, {
            "family": "UNRESOLVED",
            "kind": "UNRESOLVED",
            "reason": "NO_PROVENANCE_RULE",
        })
        operation["receiver"] = provenance["family"]
        operation["receiver_expression"] = receiver
        operation["receiver_provenance"] = {
            key: value for key, value in provenance.items() if key != "family"
        }
    signature = {
        "conditions": old_signature["predicates"],
        "state_accesses": [f"{row['mode']}:{row['base']}:{row['offset']}" for row in accesses],
        "operations": [
            f"{row['kind']}:{row['call_type']}:{row['receiver']}:{row['target']}"
            for row in features["operations"]
        ],
    }
    shape_id = hashlib.sha256(json.dumps(signature, sort_keys=True).encode()).hexdigest()[:16]
    body = str(callback["body"])
    return {
        "shape_id": shape_id,
        "conditions": features["predicates"],
        "state_accesses": accesses,
        "operations": features["operations"],
        "data_references": sorted(set(DATA_REFERENCE.findall(body))),
        "wide_strings": sorted(set(WIDE_STRING.findall(body))),
    }, signature


def load_functions(script_root: Path) -> tuple[dict[int, str], str, list[Path]]:
    files = sorted((script_root / "fun").glob("fun_*.cpp"))
    if not files:
        raise ValueError(f"no fun_*.cpp files in {script_root / 'fun'}")
    functions = {}
    digest = hashlib.sha256()
    for path in files:
        raw = path.read_bytes()
        relative = path.resolve().relative_to(script_root.resolve()).as_posix()
        digest.update(relative.encode())
        digest.update(b"\0")
        digest.update(hashlib.sha256(raw).digest())
        for match in FUNCTION_MARKER.finditer(raw.decode("utf-8")):
            value = int(match.group("address"), 16)
            symbol = match.group("symbol")
            if value in functions and functions[value] != symbol:
                raise ValueError(f"conflicting recovered function at 0x{value:x}")
            functions[value] = symbol
    return functions, digest.hexdigest(), files


def function_blocks(files: list[Path], script_root: Path, wanted: set[str]) -> dict[str, dict[str, object]]:
    result = {}
    for path in files:
        text = path.read_text(encoding="utf-8")
        for marker, body in marked_blocks(text):
            symbol = marker.group("name")
            if symbol not in wanted:
                continue
            if symbol in result:
                raise ValueError(f"duplicate recovered function body {symbol}")
            result[symbol] = {
                "body": body,
                "path": path.resolve().relative_to(script_root.resolve()).as_posix(),
                "line": text.count("\n", 0, marker.start()) + 1,
            }
    missing = sorted(wanted - result.keys())
    if missing:
        raise ValueError(f"missing recovered callback bodies: {', '.join(missing[:10])}")
    return result


def build(index_path: Path = DEFAULT_INDEX, script_root: Path = DEFAULT_SCRIPT_ROOT,
          dll_path: Path = DEFAULT_DLL, semantics_path: Path | None = DEFAULT_SEMANTICS,
          npc_source_root: Path = DEFAULT_NPC_SOURCE_ROOT,
          npc_server_binary: Path = DEFAULT_NPC_SERVER_BINARY) -> dict[str, object]:
    index = json.loads(index_path.read_text(encoding="utf-8"))
    if index.get("version") != 1 or index.get("provenance", {}).get("kind") != "RETAIL_QUEST_SCRIPT_INDEX":
        raise ValueError(f"unsupported quest ScriptDLL index: {index_path}")
    functions_by_address, source_digest, files = load_functions(script_root)
    if index["provenance"]["script_source"]["fun_sha256"] != source_digest:
        raise ValueError(f"stale quest ScriptDLL index source digest: {index_path}")
    dll_digest = sha256(dll_path)
    if index["provenance"]["dll_source"]["sha256"] != dll_digest:
        raise ValueError(f"stale quest ScriptDLL index DLL digest: {index_path}")
    pe_data, image_base, sections = load_pe(dll_path)
    semantics = load_semantics(
        semantics_path, script_root, npc_source_root, npc_server_binary
    )

    raw_targets = sorted(index["callback_symbols"])
    resolutions = {
        symbol: resolve_target(symbol, pe_data, image_base, sections, functions_by_address)
        for symbol in raw_targets
    }
    indirect_semantic_statuses = Counter()
    callback_receiver_family = semantics["callback_receiver_family"]
    for resolution in resolutions.values():
        if resolution["kind"] != "INDIRECT_VTABLE_TAIL":
            continue
        offset = resolution["vtable_offset"]
        indexed = semantics["receiver_families"][callback_receiver_family]["vtable"]["index"].get(offset)
        if indexed is None:
            raise ValueError(f"unresolved callback receiver vtable slot {offset}")
        semantic = operation_semantic({
            "kind": "CALL", "call_type": "VIRTUAL",
            "receiver": callback_receiver_family, "target": offset,
        }, semantics)
        resolution.update({
            "receiver_family": callback_receiver_family,
            "target_address": indexed["target"],
            "target_symbol": indexed["symbol"],
            "semantic": semantic,
        })
        indirect_semantic_statuses[semantic["status"]] += 1
    direct_callback_targets = {
        str(resolution["target"])
        for resolution in resolutions.values()
        if resolution["kind"] == "FUNCTION"
    }
    resolved_targets = {
        symbol: str(resolution["target"])
        for symbol, resolution in resolutions.items()
        if "target" in resolution
    }
    wanted = set(resolved_targets.values())
    blocks = function_blocks(files, script_root, wanted)

    target_usage = Counter()
    target_quests = defaultdict(set)
    event_usage = Counter()
    function_binding_contexts = defaultdict(set)
    for quest in index["quests"]:
        for obj in quest["objects"]:
            for callback in obj["callbacks"]:
                target = callback["target"]
                target_usage[target] += 1
                target_quests[target].add(quest["id"])
                resolved = resolved_targets.get(target)
                if resolved is not None:
                    function_binding_contexts[resolved].add(
                        f"EVENT:{callback['event']}" if callback["kind"] == "EVENT" else "PHASE_MASK"
                    )
                if callback["kind"] == "EVENT":
                    event_usage[int(callback["event"])] += 1
    if set(target_usage) != set(raw_targets):
        raise ValueError("callback target usage does not match the base index")

    function_usage = Counter()
    function_quests = defaultdict(set)
    for raw_target, target in resolved_targets.items():
        function_usage[target] += target_usage[raw_target]
        function_quests[target].update(target_quests[raw_target])

    functions = {}
    signatures = {}
    shape_functions = defaultdict(set)
    shape_raw_targets = defaultdict(set)
    shape_registrations = Counter()
    shape_quests = defaultdict(set)
    operation_occurrences = Counter()
    operation_functions = defaultdict(set)
    operation_registrations = Counter()
    operation_semantics = {}
    operation_semantic_occurrences = Counter()
    operation_semantic_registrations = Counter()
    receiver_occurrences = Counter()
    receiver_registrations = Counter()
    receiver_provenance_occurrences = Counter()
    unresolved_receiver_sources = Counter()
    function_semantic_statuses = Counter()
    shape_operation_semantics = {}
    condition_count = state_access_count = operation_count = 0
    for symbol in sorted(wanted):
        abi_entry = semantics["function_abis"].get(symbol)
        semantic_entry = semantics["functions"].get(symbol, {})
        parameters = semantic_entry.get("parameters", [])
        host_parameter = (
            f"param_{abi_entry['script_host_parameter']}"
            if abi_entry is not None and "script_host_parameter" in abi_entry
            else f"param_{parameters.index('script_host') + 1}"
            if "script_host" in parameters
            else "param_1" if symbol in direct_callback_targets
            else None
        )
        receiver_bindings = dict(abi_entry.get("receiver_bindings", {})) if abi_entry is not None else {}
        if host_parameter is not None:
            receiver_bindings.setdefault(
                host_parameter,
                abi_entry.get("receiver_family", semantics["callback_receiver_family"])
                if abi_entry is not None else semantics["callback_receiver_family"],
            )
        features, signature = semantic_features(
            blocks[symbol], receiver_bindings, function_binding_contexts[symbol], semantics
        )
        shape_id = features["shape_id"]
        signatures[shape_id] = signature
        shape_functions[shape_id].add(symbol)
        condition_count += len(features["conditions"])
        state_access_count += len(features["state_accesses"])
        operation_count += len(features["operations"])
        typed_operations = []
        for operation in features["operations"]:
            key = (operation["kind"], operation["call_type"], operation["receiver"], operation["target"])
            typed = operation_semantic(operation, semantics)
            operation["semantic"] = typed
            typed_operations.append(typed)
            operation_semantics[key] = typed
            operation_occurrences[key] += 1
            operation_functions[key].add(symbol)
            operation_registrations[key] += function_usage[symbol]
            operation_semantic_occurrences[typed["status"]] += 1
            operation_semantic_registrations[typed["status"]] += function_usage[symbol]
            if operation["call_type"] == "VIRTUAL":
                receiver_occurrences[operation["receiver"]] += 1
                receiver_registrations[operation["receiver"]] += function_usage[symbol]
                provenance_kind = operation["receiver_provenance"]["kind"]
                receiver_provenance_occurrences[provenance_kind] += 1
                if operation["receiver"] == "UNRESOLVED":
                    unresolved_receiver_sources[operation["receiver_expression"]] += 1
        shape_operation_semantics.setdefault(shape_id, typed_operations)
        function_semantic = mapped_semantic(
            semantics["functions"].get(symbol), "UNMAPPED_CALLBACK_FUNCTION", f"FUNCTION:{symbol}"
        )
        function_semantic_statuses[function_semantic["status"]] += 1
        functions[symbol] = {
            "source": {"path": blocks[symbol]["path"], "line": blocks[symbol]["line"]},
            "registrations": function_usage[symbol],
            "quests": len(function_quests[symbol]),
            "semantic": function_semantic,
            "facts": features,
        }

    for raw_target, target in resolved_targets.items():
        shape_id = functions[target]["facts"]["shape_id"]
        shape_raw_targets[shape_id].add(raw_target)
        shape_registrations[shape_id] += target_usage[raw_target]
        shape_quests[shape_id].update(target_quests[raw_target])

    resolution_counts = Counter(row["kind"] for row in resolutions.values())
    lab_resolutions = {symbol: row for symbol, row in resolutions.items() if symbol.startswith("LAB_")}
    event_catalog = []
    event_semantic_statuses = Counter()
    event_semantic_registrations = Counter()
    for slot in sorted(event_usage):
        semantic = mapped_semantic(
            semantics["events"].get(str(slot)), "UNMAPPED_EVENT_SLOT", f"EVENT:{slot}"
        )
        event_semantic_statuses[semantic["status"]] += 1
        event_semantic_registrations[semantic["status"]] += event_usage[slot]
        event_catalog.append({"slot": slot, "registrations": event_usage[slot], "semantic": semantic})
    operation_catalog_statuses = Counter(row["status"] for row in operation_semantics.values())
    return {
        "version": 5,
        "provenance": {
            "kind": "RETAIL_QUEST_CALLBACK_IR",
            "authoritative_retail_evidence": True,
            "input_index": {"path": str(index_path), "sha256": sha256(index_path)},
            "script_source": {"path": str(script_root), "fun_sha256": source_digest},
            "dll_source": {"path": str(dll_path), "sha256": dll_digest},
            "semantic_registry": semantics["provenance"],
            "proof_scope": [
                "direct callback functions",
                "DLL rel32 tail-jump thunk resolution",
                "indirect vtable tail-call isolation",
                "raw callback predicates",
                "parameter-relative memory accesses",
                "direct and virtual call operations",
                "receiver-aware virtual operation classification",
                "NPCServer callback receiver families and complete binary vtable indexes",
                "event-bound callback context accessor provenance",
                "host interface registry output provenance",
                "indirect callback vtable target symbolization",
                "audited shared helper callback ABI",
                "audited per-function receiver bindings",
                "normalized callback shape clustering",
                "audited callback function and virtual operation semantics",
                "audited event slot semantics",
            ],
            "excluded_semantics": [
                "predicate business meaning",
                "memory-offset ownership or field naming",
                "unmapped direct or virtual call business meaning",
                "indirect callback argument business meaning beyond the resolved virtual operation",
                "runtime graph projection",
            ],
        },
        "summary": {
            "raw_callback_targets": len(raw_targets),
            "target_resolutions": dict(sorted(resolution_counts.items())),
            "resolved_callback_targets": len(raw_targets),
            "resolved_script_callback_targets": len(resolved_targets),
            "indirect_callback_targets": resolution_counts["INDIRECT_VTABLE_TAIL"],
            "unresolved_callback_targets": 0,
            "unique_resolved_functions": len(wanted),
            "conditions": condition_count,
            "state_accesses": state_access_count,
            "operations": operation_count,
            "callback_shapes": len(signatures),
            "operation_catalog_entries": len(operation_occurrences),
            "semantic_coverage": {
                "callback_functions": dict(sorted(function_semantic_statuses.items())),
                "operation_catalog_entries": dict(sorted(operation_catalog_statuses.items())),
                "operation_occurrences": dict(sorted(operation_semantic_occurrences.items())),
                "operation_registrations": dict(sorted(operation_semantic_registrations.items())),
                "virtual_operation_receiver_occurrences": dict(sorted(receiver_occurrences.items())),
                "virtual_operation_receiver_registrations": dict(sorted(receiver_registrations.items())),
                "receiver_provenance_occurrences": dict(sorted(receiver_provenance_occurrences.items())),
                "unresolved_receiver_sources": dict(sorted(unresolved_receiver_sources.items())),
                "indirect_callback_targets": dict(sorted(indirect_semantic_statuses.items())),
                "event_slots": dict(sorted(event_semantic_statuses.items())),
                "event_registrations": dict(sorted(event_semantic_registrations.items())),
            },
        },
        "lab_resolutions": lab_resolutions,
        "semantic_catalog": semantics["catalog"],
        "event_catalog": event_catalog,
        "operation_catalog": [{
            "kind": key[0],
            "call_type": key[1],
            "receiver": key[2],
            "target": key[3],
            "occurrences": operation_occurrences[key],
            "functions": len(operation_functions[key]),
            "registrations": operation_registrations[key],
            "semantic": operation_semantics[key],
        } for key in sorted(operation_occurrences)],
        "shapes": [{
            "id": shape_id,
            "functions": len(shape_functions[shape_id]),
            "raw_targets": len(shape_raw_targets[shape_id]),
            "registrations": shape_registrations[shape_id],
            "quests": len(shape_quests[shape_id]),
            "signature": signatures[shape_id],
            "semantic_coverage": semantic_coverage(shape_operation_semantics[shape_id]),
        } for shape_id in sorted(signatures)],
        "functions": functions,
    }


def render(report: dict[str, object]) -> str:
    return json.dumps(report, ensure_ascii=False, indent=2) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--index", type=Path, default=DEFAULT_INDEX)
    parser.add_argument("--script-root", type=Path, default=DEFAULT_SCRIPT_ROOT)
    parser.add_argument("--dll", type=Path, default=DEFAULT_DLL)
    parser.add_argument("--semantics", type=Path, default=DEFAULT_SEMANTICS)
    parser.add_argument("--npc-source-root", type=Path, default=DEFAULT_NPC_SOURCE_ROOT)
    parser.add_argument("--npc-server-binary", type=Path, default=DEFAULT_NPC_SERVER_BINARY)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    content = render(build(args.index, args.script_root, args.dll, args.semantics,
                           args.npc_source_root, args.npc_server_binary))
    if args.check:
        if not args.output.is_file() or args.output.read_bytes() != content.encode():
            raise SystemExit(f"stale retail quest callback IR: {args.output}")
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(content, encoding="utf-8")
    summary = json.loads(content)["summary"]
    print(f"{'verified' if args.check else 'generated'} {summary['unique_resolved_functions']} callback functions")
    print(f"clustered {summary['callback_shapes']} mechanical callback shapes")


if __name__ == "__main__":
    main()
