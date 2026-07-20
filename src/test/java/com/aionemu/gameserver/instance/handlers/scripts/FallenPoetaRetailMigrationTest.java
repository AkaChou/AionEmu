package com.aionemu.gameserver.instance.handlers.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallenPoetaRetailMigrationTest {

	@Test
	void retailDataOwnsFallenPoetaBarriersWavesAndBoss() throws Exception {
		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "301660000");
		assertEquals(97, count(world, "<condition "));
		assertEquals(54, count(world, "<variable "));
		for (String variable : new String[] { "con_anu_start", "con_barri_wall01", "con_barri_wall10",
				"con_alarm01_40", "con_siege_poly_01_l", "con_tele_04" }) {
			assertTrue(world.contains(variable), variable);
		}

		String patterns = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns_f6_condition_jsm.xml"))
				+ Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idlf1_t_ydy.xml"));
		for (String pattern : new String[] { "LF6_I_Din_01_Enter_Attack_73", "LF6_C_Din_01_Enter_Attack_74",
				"LF6_F_Din_01_Enter_Attack_74" }) {
			assertTrue(patterns.contains("<name>" + pattern + "</name>"), pattern);
		}

		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/FallenPoetaInstance.java"));
		assertFalse(handler.contains("GameThreadPoolServices"));
		assertFalse(handler.contains("Future"));
		assertFalse(handler.contains("onDropRegistered"));
		assertFalse(handler.contains("onDie"));
		assertFalse(handler.contains("spawn("));
		assertTrue(handler.contains("164002346"));
		assertTrue(handler.contains("21805"));
		assertTrue(handler.contains("21806"));
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
