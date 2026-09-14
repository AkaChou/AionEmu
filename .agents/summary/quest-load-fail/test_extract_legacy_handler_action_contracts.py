#!/usr/bin/env python3
from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import extract_legacy_handler_action_contracts as contracts


class ExtractLegacyHandlerActionContractsTest(unittest.TestCase):
    def test_extracts_status_npc_page_and_effects_from_reward_branch(self) -> None:
        source = """if (qs.getStatus() == QuestStatus.START) {
    if (targetId == 204141) {
        switch (env.getDialog()) {
            case STEP_TO_11:
                changeQuestStep(env, 0, 0, true);
                return sendQuestDialog(env, 5);
        }
    }
} else if (qs.getStatus() == QuestStatus.REWARD) {
    return sendQuestEndDialog(env);
}
"""
        lines = source.splitlines()
        starts = contracts.branch_ranges(lines, "STEP_TO_11", 10010)

        self.assertEqual(1, len(starts))
        branch = contracts.extract_branch(lines, *starts[0])

        self.assertEqual("START", branch.status)
        self.assertEqual(204141, branch.npc_id)
        self.assertEqual(5, branch.response_page)
        self.assertEqual("PAGE", branch.response_kind)
        self.assertIn("changeQuestStep", branch.effects)

    def test_distinguishes_active_stale_and_close_responses(self) -> None:
        ready = contracts.HandlerBranch("START", 1, 5, "PAGE", "", "a")
        stale = contracts.HandlerBranch("START", 1, 2716, "PAGE", "", "b")
        close = contracts.HandlerBranch("REWARD", 1, 0, "CLOSE", "", "c")

        self.assertEqual(("READY", ""), contracts.classify([ready], "START", {5}))
        status, reason = contracts.classify([stale], "START", {5})
        self.assertEqual("STALE_PAGE", status)
        self.assertIn("2716", reason)
        self.assertEqual(("READY", ""), contracts.classify([close], "REWARD", {5}))

    def test_rejects_conflicting_handler_branches(self) -> None:
        first = contracts.HandlerBranch("START", 1, 5, "PAGE", "", "a")
        second = contracts.HandlerBranch("START", 1, 6, "PAGE", "", "b")

        status, reason = contracts.classify([first, second], "START", {5, 6})

        self.assertEqual("AMBIGUOUS", status)
        self.assertIn("disagree", reason)

    def test_reads_action_names_from_quest_dialog_enum(self) -> None:
        actions = contracts.dialog_actions(Path(__file__).resolve().parents[3])

        self.assertEqual("STEP_TO_11", actions[10010])
        self.assertEqual("STEP_TO_12", actions[10011])
        self.assertEqual("SELECT_REWARD", actions[1009])


if __name__ == "__main__":
    unittest.main()
