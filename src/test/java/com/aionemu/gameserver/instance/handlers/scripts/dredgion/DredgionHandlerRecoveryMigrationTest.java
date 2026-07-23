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
		assertTrue(source.contains("runtimeState().getLong(STATE + \"named_deadline\""));
		assertTrue(source.contains("startedAt + Rnd.get(750, 900) * 1000L"));
		assertTrue(source.contains("spawn(730311, 415.033875f, 174.003876f, 433.94046f, (byte) 0, 34)"));
		assertTrue(source.contains("spawn(730312, 572.038208f, 185.252136f, 433.94046f, (byte) 0, 10)"));
		assertTrue(source.contains("spawn(216941, 479.955719f, 314.959381f, 412.0f, (byte) 30)"));

		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300210000_Chantra_Dredgion.xml"));
		for (int npcId : new int[] { 730311, 730312, 216941 }) {
			assertFalse(spawns.contains("<spawn npc_id=\"" + npcId + "\""), Integer.toString(npcId));
		}

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = world(conditions, 300210000);
		for (String variable : new String[] { "switch_1_destroyed", "switch_2_destroyed", "teleport_4_destroyed",
				"teleport_5_destroyed", "teleport_d_destroyed", "teleport_l_destroyed" }) {
			assertTrue(world.contains("<variable name=\"" + variable + "\"/>"), variable);
		}
		for (int npcId : new int[] { 730345, 730346, 730314, 730315, 730357, 730358 }) {
			assertTrue(world.contains("<npc id=\"" + npcId + "\""), Integer.toString(npcId));
		}

		String ownership = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"300210000\"")).findFirst().orElseThrow();
		assertTrue(ownership.contains("retail static/condition spawns, waypoint, Pattern and npc-scores/npc_drops own PvE content"));
		assertTrue(ownership.contains("handler owns preparation/timers, delayed retail teleporter/named spawns"));
		assertTrue(ownership.contains("Surkana rooms, faction/PvP/NPC scoring, revive, captain settlement, rewards and exit recovery"));
	}

	@Test
	void terathUsesPersistentRetailSingleTrack() throws Exception {
		assertMigrated("TerathDredgionInstance", "terath.");
		String source = readSource("TerathDredgionInstance");
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, \"surkana_8\", 1, 1)"));
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable(instance, \"named_killed_l\""));
		assertFalse(source.contains("spawn(219264"));
		assertTrue(source.contains("runtimeState().getLong(STATE + \"named_deadline\""));
		assertTrue(source.contains("startedAt + Rnd.get(750, 1000) * 1000L"));
		assertTrue(source.contains("spawn(730558, 415.033875f, 174.003876f, 433.94046f, (byte) 0, 34)"));
		assertTrue(source.contains("spawn(730559, 572.038208f, 185.252136f, 433.94046f, (byte) 0, 10)"));
		assertTrue(source.contains("spawn(219270, 484.663666f, 314.207001f, 404.458649f, (byte) 30)"));

		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300440000_Terath_Dredgion.xml"));
		for (int npcId : new int[] { 730558, 730559 }) {
			assertFalse(spawns.contains("<spawn npc_id=\"" + npcId + "\""), Integer.toString(npcId));
		}

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = world(conditions, 300440000);
		for (String variable : new String[] { "dswitch_1_destroyed", "dswitch_2_destroyed", "named_killed_d",
				"named_killed_l", "surkana_8", "tswitch_1_destroyed", "tswitch_2_destroyed" }) {
			assertTrue(world.contains("<variable name=\"" + variable + "\"/>"), variable);
		}
		for (int npcId : new int[] { 730567, 730563, 730560, 219264, 730566, 730562, 730561 }) {
			assertTrue(world.contains("<npc id=\"" + npcId + "\""), Integer.toString(npcId));
		}

		String ownership = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"300440000\"")).findFirst().orElseThrow();
		assertTrue(ownership.contains("retail static/condition spawns, waypoint, Pattern and npc-scores/npc_drops own PvE content"));
		assertTrue(ownership.contains("handler owns preparation/timers, delayed retail teleporter/named spawns"));
		assertTrue(ownership.contains("Surkana rooms, faction/PvP/NPC scoring, revive, captain settlement, rewards and exit recovery"));
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
		assertTrue(source.contains("runtimeState().getLong(STATE + \"named_deadline\""));
		assertTrue(source.contains("startedAt + Rnd.get(750, 1000) * 1000L"));
		assertTrue(source.contains("spawn(801989, 415.033875f, 174.003876f, 433.94046f, (byte) 0, 34)"));
		assertTrue(source.contains("spawn(801990, 572.038208f, 185.252136f, 433.94046f, (byte) 0, 10)"));
		assertTrue(source.contains("spawn(243822, 484.663666f, 314.207001f, 404.458649f, (byte) 30)"));

		String spawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301650000_Ashunatal_Dredgion.xml"));
		for (int npcId : new int[] { 801989, 801990 }) {
			assertFalse(spawns.contains("<spawn npc_id=\"" + npcId + "\""), Integer.toString(npcId));
		}

		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = world(conditions, 301650000);
		assertTrue(world.contains("<variable name=\"surkana_8\"/>"));
		assertTrue(world.contains("<npc id=\"243953\""));
		assertTrue(world.contains("<npc id=\"248996\""));
		assertFalse(world.contains("<npc id=\"243816\""));
		String scores = Files.readString(Path.of("src/main/resources/aion/definitions/compact/ai/npc-scores.xml"));
		assertTrue(scores.lines().anyMatch(line -> line.contains("npc_id=\"243953\"") && line.contains("value=\"1000\"")));

		String ownership = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"301650000\"")).findFirst().orElseThrow();
		assertTrue(ownership.contains("retail static/condition spawns, waypoint, Pattern and npc-scores/npc_drops own PvE content"));
		assertTrue(ownership.contains("handler owns preparation/timers, delayed retail teleporter/named spawns"));
		assertTrue(ownership.contains("Surkana rooms, faction/PvP/NPC scoring, revive, captain settlement, rewards and exit recovery"));
	}

	private static void assertMigrated(String handler, String statePrefix) throws Exception {
		String source = readSource(handler);
		assertTrue(source.contains("scheduleDeadline(\"start\""));
		assertTrue(source.contains("runtimeState().put(STATE + \"phase\""));
		assertTrue(source.contains("setDoorState(doorId, true)"));
		assertTrue(source.contains("DataManager.RETAIL_AI_DATA.getNpcScore"));
		assertTrue(source.contains("InstanceSettlementService.settle("));
		assertTrue(source.contains("InstanceSettlementService.queue(instance, playerReward.getOwner(), \"dredgion\""));
		assertTrue(source.contains("runtimeState().snapshot(STATE + \"player.\")"));
		assertTrue(source.contains("runtimeState().put(playerState(player.getObjectId(), \"race\")"));
		String create = source.substring(source.indexOf("public void onInstanceCreate"),
				source.indexOf("protected void stopInstance"));
		assertTrue(create.contains("restorePlayers();"));
		assertTrue(create.contains("doReward();"));
		assertTrue(source.contains("RetailConditionSpawnEngine.setVariable"));
		assertTrue(source.contains("private static final String STATE = \"" + statePrefix + "\""));
		assertFalse(source.contains("GameThreadPoolServices"));
		assertFalse(source.contains("Future<"));
		assertFalse(source.contains("Map<Integer, StaticDoor>"));
		assertFalse(source.contains("AbyssPointsService.addAp"));
		assertFalse(source.contains("AbyssPointsService.addGp"));
		assertFalse(source.contains("float abyssPoint ="));
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
