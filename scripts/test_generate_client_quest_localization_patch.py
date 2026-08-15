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


if __name__ == "__main__":
    unittest.main()
