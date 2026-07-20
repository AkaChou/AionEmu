package com.aionemu.gameserver.instance.handlers.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaksangRuinsRetailMigrationTest {

	@Test
	void retailDataOwnsRaksangWavesDoorsBossAndExit() throws Exception {
		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "300610000");
		assertEquals(108, count(world, "<condition "));
		for (String variable : new String[] { "wave_a_clear", "wave_b_01_start", "wave_c_02_start",
				"wave_c_clear", "idraksha_3f_envoyspawn" }) {
			assertTrue(world.contains(variable), variable);
		}
		String patterns = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns_tamessolo_kjs.xml"));
		for (String pattern : new String[] { "Tames_Solo_A_normal_01", "Tames_Solo_B_normal_01",
				"Tames_Solo_C_normal_01", "IDRaksha_Solo_BSwitch_1", "IDRaksha_Re_Boss_KJS" }) {
			assertTrue(patterns.contains("<name>" + pattern + "</name>"), pattern);
		}

		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/RaksangRuinsInstance.java"));
		assertFalse(handler.contains("GameThreadPoolServices"));
		assertFalse(handler.contains("Future"));
		assertFalse(handler.contains("onDropRegistered"));
		assertTrue(handler.contains("onDie"));
		assertTrue(handler.contains("730445"));
	}

	private static String worldBlock(String xml, String worldId) {
		int start = xml.indexOf("<world id=\"" + worldId + "\"");
		int end = xml.indexOf("</world>", start);
		return xml.substring(start, end);
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
