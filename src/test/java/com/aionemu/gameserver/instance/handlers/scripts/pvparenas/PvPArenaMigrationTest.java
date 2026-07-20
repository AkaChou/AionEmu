package com.aionemu.gameserver.instance.handlers.scripts.pvparenas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class PvPArenaMigrationTest {
	private static final Path HANDLERS = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/pvparenas");

	@Test
	void arenaChildrenCannotRestoreHardcodedSettlement() throws IOException {
		for (String name : List.of("ArenaOfChaosInstance.java", "ChaosTrainingGroundsInstance.java",
				"ArenaOfDisciplineInstance.java", "DisciplineTrainingGroundsInstance.java",
				"ArenaOfGloryInstance.java", "ArenaOfHarmonyInstance.java",
				"HarmonyTrainingGroundInstance.java", "UnityTrainingGroundInstance.java")) {
			String source = Files.readString(HANDLERS.resolve(name));
			assertFalse(source.contains("protected void reward()"), name);
			assertFalse(source.contains("AbyssPointsService"), name);
			assertFalse(source.contains("getGloryRewardRate()"), name);
		}
	}

	@Test
	void harmonyUsesRetailTimingLedgerAndTeamSettlement() throws IOException {
		String source = Files.readString(HANDLERS.resolve("HarmonyArenaInstance.java"));
		assertTrue(source.contains("getWaitTimeSeconds()"));
		assertTrue(source.contains("getStageTimeSeconds()"));
		assertTrue(source.contains("getKillScore()"));
		assertTrue(source.contains("getDeathScore(rank)"));
		assertTrue(source.contains("getStageEndBuffId("));
		assertTrue(source.contains("getStageEndBuffTargetRank("));
		assertTrue(source.contains("getHarmonyRewardRate()"));
		assertTrue(source.contains("InstanceSettlementService.queue("));
		assertTrue(source.contains("InstanceSettlementService.settle("));
		assertFalse(source.contains("instanceReward.removePlayerReward("));
		assertFalse(source.contains("AbyssPointsService"));
		assertFalse(source.contains("ItemService"));
		assertFalse(source.contains("120000"));
		assertFalse(source.contains("}, 10000);"));
	}

	@Test
	void arenaRewardPacketKeepsRetailFixedBlockSize() throws IOException {
		String packet = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_INSTANCE_SCORE.java"));
		int helperStart = packet.indexOf("private void writeArenaReward(");
		int helperEnd = packet.indexOf("private void fillTableWithGroup(", helperStart);
		String helper = packet.substring(helperStart, helperEnd);
		assertEquals(19, helper.split("writeD\\(", -1).length - 1);
		assertTrue(packet.contains("new byte[92 * (12 - playerCount)]"));
		assertTrue(packet.contains("new byte[76]"));
		assertTrue(packet.contains("writeArenaReward(harmonyPlayerReward)"));
	}

	@Test
	void arenaHandlersUseRecoverableDeadlinesAndRetailScores() throws IOException {
		for (String name : List.of("PvPArenaInstance.java", "HarmonyArenaInstance.java")) {
			String source = Files.readString(HANDLERS.resolve(name));
			assertTrue(source.contains("runtimeState().put(STATE + \"phase\""), name);
			assertTrue(source.contains("scheduleDeadline(\"prepare\""), name);
			assertTrue(source.contains("scheduleDeadline(\"round\""), name);
			assertTrue(source.contains("scheduleDeadline(\"exit\""), name);
			assertTrue(source.contains("DataManager.RETAIL_AI_DATA.getNpcScore"), name);
			assertFalse(source.contains("GameThreadPoolServices"), name);
			assertFalse(source.contains("private int getNpcBonus(int npcId) {\n\t\tswitch"), name);
		}
		String solo = Files.readString(HANDLERS.resolve("PvPArenaInstance.java"));
		assertFalse(solo.contains("ItemService.addItem"));
		assertFalse(solo.contains("186000454"));
	}
}
