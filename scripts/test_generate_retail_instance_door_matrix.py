import importlib.util
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/generate_retail_instance_door_matrix.py"
SPEC = importlib.util.spec_from_file_location("instance_door_matrix", SCRIPT)
GENERATOR = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(GENERATOR)


class InstanceDoorMatrixTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls) -> None:
        cls.report = GENERATOR.build(ROOT)

    def test_full_matrix_counts_and_explicit_rejections(self) -> None:
        summary = self.report["summary"]
        self.assertEqual(3, self.report["version"])
        self.assertEqual({
            "kind": "RUNTIME_AUDIT_PROJECTION",
            "authoritative_retail_evidence": False,
            "retail_source_matrix": "docs/RETAIL_STATIC_DOOR_SOURCE_MATRIX.json",
            "retail_reference_graph": "docs/RETAIL_STATIC_DOOR_REFERENCE_GRAPH.json",
        }, self.report["provenance"])
        self.assertEqual(139, summary["production_worlds"])
        self.assertEqual(82, summary["static_door_worlds"])
        self.assertEqual(988, summary["static_doors"])
        self.assertEqual(25, summary["batches"])
        self.assertEqual(74, summary["handler_control_calls"])
        self.assertEqual(354, summary["pattern_control_bindings"])
        self.assertEqual(33, summary["legacy_control_bindings"])
        self.assertEqual(3, summary["service_control_bindings"])
        self.assertEqual(63, summary["unresolved_controls"])
        self.assertEqual(0, summary["ownership_mismatches"])
        self.assertEqual({"HANDLER": 74, "LEGACY_AI": 33, "PATTERN": 354, "SERVICE": 3},
                         summary["controls_by_owner"])
        self.assertEqual({"MATCH": 82, "NOT_APPLICABLE": 57},
                         summary["worlds_by_retail_source_status"])
        self.assertEqual({"MATCH": 988}, summary["door_source_associations"])
        self.assertEqual({"MATCH": 988}, summary["initial_state_comparisons"])
        self.assertEqual({"openDoor_helper": 12, "setDoorState": 62},
                         summary["handler_calls_by_mechanism"])
        self.assertEqual({
            "ALL_RESOLVED": 3,
            "MISSING": 1,
            "MISSING_RETAIL_REFERENCE": 45,
            "REJECTED_RETAIL_TARGET_ABSENT": 17,
            "RESOLVED": 98,
            "RESOLVED_SET": 292,
            "RUNTIME_EXPRESSION": 8,
        }, summary["controls_by_target_status"])
        self.assertNotIn("REJECT_RUNTIME_ONLY_HANDLER_STATE", summary["worlds_by_recovery"])
        self.assertNotIn("REJECT_RUNTIME_ONLY_LEGACY_STATE", summary["worlds_by_recovery"])
        self.assertEqual({
            "HANDLER_EVENT_BRIDGE": 14,
            "LEGACY_AI_CONTROLLED": 1,
            "MIXED_CONTROL_OWNERS": 18,
            "NO_STATIC_DOORS": 54,
            "PATTERN_CONTROLLED": 16,
            "REJECT_UNRESOLVED_DOOR_TARGET": 10,
            "SERVICE_CONTROLLED": 3,
            "STATIC_OR_KEY_OWNED": 23,
        }, summary["worlds_by_classification"])
        self.assertEqual([], [world["world_id"] for world in self.report["worlds"]
                              if world["declared_owner"] != world["suggested_owner"]])

        stale_empty_owners = [world["world_id"] for world in self.report["worlds"]
                              if world["classification"] == "NO_STATIC_DOORS"
                              and world["declared_owner"] in {"HANDLER", "RETAIL_DATA"}]
        self.assertEqual([], stale_empty_owners)
        stale_template_owners = [world["world_id"] for world in self.report["worlds"]
                                 if world["classification"] == "STATIC_OR_KEY_OWNED"
                                 and world["declared_owner"] == "HANDLER"]
        self.assertEqual([], stale_template_owners)
        pattern_owners = [world["declared_owner"] for world in self.report["worlds"]
                          if world["classification"] == "PATTERN_CONTROLLED"]
        self.assertEqual(["RETAIL_PATTERN"] * 16, pattern_owners)
        legacy_controls = [control for world in self.report["worlds"] for control in world["legacy_controls"]]
        self.assertTrue(all(control["mechanism"] == "legacy_ai_setDoorState" for control in legacy_controls))
        self.assertTrue(all(control["recovery"] == "PERSISTED_RUNTIME_STATE" for control in legacy_controls))
        legacy_owners = [world["declared_owner"] for world in self.report["worlds"]
                         if world["classification"] == "LEGACY_AI_CONTROLLED"]
        self.assertEqual(["SCRIPT_AI"], legacy_owners)

        unresolved = [(world["world_id"], control) for world in self.report["worlds"]
                      for control in world["unresolved_controls"]]
        self.assertEqual({
            "MISSING": 1,
            "MISSING_RETAIL_REFERENCE": 45,
            "REJECTED_RETAIL_TARGET_ABSENT": 17,
        }, {status: sum(control["target_status"] == status for _world, control in unresolved)
            for status in {control["target_status"] for _world, control in unresolved}})
        self.assertEqual({"PATTERN": 62, "LEGACY_AI": 1}, {
            owner: sum(control["owner_type"] == owner for _world, control in unresolved)
            for owner in {control["owner_type"] for _world, control in unresolved}
        })
        payload = "\n".join(json.dumps([world, control], sort_keys=True, separators=(",", ":"))
                            for world, control in unresolved)
        self.assertEqual("72928631b5ed4ff51e33d440d926d23ce2adfc41ab3a48fe69da664504505a9e",
                         hashlib.sha256(payload.encode()).hexdigest())

    def test_every_door_has_unique_runtime_lookup(self) -> None:
        for world in self.report["worlds"]:
            index = GENERATOR.lookup(world["doors"])
            ambiguous = {door_id: doors for door_id, doors in index.items() if len(doors) != 1}
            self.assertEqual({}, ambiguous, world["world_id"])

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
        self.assertIn("stale instance door matrix", result.stderr + result.stdout)


if __name__ == "__main__":
    unittest.main()
