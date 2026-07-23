package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SmolderingFireTempleRetailMigrationTest {

	private static final Path HANDLER = Path.of(
		"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/SmolderingFireTempleInstance.java");
	private static final Path SPAWNS = Path.of(
		"src/main/resources/aion/data/static_data/spawns/Instances/302000000_Smoldering_Fire_Temple.xml");

	@Test
	void retailConditionsOwnNormalAndMasterProgression() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String world = worldBlock(conditions, "302000000");
		assertEquals(11, count(world, "<variable "));
		assertEquals(672, count(world, "<condition "));
		assertEquals(672, count(world, "<npc "));
		assertEquals(672, count(world, "despawn_at_attack_state=\"true\""));
		assertEquals(533, count(world, "IDDF2_Dflame_Event_Reward == 0"));
		assertEquals(139, count(world, "IDDF2_Dflame_Event_Reward == 1"));

		for (String variable : new String[] { "STAGE1_WAVE", "STAGE1_END", "STAGE2_WAVE", "STAGE2_END",
				"STAGE3_WAVE", "BOSSROOM_START", "BOSSROOM_WAVE", "BOSSROOM_N2", "BOSSROOM_N3",
				"BOSSROOM_N4", "IDDF2_Dflame_Event_Reward" }) {
			assertTrue(world.contains("<variable name=\"" + variable + "\""), variable);
		}
		for (String npcId : new String[] { "244100", "245203", "834058", "834212", "834068", "730051" }) {
			assertTrue(world.contains("<npc id=\"" + npcId + "\""), npcId);
		}
		for (String page : new String[] { "page_start=\"1\" page_end=\"1\"",
				"page_start=\"2\" page_end=\"2\"", "page_start=\"0\" page_end=\"255\"" }) {
			assertTrue(world.contains(page), page);
		}
		for (String expression : new String[] {
				"(STAGE1_WAVE &gt;= 11) &amp;&amp; (STAGE1_END == 0)",
				"(STAGE2_END == 1) &amp;&amp; (BOSSROOM_START == 0)",
				"(BOSSROOM_START == 1) &amp;&amp; (BOSSROOM_WAVE &gt;= 20)",
				"IDDF2_Dflame_Event_Reward == 1" }) {
			assertTrue(world.contains(expression), expression);
		}
		assertTrue(world.contains("walker=\"retail:302000000:npcpathiddf2_dflame_event_6\""));
		assertTrue(world.contains("initial_delay=\"1\" initial_delay_extra=\"1\" respawn_time=\"600\""
			+ " respawn_time_extra=\"5\""));
	}

	@Test
	void handlerAndStaticSpawnsDoNotDuplicateRetailLifecycle() throws Exception {
		String handler = Files.readString(HANDLER);
		assertTrue(handler.contains("RetailConditionSpawnEngine.initialize(instance)"));
		assertTrue(handler.contains("RetailConditionSpawnEngine.setVariable(instance,"
			+ " \"IDDF2_Dflame_Event_Reward\", 1, 0)"));
		assertTrue(handler.contains("DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId()) != null"));
		assertTrue(handler.contains("case 244095, 245198 -> setDoorState(8, true)"));
		assertTrue(handler.contains("case 244100, 245203 -> startSettlement(kill.killedAt())"));
		assertTrue(handler.contains("Math.max(killTime(244100), killTime(245203))"));
			for (String legacy : new String[] { "spawnBoss", "spawnOnce", "guardianUnlocked",
					"smolder.guardian_unlocked_at", "SCORE_NPCS", "BOSS_X", "834066", "834067", "834068",
					"removeItems", "decreaseByItemId" }) {
				assertFalse(handler.contains(legacy), legacy);
			}
			String items = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
			for (int itemId : new int[] { 162002085, 162002086, 162002087, 162002088, 162002089, 162002090,
					185000270 }) {
				assertTrue(itemTemplateBlock(items, itemId).contains("ownership_world=\"302000000\""),
					Integer.toString(itemId));
			}
			String instanceService = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/services/instance/InstanceService.java"));
			assertTrue(instanceService.contains("getOwnershipWorld() == player.getWorldId()"));

		String spawns = Files.readString(SPAWNS);
		for (String retained : new String[] { "244094", "244095", "244096", "244435", "834055", "834056",
				"834057" }) {
			assertTrue(spawns.contains("npc_id=\"" + retained + "\""), retained);
		}
		for (String removed : new String[] { "244084", "244085", "244086", "244087", "244088", "244089",
				"244091", "244092", "244093", "730051", "834058", "834059", "834060", "834061", "834065",
				"834069", "834071" }) {
			assertFalse(spawns.contains("npc_id=\"" + removed + "\""), removed);
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

	private static String itemTemplateBlock(String items, int itemId) {
		int start = items.indexOf("<item_template id=\"" + itemId + "\"");
		return items.substring(start, items.indexOf("</item_template>", start));
	}
}
