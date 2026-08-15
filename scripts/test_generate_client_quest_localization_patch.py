#!/usr/bin/env python3
from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import generate_client_quest_localization_patch as localization


class GenerateClientQuestLocalizationPatchTest(unittest.TestCase):
    def test_generates_fixed_overrides_without_mutating_sources(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source"
            output = root / "output"
            source.mkdir()
            grouped: dict[str, list[localization.DictionaryPatch]] = {}
            for patch in localization.PATCHES:
                grouped.setdefault(patch.filename, []).append(patch)
            originals = {}
            for filename, patches in grouped.items():
                text = "<strings>\n" + "".join(
                    f"  <string><name>{patch.entry_name}</name><body>{patch.broken_body}</body></string>\n"
                    for patch in patches
                ) + "</strings>\n"
                path = source / filename
                path.write_text(text, encoding="utf-8")
                originals[filename] = text

            outputs = localization.generate_patch(source, output)

            self.assertEqual(sorted(grouped), sorted(path.name for path in outputs))
            for filename, patches in grouped.items():
                self.assertEqual(originals[filename], (source / filename).read_text(encoding="utf-8"))
                fixed = localization.dictionary_bodies(output / filename)
                for patch in patches:
                    self.assertEqual(patch.fixed_body, fixed[patch.entry_name])

    def test_rejects_unknown_source_text(self) -> None:
        patch = localization.PATCHES[0]
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / patch.filename
            source.write_text(
                f"<strings><string><name>{patch.entry_name}</name><body>changed</body></string></strings>",
                encoding="utf-8",
            )

            with self.assertRaisesRegex(ValueError, "unexpected body"):
                localization.patch_text(source, [patch])

    def test_generates_fixed_dialog_overrides_without_mutating_sources(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "Dialogs"
            output = root / "output"
            originals = {}
            paths = {}
            source.mkdir()
            for index, patch in enumerate(localization.DIALOG_REFERENCE_PATCHES):
                filename = f"{'QUEST_Q' if index % 2 else 'quest_q'}{900000 + index}.html"
                path = source / filename
                body = patch.broken_token * patch.expected_occurrences
                raw = (b"\xef\xbb\xbf" if index == 0 else b"") + body.encode("utf-8")
                path.write_bytes(raw)
                originals[path] = raw
                paths[patch] = path

            outputs = localization.generate_dialog_patch(source, output)

            self.assertEqual(len(localization.DIALOG_REFERENCE_PATCHES), len(outputs))
            for patch, source_path in paths.items():
                self.assertEqual(originals[source_path], source_path.read_bytes())
                fixed = (output / source_path.name).read_text(encoding="utf-8-sig")
                self.assertNotIn(patch.broken_token, fixed)
                self.assertEqual(patch.expected_occurrences, fixed.count(patch.replacement))
            first_patch_path = output / paths[localization.DIALOG_REFERENCE_PATCHES[0]].name
            self.assertTrue(first_patch_path.read_bytes().startswith(b"\xef\xbb\xbf"))

    def test_rejects_unexpected_dialog_reference_count(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "Dialogs"
            source.mkdir()
            for index, patch in enumerate(localization.DIALOG_REFERENCE_PATCHES):
                count = patch.expected_occurrences - (1 if index == 0 else 0)
                path = source / f"quest_q{900000 + index}.html"
                path.write_text(patch.broken_token * count, encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "unexpected reference count"):
                localization.generate_dialog_patch(source, Path(directory) / "output")

    def test_validates_all_task_dialog_dictionary_references(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            strings = root / "source" / "Strings"
            dialogs = root / "source" / "Dialogs"
            string_output = root / "output" / "Strings"
            dialog_output = root / "output" / "Dialogs"
            strings.mkdir(parents=True)
            dialogs.mkdir(parents=True)
            (strings / "client_strings_dic_people.xml").write_text(
                "<strings><string><name>STR_DIC_OK</name>"
                "<body>Display name;Description</body></string></strings>",
                encoding="utf-8",
            )
            (dialogs / "QUEST_Q900000.html").write_text(
                "<p>[%dic:STR_DIC_OK]</p>", encoding="utf-8"
            )

            result = localization.validate_task_dialogs(
                strings, string_output, dialogs, dialog_output
            )

            self.assertEqual((1, 1), result)

    def test_rejects_referenced_dictionary_without_display_separator(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            strings = root / "Strings"
            dialogs = root / "Dialogs"
            strings.mkdir()
            dialogs.mkdir()
            (strings / "client_strings_dic_people.xml").write_text(
                "<strings><string><name>STR_DIC_BROKEN</name>"
                "<body>Description only</body></string></strings>",
                encoding="utf-8",
            )
            (dialogs / "quest_q900000.html").write_text(
                "<p>[%dic:STR_DIC_BROKEN]</p>", encoding="utf-8"
            )

            with self.assertRaisesRegex(ValueError, "missing display separator"):
                localization.validate_task_dialogs(
                    strings,
                    root / "output-strings",
                    dialogs,
                    root / "output-dialogs",
                )


if __name__ == "__main__":
    unittest.main()
