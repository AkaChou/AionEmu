package com.aionemu.gameserver.instance.handlers.scripts;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrakenspireDepthsRetailMigrationTest {

	@Test
	void retailDataOwnsNormalAndQuestFlows() throws Exception {
		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String normal = worldBlock(conditions, "301390000");
		String quest = worldBlock(conditions, "301520000");
		assertEquals(63, count(normal, "<condition "));
			assertEquals(37, count(quest, "<condition "));
		for (String variable : new String[] { "oritsa_summon", "twin_resurrect", "vritra_timer", "wake_timer" }) {
			assertTrue(normal.contains(variable), variable);
		}
		for (String variable : new String[] { "bossroom", "mob2", "mob3", "vritra", "wave_leader" }) {
			assertTrue(quest.contains(variable), variable);
		}

		String normalPatterns = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idseal_named_yjh.xml"))
				+ Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idseal_wave_yjh.xml"))
				+ Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idseal_twin_yjh.xml"));
		for (String pattern : new String[] { "IDSeal_Boss_Lv1", "IDSeal_Wave1_Leader_Lv1", "IDSeal_Twin_P" }) {
			assertTrue(normalPatterns.contains("<name>" + pattern + "</name>"), pattern);
		}
			String questPatterns = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/npcaipatterns_idseal_q_yjh.xml"));
		for (String pattern : new String[] { "IDSeal_Q_keyNamed", "IDSeal_Q_Twin_P", "IDSeal_Q_Twin_M" }) {
				assertTrue(questPatterns.contains("<name>" + pattern + "</name>"), pattern);
			}
			String npcTemplates = Files.readString(Path.of(
					"src/main/resources/aion/data/static_data/npcs/npc_template.xml"));
			for (int npcId : new int[] { 805744, 805745 }) {
				assertTrue(npcTemplates.matches("(?s).*npc_id=\"" + npcId + "\"[^>]*ai=\"portal_dialog\".*"),
						Integer.toString(npcId));
			}
			assertTrue(npcTemplates.matches("(?s).*npc_id=\"805377\"[^>]*ai=\"general\".*"));
			String portals = Files.readString(Path.of(
					"src/main/resources/aion/data/static_data/portals/portal_template2.xml"));
			for (int npcId : new int[] { 805744, 805745 }) {
				assertTrue(portals.contains("<portal_dialog npc_id=\"" + npcId + "\">"), Integer.toString(npcId));
			}
			assertTrue(portals.contains("destination_alias=\"IDSeal_Q_Boss_Point\""));

			assertMinimalHandler("DrakenspireDepthsInstance.java");
			String questHandler = assertMinimalHandler("DrakenspireDepthsQInstance.java");
			assertTrue(questHandler.contains("185000219"));
			assertTrue(questHandler.contains("22778"));
			assertTrue(questHandler.contains("22779"));
			assertTrue(questHandler.contains("onPlayerLogOut(Player player) {\n\t\tremoveEffects(player);"));
			assertTrue(questHandler.contains("onLeaveInstance(Player player) {\n\t\tcleanup(player);"));
			String items = Files.readString(Path.of(
				"src/main/resources/aion/data/static_data/items/item/item_misc_templates.xml"));
			assertTrue(itemTemplateBlock(items, 185000219).contains("ownership_world=\"301390000\""));

		var coverage = Files.readAllLines(Path.of(
			"src/main/resources/aion/definitions/compact/instance/coverage.xml"));
		String normalOwnership = coverage.stream().filter(line -> line.contains("id=\"301390000\""))
			.findFirst().orElseThrow();
		assertTrue(normalOwnership.contains("retail static/condition spawns and Pattern own waves, twins, Orisza and Beritra flow"));
		assertTrue(normalOwnership.contains("855461 Pattern remains rejected because waypoint-start pathname is empty"));
		assertTrue(normalOwnership.contains("handler only owns exit"));
		String questOwnership = coverage.stream().filter(line -> line.contains("id=\"301520000\""))
			.findFirst().orElseThrow();
		assertTrue(questOwnership.contains("retail static/condition spawns and Pattern own quest waves, twins and Beritra flow"));
			assertTrue(questOwnership.contains("handler owns normal-leave item 185000219 cleanup, logout/leave effects 22778-22779 and exit"));
	}

	private static String assertMinimalHandler(String file) throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/" + file));
		assertFalse(source.contains("GameThreadPoolServices"));
		assertFalse(source.contains("Future"));
		assertFalse(source.contains("onDropRegistered"));
		assertFalse(source.contains("onDie"));
		assertFalse(source.contains("spawn("));
		assertTrue(source.contains("extends GeneralInstanceHandler"));
		return source;
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
