package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AturamSkyFortressMigrationTest {

	@Test
	void formalAndEventHandlersOnlyKeepFlyingRingCleanup() throws Exception {
		for (String relative : new String[] { "AturamSkyFortressInstance.java",
			"event/Event_AturamSkyFortressInstance.java" }) {
			String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts", relative));
			assertTrue(source.contains("ATURAM_SKY_FORTRESS_3"));
			assertTrue(source.contains("setDoorState(177, true)"));
			for (String legacy : new String[] { "onDropRegistered", "onDie(", "handleUseItemFinish",
				"GameThreadPoolServices", "Future<", "AbyssPointsService", "ItemService.addItem", "protected void sp" }) {
				assertFalse(source.contains(legacy), relative + ": " + legacy);
			}
		}

		String formal = source("AturamSkyFortressInstance.java");
		assertFalse(formal.contains("Storage"));
		assertFalse(formal.contains("decreaseByItemId"));
		assertEquals(3, count(formal, "removeEffects(player);"));

		String event = source("event/Event_AturamSkyFortressInstance.java");
		assertEquals(2, count(event, "decreaseByItemId"));
		assertTrue(event.contains("onPlayerLogOut(Player player) {\n\t\tremoveEffects(player);"));
		assertTrue(event.contains("onLeaveInstance(Player player) {\n\t\tcleanup(player);"));
	}

	@Test
	void retailDataOwnsFormalAndEventEncounters() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		var coverage = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		for (String worldId : new String[] { "300240000", "300241000" }) {
			String world = worldBlock(conditions, worldId);
			assertEquals(2, count(world, "<variable "), worldId);
			assertEquals(13, count(world, "<condition "), worldId);
			assertEquals(13, count(world, "<npc "), worldId);
			assertTrue(world.contains("<variable name=\"door_01\"/>"), worldId);
			assertTrue(world.contains("<variable name=\"door_02\"/>"), worldId);

			String ownership = coverage.stream().filter(line -> line.contains("id=\"" + worldId + "\""))
				.findFirst().orElseThrow();
			assertTrue(ownership.contains("retail static/condition spawns, waypoint and Pattern own encounters and door variables"), worldId);
			assertTrue(ownership.contains("handler only owns three flying rings, ring effect cleanup and door 177 opening"), worldId);
		}
	}

	@Test
	void formalWorldOwnsBothTemporaryItems() throws Exception {
		String items = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
		for (String itemId : new String[] { "164000163", "164000202" }) {
			int start = items.indexOf("<item_template id=\"" + itemId + "\"");
			String item = items.substring(start, items.indexOf("</item_template>", start));
			assertTrue(item.contains("ownership_world=\"300240000\""), itemId);
		}
	}

	private static String source(String relative) throws Exception {
		return Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts", relative));
	}

	private static String worldBlock(String conditions, String worldId) {
		int start = conditions.indexOf("<world id=\"" + worldId + "\"");
		return conditions.substring(start, conditions.indexOf("</world>", start));
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
