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
		assertEquals(109, count(world, "<condition "));
		for (String variable : new String[] { "wave_a_clear", "wave_b_01_start", "wave_c_02_start",
				"wave_c_clear", "idraksha_3f_envoyspawn", "idraksha_clear" }) {
			assertTrue(world.contains(variable), variable);
		}
		assertTrue(world.contains("<variable name=\"idraksha_clear\"/>"));
		assertTrue(world.contains("expression=\"idraksha_clear == 1\""));
		assertTrue(world.contains("<npc id=\"730445\" probability=\"10000\" x=\"619.643005\" y=\"685.139893\""
				+ " z=\"527.079773\" heading=\"240\" initial_delay=\"1\" initial_delay_extra=\"1\""));
		String patterns = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns_tamessolo_kjs.xml"));
		for (String pattern : new String[] { "Tames_Solo_A_normal_01", "Tames_Solo_B_normal_01",
				"Tames_Solo_C_normal_01", "IDRaksha_Solo_BSwitch_1", "IDRaksha_Re_Boss_KJS" }) {
			assertTrue(patterns.contains("<name>" + pattern + "</name>"), pattern);
		}
		assertTrue(patterns.contains("<set_condition_spawn_variable><string>idraksha_clear</string>"
				+ "<set>1</set><modify>0</modify></set_condition_spawn_variable>"));

		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/RaksangRuinsInstance.java"));
		assertFalse(handler.contains("GameThreadPoolServices"));
		assertFalse(handler.contains("Future"));
		assertFalse(handler.contains("onDropRegistered"));
		assertFalse(handler.contains("onDie"));
		assertFalse(handler.contains("730445"));
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
