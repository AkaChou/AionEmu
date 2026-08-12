#!/usr/bin/env python3
"""Self-check for quest_xml_compact_migration: format fidelity, single-line nodes,
comments/declaration preservation, and the fragment path."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import quest_xml_compact_migration as m

SAMPLE = '''<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="1131" version="1">
  <metadata name="Undelivered Armour" display-name-id="1102301" min-level="10" max-level="2147483647" category="IMPORTANT"/>
  <!-- keep this comment -->
  <progress>
    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>
  <nodes>
    <node label="unaccepted">
      <project status="NONE">
        <vars>
          <var name="var0" value="0"/>
        </vars>
      </project>
    </node>
    <node label="started"><project status="START"><vars><var name="var0" value="0"/></vars></project></node>
    <node label="complete"><project status="COMPLETE"/></node>
  </nodes>
</quest-definition>
'''

EXPECTED = '''<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="1131" version="1">
  <metadata name="Undelivered Armour" display-name-id="1102301" min-level="10" max-level="2147483647" category="IMPORTANT"/>
  <!-- keep this comment -->
  <progress>
    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>
  <nodes>
    <node label="unaccepted" status="NONE">
          <var name="var0" value="0"/>
    </node>
    <node label="started" status="START"><var name="var0" value="0"/></node>
    <node label="complete" status="COMPLETE"/>
  </nodes>
</quest-definition>
'''


class MigrationSelfCheck(unittest.TestCase):
    def test_scan_does_not_write_input_files(self):
        import tempfile
        import subprocess
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp) / "repo"
            target = repo / "target" / "quest-xml-migration"
            quests = repo / "src" / "main" / "resources" / "aion" / "data" / "static_data" / "quest_definition" / "quests"
            quests.mkdir(parents=True)
            (quests / "1131.xml").write_text(SAMPLE)
            (repo / "src" / "test" / "java").mkdir(parents=True)
            (repo / "src" / "test" / "resources").mkdir(parents=True)
            before = (quests / "1131.xml").read_bytes()
            env = dict(__import__("os").environ)
            # point module constants at the temp repo via monkeypatch
            m.REPO_ROOT = repo
            m.PROD_QUESTS = quests
            m.TEST_RESOURCES = repo / "src" / "test" / "resources"
            m.TEST_JAVA = repo / "src" / "test" / "java"
            m.REPORT_DIR = target
            m.REPORT_PATH = target / "report.json"
            m.BEFORE_DIR = target / "before"
            old_argv = sys.argv
            old_sources = m.SCAN_SOURCES
            m.SCAN_SOURCES = {"prod": [quests],
                              "test_resources": [repo / "src" / "test" / "resources"],
                              "test_java": [repo / "src" / "test" / "java"]}
            sys.argv = ["quest_xml_compact_migration.py"]
            try:
                rc = m.main()
            finally:
                sys.argv = old_argv
                m.SCAN_SOURCES = old_sources
            self.assertEqual(rc, 0)
            self.assertEqual(before, (quests / "1131.xml").read_bytes())

    def test_format_fidelity(self):
        new_text, count, fails = m.migrate_nodes_in_xml(SAMPLE, "1131.xml")
        self.assertEqual(count, 3)
        self.assertEqual(fails, [])
        self.assertEqual(new_text, EXPECTED)
        # declaration, comment, attribute order, trailing whitespace preserved
        self.assertTrue(new_text.startswith('<?xml version="1.0" encoding="UTF-8"?>'))
        self.assertIn("<!-- keep this comment -->", new_text)
        self.assertTrue(new_text.endswith("</quest-definition>\n") or new_text.endswith("</quest-definition>"))

    def test_parallel_scan_aggregates_files_deterministically(self):
        import json
        import tempfile
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp) / "repo"
            quests = repo / "src/main/resources/aion/data/static_data/quest_definition/quests"
            tests = repo / "src/test/resources"
            java = repo / "src/test/java"
            quests.mkdir(parents=True)
            tests.mkdir(parents=True)
            java.mkdir(parents=True)
            (quests / "1131.xml").write_text(SAMPLE)
            (quests / "1132.xml").write_text(SAMPLE.replace('id="1131"', 'id="1132"'))
            target = repo / "target/quest-xml-migration"
            old_values = (m.REPO_ROOT, m.REPORT_DIR, m.REPORT_PATH, m.BEFORE_DIR, m.SCAN_SOURCES)
            old_argv = sys.argv
            m.REPO_ROOT = repo
            m.REPORT_DIR = target
            m.REPORT_PATH = target / "report.json"
            m.BEFORE_DIR = target / "before"
            m.SCAN_SOURCES = {"prod": [quests], "test_resources": [tests], "test_java": [java]}
            sys.argv = ["quest_xml_compact_migration.py", "--workers", "2"]
            try:
                self.assertEqual(0, m.main())
                report = json.loads(m.REPORT_PATH.read_text())
            finally:
                sys.argv = old_argv
                m.REPO_ROOT, m.REPORT_DIR, m.REPORT_PATH, m.BEFORE_DIR, m.SCAN_SOURCES = old_values
            self.assertEqual(2, report["changed_file_count"])
            self.assertEqual(6, report["node_migration_count"])
            self.assertEqual(2, report["parallelism"]["python_processes"])

    def test_single_line_nodes(self):
        text = '<?xml version="1.0"?><quest-definition id="1" version="1"><nodes><node label="a"><project status="NONE"/></node><node label="b"><project status="START"><vars><var name="x" value="1"/></vars></project></node></nodes></quest-definition>'
        new_text, count, fails = m.migrate_nodes_in_xml(text, "one.xml")
        self.assertEqual(count, 2)
        self.assertEqual(new_text, '<?xml version="1.0"?><quest-definition id="1" version="1"><nodes><node label="a" status="NONE"/><node label="b" status="START"><var name="x" value="1"/></node></nodes></quest-definition>')

    def test_crlf_preserved(self):
        text = SAMPLE.replace("\n", "\r\n")
        new_text, count, _ = m.migrate_nodes_in_xml(text, "crlf.xml")
        self.assertEqual(count, 3)
        self.assertIn("\r\n", new_text)
        self.assertNotIn("\n\n", new_text)

    def test_fragment_with_placeholders(self):
        fragment = '<node label="start"><project status="START"><vars><var name="var0" value="0"/></vars></project></node><node label="done"><project status="REWARD"><vars><var name="var0" value="1"/></vars></project></node>%s'
        new_text, count, fails, _ = m.migrate_nodes_in_fragment(fragment, "frag")
        self.assertEqual(count, 2)
        self.assertEqual(fails, [])
        self.assertIn('<node label="start" status="START"><var name="var0" value="0"/></node>', new_text)
        self.assertIn('<node label="done" status="REWARD"><var name="var0" value="1"/></node>', new_text)
        self.assertTrue(new_text.endswith("%s"))

    def test_no_legacy_left(self):
        new_text, count, _ = m.migrate_nodes_in_xml(SAMPLE, "1131.xml")
        self.assertFalse(m.file_has_legacy(new_text))

    def test_mixed_node_format_rejected(self):
        text = SAMPLE.replace('<node label="started"><project status="START"><vars><var name="var0" value="0"/></vars></project></node>',
                              '<node label="started" status="START"><project status="START"/></node>')
        _, _, fails = m.migrate_nodes_in_xml(text, "mixed.xml")
        self.assertTrue(fails)
        self.assertIn("mixed node format", fails[0]["reason"])

    def test_legacy_normalization_is_fail_closed(self):
        invalid = SAMPLE.replace('<project status="NONE">', '<project status="NONE" unexpected="x">')
        _, _, failures = m.migrate_nodes_in_xml(invalid, "extra-project-attribute.xml")
        self.assertEqual("legacy project wrapper is not one-to-one", failures[0]["reason"])

        invalid = SAMPLE.replace('<vars>', '<vars unexpected="x">', 1)
        _, _, failures = m.migrate_nodes_in_xml(invalid, "extra-vars-attribute.xml")
        self.assertEqual("legacy vars wrapper is not one-to-one", failures[0]["reason"])

    def test_comment_with_legacy_looking_node_is_not_rewritten(self):
        comment = '<!-- example: <node label="example"><project status="NONE"/></node> -->\n'
        text = SAMPLE.replace('  <!-- keep this comment -->\n', '  ' + comment)
        migrated, count, failures = m.migrate_nodes_in_xml(text, "comment-boundary.xml")
        self.assertEqual([], failures)
        self.assertEqual(3, count)
        self.assertIn(comment.strip(), migrated)

    def test_dirty_bypass_option_is_not_exposed(self):
        import subprocess
        result = subprocess.run([sys.executable, str(Path(m.__file__).resolve()), "--help"],
                                text=True, capture_output=True, check=False)
        self.assertEqual(0, result.returncode)
        self.assertNotIn("--force-dirty", result.stdout)

    def test_all_non_overlapping_npc_dialog_rectangles_in_one_run_are_matched(self):
        transitions = m.parse_xml('''<transitions>
          <transition source="a" target="a"><event><talk-to-npc npc-id="1" dialog-id="31"/></event><after-commit><show-quest-dialog dialog-id="1352"/></after-commit></transition>
          <transition source="a" target="a"><event><talk-to-npc npc-id="2" dialog-id="31"/></event><after-commit><show-quest-dialog dialog-id="1352"/></after-commit></transition>
          <transition source="b" target="b"><event><talk-to-npc npc-id="3" dialog-id="1003"/></event><after-commit><close-dialog/></after-commit></transition>
          <transition source="b" target="b"><event><talk-to-npc npc-id="4" dialog-id="1003"/></event><after-commit><close-dialog/></after-commit></transition>
        </transitions>''')
        replacements, unmatched = m.match_domain_blocks(transitions)
        self.assertEqual(2, len(replacements))
        self.assertEqual([(0, 1), (2, 3)], [(start, end) for start, end, _, _ in replacements])
        self.assertEqual([], unmatched)

    def test_npc_dialog_rewrite_inherits_parent_indentation(self):
        xml = '''<quest-definition id="1" version="1">
  <metadata name="x" display-name-id="1" min-level="0" max-level="1" category="QUEST"/>
  <nodes><node label="a" status="START"/></nodes>
  <transitions>
    <transition source="a" target="a"><event><talk-to-npc npc-id="1" dialog-id="31"/></event><after-commit><close-dialog/></after-commit></transition>
    <transition source="a" target="a"><event><talk-to-npc npc-id="2" dialog-id="31"/></event><after-commit><close-dialog/></after-commit></transition>
  </transitions>
</quest-definition>'''
        migrated, counts, no_strict_match, failures = m.migrate_blocks_in_xml(xml, "indent.xml")
        self.assertEqual([], failures)
        self.assertEqual([], no_strict_match)
        self.assertEqual({"npc-dialog": 1}, counts)
        self.assertIn('    <npc-dialog source="a"', migrated)
        self.assertIn('\n      <close-dialog />\n    </npc-dialog>', migrated)

    def test_domain_block_rewrite_preserves_intervening_comments_and_reports_unmatched_runs(self):
        xml = '''<quest-definition id="1" version="1">
  <metadata name="x" display-name-id="1" min-level="0" max-level="1" category="QUEST"/>
  <nodes><node label="a" status="START"/></nodes>
  <transitions>
    <transition source="a" target="a"><event><talk-to-npc npc-id="1" dialog-id="31"/></event><after-commit><close-dialog/></after-commit></transition>
    <!-- this boundary must survive -->
    <transition source="a" target="a"><event><talk-to-npc npc-id="2" dialog-id="31"/></event><after-commit><close-dialog/></after-commit></transition>
  </transitions>
</quest-definition>'''
        migrated, counts, no_strict_match, failures = m.migrate_blocks_in_xml(xml, "comment.xml")
        self.assertEqual([], failures)
        self.assertEqual({}, counts)
        self.assertIn("<!-- this boundary must survive -->", migrated)
        self.assertEqual(1, len(no_strict_match))
        self.assertIn("comment", no_strict_match[0]["reason"])

    def test_unmatched_ordinary_transition_is_reported_without_rewrite(self):
        xml = '''<quest-definition id="1" version="1">
  <metadata name="x" display-name-id="1" min-level="0" max-level="1" category="QUEST"/>
  <nodes><node label="a" status="START"/></nodes>
  <transitions><transition source="a" target="a"><event><level-up/></event></transition></transitions>
</quest-definition>'''
        migrated, counts, no_strict_match, failures = m.migrate_blocks_in_xml(xml, "ordinary.xml")
        self.assertEqual([], failures)
        self.assertEqual({}, counts)
        self.assertEqual(xml, migrated)
        self.assertEqual(1, len(no_strict_match))
        self.assertEqual("ordinary.xml", no_strict_match[0]["file"])
        self.assertEqual("ordinary-transition-run", no_strict_match[0]["category"])

    def test_event_dispatch_keeps_fixed_priority_and_does_not_consume_other_events(self):
        transitions = m.parse_xml('''<transitions>
          <transition source="a" target="a"><event><talk-to-npc npc-id="1" dialog-id="31"/></event><after-commit><close-dialog/></after-commit></transition>
          <transition source="a" target="a"><event><talk-to-npc npc-id="2" dialog-id="31"/></event><after-commit><close-dialog/></after-commit></transition>
          <transition source="a" target="a"><event><level-up/></event></transition>
        </transitions>''')
        replacements, unmatched = m.match_domain_blocks(transitions)
        self.assertEqual([(0, 1, "npc-dialog")],
                         [(start, end, name) for start, end, name, _ in replacements])
        self.assertEqual([(2, 2)], [(start, end) for start, end, _ in unmatched])

    def test_counter_grid_matcher_uses_projection_index_for_large_products(self):
        nodes = []
        transitions = []
        for value0 in range(32):
            for value1 in range(16):
                nodes.append(f'<node label="n{value0}_{value1}" status="START"><var name="var0" value="{value0}"/><var name="var1" value="{value1}"/></node>')
        for value0 in range(31):
            for value1 in range(16):
                transitions.append(f'<transition source="n{value0}_{value1}" target="n{value0 + 1}_{value1}"><event><kill-npc npc-id="1"/></event><after-commit><sync-quest-state mode="PACKET_ONLY"/></after-commit></transition>')
        for value0 in range(32):
            for value1 in range(15):
                transitions.append(f'<transition source="n{value0}_{value1}" target="n{value0}_{value1 + 1}"><event><kill-npc npc-id="2"/></event><after-commit><sync-quest-state mode="PACKET_ONLY"/></after-commit></transition>')
        xml = '''<quest-definition id="1" version="1">
          <metadata name="x" display-name-id="1" min-level="0" max-level="1" category="QUEST"/>
          <progress><bit-field name="var0" offset="0" width="5" min="0" max="31" persistence="PERSISTENT" scope="LOCAL"/><bit-field name="var1" offset="5" width="4" min="0" max="15" persistence="PERSISTENT" scope="LOCAL"/></progress>
          <nodes>%s</nodes>
          <transitions>
            %s
          </transitions>
        </quest-definition>''' % ("".join(nodes), "\n            ".join(transitions))
        started = __import__("time").perf_counter()
        _, counts, no_strict_match, failures = m.migrate_blocks_in_xml(xml, "large-grid.xml")
        self.assertEqual({"counter-grid": 1}, counts)
        self.assertEqual([], no_strict_match)
        self.assertEqual([], failures)
        self.assertLess(__import__("time").perf_counter() - started, 1.0)


if __name__ == "__main__":
    unittest.main()
