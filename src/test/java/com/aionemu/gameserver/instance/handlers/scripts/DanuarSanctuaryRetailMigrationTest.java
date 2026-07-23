package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DanuarSanctuaryRetailMigrationTest {

	@Test
	void importsTrueServerFinalBossRandomGroup() throws Exception {
		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = block(conditions, "<world id=\"301380000\"", "</world>");
		assertEquals(5, count(world, "<variable "));
		assertEquals(9, count(world, "<condition "));
		assertTrue(world.contains("cSetPortal == 3"));
		String randomBoss = block(world, "<condition id=\"301380001\"", "</condition>");
		assertTrue(randomBoss.contains("expression=\"1\""));
		assertTrue(randomBoss.contains("source=\"IDLDF5_Under_02_E/world_N.xml#unconditional-random-1\""));
		for (String candidate : new String[] {
				"id=\"235624\" probability=\"3333\"", "id=\"235625\" probability=\"3333\"",
				"id=\"235626\" probability=\"3334\"" }) {
			assertTrue(randomBoss.contains(candidate), candidate);
		}
		assertEquals(3, count(randomBoss, "x=\"1056.595337\" y=\"693.456970\" z=\"287.991913\""));

		String patterns = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idldf5_under_02_jsm.xml"));
		for (String pattern : new String[] { "IDLDF5_Under_02_Summon03", "IDLDF5_Under_02_Summon04",
				"IDLDF5_Under_02_Summon05" }) {
			assertTrue(block(patterns, "<name>" + pattern + "</name>", "</npc_ai_pattern>")
				.contains("<string>cSetPortal</string>"), pattern);
		}

		String handler = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/DanuarSanctuaryInstance.java"));
		assertFalse(handler.contains("spawnDanuarSanctuaryBoss"));
		assertFalse(handler.contains("case 235624"));
		assertFalse(handler.contains("spawn(701876"));
		assertTrue(handler.contains("spawn(danuarGuard1"));
		assertFalse(handler.contains("185000181"));
		assertFalse(handler.contains("onPlayerLogOut"));
		assertFalse(handler.contains("onLeaveInstance"));
		String items = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
		for (int itemId = 185000181; itemId <= 185000183; itemId++) {
			assertTrue(block(items, "<item_template id=\"" + itemId + "\"", "</item_template>")
				.contains("ownership_world=\"301140000\""), Integer.toString(itemId));
		}

		String coverage = Files.readAllLines(Path.of(
				"src/main/resources/aion/definitions/compact/instance/coverage.xml")).stream()
			.filter(line -> line.contains("id=\"301380000\"")).findFirst().orElseThrow();
		assertTrue(coverage.contains("3333/3333/3334 final-boss random group and Pattern own the final encounter"));
		assertFalse(coverage.contains("item cleanup"));
	}

	private static String block(String value, String startMarker, String endMarker) {
		int start = value.indexOf(startMarker);
		int end = value.indexOf(endMarker, start);
		return value.substring(start, end + endMarker.length());
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
