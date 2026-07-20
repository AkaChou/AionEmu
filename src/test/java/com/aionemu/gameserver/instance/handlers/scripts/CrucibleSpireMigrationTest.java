package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CrucibleSpireMigrationTest {

	@Test
	void spireUsesPersistentFloorAndInfinityLedger() throws Exception {
		String source = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/crucible/CrucibleSpireInstance.java"));
		assertTrue(source.contains("runtimeState().put(\"infinity.floor\""));
		assertTrue(source.contains("runtimeState().put(FLOOR_CONTROLLER_DEADLINE, deadline)"));
		assertTrue(source.contains("scheduleDeadline(\"delete_floor_controller\", controllerDeadline"));
		assertTrue(source.contains("this::deleteFloorController"));
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, \"race\""));
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, variable, floor, 0)"));
		assertTrue(source.contains("InstanceSettlementService.settleInfinity("));
		assertFalse(source.contains("bossTimerStart"));
		assertFalse(source.contains("bossTimerEnd"));
		assertFalse(source.contains("Map<Integer, StaticDoor>"));
		assertFalse(source.contains("isSpawning"));
		assertFalse(source.contains("Future<?>"));
		assertFalse(source.contains("GameThreadPoolServices"));
		assertFalse(source.contains("spawnNextFloor"));
		assertFalse(source.contains("spawn(247249"));

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		int start = conditions.indexOf("<world id=\"302400000\"");
		String world = conditions.substring(start, conditions.indexOf("</world>", start));
		for (String variable : new String[] { "condition_infinity_this_season_floor_reward", "fire",
			"pre_season_check", "pre_season_reset", "timeattack_play_start" }) {
			assertTrue(world.contains("<variable name=\"" + variable + "\"/>"), variable);
		}
		assertTrue(world.contains("page_start=\"1\" page_end=\"2\""));
		for (int floor = 1; floor <= 40; floor++) {
			assertTrue(world.contains("<variable name=\"floor_" + String.format("%02d", floor) + "\"/>"));
		}
		for (String variable : new String[] { "race", "condition_infinity_pre_season_floor",
			"condition_infinity_this_season_floor" }) {
			assertTrue(world.contains("<variable name=\"" + variable + "\"/>"), variable);
		}
		assertTrue(world.contains("<npc id=\"247312\""));
		assertTrue(world.contains("<npc id=\"247350\""));
	}
}
