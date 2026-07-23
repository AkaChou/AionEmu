import importlib.util
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


SCRIPT = Path(__file__).with_name("generate_retail_simple_quests.py")
SPEC = importlib.util.spec_from_file_location("retail_hunts", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(MODULE)


class RetailSimpleQuestGeneratorTest(unittest.TestCase):
	def test_simple_talk_fields_compile_cutscene_and_step_items(self):
		with tempfile.TemporaryDirectory() as directory:
			path = Path(directory) / "Quest_SimpleTalk.xml"
			path.write_text("""<quest_simpletalks>
				<quest_simpletalk id="1"><acquired_npc_name>Start</acquired_npc_name><give_item>ITEM_DOC 1</give_item><reward_npc_name>End</reward_npc_name><cutsceneid1>93</cutsceneid1><cs1_haction>1009</cs1_haction></quest_simpletalk>
				<quest_simpletalk id="2"><acquired_npc_name>Start</acquired_npc_name><talk_npc1>Middle</talk_npc1><give_item1>ITEM_NEXT 2</give_item1><remove_item1>ITEM_OLD 1</remove_item1><reward_npc_name>End</reward_npc_name></quest_simpletalk>
			</quest_simpletalks>""")

			quests, stats, skipped = MODULE.simple_talks(path, {1, 2})

			self.assertEqual(0, stats["unsupported"])
			self.assertEqual({"unsupported": {}, "invalid": []}, skipped)
			self.assertEqual(("DOC", 1), quests[1]["start_give_item"])
			self.assertEqual({
				"type": "TALK", "names": ["End"], "dialog_id": 2375, "advance_dialog_id": 1009,
				"movie": 93, "remove_item": ("DOC", 1),
			}, quests[1]["steps"][0])
			self.assertEqual(("NEXT", 2), quests[2]["steps"][0]["give_item"])
			self.assertEqual(("OLD", 1), quests[2]["steps"][0]["remove_item"])
			content, unresolved, generated = MODULE.render(
				{}, {}, {}, {}, quests, {"start": (1,), "end": (2,), "middle": (3,)}, {},
				{"doc": (10,), "next": (11,), "old": (12,)}, ())
			self.assertEqual({}, unresolved)
			self.assertEqual(2, generated["data_driven_complex"])
			xml = {int(node.attrib["id"]): node for node in ET.fromstring(content.split(b"-->\n", 1)[1])}
			self.assertEqual({
				"type": "TALK", "ids": "2", "dialog_id": "2375", "advance_dialog_id": "1009", "movie": "93",
				"remove_item_id": "10", "remove_item_count": "1",
			}, xml[1][0].attrib)

	def test_existing_java_handlers_keep_ownership(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			handlers = root / "src/main/java/com/aionemu/gameserver/quest/handlers/test"
			handlers.mkdir(parents=True)
			(handlers / "_123Handled.java").touch()
			(handlers / "_not_a_quest.java").touch()

			self.assertEqual({123}, MODULE.existing_java_handler_ids(root))

	def test_java_handler_audit_closes_every_retained_owner(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			handlers = root / "src/main/java/com/aionemu/gameserver/quest/handlers/test"
			handlers.mkdir(parents=True)
			(handlers / "_1Simple.java").write_text("boolean onDialogEvent() { return false; }")
			(handlers / "_2Movie.java").write_text("void onMovieEndEvent() { playQuestMovie(); }")
			(handlers / "_3Outside.java").write_text("boolean onKillEvent() { return false; }")
			(handlers / "_4Action.java").write_text("boolean onDialogEvent() { return useQuestObject(); }")
			(handlers / "_5Inherited.java").write_text("class Handler extends AbstractQuest {}")
			(handlers / "_6Collect.java").write_text("boolean onDialogEvent() { return checkQuestItemsSimple(); }")
			(handlers / "_7StartItem.java").write_text("boolean onDialogEvent() { return sendQuestStartDialog(env, 1, 1); }")
			(handlers / "_8Reward.java").write_text("boolean onDialogEvent() { return sendQuestEndDialog(env, rewardId); }")
			(handlers / "_9CloseItem.java").write_text("boolean onDialogEvent() { return defaultCloseDialog(env, 1, 2, 0, 0, 182201765, 1); }")

			details, stats = MODULE.audit_java_handlers({1, 2, 4, 5, 6, 7, 8, 9}, root)

			self.assertEqual("retained_authoritative_gap", details["1"]["disposition"])
			self.assertEqual(["movie"], details["2"]["mechanisms"])
			self.assertEqual("retained_complex_semantics", details["2"]["disposition"])
			self.assertEqual("not_executable_without_base", details["3"]["disposition"])
			self.assertEqual(["action"], details["4"]["mechanisms"])
			self.assertEqual(["inherited"], details["5"]["mechanisms"])
			self.assertEqual(["item_condition"], details["6"]["mechanisms"])
			self.assertEqual(["item_mutation"], details["7"]["mechanisms"])
			self.assertEqual(["dynamic_reward"], details["8"]["mechanisms"])
			self.assertEqual(["item_mutation"], details["9"]["mechanisms"])
			self.assertEqual(9, stats["handlers"])
			self.assertEqual(8, stats["base_handlers"])
			duplicate = handlers.parent / "duplicate"
			duplicate.mkdir()
			(duplicate / "_1Duplicate.java").touch()
			with self.assertRaisesRegex(ValueError, "duplicate Java quest handler id"):
				MODULE.audit_java_handlers({1, 2, 4, 5, 6, 7, 8, 9}, root)

	def test_compiled_script_evidence_indexes_and_groups_matches(self):
		with tempfile.TemporaryDirectory() as directory:
			script_root = Path(directory)
			(script_root / "fun_001.cpp").write_text("0x64 0xc8 0xc8 0x640")
			(script_root / "fun_002.cpp").write_text("0X64 0xc8")
			(script_root / "fun_003.cpp").write_text("0x12c")

			evidence = MODULE.compiled_script_evidence(script_root, {100, 200, 300, 400})

			self.assertEqual(["fun_001.cpp", "fun_002.cpp"], evidence[100]["script_files"])
			self.assertEqual(2, evidence[100]["script_occurrences"])
			self.assertEqual(3, evidence[200]["script_occurrences"])
			self.assertEqual([100, 200], evidence[100]["script_group_ids"])
			self.assertEqual([100, 200], evidence[200]["script_group_ids"])
			self.assertEqual("evidence_audit", evidence[300]["constraint_disposition"])
			self.assertEqual([], evidence[400]["script_group_ids"])
			self.assertEqual("blocked_no_script_hit", evidence[400]["constraint_disposition"])

	def test_compiled_coalescence_evidence_generates_direct_completion(self):
		enabled = MODULE.current_quest_ids(MODULE.DEFAULT_QUEST_DATA)
		quests, skipped = MODULE.compiled_coalescence_completes(
			MODULE.DEFAULT_RETAIL / "quest.xml", MODULE.DEFAULT_RETAIL / "npcs.xml",
			MODULE.DEFAULT_RETAIL_SCRIPT, MODULE.DEFAULT_QUEST_DATA, enabled)

		self.assertEqual({}, skipped)
		self.assertEqual({15542, 25542}, set(quests))
		self.assertTrue(all(quest["complete_on_start"] for quest in quests.values()))
		content, unresolved, generated = MODULE.render(
			{}, {}, {}, {}, quests, {"lf6_felen_e": (806074,), "df6_edorin_e": (806078,)}, {}, {}, ())
		self.assertEqual({}, unresolved)
		self.assertEqual(2, generated["compiled_script_coalescence_complete"])
		self.assertEqual(2, content.count(b'complete_on_start="true"'))

	def test_compiled_bastion_movies_generates_one_talk_step(self):
		enabled = MODULE.current_quest_ids(MODULE.DEFAULT_QUEST_DATA)
		quests, skipped = MODULE.compiled_bastion_movies(
			MODULE.DEFAULT_RETAIL / "quest.xml", MODULE.DEFAULT_RETAIL / "npcs.xml",
			MODULE.DEFAULT_RETAIL_SCRIPT, MODULE.DEFAULT_QUEST_DATA, enabled)

		self.assertEqual({}, skipped)
		self.assertEqual({18036, 28036}, set(quests))
		self.assertEqual({28}, {quest["movie"] for quest in quests.values()})
		self.assertEqual([["LDF5b_IDLDF5b_TD_Drakan_Fighter"], ["LDF5b_IDLDF5b_TD_Drakan_Fighter_Da"]],
			[quest["steps"][0]["names"] for quest in quests.values()])
		content, unresolved, generated = MODULE.render(
			{}, {}, {}, {}, quests,
			{"ldf5b_demades_e": (801281,), "ldf5b_latkel_e": (801280,),
				"ldf5b_idldf5b_td_drakan_fighter": (802008,),
				"ldf5b_idldf5b_td_drakan_fighter_da": (802015,)}, {}, {}, ())
		self.assertEqual({}, unresolved)
		self.assertEqual(2, generated["compiled_script_bastion_movie"])
		self.assertEqual(2, content.count(b'movie="28"'))

	def test_compiled_simple_talks_close_script_and_npc_chain(self):
		enabled = MODULE.current_quest_ids(MODULE.DEFAULT_QUEST_DATA)
		quests, skipped = MODULE.compiled_simple_talks(
			MODULE.DEFAULT_RETAIL / "quest.xml", MODULE.DEFAULT_RETAIL / "Quest_SimpleTalk.xml", MODULE.DEFAULT_RETAIL / "Quest_SimpleUseItem.xml", MODULE.DEFAULT_RETAIL / "npcs.xml",
			MODULE.DEFAULT_RETAIL_SCRIPT, MODULE.DEFAULT_QUEST_DATA, enabled)

		self.assertEqual({}, skipped)
		self.assertEqual({1131, 1218, 1220, 1422, 1423, 1469, 1483, 1484, 1553, 1574, 1987, 2207, 2278, 2279, 2321, 2421, 2428, 2458, 2480, 2512, 2515, 2523, 2692, 2914, 2985, 3020, 3035, 3037, 3076, 3973, 4015, 4052, 4501, 11010, 16979, 26979}, set(quests))
		self.assertEqual("data_driven_simple", quests[1131]["kind"])
		self.assertEqual(("quest_1131a", 1), quests[1131]["start_give_item"])
		self.assertEqual(("doc_quest_1131b", 1), quests[1131]["steps"][0]["give_item"])
		self.assertEqual(("doc_quest_1218a", 1), quests[1218]["steps"][0]["give_item"])
		self.assertEqual(("quest_1220a", 1), quests[1220]["start_give_item"])
		self.assertEqual(("quest_1220b", 1), quests[1220]["steps"][0]["give_item"])
		self.assertEqual(("quest_1220a", 1), quests[1220]["steps"][0]["remove_item"])
		self.assertEqual("data_driven_simple", quests[1469]["kind"])
		self.assertEqual(("quest_1469a", 1), quests[1469]["start_give_item"])
		self.assertIsNone(quests[1469]["steps"][0]["give_item"])
		self.assertIsNone(quests[1469]["steps"][0]["remove_item"])
		self.assertEqual([("quest_1483a", 1), ("quest_1483b", 1)], [step["give_item"] for step in quests[1483]["steps"]])
		self.assertEqual([("quest_1484a", 1), ("quest_1484b", 1), ("quest_1484c", 1)], [step["give_item"] for step in quests[1484]["steps"]])
		self.assertEqual(("quest_1553a", 1), quests[1553]["start_give_item"])
		self.assertEqual((("quest_1553b", 1), ("quest_1553a", 1)), (quests[1553]["steps"][0]["give_item"], quests[1553]["steps"][0]["remove_item"]))
		self.assertEqual([("quest_1574a", 2)] * 3, [step["remove_item"] for step in quests[1574]["steps"]])
		self.assertEqual(("doc_quest_2207a", 1), quests[2207]["start_give_item"])
		self.assertEqual(("doc_quest_2207a", 1), quests[2207]["steps"][1]["remove_item"])
		self.assertEqual((("doc_quest_2278a", 1), ("doc_quest_2278a", 1)), (quests[2278]["steps"][0]["give_item"], quests[2278]["steps"][2]["remove_item"]))
		self.assertEqual(("quest_2279a", 1), quests[2279]["steps"][1]["give_item"])
		self.assertEqual("report_to_many", quests[1422]["kind"])
		self.assertEqual(["Laokones"], quests[1422]["talks"])
		self.assertEqual("report_to", quests[1423]["kind"])
		self.assertEqual(("Vidar", "Roskva"), (quests[2985]["start"], quests[2985]["end"]))
		self.assertEqual(["Naiting", "Lionel", "LF4_FOBJ_Q11010A"], [step["names"][0] for step in quests[11010]["steps"]])
		self.assertEqual(("quest_11010a", 1), quests[11010]["steps"][2]["give_item"])
		self.assertEqual(("quest_2512a", 2), quests[2512]["start_give_item"])
		self.assertEqual(("quest_2512a", 1), quests[2512]["steps"][0]["remove_item"])
		self.assertEqual([("doc_quest_2515a", 1), ("quest_2515c", 1), ("quest_2515e", 1)], [quests[2515]["start_give_item"], quests[2515]["steps"][1]["give_item"], quests[2515]["steps"][2]["give_item"]])
		self.assertEqual([1, 1, 1], [step["remove_item"][1] for step in quests[2523]["steps"]])
		self.assertEqual((("quest_2692a", 1), ("quest_2692b", 1), ("quest_2692a", 1)), (quests[2692]["start_give_item"], quests[2692]["steps"][2]["give_item"], quests[2692]["steps"][2]["remove_item"]))
		self.assertEqual(("quest_4501a", 1), quests[4501]["steps"][1]["give_item"])
		self.assertEqual((("quest_3035a", 1), ("quest_3035b", 1), ("quest_3035a", 1)), (quests[3035]["start_give_item"], quests[3035]["steps"][1]["give_item"], quests[3035]["steps"][1]["remove_item"]))
		self.assertEqual([("quest_3973a", 1), ("quest_3973b", 1), ("quest_3973c", 1)], [step["give_item"] for step in quests[3973]["steps"]])
		self.assertEqual((("quest_4052a", 1), ("quest_4052a", 1)), (quests[4052]["steps"][1]["give_item"], quests[4052]["steps"][2]["remove_item"]))
		self.assertEqual(("ITEM_PLAY", "quest_2321b"), (quests[2321]["start_type"], quests[2321]["start"]))
		self.assertEqual(("doc_quest_2321a", 1), quests[2321]["steps"][0]["give_item"])
		self.assertEqual(("quest_2321b", 1), quests[2321]["steps"][0]["remove_item"])
		self.assertEqual((132, ("quest_2421a", 1)), (quests[2421]["movie"], quests[2421]["start_give_item"]))
		self.assertEqual(("quest_2421b", 1), quests[2421]["steps"][0]["give_item"])
		self.assertEqual(("quest_2421a", 1), quests[2421]["steps"][0]["remove_item"])
		self.assertEqual([None, ("quest_2428a", 1)], [step.get("give_item") for step in quests[2428]["steps"]])
		self.assertEqual(("quest_2458a", 1), quests[2458]["start_give_item"])
		self.assertEqual(("doc_quest_2458b", 1), quests[2458]["steps"][0]["give_item"])
		self.assertEqual(("quest_2458a", 1), quests[2458]["steps"][0]["remove_item"])
		self.assertEqual([("quest_2480a", 1), ("quest_2480b", 1)], [step["give_item"] for step in quests[2480]["steps"]])
		self.assertEqual(("quest_2914a", 1), quests[2914]["steps"][0]["give_item"])
		self.assertEqual((363, ("quest_3020a", 1), ("quest_3020a", 1)), (quests[3020]["movie"], quests[3020]["start_give_item"], quests[3020]["steps"][0]["remove_item"]))
		self.assertEqual(("quest_3037a", 1), quests[3037]["steps"][0]["give_item"])
		self.assertEqual(["Ascalon", "Cymaon"], [step["names"][0] for step in quests[3076]["steps"]])
		self.assertEqual(("quest_3076a", 1), quests[3076]["steps"][1]["give_item"])
		self.assertEqual((394, ["DF2A_FOBJ_Q4015"]), (quests[4015]["movie"], quests[4015]["steps"][0]["names"]))
		self.assertEqual(("item_collecting", 886), (quests[16979]["kind"], quests[16979]["movie"]))
		self.assertEqual(("item_collecting", 887), (quests[26979]["kind"], quests[26979]["movie"]))
		report_quests = {quest_id: quest for quest_id, quest in quests.items() if quest["kind"] != "data_driven_simple"}
		data_quests = {quest_id: quests[quest_id] for quest_id in (1131, 1218, 1220, 1469, 1483, 1484, 1553, 1574, 2207, 2278, 2279, 2321, 2421, 2428, 2458, 2480, 2512, 2515, 2523, 2692, 2914, 3020, 3035, 3037, 3076, 3973, 4015, 4052, 4501, 11010)}
		content, unresolved, generated = MODULE.render(
			report_quests, {}, {}, {}, data_quests,
				{"hyacinte": (203097,), "shugo_lf1a_01": (799093,), "nadaelo": (203101,), "taiotus": (203121,), "shugo3": (798004,), "une": (203172,), "shugo_lender_lf2_01": (205240,), "hagne": (790004,), "treasureguardianq_36_ae": (212878,), "shugo_lf2_13": (798126,), "herodes": (203940,), "ernia": (203944,), "shugo_lf2_14": (798127,), "anasya": (204045,), "telamone": (204048,), "sandinas": (204011,), "memnes": (203912,), "laokones": (203731,), "marana": (203983,), "diana": (203786,), "df2_npc_talkingmirror": (730051,), "perento": (204500,), "piera": (204584,), "tree_move_terba": (730025,), "trou": (204560,), "arkos": (204561,), "sirilis": (204562,), "fasimedes": (203700,), "bustant": (203749,), "lycan_messenger": (203590,), "lycan_interpreter": (203591,), "sueron": (203557,), "mimir": (204206,), "balder": (204075,), "soul_zenkaka": (203682,), "vidar": (204052,), "roskva": (204072,), "gunter": (204225,), "hellione": (790018,), "asgeirr": (204309,), "kerupnise": (204187,), "kistig": (204433,), "honir": (204102,), "moreinen": (204211,), "lif": (204379,), "df2_npc_sprigg": (204386,), "tree_move_nabalu": (730038,), "tree_move_virdi": (730021,), "tree_nomove_lodas": (730019,), "loki": (204703,), "gigrite": (204801,), "kistenian": (204753,), "elli": (790015,), "araison": (204192,), "mareke": (204205,), "shugo_df2_5": (798081,), "svera": (204802,), "shugo_df3_10": (798117,), "shugo_df3_11": (798118,), "shugo_df3_12": (798119,), "horu": (204734,), "labb_beholdernamedq_43_ae": (212164,), "lanse": (204108,), "shugo_ab1_d3": (279027,), "ab1_npc_lugbug": (279029,), "lapion": (204728,), "tekor": (204340,), "virashak": (204348,), "vatonia": (203830,), "tersites": (203893,), "utisda": (203792,), "daphnis": (203793,), "andu": (798391,), "mesalina": (798949,), "df2a_npc_moai": (730152,), "df2a_npc_bumbum_lin": (205179,), "df2a_npc_bumbum_jin": (205166,), "df2a_npc_bumbum_chan": (205197,), "air": (204147,), "frana": (204236,), "ankises": (798143,), "npc_agrint_tartagan": (798149,), "grynos": (798166,), "ixion": (798199,), "atropos": (798155,), "ascalon": (278503,), "cymaon": (278556,), "vinduer": (205130,), "df2a_fobj_q4015": (730107,), "pucio": (798931,), "naiting": (799071,), "lionel": (798906,), "lf4_fobj_q11010a": (730323,), "idldf5_under_01_war_moiro_e": (802025,), "ldf5_under_timarchus_e": (801762,), "idldf5_under_01_war_atea_e": (802026,), "ldf5_under_undgankt_e": (801764,)},
				{}, {"quest_1131a": (182200506,), "doc_quest_1131b": (182200507,), "doc_quest_1218a": (182200566,), "quest_1220a": (182200568,), "quest_1220b": (182200569,), "quest_1469a": (182201386,), "quest_1483a": (182201401,), "quest_1483b": (182201402,), "quest_1484a": (182201403,), "quest_1484b": (182201404,), "quest_1484c": (182201405,), "quest_1553a": (182201794,), "quest_1553b": (182201795,), "quest_1574a": (182201736,), "doc_quest_2207a": (182203257,), "doc_quest_2278a": (182203254,), "quest_2279a": (182203261,), "quest_2321b": (182204242,), "doc_quest_2321a": (182204119,), "quest_2421a": (182204208,), "quest_2421b": (182204209,), "quest_2428a": (182204216,), "quest_2458a": (182204194,), "doc_quest_2458b": (182204195,), "quest_2480a": (182204201,), "quest_2480b": (182204202,), "quest_2512a": (182204411,), "doc_quest_2515a": (182204412,), "quest_2515c": (182204414,), "quest_2515e": (182204416,), "quest_2523a": (182204417,), "quest_2692a": (182204510,), "quest_2692b": (182204511,), "quest_4501a": (182204533,), "quest_2914a": (182207014,), "quest_3020a": (182208011,), "quest_3035a": (182208024,), "quest_3035b": (182208025,), "quest_3037a": (182208027,), "quest_3076a": (182208047,), "quest_3973a": (182206116,), "quest_3973b": (182206117,), "quest_3973c": (182206118,), "quest_4052a": (182209030,), "quest_11010a": (182206713,)}, ())
		self.assertEqual({}, unresolved)
		self.assertEqual(36, generated["compiled_script_simple_talk"])
		self.assertIn(b'<data_driven_quest id="1131"', content)
		self.assertIn(b'<data_driven_quest id="1218"', content)
		self.assertIn(b'<data_driven_quest id="1220"', content)
		self.assertIn(b'<data_driven_quest id="1469"', content)
		self.assertIn(b'<data_driven_quest id="1483"', content)
		self.assertIn(b'<data_driven_quest id="1484"', content)
		self.assertIn(b'<data_driven_quest id="1553"', content)
		self.assertIn(b'<data_driven_quest id="1574"', content)
		self.assertIn(b'<data_driven_quest id="2207"', content)
		self.assertIn(b'<data_driven_quest id="2278"', content)
		self.assertIn(b'<data_driven_quest id="2279"', content)
		self.assertIn(b'<data_driven_quest id="2321" retail="true" start_type="ITEM_PLAY"', content)
		self.assertIn(b'<data_driven_quest id="2421" retail="true" start_type="TALK" movie="132"', content)
		self.assertIn(b'<data_driven_quest id="2428"', content)
		self.assertIn(b'<data_driven_quest id="2458"', content)
		self.assertIn(b'<data_driven_quest id="2480"', content)
		self.assertIn(b'<data_driven_quest id="2512"', content)
		self.assertIn(b'<data_driven_quest id="2515"', content)
		self.assertIn(b'<data_driven_quest id="2523"', content)
		self.assertIn(b'<data_driven_quest id="2692"', content)
		self.assertIn(b'<data_driven_quest id="2914"', content)
		self.assertIn(b'<data_driven_quest id="3020" retail="true" start_type="TALK" movie="363"', content)
		self.assertIn(b'<data_driven_quest id="3035"', content)
		self.assertIn(b'<data_driven_quest id="3037"', content)
		self.assertIn(b'<data_driven_quest id="3076"', content)
		self.assertIn(b'<data_driven_quest id="3973"', content)
		self.assertIn(b'<data_driven_quest id="4015" retail="true" start_type="TALK" movie="394"', content)
		self.assertIn(b'<data_driven_quest id="4052"', content)
		self.assertIn(b'<data_driven_quest id="4501"', content)
		self.assertIn(b'<data_driven_quest id="11010"', content)
		self.assertIn(b'<report_to_many id="1422"', content)
		self.assertIn(b'<report_to id="1423"', content)
		self.assertIn(b'<item_collecting id="16979" retail="true" end_npc_ids="801762" start_npc_ids="802025" movie="886"', content)
		self.assertIn(b'<item_collecting id="26979" retail="true" end_npc_ids="801764" start_npc_ids="802026" movie="887"', content)

	def test_compiled_surama_hunts_close_targets_count_and_rewards(self):
		enabled = MODULE.current_quest_ids(MODULE.DEFAULT_QUEST_DATA)
		quests, skipped = MODULE.compiled_surama_hunts(
			MODULE.DEFAULT_RETAIL / "quest.xml", MODULE.DEFAULT_RETAIL / "npcs.xml", MODULE.DEFAULT_RETAIL / "Items.xml",
			MODULE.DEFAULT_RETAIL_SCRIPT, MODULE.DEFAULT_QUEST_DATA, enabled)

		self.assertEqual({}, skipped)
		self.assertEqual({30708, 30758}, set(quests))
		self.assertTrue(all(quest["stages"][0][1] == 5 for quest in quests.values()))
		content, unresolved, generated = MODULE.render(
			{}, quests, {}, {}, {},
			{"idtiamat_surama_1": (800369,), "idtiamat_murugan_4": (800438,),
				"idtiamat_drakan_surama_1": (800425,), "idtiamat_drakan_surama_2": (800426,), "idtiamat_drakan_surama_3": (800427,)},
			{}, {}, ())
		self.assertEqual({}, unresolved)
		self.assertEqual(2, generated["compiled_script_surama_hunt"])
		xml = ET.fromstring(content.split(b"-->\n", 1)[1])
		for node in xml:
			self.assertEqual("monster_hunt", node.tag)
			self.assertEqual("800369", node.attrib["start_npc_ids"])
			self.assertEqual("800438", node.attrib["end_npc_ids"])
			self.assertEqual({"var": "0", "end_var": "5", "npc_ids": "800425 800426 800427"}, node[0].attrib)

	def test_compiled_christmas_courier_hunts_close_santa_and_two_kills(self):
		enabled = MODULE.current_quest_ids(MODULE.DEFAULT_QUEST_DATA)
		quests, skipped = MODULE.compiled_christmas_courier_hunts(
			MODULE.DEFAULT_RETAIL / "quest.xml", MODULE.DEFAULT_RETAIL / "npcs.xml", MODULE.DEFAULT_RETAIL / "Items.xml",
			MODULE.DEFAULT_RETAIL_SCRIPT, MODULE.DEFAULT_QUEST_DATA, enabled)

		self.assertEqual({}, skipped)
		self.assertEqual({50008, 51008}, set(quests))
		content, unresolved, generated = MODULE.render(
			{}, quests, {}, {}, {},
			{"lc1_christmasevent_santa": (831032,), "dc1_christmasevent_santa": (831033,),
				"christmasevent_shugo_assistant_light_20_n": (219290,),
				"christmasevent_shugo_assistant_dark_20_n": (219291,)}, {}, {}, ())
		self.assertEqual({}, unresolved)
		self.assertEqual(2, generated["compiled_script_christmas_courier_hunt"])
		xml = {int(node.attrib["id"]): node for node in ET.fromstring(content.split(b"-->\n", 1)[1])}
		for quest_id, npc_id, target_id in ((50008, "831032", "219290"), (51008, "831033", "219291")):
			self.assertEqual("monster_hunt", xml[quest_id].tag)
			self.assertEqual(npc_id, xml[quest_id].attrib["start_npc_ids"])
			self.assertNotIn("end_npc_ids", xml[quest_id].attrib)
			self.assertEqual({"var": "0", "end_var": "2", "npc_ids": target_id}, xml[quest_id][0].attrib)

	def test_compiled_arena_item_plays_close_item_swap_and_use_chain(self):
		enabled = MODULE.current_quest_ids(MODULE.DEFAULT_QUEST_DATA)
		quests, skipped = MODULE.compiled_arena_item_plays(
			MODULE.DEFAULT_RETAIL / "quest.xml", MODULE.DEFAULT_RETAIL / "Quest_SimpleItemPlay.xml",
			MODULE.DEFAULT_RETAIL / "npcs.xml", MODULE.DEFAULT_RETAIL / "Items.xml",
			MODULE.DEFAULT_RETAIL_SCRIPT, MODULE.DEFAULT_QUEST_DATA, enabled)

		self.assertEqual({}, skipped)
		self.assertEqual({18213, 28213}, set(quests))
		self.assertEqual(("quest_18213a", 1), quests[18213]["start_give_item"])
		self.assertEqual(("quest_18213b", 1), quests[18213]["steps"][1]["give_item"])
		self.assertEqual(("quest_18213a", 1), quests[18213]["steps"][1]["remove_item"])
		self.assertEqual("quest_18213b", quests[18213]["steps"][2]["item"])
		content, unresolved, generated = MODULE.render(
			{}, {}, {}, {}, quests,
			{"junos": (205985,), "inggril": (205316,), "romedon": (798604,),
				"shinin": (205986,), "inggness": (205320,), "kijan": (798804,)}, {},
			{"quest_18213a": (182212219,), "quest_18213b": (182212220,),
				"quest_28213a": (182212222,), "quest_28213b": (182212223,)}, ())
		self.assertEqual({}, unresolved)
		self.assertEqual(2, generated["compiled_script_arena_item_play"])
		xml = ET.fromstring(content.split(b"-->\n", 1)[1])
		elyos = next(node for node in xml if node.attrib["id"] == "18213")
		self.assertEqual("205985", elyos.attrib["start_ids"])
		self.assertEqual("798604", elyos.attrib["end_npc_ids"])
		self.assertEqual("182212219", elyos.attrib["start_give_item_id"])
		self.assertEqual({"type": "TALK", "ids": "205316"}, elyos[0].attrib)
		self.assertEqual({"type": "TALK", "ids": "798604", "give_item_id": "182212220", "give_item_count": "1",
			"remove_item_id": "182212219", "remove_item_count": "1"}, elyos[1].attrib)
		self.assertEqual({"type": "ITEM_PLAY", "item_id": "182212220"}, elyos[2].attrib)

	def test_compiled_dredgion_control_hunts_close_object_and_kill_chain(self):
		enabled = MODULE.current_quest_ids(MODULE.DEFAULT_QUEST_DATA)
		quests, skipped = MODULE.compiled_dredgion_control_hunts(
			MODULE.DEFAULT_RETAIL / "quest.xml", MODULE.DEFAULT_RETAIL / "Quest_SimpleHunt.xml",
			MODULE.DEFAULT_RETAIL / "npcs.xml", MODULE.DEFAULT_RETAIL / "Items.xml",
			MODULE.DEFAULT_RETAIL_SCRIPT, MODULE.DEFAULT_QUEST_DATA, enabled)

		self.assertEqual({}, skipped)
		self.assertEqual({30702, 30752}, set(quests))
		self.assertEqual(["TALK", "HUNT"], [step["type"] for step in quests[30702]["steps"]])
		self.assertEqual(1, quests[30702]["steps"][1]["amount"])
		content, unresolved, generated = MODULE.render(
			{}, {}, {}, {}, quests,
			{"idtiamat_sorus": (800424,), "idtiamat_fobj_model_1": (730702,),
				"idtiamat_sardha_named_60_ah": (219354,), "idtiamat_sorus_2": (800461,)}, {}, {}, ())
		self.assertEqual({}, unresolved)
		self.assertEqual(2, generated["compiled_script_dredgion_control_hunt"])
		xml = ET.fromstring(content.split(b"-->\n", 1)[1])
		for node in xml:
			self.assertEqual("800424", node.attrib["start_ids"])
			self.assertEqual("800461", node.attrib["end_npc_ids"])
			self.assertEqual({"type": "TALK", "ids": "730702"}, node[0].attrib)
			self.assertEqual({"type": "HUNT", "ids": "219354", "amount": "1"}, node[1].attrib)

	def test_compiled_dredgion_navigation_hunts_close_serial_chain(self):
		enabled = MODULE.current_quest_ids(MODULE.DEFAULT_QUEST_DATA)
		quests, skipped = MODULE.compiled_dredgion_navigation_hunts(
			MODULE.DEFAULT_RETAIL / "quest.xml", MODULE.DEFAULT_RETAIL / "Quest_SimpleSerialHunt.xml",
			MODULE.DEFAULT_RETAIL / "npcs.xml", MODULE.DEFAULT_RETAIL / "Items.xml",
			MODULE.DEFAULT_RETAIL_SCRIPT, MODULE.DEFAULT_QUEST_DATA, enabled)

		self.assertEqual({}, skipped)
		self.assertEqual({30600, 30610}, set(quests))
		self.assertEqual(["TALK", "HUNT", "HUNT"], [step["type"] for step in quests[30600]["steps"]])
		self.assertEqual(["IDDreadgion_03_DrakanFiNamedAA_60_Ae", "IDDreadgion_03_DrakanFiNamedAB_60_Ae"], quests[30600]["steps"][1]["names"])
		content, unresolved, generated = MODULE.render(
			{}, {}, {}, {}, quests,
			{"hejitor": (800325,), "linocus": (800324,), "astella": (800327,), "aluna": (800326,),
				"iddreadgion_03_drakanfinamedaa_60_ae": (219256,), "iddreadgion_03_drakanfinamedab_60_ae": (219257,),
				"iddreadgion_03_drakanwi_boss_ah": (219264,)}, {}, {}, ())
		self.assertEqual({}, unresolved)
		self.assertEqual(2, generated["compiled_script_dredgion_navigation_hunt"])
		xml = {int(node.attrib["id"]): node for node in ET.fromstring(content.split(b"-->\n", 1)[1])}
		self.assertEqual("800325", xml[30600].attrib["start_ids"])
		self.assertEqual("800327", xml[30610].attrib["start_ids"])
		for node, talk_id in ((xml[30600], "800324"), (xml[30610], "800326")):
			self.assertEqual(node.attrib["start_ids"], node.attrib["end_npc_ids"])
			self.assertEqual({"type": "TALK", "ids": talk_id}, node[0].attrib)
			self.assertEqual({"type": "HUNT", "ids": "219256 219257", "amount": "1"}, node[1].attrib)
			self.assertEqual({"type": "HUNT", "ids": "219264", "amount": "1"}, node[2].attrib)

	def test_legacy_xml_audit_reports_every_retained_definition(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			(root / "legacy.xml").write_text(
				'<quest_scripts><report_to id="2"/><work_order id="3"/>'
				'<report_to id="4"/><report_to id="5"/><report_to id="6"/><report_to id="7"/>'
				'<report_to id="8"/></quest_scripts>')

			details, stats = MODULE.audit_legacy_xml(
				root, {3, 4, 5, 6, 7, 8}, set(), set(), {4, 5, 7, 8}, {5}, {4}, {8})

			self.assertEqual({
				"2": "outside_base",
					"3": "missing_supported_retail_source",
				"4": "unsupported_fields",
				"5": "unresolved_references",
				"6": "missing_supported_retail_source",
				"7": "unsupported_retail_shape",
				"8": "invalid_retail_definition",
			}, {quest_id: data["reason"] for quest_id, data in details.items()})
			self.assertEqual({
				"2": "not_executable_without_base",
					"3": "blocked_authoritative_source",
				"4": "blocked_extra_semantics",
				"5": "blocked_reference_resolution",
				"6": "blocked_authoritative_source",
				"7": "blocked_semantic_gap",
				"8": "blocked_invalid_retail_source",
			}, {quest_id: data["disposition"] for quest_id, data in details.items()})
			self.assertEqual({
				"files": 1, "definitions": 7, "shadowed_by_generated": 0, "retained": 7,
					"reasons": {**{reason: 1 for reason in {data["reason"] for data in details.values()}}, "missing_supported_retail_source": 2},
					"dispositions": {**{disposition: 1 for disposition in {data["disposition"] for data in details.values()}}, "blocked_authoritative_source": 2},
			}, stats)
			with self.assertRaisesRegex(ValueError, "both XML and Java owners"):
				MODULE.audit_legacy_xml(root, {3, 4, 5, 6, 7, 8}, set(), {7}, {4, 5, 7, 8}, {5}, {4}, {8})
			(root / "shadowed.xml").write_text('<quest_scripts><report_to id="1"/></quest_scripts>')
			with self.assertRaisesRegex(ValueError, "shadowed by generated XML"):
				MODULE.audit_legacy_xml(root, {1, 3, 4, 5, 6, 7, 8}, {1}, set(), {1, 4, 5, 7, 8}, {5}, {4}, {8})

	def test_repeated_talks_and_challenge_hunts_use_existing_runtime(self):
		with tempfile.TemporaryDirectory() as directory:
			path = Path(directory) / "data_driven_quest.xml"
			path.write_text(
				'<quests><quest><id>1</id><name>Q1</name><dev_name>Talk</dev_name>'
				'<category_acquire_>Talk</category_acquire_><value0_acquire_>Start</value0_acquire_><reward_npc_name>Start</reward_npc_name>'
				'<progress_info><data><category_progress_>Talk</category_progress_><value0_progress_>Middle</value0_progress_></data>'
				'<data><category_progress_>Talk</category_progress_><value0_progress_>Start</value0_progress_></data>'
				'<data><category_progress_>Talk</category_progress_><value0_progress_>Middle</value0_progress_></data></progress_info></quest>'
				'<quest><id>2</id><name>Q2</name><dev_name>Hunt</dev_name><category_acquire_>Talk</category_acquire_>'
				'<value0_acquire_>_challengetask_</value0_acquire_><reward_npc_name>End</reward_npc_name>'
				'<progress_info><data><category_progress_>Hunt</category_progress_><value0_progress_>Mob 10;</value0_progress_></data>'
				'</progress_info></quest></quests>')

			talks, _, _ = MODULE.data_driven_talks(path, {1})
			hunts, _, _ = MODULE.data_driven_hunts(path, {2})
			self.assertEqual("data_driven_simple", talks[1]["kind"])
			self.assertEqual(["Middle", "Start", "Middle"], [step["names"][0] for step in talks[1]["steps"]])
			self.assertIsNone(hunts[2]["start"])

			content, unresolved, _ = MODULE.render(
				{}, hunts, {}, {}, {1: talks[1]}, {"start": (10,), "middle": (11,), "end": (12,), "mob": (20,)}, {}, {}, tuple())
			quests = {int(node.attrib["id"]): node for node in ET.fromstring(content.split(b"-->\n", 1)[1])}
			self.assertEqual("data_driven_quest", quests[1].tag)
			self.assertEqual(["11", "10", "11"], [step.attrib["ids"] for step in quests[1]])
			self.assertEqual("0", quests[2].attrib["start_npc_ids"])
			self.assertEqual({}, unresolved)

	def test_generates_only_supported_enabled_resolved_hunts(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			(root / "quest_data.xml").write_text(
				'<quests><quest id="1"/><quest id="2"/><quest id="4"/><quest id="5"/><quest id="6"/><quest id="7"/><quest id="8"/><quest id="9"/><quest id="10"/><quest id="11"/><quest id="12"/><quest id="13"/><quest id="14"/><quest id="15"/>'
				'<quest id="16" category="TASK" combine_skillpoint="1"><collect_items><collect_item item_id="42" count="3"/></collect_items>'
				'<quest_work_items><quest_work_item item_id="43" count="4"/></quest_work_items></quest></quests>')
			(root / "Quest_SimpleHunt.xml").write_text(
				'<quest_simplehunts><id id="1"><acquired_npc_name>Start</acquired_npc_name>'
				'<count1>3</count1><monster1>MobA, MobB</monster1><reward_npc_name>End</reward_npc_name></id>'
				'<id id="2"><acquired_npc_name>Start</acquired_npc_name><mobile_event>1</mobile_event>'
				'<count1>1</count1><monster1>MobA</monster1><reward_npc_name>End</reward_npc_name></id>'
				'<id id="3"><acquired_npc_name>Start</acquired_npc_name>'
				'<count1>1</count1><monster1>MobA</monster1><reward_npc_name>End</reward_npc_name></id>'
				'</quest_simplehunts>')
			(root / "Quest_SimpleTalk.xml").write_text(
				'<quest_simpletalks><id id="4"><acquired_npc_name>Start</acquired_npc_name>'
				'<reward_npc_name>End</reward_npc_name></id>'
				'<id id="6"><acquired_npc_name>Start</acquired_npc_name><talk_npc1>Middle</talk_npc1>'
				'<reward_npc_name>End</reward_npc_name></id></quest_simpletalks>')
			(root / "Quest_SimpleCollectItem.xml").write_text(
				'<quest_simplecollectitems><id id="5"><acquired_npc_name>Start</acquired_npc_name>'
				'<object1>Object</object1><reward_npc_name>End</reward_npc_name></id></quest_simplecollectitems>')
			(root / "Quest_SimpleUseItem.xml").write_text(
				'<quest_simpleuseitems><id id="7"><use_item_name>ITEM_StartItem</use_item_name>'
				'<talk_npc1>Start</talk_npc1><reward_npc_name>End</reward_npc_name></id></quest_simpleuseitems>')
			(root / "Quest_SimpleItemPlay.xml").write_text('<quest_simpleitemplays/>')
			(root / "Quest_SimpleSerialHunt.xml").write_text('<quest_simpleserialhunts/>')
			(root / "Quest_CombineTask.xml").write_text(
				'<quest_combinetasks><id id="16"><dev_name>Work order</dev_name><task_npc>CrafterA,CrafterB</task_npc>'
				'<combineskill>weaponsmith</combineskill><combine_skillpoint>1</combine_skillpoint><recipe_name>r_q16</recipe_name>'
				'<product>WorkProduct 3</product><give_component1>WorkComponent 4</give_component1></id></quest_combinetasks>')
			(root / "data_driven_quest.xml").write_text(
				'<quest_data_drivens><quest_data_driven><id>8</id><name>Q8</name><dev_name>Report</dev_name>'
				'<category_acquire_>Talk</category_acquire_><value0_acquire_>Start</value0_acquire_>'
				'<reward_npc_name>End</reward_npc_name></quest_data_driven>'
				'<quest_data_driven><id>9</id><name>Q9</name><dev_name>Talk</dev_name>'
				'<category_acquire_>Talk</category_acquire_><value0_acquire_>Start</value0_acquire_>'
				'<reward_npc_name>End</reward_npc_name><progress_info><data><category_progress_>Talk</category_progress_>'
				'<value0_progress_>Middle</value0_progress_></data><data><category_progress_>Talk</category_progress_>'
				'<value0_progress_>Middle</value0_progress_></data></progress_info></quest_data_driven>'
				'<quest_data_driven><id>10</id><name>Q10</name><dev_name>Hunt</dev_name><con_quest>11</con_quest>'
				'<category_acquire_>Talk</category_acquire_><value0_acquire_>GroupedStart</value0_acquire_>'
				'<reward_npc_name>End</reward_npc_name><progress_info><data><category_progress_>Hunt</category_progress_>'
				'<value0_progress_>MobA 4; MobB 2;</value0_progress_></data></progress_info></quest_data_driven>'
					'<quest_data_driven><id>11</id><name>Q11</name><dev_name>Collect</dev_name>'
					'<category_acquire_>Talk</category_acquire_><value0_acquire_>Start</value0_acquire_>'
					'<reward_npc_name>End</reward_npc_name><progress_info><data><category_progress_>CollectItem</category_progress_>'
					'<value0_progress_>End</value0_progress_></data></progress_info></quest_data_driven>'
					'<quest_data_driven><id>12</id><name>Q12</name><dev_name>PVP</dev_name>'
					'<category_acquire_>Talk</category_acquire_><value0_acquire_>Start</value0_acquire_>'
					'<reward_npc_name>End</reward_npc_name><progress_info><data><category_progress_>PVP</category_progress_>'
					'<value0_progress_>3</value0_progress_></data></progress_info></quest_data_driven>'
					'<quest_data_driven><id>13</id><name>Q13</name><dev_name>ItemPlay</dev_name>'
						'<category_acquire_>ItemPlay</category_acquire_><value0_acquire_>DataItem</value0_acquire_>'
						'<reward_npc_name>End</reward_npc_name></quest_data_driven>'
						'<quest_data_driven><id>14</id><name>Q14</name><dev_name>PassiveTalk</dev_name>'
						'<category_acquire_>none</category_acquire_><reward_npc_name>End</reward_npc_name>'
						'<progress_info><data><category_progress_>Talk</category_progress_><value0_progress_>Middle</value0_progress_></data></progress_info></quest_data_driven>'
						'<quest_data_driven><id>15</id><name>Q15</name><dev_name>PassiveHunt</dev_name>'
						'<category_acquire_>none</category_acquire_><reward_npc_name>End</reward_npc_name>'
						'<progress_info><data><category_progress_>Hunt</category_progress_><value0_progress_>MobA 2;</value0_progress_></data></progress_info>'
						'</quest_data_driven></quest_data_drivens>')
			(root / "Items.xml").write_text('<items><item><id>40</id><name>StartItem</name></item><item><id>41</id><name>DataItem</name></item><item><id>42</id><name>WorkProduct</name></item><item><id>43</id><name>WorkComponent</name></item></items>')
			(root / "quest.xml").write_text(
				'<quests><quest><id>16</id><category1>task</category1><quest_work_item1>WorkComponent 4</quest_work_item1>'
				'<collect_item1>WorkProduct 3</collect_item1><combineskill>weaponsmith</combineskill><combine_skillpoint>1</combine_skillpoint>'
				'<recipe_name>r_q16</recipe_name><check_item1_1>WorkProduct 3</check_item1_1><race_permitted>pc_light</race_permitted></quest></quests>')
			(root / "combine_recipe.xml").write_text(
				'<combine_recipes><combine_recipe><id>155000016</id><name>r_q16</name><combineskill>weaponsmith</combineskill>'
				'<qualification_race>pc_light</qualification_race><required_skillpoint>1</required_skillpoint><task_type>1</task_type>'
				'<product>WorkProduct</product><product_quantity>1</product_quantity><combine_recipe_expansion><data>'
				'<component1>WorkComponent</component1></data></combine_recipe_expansion></combine_recipe></combine_recipes>')
			(root / "npcs.xml").write_text(
				'<npcs><npc><id>10</id><name>Start</name></npc><npc><id>11</id><name>End</name></npc><npc><id>12</id><name>Middle</name></npc>'
				'<npc><id>13</id><name>GroupedStartA</name><quest_ai_name>GroupedStart</quest_ai_name></npc>'
				'<npc><id>14</id><name>GroupedStartB</name><quest_ai_name>GroupedStart</quest_ai_name></npc>'
				'<npc><id>20</id><name>MobA</name></npc><npc><id>21</id><name>AliasTarget</name><quest_ai_name>MobB</quest_ai_name></npc>'
				'<npc><id>30</id><name>Object</name></npc><npc><id>50</id><name>CrafterA</name></npc><npc><id>51</id><name>CrafterB</name></npc></npcs>')

			legacy_dir = root / "scripts"
			legacy_dir.mkdir()
			content, report = MODULE.generate(root, root / "quest_data.xml", root / "missing-script", legacy_dir)
			stats = report["stats"]
			xml = ET.fromstring(content.split(b"-->\n", 1)[1])
			quests = {int(node.attrib["id"]): node for node in xml}
			self.assertEqual("item_collecting", quests[5].tag)
			self.assertEqual("30", quests[5].attrib["action_item_ids"])
			self.assertEqual("report_to", quests[4].tag)
			self.assertEqual("report_to", quests[8].tag)
			self.assertEqual("report_to_many", quests[6].tag)
			self.assertEqual({"var": "0", "npc_id": "12", "quest_dialog": "1352"}, quests[6][0].attrib)
			self.assertEqual("data_driven_quest", quests[9].tag)
			self.assertEqual(["12", "12"], [step.attrib["ids"] for step in quests[9]])
			self.assertEqual({"id": "1", "retail": "true", "start_npc_ids": "10", "end_npc_ids": "11"}, quests[1].attrib)
			self.assertEqual({"var": "0", "end_var": "3", "npc_ids": "20 21"}, quests[1][0].attrib)
			self.assertEqual({"id": "10", "retail": "true", "start_npc_ids": "13 14", "end_npc_ids": "11"}, quests[10].attrib)
			self.assertEqual({"var": "0", "end_var": "4", "npc_ids": "20"}, quests[10][0].attrib)
			self.assertEqual({"var": "1", "end_var": "2", "npc_ids": "21"}, quests[10][1].attrib)
			self.assertEqual("item_order", quests[7].tag)
			self.assertEqual("40", quests[7].attrib["start_item_id"])
			self.assertEqual("10", quests[7].attrib["talk_npc_id1"])
			self.assertEqual("item_collecting", quests[11].tag)
			self.assertEqual("10000", quests[11].attrib["check_ok_dialog_id"])
			self.assertEqual("10001", quests[11].attrib["check_fail_dialog_id"])
			self.assertEqual("10002", quests[11].attrib["reward_dialog_id"])
			self.assertEqual("kill_in_world", quests[12].tag)
			self.assertEqual("3", quests[12].attrib["amount"])
			self.assertEqual("10002", quests[12].attrib["reward_dialog_id"])
			self.assertEqual("41", quests[13].attrib["start_item_id"])
			self.assertNotIn("start_npc_ids", quests[14].attrib)
			self.assertEqual("11", quests[14].attrib["end_npc_ids"])
			self.assertEqual("0", quests[15].attrib["start_npc_ids"])
			self.assertEqual("11", quests[15].attrib["end_npc_ids"])
			self.assertEqual("work_order", quests[16].tag)
			self.assertEqual({"id": "16", "retail": "true", "start_npc_ids": "50 51", "recipe_id": "155000016"}, quests[16].attrib)
			self.assertEqual({"item_id": "43", "count": "4"}, quests[16][0].attrib)
			self.assertEqual(14, stats["generated"]["total"])
			self.assertEqual(1, stats["generated"]["work_order"])
			self.assertEqual(1, stats["generated"]["data_driven_report"])
			self.assertEqual(2, stats["generated"]["data_driven_talk"])
			self.assertEqual(2, stats["generated"]["data_driven_hunt"])
			self.assertEqual(1, stats["generated"]["data_driven_collect"])
			self.assertEqual(1, stats["generated"]["data_driven_pvp"])
			self.assertEqual(1, stats["generated"]["data_driven_item_play"])
			self.assertEqual({"base": 15, "executable": 14, "isolated": 1, "managed": 15}, stats["coverage"])
			self.assertEqual({"generated_xml": 14, "legacy_xml": 0, "selected_xml": 14, "java_handlers": 0, "overlap": 0}, stats["ownership"])
			self.assertEqual({"2": "missing_retail_quest"}, report["skipped"]["isolated"])
			self.assertEqual(1, stats["unsupported"])
			self.assertEqual(1, stats["missing_base"])
			self.assertEqual({"2": ["mobile_event"]}, report["skipped"]["unsupported"]["hunt"])

			(root / "combine_recipe.xml").write_text((root / "combine_recipe.xml").read_text().replace(
				"<required_skillpoint>1</required_skillpoint>", "<required_skillpoint>2</required_skillpoint>"))
			work_orders, _, skipped = MODULE.retail_work_orders(
				root / "Quest_CombineTask.xml", root / "quest.xml", root / "quest_data.xml", root / "npcs.xml",
				root / "Items.xml", root / "combine_recipe.xml", {16})
			self.assertEqual({}, work_orders)
			self.assertIn("recipe_fields", skipped["16"])

	def test_compiled_item_buyers_require_complete_retail_evidence(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			script_root = root / "fun"
			script_root.mkdir()
			quest_nodes = []
			base_nodes = []
			for quest_id, (_, _, reward_gold, race) in MODULE.COMPILED_ITEM_BUYERS.items():
				quest_nodes.append(
					f"<quest><id>{quest_id}</id><name>Q{quest_id}</name><max_repeat_count>255</max_repeat_count>"
					f"<cannot_share>1</cannot_share><collect_progress>0</collect_progress>"
					f"<collect_item1>exceed_enchant_key_01 1</collect_item1>"
					f"<inventory_item_name1>exceed_enchant_key_01</inventory_item_name1>"
					f"<check_item1_1>exceed_enchant_key_01 1</check_item1_1>"
					f"<reward_gold1>{reward_gold}</reward_gold1><race_permitted>{race}</race_permitted></quest>"
				)
				base_race = "ELYOS" if quest_id < 20000 else "ASMODIANS"
				base_nodes.append(
					f'<quest id="{quest_id}" race_permitted="{base_race}"><collect_items>'
					f'<collect_item item_id="166500002" count="1"/></collect_items><inventory_items>'
					f'<inventory_item item_id="166500002"/></inventory_items><rewards gold="{reward_gold}"/></quest>'
				)
			(root / "quest.xml").write_text(f"<quests>{''.join(quest_nodes)}</quests>")
			(root / "quest_data.xml").write_text(f"<quests>{''.join(base_nodes)}</quests>")
			self.write_compiled_buyer_scripts(script_root)

			quests, skipped = MODULE.compiled_item_buyers(
				root / "quest.xml", script_root, root / "quest_data.xml", set(MODULE.COMPILED_ITEM_BUYERS))
			self.assertEqual(set(MODULE.COMPILED_ITEM_BUYERS), set(quests))
			self.assertEqual({}, skipped)
			content, unresolved, generated = MODULE.render(
				quests, {}, {}, {}, {},
				{npc.casefold(): (805700 + index,) for index, (npc, _, _, _) in enumerate(MODULE.COMPILED_ITEM_BUYERS.values())},
				{}, {"exceed_enchant_key_01": (166500002,)}, tuple(),
			)
			xml = ET.fromstring(content.split(b"-->\n", 1)[1])
			self.assertEqual(6, generated["compiled_script_collect"])
			self.assertEqual({}, unresolved)
			for node in xml:
				self.assertEqual("item_collecting", node.tag)
				self.assertNotIn("item_id", node.attrib)

			turn_in = script_root / "fun_882.cpp"
			turn_in.write_text(turn_in.read_text().replace(",0x4a87,0,1)", ",0x4a87,0,2)"))
			quests, skipped = MODULE.compiled_item_buyers(
				root / "quest.xml", script_root, root / "quest_data.xml", set(MODULE.COMPILED_ITEM_BUYERS))
			self.assertNotIn(19079, quests)
			self.assertIn("script:turn_in", skipped["19079"])

	def test_compiled_firework_reports_require_timed_retail_evidence(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			script_root = root / "fun"
			script_root.mkdir()
			quest_nodes = []
			base_nodes = []
			for quest_id, (_, _, _, race) in MODULE.COMPILED_FIREWORK_REPORTS.items():
				quest_nodes.append(
					f"<quest><id>{quest_id}</id><name>Q{quest_id}</name><max_repeat_count>255</max_repeat_count>"
					f"<quest_repeat_cycle>all</quest_repeat_cycle><reward_exp1>0</reward_exp1><reward_gold1>0</reward_gold1>"
					f"<reward_glory_point1>10</reward_glory_point1><race_permitted>{race}</race_permitted></quest>"
				)
				base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
				base_nodes.append(
					f'<quest id="{quest_id}" race_permitted="{base_race}" max_repeat_count="255" repeat_cycle="ALL">'
					'<rewards gp="10"/></quest>'
				)
			(root / "quest.xml").write_text(f"<quests>{''.join(quest_nodes)}</quests>")
			(root / "quest_data.xml").write_text(f"<quests>{''.join(base_nodes)}</quests>")
			self.write_compiled_firework_scripts(script_root)

			quests, skipped = MODULE.compiled_firework_reports(
				root / "quest.xml", script_root, root / "quest_data.xml", set(MODULE.COMPILED_FIREWORK_REPORTS))
			self.assertEqual(set(MODULE.COMPILED_FIREWORK_REPORTS), set(quests))
			self.assertEqual({}, skipped)
			npc_names = [name for start, talks, _, _ in MODULE.COMPILED_FIREWORK_REPORTS.values() for name in (start, *talks)]
			content, unresolved, generated = MODULE.render(
				quests, {}, {}, {}, {}, {name: (800000 + index,) for index, name in enumerate(npc_names)}, {}, {}, tuple())
			xml = ET.fromstring(content.split(b"-->\n", 1)[1])
			self.assertEqual({}, unresolved)
			self.assertEqual(2, generated["compiled_script_timed_report"])
			for node in xml:
				self.assertEqual("report_to_many", node.tag)
				self.assertEqual("120", node.attrib["timeout_seconds"])
				self.assertEqual("1", node.attrib["timeout_start_var"])
				self.assertEqual("0", node.attrib["timeout_reset_var"])
				self.assertEqual(["0", "1"], [step.attrib["var"] for step in node])
				self.assertEqual(["1352", "1693"], [step.attrib["quest_dialog"] for step in node])

			first_talk = script_root / "fun_894.cpp"
			first_talk.write_text(first_talk.read_text().replace("120000", "119999", 1))
			quests, skipped = MODULE.compiled_firework_reports(
				root / "quest.xml", script_root, root / "quest_data.xml", set(MODULE.COMPILED_FIREWORK_REPORTS))
			self.assertNotIn(80761, quests)
			self.assertIn("script:first_talk_and_timer", skipped["80761"])

	def test_compiled_debris_rescues_require_action_and_reward_evidence(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			script_root = root / "fun"
			script_root.mkdir()
			quest_nodes = []
			base_nodes = []
			item_nodes = []
			for quest_id, (_quest_hex, race, reward_gold, reward_names, reward_ids, *_callbacks) in MODULE.COMPILED_DEBRIS_RESCUES.items():
				retail_rewards = "".join(
					f"<selectable_reward_item1_{index}>{name} 15</selectable_reward_item1_{index}>"
					for index, name in enumerate(reward_names, 1))
				quest_nodes.append(
					f"<quest><id>{quest_id}</id><name>Q{quest_id}</name><max_repeat_count>1</max_repeat_count>"
					f"<reward_exp1>6907092</reward_exp1><reward_gold1>{reward_gold}</reward_gold1>{retail_rewards}"
					f"<race_permitted>{race}</race_permitted></quest>"
				)
				base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
				base_rewards = "".join(
					f'<selectable_reward_item item_id="{item_id}" count="15"/>' for item_id in reward_ids)
				base_nodes.append(
					f'<quest id="{quest_id}" race_permitted="{base_race}" max_repeat_count="1">'
					f'<rewards gold="{reward_gold}" exp="6907092">{base_rewards}</rewards></quest>')
				item_nodes.extend(f"<item><id>{item_id}</id><name>{name}</name></item>" for name, item_id in zip(reward_names, reward_ids))
			(root / "quest.xml").write_text(f"<quests>{''.join(quest_nodes)}</quests>")
			(root / "quest_data.xml").write_text(f"<quests>{''.join(base_nodes)}</quests>")
			(root / "Items.xml").write_text(f"<items>{''.join(item_nodes)}</items>")
			(root / "npcs.xml").write_text("<npcs>" + "".join(
				f"<npc><id>{npc_id}</id><name>{name}</name></npc>" for name, npc_id in MODULE.COMPILED_DEBRIS_NPCS.items()) + "</npcs>")
			self.write_compiled_debris_scripts(script_root)

			quests, skipped = MODULE.compiled_debris_rescues(
				root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_DEBRIS_RESCUES))
			self.assertEqual(set(MODULE.COMPILED_DEBRIS_RESCUES), set(quests))
			self.assertEqual({}, skipped)
			content, unresolved, generated = MODULE.render(
				{}, {}, {}, {}, quests,
				{name.casefold(): (npc_id,) for name, npc_id in MODULE.COMPILED_DEBRIS_NPCS.items()}, {}, {}, tuple())
			xml = ET.fromstring(content.split(b"-->\n", 1)[1])
			self.assertEqual({}, unresolved)
			self.assertEqual(2, generated["compiled_script_action_talk"])
			for node in xml:
				self.assertEqual({"id": node.attrib["id"], "retail": "true", "start_type": "TALK", "end_npc_ids": "205438", "start_ids": "205438"}, node.attrib)
				self.assertEqual({"type": "TALK", "ids": "799541", "action_ids": "701097", "delete_action_target": "true"}, node[0].attrib)

			binding = script_root / "fun_667.cpp"
			binding.write_text(binding.read_text().replace("3,0,&LAB_180f12fc0", "3,0,&LAB_broken", 1))
			quests, skipped = MODULE.compiled_debris_rescues(
				root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_DEBRIS_RESCUES))
			self.assertNotIn(30503, quests)
			self.assertIn("script:debris_callback_binding", skipped["30503"])

	def test_compiled_world_collects_require_world_and_drop_evidence(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			script_root = root / "fun"
			script_root.mkdir()
			quest_nodes = []
			base_nodes = []
			items = {}
			npcs = {}
			for quest_id, (_quest_hex, race, drops) in MODULE.COMPILED_WORLD_COLLECTS.items():
				retail_fields = []
				collect_nodes = []
				drop_nodes = []
				for index, (npc_name, npc_id, item_name, item_id) in enumerate(drops, 1):
					retail_fields.extend((
						f"<collect_item{index}>{item_name} 1</collect_item{index}>",
						f"<drop_monster_{index}>{npc_name}</drop_monster_{index}>",
						f"<drop_item_{index}>{item_name}</drop_item_{index}><drop_prob_{index}>100</drop_prob_{index}>",
						f"<drop_each_member_{index}>1</drop_each_member_{index}>",
					))
					collect_nodes.append(f'<collect_item item_id="{item_id}" count="1"/>')
					drop_nodes.append(f'<quest_drop npc_id="{npc_id}" item_id="{item_id}" chance="100" drop_each_member="1"/>')
					items[item_name] = item_id
					npcs[npc_name] = npc_id
				quest_nodes.append(
					f"<quest><id>{quest_id}</id><name>Q{quest_id}</name><max_repeat_count>255</max_repeat_count>"
					f"<client_level>1</client_level><minlevel_permitted>1</minlevel_permitted>{''.join(retail_fields)}"
					f"<race_permitted>{race}</race_permitted></quest>")
				base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
				base_nodes.append(
					f'<quest id="{quest_id}" race_permitted="{base_race}" max_repeat_count="255">'
					f"<collect_items>{''.join(collect_nodes)}</collect_items>{''.join(drop_nodes)}</quest>")
			(root / "quest.xml").write_text(f"<quests>{''.join(quest_nodes)}</quests>")
			(root / "quest_data.xml").write_text(f"<quests>{''.join(base_nodes)}</quests>")
			(root / "Items.xml").write_text("<items>" + "".join(
				f"<item><id>{item_id}</id><name>{name}</name></item>" for name, item_id in items.items()) + "</items>")
			(root / "npcs.xml").write_text("<npcs>" + "".join(
				f"<npc><id>{npc_id}</id><name>{name}</name></npc>" for name, npc_id in npcs.items()) + "</npcs>")
			self.write_compiled_world_collect_scripts(script_root)

			quests, skipped = MODULE.compiled_world_collects(
				root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_WORLD_COLLECTS))
			self.assertEqual(set(MODULE.COMPILED_WORLD_COLLECTS), set(quests))
			self.assertEqual({}, skipped)
			content, unresolved, generated = MODULE.render({}, {}, {}, {}, quests, {}, {}, {}, tuple())
			xml = ET.fromstring(content.split(b"-->\n", 1)[1])
			self.assertEqual({}, unresolved)
			self.assertEqual(4, generated["compiled_script_world_collect"])
			for node in xml:
				self.assertEqual({
					"id": node.attrib["id"], "retail": "true", "start_type": "WORLD_ACTIVE",
					"world_id": str(MODULE.COMPILED_WORLD_COLLECT_WORLD_ID),
				}, node.attrib)

			source = script_root / "fun_873.cpp"
			source.write_text(source.read_text().replace(
				"param_2 + 0x14) == 0x11e329a0", "param_2 + 0x14) == 0x11e329a1", 1))
			quests, skipped = MODULE.compiled_world_collects(
				root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_WORLD_COLLECTS))
			self.assertNotIn(3219, quests)
			self.assertIn("script:world_lifecycle", skipped["3219"])

	def test_compiled_growth_quests_require_item_event_evidence(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			script_root = root / "fun"
			script_root.mkdir()
			quest_nodes = []
			base_nodes = []
			npcs = {}
			items = {}
			for quest_id, (npc_name, npc_id, _quest_hex, race, inventory_name, inventory_id, reward_name, reward_id, _callback) in MODULE.COMPILED_GROWTH_QUESTS.items():
				quest_nodes.append(
					f"<quest><id>{quest_id}</id><name>Q{quest_id}</name><max_repeat_count>255</max_repeat_count>"
					f"<minlevel_permitted>66</minlevel_permitted><cannot_share>1</cannot_share>"
					f"<inventory_item_name1>{inventory_name}</inventory_item_name1><quest_cooltime>2592000</quest_cooltime>"
					f"<reward_exp1>600000000</reward_exp1><reward_item1_1>{reward_name} 1</reward_item1_1>"
					f"<race_permitted>{race}</race_permitted></quest>")
				base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
				base_nodes.append(
					f'<quest id="{quest_id}" race_permitted="{base_race}" max_repeat_count="255" quest_cooltime="2592000">'
					f'<inventory_items><inventory_item item_id="{inventory_id}"/></inventory_items>'
					f'<rewards exp="600000000"><reward_item item_id="{reward_id}" count="1"/></rewards></quest>')
				npcs[npc_name] = npc_id
				items[inventory_name] = inventory_id
				items[reward_name] = reward_id
			(root / "quest.xml").write_text(f"<quests>{''.join(quest_nodes)}</quests>")
			(root / "quest_data.xml").write_text(f"<quests>{''.join(base_nodes)}</quests>")
			(root / "npcs.xml").write_text("<npcs>" + "".join(
				f"<npc><id>{npc_id}</id><name>{name}</name></npc>" for name, npc_id in npcs.items()) + "</npcs>")
			(root / "Items.xml").write_text("<items>" + "".join(
				f"<item><id>{item_id}</id><name>{name}</name></item>" for name, item_id in items.items()) + "</items>")
			self.write_compiled_growth_scripts(script_root)

			quests, skipped = MODULE.compiled_growth_quests(
				root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_GROWTH_QUESTS))
			self.assertEqual(set(MODULE.COMPILED_GROWTH_QUESTS), set(quests))
			self.assertEqual({}, skipped)
			content, unresolved, generated = MODULE.render(
				{}, {}, {}, {}, quests,
				{name.casefold(): (npc_id,) for name, npc_id in npcs.items()}, {},
				{name.casefold(): (item_id,) for name, item_id in items.items()}, tuple())
			xml = ET.fromstring(content.split(b"-->\n", 1)[1])
			self.assertEqual({}, unresolved)
			self.assertEqual(4, generated["compiled_script_get_item"])
			for node in xml:
				data = MODULE.COMPILED_GROWTH_QUESTS[int(node.attrib["id"])]
				self.assertEqual({
					"id": node.attrib["id"], "retail": "true", "start_type": "TALK",
					"end_npc_ids": str(data[1]), "start_ids": str(data[1]),
				}, node.attrib)
				self.assertEqual({"type": "GET_ITEM", "item_id": str(data[7])}, node[0].attrib)

			callback = script_root / "fun_876.cpp"
			callback.write_text(callback.read_text().replace("+ 0x100", "+ 0x101", 1))
			quests, skipped = MODULE.compiled_growth_quests(
				root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_GROWTH_QUESTS))
			self.assertNotIn(19678, quests)
			self.assertIn("script:item_event_transition", skipped["19678"])

	def test_compiled_sensory_completes_require_trigger_and_finish_evidence(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			script_root = root / "fun"
			script_root.mkdir()
			quest_nodes = []
			base_nodes = []
			npcs = {}
			for quest_id, (npc_name, npc_id, _quest_hex, race, prerequisite) in MODULE.COMPILED_SENSORY_COMPLETES.items():
				quest_nodes.append(
					f"<quest><id>{quest_id}</id><name>Q{quest_id}</name><max_repeat_count>1</max_repeat_count>"
					f"<minlevel_permitted>50</minlevel_permitted><finished_quest_cond1>Q{prerequisite}</finished_quest_cond1>"
					f"<reward_exp1>0</reward_exp1><reward_gold1>0</reward_gold1>"
					f"<race_permitted>{race}</race_permitted></quest>")
				base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
				base_nodes.append(
					f'<quest id="{quest_id}" race_permitted="{base_race}" max_repeat_count="1">'
					f'<start_conditions><finished quest_id="{prerequisite}"/></start_conditions></quest>')
				npcs[npc_name] = npc_id
			(root / "quest.xml").write_text(f"<quests>{''.join(quest_nodes)}</quests>")
			(root / "quest_data.xml").write_text(f"<quests>{''.join(base_nodes)}</quests>")
			(root / "npcs.xml").write_text("<npcs>" + "".join(
				f"<npc><id>{npc_id}</id><name>{name}</name></npc>" for name, npc_id in npcs.items()) + "</npcs>")
			self.write_compiled_sensory_scripts(script_root)

			quests, skipped = MODULE.compiled_sensory_completes(
				root / "quest.xml", root / "npcs.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_SENSORY_COMPLETES))
			self.assertEqual(set(MODULE.COMPILED_SENSORY_COMPLETES), set(quests))
			self.assertEqual({}, skipped)
			content, unresolved, generated = MODULE.render(
				{}, {}, {}, {}, quests, {name.casefold(): (npc_id,) for name, npc_id in npcs.items()}, {}, {}, tuple())
			xml = ET.fromstring(content.split(b"-->\n", 1)[1])
			self.assertEqual({}, unresolved)
			self.assertEqual(2, generated["compiled_script_sensory_complete"])
			for node in xml:
				self.assertEqual({
					"id": node.attrib["id"], "retail": "true", "start_type": "SENSORY_COMPLETE",
					"start_ids": str(MODULE.COMPILED_SENSORY_COMPLETES[int(node.attrib["id"])][1]),
				}, node.attrib)
				self.assertEqual(0, len(node))

			finish = script_root / "fun_875.cpp"
			finish.write_text(finish.read_text().replace("+ 0x268", "+ 0x269", 1))
			quests, skipped = MODULE.compiled_sensory_completes(
				root / "quest.xml", root / "npcs.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_SENSORY_COMPLETES))
			self.assertNotIn(3959, quests)
			self.assertIn("script:auto_finish", skipped["3959"])

	def test_compiled_paios_rescues_require_action_reset_and_turn_in_evidence(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			script_root = root / "fun"
			script_root.mkdir()
			quest_nodes = []
			base_nodes = []
			for quest_id, (_quest_hex, race, gold, prerequisite, *_rest) in MODULE.COMPILED_PAIOS_RESCUES.items():
				quest_nodes.append(
					f"<quest><id>{quest_id}</id><name>Q{quest_id}</name><minlevel_permitted>60</minlevel_permitted>"
					f"<max_repeat_count>1</max_repeat_count><finished_quest_cond1>Q{prerequisite}</finished_quest_cond1>"
					f"<reward_exp1>6907092</reward_exp1><reward_gold1>{gold}</reward_gold1>"
					+ "".join(f"<selectable_reward_item1_{index}>{name} 15</selectable_reward_item1_{index}>" for index, (name, _) in enumerate(MODULE.COMPILED_PAIOS_REWARDS, 1))
					+ f"<race_permitted>{race}</race_permitted></quest>")
				base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
				base_nodes.append(
					f'<quest id="{quest_id}" minlevel_permitted="60" max_repeat_count="1" race_permitted="{base_race}">'
					f'<rewards gold="{gold}" exp="6907092">'
					+ "".join(f'<selectable_reward_item item_id="{item_id}" count="15"/>' for _, item_id in MODULE.COMPILED_PAIOS_REWARDS)
					+ f'</rewards><start_conditions><finished quest_id="{prerequisite}"/></start_conditions></quest>')
			(root / "quest.xml").write_text(f"<quests>{''.join(quest_nodes)}</quests>")
			(root / "quest_data.xml").write_text(f"<quests>{''.join(base_nodes)}</quests>")
			(root / "npcs.xml").write_text("<npcs>" + "".join(
				f"<npc><id>{npc_id}</id><name>{name}</name></npc>" for name, npc_id in MODULE.COMPILED_PAIOS_NPCS.items()) + "</npcs>")
			(root / "Items.xml").write_text("<items>" + "".join(
				f"<item><id>{item_id}</id><name>{name}</name></item>" for name, item_id in MODULE.COMPILED_PAIOS_REWARDS) + "</items>")
			self.write_compiled_paios_scripts(script_root)

			quests, skipped = MODULE.compiled_paios_rescues(
				root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_PAIOS_RESCUES))
			self.assertEqual(set(MODULE.COMPILED_PAIOS_RESCUES), set(quests))
			self.assertEqual({}, skipped)
			content, unresolved, generated = MODULE.render(
				{}, {}, {}, {}, quests,
				{name.casefold(): (npc_id,) for name, npc_id in MODULE.COMPILED_PAIOS_NPCS.items()}, {}, {}, tuple())
			xml = ET.fromstring(content.split(b"-->\n", 1)[1])
			self.assertEqual({}, unresolved)
			self.assertEqual(2, generated["compiled_script_action_progress"])
			for node in xml:
				self.assertEqual({
					"id": node.attrib["id"], "retail": "true", "start_type": "TALK",
					"start_ids": "205438", "end_npc_ids": "799536", "reset_world_id": "300280000",
				}, node.attrib)
				self.assertEqual({"type": "ACTION", "action_ids": "701098"}, node[0].attrib)

			world = script_root / "fun_873.cpp"
			world.write_text(world.read_text().replace("0x11e5e8c0", "0x11e5e8c1", 1))
			quests, skipped = MODULE.compiled_paios_rescues(
				root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_PAIOS_RESCUES))
			self.assertNotIn(30504, quests)
			self.assertIn("script:world_reset", skipped["30504"])

	def test_compiled_housing_flower_visits_require_complete_talk_chain(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			script_root = root / "fun"
			script_root.mkdir()
			quest_nodes = []
			base_nodes = []
			npc_nodes = []
			blocks = []
			for quest_id, (quest_hex, retail_race, base_race, prerequisite, start_name, start_ids, middle_name, middle_id, end_name, end_id) in MODULE.COMPILED_HOUSING_FLOWERS.items():
				quest_nodes.append(
					f"<quest><id>{quest_id}</id><max_repeat_count>1</max_repeat_count><minlevel_permitted>21</minlevel_permitted>"
					f"<finished_quest_cond1>Q{prerequisite}</finished_quest_cond1><cannot_share>1</cannot_share>"
					f"<reward_exp1>12951</reward_exp1><reward_gold1>0</reward_gold1><race_permitted>{retail_race}</race_permitted></quest>")
				base_nodes.append(
					f'<quest id="{quest_id}" minlevel_permitted="21" max_repeat_count="1" race_permitted="{base_race}">'
					f'<rewards exp="12951"/><start_conditions><finished quest_id="{prerequisite}"/></start_conditions></quest>')
				npc_nodes.extend(f"<npc><id>{npc_id}</id><name>Butler{npc_id}</name><quest_ai_name>{start_name}</quest_ai_name></npc>" for npc_id in start_ids)
				npc_nodes.append(f"<npc><id>{middle_id}</id><name>{middle_name}</name></npc>")
				npc_nodes.append(f"<npc><id>{end_id}</id><name>{end_name}</name></npc>")
				blocks.extend((
					f'// @q{quest_id}_start_npc\nFUN_180cb5920(x,L"{start_name}",{quest_hex})',
					f'// @q{quest_id}_middle_npc\nFUN_180cb5920(x,L"{middle_name}",{quest_hex})',
					f'// @q{quest_id}_end_npc\nFUN_180cb5920(x,L"{end_name}",{quest_hex})',
					f"// @q{quest_id}_start_phase\nFUN_180cb3070(x,x,{quest_hex},0,0xffffffff,0)",
					f"// @q{quest_id}_middle_phase\nFUN_180cb3070(x,x,{quest_hex},3,0,0)",
					f"// @q{quest_id}_end_phase\nFUN_180cb3070(x,x,{quest_hex},3,1,0)",
					f"// @q{quest_id}_reward_phase\nFUN_180cb3070(x,x,{quest_hex},4,0xffffffff,0)",
					f"// @q{quest_id}_start_dialog\nFUN_180cab520({quest_hex},x)",
					f"// @q{quest_id}_middle_dialog\nFUN_180cabb10({quest_hex},x) FUN_180cacb30({quest_hex},x)",
					f"// @q{quest_id}_end_dialog\nFUN_180cabb10({quest_hex},x) FUN_180cacb30({quest_hex},x)",
					f"// @q{quest_id}_start_registration\n+ 0x1a8 x,{quest_hex}",
					f"// @q{quest_id}_completion\n+ 0x1b8 x,{quest_hex}",
				))
			(root / "quest.xml").write_text(f"<quests>{''.join(quest_nodes)}</quests>")
			(root / "quest_data.xml").write_text(f"<quests>{''.join(base_nodes)}</quests>")
			(root / "npcs.xml").write_text(f"<npcs>{''.join(npc_nodes)}</npcs>")
			(script_root / "fun_351.cpp").write_text("\n".join(blocks))

			quests, skipped = MODULE.compiled_housing_flower_visits(
				root / "quest.xml", root / "npcs.xml", script_root, root / "quest_data.xml",
				set(MODULE.COMPILED_HOUSING_FLOWERS))
			self.assertEqual(set(MODULE.COMPILED_HOUSING_FLOWERS), set(quests))
			self.assertEqual({}, skipped)
			exact, aliases = MODULE.npc_indexes(root / "npcs.xml", {
				name for data in MODULE.COMPILED_HOUSING_FLOWERS.values() for name in (data[4], data[6], data[8])}, set())
			content, unresolved, generated = MODULE.render({}, {}, {}, {}, quests, exact, aliases, {}, tuple())
			self.assertEqual({}, unresolved)
			self.assertEqual(2, generated["compiled_script_housing_talk"])
			for node in ET.fromstring(content.split(b"-->\n", 1)[1]):
				data = MODULE.COMPILED_HOUSING_FLOWERS[int(node.attrib["id"])]
				self.assertEqual(" ".join(map(str, data[5])), node.attrib["start_ids"])
				self.assertEqual(str(data[9]), node.attrib["end_npc_ids"])
				self.assertEqual({"type": "TALK", "ids": str(data[7])}, node[0].attrib)

			source = script_root / "fun_351.cpp"
			source.write_text(source.read_text().replace("0x4976,3,1,0)", "0x4976,3,2,0)"))
			quests, skipped = MODULE.compiled_housing_flower_visits(
				root / "quest.xml", root / "npcs.xml", script_root, root / "quest_data.xml",
				set(MODULE.COMPILED_HOUSING_FLOWERS))
			self.assertNotIn(18806, quests)
			self.assertIn("script:FUN_180cb3070", skipped["18806"])

	def test_compiled_scorched_trees_require_all_actions_and_exact_items(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			script_root = root / "fun"
			script_root.mkdir()
			quest_nodes = []
			base_nodes = []
			npc_nodes = [f"<npc><id>{npc_id}</id><name>{name}</name></npc>" for name, npc_id in MODULE.COMPILED_SCORCHED_TREE_ACTIONS]
			item_nodes = [f"<item><id>{item_id}</id><name>{name}</name></item>" for name, item_id in MODULE.COMPILED_SCORCHED_TREE_REWARDS]
			blocks = []
			for quest_id, (quest_hex, retail_race, base_race, start_name, start_id, work_items) in MODULE.COMPILED_SCORCHED_TREES.items():
				fields = [
					f"<id>{quest_id}</id>", "<max_repeat_count>1</max_repeat_count>",
					"<minlevel_permitted>65</minlevel_permitted>", "<cannot_share>1</cannot_share>",
					"<reward_exp1>3446553</reward_exp1>", "<reward_gold1>150660</reward_gold1>",
					f"<race_permitted>{retail_race}</race_permitted>",
				]
				for index, (name, item_id) in enumerate(work_items, 1):
					fields.extend((f"<quest_work_item{index}>{name} 1</quest_work_item{index}>", f"<check_item1_{index}>{name} 1</check_item1_{index}>"))
					item_nodes.append(f"<item><id>{item_id}</id><name>{name}</name></item>")
				for index, (name, _) in enumerate(MODULE.COMPILED_SCORCHED_TREE_REWARDS, 1):
					fields.append(f"<selectable_reward_item1_{index}>{name} 2</selectable_reward_item1_{index}>")
				quest_nodes.append(f"<quest>{''.join(fields)}</quest>")
				base_nodes.append(
					f'<quest id="{quest_id}" minlevel_permitted="65" max_repeat_count="1" cannot_share="true" race_permitted="{base_race}">'
					'<rewards gold="150660" exp="3446553">'
					+ ''.join(f'<selectable_reward_item item_id="{item_id}" count="2"/>' for _, item_id in MODULE.COMPILED_SCORCHED_TREE_REWARDS)
					+ '</rewards><quest_work_items>'
					+ ''.join(f'<quest_work_item item_id="{item_id}" count="1"/>' for _, item_id in work_items)
					+ '</quest_work_items></quest>')
				npc_nodes.append(f"<npc><id>{start_id}</id><name>{start_name}</name></npc>")
				for index, name in enumerate((start_name, *(name for name, _ in MODULE.COMPILED_SCORCHED_TREE_ACTIONS))):
					blocks.append(f'// @q{quest_id}_npc_{index}\nFUN_180cb5920(x,L"{name}",{quest_hex})')
				for index, phase in enumerate(((0, "0xffffffff"), (3, "0"), (3, "1"), (3, "2"), (3, "3"), (4, "0xffffffff"))):
					blocks.append(f"// @q{quest_id}_phase_{index}\nFUN_180cb3070(x,x,{quest_hex},{phase[0]},{phase[1]},0)")
				blocks.append(f"// @q{quest_id}_start\nFUN_180cab520({quest_hex},x)")
				for index in range(4):
					blocks.append(f"// @q{quest_id}_callback_{index}\nFUN_180cabb10({quest_hex},x,x,3,a,b,c,d,0)")
			(root / "quest.xml").write_text(f"<quests>{''.join(quest_nodes)}</quests>")
			(root / "quest_data.xml").write_text(f"<quests>{''.join(base_nodes)}</quests>")
			(root / "npcs.xml").write_text(f"<npcs>{''.join(npc_nodes)}</npcs>")
			(root / "Items.xml").write_text(f"<items>{''.join(item_nodes)}</items>")
			(script_root / "fun_354.cpp").write_text("\n".join(blocks))

			quests, skipped = MODULE.compiled_scorched_tree_actions(
				root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_SCORCHED_TREES))
			self.assertEqual(set(MODULE.COMPILED_SCORCHED_TREES), set(quests))
			self.assertEqual({}, skipped)
			names = {data[3] for data in MODULE.COMPILED_SCORCHED_TREES.values()} | {name for name, _ in MODULE.COMPILED_SCORCHED_TREE_ACTIONS}
			exact, aliases = MODULE.npc_indexes(root / "npcs.xml", names, set())
			content, unresolved, generated = MODULE.render({}, {}, {}, {}, quests, exact, aliases, {}, tuple())
			self.assertEqual({}, unresolved)
			self.assertEqual(2, generated["compiled_script_scorched_tree"])
			for node in ET.fromstring(content.split(b"-->\n", 1)[1]):
				data = MODULE.COMPILED_SCORCHED_TREES[int(node.attrib["id"])]
				self.assertEqual(str(data[4]), node.attrib["start_ids"])
				self.assertEqual(str(data[4]), node.attrib["end_npc_ids"])
				self.assertEqual([str(npc_id) for _, npc_id in MODULE.COMPILED_SCORCHED_TREE_ACTIONS], [step.attrib["action_ids"] for step in node])

			source = script_root / "fun_354.cpp"
			source.write_text(source.read_text().replace("FUN_180cabb10(0x35f1", "FUN_180cabba0(0x35f1", 1))
			quests, skipped = MODULE.compiled_scorched_tree_actions(
				root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root,
				root / "quest_data.xml", set(MODULE.COMPILED_SCORCHED_TREES))
			self.assertNotIn(13809, quests)
			self.assertIn("script:function_blocks", skipped["13809"])

	def test_compiled_kaldor_arrivals_require_complete_talk_chain(self):
		with tempfile.TemporaryDirectory() as directory:
			root = Path(directory)
			script_root = root / "fun"
			script_root.mkdir()
			quests_xml, base_xml, npcs, items, blocks = [], [], [], [], []
			for quest_id, (quest_hex, retail_race, base_race, start, start_id, middle, middle_id, end, end_id, work_item, work_item_id) in MODULE.COMPILED_KALDOR_ARRIVALS.items():
				quests_xml.append(
					f"<quest><id>{quest_id}</id><max_repeat_count>1</max_repeat_count><minlevel_permitted>65</minlevel_permitted><cannot_share>1</cannot_share>"
					f"<quest_work_item1>{work_item} 1</quest_work_item1><reward_exp1>3446553</reward_exp1><reward_gold1>150660</reward_gold1>"
					f"<selectable_reward_item1_1>{MODULE.COMPILED_KALDOR_ARRIVAL_REWARDS[0][0]} 1</selectable_reward_item1_1>"
					f"<selectable_reward_item1_2>{MODULE.COMPILED_KALDOR_ARRIVAL_REWARDS[1][0]} 1</selectable_reward_item1_2><race_permitted>{retail_race}</race_permitted></quest>")
				base_xml.append(
					f'<quest id="{quest_id}" minlevel_permitted="65" max_repeat_count="1" cannot_share="true" race_permitted="{base_race}">'
					'<rewards gold="150660" exp="3446553">' + ''.join(f'<selectable_reward_item item_id="{item_id}" count="1"/>' for _, item_id in MODULE.COMPILED_KALDOR_ARRIVAL_REWARDS)
					+ f'</rewards><quest_work_items><quest_work_item item_id="{work_item_id}" count="1"/></quest_work_items></quest>')
				npcs.extend((f"<npc><id>{start_id}</id><name>{start}</name></npc>", f"<npc><id>{middle_id}</id><name>{middle}</name></npc>", f"<npc><id>{end_id}</id><name>{end}</name></npc>"))
				items.append(f"<item><id>{work_item_id}</id><name>{work_item}</name></item>")
				for index, name in enumerate((start, middle, end)):
					blocks.append(f'// @q{quest_id}_npc_{index}\nFUN_180cb5920(x,L"{name}",{quest_hex})')
				for index, phase in enumerate(((0, "0xffffffff"), (3, "0"), (3, "1"), (3, "2"), (4, "0xffffffff"))):
					blocks.append(f"// @q{quest_id}_phase_{index}\nFUN_180cb3070(x,x,{quest_hex},{phase[0]},{phase[1]},0)")
				blocks.append(f"// @q{quest_id}_start\nFUN_180cab520({quest_hex},x)")
				for index in range(3):
					blocks.append(f"// @q{quest_id}_talk_{index}\nFUN_180cabb10({quest_hex},x)")
				blocks.append(f"// @q{quest_id}_complete\nFUN_180caca90({quest_hex},x)")
			for name, item_id in MODULE.COMPILED_KALDOR_ARRIVAL_REWARDS:
				items.append(f"<item><id>{item_id}</id><name>{name}</name></item>")
			(root / "quest.xml").write_text(f"<quests>{''.join(quests_xml)}</quests>")
			(root / "quest_data.xml").write_text(f"<quests>{''.join(base_xml)}</quests>")
			(root / "npcs.xml").write_text(f"<npcs>{''.join(npcs)}</npcs>")
			(root / "Items.xml").write_text(f"<items>{''.join(items)}</items>")
			(script_root / "fun_354.cpp").write_text("\n".join(blocks))
			quests, skipped = MODULE.compiled_kaldor_arrivals(root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root, root / "quest_data.xml", set(MODULE.COMPILED_KALDOR_ARRIVALS))
			self.assertEqual(set(MODULE.COMPILED_KALDOR_ARRIVALS), set(quests))
			self.assertEqual({}, skipped)
			self.assertEqual([{"type": "TALK", "names": ["LF5_OP1_ZoneTeleport_L"], "actions": []}], quests[13800]["steps"])
			source = script_root / "fun_354.cpp"
			source.write_text(source.read_text().replace("FUN_180caca90(0x35e8", "FUN_180cacaa0(0x35e8", 1))
			quests, skipped = MODULE.compiled_kaldor_arrivals(root / "quest.xml", root / "npcs.xml", root / "Items.xml", script_root, root / "quest_data.xml", set(MODULE.COMPILED_KALDOR_ARRIVALS))
			self.assertNotIn(13800, quests)
			self.assertIn("script:function_blocks", skipped["13800"])

	@staticmethod
	def write_compiled_buyer_scripts(script_root):
		sources = {number: [] for number in MODULE.COMPILED_ITEM_BUYER_SOURCE_NUMBERS}
		for quest_id, (npc_name, quest_hex, _, _) in MODULE.COMPILED_ITEM_BUYERS.items():
			light = quest_id < 20000
			sources[620 if light else 611].append(f'// @q{quest_id}_binding\nL"{npc_name}" {quest_hex}')
			sources[869 if light else 868].append(
				f"// @q{quest_id}_dialog\nx,{quest_hex}) x,{quest_hex},0,0) x,{quest_hex},0) + 0xd0 + 0x148 + 0x160 + 0x308 + 0x1b0 0x3f1"
			)
			sources[882 if light else 880].append(
				f"// @q{quest_id}_turn_in\nx,{quest_hex},0,0) x,{quest_hex},0,1) + 0x308 + 0x268 + 0x1e0 + 0x160 0x3f0 0x3f1"
			)
			for phase, number in zip((0, 3, 4), ((691, 688, 697) if light else (690, 687, 695))):
				sources[number].append(f"// @q{quest_id}_phase_{phase}\nx,{quest_hex},{phase},x")
		for number, lines in sources.items():
			(script_root / f"fun_{number:03d}.cpp").write_text("\n".join(lines))

	@staticmethod
	def write_compiled_firework_scripts(script_root):
		sources = {number: [] for number in MODULE.COMPILED_FIREWORK_SOURCE_NUMBERS}
		for quest_id, (start, talks, quest_hex, _) in MODULE.COMPILED_FIREWORK_REPORTS.items():
			for npc_name in (start, *talks):
				sources[630].append(f'// @q{quest_id}_{npc_name}\nL"{npc_name}" {quest_hex}')
			sources[694].append(f"// @q{quest_id}_phase_0\nx,{quest_hex},0,0xffffffff,0)")
			sources[701].append(f"// @q{quest_id}_phase_4\nx,{quest_hex},4,0xffffffff,0)")
			sources[705].append(f"// @q{quest_id}_talk_0\nx,{quest_hex},3,0,0)")
			sources[708].append(f"// @q{quest_id}_talk_1\nx,{quest_hex},3,1,0)")
			sources[869].append(f"// @q{quest_id}_start\n0x3f3 {quest_hex}")
			sources[894].append(f"// @q{quest_id}_first\n{quest_hex} 0x548 10000 120000 + 0xf0 x,{quest_hex},1)")
			sources[897].append(f"// @q{quest_id}_second\n{quest_hex} 0x69d 0x2711 + 0x100 x,{quest_hex},2)")
			sources[875].append(f"// @q{quest_id}_timeout\n{quest_hex} *(unsigned int *)(lVar3 + 1) < 2 + 600 + 0xf0 x,{quest_hex},0,0)")
			sources[885].append(f"// @q{quest_id}_turn_in\n{quest_hex} + 0x1a8 + 0x1a0 0x3ef")
		for number, lines in sources.items():
			(script_root / f"fun_{number:03d}.cpp").write_text("\n".join(lines))

	@staticmethod
	def write_compiled_debris_scripts(script_root):
		sources = {number: [] for number in MODULE.COMPILED_DEBRIS_SOURCE_NUMBERS}
		for quest_id, (quest_hex, _race, _gold, _names, _ids, lition_data, debris_data, rodelion_data, start_callback, turn_in_callback, rodelion_callback) in MODULE.COMPILED_DEBRIS_RESCUES.items():
			for number, npc_name, npc_data in ((624, "Lition", lition_data), (619, "IDYun_Debris_Q30503", debris_data), (626, "Rodelion", rodelion_data)):
				sources[number].append(f'// @q{quest_id}_{npc_name}\n{npc_data} L"{npc_name}" {quest_hex}')
			sources[692].append(f"// @q{quest_id}_phase_0\n{lition_data} x,{quest_hex},0,0xffffffff,0)")
			sources[698].append(f"// @q{quest_id}_phase_4\n{lition_data} x,{quest_hex},4,0xffffffff,0)")
			sources[703].append(f"// @q{quest_id}_debris_phase\n{debris_data} x,{quest_hex},3,0,3)")
			sources[704].append(f"// @q{quest_id}_rodelion_phase\n{rodelion_data} x,{quest_hex},3,0,0)")
			sources[656].append(f"// @q{quest_id}_start_binding\n{lition_data} 0x26 {start_callback}")
			sources[661].append(f"// @q{quest_id}_turn_in_binding\n{lition_data} 0x32 {turn_in_callback}")
			sources[667].append(f"// @q{quest_id}_debris_binding\n{debris_data} 3,0,&LAB_180f12fc0")
			sources[668].append(f"// @q{quest_id}_rodelion_binding\n{rodelion_data} 3,0,{rodelion_callback}")
			sources[877].append(f"// @q{quest_id}_state\n{quest_hex} param_1[3] != 0 + 0x4b8")
			sources[882].append(f"// @q{quest_id}_start\nFUN_180cab520({quest_hex},x,0,0)")
			sources[888].append(f"// @q{quest_id}_turn_in\nFUN_180caf6c0({quest_hex},x) FUN_180caf350({quest_hex},x)")
			sources[893].append(
				f"// @q{quest_id}_rodelion\nFUN_180caf740({quest_hex},x) 0x280f FUN_180caf3c0({quest_hex},x) "
				f"+ 0x5d8 x,{quest_hex},0,0) + 0x100 + 0x110 0x10f54c")
		sources[878].append("// @180f12f70 FUN_180f12f70\n+ 0x4b8")
		for number, lines in sources.items():
			(script_root / f"fun_{number:03d}.cpp").write_text("\n".join(lines))

	@staticmethod
	def write_compiled_world_collect_scripts(script_root):
		blocks = []
		for quest_id, (quest_hex, _race, _drops) in MODULE.COMPILED_WORLD_COLLECTS.items():
			blocks.append(
				f"// @q{quest_id}_world\nvoid q{quest_id}() {{\n"
				f"  if (*(int *)(param_2 + 0x14) == 0x11e329a0) + 0x148 {quest_hex};\n"
				f"  if (*(int *)(param_2 + 0xc) == 0x11e329a0) + 0xd0 {quest_hex} + 0x160;\n}}")
		(script_root / "fun_873.cpp").write_text("\n".join(blocks))

	@staticmethod
	def write_compiled_growth_scripts(script_root):
		sources = {number: [] for number in MODULE.COMPILED_GROWTH_SOURCE_NUMBERS}
		for quest_id, (npc_name, _npc_id, quest_hex, race, _inventory_name, _inventory_id, _reward_name, reward_id, callback) in MODULE.COMPILED_GROWTH_QUESTS.items():
			light = race == "pc_light"
			sources[620 if light else 611].append(f'// @q{quest_id}_npc\nL"{npc_name}" {quest_hex}')
			sources[691 if light else 690].append(f"// @q{quest_id}_start_phase\nx,{quest_hex},0,0xffffffff,0)")
			sources[717].append(f"// @q{quest_id}_turn_in_phase\nx,{quest_hex},4,1,0)")
			sources[649].append(f"// @q{quest_id}_item_binding\nx,{quest_hex},5,{hex(reward_id)},{callback})")
			sources[876].append(
				f"// @q{quest_id}_item_transition\n{callback} + 0xd0 {quest_hex} local_res8 == '\\x03' "
				f"local_res9 == 0 + 0x100 x,{quest_hex},1)")
			for number, helper in ((882 if light else 880, "FUN_180caf640"), (887 if light else 885, "FUN_180caf6c0")):
				sources[number].append(
					f"// @q{quest_id}_{helper}\n{helper}({quest_hex},x) FUN_180caf350({quest_hex},x)")
		for number, lines in sources.items():
			(script_root / f"fun_{number:03d}.cpp").write_text("\n".join(lines))

	@staticmethod
	def write_compiled_sensory_scripts(script_root):
		sources = {number: [] for number in MODULE.COMPILED_SENSORY_SOURCE_NUMBERS}
		for quest_id, (npc_name, _npc_id, quest_hex, race, prerequisite) in MODULE.COMPILED_SENSORY_COMPLETES.items():
			light = race == "pc_light"
			prerequisite_token = str(prerequisite) if light else hex(prerequisite)
			sources[620 if light else 611].append(f'// @q{quest_id}_npc\nL"{npc_name}" {quest_hex}')
			sources[872 if light else 871].append(
				f"// @q{quest_id}_sensory\n{quest_hex} + 0x1e0 {'0x6a' if light else '0x8a'}")
			sources[871 if light else 873].append(
				f"// @q{quest_id}_start\n{quest_hex} + 0x148 + 0x150 x,{quest_hex},1)")
			sources[875].append(f"// @q{quest_id}_finish\n{quest_hex} + 0xd0 + 0x268 x,{quest_hex},0,1)")
			sources[906].append(
				f"// @q{quest_id}_trigger\n{quest_hex} x,{prerequisite_token}) + 0x138 x,{quest_hex}) + 0xd8 + 0x100 + 0x1c0 0x17")
		for number, lines in sources.items():
			(script_root / f"fun_{number:03d}.cpp").write_text("\n".join(lines))

	@staticmethod
	def write_compiled_paios_scripts(script_root):
		sources = {number: [] for number in MODULE.COMPILED_PAIOS_SOURCE_NUMBERS}
		for quest_id, (quest_hex, _race, _gold, _prerequisite, lition_data, column_data, paios_data, column_callback, paios_callback) in MODULE.COMPILED_PAIOS_RESCUES.items():
			for number, name, data in ((624, "Lition", lition_data), (619, "IDYun_Column_Q30504", column_data), (625, "Paios", paios_data)):
				sources[number].append(f'// @q{quest_id}_{name}\n{data} L"{name}" {quest_hex}')
			for number, data, suffix in ((698, lition_data, "4,0xffffffff,0"), (703, column_data, "3,0,3"), (707, paios_data, "3,1,0")):
				sources[number].append(f"// @q{quest_id}_phase_{number}\n{data} x,{quest_hex},{suffix})")
			sources[667].append(f"// @q{quest_id}_column_binding\n{column_data} 3,0,{column_callback}")
			sources[671].append(f"// @q{quest_id}_paios_binding\n{paios_data} 3,1,{paios_callback}")
			sources[882].append(f"// @q{quest_id}_start\nFUN_180cab520({quest_hex},x,0,0)")
			sources[892].append(
				f"// @q{quest_id}_column\n{quest_hex} + 0xf8 x,{quest_hex},1) + 0x110 + 0x4b8")
			sources[873].append(
				f"// @q{quest_id}_world\n{quest_hex} 0x11e5e8c0 + 0xd0 local_res9 != 0 + 0xf0 x,{quest_hex},0,0)")
			sources[896].append(
				f"// @q{quest_id}_turn_in\nFUN_180caf740({quest_hex},x) 0x280f FUN_180caf3c0({quest_hex},x) "
				f"+ 0x100 + 0x110 x,{quest_hex},3000) 0x10f54d")
			sources[877].append(f"// @q{quest_id}_state\n{quest_hex} param_1[3] != 0 + 0x4b8")
		for number, lines in sources.items():
			(script_root / f"fun_{number:03d}.cpp").write_text("\n".join(lines))


if __name__ == "__main__":
	unittest.main()
