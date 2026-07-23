package com.aionemu.gameserver.instance.handlers.scripts.dredgion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DredgionHandlerRecoveryMigrationTest {

	@Test
	void baranathUsesPersistentRetailSingleTrack() throws Exception {
		assertMigrated("BaranathDredgion", "baranath.");
		String source = readSource("BaranathDredgion");
		assertTrue(source.contains("runtimeState().getLong(STATE + \"boss_finish_deadline\""));
		assertTrue(source.contains("scheduleDeadline(\"teleport\", startedAt + 1_020_000"));
		assertFalse(source.contains("startedAt + 600_000"));
	}

	@Test
	void chantraUsesPersistentRetailSingleTrack() throws Exception {
		assertMigrated("ChantraDredgionInstance", "chantra.");
		String source = readSource("ChantraDredgionInstance");
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, \"teleport_l_destroyed\""));
		assertTrue(source.contains("runtimeState().put(STATE + \"captain_spawned\""));
	}

	@Test
	void terathUsesPersistentRetailSingleTrack() throws Exception {
		assertMigrated("TerathDredgionInstance", "terath.");
		String source = readSource("TerathDredgionInstance");
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, \"surkana_8\", 1, 1)"));
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, \"named_killed_l\""));
		assertFalse(source.contains("spawn(219264"));
	}

	@Test
	void ashunatalUsesPersistentRetailSingleTrack() throws Exception {
		assertMigrated("AshunatalDredgionInstance", "ashunatal.");
		String source = readSource("AshunatalDredgionInstance");
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, \"named_killed_l\""));
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, \"surkana_8\", 1, 1)"));
		assertTrue(source.contains("case 243953:"));
		assertFalse(source.contains("captain_spawned"));
		assertFalse(source.contains("spawn(243816"));
		assertFalse(source.contains("spawn(801991"));

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = world(conditions, 301650000);
		assertTrue(world.contains("<variable name=\"surkana_8\"/>"));
		assertTrue(world.contains("<npc id=\"243953\""));
		assertTrue(world.contains("<npc id=\"248996\""));
		assertFalse(world.contains("<npc id=\"243816\""));
		String scores = Files.readString(Path.of("src/main/resources/aion/definitions/compact/ai/npc-scores.xml"));
		assertTrue(scores.lines().anyMatch(line -> line.contains("npc_id=\"243953\"") && line.contains("value=\"1000\"")));
	}

	private static void assertMigrated(String handler, String statePrefix) throws Exception {
		String source = readSource(handler);
		assertTrue(source.contains("scheduleDeadline(\"start\""));
		assertTrue(source.contains("runtimeState().put(STATE + \"phase\""));
		assertTrue(source.contains("setDoorState(doorId, true)"));
		assertTrue(source.contains("DataManager.RETAIL_AI_DATA.getNpcScore"));
		assertTrue(source.contains("InstanceSettlementService.settle("));
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable"));
		assertTrue(source.contains("private static final String STATE = \"" + statePrefix + "\""));
		assertFalse(source.contains("GameThreadPoolServices"));
		assertFalse(source.contains("Future<"));
		assertFalse(source.contains("Map<Integer, StaticDoor>"));
		assertFalse(source.contains("AbyssPointsService.addAp"));
		assertFalse(source.contains("AbyssPointsService.addGp"));
		assertFalse(source.contains("onDropRegistered"));
		assertFalse(source.contains("protected void sp("));
		assertFalse(source.contains("stopInstanceTask"));
		String surkanaHandler = source.substring(source.indexOf("private void onDieSurkan"),
			source.indexOf("protected void startInstanceTask"));
		assertFalse(surkanaHandler.contains("updateScore"));
	}

	private static String readSource(String handler) throws Exception {
		return Files.readString(Path.of("src/main/java/com/aionemu/gameserver/instance/handlers/scripts/dredgion/"
			+ handler + ".java"));
	}

	private static String world(String conditions, int worldId) {
		int start = conditions.indexOf("<world id=\"" + worldId + "\"");
		return conditions.substring(start, conditions.indexOf("</world>", start));
	}
}
