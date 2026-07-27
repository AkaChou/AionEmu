import hashlib
import json
import struct
import tempfile
import unittest
from collections import Counter
from pathlib import Path

from scripts.generate_retail_quest_callback_ir import (
    build as build_callback_ir,
    render as render_callback_ir,
    semantic_features,
)
from scripts.generate_retail_quest_script_index import build, render


ROOT = Path(__file__).resolve().parents[1]
INDEX = ROOT / "docs/RETAIL_QUEST_SCRIPT_INDEX.json"
CALLBACK_IR = ROOT / "docs/RETAIL_QUEST_CALLBACK_IR.json"


def write_pe(path: Path) -> None:
    data = bytearray(0x800)
    data[:2] = b"MZ"
    struct.pack_into("<I", data, 0x3c, 0x80)
    data[0x80:0x84] = b"PE\0\0"
    struct.pack_into("<H", data, 0x84, 0x8664)
    struct.pack_into("<H", data, 0x86, 2)
    struct.pack_into("<H", data, 0x94, 0xf0)
    struct.pack_into("<H", data, 0x98, 0x20b)
    struct.pack_into("<Q", data, 0xb0, 0x180000000)
    section_table = 0x188
    data[section_table:section_table + 8] = b".text\0\0\0"
    struct.pack_into("<IIII", data, section_table + 8, 0x1000, 0x1000, 0x200, 0x400)
    data[section_table + 40:section_table + 48] = b".data\0\0\0"
    struct.pack_into("<IIII", data, section_table + 48, 0x200, 0x3000, 0x200, 0x600)
    encoded = "DataNpc\0".encode("utf-16le")
    data[0x600:0x600 + len(encoded)] = encoded
    data[0x5a0] = 0xe9
    struct.pack_into("<i", data, 0x5a1, 0x1800011c0 - (0x1800011a0 + 5))
    data[0x5d0:0x5da] = bytes.fromhex("488b0148ffa0a8000000")
    struct.pack_into("<Q", data, 0x660, 0x180001300)
    struct.pack_into("<Q", data, 0x6a8, 0x180001300)
    path.write_bytes(data)


def write_fixture(root: Path) -> tuple[Path, Path, Path]:
    scripts = root / "ScriptDLL"
    fun = scripts / "fun"
    fun.mkdir(parents=True)
    (fun / "fun_001.cpp").write_text(
        "// @180001100 FUN_180001100 -> FUN_180001100\n"
        "void FUN_180001100(void) { FUN_180cb5920(&DAT_180003040,&DAT_180003000,1000); }\n"
        "// @180001120 FUN_180001120 -> FUN_180001120\n"
        "void FUN_180001120(void) { FUN_180cb5920(&DAT_180003080,L\"LiteralNpc\",1001); }\n"
        "// @180001140 FUN_180001140 -> FUN_180001140\n"
        "void FUN_180001140(void) { FUN_180cb3070(&DAT_180003090,&DAT_180003040,1000,0,0xffffffff,3); }\n"
        "// @180001160 FUN_180001160 -> FUN_180001160\n"
        "void FUN_180001160(void) { FUN_180cb2ac0(&DAT_180003098,&DAT_180003040,0x1d,&LAB_1800011a0,0); }\n"
        "// @180001180 FUN_180001180 -> FUN_180001180\n"
        "void FUN_180001180(void) { FUN_180cb2ad0(&DAT_1800030a0,&DAT_180003040,3,2,FUN_1800011c0,0); }\n"
        "// @180001190 FUN_180001190 -> FUN_180001190\n"
        "void FUN_180001190(void) { FUN_180cb2ac0(&DAT_1800030a8,&DAT_180003040,0x20,&LAB_1800011d0,0); }\n"
        "// @1800011c0 FUN_1800011c0 -> FUN_1800011c0\n"
        "void FUN_1800011c0(uint64_t param_1,int64_t param_2)\n"
        "{\n"
        "  int value = *(int *)(param_2 + 0xc);\n"
        "  if (value == 3) {\n"
        "    FUN_180001300(param_1);\n"
        "    (**(code **)(param_1 + 0x60))(param_1);\n"
        "    value = (**(code **)(param_1 + 0x60))(param_1);\n"
        "  }\n"
        "}\n",
        encoding="utf-8",
    )
    quest_source = root / "quest.xml"
    quest_source.write_text(
        "<quests><quest><id>1000</id></quest><quest><id>1001</id></quest></quests>",
        encoding="utf-8",
    )
    dll = root / "ScriptDLL64.dll"
    write_pe(dll)
    return scripts, quest_source, dll


def write_semantics(root: Path, scripts: Path) -> Path:
    source = scripts / "fun/fun_001.cpp"
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    end_line = len(source.read_text(encoding="utf-8").splitlines())
    evidence = lambda anchor: [{
        "file": "fixture",
        "lines": [1, end_line],
        "anchor": anchor,
    }]
    semantics = {
        "version": 4,
        "kind": "RETAIL_QUEST_CALLBACK_SEMANTICS",
        "evidence_files": {
            "fixture": {
                "root": "SCRIPT_SOURCE",
                "path": "fun/fun_001.cpp",
                "sha256": digest,
            },
            "fixture_binary": {
                "root": "SCRIPT_SOURCE",
                "path": "../ScriptDLL64.dll",
                "format": "binary",
                "sha256": hashlib.sha256((root / "ScriptDLL64.dll").read_bytes()).hexdigest(),
            },
        },
        "callback_receiver_family": "NPC_HOST",
        "receiver_families": {
            "NPC_HOST": {
                "confidence": "PROVEN",
                "evidence": evidence("FUN_1800011c0"),
                "vtable": {
                    "file": "fixture_binary",
                    "address": "0x180003000",
                    "entries": {"0x60": "0x180001300", "0xa8": "0x180001300"},
                },
            },
        },
        "events": {
            "29": {
                "name": "TEST_EVENT",
                "category": "EVENT",
                "parameters": [],
                "confidence": "PROVEN",
                "evidence": evidence("0x1d"),
            },
        },
        "function_abis": {
            "FUN_1800011c0": {
                "script_host_parameter": 1,
                "confidence": "PROVEN",
                "evidence": evidence("FUN_1800011c0"),
            },
        },
        "functions": {
            "FUN_1800011c0": {
                "name": "TEST_CALLBACK",
                "category": "ACTION",
                "parameters": [],
                "confidence": "PROVEN",
                "evidence": evidence("FUN_1800011c0"),
            },
            "FUN_180001300": {
                "name": "TEST_DIRECT_OPERATION",
                "category": "ACTION",
                "parameters": [],
                "confidence": "PROVEN",
                "evidence": evidence("FUN_180001300"),
            },
        },
        "virtual_operations": {
            "CALL:NPC_HOST:0x60": {
                "name": "TEST_VIRTUAL_OPERATION",
                "category": "STATE",
                "receiver": "NPC_HOST",
                "parameters": [],
                "confidence": "DERIVED",
                "evidence": evidence("0x60"),
            },
            "CALL:NPC_HOST:0xa8": {
                "name": "TEST_INDIRECT_OPERATION",
                "category": "ACTION",
                "receiver": "NPC_HOST",
                "parameters": [],
                "confidence": "DERIVED",
                "evidence": evidence("FUN_1800011c0"),
            },
        },
    }
    path = root / "semantics.json"
    path.write_text(json.dumps(semantics), encoding="utf-8")
    return path


class RetailQuestScriptIndexTest(unittest.TestCase):

    def test_callback_ir_honors_explicit_receiver_bindings(self):
        callback = {
            "body": "(**(code **)(*plVar1 + 0xd0))(plVar1,&local_res8,1000);",
            "path": "fun/fun_001.cpp",
            "line": 1,
        }

        facts, _ = semantic_features(callback, {"plVar1": "USER_HOST"})

        self.assertEqual("USER_HOST", facts["operations"][0]["receiver"])

    def test_fixture_builds_object_phase_and_callback_graph(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            scripts, quest_source, dll = write_fixture(root)

            report = build(scripts, quest_source, dll)
            quests = {row["id"]: row for row in report["quests"]}
            data_object = quests[1000]["objects"][0]

            self.assertEqual("DataNpc", data_object["name"])
            self.assertEqual("DAT_180003000", data_object["name_source"]["symbol"])
            self.assertEqual("LiteralNpc", quests[1001]["objects"][0]["name"])
            self.assertEqual("0xffffffff", data_object["phases"][0]["mask"])
            self.assertEqual({"EVENT", "PHASE_MASK"}, {row["kind"] for row in data_object["callbacks"]})
            self.assertEqual(3, report["summary"]["callbacks"])
            self.assertEqual(3, report["summary"]["unique_callback_targets"])
            self.assertIsNotNone(report["callback_symbols"]["FUN_1800011c0"]["source"])
            self.assertIsNone(report["callback_symbols"]["LAB_1800011a0"]["source"])
            self.assertEqual(report, json.loads(render(report)))

    def test_callback_ir_resolves_thunks_and_extracts_mechanical_facts(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            scripts, quest_source, dll = write_fixture(root)
            index_path = root / "index.json"
            index_path.write_text(render(build(scripts, quest_source, dll)), encoding="utf-8")
            semantics = write_semantics(root, scripts)

            report = build_callback_ir(index_path, scripts, dll, semantics)
            direct = report["lab_resolutions"]["LAB_1800011a0"]
            indirect = report["lab_resolutions"]["LAB_1800011d0"]
            facts = report["functions"]["FUN_1800011c0"]["facts"]

            self.assertEqual({"kind": "TAIL_JUMP", "target": "FUN_1800011c0", "jump_instruction_offset": 0}, direct)
            self.assertEqual("INDIRECT_VTABLE_TAIL", indirect["kind"])
            self.assertEqual("0xa8", indirect["vtable_offset"])
            self.assertEqual("NPC_HOST", indirect["receiver_family"])
            self.assertEqual("0x180001300", indirect["target_address"])
            self.assertEqual("FUN_180001300", indirect["target_symbol"])
            self.assertEqual("TEST_INDIRECT_OPERATION", indirect["semantic"]["name"])
            self.assertEqual({"FUNCTION": 1, "INDIRECT_VTABLE_TAIL": 1, "TAIL_JUMP": 1},
                             report["summary"]["target_resolutions"])
            self.assertEqual(1, report["summary"]["unique_resolved_functions"])
            self.assertEqual(1, report["summary"]["callback_shapes"])
            self.assertEqual("0xc", facts["state_accesses"][0]["offset"])
            self.assertEqual({"FUN_180001300", "0x60"}, {row["target"] for row in facts["operations"]})
            self.assertEqual({None, "NPC_HOST"}, {row["receiver"] for row in facts["operations"]})
            self.assertEqual(["MAPPED", "MAPPED", "MAPPED"],
                             [row["semantic"]["status"] for row in facts["operations"]])
            self.assertEqual("MAPPED", report["functions"]["FUN_1800011c0"]["semantic"]["status"])
            self.assertEqual("MAPPED", report["event_catalog"][0]["semantic"]["status"])
            self.assertEqual("UNKNOWN", report["event_catalog"][1]["semantic"]["status"])
            self.assertEqual(report, json.loads(render_callback_ir(report)))

    def test_checked_in_index_has_the_audited_retail_counts(self):
        report = json.loads(INDEX.read_text(encoding="utf-8"))
        summary = report["summary"]

        self.assertEqual(1, report["version"])
        self.assertEqual(916, summary["fun_files"])
        self.assertEqual(137285, summary["fun_function_blocks"])
        self.assertEqual(158519, summary["recovered_cpp_function_blocks"])
        self.assertEqual(10035, summary["quest_xml_ids"])
        self.assertEqual(7148, summary["quest_ids_with_script_objects"])
        self.assertEqual(18787, summary["objects"])
        self.assertEqual({"DATA": 669, "LITERAL": 18118}, summary["names_by_source"])
        self.assertEqual(174, summary["decoded_name_data_symbols"])
        self.assertEqual(25300, summary["phase_registrations"])
        self.assertEqual(60828, summary["callbacks"])
        self.assertEqual({"EVENT": 50377, "PHASE_MASK": 10451}, summary["callbacks_by_binding"])
        self.assertEqual(39, summary["ordinary_event_slots"])
        self.assertEqual(45725, summary["unique_callback_targets"])
        self.assertEqual({"FUN": 25100, "LAB": 20625}, summary["callback_targets_by_symbol"])
        self.assertEqual({"NOT_RECOVERED": 20625, "RECOVERED": 25100},
                         summary["callback_targets_by_source_status"])
        self.assertEqual(15, summary["phase_mask_callback_capacity_risk_objects"])
        self.assertEqual([9572], [quest["id"] for quest in report["quests"]
                                  if any("issues" in obj for obj in quest["objects"])])
        self.assertEqual(0, summary["object_conflicts"])
        self.assertEqual(0, summary["orphan_phase_registrations"])
        self.assertEqual(0, summary["orphan_callback_registrations"])
        self.assertEqual(0, summary["callback_targets_outside_text"])

    def test_checked_in_callback_ir_has_the_audited_retail_counts(self):
        report = json.loads(CALLBACK_IR.read_text(encoding="utf-8"))
        summary = report["summary"]

        self.assertEqual(5, report["version"])
        self.assertEqual(45725, summary["raw_callback_targets"])
        self.assertEqual({"FUNCTION": 25100, "INDIRECT_VTABLE_TAIL": 15, "TAIL_JUMP": 20610},
                         summary["target_resolutions"])
        self.assertEqual(45725, summary["resolved_callback_targets"])
        self.assertEqual(45710, summary["resolved_script_callback_targets"])
        self.assertEqual(15, summary["indirect_callback_targets"])
        self.assertEqual(0, summary["unresolved_callback_targets"])
        self.assertEqual(25131, summary["unique_resolved_functions"])
        self.assertEqual(16321, summary["conditions"])
        self.assertEqual(14651, summary["state_accesses"])
        self.assertEqual(39888, summary["operations"])
        self.assertEqual(1631, summary["callback_shapes"])
        self.assertEqual(216, summary["operation_catalog_entries"])
        self.assertEqual({
            "callback_functions": {"MAPPED": 2, "UNKNOWN": 25129},
            "operation_catalog_entries": {"MAPPED": 179, "UNKNOWN": 37},
            "operation_occurrences": {"MAPPED": 39464, "UNKNOWN": 424},
            "operation_registrations": {"MAPPED": 95515, "UNKNOWN": 723},
            "virtual_operation_receiver_occurrences": {
                "INTERFACE_REGISTRY": 504, "NPC_HOST": 12562, "OBJECT_HOST": 10,
                "QUIT_CUTSCENE_CONTEXT": 99, "USER_HOST": 10679,
            },
            "virtual_operation_receiver_registrations": {
                "INTERFACE_REGISTRY": 826, "NPC_HOST": 46049, "OBJECT_HOST": 14,
                "QUIT_CUTSCENE_CONTEXT": 99, "USER_HOST": 27633,
            },
            "receiver_provenance_occurrences": {
                "CALLBACK_CONTEXT": 99, "CALLBACK_CONTEXT_ACCESSOR": 10345,
                "EXPLICIT_ABI": 12211, "GLOBAL_INTERFACE": 504,
                "INTERFACE_REGISTRY_INPUT": 44, "INTERFACE_REGISTRY_OUTPUT": 506,
                "VIRTUAL_ACCESSOR": 145,
            },
            "unresolved_receiver_sources": {},
            "indirect_callback_targets": {"MAPPED": 15},
            "event_slots": {"MAPPED": 39},
            "event_registrations": {"MAPPED": 50377},
        }, summary["semantic_coverage"])
        self.assertEqual(29, len(report["semantic_catalog"]["function_abis"]))
        self.assertEqual("NPC_HOST", report["semantic_catalog"]["callback_receiver_family"])
        families = report["semantic_catalog"]["receiver_families"]
        self.assertEqual("0x1404640e8", families["NPC_HOST"]["vtable"]["address"])
        self.assertEqual("0x140475040", families["USER_HOST"]["vtable"]["address"])
        self.assertEqual("0x140476688", families["OBJECT_HOST"]["vtable"]["address"])
        self.assertEqual("0x1404a2778", families["INTERFACE_REGISTRY"]["vtable"]["address"])
        self.assertEqual("0x14046f968", families["QUIT_CUTSCENE_CONTEXT"]["vtable"]["address"])
        self.assertEqual(
            {"NPC_HOST": 217, "USER_HOST": 133, "OBJECT_HOST": 10,
             "INTERFACE_REGISTRY": 10, "QUIT_CUTSCENE_CONTEXT": 4},
            {name: len(family["vtable"]["index"]) for name, family in families.items()},
        )
        self.assertEqual(
            {"0xd0": "0x140185760", "0xf0": "0x140185d90", "0xf8": "0x140185f30",
             "0x100": "0x1401860c0", "0x108": "0x140186480", "0x110": "0x140186250"},
            families["USER_HOST"]["vtable"]["entries"],
        )
        self.assertEqual(
            {"MAPPED"},
            {row["semantic"]["status"] for row in report["event_catalog"]},
        )
        self.assertEqual(
            {"DISPATCH_QUEST_DIALOG_DEFAULT", "DISPATCH_QUEST_DIALOG_PROGRESS_GUARDED",
             "DISPATCH_QUEST_DIALOG_BY_MODE_CORE", "SEND_QUEST_DIALOG_10000",
             "DISPATCH_QUEST_DIALOG_MODE_0", "DISPATCH_QUEST_DIALOG_MODE_4",
             "DISPATCH_QUEST_DIALOG_MODE_3", "DISABLE_TALK", "ENABLE_TALK", "GET_NPC_DATA",
             "OPEN_DIALOG", "OPEN_QUEST_REWARD_DIALOG", "CLOSE_DIALOG", "SAY_TO_ALL",
             "GOTO_NEXT_WAYPOINT", "DESPAWN", "DIE", "FIND_USER", "RELEASE_USER",
             "FIND_NPC", "RELEASE_NPC", "GET_ITEM_COLLECTING_PROGRESS", "IS_USER",
             "GET_OBJECT_ID", "GET_USER_INTERFACE", "GET_CUTSCENE_ID", "GET_QUEST_STATE",
             "SET_QUEST_PROGRESS", "SET_QUEST_PROGRESS_MEMORY_ONLY", "SET_QUEST_SUCCESS",
             "SHARE_QUEST_PROGRESS_RECORD", "SHARE_QUEST_PROGRESS",
             "EXTRACT_PACKED_6BIT_FIELD", "GET_NPC_ID", "CHECK_USER_HAS_QUEST_ITEM",
             "OPEN_QUEST_ACQUIRE_DIALOG", "CHECK_QUEST_ACQUIRE_CONDITION",
             "GIVE_QUEST_REWARD", "PLAY_QUEST_CUTSCENE", "ADD_HATE_POINT", "GIVE_ITEM",
             "GIVE_RECIPE", "SET_DROP_OPTION", "GET_USER_ID", "GET_CLASS",
             "SET_QUEST_ACQUIRED", "GET_QUEST_PROGRESS", "ADD_HIDDEN_QUEST",
             "DELETE_WORKING_QUEST", "IS_FINISHED_QUEST", "REMOVE_ITEM", "REMOVE_ITEM_MAX",
             "HAS_QUEST_REWARD_CHECK_ITEMS", "REMOVE_QUEST_REWARD_CHECK_ITEMS",
             "AUTO_QUEST_REWARD", "HEAR", "TELEPORT", "HAS_ITEM", "SET_QUEST_BRANCH",
             "ADVANCE_PACKED_6BIT_PROGRESS_IF_SLOT_EQUALS",
             "ADVANCE_PACKED_6BIT_QUEST_PROGRESS", "ADVANCE_PACKED_10BIT_QUEST_PROGRESS",
             "SHARE_PACKED_QUEST_PROGRESS", "ADVANCE_PACKED_6BIT_QUEST_PROGRESS_IN_RANGE",
             "COMPLETE_QUEST_REWARD_AND_CONSUME_REQUIREMENTS",
             "CHECK_QUEST_ACQUIRE_CONDITION_FOR_CONTEXT",
             "DISPATCH_QUEST_DIALOG_WITH_OPTIONAL_ITEM", "ACQUIRE_QUEST_AND_GIVE_ITEM",
             "DISPATCH_QUEST_DIALOG_WITH_INITIAL_PROGRESS_AND_ITEM",
             "DISPATCH_QUEST_DIALOG_WITH_PROGRESS_AND_ITEM_EXCHANGE", "GET_NPC_NAME_ID",
             "DEBUG_MESSAGE", "CLOSE_ALL_DIALOGS", "PLAY_CUTSCENE", "SPAWN_NPC",
             "START_FOLLOWING", "SAY", "REQUEST_DECREASE_USER_MONEY", "PLAY_ANIMATION",
             "SET_DROP_OPTION_AND_OPEN_LOOT_WINDOW", "SHARE_PROGRESS_MULTIPLE",
             "GET_HIDDEN_QUEST_PROGRESS", "ENTER_INSTANCE", "DELETE_QUEST_TIMER",
             "ADD_SKILL_EFFECT", "IS_IN_RANGE", "IS_IN_IDLE_STATE", "GET_GAME_TIME",
             "DISABLE_ATTACK", "GET_CURRENT_TARGET_ID", "PLAY_PARTY_CUTSCENE",
             "ADD_QUEST_TIMER", "ADD_NPC_QUEST_TIMER", "DELETE_NPC_QUEST_TIMER",
             "DELAYED_DESPAWN", "ATTACK_MOST_HATING", "ATTACK", "USE_SKILL",
             "START_FLEEING", "SYSTEM_MESSAGE", "GOTO_WAYPOINT", "STOP_MOVING",
             "GIVE_TITLE", "CHECK_USER_EQUIPMENT", "CHECK_USER_DP", "CHECK_USER_ITEM_SET",
             "CHECK_USER_RECIPE_LEARNED", "CHECK_USER_AP", "DESPAWN_SPAWNED",
             "BROADCAST_MESSAGE", "PLAY_ANIMATION_TO_USER", "SET_DROP_OPTION_NO_LOOT",
             "DESPAWN_ALL", "SET_CONDITION_SPAWN_VARIABLE", "FIND_RANDOM_USER_ID",
             "GET_GENDER", "GET_RACE", "GET_LEVEL", "GET_PARTY_ID", "SET_QUEST_WAITING",
             "SET_HIDDEN_QUEST_PROGRESS", "ADD_MISSION", "SPAWN_USER", "PLAY_MOVIE",
             "REMOVE_ALL_QUEST_REWARD_CHECK_ITEMS", "CHANGE_CLASS", "LEAVE_INSTANCE",
             "LOCK_INSTANCE", "ADD_USER_QUEST_TIMER", "DELETE_USER_QUEST_TIMER",
             "DISPEL_SKILL_EFFECT_BY_EFFECT_TYPE", "DISPEL_SKILL_EFFECT_BY_DISPEL_TYPE",
             "TELEPORT_ALIAS", "FLY", "IS_FREE_FLYING", "ADD_ITEM", "SET_PLAY_MODE",
             "GET_CURRENT_WORLD_NUM", "GET_CURRENT_BASE_WORLD_NUM",
             "GET_WORLD_EXT_CONDITION_VARIABLE", "GET_QUEST_BRANCH",
             "IS_FINISHED_QUEST_WITH_BRANCH", "DECREASE_DP"},
            {row["semantic"]["name"] for row in report["operation_catalog"]
             if row["semantic"]["status"] == "MAPPED"},
        )
        self.assertEqual(
            {"NPC_HOST", "USER_HOST"},
            {row["receiver"] for row in report["operation_catalog"] if row["target"] == "0x188"},
        )
        self.assertEqual(
            {"NPC_HOST"},
            {row["receiver"] for row in report["operation_catalog"] if row["target"] == "0x1b0"},
        )
        self.assertEqual(
            {"NPC_HOST"},
            {row["receiver"] for row in report["operation_catalog"] if row["target"] == "0x5d8"},
        )
        operations = {
            (row["kind"], row["receiver"], row["target"]): row["semantic"]
            for row in report["operation_catalog"]
        }
        self.assertEqual({
            "FUN_180caac10": "COMPLETE_QUEST_REWARD_AND_CONSUME_REQUIREMENTS",
            "FUN_180caaf00": "CHECK_QUEST_ACQUIRE_CONDITION_FOR_CONTEXT",
            "FUN_180cab520": "DISPATCH_QUEST_DIALOG_WITH_OPTIONAL_ITEM",
            "FUN_180caca90": "ACQUIRE_QUEST_AND_GIVE_ITEM",
            "FUN_180caf7c0": "DISPATCH_QUEST_DIALOG_WITH_INITIAL_PROGRESS_AND_ITEM",
            "FUN_180cafa40": "DISPATCH_QUEST_DIALOG_WITH_PROGRESS_AND_ITEM_EXCHANGE",
        }, {
            target: operations[("CALL", None, target)]["name"]
            for target in {"FUN_180caac10", "FUN_180caaf00", "FUN_180cab520",
                           "FUN_180caca90", "FUN_180caf7c0", "FUN_180cafa40"}
        })
        virtual_operations = report["semantic_catalog"]["virtual_operations"]
        self.assertEqual("GIVE_QUEST_REWARD", virtual_operations["CALL:NPC_HOST:0x1c0"]["name"])
        self.assertEqual("REMOVE_ALL_COLLECT_ITEMS",
                         virtual_operations["CALL:USER_HOST:0x1f0"]["name"])
        self.assertEqual("REMOVE_RECIPE", virtual_operations["CALL:USER_HOST:0x1f8"]["name"])
        expected_virtual_operations = {
            "CALL:NPC_HOST:0x28": ("GET_NPC_NAME_ID", "PROVEN"),
            "CALL:NPC_HOST:0xe0": ("DEBUG_MESSAGE", "PROVEN"),
            "CALL:NPC_HOST:0x190": ("CLOSE_ALL_DIALOGS", "PROVEN"),
            "CALL:NPC_HOST:0x1d8": ("PLAY_CUTSCENE", "PROVEN"),
            "CALL:NPC_HOST:0x268": ("SPAWN_NPC", "DERIVED"),
            "CALL:NPC_HOST:0x2f0": ("START_FOLLOWING", "PROVEN"),
            "CALL:NPC_HOST:0x338": ("SAY", "PROVEN"),
            "CALL:NPC_HOST:0x460": ("REQUEST_DECREASE_USER_MONEY", "PROVEN"),
            "CALL:NPC_HOST:0x500": ("PLAY_ANIMATION", "PROVEN"),
            "CALL:NPC_HOST:0x520": ("SET_DROP_OPTION_AND_OPEN_LOOT_WINDOW", "PROVEN"),
            "CALL:USER_HOST:0x120": ("SHARE_PROGRESS_MULTIPLE", "PROVEN"),
            "CALL:USER_HOST:0x158": ("GET_HIDDEN_QUEST_PROGRESS", "PROVEN"),
            "CALL:USER_HOST:0x190": ("PLAY_CUTSCENE", "PROVEN"),
            "CALL:USER_HOST:0x220": ("ENTER_INSTANCE", "PROVEN"),
            "CALL:USER_HOST:0x258": ("DELETE_QUEST_TIMER", "PROVEN"),
            "CALL:USER_HOST:0x270": ("ADD_SKILL_EFFECT", "PROVEN"),
            "CALL:USER_HOST:0x2b8": ("SAY", "PROVEN"),
        }
        self.assertEqual(expected_virtual_operations, {
            key: (virtual_operations[key]["name"], virtual_operations[key]["confidence"])
            for key in expected_virtual_operations
        })
        expected_derived_virtual_operations = {
            "CALL:NPC_HOST:0x220": "ADD_QUEST_TIMER",
            "CALL:NPC_HOST:0x228": "DELETE_QUEST_TIMER",
            "CALL:NPC_HOST:0x268": "SPAWN_NPC",
            "CALL:USER_HOST:0x210": "LEAVE_INSTANCE",
        }
        self.assertEqual(expected_derived_virtual_operations, {
            key: row["name"] for key, row in virtual_operations.items()
            if row["confidence"] == "DERIVED"
        })
        self.assertFalse(any(
            row["semantic"]["status"] == "UNKNOWN"
            and row.get("receiver") in {"NPC_HOST", "USER_HOST"}
            for row in report["operation_catalog"]
        ))
        self.assertEqual(1569, sum(
            shape["semantic_coverage"]["unknown"] == 0 for shape in report["shapes"]
        ))
        self.assertEqual("GET_QUEST_STATE", operations[("CALL", "USER_HOST", "0xd0")]["name"])
        self.assertEqual("SET_QUEST_PROGRESS", operations[("CALL", "USER_HOST", "0xf0")]["name"])
        self.assertEqual("SET_QUEST_PROGRESS_MEMORY_ONLY",
                         operations[("CALL", "USER_HOST", "0xf8")]["name"])
        self.assertEqual("SET_QUEST_SUCCESS", operations[("CALL", "USER_HOST", "0x100")]["name"])
        self.assertEqual("FIND_NPC", operations[("READ", "INTERFACE_REGISTRY", "0x38")]["name"])
        self.assertEqual("GET_OBJECT_ID", operations[("READ", "OBJECT_HOST", "0x30")]["name"])
        self.assertEqual("GET_CUTSCENE_ID",
                         operations[("READ", "QUIT_CUTSCENE_CONTEXT", "0x10")]["name"])
        self.assertEqual("OPEN_QUEST_REWARD_DIALOG", operations[("CALL", "NPC_HOST", "0x1b0")]["name"])
        self.assertEqual("CLOSE_DIALOG", operations[("READ", "NPC_HOST", "0x5d8")]["name"])
        self.assertEqual(20625, len(report["lab_resolutions"]))
        self.assertEqual(
            {"0x348": 10, "0xa0": 1, "0xa8": 1, "0x3b0": 1, "0x4b8": 1, "0x4c0": 1},
            dict(sorted(Counter(row["vtable_offset"] for row in report["lab_resolutions"].values()
                                if row["kind"] == "INDIRECT_VTABLE_TAIL").items())),
        )
        self.assertFalse(any(
            operation.get("receiver") in {"OTHER", "UNKNOWN", "UNRESOLVED"}
            for function in report["functions"].values()
            for operation in function["facts"]["operations"]
        ))


if __name__ == "__main__":
    unittest.main()
