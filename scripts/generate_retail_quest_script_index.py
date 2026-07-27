#!/usr/bin/env python3
"""Build a quest -> ScriptDLL object/phase/callback index from retail evidence."""

import argparse
import ast
import hashlib
import json
import re
import struct
import xml.etree.ElementTree as ET
from bisect import bisect_right
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SCRIPT_ROOT = Path("/Users/mc/IdeaProjects/58Server/server58-source/MainServer_ScriptDLL64")
DEFAULT_QUEST_SOURCE = Path("/Users/mc/IdeaProjects/58Server-new/Map/XML/quest.xml")
DEFAULT_DLL = Path("/Users/mc/IdeaProjects/58Server/MainServer/ScriptDLL64.dll")
DEFAULT_OUTPUT = ROOT / "docs/RETAIL_QUEST_SCRIPT_INDEX.json"

MARKER = re.compile(r"(?m)^// @(?P<address>[0-9a-f]+)\s+(?P<symbol>\S+)\s+->")
LABEL = re.compile(r"(?m)^(?P<symbol>LAB_[0-9a-f]+):")
DATA_SYMBOL = re.compile(r"&?(DAT_[0-9a-f]+)$")
CALLBACK_SYMBOL = re.compile(r"&?((?:FUN|LAB)_[0-9a-f]+)$")
INTEGER = re.compile(r"-?(?:0x[0-9a-f]+|\d+)$", re.IGNORECASE)
WIDE_STRING = re.compile(r'^L("(?:\\.|[^"\\])*")$')
CALLS = {
    "initializer": re.compile(r"\bFUN_180cb5920\((?P<args>[^;]+)\);"),
    "phase": re.compile(r"\bFUN_180cb3070\((?P<args>[^;]+)\);"),
    "event": re.compile(r"\bFUN_180cb2ac0\((?P<args>[^;]+)\);"),
    "phase_mask": re.compile(r"\bFUN_180cb2ad0\((?P<args>[^;]+)\);"),
}


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


def data_symbol(value: str) -> str | None:
    match = DATA_SYMBOL.fullmatch(value.strip())
    return match.group(1) if match else None


def callback_symbol(value: str) -> str | None:
    match = CALLBACK_SYMBOL.fullmatch(value.strip())
    return match.group(1) if match else None


def symbol_address(symbol: str) -> int:
    return int(symbol.rsplit("_", 1)[1], 16)


def address(value: int) -> str:
    return f"0x{value:x}"


def mask(value: int) -> str:
    return f"0x{value & 0xffffffff:x}"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_pe(path: Path) -> tuple[bytes, int, list[dict[str, int | str]]]:
    data = path.read_bytes()
    if data[:2] != b"MZ":
        raise ValueError(f"not a PE image: {path}")
    pe = struct.unpack_from("<I", data, 0x3c)[0]
    if data[pe:pe + 4] != b"PE\0\0":
        raise ValueError(f"invalid PE signature: {path}")
    section_count = struct.unpack_from("<H", data, pe + 6)[0]
    optional_size = struct.unpack_from("<H", data, pe + 20)[0]
    optional = pe + 24
    if struct.unpack_from("<H", data, optional)[0] != 0x20b:
        raise ValueError(f"not a PE32+ image: {path}")
    image_base = struct.unpack_from("<Q", data, optional + 24)[0]
    section_table = optional + optional_size
    sections = []
    for index in range(section_count):
        offset = section_table + index * 40
        name = data[offset:offset + 8].split(b"\0", 1)[0].decode("ascii")
        virtual_size, virtual_address, raw_size, raw_offset = struct.unpack_from("<IIII", data, offset + 8)
        sections.append({
            "name": name,
            "virtual_size": virtual_size,
            "virtual_address": virtual_address,
            "raw_size": raw_size,
            "raw_offset": raw_offset,
        })
    return data, image_base, sections


def section_for(sections: list[dict[str, int | str]], image_base: int, value: int) -> dict[str, int | str] | None:
    for section in sections:
        start = image_base + int(section["virtual_address"])
        if start <= value < start + max(int(section["virtual_size"]), int(section["raw_size"])):
            return section
    return None


def read_wstring(data: bytes, image_base: int, sections: list[dict[str, int | str]], value: int) -> str:
    section = section_for(sections, image_base, value)
    if section is None:
        raise ValueError(f"wide string address {address(value)} is outside the PE image")
    delta = value - image_base - int(section["virtual_address"])
    if delta < 0 or delta >= int(section["raw_size"]):
        raise ValueError(f"wide string address {address(value)} has no raw PE data")
    offset = int(section["raw_offset"]) + delta
    end = offset
    while end + 1 < len(data) and data[end:end + 2] != b"\0\0":
        end += 2
    if end + 1 >= len(data):
        raise ValueError(f"unterminated wide string at {address(value)}")
    return data[offset:end].decode("utf-16le")


def source(path: Path, root: Path, line: int) -> dict[str, object]:
    return {"path": path.resolve().relative_to(root.resolve()).as_posix(), "line": line}


def quest_ids(path: Path) -> set[int]:
    result = set()
    for _event, node in ET.iterparse(path, events=("end",)):
        if node.tag != "quest":
            continue
        quest_id = int(node.findtext("id") or "0")
        if quest_id in result:
            raise ValueError(f"duplicate quest id {quest_id} in {path}")
        result.add(quest_id)
        node.clear()
    if not result:
        raise ValueError(f"no quest ids in {path}")
    return result


def callback_sources(fun_files: list[Path], root: Path, wanted: set[str]) -> dict[str, dict[str, object]]:
    result = {}
    for path in fun_files:
        text = path.read_text(encoding="utf-8")
        newlines = [match.start() for match in re.finditer("\n", text)]
        matches = list(MARKER.finditer(text)) + list(LABEL.finditer(text))
        for match in matches:
            symbol = match.group("symbol")
            if symbol not in wanted:
                continue
            location = source(path, root, bisect_right(newlines, match.start()) + 1)
            if symbol in result and result[symbol] != location:
                raise ValueError(f"duplicate callback symbol {symbol}")
            result[symbol] = location
    return result


def build(script_root: Path = DEFAULT_SCRIPT_ROOT, quest_source: Path = DEFAULT_QUEST_SOURCE,
          dll_path: Path = DEFAULT_DLL) -> dict[str, object]:
    fun_directory = script_root / "fun"
    fun_files = sorted(fun_directory.glob("fun_*.cpp"))
    if not fun_files:
        raise ValueError(f"no fun_*.cpp files in {fun_directory}")
    pe_data, image_base, sections = load_pe(dll_path)
    text_sections = [section for section in sections if section["name"] == ".text"]
    if len(text_sections) != 1:
        raise ValueError(f"expected one .text section in {dll_path}")
    text_section = text_sections[0]
    text_start = image_base + int(text_section["virtual_address"])
    text_end = text_start + max(int(text_section["virtual_size"]), int(text_section["raw_size"]))

    initializers, phases, callbacks = [], [], []
    dynamic_calls = Counter()
    source_digest = hashlib.sha256()
    fun_function_blocks = 0
    for path in fun_files:
        raw = path.read_bytes()
        relative = path.resolve().relative_to(script_root.resolve()).as_posix()
        source_digest.update(relative.encode())
        source_digest.update(b"\0")
        source_digest.update(hashlib.sha256(raw).digest())
        text = raw.decode("utf-8")
        markers = list(MARKER.finditer(text))
        marker_positions = [marker.start() for marker in markers]
        newlines = [match.start() for match in re.finditer("\n", text)]
        fun_function_blocks += len(markers)

        def registration(match: re.Match[str]) -> dict[str, object]:
            index = bisect_right(marker_positions, match.start()) - 1
            if index < 0:
                raise ValueError(f"registration outside a recovered function in {path}")
            marker = markers[index]
            return {
                "symbol": marker.group("symbol"),
                "address": address(int(marker.group("address"), 16)),
                "source": source(path, script_root, bisect_right(newlines, marker.start()) + 1),
            }

        for match in CALLS["initializer"].finditer(text):
            args = split_args(match.group("args"))
            obj = data_symbol(args[0]) if len(args) == 3 else None
            quest_id = integer(args[2]) if len(args) == 3 else None
            if obj is None or quest_id is None or not (WIDE_STRING.fullmatch(args[1]) or data_symbol(args[1])):
                raise ValueError(f"unsupported quest initializer in {path}: {match.group(0)}")
            initializers.append({
                "object": obj, "name_argument": args[1], "quest_id": quest_id,
                "initializer": registration(match),
            })

        for match in CALLS["phase"].finditer(text):
            args = split_args(match.group("args"))
            if len(args) != 6:
                raise ValueError(f"unsupported phase registration in {path}: {match.group(0)}")
            tag, obj = data_symbol(args[0]), data_symbol(args[1])
            if tag is None or obj is None:
                dynamic_calls["FUN_180cb3070"] += 1
                continue
            values = [integer(value) for value in args[2:]]
            if any(value is None for value in values):
                raise ValueError(f"unsupported static phase registration in {path}: {match.group(0)}")
            phases.append({
                "tag": tag, "object": obj, "quest_id": values[0], "phase": values[1],
                "mask_value": values[2], "slot": values[3], "registration": registration(match),
            })

        for kind, api, expected, target_index in (
            ("EVENT", "FUN_180cb2ac0", 5, 3),
            ("PHASE_MASK", "FUN_180cb2ad0", 6, 4),
        ):
            pattern = CALLS["event" if kind == "EVENT" else "phase_mask"]
            for match in pattern.finditer(text):
                args = split_args(match.group("args"))
                if len(args) != expected:
                    raise ValueError(f"unsupported callback registration in {path}: {match.group(0)}")
                tag, obj = data_symbol(args[0]), data_symbol(args[1])
                if tag is None or obj is None:
                    dynamic_calls[api] += 1
                    continue
                target = callback_symbol(args[target_index])
                if target is None:
                    raise ValueError(f"unsupported static callback target in {path}: {match.group(0)}")
                row = {
                    "kind": kind, "tag": tag, "object": obj, "target": target,
                    "registration": registration(match),
                }
                if kind == "EVENT":
                    row["event"] = integer(args[2])
                    if row["event"] is None:
                        raise ValueError(f"unsupported static event slot in {path}: {match.group(0)}")
                else:
                    row["phase"] = integer(args[2])
                    row["mask_value"] = integer(args[3])
                    if row["phase"] != 3 or row["mask_value"] is None:
                        raise ValueError(f"unsupported static phase callback in {path}: {match.group(0)}")
                callbacks.append(row)

    recovered_cpp_function_blocks = fun_function_blocks
    for path in sorted(script_root.rglob("*.cpp")):
        if path.parent == fun_directory:
            continue
        recovered_cpp_function_blocks += len(MARKER.findall(path.read_text(encoding="utf-8")))

    retail_quest_ids = quest_ids(quest_source)
    objects = {}
    decoded_names = {}
    name_sources = Counter()
    for row in initializers:
        obj = str(row["object"])
        if obj in objects:
            raise ValueError(f"duplicate ScriptDLL quest object {obj}")
        quest_id = int(row["quest_id"])
        if quest_id not in retail_quest_ids:
            raise ValueError(f"ScriptDLL quest {quest_id} is absent from {quest_source}")
        initializer = row["initializer"]
        initializer_address = int(str(initializer["address"]), 0)
        if not text_start <= initializer_address < text_end:
            raise ValueError(f"initializer {initializer['symbol']} is outside DLL .text")
        literal = WIDE_STRING.fullmatch(str(row["name_argument"]))
        if literal:
            npc_name = ast.literal_eval(literal.group(1))
            name_source = {"kind": "LITERAL"}
        else:
            name_data = data_symbol(str(row["name_argument"]))
            if name_data is None:
                raise AssertionError("validated name argument disappeared")
            if name_data not in decoded_names:
                decoded_names[name_data] = read_wstring(pe_data, image_base, sections, symbol_address(name_data))
            npc_name = decoded_names[name_data]
            name_source = {"kind": "DATA", "symbol": name_data, "address": address(symbol_address(name_data))}
        name_sources[name_source["kind"]] += 1
        objects[obj] = {
            "quest_id": quest_id,
            "object": obj,
            "address": address(symbol_address(obj)),
            "name": npc_name,
            "name_source": name_source,
            "initializer": initializer,
            "phases": [],
            "callbacks": [],
        }

    for row in phases:
        obj = objects.get(str(row["object"]))
        if obj is None:
            raise ValueError(f"phase registration references unknown object {row['object']}")
        if row["quest_id"] != obj["quest_id"]:
            raise ValueError(f"phase quest id conflicts for object {row['object']}")
        registrar_address = int(str(row["registration"]["address"]), 0)
        if not text_start <= registrar_address < text_end:
            raise ValueError(f"phase registrar {row['registration']['symbol']} is outside DLL .text")
        obj["phases"].append({
            "phase": row["phase"], "mask": mask(int(row["mask_value"])), "slot": row["slot"],
            "tag": row["tag"], "registration": row["registration"],
        })

    callback_targets = {str(row["target"]) for row in callbacks}
    target_sources = callback_sources(fun_files, script_root, callback_targets)
    callback_symbols = {}
    for symbol in sorted(callback_targets):
        value = symbol_address(symbol)
        if not text_start <= value < text_end:
            raise ValueError(f"callback target {symbol} is outside DLL .text")
        callback_symbols[symbol] = {
            "address": address(value),
            "source": target_sources.get(symbol),
        }

    phase_mask_counts = Counter()
    event_slots = set()
    for row in callbacks:
        obj = objects.get(str(row["object"]))
        if obj is None:
            raise ValueError(f"callback registration references unknown object {row['object']}")
        registrar_address = int(str(row["registration"]["address"]), 0)
        if not text_start <= registrar_address < text_end:
            raise ValueError(f"callback registrar {row['registration']['symbol']} is outside DLL .text")
        callback = {
            "kind": row["kind"], "tag": row["tag"], "target": row["target"],
            "registration": row["registration"],
        }
        if row["kind"] == "EVENT":
            callback["event"] = row["event"]
            event_slots.add(int(row["event"]))
        else:
            callback["phase"] = row["phase"]
            callback["mask"] = mask(int(row["mask_value"]))
            phase_mask_counts[str(row["object"])] += 1
        obj["callbacks"].append(callback)

    risk_objects = 0
    quests = {}
    for obj in objects.values():
        obj["phases"].sort(key=lambda row: (row["phase"], row["mask"], row["slot"], row["registration"]["address"]))
        obj["callbacks"].sort(key=lambda row: (row["registration"]["address"], row["kind"], row["target"]))
        for registration in obj["phases"] + obj["callbacks"]:
            del registration["registration"]["address"]
        phase_mask_count = phase_mask_counts[obj["object"]]
        if phase_mask_count > 11:
            risk_objects += 1
            obj["issues"] = [{
                "code": "PHASE_MASK_CALLBACK_CAPACITY",
                "registrations": phase_mask_count,
                "runtime_capacity": 11,
            }]
        quests.setdefault(obj["quest_id"], []).append(obj)
    quest_rows = []
    for quest_id, quest_objects in sorted(quests.items()):
        quest_objects.sort(key=lambda row: symbol_address(row["object"]))
        for obj in quest_objects:
            del obj["quest_id"]
        quest_rows.append({"id": quest_id, "objects": quest_objects})

    callback_kinds = Counter(symbol.split("_", 1)[0] for symbol in callback_targets)
    callback_source_status = Counter("RECOVERED" if row["source"] else "NOT_RECOVERED"
                                     for row in callback_symbols.values())
    callback_bindings = Counter(row["kind"] for row in callbacks)
    return {
        "version": 1,
        "provenance": {
            "kind": "RETAIL_QUEST_SCRIPT_INDEX",
            "authoritative_retail_evidence": True,
            "quest_source": {"path": str(quest_source), "sha256": sha256(quest_source)},
            "script_source": {"path": str(script_root), "fun_sha256": source_digest.hexdigest()},
            "dll_source": {"path": str(dll_path), "sha256": sha256(dll_path)},
            "registration_apis": {
                "FUN_180cb5920": "quest object initializer",
                "FUN_180cb3070": "quest phase/mask registration",
                "FUN_180cb2ac0": "ordinary event callback registration",
                "FUN_180cb2ad0": "phase-mask callback registration",
            },
            "proof_scope": [
                "quest id to ScriptDLL object binding",
                "object name including PE-backed DAT strings",
                "phase and mask registration",
                "ordinary event and phase-mask callback targets",
                "initializer, registrar, and callback address membership in DLL .text",
                "recovered source symbol and line provenance when present",
            ],
            "excluded_semantics": [
                "callback behavior interpretation",
                "LAB thunk resolution",
                "unrecovered LAB source bodies",
                "AionEmu runtime ownership or dispatch",
            ],
        },
        "pe": {
            "image_base": address(image_base),
            "text": {"start": address(text_start), "end": address(text_end)},
        },
        "summary": {
            "fun_files": len(fun_files),
            "fun_function_blocks": fun_function_blocks,
            "recovered_cpp_function_blocks": recovered_cpp_function_blocks,
            "quest_xml_ids": len(retail_quest_ids),
            "quest_ids_with_script_objects": len(quests),
            "quest_xml_script_coverage_percent": round(len(quests) * 100 / len(retail_quest_ids), 2),
            "objects": len(objects),
            "names_by_source": dict(sorted(name_sources.items())),
            "decoded_name_data_symbols": len(decoded_names),
            "phase_registrations": len(phases),
            "callbacks": len(callbacks),
            "callbacks_by_binding": dict(sorted(callback_bindings.items())),
            "ordinary_event_slots": len(event_slots),
            "unique_callback_targets": len(callback_targets),
            "callback_targets_by_symbol": dict(sorted(callback_kinds.items())),
            "callback_targets_by_source_status": dict(sorted(callback_source_status.items())),
            "phase_mask_callback_capacity_risk_objects": risk_objects,
            "dynamic_helper_calls_ignored": dict(sorted(dynamic_calls.items())),
            "object_conflicts": 0,
            "orphan_phase_registrations": 0,
            "orphan_callback_registrations": 0,
            "initializers_outside_text": 0,
            "callback_targets_outside_text": 0,
        },
        "ordinary_event_slots": sorted(event_slots),
        "callback_symbols": callback_symbols,
        "quests": quest_rows,
    }


def render(report: dict[str, object]) -> str:
    return json.dumps(report, ensure_ascii=False, indent=2) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--script-root", type=Path, default=DEFAULT_SCRIPT_ROOT)
    parser.add_argument("--quest-source", type=Path, default=DEFAULT_QUEST_SOURCE)
    parser.add_argument("--dll", type=Path, default=DEFAULT_DLL)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    content = render(build(args.script_root, args.quest_source, args.dll))
    if args.check:
        if not args.output.is_file() or args.output.read_bytes() != content.encode():
            raise SystemExit(f"stale retail quest ScriptDLL index: {args.output}")
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(content, encoding="utf-8")
    summary = json.loads(content)["summary"]
    print(f"{'verified' if args.check else 'generated'} {summary['objects']} ScriptDLL quest objects")
    print(f"indexed {summary['callbacks']} callbacks for {summary['quest_ids_with_script_objects']} quests")


if __name__ == "__main__":
    main()
