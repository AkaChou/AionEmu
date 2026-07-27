import json
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

from generate_retail_instance_stage_callbacks import build, render, validate_leave_instance_semantics


ROOT = Path(__file__).resolve().parents[1]
STAGE_PROJECTION = ROOT / "scripts/retail-instance-stage-reference-graph.json"


class RetailInstanceStageCallbacksTest(unittest.TestCase):

    @staticmethod
    def write_leave_instance_semantics(script_root: Path) -> None:
        server = script_root.parent / "MainServer_Server64"
        sources = {
            "fun/fun_050.cpp": "void FUN_1404dc6a0 { (*param_1 + 0x230))(param_1,0); }",
            "classes/Account/IUserImp.cpp": (
                "void IUserImp_LeaveInstance { User_LeaveInstance(*(int64_t *)(param_1 + 8),param_2); }"),
            "classes/Account/User.cpp": (
                "void User_LeaveInstance { if (param_2 != '\\0') {} WorldDb_GetDynamicWorld(); "
                "*plVar9 + 0x1c8; *in_RCX + 0x150; }"),
            "classes/World/DynamicWorld.cpp": (
                "bool DynamicWorld_ExitPointLocAlias { *param_1 + 0x1b8; "
                "if (_Src == (wchar_t *)0x0) { *(uint64_t *)(param_2 + 0x43d8); } wcsncpy_s; }"),
        }
        for relative, source in sources.items():
            path = server / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(source, encoding="utf-8")

    def test_checked_in_projection_covers_every_0x1a_registration_and_instance_closure(self):
        report = json.loads(STAGE_PROJECTION.read_text(encoding="utf-8"))
        summary = report["authority"]["script_stage_callback_inventory"]["source_summary"]
        self.assertEqual(4, report["version"])
        self.assertEqual("script_stage", report["projection"])
        self.assertEqual(139, summary["production_worlds"])
        self.assertEqual(824, summary["registrations"])
        self.assertEqual(706, summary["unique_callbacks"])
        self.assertEqual(698, summary["callback_bodies"])
        self.assertEqual(8, len(summary["missing_callback_bodies"]))
        self.assertTrue(summary["instance_bindings"] > 0)
        self.assertTrue(summary["instance_worlds_with_callbacks"] > 0)
        self.assertEqual({"ambiguous": 0, "unresolved": 0},
                         {key: report["summary"][key] for key in ("ambiguous", "unresolved")})

    def test_cradle_runtime_projection_matches_retail_callback_matrix(self):
        report = json.loads(STAGE_PROJECTION.read_text(encoding="utf-8"))
        actual = {
            int(node.get("npc_id")): {key: int(node.get(key, "1")) if key != "variable" else node.get(key) for key in (
                "world_id", "item_id", "item_count", "variable", "value",
                "failure_message_id", "success_message_id")}
            for node in ET.parse(ROOT / "src/main/resources/aion/definitions/compact/script-npcs.xml")
            .getroot().findall("item_gate_variable")
        }
        expected = {}
        candidates = report["authority"]["script_stage_callback_inventory"]["conversion_candidates"]
        for candidate in candidates:
            if candidate["status"] != "ALREADY_DATA_DRIVEN" or candidate["world_ids"] != [301550000]:
                continue
            for npc_id in candidate["npc_ids"]:
                expected[npc_id] = candidate["expected_runtime"]
        self.assertEqual(expected, actual)

    def test_leave_instance_semantics_and_endpoints_are_separate_from_stage(self):
        report = json.loads(STAGE_PROJECTION.read_text(encoding="utf-8"))
        callbacks = [reference for reference in report["references"]
                     if reference["kind"] == "script_callback"
                     and any(operation["family"] == "LEAVE_INSTANCE"
                             for target in reference["targets"]
                             for operation in target["operation_models"])]
        endpoints = [reference for reference in report["references"]
                     if reference["kind"] == "instance_exit_endpoint"]
        self.assertEqual(82, len(callbacks))
        self.assertTrue(all(reference["reference_status"] == "RESOLVED"
                            and reference["semantic_status"] == "RESOLVED" for reference in callbacks))
        self.assertEqual({"exit"}, {operation["dimension"] for reference in callbacks
                                    for operation in reference["targets"][0]["operation_models"]
                                    if operation["family"] == "LEAVE_INSTANCE"})
        self.assertEqual({"INSTANCE_RULE_ALIAS": 8, "PLAYER_PREVIOUS_LOCATION": 71, "UNMODELED": 3},
                         {model: sum(reference["raw"] == model for reference in endpoints)
                          for model in {reference["raw"] for reference in endpoints}})
        self.assertEqual({"REJECTED": 3, "RESOLVED": 79},
                         {status: sum(reference["status"] == status for reference in endpoints)
                          for status in {reference["status"] for reference in endpoints}})

    def test_build_keeps_source_model_separate_from_instance_reachability(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            scripts = root / "ScriptDLL"
            self.write_leave_instance_semantics(scripts)
            fun = scripts / "fun"
            retail = root / "Map/XML"
            worlds = root / "Map/Worlds"
            aionemu = root / "AionEmu"
            fun.mkdir(parents=True)
            (scripts / "classes/NPC").mkdir(parents=True)
            (retail / "China/ID").mkdir(parents=True)
            (worlds / "InstanceA").mkdir(parents=True)
            (worlds / "OpenWorld").mkdir(parents=True)
            definitions = aionemu / "src/main/resources/aion/definitions/compact"
            definitions.mkdir(parents=True)
            (definitions / "script-npcs.xml").write_text("<script_npcs/>\n", encoding="utf-8")
            npc_templates = aionemu / "src/main/resources/aion/data/static_data/npcs"
            npc_templates.mkdir(parents=True)
            (npc_templates / "npc_template.xml").write_text("<npc_templates/>\n", encoding="utf-8")

            (scripts / "classes/NPC/IAIScriptNpcImp.cpp").write_text(
                '// @1 FUN_1 -> FUN_1\nPTR_vftable_aaa = 0;\nL"StageButton";\n'
                '// @2 FUN_2 -> FUN_2\nPTR_vftable_bbb = 0;\nL"OutsideButton";\n'
                '// @3 FUN_3 -> FUN_3\nPTR_vftable_ccc = 0;\nL"MissingBody";\n', encoding="utf-8")
            (fun / "fun_001.cpp").write_text(
                'FUN_180cb2ab0(&x,&PTR_vftable_aaa,0x1a,FUN_100,local);\n'
                'FUN_180cb2ab0(&x,&PTR_vftable_bbb,0x1a,FUN_200,local);\n'
                'FUN_180cb2ab0(&x,&PTR_vftable_ccc,0x1a,&LAB_300,local);\n'
                'FUN_180cb2ab0(&x,&PTR_vftable_aaa,0x1b,FUN_400,local);\n', encoding="utf-8")
            (fun / "fun_100.cpp").write_text(
                '// @100 FUN_100 -> FUN_100\nvoid FUN_100(void)\n{\n'
                '  count = (**(code **)(*player + 0x300))(player,1001);\n'
                '  if (count == 0) { message = 2001; } else {\n'
                '    (**(code **)(*player + 0x1d0))(player,1001,1);\n'
                '    (**(code **)(*owner + 0x578))(owner,L"stage_flag",2);\n'
                '    message = 2002;\n  }\n'
                '  (**(code **)(*owner + 0x3a0))(owner,message,player,&data);\n}\n', encoding="utf-8")
            (fun / "fun_200.cpp").write_text(
                '// @200 FUN_200 -> FUN_200\nvoid FUN_200(void)\n{\n  return;\n}\n', encoding="utf-8")
            (retail / "China/npcs.xml").write_text(
                '<npcs><npc><id>10</id><name>StageNpc</name><ai_name>StageButton</ai_name></npc>'
                '<npc><id>20</id><name>OutsideNpc</name><ai_name>OutsideButton</ai_name></npc>'
                '<npc><id>30</id><name>MissingNpc</name><ai_name>MissingBody</ai_name></npc></npcs>',
                encoding="utf-8")
            world_ids = '<data id="300000001">InstanceA</data>' + ''.join(
                f'<data id="{300000001 + index}">Instance{index}</data>' for index in range(1, 139))
            (retail / "China/ID/WorldId.xml").write_text(
                f'<root>{world_ids}<data id="100000001">OpenWorld</data></root>', encoding="utf-8")
            (retail / "China/instance_cooltime.xml").write_text(
                '<root><instance_cooltime><id>1</id><name>InstanceA</name></instance_cooltime></root>',
                encoding="utf-8")
            (worlds / "InstanceA/world_N.xml").write_text(
                '<world><territory><condition_info><condition><extcondition>stage_flag == 1</extcondition></condition>'
                '<spawn_group_list><spawn_group><npcs><npc><name>StageNpc</name><pos><x>1</x><y>2</y><z>3</z>'
                '</pos></npc><npc><name>MissingNpc</name><pos><x>4</x><y>5</y><z>6</z></pos></npc></npcs>'
                '</spawn_group></spawn_group_list></condition_info></territory></world>', encoding="utf-8")
            (worlds / "OpenWorld/world.xml").write_text(
                '<world><territory><npcs><npc><name>OutsideNpc</name><pos><x>1</x><y>2</y><z>3</z>'
                '</pos></npc></npcs></territory></world>', encoding="utf-8")
            world_map_rows = ''.join(
                f'<map id="{300000001 + index}" name="W{index}" instance="true"/>'
                for index in range(139))
            world_maps = npc_templates.parent / "world_maps.xml"
            world_maps.write_text(f'<maps>{world_map_rows}</maps>', encoding="utf-8")

            report = build(scripts, retail, aionemu)
            rows = {row["script_name"]: row for row in report["registrations"]}
            self.assertEqual("INSTANCE_SOURCE_MODELED", rows["StageButton"]["instance_status"])
            self.assertEqual("NOT_APPLICABLE", rows["OutsideButton"]["instance_status"])
            self.assertEqual("REJECT_MISSING_CALLBACK_BODY", rows["MissingBody"]["instance_status"])
            self.assertEqual(1, report["summary"]["instance_worlds_with_callbacks"])
            world = next(row for row in report["worlds"] if row["world_id"] == 300000001)
            write = next(link for link in world["callback_bindings"]
                         if link["script_name"] == "StageButton")["condition_variable_writes"][0]
            self.assertTrue(write["consumed_by_retail_condition"])
            self.assertTrue(json.loads(render(report))["provenance"]["authoritative_retail_evidence"])

    def test_leave_instance_semantic_source_guard_rejects_stale_wrapper(self):
        with tempfile.TemporaryDirectory() as directory:
            scripts = Path(directory) / "MainServer_ScriptDLL64"
            self.write_leave_instance_semantics(scripts)
            wrapper = scripts.parent / "MainServer_Server64/fun/fun_050.cpp"
            wrapper.write_text("void FUN_1404dc6a0 {}", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "stale leave-instance semantic evidence"):
                validate_leave_instance_semantics(scripts)


if __name__ == "__main__":
    unittest.main()
