from __future__ import annotations

import importlib.util
import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
SCRIPT = ROOT / "docs/quest/quest_legacy_coverage.py"
SPEC = importlib.util.spec_from_file_location("quest_legacy_coverage", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
coverage = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = coverage
SPEC.loader.exec_module(coverage)


class QuestLegacyCoverageTest(unittest.TestCase):
	def test_report_is_byte_deterministic_and_uses_source_sets(self) -> None:
		first = coverage.report_bytes()
		second = coverage.report_bytes()
		self.assertEqual(first, second)

		report = coverage.build_report()
		ids = [entry["id"] for entry in report["entries"]]
		self.assertEqual(sorted(ids), ids)
		self.assertEqual(len(ids), len(set(ids)))
		self.assertEqual(len(coverage.load_legacy_templates()), report["counts"]["legacy_templates"])
		self.assertEqual(len(coverage.load_catalog()), report["counts"]["catalog_entries"])
		self.assertEqual(len(coverage.load_legacy_xml_owners()), report["counts"]["legacy_xml_owners"])
		self.assertEqual(len(coverage.load_java_owners()), report["counts"]["java_owners"])

	def test_metadata_conversion_preserves_reward_and_start_condition_groups(self) -> None:
		legacy = coverage.load_legacy_templates()
		rewards = coverage.metadata_only_definition(legacy[1007]).find("metadata/reward-groups")
		starts = coverage.metadata_only_definition(legacy[1510]).find("metadata/start-condition-groups")

		self.assertIsNotNone(rewards)
		self.assertEqual(6, len(list(rewards)))
		self.assertIsNotNone(starts)
		self.assertEqual(2, len(list(starts)))

	def test_inert_item_collecting_owner_requires_every_route_ingress_to_be_zero(self) -> None:
		inert = ET.fromstring('<item_collecting id="1" start_npc_ids="0"/>')
		end_route = ET.fromstring('<item_collecting id="1" start_npc_ids="0" end_npc_ids="800001"/>')
		action_route = ET.fromstring('<item_collecting id="1" start_npc_ids="0" action_item_ids="700001"/>')

		self.assertTrue(coverage.is_inert_item_collecting_owner(inert))
		self.assertFalse(coverage.is_inert_item_collecting_owner(end_route))
		self.assertFalse(coverage.is_inert_item_collecting_owner(action_route))


if __name__ == "__main__":
	unittest.main()
