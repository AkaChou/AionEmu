package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.instance.instancereward.StonespearReachReward;

class StonespearReachMigrationTest {
	private static final Path HANDLER = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/StonespearReachInstance.java");

	@Test
	void handlerUsesPersistentRetailTimeAttackFlow() throws Exception {
		String source = Files.readString(HANDLER);
		for (String required : new String[] {
			"runtimeState()", "scheduleDeadline(\"prepare\"", "scheduleDeadline(\"expire\"",
			"InstanceSettlementService.timeAttackWaitSeconds", "InstanceSettlementService.timeAttackLimitSeconds",
			"InstanceSettlementService.timeAttackRank", "InstanceSettlementService.timeAttackPlan",
			"InstanceSettlementService.settleTimeAttack", "DataManager.RETAIL_AI_DATA.getNpcScore",
			"RetailConditionSpawnEngine.consumeConditionSpawnDeath", "npc.getSpawn().getStableKey()",
			"supportsRetailNpcScore", "FINAL_BOSS = 855843"
		}) {
			assertTrue(source.contains(required), required);
		}
		for (String legacy : new String[] {
			"Future<", "GameThreadPoolServices", "onDropRegistered", "startInstanceTask", "stopInstanceTask",
			"spawnBoss", "spawnRaid", "spawnGuardianStone", "SPAWN_POSITIONS", "Rnd.get", "sendMovie", "spawn("
		}) {
			assertFalse(source.contains(legacy), legacy);
		}

		String first = StonespearReachInstance.scoreEventKey("retail:condition:1:generation.1", 100);
		assertEquals(first, StonespearReachInstance.scoreEventKey("retail:condition:1:generation.1", 101));
		assertNotEquals(first, StonespearReachInstance.scoreEventKey("retail:condition:1:generation.2", 100));
		assertEquals("stonespear.score.event.object.100", StonespearReachInstance.scoreEventKey(null, 100));

		StonespearReachReward reward = new StonespearReachReward(301500000, 1);
		reward.restore(71_600, 42, 1);
		assertEquals(71_600, reward.getPoints());
		assertEquals(42, reward.getNpcKills());
		assertEquals(1, reward.getRank());
	}

	@Test
	void retailDataOwnsStartupWavesBossesScoresAndSettlement() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/stoneSpearReach/Macadamic_JesterAI2.java")));
		String npcTemplate = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/npcs/npc_template.xml"));
		assertFalse(npcTemplate.contains("ai=\"Macadamic_Jester\""));
		String customWalkers = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/npc_walker/custom_npc_walker.xml"));
		assertFalse(customWalkers.contains("route_id=\"301500000\""));
		String definitions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldConditions(definitions, 301500000);
		assertEquals(7, occurrences(world, "<variable "));
		assertEquals(38, occurrences(world, "<condition "));
		assertEquals(38, occurrences(world, "<slot>"));
		for (String variable : new String[] {
			"boss_on", "f1_t1_obj_die", "hidden_on", "legion_on", "race_dark", "race_light", "t2_fobj_on"
		}) {
			assertTrue(world.contains("<variable name=\"" + variable + "\"/>"), variable);
		}
		for (String npcId : new String[] {
			"833284", "855774", "855775", "855776", "855797", "855798", "855799",
			"855820", "855821", "855822", "855843", "856303"
		}) {
			assertTrue(world.contains("<npc id=\"" + npcId + "\""), npcId);
		}

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301500000_Stonespear_Reach.xml"));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"856460\">"));
		assertTrue(staticSpawns.contains("x=\"231.460693\" y=\"264.565704\" z=\"96.932259\""));
		assertFalse(staticSpawns.contains("<spawn npc_id=\"833284\">"));

		String scores = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-scores.xml"));
		for (String expected : new String[] {
			"npc_id=\"855764\" name=\"BIDRegion_1F_T2_FOBJ\"",
			"npc_id=\"855774\" name=\"BIDRegion_1F_T4_Boss1_65_Ah\"",
			"npc_id=\"855797\" name=\"BIDRegion_2F_T4_Boss1_65_Ah\"",
			"npc_id=\"855820\" name=\"BIDRegion_3F_T4_Boss1_65_Ah\"",
			"npc_id=\"855843\" name=\"BIDRegion_4F_T4_Boss1_65_Ah\"",
			"npc_id=\"856303\" name=\"BIDLegion_Hidden_NPC_65\""
		}) {
			assertTrue(scores.contains(expected), expected);
		}

		String rewards = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/rewards.xml"));
		int rowAt = rewards.indexOf("world_id=\"301500000\"");
		assertTrue(rowAt >= 0);
		String row = rewards.substring(rewards.lastIndexOf("<row", rowAt), rewards.indexOf("/>", rowAt));
		assertTrue(row.contains("wait_time=\"180\""));
		assertTrue(row.contains("time_limit=\"1800\""));
		assertTrue(row.contains("s_score_minimum=\"71600\""));
		assertTrue(row.contains("a_score_minimum=\"41000\""));
		assertTrue(row.contains("b_score_minimum=\"26000\""));
		assertTrue(row.contains("c_score_minimum=\"14000\""));
		assertTrue(row.contains("d_score_minimum=\"8800\""));
	}

	private static String worldConditions(String definitions, int worldId) {
		int start = definitions.indexOf("<world id=\"" + worldId + "\"");
		int end = definitions.indexOf("</world>", start);
		return definitions.substring(start, end);
	}

	private static int occurrences(String value, String needle) {
		return (value.length() - value.replace(needle, "").length()) / needle.length();
	}
}
