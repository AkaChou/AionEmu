#!/usr/bin/env python3

import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/generate_retail_instance_portal_matrix.py"
SPEC = importlib.util.spec_from_file_location("instance_portal_matrix", SCRIPT)
GENERATOR = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(GENERATOR)


class InstancePortalMatrixTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls) -> None:
        cls.report = GENERATOR.build(ROOT)
        cls.routes = [route for world in cls.report["worlds"] for route in world["routes"]]

    def test_dialog_requirements_are_projected_per_endpoint(self) -> None:
        projection = {
            "status": "EXPRESSIBLE",
            "requirements": {},
            "requirements_by_dialog": {
                "10001": {"race": "ELYOS"},
                "10002": {"race": "ASMODIANS"},
            },
        }
        self.assertEqual(
            {"status": "EXPRESSIBLE", "requirements": {"race": "ELYOS"}},
            GENERATOR.portal_service_projection_for_event(projection, {"dialog": 10001}),
        )
        self.assertEqual(
            {"status": "REJECT_UNMODELED_DIALOG_REQUIREMENTS", "requirements": {}},
            GENERATOR.portal_service_projection_for_event(projection, {"dialog": 10003}),
        )

    def test_full_matrix_counts(self) -> None:
        self.assertEqual({
            "kind": "RUNTIME_AUDIT_PROJECTION",
            "authoritative_retail_evidence": False,
            "retail_source_matrices": {
                "direct_portals": "docs/RETAIL_DIRECT_PORTAL_SOURCE_MATRIX.json",
                "script_transports": "docs/RETAIL_INSTANCE_TRANSPORT_SOURCE_MATRIX.json",
            },
            "retail_reference_projections": {
                "script_transports": "docs/RETAIL_SCRIPT_TRANSPORT_REFERENCE_PROJECTION.json",
            },
        }, self.report["provenance"])
        self.assertEqual({
            "production_worlds": 139,
            "worlds_with_routes": 112,
            "start_npcs": 342,
            "dynamic_start_npcs": 15,
            "routes": 840,
            "batches": 93,
            "routes_by_association": {"DESTINATION": 354, "START": 486},
            "routes_by_mechanism": {
                "handler": 23,
                "legacy_ai": 87,
                "portal_dialog": 407,
                "portal_use": 299,
                "retail_pattern_alias": 8,
                "teleporter": 16,
            },
            "routes_by_conversion": {
                "ALREADY_DATA_DRIVEN": 634,
                "REJECT_INCOMPLETE_RETAIL_PATTERN": 2,
                "REJECT_NO_RETAIL_PATTERN": 70,
                "REJECT_NO_RETAIL_START": 85,
                "REJECT_RUNTIME_ADDRESS_DESTINATION": 4,
                "REJECT_RUNTIME_EVENT_TRIGGER": 23,
                "REJECT_RUNTIME_RELATIVE_DESTINATION": 11,
                "REJECT_UNMODELED_CONDITION_TRIGGER": 11,
            },
            "routes_by_start_endpoint": {
                "DYNAMIC": 44,
                "INSTANCE_CONDITIONAL": 179,
                "INSTANCE_STATIC": 263,
                "MISSING": 96,
                "WORLD_CONDITIONAL": 62,
                "WORLD_STATIC": 196,
            },
            "routes_by_destination_endpoint": {
                "DYNAMIC": 17,
                "INSTANCE_STATIC": 628,
                "WORLD_STATIC": 195,
            },
            "routes_by_transport_type": {
                "ADDRESS_TELEPORT": 4,
                "PORTAL_DIALOG": 347,
                "PORTAL_USE": 299,
                "RELATIVE_TELEPORT": 11,
                "RETAIL_PATTERN_ALIAS": 8,
                "LIFT": 2,
                "SCRIPT_DIALOG_COORDINATES": 12,
                "SCRIPT_DIALOG_CURRENT_WORLD_ALIAS": 46,
                "SCRIPT_TELEPORT": 95,
                "TELEPORTER": 16,
            },
            "routes_by_type_status": {"RETAIL_PROVEN": 60, "RUNTIME_MODELED": 780},
            "routes_by_endpoint_status": {
                "DYNAMIC_TO_INSTANCE_STATIC": 25,
                "DYNAMIC_TO_WORLD_STATIC": 19,
                "INSTANCE_CONDITIONAL_TO_DYNAMIC": 2,
                "INSTANCE_CONDITIONAL_TO_INSTANCE_STATIC": 113,
                "INSTANCE_CONDITIONAL_TO_WORLD_STATIC": 64,
                "INSTANCE_STATIC_TO_DYNAMIC": 15,
                "INSTANCE_STATIC_TO_INSTANCE_STATIC": 136,
                "INSTANCE_STATIC_TO_WORLD_STATIC": 112,
                "MISSING_TO_INSTANCE_STATIC": 96,
                "WORLD_CONDITIONAL_TO_INSTANCE_STATIC": 62,
                "WORLD_STATIC_TO_INSTANCE_STATIC": 196,
            },
            "routes_by_runtime_consumer": {
                "INSTANCE_HANDLER": 23,
                "LEGACY_AI": 87,
                "PortalService": 706,
                "RetailPatternAI2": 8,
                "TeleporterData/TeleportService2": 16,
            },
            "retail_transport_evidence": 60,
            "script_transport_candidates": 137,
            "script_transport_candidates_by_start_status": {
                "MATCH": 86,
                "MISMATCH": 37,
                "MISSING": 14,
            },
            "script_transport_candidates_by_status": {
                "ALREADY_DATA_DRIVEN_RETAIL_PROVEN": 60,
                "REJECT_MISSING_RUNTIME_START": 14,
                "REJECT_ROUTE_NOT_PROVEN": 38,
                "REJECT_RUNTIME_CONSUMER": 7,
                "REJECT_UNMODELED_CALLBACK_SHAPE": 18,
            },
        }, self.report["summary"])

    def test_batches_partition_every_route(self) -> None:
        batches = {batch["id"]: batch for batch in self.report["batches"]}
        self.assertEqual(len(self.routes), sum(batch["count"] for batch in batches.values()))
        self.assertTrue(all(route["batch_id"] in batches for route in self.routes))

    def test_external_instance_entry_uses_full_static_spawn_index(self) -> None:
        route = next(route for route in self.routes
                     if route["npc_id"] == 800509 and route["destination"].get("world_id") == 300030000)
        self.assertEqual(210020000, route["start_world_id"])
        self.assertEqual("288.708099", route["start_points"][0]["x"])
        self.assertEqual("Npcs", route["start_points"][0]["spawn_type"])
        self.assertEqual("ALREADY_DATA_DRIVEN", route["conversion"])

        conditional = next(route for route in self.routes
                           if route["npc_id"] == 731663 and route["destination"].get("world_id") == 300050000)
        self.assertEqual(400010000, conditional["start_world_id"])
        self.assertEqual("Instance_Rift", conditional["start_points"][0]["spawn_type"])
        self.assertEqual("ALREADY_DATA_DRIVEN", conditional["conversion"])

    def test_delayed_dredgion_teleporters_keep_handler_start_ownership(self) -> None:
        expected = {
            300210000: {730311: ("415.033875", "34"), 730312: ("572.038208", "10")},
            300440000: {730558: ("415.033875", "34"), 730559: ("572.038208", "10")},
            301650000: {801989: ("415.033875", "34"), 801990: ("572.038208", "10")},
        }
        for world_id, npcs in expected.items():
            routes = [route for world in self.report["worlds"] if world["world_id"] == world_id
                      for route in world["routes"] if route["npc_id"] in npcs]
            self.assertEqual(set(npcs), {route["npc_id"] for route in routes})
            for route in routes:
                x, entity_id = npcs[route["npc_id"]]
                self.assertEqual((x, "0", entity_id),
                                 tuple(route["start_points"][0][key]
                                       for key in ("x", "heading", "entity_id")))
                self.assertTrue(route["start_dynamic"])
                self.assertIn("DredgionInstance.java", route["start_owner"])
                self.assertEqual("ALREADY_DATA_DRIVEN", route["conversion"])

    def test_script_transport_source_keeps_proven_and_missing_endpoints_distinct(self) -> None:
        routes = [route for route in self.routes if route["npc_id"] in {730321, 730322}
                  and route["destination"].get("world_id") == 300200000]
        up = next(route for route in routes if route["npc_id"] == 730321)
        down = next(route for route in routes if route["npc_id"] == 730322)
        self.assertEqual(("220.868271", "213.262894", "89.27873"),
                         tuple(up["start_points"][0][key] for key in ("x", "y", "z")))
        self.assertEqual(("220", "213", "127", "60"),
                         tuple(up["destination"][key] for key in ("x", "y", "z", "heading")))
        self.assertEqual("BEAM_ANIMATION", up["semantics"]["animation"])
        self.assertEqual("300200000", up["semantics"]["attributes"]["source_world_id"])
        self.assertEqual("ALREADY_DATA_DRIVEN", up["conversion"])
        self.assertEqual(("LIFT", "RETAIL_PROVEN",
                          "INSTANCE_STATIC_TO_INSTANCE_STATIC", "PortalService"),
                         tuple(up[key] for key in
                               ("transport_type", "type_status", "endpoint_status", "runtime_consumer")))
        self.assertEqual("IDNovice_Elevator_Lever_Up", up["retail_transport_evidence"]["script_name"])
        self.assertEqual("PlaceableObject", up["retail_transport_evidence"]["starts"][0]["object_type"])
        self.assertEqual("0x2d0", up["retail_transport_evidence"]["api_offset"])
        self.assertEqual("LIFT", up["retail_transport_evidence"]["domain_type"])
        callback_features = up["retail_transport_evidence"]["callback_features"]
        self.assertEqual(["IF"], [predicate["kind"] for predicate in callback_features["predicates"]])
        self.assertEqual(
            ["CALL:0x5d8", "READ:0x338", "TRANSPORT:0x2d0", "CALL:FUN_180c51a60"],
            [f"{operation['kind']}:{operation['target']}" for operation in callback_features["operations"]],
        )
        self.assertIsNone(down["start_world_id"])
        self.assertEqual(("216.6916", "213.28465", "89.27873"),
                         tuple(down["destination"][key] for key in ("x", "y", "z")))
        self.assertEqual("REJECT_NO_RETAIL_START", down["conversion"])
        self.assertEqual(("PORTAL_USE", "RUNTIME_MODELED", "MISSING_TO_INSTANCE_STATIC", "PortalService"),
                         tuple(down[key] for key in
                               ("transport_type", "type_status", "endpoint_status", "runtime_consumer")))

        aturam = [route for route in self.routes if route["npc_id"] == 730538]
        self.assertEqual({300240000, 300241000}, {route["start_world_id"] for route in aturam})
        self.assertEqual({("691.534302", "457.016998", "656.622314", "52")},
                         {tuple(route["destination"][key] for key in ("x", "y", "z", "heading"))
                          for route in aturam})
        self.assertEqual({"ALREADY_DATA_DRIVEN"}, {route["conversion"] for route in aturam})
        self.assertEqual({"SCRIPT_DIALOG_CURRENT_WORLD_ALIAS"},
                         {route["transport_type"] for route in aturam})
        self.assertEqual({"IDStation_teleport_upper"},
                         {route["retail_transport_evidence"]["script_name"] for route in aturam})
        self.assertEqual(1, len({route["retail_transport_evidence"]["callback_features"]["shape_id"]
                                for route in aturam}))

    def test_transport_types_and_endpoints_have_closed_batch_dimensions(self) -> None:
        self.assertEqual({"RETAIL_PROVEN", "RUNTIME_MODELED"},
                         {route["type_status"] for route in self.routes})
        self.assertTrue(all(route["type_source"] and route["runtime_consumer"] for route in self.routes))
        missing = [route for route in self.routes if route["endpoint_status"].startswith("MISSING_")]
        self.assertEqual(96, len(missing))
        self.assertTrue(all(route["conversion"].startswith("REJECT_") for route in missing))
        lifts = [route for route in self.routes if route["transport_type"] == "LIFT"]
        self.assertEqual({(300200000, 730321), (302330000, 730321)},
                         {(route["start_world_id"], route["npc_id"]) for route in lifts})
        self.assertTrue(all(route["retail_transport_evidence"]["domain_type"] == "LIFT" for route in lifts))
        self.assertEqual(60, sum(route["type_status"] == "RETAIL_PROVEN" for route in self.routes))

    def test_script_transport_candidates_use_retail_portal_service_projection(self) -> None:
        candidates = self.report["script_transport_candidates"]
        self.assertEqual(137, len(candidates))
        instance_world_ids = set(GENERATOR.production_worlds(ROOT))
        self.assertTrue(all(candidate["start_world_id"] in instance_world_ids
                            or candidate["destination"]["world_id"] in instance_world_ids
                            for candidate in candidates))
        self.assertFalse(any(candidate["status"] == "CONVERSION_READY" for candidate in candidates))
        archives = [candidate for candidate in candidates if candidate["npc_id"] in {731809, 731810}]
        self.assertEqual({"ALREADY_DATA_DRIVEN_RETAIL_PROVEN"},
                         {candidate["status"] for candidate in archives})
        self.assertTrue(all(candidate["callback_shape"] == "c773c543096b3491" for candidate in archives))
        self.assertEqual({"teleport_01", "teleport_02"},
                         {candidate["runtime_routes"][0]["destination"]["alias"] for candidate in archives})
        converted = [candidate for candidate in candidates if candidate["npc_id"] in {801532, 801533}]
        self.assertEqual({"ALREADY_DATA_DRIVEN_RETAIL_PROVEN"},
                         {candidate["status"] for candidate in converted})
        self.assertTrue(all(candidate["callback_shape"] == "c0693a58b0486877" for candidate in converted))
        save_points = [candidate for candidate in candidates
                       if candidate["npc_id"] in {805744, 805745, 834188, 834189, 834190}]
        self.assertEqual(8, len(save_points))
        self.assertEqual({"ALREADY_DATA_DRIVEN_RETAIL_PROVEN"},
                         {candidate["status"] for candidate in save_points})
        self.assertTrue(all(candidate["callback_shape"] == "c773c543096b3491" for candidate in save_points))
        vault_doors = [candidate for candidate in candidates if candidate["npc_id"] in {832924, 832925}]
        self.assertEqual(4, len(vault_doors))
        self.assertEqual({"ALREADY_DATA_DRIVEN_RETAIL_PROVEN"},
                         {candidate["status"] for candidate in vault_doors})
        self.assertTrue(all(candidate["callback_shape"] == "e4ab7c479ad41c01" for candidate in vault_doors))
        heroes_exits = [candidate for candidate in candidates if candidate["npc_id"] in {835684, 835685}]
        self.assertEqual({"ALREADY_DATA_DRIVEN_RETAIL_PROVEN"},
                         {candidate["status"] for candidate in heroes_exits})
        self.assertEqual({("COORDINATES", 1, 100)}, {
            (candidate["transport_type"], candidate["requirements"]["min_level"],
             candidate["requirements"]["max_level"]) for candidate in heroes_exits
        })
        quest_portal = next(candidate for candidate in candidates if candidate["npc_id"] == 805377)
        self.assertEqual(("ALREADY_DATA_DRIVEN_RETAIL_PROVEN", "portal_dialog", "general"),
                         (quest_portal["status"], quest_portal["runtime_ai"],
                          quest_portal["runtime_ai_declared"]))
        custom = next(candidate for candidate in candidates if candidate["npc_id"] == 730641)
        self.assertEqual(("ALREADY_DATA_DRIVEN_RETAIL_PROVEN", "portal_dialog", "MISMATCH"),
                         (custom["status"], custom["runtime_ai"], custom["runtime_start_status"]))
        eternity_exits = [candidate for candidate in candidates if candidate["npc_id"] in {835349, 835350}]
        self.assertEqual({10001, 10002}, {candidate["dialog"] for candidate in eternity_exits})
        self.assertEqual({"ALREADY_DATA_DRIVEN_RETAIL_PROVEN"},
                         {candidate["status"] for candidate in eternity_exits})

        animar = next(candidate for candidate in candidates if candidate["npc_id"] == 833843)
        self.assertEqual(("ALREADY_DATA_DRIVEN_RETAIL_PROVEN", {"race": "ELYOS"}),
                         (animar["status"], animar["requirements"]))
        multi_point = next(candidate for candidate in candidates if candidate["npc_id"] == 731811)
        self.assertEqual(("ALREADY_DATA_DRIVEN_RETAIL_PROVEN", 2, "Alias_3rd_Boss_Room_In_1"),
                         (multi_point["status"], len(multi_point["destination"]["points"]),
                          multi_point["runtime_routes"][0]["destination"]["alias"]))
        repeated_starts = next(candidate for candidate in candidates if candidate["npc_id"] == 730871)
        self.assertEqual(("REJECT_MISSING_RUNTIME_START", 5),
                         (repeated_starts["status"], len(repeated_starts["starts"])))

    def test_missing_start_routes_have_closed_retail_evidence(self) -> None:
        condition = next(route for route in self.routes if route["npc_id"] == 702715)
        self.assertEqual("REJECT_UNMODELED_CONDITION_TRIGGER", condition["conversion"])
        self.assertEqual("CONDITION_INFO_LIST", condition["start_evidence"]["classification"])
        self.assertIn("idldf5re_solo/world_N.xml#60", condition["start_evidence"]["sources"])
        self.assertFalse(any(route["npc_id"] == 0 for route in self.routes))

    def test_reachable_retail_patterns_replace_only_proven_legacy_teleports(self) -> None:
        evergale = next(route for route in self.routes if route["npc_id"] == 835277)
        self.assertEqual("retail_pattern_alias", evergale["mechanism"])
        self.assertEqual("ALREADY_DATA_DRIVEN", evergale["conversion"])
        self.assertEqual("IDEternity_W_Tele_ItoOP_L_Up", evergale["destination"]["alias"])
        self.assertEqual(2, len(evergale["destination"]["points"]))

        chronomancer = [route for route in self.routes if route["npc_id"] == 247386]
        self.assertEqual(2, len(chronomancer))
        self.assertEqual({"REJECT_INCOMPLETE_RETAIL_PATTERN"},
                         {route["conversion"] for route in chronomancer})
        self.assertEqual("missing NPC skill SKILLI_INDEX_0",
                         chronomancer[0]["pattern_evidence"]["reason"])

        arena = next(route for route in self.routes if route["npc_id"] == 205426)
        self.assertEqual("REJECT_NO_RETAIL_PATTERN", arena["conversion"])

    def test_runtime_dynamic_routes_keep_explicit_owner_models(self) -> None:
        self.assertFalse(any(route["conversion"] in {"REJECT_DYNAMIC_DESTINATION", "REJECT_DYNAMIC_TRIGGER"}
                             for route in self.routes))

        castle_gates = [route for route in self.routes
                        if route["owner"].endswith("/Castle_GateAI2.java")]
        self.assertEqual(11, len(castle_gates))
        self.assertEqual({"REJECT_RUNTIME_RELATIVE_DESTINATION"},
                         {route["conversion"] for route in castle_gates})
        self.assertEqual({"RELATIVE_CURRENT_POSITION"},
                         {route["destination"]["runtime_model"]["kind"] for route in castle_gates})

        studio_portals = [route for route in self.routes
                          if route["owner"].endswith("/StudioPortalAI2.java")]
        self.assertEqual(4, len(studio_portals))
        self.assertEqual({"HOUSING_ADDRESS"},
                         {route["destination"]["runtime_model"]["kind"] for route in studio_portals})

        empyrean = [route for route in self.routes
                    if route["owner"].endswith("/EmpyreanCrucibleInstance.java")]
        self.assertEqual(7, len(empyrean))
        self.assertEqual({300300000}, {route["destination"]["world_id"] for route in empyrean})
        self.assertEqual("onReviveEvent", empyrean[0]["semantics"]["trigger"]["event"])

        transidium = [route for route in self.routes
                      if route["owner"].endswith("/TransidiumAnnexInstance.java")]
        self.assertEqual({400020000, 400040000, 400050000, 400060000},
                         {route["destination"]["world_id"] for route in transidium})

    def test_check_rejects_stale_report(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "matrix.json"
            output.write_text("{}\n", encoding="utf-8")
            result = subprocess.run(
                [sys.executable, str(SCRIPT), "--root", str(ROOT), "--output", str(output), "--check"],
                capture_output=True,
                text=True,
            )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("stale instance portal matrix", result.stderr + result.stdout)


if __name__ == "__main__":
    unittest.main()
