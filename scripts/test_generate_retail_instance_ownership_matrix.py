import importlib.util
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/generate_retail_instance_ownership_matrix.py"
SPEC = importlib.util.spec_from_file_location("instance_ownership_matrix", SCRIPT)
GENERATOR = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(GENERATOR)


class InstanceOwnershipMatrixTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls) -> None:
        cls.report = GENERATOR.build(ROOT)
        cls.worlds = {world["world_id"]: world for world in cls.report["worlds"]}

    def test_full_matrix_counts(self) -> None:
        self.assertEqual({
            "kind": "AUDIT_PROJECTION",
            "input": "src/main/resources/aion/definitions/compact/instance/coverage.xml",
            "authoritative_retail_evidence": False,
        }, self.report["provenance"])
        summary = self.report["summary"]
        self.assertEqual(139, summary["production_worlds"])
        self.assertEqual(10, summary["dimensions"])
        self.assertEqual(68, summary["batches"])
        self.assertEqual(99, summary["registered_handler_worlds"])
        self.assertEqual(86, summary["behavior_handler_worlds"])
        self.assertEqual(79, summary["worlds_with_handler_dimensions"])
        self.assertEqual(278, summary["handler_dimensions_total"])
        self.assertEqual({
            "entry": 0,
            "spawn": 15,
            "ai": 0,
            "path": 1,
            "door": 38,
            "stage": 61,
            "score": 35,
            "reward": 14,
            "exit": 58,
            "recovery": 56,
        }, summary["handler_owned_by_dimension"])
        rejected = [world["world_id"] for world in self.report["worlds"]
                    if world["dimensions"]["recovery"] == "RECOVERY_REJECTED"]
        self.assertEqual([301320000, 301380000], rejected)

    def test_reusable_instance_families_share_batches(self) -> None:
        dredgions = {self.worlds[world_id]["batch_id"]
                     for world_id in (300110000, 300210000, 300440000, 301650000)}
        harmonies = {self.worlds[world_id]["batch_id"]
                     for world_id in (300450000, 300570000, 301100000)}
        self.assertEqual(1, len(dredgions))
        self.assertEqual(1, len(harmonies))

    def test_every_handler_dimension_has_a_registered_handler(self) -> None:
        missing = [world["world_id"] for world in self.report["worlds"]
                   if world["handler_dimensions"] and not world["handler"]]
        self.assertEqual([], missing)

    def test_handler_recovery_and_path_owners_have_static_evidence(self) -> None:
        classes = {}
        handlers = ROOT / "src/main/java/com/aionemu/gameserver/instance/handlers/scripts"
        declaration = re.compile(r"\bclass\s+(\w+)(?:\s+extends\s+([\w.]+))?")
        for path in handlers.rglob("*.java"):
            source = path.read_text(encoding="utf-8")
            match = declaration.search(source)
            if match:
                classes[match.group(1)] = ((match.group(2) or "").rsplit(".", 1)[-1], source)

        requirements = {
            "recovery": ("runtimeState(", "scheduleDeadline(", "setDoorState(", "InstanceSettlementService"),
            "path": ("moveTo(", "moveToLocation(", "PathService", "TeleportService"),
        }
        missing = []
        for dimension, markers in requirements.items():
            for world in self.report["worlds"]:
                if world["dimensions"][dimension] != "HANDLER":
                    continue
                source = (ROOT / world["handler"]).read_text(encoding="utf-8")
                match = declaration.search(source)
                class_name = match.group(1) if match else ""
                chain = []
                seen = set()
                while class_name in classes and class_name not in seen:
                    seen.add(class_name)
                    class_name, source = classes[class_name]
                    chain.append(source)
                if not any(marker in source for marker in markers for source in chain):
                    missing.append((dimension, world["world_id"]))
        self.assertEqual([], missing)

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
        self.assertIn("stale instance ownership matrix", result.stderr + result.stdout)


if __name__ == "__main__":
    unittest.main()
