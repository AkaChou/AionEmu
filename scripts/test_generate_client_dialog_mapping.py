#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import sys
import tempfile
import unittest
from unittest import mock
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import generate_client_dialog_mapping as mapping


class GenerateClientDialogMappingTest(unittest.TestCase):
    def test_parse_args_accepts_check_without_changing_default_output(self) -> None:
        with mock.patch.object(sys, "argv", ["generate_client_dialog_mapping.py", "--check"]):
            args = mapping.parse_args()

        self.assertTrue(args.check)
        self.assertEqual(Path("docs/quest/client-dialog-mapping"), args.output_dir)

    def test_stale_outputs_reports_changed_and_missing_files(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            generated = root / "generated"
            expected = root / "expected"
            generated.mkdir()
            expected.mkdir()
            for name in mapping.OUTPUT_FILES:
                (generated / name).write_bytes(b"same")
                (expected / name).write_bytes(b"same")
            (expected / mapping.OUTPUT_FILES[0]).write_bytes(b"changed")
            (expected / mapping.OUTPUT_FILES[1]).unlink()

            stale = mapping.stale_outputs(generated, expected)

        self.assertEqual(list(mapping.OUTPUT_FILES[:2]), stale)

    def test_records_pages_without_actions_and_preserves_page_order(self) -> None:
        source = """<HtmlPages>
  <HtmlPage name="select1">
    <Act href="HACTION_SELECT1_1">Continue.</Act>
  </HtmlPage>
  <HtmlPage name="select1_1">
    <p>Terminal response.</p>
  </HtmlPage>
</HtmlPages>
"""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "QUEST_Q90001.html"
            path.write_text(source, encoding="utf-8")

            pages, actions, diagnostic = mapping.parse_quest_file(path, root)

        digest = hashlib.sha256(source.encode("utf-8")).hexdigest()
        self.assertEqual("", diagnostic)
        self.assertEqual(["select1", "select1_1"], [page.html_page_name for page in pages])
        self.assertEqual([1, 2], [page.page_order for page in pages])
        self.assertEqual([1, 0], [page.action_count for page in pages])
        self.assertEqual([digest, digest], [page.source_sha256 for page in pages])
        self.assertEqual(1, len(actions))
        self.assertEqual(1, actions[0].page_order)
        self.assertEqual("HACTION_SELECT1_1", actions[0].action_constant)

    def test_active_manifest_is_stable_and_excludes_unused_files(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "QUEST_Q2.html").write_text("second", encoding="utf-8")
            (root / "QUEST_Q1.html").write_text("first", encoding="utf-8")
            unused = root / "unused"
            unused.mkdir()
            (unused / "QUEST_Q3.html").write_text("ignored", encoding="utf-8")

            count, digest = mapping.active_quest_html_manifest(root)

        expected = hashlib.sha256(
            (
                "quest_q1.html\0"
                + hashlib.sha256(b"first").hexdigest()
                + "\nquest_q2.html\0"
                + hashlib.sha256(b"second").hexdigest()
                + "\n"
            ).encode("utf-8")
        ).hexdigest()
        self.assertEqual(2, count)
        self.assertEqual(expected, digest)


if __name__ == "__main__":
    unittest.main()
