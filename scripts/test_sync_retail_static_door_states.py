import json
import tempfile
import unittest
from pathlib import Path

from sync_retail_static_door_states import synchronized_content


class SyncRetailStaticDoorStatesTest(unittest.TestCase):

    def test_only_proven_matching_door_state_is_changed(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "docs/RETAIL_STATIC_DOOR_SOURCE_MATRIX.json"
            target = root / "src/main/resources/aion/data/static_data/staticdoors/staticdoor_templates.xml"
            source.parent.mkdir(parents=True)
            target.parent.mkdir(parents=True)
            source.write_text(json.dumps({
                "version": 1,
                "provenance": {"kind": "RETAIL_SOURCE_MATRIX", "authoritative_retail_evidence": True},
                "worlds": [{
                    "world_id": 300040000,
                    "source": "Worlds/idlf1/world.xml",
                    "duplicate_editor_ids": [],
                    "doors": [
                        {
                            "editor_id": 33, "x": "1", "y": "2", "z": "3",
                            "opened": False, "clickable": True, "closeable": False, "status": "PROVEN",
                            "fields": {"id": "5"},
                        },
                        {
                            "editor_id": 35, "x": "7", "y": "8", "z": "9",
                            "opened": False, "clickable": False, "closeable": False, "status": "PROVEN",
                            "fields": {"id": "0"},
                        },
                    ],
                }],
            }), encoding="utf-8")
            original = ('<staticdoor_templates>\n\t<world world="300040000">\n'
                        '\t\t<!-- <staticdoor doorid="33" x="1" y="2" z="3"/> -->\n'
                        '\t\t<staticdoor doorid="33" state="0x8" x="1" y="2" z="3" mesh="door.cga"/>\n'
                        '\t\t<staticdoor doorid="34" state="0x1" x="4" y="5" z="6" mesh="other.cga"/>\n'
                        '\t\t<staticdoor doorid="35" retailid="7" state="0x1" x="7" y="8" z="9" mesh="closed.cga"/>\n'
                        '\t</world>\n</staticdoor_templates>\n')
            target.write_text(original, encoding="utf-8")

            content, count = synchronized_content(root, {300040000})

            self.assertEqual(2, count)
            self.assertIn('<staticdoor doorid="33" retailid="5" x="1" y="2" z="3" mesh="door.cga" state="0xa"/>', content)
            self.assertIn('<staticdoor doorid="34" state="0x1" x="4" y="5" z="6" mesh="other.cga"/>', content)
            self.assertIn('<staticdoor doorid="35" x="7" y="8" z="9" mesh="closed.cga"/>', content)


if __name__ == "__main__":
    unittest.main()
