package com.aionemu.gameserver.instance.handlers.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaksangRuinsRetailMigrationTest {

	@Test
	void retailDataOwnsRaksangWavesDoorsBossExitAndTeleporters() throws Exception {
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

		// 六个传送 NPC（206378-380 Abiso / 206395-397 Proqura）的传送逻辑由真端
		// Tames_Solo_*_Teleporter Pattern 接管：on_hyperlink_clicked 传送至 Alias_Start_A/B/C
		// 并启用 QuestArea_Course_A/B/C。旧 ProquraAI2/AbisoAI2 桥接已删除。
		for (String teleporter : new String[] { "Tames_Solo_A_Teleporter", "Tames_Solo_B_Teleporter",
				"Tames_Solo_C_Teleporter" }) {
			assertTrue(patterns.contains("<name>" + teleporter + "</name>"), teleporter);
		}
		assertTrue(patterns.contains("<alias>Alias_Start_A</alias>")
				|| patterns.contains("alias>Alias_Start_A</alias"));
		String aliases = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/ai-location-aliases.xml"));
		for (String alias : new String[] { "Alias_Start_A", "Alias_Start_B", "Alias_Start_C" }) {
			assertTrue(aliases.contains("world_id=\"300610000\" world_name=\"idraksha_solo\" name=\"" + alias + "\""),
				alias);
		}
		String areas = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/ai-areas.xml"));
		for (String area : new String[] { "QuestArea_Course_A", "QuestArea_Course_B", "QuestArea_Course_C" }) {
			assertTrue(areas.contains("world_id=\"300610000\" world_name=\"idraksha_solo\" name=\"" + area + "\""),
				area);
		}

		// 旧 Java 桥接已删除：无副本 Handler、无逐 NPC 传送 AI2。
		assertFalse(Files.exists(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/RaksangRuinsInstance.java")));
		assertFalse(Files.exists(Path.of(
				"src/main/java/com/aionemu/gameserver/ai/instance/raksangRuins/ProquraAI2.java")));
		assertFalse(Files.exists(Path.of(
				"src/main/java/com/aionemu/gameserver/ai/instance/raksangRuins/AbisoAI2.java")));
		String npcs = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/npcs/npc_template.xml"));
		for (int npcId : new int[] { 206378, 206379, 206380, 206395, 206396, 206397 }) {
			assertFalse(npcs.contains("npc_id=\"" + npcId + "\" ai=\"abiso\"")
					|| npcs.contains("npc_id=\"" + npcId + "\" ai=\"proqura\""), "legacy ai for " + npcId);
		}
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
