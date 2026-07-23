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
		for (String legacy : new String[] { "onDie", "handleUseItemFinish", "spawnCommanderBakarma",
				"236900", "236929", "237275", "702857", "702858", "805736", "805737",
				"draupnir.adjutants", "draupnir.chargers", "draupnir.race" }) {
			assertFalse(handler.contains(legacy), legacy);
		}
		assertTrue(handler.contains("draupnir.phantasm_deadline"));
		assertTrue(handler.contains("spawn(237276"));

		String npcAi = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npc-ai.xml"));
		assertTrue(npcAi.contains("id=\"702858\" name=\"IDDF3_Dragon_artifact_boost_entrance\""
			+ " ai=\"IDDF3_T_Control_01\""));
		assertTrue(npcAi.contains("id=\"702861\" name=\"IDDF3_Dragon_entrance_racecheck\""
			+ " ai=\"IDDF3_T_Control_05\""));
		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_iddf3_dragon_sp_ydy.xml"));
		for (String producer : new String[] { "IDDF3_Dragon_entrance_racecheck", "IDDF3_Dragon_Veris_E",
				"IDDF3_Dragon_Unfelhaitz_E", "IDDF3_dragon_t_boss", "IDDF3_dragon_t_waveend" }) {
			assertTrue(patterns.contains(producer), producer);
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
