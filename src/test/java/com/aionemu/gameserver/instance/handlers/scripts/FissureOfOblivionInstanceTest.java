package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 锁定忘却之裂缝空气墙、唯一房间触发器和任务出口的生成与清理合同。
 * Locks the spawn and cleanup contracts for Fissure of Oblivion air walls, its single room trigger, and quest exit.
 */
class FissureOfOblivionInstanceTest {

	private static final Path INSTANCE_SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/FissureOfOblivionInstance.java");
	private static final Path FIRST_ROOM_AI_SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/fissureOfOblivion/IDTransformTransRoom01AI2.java");
	private static final Path FOURTH_ROOM_AI_SOURCE = Path.of(
			"src/main/java/com/aionemu/gameserver/ai/instance/fissureOfOblivion/IDTransformTransRoom04AI2.java");
	private static final Path INSTANCE_SPAWNS = Path.of(
			"src/main/resources/aion/data/static_data/spawns/Instances/302100000_Fissure_Of_Oblivion.xml");
	private static final Path NPC_TEMPLATES = Path.of(
			"src/main/resources/aion/data/static_data/npcs/npc_template.xml");
	private static final Path PORTAL_TEMPLATES = Path.of(
			"src/main/resources/aion/data/static_data/portals/portal_template2.xml");
	private static final Path QUEST_DEFINITION = Path.of(
			"src/main/resources/aion/data/static_data/quest_definition/quests/17510.xml");

	@Test
	void fourthMinionKillDeletesEveryFirstRoomAirWallController() throws Exception {
		String onDie = methodBody(Files.readString(INSTANCE_SOURCE), "public void onDie(Npc npc)");
		int firstStageStart = onDie.indexOf("if (killCounters[idx] == 4)");
		assertTrue(firstStageStart >= 0, "the fourth minion kill must trigger the first-stage cleanup");
		String firstStage = blockBody(onDie, firstStageStart);
		int killControllers = firstStage.indexOf("killNpc(getNpcs(245402));");
		int deleteControllers = firstStage.indexOf("deleteNpcs(245402);");

		assertTrue(killControllers >= 0, "the first stage must kill controller 245402");
		assertTrue(deleteControllers > killControllers, "every 245402 controller must be deleted after it is killed");
		assertTrue(onDie.contains("else if (killCounters[idx] == 12)"),
				"the three room gates must keep their 4/8/12 progression");
	}

	@Test
	void firstRoomSpawnsTwoControllersAndCleanupDeletesBoth() throws Exception {
		String room66 = methodBody(Files.readString(FIRST_ROOM_AI_SOURCE),
				"private void IDTransformTransRoom01_66()");
		assertTrue(room66.contains("spawn(245402, 762.08215f, 514.16248f, 346.31735f, (byte) 0, 18);"));
		assertTrue(room66.contains("spawn(245402, 762.08215f, 514.16248f, 346.31735f, (byte) 0, 107);"));

		String deleteNpcs = methodBody(Files.readString(INSTANCE_SOURCE), "private void deleteNpcs(int npcId)");
		assertTrue(deleteNpcs.contains("List<Npc> npcs = getNpcs(npcId);"));
		assertTrue(deleteNpcs.contains("for (Npc npc : npcs)"));
		assertTrue(deleteNpcs.contains("npc.getController().onDelete();"));
		assertFalse(deleteNpcs.contains("getNpc(npcId)"), "cleanup must not stop after the first matching controller");
	}

	@Test
	void firstRoomKeepsOnlyTheRetailTransformationTrigger() throws Exception {
		NodeList spots = nodes(INSTANCE_SPAWNS,
				"/spawns/spawn_map[@map_id='302100000']/spawn[@npc_id='245396']/spot");
		assertEquals(1, spots.getLength(), "a second trigger can recreate the paired air walls after cleanup");

		Element spot = (Element) spots.item(0);
		assertEquals("745.604980", spot.getAttribute("x"));
		assertEquals("513.935608", spot.getAttribute("y"));
		assertEquals("340.437042", spot.getAttribute("z"));
	}

	@Test
	void hiddenRoomControllersCannotInterceptBossAttacks() throws Exception {
		for (int npcId = 245402; npcId <= 245405; npcId++) {
			Element controller = (Element) nodes(NPC_TEMPLATES,
					"/npc_templates/npc_template[@npc_id='" + npcId + "']").item(0);
			assertEquals("NON_ATTACKABLE", controller.getAttribute("npc_type"),
					"hidden room controller " + npcId + " must not become a hostile client target");
		}

		for (int npcId = 244490; npcId <= 244494; npcId++) {
			Element boss = (Element) nodes(NPC_TEMPLATES,
					"/npc_templates/npc_template[@npc_id='" + npcId + "']").item(0);
			assertEquals("ATTACKABLE", boss.getAttribute("npc_type"),
					"Shadow of Oblivion form " + npcId + " must remain attackable");
		}

		String room66 = methodBody(Files.readString(FOURTH_ROOM_AI_SOURCE),
				"private void IDTransformTransRoom04_66()");
		int controllerSpawn = room66.indexOf(
				"spawn(245405, 301.12494f, 513.34650f, 352.99631f, (byte) 0, 33);");
		int bossSpawn = room66.indexOf(
				"spawn(244490, 301.1525f, 512.97736f, 350.8281f, (byte) 0);");
		assertTrue(controllerSpawn >= 0, "the fourth-room air-wall controller must still spawn");
		assertTrue(bossSpawn > controllerSpawn,
				"the attackable boss must spawn after the overlapping non-attackable controller");
	}

	@Test
	void entranceKeepsOnlyTheQuestAndPortalOwnedExit() throws Exception {
		assertEquals(1, nodes(INSTANCE_SPAWNS,
				"/spawns/spawn_map[@map_id='302100000']/spawn[@npc_id='834194']").getLength());
		assertEquals(0, nodes(INSTANCE_SPAWNS,
				"/spawns/spawn_map[@map_id='302100000']/spawn[@npc_id='834195']").getLength());
		assertTrue(nodes(PORTAL_TEMPLATES, "//portal_dialog[@npc_id='834194']/portal_path").getLength() > 0);
		assertTrue(nodes(QUEST_DEFINITION, "//dialog[@npc-id='834194']").getLength() > 0);
		assertEquals(0, nodes(QUEST_DEFINITION, "//dialog[@npc-id='834195']").getLength());
	}

	private static NodeList nodes(Path path, String expression) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try (InputStream input = Files.newInputStream(path)) {
			return (NodeList) XPathFactory.newInstance().newXPath().evaluate(expression,
					factory.newDocumentBuilder().parse(input), XPathConstants.NODESET);
		}
	}

	private static String methodBody(String source, String signature) {
		int signatureStart = source.indexOf(signature);
		assertTrue(signatureStart >= 0, signature + " must exist");
		return blockBody(source, signatureStart);
	}

	private static String blockBody(String source, int blockStart) {
		int bodyStart = source.indexOf('{', blockStart);
		assertTrue(bodyStart >= 0, "block must have a body");

		int depth = 0;
		for (int i = bodyStart; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0) {
					return source.substring(bodyStart + 1, i);
				}
			}
		}
		throw new AssertionError("block body was not closed");
	}
}
