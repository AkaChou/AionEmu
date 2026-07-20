package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ContaminatedUnderpathMigrationTest {

	private static final Path HANDLERS = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts");

	@Test
	void eventAndLunaHandlersUseRetailSpawnsScoresAndPersistentSettlement() throws Exception {
		assertMigrated("event/Event_ContaminatedUnderpathInstance.java", "TIMEATTACK_PLAY_START",
			"InstanceSettlementService.settleTimeAttack(");
		assertMigrated("luna/ContaminatedUnderpathInstance.java", "IDLUNA_DEF_PHASE_1_1",
			"InstanceSettlementService.settleLuna(");
	}

	private static void assertMigrated(String relative, String startVariable, String settlement) throws Exception {
		String source = Files.readString(HANDLERS.resolve(relative));
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, \"" + startVariable + "\""), relative);
		assertTrue(source.contains("score.scoreApplyType() == 3"), relative);
		assertTrue(source.contains("scheduleDeadline(\"prepare\""), relative);
		assertTrue(source.contains("scheduleDeadline(\"expire\""), relative);
		assertTrue(source.contains("scheduleDeadline(\"settle\""), relative);
		assertTrue(source.contains("runtimeState().snapshot(STATE + \"kill.\")"), relative);
		assertTrue(source.contains("runtimeState().put(playerRewardKey("), relative);
		assertTrue(source.contains(settlement), relative);
		for (String legacy : new String[] { "Future<", "GameThreadPoolServices", "onDropRegistered",
			"handleUseItemFinish", "ItemService.addItem", "RewardType.QUEST", "protected void sp",
			"startContaminedUnderPath", "spawn(" }) {
			assertFalse(source.contains(legacy), relative + ": " + legacy);
		}
	}
}
