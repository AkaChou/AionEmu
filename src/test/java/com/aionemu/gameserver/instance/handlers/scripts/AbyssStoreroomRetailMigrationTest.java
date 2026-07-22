package com.aionemu.gameserver.instance.handlers.scripts;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbyssStoreroomRetailMigrationTest {

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
