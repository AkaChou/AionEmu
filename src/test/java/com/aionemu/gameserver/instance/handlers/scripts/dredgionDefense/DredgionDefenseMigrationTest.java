package com.aionemu.gameserver.instance.handlers.scripts.dredgionDefense;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DredgionDefenseMigrationTest {
	private static final Path HANDLERS = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/dredgionDefense");

	@Test
	void handlersOnlyKeepPlayerExitCleanupAndDeathProtocol() throws Exception {
		for (String file : new String[] { "PandaemoniumInstance.java", "SanctumInstance.java" }) {
			String source = Files.readString(HANDLERS.resolve(file));
			assertTrue(source.contains("moveToInstanceExit"), file);
			assertTrue(source.contains("onPlayerLogOut(Player player) {\n\t\tremoveEffects(player);\n\t}"), file);
			assertFalse(source.contains("onPlayerLogOut(Player player) {\n\t\tremoveEffects(player);\n\t\tTeleportService2"), file);
			assertTrue(source.contains("removeEffect(18290)"), file);
			assertTrue(source.contains("removeEffect(18300)"), file);
			assertTrue(source.contains("new SM_EMOTION"), file);
			assertTrue(source.contains("new SM_DIE"), file);
			for (String legacy : new String[] { "Future<", "GameThreadPoolServices", "ThreadPoolManager",
				"ItemService", "Rnd", "onDropRegistered", "handleUseItemFinish", "onDie(Npc", "spawn(",
				"doReward", "onInstanceCreate", "onInstanceDestroy" }) {
				assertFalse(source.contains(legacy), file + " still contains " + legacy);
			}
		}
	}

	@Test
	void retailConditionSpawnsOwnBothEncounters() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String[] worldIds = { "302200000", "302300000" };
		int[] conditionCounts = { 966, 968 };
		int[] npcCounts = { 2242, 2244 };
		for (int i = 0; i < worldIds.length; i++) {
			String worldId = worldIds[i];
			String world = worldBlock(conditions, worldId);
			assertEquals(55, count(world, "<variable "), worldId);
			assertEquals(conditionCounts[i], count(world, "<condition "), worldId);
			assertEquals(npcCounts[i], count(world, "<npc "), worldId);
			for (String variable : new String[] { "dreadgion_invasion", "dreadgion_invasion_boss",
				"dreadgion_raid_01", "dreadgion_strong_raid", "dreadgion_weapon", "mainmodule",
				"submodule_a_01", "submodule_b_01", "set_energy_a", "turret_01" }) {
				assertTrue(world.contains("<variable name=\"" + variable + "\""), worldId + ':' + variable);
			}
		}
	}

	@Test
	void retailAiOwnsTimersObjectivesAndRankState() throws Exception {
		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_dreadgion_ctrl_ssh.xml"));
		String npcAi = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-ai.xml"));
		for (String pattern : new String[] { "Dreadgion_main_ctrl", "Dreadgion_raid_ctrl",
			"Dreadgion_ship_ctrl", "Dreadgion_invasion_ctrl", "Dreadgion_strong_raid_ctrl",
			"Dreadgion_defense_ctrl", "Dreadgion_S_Rank", "Dreadgion_A_Rank", "Dreadgion_B_Rank",
			"Dreadgion_C_Rank", "Dreadgion_D_Rank", "Dreadgion_F_Rank" }) {
			assertTrue(patterns.contains("<name>" + pattern + "</name>"), pattern);
			assertTrue(npcAi.contains("ai=\"" + pattern + "\""), pattern);
		}
		assertTrue(patterns.contains("<string>dreadgion_finish_n</string>"));
		assertTrue(patterns.contains("<string>mainmodule</string><set>1</set><modify>0</modify>"));

		var coverage = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		for (String worldId : new String[] { "302200000", "302300000" }) {
			String ownership = coverage.stream().filter(line -> line.contains("id=\"" + worldId + "\""))
				.findFirst().orElseThrow();
			assertTrue(ownership.contains("retail static/condition spawns and Pattern own invasion timers, waves, objectives and S-F rank state"), worldId);
				assertTrue(ownership.contains("true-server UNIONMATCH instance data owns logout reentry; handler only owns player death protocol, effects 18290/18300 cleanup and explicit exit"), worldId);
		}
	}

	private int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}

	private String worldBlock(String conditions, String worldId) {
		int start = conditions.indexOf("<world id=\"" + worldId + "\"");
		int end = conditions.indexOf("</world>", start);
		assertTrue(start >= 0 && end > start, worldId);
		return conditions.substring(start, end);
	}
}
