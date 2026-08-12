#!/usr/bin/env python3
from __future__ import annotations

import sys
import unittest
import xml.etree.ElementTree as ElementTree
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parent))

import migrate_quest_dialog_xml as migration


ACTIONS = {
    -1: "USE_OBJECT",
    8: "SELECTED_QUEST_REWARD1",
    9: "SELECTED_QUEST_REWARD2",
    10: "SELECTED_QUEST_REWARD3",
    11: "SELECTED_QUEST_REWARD4",
    12: "SELECTED_QUEST_REWARD5",
    1009: "SELECT_QUEST_REWARD",
}


class MigrateQuestDialogXmlTest(unittest.TestCase):
	def test_rewrites_legacy_npc_complete_and_is_idempotent(self) -> None:
		source = """<quest-definition>
  <transitions>
    <npc-complete npc-id="203067" source="reward" target="complete" dialog-ids="8" preview-dialog-ids="-1 1009" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice dialog-id="9" reward-index="1"/>
      <fallback dialog-ids="10..12"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

		migrated = migration.migrate(source, ACTIONS, {})
		npc_complete = ElementTree.fromstring(migrated).find("./transitions/npc-complete")
		self.assertIsNotNone(npc_complete)
		assert npc_complete is not None
		self.assertEqual("SELECTED_QUEST_REWARD1", npc_complete.get("actions"))
		self.assertIsNone(npc_complete.get("dialog-ids"))
		self.assertEqual("USE_OBJECT SELECT_QUEST_REWARD", npc_complete.find("preview").get("actions"))
		self.assertEqual("SELECTED_QUEST_REWARD2", npc_complete.find("choice").get("action"))
		self.assertEqual("SELECTED_QUEST_REWARD3..SELECTED_QUEST_REWARD5", npc_complete.find("fallback").get("actions"))
		self.assertEqual(migrated, migration.migrate(migrated, ACTIONS, {}))

	def test_rejects_mixed_legacy_and_typed_npc_complete_attributes(self) -> None:
		for legacy, typed, expected in (
			('dialog-ids="8" actions="SELECTED_QUEST_REWARD1"', "", "npc-complete must declare dialog-ids or actions, not both"),
			('preview-dialog-ids="-1"', '<preview actions="USE_OBJECT"/>', "npc-complete must declare preview-dialog-ids or preview, not both"),
			('', '<choice dialog-id="9" action="SELECTED_QUEST_REWARD2" reward-index="1"/>', "choice must declare dialog-id or action/actions, not both"),
			('', '<fallback dialog-ids="10" actions="SELECTED_QUEST_REWARD3"/>', "fallback must declare dialog-ids or action/actions, not both"),
		):
			with self.subTest(expected=expected):
				source = f"""<quest-definition>
  <transitions>
    <npc-complete npc-id="203067" source="reward" target="complete" {legacy} complete-reward-index="0" finish="SELECTION_DIALOG">
      {typed}
      <choice action="SELECTED_QUEST_REWARD2" reward-index="1"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""
				with self.assertRaisesRegex(ValueError, expected):
					migration.migrate(source, ACTIONS, {})


if __name__ == "__main__":
	unittest.main()
