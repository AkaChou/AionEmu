package com.aionemu.gameserver.instance.handlers.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DraupnirCaveRetailMigrationTest {

	@Test
	void retailConditionsOwnDraupnirStageSpawns() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = block(conditions, "<world id=\"320080000\"", "</world>");
		assertEquals(8, count(world, "<variable "));
		assertEquals(18, count(world, "<condition "));
		assertEquals(24, count(world, "<slot>"));
		for (String expression : new String[] { "master_mode == 1", "lastboss == 4",
			"lastboss_t == 4", "IDDF3_dragon_t_waveend == 3" }) {
			assertTrue(world.contains(expression), expression);
		}
		for (String npcId : new String[] { "213780", "236929", "237275", "237263", "702857", "702893" }) {
			assertTrue(world.contains("<npc id=\"" + npcId + "\""), npcId);
		}

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/320080000_Draupnir_Cave.xml"));
		for (String npcId : new String[] { "237265", "702893", "833627", "833626", "237263",
			"833628", "237267", "213776", "213779", "236925", "236928" }) {
			assertFalse(staticSpawns.contains("<spawn npc_id=\"" + npcId + "\""), npcId);
		}

		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DraupnirCaveInstance.java"));
		for (String legacy : new String[] { "onDropRegistered", "spawnRewardChest", "702658", "702659" }) {
			assertFalse(handler.contains(legacy), legacy);
		}
	}

	private static String block(String source, String startToken, String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start);
		return source.substring(start, end);
	}

	private static int count(String source, String token) {
		return (source.length() - source.replace(token, "").length()) / token.length();
	}
}
