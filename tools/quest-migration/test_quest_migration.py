from __future__ import annotations

import base64
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import quest_migration as qm


class QuestMigrationTest(unittest.TestCase):

    def test_real_compiler_gate_results_are_member_scoped(self) -> None:
        members = {"000000.xml": "Q1.java", "000001.xml": "Q2.java", "000002.xml": "Q3.java"}
        failure = base64.b64encode(b"Quest 2 has invalid action phase order").decode("ascii")

        failures = qm.parse_real_compiler_results(
            f"OK\t000000.xml\t1\t3\nFAIL\t000001.xml\t{failure}\n",
            members,
        )

        self.assertEqual({
            "Q2.java": "JAVA_COMPILER_REJECTED:Quest 2 has invalid action phase order",
            "Q3.java": "JAVA_COMPILER_REJECTED:missing member result",
        }, failures)

    def config(self, root: Path, dialogs: Path | None = None) -> qm.Config:
        return qm.Config(
            project_root=root,
            java_root=root / "src/main/java",
            java_handler_root=root / "src/main/java/com/aionemu/gameserver/quest/handlers",
            xml_handler_root=root / "src/main/resources/quest_script_data",
            dialogs_root=dialogs or root / "dialogs",
            report_dir=root / "docs/quest/audit/reports",
            check=False,
        )

    def test_java_ast_inventory_extracts_handler_shape(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "src/main/java/com/aionemu/gameserver/quest/handlers/demo/_1234.java"
            source.parent.mkdir(parents=True)
            source.write_text(
                """
                package com.aionemu.gameserver.quest.handlers.demo;
                public class _1234 extends QuestHandler {
                    private static final int questId = 1234;
                    public _1234() { super(questId); }
                    @Override public void register() {
                        qe.registerQuestNpc(700001).addOnTalkEvent(questId);
                    }
                    @Override public boolean onDialogEvent(Object env) {
                        QuestState qs = null;
                        qs.setStatus(null);
                        switch (env.getDialog()) {
                            case ACCEPT_QUEST: return true;
                            default: break;
                        }
                        if (env.getDialog() == QuestDialog.SELECT_REWARD) return true;
                        if (env != null) {
                            boolean changed = changeQuestStep(null, 0, 1, false);
                            if (!giveQuestItem(env, 182400001, 3)) return false;
                            removeQuestItem(env, 182400002, 1);
                            return true;
                        }
                        return false;
                    }
                }
                """,
                encoding="utf-8",
            )
            rows = qm.run_java_inventory(self.config(root))
            self.assertEqual(1, len(rows))
            self.assertTrue(rows[0]["handler_candidate"])
            self.assertEqual([1234], rows[0]["quest_ids"])
            self.assertIn("onDialogEvent", rows[0]["overrides"])
            self.assertEqual(3, rows[0]["controls"]["IF"])
            self.assertEqual({}, rows[0]["control_methods"])
            self.assertIn("addOnTalkEvent", {call["method"] for call in rows[0]["calls"]})
            status_call = next(call for call in rows[0]["calls"] if call["method"] == "setStatus")
            self.assertEqual(["QuestState"], status_call["receiver_types"])
            npc_call = next(call for call in rows[0]["calls"] if call["method"] == "registerQuestNpc")
            self.assertEqual([700001], npc_call["argument_values"])
            self.assertIn(
                {"kind": "SWITCH_CASE", "value": "ACCEPT_QUEST", "line": 13},
                rows[0]["dialog_branches"],
            )
            self.assertIn(
                {"kind": "ENUM_REFERENCE", "value": "SELECT_REWARD", "line": 16},
                rows[0]["dialog_branches"],
            )
            step_call = next(call for call in rows[0]["calls"] if call["method"] == "changeQuestStep")
            self.assertEqual(
                ["IF_TRUE:(env != null)"],
                step_call["control_path"],
            )
            self.assertEqual("VARIABLE_INITIALIZER", step_call["result_usage"]);
            give_call = next(call for call in rows[0]["calls"] if call["method"] == "giveQuestItem")
            self.assertEqual("CONDITION", give_call["result_usage"])
            self.assertEqual("(!giveQuestItem(env, 182400001, 3))", give_call["result_context"])
            self.assertEqual("RETURN_FALSE", give_call["condition_true_outcome"])
            self.assertEqual("FALLTHROUGH", give_call["condition_false_outcome"])
            remove_call = next(call for call in rows[0]["calls"] if call["method"] == "removeQuestItem")
            self.assertEqual("IGNORED", remove_call["result_usage"])
            self.assertEqual("NONE", remove_call["condition_true_outcome"])
            self.assertGreater(remove_call["source_position"], give_call["source_position"]);

    def test_java_ast_inventory_marks_dynamic_quest_id(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "src/main/java/com/aionemu/gameserver/quest/handlers/demo/Dynamic.java"
            source.parent.mkdir(parents=True)
            source.write_text(
                """
                package com.aionemu.gameserver.quest.handlers.demo;
                public class Dynamic extends QuestHandler {
                    public Dynamic() { super(resolveQuestId()); }
                    private static int resolveQuestId() { return 1; }
                }
                """,
                encoding="utf-8",
            )
            rows = qm.run_java_inventory(self.config(root))
            self.assertEqual(1, len(rows))
            self.assertTrue(rows[0]["dynamic_quest_id"])
            self.assertEqual([], rows[0]["quest_ids"])

    def test_java_ast_inventory_normalizes_registration_loop_and_routing_aliases(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "src/main/java/com/aionemu/gameserver/quest/handlers/demo/_1234.java"
            source.parent.mkdir(parents=True)
            source.write_text(
                """
                package com.aionemu.gameserver.quest.handlers.demo;
                public class _1234 extends QuestHandler {
                    private static final int questId = 1234;
                    public _1234() { super(questId); }
                    public void register() {
                        int[] npcs = {700001, 700002};
                        for (int npc : npcs) qe.registerQuestNpc(npc).addOnTalkEvent(questId);
                    }
                    public boolean onDialogEvent(QuestEnv env) {
                        int targetId = env.getTargetId();
                        QuestDialog dialog = env.getDialog();
                        int var = qs.getQuestVarById(0);
                        if (targetId == 700001 && dialog == QuestDialog.START_DIALOG) {
                            qs.setQuestVarById(0, var + 1);
                            return sendQuestDialog(env, getQuestId());
                        }
                        if (targetId == 700002 && dialog == QuestDialog.USE_OBJECT) return true;
                        return false;
                    }
                }
                """,
                encoding="utf-8",
            )

            row = qm.run_java_inventory(self.config(root))[0]

            self.assertEqual(["register"], row["control_methods"]["ENHANCED_FOR"])
            registration = next(call for call in row["calls"] if call["method"] == "registerQuestNpc")
            self.assertEqual([[700001, 700002]], registration["argument_value_sets"])
            dialog = next(call for call in row["calls"] if call["method"] == "sendQuestDialog")
            self.assertIn("env.getTargetId()", dialog["control_path"][0])
            self.assertIn("env.getDialog()", dialog["control_path"][0])
            self.assertEqual(1234, dialog["argument_values"][1])
            mutation = next(call for call in row["calls"] if call["method"] == "setQuestVarById")
            self.assertEqual("(qs.getQuestVarById(0)) + 1", mutation["arguments"][1])
            returned = next(value for value in row["boolean_returns"] if value["value"])
            self.assertIn("env.getTargetId()", returned["control_path"][0])
            self.assertIn("env.getDialog()", returned["control_path"][0])

    def test_content_addressed_cache_hits_and_invalidates(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            config = self.config(Path(directory))
            calls = []

            def build() -> dict[str, int]:
                calls.append(len(calls) + 1)
                return {"build": calls[-1]}

            first = qm.cached_json(config, "fixture", "a" * 64, build)
            hit = qm.cached_json(config, "fixture", "a" * 64, build)
            invalidated = qm.cached_json(config, "fixture", "b" * 64, build)

            self.assertEqual(first, hit)
            self.assertEqual({"build": 2}, invalidated)
            self.assertEqual([1, 2], calls)

    def test_java_ast_inventory_preserves_enhanced_for_receiver_type(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "src/main/java/demo/QuestListWriter.java"
            source.parent.mkdir(parents=True)
            source.write_text(
                """
                package demo;
                public class QuestListWriter {
                    public void write(QuestState[] states) {
                        for (QuestState state : states) state.getStatus();
                    }
                }
                """,
                encoding="utf-8",
            )

            row = qm.run_java_inventory(self.config(root))[0]
            status = next(call for call in row["calls"] if call["method"] == "getStatus")

            self.assertEqual(["QuestState"], status["receiver_types"])

    def test_verify_reads_existing_outputs_without_rebuilding_inventory(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            config = self.config(Path(directory))
            with patch.object(qm, "collect_inventory", side_effect=AssertionError("inventory rebuilt")), \
                    patch.object(qm, "verify_dialog_standard_outputs", return_value=[]) as verify:
                outputs, blockers = qm.command_outputs("verify", config)

            self.assertEqual({}, outputs)
            self.assertEqual([], blockers)
            verify.assert_called_once_with(config)

    def test_dialog_parser_expands_known_html_entities(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "QUEST_Q1234.html"
            path.write_text(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <HtmlPages><HtmlPage name="select1"><Selects>
                <Act href="PLAYEMOTION_yes;HACTION_SELECT1_1">&ldquo;Go&rdquo;</Act>
                </Selects></HtmlPage><HtmlPage name="select1_1"/></HtmlPages>
                """.lstrip(),
                encoding="utf-8",
            )
            parsed = qm.parse_dialog(path, root, 1234)
            self.assertEqual("\u201cGo\u201d", parsed["actions"][0]["text"])

    def test_dialog_parser_recovers_only_opaque_cdata_contents(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "QUEST_Q1234.html"
            path.write_text(
                """
                <HtmlPages><HtmlPage name="select1">
                <Contents cdata="true"><html><body><p>broken</body></html></Contents>
                <Selects><Act href="HACTION_SELECT1_1">Go</Act></Selects>
                </HtmlPage></HtmlPages>
                """.strip(),
                encoding="utf-8",
            )
            parsed = qm.parse_dialog(path, root, 1234)
            self.assertIn("OPAQUE_CDATA_CONTENT_RECOVERY", parsed["normalizations"])
            self.assertEqual("HACTION_SELECT1_1", parsed["actions"][0]["href"])

    def test_dialog_inventory_reports_conflicts_and_parse_errors(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            dialogs = root / "dialogs"
            (dialogs / "a").mkdir(parents=True)
            (dialogs / "b").mkdir(parents=True)
            (dialogs / "a/QUEST_Q1.html").write_text(
                "<HtmlPages><HtmlPage name=\"select1\"/></HtmlPages>", encoding="utf-8")
            (dialogs / "b/quest_q1.html").write_text(
                "<HtmlPages><HtmlPage name=\"select2\"/></HtmlPages>", encoding="utf-8")
            (dialogs / "QUEST_Q2.html").write_text(
                "<HtmlPages><HtmlPage></HtmlPages>", encoding="utf-8")
            bundle = qm.collect_dialogs(self.config(root, dialogs))
            self.assertEqual(1, len(bundle.conflict_report["conflicts"]))
            self.assertEqual(1, len(bundle.conflict_report["parse_errors"]))
            self.assertEqual(
                ["DIALOG_PARSE_ERROR", "DIALOG_CONFLICT"],
                [blocker["kind"] for blocker in bundle.conflict_report["blockers"]],
            )

    def test_dialog_inventory_blocks_unknown_action(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            dialogs = root / "dialogs"
            dialogs.mkdir()
            (dialogs / "QUEST_Q1.html").write_text(
                '<HtmlPages><HtmlPage name="select1"><Selects><Act href="UNKNOWN">Go</Act></Selects>'
                '</HtmlPage></HtmlPages>',
                encoding="utf-8",
            )
            bundle = qm.collect_dialogs(self.config(root, dialogs))
            self.assertEqual([{"kind": "UNKNOWN_DIALOG_ACTION", "count": 1}],
                             bundle.action_report["blockers"])

    def test_shape_groups_repeated_handlers(self) -> None:
        handlers = []
        for quest_id in (1, 2):
            handlers.append({
                "quest_id": quest_id,
                "class": f"Q{quest_id}",
                "path": f"Q{quest_id}.java",
                "event_methods": ["onDialogEvent"],
                "register_calls": ["addOnTalkEvent/1", "registerQuestNpc/1"],
                "state_reads": ["getStatus/0"],
                "state_writes": ["changeQuestStep/4"],
                "helper_calls": ["changeQuestStep/4"],
                "service_calls": [],
                "direct_calls": ["getDialog/0", "hasNext/0", "iterator/0", "next/0"],
                "direct_call_sites": [{
                    "signature": "getDialog/0",
                    "method": "getDialog",
                    "select": "env.getDialog",
                    "enclosing_method": "onDialogEvent",
                    "line": 1,
                }],
                "reference_signatures": ["NPC:registerQuestNpc/1:0"],
                "controls": {"WHILE": 1},
                "control_methods": {"WHILE": ["register"]},
            })
        inventory = qm.InventoryBundle(
            manifest={},
            input_hash="0" * 64,
            java_rows=[],
            handler_report={"handlers": handlers},
            external_report={},
        )
        shapes = qm.build_shapes(inventory)
        self.assertEqual(1, shapes["counts"]["shapes"])
        self.assertEqual(2, shapes["counts"]["handlers_in_repeated_shapes"])
        self.assertEqual("REPEATED", shapes["shapes"][0]["mechanical_classification"])
        self.assertEqual("DIALOG", shapes["shapes"][0]["capability_family"])
        self.assertEqual(["BOUNDED_REGISTRATION_ITERATION"], shapes["shapes"][0]["mechanical_notes"])
        self.assertEqual([], shapes["shapes"][0]["outlier_reasons"])

    def test_dialog_shape_report_ranks_capability_gaps_by_handler_coverage(self) -> None:
        shapes = {
            **qm.base_report("0" * 64),
            "shapes": [{
                "fingerprint": "dialog-a",
                "capability_family": "DIALOG",
                "member_count": 3,
                "members": [{"quest_id": quest_id, "class": f"Q{quest_id}", "path": f"Q{quest_id}.java"}
                            for quest_id in (1, 2, 3)],
                "signature": {
                    "register_calls": ["addOnTalkEvent/1", "registerQuestNpc/1"],
                    "helper_calls": ["customThing/1", "sendQuestDialog/2", "sendQuestStartDialog/1"],
                    "state_reads": ["getQuestState/1", "getStatus/0"],
                    "state_writes": [],
                    "service_calls": [],
                    "direct_calls": ["getDialog/0", "getPlayer/0", "getTargetId/0", "super/1"],
                    "controls": {"IF": 1, "SWITCH": 1},
                },
            }],
        }

        report = qm.build_dialog_shape_capability_report(shapes)

        self.assertEqual(0, report["counts"]["generation_selected_shapes"])
        self.assertEqual(0, report["counts"]["generation_selected_handlers"])
        self.assertEqual(
            ["UNMAPPED_HELPER:customThing/1"],
            report["shapes"][0]["missing_capabilities"],
        )
        self.assertFalse(report["shapes"][0]["capability_ready"])
        self.assertEqual("NOT_CAPABILITY_READY", report["shapes"][0]["compiler_status"])
        self.assertEqual(3, report["missing_capability_ranking"][0]["handler_count"])

    def test_dialog_shape_report_selects_each_compiler_ready_member(self) -> None:
        def shape(fingerprint: str, quest_ids: tuple[int, ...]) -> dict[str, object]:
            return {
                "fingerprint": fingerprint,
                "capability_family": "DIALOG",
                "member_count": len(quest_ids),
                "members": [{"quest_id": quest_id, "class": f"Q{quest_id}", "path": f"Q{quest_id}.java"}
                            for quest_id in quest_ids],
                "signature": {
                    "register_calls": ["addOnTalkEvent/1", "registerQuestNpc/1"],
                    "helper_calls": ["sendQuestDialog/2", "sendQuestStartDialog/1"],
                    "state_reads": ["getQuestState/1", "getStatus/0"],
                    "state_writes": [],
                    "service_calls": [],
                    "direct_calls": ["getDialog/0", "getPlayer/0", "getTargetId/0", "super/1"],
                    "controls": {"IF": 1, "SWITCH": 1},
                },
            }

        shapes = {**qm.base_report("0" * 64), "shapes": [shape("ready", (1, 2, 3)), shape("partial", (4, 5))]}
        readiness = {
            "ready": {"ready_handlers": 3, "blocked_handlers": 0, "failures": []},
            "partial": {"ready_handlers": 1, "blocked_handlers": 1, "failures": [{
                "quest_id": 5,
                "path": "Q5.java",
                "family": "AMBIGUOUS_START_HELPER_GROUP",
                "error": "Ambiguous start helper group at Q5.java:1",
            }]},
        }

        report = qm.build_dialog_shape_capability_report(shapes, readiness)

        self.assertEqual(2, report["counts"]["generation_selected_shapes"])
        self.assertEqual(4, report["counts"]["generation_selected_handlers"])
        self.assertEqual(1, report["counts"]["handlers_selected_from_partial_shapes"])
        self.assertEqual(4, report["counts"]["compiler_ready_handlers"])
        self.assertEqual(1, report["counts"]["compiler_blocked_handlers"])
        self.assertEqual(["ready", "partial"], [row["fingerprint"] for row in report["selected_shapes"]])
        self.assertEqual("PARTIAL", report["shapes"][1]["compiler_status"])
        self.assertEqual([4], [member["quest_id"] for member in report["shapes"][1]["generation_members"]])
        self.assertEqual("AMBIGUOUS_START_HELPER_GROUP", report["compiler_failure_ranking"][0]["family"])

    def test_dialog_shape_report_distinguishes_start_with_initial_item(self) -> None:
        self.assertEqual("ACTION_STANDARD_START",
                         qm.dialog_signature_capability("helper", "sendQuestStartDialog/1"))
        self.assertEqual("ACTION_START_WITH_INITIAL_ITEM",
                         qm.dialog_signature_capability("helper", "sendQuestStartDialog/3"))

    def test_capability_dependency_graph_ranks_marginal_unlocks(self) -> None:
        report = {
            **qm.base_report("0" * 64),
            "counts": {
                "dialog_handlers": 10,
                "capability_ready_handlers": 4,
                "compiler_ready_handlers": 3,
                "generation_selected_handlers": 3,
            },
            "compiler_failure_ranking": [],
            "shapes": [
                {"fingerprint": "x", "member_count": 3, "missing_capabilities": ["X"]},
                {"fingerprint": "xy", "member_count": 2, "missing_capabilities": ["X", "Y"]},
                {"fingerprint": "y", "member_count": 1, "missing_capabilities": ["Y"]},
            ],
        }

        graph = qm.build_capability_dependency_graph(report)

        self.assertEqual(2, graph["counts"]["missing_capabilities"])
        self.assertEqual(3, graph["counts"]["dependency_sets"])
        self.assertEqual("X", graph["recommended_portfolio"][0]["capability"])
        self.assertEqual(3, graph["recommended_portfolio"][0]["projected_newly_capability_ready_handlers"])
        self.assertEqual(3, graph["recommended_portfolio"][1]["projected_newly_capability_ready_handlers"])
        self.assertEqual({"left": "X", "right": "Y", "handler_count": 2}, graph["edges"][0])

    def test_dialog_control_path_extracts_explicit_targets_dialogs_and_inventory(self) -> None:
        path = (
            "IF_TRUE:(targetId == 700001 || targetId == 700002)",
            "SWITCH:(env.getDialog())=ACCEPT_QUEST|ACCEPT_QUEST_SIMPLE",
            "IF_TRUE:(player.getInventory().getItemCountByItemId(164000335) >= 1)",
        )

        self.assertEqual([700001, 700002], qm.control_targets(path))
        self.assertEqual(["ACCEPT_QUEST", "ACCEPT_QUEST_SIMPLE"], qm.control_dialogs(path))
        self.assertEqual(
            {"tag": "player-inventory", "attrs": {
                "item_id": "164000335", "op": "GREATER_EQUAL", "count": "1",
            }},
            qm.inventory_condition(path),
        )
        self.assertEqual([700003], qm.control_targets(("IF_TRUE:(env.getTargetId() == 700003)",)))
        self.assertEqual(["SELECT_REWARD"],
                         qm.control_dialogs(("IF_TRUE:(env.getDialogId() == 1009)",)))
        self.assertEqual(["ASK_ACCEPTION"],
                         qm.control_dialogs(("IF_TRUE:(env.getDialogId() == 1007)",)))
        self.assertEqual(["SELECT_ACTION_1013"],
                         qm.control_dialogs(("IF_TRUE:(env.getDialogId() == 1013)",)))

    def test_start_helper_with_static_item_compiles_typed_starter_action(self) -> None:
        path = (
            "IF_TRUE:(qs == null || qs.getStatus() == QuestStatus.NONE)",
            "IF_TRUE:(targetId == 700001)",
            "IF_FALSE:(env.getDialog() == QuestDialog.START_DIALOG)",
        )
        call = {
            "method": "sendQuestStartDialog", "line": 20, "source_position": 200,
            "control_path": list(path), "arguments": ["env", "182400001", "3"],
            "argument_values": [None, 182400001, 3], "result_usage": "RETURNED",
            "result_context": "sendQuestStartDialog(env, 182400001, 3)",
        }
        plans = []

        qm.compile_start_helper_group(plans, [call], path, "Q1.java")

        accept = next(plan for plan in plans if plan["dialog"] == "ACCEPT_QUEST")
        self.assertEqual(
            ["start-quest", "give-quest-item", "sync-quest-status", "send-dialog"],
            [value["tag"] for value in accept["actions"]],
        )
        self.assertEqual(
            {"item_id": "182400001", "count": "3", "mode": "TOP_UP_TO"},
            accept["actions"][1]["attrs"],
        )

    def test_dialog_window_packet_requires_event_target_object_metadata(self) -> None:
        packet = {
            "method": "sendPacket", "line": 20,
            "arguments": ["player", "new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 0)"],
        }

        self.assertEqual("close-dialog", qm.dialog_window_packet_action(packet)["tag"])
        opened = dict(packet, arguments=[
            "player", "new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 1011)",
        ])
        self.assertEqual({"dialog_id": "1011"}, qm.dialog_window_packet_action(opened)["attrs"])
        hard_coded = dict(packet, arguments=["player", "new SM_DIALOG_WINDOW(700001, 0)"])
        with self.assertRaisesRegex(qm.AuditError, "Unsupported SM_DIALOG_WINDOW target"):
            qm.dialog_window_packet_action(hard_coded)

    def test_protocol_metadata_must_share_an_outer_action_call(self) -> None:
        context = "PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 0))"
        metadata = {
            "method": "getObjectId", "line": 20, "result_usage": "ARGUMENT", "result_context": context,
        }
        packet = {"method": "sendPacket", "line": 20, "result_context": context}

        self.assertEqual([packet], qm.validate_metadata_calls([metadata, packet]))
        with self.assertRaisesRegex(qm.AuditError, "Unbound getObjectId metadata"):
            qm.validate_metadata_calls([dict(metadata, result_context="other"), packet])

    def test_mutation_group_compiles_guarded_exact_remove(self) -> None:
        path = (
            "IF_TRUE:(qs.getStatus() == QuestStatus.START)",
            "IF_TRUE:(targetId == 700001)",
            "SWITCH:(env.getDialog())=STEP_TO_1",
            "IF_TRUE:(player.getInventory().getItemCountByItemId(182400002) >= 2)",
        )
        remove = {
            "method": "removeQuestItem", "line": 20, "source_position": 200,
            "control_path": list(path), "arguments": ["env", "182400002", "2"],
            "argument_values": [None, 182400002, 2], "result_usage": "IGNORED",
        }
        close = {
            "method": "closeDialogWindow", "line": 21, "source_position": 210,
            "control_path": list(path), "arguments": ["env"], "argument_values": [None],
            "result_usage": "RETURNED",
        }
        plans = []

        qm.compile_mutation_group(plans, [remove, close], path, set())

        self.assertEqual("player-inventory", plans[0]["conditions"][1]["tag"])
        self.assertEqual(
            {"item_id": "182400002", "count": "2", "mode": "EXACT"},
            plans[0]["actions"][0]["attrs"],
        )

    def test_mutation_group_expands_packed_quest_variables_and_selection_protocol(self) -> None:
        path = (
            "IF_TRUE:(qs.getStatus() == QuestStatus.START)",
            "IF_TRUE:(targetId == 700001)",
            "IF_TRUE:(env.getDialogId() == 1009)",
        )
        packed = {
            "method": "setQuestVar", "line": 20, "source_position": 200,
            "control_path": list(path), "arguments": ["99"], "argument_values": [99],
            "result_usage": "IGNORED",
        }
        update = {
            "method": "updateQuestStatus", "line": 21, "source_position": 210,
            "control_path": list(path), "arguments": ["env"], "argument_values": [None],
            "result_usage": "IGNORED",
        }
        selection = {
            "method": "sendQuestSelectionDialog", "line": 22, "source_position": 220,
            "control_path": list(path), "arguments": ["env"], "argument_values": [None],
            "result_usage": "RETURNED",
        }
        plans = []
        variables = set()

        qm.compile_mutation_group(plans, [packed, update, selection], path, variables)

        self.assertEqual({f"var{index}" for index in range(6)}, variables)
        self.assertEqual([35, 1, 0, 0, 0, 0],
                         [int(value["attrs"]["value"]) for value in plans[0]["actions"][:6]])
        self.assertEqual(["sync-quest-status", "show-quest-list"],
                         [value["tag"] for value in plans[0]["actions"][-2:]])

    def test_mutation_group_accepts_proven_action_only_true_return(self) -> None:
        path = (
            "IF_TRUE:(qs.getStatus() == QuestStatus.START)",
            "SWITCH:(env.getTargetId())=700418",
            "SWITCH:(env.getDialog())=USE_OBJECT",
            "IF_TRUE:(player.getInventory().getItemCountByItemId(182208062) < 1)",
        )
        set_variable = {
            "method": "setQuestVarById", "line": 20, "source_position": 200,
            "control_path": list(path), "arguments": ["0", "qs.getQuestVarById(0) + 1"],
            "argument_values": [0, None], "result_usage": "IGNORED",
        }
        set_status = {
            "method": "setStatus", "line": 21, "source_position": 210,
            "control_path": list(path), "arguments": ["QuestStatus.REWARD"],
            "argument_values": [None], "result_usage": "IGNORED",
        }
        update = {
            "method": "updateQuestStatus", "line": 22, "source_position": 220,
            "control_path": list(path), "arguments": ["env"], "argument_values": [None],
            "result_usage": "IGNORED",
        }
        returned = [{"value": True, "line": 23, "source_position": 230, "control_path": list(path)}]
        plans = []

        qm.compile_mutation_group(plans, [set_variable, set_status, update], path, set(), returned)

        self.assertEqual(["quest-status", "player-inventory"],
                         [value["tag"] for value in plans[0]["conditions"]])
        self.assertEqual(["add-quest-variable", "set-quest-status", "sync-quest-status"],
                         [value["tag"] for value in plans[0]["actions"]])
        with self.assertRaisesRegex(qm.AuditError, "no proven action-only return"):
            qm.compile_mutation_group([], [set_variable, set_status, update], path, set())

    def test_default_close_expands_state_item_and_protocol_actions(self) -> None:
        path = (
            "IF_TRUE:(qs.getStatus() == QuestStatus.START)",
            "IF_TRUE:(env.getTargetId() == 700001)",
            "SWITCH:(env.getDialog())=STEP_TO_1",
            "IF_TRUE:(player.getInventory().getItemCountByItemId(182400002) >= 1)",
        )
        helper = {
            "method": "defaultCloseDialog", "line": 20, "source_position": 200,
            "control_path": list(path),
            "arguments": ["env", "0", "1", "false", "false", "0", "182400001", "2",
                          "182400002", "1"],
            "argument_values": [None, 0, 1, None, None, 0, 182400001, 2, 182400002, 1],
            "result_usage": "RETURNED",
        }
        plans = []
        variables = set()

        qm.compile_default_close_group(plans, [helper], path, variables, {700001: "general"})

        self.assertEqual({"var0"}, variables)
        self.assertEqual(["quest-status", "quest-variable", "player-inventory"],
                         [value["tag"] for value in plans[0]["conditions"]])
        self.assertEqual(
            ["set-quest-variable", "give-quest-item", "remove-quest-item", "sync-quest-status",
             "show-quest-list"],
            [value["tag"] for value in plans[0]["actions"]],
        )
        self.assertEqual("TOP_UP_TO", plans[0]["actions"][1]["attrs"]["mode"])
        self.assertEqual("EXACT", plans[0]["actions"][2]["attrs"]["mode"])

    def test_default_close_same_npc_opens_proven_reward_page(self) -> None:
        path = (
            "IF_TRUE:(qs.getStatus() == QuestStatus.START)",
            "IF_TRUE:(targetId == 700001)",
            "IF_TRUE:(env.getDialog() == QuestDialog.SELECT_REWARD)",
        )
        helper = {
            "method": "defaultCloseDialog", "line": 20, "source_position": 200,
            "control_path": list(path), "arguments": ["env", "3", "3", "true", "true", "2"],
            "argument_values": [None, 3, 3, None, None, 2], "result_usage": "RETURNED",
        }
        plans = []

        qm.compile_default_close_group(plans, [helper], path, set(), {700001: "general"})

        self.assertEqual("reward", plans[0]["target"])
        self.assertEqual(["set-quest-status", "sync-quest-status", "send-dialog"],
                         [value["tag"] for value in plans[0]["actions"]])
        self.assertEqual("7", plans[0]["actions"][-1]["attrs"]["dialog_id"])

    def test_default_close_preserves_explicit_variable_prefix_order(self) -> None:
        path = (
            "IF_TRUE:(qs.getStatus() == QuestStatus.START)",
            "IF_TRUE:(targetId == 700001)",
            "IF_TRUE:(env.getDialog() == QuestDialog.STEP_TO_1)",
        )
        packed = {
            "method": "setQuestVar", "line": 20, "source_position": 200,
            "control_path": list(path), "arguments": ["2"], "argument_values": [2],
            "result_usage": "IGNORED",
        }
        helper = {
            "method": "defaultCloseDialog", "line": 21, "source_position": 210,
            "control_path": list(path), "arguments": ["env", "2", "2", "true", "false"],
            "argument_values": [None, 2, 2, None, None], "result_usage": "RETURNED",
        }
        plans = []

        qm.compile_default_close_group(plans, [packed, helper], path, set(), {700001: "general"})

        self.assertEqual(["quest-status"], [value["tag"] for value in plans[0]["conditions"]])
        self.assertEqual(
            ["set-quest-variable"] * 6 + ["set-quest-status", "sync-quest-status", "show-quest-list"],
            [value["tag"] for value in plans[0]["actions"]],
        )

    def test_default_close_rejects_unproven_remove_and_result_paths(self) -> None:
        path = (
            "IF_TRUE:(qs.getStatus() == QuestStatus.START)",
            "IF_TRUE:(targetId == 700001)",
            "SWITCH:(env.getDialog())=STEP_TO_1",
        )
        helper = {
            "method": "defaultCloseDialog", "line": 20, "source_position": 200,
            "control_path": list(path),
            "arguments": ["env", "0", "1", "0", "0", "182400002", "1"],
            "argument_values": [None, 0, 1, 0, 0, 182400002, 1], "result_usage": "RETURNED",
        }
        with self.assertRaisesRegex(qm.AuditError, "lacks a sufficient inventory guard"):
            qm.compile_default_close_group([], [helper], path, set(), {700001: "general"})

        ignored = dict(helper, arguments=["env", "0", "1"], argument_values=[None, 0, 1],
                       result_usage="IGNORED")
        with self.assertRaisesRegex(qm.AuditError, "result is not returned"):
            qm.compile_default_close_group([], [ignored], path, set(), {700001: "general"})

        same_npc = dict(helper, arguments=["env", "0", "1", "true", "true"],
                        argument_values=[None, 0, 1, None, None])
        with self.assertRaisesRegex(qm.AuditError, "sameNpc dialog is unsupported"):
            qm.compile_default_close_group([], [same_npc], path, set(), {700001: "general"})

    def test_mutation_group_rejects_dynamic_and_unproven_item_calls(self) -> None:
        path = (
            "IF_TRUE:(qs.getStatus() == QuestStatus.START)",
            "IF_TRUE:(targetId == 700001)",
            "SWITCH:(env.getDialog())=STEP_TO_1",
        )
        dynamic = {
            "method": "removeQuestItem", "line": 20, "source_position": 200,
            "control_path": list(path), "arguments": ["env", "itemId", "count"],
            "argument_values": [None, None, None], "result_usage": "IGNORED",
        }
        with self.assertRaisesRegex(qm.AuditError, "quest item id is not a constant integer"):
            qm.compile_mutation_group([], [dynamic], path, set())
        self.assertEqual("DYNAMIC_QUEST_ITEM_ARGUMENT",
                         qm.dialog_compiler_error_family("quest item id is not a constant integer at line 20: itemId"))

        ignored = {
            "method": "giveQuestItem", "line": 21, "source_position": 210,
            "control_path": list(path), "arguments": ["env", "182400001", "1"],
            "argument_values": [None, 182400001, 1], "result_usage": "IGNORED",
        }
        with self.assertRaisesRegex(qm.AuditError, "Ignored giveQuestItem has no explicit failure edge"):
            qm.compile_mutation_group([], [ignored], path, set())

        compound = dict(ignored, result_usage="CONDITION",
                        result_context="(giveQuestItem(env, 182400001, 1) && ready)",
                        condition_true_outcome="RETURN_TRUE", condition_false_outcome="FALLTHROUGH")
        with self.assertRaisesRegex(qm.AuditError, "requires an exact success branch"):
            qm.compile_mutation_group([], [compound], path, set())

    def test_condition_give_folds_only_proven_success_branch(self) -> None:
        base = (
            "IF_TRUE:(qs == null || qs.getStatus() == QuestStatus.NONE)",
            "IF_TRUE:(targetId == 700001)",
            "IF_FALSE:(env.getDialog() == QuestDialog.START_DIALOG)",
            "IF_TRUE:(env.getDialogId() == 1002)",
        )
        context = "(giveQuestItem(env, 182400001, 1))"
        give = {
            "method": "giveQuestItem", "select": "giveQuestItem", "line": 20, "source_position": 200,
            "control_path": list(base), "arguments": ["env", "182400001", "1"],
            "argument_values": [None, 182400001, 1], "result_usage": "CONDITION", "result_context": context,
            "condition_true_outcome": "RETURN_VALUE", "condition_false_outcome": "FALLTHROUGH",
        }
        start = {
            "method": "sendQuestStartDialog", "select": "sendQuestStartDialog", "line": 21, "source_position": 210,
            "control_path": list(base + (f"IF_TRUE:{context}",)), "arguments": ["env"],
            "argument_values": [None], "result_usage": "RETURNED",
        }
        groups = {base: [give], base + (f"IF_TRUE:{context}",): [start]}

        qm.fold_positive_condition_gives(groups)

        self.assertNotIn(base, groups)
        success_path = base + (f"IF_TRUE:{context}",)
        self.assertEqual({"giveQuestItem", "sendQuestStartDialog"},
                         {call["method"] for call in groups[success_path]})
        plans = []
        qm.compile_start_helper_group(plans, sorted(groups[success_path], key=lambda call: call["source_position"]),
                                      success_path, "Q1.java")
        self.assertIsNotNone(next(plan for plan in plans if plan["dialog"] == "ACCEPT_QUEST"
                                  and any(value["tag"] == "give-quest-item" for value in plan["actions"])))

    def test_condition_give_negated_failure_branch_is_explicit(self) -> None:
        path = (
            "IF_TRUE:(qs.getStatus() == QuestStatus.START)",
            "IF_TRUE:(targetId == 700001)",
            "SWITCH:(env.getDialog())=STEP_TO_1",
        )
        give = {
            "method": "giveQuestItem", "select": "giveQuestItem", "line": 20, "source_position": 200,
            "control_path": list(path), "arguments": ["env", "182400001", "1"],
            "argument_values": [None, 182400001, 1], "result_usage": "CONDITION",
            "result_context": "(!giveQuestItem(env, 182400001, 1))",
            "condition_true_outcome": "RETURN_TRUE", "condition_false_outcome": "FALLTHROUGH",
        }
        close = {
            "method": "closeDialogWindow", "line": 21, "source_position": 210,
            "control_path": list(path), "arguments": ["env"], "argument_values": [None],
            "result_usage": "RETURNED",
        }
        plans = []

        qm.compile_mutation_group(plans, [give, close], path, set())

        self.assertEqual(["give-quest-item", "close-dialog"],
                         [value["tag"] for value in plans[0]["actions"]])

    def test_graph_item_action_references_include_give_and_remove(self) -> None:
        graph = qm.ET.fromstring(
            '<quest_graph><node><transition><actions>'
            '<give-quest-item item_id="182400002"/><remove-quest-item item_id="182400001"/>'
            '</actions></transition></node></quest_graph>'
        )
        self.assertEqual([182400001, 182400002], qm.graph_item_action_references(graph))

    def test_dialog_action_rejects_page_outside_formal_ir_contract(self) -> None:
        with self.assertRaisesRegex(qm.AuditError, "Dialog page must be positive for collect failure: 0"):
            qm.send_dialog_action(0, "collect failure")
        self.assertEqual("NON_POSITIVE_DIALOG_PAGE",
                         qm.dialog_compiler_error_family("Dialog page must be positive for collect failure: 0"))

    def test_dialog_handler_compiles_repeat_collect_and_terminal_protocol_paths(self) -> None:
        def call(method: str, line: int, path: list[str], arguments: list[str], values: list[object],
                 enclosing: str = "onDialogEvent") -> dict[str, object]:
            return {
                "method": method,
                "line": line,
                "control_path": path,
                "arguments": arguments,
                "argument_values": values,
                "enclosing_method": enclosing,
                "select": method,
            }

        offer = ["IF_TRUE:(qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat())",
                 "IF_TRUE:(targetId == 700001)"]
        active = ["IF_FALSE:(qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat())",
                  "IF_TRUE:(qs.getStatus() == QuestStatus.START)", "IF_TRUE:(targetId == 700001)"]
        collect = active + ["SWITCH:(env.getDialog())=CHECK_COLLECTED_ITEMS"]
        reward = ["IF_TRUE:(qs.getStatus() == QuestStatus.REWARD)", "IF_TRUE:(targetId == 700001)"]
        use_object = ["IF_FALSE:(qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat())",
                      "IF_TRUE:(targetId == 700002)", "IF_TRUE:(env.getDialog() == QuestDialog.USE_OBJECT)"]
        calls = [
            call("registerQuestNpc", 10, [], ["700001"], [700001], "register"),
            call("addOnQuestStart", 10, [], ["questId"], [1], "register"),
            call("registerQuestNpc", 11, [], ["700001"], [700001], "register"),
            call("addOnTalkEvent", 11, [], ["questId"], [1], "register"),
            call("registerQuestNpc", 12, [], ["700002"], [700002], "register"),
            call("addOnTalkEvent", 12, [], ["questId"], [1], "register"),
            call("sendQuestDialog", 20, offer + ["SWITCH:(env.getDialog())=START_DIALOG"], ["env", "4762"], [None, 4762]),
            call("sendQuestStartDialog", 21, offer + ["IF_FALSE:(env.getDialog() == QuestDialog.START_DIALOG)"],
                 ["env"], [None]),
            call("closeDialogWindow", 30, use_object, ["env"], [None]),
            call("sendQuestDialog", 40, active + ["SWITCH:(env.getDialog())=START_DIALOG"], ["env", "1011"], [None, 1011]),
            call("collectItemCheck", 41, collect, ["env", "true"], [None, None]),
            call("setStatus", 42, collect + ["IF_TRUE:(QuestService.collectItemCheck(env, true))"],
                 ["QuestStatus.REWARD"], [None]),
            call("updateQuestStatus", 43, collect + ["IF_TRUE:(QuestService.collectItemCheck(env, true))"], ["env"], [None]),
            call("sendQuestDialog", 44, collect + ["IF_TRUE:(QuestService.collectItemCheck(env, true))"],
                 ["env", "10000"], [None, 10000]),
            call("sendQuestDialog", 45, collect + ["IF_FALSE:(QuestService.collectItemCheck(env, true))"],
                 ["env", "10001"], [None, 10001]),
            call("sendQuestEndDialog", 50, reward, ["env"], [None]),
        ]
        graph, parity = qm.compile_dialog_handler(
            {"quest_ids": [1], "path": "Q1.java", "calls": calls},
            "shape", {"max_repeat_count": 255, "requires_deadline": True,
                      "repeat_policy": {"repeat_kind": "DAILY", "time_basis": "SERVER_LOCAL", "reset_hour": 9}},
            {700001: "general", 700002: "quest_use_item"},
        )

        self.assertEqual(["offer", "active", "reward", "complete"],
                         [node.get("id") for node in graph.findall("node")])
        self.assertTrue(parity["matched"])
        self.assertFalse(parity["start_marker_dialog_mismatch"])
        offer_transitions = graph.findall("./node[@id='offer']/transition")
        self.assertIsNotNone(next(transition for transition in offer_transitions
                                  if transition.find("dialog[@dialog='ASK_ACCEPTION']") is not None
                                  and transition.find("actions/send-dialog[@dialog_id='4']") is not None))
        self.assertEqual(3, sum(transition.find("actions/close-dialog") is not None
                                for transition in offer_transitions
                                if transition.find("dialog") is not None
                                and transition.find("dialog").get("dialog", "").startswith("REFUSE_QUEST")))
        self.assertIsNotNone(next(transition for transition in offer_transitions
                                  if transition.find("dialog[@dialog='FINISH_DIALOG']") is not None
                                  and transition.find("actions/show-quest-list") is not None))
        self.assertEqual(16, len([
            transition for transition in graph.findall("./node[@id='reward']/transition")
            if transition.find("actions/finish-quest") is not None
        ]))
        for transition in graph.findall("./node[@id='reward']/transition"):
            finish = transition.find("actions/finish-quest")
            if finish is None:
                continue
            self.assertEqual(
                {"reward_index": "0", "repeat_kind": "DAILY", "time_basis": "SERVER_LOCAL", "reset_hour": "9"},
                finish.attrib,
            )
            message = transition.find("actions/send-repeat-deadline-message")
            self.assertIsNotNone(message)
            self.assertEqual(
                {"repeat_kind": "DAILY", "time_basis": "SERVER_LOCAL", "reset_hour": "9"},
                message.attrib,
            )
        collect_transition = next(
            transition for transition in graph.findall("./node[@id='active']/transition")
            if transition.find("conditions/quest-collect-items") is not None
        )
        self.assertEqual(
            ["set-quest-status", "remove-collected-items", "sync-quest-status", "send-dialog"],
            [value.tag for value in collect_transition.find("actions")],
        )
        complete = graph.find("./node[@id='complete']")
        self.assertIsNotNone(complete)
        repeat_values = [condition.get("expected") for condition in complete.findall("transition/conditions/quest-repeat-available")]
        self.assertIn("true", repeat_values)
        self.assertIn("false", repeat_values)
        unavailable_loop = next(
            transition for transition in complete.findall("transition")
            if transition.find("conditions/quest-repeat-available[@expected='false']") is not None
        )
        self.assertEqual("complete", unavailable_loop.get("to"))
        self.assertIsNotNone(unavailable_loop.find("actions/close-dialog"))

    def test_repeat_policy_parser_and_full_report_share_one_classification(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            quest_data = root / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml"
            quest_data.parent.mkdir(parents=True)
            quest_data.write_text(
                """
                <quests>
                  <quest id="1" max_repeat_count="255" repeat_cycle="ALL"/>
                  <quest id="2" max_repeat_count="255" repeat_cycle="MON WED"/>
                  <quest id="3" max_repeat_count="255" quest_cooltime="3600"/>
                  <quest id="4" max_repeat_count="1"/>
                </quests>
                """,
                encoding="utf-8",
            )

            config = self.config(root)
            evidence = qm.load_quest_evidence(config, {1, 2, 3, 4})
            report = qm.build_repeat_policy_report(config)

            self.assertEqual("DAILY", evidence[1]["repeat_policy"]["repeat_kind"])
            self.assertEqual("MON WED", evidence[2]["repeat_policy"]["weekdays"])
            self.assertEqual(3600, evidence[3]["repeat_policy"]["cooldown_seconds"])
            self.assertIsNone(evidence[4]["repeat_policy"])
            self.assertEqual(
                {"quests": 4, "time_based_quests": 3, "daily": 1, "weekly": 1,
                 "anchored_cooldown": 1, "unknown_or_conflicting": 0},
                report["counts"],
            )
            self.assertEqual([], report["blockers"])

    def test_repeat_policy_report_blocks_conflicting_sources(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            quest_data = root / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml"
            quest_data.parent.mkdir(parents=True)
            quest_data.write_text(
                '<quests><quest id="1" max_repeat_count="255" repeat_cycle="ALL" quest_cooltime="3600"/></quests>',
                encoding="utf-8",
            )

            report = qm.build_repeat_policy_report(self.config(root))

            self.assertEqual(1, report["counts"]["unknown_or_conflicting"])
            self.assertEqual([{"kind": "UNKNOWN_OR_CONFLICTING_REPEAT_POLICY", "count": 1}], report["blockers"])

    def test_direct_call_catalog_groups_shared_signatures(self) -> None:
        handlers = []
        for quest_id in (1, 2):
            handlers.append({
                "quest_id": quest_id,
                "path": f"Q{quest_id}.java",
                "direct_call_sites": [{
                    "signature": "getLevel/0",
                    "method": "getLevel",
                    "select": "player.getLevel",
                    "enclosing_method": "onDialogEvent",
                    "line": quest_id,
                }],
            })
        inventory = qm.InventoryBundle({}, "0" * 64, [], {"handlers": handlers}, {})
        catalog = qm.build_direct_call_catalog(inventory)
        self.assertEqual(1, catalog["counts"]["shared_signatures"])
        self.assertEqual("SHARED_MECHANISM_CANDIDATE", catalog["operations"][0]["mechanical_classification"])

    def test_direct_call_catalog_blocks_unreviewed_signature(self) -> None:
        inventory = qm.InventoryBundle({}, "0" * 64, [], {"handlers": [{
            "quest_id": 1,
            "path": "Q1.java",
            "direct_call_sites": [{
                "signature": "unknownCall/0",
                "method": "unknownCall",
                "select": "target.unknownCall",
                "enclosing_method": "onDialogEvent",
                "line": 1,
            }],
        }]}, {})
        catalog = qm.build_direct_call_catalog(inventory)
        self.assertEqual(1, catalog["counts"]["pending_review_signatures"])
        self.assertIn({"kind": "PENDING_DIRECT_CALL_REVIEW", "count": 1}, catalog["blockers"])

    def test_direct_call_catalog_expands_reviewed_local_composite(self) -> None:
        inventory = qm.InventoryBundle({}, "0" * 64, [], {"handlers": [{
            "quest_id": 1,
            "path": "Q1.java",
            "direct_call_sites": [{
                "signature": "checkReward/1",
                "method": "checkReward",
                "select": "checkReward",
                "enclosing_method": "onEnterZoneEvent",
                "line": 1,
            }],
        }]}, {})
        catalog = qm.build_direct_call_catalog(inventory)
        operation = catalog["operations"][0]
        self.assertEqual("DERIVED_REVIEWED", operation["semantic_status"])
        self.assertEqual(["Q1.java#checkReward"], operation["semantic_evidence"])
        self.assertEqual("ALL_OF_JOIN", operation["semantic_expansion"][1])
        self.assertEqual(1, catalog["counts"]["reviewed_signatures"])
        self.assertEqual(0, catalog["counts"]["pending_review_signatures"])

    def test_direct_call_catalog_qualifies_overloaded_get_by_receiver(self) -> None:
        inventory = qm.InventoryBundle({}, "0" * 64, [], {"handlers": [{
            "quest_id": 1,
            "path": "Q1.java",
            "direct_call_sites": [
                {"signature": "get/1", "method": "get", "select": "ZoneName.get",
                 "enclosing_method": "register", "line": 1},
                {"signature": "get/1", "method": "get", "select": "mobs.get",
                 "enclosing_method": "spawn", "line": 2},
            ],
        }]}, {})
        operation = qm.build_direct_call_catalog(inventory)["operations"][0]
        self.assertEqual("DERIVED_REVIEWED", operation["semantic_status"])
        self.assertEqual(
            ["ZONE_NAME_LOOKUP", "COLLECTION_INDEX_LOOKUP"],
            [row["semantic_expansion"][0] for row in operation["receiver_semantics"]],
        )

    def test_routing_policy_inventory_has_one_policy_per_entry(self) -> None:
        methods = [method for policy in qm.ROUTING_POLICIES for method in policy["methods"]]
        self.assertEqual(35, len(methods))
        self.assertEqual(len(methods), len(set(methods)))

    def test_handler_dialog_topology_compares_protocol_families(self) -> None:
        row = {
            "handler_candidate": True,
            "methods": ["onDialogEvent"],
            "quest_ids": [1],
            "path": "Q1.java",
            "dialog_branches": [
                {"kind": "SWITCH_CASE", "value": "ACCEPT_QUEST", "line": 1},
                {"kind": "ENUM_REFERENCE", "value": "SELECT_REWARD", "line": 2},
            ],
            "calls": [{"method": "sendQuestEndDialog", "enclosing_method": "onDialogEvent"}],
        }
        inventory = qm.InventoryBundle({}, "0" * 64, [row], {}, {})
        dialogs = qm.DialogBundle({}, "1" * 64, {"quests": [{
            "quest_id": 1,
            "path": "QUEST_Q1.html",
            "actions": [
                {"hactions": ["HACTION_QUEST_ACCEPT_1"]},
                {"hactions": ["HACTION_SELECT_QUEST_REWARD"]},
            ],
        }]}, {}, {})
        report = qm.build_handler_dialog_topology(inventory, dialogs)
        self.assertEqual([], report["blockers"])
        self.assertEqual([], report["quests"][0]["client_only_family_observations"])
        self.assertEqual("REWARD", qm.client_hyperlink_family("HACTION_SET_SUCCEED"))
        self.assertEqual("CUSTOM_SELECT", qm.client_hyperlink_family("HACTION_select_none_1"))

    def test_external_access_separates_quest_calls_from_name_collisions(self) -> None:
        rows = [{
            "path": "src/main/java/com/aionemu/gameserver/services/External.java",
            "handler_candidate": False,
            "methods": ["run"],
            "calls": [
                {"method": "setStatus", "select": "qs.setStatus", "enclosing_method": "run", "line": 1,
                 "arguments": ["status"], "receiver_types": ["QuestState"]},
                {"method": "setStatus", "select": "house.setStatus", "enclosing_method": "run", "line": 2,
                 "arguments": ["status"], "receiver_types": ["House"]},
                {"method": "getStatus", "select": "unknown.getStatus", "enclosing_method": "run", "line": 3,
                 "arguments": [], "receiver_types": []},
            ],
        }]
        report = qm.build_external_access_report(rows, "0" * 64)
        self.assertEqual(1, report["counts"]["confirmed_task_writes"])
        self.assertEqual(1, report["counts"]["non_quest_method_collisions"])
        self.assertEqual(1, report["counts"]["ambiguous_accesses"])
        self.assertEqual([{"kind": "AMBIGUOUS_TASK_ACCESS", "count": 1}], report["blockers"])
        self.assertEqual("GAMEPLAY_ACTION_BRIDGE", report["owner_groups"][0]["role"])
        self.assertEqual("PLAYER_QUEST_STATE", report["owner_groups"][0]["observed_state_scope"])

    def test_external_access_blocks_unreviewed_execution_surface(self) -> None:
        rows = [{
            "path": "src/main/java/com/aionemu/gameserver/misc/External.java",
            "handler_candidate": False,
            "methods": ["run"],
            "calls": [{
                "method": "setStatus",
                "select": "qs.setStatus",
                "enclosing_method": "run",
                "line": 1,
                "arguments": ["status"],
                "receiver_types": ["QuestState"],
            }],
        }]
        report = qm.build_external_access_report(rows, "0" * 64)
        self.assertEqual(1, report["counts"]["pending_execution_surfaces"])
        self.assertIn(
            {"kind": "PENDING_EXECUTION_SURFACE_OWNERSHIP_REVIEW", "count": 1},
            report["blockers"],
        )

    def test_check_rejects_drift(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "report.json"
            qm.write_or_check(path, "one\n", check=False)
            qm.write_or_check(path, "one\n", check=True)
            with self.assertRaises(qm.AuditError):
                qm.write_or_check(path, "two\n", check=True)

    def test_quest_graph_input_report_is_deterministic_and_detects_duplicate_owner(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            graph_root = root / "src/main/resources/aion/data/static_data/quest_graph_data"
            graphs = graph_root / "graphs"
            graphs.mkdir(parents=True)
            (graph_root / "quest_graph_data.xsd").write_text("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>", encoding="utf-8")
            (graph_root / "quest_graph_data.xml").write_text(
                "<quest_graphs><import file=\"graphs/a.xml\" skipRoot=\"true\"/>"
                "<import file=\"graphs/b.xml\" skipRoot=\"true\"/></quest_graphs>", encoding="utf-8")
            (graphs / "b.xml").write_text(
                "<quest_graphs><quest_graph quest_id=\"2\"/></quest_graphs>", encoding="utf-8")
            (graphs / "a.xml").write_text(
                "<quest_graphs><quest_graph quest_id=\"1\"/></quest_graphs>", encoding="utf-8")

            first = qm.build_quest_graph_input_report(self.config(root))
            second = qm.build_quest_graph_input_report(self.config(root))

            self.assertEqual(first, second)
            self.assertEqual([1, 2], first["quest_ids"])
            self.assertEqual([], first["blockers"])

            (graphs / "b.xml").write_text(
                "<quest_graphs><quest_graph quest_id=\"1\"/></quest_graphs>", encoding="utf-8")
            duplicate = qm.build_quest_graph_input_report(self.config(root))
            self.assertEqual(
                [{"kind": "DUPLICATE_QUEST_GRAPH_OWNER", "count": 1, "quest_ids": [1]}],
                duplicate["blockers"],
            )

            (graph_root / "quest_graph_data.xml").write_text(
                "<quest_graphs><import file=\"graphs/b.xml\" skipRoot=\"true\"/>"
                "<import file=\"graphs/a.xml\" skipRoot=\"true\"/></quest_graphs>", encoding="utf-8")
            with self.assertRaises(qm.AuditError):
                qm.build_quest_graph_input_report(self.config(root))

    def test_start_condition_report_expands_call_path_semantics_deterministically(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            quest_data = root / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml"
            quest_data.parent.mkdir(parents=True)
            quest_data.write_text(
                """
                <quests>
                  <quest id="1" max_repeat_count="1" category="QUEST"/>
                  <quest id="2" max_repeat_count="3" category="QUEST"/>
                  <quest id="3" max_repeat_count="1" category="MISSION">
                    <start_conditions>
                      <finished quest_id="2" reward="2"/>
                      <unfinished>1</unfinished>
                      <equipped>9001</equipped>
                    </start_conditions>
                    <start_conditions>
                      <noacquired>1</noacquired>
                      <acquired>2</acquired>
                    </start_conditions>
                  </quest>
                </quests>
                """,
                encoding="utf-8",
            )

            first = qm.build_start_condition_report(self.config(root))
            second = qm.build_start_condition_report(self.config(root))

            self.assertEqual(first, second)
            self.assertEqual([], first["blockers"])
            self.assertEqual([], first["migration_blockers"])
            self.assertEqual(2, first["counts"]["condition_groups"])
            self.assertEqual(1, first["counts"]["finished_repeatable"])
            quest = first["quests"][0]
            self.assertEqual("OR", quest["explicit_acceptance"]["operator"])
            self.assertEqual(2, len(quest["explicit_acceptance"]["branches"]))
            self.assertEqual("AND", quest["automatic_mission"]["operator"])
            self.assertEqual(6, len(quest["automatic_mission"]["conditions"]))
            self.assertNotIn("PLAYER_EQUIPPED", {condition["type"] for condition in quest["automatic_mission"]["conditions"]})
            self.assertIn(
                {"type": "QUEST_COMPLETION_COUNT", "quest_id": 2, "op": "EQUAL", "count": 3},
                quest["source_groups"][0]["conditions"],
            )

            quest_data.write_text(quest_data.read_text(encoding="utf-8").replace("<acquired>2</acquired>", "<acquired>99</acquired>"),
                                  encoding="utf-8")
            missing = qm.build_start_condition_report(self.config(root))
            self.assertEqual([], missing["blockers"])
            self.assertFalse(missing["quests"][0]["migration_ready"])
            self.assertEqual([99], missing["quests"][0]["missing_referenced_quest_ids"])
            self.assertEqual("MISSING_START_CONDITION_QUEST", missing["migration_blockers"][0]["kind"])
            self.assertEqual([{"owner_quest_id": 3, "referenced_quest_id": 99}],
                             missing["migration_blockers"][0]["references"])

    def test_duplicate_owner_is_a_conflict(self) -> None:
        owners = [
            {"quest_id": 1, "kind": "JAVA_HANDLER", "path": "A.java"},
            {"quest_id": 1, "kind": "XML_HANDLER", "path": "a.xml"},
        ]
        conflicts = qm.find_owner_conflicts(owners)
        self.assertEqual(1, len(conflicts))
        self.assertEqual(1, conflicts[0]["quest_id"])


if __name__ == "__main__":
    unittest.main()
