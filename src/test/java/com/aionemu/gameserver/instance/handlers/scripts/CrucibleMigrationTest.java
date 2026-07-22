package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CrucibleMigrationTest {

	private static final Path HANDLERS = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/crucible");
	private static final Path AI = Path.of("src/main/java/com/aionemu/gameserver/ai/instance");

	@Test
	void retailConditionsOwnGroupAndSoloEncounters() throws Exception {
		String definitions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String group = world(definitions, 300300000);
		String solo = world(definitions, 300320000);

		assertEquals(36, count(group, "<variable "));
		assertEquals(310, count(group, "<condition "));
		for (String variable : new String[] { "condition_s1_l", "condition_s6", "condition_s10",
				"stage5_start", "stage10_start" }) {
			assertTrue(group.contains("<variable name=\"" + variable + "\"/>"), variable);
		}

		assertEquals(31, count(solo, "<variable "));
		assertEquals(137, count(solo, "<condition "));
		for (String variable : new String[] { "clear", "condition_s1_l", "condition_s1_d", "condition_s2a",
				"condition_s2b", "condition_s3a", "condition_s3b", "condition_s4a", "condition_s4b",
				"condition_s5_l", "condition_s5_d", "condition_s6", "hidden_l", "hidden_d", "stage" }) {
			assertTrue(solo.contains("<variable name=\"" + variable + "\"/>"), variable);
		}
		assertTrue(solo.contains("<npc id=\"217784\""));
		assertTrue(solo.contains("<npc id=\"217785\""));
	}

	@Test
	void handlersKeepOnlyProtocolSettlementAndUnsupportedPortal() throws Exception {
		String base = Files.readString(HANDLERS.resolve("CrucibleInstance.java"));
		String solo = Files.readString(HANDLERS.resolve("CrucibleChallengeInstance.java"));
		String group = Files.readString(HANDLERS.resolve("EmpyreanCrucibleInstance.java"));

		assertTrue(base.contains("DataManager.RETAIL_AI_DATA.getNpcScore"));
		assertTrue(base.contains("RetailConditionSpawnEngine.consumeConditionSpawnDeath"));
		assertTrue(base.contains("new SM_INSTANCE_STAGE_INFO"));
		assertTrue(solo.contains("spawn(730459"));
		assertTrue(solo.contains("scheduleDeadline(\"revive.\""));
		assertTrue(solo.contains("InstanceSettlementService.settleCrucible"));
		assertTrue(group.contains("InstanceSettlementService.settleCrucible"));

		for (String source : new String[] { solo, group }) {
			for (String legacy : new String[] { "GameThreadPoolServices", "Future<", "onDropRegistered",
					"void onChangeStage", "private void sp(", "rewardCount", "EmpyreanStage" }) {
				assertFalse(source.contains(legacy), legacy);
			}
		}
	}

	@Test
	void recordKeepersUseOneRetailBridge() throws Exception {
		String solo = Files.readString(AI.resolve("crucibleChallenge/CrucibleChallengeRecordkeeperAI2.java"));
		String group = Files.readString(AI.resolve("empyreanCrucible/EmpyreanCrucibleRecordkeeperAI2.java"));
		assertTrue(solo.contains("set(\"Condition_S2\", 1, 0)"));
		assertTrue(solo.contains("set(\"Condition_S3\", 1, 0)"));
		assertTrue(solo.contains("set(\"STAGE\", Rnd.nextBoolean() ? 3 : 4, 0)"));
		assertTrue(solo.contains("finish(5, \"STAGE5_START\")"));
		assertTrue(solo.contains("set(\"CLEAR\", 1, 0)"));
		assertFalse(solo.contains("TeleportService2"));
		assertFalse(Files.exists(AI.resolve("crucibleChallenge/RecordkeeperAI2.java")));
		assertTrue(group.contains("case 799568 ->"));
		assertTrue(group.contains("set(\"STAGE2_START\", 0, 1)"));
		assertTrue(group.contains("set(\"Condition_S2_L\", 0, 1)"));
		assertTrue(group.contains("start(205000, \"Condition_S5_L\")"));
		assertTrue(group.contains("start(407000, \"Condition_S7_L\")"));
		assertFalse(group.contains("Condition_S2_D"));
		assertFalse(group.contains("Condition_S5_D"));
		assertFalse(group.contains("Condition_S7_D"));
		assertTrue(group.contains("getNpcId() >= 205331 && getNpcId() <= 205337 && startStage(player)"));
		for (int sceneStatus : new int[] { 101000, 102000, 103000, 104000, 205000, 306000, 407000,
				508000, 609000, 710000 }) {
			assertTrue(group.contains(Integer.toString(sceneStatus)), Integer.toString(sceneStatus));
		}
		for (int legacyStatus : new int[] { 200000, 300000, 400000, 500000, 600000, 700000 }) {
			assertFalse(group.contains(Integer.toString(legacyStatus)), Integer.toString(legacyStatus));
		}
		assertTrue(group.contains("for (Player player : getPosition().getWorldMapInstance().getPlayersInside())"));
		for (String removed : new String[] { "Empyrean_Record_KeeperAI2.java",
				"Empyrean_Record_Keeper2AI2.java", "RecordKeeperRewardAI2.java" }) {
			assertFalse(Files.exists(AI.resolve("empyreanCrucible").resolve(removed)), removed);
		}
		assertFalse(group.contains("TeleportService2"));

		String templates = Files.readString(Path.of("src/main/resources/aion/data/static_data/npcs/npc_template.xml"));
		for (int npcId = 205666; npcId <= 205679; npcId++) {
			int start = templates.indexOf("npc_id=\"" + npcId + "\"");
			assertTrue(start >= 0, Integer.toString(npcId));
			assertTrue(templates.substring(start, templates.indexOf('>', start))
				.contains("ai=\"crucible_challenge_recordkeeper\""), Integer.toString(npcId));
		}
		for (int npcId : new int[] { 799567, 799568, 799569, 205331, 205332, 205333, 205334, 205335,
				205336, 205337, 205338, 205339, 205340, 205341, 205342, 205343, 205344 }) {
			int start = templates.indexOf("npc_id=\"" + npcId + "\"");
			assertTrue(start >= 0, Integer.toString(npcId));
			assertTrue(templates.substring(start, templates.indexOf('>', start))
				.contains("ai=\"empyrean_crucible_recordkeeper\""), Integer.toString(npcId));
		}

		String handler = Files.readString(HANDLERS.resolve("EmpyreanCrucibleInstance.java"));
		assertTrue(handler.contains("new SM_INSTANCE_STAGE_INFO(2, status & 0xffff, status >>> 16)"));
	}

	@Test
	void soloBonusRiftWritesRecoveredRetailBranchVariables() throws Exception {
		String source = Files.readString(AI.resolve("crucibleChallenge/CrucibleRiftAI2.java"));
		assertTrue(source.contains("\"hidden_L\" : \"hidden_D\""));
		assertTrue(source.contains("\"STAGE\", player.getLevel() <= 50 ? 7 : 8"));
		assertFalse(source.contains("spawn(218200"));
		assertFalse(source.contains("spawn(218192"));
		assertFalse(source.contains("GameThreadPoolServices"));
	}

	private static String world(String definitions, int worldId) {
		int start = definitions.indexOf("<world id=\"" + worldId + "\"");
		return definitions.substring(start, definitions.indexOf("</world>", start));
	}

	private static int count(String text, String token) {
		return (text.length() - text.replace(token, "").length()) / token.length();
	}
}
