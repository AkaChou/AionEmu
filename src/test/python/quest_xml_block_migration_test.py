from __future__ import annotations

import contextlib
import importlib.util
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[3]
SCRIPT = ROOT / "docs/quest/quest_xml_block_migration.py"
SPEC = importlib.util.spec_from_file_location("quest_xml_block_migration", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
migration = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = migration
SPEC.loader.exec_module(migration)


def quest_xml(nodes: str, transitions: str, progress: str = "") -> str:
	return f'''<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="990900" version="1">
  <metadata name="fixture" display-name-id="1" min-level="0" max-level="99" category="QUEST"/>
  {progress}
  <nodes>{nodes}</nodes>
  <transitions>{transitions}</transitions>
</quest-definition>
'''


NODES_1D = '''
    <node label="started"><project status="START"><vars><var name="var0" value="0"/></vars></project></node>
    <node label="k1"><project status="START"><vars><var name="var0" value="1"/></vars></project></node>
    <node label="k2"><project status="START"><vars><var name="var0" value="2"/></vars></project></node>
    <node label="reward"><project status="REWARD"><vars><var name="var0" value="2"/></vars></project></node>
'''

PROGRESS_1D = '''
  <progress>
    <bit-field name="var0" offset="0" width="2" min="0" max="3" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>
'''

KILL = '''
      <event><kill-npc npc-id="{npc_id}"/></event>
      <after-commit><sync-quest-state mode="PACKET_ONLY"/></after-commit>
'''


class QuestXmlBlockMigrationTest(unittest.TestCase):
	def setUp(self) -> None:
		self.temp_dir = tempfile.TemporaryDirectory(dir=ROOT / "target", prefix="quest-xml-migration-")
		self.directory = Path(self.temp_dir.name)

	def tearDown(self) -> None:
		self.temp_dir.cleanup()

	def write(self, name: str, content: str) -> Path:
		path = self.directory / name
		path.write_text(content, encoding="utf-8")
		return path

	def test_all_four_blocks_have_the_same_normalized_ir_as_manual_expansion(self) -> None:
		blocks = '''
    <kill-routes source="started" target="k1" npc-ids="215468 215469"/>
    <npc-report npc-id="203941" source="started" target="reward" page="1352"/>
    <npc-item-report npc-id="800937" source="started" target="reward" item-id="182215285" required="1" remove-count="ALL"/>
    <counter-grid><dimension field="var0" required="2" npc-ids="212600 212601"/></counter-grid>
'''
		manual = f'''
    <transition source="started" target="k1">{KILL.format(npc_id=215468)}</transition>
    <transition source="started" target="k1">{KILL.format(npc_id=215469)}</transition>
    <transition source="started" target="started"><event><talk-to-npc npc-id="203941" dialog-id="31"/></event><after-commit><show-quest-dialog dialog-id="1352"/></after-commit></transition>
    <transition source="started" target="reward"><event><talk-to-npc npc-id="203941" dialog-id="1009"/></event><after-commit><sync-quest-state mode="PACKET_ONLY"/><show-quest-dialog dialog-id="5"/></after-commit></transition>
    <transition source="started" target="reward" priority="0"><event><talk-to-npc npc-id="800937" dialog-id="39"/></event><conditions><has-item item-id="182215285" count="1"/></conditions><actions><remove-item item-id="182215285" count="ALL"/></actions><after-commit><sync-quest-state mode="PACKET_ONLY"/><show-quest-dialog dialog-id="5"/></after-commit></transition>
    <transition source="started" target="started" priority="1"><event><talk-to-npc npc-id="800937" dialog-id="39"/></event><after-commit><show-quest-dialog dialog-id="2716"/></after-commit></transition>
    <transition source="started" target="reward" priority="0"><event><talk-to-npc npc-id="800937" dialog-id="20002"/></event><conditions><has-item item-id="182215285" count="1"/></conditions><actions><remove-item item-id="182215285" count="ALL"/></actions><after-commit><sync-quest-state mode="PACKET_ONLY"/><show-quest-dialog dialog-id="5"/></after-commit></transition>
    <transition source="started" target="started" priority="1"><event><talk-to-npc npc-id="800937" dialog-id="20002"/></event><after-commit><close-dialog/></after-commit></transition>
    <transition source="started" target="k1">{KILL.format(npc_id=212600)}</transition>
    <transition source="started" target="k1">{KILL.format(npc_id=212601)}</transition>
    <transition source="k1" target="k2">{KILL.format(npc_id=212600)}</transition>
    <transition source="k1" target="k2">{KILL.format(npc_id=212601)}</transition>
'''
		block_xml = quest_xml(NODES_1D, blocks, PROGRESS_1D)
		manual_xml = quest_xml(NODES_1D, manual, PROGRESS_1D)
		self.assertEqual(migration.semantic_summary(block_xml.encode()), migration.semantic_summary(manual_xml.encode()))

	def test_matcher_order_prefers_counter_grid_before_kill_routes(self) -> None:
		transitions = f'''
    <transition source="started" target="k1">{KILL.format(npc_id=212600)}</transition>
    <transition source="started" target="k1">{KILL.format(npc_id=212601)}</transition>
    <transition source="k1" target="k2">{KILL.format(npc_id=212600)}</transition>
    <transition source="k1" target="k2">{KILL.format(npc_id=212601)}</transition>
    <transition source="k2" target="reward">{KILL.format(npc_id=215468)}</transition>
    <transition source="k2" target="reward">{KILL.format(npc_id=215469)}</transition>
'''
		path = self.write("priority.xml", quest_xml(NODES_1D, transitions, PROGRESS_1D))
		result, _ = migration.analyze_file(path, False)
		self.assertEqual("strict_match", result["classification"])
		self.assertEqual(["counter-grid", "kill-routes"],
			[replacement["type"] for replacement in result["replacements"]])
		self.assertEqual(result["before_ir"], result["after_ir"])

	def test_value_then_node_order_is_preserved(self) -> None:
		values = [(2, 0), (0, 2), (1, 1), (0, 0), (2, 1), (1, 0), (0, 1), (1, 2), (2, 2)]
		node_by_key = {value: f"v{value[0]}{value[1]}" for value in values}
		nodes = "".join(
			f'<node label="{node_by_key[value]}"><project status="START"><vars>'
			f'<var name="var0" value="{value[0]}"/><var name="var1" value="{value[1]}"/>'
			"</vars></project></node>"
			for value in values)
		progress = '''
  <progress>
    <bit-field name="var0" offset="0" width="2" min="0" max="2" persistence="PERSISTENT" scope="LOCAL"/>
    <bit-field name="var1" offset="2" width="2" min="0" max="2" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>
'''
		transitions: list[str] = []
		for field_index, npc_ids in ((0, (212600, 212601)), (1, (212603, 212604))):
			for value in range(2):
				for key in values:
					if key[field_index] != value:
						continue
					target_key = list(key)
					target_key[field_index] += 1
					target = node_by_key[tuple(target_key)]
					for npc_id in npc_ids:
						transitions.append(
							f'<transition source="{node_by_key[key]}" target="{target}">'
							f'{KILL.format(npc_id=npc_id)}</transition>')
		path = self.write("value-order.xml", quest_xml(nodes, "".join(transitions), progress))
		result, _ = migration.analyze_file(path, False)
		self.assertEqual("counter-grid", result["replacements"][0]["type"])
		self.assertEqual("VALUE_THEN_NODE", result["replacements"][0]["dimensions"][0]["source_order"])
		self.assertEqual("VALUE_THEN_NODE", result["replacements"][0]["dimensions"][1]["source_order"])

	def test_comments_between_transitions_are_left_explicit_and_other_bytes_survive_write(self) -> None:
		transitions = f'''
    <!-- before matched transitions -->
    <transition source="started" target="k1">{KILL.format(npc_id=215468)}</transition>
    <transition source="started" target="k1">{KILL.format(npc_id=215469)}</transition>
    <!-- after matched transitions -->
'''
		path = self.write("comments.xml", quest_xml(NODES_1D, transitions, PROGRESS_1D))
		before = path.read_bytes()
		result, _ = migration.analyze_file(path, True)
		after = path.read_bytes()
		self.assertEqual("migrated", result["classification"])
		self.assertIn(b"<!-- before matched transitions -->", after)
		self.assertIn(b"<!-- after matched transitions -->", after)
		self.assertEqual(before.split(b"<transitions>", 1)[0], after.split(b"<transitions>", 1)[0])

		commented = transitions.replace(
			f'    <transition source="started" target="k1">{KILL.format(npc_id=215469)}</transition>',
			f'    <!-- between -->\n    <transition source="started" target="k1">{KILL.format(npc_id=215469)}</transition>')
		commented_path = self.write("comment-between.xml", quest_xml(NODES_1D, commented, PROGRESS_1D))
		commented_result, _ = migration.analyze_file(commented_path, False)
		self.assertEqual("no_strict_match", commented_result["classification"])
		self.assertIn("non_whitespace_between_transitions", commented_result["skip_reasons"])

	def test_ir_mismatch_is_reported_without_writing(self) -> None:
		transitions = f'''
    <transition source="started" target="k1">{KILL.format(npc_id=215468)}</transition>
    <transition source="started" target="k1">{KILL.format(npc_id=215469)}</transition>
'''
		path = self.write("rollback.xml", quest_xml(NODES_1D, transitions, PROGRESS_1D)
)
		original = path.read_bytes()
		real_summary = migration.semantic_summary
		calls = 0

		def mismatching_summary(data: bytes) -> dict[str, object]:
			nonlocal calls
			calls += 1
			value = real_summary(data)
			return value if calls == 1 else {**value, "sha256": "forced-mismatch"}

		with patch.object(migration, "semantic_summary", mismatching_summary):
			result, _ = migration.analyze_file(path, True)
		self.assertEqual("ir_mismatch_rolled_back", result["classification"])
		self.assertEqual(original, path.read_bytes())

	def test_dirty_skip_and_report_are_idempotent(self) -> None:
		transitions = f'''
    <transition source="started" target="k1">{KILL.format(npc_id=215468)}</transition>
    <transition source="started" target="k1">{KILL.format(npc_id=215469)}</transition>
'''
		path = self.write("report.xml", quest_xml(NODES_1D, transitions, PROGRESS_1D))
		relative = str(path.relative_to(ROOT))
		dirty_report = self.directory / "dirty-report.json"
		with patch.object(migration, "QUEST_DIR", self.directory), \
			patch.object(migration, "modified_paths", return_value={relative}), \
			patch.object(sys, "argv", ["quest_xml_block_migration.py", "--report", str(dirty_report)]), \
			contextlib.redirect_stdout(io.StringIO()):
			self.assertEqual(0, migration.main())
		dirty_data = json.loads(dirty_report.read_text(encoding="utf-8"))
		self.assertEqual(2, dirty_data["schema_version"])
		self.assertEqual(1, dirty_data["classification_counts"]["dirty_skipped"])
		self.assertEqual(["dirty_worktree"], dirty_data["files"][0]["skip_reasons"])

		first_report = self.directory / "first.json"
		second_report = self.directory / "second.json"
		for report in (first_report, second_report):
			with patch.object(migration, "QUEST_DIR", self.directory), \
				patch.object(migration, "modified_paths", return_value=set()), \
				patch.object(sys, "argv", ["quest_xml_block_migration.py", "--report", str(report)]), \
				contextlib.redirect_stdout(io.StringIO()):
				self.assertEqual(0, migration.main())
		self.assertEqual(json.loads(first_report.read_text(encoding="utf-8")),
			json.loads(second_report.read_text(encoding="utf-8")))


if __name__ == "__main__":
	unittest.main()
