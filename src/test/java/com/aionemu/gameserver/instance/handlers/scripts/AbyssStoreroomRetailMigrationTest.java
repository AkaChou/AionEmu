package com.aionemu.gameserver.instance.handlers.scripts;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbyssStoreroomRetailMigrationTest {
	private static final Path HANDLERS = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts");

	@Test
	void retailDataOwnsStoreroomMechanics() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		int[] worldIds = { 300120000, 300130000, 300140000 };
		int[] conditionCounts = { 12, 11, 11 };
		int[] npcCounts = { 32, 38, 38 };
		for (int i = 0; i < worldIds.length; i++) {
			String world = block(conditions, "<world id=\"" + worldIds[i] + "\"", "</world>");
			assertEquals(conditionCounts[i], count(world, "<condition "));
			assertEquals(npcCounts[i], count(world, "<npc "));
		}

		String[] spawnFiles = {
			"300120000_Grave_Of_Steel_Storeroom.xml",
			"300130000_Twilight_Battlefield_Storeroom.xml",
			"300140000_Isle_Of_Roots_Storeroom.xml"
		};
		int[] spotCounts = { 212, 219, 221 };
		for (int i = 0; i < spawnFiles.length; i++) {
			String spawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/" + spawnFiles[i]));
			assertEquals(spotCounts[i], count(spawns, "<spot "));
			assertFalse(spawns.contains("731580"));
			assertFalse(spawns.contains("254574"));
		}

		String chests = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/chests/chest_templates.xml"));
		String npcTemplates = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/npcs/npc_template.xml"));
		for (String npcId : new String[] { "700540", "700542", "700544" }) {
			assertTrue(chests.contains("<chest npcid=\"" + npcId + "\">"), npcId);
			String template = block(npcTemplates, "<npc_template npc_id=\"" + npcId + "\"", "</npc_template>");
			assertTrue(template.contains("ai=\"chest\""), npcId);
		}

		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_005.xml"));
		for (String group : new String[] { "CROTAN", "DKISAS", "LAMIREN" }) {
			assertTrue(drops.contains("KEY_IDAB1_REWARD_" + group), group);
		}
		for (String itemId : new String[] { "185000059", "185000060", "185000064", "185000065",
			"185000069", "185000070" }) {
			assertTrue(drops.contains("item_id=\"" + itemId + "\""), itemId);
		}
		for (String wrong : new String[] { "KEY_IDAB1_REWARD_ROOT", "KEY_IDAB1_REWARD_IRON",
			"KEY_IDAB1_REWARD_TWILIGHT", "185000251", "185000256", "185000261" }) {
			assertFalse(drops.contains(wrong), wrong);
		}

		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/AbyssStoreroomInstance.java"));
		assertTrue(handler.contains("onPlayerLogOut"));
		for (String legacy : new String[] { "onLeaveInstance", "onDropRegistered", "onDie(", "scheduleDeadline",
			"spawn(", "CHEST_STAGE_DURATION", "Config" }) {
			assertFalse(handler.contains(legacy), legacy);
		}

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		for (int worldId : worldIds) {
			String world = block(coverage, "<world ", "/>", coverage.indexOf("id=\"" + worldId + "\""));
			assertTrue(world.contains("behavior=\"HANDLER\""), Integer.toString(worldId));
			assertTrue(world.contains("handler logout key cleanup"), Integer.toString(worldId));
		}
	}

	@Test
	void retailDataAndHandlersOwnLowerStoreroomMechanics() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		int[] worldIds = { 300050000, 300060000, 300070000, 300080000, 300090000 };
		int[] conditionCounts = { 12, 156, 12, 6, 274 };
		int[] npcCounts = { 12, 468, 12, 168, 826 };
		for (int i = 0; i < worldIds.length; i++) {
			String world = block(conditions, "<world id=\"" + worldIds[i] + "\"", "</world>");
			assertEquals(conditionCounts[i], count(world, "<condition "));
			assertEquals(conditionCounts[i], count(world, "<slot>"));
			assertEquals(npcCounts[i], count(world, "<npc "));
			assertTrue(world.contains("<variable name=\"lightin\"/>"));
			assertTrue(world.contains("<variable name=\"darkin\"/>"));
		}

		String[] spawnFiles = {
			"300050000_Carpus_Isle_Storeroom.xml",
			"300060000_Sulfur_Tree_Nest.xml",
			"300070000_Hamate_Isle_Storeroom.xml",
			"300080000_Left_Wing_Chamber.xml",
			"300090000_Right_Wing_Chamber.xml"
		};
		int[] spotCounts = { 64, 238, 66, 197, 172 };
		for (int i = 0; i < spawnFiles.length; i++) {
			String spawns = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/spawns/Instances/" + spawnFiles[i]));
			assertEquals(spotCounts[i], count(spawns, "<spot "), spawnFiles[i]);
			assertTrue(spawns.contains("npc_id=\"283080\""), spawnFiles[i]);
		}
		String drops = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/npc_drops/npc_drops_part_005.xml"));
		String items = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
		for (int itemId = 185000033; itemId <= 185000038; itemId++) {
			assertTrue(drops.contains("item_id=\"" + itemId + "\""), Integer.toString(itemId));
		}
		for (String ownership : new String[] { "ownership_world=\"300050000\"",
			"ownership_world=\"300070000\"" }) {
			assertTrue(items.contains(ownership), ownership);
		}

		String carpus = Files.readString(HANDLERS.resolve("CarpusIsleStoreroomInstance.java"));
		String hamate = Files.readString(HANDLERS.resolve("HamateIsleStoreroomInstance.java"));
		for (String handler : new String[] { carpus, hamate }) {
			assertTrue(handler.contains("onPlayerLogOut"));
			assertTrue(handler.contains("new SM_QUEST_ACTION"));
			assertFalse(handler.contains("onLeaveInstance"));
			assertFalse(handler.contains("onDropRegistered"));
		}
		String left = Files.readString(HANDLERS.resolve("LeftWingChamberInstance.java"));
		assertTrue(left.contains("CHEST_STAGE_COUNT = 6"));
		assertFalse(left.contains("CHEST_POSITIONS"));
		String right = Files.readString(HANDLERS.resolve("RightWingChamberInstance.java"));
		assertTrue(right.contains("CHEST_DURATION = 15 * 60_000L"));
		assertFalse(right.contains("spawnTreasureBoxes"));
		String sulfur = Files.readString(HANDLERS.resolve("SulfurTreeNestInstance.java"));
		assertTrue(sulfur.contains("else if (deadline > 0 && deadline <= System.currentTimeMillis())"));
		assertTrue(sulfur.contains("expire();"));

		String coverage = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		for (int worldId : worldIds) {
			String world = block(coverage, "<world ", "/>", coverage.indexOf("id=\"" + worldId + "\""));
			assertTrue(world.contains("behavior=\"HANDLER\""), Integer.toString(worldId));
		}
	}

	private static String block(String source, String startToken, String endToken) {
		int start = source.indexOf(startToken);
		int end = source.indexOf(endToken, start);
		return source.substring(start, end);
	}

	private static String block(String source, String startToken, String endToken, int position) {
		int start = source.lastIndexOf(startToken, position);
		int end = source.indexOf(endToken, position);
		return source.substring(start, end);
	}

	private static int count(String value, String token) {
		return (value.length() - value.replace(token, "").length()) / token.length();
	}
}
