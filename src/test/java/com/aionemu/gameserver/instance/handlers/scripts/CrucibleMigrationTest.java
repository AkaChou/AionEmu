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

		assertEquals(30, count(solo, "<variable "));
		assertEquals(137, count(solo, "<condition "));
		for (String variable : new String[] { "condition_s1_l", "condition_s1_d", "condition_s2a",
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
	void recordKeepersWriteRetailVariablesWithoutLegacyStageCallbacks() throws Exception {
		String solo = Files.readString(AI.resolve("crucibleChallenge/RecordkeeperAI2.java"));
		String group = Files.readString(AI.resolve("empyreanCrucible/Empyrean_Record_KeeperAI2.java"));
		String first = Files.readString(AI.resolve("empyreanCrucible/Empyrean_Record_Keeper2AI2.java"));
		for (String source : new String[] { solo, group, first }) {
			assertTrue(source.contains("RetailConditionSpawnEngine.setVariable"));
			assertFalse(source.contains("onChangeStage"));
			assertFalse(source.contains("spawn("));
			assertFalse(source.contains("GameThreadPoolServices"));
		}
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
