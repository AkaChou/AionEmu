package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.instance.playerreward.EngulfedOphidanBridgePlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.EvergaleCanyonPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.KamarBattlefieldPlayerReward;

class BattlefieldInstanceMigrationTest {
	@Test
	void restoresPersistedParticipationStart() throws Exception {
		assertNotNull(KamarBattlefieldPlayerReward.class
			.getConstructor(int.class, byte.class, Race.class, long.class));
		assertNotNull(EngulfedOphidanBridgePlayerReward.class
			.getConstructor(int.class, byte.class, Race.class, long.class));
		assertNotNull(EvergaleCanyonPlayerReward.class
			.getConstructor(int.class, byte.class, Race.class, long.class));
	}

	@Test
	void kamarUsesRetailBattlefieldDataAndPersistentLifecycle() throws Exception {
		String handler = handler("KamarBattlefieldInstance");
		assertTrue(handler.contains("scheduleDeadline(\"preparation\""));
		assertTrue(handler.contains("scheduleDeadline(\"battle\""));
		assertTrue(handler.contains("scheduleDeadline(\"exit\""));
		assertTrue(handler.contains("runtimeState().put(STATE_PREFIX + \"phase\""));
		assertTrue(handler.contains("InstanceSettlementService.queueBattleground"));
		assertTrue(handler.contains("DataManager.RETAIL_AI_DATA.getNpcScore"));
		assertTrue(handler.contains("case 730861, 730878, 801766, 801767, 801818, 801819, 801820, 801821 -> true"));
		assertTrue(handler.contains("npc.getSpawn().getStableKey()"));
		assertFalse(handler.contains("case 232855, 232856"));
		assertFalse(handler.contains("Future<?>"));
		assertFalse(handler.contains("GameThreadPoolServices"));
		assertFalse(handler.contains("spawn("));

		String firstScore = KamarBattlefieldInstance.scoreEventKey("static:301120000:730861:1", 100);
		assertEquals(firstScore, KamarBattlefieldInstance.scoreEventKey("static:301120000:730861:1", 101));
		assertFalse(firstScore.equals(KamarBattlefieldInstance.scoreEventKey("static:301120000:730861:2", 100)));
		assertEquals("kamar.score.event.object.100", KamarBattlefieldInstance.scoreEventKey(null, 100));

		String rewards = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/rewards.xml"));
		assertTrue(rewards.contains("base_score=\"3600\" door_id=\"1\" id=\"1\" limit_time=\"1800\""));
		assertTrue(rewards.contains("pc_die_score=\"100\" pc_kill_score=\"150\""));
		assertTrue(rewards.contains("score_limit_gap=\"20000\" score_limit_max=\"100000\""));

		String scores = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-scores.xml"));
		assertTrue(scores.contains("npc_id=\"232841\" name=\"IDKamar_DrakanFi_N_65_Ae\""));
		assertTrue(scores.contains("npc_id=\"233327\" name=\"IDKamar_LightGeneral_Nmd_D1_65_Ah\""));

		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301120000_Kamar_Battlefield.xml"));
		for (String npcId : new String[] { "232841", "232847", "233260", "701806", "701906", "801771" }) {
			assertTrue(spawns.contains("npc_id=\"" + npcId + "\""));
		}
	}

	@Test
	void ophidanWarpathUsesRetailAiAndPersistentLifecycle() throws Exception {
		String handler = handler("OphidanWarpathInstance");
		assertTrue(handler.contains("scheduleDeadline(\"preparation\""));
		assertTrue(handler.contains("scheduleDeadline(\"battle\""));
		assertTrue(handler.contains("scheduleDeadline(\"exit\""));
		assertTrue(handler.contains("InstanceSettlementService.queueBattleground"));
		assertTrue(handler.contains("DataManager.RETAIL_AI_DATA.getNpcScore"));
		assertTrue(handler.contains("npcId == 833935 || npcId == 833936 || npcId == 833961"));
		assertFalse(handler.contains("Future<?>"));
		assertFalse(handler.contains("GameThreadPoolServices"));
		assertFalse(handler.contains("spawn("));
		assertFalse(handler.contains("onEnterZone"));

		String rewards = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/rewards.xml"));
		assertTrue(rewards.contains("name=\"IDLDF5_Under_02_War\" pc_die_score=\"0\" pc_kill_score=\"75\""));
		assertTrue(rewards.contains("score_limit_gap=\"23000\" score_limit_max=\"60000\""));

		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_new_pvp_battleground_.xml"));
		assertTrue(patterns.contains("<name>IDLDF5_Under_01_war_N_TalkingMach_01_Li</name>"));
		assertTrue(patterns.contains("<string>Li_Win_01</string>"));
		assertTrue(patterns.contains("<give_score><target>USERI_TALKER</target></give_score>"));
	}

	@Test
	void engulfedOphidanUsesRetailBattlefieldDataAndPersistentLifecycle() throws Exception {
		String handler = handler("EngulfedOphidanBridgeInstance");
		assertTrue(handler.contains("scheduleDeadline(\"preparation\""));
		assertTrue(handler.contains("scheduleDeadline(\"battle\""));
		assertTrue(handler.contains("scheduleDeadline(\"exit\""));
		assertTrue(handler.contains("InstanceSettlementService.queueBattleground"));
		assertTrue(handler.contains("DataManager.RETAIL_AI_DATA.getNpcScore"));
		assertTrue(handler.contains("scheduleBombardment"));
		assertTrue(handler.contains("spawn(855240"));
		assertTrue(handler.contains("decreaseByItemId(164000277, 1)"));
		assertTrue(handler.contains("decreaseByItemId(164000278, 1)"));
		assertFalse(handler.contains("Future<?>"));
		assertFalse(handler.contains("GameThreadPoolServices"));
		assertFalse(handler.contains("onDropRegistered"));
		assertFalse(handler.contains("onEnterZone"));
		assertFalse(handler.contains("powerGenerator"));
		assertFalse(handler.contains("spawn(233"));

		String definitions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String engulfed = worldConditions(definitions, 301210000);
		assertEquals(37, occurrences(engulfed, "<variable "));
		assertEquals(90, occurrences(engulfed, "<condition "));
		assertEquals(90, occurrences(engulfed, "<slot>"));
		assertEquals(90, occurrences(engulfed, "<npc "));
		assertTrue(engulfed.contains("<npc id=\"233491\""));

		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301210000_Engulfed_Ophidan_Bridge.xml"));
		for (String npcId : new String[] { "233473", "233856", "701945", "701947", "802044" }) {
			assertTrue(spawns.contains("npc_id=\"" + npcId + "\""), npcId);
		}

		String rewards = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/rewards.xml"));
		assertTrue(rewards.contains("base_score=\"0\" door_id=\"1\" id=\"2\" limit_time=\"1200\""));
		assertTrue(rewards.contains("pc_die_score=\"0\" pc_kill_score=\"300\""));
		assertTrue(rewards.contains("score_limit_gap=\"30000\" score_limit_max=\"200000\""));
		assertTrue(rewards.contains("wait_time=\"120\" wait_time_after_noenemy=\"0\""));

		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_new_pvp_battleground_.xml"));
		assertTrue(patterns.contains("<name>IDLDF5_Under_01_war_N_ControlNPC_01</name>"));
		assertTrue(patterns.contains("<string>Li_Win_01</string>"));
		assertTrue(patterns.contains("<string>Da_Win_01</string>"));

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		for (String npcId : new String[] { "701974", "701975", "701976" }) {
			assertTrue(drops.contains("<npc_drop npc_id=\"" + npcId + "\">"));
		}
	}

	@Test
	void ironWallUsesRetailPopulationAndPersistentLifecycle() throws Exception {
		String handler = handler("IronWallWarfrontInstance");
		for (String required : new String[] {
			"scheduleDeadline(\"adjustment\"", "scheduleDeadline(\"preparation\"",
			"scheduleDeadline(\"battle\"", "scheduleDeadline(\"exit\"",
			"InstanceSettlementService.queueBattleground", "consumeNpcScore", "scoreEventKey",
			"setDoorState(doorId, true)", "removeItems(player)"
		}) {
			assertTrue(handler.contains(required), required);
		}
		for (String legacy : new String[] { "Future<", "GameThreadPoolServices", "SpawnEngine", "spawn(" }) {
			assertFalse(handler.contains(legacy), legacy);
		}
		assertTrue(method(handler, "onLeaveInstance").contains("removeItems(player)"));
		assertFalse(method(handler, "onPlayerLogOut").contains("removeItems(player)"));

		String first = IronWallWarfrontInstance.scoreEventKey("condition:7582:generation.1", 1);
		assertEquals(first, IronWallWarfrontInstance.scoreEventKey("condition:7582:generation.1", 2));
		assertFalse(first.equals(IronWallWarfrontInstance.scoreEventKey("condition:7582:generation.2", 1)));

		String definitions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String ironWall = worldConditions(definitions, 301220000);
		assertEquals(28, occurrences(ironWall, "<variable "));
		assertEquals(486, occurrences(ironWall, "<condition "));
		assertEquals(486, occurrences(ironWall, "<slot>"));
		assertEquals(542, occurrences(ironWall, "<npc "));
		assertTrue(ironWall.contains("<npc id=\"233544\""));
		assertTrue(ironWall.contains("<npc id=\"233548\""));

		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301220000_Iron_Wall_Warfront.xml"));
		for (String npcId : new String[] { "233537", "233564", "233567", "701660", "831328", "831329", "831330" }) {
			assertTrue(spawns.contains("npc_id=\"" + npcId + "\""), npcId);
		}
		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		String ammoBox = drops.substring(drops.indexOf("<npc_drop npc_id=\"831329\""));
		ammoBox = ammoBox.substring(0, ammoBox.indexOf("</npc_drop>"));
		assertTrue(ammoBox.contains("item_id=\"182006996\""));
		assertTrue(ammoBox.contains("item_id=\"182006997\""));
		String items = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
		for (int itemId : new int[] { 185000137, 182006996, 182006997 }) {
			assertTrue(item(items, itemId).contains("ownership_world=\"300540000\""));
		}

		String rewards = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/rewards.xml"));
		assertTrue(rewards.contains("name=\"IDF5_TD_War\" pc_die_score=\"0\" pc_kill_score=\"200\""));
		String scores = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-scores.xml"));
		assertTrue(scores.contains("npc_id=\"233498\" name=\"IDF5_TD_War_Dark_Officer_01_Ae2\""));
	}

	@Test
	void generatedBattlefieldConditionsCoverEvergaleAndEternalBastion() throws Exception {
		String definitions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String evergale = worldConditions(definitions, 302350000);
		for (String variable : new String[] {
			"24_bottom", "24_middle", "24_top", "bottom_boss_kill", "f6_event_kill", "top_boss_kill"
		}) {
			assertTrue(evergale.contains("<variable name=\"" + variable + "\"/>"), variable);
		}
		assertTrue(evergale.contains("source=\"IDEternity_War/world_N.xml#producer-page-1-2\""));
		assertTrue(evergale.contains("<npc id=\"835324\""));

		String eternalBastion = worldConditions(definitions, 300540000);
		assertTrue(eternalBastion.contains("<variable name=\"castle_gate_02_bomb\"/>"));
		assertTrue(eternalBastion.contains("<variable name=\"timewave_down\"/>"));
		assertTrue(eternalBastion.contains("expression=\"castle_gate_02_Bomb == 2\""));
		assertTrue(eternalBastion.contains("expression=\"TimeWave_Down &gt;= 3\""));
		assertTrue(eternalBastion.contains("source=\"idldf5b_td/world_N.xml#15\""));
		assertTrue(eternalBastion.contains("<npc id=\"231113\""));
	}

	@Test
	void evergaleUsesRetailLifecyclePopulationAndSettlement() throws Exception {
		String handler = handler("EvergaleCanyonInstance");
		for (String required : new String[] {
			"scheduleDeadline(\"preparation\"", "scheduleDeadline(\"battle\"",
			"scheduleDeadline(\"noEnemy\"", "scheduleDeadline(\"exit\"",
			"PHASE_NO_ENEMY", "wait_time_after_noenemy", "InstanceSettlementService.queueBattleground",
			"RetailConditionSpawnEngine.setVariable(instance, \"people_expand_con\"",
			"DataManager.RETAIL_AI_DATA.getNpcScore", "npc.getSpawn().getStableKey()",
			"setDoorState(352, true)", "setDoorState(507, true)"
		}) {
			assertTrue(handler.contains(required), required);
		}
		for (String legacy : new String[] {
			"Future<", "GameThreadPoolServices", "onDropRegistered", "handleUseItemFinish", "ItemService",
			"sendMovie", "loosingGroupMultiplier", "stopInstanceTask", " sp(", "spawn("
		}) {
			assertFalse(handler.contains(legacy), legacy);
		}

		int[] thresholds = { 37, 55, 73, 74, 75 };
		assertEquals(0, EvergaleCanyonInstance.populationLevelForCount(36, thresholds));
		assertEquals(1, EvergaleCanyonInstance.populationLevelForCount(37, thresholds));
		assertEquals(3, EvergaleCanyonInstance.populationLevelForCount(73, thresholds));
		assertEquals(5, EvergaleCanyonInstance.populationLevelForCount(96, thresholds));
		assertEquals(73, EvergaleCanyonInstance.populationThresholdForLevel(3, thresholds));
		assertEquals(Race.PC_ALL, EvergaleCanyonInstance.noEnemyWinner(0, 0));
		assertEquals(Race.ELYOS, EvergaleCanyonInstance.noEnemyWinner(1, 0));
		assertEquals(Race.ASMODIANS, EvergaleCanyonInstance.noEnemyWinner(0, 1));
		String first = EvergaleCanyonInstance.scoreEventKey("condition:1:generation.1", 1);
		assertEquals(first, EvergaleCanyonInstance.scoreEventKey("condition:1:generation.1", 2));
		assertFalse(first.equals(EvergaleCanyonInstance.scoreEventKey("condition:1:generation.2", 1)));

		String definitions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String evergale = worldConditions(definitions, 302350000);
		assertEquals(20, occurrences(evergale, "<variable "));
		assertEquals(347, occurrences(evergale, "<condition "));
		assertEquals(1_183, occurrences(evergale, "<slot>"));
		assertEquals(1_197, occurrences(evergale, "<npc "));
		assertFalse(Files.exists(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/302350000_Evergale_Canyon.xml")));

		String rewards = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/rewards.xml"));
		assertTrue(rewards.contains("name=\"IDEternity_War\" pc_die_score=\"0\" pc_kill_score=\"5\""));
		assertTrue(rewards.contains("spawn_page=\"1\" spawn_type=\"0\" wait_time=\"180\" wait_time_after_noenemy=\"60\""));
		assertTrue(rewards.contains("name=\"IDEternity_War_SP\" pc_die_score=\"0\" pc_kill_score=\"5\""));
		assertTrue(rewards.contains("spawn_page=\"2\" spawn_type=\"0\" wait_time=\"180\" wait_time_after_noenemy=\"60\""));

		String reward = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/model/instance/instancereward/EvergaleCanyonReward.java"));
		assertFalse(reward.contains("instanceTime"));
		assertFalse(reward.contains("capPoints"));

		String quests = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/quests/scripts/zz_retail_simple_quests.xml"));
		for (String questId : new String[] { "13962", "23962" }) {
			String quest = questDefinition(quests, questId);
			assertTrue(quest.contains("<step type=\"ENTER_AREA\" ids=\"835385\""));
			for (String legacyTarget : new String[] { "835447", "835474", "835476", "835478", "835480" }) {
				assertFalse(quest.contains(legacyTarget), questId + ':' + legacyTarget);
			}
		}
	}

	private static String questDefinition(String definitions, String questId) {
		int start = definitions.indexOf("id=\"" + questId + "\"");
		int end = definitions.indexOf("</data_driven_quest>", start);
		return definitions.substring(start, end);
	}

	private static String worldConditions(String definitions, int worldId) {
		int start = definitions.indexOf("<world id=\"" + worldId + "\"");
		int end = definitions.indexOf("</world>", start);
		return definitions.substring(start, end);
	}

	private static String handler(String name) throws Exception {
		return Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/" + name + ".java"));
	}

	private static String method(String source, String name) {
		int start = source.indexOf(name + "(");
		return source.substring(start, source.indexOf("\n\t}", start));
	}

	private static String item(String source, int itemId) {
		int start = source.indexOf("<item_template id=\"" + itemId + "\"");
		return source.substring(start, source.indexOf("</item_template>", start));
	}

	private static int occurrences(String value, String needle) {
		return (value.length() - value.replace(needle, "").length()) / needle.length();
	}
}
