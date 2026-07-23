package com.aionemu.gameserver.instance.handlers.scripts.illuminaryObelisk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class IlluminaryObeliskMigrationTest {
	private static final Path HANDLERS = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/illuminaryObelisk");

	@Test
	void handlersKeepOnlyTheirActualExitItemOwners() throws Exception {
		for (String file : new String[] { "IlluminaryObeliskInstance.java",
			"Infernal_IlluminaryObeliskInstance.java" }) {
			String source = Files.readString(HANDLERS.resolve(file));
			assertTrue(source.contains("decreaseByItemId(164000289"));
			assertTrue(source.contains("decreaseByItemId(164000290"));
			assertTrue(source.contains("moveToInstanceExit"));
			assertTrue(source.contains("onExitInstance(Player player) {\n\t\tremoveItems(player);"));
			assertFalse(source.contains("onPlayerLogOut("));
			for (String legacy : new String[] { "Future<", "GameThreadPoolServices", "onDropRegistered",
				"handleUseItemFinish", "onDie(", "spawn(", "sendMessage(", "StaticDoor" }) {
				assertFalse(source.contains(legacy), file + " still contains " + legacy);
			}
		}
		assertFalse(Files.readString(HANDLERS.resolve("IlluminaryObeliskInstance.java"))
			.contains("onLeaveInstance("));
		assertTrue(Files.readString(HANDLERS.resolve("Infernal_IlluminaryObeliskInstance.java"))
			.contains("onLeaveInstance(Player player) {\n\t\tremoveItems(player);"));
	}

	@Test
	void temporaryItemsBelongToTheFormalWorld() throws Exception {
		String items = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
		for (int itemId : new int[] { 164000289, 164000290 }) {
			int start = items.indexOf("<item_template id=\"" + itemId + "\"");
			String item = items.substring(start, items.indexOf("</item_template>", start));
			assertTrue(item.contains("ownership_world=\"301230000\""), Integer.toString(itemId));
		}
	}

	@Test
	void retailAiAndConditionSpawnsOwnTheEncounter() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		for (String worldId : new String[] { "301230000", "301370000" }) {
			String world = worldBlock(conditions, worldId);
			for (String variable : new String[] { "h_wave_01_01", "h_wave_01_03", "h_wave_02_01",
				"h_wave_02_03", "h_wave_03_01", "h_wave_03_03", "h_wave_04_01" }) {
				assertTrue(world.contains("<variable name=\"" + variable + "\""), worldId + ':' + variable);
			}
		}

		String patterns = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idldf5_under_03_yjh.xml"));
		for (String pattern : new String[] { "IDF5_U3_StartNPC", "IDF5_U3_GameTimer", "IDF5_U3_BossTimer",
			"IDF5_U3_DEF_CTRL_01", "IDF5_U3_DEF_CTRL_02", "IDF5_U3_DEF_CTRL_03",
			"IDF5_U3_DEF_CTRL_04", "IDF5_U3_TimeOver", "IDF5_U3_Hard_StartNPC",
			"IDF5_U3_Hard_GameTimer" }) {
			assertTrue(patterns.contains("<name>" + pattern + "</name>"), pattern);
		}
	}

	@Test
	void retailDropsOwnResourcesAndEventBoxes() throws Exception {
		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_018.xml"));
		for (String mapping : new String[] { "730884:164000289", "730885:164000290",
			"702658:188053579", "702659:188053580" }) {
			String[] ids = mapping.split(":");
			assertTrue(drops.matches("(?s).*<npc_drop npc_id=\"" + ids[0]
				+ "\">.*?<drop item_id=\"" + ids[1] + "\".*"));
		}
	}

	private String worldBlock(String conditions, String worldId) {
		int start = conditions.indexOf("<world id=\"" + worldId + "\"");
		int end = conditions.indexOf("</world>", start);
		assertTrue(start >= 0 && end > start, worldId);
		return conditions.substring(start, end);
	}
}
