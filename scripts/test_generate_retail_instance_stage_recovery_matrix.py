import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/generate_retail_instance_stage_recovery_matrix.py"
SPEC = importlib.util.spec_from_file_location("instance_stage_recovery_matrix", SCRIPT)
GENERATOR = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.path.insert(0, str(ROOT / "scripts"))
sys.modules[SPEC.name] = GENERATOR
SPEC.loader.exec_module(GENERATOR)


class InstanceStageRecoveryMatrixTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls) -> None:
        cls.report = GENERATOR.build(ROOT)
        cls.worlds = {world["world_id"]: world for world in cls.report["worlds"]}

    def test_full_matrix_is_conservative_and_explicit(self) -> None:
        summary = self.report["summary"]
        self.assertEqual(2, self.report["version"])
        self.assertEqual("RUNTIME_AUDIT_PROJECTION", self.report["provenance"]["kind"])
        self.assertFalse(self.report["provenance"]["authoritative_retail_evidence"])
        self.assertEqual(139, summary["production_worlds"])
        self.assertEqual(99, summary["registered_handler_worlds"])
        self.assertEqual(92, summary["condition_worlds"])
        self.assertEqual(1183, summary["condition_variables"])
        self.assertEqual(203, summary["condition_variables_missing_producers"])
        self.assertEqual(2, summary["pattern_spawn_gaps"])
        self.assertEqual(60, summary["declared_stage_owners"]["HANDLER"])
        self.assertEqual(1, summary["declared_stage_owners"]["SCRIPT_NPC"])
        self.assertEqual(56, summary["declared_recovery_owners"]["HANDLER"])
        self.assertEqual({
            "ALREADY_EXTERNAL_OWNER": 70,
            "REJECT_EVIDENCE_GAP": 12,
            "RETAIN_HANDLER": 57,
        }, summary["conversion_statuses"])
        self.assertEqual(421, summary["script_stage_bindings"])
        self.assertEqual(6, summary["script_npc_runtime_entries"])
        self.assertEqual(6, summary["script_npc_runtime_matches"])
        self.assertEqual(0, summary["script_npc_runtime_mismatches"])
        self.assertNotIn("CONVERSION_READY", summary["conversion_statuses"])
        for world in self.report["worlds"]:
            if "HANDLER" in {world["declared_stage_owner"], world["declared_recovery_owner"]}:
                self.assertIn(world["conversion_status"], {"RETAIN_HANDLER", "REJECT_EVIDENCE_GAP"})

    def test_pattern_bindings_reference_one_catalog_model(self) -> None:
        catalog = {model["pattern"]: model for model in self.report["pattern_models"]}
        self.assertEqual(len(catalog), len(self.report["pattern_models"]))
        for world in self.report["worlds"]:
            for binding in world["pattern_bindings"]:
                self.assertIn(binding["pattern"], catalog)
                self.assertNotIn("operations", binding)

    def test_known_stage_families_remain_owned_until_semantics_are_proven(self) -> None:
        sulfur = self.worlds[300060000]
        self.assertEqual("HANDLER_EVENT_FLOW", sulfur["stage_classification"])
        self.assertEqual("HANDLER_USES_PERSISTENT_STATE", sulfur["recovery_classification"])
        self.assertEqual("RETAIN_HANDLER", sulfur["conversion_status"])
        self.assertIn("scriptdll_stage_semantics_not_compiled", sulfur["evidence_gaps"])

        arena = self.worlds[300350000]
        self.assertEqual("HANDLER_STATE_MACHINE", arena["stage_classification"])
        self.assertIn("PvPArenaInstance", arena["handler_model"]["lineage"])
        self.assertTrue(arena["handler_model"]["score_states"])

        tiamat = self.worlds[300510000]
        self.assertEqual("RETAIL_PATTERN_MODELED", tiamat["stage_classification"])
        self.assertEqual("DECLARED_STATELESS_CONTRADICTION", tiamat["recovery_classification"])
        self.assertIn("declared_stateless_has_runtime_state", tiamat["evidence_gaps"])

    def test_cradle_script_npc_stage_owner_has_retail_source_closure(self) -> None:
        cradle = self.worlds[301550000]
        self.assertEqual("SCRIPT_NPC_MODELED", cradle["stage_classification"])
        self.assertEqual(6, len(cradle["script_npc_runtime_entries"]))
        self.assertTrue(all(entry["source_status"] == "RESOLVED"
                            for entry in cradle["script_npc_runtime_entries"]))
        matches = [match for binding in cradle["script_npc_bindings"] for match in binding["runtime_matches"]]
        self.assertEqual(6, len(matches))
        variables = {variable["name"]: variable for variable in cradle["condition_model"]["variables"]}
        for match in matches:
            self.assertTrue(any(producer["owner"] == "SCRIPT_NPC"
                                for producer in variables[match["variable"]]["producers"]))

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
        self.assertIn("stale instance stage/recovery matrix", result.stderr + result.stdout)


if __name__ == "__main__":
    unittest.main()
