package com.aionemu.gameserver.model.instance.instancereward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData;
import com.aionemu.gameserver.model.Race;

class TreasureIslandRewardTest {
	@BeforeAll
	static void initializeBuffData() {
		if (DataManager.RETAIL_INSTANCE_DATA == null) {
			DataManager.RETAIL_INSTANCE_DATA = RetailInstanceData.load(
				new File("src/main/resources/aion/definitions/compact/instance"),
				new File("src/main/resources/aion/definitions/schemas/retail-instance-data.xsd"));
		}
	}

	@Test
	void awardsEachStageOnceInGlobalArrivalOrder() {
		TreasureIslandReward reward = new TreasureIslandReward(301700000, 1);
		int[] expected = { 100, 80, 60, 40, 20, 10 };
		for (int i = 0; i < expected.length; i++) {
			int objectId = i + 1;
			reward.registerPlayer(objectId, i % 2 == 0 ? Race.ELYOS : Race.ASMODIANS);
			assertEquals(expected[i], reward.registerStage(objectId, 1));
		}

		assertEquals(-1, reward.registerStage(1, 1));
		assertEquals(1, reward.getStageMask(1));
		assertEquals(6, reward.getStageArrivals(1));
		assertEquals(180, reward.getPointsByRace(Race.ELYOS));
		assertEquals(130, reward.getPointsByRace(Race.ASMODIANS));
		assertEquals(200, reward.registerStage(1, 3));
		assertEquals(5, reward.getStageMask(1));
	}

	@Test
	void restoresPlayersScoresStagesAndActivity() {
		TreasureIslandReward reward = new TreasureIslandReward(301700000, 1);
		var player = reward.restorePlayer(7, Race.ASMODIANS, 100, 500, 5, 200, 300);
		reward.restoreStageArrivals(1, 3);

		assertEquals(500, reward.getPointsByRace(Race.ASMODIANS));
		assertEquals(5, reward.getStageMask(7));
		assertEquals(3, reward.getStageArrivals(1));
		assertEquals(100, player.getJoinedAt());
		assertEquals(200, player.getLogoutAt());
		assertEquals(300, player.getOfflineMillis());
	}

	@Test
	void retailSensorsChestsAndScorePacketStayWired() throws Exception {
		String npcs = Files.readString(Path.of("src/main/resources/aion/data/static_data/npcs/npc_template.xml"));
		for (int npcId = 836199; npcId <= 836203; npcId++) {
			assertTrue(npcs.matches("(?s).*npc_id=\"" + npcId
					+ "\"[^>]*ai=\"idrun_sensory_score\".*"));
		}
		for (int npcId = 836347; npcId <= 836348; npcId++) {
			assertTrue(npcs.matches("(?s).*npc_id=\"" + npcId + "\"[^>]*ai=\"useitem\".*"));
		}
		String packet = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/network/aion/serverpackets/SM_INSTANCE_SCORE.java"));
		assertTrue(packet.contains("case 301700000"));
		assertTrue(packet.contains("69 * (96 - count)"));
		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TreasureIslandOfCourageInstance.java"));
		assertTrue(handler.contains("InstanceSettlementService.settleBattleground("));
		assertTrue(handler.contains("RetailConditionSpawnEngine.setVariable("));
		assertTrue(handler.contains("scheduleDeadline(\"preparation\""));
		assertTrue(handler.contains("runtimeState().put(\"idrun.phase\""));
		assertTrue(!handler.contains("Future<?>"));
	}
}
