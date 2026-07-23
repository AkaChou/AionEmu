package com.aionemu.gameserver.instance.handlers.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrakenseerLairRetailMigrationTest {

	@Test
	void retailDataOwnsTheCompleteInstanceFlow() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = block(conditions, "<world id=\"301620000\"", "</world>");
		assertEquals(12, count(world, "<variable "));
		assertEquals(133, count(world, "<condition "));
		assertEquals(133, count(world, "<slot>"));
		assertEquals(100, Pattern.compile("<condition [^>]*expression=\"[^\"]*Wave_Control_")
			.matcher(world).results().count());
		for (String variable : new String[] { "iddf3_dragon_t_boss", "idf6_dragon_start", "no_exp",
				"wave_control_01", "wave_control_02", "wave_control_03" }) {
			assertTrue(world.contains("<variable name=\"" + variable + "\"/>"), variable);
		}
		for (String npcId : new String[] { "248970", "248972", "248976", "220450", "806240", "857973",
				"857974", "857975", "857976", "857977" }) {
			assertTrue(world.contains("<npc id=\"" + npcId + "\""), npcId);
		}
		assertTrue(world.contains("expression=\"End_Boss_Die == 1\""));
		assertTrue(world.contains("expression=\"boss_summon == 2\""));
		assertTrue(world.contains("<sensory_area bottom=\"251.100006\" top=\"351.100006\">"));

		String staticSpawns = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/301620000_Drakenseer's_Lair.xml"));
		assertEquals(2, count(staticSpawns, "<spawn npc_id="));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"806240\" respawn_time=\"1\">"));
		assertTrue(staticSpawns.contains("<spawn npc_id=\"703154\" respawn_time=\"1\">"));
		assertFalse(staticSpawns.contains("entity_id="));

		// Drakenseer's Lair 已无 Instance Handler，retail 条件出生 + Pattern 接管全部流程。
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DrakenseerLairInstance.java")));

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_009.xml"));
		String bossDrops = block(drops, "<npc_drop npc_id=\"220450\">", "</npc_drop>");
		assertEquals(6, count(bossDrops, "<common_drop_group "));
		for (String group : new String[] { "IDF6_DRAGON_ACCESSORY_M_69A", "IDF6_DRAGON_EQUIP_BOX_M2_73A",
				"IDF6_DRAGON_MATTER_PROC_U_73A", "IDF6_DRAGON_TREASURE_R_69A",
				"IDF6_DRAGON_SUB_MATTER_M_69A", "MATTER_ENCHANT_CPSTONE_ID_01" }) {
			assertTrue(bossDrops.contains("name=\"" + group + "\""), group);
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
