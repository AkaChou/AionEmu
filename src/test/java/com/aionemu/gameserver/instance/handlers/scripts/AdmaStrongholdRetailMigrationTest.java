package com.aionemu.gameserver.instance.handlers.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdmaStrongholdRetailMigrationTest {

	@Test
	void retailConditionsOwnAdmaStagesAndHandlerHasNoDuplicateFlow() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = block(conditions, "<world id=\"320130000\"", "</world>");
		assertEquals(2, count(world, "<variable "));
		assertEquals(9, count(world, "<condition "));
		assertEquals(9, count(world, "<slot>"));
		for (String expression : new String[] { "adma_t_boss == 1", "adma_t_boss == 7" }) {
			assertTrue(world.contains(expression), expression);
		}
		for (String npcId : new String[] { "237239", "237240", "237241", "237242", "237243", "237244", "856574" }) {
			assertTrue(world.contains("<npc id=\"" + npcId + "\""), npcId);
		}

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/320130000_Adma_Stronghold.xml"));
		assertFalse(staticSpawns.contains("<spawn npc_id=\"237242\""));
		assertFalse(staticSpawns.contains("<spawn npc_id=\"237243\""));
		assertTrue(staticSpawns.contains("alternate_id=\"237245\" select_prob=\"2500\""));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"730176\""));

		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AdmaStrongholdInstance.java"));
		for (String legacy : new String[] { "onDropRegistered", "onDie", "scheduleDeadline", "spawnReaper",
			"spawnExit", "case 237239", "case 237240", "case 237241", "case 237242", "case 237243", "case 237244",
			"702658", "702659" }) {
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
