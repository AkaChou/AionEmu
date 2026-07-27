import json
import tempfile
import unittest
from pathlib import Path

from generate_retail_script_transports import (
    build,
    event_for_call,
    float32,
    portal_service_projection,
    render,
    transport_domain_type,
)

ROOT = Path(__file__).resolve().parents[1]
SOURCE_MATRIX = ROOT / "docs/RETAIL_INSTANCE_TRANSPORT_SOURCE_MATRIX.json"


class RetailScriptTransportsTest(unittest.TestCase):

    def test_hex_float_keeps_trailing_hex_digit(self):
        self.assertAlmostEqual(278.1000061035156, float32("0x438b0ccd"))

    def test_checked_in_matrix_covers_every_dialog_registration_and_transport_type(self):
        report = json.loads(SOURCE_MATRIX.read_text(encoding="utf-8"))
        registrations = report["registrations"]
        calls = [call for registration in registrations for call in registration["calls"]]
        routes = [route for call in calls for route in call["routes"]]

        self.assertEqual(6, report["version"])
        self.assertEqual(818, len(registrations))
        self.assertEqual({0x1b}, {registration["event_code"] for registration in registrations})
        self.assertEqual({"DYNAMIC": 1, "STATIC": 817}, report["summary"]["registrations_by_binding"])
        self.assertEqual(520, len(calls))
        self.assertEqual({"ARENA_REENTRY": 19, "ITEM_GATED_TELEPORT": 2, "LIFT": 1, "TELEPORT": 498},
                         {domain: sum(call["domain_type"] == domain for call in calls)
                          for domain in {call["domain_type"] for call in calls}})
        self.assertTrue(all(call["domain_type_source"] in {"AUDITED_RULE", "TRANSPORT_API"}
                            for call in calls))
        self.assertTrue(all("endpoint_reasons" in call and "semantic_reasons" in call for call in calls))
        self.assertEqual(509, sum(route["status"] == "ENDPOINT_PROVEN" for route in routes))
        self.assertEqual(509, report["summary"]["endpoint_proven_routes"])

    def test_domain_type_requires_the_complete_retail_evidence_tuple(self):
        evidence = ("IDNovice_Elevator_Lever_Up", "FUN_180c78400", "0eff6fbaa0598942", "0x2d0")
        self.assertEqual("LIFT", transport_domain_type(*evidence))
        self.assertEqual("TELEPORT", transport_domain_type(*evidence[:-1], "0x2e0"))
        self.assertEqual("ARENA_REENTRY",
                         transport_domain_type("IDArena_1st_Resurrect_NPC", "FUN_180c6a5f0",
                                               "1f81f2a216ad13ba", "0x2e0"))
        self.assertEqual("ITEM_GATED_TELEPORT",
                         transport_domain_type("IDDC1_Dreadgion_Invade_Key_D", "FUN_180c70b80",
                                               "ffee58d2fe09b860", "0x2e0"))
        self.assertEqual("ITEM_GATED_TELEPORT",
                         transport_domain_type("Unrelated", "FUN_180c70b80", "ffee58d2fe09b860", "0x2e0"))

    def test_audited_callback_shapes_project_only_supported_portal_requirements(self):
        self.assertEqual(
            {"status": "EXPRESSIBLE", "requirements": {"race": "ELYOS", "min_level": 65, "max_level": 100}},
            portal_service_projection("ROUTE_PROVEN", "bbb59816bae2e848"),
        )
        self.assertEqual(
            "REJECT_UNMODELED_CALLBACK_SHAPE",
            portal_service_projection("ROUTE_PROVEN", "ffee58d2fe09b860")["status"],
        )
        self.assertEqual(
            {"status": "EXPRESSIBLE", "requirements": {"race": "ELYOS"}},
            portal_service_projection("ROUTE_PROVEN", "fa139d6ebdb0d079"),
        )
        self.assertEqual(
            {"status": "EXPRESSIBLE", "requirements": {"race": "ASMODIANS"}},
            portal_service_projection("ROUTE_PROVEN", "052b3137dad50d20"),
        )
        self.assertEqual(
            {"status": "EXPRESSIBLE", "requirements": {"min_level": 1, "max_level": 100}},
            portal_service_projection("ROUTE_PROVEN", "d85daa66a231db7b"),
        )
        self.assertEqual(
            {"status": "EXPRESSIBLE", "requirements": {}, "requirements_by_dialog": {
                "10001": {"race": "ELYOS"},
                "10002": {"race": "ASMODIANS"},
            }},
            portal_service_projection("ROUTE_PROVEN", "33ee7fc6fc736e7f"),
        )
        self.assertEqual(
            {"status": "EXPRESSIBLE", "requirements": {
                "items": [{"item_id": 185000283, "item_count": 1, "consume": False,
                           "err_message_id": 1403685}],
            }},
            portal_service_projection("ROUTE_PROVEN", "ffee58d2fe09b860",
                                      [
                                          {"kind": "READ", "target": "0x300", "raw":
                                              "(**(code **)(*plVar1 + 0x300))(plVar1,0xb06e15b);"},
                                          {"kind": "CALL", "target": "0x2a8", "raw":
                                              "(**(code **)(*plVar1 + 0x2a8))(plVar1,0x156b25,0x1e);"},
                                      ]),
        )
        self.assertEqual(
            {"status": "EXPRESSIBLE", "requirements": {
                "items": [{"item_id": 186000134, "item_count": 1, "consume": True, "err_item": 1097}],
                "instance_action": "JOIN_PLAY",
            }},
            portal_service_projection("ROUTE_PROVEN", "1f81f2a216ad13ba",
                                      [
                                          {"kind": "READ", "target": "0x300", "raw":
                                              "(**(code **)(*plVar1 + 0x300))(plVar1,0xb162306);"},
                                          {"kind": "READ", "target": "0x1d0", "raw":
                                              "(**(code **)(*plVar1 + 0x1d0))(plVar1,0xb162306,1);"},
                                          {"kind": "CALL", "target": "0x590", "raw":
                                              "(**(code **)(*owner + 0x590))(owner,plVar1,L\"JOIN_PLAY\");"},
                                          {"kind": "CALL", "target": "0x188", "raw":
                                              "(**(code **)(*owner + 0x188))(owner,plVar1,0x449,0);"},
                                      ]),
        )

    def test_dialog_variable_reassignment_stays_inside_its_branch(self):
        body = (
            "  dialog = *(int *)(param_2 + 0x28);\n"
            "  if (dialog == 10001) {\n"
            "    dialog = get_race();\n"
            "    if (dialog != 0) { return; }\n"
            "    teleport_light();\n"
            "  } else if (dialog == 10002) {\n"
            "    dialog = get_race();\n"
            "    if (dialog != 1) { return; }\n"
            "    teleport_dark();\n"
            "  }\n"
        )
        self.assertEqual(10001, event_for_call(body, body.index("teleport_light"))["dialog"])
        self.assertEqual(10002, event_for_call(body, body.index("teleport_dark"))["dialog"])

    def test_registered_callbacks_require_closed_retail_routes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            scripts = root / "scripts"
            fun = scripts / "fun"
            xml = root / "Map/XML"
            worlds = root / "Map/Worlds"
            fun.mkdir(parents=True)
            (xml / "China/ID").mkdir(parents=True)
            (worlds / "A").mkdir(parents=True)
            (worlds / "B").mkdir(parents=True)
            names = scripts / "IAIScriptNpcImp.cpp"
            names.write_text(
                '// @1 FUN_1 -> FUN_1\nPTR_vftable_aaa = 0;\nL"LiftScript";\n'
                '// @2 FUN_2 -> FUN_2\nPTR_vftable_bbb = 0;\nL"AliasScript";\n'
                '// @3 FUN_3 -> FUN_3\nPTR_vftable_ccc = 0;\nL"TalkOnly";\n', encoding="utf-8")
            registrations = fun / "fun_004.cpp"
            registrations.write_text(
                'FUN_180cb2ab0(&x,&PTR_vftable_aaa,0x1b,FUN_100,local);\n'
                'FUN_180cb2ab0(&x,&PTR_vftable_bbb,0x1b,FUN_200,local);\n'
                'FUN_180cb2ab0(&x,&PTR_vftable_ccc,0x1b,FUN_300,local);\n'
                'FUN_180cb2ab0(&x,&PTR_vftable_ddd,0x1b,FUN_400,local);\n'
                'FUN_180cb2ab0(&x,&PTR_vftable_eee,0x1a,FUN_500,local);\n', encoding="utf-8")
            (fun / "fun_100.cpp").write_text(
                '// @100 FUN_100 -> FUN_100\nvoid FUN_100(void)\n{\n'
                '  p = (int *)(*(code *)**(x **)(param_2 + 0x28))(param_2 + 0x28);\n'
                '  value = *p;\n  if (value != 10000) {\n    return;\n  }\n'
                '  value = (**(code **)(v + 0x60))(player);\n'
                '  if (value != 1) {\n    value = (**(code **)(v + 0x60))(player);\n'
                '    if (value != 0) {\n      return;\n    }\n  }\n'
                '  w = (**(code **)(v + 0x338))(player);\n'
                '  (**(code **)(v + 0x2d0))(player,w,0x435c0000,0x43550000,0x42fe0000,0xb4,1);\n'
                '  (**(code **)(owner + 0x5d8))(owner,player,&DAT);\n}\n',
                encoding="utf-8")
            (fun / "fun_200.cpp").write_text(
                '// @200 FUN_200 -> FUN_200\nvoid FUN_200(void)\n{\n'
                '  p = (int *)(*(code *)**(x **)(param_2 + 0x28))(param_2 + 0x28);\n'
                '  if (*p == 10000) {\n'
                '    (**(code **)(v + 0x2e0))(player,L"UPPER",0x2701);\n  }\n}\n', encoding="utf-8")
            (fun / "fun_300.cpp").write_text(
                '// @300 FUN_300 -> FUN_300\nvoid FUN_300(void)\n{\n  return;\n}\n', encoding="utf-8")
            (fun / "fun_400.cpp").write_text(
                '// @400 FUN_400 -> FUN_400\nvoid FUN_400(void)\n{\n  return;\n}\n', encoding="utf-8")
            npc_source = xml / "China/npcs.xml"
            npc_source.write_text(
                '<npcs><npc><id>1</id><name>Lift</name><ai_name>LiftScript</ai_name></npc>'
                '<npc><id>2</id><name>Alias</name><ai_name>AliasScript</ai_name></npc>'
                '<npc><id>3</id><name>Talk</name><ai_name>TalkOnly</ai_name></npc></npcs>', encoding="utf-8")
            world_ids = xml / "China/ID/WorldId.xml"
            world_ids.write_text('<root><data id="300200000">A</data><data id="300240000">B</data></root>',
                                 encoding="utf-8")
            (worlds / "A/world.xml").write_text(
                '<world><territory><npcs><npc><name>Lift</name><pos><x>1</x><y>2</y><z>3</z></pos>'
                '<dir>1</dir><editor_classname>PlaceableObject</editor_classname></npc></npcs></territory></world>',
                encoding="utf-8")
            (worlds / "B/world.xml").write_text(
                '<world><location_alias><name>UPPER</name><points><data><x>4</x><y>5</y><z>6</z>'
                '<dir>158</dir></data></points></location_alias><territory><npcs><npc><name>Alias</name>'
                '<pos><x>7</x><y>8</y><z>9</z></pos><dir>2</dir></npc></npcs></territory></world>',
                encoding="utf-8")

            report = build(scripts, fun, names, fun, xml, npc_source, worlds, world_ids)
            rows = {row["script_name"]: row for row in report["registrations"]}

            self.assertEqual({"NOT_TRANSPORT": 1, "ROUTE_PROVEN": 2, "ROUTE_REJECTED": 1},
                             report["summary"]["by_status"])
            self.assertEqual(4, report["summary"]["registrations"])
            self.assertEqual(1, report["summary"]["registrations_without_script_name"])
            self.assertEqual(2, report["summary"]["endpoint_proven_routes"])
            self.assertEqual(
                {"NOT_APPLICABLE": 1, "REJECT_ROUTE_NOT_PROVEN": 1,
                 "REJECT_UNMODELED_CALLBACK_SHAPE": 2},
                report["summary"]["portal_service_projection_by_status"],
            )
            self.assertEqual("PlaceableObject", rows["LiftScript"]["starts"][0]["object_type"])
            self.assertEqual("TELEPORT", rows["LiftScript"]["calls"][0]["domain_type"])
            self.assertEqual("TRANSPORT_API", rows["LiftScript"]["calls"][0]["domain_type_source"])
            self.assertEqual(220.0, rows["LiftScript"]["calls"][0]["routes"][0]["destination"]["x"])
            self.assertEqual(180, rows["LiftScript"]["calls"][0]["routes"][0]["destination"]["dir"])
            self.assertEqual(10000, rows["LiftScript"]["calls"][0]["event"]["dialog"])
            self.assertEqual("UPPER", rows["AliasScript"]["calls"][0]["routes"][0]["destination"]["alias"])
            self.assertEqual(10000, rows["AliasScript"]["calls"][0]["event"]["dialog"])
            features = rows["LiftScript"]["callback_features"]
            self.assertEqual(["IF", "IF", "IF"], [entry["kind"] for entry in features["predicates"]])
            self.assertEqual(
                ["READ:0x60", "READ:0x60", "READ:0x338", "TRANSPORT:0x2d0", "CALL:0x5d8"],
                [f"{entry['kind']}:{entry['target']}" for entry in features["operations"]],
            )
            self.assertEqual(3, report["summary"]["callback_shapes"])
            self.assertEqual(2, report["summary"]["route_proven_callback_shapes"])
            self.assertTrue(json.loads(render(report))["provenance"]["authoritative_retail_evidence"])
            self.assertEqual("fun/fun_*.cpp", report["provenance"]["registrations"])
            self.assertIn("raw callback predicates", report["provenance"]["proof_scope"])
            self.assertIn("unaudited predicate semantic interpretation",
                          report["provenance"]["excluded_semantics"])
            self.assertEqual({"ARENA_REENTRY", "ITEM_GATED_TELEPORT"},
                             {rule["domain_type"]
                              for rule in report["provenance"]["transport_shape_domain_type_rules"]})


if __name__ == "__main__":
    unittest.main()
