#!/usr/bin/env python3
"""Generate equivalent AionEmu Simple* XML from the local retail 5.8 data."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path
from types import SimpleNamespace


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_RETAIL = Path("/Users/mc/IdeaProjects/58Server-new/Map/XML")
DEFAULT_RETAIL_SCRIPT = Path("/Users/mc/IdeaProjects/58Server/server58-source/MainServer_ScriptDLL64/fun")
DEFAULT_CLIENT_QUEST = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml")
DEFAULT_RETAIL_REGION = "China"
LEGACY_RETAIL_DATA_ROOTS = (
	Path("/Users/mc/IdeaProjects/58Server/Map/XML").resolve(),
)
DEFAULT_QUEST_DATA = ROOT / "src/main/resources/aion/definitions/compact/quests/quest_data.xml"
DEFAULT_OUTPUT = ROOT / "src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml"
DEFAULT_REPORT = DEFAULT_OUTPUT.with_suffix(".report.json")
DEFAULT_REFERENCE_GRAPH = ROOT / "scripts/retail-reference-graph.json"
QUEST_SOURCE_FILES = (
	"Quest_SimpleHunt.xml",
	"Quest_SimpleSerialHunt.xml",
	"Quest_SimpleTalk.xml",
	"Quest_SimpleCollectItem.xml",
	"Quest_SimpleUseItem.xml",
	"Quest_SimpleItemPlay.xml",
	"Quest_CombineTask.xml",
	"data_driven_quest.xml",
	"npcfactions_quest.xml",
	"challenge_task.xml",
	"npcs.xml",
	"Items.xml",
	"quest.xml",
	"combine_recipe.xml",
)
IGNORED_FIELDS = {"dev_name", "con_quest"}
BASE_FIELDS = {"acquired_npc_name", "reward_npc_name"}
GENERATED_TEMPLATE_TAGS = {"data_driven_quest", "item_collecting", "item_order", "kill_in_world", "monster_hunt", "report_to", "report_to_many", "work_order"}
LEGACY_DISPOSITIONS = {
	"outside_base": "not_executable_without_base",
	"unresolved_references": "blocked_reference_resolution",
	"unsupported_fields": "blocked_extra_semantics",
	"invalid_retail_definition": "blocked_invalid_retail_source",
	"unsupported_template": "already_uses_xml_runtime",
	"missing_supported_retail_source": "blocked_authoritative_source",
	"unsupported_retail_shape": "blocked_semantic_gap",
}
COMPILED_ITEM_BUYERS = {
	19079: ("LC1_L_exceed_key_buyer", "0x4a87", 1140000000, "pc_light"),
	19080: ("LC1_L_exceed_key_buyer_02", "0x4a88", 1600000000, "pc_light"),
	19081: ("LC1_L_exceed_key_buyer_03", "0x4a89", 2020000000, "pc_light"),
	29079: ("DC1_D_exceed_key_buyer", "0x7197", 1140000000, "pc_dark"),
	29080: ("DC1_D_exceed_key_buyer_02", "0x7198", 1600000000, "pc_dark"),
	29081: ("DC1_D_exceed_key_buyer_03", "0x7199", 2020000000, "pc_dark"),
}
COMPILED_ISOLATION_DETAILS = {
	1489: "only generic quest-id dispatch recovered; trigger, target and completion chain are missing",
	1908: "multi-stage multi-NPC branching dialogue cannot be reconstructed losslessly from the recovered arrays",
	2590: "no matching executable quest chain recovered from ScriptDLL",
	3326: "cross-quest state machine with quest 1037, hidden count 20 and multiple monster branches",
	4338: "multi-stage Urakon/Gundalpun/Kimci/five-friends flow with item callbacks and quest 2033 state",
	9554: "retail data contains only race/gender reward variants; no matching ScriptDLL trigger chain",
	9555: "retail data contains only race/gender reward variants; no matching ScriptDLL trigger chain",
	9556: "retail data contains only race/gender reward variants; no matching ScriptDLL trigger chain",
	9557: "retail data contains only race/gender reward variants; no matching ScriptDLL trigger chain",
	16984: "movie creation and completion functions recovered, but the task-start trigger registration is incomplete",
	16989: "movie creation and completion functions recovered, but the task-start trigger registration is incomplete",
	26984: "movie creation and completion functions recovered, but the task-start trigger registration is incomplete",
	50102: "NPC dialogue dynamically selects one of five item types; OR inventory branches are not representable",
}
COMPILED_ITEM_BUYER_SOURCE_NUMBERS = (611, 620, 687, 688, 690, 691, 695, 697, 868, 869, 880, 882)
COMPILED_FIREWORK_REPORTS = {
	80761: ("event_l_conqueror_01", ("event_l_fireworkbox_01", "event_l_fireworkbox_02"), "0x13b79", "pc_light"),
	80766: ("event_d_conqueror_01", ("event_d_fireworkbox_01", "event_d_fireworkbox_02"), "0x13b7e", "pc_dark"),
}
COMPILED_FIREWORK_SOURCE_NUMBERS = (630, 694, 701, 705, 708, 869, 875, 885, 894, 897)
COMPILED_DEBRIS_RESCUES = {
	30503: ("0x7727", "pc_light", 598320, ("food_4stats_60c", "food_4stats_60d", "food_4stats_60e", "food_4stats_60f", "food_4stats_60g", "food_4stats_60h"), (160001338, 160001339, 160001340, 160001341, 160001342, 160001343), "DAT_1862cd730", "DAT_1862cdd70", "DAT_1862ce3b0", "FUN_180f3d4d0", "FUN_180f63900", "FUN_180f85c90"),
	30553: ("0x7759", "pc_dark", 897480, ("food_d_4stats_60c", "food_d_4stats_60d", "food_d_4stats_60e", "food_d_4stats_60f", "food_d_4stats_60g", "food_d_4stats_60h"), (160002336, 160002337, 160002338, 160002339, 160002340, 160002341), "DAT_1862cfcc0", "DAT_1862d0310", "DAT_1862d0950", "FUN_180f3d530", "FUN_180f63980", "FUN_180f85dc0"),
}
COMPILED_DEBRIS_NPCS = {"Lition": 205438, "IDYun_Debris_Q30503": 701097, "Rodelion": 799541}
COMPILED_DEBRIS_SOURCE_NUMBERS = (619, 624, 626, 656, 661, 667, 668, 692, 698, 703, 704, 877, 878, 882, 888, 893)
COMPILED_WORLD_COLLECTS = {
	3219: ("0xc93", "pc_light", (("IDShip_ShulackWiKnmd_42_Ae", 215064, "key_idshulack_f2_d04", 185000046), ("IDShip_ShulackPrKnmd_42_Ae", 215065, "key_idshulack_f2_d05", 185000047))),
	3220: ("0xc94", "pc_light", (("IDShip_ShulackWiHKnmd_45_Ae", 215058, "key_idshulack_f1_hd02", 185000055), ("IDShip_ShulackAsHardKnmd_45_Ae", 215066, "key_idshulack_f1_d08", 185000050), ("IDShip_ShulackRaKnmd_44_Ae", 215411, "key_idshulack_f3_d12", 185000073))),
	4219: ("0x107b", "pc_dark", (("IDShip_ShulackWiKnmd_42_Ae", 215064, "key_d_idshulack_f2_d04", 185000077), ("IDShip_ShulackPrKnmd_42_Ae", 215065, "key_d_idshulack_f2_d05", 185000078))),
	4220: ("0x107c", "pc_dark", (("IDShip_ShulackWiHKnmd_45_Ae", 215058, "key_d_idshulack_f1_hd02", 185000079), ("IDShip_ShulackAsHardKnmd_45_Ae", 215066, "key_d_idshulack_f1_d08", 185000080), ("IDShip_ShulackRaKnmd_44_Ae", 215411, "key_d_idshulack_f3_d12", 185000081))),
}
COMPILED_WORLD_COLLECT_WORLD_ID = 300100000
COMPILED_WORLD_COLLECT_SOURCE_NUMBERS = (873,)
COMPILED_GROWTH_QUESTS = {
	19678: ("LC1_L_grow_npc_Rena_01", 806698, "0x4cde", "pc_light", "item_newbie_start_coin_l", 164000506, "matter_enchant_exceed_01", 166020000, "FUN_180f012e0"),
	19679: ("LC1_L_grow_npc_Rena_01", 806698, "0x4cdf", "pc_light", "item_newbie_start_coin_l", 164000506, "matter_2stenchant_m_65", 166030005, "FUN_180f01330"),
	29678: ("DC1_D_grow_npc_Melrania_01", 806700, "0x73ee", "pc_dark", "item_newbie_start_coin_d", 164000507, "matter_enchant_exceed_01", 166020000, "FUN_180f01ae0"),
	29679: ("DC1_D_grow_npc_Melrania_01", 806700, "0x73ef", "pc_dark", "item_newbie_start_coin_d", 164000507, "matter_2stenchant_m_65", 166030005, "FUN_180f01b30"),
}
COMPILED_GROWTH_SOURCE_NUMBERS = (611, 620, 649, 690, 691, 717, 876, 880, 882, 885, 887)
COMPILED_SENSORY_COMPLETES = {
	3959: ("LC1_SensoryArea_Q3959", 206101, "0xf77", "pc_light", 1099),
	4963: ("DC1_SensoryArea_Q4963", 206102, "0x1363", "pc_dark", 2099),
}
COMPILED_SENSORY_SOURCE_NUMBERS = (611, 620, 871, 872, 873, 875, 906)
COMPILED_PAIOS_RESCUES = {
	30504: ("0x7728", "pc_light", 299160, 30503, "DAT_1862cea00", "DAT_1862cf040", "DAT_1862cf680", "FUN_180f7c5c0", "FUN_180f9b950"),
	30554: ("0x775a", "pc_dark", 897480, 30553, "DAT_1862d0f90", "DAT_1862d15e0", "DAT_1862d1c20", "FUN_180f7c650", "FUN_180f9ba90"),
}
COMPILED_PAIOS_NPCS = {"Lition": 205438, "IDYun_Column_Q30504": 701098, "Paios": 799536}
COMPILED_PAIOS_REWARDS = (("scroll_critical_phy_50a", 164000066), ("scroll_critical_mag_50a", 164000121), ("scroll_shield_all_50a", 164000070))
COMPILED_PAIOS_WORLD_ID = 300280000
COMPILED_PAIOS_SOURCE_NUMBERS = (619, 624, 625, 667, 671, 698, 703, 707, 873, 877, 882, 892, 896)
COMPILED_HOUSING_FLOWERS = {
	18806: ("0x4976", "pc_light", "ELYOS", 18831, "HousingManager_Li", (810017, 810018, 810019, 810020, 810021), "Pesarius", 830528, "Kaionen", 830194),
	28806: ("0x7086", "pc_dark", "ASMODIANS", 28831, "HousingManager_Da", (810022, 810023, 810024, 810025, 810026), "Prakon", 830530, "Katenon", 830211),
}
COMPILED_HOUSING_FLOWER_SOURCE_NUMBERS = (351, 353, 361, 362, 543, 554, 564, 586, 805, 813, 839, 849, 857)
COMPILED_SCORCHED_TREES = {
	13809: ("0x35f1", "pc_light", "ELYOS", "LDF5_Fortress_Village_Guard01_L", 802427,
		(("quest_13809a", 182215485), ("quest_13809b", 182215486), ("quest_13809c", 182215487))),
	23809: ("0x5d01", "pc_dark", "ASMODIANS", "LDF5_Fortress_Village_Guard01_D", 802429,
		(("quest_23809a", 182215493), ("quest_23809b", 182215494), ("quest_23809c", 182215495))),
}
COMPILED_SCORCHED_TREE_ACTIONS = (
	("LDF5_Fortress_FOBJ_B1_DeadTree_a", 730969),
	("LDF5_Fortress_FOBJ_B1_DeadTree_b", 730970),
	("LDF5_Fortress_FOBJ_B1_DeadTree_c", 730971),
)
COMPILED_SCORCHED_TREE_REWARDS = (("phyatt_l_con_polish_enchant_01a", 166050221), ("magatt_l_con_polish_enchant_01a", 166050222))
COMPILED_SCORCHED_TREE_SOURCE_NUMBERS = (354, 539, 554, 557, 558, 566, 586, 814, 835, 850, 852, 853)
COMPILED_KALDOR_ARRIVALS = {
	13800: ("0x35e8", "pc_light", "ELYOS", "LF5_Atmos_E", 804699, "LF5_OP1_ZoneTeleport_L", 804782,
		"LDF5_Fortress_Alphion_E", 802431, "quest_13800a", 182215482),
	23800: ("0x5cf8", "pc_dark", "ASMODIANS", "DF5_Haldor_E", 804719, "DF5_OP1_ZoneTeleport_D", 804753,
		"LDF5_Fortress_Pintz_E", 802433, "quest_23800a", 182215490),
}
COMPILED_KALDOR_ARRIVAL_REWARDS = (("phydef_l_con_polish_enchant_01a", 166050223), ("magdef_l_con_polish_enchant_01a", 166050224))
COMPILED_KALDOR_ARRIVAL_SOURCE_NUMBERS = (347, 354, 357, 535, 541, 554, 557, 562, 568, 586, 810, 816, 830, 837, 850, 852, 854)
COMPILED_COALESCENCE_COMPLETES = {
	15542: ("0x3cb6", "pc_light", "ELYOS", "LF6_Felen_E", 806074, 15550),
	25542: ("0x63c6", "pc_dark", "ASMODIANS", "DF6_Edorin_E", 806078, 25550),
}
COMPILED_COALESCENCE_SOURCE_NUMBERS = (614, 623, 690, 692, 696, 698, 874, 880, 882, 886, 887, 904, 905, 907, 908)
COMPILED_BASTION_MOVIES = {
	18036: ("0x4674", "pc_light", "ELYOS", "LDF5b_Demades_E", 801281,
		"LDF5b_IDLDF5b_TD_Drakan_Fighter", 802008, 13305, "FUN_180df9c00"),
	28036: ("0x6d84", "pc_dark", "ASMODIANS", "LDF5b_Latkel_E", 801280,
		"LDF5b_IDLDF5b_TD_Drakan_Fighter_Da", 802015, 23305, "FUN_180df9cf0"),
}
COMPILED_BASTION_MOVIE_SOURCE_NUMBERS = (442, 620, 691, 697, 703, 806, 882, 887, 892)
COMPILED_SIMPLE_TALKS = {
	1131: {
		"quest_hex": "0x46b", "race": "pc_light", "base_race": "ELYOS", "start": "Hyacinte", "start_id": 203097,
		"end": "Nadaelo", "end_id": 203101, "talks": [("Shugo_LF1a_01", 799093)], "data_driven": True,
		"start_give_item": ("quest_1131a", 1), "step_give_item": ("doc_quest_1131b", 1), "step_remove_item": ("quest_1131a", 1),
		"retail": {"name": "Q1131", "max_repeat_count": "1", "minlevel_permitted": "10", "reward_exp1": "18809", "reward_gold1": "610", "reward_item1_1": "potion_hp_mp_30a 5", "check_item1_1": "doc_quest_1131b 1", "race_permitted": "pc_light"},
		"talk": {"give_item": "ITEM_QUEST_1131A 1", "give_item1": "ITEM_DOC_QUEST_1131B 1", "remove_item1": "ITEM_QUEST_1131A 1"},
		"base": {"minlevel_permitted": "10", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "IMPORTANT", "rewards": {"gold": "610", "exp": "18809"}, "reward_items": (("162000048", "5"),), "work_items": (("182200507", "1"), ("182200506", "1"))},
		"evidence": ((351, ("L\"Hyacinte\"",)), (360, ("L\"Nadaelo\"",)), (364, ("L\"Shugo_LF1a_01\"",)), (564, (",0x46b,0,0xffffffff,0",)), (545, (",0x46b,3,0,0",)), (555, (",0x46b,3,1,0",)), (590, (",0x46b,4,0xffffffff,0",)), (813, ("FUN_180cab520(0x46b", "0xadc28ba")), (841, ("FUN_180cabb10(0x46b",)), (850, ("FUN_180cabb10(0x46b",)), (854, ("FUN_180caca90(0x46b", "0xadc28ba")))
	},
	1218: {
		"quest_hex": "0x4c2", "race": "pc_light", "base_race": "ELYOS", "start": "Taiotus", "start_id": 203121,
		"end": "Une", "end_id": 203172, "talks": [("Shugo3", 798004)], "data_driven": True,
		"step_give_item": ("doc_quest_1218a", 1),
		"retail": {"name": "Q1218", "max_repeat_count": "1", "minlevel_permitted": "19", "cannot_share": "1", "quest_work_item1": "doc_quest_1218a", "reward_exp1": "60300", "reward_gold1": "0", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Taiotus", "talk_npc1": "Shugo3", "give_item1": "ITEM_DOC_QUEST_1218A 1", "reward_npc_name": "Une", "con_quest": "1219"},
		"base": {"minlevel_permitted": "19", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"exp": "60300"}, "reward_items": (), "work_items": (("182200566", "1"),)},
		"evidence": ((363, ("DAT_181249090",)), (366, ("L\"Taiotus\"",)), (368, ("DAT_18133a410",)), (545, (",0x4c2,3,0,0",)), (556, (",0x4c2,3,1,0",)), (573, (",0x4c2,0,0xffffffff,0",)), (595, (",0x4c2,4,0xffffffff,0",)), (719, ("case 0x4c2:", "uVar2 = 0x4c2")), (821, ("FUN_180cab520(0x4c2",)), (840, ("FUN_180cabb10(0x4c2",)), (851, ("FUN_180cabb10(0x4c2",)), (859, ("+ 0x1b8", "0x4c2")))
	},
	1220: {
		"quest_hex": "0x4c4", "race": "pc_light", "base_race": "ELYOS", "start": "Une", "start_id": 203172,
		"end": "shugo_Lender_LF2_01", "end_id": 205240, "talks": [("Shugo3", 798004)], "data_driven": True,
		"start_give_item": ("quest_1220a", 1), "step_give_item": ("quest_1220b", 1), "step_remove_item": ("quest_1220a", 1),
		"retail": {"name": "Q1220", "max_repeat_count": "1", "minlevel_permitted": "17", "finished_quest_cond1": "Q1219", "cannot_share": "1", "quest_work_item1": "quest_1220a", "quest_work_item2": "quest_1220b", "reward_exp1": "91950", "reward_gold1": "6620", "reward_item1_1": "assembly_matter_enchant_dust_01 14", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Une", "give_item": "ITEM_QUEST_1220A 1", "talk_npc1": "Shugo3", "give_item1": "ITEM_QUEST_1220B 1", "remove_item1": "ITEM_QUEST_1220A 1", "reward_npc_name": "shugo_Lender_LF2_01"},
		"base": {"minlevel_permitted": "17", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"gold": "6620", "exp": "91950"}, "reward_items": (("188100335", "14"),), "work_items": (("182200568", "1"), ("182200569", "1")), "finished_quests": ("1219",)},
		"evidence": ((363, ("DAT_181249090",)), (368, ("DAT_18133a410",)), (374, ("L\"shugo_Lender_LF2_01\"",)), (545, (",0x4c4,3,0,0",)), (556, (",0x4c4,3,1,0",)), (574, (",0x4c4,0,0xffffffff,0",)), (600, (",0x4c4,4,0xffffffff,0",)), (719, ("case 0x4c4:", "uVar2 = 0x4c4")), (755, ("+ 0x1a8", "0x4c4")), (822, ("FUN_180cab520(0x4c4", "0xadc28f8")), (840, ("FUN_180cabb10(0x4c4",)), (851, ("FUN_180cabb10(0x4c4",)), (856, ("FUN_180caca90(0x4c4", "0xadc28f8")))
	},
	1319: {
		"quest_hex": "0x527", "race": "pc_light", "base_race": "ELYOS",
		"start": "Prorite", "start_id": 203908, "end": "Prorite", "end_id": 203908,
		"talks": [("Krato", 203923), ("Hebestis", 203910), ("Benos", 203906), ("Diokles", 203915), ("TusKeos", 203907), ("Shugo_LF2_6", 798050), ("Shugo_LF2_5", 798049), ("shugo_Lender_LF2_01", 205240)], "data_driven": True, "start_dialog_id": 1011,
		"steps": [{"type": "TALK", "names": ["Krato"], "dialog_id": 1693}, {"type": "TALK", "names": ["Hebestis"], "dialog_id": 2034}, {"type": "TALK", "names": ["Benos"], "dialog_id": 2375}, {"type": "TALK", "names": ["Diokles"], "dialog_id": 2716}, {"type": "TALK", "names": ["TusKeos"], "dialog_id": 3057}, {"type": "TALK", "names": ["Shugo_LF2_6"], "dialog_id": 3398}, {"type": "TALK", "names": ["Shugo_LF2_5"], "dialog_id": 3739}, {"type": "TALK", "names": ["shugo_Lender_LF2_01"], "dialog_id": 4080}],
		"retail": {"name": "Q1319", "max_repeat_count": "1", "minlevel_permitted": "19", "reward_exp1": "30000", "reward_gold1": "0", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "19", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"exp": "30000", "title": "9"}, "reward_items": (), "work_items": (("182201370", "1"),)},
		"evidence": ((693, (",0x527,0,0xffffffff,0)",)), (703, (",0x527,3,0,0)",)), (706, (",0x527,3,1,0)",)), (708, (",0x527,3,2,0)",)), (710, (",0x527,3,3,0)",)), (712, (",0x527,3,4,0)",)), (713, (",0x527,3,5,0)",)), (714, (",0x527,3,6,0)",)), (714, (",0x527,3,7,0)",)), (699, (",0x527,4,0xffffffff,0)",)), (895, (",0x69d,0x527)",)), (897, (",0x7f2,0x527)",)), (899, (",0x947,0x527)",)), (900, (",0xa9c,0x527)",)), (901, (",0xbf1,0x527)",)), (901, (",0xd46,0x527)",)), (901, (",0xe9b,0x527)",)), (888, (",0xff0,0x527)",))),
	},
	1553: {
		"quest_hex": "0x611", "race": "pc_light", "base_race": "ELYOS", "start": "Diana", "start_id": 203786,
		"end": "Piera", "end_id": 204584, "talks": [("DF2_NPC_TalkingMirror", 730051), ("Perento", 204500)], "data_driven": True,
		"start_give_item": ("quest_1553a", 1),
		"steps": [{"type": "TALK", "names": ["DF2_NPC_TalkingMirror"], "give_item": ("quest_1553b", 1), "remove_item": ("quest_1553a", 1)}, {"type": "TALK", "names": ["Perento"]}],
		"retail": {"name": "Q1553", "max_repeat_count": "1", "minlevel_permitted": "43", "finished_quest_cond1": "Q1550", "cannot_share": "1", "quest_work_item1": "quest_1553a", "quest_work_item2": "quest_1553b", "reward_exp1": "2954681", "reward_gold1": "0", "reward_abyss_point1": "200", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Diana", "give_item": "ITEM_QUEST_1553A 1", "talk_npc1": "DF2_NPC_TalkingMirror", "give_item1": "ITEM_QUEST_1553B 1", "remove_item1": "ITEM_QUEST_1553A 1", "talk_npc2": "Perento", "reward_npc_name": "Piera", "con_quest": "1554"},
		"base": {"minlevel_permitted": "43", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"exp": "2954681", "ap": "200"}, "reward_items": (), "work_items": (("182201794", "1"), ("182201795", "1")), "finished_quests": ("1550",)},
		"evidence": ((346, ("L\"DF2_NPC_TalkingMirror\"",)), (348, ("L\"Diana\"",)), (361, ("L\"Perento\"",)), (361, ("L\"Piera\"",)), (534, (",0x611,3,0,0",)), (555, (",0x611,3,1,0",)), (557, (",0x611,3,2,0",)), (562, (",0x611,0,0xffffffff,0",)), (591, (",0x611,4,0xffffffff,0",)), (805, ("+ 0x1a8", "0x611")), (810, ("FUN_180cab520(0x611", "0xadc2dc2", "1")), (830, ("FUN_180cabb10(0x611",)), (851, ("FUN_180cabb10(0x611",)), (852, ("FUN_180cabb10(0x611",)), (854, ("FUN_180caca90(0x611", "0xadc2dc2", "1")), (858, ("+ 0x1b8", "0x611")))
	},
	1574: {
		"quest_hex": "0x626", "race": "pc_light", "base_race": "ELYOS", "start": "Tree_Move_Terba", "start_id": 730025,
		"end": "Tree_Move_Terba", "end_id": 730025, "talks": [("Trou", 204560), ("Arkos", 204561), ("Sirilis", 204562)], "data_driven": True,
		"start_give_item": ("quest_1574a", 6),
		"steps": [{"type": "TALK", "names": ["Trou"], "remove_item": ("quest_1574a", 2)}, {"type": "TALK", "names": ["Arkos"], "remove_item": ("quest_1574a", 2)}, {"type": "TALK", "names": ["Sirilis"], "remove_item": ("quest_1574a", 2)}],
		"retail": {"name": "Q1574", "max_repeat_count": "1", "minlevel_permitted": "34", "finished_quest_cond1": "Q1573", "cannot_share": "1", "quest_work_item1": "quest_1574a 6", "reward_exp1": "161245", "reward_gold1": "0", "reward_item1_1": "potion_hp_mp_30a 8", "reward_item1_2": "coin_03 2", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Tree_Move_Terba", "give_item": "ITEM_QUEST_1574A 6", "talk_npc1": "Trou", "remove_item1": "ITEM_QUEST_1574A 2", "talk_npc2": "Arkos", "remove_item2": "ITEM_QUEST_1574A 2", "talk_npc3": "Sirilis", "remove_item3": "ITEM_QUEST_1574A 2", "reward_npc_name": "Tree_Move_Terba", "con_quest": "1575"},
		"base": {"minlevel_permitted": "34", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"exp": "161245"}, "reward_items": (("162000048", "8"), ("186000003", "2")), "work_items": (("182201736", "6"),), "finished_quests": ("1573",)},
		"evidence": ((344, ("L\"Arkos\"",)), (366, ("L\"Sirilis\"",)), (368, ("L\"Tree_Move_Terba\"",)), (368, ("L\"Trou\"",)), (548, (",0x626,3,0,0",)), (553, (",0x626,3,1,0",)), (558, (",0x626,3,2,0",)), (559, (",0x626,3,3,0",)), (574, (",0x626,0,0xffffffff,0",)), (595, (",0x626,4,0xffffffff,0",)), (822, ("FUN_180cab520(0x626", "0xadc2d88", "6")), (843, ("FUN_180cabb10(0x626",)), (848, ("FUN_180cabb10(0x626",)), (852, ("FUN_180cabb10(0x626",)), (853, ("FUN_180cabb10(0x626",)), (855, ("FUN_180caca90(0x626", "0xadc2d88", "6")), (859, ("+ 0x1b8", "0x626")), (873, ("+ 0x1a8", "0x626")))
	},
	1987: {
		"quest_hex": "0x7c3", "race": "pc_light", "base_race": "ELYOS", "start": "Fasimedes", "start_id": 203700,
		"end": "Bustant", "end_id": 203749, "talks": [],
		"retail": {"name": "Q1987", "max_repeat_count": "1", "minlevel_permitted": "29", "combineskill": "any", "combine_skillpoint": "400", "reward_exp1": "291412", "reward_gold1": "0", "reward_extend_inventory1": "2", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "29", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "combineskill": "-1", "combine_skillpoint": "400", "category": "SIGNIFICANT", "rewards": {"extend_inventory": "2", "exp": "291412"}, "reward_items": (), "work_items": ()},
		"evidence": ((611, ("L\"Bustant\"",)), (615, ("L\"Fasimedes\"",)), (690, (",0x7c3,0,0xffffffff,0",)), (695, (",0x7c3,4,0xffffffff,0",)), (881, ("FUN_180cab520(0x7c3", "0xff0,0x7c3")), (890, ("FUN_180cabb10(0x7c3", "FUN_180cacb30(0x7c3")), (904, ("FUN_180caca90(0x7c3",)))
	},
	2207: {
		"quest_hex": "0x89f", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Lycan_Messenger", "start_id": 203590,
		"end": "Lycan_Interpreter", "end_id": 203591, "talks": [("Lycan_Interpreter", 203591), ("Sueron", 203557)], "data_driven": True,
		"start_give_item": ("doc_quest_2207a", 1),
		"steps": [{"type": "TALK", "names": ["Lycan_Interpreter"]}, {"type": "TALK", "names": ["Sueron"], "remove_item": ("doc_quest_2207a", 1)}],
		"retail": {"name": "Q2207", "max_repeat_count": "1", "minlevel_permitted": "10", "cannot_share": "1", "quest_work_item1": "doc_quest_2207a", "reward_exp1": "18809", "reward_gold1": "0", "reward_item1_1": "potion_hp_mp_30a 5", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Lycan_Messenger", "give_item": "ITEM_DOC_QUEST_2207A 1", "talk_npc1": "Lycan_Interpreter", "talk_npc2": "Sueron", "remove_item2": "ITEM_DOC_QUEST_2207A 1", "reward_npc_name": "Lycan_Interpreter", "con_quest": "2208"},
		"base": {"minlevel_permitted": "10", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "IMPORTANT", "rewards": {"exp": "18809"}, "reward_items": (("162000048", "5"),), "work_items": (("182203257", "1"),)},
		"evidence": ((359, ("L\"Lycan_Interpreter\"",)), (359, ("L\"Lycan_Messenger\"",)), (366, ("L\"Sueron\"",)), (542, (",0x89f,3,0,0",)), (556, (",0x89f,3,1,0",)), (557, (",0x89f,3,2,0",)), (569, (",0x89f,0,0xffffffff,0",)), (589, (",0x89f,4,0xffffffff,0",)), (817, ("FUN_180cab520(0x89f", "0xadc3379", "1")), (838, ("FUN_180cabb10(0x89f",)), (851, ("FUN_180cabb10(0x89f",)), (852, ("FUN_180cabb10(0x89f",)), (855, ("FUN_180caca90(0x89f", "0xadc3379", "1")), (858, ("+ 0x1b8", "0x89f")))
	},
	2114: {
		"quest_hex": "0x842", "race": "pc_dark", "base_race": "ASMODIANS",
		"start": "Motgar", "start_id": 203533, "end": "Motgar", "end_id": 203533,
		"talks": [],
		"stages": [(["spakyD_4_n"], 10), (["MutaD_4_n", "MutaD_5_n"], 10)],
		"retail": {"name": "Q2114", "max_repeat_count": "1", "minlevel_permitted": "4", "reward_exp1": "2145", "reward_gold1": "760", "reward_item1_1": "shop_food_d_maxmp_10a 3", "race_permitted": "pc_dark"},
		"base": {"minlevel_permitted": "4", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "760", "exp": "2145"}, "reward_items": (("160003504", "3"), ("169300002", "50")), "work_items": (), "finished_quests": ("2110",)},
		"evidence": ((630, ('L"spakyD_4_n",0x842',)), (625, ('L"MutaD_4_n",0x842',)), (625, ('L"MutaD_5_n",0x842',)), (692, (",0x842,0,0xffffffff,0)",)), (698, (",0x842,4,0xffffffff,0)",))),
	},
	2278: {
		"quest_hex": "0x8e6", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Lycan_Messenger", "start_id": 203590,
		"end": "Sueron", "end_id": 203557, "talks": [("Sueron", 203557), ("Mimir", 204206), ("Balder", 204075)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Sueron"], "give_item": ("doc_quest_2278a", 1)}, {"type": "TALK", "names": ["Mimir"]}, {"type": "TALK", "names": ["Balder"], "remove_item": ("doc_quest_2278a", 1)}],
		"retail": {"name": "Q2278", "max_repeat_count": "1", "minlevel_permitted": "16", "finished_quest_cond1": "Q2208", "cannot_share": "1", "quest_work_item1": "doc_quest_2278a", "reward_exp1": "48600", "reward_gold1": "13870", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Lycan_Messenger", "talk_npc1": "Sueron", "give_item1": "ITEM_DOC_QUEST_2278A 1", "talk_npc2": "Mimir", "talk_npc3": "Balder", "remove_item3": "ITEM_DOC_QUEST_2278A 1", "reward_npc_name": "Sueron", "con_quest": "2279"},
		"base": {"minlevel_permitted": "17", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "13870", "exp": "48600"}, "reward_items": (), "work_items": (("182203254", "1"),), "finished_quests": ("2208",)},
		"evidence": ((344, ("L\"Balder\"",)), (359, ("L\"Lycan_Messenger\"",)), (359, ("L\"Mimir\"",)), (366, ("L\"Sueron\"",)), (546, (",0x8e6,3,0,0",)), (555, (",0x8e6,3,1,0",)), (557, (",0x8e6,3,2,0",)), (559, (",0x8e6,3,3,0",)), (569, (",0x8e6,0,0xffffffff,0",)), (594, (",0x8e6,4,0xffffffff,0",)), (817, ("FUN_180cab520(0x8e6",)), (842, ("FUN_180cabb10(0x8e6",)), (850, ("FUN_180cabb10(0x8e6",)), (852, ("FUN_180cabb10(0x8e6",)), (853, ("FUN_180cabb10(0x8e6",)), (859, ("+ 0x1b8", "0x8e6")))
	},
	2279: {
		"quest_hex": "0x8e7", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Sueron", "start_id": 203557,
		"end": "Sueron", "end_id": 203557, "talks": [("Lycan_Messenger", 203590), ("Soul_Zenkaka", 203682)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Lycan_Messenger"]}, {"type": "TALK", "names": ["Soul_Zenkaka"], "give_item": ("quest_2279a", 1)}],
		"retail": {"name": "Q2279", "max_repeat_count": "1", "minlevel_permitted": "16", "finished_quest_cond1": "Q2278", "cannot_share": "1", "quest_work_item1": "quest_2279a", "reward_exp1": "38100", "reward_gold1": "6420", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Sueron", "talk_npc1": "Lycan_Messenger", "talk_npc2": "Soul_Zenkaka", "give_item2": "ITEM_QUEST_2279A 1", "reward_npc_name": "Sueron", "con_quest": "2282"},
		"base": {"minlevel_permitted": "17", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "6420", "exp": "38100"}, "reward_items": (), "work_items": (("182203261", "1"),), "finished_quests": ("2278",)},
		"evidence": ((359, ("L\"Lycan_Messenger\"",)), (366, ("L\"Soul_Zenkaka\"",)), (366, ("L\"Sueron\"",)), (542, (",0x8e7,3,0,0",)), (556, (",0x8e7,3,1,0",)), (558, (",0x8e7,3,2,0",)), (573, (",0x8e7,0,0xffffffff,0",)), (594, (",0x8e7,4,0xffffffff,0",)), (807, ("+ 0x1a8", "0x8e7")), (821, ("FUN_180cab520(0x8e7",)), (838, ("FUN_180cabb10(0x8e7",)), (851, ("FUN_180cabb10(0x8e7",)), (852, ("FUN_180cabb10(0x8e7",)), (859, ("+ 0x1b8", "0x8e7")))
	},
	2985: {
		"quest_hex": "0xba9", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Vidar", "start_id": 204052,
		"end": "Roskva", "end_id": 204072, "talks": [],
		"retail": {"name": "Q2985", "max_repeat_count": "1", "minlevel_permitted": "29", "combineskill": "any", "combine_skillpoint": "400", "reward_exp1": "291412", "reward_gold1": "0", "reward_extend_inventory1": "2", "race_permitted": "pc_dark"},
		"base": {"minlevel_permitted": "29", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "combineskill": "-1", "combine_skillpoint": "400", "category": "SIGNIFICANT", "rewards": {"extend_inventory": "2", "exp": "291412"}, "reward_items": (), "work_items": ()},
		"evidence": ((626, ("DAT_18124e758",)), (630, ("L\"Vidar\"",)), (694, (",0xba9,0,0xffffffff,0",)), (699, (",0xba9,4,0xffffffff,0",)), (717, (",0xba9,3,0,0",)), (884, ("FUN_180cab520(0xba9", "0xff0,0xba9", "0xb73", "0xb74", "0xb75", "0xb76", "0xb77", "0xb78", "0x717a")), (890, ("FUN_180cabb10(0xba9", "FUN_180cacb30(0xba9")), (906, ("FUN_180caca90(0xba9",)))
	},
	2914: {
		"quest_hex": "0xb62", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Air", "start_id": 204147,
		"end": "Air", "end_id": 204147, "talks": [("Frana", 204236)], "data_driven": True,
		"step_give_item": ("quest_2914a", 1),
		"retail": {"name": "Q2914", "max_repeat_count": "1", "minlevel_permitted": "10", "finished_quest_cond1": "Q2912", "cannot_share": "1", "quest_work_item1": "quest_2914a", "reward_exp1": "8580", "reward_gold1": "0", "reward_item1_1": "ring_d_n_c1_q_10a 1", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Air", "talk_npc1": "Frana", "give_item1": "ITEM_QUEST_2914A 1", "reward_npc_name": "Air"},
		"base": {"minlevel_permitted": "10", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"exp": "8580"}, "reward_items": (("122000870", "1"),), "work_items": (("182207014", "1"),), "finished_quests": ("2912",)},
		"evidence": ((343, ("DAT_18124f0e0",)), (349, ("L\"Frana\"",)), (536, (",0xb62,3,0,0",)), (553, (",0xb62,3,1,0",)), (560, (",0xb62,0,0xffffffff,0",)), (580, (",0xb62,4,0xffffffff,0",)), (808, ("FUN_180cab520(0xb62",)), (831, ("FUN_180cabb10(0xb62",)), (848, ("FUN_180cabb10(0xb62",)))
	},
	3037: {
		"quest_hex": "0xbdd", "race": "pc_light", "base_race": "ELYOS", "start": "Grynos", "start_id": 798166,
		"end": "Grynos", "end_id": 798166, "talks": [("Ixion", 798199)], "data_driven": True,
		"step_give_item": ("quest_3037a", 1),
		"retail": {"name": "Q3037", "max_repeat_count": "1", "minlevel_permitted": "47", "quest_work_item1": "quest_3037a", "reward_exp1": "3884596", "reward_gold1": "0", "reward_item1_1": "SCROLL_RETURN_LF2B 3", "reward_item1_2": "coin_05 4", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Grynos", "talk_npc1": "Ixion", "give_item1": "ITEM_QUEST_3037A 1", "reward_npc_name": "Grynos"},
		"base": {"minlevel_permitted": "47", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"exp": "3884596"}, "reward_items": (("164000093", "3"), ("186000005", "4")), "work_items": (("182208027", "1"),)},
		"evidence": ((350, ("L\"Grynos\"",)), (352, ("L\"Ixion\"",)), (538, (",0xbdd,3,0,0",)), (554, (",0xbdd,3,1,0",)), (564, (",0xbdd,0,0xffffffff,0",)), (584, (",0xbdd,4,0xffffffff,0",)), (812, ("FUN_180cab520(0xbdd",)), (834, ("FUN_180cabb10(0xbdd",)), (849, ("FUN_180cabb10(0xbdd",)))
	},
	3044: {
		"quest_hex": "0xbe4", "race": "pc_light", "base_race": "ELYOS",
		"start": "LF2A_Wanted_Q3044", "start_id": 730145, "end": "Pygmalion", "end_id": 798206,
		"talks": [("Pygmalion", 798206)], "data_driven": True,
		"retail": {"name": "Q3044", "max_repeat_count": "1", "minlevel_permitted": "46", "reward_exp1": "3360640", "reward_gold1": "26210", "reward_item1_1": "coin_05 4", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "46", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"gold": "26210", "exp": "3360640"}, "reward_items": (("186000005", "4"),), "work_items": ()},
		"evidence": ((621, ('L"LF2A_Wanted_Q3044",0xbe4',)), (626, ('L"Pygmalion",0xbe4',)), (691, (",0xbe4,0,0xffffffff,0)",)), (699, (",0xbe4,4,0xffffffff,0)",)), (704, (",0xbe4,3,0,0)",)), (882, ("FUN_180caf640(0xbe4", "FUN_180caf350(0xbe4")), (888, ("FUN_180caf350(0xbe4",)), (893, ("FUN_180caf740(0xbe4", "0x3f1", "FUN_180caf3c0(0xbe4"))),
	},
	3056: {
		"quest_hex": "0xbf0", "race": "pc_light", "base_race": "ELYOS",
		"start": "LF2A_Wanted_Q3056", "start_id": 730147, "end": "Siraus", "end_id": 798213,
		"talks": [("Siraus", 798213)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Siraus"]}, {"type": "HUNT", "names": ["LF2A_SpectreFxQ_50_An"], "amount": 1}],
		"retail": {"name": "Q3056", "max_repeat_count": "1", "minlevel_permitted": "49", "reward_exp1": "5390338", "reward_gold1": "0", "reward_item1_1": "coin_06 6", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "49", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"exp": "5390338"}, "reward_items": (("186000018", "6"),), "work_items": ()},
		"evidence": ((621, ('L"LF2A_Wanted_Q3056",0xbf0',)), (621, ('L"LF2A_SpectreFxQ_50_An",0xbf0',)), (627, ('L"Siraus",0xbf0',)), (691, (",0xbf0,0,0xffffffff,0)",)), (704, (",0xbf0,3,0,0)",)), (700, (",0xbf0,4,0xffffffff,0)",))),
	},
	3020: {
		"quest_hex": "0xbcc", "race": "pc_light", "base_race": "ELYOS", "start": "Ankises", "start_id": 798143,
		"end": "Ankises", "end_id": 798143, "talks": [("NPC_Agrint_Tartagan", 798149)], "data_driven": True, "movie": 363,
		"start_give_item": ("quest_3020a", 1), "step_remove_item": ("quest_3020a", 1),
		"retail": {"name": "Q3020", "max_repeat_count": "1", "minlevel_permitted": "21", "cannot_share": "1", "quest_work_item1": "quest_3020a", "reward_exp1": "80494", "reward_gold1": "6480", "reward_item1_1": "coin_02 1", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Ankises", "give_item": "ITEM_QUEST_3020A 1", "talk_npc1": "NPC_Agrint_Tartagan", "remove_item1": "ITEM_QUEST_3020A 1", "reward_npc_name": "Ankises", "cutsceneid1": "363", "cs1_haction": "1007"},
		"base": {"minlevel_permitted": "21", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"gold": "6480", "exp": "80494"}, "reward_items": (("186000002", "1"),), "work_items": (("182208011", "1"),)},
		"evidence": ((344, ("L\"Ankises\"",)), (360, ("L\"NPC_Agrint_Tartagan\"",)), (543, (",0xbcc,3,0,0",)), (553, (",0xbcc,3,1,0",)), (560, (",0xbcc,0,0xffffffff,0",)), (580, (",0xbcc,4,0xffffffff,0",)), (808, ("FUN_180cab520(0xbcc", "0xadc460b", "1", "FUN_180cacb30(0xbcc")), (838, ("FUN_180cabb10(0xbcc", "FUN_180cacb30(0xbcc")), (848, ("FUN_180cabb10(0xbcc", "FUN_180cacb30(0xbcc")), (853, ("FUN_180caca90(0xbcc", "0xadc460b", "1")))
	},
	3031: {
		"quest_hex": "0xbd7", "race": "pc_light", "base_race": "ELYOS",
		"start": "LF2A_Wanted_Q3031", "start_id": 730144, "end": "Eunomia", "end_id": 798172,
		"talks": [],
		"stages": [(["LF2A_TesinonSeamanM_46_An", "LF2A_TesinonSeamanM_47_An"], 15), (["LF2A_TesinonSeamanR_46_An", "LF2A_TesinonSeamanR_47_An"], 12)],
		"retail": {"name": "Q3031", "max_repeat_count": "1", "minlevel_permitted": "46", "reward_exp1": "6620429", "reward_gold1": "61620", "reward_item1_1": "coin_05 4", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "46", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ELYOS", "category": "IMPORTANT", "rewards": {"gold": "61620", "exp": "6620429"}, "reward_items": (("186000005", "4"),), "work_items": ()},
		"evidence": ((621, ('L"LF2A_Wanted_Q3031",0xbd7)',)), (614, ('L"Eunomia",0xbd7)',)), (621, ('L"LF2A_TesinonSeamanM_46_An",0xbd7)',)), (621, ('L"LF2A_TesinonSeamanR_46_An",0xbd7)',)), (691, (",0xbd7,0,0xffffffff,0)",)), (716, (",0xbd7,3,0xc3c0,0)",)), (696, (",0xbd7,4,0xffffffff,0)",)), (867, ("FUN_180caa850(0xbd7", "0xf,2)")), (867, ("FUN_180caa850(0xbd7", "0xc,3)"))),
	},
	3076: {
		"quest_hex": "0xc04", "race": "pc_light", "base_race": "ELYOS", "start": "Atropos", "start_id": 798155,
		"end": "Atropos", "end_id": 798155, "talks": [("Ascalon", 278503), ("Cymaon", 278556)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Ascalon"]}, {"type": "TALK", "names": ["Cymaon"], "give_item": ("quest_3076a", 1)}],
		"retail": {"name": "Q3076", "max_repeat_count": "1", "minlevel_permitted": "45", "quest_work_item1": "quest_3076a", "reward_exp1": "2954681", "reward_gold1": "27300", "reward_item1_1": "coin_05 4", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Atropos", "talk_npc1": "Ascalon", "talk_npc2": "Cymaon", "give_item2": "ITEM_QUEST_3076A 1", "reward_npc_name": "Atropos"},
		"base": {"minlevel_permitted": "45", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"gold": "27300", "exp": "2954681"}, "reward_items": (("186000005", "4"),), "work_items": (("182208047", "1"),)},
		"evidence": ((344, ("L\"Ascalon\"",)), (344, ("L\"Atropos\"",)), (346, ("L\"Cymaon\"",)), (533, (",0xc04,3,0,0",)), (553, (",0xc04,3,1,0",)), (557, (",0xc04,3,2,0",)), (560, (",0xc04,0,0xffffffff,0",)), (580, (",0xc04,4,0xffffffff,0",)), (809, ("FUN_180cab520(0xc04",)), (828, ("FUN_180cabb10(0xc04",)), (849, ("FUN_180cabb10(0xc04",)), (852, ("FUN_180cabb10(0xc04",)))
	},
	2321: {
		"quest_hex": "0x911", "race": "pc_dark", "base_race": "ASMODIANS", "start_type": "ITEM_PLAY", "start": "quest_2321b",
		"end": "Hellione", "end_id": 790018, "talks": [("Gunter", 204225)], "data_driven": True, "definition": "use_item",
		"steps": [{"type": "TALK", "names": ["Gunter"], "give_item": ("doc_quest_2321a", 1), "remove_item": ("quest_2321b", 1)}],
		"retail": {"name": "Q2321", "max_repeat_count": "1", "minlevel_permitted": "26", "cannot_share": "1", "quest_work_item1": "quest_2321b", "quest_work_item2": "doc_quest_2321a", "reward_exp1": "225900", "reward_gold1": "0", "selectable_reward_item1_1": "sword_d_n_l1_q_26a 1", "selectable_reward_item1_2": "mace_d_n_l1_q_26a 1", "selectable_reward_item1_3": "polearm_d_n_l1_q_26a 1", "selectable_reward_item1_4": "bow_d_n_l1_q_26a 1", "selectable_reward_item1_5": "book_d_n_l1_q_26a 1", "selectable_reward_item1_6": "cannon_d_n_l1_q_26a 1", "selectable_reward_item1_7": "harp_d_n_l1_q_26a 1", "selectable_reward_item1_8": "keyblade_d_n_l1_q_26a 1", "race_permitted": "pc_dark"},
		"talk": {"use_item_name": "ITEM_QUEST_2321B", "talk_npc1": "Gunter", "give_item1": "ITEM_DOC_QUEST_2321A 1", "remove_item1": "ITEM_QUEST_2321B 1", "reward_npc_name": "Hellione"},
		"base": {"minlevel_permitted": "26", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"exp": "225900"}, "reward_items": (), "selectable_reward_items": (("100000643", "1"), ("100100498", "1"), ("101300481", "1"), ("101700518", "1"), ("100600535", "1"), ("101900495", "1"), ("102000526", "1"), ("102100854", "1")), "work_items": (("182204242", "1"), ("182204119", "1"))},
		"evidence": ((601, ("L\"Gunter\"",)), (601, ("L\"Hellione\"",)), (603, ("FUN_180cb2eb0", "0x911,5,0xadc3752,FUN_180eb77c0")), (606, (",0x911,3,0,0",)), (607, (",0x911,3,1,0",)), (608, (",0x911,4,0xffffffff,0",)), (860, ("0x911", "+ 0xd0", "+ 0x2c8", "+ 0x318")), (861, ("FUN_180cabb10(0x911",)), (862, ("FUN_180cabb10(0x911",)))
	},
	2428: {
		"quest_hex": "0x97c", "race": "pc_dark", "base_race": "ASMODIANS", "start": "kistig", "start_id": 204433,
		"end": "kistig", "end_id": 204433, "talks": [("Honir", 204102), ("Moreinen", 204211)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Honir"]}, {"type": "TALK", "names": ["Moreinen"], "give_item": ("quest_2428a", 1)}],
		"retail": {"name": "Q2428", "max_repeat_count": "1", "minlevel_permitted": "23", "finished_quest_cond1": "Q2427", "cannot_share": "1", "quest_work_item1": "quest_2428a", "reward_exp1": "128850", "reward_gold1": "31680", "reward_item1_1": "rec_D_al_potion_hp_mp_20a 1", "reward_item1_2": "coin_d_02 1", "selectable_reward_item1_1": "ring_d_n_r1_q_24a 1", "selectable_reward_item1_2": "ring_d_n_r1_q_24b 1", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "kistig", "talk_npc1": "Honir", "talk_npc2": "Moreinen", "give_item2": "ITEM_QUEST_2428A 1", "reward_npc_name": "kistig"},
		"base": {"minlevel_permitted": "23", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "31680", "exp": "128850"}, "reward_items": (("152232142", "1"), ("186000007", "1")), "selectable_reward_items": (("122001288", "1"), ("122001289", "1")), "work_items": (("182204216", "1"),), "finished_quests": ("2427",)},
		"evidence": ((351, ("DAT_18124edf0",)), (360, ("L\"Moreinen\"",)), (374, ("L\"kistig\"",)), (537, (",0x97c,3,0,0",)), (555, (",0x97c,3,1,0",)), (558, (",0x97c,3,2,0",)), (579, (",0x97c,0,0xffffffff,0",)), (600, (",0x97c,4,0xffffffff,0",)), (827, ("FUN_180cab520(0x97c",)), (833, ("FUN_180cabb10(0x97c",)), (850, ("FUN_180cabb10(0x97c",)), (853, ("FUN_180cabb10(0x97c",)))
	},
	2449: {
		"quest_hex": "0x991", "race": "pc_dark", "base_race": "ASMODIANS",
		"start": "Shugo_DF2_4", "start_id": 798080, "end": "Shugo_DF2_4", "end_id": 798080,
		"talks": [("Shugo_DF2_15", 798115)], "data_driven": True,
		"retail": {"name": "Q2449", "max_repeat_count": "1", "minlevel_permitted": "37", "reward_exp1": "1496758", "reward_gold1": "0", "reward_item1_1": "coin_d_04 3", "race_permitted": "pc_dark"},
		"base": {"minlevel_permitted": "37", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"exp": "1496758"}, "reward_items": (("186000009", "3"),), "selectable_reward_items": (("111100768", "1"), ("111300773", "1"), ("111500756", "1"), ("111600748", "1")), "work_items": (), "finished_quests": ("2448",)},
		"evidence": ((627, ('L"Shugo_DF2_4",0x991',)), (627, ('L"Shugo_DF2_15",0x991',)), (693, (",0x991,0,0xffffffff,0)",)), (699, (",0x991,4,0xffffffff,0)",)), (704, (",0x991,3,0,0)",)), (883, ("FUN_180caf640(0x991", "FUN_180caf350(0x991")), (889, ("FUN_180caf6c0(0x991",)), (894, ("FUN_180caf740(0x991", "FUN_180caf3c0(0x991")), (905, (",0x3eb,0x991)",)), (909, (",0x3ec,0x991)",))),
	},
	2421: {
		"quest_hex": "0x975", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Asgeirr", "start_id": 204309,
		"end": "Asgeirr", "end_id": 204309, "talks": [("Kerupnise", 204187)], "data_driven": True, "movie": 132,
		"start_give_item": ("quest_2421a", 1), "step_give_item": ("quest_2421b", 1), "step_remove_item": ("quest_2421a", 1),
		"retail": {"name": "Q2421", "max_repeat_count": "1", "minlevel_permitted": "20", "cannot_share": "1", "quest_work_item1": "quest_2421a", "quest_work_item2": "quest_2421b", "reward_exp1": "50008", "reward_gold1": "15880", "reward_item1_1": "coin_d_02 1", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Asgeirr", "give_item": "ITEM_QUEST_2421A 1", "talk_npc1": "Kerupnise", "give_item1": "ITEM_QUEST_2421B 1", "remove_item1": "ITEM_QUEST_2421A 1", "reward_npc_name": "Asgeirr", "cutsceneid1": "132", "cs1_haction": "1353"},
		"base": {"minlevel_permitted": "30", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"exp": "50008", "gold": "15880"}, "reward_items": (("186000007", "1"),), "work_items": (("182204208", "1"), ("182204209", "1"))},
		"evidence": ((344, ("L\"Asgeirr\"",)), (353, ("L\"Kerupnise\"",)), (538, (",0x975,3,0,0",)), (553, (",0x975,3,1,0",)), (560, (",0x975,0,0xffffffff,0",)), (580, (",0x975,4,0xffffffff,0",)), (808, ("FUN_180cab520(0x975", "0xadc3730", "1")), (834, ("FUN_180cabb10(0x975", "FUN_180cacb30(0x975")), (849, ("FUN_180cabb10(0x975", "FUN_180cacb30(0x975")), (853, ("FUN_180caca90(0x975", "0xadc3730", "1")))
	},
	2458: {
		"quest_hex": "0x99a", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Lif", "start_id": 204379,
		"end": "Lif", "end_id": 204379, "talks": [("DF2_NPC_Sprigg", 204386)], "data_driven": True,
		"start_give_item": ("quest_2458a", 1), "step_give_item": ("doc_quest_2458b", 1), "step_remove_item": ("quest_2458a", 1),
		"retail": {"name": "Q2458", "max_repeat_count": "1", "minlevel_permitted": "23", "finished_quest_cond1": "Q2457", "cannot_share": "1", "quest_work_item1": "quest_2458a", "quest_work_item2": "doc_quest_2458b", "reward_exp1": "20020", "reward_gold1": "4470", "reward_item1_1": "coin_d_02 1", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Lif", "give_item": "ITEM_QUEST_2458A 1", "talk_npc1": "DF2_NPC_Sprigg", "give_item1": "ITEM_DOC_QUEST_2458B 1", "remove_item1": "ITEM_QUEST_2458A 1", "reward_npc_name": "Lif"},
		"base": {"minlevel_permitted": "22", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "4470", "exp": "20020"}, "reward_items": (("186000007", "1"),), "work_items": (("182204194", "1"), ("182204195", "1")), "finished_quests": ("2457",)},
		"evidence": ((346, ("L\"DF2_NPC_Sprigg\"",)), (358, ("DAT_18133aba8",)), (534, (",0x99a,3,0,0",)), (555, (",0x99a,3,1,0",)), (568, (",0x99a,0,0xffffffff,0",)), (589, (",0x99a,4,0xffffffff,0",)), (806, ("+ 0x1a8", "0x99a")), (817, ("FUN_180cab520(0x99a", "0xadc3722", "1")), (830, ("FUN_180cabb10(0x99a",)), (850, ("FUN_180cabb10(0x99a",)), (855, ("FUN_180caca90(0x99a", "0xadc3722", "1")), (858, ("+ 0x1b8", "0x99a")))
	},
	2480: {
		"quest_hex": "0x9b0", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Tree_Move_Nabalu", "start_id": 730038,
		"end": "Tree_Move_Nabalu", "end_id": 730038, "talks": [("Tree_Move_virdi", 730021), ("Tree_NoMove_Lodas", 730019)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Tree_Move_virdi"], "give_item": ("quest_2480a", 1)}, {"type": "TALK", "names": ["Tree_NoMove_Lodas"], "give_item": ("quest_2480b", 1)}],
		"retail": {"name": "Q2480", "max_repeat_count": "1", "minlevel_permitted": "21", "finished_quest_cond1": "Q2479", "cannot_share": "1", "quest_work_item1": "quest_2480a", "quest_work_item2": "quest_2480b", "reward_exp1": "142950", "reward_gold1": "0", "reward_abyss_point1": "100", "reward_item1_1": "medal_07 2", "selectable_reward_item1_1": "rb_torso_d_n_r0_q_23a 1", "selectable_reward_item1_2": "lt_torso_d_n_r0_q_23a 1", "selectable_reward_item1_3": "ch_torso_d_n_r0_q_23a 1", "selectable_reward_item1_4": "pl_torso_d_n_r0_q_23a 1", "reward_title1": "dark_title17", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Tree_Move_Nabalu", "talk_npc1": "Tree_Move_virdi", "give_item1": "ITEM_QUEST_2480A 1", "talk_npc2": "Tree_NoMove_Lodas", "give_item2": "ITEM_QUEST_2480B 1", "reward_npc_name": "Tree_Move_Nabalu"},
		"base": {"minlevel_permitted": "21", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"exp": "142950", "ap": "100", "title": "67"}, "reward_items": (("186000469", "2"),), "selectable_reward_items": (("110100861", "1"), ("110300816", "1"), ("110500786", "1"), ("110600772", "1")), "work_items": (("182204201", "1"), ("182204202", "1")), "finished_quests": ("2479",)},
		"evidence": ((368, ("L\"Tree_Move_Nabalu\"",)), (368, ("L\"Tree_Move_virdi\"",)), (368, ("L\"Tree_NoMove_Lodas\"",)), (547, (",0x9b0,3,0,0",)), (556, (",0x9b0,3,1,0",)), (558, (",0x9b0,3,2,0",)), (574, (",0x9b0,0,0xffffffff,0",)), (595, (",0x9b0,4,0xffffffff,0",)), (807, ("+ 0x1a8", "0x9b0")), (822, ("FUN_180cab520(0x9b0",)), (843, ("FUN_180cabb10(0x9b0",)), (851, ("FUN_180cabb10(0x9b0",)), (852, ("FUN_180cabb10(0x9b0",)))
	},
	4015: {
		"quest_hex": "0xfaf", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Vinduer", "start_id": 205130,
		"end": "Vinduer", "end_id": 205130, "talks": [("DF2A_FOBJ_Q4015", 730107)], "data_driven": True, "movie": 394,
		"retail": {"name": "Q4015", "max_repeat_count": "1", "minlevel_permitted": "24", "reward_exp1": "134338", "reward_gold1": "0", "reward_item1_1": "wrap_q_matter_enchant_20a 1", "reward_item1_2": "coin_d_02 1", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Vinduer", "talk_npc1": "DF2A_FOBJ_Q4015", "reward_npc_name": "Vinduer", "con_quest": "4016", "cutsceneid1": "394", "cs1_haction": "1353"},
		"base": {"minlevel_permitted": "24", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"exp": "134338"}, "reward_items": (("188051192", "1"), ("186000007", "1")), "work_items": ()},
		"evidence": ((346, ("L\"DF2A_FOBJ_Q4015\"",)), (369, ("L\"Vinduer\"",)), (534, (",0xfaf,3,0,0",)), (556, (",0xfaf,3,1,0",)), (575, (",0xfaf,0,0xffffffff,0",)), (596, (",0xfaf,4,0xffffffff,0",)), (823, ("FUN_180cab520(0xfaf", "FUN_180cacb30(0xfaf")), (829, ("FUN_180cabb10(0xfaf", "FUN_180cacb30(0xfaf")), (851, ("FUN_180cabb10(0xfaf", "FUN_180cacb30(0xfaf")), (859, ("+ 0x1b8", "0xfaf")))
	},
	11010: {
		"quest_hex": "0x2b02", "race": "pc_light", "base_race": "ELYOS", "start": "Pucio", "start_id": 798931,
		"end": "Naiting", "end_id": 799071, "talks": [("Naiting", 799071), ("Lionel", 798906), ("LF4_FOBJ_Q11010A", 730323)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Naiting"]}, {"type": "TALK", "names": ["Lionel"]}, {"type": "TALK", "names": ["LF4_FOBJ_Q11010A"], "give_item": ("quest_11010a", 1)}],
		"retail": {"name": "Q11010", "max_repeat_count": "1", "minlevel_permitted": "50", "cannot_share": "1", "quest_work_item1": "quest_11010a 1", "reward_exp1": "2924378", "reward_gold1": "5690", "reward_item1_1": "coin_06 6", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Pucio", "talk_npc1": "Naiting", "talk_npc2": "Lionel", "talk_npc3": "LF4_FOBJ_Q11010A", "give_item3": "ITEM_QUEST_11010A 1", "reward_npc_name": "Naiting"},
		"base": {"minlevel_permitted": "50", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"gold": "5690", "exp": "2924378"}, "reward_items": (("186000018", "6"),), "work_items": (("182206713", "1"),)},
		"evidence": ((357, ("L\"LF4_FOBJ_Q11010A\"",)), (358, ("L\"Lionel\"",)), (360, ("L\"Naiting\"",)), (362, ("L\"Pucio\"",)), (543, (",0x2b02,3,0,0",)), (555, (",0x2b02,3,1,0",)), (557, (",0x2b02,3,2,0",)), (558, (",0x2b02,3,3,0",)), (570, (",0x2b02,0,0xffffffff,0",)), (590, (",0x2b02,4,0xffffffff,0",)), (819, ("FUN_180cab520(0x2b02",)), (838, ("FUN_180cabb10(0x2b02",)), (850, ("FUN_180cabb10(0x2b02",)), (852, ("FUN_180cabb10(0x2b02",)), (853, ("FUN_180cabb10(0x2b02",)))
	},
	14200: {
		"quest_hex": "0x3778", "race": "pc_light", "base_race": "ELYOS",
		"start": "Atropos", "start_id": 798155, "end": "Atropos", "end_id": 798155,
		"talks": [], "data_driven": True,
		"steps": [{"type": "HUNT", "names": ["LF2a_FOBJ_Q1092"], "amount": 3}],
		"retail": {"name": "Q14200", "max_repeat_count": "1", "minlevel_permitted": "45", "reward_exp1": "1181639", "reward_gold1": "65400", "reward_item1_1": "potion_hp_mp_50a 20", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "45", "max_repeat_count": "1", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"gold": "65400", "exp": "1181639"}, "reward_items": (("162000050", "20"), ("188053060", "2")), "work_items": ()},
		"evidence": ((145, ('L"Atropos",0x3778',)), (183, ('L"LF2a_FOBJ_Q1092",0x3778',)), (314, (",0x3778,0,0xffffffff,0)",)), (301, (",0x3778,3,0x40000000,0)",)), (308, (",0x3778,3,3,0)",)), (325, (",0x3778,4,0xffffffff,0)",)), (772, ("FUN_180cb13b0(0x3778",))),
	},
	2512: {
		"quest_hex": "0x9d0", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Loki", "start_id": 204703,
		"end": "Kistenian", "end_id": 204753, "talks": [("Gigrite", 204801)], "data_driven": True,
		"start_give_item": ("quest_2512a", 2), "step_remove_item": ("quest_2512a", 1),
		"retail": {"name": "Q2512", "max_repeat_count": "1", "minlevel_permitted": "35", "cannot_share": "1", "quest_work_item1": "quest_2512a 2", "reward_exp1": "1301093", "reward_gold1": "26170", "reward_item1_1": "potion_hp_mp_30a 3", "reward_item1_2": "coin_d_03 2", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Loki", "give_item": "ITEM_QUEST_2512A 2", "talk_npc1": "Gigrite", "remove_item1": "ITEM_QUEST_2512A 1", "reward_npc_name": "Kistenian"},
		"base": {"minlevel_permitted": "35", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "26170", "exp": "1301093"}, "reward_items": (("162000048", "3"), ("186000008", "2")), "work_items": (("182204411", "2"),)},
		"evidence": ((350, ("L\"Gigrite\"",)), (353, ("L\"Kistenian\"",)), (358, ("L\"Loki\"",)), (536, (",0x9d0,3,0,0",)), (554, (",0x9d0,3,1,0",)), (569, (",0x9d0,0,0xffffffff,0",)), (586, (",0x9d0,4,0xffffffff,0",)), (817, ("FUN_180cab520(0x9d0", "0xadc37fb")), (832, ("FUN_180cabb10(0x9d0",)), (849, ("FUN_180cabb10(0x9d0",)), (855, ("FUN_180caca90(0x9d0", "0xadc37fb")))
	},
	2515: {
		"quest_hex": "0x9d3", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Elli", "start_id": 790015,
		"end": "Elli", "end_id": 790015, "talks": [("Araison", 204192), ("Mareke", 204205), ("Shugo_DF2_5", 798081)], "data_driven": True,
		"start_give_item": ("doc_quest_2515a", 1),
		"steps": [{"type": "TALK", "names": ["Araison"], "remove_item": ("doc_quest_2515a", 1)}, {"type": "TALK", "names": ["Mareke"], "give_item": ("quest_2515c", 1)}, {"type": "TALK", "names": ["Shugo_DF2_5"], "give_item": ("quest_2515e", 1)}],
		"retail": {"name": "Q2515", "max_repeat_count": "1", "minlevel_permitted": "31", "cannot_share": "1", "quest_work_item1": "doc_quest_2515a", "quest_work_item2": "quest_2515c", "quest_work_item3": "quest_2515e", "reward_exp1": "340413", "reward_gold1": "30160", "selectable_reward_item1_1": "rb_pants_d_n_r1_nc_31a 1", "selectable_reward_item1_2": "lt_pants_d_n_r1_nc_31a 1", "selectable_reward_item1_3": "ch_pants_d_n_r1_nc_31a 1", "selectable_reward_item1_4": "pl_pants_d_n_r1_nc_31a 1", "selectable_reward_item1_5": "ch_pants_d_n_r1_nc_31b 1", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Elli", "give_item": "ITEM_DOC_QUEST_2515A 1", "talk_npc1": "Araison", "remove_item1": "ITEM_DOC_QUEST_2515A 1", "talk_npc2": "Mareke", "give_item2": "ITEM_QUEST_2515C 1", "talk_npc3": "Shugo_DF2_5", "give_item3": "ITEM_QUEST_2515E 1", "reward_npc_name": "Elli"},
		"base": {"minlevel_permitted": "31", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "30160", "exp": "340413"}, "reward_items": (), "selectable_reward_items": (("113101098", "1"), ("113301101", "1"), ("113501077", "1"), ("113601042", "1"), ("113501596", "1")), "work_items": (("182204412", "1"), ("182204414", "1"), ("182204416", "1"))},
		"evidence": ((344, ("L\"Araison\"",)), (348, ("L\"Elli\"",)), (359, ("L\"Mareke\"",)), (364, ("L\"Shugo_DF2_5\"",)), (533, (",0x9d3,3,0,0",)), (555, (",0x9d3,3,1,0",)), (558, (",0x9d3,3,2,0",)), (558, (",0x9d3,3,3,0",)), (563, (",0x9d3,0,0xffffffff,0",)), (583, (",0x9d3,4,0xffffffff,0",)), (811, ("FUN_180cab520(0x9d3", "0xadc37fc", "1")), (828, ("FUN_180cabb10(0x9d3",)), (850, ("FUN_180cabb10(0x9d3",)), (852, ("FUN_180cabb10(0x9d3",)), (853, ("FUN_180cabb10(0x9d3",)), (854, ("FUN_180caca90(0x9d3", "0xadc37fc", "1")))
	},
	2523: {
		"quest_hex": "0x9db", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Svera", "start_id": 204802,
		"end": "Horu", "end_id": 204734, "talks": [("Shugo_DF3_10", 798117), ("Shugo_DF3_11", 798118), ("Shugo_DF3_12", 798119)], "data_driven": True,
		"start_give_item": ("quest_2523a", 3),
		"steps": [{"type": "TALK", "names": ["Shugo_DF3_10"], "remove_item": ("quest_2523a", 1)}, {"type": "TALK", "names": ["Shugo_DF3_11"], "remove_item": ("quest_2523a", 1)}, {"type": "TALK", "names": ["Shugo_DF3_12"], "remove_item": ("quest_2523a", 1)}],
		"retail": {"name": "Q2523", "max_repeat_count": "1", "minlevel_permitted": "32", "finished_quest_cond1": "Q2508", "cannot_share": "1", "quest_work_item1": "quest_2523a 3", "reward_exp1": "404557", "reward_gold1": "33320", "reward_item1_1": "coin_d_03 2", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Svera", "give_item": "ITEM_QUEST_2523A 3", "talk_npc1": "Shugo_DF3_10", "remove_item1": "ITEM_QUEST_2523A 1", "talk_npc2": "Shugo_DF3_11", "remove_item2": "ITEM_QUEST_2523A 1", "talk_npc3": "Shugo_DF3_12", "remove_item3": "ITEM_QUEST_2523A 1", "reward_npc_name": "Horu"},
		"base": {"minlevel_permitted": "32", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "33320", "exp": "404557"}, "reward_items": (("186000008", "2"),), "work_items": (("182204417", "3"),), "finished_quests": ("2508",)},
		"evidence": ((351, ("L\"Horu\"",)), (364, ("L\"Shugo_DF3_10\"",)), (364, ("L\"Shugo_DF3_11\"",)), (364, ("L\"Shugo_DF3_12\"",)), (366, ("L\"Svera\"",)), (545, (",0x9db,3,0,0",)), (556, (",0x9db,3,1,0",)), (558, (",0x9db,3,2,0",)), (558, (",0x9db,3,3,0",)), (573, (",0x9db,0,0xffffffff,0",)), (585, (",0x9db,4,0xffffffff,0",)), (807, ("+ 0x1a8", "0x9db")), (821, ("FUN_180cab520(0x9db", "0xadc3801", "3")), (841, ("FUN_180cabb10(0x9db",)), (851, ("FUN_180cabb10(0x9db",)), (852, ("FUN_180cabb10(0x9db",)), (853, ("FUN_180cabb10(0x9db",)), (855, ("FUN_180caca90(0x9db", "0xadc3801", "3")))
	},
	2692: {
		"quest_hex": "0xa84", "race": "pc_dark", "base_race": "ASMODIANS", "start": "LabB_BeholderNamedQ_43_Ae", "start_id": 212164,
		"end": "LabB_BeholderNamedQ_43_Ae", "end_id": 212164, "talks": [("Lanse", 204108), ("Shugo_AB1_D3", 279027), ("Ab1_NPC_LugBug", 279029)], "data_driven": True,
		"start_give_item": ("quest_2692a", 1),
		"steps": [{"type": "TALK", "names": ["Lanse"]}, {"type": "TALK", "names": ["Shugo_AB1_D3"]}, {"type": "TALK", "names": ["Ab1_NPC_LugBug"], "give_item": ("quest_2692b", 1), "remove_item": ("quest_2692a", 1)}],
		"retail": {"name": "Q2692", "max_repeat_count": "1", "minlevel_permitted": "40", "quest_work_item1": "quest_2692a", "quest_work_item2": "quest_2692b", "reward_exp1": "579599", "reward_gold1": "0", "reward_item1_1": "belt_d_n_r1_q_41a 1", "reward_item1_2": "coin_d_04 3", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "LabB_BeholderNamedQ_43_Ae", "give_item": "ITEM_QUEST_2692A 1", "talk_npc1": "Lanse", "talk_npc2": "Shugo_AB1_D3", "talk_npc3": "Ab1_NPC_LugBug", "give_item3": "ITEM_QUEST_2692B 1", "remove_item3": "ITEM_QUEST_2692A 1", "reward_npc_name": "LabB_BeholderNamedQ_43_Ae"},
		"base": {"minlevel_permitted": "40", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"exp": "579599"}, "reward_items": (("123000875", "1"), ("186000009", "3")), "work_items": (("182204510", "1"), ("182204511", "1"))},
		"evidence": ((343, ("L\"Ab1_NPC_LugBug\"",)), (357, ("L\"LabB_BeholderNamedQ_43_Ae\"",)), (358, ("L\"Lanse\"",)), (363, ("L\"Shugo_AB1_D3\"",)), (541, (",0xa84,3,0,0",)), (556, (",0xa84,3,1,0",)), (557, (",0xa84,3,2,0",)), (558, (",0xa84,3,3,0",)), (568, (",0xa84,0,0xffffffff,0",)), (588, (",0xa84,4,0xffffffff,0",)), (816, ("FUN_180cab520(0xa84", "0xadc385e", "1")), (837, ("FUN_180cabb10(0xa84",)), (851, ("FUN_180cabb10(0xa84", "&DAT_184710408")), (851, ("FUN_180cabb10(0xa84", "&DAT_184710448")), (853, ("FUN_180cabb10(0xa84",)), (854, ("FUN_180caca90(0xa84", "0xadc385e", "1")))
	},
	4501: {
		"quest_hex": "0x1195", "race": "pc_dark", "base_race": "ASMODIANS", "start": "Lapion", "start_id": 204728,
		"end": "Lapion", "end_id": 204728, "talks": [("Tekor", 204340), ("Virashak", 204348)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Tekor"]}, {"type": "TALK", "names": ["Virashak"], "give_item": ("quest_4501a", 1)}],
		"retail": {"name": "Q4501", "max_repeat_count": "1", "minlevel_permitted": "31", "quest_work_item1": "quest_4501a", "reward_exp1": "340413", "reward_gold1": "76310", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "Lapion", "talk_npc1": "Tekor", "talk_npc2": "Virashak", "give_item2": "ITEM_QUEST_4501A 1", "reward_npc_name": "Lapion"},
		"base": {"minlevel_permitted": "31", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "76310", "exp": "340413"}, "reward_items": (), "work_items": (("182204533", "1"),)},
		"evidence": ((358, ("L\"Lapion\"",)), (367, ("L\"Tekor\"",)), (369, ("L\"Virashak\"",)), (547, (",0x1195,3,0,0",)), (556, (",0x1195,3,1,0",)), (557, (",0x1195,3,2,0",)), (568, (",0x1195,0,0xffffffff,0",)), (589, (",0x1195,4,0xffffffff,0",)), (816, ("FUN_180cab520(0x1195", "0,0")), (842, ("FUN_180cabb10(0x1195",)), (851, ("FUN_180cabb10(0x1195",)), (852, ("FUN_180cabb10(0x1195",)))
	},
	3035: {
		"quest_hex": "0xbdb", "race": "pc_light", "base_race": "ELYOS", "start": "Atropos", "start_id": 798155,
		"end": "Atropos", "end_id": 798155, "talks": [("Vatonia", 203830), ("Ab1_NPC_LugBug", 279029)], "data_driven": True,
		"start_give_item": ("quest_3035a", 1),
		"steps": [{"type": "TALK", "names": ["Vatonia"]}, {"type": "TALK", "names": ["Ab1_NPC_LugBug"], "give_item": ("quest_3035b", 1), "remove_item": ("quest_3035a", 1)}],
		"retail": {"name": "Q3035", "max_repeat_count": "1", "minlevel_permitted": "47", "finished_quest_cond1": "Q3034", "cannot_share": "1", "quest_work_item1": "quest_3035a", "quest_work_item2": "quest_3035b", "reward_exp1": "6937236", "reward_gold1": "44360", "reward_item1_1": "coin_05 4", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Atropos", "give_item": "ITEM_QUEST_3035A 1", "talk_npc1": "Vatonia", "talk_npc2": "Ab1_NPC_LugBug", "give_item2": "ITEM_QUEST_3035B 1", "remove_item2": "ITEM_QUEST_3035A 1", "reward_npc_name": "Atropos", "con_quest": "3036"},
		"base": {"minlevel_permitted": "46", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "IMPORTANT", "rewards": {"gold": "44360", "exp": "6937236"}, "reward_items": (("186000005", "4"),), "work_items": (("182208024", "1"), ("182208025", "1")), "finished_quests": ("3034",)},
		"evidence": ((343, ("L\"Ab1_NPC_LugBug\"",)), (344, ("L\"Atropos\"",)), (369, ("L\"Vatonia\"",)), (548, (",0xbdb,3,0,0",)), (553, (",0xbdb,3,1,0",)), (557, (",0xbdb,3,2,0",)), (560, (",0xbdb,0,0xffffffff,0",)), (580, (",0xbdb,4,0xffffffff,0",)), (804, ("+ 0x1a8", "0xbdb")), (809, ("FUN_180cab520(0xbdb", "0xadc4618", "1")), (844, ("FUN_180cabb10(0xbdb",)), (848, ("FUN_180cabb10(0xbdb",)), (852, ("FUN_180cabb10(0xbdb",)), (853, ("FUN_180caca90(0xbdb", "0xadc4618", "1")), (856, ("+ 0x1b8", "0xbdb")))
	},
	3973: {
		"quest_hex": "0xf85", "race": "pc_light", "base_race": "ELYOS", "start": "Tersites", "start_id": 203893,
		"end": "Mesalina", "end_id": 798949, "talks": [("Utisda", 203792), ("Daphnis", 203793), ("Andu", 798391)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Utisda"], "give_item": ("quest_3973a", 1)}, {"type": "TALK", "names": ["Daphnis"], "give_item": ("quest_3973b", 1)}, {"type": "TALK", "names": ["Andu"], "give_item": ("quest_3973c", 1)}],
		"retail": {"name": "Q3973", "max_repeat_count": "1", "minlevel_permitted": "51", "finished_quest_cond1": "Q3972", "cannot_share": "1", "quest_work_item1": "quest_3973a", "quest_work_item2": "quest_3973b", "quest_work_item3": "quest_3973c", "reward_exp1": "1495244", "reward_gold1": "27840", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Tersites", "talk_npc1": "Utisda", "give_item1": "ITEM_QUEST_3973A 1", "talk_npc2": "Daphnis", "give_item2": "ITEM_QUEST_3973B 1", "talk_npc3": "Andu", "give_item3": "ITEM_QUEST_3973C 1", "reward_npc_name": "Mesalina", "con_quest": "3974"},
		"base": {"minlevel_permitted": "51", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"gold": "27840", "exp": "1495244"}, "reward_items": (), "work_items": (("182206116", "1"), ("182206117", "1"), ("182206118", "1")), "finished_quests": ("3972",)},
		"evidence": ((343, ("L\"Andu\"",)), (347, ("L\"Daphnis\"",)), (359, ("L\"Mesalina\"",)), (367, ("L\"Tersites\"",)), (369, ("&DAT_18124cc30",)), (548, (",0xf85,3,0,0",)), (554, (",0xf85,3,1,0",)), (557, (",0xf85,3,2,0",)), (558, (",0xf85,3,3,0",)), (574, (",0xf85,0,0xffffffff,0",)), (590, (",0xf85,4,0xffffffff,0",)), (807, ("+ 0x1a8", "0xf85")), (822, ("FUN_180cab520(0xf85", "0,0")), (844, ("FUN_180cabb10(0xf85",)), (849, ("FUN_180cabb10(0xf85",)), (851, ("FUN_180cabb10(0xf85",)), (853, ("FUN_180cabb10(0xf85",)), (858, ("+ 0x1b8", "0xf85")))
	},
	4011: {
		"quest_hex": "0xfab", "race": "pc_dark", "base_race": "ASMODIANS",
		"start": "DF2A_FOBJ_Q4011", "start_id": 730139, "end": "Dettil", "end_id": 205132,
		"talks": [("Dettil", 205132), ("Kaindal", 203522)], "data_driven": True, "start_dialog_id": 1011,
		"steps": [{"type": "TALK", "names": ["Dettil"], "dialog_id": 1352}, {"type": "TALK", "names": ["Kaindal"], "dialog_id": 1693}],
		"retail": {"name": "Q4011", "max_repeat_count": "1", "minlevel_permitted": "23", "reward_exp1": "117433", "reward_gold1": "10760", "reward_item1_1": "food_d_maxhp_20a 6", "race_permitted": "pc_dark"},
		"base": {"minlevel_permitted": "23", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "10760", "exp": "117433"}, "reward_items": (("160002003", "6"), ("186000007", "1")), "work_items": (("182209004", "1"),)},
		"evidence": ((612, ('L"DF2A_FOBJ_Q4011",0xfab',)), (614, ('L"Dettil",0xfab',)), (619, ('L"Kaindal",0xfab',)), (690, (",0xfab,0,0xffffffff,3)",)), (696, (",0xfab,4,0xffffffff,0)",)), (702, (",0xfab,3,0,0)",)), (707, (",0xfab,3,1,0)",)), (880, (",0x3f3,0xfab)", "FUN_180caf350(0xfab")), (886, ("FUN_180caf6c0(0xfab",)), (892, (",0x548,0xfab)", "FUN_180caf3c0(0xfab")), (896, (",0x69d,0xfab)", "FUN_180caf3c0(0xfab"))),
	},
	4052: {
		"quest_hex": "0xfd4", "race": "pc_dark", "base_race": "ASMODIANS", "start": "DF2a_NPC_Moai", "start_id": 730152,
		"end": "DF2a_NPC_Moai", "end_id": 730152, "talks": [("DF2a_NPC_Bumbum_Lin", 205179), ("DF2a_NPC_Bumbum_Jin", 205166), ("DF2a_NPC_Bumbum_Chan", 205197)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["DF2a_NPC_Bumbum_Lin"]}, {"type": "TALK", "names": ["DF2a_NPC_Bumbum_Jin"], "give_item": ("quest_4052a", 1)}, {"type": "TALK", "names": ["DF2a_NPC_Bumbum_Chan"], "remove_item": ("quest_4052a", 1)}],
		"retail": {"name": "Q4052", "max_repeat_count": "1", "minlevel_permitted": "48", "cannot_share": "1", "quest_work_item1": "quest_4052a", "reward_exp1": "7283931", "reward_gold1": "0", "reward_item1_1": "coin_d_06 6", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "DF2a_NPC_Moai", "talk_npc1": "DF2a_NPC_Bumbum_Lin", "talk_npc2": "DF2a_NPC_Bumbum_Jin", "give_item2": "ITEM_QUEST_4052A 1", "talk_npc3": "DF2a_NPC_Bumbum_Chan", "remove_item3": "ITEM_QUEST_4052A 1", "reward_npc_name": "DF2a_NPC_Moai"},
		"base": {"minlevel_permitted": "48", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "IMPORTANT", "rewards": {"exp": "7283931"}, "reward_items": (("186000019", "6"),), "work_items": (("182209030", "1"),)},
		"evidence": ((346, ("L\"DF2a_NPC_Bumbum_Chan\"",)), (346, ("L\"DF2a_NPC_Bumbum_Jin\"",)), (346, ("L\"DF2a_NPC_Bumbum_Lin\"",)), (346, ("L\"DF2a_NPC_Moai\"",)), (534, (",0xfd4,3,0,0",)), (554, (",0xfd4,3,1,0",)), (557, (",0xfd4,3,2,0",)), (558, (",0xfd4,3,3,0",)), (561, (",0xfd4,0,0xffffffff,0",)), (581, (",0xfd4,4,0xffffffff,0",)), (810, ("FUN_180cab520(0xfd4", "0,0")), (830, ("FUN_180cabb10(0xfd4",)), (849, ("FUN_180cabb10(0xfd4",)), (852, ("FUN_180cabb10(0xfd4",)), (853, ("FUN_180cabb10(0xfd4",)))
	},
	1469: {
		"quest_hex": "0x5bd", "race": "pc_light", "base_race": "ELYOS", "start": "Hagne", "start_id": 790004,
		"end": "Hagne", "end_id": 790004, "talks": [("TreasureGuardianQ_36_Ae", 212878)], "data_driven": True,
		"start_give_item": ("quest_1469a", 1),
		"retail": {"name": "Q1469", "max_repeat_count": "1", "minlevel_permitted": "30", "cannot_share": "1", "quest_work_item1": "quest_1469a", "reward_exp1": "1244918", "reward_gold1": "0", "reward_item1_1": "potion_hp_mp_50a 20", "reward_item1_2": "wrap_scroll_speed_tq_reward 6", "reward_item1_3": "coin_03 35", "selectable_reward_item1_1": "ring_n_l1_tq_34a 1", "selectable_reward_item1_2": "ring_n_l1_tq_34b 1", "race_permitted": "pc_light"},
		"talk": {"give_item1": "ITEM_QUEST_1469A 1"},
		"base": {"minlevel_permitted": "30", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "IMPORTANT", "rewards": {"exp": "1244918"}, "reward_items": (("162000050", "20"), ("188053405", "6"), ("186000003", "35")), "work_items": (("182201386", "1"),)},
		"evidence": ((350, ("L\"Hagne\"",)), (368, ("L\"TreasureGuardianQ_36_Ae\"",)), (547, (",0x5bd,3,0,0",)), (564, (",0x5bd,0,0xffffffff,0",)), (584, (",0x5bd,4,0xffffffff,0",)), (812, ("FUN_180cab520(0x5bd",)), (843, ("FUN_180cabb10(0x5bd",)), (849, ("FUN_180cabb10(0x5bd",)), (857, ("0x5bd", "0x1b8")))
	},
	1472: {
		"quest_hex": "0x5c0", "race": "pc_light", "base_race": "ELYOS",
		"start": "Valerius", "start_id": 203903, "end": "Valerius", "end_id": 203903,
		"talks": [("Shugo_DF2_14", 798114)], "data_driven": True,
		"retail": {"name": "Q1472", "max_repeat_count": "1", "minlevel_permitted": "38", "reward_exp1": "2108696", "reward_gold1": "0", "reward_item1_1": "medal_07 4", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "38", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"exp": "2108696", "ap": "100"}, "reward_items": (("186000469", "4"),), "selectable_reward_items": (("122000904", "1"), ("122000905", "1")), "work_items": ()},
		"evidence": ((629, ('L"Valerius",0x5c0',)), (627, ('L"Shugo_DF2_14",0x5c0',)), (694, (",0x5c0,0,0xffffffff,0)",)), (704, (",0x5c0,3,0,0)",)), (700, (",0x5c0,4,0xffffffff,0)",)), (884, ("FUN_180caf640(0x5c0", "FUN_180caf350(0x5c0")), (890, ("FUN_180caf6c0(0x5c0",)), (894, ("FUN_180caf740(0x5c0", "FUN_180caf3c0(0x5c0")), (906, (",0x3eb,0x5c0)",)), (909, (",0x3ec,0x5c0)",))),
	},
	1483: {
		"quest_hex": "0x5cb", "race": "pc_light", "base_race": "ELYOS", "start": "Shugo_LF2_13", "start_id": 798126,
		"end": "Shugo_LF2_14", "end_id": 798127, "talks": [("Herodes", 203940), ("Ernia", 203944)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Herodes"], "give_item": ("quest_1483a", 1)}, {"type": "TALK", "names": ["Ernia"], "give_item": ("quest_1483b", 1)}],
		"retail": {"name": "Q1483", "max_repeat_count": "1", "minlevel_permitted": "23", "cannot_share": "1", "quest_work_item1": "quest_1483a", "quest_work_item2": "quest_1483b", "reward_exp1": "136650", "reward_gold1": "0", "reward_item1_1": "coin_02 1", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Shugo_LF2_13", "talk_npc1": "Herodes", "give_item1": "ITEM_QUEST_1483A 1", "talk_npc2": "Ernia", "give_item2": "ITEM_QUEST_1483B 1", "reward_npc_name": "Shugo_LF2_14", "con_quest": "1484"},
		"base": {"minlevel_permitted": "23", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"exp": "136650"}, "reward_items": (("186000002", "1"),), "work_items": (("182201401", "1"), ("182201402", "1"))},
		"evidence": ((348, ("L\"Ernia\"",)), (351, ("L\"Herodes\"",)), (364, ("L\"Shugo_LF2_13\"",)), (364, ("L\"Shugo_LF2_14\"",)), (537, (",0x5cb,3,0,0",)), (554, (",0x5cb,3,1,0",)), (558, (",0x5cb,3,2,0",)), (572, (",0x5cb,0,0xffffffff,0",)), (593, (",0x5cb,4,0xffffffff,0",)), (719, ("case 0x5cb:", "uVar2 = 0x5cb")), (820, ("FUN_180cab520(0x5cb",)), (833, ("FUN_180cabb10(0x5cb",)), (849, ("FUN_180cabb10(0x5cb",)), (852, ("FUN_180cabb10(0x5cb",)), (858, ("+ 0x1b8", "0x5cb")))
	},
	1484: {
		"quest_hex": "0x5cc", "race": "pc_light", "base_race": "ELYOS", "start": "Shugo_LF2_14", "start_id": 798127,
		"end": "Shugo_LF2_13", "end_id": 798126, "talks": [("Anasya", 204045), ("Telamone", 204048), ("Sandinas", 204011)], "data_driven": True,
		"steps": [{"type": "TALK", "names": ["Anasya"], "give_item": ("quest_1484a", 1)}, {"type": "TALK", "names": ["Telamone"], "give_item": ("quest_1484b", 1)}, {"type": "TALK", "names": ["Sandinas"], "give_item": ("quest_1484c", 1)}],
		"retail": {"name": "Q1484", "max_repeat_count": "1", "minlevel_permitted": "23", "finished_quest_cond1": "Q1483", "cannot_share": "1", "quest_work_item1": "quest_1484a", "quest_work_item2": "quest_1484b", "quest_work_item3": "quest_1484c", "reward_exp1": "136650", "reward_gold1": "26930", "reward_item1_1": "coin_02 1", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "Shugo_LF2_14", "talk_npc1": "Anasya", "give_item1": "ITEM_QUEST_1484A 1", "talk_npc2": "Telamone", "give_item2": "ITEM_QUEST_1484B 1", "talk_npc3": "Sandinas", "give_item3": "ITEM_QUEST_1484C 1", "reward_npc_name": "Shugo_LF2_13"},
		"base": {"minlevel_permitted": "23", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"gold": "26930", "exp": "136650"}, "reward_items": (("186000002", "1"),), "work_items": (("182201403", "1"), ("182201404", "1"), ("182201405", "1")), "finished_quests": ("1483",)},
		"evidence": ((343, ("L\"Anasya\"",)), (363, ("L\"Sandinas\"",)), (364, ("L\"Shugo_LF2_13\"",)), (364, ("L\"Shugo_LF2_14\"",)), (367, ("L\"Telamone\"",)), (532, (",0x5cc,3,0,0",)), (556, (",0x5cc,3,1,0",)), (557, (",0x5cc,3,2,0",)), (558, (",0x5cc,3,3,0",)), (572, (",0x5cc,0,0xffffffff,0",)), (593, (",0x5cc,4,0xffffffff,0",)), (719, ("case 0x5cc:", "uVar2 = 0x5cc")), (807, ("+ 0x1a8", "0x5cc")), (820, ("FUN_180cab520(0x5cc",)), (828, ("FUN_180cabb10(0x5cc",)), (851, ("FUN_180cabb10(0x5cc",)), (852, ("FUN_180cabb10(0x5cc",)), (853, ("FUN_180cabb10(0x5cc",)))
	},
	1422: {
		"quest_hex": "0x58e", "race": "pc_light", "base_race": "ELYOS", "start": "Memnes", "start_id": 203912,
		"end": "Memnes", "end_id": 203912, "talks": [("Laokones", 203731)],
		"retail": {"name": "Q1422", "max_repeat_count": "1", "minlevel_permitted": "20", "reward_exp1": "50008", "reward_gold1": "15880", "reward_item1_1": "coin_02 1", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "20", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"gold": "15880", "exp": "50008"}, "reward_items": (("186000002", "1"),), "work_items": (("182201389", "1"), ("182201390", "1"))},
		"evidence": ((358, ("L\"Laokones\"",)), (359, ("L\"Memnes\"",)), (569, (",0x58e,0,0xffffffff,0",)), (541, (",0x58e,3,0,0",)), (555, (",0x58e,3,1,0",)), (589, (",0x58e,4,0xffffffff,0",)), (817, ("FUN_180cab520(0x58e", "0xadc2c2d")), (850, ("FUN_180cabb10(0x58e", "FUN_180cacb30(0x58e")), (855, ("FUN_180caca90(0x58e", "0xadc2c2d")))
	},
	1423: {
		"quest_hex": "0x58f", "race": "pc_light", "base_race": "ELYOS", "start": "Marana", "start_id": 203983,
		"end": "Marana", "end_id": 203983, "talks": [],
		"retail": {"name": "Q1423", "max_repeat_count": "1", "minlevel_permitted": "29", "reward_exp1": "476911", "reward_gold1": "0", "reward_item1_1": "coin_03 9", "reward_item1_2": "potion_hp_mp_30a 10", "reward_item1_3": "potion_flytime_30a 10", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "29", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "IMPORTANT", "rewards": {"exp": "476911"}, "reward_items": (("186000003", "9"), ("162000048", "10"), ("162000025", "10")), "work_items": ()},
		"evidence": ((624, ("L\"Marana\"",)), (692, (",0x58f,0,0xffffffff,0",)), (703, (",0x58f,3,0,0",)), (716, (",0x58f,4,0,0",)), (806, ("0x58f", "+ 0x1a8")), (878, ("0x58f", "0x947")), (883, ("0x3f3,0x58f", "0x3ef", "0x58f")), (893, ("0x3eb,0x58f", "0x1e0", "0x58f")), (905, ("0x58f", "0x3eb")), (909, ("0x3ec,0x58f",)))
	},
	16979: {
		"quest_hex": "0x4253", "race": "pc_light", "base_race": "ELYOS", "start": "IDLDF5_Under_01_War_Moiro_E", "start_id": 802025,
		"end": "LDF5_Under_Timarchus_E", "end_id": 801762, "talks": [], "kind": "item_collecting", "movie": 886,
		"retail": {"name": "Q16979", "max_repeat_count": "1", "minlevel_permitted": "61", "cannot_share": "1", "reward_exp1": "2807767", "reward_gold1": "133740", "race_permitted": "pc_light"},
		"talk": {"acquired_npc_name": "IDLDF5_Under_01_War_Moiro_E", "reward_npc_name": "LDF5_Under_Timarchus_E", "cutsceneid1": "886", "cs1_haction": "20000"},
		"base": {"minlevel_permitted": "61", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"gold": "133740", "exp": "2807767"}, "reward_items": (), "work_items": ()},
		"evidence": ((352, ("L\"IDLDF5_Under_01_War_Moiro_E\"",)), (354, ("L\"LDF5_Under_Timarchus_E\"",)), (565, (",0x4253,0,0xffffffff,0",)), (539, (",0x4253,3,0,0",)), (587, (",0x4253,4,0xffffffff,0",)), (813, ("FUN_180cab520(0x4253", "FUN_180cacb30(0x4253")), (835, ("FUN_180cabb10(0x4253", "FUN_180cacb30(0x4253")))
	},
	18208: {
		"quest_hex": "0x4720", "race": "pc_light", "base_race": "ELYOS",
		"start": "Inggril", "start_id": 205316, "end": "Molfus", "end_id": 205309,
		"talks": [], "data_driven": True,
		"steps": [{"type": "HUNT", "names": ["IDArena_Solo_S6_VanqJr_55_An"], "amount": 5},
			{"type": "HUNT", "names": ["IDArena_Solo_H1_DrakanAs_noble_55_Ae", "IDArena_Solo_H2_TempleD_Fi_55_Ae"], "amount": 1}],
		"retail": {"name": "Q18208", "max_repeat_count": "1", "minlevel_permitted": "50", "reward_exp1": "7448473", "reward_gold1": "0", "reward_item1_1": "coin_arena_01 100", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "50", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"exp": "7448473"}, "reward_items": (("186000130", "100"),), "work_items": (), "finished_quests": ("18207",)},
		"evidence": ((619, ('L"Inggril",0x4720',)), (624, ('L"Molfus",0x4720',)), (617, ('L"IDArena_Solo_S6_VanqJr_55_An",0x4720',)), (616, ('L"IDArena_Solo_H1_DrakanAs_noble_55_Ae",0x4720',)), (616, ('L"IDArena_Solo_H2_TempleD_Fi_55_Ae",0x4720',)), (691, (",0x4720,0,0xffffffff,0)",)), (715, (",0x4720,3,0x1001,0)",)), (698, (",0x4720,4,0xffffffff,0)",))),
	},
	18209: {
		"quest_hex": "0x4721", "race": "pc_light", "base_race": "ELYOS",
		"start": "Molfus", "start_id": 205309, "end": "Molfus", "end_id": 205309,
		"talks": [], "data_driven": True,
		"steps": [{"type": "HUNT", "names": ["IDArena_Solo_S6_VanqJr_55_An"], "amount": 5},
			{"type": "HUNT", "names": ["IDArena_Solo_H1_DrakanAs_noble_55_Ae", "IDArena_Solo_H2_TempleD_Fi_55_Ae"], "amount": 1}],
		"retail": {"name": "Q18209", "max_repeat_count": "255", "minlevel_permitted": "50", "reward_exp1": "5213931", "reward_gold1": "0", "reward_item1_1": "coin_arena_01 100", "race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "50", "max_repeat_count": "255", "cannot_share": "true", "race_permitted": "ELYOS", "category": "QUEST", "rewards": {"exp": "5213931"}, "reward_items": (("186000130", "100"),), "work_items": (), "finished_quests": ("18208",)},
		"evidence": ((624, ('L"Molfus",0x4721',)), (617, ('L"IDArena_Solo_S6_VanqJr_55_An",0x4721',)), (616, ('L"IDArena_Solo_H1_DrakanAs_noble_55_Ae",0x4721',)), (616, ('L"IDArena_Solo_H2_TempleD_Fi_55_Ae",0x4721',)), (692, (",0x4721,0,0xffffffff,0)",)), (715, (",0x4721,3,0x1001,0)",)), (698, (",0x4721,4,0xffffffff,0)",))),
	},
	24155: {
		"quest_hex": "0x5e5b", "race": "pc_dark", "base_race": "ASMODIANS",
		"start": "Hod", "start_id": 204701, "end": "Hod", "end_id": 204701,
		"talks": [], "data_driven": True,
		"steps": [{"type": "HUNT", "names": ["DF3_DragonObelisk_Q2060A"], "amount": 3}],
		"retail": {"name": "Q24155", "max_repeat_count": "1", "minlevel_permitted": "39", "reward_exp1": "1567048", "reward_gold1": "119880", "reward_item1_1": "wrap_quest_matter_option_40d 1", "race_permitted": "pc_dark"},
		"base": {"minlevel_permitted": "39", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "119880", "exp": "1567048"}, "reward_items": (("188053063", "1"),), "work_items": ()},
		"evidence": ((148, ('L"DF3_DragonObelisk_Q2060A",0x5e5b',)), (316, (",0x5e5b,0,0xffffffff,0)",)), (301, (",0x5e5b,3,0x40000000,0)",)), (308, (",0x5e5b,3,3,0)",)), (327, (",0x5e5b,4,0xffffffff,0)",))),
	},
	26979: {
		"quest_hex": "0x6963", "race": "pc_dark", "base_race": "ASMODIANS", "start": "IDLDF5_Under_01_War_Atea_E", "start_id": 802026,
		"end": "LDF5_Under_Undgankt_E", "end_id": 801764, "talks": [], "kind": "item_collecting", "movie": 887,
		"retail": {"name": "Q26979", "max_repeat_count": "1", "minlevel_permitted": "61", "cannot_share": "1", "reward_exp1": "2807767", "reward_gold1": "133740", "race_permitted": "pc_dark"},
		"talk": {"acquired_npc_name": "IDLDF5_Under_01_War_Atea_E", "reward_npc_name": "LDF5_Under_Undgankt_E", "cutsceneid1": "887", "cs1_haction": "20000"},
		"base": {"minlevel_permitted": "61", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"gold": "133740", "exp": "2807767"}, "reward_items": (), "work_items": ()},
		"evidence": ((352, ("L\"IDLDF5_Under_01_War_Atea_E\"",)), (354, ("L\"LDF5_Under_Undgankt_E\"",)), (565, (",0x6963,0,0xffffffff,0",)), (539, (",0x6963,3,0,0",)), (587, (",0x6963,4,0xffffffff,0",)), (813, ("FUN_180cab520(0x6963", "FUN_180cacb30(0x6963")), (835, ("FUN_180cabb10(0x6963", "FUN_180cacb30(0x6963")))
	},
	28209: {
		"quest_hex": "0x6e31", "race": "pc_dark", "base_race": "ASMODIANS",
		"start": "Anja", "start_id": 205321, "end": "Anja", "end_id": 205321,
		"talks": [], "data_driven": True,
		"steps": [{"type": "HUNT", "names": ["IDArena_Solo_S6_VanqJr_55_An"], "amount": 5},
			{"type": "HUNT", "names": ["IDArena_Solo_H1_DrakanAs_noble_55_Ae", "IDArena_Solo_H2_TempleL_Fi_55_Ae"], "amount": 1}],
		"retail": {"name": "Q28209", "max_repeat_count": "255", "minlevel_permitted": "50", "reward_exp1": "5213931", "reward_gold1": "0", "reward_item1_1": "coin_arena_01 100", "race_permitted": "pc_dark"},
		"base": {"minlevel_permitted": "50", "max_repeat_count": "255", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"exp": "5213931"}, "reward_items": (("186000130", "100"),), "work_items": (), "finished_quests": ("28208",)},
		"evidence": ((617, ('L"IDArena_Solo_S6_VanqJr_55_An",0x6e31',)), (616, ('L"IDArena_Solo_H1_DrakanAs_noble_55_Ae",0x6e31',)), (617, ('L"IDArena_Solo_H2_TempleL_Fi_55_Ae",0x6e31',)), (689, (",0x6e31,0,0xffffffff,0)",)), (715, (",0x6e31,3,0x1001,0)",)), (694, (",0x6e31,4,0xffffffff,0)",))),
	},
	28208: {
		"quest_hex": "0x6e30", "race": "pc_dark", "base_race": "ASMODIANS",
		"start": "Inggness", "start_id": 205320, "end": "Anja", "end_id": 205321,
		"talks": [], "data_driven": True,
		"steps": [{"type": "HUNT", "names": ["IDArena_Solo_S6_VanqJr_55_An"], "amount": 5},
			{"type": "HUNT", "names": ["IDArena_Solo_H1_DrakanAs_noble_55_Ae", "IDArena_Solo_H2_TempleL_Fi_55_Ae"], "amount": 1}],
		"retail": {"name": "Q28208", "max_repeat_count": "1", "minlevel_permitted": "50", "reward_exp1": "7448473", "reward_gold1": "0", "reward_item1_1": "coin_arena_01 100", "race_permitted": "pc_dark"},
		"base": {"minlevel_permitted": "50", "max_repeat_count": "1", "cannot_share": "true", "race_permitted": "ASMODIANS", "category": "QUEST", "rewards": {"exp": "7448473"}, "reward_items": (("186000130", "100"),), "work_items": (), "finished_quests": ("28207",)},
		"evidence": ((619, ('L"Inggness",0x6e30',)), (617, ('L"IDArena_Solo_S6_VanqJr_55_An",0x6e30',)), (616, ('L"IDArena_Solo_H1_DrakanAs_noble_55_Ae",0x6e30',)), (617, ('L"IDArena_Solo_H2_TempleL_Fi_55_Ae",0x6e30',)), (691, (",0x6e30,0,0xffffffff,0)",)), (715, (",0x6e30,3,0x1001,0)",)), (694, (",0x6e30,4,0xffffffff,0)",))),
	},
}
COMPILED_SIMPLE_TALK_SOURCE_NUMBERS = tuple(sorted({number for data in COMPILED_SIMPLE_TALKS.values() for number, _ in data["evidence"]}))
# 德雷得奇安副本内线性任务：领取->对话->物件(获取情报)->击杀 Boss->报告领奖。
# 光暗对偶共享物件 NPC 与击杀目标，仅起止 NPC 与奖励按种族区分。
COMPILED_DREDGION_CAPTAIN_HUNTS = {
	3711: {
		"quest_hex": "0xe7f", "race": "pc_light", "base_race": "ELYOS",
		"start": "Taranis", "start_id": 278501,
		"talk_npc": "Maias", "talk_npc_id": 279045,
		"object": "IDAB1_Dreadgion_ShugoExhausted_Q3711", "object_id": 730196,
		"target": "IDAb1_Dreadgion_DrakanBoss_50_Ah", "target_id": 214823,
		"retail": {"name": "Q3711", "max_repeat_count": "1", "minlevel_permitted": "46", "maxlevel_permitted": "50",
			"reward_exp1": "3884596", "reward_gold1": "118580", "reward_abyss_point1": "1500",
			"reward_item1_1": "soulstone_a_resurrect_self_01 5", "reward_item1_2": "coin_05 1", "reward_item1_3": "wrap_l_coin_platinum_3_0 1",
			"race_permitted": "pc_light"},
		"base": {"minlevel_permitted": "46", "maxlevel_permitted": "50", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ELYOS", "category": "QUEST",
			"rewards": {"gold": "118580", "exp": "3884596", "ap": "1500"},
			"reward_items": (("161000004", "5"), ("186000005", "1"), ("188050830", "1"))},
		"evidence": ((616, ("L\"IDAB1_Dreadgion_ShugoExhausted_Q3711\",0xe7f",)), (616, ("L\"IDAb1_Dreadgion_DrakanBoss_50_Ah\",0xe7f",)),
			(624, ("L\"Maias\",0xe7f",)), (628, ("L\"Taranis\",0xe7f",)),
			(693, (",0xe7f,0,0xffffffff,0)",)), (703, (",0xe7f,3,0,0)",)), (707, (",0xe7f,3,1,0)",)), (700, (",0xe7f,4,0xffffffff,0)",)),
			(866, (",0xe7f,2,2,1,1",))),
	},
	4711: {
		"quest_hex": "0x1267", "race": "pc_dark", "base_race": "ASMODIANS",
		"start": "Votan", "start_id": 278001,
		"talk_npc": "Henir", "talk_npc_id": 279042,
		"object": "IDAB1_Dreadgion_ShugoExhausted_Q3711", "object_id": 730196,
		"target": "IDAb1_Dreadgion_DrakanBoss_50_Ah", "target_id": 214823,
		"retail": {"name": "Q4711", "max_repeat_count": "1", "minlevel_permitted": "46", "maxlevel_permitted": "50",
			"reward_exp1": "3732496", "reward_gold1": "118580", "reward_abyss_point1": "1500",
			"reward_item1_1": "soulstone_a_resurrect_self_01 5", "reward_item1_2": "coin_d_05 1", "reward_item1_3": "wrap_d_coin_platinum_3_0 1",
			"race_permitted": "pc_dark"},
		"base": {"minlevel_permitted": "46", "maxlevel_permitted": "50", "max_repeat_count": "1", "cannot_share": "false", "race_permitted": "ASMODIANS", "category": "QUEST",
			"rewards": {"gold": "118580", "exp": "3732496", "ap": "1500"},
			"reward_items": (("161000004", "5"), ("186000010", "1"), ("188050920", "1"))},
		"evidence": ((616, ("L\"IDAB1_Dreadgion_ShugoExhausted_Q3711\",0x1267",)), (616, ("L\"IDAb1_Dreadgion_DrakanBoss_50_Ah\",0x1267",)),
			(616, ("L\"Henir\",0x1267",)), (630, ("L\"Votan\",0x1267",)),
			(694, (",0x1267,0,0xffffffff,0)",)), (702, (",0x1267,3,0,0)",)), (707, (",0x1267,3,1,0)",)), (701, (",0x1267,4,0xffffffff,0)",)),
			(866, (",0x1267,2,2,1,1",))),
	},
}
COMPILED_DREDGION_CAPTAIN_NPCS = {
	"Taranis": 278501, "Maias": 279045, "Votan": 278001, "Henir": 279042,
	"IDAB1_Dreadgion_ShugoExhausted_Q3711": 730196, "IDAb1_Dreadgion_DrakanBoss_50_Ah": 214823,
}
COMPILED_DREDGION_CAPTAIN_SOURCE_NUMBERS = (616, 624, 628, 630, 693, 694, 700, 701, 702, 703, 707, 866)
COMPILED_SURAMA_HUNTS = {
	30708: ("0x77f4", "pc_light", "ELYOS"),
	30758: ("0x7826", "pc_dark", "ASMODIANS"),
}
COMPILED_SURAMA_NPCS = {
	"IDTiamat_Surama_1": 800369,
	"IDTiamat_Murugan_4": 800438,
	"IDTiamat_Drakan_Surama_1": 800425,
	"IDTiamat_Drakan_Surama_2": 800426,
	"IDTiamat_Drakan_Surama_3": 800427,
}
COMPILED_SURAMA_SOURCE_NUMBERS = (618, 691, 697, 866, 878, 881, 887)
COMPILED_ARENA_ITEM_PLAYS = {
	18213: {
		"quest_hex": "0x4725", "retail_race": "pc_light", "base_race": "ELYOS", "prerequisite": 18212,
		"start": ("Junos", 205985), "talks": (("Inggril", 205316), ("Romedon", 798604)),
		"items": (("quest_18213a", 182212219), ("quest_18213b", 182212220)),
		"messages": ("0xadc567a", "0xadc567b", "0xadc567c"),
		"dialog_data": ("&DAT_18470ebc8", "&DAT_18470ec08", "&DAT_18470ec48"),
		"callback": "FUN_180dbb1d0",
	},
	28213: {
		"quest_hex": "0x6e35", "retail_race": "pc_dark", "base_race": "ASMODIANS", "prerequisite": 28212,
		"start": ("Shinin", 205986), "talks": (("Inggness", 205320), ("Kijan", 798804)),
		"items": (("quest_28213a", 182212222), ("quest_28213b", 182212223)),
		"messages": ("0xadc567d", "0xadc567e", "0xadc567f"),
		"dialog_data": ("&DAT_18470ec88", "&DAT_18470ecc8", "&DAT_18470ed08"),
		"callback": "FUN_180dbb6e0",
	},
}
COMPILED_ARENA_ITEM_PLAY_SOURCE_NUMBERS = (336, 337, 338, 339, 340, 802, 803, 872)
COMPILED_DREDGION_CONTROL_HUNTS = {
	30702: ("0x77ee", "pc_light", "ELYOS"),
	30752: ("0x7820", "pc_dark", "ASMODIANS"),
}
COMPILED_DREDGION_CONTROL_NPCS = {
	"IDTiamat_Sorus": 800424,
	"IDTiamat_FOBJ_Model_1": 730702,
	"IDTiamat_Sardha_Named_60_Ah": 219354,
	"IDTiamat_Sorus_2": 800461,
}
COMPILED_DREDGION_CONTROL_SOURCE_NUMBERS = (163, 301, 305, 317, 328, 766, 780, 787, 792, 800)
COMPILED_DREDGION_NAVIGATION_HUNTS = {
	30600: ("0x7788", "pc_light", "ELYOS", "Hejitor", "Linocus"),
	30610: ("0x7792", "pc_dark", "ASMODIANS", "Astella", "Aluna"),
}
COMPILED_DREDGION_NAVIGATION_NPCS = {
	"Hejitor": 800325,
	"Linocus": 800324,
	"Astella": 800327,
	"Aluna": 800326,
	"IDDreadgion_03_DrakanFiNamedAA_60_Ae": 219256,
	"IDDreadgion_03_DrakanFiNamedAB_60_Ae": 219257,
	"IDDreadgion_03_DrakanWi_Boss_Ah": 219264,
}
COMPILED_DREDGION_NAVIGATION_SOURCE_NUMBERS = (340, 341, 342, 804)
COMPILED_CHRISTMAS_COURIER_HUNTS = {
	50008: {
		"quest_hex": "0xc358", "retail_race": "pc_light", "base_race": "ELYOS",
		"start": "LC1_ChristmasEvent_Santa", "target": "ChristmasEvent_Shugo_assistant_light_20_n",
		"target_hex": "0x3589a", "sensor": "Housing_Lf_Event_SensoryArea",
		"start_phase_source": 691, "reward_phase_source": 697, "start_dialog_source": 882, "reward_dialog_source": 887,
	},
	51008: {
		"quest_hex": "0xc740", "retail_race": "pc_dark", "base_race": "ASMODIANS",
		"start": "DC1_ChristmasEvent_Santa", "target": "ChristmasEvent_Shugo_assistant_dark_20_n",
		"target_hex": "0x3589b", "sensor": "Housing_Df_Event_SensoryArea",
		"start_phase_source": 690, "reward_phase_source": 695, "start_dialog_source": 880, "reward_dialog_source": 885,
	},
}
COMPILED_CHRISTMAS_COURIER_NPCS = {
	"LC1_ChristmasEvent_Santa": 831032,
	"DC1_ChristmasEvent_Santa": 831033,
	"ChristmasEvent_Shugo_assistant_light_20_n": 219290,
	"ChristmasEvent_Shugo_assistant_dark_20_n": 219291,
	"Housing_Lf_Event_SensoryArea": 206234,
	"Housing_Df_Event_SensoryArea": 206235,
}
COMPILED_CHRISTMAS_COURIER_SOURCE_NUMBERS = (611, 616, 620, 690, 691, 695, 697, 878, 880, 882, 885, 887)


def parse_item_reference(value: str) -> tuple[str, int] | None:
	match = re.fullmatch(r"(?:ITEM_)?(\S+)\s+(\d+)", value)
	return None if match is None else (match.group(1), int(match.group(2)))


EXTRA_ACTION_NAMES = {
	1: "give_item", 2: "remove_item", 3: "teleport", 4: "cutscene", 5: "spawn_npc",
	6: "delay", 7: "message", 8: "message2", 9: "enter_instance", 10: "timer",
}
EXTRA_ACTION_CATEGORIES = {"itemplay", "talk", "enterarea", "enterworld", "levelup", "talkfobj", "leveluplogin"}


def normalized_extra_actions(fields: dict[str, str], phase: str) -> dict[str, str]:
	category = fields.get(f"category_{phase}_", "").casefold()
	if category == "hunt":
		slots = (4, 5)
	elif category in EXTRA_ACTION_CATEGORIES:
		slots = range(1, 11)
	else:
		slots = ()
	return {
		EXTRA_ACTION_NAMES[slot]: value
		for slot in slots if (value := fields.get(f"value{slot}_{phase}_"))
	}


def parse_teleport(value: str) -> tuple[int, int, int, int, int] | None:
	parts = re.split(r"[\s,]+", value.strip())
	if len(parts) != 5 or any(re.fullmatch(r"-?\d+", part) is None for part in parts):
		return None
	world_id, x, y, z, heading = map(int, parts)
	return (world_id, x, y, z, heading) if world_id > 0 and 0 <= heading <= 255 else None


def parse_cutscene(value: str) -> int | None:
	match = re.fullmatch(r"Cutscene\s+(\d+)", value, re.IGNORECASE)
	return None if match is None or int(match.group(1)) == 0 else int(match.group(1))


def parse_timer(value: str) -> tuple[int, int] | None:
	parts = re.split(r"[\s,]+", value.strip())
	if len(parts) != 3 or any(re.fullmatch(r"\d+", part) is None for part in parts):
		return None
	seconds, destination, unknown = map(int, parts)
	return (seconds, destination) if seconds > 0 and unknown == 0 else None


def parse_spawns(value: str) -> list[tuple[str, int, int, int | float | None, int | float | None, int | float | None, int | None]] | None:
	result = []
	number = r"-?\d+(?:\.\d+)?"
	absolute_pattern = re.compile(
		rf"Absolute\s+([^,]+?)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*({number})\s+({number})\s+({number})\s+(\d+)",
		re.IGNORECASE,
	)
	relative_pattern = re.compile(r"Relative\s+([^,]+?)\s*,\s*(\d+)\s*,\s*(\d+)", re.IGNORECASE)
	for action in (part.strip() for part in value.split(";")):
		if not action:
			continue
		match = absolute_pattern.fullmatch(action)
		if match is not None:
			name, count, lifetime, x, y, z, heading = match.groups()
			count, lifetime, heading = map(int, (count, lifetime, heading))
			if count == 0 or lifetime == 0 or heading > 255:
				return None
			coords = tuple(float(part) if "." in part else int(part) for part in (x, y, z))
			result.append((name, count, lifetime, *coords, heading))
			continue
		match = relative_pattern.fullmatch(action)
		if match is None:
			return None
		name, count, lifetime = match.groups()
		count, lifetime = map(int, (count, lifetime))
		if count == 0 or lifetime == 0:
			return None
		result.append((name, count, lifetime, None, None, None, None))
	return result or None


def supported_extra_actions(fields: dict[str, str], phase: str) -> dict[str, object] | None:
	result: dict[str, object] = {}
	for action, value in normalized_extra_actions(fields, phase).items():
		if action in {"give_item", "remove_item"}:
			parsed = parse_item_reference(value)
		elif action == "teleport" and phase == "progress":
			parsed = parse_teleport(value)
		elif action == "cutscene" and phase == "progress":
			parsed = parse_cutscene(value)
		elif action == "spawn_npc" and phase == "progress":
			parsed = parse_spawns(value)
		elif action == "timer" and phase == "progress":
			parsed = parse_timer(value)
		else:
			return None
		if parsed is None:
			return None
		result[action] = parsed
	return result


def data_driven_action_coverage(path: Path) -> dict[str, dict[str, int]]:
	coverage = {name: {"total": 0, "supported": 0} for name in EXTRA_ACTION_NAMES.values()}
	for quest in ET.parse(path).getroot():
		phases = [("acquire", {child.tag: (child.text or "").strip() for child in quest if child.tag != "progress_info"})]
		phases.extend(("progress", {child.tag: (child.text or "").strip() for child in step}) for step in quest.findall("./progress_info/data"))
		for phase, fields in phases:
			for action, value in normalized_extra_actions(fields, phase).items():
				coverage[action]["total"] += 1
				probe = dict(fields)
				for slot, name in EXTRA_ACTION_NAMES.items():
					if name != action:
						probe.pop(f"value{slot}_{phase}_", None)
				if supported_extra_actions(probe, phase) is not None:
					coverage[action]["supported"] += 1
	return {name: counts for name, counts in coverage.items() if counts["total"]}


def has_function_evidence(source: str, anchor: str, tokens: tuple[str, ...]) -> bool:
	blocks = re.findall(r"(?ms)^// @[^\n]+\n.*?(?=^// @|\Z)", source)
	return any(anchor in block and all(token in block for token in tokens) for block in blocks)


def parse_hunt_actions(value: str) -> list[tuple[list[str], int]] | None:
	stages: list[tuple[list[str], int]] = []
	for action in (part.strip() for part in value.split(";")):
		if not action:
			continue
		match = re.fullmatch(r"(.+?)\s+(\d+)", action)
		targets = [] if match is None else [name for name in re.split(r"[\s,]+", match.group(1).strip()) if name]
		if not targets:
			return None
		stages.append((targets, int(match.group(2))))
	return stages or None


def sha256(path: Path) -> str:
	digest = hashlib.sha256()
	with path.open("rb") as stream:
		for chunk in iter(lambda: stream.read(1024 * 1024), b""):
			digest.update(chunk)
	return digest.hexdigest()


def _case_insensitive_file(directory: Path, name: str) -> Path | None:
	if not directory.is_dir():
		return None
	wanted = name.casefold()
	return next((path for path in directory.iterdir() if path.is_file() and path.name.casefold() == wanted), None)


def source_file(source: Path, name: str, region: str = DEFAULT_RETAIL_REGION,
		prefer_common: bool = False) -> tuple[Path, str]:
	source = source.expanduser().resolve()
	common = _case_insensitive_file(source, name)
	if prefer_common and common is not None:
		return common, "common"
	regional = _case_insensitive_file(source / region, name)
	if regional is not None:
		return regional, region
	if common is not None:
		return common, "common"
	raise FileNotFoundError(f"missing retail file: {name} (source={source}, region={region})")


def source_logical_path(source: Path, path: Path, region: str) -> str:
	source = source.expanduser().resolve()
	path = path.expanduser().resolve()
	try:
		return path.relative_to(source).as_posix()
	except ValueError:
		return f"{region}/{path.name}" if region != "common" else path.name


def record_count(path: Path) -> int:
	return sum(1 for _ in ET.parse(path).getroot())


def describe_source(source: Path, name: str, path: Path, region: str) -> dict[str, object]:
	return {
		"name": name,
		"logical_path": source_logical_path(source, path, region),
		"region": region,
		"path": str(path.resolve()),
		"sha256": sha256(path),
		"records": record_count(path),
	}


def assert_not_legacy_data_root(source: Path) -> None:
	resolved = source.expanduser().resolve()
	if resolved in LEGACY_RETAIL_DATA_ROOTS:
		raise SystemExit(f"refusing legacy retail data root: {resolved}; use {DEFAULT_RETAIL}")


def current_quest_ids(path: Path) -> set[int]:
	return {int(node.attrib["id"]) for node in ET.parse(path).getroot().findall("quest")}


def validate_client_quest_coverage(server_ids: set[int], client_quest: Path) -> set[int]:
	client_ids = {
		int(node.findtext("id"))
		for node in ET.parse(client_quest).getroot().findall("quest")
		if (node.findtext("id") or "").isdigit()
	}
	missing = server_ids - client_ids
	if missing:
		raise ValueError(f"server quest definitions contain IDs absent from client quest.xml: {sorted(missing)}")
	return client_ids


def existing_java_handler_ids(root: Path = ROOT) -> set[int]:
	result: set[int] = set()
	for path in (root / "src/main/java/com/aionemu/gameserver/quest/handlers").rglob("_*.java"):
		match = re.match(r"_(\d+)", path.name)
		if match:
			result.add(int(match.group(1)))
	return result


def audit_java_handlers(enabled_ids: set[int], root: Path = ROOT) -> tuple[dict[str, dict[str, object]], dict[str, object]]:
	details: dict[str, dict[str, object]] = {}
	reasons: dict[str, int] = {}
	dispositions: dict[str, int] = {}
	event_signatures: dict[str, int] = {}
	mechanism_counts: dict[str, int] = {}
	mechanisms = {
		"action": r"useQuestObject",
		"dynamic_reward": r"sendQuestEndDialog\s*\(\s*env\s*,",
		"inherited": r"extends\s+(?!QuestHandler\b)",
		"instance": r"InstanceService|InstanceHandler|getPosition\(\)\.getWorldMapInstance|setInstance",
		"item_condition": r"collectItemCheck|checkQuestItems(?:Simple)?|getItemCountByItemId|getItemByItemId|isFullSpecialCube",
		"item_mutation": r"giveQuestItem|removeQuestItem|decreaseByItemId|addItem|sendQuestStartDialog\s*\(\s*env\s*,|defaultCloseDialog\s*\([^;]*\b1\d{8}\b",
		"movie": r"playQuestMovie|onMovieEndEvent|MovieEnd",
		"skill": r"SkillEngine|SkillTemplate|onSkillUseEvent",
		"spawn": r"addNewSpawn|SpawnEngine|\.spawn\(",
		"teleport": r"TeleportService|teleportTo",
		"timer": r"ThreadPoolManager|threadPoolManager|questTimer|\.schedule\(",
	}
	root_dir = root / "src/main/java/com/aionemu/gameserver/quest/handlers"
	for path in sorted(root_dir.rglob("_*.java")):
		match = re.match(r"_(\d+)", path.name)
		if not match:
			continue
		quest_id = int(match.group(1))
		if str(quest_id) in details:
			raise ValueError(f"duplicate Java quest handler id: {quest_id}")
		source = path.read_text(errors="ignore")
		events = sorted(set(re.findall(r"\b(?:boolean|void)\s+(on[A-Z]\w+Event)\s*\(", source)))
		used_mechanisms = sorted(name for name, pattern in mechanisms.items() if re.search(pattern, source))
		if quest_id not in enabled_ids:
			reason = "outside_base"
			disposition = "not_executable_without_base"
		elif set(events) <= {"onDialogEvent", "onKillEvent"} and not used_mechanisms:
			reason = "missing_compiled_trigger_chain"
			disposition = "retained_authoritative_gap"
		else:
			reason = "non_linear_or_side_effecting"
			disposition = "retained_complex_semantics"
		signature = "+".join(event.removeprefix("on").removesuffix("Event") for event in events) or "register_only"
		details[str(quest_id)] = {
			"file": str(path.relative_to(root)), "events": events, "mechanisms": used_mechanisms,
			"reason": reason, "disposition": disposition,
		}
		reasons[reason] = reasons.get(reason, 0) + 1
		dispositions[disposition] = dispositions.get(disposition, 0) + 1
		event_signatures[signature] = event_signatures.get(signature, 0) + 1
		for mechanism in used_mechanisms:
			mechanism_counts[mechanism] = mechanism_counts.get(mechanism, 0) + 1
	return details, {
		"handlers": len(details), "base_handlers": sum(int(quest_id) in enabled_ids for quest_id in details),
		"reasons": reasons, "dispositions": dispositions,
		"event_signatures": event_signatures, "mechanisms": mechanism_counts,
	}


def compiled_script_evidence(script_root: Path, quest_ids: set[int]) -> dict[int, dict[str, object]]:
	result = {
		quest_id: {"quest_hex": hex(quest_id), "script_files": [], "script_occurrences": 0}
		for quest_id in quest_ids
	}
	if not result:
		return result
	quest_by_hex = {hex(quest_id): quest_id for quest_id in quest_ids}
	suffixes = "|".join(re.escape(value[2:]) for value in sorted(quest_by_hex, key=len, reverse=True))
	pattern = re.compile(r"(?<![0-9a-f])0x(?:" + suffixes + r")(?![0-9a-f])", re.IGNORECASE)
	for path in sorted(script_root.glob("fun_*.cpp")):
		counts: dict[int, int] = {}
		for match in pattern.finditer(path.read_text(encoding="utf-8", errors="ignore")):
			quest_id = quest_by_hex[match.group().lower()]
			counts[quest_id] = counts.get(quest_id, 0) + 1
		for quest_id, count in counts.items():
			result[quest_id]["script_files"].append(path.name)  # type: ignore[union-attr]
			result[quest_id]["script_occurrences"] += count  # type: ignore[operator]
	groups: dict[tuple[str, ...], list[int]] = {}
	for quest_id, evidence in result.items():
		files = tuple(evidence["script_files"])
		if files:
			groups.setdefault(files, []).append(quest_id)
	for evidence in result.values():
		files = tuple(evidence["script_files"])
		evidence["script_group_ids"] = sorted(groups.get(files, []))
		evidence["constraint_disposition"] = "evidence_audit" if files else "blocked_no_script_hit"
	return result


def legacy_xml_definitions(directory: Path = DEFAULT_OUTPUT.parent) -> dict[int, dict[str, str]]:
	result: dict[int, dict[str, str]] = {}
	for path in directory.glob("*.xml"):
		if path.name == DEFAULT_OUTPUT.name:
			continue
		for node in ET.parse(path).getroot():
			if not node.attrib.get("id", "").isdigit():
				continue
			quest_id = int(node.attrib["id"])
			if quest_id in result:
				raise ValueError(f"duplicate legacy XML quest id: {quest_id}")
			result[quest_id] = {"file": path.name, "template": node.tag}
	return result


def existing_handler_ids(legacy_dir: Path = DEFAULT_OUTPUT.parent, root: Path = ROOT) -> set[int]:
	return set(legacy_xml_definitions(legacy_dir)) | existing_java_handler_ids(root)


def retail_source_ids(retail: Path, region: str = DEFAULT_RETAIL_REGION) -> set[int]:
	result = {
		int(node.attrib["id"])
		for name in ("Quest_SimpleHunt.xml", "Quest_SimpleTalk.xml", "Quest_SimpleCollectItem.xml", "Quest_SimpleUseItem.xml", "Quest_SimpleItemPlay.xml", "Quest_CombineTask.xml")
		for node in ET.parse(source_file(retail, name, region)[0]).getroot()
	}
	result.update(
		int(value) for node in ET.parse(source_file(retail, "data_driven_quest.xml", region)[0]).getroot()
		if (value := node.findtext("id")) and value.isdigit()
	)
	return result


def audit_legacy_xml(legacy_dir: Path, enabled_ids: set[int], generated_ids: set[int], java_handler_ids: set[int],
		retail_ids: set[int], unresolved_ids: set[int], unsupported_ids: set[int], invalid_ids: set[int]) -> tuple[dict[str, dict[str, str]], dict[str, object]]:
	definitions = legacy_xml_definitions(legacy_dir)
	# 被生成产物遮蔽的旧定义是三层机制的预期状态：retail 覆盖 legacy，legacy 保留为回退。
	shadowed_ids = set(definitions) & generated_ids
	retained_ids = set(definitions) - shadowed_ids
	overlap = retained_ids & java_handler_ids
	if overlap:
		raise ValueError(f"quest ids have both XML and Java owners: {sorted(overlap)}")
	details: dict[str, dict[str, str]] = {}
	reasons: dict[str, int] = {}
	dispositions: dict[str, int] = {}
	for quest_id in sorted(retained_ids):
		definition = definitions[quest_id]
		if quest_id not in enabled_ids:
			reason = "outside_base"
		elif quest_id in unresolved_ids:
			reason = "unresolved_references"
		elif quest_id in unsupported_ids:
			reason = "unsupported_fields"
		elif quest_id in invalid_ids:
			reason = "invalid_retail_definition"
		elif definition["template"] not in GENERATED_TEMPLATE_TAGS:
			reason = "unsupported_template"
		elif quest_id not in retail_ids:
			reason = "missing_supported_retail_source"
		else:
			reason = "unsupported_retail_shape"
		disposition = LEGACY_DISPOSITIONS[reason]
		details[str(quest_id)] = {**definition, "reason": reason, "disposition": disposition}
		reasons[reason] = reasons.get(reason, 0) + 1
		dispositions[disposition] = dispositions.get(disposition, 0) + 1
	return details, {
		"files": len({definition["file"] for definition in definitions.values()}),
		"definitions": len(definitions),
		"shadowed_by_generated": len(shadowed_ids),
		"retained": len(retained_ids),
		"reasons": reasons,
		"dispositions": dispositions,
	}


def hunt_fields() -> set[str]:
	return BASE_FIELDS | IGNORED_FIELDS | {
		f"{field}{index}" for index in range(1, 6) for field in ("count", "monster")
	}


def simple_talks(path: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, int], dict[str, object]]:
	result: dict[int, dict[str, object]] = {}
	stats = {"retail": 0, "missing_base": 0, "unsupported": 0, "invalid": 0}
	skipped: dict[str, object] = {"unsupported": {}, "invalid": []}
	allowed = BASE_FIELDS | IGNORED_FIELDS | {"item_check", "give_item", "cutsceneid1", "cs1_haction"} | {
		f"{field}{index}" for index in range(1, 4) for field in ("talk_npc", "give_item", "remove_item")
	}
	for node in ET.parse(path).getroot():
		quest_id = int(node.attrib["id"])
		stats["retail"] += 1
		if quest_id not in enabled_ids:
			stats["missing_base"] += 1
			continue
		fields = {child.tag: (child.text or "").strip() for child in node}
		unsupported = sorted(set(fields) - allowed)
		if unsupported:
			stats["unsupported"] += 1
			skipped["unsupported"][str(quest_id)] = unsupported
			continue
		talks = [(index, fields[f"talk_npc{index}"]) for index in range(1, 4) if fields.get(f"talk_npc{index}")]
		if not fields.get("acquired_npc_name") or not fields.get("reward_npc_name") or fields.get("item_check", "1") != "1":
			stats["invalid"] += 1
			skipped["invalid"].append(quest_id)
			continue
		give_item = parse_item_reference(fields.get("give_item", "")) if fields.get("give_item") else None
		movie = fields.get("cutsceneid1")
		movie_action = fields.get("cs1_haction")
		if (fields.get("give_item") and give_item is None or bool(movie) != bool(movie_action)
				or movie and (not movie.isdigit() or not movie_action.isdigit())):
			stats["invalid"] += 1
			skipped["invalid"].append(quest_id)
			continue
		if movie and talks:
			stats["unsupported"] += 1
			skipped["unsupported"][str(quest_id)] = ["cutscene_with_talk"]
			continue
		steps = []
		valid = True
		for index in range(1, 4):
			give = parse_item_reference(fields.get(f"give_item{index}", "")) if fields.get(f"give_item{index}") else None
			remove = parse_item_reference(fields.get(f"remove_item{index}", "")) if fields.get(f"remove_item{index}") else None
			name = fields.get(f"talk_npc{index}")
			if (fields.get(f"give_item{index}") and give is None or fields.get(f"remove_item{index}") and remove is None
					or (give or remove) and not name):
				valid = False
				break
			if name:
				steps.append({"type": "TALK", "names": [name], "give_item": give, "remove_item": remove})
		if not valid:
			stats["invalid"] += 1
			skipped["invalid"].append(quest_id)
			continue
		if steps and (give_item or any(step["give_item"] or step["remove_item"] for step in steps) or "item_check" in fields):
			if "item_check" in fields:
				steps.append({"type": "COLLECT_ITEM", "names": [fields["reward_npc_name"]], "actions": []})
			result[quest_id] = {
				"kind": "data_driven_simple", "start_type": "TALK", "start": fields["acquired_npc_name"],
				"end": fields["reward_npc_name"], "start_give_item": give_item, "steps": steps,
			}
		elif movie and movie_action == "1009":
			step = {
				"type": "TALK", "names": [fields["reward_npc_name"]], "dialog_id": 2375,
				"advance_dialog_id": 1009, "movie": int(movie), "remove_item": give_item,
			}
			result[quest_id] = {
				"kind": "data_driven_simple", "start_type": "TALK", "start": fields["acquired_npc_name"],
				"end": fields["reward_npc_name"], "start_give_item": give_item, "steps": [step],
			}
		elif movie:
			stats["unsupported"] += 1
			skipped["unsupported"][str(quest_id)] = ["cutscene_action"]
		else:
			result[quest_id] = {
				"kind": "report_to_many" if talks else "item_collecting" if "item_check" in fields else "report_to",
				"start": fields["acquired_npc_name"], "end": fields["reward_npc_name"], "talks": [name for _, name in talks],
				"item": give_item,
			}
	return result, stats, skipped


def resolve_sentinel_starts(quests: dict[int, dict[str, object]], stats: dict[str, int], skipped: dict[str, object],
		sentinel: str, closed_ids: set[int], reason: str) -> None:
	"""获取哨兵（_faction_ / _challengetask_）：任务由对应系统发放（NPC 势力每日池、挑战任务列表），
	无起始 NPC。闭包集合由调用侧按双侧权威表求交；不在集合内则以原因码隔离，继续由旧实现兜底。"""
	for quest_id in sorted(quests):
		if quests[quest_id].get("start") != sentinel:
			continue
		if quest_id in closed_ids:
			quests[quest_id]["start"] = None
		else:
			del quests[quest_id]
			stats["unsupported"] += 1
			skipped["unsupported"][str(quest_id)] = [reason]


def reference_owners(path: Path) -> tuple[dict[str, set[int]], dict[int, tuple[str, tuple[int, ...]]]]:
	graph = json.loads(path.read_text(encoding="utf-8"))
	if graph.get("projection") != "runtime_scope":
		raise ValueError("quest generation requires a runtime-scope reference graph")
	owners: dict[str, set[int]] = {}
	aliases: dict[int, tuple[str, tuple[int, ...]]] = {}
	for reference in graph.get("references", []):
		if reference.get("status") != "RESOLVED":
			raise ValueError(f"open reference graph entry {reference.get('consumer')}")
		consumer = reference.get("consumer", {})
		if consumer.get("type") != "quest":
			continue
		quest_id = int(consumer["id"])
		kind = str(reference.get("kind"))
		owners.setdefault(kind, set()).add(quest_id)
		if kind == "npc_quest_alias":
			targets = reference.get("targets", [])
			ids = tuple(sorted({int(target["id"]) for target in targets if target.get("type") == "npc"}))
			binding = (str(reference.get("raw", "")).casefold(), ids)
			if not binding[0] or not ids or quest_id in aliases and aliases[quest_id] != binding:
				raise ValueError(f"invalid NPC alias reference for quest {quest_id}")
			aliases[quest_id] = binding
	return owners, aliases


def simple_collects(path: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, int], dict[str, object]]:
	result: dict[int, dict[str, object]] = {}
	stats = {"retail": 0, "missing_base": 0, "unsupported": 0, "invalid": 0}
	skipped: dict[str, object] = {"unsupported": {}, "invalid": []}
	allowed = BASE_FIELDS | IGNORED_FIELDS | {"give_item", "party_drop"} | {f"object{index}" for index in range(1, 5)}
	for node in ET.parse(path).getroot():
		quest_id = int(node.attrib["id"])
		stats["retail"] += 1
		if quest_id not in enabled_ids:
			stats["missing_base"] += 1
			continue
		fields = {child.tag: (child.text or "").strip() for child in node}
		unsupported = sorted(set(fields) - allowed)
		if unsupported:
			stats["unsupported"] += 1
			skipped["unsupported"][str(quest_id)] = unsupported
			continue
		objects = [fields[f"object{index}"] for index in range(1, 5) if fields.get(f"object{index}")]
		give_item = parse_item_reference(fields.get("give_item", "")) if fields.get("give_item") else None
		if (not fields.get("acquired_npc_name") or not fields.get("reward_npc_name") or not objects
				or fields.get("give_item") and give_item is None or fields.get("party_drop", "1") != "1"):
			stats["invalid"] += 1
			skipped["invalid"].append(quest_id)
			continue
		result[quest_id] = {
			"kind": "item_collecting",
			"start": fields["acquired_npc_name"],
			"end": fields["reward_npc_name"],
			"objects": objects,
			"item": give_item,
		}
	return result, stats, skipped


def simple_use_items(path: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[int, dict[str, object]], dict[str, int], dict[str, object]]:
	# 简单形状（use_item + 至多 2 talk，无物品流转）走 item_order 模板；
	# 复杂形状（3 talk 或 give/remove_item 物品流转）走 data_driven 步骤链，保留严格顺序语义。
	result: dict[int, dict[str, object]] = {}
	data_result: dict[int, dict[str, object]] = {}
	stats = {"retail": 0, "missing_base": 0, "unsupported": 0, "invalid": 0, "data_driven": 0}
	skipped: dict[str, object] = {"unsupported": {}, "invalid": []}
	simple_allowed = IGNORED_FIELDS | {"use_item_name", "reward_npc_name", "talk_npc1", "talk_npc2", "item_check"}
	data_allowed = IGNORED_FIELDS | {"use_item_name", "reward_npc_name", "talk_npc1", "talk_npc2", "talk_npc3",
		"give_item1", "give_item2", "give_item3", "remove_item1", "remove_item2", "remove_item3", "item_check"}
	for node in ET.parse(path).getroot():
		quest_id = int(node.attrib["id"])
		stats["retail"] += 1
		if quest_id not in enabled_ids:
			stats["missing_base"] += 1
			continue
		fields = {child.tag: (child.text or "").strip() for child in node}
		if not fields.get("use_item_name") or not fields.get("reward_npc_name") or fields.get("item_check", "1") != "1":
			stats["invalid"] += 1
			skipped["invalid"].append(quest_id)
			continue
		# 复杂形状：3 talk 或带 give/remove_item 物品流转 -> data_driven 步骤链
		has_item_flow = any(fields.get(k) for k in ("give_item1", "give_item2", "give_item3", "remove_item1", "remove_item2", "remove_item3"))
		if fields.get("talk_npc3") or has_item_flow:
			unsupported = sorted(set(fields) - data_allowed)
			if unsupported:
				stats["unsupported"] += 1
				skipped["unsupported"][str(quest_id)] = unsupported
				continue
			# use_item_name 形如 "ITEM_DOC_QUEST_3060A"（纯名无数量），start_item 仅需名字解析 ID
			start_item_name = fields["use_item_name"].removeprefix("ITEM_")
			steps: list[dict[str, object]] = []
			for idx in (1, 2, 3):
				talk = fields.get(f"talk_npc{idx}")
				if not talk:
					continue
				step: dict[str, object] = {"type": "TALK", "names": [talk]}
				give = parse_item_reference(fields.get(f"give_item{idx}", ""))
				remove = parse_item_reference(fields.get(f"remove_item{idx}", ""))
				if give:
					step["give_item"] = give
				if remove:
					step["remove_item"] = remove
				steps.append(step)
			data_result[quest_id] = {
				"kind": "data_driven_simple", "source": "compiled_script_use_item", "start_type": "ITEM_PLAY",
				"start": start_item_name, "end": fields["reward_npc_name"], "steps": steps,
			}
			stats["data_driven"] += 1
			continue
		# 简单形状 -> item_order
		unsupported = sorted(set(fields) - simple_allowed)
		if unsupported:
			stats["unsupported"] += 1
			skipped["unsupported"][str(quest_id)] = unsupported
			continue
		result[quest_id] = {
			"item": fields["use_item_name"].removeprefix("ITEM_"),
			"end": fields["reward_npc_name"],
			"talks": [fields[key] for key in ("talk_npc1", "talk_npc2") if fields.get(key)],
		}
	return result, data_result, stats, skipped


def simple_item_plays(path: Path, enabled_ids: set[int]) -> dict[int, dict[str, object]]:
	result: dict[int, dict[str, object]] = {}
	allowed = BASE_FIELDS | IGNORED_FIELDS | {"talk_npc1", "talk_npc2", "give_item1", "remove_item1", "use_item_name"}
	for node in ET.parse(path).getroot():
		quest_id = int(node.attrib["id"])
		if quest_id not in enabled_ids:
			continue
		fields = {child.tag: (child.text or "").strip() for child in node}
		give_item = parse_item_reference(fields.get("give_item1", ""))
		remove_item = parse_item_reference(fields.get("remove_item1", ""))
		use_item = parse_item_reference(fields.get("use_item_name", ""))
		if (set(fields) - allowed or not fields.get("acquired_npc_name") or not fields.get("reward_npc_name")
				or give_item is None or remove_item is None or use_item is None or use_item != give_item):
			continue
		steps = [{"type": "TALK", "names": [fields[key]], "give_item": None} for key in ("talk_npc1", "talk_npc2") if fields.get(key)]
		steps.append({"type": "ITEM_PLAY", "item": use_item[0]})
		result[quest_id] = {
			"kind": "data_driven_simple", "start_type": "TALK", "start": fields["acquired_npc_name"],
			"end": fields["reward_npc_name"], "start_give_item": give_item, "start_remove_item": remove_item, "steps": steps,
		}
	return result


def data_driven_talks(path: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, int], int]:
	result: dict[int, dict[str, object]] = {}
	candidates = {"data_driven_report": 0, "data_driven_talk": 0}
	missing_base = 0
	allowed = {"id", "name", "dev_name", "category_acquire_", "value0_acquire_", "reward_npc_name"}
	for node in ET.parse(path).getroot():
		fields = {child.tag: (child.text or "").strip() for child in node if child.tag != "progress_info"}
		acquire = fields.get("category_acquire_")
		if (set(fields) - allowed or acquire not in {"Talk", "none"} or not fields.get("reward_npc_name")
				or acquire == "Talk" and not fields.get("value0_acquire_") or acquire == "none" and fields.get("value0_acquire_")):
			continue
		steps = []
		for step in node.findall("./progress_info/data"):
			step_fields = {child.tag: (child.text or "").strip() for child in step}
			if set(step_fields) - {"category_progress_", "value0_progress_"} or step_fields.get("category_progress_") != "Talk" or not step_fields.get("value0_progress_"):
				break
			steps.append(step_fields["value0_progress_"])
		else:
			if acquire == "none" and not steps:
				continue
			source = "data_driven_talk" if steps else "data_driven_report"
			candidates[source] += 1
			quest_id = int(fields["id"])
			if quest_id not in enabled_ids:
				missing_base += 1
				continue
			if len({name.casefold() for name in steps}) != len(steps):
				result[quest_id] = {
					"kind": "data_driven_simple", "source": source, "start_type": "TALK",
					"start": fields.get("value0_acquire_") or None, "end": fields["reward_npc_name"],
					"steps": [{"type": "TALK", "names": [name]} for name in steps],
				}
			else:
				result[quest_id] = {
					"kind": "report_to_many" if steps else "report_to", "source": source,
					"start": fields.get("value0_acquire_") or None, "end": fields["reward_npc_name"], "talks": steps,
				}
			continue
		continue
	return result, candidates, missing_base


def data_driven_hunts(path: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], int, int]:
	result: dict[int, dict[str, object]] = {}
	candidates = 0
	missing_base = 0
	allowed = {"id", "name", "dev_name", "con_quest", "category_acquire_", "value0_acquire_", "reward_npc_name"}
	for node in ET.parse(path).getroot():
		fields = {child.tag: (child.text or "").strip() for child in node if child.tag != "progress_info"}
		progress = node.findall("./progress_info/data")
		acquire = fields.get("category_acquire_")
		if (set(fields) - allowed or acquire not in {"Talk", "none"}
				or not fields.get("id") or not fields.get("reward_npc_name")
				or acquire == "Talk" and not fields.get("value0_acquire_") or acquire == "none" and fields.get("value0_acquire_")
				or fields.get("con_quest") and not fields["con_quest"].isdigit() or len(progress) != 1):
			continue
		step = {child.tag: (child.text or "").strip() for child in progress[0]}
		stages = parse_hunt_actions(step.get("value0_progress_", ""))
		if set(step) - {"category_progress_", "value0_progress_"} or step.get("category_progress_") != "Hunt" or stages is None:
			continue
		candidates += 1
		quest_id = int(fields["id"])
		if quest_id not in enabled_ids:
			missing_base += 1
			continue
		result[quest_id] = {
			"source": "data_driven_hunt", "start": None if fields.get("value0_acquire_") == "_challengetask_" else fields.get("value0_acquire_") or None, "end": fields["reward_npc_name"],
			"stages": stages,
		}
	return result, candidates, missing_base


def data_driven_collects(path: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], int, int]:
	result: dict[int, dict[str, object]] = {}
	candidates = 0
	missing_base = 0
	allowed = {"id", "name", "dev_name", "con_quest", "category_acquire_", "value0_acquire_", "reward_npc_name"}
	for node in ET.parse(path).getroot():
		fields = {child.tag: (child.text or "").strip() for child in node if child.tag != "progress_info"}
		progress = node.findall("./progress_info/data")
		if (set(fields) - allowed or fields.get("category_acquire_") != "Talk"
				or not fields.get("id") or not fields.get("value0_acquire_") or not fields.get("reward_npc_name")
				or fields.get("con_quest") and not fields["con_quest"].isdigit() or len(progress) != 1):
			continue
		step = {child.tag: (child.text or "").strip() for child in progress[0]}
		if (set(step) - {"category_progress_", "value0_progress_"} or step.get("category_progress_") != "CollectItem"
				or step.get("value0_progress_") != fields["reward_npc_name"]):
			continue
		candidates += 1
		quest_id = int(fields["id"])
		if quest_id not in enabled_ids:
			missing_base += 1
			continue
		result[quest_id] = {
			"kind": "item_collecting", "source": "data_driven_collect",
			"start": fields["value0_acquire_"], "end": fields["reward_npc_name"], "talks": [],
		}
	return result, candidates, missing_base


def data_driven_pvps(path: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], int, int]:
	result: dict[int, dict[str, object]] = {}
	candidates = 0
	missing_base = 0
	allowed = {"id", "name", "dev_name", "con_quest", "category_acquire_", "value0_acquire_", "reward_npc_name"}
	for node in ET.parse(path).getroot():
		fields = {child.tag: (child.text or "").strip() for child in node if child.tag != "progress_info"}
		progress = node.findall("./progress_info/data")
		if (set(fields) - allowed or fields.get("category_acquire_") != "Talk"
				or not fields.get("id") or not fields.get("value0_acquire_") or not fields.get("reward_npc_name")
				or fields.get("con_quest") and not fields["con_quest"].isdigit() or len(progress) != 1):
			continue
		step = {child.tag: (child.text or "").strip() for child in progress[0]}
		if set(step) - {"category_progress_", "value0_progress_"} or step.get("category_progress_") != "PVP":
			continue
		try:
			amount = int(step.get("value0_progress_", ""))
		except ValueError:
			continue
		if amount <= 0:
			continue
		candidates += 1
		quest_id = int(fields["id"])
		if quest_id not in enabled_ids:
			missing_base += 1
			continue
		result[quest_id] = {"start": fields["value0_acquire_"], "end": fields["reward_npc_name"], "amount": amount}
	return result, candidates, missing_base


def data_driven_item_plays(path: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], int, int]:
	result: dict[int, dict[str, object]] = {}
	candidates = 0
	missing_base = 0
	allowed = {"id", "name", "dev_name", "con_quest", "category_acquire_", "value0_acquire_", "reward_npc_name"}
	for node in ET.parse(path).getroot():
		fields = {child.tag: (child.text or "").strip() for child in node if child.tag != "progress_info"}
		if (set(fields) - allowed or fields.get("category_acquire_") != "ItemPlay"
				or not fields.get("id") or not fields.get("value0_acquire_") or not fields.get("reward_npc_name")
				or fields.get("con_quest") and not fields["con_quest"].isdigit() or node.find("progress_info") is not None):
			continue
		candidates += 1
		quest_id = int(fields["id"])
		if quest_id not in enabled_ids:
			missing_base += 1
			continue
		result[quest_id] = {
			"source": "data_driven_item_play", "item": fields["value0_acquire_"],
			"end": fields["reward_npc_name"], "talks": [],
		}
	return result, candidates, missing_base


def data_driven_complex(path: Path, enabled_ids: set[int], handled_ids: set[int]) -> dict[int, dict[str, object]]:
	result: dict[int, dict[str, object]] = {}
	allowed = {"id", "name", "dev_name", "con_quest", "category_acquire_", "reward_npc_name"} | {
		f"value{slot}_acquire_" for slot in range(11)
	}
	progress_fields = {"category_progress_"} | {f"value{slot}_progress_" for slot in range(11)}
	for node in ET.parse(path).getroot():
		fields = {child.tag: (child.text or "").strip() for child in node if child.tag != "progress_info"}
		if not fields.get("id") or not fields["id"].isdigit():
			continue
		quest_id = int(fields["id"])
		if quest_id not in enabled_ids or quest_id in handled_ids or set(fields) - allowed:
			continue
		acquire = fields.get("category_acquire_", "").casefold()
		if acquire not in {"talk", "itemplay", "enterarea"} or not fields.get("reward_npc_name"):
			continue
		if acquire in {"talk", "itemplay"} and not fields.get("value0_acquire_") or acquire == "enterarea" and fields.get("value0_acquire_"):
			continue
		acquire_actions = supported_extra_actions(fields, "acquire")
		if acquire_actions is None:
			continue
		steps: list[dict[str, object]] = []
		valid = True
		for step_node in node.findall("./progress_info/data"):
			step = {child.tag: (child.text or "").strip() for child in step_node}
			category = step.get("category_progress_", "").casefold()
			value = step.get("value0_progress_", "")
			actions = supported_extra_actions(step, "progress")
			if set(step) - progress_fields or actions is None:
				valid = False
				break
			actions = dict(actions)
			if movie := actions.pop("cutscene", None):
				actions["movie"] = movie
			if spawns := actions.pop("spawn_npc", None):
				actions["spawns"] = spawns
			if timer := actions.pop("timer", None):
				actions["timer_seconds"], actions["timer_destination_progress"] = timer
			if category == "talk" and value:
				steps.append({"type": "TALK", "names": [value], **actions})
			elif category == "collectitem" and value and (not step.get("value5_progress_") or step["value5_progress_"].isdigit()):
				steps.append({
					"type": "COLLECT_ITEM", "names": [value],
					"actions": [step[f"value{slot}_progress_"] for slot in range(1, 5) if step.get(f"value{slot}_progress_")],
				})
			elif category == "hunt":
				stages = parse_hunt_actions(value)
				if stages is None or len({count for _, count in stages}) != 1:
					valid = False
					break
				steps.append({"type": "HUNT", "names": [name for names, _ in stages for name in names], "amount": stages[0][1], **actions})
			elif category == "enterarea" and value:
				steps.append({"type": "ENTER_AREA", "names": [value], **actions})
			elif category == "enterworld" and value.isdigit():
				steps.append({"type": "ENTER_WORLD", "world_id": int(value), **actions})
			elif category == "itemplay" and value:
				steps.append({"type": "ITEM_PLAY", "item": value.removeprefix("ITEM_"), **actions})
			else:
				valid = False
				break
		if valid and (steps or acquire == "enterarea"):
			result[quest_id] = {
				"start_type": {"talk": "TALK", "itemplay": "ITEM_PLAY", "enterarea": "ENTER_AREA"}[acquire],
				"start": fields.get("value0_acquire_"), "end": fields["reward_npc_name"],
				"start_give_item": acquire_actions.get("give_item"),
				"start_remove_item": acquire_actions.get("remove_item"), "steps": steps,
			}
	return result


def data_driven_shape_audit(path: Path) -> tuple[dict[str, object], list[dict[str, object]]]:
	root = ET.parse(path).getroot()
	nodes = [node for node in root if (node.findtext("id") or "").isdigit()]
	quest_ids = {int(node.findtext("id")) for node in nodes}
	if len(nodes) != len(quest_ids):
		raise ValueError("data-driven quest ids must be unique integers")

	rules: dict[int, str] = {}

	def assign(rule: str, quests: dict[int, dict[str, object]]) -> None:
		overlap = set(quests) & set(rules)
		if overlap:
			raise ValueError(f"data-driven mapping rules overlap: {sorted(overlap)}")
		rules.update({quest_id: rule for quest_id in quests})

	talks = data_driven_talks(path, quest_ids)[0]
	for rule in ("data_driven_report", "data_driven_talk"):
		assign(rule, {quest_id: quest for quest_id, quest in talks.items() if quest["source"] == rule})
	assign("data_driven_hunt", data_driven_hunts(path, quest_ids)[0])
	assign("data_driven_collect", data_driven_collects(path, quest_ids)[0])
	assign("data_driven_pvp", data_driven_pvps(path, quest_ids)[0])
	assign("data_driven_item_play", data_driven_item_plays(path, quest_ids)[0])
	assign("data_driven_complex", data_driven_complex(path, quest_ids, set(rules)))

	def phase_shape(node: ET.Element, phase: str) -> tuple[str, tuple[int, ...], tuple[int, ...], tuple[str, ...]]:
		category_tag = f"category_{phase}_"
		value_tags = {f"value{slot}_{phase}_" for slot in range(11)}
		present = tuple(slot for slot in range(11) if node.find(f"value{slot}_{phase}_") is not None)
		nonempty = tuple(slot for slot in present if (node.findtext(f"value{slot}_{phase}_") or "").strip())
		ignored = {"id", "name", "dev_name", "reward_npc_name", "progress_info", category_tag, *value_tags}
		other_fields = tuple(child.tag for child in node if child.tag not in ignored)
		return ((node.findtext(category_tag) or "").strip().casefold(), present, nonempty, other_fields)

	groups: dict[tuple[object, ...], dict[str, object]] = {}
	for node in nodes:
		quest_id = int(node.findtext("id"))
		shape = (phase_shape(node, "acquire"), tuple(phase_shape(step, "progress") for step in node.findall("./progress_info/data")))
		group = groups.setdefault(shape, {
			"current_parser_candidate_quest_ids": [], "semantic_gap_quest_ids": [], "mapping_rules": {},
		})
		if rule := rules.get(quest_id):
			group["current_parser_candidate_quest_ids"].append(quest_id)
			mapping_rules = group["mapping_rules"]
			mapping_rules[rule] = mapping_rules.get(rule, 0) + 1
		else:
			group["semantic_gap_quest_ids"].append(quest_id)

	def describe(shape: tuple[str, tuple[int, ...], tuple[int, ...], tuple[str, ...]]) -> dict[str, object]:
		category, present, nonempty, other_fields = shape
		return {
			"category": category, "present_slots": list(present), "nonempty_slots": list(nonempty),
			"other_fields": list(other_fields),
		}

	details = []
	for (acquire, progress), group in sorted(groups.items(), key=lambda entry: repr(entry[0])):
		details.append({
			"acquire": describe(acquire), "progress": [describe(step) for step in progress],
			"current_parser_candidate": len(group["current_parser_candidate_quest_ids"]),
			"semantic_gap": len(group["semantic_gap_quest_ids"]),
			**group,
		})
	rule_counts = {rule: sum(mapped == rule for mapped in rules.values()) for rule in sorted(set(rules.values()))}
	return {
		"source_records": len(quest_ids), "source_shapes": len(details),
		"candidate_scope": "parser_only", "current_parser_candidate": len(rules),
		"semantic_gap": len(quest_ids - set(rules)),
		"mapping_rules": rule_counts,
	}, details


def compiled_item_buyers(retail_quest: Path, script_root: Path, quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	sources = {}
	for number in COMPILED_ITEM_BUYER_SOURCE_NUMBERS:
		path = script_root / f"fun_{number:03d}.cpp"
		sources[number] = path.read_text(encoding="utf-8") if path.is_file() else ""
	for quest_id, (npc_name, quest_hex, reward_gold, race) in COMPILED_ITEM_BUYERS.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		light = quest_id < 20000
		binding_source = sources[620 if light else 611]
		dialog_source = sources[869 if light else 868]
		turn_in_source = sources[882 if light else 880]
		phase_sources = [sources[number] for number in ((691, 688, 697) if light else (690, 687, 695))]
		reasons = []
		expected_fields = {
			"name": f"Q{quest_id}", "max_repeat_count": "255", "cannot_share": "1",
			"collect_progress": "0", "collect_item1": "exceed_enchant_key_01 1",
			"inventory_item_name1": "exceed_enchant_key_01", "check_item1_1": "exceed_enchant_key_01 1",
			"reward_gold1": str(reward_gold), "race_permitted": race,
		}
		for field, expected in expected_fields.items():
			if fields.get(field) != expected:
				reasons.append(f"quest.xml:{field}")
		base = base_nodes.get(quest_id)
		collect = None if base is None else base.find("./collect_items/collect_item")
		inventory = None if base is None else base.find("./inventory_items/inventory_item")
		rewards = None if base is None else base.find("rewards")
		expected_base_race = "ELYOS" if light else "ASMODIANS"
		if (base is None or base.attrib.get("race_permitted") != expected_base_race
				or collect is None or collect.attrib != {"item_id": "166500002", "count": "1"}
				or inventory is None or inventory.attrib != {"item_id": "166500002"}
				or rewards is None or rewards.attrib.get("gold") != str(reward_gold)):
			reasons.append("quest_data.xml:item_and_reward")
		if not has_function_evidence(binding_source, quest_hex, (f'L"{npc_name}"', quest_hex)):
			reasons.append("script:npc_binding")
		dialog_tokens = (f",{quest_hex})", f",{quest_hex},0,0)", f",{quest_hex},0)", "+ 0xd0", "+ 0x148", "+ 0x160", "+ 0x308", "+ 0x1b0", "0x3f1")
		if not has_function_evidence(dialog_source, quest_hex, dialog_tokens):
			reasons.append("script:dialog_and_inventory")
		turn_in_tokens = (f",{quest_hex},0,0)", f",{quest_hex},0,1)", "+ 0x308", "+ 0x268", "+ 0x1e0", "+ 0x160", "0x3f0", "0x3f1")
		if not has_function_evidence(turn_in_source, quest_hex, turn_in_tokens):
			reasons.append("script:turn_in")
		for phase, source in zip((0, 3, 4), phase_sources):
			if not has_function_evidence(source, quest_hex, (f",{quest_hex},{phase},",)):
				reasons.append(f"script:phase_{phase}")
		if reasons:
			skipped[str(quest_id)] = reasons
			continue
		result[quest_id] = {
			"kind": "item_collecting", "source": "compiled_script_collect",
			"start": npc_name, "end": npc_name, "talks": [],
			"evidence_item": "exceed_enchant_key_01",
		}
	return result, skipped


def compiled_firework_reports(retail_quest: Path, script_root: Path, quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	sources = {}
	for number in COMPILED_FIREWORK_SOURCE_NUMBERS:
		path = script_root / f"fun_{number:03d}.cpp"
		sources[number] = path.read_text(encoding="utf-8") if path.is_file() else ""
	for quest_id, (start, talks, quest_hex, race) in COMPILED_FIREWORK_REPORTS.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		reasons = []
		expected_fields = {
			"name": f"Q{quest_id}", "max_repeat_count": "255", "quest_repeat_cycle": "all",
			"reward_exp1": "0", "reward_gold1": "0", "reward_glory_point1": "10", "race_permitted": race,
		}
		for field, expected in expected_fields.items():
			if fields.get(field) != expected:
				reasons.append(f"quest.xml:{field}")
		base = base_nodes.get(quest_id)
		rewards = None if base is None else base.find("rewards")
		expected_base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
		if (base is None or base.attrib.get("race_permitted") != expected_base_race
				or base.attrib.get("max_repeat_count") != "255" or base.attrib.get("repeat_cycle") != "ALL"
				or rewards is None or rewards.attrib != {"gp": "10"}):
			reasons.append("quest_data.xml:repeat_race_and_reward")
		for npc_name in (start, *talks):
			if not has_function_evidence(sources[630], quest_hex, (f'L"{npc_name}"', quest_hex)):
				reasons.append(f"script:npc_binding:{npc_name}")
		for phase, number in ((0, 694), (4, 701)):
			if not has_function_evidence(sources[number], quest_hex, (f",{quest_hex},{phase},0xffffffff,0)",)):
				reasons.append(f"script:phase_{phase}")
		for phase, number in ((0, 705), (1, 708)):
			if not has_function_evidence(sources[number], quest_hex, (f",{quest_hex},3,{phase},0)",)):
				reasons.append(f"script:talk_phase_{phase}")
		if not has_function_evidence(sources[869], quest_hex, ("0x3f3", quest_hex)):
			reasons.append("script:start_dialog")
		if not has_function_evidence(sources[894], quest_hex, ("0x548", "10000", "120000", "+ 0xf0", f",{quest_hex},1)")):
			reasons.append("script:first_talk_and_timer")
		if not has_function_evidence(sources[897], quest_hex, ("0x69d", "0x2711", "+ 0x100", f",{quest_hex},2)")):
			reasons.append("script:second_talk")
		if not has_function_evidence(sources[875], quest_hex, ("*(unsigned int *)(lVar3 + 1) < 2", "+ 600", "+ 0xf0", f",{quest_hex},0,0)")):
			reasons.append("script:timeout_reset")
		if not has_function_evidence(sources[885], quest_hex, ("+ 0x1a8", "+ 0x1a0", "0x3ef")):
			reasons.append("script:turn_in")
		if reasons:
			skipped[str(quest_id)] = reasons
			continue
		result[quest_id] = {
			"kind": "report_to_many", "source": "compiled_script_timed_report",
			"start": start, "end": start, "talks": list(talks),
			"timeout_seconds": 120, "timeout_start_var": 1, "timeout_reset_var": 0,
		}
	return result, skipped


def compiled_debris_rescues(retail_quest: Path, retail_npcs: Path, retail_items: Path, script_root: Path, quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_index, _ = npc_indexes(retail_npcs, set(COMPILED_DEBRIS_NPCS), set())
	item_names = {name for data in COMPILED_DEBRIS_RESCUES.values() for name in data[3]}
	retail_item_ids = item_ids(retail_items, item_names)
	sources = {}
	for number in COMPILED_DEBRIS_SOURCE_NUMBERS:
		path = script_root / f"fun_{number:03d}.cpp"
		sources[number] = path.read_text(encoding="utf-8") if path.is_file() else ""
	for quest_id, (quest_hex, race, reward_gold, reward_names, reward_ids, lition_data, debris_data, rodelion_data, start_callback, turn_in_callback, rodelion_callback) in COMPILED_DEBRIS_RESCUES.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		reasons = []
		expected_fields = {
			"name": f"Q{quest_id}", "max_repeat_count": "1", "reward_exp1": "6907092",
			"reward_gold1": str(reward_gold), "race_permitted": race,
		}
		for field, expected in expected_fields.items():
			if fields.get(field) != expected:
				reasons.append(f"quest.xml:{field}")
		expected_rewards = {f"selectable_reward_item1_{index}": f"{name} 15" for index, name in enumerate(reward_names, 1)}
		actual_rewards = {field: value for field, value in fields.items() if field.startswith("selectable_reward_item1_")}
		if actual_rewards != expected_rewards:
			reasons.append("quest.xml:selectable_rewards")
		base = base_nodes.get(quest_id)
		rewards = None if base is None else base.find("rewards")
		base_rewards = [] if rewards is None else [node.attrib for node in rewards.findall("selectable_reward_item")]
		expected_base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
		if (base is None or base.attrib.get("race_permitted") != expected_base_race
				or base.attrib.get("max_repeat_count") != "1" or rewards is None
				or rewards.attrib != {"gold": str(reward_gold), "exp": "6907092"}
				or base_rewards != [{"item_id": str(item_id), "count": "15"} for item_id in reward_ids]):
			reasons.append("quest_data.xml:race_and_rewards")
		for name, expected_id in zip(reward_names, reward_ids):
			if retail_item_ids.get(name.casefold()) != (expected_id,):
				reasons.append(f"Items.xml:{name}")
		for name, expected_id in COMPILED_DEBRIS_NPCS.items():
			if npc_index.get(name.casefold()) != (expected_id,):
				reasons.append(f"npcs.xml:{name}")
		for number, npc_name, npc_data in ((624, "Lition", lition_data), (619, "IDYun_Debris_Q30503", debris_data), (626, "Rodelion", rodelion_data)):
			if not has_function_evidence(sources[number], quest_hex, (npc_data, f'L"{npc_name}"', quest_hex)):
				reasons.append(f"script:npc_binding:{npc_name}")
		for phase, number, npc_data, suffix in ((0, 692, lition_data, "0,0xffffffff,0"), (4, 698, lition_data, "4,0xffffffff,0"), (3, 703, debris_data, "3,0,3"), (3, 704, rodelion_data, "3,0,0")):
			if not has_function_evidence(sources[number], quest_hex, (npc_data, f",{quest_hex},{suffix})")):
				reasons.append(f"script:phase_{phase}:{npc_data}")
		if not has_function_evidence(sources[656], lition_data, (lition_data, "0x26", start_callback)):
			reasons.append("script:start_callback_binding")
		if not has_function_evidence(sources[661], lition_data, (lition_data, "0x32", turn_in_callback)):
			reasons.append("script:turn_in_callback_binding")
		if not has_function_evidence(sources[667], debris_data, (debris_data, "3,0,&LAB_180f12fc0")):
			reasons.append("script:debris_callback_binding")
		if not has_function_evidence(sources[668], rodelion_data, (rodelion_data, f"3,0,{rodelion_callback}")):
			reasons.append("script:rodelion_callback_binding")
		if not has_function_evidence(sources[878], "FUN_180f12f70", ("+ 0x4b8",)):
			reasons.append("script:debris_delete")
		if not has_function_evidence(sources[882], quest_hex, (f"FUN_180cab520({quest_hex}", ",0,0)")):
			reasons.append("script:start_dialog")
		if not has_function_evidence(sources[888], quest_hex, (f"FUN_180caf6c0({quest_hex}", f"FUN_180caf350({quest_hex}")):
			reasons.append("script:turn_in")
		rodelion_tokens = (f"FUN_180caf740({quest_hex}", "0x280f", f"FUN_180caf3c0({quest_hex}", "+ 0x5d8", f",{quest_hex},0,0)", "+ 0x100", "+ 0x110", "0x10f54c")
		if not has_function_evidence(sources[893], quest_hex, rodelion_tokens):
			reasons.append("script:rodelion_dialog")
		if not has_function_evidence(sources[877], quest_hex, ("param_1[3] != 0", "+ 0x4b8")):
			reasons.append("script:quest_state_notification")
		if reasons:
			skipped[str(quest_id)] = reasons
			continue
		result[quest_id] = {
			"source": "compiled_script_action_talk", "start_type": "TALK", "start": "Lition", "end": "Lition",
			"steps": [{"type": "TALK", "names": ["Rodelion"], "actions": ["IDYun_Debris_Q30503"], "delete_action_target": True}],
		}
	return result, skipped


def compiled_world_collects(retail_quest: Path, retail_npcs: Path, retail_items: Path, script_root: Path, quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_names = {npc_name for _, _, drops in COMPILED_WORLD_COLLECTS.values() for npc_name, *_ in drops}
	item_names = {item_name for _, _, drops in COMPILED_WORLD_COLLECTS.values() for _, _, item_name, _ in drops}
	npc_index, _ = npc_indexes(retail_npcs, npc_names, set())
	retail_item_ids = item_ids(retail_items, item_names)
	source_path = script_root / "fun_873.cpp"
	source = source_path.read_text(encoding="utf-8") if source_path.is_file() else ""
	for quest_id, (quest_hex, race, drops) in COMPILED_WORLD_COLLECTS.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		reasons = []
		expected_fields = {
			"name": f"Q{quest_id}", "max_repeat_count": "255", "client_level": "1",
			"minlevel_permitted": "1", "race_permitted": race,
		}
		for index, (npc_name, _, item_name, _) in enumerate(drops, 1):
			expected_fields.update({
				f"collect_item{index}": f"{item_name} 1", f"drop_monster_{index}": npc_name,
				f"drop_item_{index}": item_name, f"drop_prob_{index}": "100", f"drop_each_member_{index}": "1",
			})
		for field, expected in expected_fields.items():
			if fields.get(field) != expected:
				reasons.append(f"quest.xml:{field}")
		base = base_nodes.get(quest_id)
		expected_base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
		collect_items = [] if base is None else [node.attrib for node in base.findall("./collect_items/collect_item")]
		quest_drops = [] if base is None else [node.attrib for node in base.findall("quest_drop")]
		if (base is None or base.attrib.get("race_permitted") != expected_base_race
				or base.attrib.get("max_repeat_count") != "255"
				or collect_items != [{"item_id": str(item_id), "count": "1"} for _, _, _, item_id in drops]
				or quest_drops != [{"npc_id": str(npc_id), "item_id": str(item_id), "chance": "100", "drop_each_member": "1"} for _, npc_id, _, item_id in drops]):
			reasons.append("quest_data.xml:collect_and_drops")
		for npc_name, npc_id, item_name, item_id in drops:
			if npc_index.get(npc_name.casefold()) != (npc_id,):
				reasons.append(f"npcs.xml:{npc_name}")
			if retail_item_ids.get(item_name.casefold()) != (item_id,):
				reasons.append(f"Items.xml:{item_name}")
		world_hex = hex(COMPILED_WORLD_COLLECT_WORLD_ID)
		script_tokens = (f"param_2 + 0x14) == {world_hex}", "+ 0x148", f"param_2 + 0xc) == {world_hex}", "+ 0xd0", "+ 0x160")
		if not has_function_evidence(source, quest_hex, script_tokens):
			reasons.append("script:world_lifecycle")
		if reasons:
			skipped[str(quest_id)] = reasons
			continue
		result[quest_id] = {
			"source": "compiled_script_world_collect", "start_type": "WORLD_ACTIVE",
			"world_id": COMPILED_WORLD_COLLECT_WORLD_ID, "steps": [],
		}
	return result, skipped


def compiled_growth_quests(retail_quest: Path, retail_npcs: Path, retail_items: Path, script_root: Path, quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_names = {data[0] for data in COMPILED_GROWTH_QUESTS.values()}
	item_names = {name for data in COMPILED_GROWTH_QUESTS.values() for name in (data[4], data[6])}
	npc_index, _ = npc_indexes(retail_npcs, npc_names, set())
	retail_item_ids = item_ids(retail_items, item_names)
	sources = {}
	for number in COMPILED_GROWTH_SOURCE_NUMBERS:
		path = script_root / f"fun_{number:03d}.cpp"
		sources[number] = path.read_text(encoding="utf-8") if path.is_file() else ""
	for quest_id, (npc_name, npc_id, quest_hex, race, inventory_name, inventory_id, reward_name, reward_id, callback) in COMPILED_GROWTH_QUESTS.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		reasons = []
		expected_fields = {
			"name": f"Q{quest_id}", "max_repeat_count": "255", "minlevel_permitted": "66",
			"cannot_share": "1", "inventory_item_name1": inventory_name, "quest_cooltime": "2592000",
			"reward_exp1": "600000000", "reward_item1_1": f"{reward_name} 1", "race_permitted": race,
		}
		for field, expected in expected_fields.items():
			if fields.get(field) != expected:
				reasons.append(f"quest.xml:{field}")
		base = base_nodes.get(quest_id)
		inventory = None if base is None else base.find("./inventory_items/inventory_item")
		rewards = None if base is None else base.find("rewards")
		reward_items = [] if rewards is None else [node.attrib for node in rewards.findall("reward_item")]
		expected_base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
		if (base is None or base.attrib.get("race_permitted") != expected_base_race
				or base.attrib.get("max_repeat_count") != "255" or base.attrib.get("quest_cooltime") != "2592000"
				or inventory is None or inventory.attrib != {"item_id": str(inventory_id)}
				or rewards is None or rewards.attrib != {"exp": "600000000"}
				or reward_items != [{"item_id": str(reward_id), "count": "1"}]):
			reasons.append("quest_data.xml:inventory_and_reward")
		if npc_index.get(npc_name.casefold()) != (npc_id,):
			reasons.append(f"npcs.xml:{npc_name}")
		for name, item_id in ((inventory_name, inventory_id), (reward_name, reward_id)):
			if retail_item_ids.get(name.casefold()) != (item_id,):
				reasons.append(f"Items.xml:{name}")
		light = race == "pc_light"
		if not has_function_evidence(sources[620 if light else 611], quest_hex, (f'L"{npc_name}"', quest_hex)):
			reasons.append("script:npc_binding")
		if not has_function_evidence(sources[691 if light else 690], quest_hex, (f",{quest_hex},0,0xffffffff,0)",)):
			reasons.append("script:start_phase")
		if not has_function_evidence(sources[717], quest_hex, (f",{quest_hex},4,1,0)",)):
			reasons.append("script:turn_in_phase")
		if not has_function_evidence(sources[649], quest_hex, (f",{quest_hex},5,{hex(reward_id)},{callback})",)):
			reasons.append("script:item_event_binding")
		if not has_function_evidence(sources[876], quest_hex, ("+ 0xd0", "local_res8 == '\\x03'", "local_res9 == 0", "+ 0x100", f",{quest_hex},1)")):
			reasons.append("script:item_event_transition")
		for number, helper in ((882 if light else 880, "FUN_180caf640"), (887 if light else 885, "FUN_180caf6c0")):
			if not has_function_evidence(sources[number], quest_hex, (f"{helper}({quest_hex}", f"FUN_180caf350({quest_hex}")):
				reasons.append(f"script:dialog:{helper}")
		if reasons:
			skipped[str(quest_id)] = reasons
			continue
		result[quest_id] = {
			"source": "compiled_script_get_item", "start_type": "TALK", "start": npc_name, "end": npc_name,
			"steps": [{"type": "GET_ITEM", "item": reward_name}],
		}
	return result, skipped


def compiled_sensory_completes(retail_quest: Path, retail_npcs: Path, script_root: Path, quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_names = {data[0] for data in COMPILED_SENSORY_COMPLETES.values()}
	npc_index, _ = npc_indexes(retail_npcs, npc_names, set())
	sources = {}
	for number in COMPILED_SENSORY_SOURCE_NUMBERS:
		path = script_root / f"fun_{number:03d}.cpp"
		sources[number] = path.read_text(encoding="utf-8") if path.is_file() else ""
	for quest_id, (npc_name, npc_id, quest_hex, race, prerequisite) in COMPILED_SENSORY_COMPLETES.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		reasons = []
		expected_fields = {
			"name": f"Q{quest_id}", "max_repeat_count": "1", "minlevel_permitted": "50",
			"finished_quest_cond1": f"Q{prerequisite}", "reward_exp1": "0", "reward_gold1": "0",
			"race_permitted": race,
		}
		for field, expected in expected_fields.items():
			if fields.get(field) != expected:
				reasons.append(f"quest.xml:{field}")
		base = base_nodes.get(quest_id)
		finished = None if base is None else base.find("./start_conditions/finished")
		expected_base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
		if (base is None or base.attrib.get("race_permitted") != expected_base_race
				or base.attrib.get("max_repeat_count") != "1"
				or finished is None or finished.attrib != {"quest_id": str(prerequisite)}):
			reasons.append("quest_data.xml:race_and_prerequisite")
		if npc_index.get(npc_name.casefold()) != (npc_id,):
			reasons.append(f"npcs.xml:{npc_name}")
		light = race == "pc_light"
		if not has_function_evidence(sources[620 if light else 611], quest_hex, (f'L"{npc_name}"', quest_hex)):
			reasons.append("script:npc_binding")
		register_tokens = ("+ 0x1e0", "0x6a" if light else "0x8a", quest_hex)
		if not has_function_evidence(sources[872 if light else 871], quest_hex, register_tokens):
			reasons.append("script:sensory_callback")
		if not has_function_evidence(sources[871 if light else 873], quest_hex, ("+ 0x148", "+ 0x150", f",{quest_hex},1)")):
			reasons.append("script:start_and_progress")
		if not has_function_evidence(sources[875], quest_hex, ("+ 0xd0", "+ 0x268", f",{quest_hex},0,1)")):
			reasons.append("script:auto_finish")
		prerequisite_token = str(prerequisite) if light else hex(prerequisite)
		trigger_tokens = (f",{prerequisite_token})", "+ 0x138", f",{quest_hex})", "+ 0xd8", "+ 0x100", "+ 0x1c0", "0x17")
		if not has_function_evidence(sources[906], quest_hex, trigger_tokens):
			reasons.append("script:prerequisite_and_trigger")
		if reasons:
			skipped[str(quest_id)] = reasons
			continue
		result[quest_id] = {
			"source": "compiled_script_sensory_complete", "start_type": "SENSORY_COMPLETE",
			"start": npc_name, "steps": [],
		}
	return result, skipped


def compiled_paios_rescues(retail_quest: Path, retail_npcs: Path, retail_items: Path, script_root: Path, quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_index, _ = npc_indexes(retail_npcs, set(COMPILED_PAIOS_NPCS), set())
	retail_item_ids = item_ids(retail_items, {name for name, _ in COMPILED_PAIOS_REWARDS})
	sources = {}
	for number in COMPILED_PAIOS_SOURCE_NUMBERS:
		path = script_root / f"fun_{number:03d}.cpp"
		sources[number] = path.read_text(encoding="utf-8") if path.is_file() else ""
	for quest_id, (quest_hex, race, reward_gold, prerequisite, lition_data, column_data, paios_data, column_callback, paios_callback) in COMPILED_PAIOS_RESCUES.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		reasons = []
		expected_fields = {
			"name": f"Q{quest_id}", "minlevel_permitted": "57", "max_repeat_count": "1",
			"finished_quest_cond1": f"Q{prerequisite}", "reward_exp1": "6907092",
			"reward_gold1": str(reward_gold), "race_permitted": race,
		}
		for field, expected in expected_fields.items():
			if fields.get(field) != expected:
				reasons.append(f"quest.xml:{field}")
		expected_rewards = {f"selectable_reward_item1_{index}": f"{name} 15" for index, (name, _) in enumerate(COMPILED_PAIOS_REWARDS, 1)}
		actual_rewards = {field: value.casefold() for field, value in fields.items() if field.startswith("selectable_reward_item1_")}
		if actual_rewards != expected_rewards:
			reasons.append("quest.xml:selectable_rewards")
		base = base_nodes.get(quest_id)
		rewards = None if base is None else base.find("rewards")
		finished = None if base is None else base.find("./start_conditions/finished")
		expected_base_race = "ELYOS" if race == "pc_light" else "ASMODIANS"
		if (base is None or base.attrib.get("race_permitted") != expected_base_race
				or base.attrib.get("minlevel_permitted") != "60" or base.attrib.get("max_repeat_count") != "1"
				or finished is None or finished.attrib != {"quest_id": str(prerequisite)}
				or rewards is None or rewards.attrib != {"gold": str(reward_gold), "exp": "6907092"}
				or [node.attrib for node in rewards.findall("selectable_reward_item")] != [
					{"item_id": str(item_id), "count": "15"} for _, item_id in COMPILED_PAIOS_REWARDS]):
			reasons.append("quest_data.xml:conditions_and_rewards")
		for name, expected_id in COMPILED_PAIOS_NPCS.items():
			if npc_index.get(name.casefold()) != (expected_id,):
				reasons.append(f"npcs.xml:{name}")
		for name, expected_id in COMPILED_PAIOS_REWARDS:
			if retail_item_ids.get(name.casefold()) != (expected_id,):
				reasons.append(f"Items.xml:{name}")
		for number, npc_name, npc_data in ((624, "Lition", lition_data), (619, "IDYun_Column_Q30504", column_data), (625, "Paios", paios_data)):
			if not has_function_evidence(sources[number], quest_hex, (npc_data, f'L"{npc_name}"', quest_hex)):
				reasons.append(f"script:npc_binding:{npc_name}")
		for number, npc_data, suffix in ((698, lition_data, "4,0xffffffff,0"), (703, column_data, "3,0,3"), (707, paios_data, "3,1,0")):
			if not has_function_evidence(sources[number], quest_hex, (npc_data, f",{quest_hex},{suffix})")):
				reasons.append(f"script:phase:{npc_data}")
		if not has_function_evidence(sources[667], column_data, (column_data, "3,0", column_callback)):
			reasons.append("script:column_callback_binding")
		if not has_function_evidence(sources[671], paios_data, (paios_data, "3,1", paios_callback)):
			reasons.append("script:paios_callback_binding")
		if not has_function_evidence(sources[882], quest_hex, (f"FUN_180cab520({quest_hex}", ",0,0)")):
			reasons.append("script:start_dialog")
		if not has_function_evidence(sources[892], quest_hex, ("+ 0xf8", f",{quest_hex},1)", "+ 0x110", "+ 0x4b8")):
			reasons.append("script:column_progress")
		if not has_function_evidence(sources[873], quest_hex, (hex(COMPILED_PAIOS_WORLD_ID), "+ 0xd0", "local_res9 != 0", "+ 0xf0", f",{quest_hex},0,0)")):
			reasons.append("script:world_reset")
		paios_tokens = (f"FUN_180caf740({quest_hex}", "0x280f", f"FUN_180caf3c0({quest_hex}", "+ 0x100", "+ 0x110", f",{quest_hex},3000)", "0x10f54d")
		if not has_function_evidence(sources[896], quest_hex, paios_tokens):
			reasons.append("script:paios_turn_in")
		if not has_function_evidence(sources[877], quest_hex, ("param_1[3] != 0", "+ 0x4b8")):
			reasons.append("script:quest_state_notification")
		if reasons:
			skipped[str(quest_id)] = reasons
			continue
		result[quest_id] = {
			"source": "compiled_script_action_progress", "start_type": "TALK", "start": "Lition", "end": "Paios",
			"reset_world_id": COMPILED_PAIOS_WORLD_ID,
			"steps": [{"type": "ACTION", "actions": ["IDYun_Column_Q30504"]}],
		}
	return result, skipped


def compiled_housing_flower_visits(retail_quest: Path, retail_npcs: Path, script_root: Path, quest_data: Path,
		enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot() if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	names = {name for data in COMPILED_HOUSING_FLOWERS.values() for name in (data[4], data[6], data[8])}
	exact, aliases = npc_indexes(retail_npcs, names, set())
	source = "\n".join(
		path.read_text(errors="ignore") for number in COMPILED_HOUSING_FLOWER_SOURCE_NUMBERS
		if (path := script_root / f"fun_{number:03d}.cpp").is_file()
	)
	blocks = re.findall(r"(?ms)^// @[^\n]+\n.*?(?=^// @|\Z)", source)
	for quest_id, (quest_hex, retail_race, base_race, prerequisite, start_name, start_ids, middle_name, middle_id, end_name, end_id) in COMPILED_HOUSING_FLOWERS.items():
		if quest_id not in enabled_ids:
			continue
		reasons: list[str] = []
		retail = retail_nodes.get(quest_id)
		if retail is None:
			reasons.append("quest_xml_missing")
		else:
			expected = {
				"max_repeat_count": "1", "minlevel_permitted": "21", "finished_quest_cond1": f"Q{prerequisite}",
				"cannot_share": "1", "reward_exp1": "12951", "reward_gold1": "0", "race_permitted": retail_race,
			}
			if any(retail.get(field) != value for field, value in expected.items()):
				reasons.append("quest_xml_fields")
			if any(re.fullmatch(r"(?:quest_work|collect|check|drop|reward|selectable_reward)_item.*", field) for field in retail):
				reasons.append("quest_xml_items")
		base = base_nodes.get(quest_id)
		if base is None:
			reasons.append("quest_data_missing")
		else:
			finished = [int(node.attrib["quest_id"]) for node in base.findall("./start_conditions/finished")]
			rewards = base.find("rewards")
			if base.attrib.get("race_permitted") != base_race or base.attrib.get("minlevel_permitted") != "21" or base.attrib.get("max_repeat_count") != "1" or finished != [prerequisite]:
				reasons.append("quest_data_fields")
			if rewards is None or rewards.attrib != {"exp": "12951"} or list(rewards):
				reasons.append("quest_data_rewards")
		if aliases.get(start_name.casefold()) != start_ids or exact.get(middle_name.casefold()) != (middle_id,) or exact.get(end_name.casefold()) != (end_id,):
			reasons.append("npc_resolution")
		for name in (start_name, middle_name, end_name):
			if not has_function_evidence(source, name, ("FUN_180cb5920", quest_hex)):
				reasons.append(f"script:npc:{name}")
		for tokens in (
			("FUN_180cb3070", quest_hex, ",0,0xffffffff,0)"),
			("FUN_180cb3070", quest_hex, ",3,0,0)"),
			("FUN_180cb3070", quest_hex, ",3,1,0)"),
			("FUN_180cb3070", quest_hex, ",4,0xffffffff,0)"),
			("FUN_180cab520", quest_hex), ("+ 0x1a8", quest_hex), ("+ 0x1b8", quest_hex),
		):
			if not any(all(token in block for token in tokens) for block in blocks):
				reasons.append(f"script:{tokens[0]}")
		if sum(quest_hex in block and "FUN_180cabb10" in block and "FUN_180cacb30" in block for block in blocks) != 2:
			reasons.append("script:talk_callbacks")
		if reasons:
			skipped[str(quest_id)] = sorted(set(reasons))
			continue
		result[quest_id] = {
			"source": "compiled_script_housing_talk", "start_type": "TALK", "start": start_name, "end": end_name,
			"steps": [{"type": "TALK", "names": [middle_name], "actions": []}],
		}
	return result, skipped


def compiled_scorched_tree_actions(retail_quest: Path, retail_npcs: Path, retail_items: Path, script_root: Path,
		quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot() if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	names = {data[3] for data in COMPILED_SCORCHED_TREES.values()} | {name for name, _ in COMPILED_SCORCHED_TREE_ACTIONS}
	exact, aliases = npc_indexes(retail_npcs, names, set())
	item_names = {
		name for data in COMPILED_SCORCHED_TREES.values() for name, _ in data[5]
	} | {name for name, _ in COMPILED_SCORCHED_TREE_REWARDS}
	items = item_ids(retail_items, item_names)
	source = "\n".join(
		path.read_text(errors="ignore") for number in COMPILED_SCORCHED_TREE_SOURCE_NUMBERS
		if (path := script_root / f"fun_{number:03d}.cpp").is_file()
	)
	blocks = re.findall(r"(?ms)^// @[^\n]+\n.*?(?=^// @|\Z)", source)
	for quest_id, (quest_hex, retail_race, base_race, start_name, start_id, work_items) in COMPILED_SCORCHED_TREES.items():
		if quest_id not in enabled_ids:
			continue
		reasons: list[str] = []
		retail = retail_nodes.get(quest_id)
		if retail is None:
			reasons.append("quest_xml_missing")
		else:
			expected = {
				"max_repeat_count": "1", "minlevel_permitted": "65", "cannot_share": "1",
				"reward_exp1": "3446553", "reward_gold1": "150660", "race_permitted": retail_race,
			}
			expected.update({f"quest_work_item{index}": f"{name} 1" for index, (name, _) in enumerate(work_items, 1)})
			expected.update({f"check_item1_{index}": f"{name} 1" for index, (name, _) in enumerate(work_items, 1)})
			expected.update({f"selectable_reward_item1_{index}": f"{name} 2" for index, (name, _) in enumerate(COMPILED_SCORCHED_TREE_REWARDS, 1)})
			if any(retail.get(field) != value for field, value in expected.items()):
				reasons.append("quest_xml_fields")
			item_fields = {field for field in retail if re.fullmatch(r"(?:quest_work|check|selectable_reward)_item.*", field)}
			if item_fields != set(expected) - {"max_repeat_count", "minlevel_permitted", "cannot_share", "reward_exp1", "reward_gold1", "race_permitted"}:
				reasons.append("quest_xml_items")
		base = base_nodes.get(quest_id)
		if base is None:
			reasons.append("quest_data_missing")
		else:
			rewards = base.find("rewards")
			reward_items = [] if rewards is None else [
				(int(node.attrib["item_id"]), int(node.attrib["count"])) for node in rewards.findall("selectable_reward_item")
			]
			base_work_items = [
				(int(node.attrib["item_id"]), int(node.attrib["count"])) for node in base.findall("./quest_work_items/quest_work_item")
			]
			if base.attrib.get("race_permitted") != base_race or base.attrib.get("minlevel_permitted") != "65" or base.attrib.get("max_repeat_count") != "1" or base.attrib.get("cannot_share") != "true":
				reasons.append("quest_data_fields")
			if rewards is None or rewards.attrib != {"gold": "150660", "exp": "3446553"} or reward_items != [(item_id, 2) for _, item_id in COMPILED_SCORCHED_TREE_REWARDS]:
				reasons.append("quest_data_rewards")
			if base_work_items != [(item_id, 1) for _, item_id in work_items]:
				reasons.append("quest_data_work_items")
		if exact.get(start_name.casefold()) != (start_id,) or any(exact.get(name.casefold()) != (npc_id,) for name, npc_id in COMPILED_SCORCHED_TREE_ACTIONS):
			reasons.append("npc_resolution")
		if any(items.get(name.casefold()) != (item_id,) for name, item_id in (*work_items, *COMPILED_SCORCHED_TREE_REWARDS)):
			reasons.append("item_resolution")
		for name in (start_name, *(name for name, _ in COMPILED_SCORCHED_TREE_ACTIONS)):
			if not has_function_evidence(source, name, ("FUN_180cb5920", quest_hex)):
				reasons.append(f"script:npc:{name}")
		for tokens in (
			("FUN_180cb3070", quest_hex, ",0,0xffffffff,0)"),
			("FUN_180cb3070", quest_hex, ",3,0,0)"),
			("FUN_180cb3070", quest_hex, ",3,1,0)"),
			("FUN_180cb3070", quest_hex, ",3,2,0)"),
			("FUN_180cb3070", quest_hex, ",3,3,0)"),
			("FUN_180cb3070", quest_hex, ",4,0xffffffff,0)"),
			("FUN_180cab520", quest_hex),
		):
			if not any(all(token in block for token in tokens) for block in blocks):
				reasons.append(f"script:{tokens[0]}")
		quest_blocks = [block for block in blocks if quest_hex in block]
		expected_calls = {"FUN_180cb5920": 4, "FUN_180cb3070": 6, "FUN_180cab520": 1, "FUN_180cabb10": 4}
		if len(quest_blocks) != 15 or any(sum(call in block for call in expected_calls) != 1 for block in quest_blocks):
			reasons.append("script:function_blocks")
		if any(sum(call in block for block in quest_blocks) != count for call, count in expected_calls.items()):
			reasons.append("script:function_counts")
		if reasons:
			skipped[str(quest_id)] = sorted(set(reasons))
			continue
		result[quest_id] = {
			"source": "compiled_script_scorched_tree", "start_type": "TALK", "start": start_name, "end": start_name,
			"steps": [{"type": "ACTION", "actions": [name]} for name, _ in COMPILED_SCORCHED_TREE_ACTIONS],
		}
	return result, skipped


def compiled_kaldor_arrivals(retail_quest: Path, retail_npcs: Path, retail_items: Path, script_root: Path,
		quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot() if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	names = {name for data in COMPILED_KALDOR_ARRIVALS.values() for name in (data[3], data[5], data[7])}
	exact, aliases = npc_indexes(retail_npcs, names, set())
	item_names = {data[9] for data in COMPILED_KALDOR_ARRIVALS.values()} | {name for name, _ in COMPILED_KALDOR_ARRIVAL_REWARDS}
	items = item_ids(retail_items, item_names)
	source = "\n".join(
		path.read_text(errors="ignore") for number in COMPILED_KALDOR_ARRIVAL_SOURCE_NUMBERS
		if (path := script_root / f"fun_{number:03d}.cpp").is_file()
	)
	blocks = re.findall(r"(?ms)^// @[^\n]+\n.*?(?=^// @|\Z)", source)
	for quest_id, (quest_hex, retail_race, base_race, start_name, start_id, middle_name, middle_id, end_name, end_id, work_item_name, work_item_id) in COMPILED_KALDOR_ARRIVALS.items():
		if quest_id not in enabled_ids:
			continue
		reasons: list[str] = []
		retail = retail_nodes.get(quest_id)
		expected = {
			"max_repeat_count": "1", "minlevel_permitted": "65", "cannot_share": "1",
			"quest_work_item1": f"{work_item_name} 1", "reward_exp1": "3446553", "reward_gold1": "150660",
			"selectable_reward_item1_1": f"{COMPILED_KALDOR_ARRIVAL_REWARDS[0][0]} 1",
			"selectable_reward_item1_2": f"{COMPILED_KALDOR_ARRIVAL_REWARDS[1][0]} 1", "race_permitted": retail_race,
		}
		if retail is None:
			reasons.append("quest_xml_missing")
		elif any(retail.get(field) != value for field, value in expected.items()):
			reasons.append("quest_xml_fields")
		elif {field for field in retail if re.fullmatch(r"(?:quest_work|check|selectable_reward)_item.*", field)} != {"quest_work_item1", "selectable_reward_item1_1", "selectable_reward_item1_2"}:
			reasons.append("quest_xml_items")
		base = base_nodes.get(quest_id)
		if base is None:
			reasons.append("quest_data_missing")
		else:
			rewards = base.find("rewards")
			reward_items = [] if rewards is None else [(int(node.attrib["item_id"]), int(node.attrib["count"])) for node in rewards.findall("selectable_reward_item")]
			work_items = [(int(node.attrib["item_id"]), int(node.attrib["count"])) for node in base.findall("./quest_work_items/quest_work_item")]
			if base.attrib.get("race_permitted") != base_race or base.attrib.get("minlevel_permitted") != "65" or base.attrib.get("max_repeat_count") != "1" or base.attrib.get("cannot_share") != "true":
				reasons.append("quest_data_fields")
			if rewards is None or rewards.attrib != {"gold": "150660", "exp": "3446553"} or reward_items != [(item_id, 1) for _, item_id in COMPILED_KALDOR_ARRIVAL_REWARDS]:
				reasons.append("quest_data_rewards")
			if work_items != [(work_item_id, 1)]:
				reasons.append("quest_data_work_items")
		if exact.get(start_name.casefold()) != (start_id,) or exact.get(middle_name.casefold()) != (middle_id,) or exact.get(end_name.casefold()) != (end_id,):
			reasons.append("npc_resolution")
		if items.get(work_item_name.casefold()) != (work_item_id,) or any(items.get(name.casefold()) != (item_id,) for name, item_id in COMPILED_KALDOR_ARRIVAL_REWARDS):
			reasons.append("item_resolution")
		for name in (start_name, middle_name, end_name):
			if not has_function_evidence(source, name, ("FUN_180cb5920", quest_hex)):
				reasons.append(f"script:npc:{name}")
		for tokens in (
			("FUN_180cb3070", quest_hex, ",0,0xffffffff,0)"), ("FUN_180cb3070", quest_hex, ",3,0,0)"),
			("FUN_180cb3070", quest_hex, ",3,1,0)"), ("FUN_180cb3070", quest_hex, ",3,2,0)"),
			("FUN_180cb3070", quest_hex, ",4,0xffffffff,0)"), ("FUN_180cab520", quest_hex),
		):
			if not any(all(token in block for token in tokens) for block in blocks):
				reasons.append(f"script:{tokens[0]}")
		quest_blocks = [block for block in blocks if quest_hex in block]
		expected_calls = {"FUN_180cb5920": 3, "FUN_180cb3070": 5, "FUN_180cab520": 1, "FUN_180cabb10": 3, "FUN_180caca90": 1}
		if len(quest_blocks) != 13 or any(sum(call in block for call in expected_calls) != 1 for block in quest_blocks):
			reasons.append("script:function_blocks")
		if any(sum(call in block for block in quest_blocks) != count for call, count in expected_calls.items()):
			reasons.append("script:function_counts")
		if reasons:
			skipped[str(quest_id)] = sorted(set(reasons))
			continue
		result[quest_id] = {
			"source": "compiled_script_kaldor_arrival", "start_type": "TALK", "start": start_name, "end": end_name,
			"steps": [{"type": "TALK", "names": [middle_name], "actions": []}],
		}
	return result, skipped


def compiled_coalescence_completes(retail_quest: Path, retail_npcs: Path, script_root: Path, quest_data: Path,
		enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_names = {data[3] for data in COMPILED_COALESCENCE_COMPLETES.values()}
	npc_index, _ = npc_indexes(retail_npcs, npc_names, set())
	sources = {
		number: (path.read_text(encoding="utf-8") if (path := script_root / f"fun_{number:03d}.cpp").is_file() else "")
		for number in COMPILED_COALESCENCE_SOURCE_NUMBERS
	}
	for quest_id, (quest_hex, retail_race, base_race, npc_name, npc_id, prerequisite) in COMPILED_COALESCENCE_COMPLETES.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		reasons = []
		expected_fields = {
			"name": f"Q{quest_id}", "max_repeat_count": "1", "minlevel_permitted": "66",
			"finished_quest_cond1": f"Q{prerequisite}", "reward_exp1": "26524800", "reward_gold1": "0",
			"race_permitted": retail_race,
		}
		for field, expected in expected_fields.items():
			if fields.get(field) != expected:
				reasons.append(f"quest.xml:{field}")
		base = base_nodes.get(quest_id)
		finished = None if base is None else base.find("./start_conditions/finished")
		rewards = None if base is None else base.find("rewards")
		reward_item = None if rewards is None else rewards.find("reward_item")
		if (base is None or base.attrib.get("race_permitted") != base_race
				or base.attrib.get("minlevel_permitted") != "66" or base.attrib.get("max_repeat_count") != "1"
				or finished is None or finished.attrib != {"quest_id": str(prerequisite)}
				or rewards is None or rewards.attrib != {"exp": "26524800"}
				or reward_item is None or reward_item.attrib != {"item_id": "165060002", "count": "1"}):
			reasons.append("quest_data.xml:race_prerequisite_and_reward")
		if npc_index.get(npc_name.casefold()) != (npc_id,):
			reasons.append(f"npcs.xml:{npc_name}")
		light = retail_race == "pc_light"
		if not has_function_evidence(sources[623 if light else 614], quest_hex, (f'L"{npc_name}"', quest_hex)):
			reasons.append("script:npc_binding")
		if not has_function_evidence(sources[692 if light else 690], quest_hex, (f",{quest_hex},0,0xffffffff,0)",)):
			reasons.append("script:start_state")
		if not has_function_evidence(sources[698 if light else 696], quest_hex, (f",{quest_hex},4,0xffffffff,0)",)):
			reasons.append("script:reward_state")
		if not has_function_evidence(sources[882 if light else 880], quest_hex, ("0x129a", quest_hex, "20000", "+ 0x5d8")):
			reasons.append("script:start_dialog")
		if not has_function_evidence(sources[887 if light else 886], quest_hex, ("0x2712", quest_hex, "0x3f1", "+ 0x1b0")):
			reasons.append("script:reward_dialog")
		if not has_function_evidence(sources[874], quest_hex, ("+ 0xd0", "+ 0x100", quest_hex)):
			reasons.append("script:completion_cleanup")
		if not has_function_evidence(sources[905 if light else 904], quest_hex, ("+ 0xd8", "0x3eb", quest_hex)):
			reasons.append("script:start_trigger")
		if not has_function_evidence(sources[908 if light else 907], quest_hex, ("0x3ec", quest_hex)):
			reasons.append("script:reward_trigger")
		if reasons:
			skipped[str(quest_id)] = reasons
			continue
		result[quest_id] = {
			"source": "compiled_script_coalescence_complete", "start_type": "TALK",
			"start": npc_name, "end": npc_name, "steps": [], "complete_on_start": True,
		}
	return result, skipped


def compiled_bastion_movies(retail_quest: Path, retail_npcs: Path, script_root: Path, quest_data: Path,
		enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_names = {name for data in COMPILED_BASTION_MOVIES.values() for name in (data[3], data[5])}
	npc_index, _ = npc_indexes(retail_npcs, npc_names, set())
	sources = {
		number: (path.read_text(encoding="utf-8") if (path := script_root / f"fun_{number:03d}.cpp").is_file() else "")
		for number in COMPILED_BASTION_MOVIE_SOURCE_NUMBERS
	}
	for quest_id, (quest_hex, retail_race, base_race, start_name, start_id, talk_name, talk_id, prerequisite,
			trigger_callback) in COMPILED_BASTION_MOVIES.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		reasons = []
		expected_fields = {
			"name": f"Q{quest_id}", "max_repeat_count": "1", "minlevel_permitted": "65",
			"finished_quest_cond1": f"Q{prerequisite}", "reward_exp1": "1723277", "reward_gold1": "0",
			"race_permitted": retail_race,
		}
		for field, expected in expected_fields.items():
			if fields.get(field) != expected:
				reasons.append(f"quest.xml:{field}")
		base = base_nodes.get(quest_id)
		finished = None if base is None else base.find("./start_conditions/finished")
		rewards = None if base is None else base.find("rewards")
		if (base is None or base.attrib.get("race_permitted") != base_race
				or base.attrib.get("minlevel_permitted") != "65" or base.attrib.get("max_repeat_count") != "1"
				or base.attrib.get("cannot_share") != "true"
				or finished is None or finished.attrib != {"quest_id": str(prerequisite)}
				or rewards is None or rewards.attrib != {"exp": "1723277"} or len(rewards) != 0):
			reasons.append("quest_data.xml:race_prerequisite_and_reward")
		for npc_name, npc_id in ((start_name, start_id), (talk_name, talk_id)):
			if npc_index.get(npc_name.casefold()) != (npc_id,):
				reasons.append(f"npcs.xml:{npc_name}")
			if not has_function_evidence(sources[620], quest_hex, (f'L"{npc_name}"', quest_hex)):
				reasons.append(f"script:npc_binding:{npc_name}")
		for phase, number, suffix in ((0, 691, "0,0xffffffff,0"), (3, 703, "3,0,0"), (4, 697, "4,0xffffffff,0")):
			if not has_function_evidence(sources[number], quest_hex, (f",{quest_hex},{suffix})",)):
				reasons.append(f"script:phase_{phase}")
		if not has_function_evidence(sources[442], trigger_callback, ("0x1e", trigger_callback)):
			reasons.append("script:trigger_binding")
		if not has_function_evidence(sources[806], trigger_callback, ("+ 0x1a8", quest_hex)):
			reasons.append("script:start_trigger")
		if not has_function_evidence(sources[882], quest_hex, (f"FUN_180caf640({quest_hex}", "20000", "+ 0xd8", f"FUN_180caf350({quest_hex}")):
			reasons.append("script:start_dialog")
		if not has_function_evidence(sources[892], quest_hex, (f"FUN_180caf740({quest_hex}", "10000", "+ 0x1b8", "0x1c", "+ 0x100")):
			reasons.append("script:movie_and_progress")
		if not has_function_evidence(sources[887], quest_hex, (f"FUN_180caf6c0({quest_hex}", f"FUN_180caf350({quest_hex}")):
			reasons.append("script:reward_dialog")
		if reasons:
			skipped[str(quest_id)] = reasons
			continue
		result[quest_id] = {
			"source": "compiled_script_bastion_movie", "start_type": "TALK", "start": start_name, "end": start_name,
			"movie": 28, "steps": [{"type": "TALK", "names": [talk_name], "actions": []}],
		}
	return result, skipped


def compiled_simple_talks(retail_quest: Path, retail_talk: Path, retail_use_item: Path, retail_npcs: Path, script_root: Path, quest_data: Path,
		enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	talk_nodes = {
		int(node.attrib["id"]): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_talk).getroot()
		if node.attrib.get("id", "").isdigit()
	}
	use_item_nodes = {
		int(node.attrib["id"]): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_use_item).getroot()
		if node.attrib.get("id", "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_names = {name for data in COMPILED_SIMPLE_TALKS.values() for name in (data["end"], *(name for name, _ in data["talks"]))}
	npc_names.update(data["start"] for data in COMPILED_SIMPLE_TALKS.values() if data.get("start_type", "TALK") == "TALK")
	npc_index, _ = npc_indexes(retail_npcs, npc_names, set())
	sources = {
		number: (path.read_text(encoding="utf-8") if (path := script_root / f"fun_{number:03d}.cpp").is_file() else "")
		for number in COMPILED_SIMPLE_TALK_SOURCE_NUMBERS
	}
	for quest_id, data in COMPILED_SIMPLE_TALKS.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		reasons = [f"quest.xml:{field}" for field, expected in data["retail"].items() if fields.get(field) != expected]
		definition_name, definition_nodes = ("Quest_SimpleUseItem.xml", use_item_nodes) if data.get("definition") == "use_item" else ("Quest_SimpleTalk.xml", talk_nodes)
		reasons.extend(f"{definition_name}:{field}" for field, expected in data.get("talk", {}).items() if definition_nodes.get(quest_id, {}).get(field) != expected)
		base = base_nodes.get(quest_id)
		rewards = None if base is None else base.find("rewards")
		actual_reward_items = () if rewards is None else tuple((node.attrib.get("item_id"), node.attrib.get("count")) for node in rewards.findall("reward_item"))
		actual_selectable_reward_items = () if rewards is None else tuple((node.attrib.get("item_id"), node.attrib.get("count")) for node in rewards.findall("selectable_reward_item"))
		actual_work_items = () if base is None or base.find("quest_work_items") is None else tuple((node.attrib.get("item_id"), node.attrib.get("count")) for node in base.findall("./quest_work_items/quest_work_item"))
		actual_finished_quests = () if base is None else tuple(node.attrib.get("quest_id") for node in base.findall("./start_conditions/finished"))
		if (base is None or any(base.attrib.get(field) != expected for field, expected in data["base"].items() if field not in {"rewards", "reward_items", "selectable_reward_items", "work_items", "finished_quests"})
				or rewards is None or rewards.attrib != data["base"]["rewards"] or actual_reward_items != data["base"]["reward_items"]
				or "selectable_reward_items" in data["base"] and actual_selectable_reward_items != data["base"]["selectable_reward_items"]
				or actual_work_items != data["base"]["work_items"]
				or "finished_quests" in data["base"] and actual_finished_quests != data["base"]["finished_quests"]):
			reasons.append("quest_data.xml:task_and_rewards")
		npc_evidence = [(data["end"], data["end_id"]), *data["talks"]]
		if data.get("start_type", "TALK") == "TALK":
			npc_evidence.insert(0, (data["start"], data["start_id"]))
		for name, expected_id in npc_evidence:
			if npc_index.get(name.casefold()) != (expected_id,):
				reasons.append(f"npcs.xml:{name}")
		for number, tokens in data["evidence"]:
			if not has_function_evidence(sources[number], data["quest_hex"], tokens):
				reasons.append(f"script:fun_{number:03d}")
		if reasons:
			skipped[str(quest_id)] = sorted(set(reasons))
			continue
		if data.get("data_driven"):
			result[quest_id] = {
				"kind": "data_driven_simple", "source": "compiled_script_simple_talk", "start_type": data.get("start_type", "TALK"),
				"start": data["start"], "end": data["end"], "start_give_item": data.get("start_give_item"),
				"start_dialog_id": data.get("start_dialog_id"),
				"movie": data.get("movie"),
				"steps": data.get("steps") or [{"type": "TALK", "names": [data["talks"][0][0]], "give_item": data.get("step_give_item"), "remove_item": data.get("step_remove_item")}],
			}
		elif data.get("stages"):
			# 并行击杀目标：monster_hunt 模板按 var 分槽独立计数，不能压成线性 step 链。
			result[quest_id] = {
				"kind": "monster_hunt", "source": "compiled_script_simple_talk",
				"start": data["start"], "end": data["end"], "stages": [(list(names), int(count)) for names, count in data["stages"]],
			}
		else:
			result[quest_id] = {
				"kind": data.get("kind") or ("report_to_many" if data["talks"] else "report_to"), "source": "compiled_script_simple_talk",
				"start": data["start"], "end": data["end"], "talks": [name for name, _ in data["talks"]],
				"movie": data.get("movie"),
			}
	return result, skipped


def compiled_surama_hunts(retail_quest: Path, retail_npcs: Path, retail_items: Path, script_root: Path, quest_data: Path,
		enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_index, _ = npc_indexes(retail_npcs, set(COMPILED_SURAMA_NPCS), set())
	coin_ids = item_ids(retail_items, {"tiamat_coin_01"})
	sources = {
		number: (path.read_text(encoding="utf-8") if (path := script_root / f"fun_{number:03d}.cpp").is_file() else "")
		for number in COMPILED_SURAMA_SOURCE_NUMBERS
	}
	for quest_id, (quest_hex, race, base_race) in COMPILED_SURAMA_HUNTS.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		expected_retail = {"name": f"Q{quest_id}", "max_repeat_count": "1", "minlevel_permitted": "57", "reward_exp1": "7086913", "reward_gold1": "0", "reward_item1_1": "tiamat_coin_01 1", "race_permitted": race}
		reasons = [f"quest.xml:{field}" for field, expected in expected_retail.items() if fields.get(field) != expected]
		base = base_nodes.get(quest_id)
		rewards = None if base is None else base.find("rewards")
		if (base is None or base.attrib.get("minlevel_permitted") != "57" or base.attrib.get("max_repeat_count") != "1"
				or base.attrib.get("race_permitted") != base_race or base.attrib.get("category") != "QUEST"
				or rewards is None or rewards.attrib != {"exp": "7086913"}
				or tuple((node.attrib.get("item_id"), node.attrib.get("count")) for node in rewards.findall("reward_item")) != (("186000201", "1"),)):
			reasons.append("quest_data.xml:task_and_rewards")
		for name, npc_id in COMPILED_SURAMA_NPCS.items():
			if npc_index.get(name.casefold()) != (npc_id,):
				reasons.append(f"npcs.xml:{name}")
		if coin_ids.get("tiamat_coin_01") != (186000201,):
			reasons.append("Items.xml:tiamat_coin_01")
		for name in COMPILED_SURAMA_NPCS:
			if not has_function_evidence(sources[618], quest_hex, (f'L"{name}"',)):
				reasons.append(f"script:npc_binding:{name}")
		if not has_function_evidence(sources[691], quest_hex, (f",{quest_hex},0,0xffffffff,0)",)):
			reasons.append("script:start_phase")
		if not has_function_evidence(sources[697], quest_hex, (f",{quest_hex},4,0xffffffff,0)",)):
			reasons.append("script:reward_phase")
		if not has_function_evidence(sources[878], quest_hex, ("0xc36a9", "0xc36aa", "0xc36ab", "local_res11 < 5", "local_res11 + 1", f",{quest_hex},0,5,0,1")):
			reasons.append("script:kill_targets_and_count")
		if not has_function_evidence(sources[866], quest_hex, ("*(int *)(lVar3 + 1) == 5", f",{quest_hex},0,0)", f",{quest_hex},5,5,1,1")):
			reasons.append("script:completion")
		if not has_function_evidence(sources[881], quest_hex, (f"FUN_180caf640({quest_hex}", "20000", "+ 0xd8", f"FUN_180caf350({quest_hex}")):
			reasons.append("script:start_dialog")
		if not has_function_evidence(sources[887], quest_hex, (f"FUN_180caf6c0({quest_hex}", f"FUN_180caf350({quest_hex}")):
			reasons.append("script:reward_dialog")
		if reasons:
			skipped[str(quest_id)] = sorted(set(reasons))
			continue
		result[quest_id] = {
			"source": "compiled_script_surama_hunt", "start": "IDTiamat_Surama_1", "end": "IDTiamat_Murugan_4",
			"stages": [(["IDTiamat_Drakan_Surama_1", "IDTiamat_Drakan_Surama_2", "IDTiamat_Drakan_Surama_3"], 5)],
		}
	return result, skipped


def compiled_dredgion_captain_hunts(retail_quest: Path, retail_npcs: Path, retail_items: Path, script_root: Path,
		quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_index, _ = npc_indexes(retail_npcs, set(COMPILED_DREDGION_CAPTAIN_NPCS), set())
	item_names = {name for data in COMPILED_DREDGION_CAPTAIN_HUNTS.values() for field in ("reward",) for key in data["retail"] if key.startswith("reward_item") for name in [data["retail"][key].split()[0]]}
	retail_item_ids = item_ids(retail_items, item_names)
	sources = {
		number: (path.read_text(encoding="utf-8") if (path := script_root / f"fun_{number:03d}.cpp").is_file() else "")
		for number in COMPILED_DREDGION_CAPTAIN_SOURCE_NUMBERS
	}
	for quest_id, data in COMPILED_DREDGION_CAPTAIN_HUNTS.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		reasons = [f"quest.xml:{field}" for field, expected in data["retail"].items() if fields.get(field) != expected]
		base = base_nodes.get(quest_id)
		rewards = None if base is None else base.find("rewards")
		if (base is None or any(base.attrib.get(field) != expected for field, expected in data["base"].items() if field not in {"rewards", "reward_items"})
				or rewards is None or rewards.attrib != data["base"]["rewards"]
				or tuple((node.attrib.get("item_id"), node.attrib.get("count")) for node in rewards.findall("reward_item")) != data["base"]["reward_items"]):
			reasons.append("quest_data.xml:task_and_rewards")
		for name, expected_id in COMPILED_DREDGION_CAPTAIN_NPCS.items():
			if npc_index.get(name.casefold()) != (expected_id,):
				reasons.append(f"npcs.xml:{name}")
		for name in item_names:
			if not retail_item_ids.get(name.casefold()):
				reasons.append(f"Items.xml:{name}")
		for number, tokens in data["evidence"]:
			if not has_function_evidence(sources[number], data["quest_hex"], tokens):
				reasons.append(f"script:fun_{number:03d}")
		if reasons:
			skipped[str(quest_id)] = sorted(set(reasons))
			continue
		result[quest_id] = {
			"source": "compiled_script_dredgion_captain_hunt", "kind": "data_driven_complex",
			"start_type": "TALK", "start": data["start"], "end": data["start"],
			"steps": [
				{"type": "TALK", "names": [data["talk_npc"]]},
				{"type": "ACTION", "actions": [data["object"]]},
				{"type": "HUNT", "names": [data["target"]], "amount": 1},
			],
		}
	return result, skipped


def compiled_arena_item_plays(retail_quest: Path, retail_item_play: Path, retail_npcs: Path, retail_items: Path,
		script_root: Path, quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	item_play_nodes = {
		int(node.attrib["id"]): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_item_play).getroot()
		if node.attrib.get("id", "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_names = {name for data in COMPILED_ARENA_ITEM_PLAYS.values() for name, _ in (data["start"], *data["talks"])}
	item_names = {name for data in COMPILED_ARENA_ITEM_PLAYS.values() for name, _ in data["items"]}
	npc_index, _ = npc_indexes(retail_npcs, npc_names, set())
	item_index = item_ids(retail_items, item_names | {"coin_arena_pvp_01"})
	sources = {
		number: (path.read_text(encoding="utf-8") if (path := script_root / f"fun_{number:03d}.cpp").is_file() else "")
		for number in COMPILED_ARENA_ITEM_PLAY_SOURCE_NUMBERS
	}
	for quest_id, data in COMPILED_ARENA_ITEM_PLAYS.items():
		if quest_id not in enabled_ids:
			continue
		quest_hex = data["quest_hex"]
		start_name, start_id = data["start"]
		(first_talk, first_talk_id), (end_name, end_id) = data["talks"]
		(first_item, first_item_id), (second_item, second_item_id) = data["items"]
		prerequisite = data["prerequisite"]
		expected_retail = {
			"name": f"Q{quest_id}", "max_repeat_count": "1", "minlevel_permitted": "51",
			"finished_quest_cond1": f"Q{prerequisite}", "cannot_share": "1",
			"quest_work_item1": f"{first_item} 1", "quest_work_item2": f"{second_item} 1",
			"reward_exp1": "2010386", "reward_gold1": "92940", "reward_item1_1": "coin_arena_pvp_01 100",
			"race_permitted": data["retail_race"],
		}
		fields = retail_nodes.get(quest_id, {})
		reasons = [f"quest.xml:{field}" for field, expected in expected_retail.items() if fields.get(field) != expected]
		expected_item_play = {
			"acquired_npc_name": start_name, "give_item": f"ITEM_{first_item.upper()} 1", "talk_npc1": first_talk,
			"talk_npc2": end_name, "give_item2": f"ITEM_{second_item.upper()} 1",
			"remove_item2": f"ITEM_{first_item.upper()} 1", "use_item_name": f"ITEM_{second_item.upper()} 1",
			"reward_npc_name": end_name,
		}
		item_play_fields = item_play_nodes.get(quest_id, {})
		if set(item_play_fields) != set(expected_item_play) | {"dev_name"}:
			reasons.append("Quest_SimpleItemPlay.xml:shape")
		reasons.extend(f"Quest_SimpleItemPlay.xml:{field}" for field, expected in expected_item_play.items() if item_play_fields.get(field) != expected)
		base = base_nodes.get(quest_id)
		rewards = None if base is None else base.find("rewards")
		finished = None if base is None else base.find("./start_conditions/finished")
		work_items = () if base is None else tuple((node.attrib.get("item_id"), node.attrib.get("count")) for node in base.findall("./quest_work_items/quest_work_item"))
		if (base is None or base.attrib.get("minlevel_permitted") != "51" or base.attrib.get("max_repeat_count") != "1"
				or base.attrib.get("cannot_share") != "true" or base.attrib.get("race_permitted") != data["base_race"]
				or base.attrib.get("category") != "SIGNIFICANT" or finished is None or finished.attrib != {"quest_id": str(prerequisite)}
				or rewards is None or rewards.attrib != {"gold": "92940", "exp": "2010386"}
				or tuple((node.attrib.get("item_id"), node.attrib.get("count")) for node in rewards.findall("reward_item")) != (("186000137", "100"),)
				or work_items != ((str(first_item_id), "1"), (str(second_item_id), "1"))):
			reasons.append("quest_data.xml:task_items_and_rewards")
		for name, expected_id in ((start_name, start_id), (first_talk, first_talk_id), (end_name, end_id)):
			if npc_index.get(name.casefold()) != (expected_id,):
				reasons.append(f"npcs.xml:{name}")
			number = 336 if name in {"Inggril", "Inggness"} else 337
			if not has_function_evidence(sources[number], quest_hex, (f'L"{name}"',)):
				reasons.append(f"script:npc_binding:{name}")
		for name, expected_id in ((first_item, first_item_id), (second_item, second_item_id), ("coin_arena_pvp_01", 186000137)):
			if item_index.get(name.casefold()) != (expected_id,):
				reasons.append(f"Items.xml:{name}")
		start_phase_source = 339 if quest_id == 18213 else 340
		for source_number, token, reason in (
			(start_phase_source, f",{quest_hex},0,0xffffffff,0)", "start_phase"),
			(339, f",{quest_hex},3,0,0)", "talk_phase_0"),
			(339, f",{quest_hex},3,1,0)", "talk_phase_1"),
			(339, f",{quest_hex},3,3,0)", "reward_ready_phase"),
			(340, f",{quest_hex},4,0xffffffff,0)", "reward_phase"),
		):
			if not has_function_evidence(sources[source_number], quest_hex, (token,)):
				reasons.append(f"script:{reason}")
		start_message, dialog_message, item_message = data["messages"]
		if not has_function_evidence(sources[338], quest_hex, (f"5,{item_message},{data['callback']}",)):
			reasons.append("script:item_trigger_binding")
		if not has_function_evidence(sources[802], quest_hex, ("local_res9 == 2", f"{quest_hex},3,0", f"{quest_hex},2,3,0,1,0,0,0,0")):
			reasons.append("script:item_completion")
		if not has_function_evidence(sources[803], quest_hex, (f"FUN_180cab520({quest_hex}", dialog_message)):
			reasons.append("script:start_dialog")
		if not has_function_evidence(sources[803], quest_hex, (f"FUN_180caca90({quest_hex}", dialog_message)):
			reasons.append("script:reward_dialog")
		for dialog_data in data["dialog_data"]:
			if not has_function_evidence(sources[803], quest_hex, (f"FUN_180cabb10({quest_hex}", dialog_data)):
				reasons.append(f"script:talk_dialog:{dialog_data}")
		if not has_function_evidence(sources[872], quest_hex, ("+ 0x1a8", start_message)):
			reasons.append("script:start_item_grant")
		if reasons:
			skipped[str(quest_id)] = sorted(set(reasons))
			continue
		result[quest_id] = {
			"source": "compiled_script_arena_item_play", "start_type": "TALK", "start": start_name, "end": end_name,
			"start_give_item": (first_item, 1),
			"steps": [
				{"type": "TALK", "names": [first_talk], "give_item": None},
				{"type": "TALK", "names": [end_name], "give_item": (second_item, 1), "remove_item": (first_item, 1)},
				{"type": "ITEM_PLAY", "item": second_item},
			],
		}
	return result, skipped


def compiled_dredgion_control_hunts(retail_quest: Path, retail_hunt: Path, retail_npcs: Path, retail_items: Path,
		script_root: Path, quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	hunt_nodes = {
		int(node.attrib["id"]): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_hunt).getroot()
		if node.attrib.get("id", "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_index, _ = npc_indexes(retail_npcs, set(COMPILED_DREDGION_CONTROL_NPCS), set())
	reward_items = item_ids(retail_items, {"potion_hp_mp_50a"})
	sources = {
		number: (path.read_text(encoding="utf-8") if (path := script_root / f"fun_{number:03d}.cpp").is_file() else "")
		for number in COMPILED_DREDGION_CONTROL_SOURCE_NUMBERS
	}
	for quest_id, (quest_hex, retail_race, base_race) in COMPILED_DREDGION_CONTROL_HUNTS.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		expected_retail = {
			"name": f"Q{quest_id}", "max_repeat_count": "1", "minlevel_permitted": "57",
			"reward_exp1": "7086913", "reward_gold1": "0", "reward_item1_1": "potion_hp_mp_50a 34",
			"race_permitted": retail_race,
		}
		reasons = [f"quest.xml:{field}" for field, expected in expected_retail.items() if fields.get(field) != expected]
		expected_hunt = {
			"acquired_npc_name": "IDTiamat_Sorus", "talk_npc1": "IDTiamat_FOBJ_Model_1", "count1": "1",
			"monster1": "IDTiamat_Sardha_Named_60_Ah", "reward_npc_name": "IDTiamat_Sorus_2",
		}
		hunt_fields = hunt_nodes.get(quest_id, {})
		if set(hunt_fields) != set(expected_hunt) | {"dev_name"}:
			reasons.append("Quest_SimpleHunt.xml:shape")
		reasons.extend(f"Quest_SimpleHunt.xml:{field}" for field, expected in expected_hunt.items() if hunt_fields.get(field) != expected)
		base = base_nodes.get(quest_id)
		rewards = None if base is None else base.find("rewards")
		if (base is None or base.attrib.get("minlevel_permitted") != "57" or base.attrib.get("max_repeat_count") != "1"
				or base.attrib.get("race_permitted") != base_race or base.attrib.get("category") != "QUEST"
				or rewards is None or rewards.attrib != {"exp": "7086913"}
				or tuple((node.attrib.get("item_id"), node.attrib.get("count")) for node in rewards.findall("reward_item")) != (("162000050", "34"),)):
			reasons.append("quest_data.xml:task_and_rewards")
		for name, expected_id in COMPILED_DREDGION_CONTROL_NPCS.items():
			if npc_index.get(name.casefold()) != (expected_id,):
				reasons.append(f"npcs.xml:{name}")
			if not has_function_evidence(sources[163], quest_hex, (f'L"{name}"',)):
				reasons.append(f"script:npc_binding:{name}")
		if reward_items.get("potion_hp_mp_50a") != (162000050,):
			reasons.append("Items.xml:potion_hp_mp_50a")
		for source_number, token, reason in (
			(317, f",{quest_hex},0,0xffffffff,0)", "start_phase"),
			(301, f",{quest_hex},3,0x40000000,0)", "object_phase"),
			(305, f",{quest_hex},3,1,0)", "kill_complete_phase"),
			(328, f",{quest_hex},4,0xffffffff,0)", "reward_phase"),
		):
			if not has_function_evidence(sources[source_number], quest_hex, (token,)):
				reasons.append(f"script:{reason}")
		if not has_function_evidence(sources[780], quest_hex, (f"FUN_180caf7c0({quest_hex}", "1,0,0")):
			reasons.append("script:start_dialog")
		if not has_function_evidence(sources[787], quest_hex, (f"FUN_180cafa40({quest_hex}", "1,1,&DAT_")):
			reasons.append("script:object_dialog")
		if not has_function_evidence(sources[792], quest_hex, (f"FUN_180cafa40({quest_hex}", "1,1,0,0,0,0")):
			reasons.append("script:reward_dialog")
		if not has_function_evidence(sources[800], quest_hex, (f"FUN_180cafe40({quest_hex}", "0,0,0x40000000")):
			reasons.append("script:object_progress")
		if not has_function_evidence(sources[766], quest_hex, (f"FUN_180cb13b0({quest_hex}", "1,1,1,1")):
			reasons.append("script:kill_count_and_completion")
		if reasons:
			skipped[str(quest_id)] = sorted(set(reasons))
			continue
		result[quest_id] = {
			"source": "compiled_script_dredgion_control_hunt", "start_type": "TALK",
			"start": "IDTiamat_Sorus", "end": "IDTiamat_Sorus_2",
			"steps": [
				{"type": "TALK", "names": ["IDTiamat_FOBJ_Model_1"], "give_item": None},
				{"type": "HUNT", "names": ["IDTiamat_Sardha_Named_60_Ah"], "amount": 1},
			],
		}
	return result, skipped


def compiled_dredgion_navigation_hunts(retail_quest: Path, retail_serial_hunt: Path, retail_npcs: Path,
		retail_items: Path, script_root: Path, quest_data: Path,
		enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	hunt_nodes = {
		int(node.attrib["id"]): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_serial_hunt).getroot()
		if node.attrib.get("id", "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_index, _ = npc_indexes(retail_npcs, set(COMPILED_DREDGION_NAVIGATION_NPCS), set())
	reward_items = item_ids(retail_items, {"wrap_quest_reward_option_enchant_90"})
	sources = {
		number: (path.read_text(encoding="utf-8") if (path := script_root / f"fun_{number:03d}.cpp").is_file() else "")
		for number in COMPILED_DREDGION_NAVIGATION_SOURCE_NUMBERS
	}
	for quest_id, (quest_hex, retail_race, base_race, start_name, talk_name) in COMPILED_DREDGION_NAVIGATION_HUNTS.items():
		if quest_id not in enabled_ids:
			continue
		fields = retail_nodes.get(quest_id, {})
		expected_retail = {
			"name": f"Q{quest_id}", "max_repeat_count": "1", "minlevel_permitted": "56", "maxlevel_permitted": "60",
			"reward_exp1": "6775529", "reward_gold1": "577800", "reward_abyss_point1": "2000",
			"reward_item1_1": "wrap_quest_reward_option_enchant_90 1", "race_permitted": retail_race,
		}
		reasons = [f"quest.xml:{field}" for field, expected in expected_retail.items() if fields.get(field) != expected]
		expected_hunt = {
			"acquired_npc_name": start_name, "talk_npc1": talk_name, "count_first": "1",
			"monster_first": "IDDreadgion_03_DrakanFiNamedAA_60_Ae, IDDreadgion_03_DrakanFiNamedAB_60_Ae",
			"count_second": "1", "monster_second": "IDDreadgion_03_DrakanWi_Boss_Ah", "reward_npc_name": start_name,
		}
		hunt_fields = hunt_nodes.get(quest_id, {})
		if set(hunt_fields) != set(expected_hunt) | {"dev_name"}:
			reasons.append("Quest_SimpleSerialHunt.xml:shape")
		reasons.extend(f"Quest_SimpleSerialHunt.xml:{field}" for field, expected in expected_hunt.items() if hunt_fields.get(field) != expected)
		base = base_nodes.get(quest_id)
		rewards = None if base is None else base.find("rewards")
		if (base is None or base.attrib.get("minlevel_permitted") != "56" or base.attrib.get("maxlevel_permitted") != "60"
				or base.attrib.get("max_repeat_count") != "1" or base.attrib.get("race_permitted") != base_race
				or base.attrib.get("category") != "QUEST" or rewards is None
				or rewards.attrib != {"gold": "577800", "exp": "6775529", "ap": "2000"}
				or tuple((node.attrib.get("item_id"), node.attrib.get("count")) for node in rewards.findall("reward_item")) != (("188051598", "1"),)):
			reasons.append("quest_data.xml:task_and_rewards")
		required_names = {start_name, talk_name, "IDDreadgion_03_DrakanFiNamedAA_60_Ae",
			"IDDreadgion_03_DrakanFiNamedAB_60_Ae", "IDDreadgion_03_DrakanWi_Boss_Ah"}
		for name in required_names:
			expected_id = COMPILED_DREDGION_NAVIGATION_NPCS[name]
			if npc_index.get(name.casefold()) != (expected_id,):
				reasons.append(f"npcs.xml:{name}")
			if not has_function_evidence(sources[340] + sources[341], quest_hex, (f'L"{name}"',)):
				reasons.append(f"script:npc_binding:{name}")
		if reward_items.get("wrap_quest_reward_option_enchant_90") != (188051598,):
			reasons.append("Items.xml:wrap_quest_reward_option_enchant_90")
		for token, reason in (
			(f",{quest_hex},0,0xffffffff,0)", "start_phase"),
			(f",{quest_hex},3,0x41,0)", "talk_phase"),
			(f",{quest_hex},3,0x40000000,0)", "hunt_phase"),
			(f",{quest_hex},4,0xffffffff,0)", "reward_phase"),
		):
			if not has_function_evidence(sources[342], quest_hex, (token,)):
				reasons.append(f"script:{reason}")
		for tokens, reason in (
			((f"FUN_180caf7c0({quest_hex}", "1,0,0"), "start_dialog"),
			((f"FUN_180cafa40({quest_hex}", "0x41,1,&DAT_"), "talk_dialog"),
			((f"FUN_180cafa40({quest_hex}", "0x41,1,0,0,0,0"), "reward_dialog"),
			((f"FUN_180cafe40({quest_hex}", "0,0,0x40000000"), "hunt_progress"),
			((f"FUN_180cb2450({quest_hex}", "param_2,1,0,1,1,0x41,1"), "first_hunt_completion"),
			((f"FUN_180cb2450({quest_hex}", "param_2,2,1,0x41,1,0x41,1"), "second_hunt_completion"),
		):
			if not has_function_evidence(sources[804], quest_hex, tokens):
				reasons.append(f"script:{reason}")
		if reasons:
			skipped[str(quest_id)] = sorted(set(reasons))
			continue
		result[quest_id] = {
			"source": "compiled_script_dredgion_navigation_hunt", "start_type": "TALK", "start": start_name, "end": start_name,
			"steps": [
				{"type": "TALK", "names": [talk_name]},
				{"type": "HUNT", "names": ["IDDreadgion_03_DrakanFiNamedAA_60_Ae", "IDDreadgion_03_DrakanFiNamedAB_60_Ae"], "amount": 1},
				{"type": "HUNT", "names": ["IDDreadgion_03_DrakanWi_Boss_Ah"], "amount": 1},
			],
		}
	return result, skipped


def compiled_christmas_courier_hunts(retail_quest: Path, retail_npcs: Path, retail_items: Path,
		script_root: Path, quest_data: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, list[str]]]:
	result: dict[int, dict[str, object]] = {}
	skipped: dict[str, list[str]] = {}
	retail_nodes = {
		int(node.findtext("id")): {child.tag: (child.text or "").strip() for child in node}
		for node in ET.parse(retail_quest).getroot()
		if (node.findtext("id") or "").isdigit()
	}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	npc_index, _ = npc_indexes(retail_npcs, set(COMPILED_CHRISTMAS_COURIER_NPCS), set())
	reward_items = item_ids(retail_items, {"event_christmas_candy", "event_christmas_coin_01"})
	sources = {
		number: (path.read_text(encoding="utf-8") if (path := script_root / f"fun_{number:03d}.cpp").is_file() else "")
		for number in COMPILED_CHRISTMAS_COURIER_SOURCE_NUMBERS
	}
	bindings = sources[611] + sources[616] + sources[620]
	for quest_id, data in COMPILED_CHRISTMAS_COURIER_HUNTS.items():
		if quest_id not in enabled_ids:
			continue
		quest_hex = str(data["quest_hex"])
		fields = retail_nodes.get(quest_id, {})
		expected_retail = {
			"name": f"Q{quest_id}", "category1": "event", "max_repeat_count": "255", "minlevel_permitted": "9",
			"maxlevel_permitted": "0", "cannot_share": "1", "quest_repeat_cycle": "all", "reward_exp1": "0",
			"reward_gold1": "0", "reward_item1_1": "event_christmas_candy 1",
			"reward_item1_2": "event_christmas_coin_01 4", "race_permitted": data["retail_race"],
		}
		reasons = [f"quest.xml:{field}" for field, expected in expected_retail.items() if fields.get(field) != expected]
		base = base_nodes.get(quest_id)
		rewards = None if base is None else base.find("rewards")
		if (base is None or base.attrib.get("minlevel_permitted") != "9" or base.attrib.get("max_repeat_count") != "255"
				or base.attrib.get("cannot_share") != "true" or base.attrib.get("repeat_cycle") != "ALL"
				or base.attrib.get("race_permitted") != data["base_race"] or base.attrib.get("category") != "EVENT"
				or rewards is None or rewards.attrib
				or tuple((node.attrib.get("item_id"), node.attrib.get("count")) for node in rewards.findall("reward_item"))
				!= (("160010203", "1"), ("186000177", "4"))):
			reasons.append("quest_data.xml:task_and_rewards")
		for name in (str(data["start"]), str(data["target"]), str(data["sensor"])):
			if npc_index.get(name.casefold()) != (COMPILED_CHRISTMAS_COURIER_NPCS[name],):
				reasons.append(f"npcs.xml:{name}")
			if not has_function_evidence(bindings, quest_hex, (f'L"{name}"',)):
				reasons.append(f"script:npc_binding:{name}")
		if reward_items != {"event_christmas_candy": (160010203,), "event_christmas_coin_01": (186000177,)}:
			reasons.append("Items.xml:christmas_rewards")
		if not has_function_evidence(sources[int(data["start_phase_source"])], quest_hex, (f",{quest_hex},0,0xffffffff,0)",)):
			reasons.append("script:start_phase")
		if not has_function_evidence(sources[int(data["reward_phase_source"])], quest_hex, (f",{quest_hex},4,0xffffffff,0)",)):
			reasons.append("script:reward_phase")
		if not has_function_evidence(sources[878], quest_hex, (str(data["target_hex"]), "local_res10 == '\\x03'",
				"local_res11 == 0", f",{quest_hex},1)", "local_res11 == 1", f",{quest_hex},0,0)")):
			reasons.append("script:two_kill_completion")
		if not has_function_evidence(sources[int(data["start_dialog_source"])], quest_hex,
				(f"FUN_180caf640({quest_hex}", "20000", f",{quest_hex},0,0)", f"FUN_180caf350({quest_hex}")):
			reasons.append("script:start_dialog")
		if not has_function_evidence(sources[int(data["reward_dialog_source"])], quest_hex,
				(f"FUN_180caf6c0({quest_hex}", f"FUN_180caf350({quest_hex}")):
			reasons.append("script:reward_dialog")
		if reasons:
			skipped[str(quest_id)] = sorted(set(reasons))
			continue
		result[quest_id] = {
			"source": "compiled_script_christmas_courier_hunt", "start": data["start"], "end": data["start"],
			"stages": [([str(data["target"])], 2)],
		}
	return result, skipped


def retail_hunts(path: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, int], dict[str, object]]:
	hunts: dict[int, dict[str, object]] = {}
	stats = {"retail": 0, "missing_base": 0, "unsupported": 0, "invalid": 0}
	skipped: dict[str, object] = {"unsupported": {}, "invalid": []}
	for node in ET.parse(path).getroot():
		quest_id = int(node.attrib["id"])
		stats["retail"] += 1
		if quest_id not in enabled_ids:
			stats["missing_base"] += 1
			continue
		fields = {child.tag: (child.text or "").strip() for child in node}
		# talk_npc1 与 reward_npc_name 同名时只是重复点名交还 NPC（ScriptDLL 相位 3/4 同挂该 NPC，
		# 无独立中间站），不携带额外语义；不同名则是真中间站，继续按不支持隔离。
		redundant = {"talk_npc1"} if fields.get("talk_npc1") and fields["talk_npc1"] == fields.get("reward_npc_name") else set()
		unsupported = sorted(set(fields) - hunt_fields() - redundant)
		if unsupported:
			stats["unsupported"] += 1
			skipped["unsupported"][str(quest_id)] = unsupported
			continue
		start = fields.get("acquired_npc_name", "")
		end = fields.get("reward_npc_name", "")
		stages: list[tuple[list[str], int]] = []
		valid = bool(start and end)
		for index in range(1, 6):
			count = fields.get(f"count{index}")
			targets = fields.get(f"monster{index}")
			if not count and not targets:
				continue
			if not count or not targets:
				valid = False
				break
			stages.append(([name.strip() for name in targets.split(",") if name.strip()], int(count)))
		if not valid or not stages or any(not names for names, _ in stages):
			stats["invalid"] += 1
			skipped["invalid"].append(quest_id)
			continue
		hunts[quest_id] = {"start": start, "end": end, "stages": stages}
	return hunts, stats, skipped


def npc_indexes(path: Path, exact_names: set[str], target_names: set[str]) -> tuple[dict[str, tuple[int, ...]], dict[str, tuple[int, ...]]]:
	wanted_exact = {name.casefold() for name in exact_names}
	wanted_targets = {name.casefold() for name in target_names}
	wanted_aliases = wanted_exact | wanted_targets
	exact: dict[str, set[int]] = {}
	aliases: dict[str, set[int]] = {}
	for _, node in ET.iterparse(path, events=("end",)):
		if node.tag != "npc":
			continue
		name = (node.findtext("name") or "").strip().casefold()
		quest_ai_name = (node.findtext("quest_ai_name") or "").strip().casefold()
		raw_id = (node.findtext("id") or "").strip()
		if raw_id:
			if name in wanted_exact or name in wanted_targets:
				exact.setdefault(name, set()).add(int(raw_id))
			if quest_ai_name in wanted_aliases:
				aliases.setdefault(quest_ai_name, set()).add(int(raw_id))
		node.clear()
	return (
		{name: tuple(sorted(ids)) for name, ids in exact.items()},
		{name: tuple(sorted(ids)) for name, ids in aliases.items()},
	)


def item_ids(path: Path, names: set[str]) -> dict[str, tuple[int, ...]]:
	wanted = {name.casefold() for name in names}
	result: dict[str, set[int]] = {}
	for _, node in ET.iterparse(path, events=("end",)):
		if node.tag != "item":
			continue
		name = (node.findtext("name") or "").strip().casefold()
		raw_id = (node.findtext("id") or "").strip()
		if name in wanted and raw_id:
			result.setdefault(name, set()).add(int(raw_id))
		node.clear()
	return {name: tuple(sorted(ids)) for name, ids in result.items()}


def retail_work_orders(task_path: Path, retail_quest_path: Path, quest_data_path: Path, npc_path: Path,
		item_path: Path, recipe_path: Path, enabled_ids: set[int]) -> tuple[dict[int, dict[str, object]], dict[str, int], dict[str, list[str]]]:
	allowed = {"dev_name", "task_npc", "combineskill", "combine_skillpoint", "recipe_name", "product", "give_component1", "give_component2"}
	stats = {"retail": 0, "missing_base": 0, "invalid": 0}
	invalid: dict[str, list[str]] = {}
	candidates: dict[int, dict[str, object]] = {}
	for node in ET.parse(task_path).getroot():
		quest_id = int(node.attrib["id"])
		stats["retail"] += 1
		if quest_id not in enabled_ids:
			stats["missing_base"] += 1
			continue
		fields = {child.tag: (child.text or "").strip() for child in node}
		errors = []
		if set(fields) not in (allowed - {"give_component2"}, allowed) or len(fields) != len(node):
			errors.append("task_fields")
		npcs = [name.strip() for name in fields.get("task_npc", "").split(",") if name.strip()]
		product = parse_item_reference(fields.get("product", ""))
		components = [parse_item_reference(fields.get(f"give_component{index}", "")) for index in range(1, 3) if fields.get(f"give_component{index}")]
		if not fields.get("dev_name") or not npcs or not fields.get("combineskill") or not fields.get("combine_skillpoint", "").isdigit() or not fields.get("recipe_name") or product is None or not components or any(component is None for component in components):
			errors.append("task_values")
		if quest_id in candidates or str(quest_id) in invalid:
			errors.append("duplicate_task_id")
		if errors:
			invalid[str(quest_id)] = sorted(set(errors))
			continue
		candidates[quest_id] = {**fields, "npcs": npcs, "product_ref": product, "component_refs": components}

	npc_names = {name for quest in candidates.values() for name in quest["npcs"]}
	exact_npcs, _ = npc_indexes(npc_path, npc_names, set())
	item_names = {
		str(reference[0]) for quest in candidates.values()
		for reference in (quest["product_ref"], *quest["component_refs"])
	}
	items = item_ids(item_path, item_names)
	recipe_names = {str(quest["recipe_name"]) for quest in candidates.values()}
	recipes: dict[str, list[ET.Element]] = {}
	for node in ET.parse(recipe_path).getroot():
		name = (node.findtext("name") or "").strip()
		if name in recipe_names:
			recipes.setdefault(name, []).append(node)
	retail_quests = {
		int(node.findtext("id")): node for node in ET.parse(retail_quest_path).getroot()
		if (node.findtext("id") or "").isdigit() and int(node.findtext("id")) in candidates
	}
	base_quests = {int(node.attrib["id"]): node for node in ET.parse(quest_data_path).getroot().findall("quest") if int(node.attrib["id"]) in candidates}
	result: dict[int, dict[str, object]] = {}
	for quest_id, quest in candidates.items():
		errors = []
		resolved_npcs = [exact_npcs.get(str(name).casefold(), ()) for name in quest["npcs"]]
		if any(len(ids) != 1 for ids in resolved_npcs):
			errors.append("task_npc_resolution")
		references = (quest["product_ref"], *quest["component_refs"])
		resolved_items = [items.get(str(reference[0]).casefold(), ()) for reference in references]
		if any(len(ids) != 1 for ids in resolved_items):
			errors.append("item_resolution")
		recipe_nodes = recipes.get(str(quest["recipe_name"]), [])
		if len(recipe_nodes) != 1:
			errors.append("recipe_resolution")
		else:
			recipe = recipe_nodes[0]
			recipe_id = (recipe.findtext("id") or "").strip()
			recipe_components = {(child.text or "").strip() for child in recipe.findall(".//*") if re.fullmatch(r"component\d+", child.tag)}
			recipe_fields = {
				"combineskill": str(quest["combineskill"]), "required_skillpoint": str(quest["combine_skillpoint"]),
				"task_type": "1", "product": str(quest["product_ref"][0]), "product_quantity": "1",
			}
			if not recipe_id.isdigit() or any(len(recipe.findall(field)) != 1 or (recipe.findtext(field) or "").strip() != value for field, value in recipe_fields.items()):
				errors.append("recipe_fields")
			if any(str(component[0]) not in recipe_components for component in quest["component_refs"]):
				errors.append("recipe_components")
		retail_quest = retail_quests.get(quest_id)
		if retail_quest is None:
			errors.append("quest_xml_missing")
		else:
			product_fields = [
				parse_item_reference((child.text or "").strip()) for child in retail_quest
				if re.fullmatch(r"collect_item\d+|check_item\d+_\d+", child.tag)
			]
			component_fields = [
				parse_item_reference((child.text or "").strip()) for child in retail_quest
				if re.fullmatch(r"quest_work_item\d+", child.tag)
			]
			if (retail_quest.findtext("category1") or "").strip() != "task" or (retail_quest.findtext("combineskill") or "").strip() != quest["combineskill"] or (retail_quest.findtext("combine_skillpoint") or "").strip() != quest["combine_skillpoint"] or (retail_quest.findtext("recipe_name") or "").strip() != quest["recipe_name"]:
				errors.append("quest_xml_fields")
			if product_fields != [quest["product_ref"], quest["product_ref"]] or component_fields != quest["component_refs"]:
				errors.append("quest_xml_items")
			if len(recipe_nodes) == 1 and (retail_quest.findtext("race_permitted") or "").strip() != (recipe_nodes[0].findtext("qualification_race") or "").strip():
				errors.append("quest_xml_race")
		base_quest = base_quests.get(quest_id)
		if base_quest is None:
			errors.append("quest_data_missing")
		elif not any(len(ids) != 1 for ids in resolved_items):
			product = quest["product_ref"]
			components = quest["component_refs"]
			base_products = [(int(node.attrib["item_id"]), int(node.attrib["count"])) for node in base_quest.findall("./collect_items/collect_item")]
			base_components = [(int(node.attrib["item_id"]), int(node.attrib["count"])) for node in base_quest.findall("./quest_work_items/quest_work_item")]
			if base_products != [(resolved_items[0][0], product[1])] or base_components != [(resolved_items[index + 1][0], component[1]) for index, component in enumerate(components)]:
				errors.append("quest_data_items")
			if base_quest.attrib.get("category") != "TASK" or base_quest.attrib.get("combine_skillpoint") != quest["combine_skillpoint"]:
				errors.append("quest_data_fields")
		if errors:
			invalid[str(quest_id)] = sorted(set(errors))
			continue
		result[quest_id] = {
			"start_npc_ids": tuple(ids[0] for ids in resolved_npcs),
			"recipe_id": int(recipe_nodes[0].findtext("id")),
			"give_components": tuple((resolved_items[index + 1][0], component[1]) for index, component in enumerate(quest["component_refs"])),
		}
	stats["invalid"] = len(invalid)
	return result, stats, invalid


def compiled_sink(quest: dict[str, object], default: str) -> str:
	kind = quest.get("kind")
	if kind == "data_driven_simple":
		return "data"
	if kind in ("item_collecting", "report_to", "report_to_many"):
		return "simple"
	if kind == "monster_hunt":
		return "hunt"
	return default


def runtime_projection(value: object, casefold_references: bool = False) -> object:
	if isinstance(value, dict):
		return tuple(sorted(
			(key, runtime_projection(item, key in {
				"start", "end", "names", "actions", "talks", "objects", "stages", "item", "evidence_item",
				"give_item", "remove_item", "start_give_item", "start_remove_item", "spawns",
			})) for key, item in value.items()
			if key != "source" and item is not None
		))
	if isinstance(value, (list, tuple)):
		return tuple(runtime_projection(item, casefold_references) for item in value)
	return value.casefold() if casefold_references and isinstance(value, str) else value


def reclaim_generic_projections(compiled: dict[int, dict[str, object]], generic_by_sink: dict[str, dict[int, dict[str, object]]],
		default_sink: str) -> tuple[dict[int, dict[str, object]], list[int]]:
	reclaimed = [
		quest_id for quest_id, quest in compiled.items()
		if (generic := generic_by_sink[compiled_sink(quest, default_sink)].get(quest_id)) is not None
		and runtime_projection(generic) == runtime_projection(quest)
	]
	return {quest_id: quest for quest_id, quest in compiled.items() if quest_id not in reclaimed}, sorted(reclaimed)


# 能力族注册表：新增一族只需 ①写 COMPILED_* 数据与 compiled_*() 校验函数（推荐参照
# compiled_simple_talks 的"证据全在数据表"形态）②在此登记一行；其余（调用、并集、分桶、
# 账目、eligible/generated/skipped、script_sources）全部由注册表驱动。
# 字段：(source 标签, ScriptDLL 源码编号, 无 kind 条目的默认桶 data/hunt,
#        需从通用族收回的账目 {桶: "full"|"unsupported"}, 构造器)
COMPILED_FAMILIES = (
	("compiled_script_collect", COMPILED_ITEM_BUYER_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_item_buyers(c.retail_quest, c.script, c.quest_data, c.enabled)),
	("compiled_script_timed_report", COMPILED_FIREWORK_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_firework_reports(c.retail_quest, c.script, c.quest_data, c.enabled)),
	("compiled_script_action_talk", COMPILED_DEBRIS_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_debris_rescues(c.retail_quest, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_world_collect", COMPILED_WORLD_COLLECT_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_world_collects(c.retail_quest, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_get_item", COMPILED_GROWTH_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_growth_quests(c.retail_quest, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_sensory_complete", COMPILED_SENSORY_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_sensory_completes(c.retail_quest, c.npc, c.script, c.quest_data, c.enabled)),
	("compiled_script_action_progress", COMPILED_PAIOS_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_paios_rescues(c.retail_quest, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_housing_talk", COMPILED_HOUSING_FLOWER_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_housing_flower_visits(c.retail_quest, c.npc, c.script, c.quest_data, c.enabled)),
	("compiled_script_scorched_tree", COMPILED_SCORCHED_TREE_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_scorched_tree_actions(c.retail_quest, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_kaldor_arrival", COMPILED_KALDOR_ARRIVAL_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_kaldor_arrivals(c.retail_quest, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_coalescence_complete", COMPILED_COALESCENCE_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_coalescence_completes(c.retail_quest, c.npc, c.script, c.quest_data, c.enabled)),
	("compiled_script_bastion_movie", COMPILED_BASTION_MOVIE_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_bastion_movies(c.retail_quest, c.npc, c.script, c.quest_data, c.enabled)),
	("compiled_script_simple_talk", COMPILED_SIMPLE_TALK_SOURCE_NUMBERS, "data", {"talk": "full", "use_item": "full"},
		lambda c: compiled_simple_talks(c.retail_quest, c.talk, c.use_item, c.npc, c.script, c.quest_data, c.enabled)),
	("compiled_script_surama_hunt", COMPILED_SURAMA_SOURCE_NUMBERS, "hunt", {},
		lambda c: compiled_surama_hunts(c.retail_quest, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_dredgion_captain_hunt", COMPILED_DREDGION_CAPTAIN_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_dredgion_captain_hunts(c.retail_quest, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_arena_item_play", COMPILED_ARENA_ITEM_PLAY_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_arena_item_plays(c.retail_quest, c.item_play, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_christmas_courier_hunt", COMPILED_CHRISTMAS_COURIER_SOURCE_NUMBERS, "hunt", {},
		lambda c: compiled_christmas_courier_hunts(c.retail_quest, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_dredgion_control_hunt", COMPILED_DREDGION_CONTROL_SOURCE_NUMBERS, "data", {"hunt": "unsupported"},
		lambda c: compiled_dredgion_control_hunts(c.retail_quest, c.hunt, c.npc, c.item, c.script, c.quest_data, c.enabled)),
	("compiled_script_dredgion_navigation_hunt", COMPILED_DREDGION_NAVIGATION_SOURCE_NUMBERS, "data", {},
		lambda c: compiled_dredgion_navigation_hunts(c.retail_quest, c.serial_hunt, c.npc, c.item, c.script, c.quest_data, c.enabled)),
)


def render(simple: dict[int, dict[str, object]], hunts: dict[int, dict[str, object]], pvps: dict[int, dict[str, object]], use_items: dict[int, dict[str, object]], data_quests: dict[int, dict[str, object]], exact_index: dict[str, tuple[int, ...]], aliases: dict[str, tuple[int, ...]], item_index: dict[str, tuple[int, ...]], sources: tuple[Path, ...], work_orders: dict[int, dict[str, object]] | None = None) -> tuple[bytes, dict[str, list[str]], dict[str, int]]:
	root = ET.Element("quest_scripts", {
		"xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
		"xsi:noNamespaceSchemaLocation": "../../../schemas/quest_script_data.xsd",
	})
	unresolved: dict[str, list[str]] = {}
	generated = {"talk": 0, "collect": 0, "hunt": 0, "use_item": 0, "work_order": 0, "data_driven_report": 0, "data_driven_talk": 0, "data_driven_hunt": 0, "data_driven_collect": 0, "data_driven_pvp": 0, "data_driven_item_play": 0, "data_driven_complex": 0, "compiled_script_use_item": 0, **{label: 0 for label, *_ in COMPILED_FAMILIES}}
	for kind in ("item_collecting", "report_to", "report_to_many"):
		for quest_id, quest in sorted(simple.items()):
			if quest["kind"] != kind:
				continue
			names = {str(quest["end"]), *map(str, quest.get("objects", [])), *map(str, quest.get("talks", []))}
			if quest.get("start"):
				names.add(str(quest["start"]))
			missing = sorted(name for name in names if name.casefold() not in exact_index and name.casefold() not in aliases)
			if quest.get("item") and len(item_index.get(str(quest["item"][0]).casefold(), ())) != 1:
				missing.append(f"item:{quest['item'][0]}")
			if quest.get("evidence_item") and len(item_index.get(str(quest["evidence_item"]).casefold(), ())) != 1:
				missing.append(f"item:{quest['evidence_item']}")
			if missing:
				unresolved[str(quest_id)] = missing
				continue
			end = exact_index.get(str(quest["end"]).casefold()) or aliases[str(quest["end"]).casefold()]
			attrs = {"id": str(quest_id), "retail": "true", "end_npc_ids": " ".join(map(str, end))}
			if quest.get("start"):
				start = exact_index.get(str(quest["start"]).casefold()) or aliases[str(quest["start"]).casefold()]
				attrs["start_npc_ids"] = " ".join(map(str, start))
			else:
				# 势力每日任务无起始 NPC；照旧 XML 形态写 0 占位（模型属性为必填）。
				attrs["start_npc_ids"] = "0"
			if quest.get("movie"):
				attrs["movie"] = str(quest["movie"])
			objects = quest.get("objects", [])
			if objects:
				object_ids = sorted({npc_id for name in objects for npc_id in (exact_index.get(str(name).casefold()) or aliases[str(name).casefold()])})
				attrs["action_item_ids"] = " ".join(map(str, object_ids))
			if quest.get("item"):
				attrs["item_id"] = str(item_index[str(quest["item"][0]).casefold()][0])
			if quest.get("source") == "data_driven_collect":
				attrs.update({
					"start_dialog_id": "4762", "start_dialog_id2": "1011",
					"check_ok_dialog_id": "10000", "check_fail_dialog_id": "10001", "reward_dialog_id": "10002",
				})
			for key in ("timeout_seconds", "timeout_start_var", "timeout_reset_var"):
				if key in quest:
					attrs[key] = str(quest[key])
			talks = quest.get("talks", [])
			talk_ids = [exact_index.get(str(name).casefold()) or aliases[str(name).casefold()] for name in talks]
			if sum(map(len, talk_ids)) != len({npc_id for ids in talk_ids for npc_id in ids}):
				unresolved[str(quest_id)] = ["repeated talk NPC cannot preserve ordered steps"]
				continue
			element = ET.SubElement(root, kind, attrs)
			for var, ids in enumerate(talk_ids):
				for npc_id in ids:
					ET.SubElement(element, "npc_infos", {
						"var": str(var), "npc_id": str(npc_id), "quest_dialog": str(1352 + 341 * var),
					})
			generated[str(quest.get("source") or ("collect" if objects else "talk"))] += 1
	for quest_id, hunt in sorted(hunts.items()):
		start_name = str(hunt["start"]) if hunt.get("start") else None
		end_name = str(hunt["end"])
		names = {end_name}
		if start_name:
			names.add(start_name)
		for targets, _ in hunt["stages"]:  # type: ignore[assignment]
			names.update(targets)
		missing = sorted(
			name for name in names
			if name in {start_name, end_name} and name.casefold() not in exact_index and name.casefold() not in aliases
			or name not in {start_name, end_name}
			and name.casefold() not in exact_index and name.casefold() not in aliases
		)
		if missing:
			unresolved[str(quest_id)] = missing
			continue
		start = (0,) if start_name is None else exact_index.get(start_name.casefold()) or aliases[start_name.casefold()]
		end = exact_index.get(end_name.casefold()) or aliases[end_name.casefold()]
		attrs = {"id": str(quest_id), "retail": "true", "start_npc_ids": " ".join(map(str, start))}
		if end != start:
			attrs["end_npc_ids"] = " ".join(map(str, end))
		element = ET.SubElement(root, "monster_hunt", attrs)
		for var, (targets, count) in enumerate(hunt["stages"]):  # type: ignore[assignment]
			target_ids = sorted({
				npc_id for name in targets
				for npc_id in (exact_index.get(name.casefold()) or aliases[name.casefold()])
			})
			ET.SubElement(element, "monster", {
				"var": str(var), "end_var": str(count), "npc_ids": " ".join(map(str, target_ids)),
			})
		generated[str(hunt.get("source") or "hunt")] += 1
	for quest_id, pvp in sorted(pvps.items()):
		start_name = str(pvp["start"]).casefold() if pvp.get("start") else None
		end_name = str(pvp["end"]).casefold()
		if start_name is not None and start_name not in exact_index and start_name not in aliases or end_name not in exact_index and end_name not in aliases:
			unresolved[str(quest_id)] = sorted(name for name in (str(pvp["start"]), str(pvp["end"])) if name.casefold() not in exact_index and name.casefold() not in aliases)
			continue
		start = (0,) if start_name is None else exact_index.get(start_name) or aliases[start_name]
		end = exact_index.get(end_name) or aliases[end_name]
		ET.SubElement(root, "kill_in_world", {
			"id": str(quest_id), "retail": "true", "start_npc_ids": " ".join(map(str, start)),
			"end_npc_ids": " ".join(map(str, end)), "worlds": "0", "amount": str(pvp["amount"]),
			"reward_dialog_id": "10002",
		})
		generated["data_driven_pvp"] += 1
	for quest_id, quest in sorted(use_items.items()):
		item_name = str(quest["item"])
		end_name = str(quest["end"])
		resolved_end = exact_index.get(end_name.casefold()) or aliases.get(end_name.casefold(), ())
		end_ids = quest.get("end_ids") or resolved_end
		missing = [] if quest.get("end_ids") or len(resolved_end) == 1 else [f"npc:{end_name}"]
		missing.extend(f"npc:{name}" for name in map(str, quest["talks"])
			if len(exact_index.get(name.casefold()) or aliases.get(name.casefold(), ())) != 1)
		if len(item_index.get(item_name.casefold(), ())) != 1:
			missing.append(f"item:{item_name}")
		if missing:
			unresolved[str(quest_id)] = sorted(missing)
			continue
		attrs = {
			"id": str(quest_id), "retail": "true",
			"start_item_id": str(item_index[item_name.casefold()][0]),
		}
		attrs["end_npc_id" if len(end_ids) == 1 else "end_npc_ids"] = " ".join(map(str, end_ids))
		for index, name in enumerate(quest["talks"], 1):
			attrs[f"talk_npc_id{index}"] = str((exact_index.get(str(name).casefold()) or aliases[str(name).casefold()])[0])
		ET.SubElement(root, "item_order", attrs)
		generated[str(quest.get("source") or "use_item")] += 1
	for quest_id, quest in sorted(data_quests.items()):
		npc_names = {str(quest["end"])} if quest.get("end") else set()
		if quest.get("start") and quest["start_type"] in ("TALK", "SENSORY_COMPLETE"):
			npc_names.add(str(quest["start"]))
		item_names = set()
		if quest.get("start") and quest["start_type"] == "ITEM_PLAY":
			item_names.add(str(quest["start"]))
		if quest.get("start_give_item"):
			item_names.add(str(quest["start_give_item"][0]))
		if quest.get("start_remove_item"):
			item_names.add(str(quest["start_remove_item"][0]))
		for step in quest["steps"]:
			npc_names.update(map(str, step.get("names", [])))
			npc_names.update(map(str, step.get("actions", [])))
			if step.get("item"):
				item_names.add(str(step["item"]))
			if step.get("give_item"):
				item_names.add(str(step["give_item"][0]))
			if step.get("remove_item"):
				item_names.add(str(step["remove_item"][0]))
		missing = [f"npc:{name}" for name in npc_names if name.casefold() not in exact_index and name.casefold() not in aliases]
		spawn_names = {str(spawn[0]) for step in quest["steps"] for spawn in step.get("spawns", [])}
		missing.extend(f"spawn:{name}" for name in spawn_names if len(exact_index.get(name.casefold()) or aliases.get(name.casefold(), ())) != 1)
		missing.extend(f"item:{name}" for name in item_names if len(item_index.get(name.casefold(), ())) != 1)
		if missing:
			unresolved[str(quest_id)] = sorted(missing)
			continue
		attrs = {
			"id": str(quest_id), "retail": "true", "start_type": str(quest["start_type"]),
		}
		if quest.get("movie"):
			attrs["movie"] = str(quest["movie"])
		if quest.get("complete_on_start"):
			attrs["complete_on_start"] = "true"
		if quest.get("end"):
			attrs["end_npc_ids"] = " ".join(map(str, exact_index.get(str(quest["end"]).casefold()) or aliases[str(quest["end"]).casefold()]))
		if quest.get("world_id"):
			attrs["world_id"] = str(quest["world_id"])
		if quest.get("reset_world_id"):
			attrs["reset_world_id"] = str(quest["reset_world_id"])
		if quest.get("start_dialog_id"):
			attrs["start_dialog_id"] = str(quest["start_dialog_id"])
		if quest.get("start") and quest["start_type"] in ("TALK", "SENSORY_COMPLETE"):
			attrs["start_ids"] = " ".join(map(str, exact_index.get(str(quest["start"]).casefold()) or aliases[str(quest["start"]).casefold()]))
		elif quest.get("start") and quest["start_type"] == "ITEM_PLAY":
			attrs["start_item_id"] = str(item_index[str(quest["start"]).casefold()][0])
		if quest.get("start_give_item"):
			name, count = quest["start_give_item"]
			attrs["start_give_item_id"] = str(item_index[str(name).casefold()][0])
			attrs["start_give_item_count"] = str(count)
		if quest.get("start_remove_item"):
			name, count = quest["start_remove_item"]
			attrs["start_remove_item_id"] = str(item_index[str(name).casefold()][0])
			attrs["start_remove_item_count"] = str(count)
		element = ET.SubElement(root, "data_driven_quest", attrs)
		for step in quest["steps"]:
			step_attrs = {"type": str(step["type"])}
			if step.get("dialog_id"):
				step_attrs["dialog_id"] = str(step["dialog_id"])
			if step.get("names"):
				ids = sorted({npc_id for name in step["names"] for npc_id in (exact_index.get(str(name).casefold()) or aliases[str(name).casefold()])})
				step_attrs["ids"] = " ".join(map(str, ids))
			if step.get("actions"):
				ids = sorted({npc_id for name in step["actions"] for npc_id in (exact_index.get(str(name).casefold()) or aliases[str(name).casefold()])})
				step_attrs["action_ids"] = " ".join(map(str, ids))
			if step.get("delete_action_target"):
				step_attrs["delete_action_target"] = "true"
			if step.get("amount"):
				step_attrs["amount"] = str(step["amount"])
			if step.get("dialog_id"):
				step_attrs["dialog_id"] = str(step["dialog_id"])
			if step.get("advance_dialog_id"):
				step_attrs["advance_dialog_id"] = str(step["advance_dialog_id"])
			if step.get("movie"):
				step_attrs["movie"] = str(step["movie"])
			if step.get("teleport"):
				world_id, x, y, z, heading = step["teleport"]
				step_attrs.update({
					"teleport_world_id": str(world_id), "teleport_x": str(x), "teleport_y": str(y),
					"teleport_z": str(z), "teleport_heading": str(heading),
				})
			if step.get("timer_seconds"):
				step_attrs["timer_seconds"] = str(step["timer_seconds"])
				step_attrs["timer_destination_progress"] = str(step["timer_destination_progress"])
			if step.get("world_id"):
				step_attrs["world_id"] = str(step["world_id"])
			if step.get("item"):
				step_attrs["item_id"] = str(item_index[str(step["item"]).casefold()][0])
			if step.get("give_item"):
				name, count = step["give_item"]
				step_attrs["give_item_id"] = str(item_index[str(name).casefold()][0])
				step_attrs["give_item_count"] = str(count)
			if step.get("remove_item"):
				name, count = step["remove_item"]
				step_attrs["remove_item_id"] = str(item_index[str(name).casefold()][0])
				step_attrs["remove_item_count"] = str(count)
			step_element = ET.SubElement(element, "step", step_attrs)
			for name, count, lifetime, x, y, z, heading in step.get("spawns", []):
				npc_id = (exact_index.get(str(name).casefold()) or aliases[str(name).casefold()])[0]
				spawn_attrs = {"npc_id": str(npc_id), "count": str(count), "lifetime_seconds": str(lifetime)}
				if x is None:
					spawn_attrs["relative"] = "true"
				else:
					spawn_attrs.update({"x": str(x), "y": str(y), "z": str(z), "heading": str(heading)})
				ET.SubElement(step_element, "spawn", spawn_attrs)
		generated[str(quest.get("source") or "data_driven_complex")] += 1
	for quest_id, quest in sorted((work_orders or {}).items()):
		element = ET.SubElement(root, "work_order", {
			"id": str(quest_id), "retail": "true",
			"start_npc_ids": " ".join(map(str, quest["start_npc_ids"])), "recipe_id": str(quest["recipe_id"]),
		})
		for item_id, count in quest["give_components"]:
			ET.SubElement(element, "give_component", {"item_id": str(item_id), "count": str(count)})
		generated["work_order"] += 1
	ET.indent(root, space="  ")
	hashes = " ".join(f"{path.name}:{sha256(path)}" for path in sources)
	body = ET.tostring(root, encoding="unicode", short_empty_elements=True)
	return f'<?xml version="1.0" encoding="UTF-8"?>\n<!-- Generated from retail 5.8: {hashes} -->\n{body}\n'.encode(), unresolved, generated


def generate(retail: Path, quest_data: Path, retail_script: Path = DEFAULT_RETAIL_SCRIPT,
		legacy_dir: Path = DEFAULT_OUTPUT.parent, client_quest: Path | None = None,
		region: str = DEFAULT_RETAIL_REGION, reference_graph: Path | None = None) -> tuple[bytes, dict[str, object]]:
	assert_not_legacy_data_root(retail)
	selected = {
		name: source_file(retail, name, region, prefer_common=name == "quest.xml")
		for name in QUEST_SOURCE_FILES
	}
	hunt_file = selected["Quest_SimpleHunt.xml"][0]
	serial_hunt_file = selected["Quest_SimpleSerialHunt.xml"][0]
	talk_file = selected["Quest_SimpleTalk.xml"][0]
	collect_file = selected["Quest_SimpleCollectItem.xml"][0]
	use_item_file = selected["Quest_SimpleUseItem.xml"][0]
	item_play_file = selected["Quest_SimpleItemPlay.xml"][0]
	work_order_file = selected["Quest_CombineTask.xml"][0]
	data_driven_file = selected["data_driven_quest.xml"][0]
	npc_file = selected["npcs.xml"][0]
	item_file = selected["Items.xml"][0]
	retail_quest_file = selected["quest.xml"][0]
	recipe_file = selected["combine_recipe.xml"][0]
	source_manifest = [
		describe_source(retail, name, path, file_region)
		for name, (path, file_region) in selected.items()
	]
	action_coverage = data_driven_action_coverage(data_driven_file)
	shape_summary, shape_details = data_driven_shape_audit(data_driven_file)
	enabled_ids = current_quest_ids(quest_data)
	java_handler_ids = existing_java_handler_ids()
	xml_owned_ids = enabled_ids - java_handler_ids
	legacy_ids = set(legacy_xml_definitions(legacy_dir))
	client_ids = validate_client_quest_coverage(enabled_ids | java_handler_ids | legacy_ids, client_quest) if client_quest is not None else None
	simple_hunts, hunt_stats, hunt_skipped = retail_hunts(hunt_file, xml_owned_ids)
	talks, talk_stats, talk_skipped = simple_talks(talk_file, xml_owned_ids)
	collects, collect_stats, collect_skipped = simple_collects(collect_file, xml_owned_ids)
	use_items, use_item_data_quests, use_item_stats, use_item_skipped = simple_use_items(use_item_file, xml_owned_ids)
	simple_item_play_quests = simple_item_plays(item_play_file, xml_owned_ids)
	data_talks, data_talk_candidates, data_talk_missing_base = data_driven_talks(data_driven_file, xml_owned_ids)
	data_hunts, data_hunt_candidates, data_hunt_missing_base = data_driven_hunts(data_driven_file, xml_owned_ids)
	data_collects, data_collect_candidates, data_collect_missing_base = data_driven_collects(data_driven_file, xml_owned_ids)
	pvps, pvp_candidates, pvp_missing_base = data_driven_pvps(data_driven_file, xml_owned_ids)
	data_item_plays, data_item_play_candidates, data_item_play_missing_base = data_driven_item_plays(data_driven_file, xml_owned_ids)
	faction_quest_file = selected["npcfactions_quest.xml"][0]
	retail_faction_quests = {int(node.attrib["quest_id"]) for node in ET.parse(faction_quest_file).getroot() if node.attrib.get("quest_id", "").isdigit()}
	quest_factions = {int(node.attrib["id"]): int(node.attrib["npcfaction_id"]) for node in ET.parse(quest_data).getroot().findall("quest") if node.attrib.get("npcfaction_id", "").isdigit()}
	faction_ids = {int(node.attrib["id"]) for node in ET.parse(ROOT / "src/main/resources/aion/data/static_data/npc_factions/npc_factions.xml").getroot() if node.attrib.get("id", "").isdigit()}
	faction_closed = {quest_id for quest_id in retail_faction_quests if quest_factions.get(quest_id) in faction_ids}
	challenge_file = selected["challenge_task.xml"][0]
	retail_challenge_quests = {int(node.text) for node in ET.parse(challenge_file).getroot().iter("quest_id") if (node.text or "").strip().isdigit()}
	aion_challenge_quests = {int(node.attrib["id"]) for node in ET.parse(ROOT / "src/main/resources/aion/definitions/compact/quests/challenge_tasks.xml").getroot().iter("quest") if node.attrib.get("id", "").isdigit()}
	challenge_closed = retail_challenge_quests & aion_challenge_quests
	owners, npc_alias_owners = reference_owners(reference_graph) if reference_graph else ({}, {})
	if reference_graph:
		faction_closed &= owners.get("faction", set())
		challenge_closed &= owners.get("challenge_task", set())
	for quests, stats_map, skipped_map in ((simple_hunts, hunt_stats, hunt_skipped), (collects, collect_stats, collect_skipped)):
		resolve_sentinel_starts(quests, stats_map, skipped_map, "_faction_", faction_closed, "blocked_faction_binding")
		resolve_sentinel_starts(quests, stats_map, skipped_map, "_challengetask_", challenge_closed, "blocked_challenge_binding")
		resolve_sentinel_starts(quests, stats_map, skipped_map, "_area_", owners.get("area", set()), "blocked_area_binding")
	# PVP 无独立账本：闭包成立则置空 start，否则保持哨兵名（unresolved 报告原因可见）。
	for quest_id, pvp in pvps.items():
		if pvp.get("start") == "_challengetask_" and quest_id in challenge_closed:
			pvp["start"] = None
	ctx = SimpleNamespace(retail_quest=retail_quest_file, talk=talk_file, use_item=use_item_file,
		item_play=item_play_file, hunt=hunt_file, serial_hunt=serial_hunt_file, npc=npc_file,
		item=item_file, script=retail_script, quest_data=quest_data, enabled=xml_owned_ids)
	generic_by_sink: dict[str, dict[int, dict[str, object]]] = {"simple": {}, "hunt": {}, "data": {}}
	for default_sink, quests in (("hunt", simple_hunts), ("simple", talks), ("simple", collects),
			("data", simple_item_play_quests), ("simple", data_talks), ("hunt", data_hunts), ("simple", data_collects)):
		for quest_id, quest in quests.items():
			if quest_id not in legacy_ids:
				generic_by_sink[compiled_sink(quest, default_sink)][quest_id] = quest
	compiled_results: dict[str, dict[int, dict[str, object]]] = {}
	compiled_skipped: dict[str, list[str]] = {}
	generic_reclaims: dict[str, list[int]] = {}
	bucket_families: dict[str, dict[str, dict[int, dict[str, object]]]] = {"simple": {}, "hunt": {}, "data": {}}
	for label, _, default_sink, _, build in COMPILED_FAMILIES:
		result, family_skipped = build(ctx)
		result, reclaimed = reclaim_generic_projections(result, generic_by_sink, default_sink)
		compiled_results[label] = result
		if reclaimed:
			generic_reclaims[label] = reclaimed
		compiled_skipped.update(family_skipped)
		for quest_id, quest in result.items():
			bucket_families[compiled_sink(quest, default_sink)].setdefault(label, {})[quest_id] = quest
	compiled_ids = set().union(*(set(result) for result in compiled_results.values()))
	talks = {quest_id: quest for quest_id, quest in talks.items() if quest_id not in legacy_ids | compiled_ids}
	reclaim_targets = {"talk": (talk_skipped, talk_stats), "use_item": (use_item_skipped, use_item_stats), "hunt": (hunt_skipped, hunt_stats)}
	for label, _, _, reclaim_spec, _ in COMPILED_FAMILIES:
		for bucket_name, mode in reclaim_spec.items():
			skipped_map, stats_map = reclaim_targets[bucket_name]
			for quest_id in compiled_results[label]:
				if skipped_map["unsupported"].pop(str(quest_id), None) is not None:
					stats_map["unsupported"] -= 1
				if mode == "full" and quest_id in skipped_map["invalid"]:
					skipped_map["invalid"].remove(quest_id)
					stats_map["invalid"] -= 1
	work_orders, work_order_stats, work_order_skipped = retail_work_orders(
		work_order_file, retail_quest_file, quest_data, npc_file, item_file, recipe_file, xml_owned_ids)
	handled_ids = set(simple_hunts) | set(talks) | set(collects) | set(use_items) | set(use_item_data_quests) | set(simple_item_play_quests) | set(data_talks) | set(data_hunts) | set(data_collects) | set(pvps) | set(data_item_plays) | compiled_ids | set(work_orders)
	data_quests = data_driven_complex(data_driven_file, xml_owned_ids, handled_ids)
	for family in bucket_families["data"].values():
		data_quests.update(family)
	simple_data_quests = {quest_id: quest for quest_id, quest in talks.items() if quest["kind"] == "data_driven_simple"}
	simple_data_quests.update({quest_id: quest for quest_id, quest in data_talks.items() if quest["kind"] == "data_driven_simple"})
	simple_data_quests.update(simple_item_play_quests)
	simple_data_quests.update(use_item_data_quests)
	data_quests.update(simple_data_quests)
	families = (set(simple_hunts), set(talks), set(collects), set(use_items), set(use_item_data_quests), set(simple_item_play_quests), set(data_talks), set(data_hunts), set(data_collects), set(pvps), set(data_item_plays),
		*(set(family) for bucket in ("simple", "hunt") for family in bucket_families[bucket].values()),
		set(data_quests) - set(simple_data_quests), set(work_orders))
	duplicates = set().union(*(left & right for index, left in enumerate(families) for right in families[index + 1:]))
	if duplicates:
		raise ValueError(f"duplicate retail quest ids: {sorted(duplicates)}")
	hunts = {**simple_hunts, **data_hunts}
	for family in bucket_families["hunt"].values():
		hunts.update(family)
	simple = {**{quest_id: quest for quest_id, quest in talks.items() if quest["kind"] != "data_driven_simple"}, **collects,
		**{quest_id: quest for quest_id, quest in data_talks.items() if quest["kind"] != "data_driven_simple"},
		**data_collects}
	for family in bucket_families["simple"].values():
		simple.update(family)
	use_items.update(data_item_plays)
	exact_names = {str(hunt[key]) for hunt in hunts.values() for key in ("start", "end") if hunt.get(key)}
	target_names: set[str] = set()
	for hunt in hunts.values():
		for targets, _ in hunt["stages"]:  # type: ignore[assignment]
			target_names.update(targets)
	for quest in simple.values():
		if quest.get("start"):
			exact_names.add(str(quest["start"]))
		exact_names.add(str(quest["end"]))
		exact_names.update(map(str, quest.get("objects", [])))
		exact_names.update(map(str, quest.get("talks", [])))
	for quest in use_items.values():
		exact_names.add(str(quest["end"]))
		exact_names.update(map(str, quest["talks"]))
	for quest in pvps.values():
		exact_names.update((str(quest["start"]), str(quest["end"])))
	for quest in data_quests.values():
		if quest.get("end"):
			exact_names.add(str(quest["end"]))
		if quest.get("start") and quest["start_type"] in ("TALK", "SENSORY_COMPLETE"):
			exact_names.add(str(quest["start"]))
		for step in quest["steps"]:
			exact_names.update(map(str, step.get("names", [])))
			exact_names.update(map(str, step.get("actions", [])))
			exact_names.update(str(spawn[0]) for spawn in step.get("spawns", []))
	exact_index, aliases = npc_indexes(npc_file, exact_names, target_names)
	for quest_id, (name, ids) in npc_alias_owners.items():
		quest = use_items.get(quest_id)
		if quest is None:
			continue
		resolved = exact_index.get(name) or aliases.get(name, ())
		if str(quest["end"]).casefold() != name or resolved != ids:
			raise ValueError(f"NPC alias reference drift for quest {quest_id}")
		quest["end_ids"] = ids
	item_names = {str(quest["item"]) for quest in use_items.values()}
	for quest in simple.values():
		if quest.get("item"):
			item_names.add(str(quest["item"][0]))
		if quest.get("evidence_item"):
			item_names.add(str(quest["evidence_item"]))
	for quest in data_quests.values():
		if quest.get("start") and quest["start_type"] == "ITEM_PLAY":
			item_names.add(str(quest["start"]))
		if quest.get("start_give_item"):
			item_names.add(str(quest["start_give_item"][0]))
		if quest.get("start_remove_item"):
			item_names.add(str(quest["start_remove_item"][0]))
		for step in quest["steps"]:
			if step.get("item"):
				item_names.add(str(step["item"]))
			if step.get("give_item"):
				item_names.add(str(step["give_item"][0]))
			if step.get("remove_item"):
				item_names.add(str(step["remove_item"][0]))
	items = item_ids(item_file, item_names)
	script_sources = tuple(path for number in sorted({number for _, numbers, *_ in COMPILED_FAMILIES for number in numbers}) if (path := retail_script / f"fun_{number:03d}.cpp").is_file())
	sources = (hunt_file, serial_hunt_file, talk_file, collect_file, use_item_file, item_play_file, work_order_file,
		data_driven_file, npc_file, item_file, retail_quest_file, recipe_file, quest_data, *script_sources)
	if reference_graph:
		sources += (reference_graph,)
	content, unresolved, generated = render(
		simple, hunts, pvps, use_items, data_quests, exact_index, aliases, items,
		sources,
		work_orders,
	)
	generated_ids = {int(node.attrib["id"]) for node in ET.fromstring(content.split(b"-->\n", 1)[1])}
	unsupported_ids = {
		int(quest_id)
		for skipped in (hunt_skipped, talk_skipped, collect_skipped, use_item_skipped)
		for quest_id in skipped["unsupported"]
	}
	legacy_details, legacy_stats = audit_legacy_xml(
		legacy_dir, enabled_ids, generated_ids, java_handler_ids, retail_source_ids(retail, region),
		{int(quest_id) for quest_id in unresolved}, unsupported_ids,
		{int(quest_id) for skipped in (hunt_skipped, talk_skipped, collect_skipped, use_item_skipped) for quest_id in skipped["invalid"]} | {int(quest_id) for quest_id in work_order_skipped},
	)
	java_details, java_stats = audit_java_handlers(enabled_ids)
	constraint_ids = {
		int(quest_id) for quest_id, detail in java_details.items()
		if detail["disposition"] == "retained_authoritative_gap"
	}
	constraint_dispositions: dict[str, int] = {}
	for quest_id, evidence in compiled_script_evidence(retail_script, constraint_ids).items():
		java_details[str(quest_id)].update(evidence)
		disposition = str(evidence["constraint_disposition"])
		constraint_dispositions[disposition] = constraint_dispositions.get(disposition, 0) + 1
	java_stats["constraint_dispositions"] = constraint_dispositions
	if java_stats["base_handlers"] != len(enabled_ids & java_handler_ids):
		raise ValueError("Java quest handler audit does not match ownership")
	selected_xml_ids = generated_ids | {int(quest_id) for quest_id in legacy_details}
	overlap = selected_xml_ids & java_handler_ids
	if overlap:
		raise ValueError(f"quest ids have both XML and Java owners: {sorted(overlap)}")
	executable_ids = enabled_ids & (existing_handler_ids(legacy_dir) | generated_ids)
	retail_ids = {int(node.findtext("id")) for node in ET.parse(retail_quest_file).getroot() if (node.findtext("id") or "").isdigit()}
	base_nodes = {int(node.attrib["id"]): node for node in ET.parse(quest_data).getroot().findall("quest")}
	isolated = {}
	for quest_id in sorted(enabled_ids - executable_ids):
		if quest_id not in retail_ids:
			reason = "missing_retail_quest"
		elif base_nodes[quest_id].attrib.get("minlevel_permitted") == "999":
			reason = "disabled_level_999"
		else:
			reason = "compiled_retail_script_unavailable"
		isolated[str(quest_id)] = reason
	stats = {
		"authority": {
			"data_root": str(retail.expanduser().resolve()),
			"script_root": str(retail_script.expanduser().resolve()),
			"region": region,
			"sources": source_manifest,
		},
		"client_authority": None if client_ids is None else {
			"quest_ids": len(client_ids), "sha256": sha256(client_quest), "server_outside": 0,
		},
			"data_driven_actions": action_coverage,
			"data_driven_shapes": shape_summary,
			"generic_reclaims": generic_reclaims,
		"retail": {"hunt": hunt_stats["retail"], "talk": talk_stats["retail"], "collect": collect_stats["retail"], "use_item": use_item_stats["retail"], "work_order": work_order_stats["retail"], **data_talk_candidates, "data_driven_hunt": data_hunt_candidates, "data_driven_collect": data_collect_candidates, "data_driven_pvp": pvp_candidates, "data_driven_item_play": data_item_play_candidates},
		"eligible": {"hunt": len(simple_hunts), "talk": len(talks), "collect": len(collects), "use_item": len(use_items) - len(data_item_plays), "work_order": len(work_orders), **{source: sum(quest.get("source") == source for quest in data_talks.values()) for source in data_talk_candidates}, "data_driven_hunt": len(data_hunts), "data_driven_collect": len(data_collects), "data_driven_pvp": len(pvps), "data_driven_item_play": len(data_item_plays), "data_driven_complex": len(data_quests) - sum(len(family) for family in bucket_families["data"].values()), **{label: len(compiled_results[label]) for label, *_ in COMPILED_FAMILIES}},
		"generated": {**generated, "total": sum(generated.values())},
		"missing_base": hunt_stats["missing_base"] + talk_stats["missing_base"] + collect_stats["missing_base"] + use_item_stats["missing_base"] + work_order_stats["missing_base"] + data_talk_missing_base + data_hunt_missing_base + data_collect_missing_base + pvp_missing_base + data_item_play_missing_base,
		"unsupported": hunt_stats["unsupported"] + talk_stats["unsupported"] + collect_stats["unsupported"] + use_item_stats["unsupported"],
		"invalid": hunt_stats["invalid"] + talk_stats["invalid"] + collect_stats["invalid"] + use_item_stats["invalid"] + work_order_stats["invalid"],
		"unresolved": len(unresolved),
		"coverage": {"base": len(enabled_ids), "executable": len(executable_ids), "isolated": len(isolated), "managed": len(executable_ids) + len(isolated)},
			"legacy_xml": legacy_stats,
			"java_handlers": java_stats,
			"ownership": {"generated_xml": len(generated_ids), "legacy_xml": len(legacy_details), "selected_xml": len(selected_xml_ids), "java_handlers": len(enabled_ids & java_handler_ids), "overlap": len(overlap)},
	}
	skipped = {
		"unsupported": {
			"hunt": hunt_skipped["unsupported"], "talk": talk_skipped["unsupported"], "collect": collect_skipped["unsupported"], "use_item": use_item_skipped["unsupported"],
		},
		"invalid": {
			"hunt": hunt_skipped["invalid"], "talk": talk_skipped["invalid"], "collect": collect_skipped["invalid"], "use_item": use_item_skipped["invalid"], "work_order": work_order_skipped,
		},
			"compiled_script": dict(compiled_skipped),
			"compiled_script_isolation_details": {str(quest_id): COMPILED_ISOLATION_DETAILS[quest_id] for quest_id in sorted(COMPILED_ISOLATION_DETAILS) if str(quest_id) in isolated},
			"java_handlers": java_details,
			"legacy_xml": legacy_details,
		"unresolved": unresolved,
		"isolated": isolated,
	}
	return content, {"stats": stats, "skipped": skipped, "data_driven_shapes": shape_details}


def main() -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("--retail", type=Path, default=DEFAULT_RETAIL)
	parser.add_argument("--region", default=DEFAULT_RETAIL_REGION)
	parser.add_argument("--retail-script", type=Path, default=DEFAULT_RETAIL_SCRIPT)
	parser.add_argument("--client-quest", type=Path, default=DEFAULT_CLIENT_QUEST)
	parser.add_argument("--quest-data", type=Path, default=DEFAULT_QUEST_DATA)
	parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
	parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
	parser.add_argument("--reference-graph", type=Path, default=DEFAULT_REFERENCE_GRAPH)
	parser.add_argument("--check", action="store_true")
	args = parser.parse_args()
	content, report = generate(args.retail, args.quest_data, args.retail_script, args.output.parent,
		args.client_quest, args.region, args.reference_graph)
	report["output_sha256"] = hashlib.sha256(content).hexdigest()
	report_content = (json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n").encode()
	if args.check:
		if not args.output.is_file() or args.output.read_bytes() != content or not args.report.is_file() or args.report.read_bytes() != report_content:
			raise SystemExit(f"stale generated file: {args.output}")
	else:
		args.output.parent.mkdir(parents=True, exist_ok=True)
		args.output.write_bytes(content)
		args.report.write_bytes(report_content)
	print(json.dumps(report["stats"], ensure_ascii=False, sort_keys=True))
	return 0


if __name__ == "__main__":
	raise SystemExit(main())
