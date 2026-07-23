package com.aionemu.gameserver.instance.handlers.scripts.dredgion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BaranathDredgionRetailMigrationTest {

	@Test
	void retailDataOwnsBaranathConditionsNpcAiItemsAndPatrol() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "300110000");
		assertEquals(7, count(world, "<variable "));
		assertEquals(17, count(world, "<condition "));
		assertEquals(25, count(world, "respawn_time=\"120\""));
		assertEquals(25, count(world, "despawn_at_attack_state=\"true\""));
		for (String variable : new String[] { "idab1_dreadgion_teleport_17minuteslater", "surkana_8",
				"switch_1_destroyed", "switch_2_destroyed", "teleport_1_destroyed",
				"teleport_2_destroyed", "teleport_3_destroyed" }) {
			assertTrue(world.contains("<variable name=\"" + variable + "\"/>"), variable);
		}

		String[][] conditional = {
			{ "1", "TELEPORT_1_DESTROYED == 1", "730187", "398.361389", "160.270599" },
			{ "2", "IDAB1_Dreadgion_TELEPORT_17MINUTESLATER == 1", "730214", "567.360168", "175.282623" },
			{ "3", "SWITCH_1_DESTROYED == 1", "700501", "448.539429", "493.63681" },
			{ "4", "IDAB1_Dreadgion_TELEPORT_17MINUTESLATER == 1", "730213", "402.332336", "175.003662" },
			{ "5", "TELEPORT_3_DESTROYED == 1", "730197", "484.948883", "761.510498" },
			{ "6", "TELEPORT_2_DESTROYED == 1", "730188", "571.886963", "160.790802" },
			{ "7", "Surkana_8 &gt;= 5", "214823", "485.211609", "807.46875" },
			{ "8", "SWITCH_2_DESTROYED == 1", "700502", "521.112915", "493.670349" }
		};
		for (String[] expected : conditional) {
			String condition = conditionBlock(world, "idab1_dreadgion/world_N.xml#" + expected[0]);
			assertTrue(condition.contains("expression=\"" + expected[1] + "\""), expected[0]);
			assertTrue(condition.contains("id=\"" + expected[2] + "\" probability=\"10000\" x=\""
				+ expected[3] + "\" y=\"" + expected[4] + "\""), expected[2]);
		}

		String supply = conditionBlock(world, "idab1_dreadgion/world_N.xml#unconditional-random-1");
		assertTrue(supply.contains("id=\"215391\" probability=\"8750\" x=\"415.276886\" y=\"282.021606\""));
		assertTrue(supply.contains("id=\"215391\" probability=\"1250\" x=\"556.535339\" y=\"279.291809\""));

		assertPair(world, 2, "798327", "798328", "380.00824", "697.24823");
		assertPair(world, 3, "798325", "798326", "589.893555", "697.263428");
		assertPair(world, 4, "798329", "798330", "591.781738", "711.621399");
		assertTrue(conditionBlock(world, "idab1_dreadgion/world_N.xml#unconditional-random-5")
			.contains("id=\"215093\" probability=\"10000\" x=\"485.417267\" y=\"319.098633\""));
		String inspector = conditionBlock(world, "idab1_dreadgion/world_N.xml#unconditional-random-6");
		assertTrue(inspector.contains("id=\"215390\" probability=\"10000\" x=\"460.611206\" y=\"877.78595\""));
		assertTrue(inspector.contains("walker=\"retail:300110000:path_idab1_drd_17\""));
		assertTrue(conditionBlock(world, "idab1_dreadgion/world_N.xml#unconditional-random-7")
			.contains("id=\"215427\" probability=\"10000\" x=\"504.50238\" y=\"607.622375\""));

		String party = conditionBlock(world, "idab1_dreadgion/world_N.xml#unconditional-random-8");
		assertEquals(2, count(party, "<party probability=\"5000\""));
		assertTrue(party.contains("id=\"798323\" probability=\"10000\" x=\"377.175385\" y=\"704.659302\""));
		assertTrue(party.contains("id=\"798327\" probability=\"10000\" x=\"592.777283\" y=\"704.227356\""));
		assertTrue(party.contains("id=\"798328\" probability=\"10000\" x=\"377.175385\" y=\"704.659302\""));
		assertTrue(party.contains("id=\"798324\" probability=\"10000\" x=\"592.777283\" y=\"704.227356\""));
		assertPair(world, 9, "798323", "798324", "378.52005", "712.347778");

		String waypoints = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/ai-waypoints.xml"));
		String patrol = walkerBlock(waypoints, "retail:300110000:path_idab1_drd_17");
		assertEquals(24, count(patrol, "<routestep "));
		assertTrue(patrol.contains("step=\"12\" x=\"509.897339\" y=\"877.986755\" z=\"408.000061\" rest_time=\"3000\""));
		assertTrue(patrol.contains("step=\"24\" x=\"460.804565\" y=\"878.160706\" z=\"408.090607\" rest_time=\"3000\""));

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300110000_Baranath_Dredgion.xml"));
		for (String npcId : new String[] { "215427", "798323", "798324", "798325", "798326",
				"798327", "798328", "798329", "798330" }) {
			assertFalse(staticSpawns.contains("<spawn npc_id=\"" + npcId + "\""), npcId);
		}

		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/dredgion/BaranathDredgion.java"));
		assertTrue(handler.contains("scheduleDeadline(\"teleport\", startedAt + 1_020_000"));
		assertTrue(handler.contains("sendMsgByRace(1400265, Race.PC_ALL, 0)"));
		for (String variable : new String[] { "idab1_dreadgion_teleport_17minuteslater",
				"teleport_1_destroyed", "teleport_2_destroyed", "teleport_3_destroyed",
				"switch_1_destroyed", "switch_2_destroyed" }) {
			assertTrue(handler.contains("\"" + variable + "\", 1, 0"), variable);
		}
		assertTrue(handler.contains("RetailConditionSpawnEngine.clear(instance)"));
		for (String legacy : new String[] { "Rnd", "spawnOpeningNamed", "activateCentralTeleporters",
				"opening_spawned", "opening_captain", "captain_teleporter", "supply_port",
				"supply_starboard", "bulkhead_named", "secret_cache", "secret_chest", "spawn(701455",
				"spawn(730187", "spawn(730188",
				"spawn(730197", "spawn(730213", "spawn(730214", "case 215427:", "spawn(" }) {
			assertFalse(handler.contains(legacy), legacy);
		}

		String npcAi = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-ai.xml"));
		assertTrue(npcAi.contains("<npc id=\"214823\" name=\"IDAb1_Dreadgion_DrakanBoss_50_Ah\" ai=\"Dread_DrakanBoss\""));
		assertTrue(npcAi.contains("<npc id=\"215085\" name=\"IDAb1_Dreadgion_DrakanMaNamed_50_Ae\" ai=\"Dread_SurkanaNm05\""));
		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns.xml"));
		assertTrue(patterns.contains("<name>Dread_DrakanBoss</name>"));
		assertTrue(patterns.contains("<name>Dread_SurkanaNm05</name>"));

		String quests = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/quests/quest_data.xml"));
		assertTrue(quests.contains("<quest_drop npc_id=\"215391\" item_id=\"182202185\" chance=\"100\" drop_each_member=\"1\"/>"));
		assertTrue(quests.contains("<quest_drop npc_id=\"215391\" item_id=\"182205682\" chance=\"100\" drop_each_member=\"1\"/>"));
		String items = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_etc_templates.xml"));
		assertTrue(items.contains("<item_template id=\"182202185\" name=\"Surkanate\""));
		assertTrue(items.contains("<item_template id=\"182205682\" name=\"Surkanate\""));
		for (String quest : new String[] { "_3711To_Kill_A_Captain", "_4711The_Dredgion_Captain" }) {
			String questHandler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/quest/handlers/baranath_dredgion/" + quest + ".java"));
			assertTrue(questHandler.contains("registerQuestNpc(730196).addOnTalkEvent"), quest);
			assertTrue(questHandler.contains("defaultOnKillEvent(env, 214823, 2, true)"), quest);
		}

		String scores = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-scores.xml"));
		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_005.xml"));
		for (String npcId : new String[] { "214823", "215085", "215093", "215390", "215391", "215427" }) {
			assertTrue(scores.contains("npc_id=\"" + npcId + "\""), "score " + npcId);
		}
			for (String npcId : new String[] { "215085", "215093", "215390", "215391", "215427" }) {
				assertTrue(drops.contains("<npc_drop npc_id=\"" + npcId + "\">"), "drop " + npcId);
			}
			assertFalse(drops.contains("<npc_drop npc_id=\"214823\">"));

			String ownership = Files.readAllLines(Path.of(
				"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
				.filter(line -> line.contains("id=\"300110000\"")).findFirst().orElseThrow();
			assertTrue(ownership.contains("retail condition/static pools, waypoint and Pattern own PvE spawns"));
			assertTrue(ownership.contains("npc-scores/npc_drops/quest data own PvE points/loot/objectives"));
			assertTrue(ownership.contains(
				"handler owns Dredgion preparation/timers, variable bridges, Surkana rooms, faction/PvP/NPC scoring, revive, captain settlement, rewards and exit recovery"));
		}

	private static void assertPair(String world, int pool, String first, String second, String x, String y) {
		String condition = conditionBlock(world, "idab1_dreadgion/world_N.xml#unconditional-random-" + pool);
		assertTrue(condition.contains("id=\"" + first + "\" probability=\"5000\" x=\"" + x + "\" y=\"" + y + "\""));
		assertTrue(condition.contains("id=\"" + second + "\" probability=\"5000\" x=\"" + x + "\" y=\"" + y + "\""));
	}

	private static String worldBlock(String xml, String worldId) {
		int start = xml.indexOf("<world id=\"" + worldId + "\"");
		return xml.substring(start, xml.indexOf("</world>", start));
	}

	private static String conditionBlock(String world, String source) {
		int sourceIndex = world.indexOf("source=\"" + source + "\"");
		int start = world.lastIndexOf("<condition ", sourceIndex);
		return world.substring(start, world.indexOf("</condition>", sourceIndex));
	}

	private static String walkerBlock(String xml, String routeId) {
		int start = xml.indexOf("<walker_template route_id=\"" + routeId + "\"");
		return xml.substring(start, xml.indexOf("</walker_template>", start));
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
