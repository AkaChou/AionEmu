#!/usr/bin/env python3
from __future__ import annotations

import csv
import io
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import extract_legacy_quest_dialog_contracts as contracts


class ExtractLegacyQuestDialogContractsTest(unittest.TestCase):
    def test_distinguishes_monster_hunt_and_data_driven_report_states(self) -> None:
        source = b"""<?xml version="1.0" encoding="UTF-8"?>
<quest_scripts>
  <monster_hunt id="25512" retail="true" start_npc_ids="806105">
    <monster var="0" end_var="30" npc_ids="237450"/>
  </monster_hunt>
  <data_driven_quest id="1118" retail="true" start_type="TALK"
                     start_ids="203059" end_npc_ids="203079">
    <step type="TALK" ids="203070"/>
  </data_driven_quest>
</quest_scripts>
"""

        rows = contracts.extract_contracts(source, "history", "quests.xml", "object-id")

        data_driven, monster_hunt = rows
        self.assertEqual(1118, data_driven.quest_id)
        self.assertEqual(("4762", "SELECT_NONE"), (data_driven.start_page_id, data_driven.start_page))
        self.assertEqual(("REWARD", "REWARD"),
                         (data_driven.report_source_status, data_driven.report_target_status))
        self.assertEqual(("10002", "DEFAULT_SUCCESS"),
                         (data_driven.report_page_id, data_driven.report_page))

        self.assertEqual(25512, monster_hunt.quest_id)
        self.assertEqual(("1011", "SELECT1"), (monster_hunt.start_page_id, monster_hunt.start_page))
        self.assertEqual(("START", "REWARD"),
                         (monster_hunt.report_source_status, monster_hunt.report_target_status))
        self.assertEqual(("1352", "SELECT2"),
                         (monster_hunt.report_page_id, monster_hunt.report_page))
        self.assertEqual("806105", monster_hunt.end_npc_ids)

    def test_render_is_utf8_bom_csv_with_source_identity(self) -> None:
        source = b"<quest_scripts><monster_hunt id=\"1\" start_npc_ids=\"2\"><monster/></monster_hunt></quest_scripts>"
        extracted = contracts.extract_contracts(source, "history", "quests.xml", "object-id")

        rendered = contracts.render(extracted)
        self.assertTrue(rendered.startswith(b"\xef\xbb\xbf"))
        rows = list(csv.DictReader(io.StringIO(rendered.decode("utf-8-sig"))))
        self.assertEqual("object-id", rows[0]["source_git_object"])
        self.assertEqual("history", rows[0]["source_revision"])
        self.assertEqual(64, len(rows[0]["source_sha256"]))

    def test_extracts_report_to_collecting_and_kill_in_world_protocols(self) -> None:
        source = b"""<quest_scripts>
  <report_to id="1" start_npc_ids="101" end_npc_ids="102"/>
  <item_collecting id="2" start_npc_ids="201" end_npc_ids="202"/>
  <kill_in_world id="3" start_npc_ids="301" end_npc_ids="302" reward_dialog_id="10002"/>
</quest_scripts>"""

        report, collecting, kill = contracts.extract_contracts(
            source, "history", "quests.xml", "object-id"
        )

        self.assertEqual(("1011", "2375", "1009", "START", "REWARD"),
                         (report.start_page_id, report.report_page_id, report.report_action_id,
                          report.report_source_status, report.report_target_status))
        self.assertEqual(("1011", "2375", "39", "CHECK_COLLECTED_ITEMS"),
                         (collecting.start_page_id, collecting.report_page_id,
                          collecting.report_action_id, collecting.report_action))
        self.assertEqual(("4762", "10002", "1009", "REWARD", "REWARD"),
                         (kill.start_page_id, kill.report_page_id, kill.report_action_id,
                          kill.report_source_status, kill.report_target_status))

    def test_extracts_report_to_many_after_the_last_intermediate_step(self) -> None:
        source = b"""<quest_scripts>
  <report_to_many id="1913" start_npc_ids="203758" end_npc_ids="203097">
    <npc_infos var="0" npc_id="203726" quest_dialog="1352"/>
  </report_to_many>
</quest_scripts>"""

        [contract] = contracts.extract_contracts(source, "history", "quests.xml", "object-id")

        self.assertEqual("FULL", contract.contract_scope)
        self.assertEqual(("203758", "203097"),
                         (contract.start_npc_ids, contract.end_npc_ids))
        self.assertEqual(("1011", "SELECT1"),
                         (contract.start_page_id, contract.start_page))
        self.assertEqual(("2375", "SELECT5"),
                         (contract.report_page_id, contract.report_page))
        self.assertEqual(("REWARD", "1009", "REWARD"),
                         (contract.report_source_status, contract.report_action_id,
                          contract.report_target_status))
        self.assertEqual(("0", "203726", "1352", "SELECT2", "10000", "SETPRO1"),
                         (contract.progress_vars, contract.progress_npc_ids,
                          contract.progress_page_ids, contract.progress_pages,
                          contract.progress_action_ids, contract.progress_actions))

    def test_keeps_nonsequential_report_to_many_out_of_the_full_contract_set(self) -> None:
        source = b"""<quest_scripts>
  <report_to_many id="18830" start_npc_ids="1" end_npc_ids="2">
    <npc_infos var="0" npc_id="10" quest_dialog="1352"/>
    <npc_infos var="0" npc_id="11" quest_dialog="1352"/>
  </report_to_many>
</quest_scripts>"""

        [contract] = contracts.extract_contracts(source, "history", "quests.xml", "object-id")

        self.assertEqual("PARTIAL", contract.contract_scope)
        self.assertIn("not one contiguous unique-NPC sequence", contract.unresolved_reason)

    def test_extracts_item_order_item_start_progress_and_report_contract(self) -> None:
        source = b"""<quest_scripts>
  <item_order id="1514" start_item_id="182201710" end_npc_id="203831"
              talk_npc_id1="204582" talk_npc_id2="204505"/>
</quest_scripts>"""

        [contract] = contracts.extract_contracts(source, "history", "quests.xml", "object-id")

        self.assertEqual("FULL", contract.contract_scope)
        self.assertEqual("", contract.start_npc_ids)
        self.assertEqual("203831", contract.end_npc_ids)
        self.assertEqual(("2375", "SELECT5", "START", "1009", "REWARD"),
                         (contract.report_page_id, contract.report_page,
                          contract.report_source_status, contract.report_action_id,
                          contract.report_target_status))
        self.assertEqual(("0 1", "204582 204505", "1352 1352", "10000 10000"),
                         (contract.progress_vars, contract.progress_npc_ids,
                          contract.progress_page_ids, contract.progress_action_ids))


if __name__ == "__main__":
    unittest.main()
