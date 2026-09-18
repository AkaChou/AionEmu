"""前置审计解析回归。 / Prerequisite audit parser regression tests."""
import unittest
import xml.etree.ElementTree as ET
from audit_prerequisite_axis import canonical, retail_contract, production_contract


class PrerequisiteAuditTest(unittest.TestCase):
    def test_preserves_or_between_fields_and_and_within_field(self):
        q = ET.fromstring("<quest><finished_quest_cond1>Q1,Q2</finished_quest_cond1>"
                          "<finished_quest_cond2>Q3</finished_quest_cond2></quest>")
        groups, unresolved = retail_contract(q, {"q1": 1, "q2": 2, "q3": 3})
        self.assertEqual([], unresolved)
        self.assertEqual([[('finished', 1, 0), ('finished', 2, 0)], [('finished', 3, 0)]], groups)

    def test_noncontiguous_exclusions_are_common_to_each_branch(self):
        q = ET.fromstring("<quest><finished_quest_cond1>Q1</finished_quest_cond1>"
                          "<finished_quest_cond2>Q2</finished_quest_cond2>"
                          "<unfinished_quest_cond3>Q3</unfinished_quest_cond3></quest>")
        groups, _ = retail_contract(q, {"q1": 1, "q2": 2, "q3": 3})
        self.assertEqual(2, len(groups))
        self.assertTrue(all(('unfinished', 3, 0) in group for group in groups))

    def test_names_and_one_based_reward_outcomes(self):
        q = ET.fromstring("<quest><finished_quest_cond1>ws_q5015:2</finished_quest_cond1></quest>")
        self.assertEqual(([[('finished', 5015, 1)]], []), retail_contract(q, {"ws_q5015": 5015}))

    def test_unresolved_names_are_reported(self):
        q = ET.fromstring("<quest><finished_quest_cond1>missing</finished_quest_cond1></quest>")
        self.assertEqual(['missing'], retail_contract(q, {})[1])

    def test_production_global_prerequisites_conjoin_each_branch(self):
        meta = ET.fromstring('<metadata><prerequisites><quest id="1"/></prerequisites>'
                            '<start-condition-groups><group><condition type="finished" quest-id="2"/>'
                            '</group><group><condition type="finished" quest-id="3" reward-mode="1"/>'
                            '</group></start-condition-groups></metadata>')
        self.assertEqual([[('finished', 1, 0), ('finished', 2, 0)],
                          [('finished', 1, 0), ('finished', 3, 1)]], production_contract(meta))

    def test_absorption_preserves_alternative_branches(self):
        a, b = ('finished', 1, 0), ('finished', 2, 0)
        self.assertEqual([[a]], canonical([[a], [a, b], [a]]))
        self.assertEqual([[a], [b]], canonical([[a], [b]]))


if __name__ == '__main__':
    unittest.main()
