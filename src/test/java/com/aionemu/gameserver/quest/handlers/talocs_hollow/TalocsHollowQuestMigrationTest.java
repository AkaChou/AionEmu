package com.aionemu.gameserver.quest.handlers.talocs_hollow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class TalocsHollowQuestMigrationTest {

	private static final Path HANDLER = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TalocsHollowInstance.java");
	private static final Path ELYOS_QUEST = Path.of(
			"src/main/java/com/aionemu/gameserver/quest/handlers/inggison/_10032Help_In_The_Hollow.java");
	private static final Path ASMODIAN_QUEST = Path.of(
			"src/main/java/com/aionemu/gameserver/quest/handlers/gelkmaros/_20032All_About_Abnormal_Aether.java");
	private static final Path SPAWNS = Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/300190000_Taloc's_Hollow.xml");

	@Test
	void questEntriesGrantBothItemsAndRollThemBackWhenTeleportFails() throws Exception {
		assertPairedEntry(ELYOS_QUEST, "FRUIT", "TEARS");
		assertPairedEntry(ASMODIAN_QUEST, "TEARS", "FRUIT");
	}

	@Test
	void handlerKeepsOnlyRetailGapsAndUsesRetailHealingSkills() throws Exception {
		String source = Files.readString(HANDLER);
		assertTrue(source.contains("case 700940 -> 19229"));
		assertTrue(source.contains("case 700941 -> 19230"));
		assertTrue(source.contains("if (GameEngineServices.skillEngine().getSkill(npc, skillId, 1, player).useNoAnimationSkill())"));
		assertTrue(source.contains("sendMovie(player, 434)"));
		assertTrue(source.contains("sendMovie(player, 438)"));
		assertTrue(source.contains("sendMovie(player, 463)"));
		assertTrue(source.contains("sendMovie(player, 464)"));
		for (String forbidden : new String[] { "onDropRegistered", "onDie(", "increaseHp(", "Future<?>",
				"182215618", "182215619", "182215592", "182215593", "188900011", "170170044",
				"HTMLService", "sendMsgByRace", "spawnHugeInsectEgg" }) {
			assertFalse(source.contains(forbidden), forbidden);
		}
	}

	@Test
	void hugeHealingPlantIsConditionSpawnedAndHugeEggDoesNotRespawn() throws Exception {
		NodeList spawns = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(SPAWNS.toFile())
				.getElementsByTagName("spawn");
		int hugeEggCount = 0;
		for (int index = 0; index < spawns.getLength(); index++) {
			Element spawn = (Element) spawns.item(index);
			assertFalse("700941".equals(spawn.getAttribute("npc_id")));
			if ("700738".equals(spawn.getAttribute("npc_id"))) {
				hugeEggCount++;
				assertFalse(spawn.hasAttribute("respawn_time"));
			}
		}
		assertEquals(1, hugeEggCount);
		String conditions = Files.readString(Path.of(
				"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		assertTrue(conditions.contains("expression=\"IDElim_3F_Heal_Plant_Giant == 50\""));
		assertTrue(conditions.contains("<npc id=\"700941\""));
	}

	private static void assertPairedEntry(Path path, String first, String second) throws Exception {
		String source = Files.readString(path);
		String grant = "List.of(new QuestItems(" + first + ", 1), new QuestItems(" + second + ", 1))";
		int grantIndex = source.indexOf(grant);
		int teleportIndex = source.indexOf("if (!TeleportService2.teleportToInstance", grantIndex);
		int firstRollbackIndex = source.indexOf("removeQuestItem(env, " + first + ", 1)", teleportIndex);
		int secondRollbackIndex = source.indexOf("removeQuestItem(env, " + second + ", 1)", firstRollbackIndex);
		assertTrue(grantIndex >= 0);
		assertTrue(teleportIndex > grantIndex);
		assertTrue(firstRollbackIndex > teleportIndex);
		assertTrue(secondRollbackIndex > firstRollbackIndex);
	}
}
