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
	private static final Path CONDITION_SPAWNS = Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml");

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

	@Test
	void harmonyConditionsOwnRetailActorsAndStaticSpawnsAreEmpty() throws IOException {
		String conditions = Files.readString(CONDITION_SPAWNS);
		int[] worlds = { 300450000, 300570000, 301100000 };
		int[] slots = { 693, 691, 691 };
		int[] sensoryAreas = { 41, 40, 40 };
		for (int i = 0; i < worlds.length; i++) {
			String world = world(conditions, worlds[i]);
			assertEquals(134, count(world, "<condition "));
			assertEquals(slots[i], count(world, "<slot>"));
			assertEquals(36, count(world, "id=\"207101\""));
			assertEquals(sensoryAreas[i], count(world, "<sensory_area "));
			assertEquals(7, count(world, "id=\"207102\""));
			assertEquals(4, count(world, "id=\"219272\""));
			assertEquals(1, count(world, "id=\"207106\""));
			assertEquals(2, count(world, "id=\"207107\""));
			assertEquals(1, count(world, "id=\"207117\""));
			assertTrue(world.contains("s1_lever == 1"));
			assertTrue(world.contains("s2_track"));
		}
		for (String name : List.of("300450000_Arena_Of_Harmony.xml", "300570000_Harmony_Training_Grounds.xml",
				"301100000_Unity_Training_Grounds.xml")) {
			String source = Files.readString(Path.of("src/main/resources/aion/data/static_data/spawns/Instances", name));
			assertFalse(source.contains("<spawn "), name);
		}
		for (String name : List.of("HarmonyTrainingGroundInstance.java", "UnityTrainingGroundInstance.java")) {
			assertFalse(Files.readString(HANDLERS.resolve(name)).contains("FlyRing"), name);
		}
	}

	@Test
	void harmonyCoinScoreUsesTeamPathAndObjectIdIdempotence() throws IOException {
		String source = Files.readString(HANDLERS.resolve("HarmonyArenaInstance.java"));
		assertTrue(source.contains("return npcId == 207101 && scoreApplyType == 0;"));
		int start = source.indexOf("public synchronized boolean onRetailNpcScore(");
		String score = source.substring(start, source.indexOf("private int getTime()", start));
		assertTrue(score.contains("consumedScoreNpcs.add(npc.getObjectId())"));
		assertTrue(score.contains("restoreGroup(group)"));
		assertTrue(score.contains("group.addPoints(points)"));
		assertTrue(score.contains("persistGroup(group)"));
		assertTrue(score.contains("sendSystemMsg(player, npc, points)"));
		assertTrue(score.contains("instanceReward.sendPacket(10, player.getObjectId())"));
		assertTrue(score.contains("finishBattle(true)"));
		assertFalse(score.contains("stableKey"));
	}

	private static String world(String source, int id) {
		int start = source.indexOf("<world id=\"" + id + "\"");
		assertTrue(start >= 0, Integer.toString(id));
		return source.substring(start, source.indexOf("</world>", start));
	}

	private static int count(String source, String token) {
		int count = 0;
		for (int at = 0; (at = source.indexOf(token, at)) >= 0; at += token.length()) {
			count++;
		}
		return count;
	}
}
